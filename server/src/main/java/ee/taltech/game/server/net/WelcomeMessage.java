package ee.taltech.game.server.net;

/** First message every client receives: tells it the id the server assigned to its connection. */
public class WelcomeMessage {
    public int id;

    public WelcomeMessage() {
    }

    public WelcomeMessage(int id) {
        this.id = id;
    }
}
