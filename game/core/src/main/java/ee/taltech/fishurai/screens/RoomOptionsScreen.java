package main.java.ee.taltech.fishurai.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import main.java.ee.taltech.fishurai.FishuraiGame;
import main.java.ee.taltech.fishurai.rooms.GameRoom;

import java.util.ArrayList;
import java.util.List;

import static main.java.ee.taltech.fishurai.FishuraiGame.level;
import static main.java.ee.taltech.fishurai.rooms.ClientRoomManager.saveRooms;

public class RoomOptionsScreen {

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Texture background;
    public static boolean SinglePlayer = false;

    private static final float BASE_SCREEN_HEIGHT = 1080f;

    public RoomOptionsScreen() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        layout = new GlyphLayout();
        background = new Texture(Gdx.files.internal("background/SettingsBack.png"));

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/MaruMonica.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 80;
        param.color = Color.WHITE;
        font = generator.generateFont(param);
        generator.dispose();
    }

    public void render() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenWidth = camera.viewportWidth;
        float screenHeight = camera.viewportHeight;
        float fontScale = screenHeight / BASE_SCREEN_HEIGHT;

        float buttonWidth = screenWidth * 0.4f;
        float buttonHeight = 100f;
        float buttonX = (screenWidth - buttonWidth) / 2f;

        float createY = screenHeight * 0.65f;
        float joinY = screenHeight * 0.25f;

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        font.getData().setScale(fontScale);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(buttonX, createY, buttonWidth, buttonHeight);
        shapeRenderer.rect(buttonX, joinY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        batch.begin();
        layout.setText(font, "SinglePlayer");
        font.draw(batch, layout, buttonX + (buttonWidth - layout.width) / 2f, createY + buttonHeight / 1.5f);

        layout.setText(font, "MultiPlayer");
        font.draw(batch, layout, buttonX + (buttonWidth - layout.width) / 2f, joinY + buttonHeight / 1.5f);
        batch.end();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();

            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth) {
                if (mouseY >= createY && mouseY <= createY + buttonHeight) {
                    SinglePlayer = true;
                    FishuraiGame.gameStateFlag = FishuraiGame.LevelChoosing;


                } else if (mouseY >= joinY && mouseY <= joinY + buttonHeight) {
                    FishuraiGame.gameStateFlag = FishuraiGame.RoomScreen;

                }
            }
        }
        if (FishuraiGame.AiChosen && SinglePlayer) {
            List<GameRoom> newRooms = new ArrayList<>();
            GameRoom newRoom = new GameRoom();
            newRoom.setRoomId(String.valueOf(System.currentTimeMillis()));
            newRoom.setMaxPlayers(1);
            List<Integer> players = new ArrayList<>();
            players.add(FishuraiGame.myPlayerData.id);
            newRoom.setPlayers(players);
            newRoom.setAiDifficulty(level);
            newRooms.add(newRoom);
            saveRooms(newRooms);
            FishuraiGame.sendRoomToServer(newRoom);
            FishuraiGame.gameStateFlag = FishuraiGame.GameScreen;
            FishuraiGame.gameStarted = true;
        }
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        background.dispose();
    }
}
