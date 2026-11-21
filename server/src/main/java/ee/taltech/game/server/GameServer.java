package ee.taltech.game.server;

import ee.taltech.game.server.exceptions.ServerBindingException;
import ee.taltech.game.server.fish.FishCaughtMessage;
import ee.taltech.game.server.fish.FishGameState;
import ee.taltech.game.server.fish.FishState;
import ee.taltech.game.server.fish.FishStateList;
import ee.taltech.game.server.fish.FishingAttempt;
import ee.taltech.game.server.net.MessageCodec;
import ee.taltech.game.server.net.NetServer;
import ee.taltech.game.server.rooms.GameRoom;
import ee.taltech.game.server.rooms.GameRoomList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ee.taltech.game.server.rooms.RoomLoader.saveRooms;

@Slf4j
public class GameServer {

    /** Used when the PORT environment variable (set by Render) is absent, e.g. when running locally. */
    private static final int DEFAULT_PORT = 8080;
    private static final int AI_PLAYER_ID = 999;
    private static final float AI_SPEED_MULTIPLIER = 1.5f;
    private static final int GAME_LENGTH_SECONDS = 120;

    private final NetServer server;
    private final Map<Integer, PlayerData> players = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();
    private final Map<String, Integer> roomTimeLeft = new ConcurrentHashMap<>();

    /** All periodic work runs on this single thread, so the loops never race with each other. */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Sent to clients as a list of rooms. */
    private final List<GameRoom> rooms = new ArrayList<>();
    private volatile WaterTilesMessage lastWaterTilesMessage;

    private boolean aiUpdatesStarted = false;
    private boolean fishUpdatesStarted = false;
    private long lastAIUpdateTime = System.currentTimeMillis();
    private long lastFishUpdateTime = System.currentTimeMillis();

    public GameServer() throws ServerBindingException {
        int port = resolvePort();
        server = new NetServer(port, AI_PLAYER_ID, new NetServer.Listener() {
            @Override
            public void received(int connectionId, Object object) {
                handleMessage(connectionId, object);
            }

            @Override
            public void disconnected(int connectionId) {
                handleDisconnect(connectionId);
            }
        });

        try {
            server.startAndWait(10_000);
        } catch (IOException e) {
            throw new ServerBindingException("Error binding the server to port " + port, e);
        }

        scheduleRepeating(this::broadcastAllPlayers, 0, 100);
        log.info("Server started on port {}!", port);
    }

    /** Render tells the service which port to listen on through the PORT environment variable. */
    private static int resolvePort() {
        String portStr = System.getenv("PORT");
        return portStr != null && !portStr.isBlank() ? Integer.parseInt(portStr.trim()) : DEFAULT_PORT;
    }

    private void handleMessage(int connectionId, Object object) {
        if (object instanceof GameRoomList roomList) {
            removeRooms(roomList.rooms, rooms);
            saveRooms(rooms);
            broadcastUpdatedRoomsToAllClients(rooms);
        } else if (object instanceof GameRoom room) {
            PlayerData data = players.get(connectionId);
            if (data != null) {
                data.roomId = room.getRoomId();
            }
            handleRoomFromClient(room, connectionId);
        } else if (MessageCodec.GET_ROOMS.equals(object)) {
            server.sendTo(connectionId, rooms);
        } else if (object instanceof WaterTilesMessage msg) {
            handleWaterTiles(msg);
        } else if (object instanceof CollisionTilesMessage msg) {
            handleCollisionTiles(msg);
        } else if (object instanceof FishingAttempt attempt) {
            handleFishingAttempt(connectionId, attempt);
        } else if (object instanceof PlayerData data) {
            handlePlayerData(connectionId, data);
        }
    }

    private void handleDisconnect(int disconnectedId) {
        if (players.containsKey(disconnectedId)) {
            players.remove(disconnectedId);

            PlayerData disconnectMsg = new PlayerData();
            disconnectMsg.id = disconnectedId;
            disconnectMsg.isDisconnected = true;
            server.sendToAll(disconnectMsg);

            log.info("Player {} disconnected", disconnectedId);
            players.values().forEach(this::broadcastPositions);
        } else {
            log.debug("Unidentified connection {} disconnected", disconnectedId);
        }
    }

    // ---- incoming messages ------------------------------------------------------------------------------------

    private void handleWaterTiles(WaterTilesMessage msg) {
        lastWaterTilesMessage = msg;

        for (GameRoom room : rooms) {
            AiPlayer ai = room.getAiController();
            if (ai != null) {
                ai.setMapInfo(msg.waterTiles, msg.tileSize);
            }
        }
        startAIUpdates();
    }

    private void handleCollisionTiles(CollisionTilesMessage msg) {
        for (GameRoom room : rooms) {
            AiPlayer ai = room.getAiController();
            if (ai != null) {
                ai.setMapInfo(msg.collisionTiles, msg.tileSize);
                log.info("AI collision map updated for room {}", room.getRoomId());
            }
        }
    }

