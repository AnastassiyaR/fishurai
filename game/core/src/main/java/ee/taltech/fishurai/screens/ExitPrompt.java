package main.java.ee.taltech.fishurai.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import main.java.ee.taltech.fishurai.FishuraiGame;

public class ExitPrompt {

    private final BitmapFont fontTitle;
    private final BitmapFont fontSubtitle;
    private final SpriteBatch batch;
    private final GlyphLayout layout;
    private final Texture background;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;

    private final String titleText = "Do you want to exit?";
    private final String subtitleTextYES = "YES";
    private final String subtitleTextNO = "NO";
    private final String subtitleTextROW = ">";

    private boolean isYesSelected = true;

    private final float BASE_SCREEN_HEIGHT = 1080f;

    public ExitPrompt() {
        batch = new SpriteBatch();
        layout = new GlyphLayout();
        shapeRenderer = new ShapeRenderer();
        background = new Texture(Gdx.files.internal("background/exitBack.jpg"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/MaruMonica.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 90;
        titleParam.color = Color.WHITE;
        titleParam.borderWidth = 2;
        titleParam.borderColor = Color.DARK_GRAY;
        fontTitle = generator.generateFont(titleParam);

        FreeTypeFontGenerator.FreeTypeFontParameter subtitleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        subtitleParam.size = 70;
        subtitleParam.color = Color.WHITE;
        fontSubtitle = generator.generateFont(subtitleParam);

        generator.dispose();
    }

    public void render() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenWidth = camera.viewportWidth;
        float screenHeight = camera.viewportHeight;
        float fontScale = screenHeight / BASE_SCREEN_HEIGHT;

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        fontTitle.getData().setScale(fontScale);
        fontSubtitle.getData().setScale(fontScale);

        batch.begin();

        layout.setText(fontTitle, titleText);
        float titleX = (screenWidth - layout.width) / 2f;
        float titleY = screenHeight / 2f + 200 * fontScale;
        fontTitle.draw(batch, layout, titleX, titleY);

        layout.setText(fontSubtitle, subtitleTextYES);
        float yesX = (screenWidth / 2f) - 200 * fontScale - layout.width / 2f;
        float optionY = screenHeight / 2f - 50 * fontScale;
        fontSubtitle.draw(batch, layout, yesX, optionY);

        layout.setText(fontSubtitle, subtitleTextNO);
        float noX = (screenWidth / 2f) + 200 * fontScale - layout.width / 2f;
        fontSubtitle.draw(batch, layout, noX, optionY);

        layout.setText(fontSubtitle, subtitleTextROW);
        float arrowX = isYesSelected ? yesX - 60 * fontScale : noX - 60 * fontScale;
        fontSubtitle.draw(batch, layout, arrowX, optionY);

        batch.end();

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT) ||
            Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            isYesSelected = !isYesSelected;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
            if (isYesSelected) {
                Gdx.app.exit();
            } else {
                FishuraiGame.gameStateFlag = FishuraiGame.GameScreen;
            }
        }
    }

    public void dispose() {
        fontTitle.dispose();
        fontSubtitle.dispose();
        batch.dispose();
        background.dispose();
        shapeRenderer.dispose();
    }
}
