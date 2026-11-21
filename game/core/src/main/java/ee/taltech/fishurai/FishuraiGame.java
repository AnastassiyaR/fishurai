package main.java.ee.taltech.fishurai;

import lombok.extern.slf4j.Slf4j;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.Input;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import main.java.ee.taltech.fishurai.bonus.BootBonus;
import main.java.ee.taltech.fishurai.exceptions.ConnectionException;
import main.java.ee.taltech.fishurai.fish.*;
import main.java.ee.taltech.fishurai.rooms.GameRoomList;
import main.java.ee.taltech.fishurai.screens.*;
import main.java.ee.taltech.fishurai.rooms.ClientRoomManager;
import main.java.ee.taltech.fishurai.rooms.GameRoom;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static main.java.ee.taltech.fishurai.rooms.ClientRoomManager.rooms;


@Slf4j
public class FishuraiGame extends ApplicationAdapter {

    private static final int AI_PLAYER_ID = 999;
    public static int level = -1;

    private ShapeRenderer brightnessRenderer;
    public static float brightness = 0.80f;
    public static Music temmieMusic;

    public static boolean gameStarted = false;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Texture playerLeftTexture;
    private Texture playerRightTexture;
    private Texture currentPlayerTexture;
    private FishManagerClient fishManagerClient;

    private CollisionChecker collisionChecker;
    private Map map;

    private float playerX, playerY;
    private float speed = 400f;

    public static Client client;

    public static int gameStateFlag = 0;
    private static final int LobbyScreen = 0;
    public static final int GameScreen = 1;
    private static final int ExitScreen = 2;
    private static final int SettingsScreen = 3;
    public static final int RoomScreen = 4;
    public static final int RoomOptionsScreen = 5;
    private static final int ResultsScreen = 6;
    public static final int LevelChoosing = 7;

    private LobbyScreen lobbyScreen;
    private SettingsScreen settingsScreen;
    private ExitPrompt exitPrompt;
    private OrthographicCamera uiCamera;
    private RoomScreen roomScreen;
    private RoomOptionsScreen roomOptionsScreen;
    private LevelChoosing levelChoosing;
    private InstructionScreen instructionScreen;
    private boolean showInstructionScreen = true;

    private boolean isFishing = false;
    private float hookX, hookY;
    private Texture hookTexture;
    private ShapeRenderer shapeRenderer;

    public static float volume = 0.5f;
    private BitmapFont playerOwnScoreFont;
    private BitmapFont floatingScoreFont;

    private int playerFishCaught;
    private Texture rankingTexture;
    private BitmapFont rankingFont;
    /** Everyone we know about, including this player and the AI bot. Written by the network thread. */
    private final ConcurrentMap<Integer, PlayerData> playersData = new ConcurrentHashMap<>();

    private Texture bootsTexture;
    private List<BootBonus> activeBoots;
    private float speedBoostTimer = 0f;
    private static final float SPEED_BOOST_DURATION = 5f;

    private int counter = 0;
    private int counter2 = 0;
    private boolean bootsSpawned = false;

    private List<FloatingText> floatingTexts = new ArrayList<>();
    private int aiScore = 0;

    public static PlayerData myPlayerData;

    private int gameTimeLeft;
    private boolean gameTimerStarted = false;

    public static boolean AiChosen = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();

        map = new Map(camera);
        collisionChecker = new CollisionChecker(map);

        brightnessRenderer = new ShapeRenderer();
        temmieMusic = Gdx.audio.newMusic(Gdx.files.internal("music/temmie.ogg"));
        temmieMusic.setVolume(volume);

        float zoom = 0.5f;
        camera.setToOrtho(false, Gdx.graphics.getWidth() / zoom,
            Gdx.graphics.getHeight() / zoom);

        playerLeftTexture = new Texture("playerLeft.png");
        playerRightTexture = new Texture("playerRight.png");

        instructionScreen = new InstructionScreen();

        Texture commonFishTexture = new Texture("fish.png");
        Texture uncommonFishTexture = new Texture("fish_fast.png");
        Texture rareFishTexture = new Texture("fish_rare.png");
        Texture trashTexture = new Texture("trash.png");
        fishManagerClient = new FishManagerClient(
            commonFishTexture,
            uncommonFishTexture,
            rareFishTexture,
            trashTexture,
            40
        );

