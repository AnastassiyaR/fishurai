package ee.taltech.game.server.rooms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ee.taltech.game.server.AiPlayer;
import ee.taltech.game.server.PlayerData;
import ee.taltech.game.server.fish.FishGameState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GameRoom {
    private String roomId;
    private List<Integer> players = new ArrayList<>();
    private int maxPlayers;
    private int id;
    private int AiDifficulty;
    private int AiCount = 1;

    // Runtime-only state: excluded from Kryo (transient) and from Jackson (@JsonIgnore).
    @JsonIgnore private transient AiPlayer aiController;
    @JsonIgnore private transient PlayerData aiPlayer;
    @JsonIgnore private transient FishGameState fishGameState;

    public boolean isGameStarted() {
        return players.size() >= maxPlayers || maxPlayers == 1;
    }

    public void addPlayer(int player) {
        players.add(player);
    }
}
