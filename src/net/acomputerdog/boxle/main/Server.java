package net.acomputerdog.boxle.main;

/**
 * Boxle Server instance
 */
public class Server {
    /**
     * The owning Boxle instance;
     */
    private final Boxle boxle;

    /**
     * Create a new Server instance.
     *
     * @param boxle The parent boxle instance.
     */
    public Server(Boxle boxle) {
        if (boxle == null) throw new IllegalArgumentException("Boxle instance must not be null!");
        this.boxle = boxle;
    }

    /**
     * Initializes this server
     */
    public void init() {

    }

    /**
     * Ticks this server
     */
    public void tick() {

    }

    /**
     * Shuts down this server
     */
    public void shutdown() {

    }

    /**
     * Get the boxle instance of this Server.
     *
     * @return Return the boxle instance of this Server.
     */
    public Boxle getBoxle() {
        return boxle;
    }
}
