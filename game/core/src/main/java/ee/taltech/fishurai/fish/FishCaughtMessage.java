package main.java.ee.taltech.fishurai.fish;

import lombok.NoArgsConstructor;

/** Received from the server only; Kryo needs the no-args constructor. */
@NoArgsConstructor
public class FishCaughtMessage {
    public int playerId;
    public boolean success;
    public int totalFishCaught;
    public int scoreGained;
}
