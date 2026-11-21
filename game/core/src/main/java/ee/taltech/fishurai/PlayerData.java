package main.java.ee.taltech.fishurai;

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
    // Must mirror the server's PlayerData field-for-field: Kryo's FieldSerializer is layout-sensitive.
    public boolean gameStarted;
}
