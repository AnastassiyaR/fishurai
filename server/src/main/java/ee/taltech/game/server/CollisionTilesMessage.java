package ee.taltech.game.server;

import lombok.NoArgsConstructor;

/** Received from the client only; Kryo needs the no-args constructor and reads/writes the public fields. */
@NoArgsConstructor
public class CollisionTilesMessage {
    public int[][] collisionTiles;
    public int tileSize;
}
