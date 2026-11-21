package ee.taltech.game.server.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.game.server.CollisionTilesMessage;
import ee.taltech.game.server.GameTimerMessage;
import ee.taltech.game.server.PlayerData;
import ee.taltech.game.server.WaterTilesMessage;
import ee.taltech.game.server.fish.FishCaughtMessage;
import ee.taltech.game.server.fish.FishStateList;
import ee.taltech.game.server.fish.FishingAttempt;
import ee.taltech.game.server.rooms.GameRoom;
import ee.taltech.game.server.rooms.GameRoomList;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JSON wire format shared with the client: every WebSocket text frame is {@code {"type": "...", "data": {...}}}.
 * The type tag (not the Java class name) identifies the message, because client and server use different packages.
 * Keep the type names in sync with the client's {@code MessageCodec}.
 */
public final class MessageCodec {

    /** Marker message with no payload: the client asks for the room list. */
    public static final String GET_ROOMS = "getRooms";

    private static final String ROOMS = "Rooms";
    private static final String WELCOME = "Welcome";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Class<?>> INCOMING = Map.of(
            "GameRoomList", GameRoomList.class,
            "GameRoom", GameRoom.class,
            "PlayerData", PlayerData.class,
            "WaterTilesMessage", WaterTilesMessage.class,
            "CollisionTilesMessage", CollisionTilesMessage.class,
            "FishingAttempt", FishingAttempt.class
    );

    private static final Map<Class<?>, String> OUTGOING = Map.of(
            PlayerData.class, "PlayerData",
            GameTimerMessage.class, "GameTimerMessage",
            FishStateList.class, "FishStateList",
            FishCaughtMessage.class, "FishCaughtMessage",
            WelcomeMessage.class, WELCOME
    );

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

    /** A {@code List} is sent as the room list; everything else by its class. */
    public static String encode(Object message) throws JsonProcessingException {
        String type = message instanceof List ? ROOMS : OUTGOING.get(message.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No wire type for " + message.getClass().getName());
        }
        return MAPPER.writeValueAsString(new Envelope(type, message));
    }

    /** Returns the decoded message, or the string {@link #GET_ROOMS}. Throws on malformed or unknown input. */
    public static Object decode(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        String type = root == null ? "" : root.path("type").asText("");
        if (GET_ROOMS.equals(type)) {
            return GET_ROOMS;
        }
        Class<?> target = INCOMING.get(type);
        if (target == null) {
            throw new IOException("Unknown message type '" + type + "'");
        }
        if (!root.hasNonNull("data")) {
            throw new IOException("Message '" + type + "' has no data");
        }
        return MAPPER.treeToValue(root.get("data"), target);
    }
}
