package ee.taltech.game.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import ee.taltech.game.server.exceptions.ServerBindingException;
import ee.taltech.game.server.fish.FishCaughtMessage;
import ee.taltech.game.server.fish.FishGameState;
import ee.taltech.game.server.fish.FishState;
import ee.taltech.game.server.fish.FishStateList;
import ee.taltech.game.server.fish.FishingAttempt;
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

    private static final int TCP_PORT = 8087;
    private static final int UDP_PORT = 8088;
    private static final int AI_PLAYER_ID = 999;
    private static final float AI_SPEED_MULTIPLIER = 1.5f;
    private static final int GAME_LENGTH_SECONDS = 120;

    private final Server server = new Server();
    private final Map<Integer, PlayerData> players = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();
    private final Map<String, Integer> roomTimeLeft = new ConcurrentHashMap<>();

    /** All periodic work runs on this single thread, so the loops never race with each other. */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Must stay an ArrayList: it is sent to clients as-is and ArrayList is the registered list type. */
    private final List<GameRoom> rooms = new ArrayList<>();
    private volatile WaterTilesMessage lastWaterTilesMessage;

    private boolean aiUpdatesStarted = false;
    private boolean fishUpdatesStarted = false;
    private long lastAIUpdateTime = System.currentTimeMillis();
    private long lastFishUpdateTime = System.currentTimeMillis();

    public GameServer() throws ServerBindingException {
        try {
            server.start();
            server.bind(TCP_PORT, UDP_PORT);
        } catch (IOException e) {
            throw new ServerBindingException("Error binding the server to ports " + TCP_PORT + " and " + UDP_PORT, e);
        }

        registerKryoClasses();
        scheduleRepeating(this::broadcastAllPlayers, 0, 100);

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof GameRoomList roomList) {
                    removeRooms(roomList.rooms, rooms);
                    saveRooms(rooms);
                    broadcastUpdatedRoomsToAllClients(rooms);
                } else if (object instanceof GameRoom room) {
                    PlayerData data = players.get(connection.getID());
                    if (data != null) {
                        data.roomId = room.getRoomId();
                    }
                    handleRoomFromClient(room, connection);
                } else if ("getRooms".equals(object)) {
                    server.sendToUDP(connection.getID(), rooms);
                } else if (object instanceof WaterTilesMessage msg) {
                    handleWaterTiles(msg);
                } else if (object instanceof CollisionTilesMessage msg) {
                    handleCollisionTiles(msg);
                } else if (object instanceof FishingAttempt attempt) {
                    handleFishingAttempt(connection, attempt);
                } else if (object instanceof PlayerData data) {
                    handlePlayerData(connection, data);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                int disconnectedId = connection.getID();
                players.remove(disconnectedId);

                PlayerData disconnectMsg = new PlayerData();
                disconnectMsg.id = disconnectedId;
                disconnectMsg.isDisconnected = true;
                server.sendToAllTCP(disconnectMsg);

                log.info("Player {} disconnected", disconnectedId);
                players.values().forEach(GameServer.this::broadcastPositions);
            }
        });

        log.info("Server started!");
    }

    /**
     * The registration order defines the Kryo class ids and must match the client's exactly.
     * Primitives, String and the like are pre-registered by Kryo itself.
     */
    private void registerKryoClasses() {
        Class<?>[] classes = {
                GameRoomList.class,
                GameRoom.class,
                PlayerData.class,
                GameTimerMessage.class,
                int[].class,
                int[][].class,
                WaterTilesMessage.class,
                FishState.class,
                FishStateList.class,
                ArrayList.class,
                CollisionTilesMessage.class,
                FishingAttempt.class,
                FishCaughtMessage.class
        };
        Kryo kryo = server.getKryo();
        for (Class<?> type : classes) {
            kryo.register(type);
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

    private void handleFishingAttempt(Connection connection, FishingAttempt attempt) {
        PlayerData player = players.get(connection.getID());
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
            server.sendToAllTCP(new FishCaughtMessage(player.id, true, player.fishCaught, gainedScore));
            broadcastPositions(player);
            log.info("Player {} caught something! Score: {}", player.id, gainedScore);
        } else {
            connection.sendTCP(new FishCaughtMessage(player.id, false, player.fishCaught, 0));
        }
    }

    private void handlePlayerData(Connection connection, PlayerData data) {
        if (data.id <= 0 || data.x < 0 || data.y < 0) {
            log.warn("Invalid player data received: {}", data);
            return;
        }

        // Score, catch count and room are owned by the server: keep them from the previous snapshot.
        PlayerData existing = players.getOrDefault(connection.getID(), new PlayerData());
        data.fishCaught = existing.fishCaught;
        data.score = existing.score;
        data.roomId = existing.roomId;
        players.put(connection.getID(), data);

        broadcastPositions(data);
    }

    // ---- rooms ------------------------------------------------------------------------------------------------

    private GameRoom getRoomById(String roomId) {
        return rooms.stream()
                .filter(room -> Objects.equals(room.getRoomId(), roomId))
                .findFirst()
                .orElse(null);
    }

    private void handleRoomFromClient(GameRoom incomingRoom, Connection connection) {
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

        matchedRoom.addPlayer(connection.getID());

        boolean roomFull = matchedRoom.getPlayers().size() >= matchedRoom.getMaxPlayers() + 1
                || matchedRoom.getMaxPlayers() == 1;
        matchedRoom.getAiController().setStartFishingAI(roomFull);

        if (roomFull) {
            startGameTimer(matchedRoom.getRoomId());

            GameTimerMessage startMsg = new GameTimerMessage();
            startMsg.gameStarted = true;
            startMsg.timeLeft = GAME_LENGTH_SECONDS;
            for (Integer playerId : matchedRoom.getPlayers()) {
                server.sendToTCP(playerId, startMsg);
            }
        }

        // Introduce the new player to the others in the room, and the others to the new player.
        PlayerData newPlayer = players.get(connection.getID());
        for (Integer playerId : matchedRoom.getPlayers()) {
            PlayerData other = players.get(playerId);
            if (playerId != connection.getID() && other != null) {
                connection.sendUDP(other);
                if (newPlayer != null) {
                    server.sendToUDP(playerId, newPlayer);
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
        server.sendToAllTCP(roomsToSend);
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
                for (Integer playerId : room.getPlayers()) {
                    if (players.containsKey(playerId)) {
                        server.sendToTCP(playerId, msg);
                    }
                }
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
                server.sendToTCP(aiPlayer.id, new FishingAttempt(aiPlayer.hookX, aiPlayer.hookY));
                server.sendToAllTCP(new FishCaughtMessage(aiPlayer.id, true, aiPlayer.fishCaught, gained));
            }

            for (Integer playerId : room.getPlayers()) {
                if (players.containsKey(playerId)) {
                    server.sendToUDP(playerId, aiPlayer);
                }
            }
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
                for (Integer playerId : room.getPlayers()) {
                    server.sendToTCP(playerId, fishStateList);
                }
            }
        }
    }

    // ---- broadcasting -----------------------------------------------------------------------------------------

    /** Sends the player's snapshot to everyone in the room that player belongs to. */
    private void broadcastPositions(PlayerData data) {
        GameRoom room = getRoomById(data.roomId);
        if (room == null) return;

        for (Integer playerId : room.getPlayers()) {
            if (players.containsKey(playerId)) {
                server.sendToUDP(playerId, data);
            }
        }
    }

    public static void main(String[] args) throws ServerBindingException {
        new GameServer();
    }

    public void dispose() {
        scheduler.shutdownNow();
        server.stop();
        server.close();
        log.info("Server disposed and ports released.");
    }
}
