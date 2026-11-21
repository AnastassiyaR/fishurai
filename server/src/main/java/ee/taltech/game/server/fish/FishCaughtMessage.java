package ee.taltech.game.server.fish;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class FishCaughtMessage {
    public int playerId;
    public boolean success;
    public int totalFishCaught;
    public int scoreGained;
}
