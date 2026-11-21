package main.java.ee.taltech.fishurai.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import main.java.ee.taltech.fishurai.FishuraiGame;
import main.java.ee.taltech.fishurai.rooms.GameRoom;

import java.util.ArrayList;
import java.util.List;

import static main.java.ee.taltech.fishurai.FishuraiGame.level;
import static main.java.ee.taltech.fishurai.FishuraiGame.myPlayerData;
import static main.java.ee.taltech.fishurai.rooms.ClientRoomManager.loadRooms;
import static main.java.ee.taltech.fishurai.rooms.ClientRoomManager.saveRooms;

public class RoomScreen {
    private String statusMessage = "";

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Texture background;
    private static final float BASE_SCREEN_HEIGHT = 1080f;
    private int maxPlayers;
    private String roomNameText;

    public List<GameRoom> rooms = updateRooms();
    public GameRoom playerRoom = null;
    private boolean isClicked = false;

    private boolean Joined = false;

    private Stage stage;
    private Skin skin;
    public static TextField maxPlayersField;
    public static TextField roomName;
    private boolean showInput = false;

    private boolean showCreateButton = true;
    private String createButtonText = "Create Room";
    private float createButtonX, createButtonY, createButtonWidth, createButtonHeight;

    private float roomUpdateTimer = 0;
    private static final float ROOM_UPDATE_INTERVAL = 1.0f;

    public RoomScreen() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        layout = new GlyphLayout();
        background = new Texture(Gdx.files.internal("background/back.jpg"));

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/MaruMonica.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 80;
        param.color = Color.WHITE;
        font = generator.generateFont(param);
        generator.dispose();

