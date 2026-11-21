package main.java.ee.taltech.fishurai;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;

public class FloatingText {
    private static final float DURATION = 2f;

    private final String text;
    private final Color color;
    private final float x;
    private float y;
    private float elapsed = 0f;

    public FloatingText(String text, float x, float y, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void update(float delta) {
        elapsed += delta;
        y += delta * 20;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        font.setColor(color.r, color.g, color.b, 1f - (elapsed / DURATION));
        font.draw(batch, text, x, y);
    }

    public boolean isFinished() {
        return elapsed >= DURATION;
    }
}