    private void handleFishingAttempt(int connectionId, FishingAttempt attempt) {
        PlayerData player = players.get(connectionId);
        if (player == null) return;

        player.isFishing = true;
        player.hookX = attempt.x;
        player.hookY = attempt.y;

        broadcastPositions(player);

        if (player.fishCaught == null) {
            player.fishCaught = 0;
        }

        GameRoom room = getRoomById(player.roomId);
        if (room == null || room.getFishGameState() == null) return;

        int gainedScore = room.getFishGameState().handleCatchAttemptAndReturnScore(attempt.x, attempt.y);

        if (gainedScore != 0) {
            player.fishCaught++;
            player.score += gainedScore;
            server.sendToAll(new FishCaughtMessage(player.id, true, player.fishCaught, gainedScore));
            broadcastPositions(player);
            log.info("Player {} caught something! Score: {}", player.id, gainedScore);
        } else {
            server.sendTo(connectionId, new FishCaughtMessage(player.id, false, player.fishCaught, 0));
        }
    }

    private void handlePlayerData(int connectionId, PlayerData data) {
        if (data.id <= 0 || data.x < 0 || data.y < 0) {
            log.warn("Invalid player data received: {}", data);
            return;
        }

        // Score, catch count and room are owned by the server: keep them from the previous snapshot.
        PlayerData existing = players.getOrDefault(connectionId, new PlayerData());
        data.fishCaught = existing.fishCaught;
        data.score = existing.score;
        data.roomId = existing.roomId;
        players.put(connectionId, data);

        broadcastPositions(data);
    }

    // ---- rooms ------------------------------------------------------------------------------------------------

    private GameRoom getRoomById(String roomId) {
        return rooms.stream()
                .filter(room -> Objects.equals(room.getRoomId(), roomId))
                .findFirst()
                .orElse(null);
    }

    private void handleRoomFromClient(GameRoom incomingRoom, int connectionId) {
        AiPlayer ai = new AiPlayer(AI_PLAYER_ID, 1000, 1050);
        ai.setDifficulty(incomingRoom.getAiDifficulty());
        incomingRoom.setAiController(ai);

        if (incomingRoom.getAiPlayer() == null) {
            PlayerData aiPlayer = new PlayerData();
            aiPlayer.id = AI_PLAYER_ID;
            aiPlayer.x = 1000;
            aiPlayer.y = 1050;
            aiPlayer.texture = "catTestPlayer1.png";
            aiPlayer.facingRight = true;
            aiPlayer.fishCaught = 0;
            incomingRoom.setAiPlayer(aiPlayer);
        }
        if (incomingRoom.getFishGameState() == null) {
            FishGameState state = new FishGameState(lastWaterTilesMessage.waterTiles, lastWaterTilesMessage.tileSize);
            incomingRoom.setFishGameState(state);
            ai.setMapInfo(lastWaterTilesMessage.waterTiles, lastWaterTilesMessage.tileSize);
            ai.setFishGameState(state);
            startFishUpdates();
        }

        GameRoom matchedRoom = getRoomById(incomingRoom.getRoomId());
        if (matchedRoom == null) {
            matchedRoom = incomingRoom;
            rooms.add(matchedRoom);
            roomTimeLeft.put(matchedRoom.getRoomId(), GAME_LENGTH_SECONDS);
        }

        saveRooms(rooms);
        startAIUpdates();

        matchedRoom.addPlayer(connectionId);

        boolean roomFull = matchedRoom.getPlayers().size() >= matchedRoom.getMaxPlayers() + 1
                || matchedRoom.getMaxPlayers() == 1;
        matchedRoom.getAiController().setStartFishingAI(roomFull);

        if (roomFull) {
            startGameTimer(matchedRoom.getRoomId());

            GameTimerMessage startMsg = new GameTimerMessage();
            startMsg.gameStarted = true;
            startMsg.timeLeft = GAME_LENGTH_SECONDS;
            server.sendToMany(new ArrayList<>(matchedRoom.getPlayers()), startMsg);
        }

        // Introduce the new player to the others in the room, and the others to the new player.
        PlayerData newPlayer = players.get(connectionId);
        for (Integer playerId : matchedRoom.getPlayers()) {
            PlayerData other = players.get(playerId);
            if (playerId != connectionId && other != null) {
                server.sendTo(connectionId, other);
                if (newPlayer != null) {
                    server.sendTo(playerId, newPlayer);
                }
            }
        }
        broadcastUpdatedRoomsToAllClients(rooms);
    }

    /** Removes from {@code rooms} every room whose id appears in {@code roomList}; returns {@code rooms}. */
    public static List<GameRoom> removeRooms(List<GameRoom> roomList, List<GameRoom> rooms) {
        Set<String> idsToRemove = roomList.stream()
                .map(GameRoom::getRoomId)
                .collect(Collectors.toSet());
        rooms.removeIf(room -> idsToRemove.contains(room.getRoomId()));
        return rooms;
    }

