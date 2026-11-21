package main.java.ee.taltech.fishurai.fish;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@RequiredArgsConstructor
public class FishClient {
    @NonNull private Texture texture;
    private Vector2 position = new Vector2();
    private boolean isCaught;
    private float scale = 0.2f;
    private boolean active = true;

    public void render(SpriteBatch batch) {
        if (!isCaught && active) {
            batch.draw(texture, position.x, position.y,
                texture.getWidth() * scale,
                texture.getHeight() * scale);
        }
    }

    public void updateFromServer(FishState state) {
        this.position.set(state.x, state.y);
        this.isCaught = state.isCaught;
    }

}
