package main.java.ee.taltech.fishurai.net;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.ee.taltech.fishurai.CollisionTilesMessage;
import main.java.ee.taltech.fishurai.GameTimerMessage;
import main.java.ee.taltech.fishurai.PlayerData;
import main.java.ee.taltech.fishurai.WaterTilesMessage;
import main.java.ee.taltech.fishurai.fish.FishCaughtMessage;
import main.java.ee.taltech.fishurai.fish.FishStateList;
import main.java.ee.taltech.fishurai.fish.FishingAttempt;
import main.java.ee.taltech.fishurai.rooms.GameRoom;
import main.java.ee.taltech.fishurai.rooms.GameRoomList;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON wire format shared with the server: every WebSocket text frame is {@code {"type": "...", "data": {...}}}.
 * The type tag (not the Java class name) identifies the message, because client and server use different packages.
 * Keep the type names in sync with the server's {@code MessageCodec}.
 */
public final class MessageCodec {

    /** Marker message with no payload: asks the server for the room list. */
    public static final String GET_ROOMS = "getRooms";

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Class<?>> INCOMING = new HashMap<String, Class<?>>();
    private static final Map<Class<?>, String> OUTGOING = new HashMap<Class<?>, String>();

    static {
        INCOMING.put("Welcome", WelcomeMessage.class);
        INCOMING.put("PlayerData", PlayerData.class);
        INCOMING.put("GameTimerMessage", GameTimerMessage.class);
        INCOMING.put("FishStateList", FishStateList.class);
        INCOMING.put("FishCaughtMessage", FishCaughtMessage.class);

        OUTGOING.put(GameRoomList.class, "GameRoomList");
        OUTGOING.put(GameRoom.class, "GameRoom");
        OUTGOING.put(PlayerData.class, "PlayerData");
        OUTGOING.put(WaterTilesMessage.class, "WaterTilesMessage");
        OUTGOING.put(CollisionTilesMessage.class, "CollisionTilesMessage");
        OUTGOING.put(FishingAttempt.class, "FishingAttempt");
    }

    /** Serialized form of one frame. */
    public static final class Envelope {
        public final String type;
        public final Object data;

        public Envelope(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    private MessageCodec() {
    }

    public static String encode(Object message) throws IOException {
        if (GET_ROOMS.equals(message)) {
            return MAPPER.writeValueAsString(new Envelope(GET_ROOMS, null));
        }
        String type = OUTGOING.get(message.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No wire type for " + message.getClass().getName());
        }
        return MAPPER.writeValueAsString(new Envelope(type, message));
    }

    /** Returns a {@link WelcomeMessage}, {@link PlayerData}, {@code List<GameRoom>}, etc. Throws on bad input. */
    public static Object decode(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        String type = root == null ? "" : root.path("type").asText("");
        if (!root.hasNonNull("data")) {
            throw new IOException("Message '" + type + "' has no data");
        }
        JsonNode data = root.get("data");
        if ("Rooms".equals(type)) {
            List<GameRoom> rooms = MAPPER.readValue(MAPPER.treeAsTokens(data), new TypeReference<List<GameRoom>>() { });
            return rooms;
        }
        Class<?> target = INCOMING.get(type);
        if (target == null) {
            throw new IOException("Unknown message type '" + type + "'");
        }
        return MAPPER.treeToValue(data, target);
    }
}
