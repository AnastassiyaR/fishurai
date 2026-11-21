package main.java.ee.taltech.fishurai.rooms;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Must mirror the server's GameRoom (non-transient fields): Kryo's FieldSerializer is layout-sensitive.
@Setter
@Getter
public class GameRoom {
    private String roomId;
    private List<Integer> players = new ArrayList<>();
    private int maxPlayers;
    private int id;
    private int AiDifficulty;
    private int AiCount = 1;
}
