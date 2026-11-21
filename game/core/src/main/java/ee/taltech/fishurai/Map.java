package main.java.ee.taltech.fishurai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import lombok.Getter;

import java.io.BufferedReader;

public class Map {
    @Getter private final int maxWorldCol = 64;
    @Getter private final int maxWorldRow = 30;
    @Getter private final int tileSize = 48;

    private int[][] mapTilesNum;
    @Getter private final Tiles[] tiles = new Tiles[30];
    private final OrthographicCamera camera;

    public Map(OrthographicCamera camera) {
        this.camera = camera;
        mapTilesNum = new int[maxWorldCol][maxWorldRow];
        tiles[0] = new Tiles(new Texture("tiles/cats.png"), true);
        tiles[1] = new Tiles(new Texture("tiles/earth.png"), false);
        tiles[2] = new Tiles(new Texture("tiles/flowers.png"), false);
        tiles[3] = new Tiles(new Texture("tiles/Hugetree.png"), true);
        tiles[4] = new Tiles(new Texture("tiles/newGrassBase.png"), false);

        tiles[5] = new Tiles(new Texture("tiles/NewBridge1.png"), true);
        tiles[6] = new Tiles(new Texture("tiles/newBridge3.png"), true);
        tiles[23] = new Tiles(new Texture("tiles/newBridge2.png"), true);

        tiles[7] = new Tiles(new Texture("tiles/water00.png"), true);
        tiles[8] = new Tiles(new Texture("tiles/water01.png"), true);

        tiles[9] = new Tiles(new Texture("tiles/NewWaterLeft.png"), true);
        tiles[10] = new Tiles(new Texture("tiles/WaterDown.png"), true);
        tiles[11] = new Tiles(new Texture("tiles/WaterRight.png"), true);
        tiles[12] = new Tiles(new Texture("tiles/WaterUp.png"), true);

        tiles[13] = new Tiles(new Texture("tiles/NewWaterLeftLeft.png"), true);
        tiles[14] = new Tiles(new Texture("tiles/NewWaterLeftUp.png"), true);
        tiles[15] = new Tiles(new Texture("tiles/NewWaterRightRight.png"), true);
        tiles[16] = new Tiles(new Texture("tiles/NewWaterRightUp.png"), true);

        tiles[17] = new Tiles(new Texture("tiles/newBridge.png"), true);
        tiles[18] = new Tiles(new Texture("tiles/newBridgeLeft.png"), true);

        tiles[19] = new Tiles(new Texture("tiles/newBridgeLeftUp.png"), true);
        tiles[20] = new Tiles(new Texture("tiles/newBridgeLeftDown.png"), true);

        tiles[21] = new Tiles(new Texture("tiles/newBridgeRightUp.png"), true);
        tiles[22] = new Tiles(new Texture("tiles/newBridgeRightDown.png"), true);

        loadMap("worldmap.txt");
    }

    private void loadMap(String filePath) {
        try (BufferedReader br = new BufferedReader(Gdx.files.internal(filePath).reader())) {
            int row = maxWorldRow - 1;
            String line;
            while ((line = br.readLine()) != null && row >= 0) {
                String[] numbers = line.split(" ");
                for (int col = 0; col < maxWorldCol; col++) {
                    mapTilesNum[col][row] = Integer.parseInt(numbers[col]);
                }
                row--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getMapTileNum(int col, int row) {
        return mapTilesNum[col][row];
    }

    public void draw(SpriteBatch batch, float offsetX, float offsetY) {
        int startCol = (int) (offsetX / tileSize);
        int startRow = (int) (offsetY / tileSize);

        startCol = Math.max(0, startCol);
        startRow = Math.max(0, startRow);

        int endCol = Math.min(maxWorldCol, startCol + (int) (camera.viewportWidth / tileSize) + 2);
        int endRow = Math.min(maxWorldRow, startRow + (int) (camera.viewportHeight / tileSize) + 2);

        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                int tileNum = mapTilesNum[col][row];
                Texture texture = tiles[tileNum].texture;
                float worldX = col * tileSize;
                float worldY = row * tileSize;
                batch.draw(texture, worldX, worldY, tileSize, tileSize);
            }
        }
    }

    public int getWidth() {
        return maxWorldCol * tileSize;
    }

    public int getHeight() {
        return maxWorldRow * tileSize;
    }

    public int[][] getWaterTiles() {
        int[][] waterTiles = new int[maxWorldCol][maxWorldRow];
        for (int x = 0; x < maxWorldCol; x++) {
            for (int y = 0; y < maxWorldRow; y++) {
                int tileId = mapTilesNum[x][y];
                waterTiles[x][y] = (tileId >= 7 && tileId <= 16) ? 1 : 0;
            }
        }
        return waterTiles;
    }

    public void dispose() {
        for (Tiles tile : tiles) {
            if (tile != null) { // not every slot of the tile table is used
                tile.texture.dispose();
            }
        }
    }

    public int[][] getCollisionTiles() {
        int[][] collisionTiles = new int[maxWorldCol][maxWorldRow];
        for (int col = 0; col < maxWorldCol; col++) {
            for (int row = 0; row < maxWorldRow; row++) {
                collisionTiles[col][row] = tiles[mapTilesNum[col][row]].collision ? 1 : 0;
            }
        }
        return collisionTiles;
    }
}
