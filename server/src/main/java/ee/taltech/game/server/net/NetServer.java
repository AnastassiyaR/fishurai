package ee.taltech.game.server.net;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket transport. Every connection gets an integer id (sent to it in a {@link WelcomeMessage}), and the game
 * code talks to clients only through ids, never through socket objects.
 * <p>
 * Incoming messages are handled on a single worker thread, in arrival order, so game state is never touched by two network threads at once.
 */
public class NetServer extends WebSocketServer {

    public interface Listener {
        void received(int connectionId, Object message);

        void disconnected(int connectionId);
    }

    private static final Logger log = LoggerFactory.getLogger(NetServer.class);

    /** Ping interval: keeps idle connections alive through proxies (Render) and detects dead peers. */
    private static final int PING_INTERVAL_SECONDS = 30;

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<Integer, WebSocket> connections = new ConcurrentHashMap<>();
    private final Listener listener;
    private final int reservedId;
    private final CountDownLatch started = new CountDownLatch(1);
    private volatile Exception startupError;

    /**
     * @param reservedId an id that is never handed to a client (the AI player's id)
     */
    public NetServer(int port, int reservedId, Listener listener) {
        super(new InetSocketAddress(port), 1); // one decoder thread => messages are processed sequentially
        this.listener = listener;
        this.reservedId = reservedId;
        setReuseAddr(true);
        setTcpNoDelay(true);
        setConnectionLostTimeout(PING_INTERVAL_SECONDS);
    }

    /** Starts listening and waits until the port is bound; throws if it could not be. */
    public void startAndWait(long timeoutMs) throws IOException {
        start();
        try {
            if (!started.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IOException("Server did not start within " + timeoutMs + " ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while starting the server", e);
        }
        if (startupError != null) {
            throw new IOException(startupError.getMessage(), startupError);
        }
    }

    // ---- sending ----------------------------------------------------------------------------------------------

    public void sendTo(int connectionId, Object message) {
        String json = encode(message);
        if (json != null) {
            sendRaw(connections.get(connectionId), json);
        }
    }

    /** Serializes once, then sends to each id (ids that are not connected are skipped). */
    public void sendToMany(Collection<Integer> connectionIds, Object message) {
        if (connectionIds.isEmpty()) return;
        String json = encode(message);
        if (json == null) return;
        for (Integer id : connectionIds) {
            sendRaw(connections.get(id), json);
        }
    }

    public void sendToAll(Object message) {
        String json = encode(message);
        if (json == null) return;
        for (WebSocket socket : connections.values()) {
            sendRaw(socket, json);
        }
    }

    private static String encode(Object message) {
        try {
            return MessageCodec.encode(message);
        } catch (Exception e) {
            log.error("Could not encode {}", message.getClass().getSimpleName(), e);
            return null;
        }
    }

    private static void sendRaw(WebSocket socket, String json) {
        if (socket == null || !socket.isOpen()) return;
        try {
            socket.send(json);
        } catch (WebsocketNotConnectedException e) {
            // The client went away between the check and the send; onClose cleans up.
        }
    }

    // ---- WebSocketServer callbacks ----------------------------------------------------------------------------

    @Override
    public void onOpen(WebSocket socket, ClientHandshake handshake) {
        int id;
        do {
            id = nextId.getAndIncrement();
        } while (id == reservedId);

        socket.setAttachment(id);
        connections.put(id, socket);
        log.info("Connection {} opened from {}", id, socket.getRemoteSocketAddress());
        sendTo(id, new WelcomeMessage(id));
    }

    @Override
    public void onClose(WebSocket socket, int code, String reason, boolean remote) {
        Integer id = socket.getAttachment();
        if (id == null) return;
        connections.remove(id);
        log.info("Connection {} closed (code {}, {})", id, code, reason);
        try {
            listener.disconnected(id);
        } catch (Exception e) {
            log.error("Error handling disconnect of {}", id, e);
        }
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        Integer id = socket.getAttachment();
        if (id == null) return;

        Object message;
        try {
            message = MessageCodec.decode(text);
        } catch (IOException e) {
            log.warn("Dropping bad message from {}: {}", id, e.getMessage());
            return;
        }
        try {
            listener.received(id, message);
        } catch (Exception e) {
            log.error("Error handling {} from {}", message.getClass().getSimpleName(), id, e);
        }
    }

    @Override
    public void onError(WebSocket socket, Exception ex) {
        if (socket == null && started.getCount() > 0) {
            // Failed to bind the port.
            startupError = ex;
            started.countDown();
        } else {
            log.warn("WebSocket error on connection {}", socket == null ? "-" : socket.getAttachment(), ex);
        }
    }

    @Override
    public void onStart() {
        log.info("WebSocket server listening on port {}", getPort());
        started.countDown();
    }
}
