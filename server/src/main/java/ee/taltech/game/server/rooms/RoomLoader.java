package ee.taltech.game.server.rooms;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
public class RoomLoader {

    private static final File ROOMS_FILE = new File("rooms.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RoomLoader() {
    }

    public static void saveRooms(List<GameRoom> rooms) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(ROOMS_FILE, rooms);
            log.info("Rooms saved to {}", ROOMS_FILE);
        } catch (IOException e) {
            log.error("Failed to save client rooms", e);
        }
    }
}
