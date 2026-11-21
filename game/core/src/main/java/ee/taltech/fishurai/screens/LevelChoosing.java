package main.java.ee.taltech.fishurai.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import main.java.ee.taltech.fishurai.FishuraiGame;

import static main.java.ee.taltech.fishurai.screens.RoomOptionsScreen.SinglePlayer;

public class LevelChoosing {

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Texture background;
    private final float BASE_SCREEN_HEIGHT = 1080f;
    private String selectedDifficulty = "MEDIUM";

    private String levelText = "";
    private float levelTextDisplayTime = 0f;
    private final float MAX_DISPLAY_TIME = 2f;

    public LevelChoosing() {
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
        if (levelTextDisplayTime > 0) {
            levelTextDisplayTime -= Gdx.graphics.getDeltaTime();
        } else {
            levelText = "";
        }

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenWidth = camera.viewportWidth;
        float screenHeight = camera.viewportHeight;
        float fontScale = screenHeight / BASE_SCREEN_HEIGHT;

        float buttonWidth = screenWidth * 0.25f;
        float buttonHeight = screenHeight * 0.15f;
        float buttonY = screenHeight * 0.5f;
        float buttonSpacing = screenWidth * 0.05f;

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();

            if (mouseY > buttonY && mouseY < buttonY + buttonHeight) {
                if (mouseX > buttonSpacing && mouseX < buttonSpacing + buttonWidth) {
                    selectedDifficulty = "EASY";
                    FishuraiGame.level = 1;
                    levelText = "EASY | Click Enter to continue";
                    levelTextDisplayTime = MAX_DISPLAY_TIME;
                } else if (mouseX > buttonSpacing*2 + buttonWidth && mouseX < buttonSpacing*2 + buttonWidth*2) {
                    selectedDifficulty = "MEDIUM";
                    FishuraiGame.level = 2;
                    levelText = "MEDIUM | Click Enter to continue";
                    levelTextDisplayTime = MAX_DISPLAY_TIME;
                } else if (mouseX > buttonSpacing*3 + buttonWidth*2 && mouseX < buttonSpacing*3 + buttonWidth*3) {
                    selectedDifficulty = "HARD";
                    FishuraiGame.level = 3;
                    levelText = "HARD | Click Enter to continue";
                    levelTextDisplayTime = MAX_DISPLAY_TIME;
                }
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.ENTER) && FishuraiGame.level == -1) {
            levelText = "Choose level >:(";
        }

        if (Gdx.input.isKeyPressed(Input.Keys.ENTER) && FishuraiGame.level != -1) {
            FishuraiGame.AiChosen = true;
            if (SinglePlayer) {
                FishuraiGame.gameStateFlag = FishuraiGame.RoomOptionsScreen;
                FishuraiGame.gameStarted = true;
            } else {
                FishuraiGame.gameStateFlag = FishuraiGame.RoomScreen;
            }
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        font.getData().setScale(fontScale);

        layout.setText(font, "Choose Difficulty");
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight * 0.8f);

        layout.setText(font, "EASY");
        font.draw(batch, layout, buttonSpacing + (buttonWidth - layout.width)/2
            , buttonY + buttonHeight/2 + layout.height/2);

        layout.setText(font, "MEDIUM");
        font.draw(batch, layout, buttonSpacing*2 + buttonWidth + (buttonWidth - layout.width)/2,
            buttonY + buttonHeight/2 + layout.height/2);

        layout.setText(font, "HARD");
        font.draw(batch, layout, buttonSpacing*3 + buttonWidth*2 + (buttonWidth - layout.width)/2,
            buttonY + buttonHeight/2 + layout.height/2);

        if (!levelText.isEmpty()) {
            layout.setText(font, levelText);
            float textX = (screenWidth - layout.width) / 2f;
            float textY = screenHeight * 0.3f;

            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0, 0, 0, 0.5f));
            shapeRenderer.rect(textX - 20, textY - layout.height - 20, layout.width + 40,
                layout.height + 40);
            shapeRenderer.end();
            batch.begin();

            font.setColor(Color.WHITE);
            font.draw(batch, levelText, textX, textY);
        }

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(selectedDifficulty.equals("EASY") ? Color.GREEN : Color.LIGHT_GRAY);
        shapeRenderer.rect(buttonSpacing, buttonY, buttonWidth, buttonHeight);

        shapeRenderer.setColor(selectedDifficulty.equals("MEDIUM") ? Color.YELLOW : Color.LIGHT_GRAY);
        shapeRenderer.rect(buttonSpacing*2 + buttonWidth, buttonY, buttonWidth, buttonHeight);

        shapeRenderer.setColor(selectedDifficulty.equals("HARD") ? Color.RED : Color.LIGHT_GRAY);
        shapeRenderer.rect(buttonSpacing*3 + buttonWidth*2, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(buttonSpacing, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(buttonSpacing*2 + buttonWidth, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(buttonSpacing*3 + buttonWidth*2, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        background.dispose();
    }
}
