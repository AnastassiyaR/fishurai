package main.java.ee.taltech.fishurai.net;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket connection to the game server.
 * <p>
 * Connecting happens on a background thread and is retried for a couple of minutes, because a free Render service
 * goes to sleep when idle and needs up to a minute to wake up on the first request. The game keeps rendering and
 * shows {@link #getStatus()} until {@link #isReady()} turns true.
 * <p>
 * The server address comes from the {@code fishurai.server} system property, then the {@code FISHURAI_SERVER}
 * environment variable, then {@link #DEFAULT_SERVER_URL}. For a local server use e.g.
 * {@code java -Dfishurai.server=ws://localhost:8080 -jar FISHURAI.jar}.
 */
public class GameClient {

    public interface Listener {
        /** Called on the network thread. */
        void received(Object message);
    }

    public static final String DEFAULT_SERVER_URL = "wss://fishurai.onrender.com";

    private enum State { CONNECTING, READY, FAILED, CLOSED }

    private static final Logger log = LoggerFactory.getLogger(GameClient.class);

    private static final long TOTAL_CONNECT_BUDGET_MS = 150_000;
    private static final long ATTEMPT_TIMEOUT_MS = 25_000;
    private static final long WELCOME_TIMEOUT_MS = 10_000;
    private static final long RETRY_DELAY_MS = 2_000;
    private static final int PING_INTERVAL_SECONDS = 30;

    private final URI uri;
    private volatile Listener listener = message -> { };
    private volatile Runnable onReady = () -> { };

    private volatile State state = State.CONNECTING;
    private volatile String status = "Connecting to server...";
    private volatile int id;
    private volatile WebSocketClient socket;
    private volatile boolean stopped;

    public GameClient(String url) {
        this.uri = URI.create(url);
    }

    public static String resolveServerUrl() {
        String url = System.getProperty("fishurai.server");
        if (url == null || url.trim().isEmpty()) {
            url = System.getenv("FISHURAI_SERVER");
        }
        if (url == null || url.trim().isEmpty()) {
            url = DEFAULT_SERVER_URL;
        }
        return url.trim();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Called once, on the network thread, after the server has assigned this client its id. */
    public void setOnReady(Runnable onReady) {
        this.onReady = onReady;
    }

    /** Starts connecting in the background. */
    public void start() {
        Thread thread = new Thread(this::connectLoop, "fishurai-connect");
        thread.setDaemon(true);
        thread.start();
    }

    /** The id the server gave this connection; 0 until {@link #isReady()}. */
    public int getID() {
        return id;
    }

    public boolean isReady() {
        return state == State.READY;
    }

    /** Human-readable connection state, for showing on screen while not ready. */
    public String getStatus() {
        return status;
    }

    public void send(Object message) {
        WebSocketClient current = socket;
        if (state != State.READY || current == null) {
            log.debug("Not connected, dropping {}", message.getClass().getSimpleName());
            return;
        }
        try {
            current.send(MessageCodec.encode(message));
        } catch (Exception e) {
            log.warn("Could not send {}: {}", message.getClass().getSimpleName(), e.getMessage());
        }
    }

    public void stop() {
        stopped = true;
        WebSocketClient current = socket;
        if (current != null) {
            current.close();
        }
    }

    // ---- connecting -------------------------------------------------------------------------------------------

    private void connectLoop() {
        long deadline = System.currentTimeMillis() + TOTAL_CONNECT_BUDGET_MS;
        int attempt = 0;

        while (!stopped && System.currentTimeMillis() < deadline) {
            attempt++;
            status = attempt == 1
                ? "Connecting to server..."
                : "Waiting for the server to wake up (attempt " + attempt + ")...";

            if (tryConnect()) {
                return;
            }
            if (stopped) return;
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        if (!stopped) {
            state = State.FAILED;
            status = "Could not connect to the server.\nCheck your internet connection and restart the game.";
            log.error("Giving up connecting to {}", uri);
        }
    }

    /** One connection attempt; returns true once the server has welcomed us. */
    private boolean tryConnect() {
        final CountDownLatch welcomed = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                log.info("Connected to {}", uri);
            }

            @Override
            public void onMessage(String text) {
                handleText(this, text, welcomed);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("Connection closed (code {}, {})", code, reason);
                if (socket == this && state == State.READY && !stopped) {
                    state = State.CLOSED;
                    status = "Connection to the server was lost.\nRestart the game to reconnect.";
                }
            }

            @Override
            public void onError(Exception ex) {
                log.warn("Connection error: {}", ex.toString());
            }
        };
        client.setConnectionLostTimeout(PING_INTERVAL_SECONDS);

        try {
            socket = client;
            if (!client.connectBlocking(ATTEMPT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                client.close();
                return false;
            }
            if (!welcomed.await(WELCOME_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("Server did not send a welcome message");
                client.close();
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            client.close();
            return false;
        } catch (RuntimeException e) {
            log.warn("Connection attempt failed: {}", e.toString());
            client.close();
            return false;
        }

        if (stopped) {
            client.close();
            return true;
        }

        state = State.READY;
        status = "Connected";
        try {
            onReady.run();
        } catch (RuntimeException e) {
            log.error("Error in onReady handler", e);
        }
        return true;
    }

    private void handleText(WebSocketClient from, String text, CountDownLatch welcomed) {
        Object message;
        try {
            message = MessageCodec.decode(text);
        } catch (Exception e) {
            log.warn("Dropping bad message from server: {}", e.getMessage());
            return;
        }

        if (message instanceof WelcomeMessage) {
            id = ((WelcomeMessage) message).id;
            welcomed.countDown();
            return;
        }
        if (state != State.READY || socket != from) {
            return; // anything before the welcome / from an abandoned attempt is ignored
        }
        try {
            listener.received(message);
        } catch (RuntimeException e) {
            log.error("Error processing network message", e);
        }
    }
}
