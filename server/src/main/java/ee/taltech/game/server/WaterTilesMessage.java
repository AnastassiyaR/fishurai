package ee.taltech.game.server;

import lombok.NoArgsConstructor;

/** Received from the client only; Jackson needs the no-args constructor and reads/writes the public fields. */
@NoArgsConstructor
public class WaterTilesMessage {
    public int[][] waterTiles;
    public int tileSize;
}
