package main.java.ee.taltech.fishurai.rooms;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ClientRoomManager {
    public static final List<GameRoom> rooms = new ArrayList<>();

    public static void saveRooms(List<GameRoom> roomies) {
        rooms.addAll(roomies);
    }

    /** Returns the latest version of every known room, one entry per room id. */
    public static List<GameRoom> loadRooms() {
        Map<String, GameRoom> latestRooms = new HashMap<>();
        for (GameRoom room : rooms) {
            if (room.getRoomId() != null) {
                latestRooms.put(room.getRoomId(), room);
            }
        }

        List<GameRoom> uniqueRooms = new ArrayList<>(latestRooms.values());

        log.info("Loaded rooms");
        for (GameRoom room : uniqueRooms) {
            log.info("{}: {}", room.getRoomId(), room.getPlayers());
        }
        return uniqueRooms;
    }
}