        createButtonWidth = 160;
        createButtonHeight = 60;
        createButtonX = 20;
        createButtonY = Gdx.graphics.getHeight() - createButtonHeight - 20;


        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        skin = new Skin();
        BitmapFont textFieldFont = new BitmapFont();
        skin.add("default-font", textFieldFont);

        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0.5f, 0.8f, 1f, 1f));
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        skin.add("background-texture", bgTexture);
        Drawable backgroundDrawable = new TextureRegionDrawable(new TextureRegion(bgTexture));

        Pixmap cursorPixmap = new Pixmap(1, 20, Pixmap.Format.RGBA8888);
        cursorPixmap.setColor(Color.WHITE);
        cursorPixmap.fill();
        Texture cursorTexture = new Texture(cursorPixmap);
        Drawable cursorDrawable = new TextureRegionDrawable(new TextureRegion(cursorTexture));

        Pixmap selectionPixmap = new Pixmap(1, 20, Pixmap.Format.RGBA8888);
        selectionPixmap.setColor(new Color(0.2f, 0.5f, 1f, 0.5f));
        selectionPixmap.fill();
        Texture selectionTexture = new Texture(selectionPixmap);
        Drawable selectionDrawable = new TextureRegionDrawable(new TextureRegion(selectionTexture));

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = textFieldFont;
        textFieldStyle.fontColor = Color.BLACK;
        textFieldStyle.background = backgroundDrawable;
        textFieldStyle.cursor = cursorDrawable;
        textFieldStyle.selection = selectionDrawable;

        skin.add("default", textFieldStyle);

        maxPlayersField = new TextField("2", skin);
        maxPlayersField.setPosition(200, 200);
        maxPlayersField.setSize(200, 50);
        maxPlayersField.setVisible(false);
        stage.addActor(maxPlayersField);

        roomName = new TextField("My Room", skin);
        roomName.setPosition(200, 270);
        roomName.setSize(200, 50);
        roomName.setVisible(false);
        stage.addActor(roomName);

    }

    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        roomUpdateTimer += delta;

        if (roomUpdateTimer >= ROOM_UPDATE_INTERVAL) {
            rooms = updateRooms();
            roomUpdateTimer = 0;
        }

        if (FishuraiGame.AiChosen) {
            FishuraiGame.AiChosen = false;
            List<GameRoom> newRooms = new ArrayList<>();
            GameRoom newRoom = new GameRoom();
            newRoom.setRoomId(roomNameText);
            newRoom.setMaxPlayers(maxPlayers);
            List<Integer> players = new ArrayList<>();
            players.add(myPlayerData.id);
            myPlayerData.roomId = newRoom.getRoomId();
            newRoom.setPlayers(players);
            newRoom.setAiDifficulty(level);
            newRooms.add(newRoom);
            saveRooms(newRooms);
            FishuraiGame.sendRoomToServer(newRoom);
            FishuraiGame.gameStateFlag = FishuraiGame.GameScreen;
            FishuraiGame.gameStarted = true;
            statusMessage = "Created and joined room " + newRoom.getRoomId();
            updateRooms();
            showInput = false;
            maxPlayersField.setVisible(false);
            roomName.setVisible(false);
            playerRoom = newRoom;
        }

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenWidth = camera.viewportWidth;
        float screenHeight = camera.viewportHeight;
        float fontScale = screenHeight / BASE_SCREEN_HEIGHT;

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        font.getData().setScale(fontScale);

        layout.setText(font, "Available Rooms");
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight * 0.9f);
        batch.end();

        List<GameRoom> availableRooms = new ArrayList<>();
        for (GameRoom room : rooms) {
            if (room.getPlayers().size() < room.getMaxPlayers() + 1) {
                availableRooms.add(room);
            }
        }

        if (!showInput && !availableRooms.isEmpty()) {
            float buttonHeight = 80;
            float buttonSpacing = screenHeight * 0.05f;
            float yOffset = screenHeight * 0.75f;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (GameRoom room : availableRooms) {
                String roomInfo = roomLabel(room);

                layout.setText(font, roomInfo);

                shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.2f, 0.7f));
                shapeRenderer.rect(
                    (screenWidth - layout.width) / 2f - 20,
                    yOffset - layout.height - 10,
                    layout.width + 40,
                    layout.height + 20
                );

                yOffset -= buttonHeight + buttonSpacing;
            }
            shapeRenderer.end();

            batch.begin();
            yOffset = screenHeight * 0.75f;
            for (GameRoom room : availableRooms) {
                String roomInfo = roomLabel(room);

                layout.setText(font, roomInfo);
                font.draw(batch, roomInfo, (screenWidth - layout.width) / 2f, yOffset);

                yOffset -= buttonHeight + buttonSpacing;
            }
            batch.end();
        }
        if (showInput) {
            stage.act(Gdx.graphics.getDeltaTime());
            stage.draw();
        }

        if (showCreateButton && !showInput) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(createButtonX, createButtonY, createButtonWidth, createButtonHeight);
            shapeRenderer.end();

            Gdx.gl.glLineWidth(3);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rect(createButtonX, createButtonY, createButtonWidth, createButtonHeight);
            shapeRenderer.end();
            Gdx.gl.glLineWidth(1);

            batch.begin();

            font.getData().setScale(fontScale * 0.6f);
            font.setColor(Color.BLACK);
            layout.setText(font, createButtonText);
            font.draw(batch, createButtonText,
                createButtonX + (createButtonWidth - layout.width) / 2f,
                createButtonY + createButtonHeight / 1.5f);

            font.setColor(Color.WHITE);
            font.getData().setScale(fontScale);
            batch.end();

            if (Gdx.input.isTouched() && !isClicked) {
                float touchX = Gdx.input.getX();
                float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();

                if (touchX >= createButtonX && touchX <= createButtonX + createButtonWidth &&
                    touchY >= createButtonY && touchY <= createButtonY + createButtonHeight) {

                    showInput = true;
                    showCreateButton = false;
                    maxPlayersField.setVisible(true);
                    roomName.setVisible(true);
                    statusMessage = "Enter room name and max players, then press ENTER";
                    isClicked = true;
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showInput = false;
            showCreateButton = true;
            maxPlayersField.setVisible(false);
            roomName.setVisible(false);
        }

        if (showInput && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            try {
                maxPlayers = Integer.parseInt(maxPlayersField.getText());
                roomNameText = roomName.getText().trim();

                boolean roomUniqueName = rooms.stream().anyMatch(room -> room.getRoomId().equals(roomNameText));
                if (maxPlayers > 0 && !roomNameText.isEmpty() && !roomUniqueName) {
                    FishuraiGame.gameStateFlag = FishuraiGame.LevelChoosing;
                } else {
                    if (roomNameText.isEmpty()) {
                        statusMessage = "Room name is empty!";
                    }
                    else if (roomUniqueName) {
                        statusMessage = "Already exists!";
                    }
                    else if (maxPlayers <= 0) {
                        statusMessage = "Max players must be greater than 0!";
                    }
                    else {
                        statusMessage = "INVALID";
                    }
                }
            } catch (NumberFormatException e) {
                statusMessage = "Invalid number!";
            }
        }

        if (Gdx.input.isTouched() && !isClicked) {
            float touchX = Gdx.input.getX();
            float touchY = screenHeight - Gdx.input.getY();

            if (rooms != null && !rooms.isEmpty()) {
                float buttonHeight = 80;
                float buttonSpacing = screenHeight * 0.05f;
                float yOffset = screenHeight * 0.75f;

                for (GameRoom room : availableRooms) {
                    String roomInfo = roomLabel(room);

                    layout.setText(font, roomInfo);

                    if (touchX >= (screenWidth - layout.width) / 2f - 20 &&
                        touchX <= (screenWidth + layout.width) / 2f + 20 &&
                        touchY >= yOffset - layout.height - 10 &&
                        touchY <= yOffset + 10
                        && !Joined) {

                        Joined = true;
                        if (Joined && (room.getPlayers().size()) < room.getMaxPlayers() + 1) {
                            List<Integer> newRoom = room.getPlayers();
                            newRoom.add(myPlayerData.id);
                            myPlayerData.roomId = room.getRoomId();
                            room.setPlayers(newRoom);
                            saveRooms(rooms);
                            FishuraiGame.sendRoomToServer(room);
                            updateRooms();
                            FishuraiGame.gameStateFlag = FishuraiGame.GameScreen;
                            FishuraiGame.gameStarted = true;
                            statusMessage = "You joined room " + room.getRoomId();
                            playerRoom = room;
                            Joined = false;
                        } else {
                            statusMessage = "Room is full!";
                        }
                        isClicked = true;
                        break;
                    }
                    yOffset -= buttonHeight + buttonSpacing;
                }
            }
        }

        if (!Gdx.input.isTouched()) {
            isClicked = false;
        }

        if (!statusMessage.isEmpty()) {
            batch.begin();
            layout.setText(font, statusMessage);
            font.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight * 0.1f);
            batch.end();
        }
    }

    /** The player list includes the AI bot, hence the {@code - 1}. */
    private static String roomLabel(GameRoom room) {
        return String.format("%s (%d/%d)", room.getRoomId(), room.getPlayers().size() - 1, room.getMaxPlayers());
    }

    public List<GameRoom> updateRooms() {
        rooms = loadRooms();
        return rooms;
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        background.dispose();
    }
}