    public void broadcastUpdatedRoomsToAllClients(List<GameRoom> roomsToSend) {
        roomsToSend.forEach(room -> log.info("Broadcasting room update: {}", room.getRoomId()));
        server.sendToAll(roomsToSend);
    }

    // ---- periodic tasks ---------------------------------------------------------------------------------------

    private void scheduleRepeating(Runnable task, long initialDelayMs, long periodMs) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception e) {
                // An uncaught exception would silently cancel every later run of this task.
                log.error("Error in scheduled task", e);
            }
        }, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    private void startGameTimer(String roomId) {
        roomTimers.computeIfAbsent(roomId, id -> scheduler.scheduleAtFixedRate(
                () -> tickRoomTimer(id), 1000, 1000, TimeUnit.MILLISECONDS));
    }

    private void tickRoomTimer(String roomId) {
        try {
            int time = roomTimeLeft.merge(roomId, -1, Integer::sum);

            GameTimerMessage msg = new GameTimerMessage();
            msg.timeLeft = time;
            msg.gameStarted = true;

            GameRoom room = getRoomById(roomId);
            if (room != null) {
                server.sendToMany(connectedPlayers(room), msg);
                if (time <= 0 && room.getAiPlayer() != null) {
                    room.getAiPlayer().isFishing = false;
                }
            }

            if (time <= 0) {
                ScheduledFuture<?> timer = roomTimers.remove(roomId);
                if (timer != null) {
                    timer.cancel(false);
                }
            }
        } catch (Exception e) {
            log.error("Error in room timer for {}", roomId, e);
        }
    }

    private void broadcastAllPlayers() {
        players.values().forEach(this::broadcastPositions);
    }

    private void startAIUpdates() {
        if (aiUpdatesStarted) return;
        aiUpdatesStarted = true;
        scheduleRepeating(this::updateAiPlayers, 0, 30);
    }

    private void updateAiPlayers() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastAIUpdateTime) / 1000.0f;
        lastAIUpdateTime = now;

        for (GameRoom room : rooms) {
            Integer timeLeft = roomTimeLeft.get(room.getRoomId());
            if (timeLeft != null && timeLeft <= 0) continue;

            AiPlayer aiController = room.getAiController();
            PlayerData aiPlayer = room.getAiPlayer();
            if (aiController == null || aiPlayer == null) continue;

            if (room.getFishGameState() != null) {
                aiController.setFishGameState(room.getFishGameState());
            }

            aiController.update(deltaTime * AI_SPEED_MULTIPLIER);

            aiPlayer.x = aiController.getX();
            aiPlayer.y = aiController.getY();
            aiPlayer.facingRight = aiController.getVelocityX() >= 0;
            aiPlayer.isFishing = aiController.isFishing();
            aiPlayer.hookX = aiController.getHookX();
            aiPlayer.hookY = aiController.getHookY();

            if (aiController.isFishing() && aiController.tryCatchFish()) {
                int gained = aiController.getLastCaughtFishScore();
                aiPlayer.fishCaught++;
                aiPlayer.score += gained;

                log.info("AI fishing attempt at ({},{})", aiPlayer.hookX, aiPlayer.hookY);
                server.sendToAll(new FishCaughtMessage(aiPlayer.id, true, aiPlayer.fishCaught, gained));
            }

            server.sendToMany(connectedPlayers(room), aiPlayer);
        }
    }

    private void startFishUpdates() {
        if (fishUpdatesStarted) return;
        fishUpdatesStarted = true;
        scheduleRepeating(this::updateFish, 0, 50);
    }

    private void updateFish() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastFishUpdateTime) / 1000.0f;
        lastFishUpdateTime = now;

        for (GameRoom room : rooms) {
            FishGameState state = room.getFishGameState();
            if (state != null) {
                state.update(deltaTime);
                FishStateList fishStateList = new FishStateList(state.getFishStates());
                server.sendToMany(new ArrayList<>(room.getPlayers()), fishStateList);
            }
        }
    }

    // ---- broadcasting -----------------------------------------------------------------------------------------

    /** Sends the player's snapshot to everyone in the room that player belongs to. */
    private void broadcastPositions(PlayerData data) {
        GameRoom room = getRoomById(data.roomId);
        if (room == null) return;

        server.sendToMany(connectedPlayers(room), data);
    }

    /** Ids of the room's players that have introduced themselves to the server (copied, so it is safe to iterate). */
    private List<Integer> connectedPlayers(GameRoom room) {
        List<Integer> ids = new ArrayList<>();
        for (Integer playerId : new ArrayList<>(room.getPlayers())) {
            if (players.containsKey(playerId)) {
                ids.add(playerId);
            }
        }
        return ids;
    }

    public static void main(String[] args) throws ServerBindingException {
        new GameServer();
    }

    public void dispose() {
        scheduler.shutdownNow();
        try {
            server.stop(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Server disposed and port released.");
    }
}
