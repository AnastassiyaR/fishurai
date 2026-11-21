package ee.taltech.game.server;

import ee.taltech.game.server.fish.FishGameState;
import ee.taltech.game.server.fish.FishState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Random;

@Slf4j
@Getter
public class AiPlayer {
    private static final long ACTION_INTERVAL = 1500;
    private static final float FISHING_DISTANCE = 800f;
    private static final float WALK_SPEED = 120f;

    private final int id;
    private final Random random = new Random();

    private float x, y;
    private float velocityX, velocityY;

    @Setter private boolean startFishingAI;
    @Setter private FishGameState fishGameState;

    private int[][] collisionMap;
    private int tileSize;

    private float detectionRange = 700f;
    private float fishingDuration = 8f;
    private float hookSpeed = 150f;

    private float fishingTimer;
    private long lastActionTime = 0;
    private boolean isFishing = false;
    private float hookX, hookY;
    private FishState currentTarget;
    private int lastCaughtFishScore = 0;

    public AiPlayer(int id, float startX, float startY) {
        this.id = id;
        this.x = startX;
        this.y = startY;
    }

    public void setDifficulty(int difficulty) {
        log.info("AI difficulty set to {}", difficulty);
        switch (difficulty) {
            case 0, 1 -> configure(500f, 100f, 10f);
            case 2 -> configure(700f, 150f, 8f);
            case 3 -> configure(1000f, 300f, 6f);
            default -> { /* unknown difficulty: keep the current settings */ }
        }
    }

    private void configure(float detectionRange, float hookSpeed, float fishingDuration) {
        this.detectionRange = detectionRange;
        this.hookSpeed = hookSpeed;
        this.fishingDuration = fishingDuration;
    }

    public void update(float deltaTime) {
        if (!startFishingAI) return;

        if (isFishing) {
            updateFishing(deltaTime);
            return;
        }

        findTarget();

        if (currentTarget != null) {
            hunt(deltaTime);
        } else {
            wander(deltaTime);
        }
    }

    private void updateFishing(float deltaTime) {
        fishingTimer -= deltaTime;
        hookY -= hookSpeed * deltaTime;

        float distanceTraveled = y - hookY;
        if (fishingTimer <= 0 || hookY < 0 || distanceTraveled > FISHING_DISTANCE) {
            isFishing = false;
        }
    }

    private void findTarget() {
        if (fishGameState == null) return;

        currentTarget = fishGameState.getFishStates().stream()
                .filter(f -> !f.isCaught && f.y < y)
                .min(Comparator.comparingDouble(f -> distanceTo(f.x, f.y)))
                .filter(f -> distanceTo(f.x, f.y) < detectionRange)
                .orElse(null);
    }

    private void hunt(float deltaTime) {
        if (currentTarget == null || currentTarget.isCaught) {
            currentTarget = null;
            return;
        }

        float distance = distanceTo(currentTarget.x, currentTarget.y);

        if (Math.abs(currentTarget.x - x) < 100 && distance < FISHING_DISTANCE + 100) {
            startFishing();
            return;
        }

        float dirX = Math.signum(currentTarget.x - x);
        if (dirX == 0) return;

        float newX = x + dirX * WALK_SPEED * deltaTime;

        if (isCollision(newX, y)) {
            currentTarget = null;
            lastActionTime = System.currentTimeMillis();
        } else {
            x = newX;
            velocityX = dirX * WALK_SPEED;
            velocityY = 0;
        }
    }

    private void startFishing() {
        isFishing = true;
        fishingTimer = fishingDuration;
        hookX = x;
        hookY = y;
        velocityX = 0;
        velocityY = 0;
        lastActionTime = System.currentTimeMillis();
    }

    private void wander(float deltaTime) {
        if (System.currentTimeMillis() - lastActionTime > ACTION_INTERVAL) {
            lastActionTime = System.currentTimeMillis();

            switch (random.nextInt(5)) {
                case 0 -> setVelocity(0);
                case 1 -> setVelocity((random.nextBoolean() ? 1 : -1) * 80);
                case 2 -> setVelocity(-100);
                case 3 -> setVelocity(100);
                case 4 -> {
                    if (random.nextFloat() < 0.3f) {
                        startFishing();
                    }
                }
                default -> throw new IllegalStateException("Unreachable: nextInt(5)");
            }
        }

        float newX = x + velocityX * deltaTime;

        if (isCollision(newX, y)) {
            velocityX = -velocityX;
            lastActionTime = System.currentTimeMillis();
        } else {
            x = newX;
        }
    }

    private void setVelocity(float vx) {
        velocityX = vx;
        velocityY = 0;
    }

    public boolean tryCatchFish() {
        if (fishGameState == null) return false;

        lastCaughtFishScore = fishGameState.handleCatchAttemptAndReturnScore(hookX, hookY);
        return lastCaughtFishScore != 0;
    }

    public void setMapInfo(int[][] collisionMap, int tileSize) {
        this.collisionMap = collisionMap;
        this.tileSize = tileSize;
    }

    private float distanceTo(float fx, float fy) {
        float dx = fx - x;
        float dy = fy - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private boolean isCollision(float checkX, float checkY) {
        if (collisionMap == null) return false;

        int col = (int) (checkX / tileSize);
        int row = (int) (checkY / tileSize);

        if (col < 0 || row < 0 || col >= collisionMap.length || row >= collisionMap[0].length) {
            return true;
        }

        return collisionMap[col][row] == 1;
    }
}
