package ee.taltech.game.server.fish;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class FishGameState {

    private final int[][] waterTiles;
    private final int tileSize;
    private final List<FishServer> fishes = new ArrayList<>();

    public FishGameState(int[][] waterTiles, int tileSize) {
        this.waterTiles = waterTiles;
        this.tileSize = tileSize;
        for (FishServer.FishType type : FishServer.FishType.values()) {
            spawnFishesInWater(type, type.getTargetCount());
        }
    }

    private void spawnFishesInWater(FishServer.FishType type, int count) {
        int spawned = 0;
        int maxAttempts = count * 5;

        for (int attempts = 0; spawned < count && attempts < maxAttempts; attempts++) {
            float x = (float) (Math.random() * getMaxX());
            float y = (float) (Math.random() * getMaxY());

            if (isWater(x, y)) {
                fishes.add(new FishServer(x, y, type));
                spawned++;
            }
        }

        if (spawned < count) {
            log.warn("Only spawned {} out of {} requested {} fishes.", spawned, count, type);
        }
    }

    public boolean isWater(float x, float y) {
        if (waterTiles == null || waterTiles.length == 0 || waterTiles[0].length == 0) {
            return false;
        }

        int tileX = (int) (x / tileSize);
        int tileY = (int) (y / tileSize);

        if (tileX < 0 || tileY < 0 || tileX >= waterTiles.length || tileY >= waterTiles[0].length) {
            return false;
        }
        return waterTiles[tileX][tileY] == 1;
    }

    public float getMaxX() {
        return waterTiles.length * tileSize;
    }

    public float getMaxY() {
        return waterTiles[0].length * tileSize;
    }

    public void update(float delta) {
        fishes.removeIf(FishServer::isCaught);
        fishes.forEach(fish -> fish.update(delta, this));

        // Restock every type up to its target count.
        for (FishServer.FishType type : FishServer.FishType.values()) {
            long missing = type.getTargetCount() - fishes.stream().filter(f -> f.getType() == type).count();
            if (missing > 0) {
                spawnFishesInWater(type, (int) missing);
            }
        }
    }

    /**
     * Goes over the wire inside {@link FishStateList} as a JSON array.
     */
    public List<FishState> getFishStates() {
        return fishes.stream()
                .map(fish -> new FishState(fish.getPositionX(), fish.getPositionY(), fish.isCaught(), fish.getType()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Tries to catch a fish at the hook position; returns the score gained, or 0 if nothing was caught. */
    public synchronized int handleCatchAttemptAndReturnScore(float hookX, float hookY) {
        for (FishServer fish : fishes) {
            if (fish.tryCatch(hookX, hookY)) {
                return fish.getType().getScore();
            }
        }
        return 0;
    }
}
