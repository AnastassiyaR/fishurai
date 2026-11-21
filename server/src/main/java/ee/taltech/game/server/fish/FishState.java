package ee.taltech.game.server.fish;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FishState {
    public float x;
    public float y;
    public boolean isCaught;
    public int type;

    public FishState(float x, float y, boolean isCaught, FishServer.FishType type) {
        this.x = x;
        this.y = y;
        this.isCaught = isCaught;
        this.type = type.ordinal();
    }
}
