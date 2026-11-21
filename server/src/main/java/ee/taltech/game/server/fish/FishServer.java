package ee.taltech.game.server.fish;

import lombok.Getter;

@Getter
public class FishServer {

    private static final float FISH_SCALE = 0.2f;
    private static final float FISH_WIDTH = 370f * FISH_SCALE;
    private static final float FISH_HEIGHT = 250f * FISH_SCALE;
    private static final float EARLY_COLLISION_MARGIN = 12f;

    /**
     * Declaration order is part of the network protocol: {@link FishState#type} is the ordinal.
     */
    @Getter
    public enum FishType {
        COMMON(1.3f, 100, 25),
        UNCOMMON(1.6f, 250, 4),
        RARE(1.3f, 500, 1),
        TRASH(1.2f, -100, 10);

        private final float speedMultiplier;
        private final int score;
        /** How many fish of this type the pond is kept stocked with. */
        private final int targetCount;

        FishType(float speedMultiplier, int score, int targetCount) {
            this.speedMultiplier = speedMultiplier;
            this.score = score;
            this.targetCount = targetCount;
        }
    }

    private final FishType type;
    private float positionX, positionY;
    private float velocityX, velocityY;
    private boolean isCaught;

    public FishServer(float x, float y, FishType type) {
        this.positionX = x;
        this.positionY = y;
        this.type = type;

        this.velocityX = (float) (Math.random() * 80 - 40) * type.speedMultiplier;
        this.velocityY = (float) (Math.random() * 40 - 20) * type.speedMultiplier;
    }

    public void update(float deltaTime, FishGameState water) {
        if (isCaught) return;

        float newX = positionX + velocityX * deltaTime;
        float newY = positionY + velocityY * deltaTime;

        boolean collisionX = velocityX != 0
                && !water.isWater(newX + (velocityX > 0 ? FISH_WIDTH : 0), newY + FISH_HEIGHT / 2);

        boolean collisionY = velocityY != 0
                && !water.isWater(newX + FISH_WIDTH / 2,
                velocityY > 0 ? newY + FISH_HEIGHT + EARLY_COLLISION_MARGIN : newY - EARLY_COLLISION_MARGIN);

        if (collisionX) {
            velocityX = -velocityX;
        } else {
            positionX = newX;
        }

        if (collisionY) {
            velocityY = -velocityY;
        } else {
            positionY = newY;
        }
    }

    public boolean tryCatch(float hookX, float hookY) {
        if (isCaught) return false;

        isCaught = hookX >= positionX && hookX <= positionX + FISH_WIDTH
                && hookY >= positionY && hookY <= positionY + FISH_HEIGHT;
        return isCaught;
    }
}
