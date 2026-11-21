package main.java.ee.taltech.fishurai.fish;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import java.util.List;

public class FishManagerClient {
    private final Array<FishClient> fishes = new Array<>();
    private final Texture commonFishTexture;
    private final Texture uncommonFishTexture;
    private final Texture rareFishTexture;
    private final Texture trashTexture;

    public FishManagerClient(Texture commonFishTexture, Texture uncommonFishTexture,
                             Texture rareFishTexture, Texture trashTexture, int initialCount) {
        this.commonFishTexture = commonFishTexture;
        this.uncommonFishTexture = uncommonFishTexture;
        this.rareFishTexture = rareFishTexture;
        this.trashTexture = trashTexture;

        for (int i = 0; i < initialCount; i++) {
            fishes.add(new FishClient(commonFishTexture));
        }
    }

    public void updateFromServer(List<FishState> fishStates) {
        while (fishes.size < fishStates.size()) {
            fishes.add(new FishClient(commonFishTexture));
        }

        for (int i = 0; i < fishStates.size(); i++) {
            FishState state = fishStates.get(i);
            FishClient fish = fishes.get(i);

            fish.setTexture(textureFor(state.type));
            fish.updateFromServer(state);
            fish.setActive(true);
        }

        for (int i = fishStates.size(); i < fishes.size; i++) {
            fishes.get(i).setActive(false);
        }
    }

    /** The type is the ordinal of the server's FishType enum: 0 common, 1 uncommon, 2 rare, 3 trash. */
    private Texture textureFor(int type) {
        switch (type) {
            case 1: return uncommonFishTexture;
            case 2: return rareFishTexture;
            case 3: return trashTexture;
            default: return commonFishTexture;
        }
    }

    public void render(SpriteBatch batch) {
        for (FishClient fish : fishes) {
            fish.render(batch);
        }
    }
}
