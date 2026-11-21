package main.java.ee.taltech.fishurai.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class InstructionScreen {

    private final SpriteBatch batch;
    private final Texture background;
    private final OrthographicCamera camera;
    private final GlyphLayout layout;
    private final ShapeRenderer shapeRenderer;

    private final BitmapFont headerFont;
    private final BitmapFont controlFont;

    private final float BASE_SCREEN_HEIGHT = 1080f;

    private final String[] controls = {
        "⇧  (Arrow Up)    - Move Forward",
        "⇩  (Arrow Down)  - Move Back",
        "⇨  (Arrow Right) - Move Right",
        "⇦  (Arrow Left)  - Move Left",
        "SPACE            - Catch Fish",
        "ESC              - End Game",
        "ENTER            - Start Game",
        "T                - Settings"
    };

    public InstructionScreen() {
        batch = new SpriteBatch();
        layout = new GlyphLayout();
        shapeRenderer = new ShapeRenderer();
        background = new Texture(Gdx.files.internal("background/back.jpg"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/SuperShiny.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter headerParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        headerParam.size = 100;
        headerParam.color = Color.CYAN;
        headerParam.borderWidth = 2;
        headerParam.borderColor = Color.NAVY;
        headerFont = generator.generateFont(headerParam);

        FreeTypeFontGenerator.FreeTypeFontParameter textParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textParam.size = 52;
        textParam.color = Color.WHITE;
        textParam.borderWidth = 3;
        textParam.borderColor = Color.BLACK;
        controlFont = generator.generateFont(textParam);

        generator.dispose();
    }

    public void render() {
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        float screenWidth = camera.viewportWidth;
        float screenHeight = camera.viewportHeight;
        float fontScale = screenHeight / BASE_SCREEN_HEIGHT;

        headerFont.getData().setScale(fontScale);
        controlFont.getData().setScale(fontScale);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.2f, 0.6f));
        shapeRenderer.rect(screenWidth * 0.15f, screenHeight * 0.28f, screenWidth * 0.7f, screenHeight * 0.54f);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        layout.setText(headerFont, "Game Controls");
        float titleX = (screenWidth - layout.width) / 2f;
        float titleY = screenHeight - 100 * fontScale;
        headerFont.draw(batch, layout, titleX, titleY);

        float y = titleY - 100 * fontScale;
        for (String line : controls) {
            layout.setText(controlFont, line);
            float x = (screenWidth - layout.width) / 2f;
            controlFont.draw(batch, layout, x, y);
            y -= 60 * fontScale;
        }

        layout.setText(controlFont, "Press ENTER to continue...");
        float tipX = (screenWidth - layout.width) / 2f;
        controlFont.setColor(Color.YELLOW);
        controlFont.draw(batch, layout, tipX, y - 40 * fontScale);
        controlFont.setColor(Color.WHITE);

        batch.end();
    }

    public void dispose() {
        headerFont.dispose();
        controlFont.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        background.dispose();
    }
}