        hookTexture = new Texture("hook.png");
        shapeRenderer = new ShapeRenderer();
        currentPlayerTexture = playerRightTexture;

        playerX = map.getWidth() / 2f;
        playerY = map.getHeight() / 2f + 400;

        client = new Client();
        client.start();

        client.getKryo().register(GameRoomList.class);
        client.getKryo().register(GameRoom.class);
        client.getKryo().register(PlayerData.class);
        client.getKryo().register(GameTimerMessage.class);

        client.getKryo().register(int[].class);
        client.getKryo().register(int[][].class);
        client.getKryo().register(WaterTilesMessage.class);
        client.getKryo().register(FishState.class);
        client.getKryo().register(FishStateList.class);
        client.getKryo().register(ArrayList.class);
        client.getKryo().register(CollisionTilesMessage.class);

        client.getKryo().register(FishingAttempt.class);
        client.getKryo().register(FishCaughtMessage.class);

        playerOwnScoreFont = new BitmapFont();
        playerOwnScoreFont.getData().setScale(2.2f);

        createRankingBackground();
        rankingFont = new BitmapFont();

        floatingScoreFont = new BitmapFont();
        floatingScoreFont.getData().setScale(2.0f);

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        try {
            client.connect(5000, "localhost", 8087, 8088);
            log.info("Connected to server!");

            int[][] waterTiles = map.getWaterTiles();
            client.sendTCP(new WaterTilesMessage(waterTiles, map.getTileSize()));

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> client.sendUDP("getRooms"), 1, TimeUnit.SECONDS);
            scheduler.shutdown(); // the delayed task still runs; the thread then exits

