package main.java.ee.taltech.fishurai.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class LobbyScreen {

    private final BitmapFont fontTitle;
    private final BitmapFont fontSubtitle;
    private final SpriteBatch batch;
    private final GlyphLayout layout;
    private final Texture background;
    private final OrthographicCamera camera;

    private static final String TITLE_TEXT = "Fishurai";
    private static final String SUBTITLE_TEXT = "Press ENTER to start the game";

    private static final float BASE_SCREEN_HEIGHT = 1080f;

    public LobbyScreen() {
        batch = new SpriteBatch();
        layout = new GlyphLayout();

        background = new Texture(Gdx.files.internal("background/back.jpg"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/SuperShiny.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 200;
        titleParam.color = Color.WHITE;
        titleParam.borderWidth = 2;
        titleParam.borderColor = Color.LIGHT_GRAY;
        fontTitle = generator.generateFont(titleParam);

        FreeTypeFontGenerator.FreeTypeFontParameter subtitleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        subtitleParam.size = 64;
        subtitleParam.color = Color.WHITE;
        subtitleParam.borderWidth = 5;
        subtitleParam.borderColor = Color.BLACK;
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

        fontTitle.getData().setScale(fontScale);
        fontSubtitle.getData().setScale(fontScale);

        float offsetY = 200 * fontScale;

        layout.setText(fontTitle, TITLE_TEXT);
        float titleX = (screenWidth - layout.width) / 2f;
        float titleY = screenHeight / 2f + offsetY * 2;
        fontTitle.draw(batch, layout, titleX, titleY);

        layout.setText(fontSubtitle, SUBTITLE_TEXT);
        float subtitleX = (screenWidth - layout.width) / 2f;
        float subtitleY = titleY - (layout.height + 600 * fontScale);
        fontSubtitle.draw(batch, layout, subtitleX, subtitleY);

        batch.end();
    }
}
