package main.java.ee.taltech.fishurai;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CollisionChecker {
    private final Map map;

    public boolean isTileCollision(int col, int row) {
        if (col < 0 || col >= map.getMaxWorldCol() || row < 0 || row >= map.getMaxWorldRow()) {
            return true;
        }

        int tileNum = map.getMapTileNum(col, row);
        return map.getTiles()[tileNum].collision;
    }
}