            int[][] collisionTiles = map.getCollisionTiles();
            client.sendTCP(new CollisionTilesMessage(collisionTiles, map.getTileSize()));

        } catch (IOException e) {
            log.error("Connection error: " + e.getMessage(), e);
            throw new ConnectionException("Unable to connect to server: " + e.getMessage());
        }

        PlayerData aiPlayer = new PlayerData();
        aiPlayer.id = AI_PLAYER_ID;
        aiPlayer.fishCaught = 0;
        playersData.put(aiPlayer.id, aiPlayer);

        myPlayerData = new PlayerData();
        myPlayerData.id = client.getID();
        myPlayerData.score = 0;
        playersData.put(myPlayerData.id, myPlayerData);
        client.sendUDP(myPlayerData);

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                try {
                    if (object instanceof GameTimerMessage) {
                        GameTimerMessage timerMsg = (GameTimerMessage) object;
                        gameTimeLeft = timerMsg.timeLeft;
                        gameTimerStarted = timerMsg.gameStarted;

                        if (gameTimeLeft <= 0) {
                            Gdx.app.postRunnable(() -> gameStateFlag = ResultsScreen);
                        }
                    } else if (object instanceof List) {
                        List<?> list = (List<?>) object;
                        if (!list.isEmpty() && list.get(0) instanceof GameRoom) {
                            @SuppressWarnings("unchecked")
                            List<GameRoom> receivedRooms = (List<GameRoom>) list;
                            ClientRoomManager.saveRooms(receivedRooms);
                        }
                    } else if (object instanceof PlayerData) {
                        handlePlayerData((PlayerData) object);
                    } else if (object instanceof FishStateList) {
                        List<FishState> states = ((FishStateList) object).states;
                        if (states != null && !states.isEmpty()) {
                            fishManagerClient.updateFromServer(states);
                        }
                    } else if (object instanceof FishState) {
                        FishState state = (FishState) object;
                        if (!state.isCaught) {
                            fishManagerClient.updateFromServer(Collections.singletonList(state));
                        }
                    } else if (object instanceof FishCaughtMessage) {
                        handleFishCaught((FishCaughtMessage) object);
                    }
                } catch (Exception e) {
                    log.error("Error processing network message", e);
                }
            }
        });

        lobbyScreen = new LobbyScreen();
        settingsScreen = new SettingsScreen();
        exitPrompt = new ExitPrompt();
        roomScreen = new RoomScreen();
        roomOptionsScreen = new RoomOptionsScreen();
        levelChoosing = new LevelChoosing();

        bootsTexture = new Texture("bottle.png");
        activeBoots = new ArrayList<>();

        int bootCount = 1;
        int tileSize = 48;

        for (int i = 0; i < bootCount; i++) {
            float randomX = (float) (Math.random() * (map.getWidth() - tileSize));
            float randomY;
            if (Math.random() < 0.5) {
                randomY = 4 * tileSize + (float) (Math.random() * (5 * tileSize));
            } else {
                randomY = 22 * tileSize + (float) (Math.random() * (5 * tileSize));
            }
            int col = (int) (randomX / tileSize);
            int row = (int) (randomY / tileSize);

            if (!collisionChecker.isTileCollision(col, row)) {
                activeBoots.add(new BootBonus(randomX, randomY));
            } else {
                i--;
            }
        }

    }

    private void handlePlayerData(PlayerData data) {
        if (data.id <= 0 || data.x < 0 || data.y < 0) {
            log.warn("Invalid player data received: {}", data);
            return;
        }

        if (data.isDisconnected) {
            playersData.remove(data.id);
        } else {
            playersData.put(data.id, data);
        }
    }

    private void handleFishCaught(FishCaughtMessage msg) {
        if (msg.playerId == AI_PLAYER_ID) {
            aiScore += msg.scoreGained;
            PlayerData ai = playersData.get(AI_PLAYER_ID);
            if (ai != null) {
                ai.fishCaught = msg.totalFishCaught;
                ai.score = aiScore;
            }
        } else if (msg.playerId == client.getID()) {
            playerFishCaught = msg.totalFishCaught;
            PlayerData player = playersData.get(client.getID());
            if (player != null) {
                player.fishCaught = playerFishCaught;
                player.score += msg.scoreGained;

                if (msg.success) {
                    isFishing = false;
                    Color textColor = msg.scoreGained >= 0 ? Color.GREEN : Color.RED;
                    floatingTexts.add(new FloatingText(
                        (msg.scoreGained > 0 ? "+" : "") + msg.scoreGained,
                        playerX,
                        playerY + 40,
                        textColor
                    ));
                }
            }
        }
    }

    public static void sendRemovedRoomsToServer(List<GameRoom> rooms) {
        GameRoomList roomList = new GameRoomList();
        roomList.rooms = rooms;
        client.sendTCP(roomList);

    }

    public static void sendRoomToServer(GameRoom room) {
        client.sendTCP(room);
    }

    private void createRankingBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        rankingTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void renderResultsScreen() {
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        List<PlayerData> sortedPlayers = new ArrayList<>(playersData.values());
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.score, p1.score));

        final float BASE_WIDTH = 1920f;
        final float BASE_HEIGHT = 1080f;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float scaleX = screenWidth / BASE_WIDTH;
        float scaleY = screenHeight / BASE_HEIGHT;
        float scale = Math.min(scaleX, scaleY);

        float resultWidth = BASE_WIDTH * scale;
        float resultHeight = BASE_HEIGHT * scale;
        float resultX = (screenWidth - resultWidth) / 2f;
        float resultY = (screenHeight - resultHeight) / 2f;

        uiCamera.setToOrtho(false, BASE_WIDTH, BASE_HEIGHT);
        batch.setProjectionMatrix(uiCamera.combined);

        batch.begin();

        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0, 0, 0, 0.7f));
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        batch.draw(bgTexture, 0, 0, BASE_WIDTH, BASE_HEIGHT);
        bgTexture.dispose();
        bgPixmap.dispose();

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(3.0f);
        titleFont.setColor(Color.GOLD);
        String title = "Game Results";
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, title,
            (BASE_WIDTH - titleLayout.width) / 2,
            BASE_HEIGHT - 100);

        BitmapFont headerFont = new BitmapFont();
        headerFont.getData().setScale(2.0f);
        BitmapFont dataFont = new BitmapFont();
        dataFont.getData().setScale(1.8f);

        float tableY = BASE_HEIGHT - 200;
        float rowHeight = 50;
        float headerHeight = rowHeight * 1.2f;
        float padding = 100;

        headerFont.setColor(Color.CYAN);
        batch.setColor(0.2f, 0.3f, 0.5f, 0.8f);
        batch.draw(bgTexture, padding, tableY - headerHeight, BASE_WIDTH - 2*padding, headerHeight);
        batch.setColor(Color.WHITE);

        float col1 = padding + 20;
        float col2 = col1 + (BASE_WIDTH - 2*padding) * 0.3f;
        float col3 = col2 + (BASE_WIDTH - 2*padding) * 0.3f;
        float col4 = col3 + (BASE_WIDTH - 2*padding) * 0.2f;

        headerFont.draw(batch, "Rank", col1, tableY - 20);
        headerFont.draw(batch, "Player", col2, tableY - 20);
        headerFont.draw(batch, "Score", col3, tableY - 20);
        headerFont.draw(batch, "Fish Caught", col4, tableY - 20);

        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerData player = sortedPlayers.get(i);
            float rowY = tableY - headerHeight - (i + 1) * rowHeight;

            batch.setColor(i % 2 == 0 ?
                new Color(0.3f, 0.3f, 0.5f, 0.7f) :
                new Color(0.2f, 0.2f, 0.4f, 0.7f));
            batch.draw(bgTexture, padding, rowY, BASE_WIDTH - 2*padding, rowHeight);
            batch.setColor(Color.WHITE);

            if (player.id == client.getID()) {
                dataFont.setColor(Color.GREEN);
            } else if (player.id == AI_PLAYER_ID) {
                dataFont.setColor(Color.RED);
            } else {
                dataFont.setColor(Color.WHITE);
            }

            String playerName = player.id == client.getID() ? "YOU" :
                player.id == AI_PLAYER_ID ? "AI" :
                    "Player " + player.id;

            dataFont.draw(batch, String.valueOf(i + 1), col1, rowY + rowHeight * 0.7f);
            dataFont.draw(batch, playerName, col2, rowY + rowHeight * 0.7f);
            dataFont.draw(batch, String.valueOf(player.score), col3, rowY + rowHeight * 0.7f);
            dataFont.draw(batch, String.valueOf(player.fishCaught), col4, rowY + rowHeight * 0.7f);
        }

        BitmapFont hintFont = new BitmapFont();
        hintFont.getData().setScale(1.5f);
        hintFont.setColor(Color.LIGHT_GRAY);
        String hintText = "Press ENTER to close the game";
        GlyphLayout hintLayout = new GlyphLayout(hintFont, hintText);
        hintFont.draw(batch, hintText,
            (BASE_WIDTH - hintLayout.width) / 2,
            100);

        batch.end();

        titleFont.dispose();
        headerFont.dispose();
        dataFont.dispose();
        hintFont.dispose();

        uiCamera.setToOrtho(false, screenWidth, screenHeight);
        batch.setProjectionMatrix(uiCamera.combined);
    }

    private void renderTimer() {
        if (!gameTimerStarted) return;

        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(uiCamera.combined);

        int minutes = gameTimeLeft / 60;
        int seconds = gameTimeLeft % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);

        playerOwnScoreFont.getData().setScale(1.8f);
        playerOwnScoreFont.setColor(Color.WHITE);
        if (gameTimeLeft <= 10) {
            playerOwnScoreFont.setColor(Color.RED);
        } else if (gameTimeLeft <= 30) {
            playerOwnScoreFont.setColor(Color.ORANGE);
        }

        float textWidth = 100;
        float textHeight = playerOwnScoreFont.getCapHeight();
        float x = Gdx.graphics.getWidth() - textWidth - 20;
        float y = Gdx.graphics.getHeight() - 20;

        batch.begin();
        playerOwnScoreFont.draw(batch, timeText, x, y);
        batch.end();
    }


    private void renderRankingTable() {
        if (playersData.isEmpty()) return;

        float baseWidth = 300;
        float baseHeight = 220;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float scaleFactor = Math.max(0.6f, Math.min(screenWidth / 1600f, 1.5f));

        float tableWidth = baseWidth * scaleFactor;
        float tableHeight = baseHeight * scaleFactor;

        float tableX = screenWidth - tableWidth - 20;
        float tableY = 20;

        if (rankingTexture.getWidth() != (int) tableWidth || rankingTexture.getHeight() != (int) tableHeight) {
            if (rankingTexture != null) rankingTexture.dispose();

            Pixmap pixmap = new Pixmap((int) tableWidth, (int) tableHeight, Pixmap.Format.RGBA8888);
            for (int y = 0; y < pixmap.getHeight(); y++) {
                float alpha = 0.7f + (0.2f * y / pixmap.getHeight());
                pixmap.setColor(new Color(0.1f, 0.2f, 0.5f, alpha));
                pixmap.drawLine(0, y, pixmap.getWidth(), y);
            }
            pixmap.setColor(Color.WHITE);
            pixmap.drawRectangle(0, 0, pixmap.getWidth() - 1, pixmap.getHeight() - 1);

            rankingTexture = new Texture(pixmap);
            pixmap.dispose();
        }

        uiCamera.setToOrtho(false, screenWidth, screenHeight);
        batch.setProjectionMatrix(uiCamera.combined);

        rankingFont.getData().setScale(1.0f);

        float rowHeight = tableHeight / 7f;
        float textX = tableX + 20;

        batch.begin();
        batch.draw(rankingTexture, tableX, tableY, tableWidth, tableHeight);

        rankingFont.setColor(Color.ORANGE);
        rankingFont.draw(batch, "TOP PLAYERS", textX, tableY + tableHeight - rowHeight * 0.6f);

        List<PlayerData> allPlayers = new ArrayList<>(playersData.values());

        if (!playersData.containsKey(AI_PLAYER_ID) && aiScore > 0) {
            PlayerData aiData = new PlayerData();
            aiData.id = AI_PLAYER_ID;
            aiData.score = aiScore;
            allPlayers.add(aiData);
        }

        allPlayers.sort((p1, p2) -> Integer.compare(p2.score, p1.score));

        for (int i = 0; i < Math.min(5, allPlayers.size()); i++) {
            PlayerData player = allPlayers.get(i);
            String name = player.id == client.getID() ? "YOU" :
                player.id == AI_PLAYER_ID ? "AI BOT" :
                    "Player " + player.id;
            Color color = player.id == client.getID() ? Color.GREEN :
                player.id == AI_PLAYER_ID ? Color.RED :
                    Color.WHITE;

            rankingFont.setColor(color);
            float yPos = tableY + tableHeight - rowHeight * (i + 1.6f);
            rankingFont.draw(batch, String.format("%s: %d", name, player.score), textX, yPos);
        }
        batch.end();
    }

    private void applyBrightness() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        brightnessRenderer.setProjectionMatrix(camera.combined);
        brightnessRenderer.begin(ShapeRenderer.ShapeType.Filled);
        brightnessRenderer.setColor(0, 0, 0, 1f - brightness);
        brightnessRenderer.rect(
            camera.position.x - camera.viewportWidth/2,
            camera.position.y - camera.viewportHeight/2,
            camera.viewportWidth,
            camera.viewportHeight
        );
        brightnessRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void render() {
        if (showInstructionScreen && gameStateFlag == RoomOptionsScreen) {
            instructionScreen.render();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                showInstructionScreen = false;
                instructionScreen.dispose();
            }
            return;
        }

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        if (gameStateFlag == LobbyScreen) {
            lobbyScreen.render();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                gameStateFlag = RoomOptionsScreen;
            }
        }
        else if (gameStateFlag == RoomOptionsScreen) {
            roomOptionsScreen.render();
        }
        else if (gameStateFlag == RoomScreen) {
            roomScreen.render();
        }
        else if (gameStateFlag == LevelChoosing) {
            levelChoosing.render();
        }
        else if (gameStateFlag == GameScreen && gameStarted) {
            if (!temmieMusic.isPlaying()) {
                temmieMusic.setLooping(true);
                temmieMusic.play();
            }
            float deltaTime = Gdx.graphics.getDeltaTime();
            handleInput();
            sendPlayerData();

            floatingTexts.removeIf(FloatingText::isFinished);
            floatingTexts.forEach(text -> text.update(deltaTime));

            PlayerData aiData = playersData.get(AI_PLAYER_ID);
            if (aiData != null && aiData.isFishing && aiData.hookY < 0) {
                aiData.isFishing = false;
            }

            float cameraX = playerX;
            float cameraY = playerY;

            float halfViewportWidth = camera.viewportWidth / 2f;
            float halfViewportHeight = camera.viewportHeight / 2f;

            cameraX = Math.max(halfViewportWidth, Math.min(cameraX, map.getWidth() - halfViewportWidth));
            cameraY = Math.max(halfViewportHeight, Math.min(cameraY, map.getHeight() - halfViewportHeight));

            camera.position.set(cameraX, cameraY, 0);
            camera.update();
            batch.setProjectionMatrix(camera.combined);

            batch.begin();
            map.draw(batch, cameraX - halfViewportWidth, cameraY - halfViewportHeight);

            for (BootBonus boot : activeBoots) {
                if (!boot.isCollected()) {
                    batch.draw(bootsTexture, boot.getX() - bootsTexture.getWidth()/2f,
                        boot.getY() - bootsTexture.getHeight()/2f, 48, 48);
                }
            }
            batch.end();

            batch.begin();
            for (PlayerData data : playersData.values()) {
                if (data.id != client.getID()) {
                    Texture otherPlayerTexture = data.facingRight ? playerRightTexture : playerLeftTexture;
                    batch.draw(otherPlayerTexture,
                        data.x - otherPlayerTexture.getWidth() / 2f,
                        data.y - otherPlayerTexture.getHeight() / 2f);

                    if (data.isFishing) {
                        batch.draw(hookTexture,
                            data.hookX - hookTexture.getWidth()/2f,
                            data.hookY - hookTexture.getHeight()/2f);
                    }

                }
            }

            batch.draw(currentPlayerTexture,
                playerX - currentPlayerTexture.getWidth() / 2f,
                playerY - currentPlayerTexture.getHeight() / 2f);

            if (isFishing) {
                batch.draw(hookTexture,
                    hookX - hookTexture.getWidth()/2f,
                    hookY - hookTexture.getHeight()/2f);
            }

            fishManagerClient.render(batch);

            for (FloatingText text : floatingTexts) {
                text.render(batch, floatingScoreFont);
            }

            batch.end();

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.LIGHT_GRAY);

            for (PlayerData data : playersData.values()) {
                if (data.id != client.getID() && data.isFishing) {
                    shapeRenderer.rectLine(
                        data.x, data.y,
                        data.hookX, data.hookY,
                        2f
                    );
                }
            }

            if (isFishing) {
                shapeRenderer.rectLine(
                    playerX, playerY,
                    hookX, hookY,
                    2f
                );
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            playerOwnScoreFont.getData().setScale(1.3f);
            playerOwnScoreFont.setColor(Color.WHITE);
            PlayerData playerData = playersData.get(client.getID());
            if (playerData != null) {
                playerOwnScoreFont.draw(batch, "Score: " + playerData.score, 18, Gdx.graphics.getHeight() - 18);
            }
            batch.end();

            renderTimer();
            renderRankingTable();
            counter++;

            if (counter > 600 && !bootsSpawned) {
                spawnBootBonus();
                counter = 0;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                gameStateFlag = ExitScreen;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
                gameStateFlag = SettingsScreen;
            }

        }
        else if (gameStateFlag == SettingsScreen) {
            settingsScreen.render();
            if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
                gameStateFlag = GameScreen;
            }
        }
        else if (gameStateFlag == ResultsScreen) {
            temmieMusic.stop();
            renderResultsScreen();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                resetGameState();
            }

            if (counter2 >= 1000) {
                resetGameState();

            } else {
                counter2++;
            }
        }
        else if (gameStateFlag == ExitScreen) {
            exitPrompt.render();
        }
        applyBrightness();
    }

    private void spawnBootBonus() {
        int tileSize = 48;
        float randomX = (float) (Math.random() * (map.getWidth() - tileSize));

        float randomY;
        if (Math.random() < 0.5) {
            randomY = 4 * tileSize + (float) (Math.random() * 20);
        } else {
            randomY = 22 * tileSize + (float) (Math.random() * (5 * tileSize));
        }

        int col = (int) (randomX / tileSize);
        int row = (int) (randomY / tileSize);

        if (!collisionChecker.isTileCollision(col, row)) {
            activeBoots.add(new BootBonus(randomX, randomY));
            bootsSpawned = true;
        }
    }

    private void resetGameState() {
        List<GameRoom> removedRooms = new ArrayList<>();
        for (GameRoom i : rooms) {
            if (i.getPlayers().contains(myPlayerData.id)) {
                removedRooms.add(i);
            }
        }

        FishuraiGame.sendRemovedRoomsToServer(removedRooms);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        myPlayerData = null;

        if (client != null) {
            client.stop();
        }

        Gdx.app.exit();
    }

    private void handleInput() {
        if (gameTimeLeft <= 0) return;
        float delta = Gdx.graphics.getDeltaTime();
        float newX = playerX;
        float newY = playerY;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            newX -= speed * delta;
            currentPlayerTexture = playerLeftTexture;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            newX += speed * delta;
            currentPlayerTexture = playerRightTexture;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            newY += speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            newY -= speed * delta;
        }

        int col = (int) (newX / map.getTileSize());
        int row = (int) (newY / map.getTileSize());

        if (!collisionChecker.isTileCollision(col, row)) {
            playerX = newX;
            playerY = newY;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.PLUS)) {
            camera.zoom -= 0.1f;
            camera.update();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            camera.zoom += 0.1f;
            camera.update();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (!isFishing && gameTimerStarted) {
                isFishing = true;
                hookX = playerX;
                hookY = playerY;
            }
        }

        if (isFishing) {
            hookY -= 300 * Gdx.graphics.getDeltaTime();
            client.sendTCP(new FishingAttempt(hookX, hookY));

            if (hookY < 0) {
                isFishing = false;
            }
        }

        playerX = Math.max(0, Math.min(playerX, map.getWidth()));
        playerY = Math.max(0, Math.min(playerY, map.getHeight()));

        for (BootBonus boot : activeBoots) {
            if (!boot.isCollected() && Math.hypot(playerX - boot.getX(), playerY - boot.getY()) < 40) {
                boot.setCollected(true);
                speedBoostTimer = SPEED_BOOST_DURATION;
            }
        }

        if (speedBoostTimer > 0) {
            speedBoostTimer -= Gdx.graphics.getDeltaTime();
            speed = 600f;
        } else {
            speed = 400f;
        }

    }

    private void sendPlayerData() {
        PlayerData data = new PlayerData();
        data.id = client.getID();
        data.x = playerX;
        data.y = playerY;
        data.hookX = hookX;
        data.hookY = hookY;
        data.isFishing = isFishing;
        data.texture = currentPlayerTexture == playerLeftTexture ? "player-left.png" : "player-right.png";
        data.facingRight = (currentPlayerTexture == playerRightTexture);
        data.fishCaught = playerFishCaught;

        PlayerData playerData = playersData.get(client.getID());
        if (playerData != null) {
            data.score = playerData.score;
        }

        try {
            client.sendTCP(data);
        } catch (Exception e) {
            log.error("Error sending player data", e);
        }
    }

    @Override
    public void dispose() {
        if (temmieMusic != null) temmieMusic.dispose();
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (playerLeftTexture != null) playerLeftTexture.dispose();
        if (playerRightTexture != null) playerRightTexture.dispose();
        if (hookTexture != null) hookTexture.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (brightnessRenderer != null) brightnessRenderer.dispose();
        if (rankingTexture != null) rankingTexture.dispose();
        if (playerOwnScoreFont != null) playerOwnScoreFont.dispose();
        if (rankingFont != null) rankingFont.dispose();
        if (floatingScoreFont != null) floatingScoreFont.dispose();
        if (bootsTexture != null) bootsTexture.dispose();
        if (client != null && client.isConnected()) {
            client.stop();
        }
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    System.exit(0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
