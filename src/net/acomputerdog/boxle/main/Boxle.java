package net.acomputerdog.boxle.main;

import net.acomputerdog.boxle.config.GameConfig;
import net.acomputerdog.boxle.render.engine.RenderEngine;
import net.acomputerdog.boxle.world.World;
import net.acomputerdog.core.logger.CLogger;
import org.lwjgl.opengl.Display;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boxle main class
 */
public class Boxle {
    /**
     * Logger that logs without date or time.  Useful for high-output debug messages.
     */
    public static final CLogger LOGGER_FAST = new CLogger("Boxle", false, false);
    /**
     * Normal logger that just logs time.
     */
    public static final CLogger LOGGER_MAIN = new CLogger("Boxle", false, true);
    /**
     * Full logger that outputs date and time.  Useful for crash messages.
     */
    public static final CLogger LOGGER_FULL = new CLogger("Boxle", true, true);

    /**
     * Central render engine
     */
    private final RenderEngine renderEngine;

    /**
     * Map of world names to instances.
     */
    private final Map<String, World> worldMap;

    /**
     * Global game config.
     */
    private final GameConfig gameConfig;

    /**
     * The instance of the game client
     */
    private final Client client;

    /**
     * The instance of the game server;
     */
    private final Server server;

    /**
     * If true the game can run, if false, a close has been requested.
     */
    private boolean canRun = true;

    /**
     * Creates a new Boxle instance
     */
    private Boxle() {
        this.renderEngine = new RenderEngine(this);
        worldMap = new ConcurrentHashMap<>();
        gameConfig = new GameConfig(this);
        client = new Client(this);
        server = new Server(this);
        try {
            init();
        } catch (Throwable t) {
            LOGGER_FULL.logFatal("Caught exception during init phase!", t);
            end(-1);
        }
        try {
            run();
        } catch (Throwable t) {
            LOGGER_FULL.logFatal("Caught exception during run phase!", t);
            end(-2);
        }
        try {
            LOGGER_FULL.logError("Reached invalid area of code!  Shutting down!");
            cleanup();
        } catch (Throwable t) {
            LOGGER_FULL.logFatal("Caught excpetion in invalid area of code!", t);
        }
        end(-3);
    }

    /**
     * Initializes boxle to start.
     */
    private void init() {
        LOGGER_FULL.logInfo("Boxle is initializing.");
        //must be in order server -> client -> render
        server.init();
        client.init();
        renderEngine.init();
    }

    /**
     * Performs actual game loop.
     */
    private void run() {
        LOGGER_FULL.logInfo("Boxle is starting.");
        while (canRun) {
            server.tick(); //todo separate thread
            client.tick(); //todo separate thread
            renderEngine.render(); //todo separate thread
            Display.sync(gameConfig.maxFPS); //limit the tick speed to max FPS
        }
        cleanup();
        end(0);
    }

    /**
     * Cleanup and prepare to close.
     */
    private void cleanup() {
        //must be in order render -> client -> server
        renderEngine.cleanup();
        client.shutdown();
        server.shutdown();
    }

    /**
     * Saves and exits.  Should not do any game actions, and should be safe to call without try-catch blocks.
     * @param code The error code to return.
     */
    private void end(int code) {
        if (code == 0) {
            LOGGER_FULL.logInfo("Boxle shutting down normally.");
        } else {
            LOGGER_FULL.logWarning("Boxle shutting down abnormally: error code " + code + ".");
        }
        System.exit(code);
    }

    /**
     * Gets a world by it's name.
     *
     * @param name The name of the world.
     * @return Returns the instance of the world, or null if none exists or name is null.
     */
    public World getWorld(String name) {
        if (name == null) {
            throw new IllegalArgumentException("World name must not be null!");
        }
        return worldMap.get(name);
    }

    /**
     * Adds a world to the world map.
     *
     * @param world The world to add.
     */
    public void addWorld(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null!");
        }
        String name = world.getName();
        if (name == null) {
            throw new IllegalArgumentException("World has a null name!");
        }
        worldMap.put(name, world);
    }

    /**
     * Requests the game to stop.
     */
    public void stop() {
        canRun = false;
    }

    /**
     * Checks if the game is running, or if game components should begin shutting down.
     * @return return true if the game is running
     */
    public boolean canRun() {
        return canRun;
    }

    /**
     * Gets the RenderEngine of this Boxle instance.
     *
     * @return return the RenderEngine of this Boxle instance
     */
    public RenderEngine getRenderEngine() {
        return renderEngine;
    }

    /**
     * Gets the entire world map.
     *
     * @return Return the world map.
     */
    public Map<String, World> getWorldMap() {
        return worldMap;
    }

    /**
     * Gets the game config.
     *
     * @return Return the GameConfig controlling this game.
     */
    public GameConfig getGameConfig() {
        return gameConfig;
    }

    /**
     * Gets the client of this game.
     *
     * @return Return the client of this game.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Gets the server of this game.
     *
     * @return Return the server of this game.
     */
    public Server getServer() {
        return server;
    }

    /**
     * Starts boxle
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        new Boxle();
    }
}
