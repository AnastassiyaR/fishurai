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


public class SettingsScreen {

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Texture background;

    private float volume = FishuraiGame.volume;

    private final float BASE_SCREEN_HEIGHT = 1080f;

    public SettingsScreen() {
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

        float sliderWidth = screenWidth * 0.5f;
        float sliderHeight = 20f;
        float sliderX = (screenWidth - sliderWidth) / 2f;
        float volumeY = screenHeight * 0.6f;
        float brightnessY = screenHeight * 0.4f;

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();

            if (mouseY > volumeY - 20 && mouseY < volumeY + 20)
                volume = Math.min(1, Math.max(0, (mouseX - sliderX) / sliderWidth));
            if (mouseY > brightnessY - 20 && mouseY < brightnessY + 20)
                FishuraiGame.brightness = Math.min(1, Math.max(0, (mouseX - sliderX) / sliderWidth));
        }

        FishuraiGame.volume = volume;

        if (FishuraiGame.temmieMusic != null) {
            FishuraiGame.temmieMusic.setVolume(FishuraiGame.volume);
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        font.getData().setScale(fontScale);

        layout.setText(font, "Settings");
        font.draw(batch, layout, (screenWidth - layout.width) / 2f, screenHeight * 0.8f);

        layout.setText(font, "Volume");
        font.draw(batch, layout, sliderX, volumeY + 60);

        layout.setText(font, "Brightness");
        font.draw(batch, layout, sliderX, brightnessY + 60);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.rect(sliderX, volumeY, sliderWidth, sliderHeight);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(sliderX, volumeY, sliderWidth * volume, sliderHeight);

        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.rect(sliderX, brightnessY, sliderWidth, sliderHeight);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(sliderX, brightnessY, sliderWidth * FishuraiGame.brightness, sliderHeight);
        shapeRenderer.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 1f - FishuraiGame.brightness);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        background.dispose();
    }
}
