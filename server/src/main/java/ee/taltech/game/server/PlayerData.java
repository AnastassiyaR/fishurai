package ee.taltech.game.server;

import lombok.ToString;

@ToString
public class PlayerData {
    public int id;
    public float x, y;
    public String texture;
    public boolean facingRight;
    public Integer fishCaught;
    public boolean isFishing;
    public float hookX, hookY;
    public boolean isDisconnected;
    public int score;
    public String roomId;
    public boolean gameStarted;
}
