package net.acomputerdog.boxle.main;

import com.jme3.scene.Node;
import net.acomputerdog.boxle.config.GameConfig;
import net.acomputerdog.boxle.entity.Entity;
import net.acomputerdog.boxle.entity.types.EntityPlayer;
import net.acomputerdog.boxle.math.loc.CoordConverter;
import net.acomputerdog.boxle.math.spiral.Spiral2i;
import net.acomputerdog.boxle.math.vec.Vec2i;
import net.acomputerdog.boxle.math.vec.Vec3i;
import net.acomputerdog.boxle.math.vec.VecConverter;
import net.acomputerdog.boxle.math.vec.VecPool;
import net.acomputerdog.boxle.render.engine.ChunkRenderer;
import net.acomputerdog.boxle.render.engine.RenderEngine;
import net.acomputerdog.boxle.render.util.ChunkNode;
import net.acomputerdog.boxle.world.Chunk;
import net.acomputerdog.boxle.world.World;
import net.acomputerdog.boxle.world.structure.ChunkTable;
import net.acomputerdog.core.logger.CLogger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Boxle Server instance
 */
public class Server {
    private static final int chunkSize = Chunk.CHUNK_SIZE;
    /**
     * The owning Boxle instance;
     */
    private final Boxle boxle;

    private final Set<World> hostedWorlds = new CopyOnWriteArraySet<>();

    private World defaultWorld;

    private final GameConfig config;

    final int renderDistanceH;
    final int renderDistanceV;

    private final Set<Chunk> rebuildChunks = new ConcurrentSkipListSet<>();

    private int numChunks = 0;
    private int numUnload = 0;

    private Vec3i lastPlayerCLoc;
    private Spiral2i spiral;
    private final Vec2i spiralLoc;

    private final CLogger logger;

    private final RenderEngine engine;

    /**
     * Create a new Server instance.
     *
     * @param boxle The parent boxle instance.
     */
    public Server(Boxle boxle) {
        if (boxle == null) throw new IllegalArgumentException("Boxle instance must not be null!");
        this.boxle = boxle;
        config = boxle.getGameConfig();
        renderDistanceH = config.renderDistanceHorizontal;
        renderDistanceV = config.renderDistanceVertical;
        logger = new CLogger("Server", false, true);
        engine = boxle.getRenderEngine();
        spiralLoc = VecPool.getVec2i(0, 0);
    }

    /**
     * Initializes this server
     */
    public void init() {
        defaultWorld = new World(boxle, "server_default");
        boxle.getWorlds().addWorld(defaultWorld);
        hostedWorlds.add(defaultWorld);
    }

    /**
     * Ticks this server
     */
    public void tick() {
        long oldTime = System.currentTimeMillis();
        numChunks = 0;
        numUnload = 0;
        rebuildNeighborChunks();
        rebuildChangedChunks();
        unloadExtraChunks();
        if (config.outputRenderDebugInfo && numChunks > 0) {
            long newTime = System.currentTimeMillis();
            logger.logDetail("Built " + numChunks + " chunk meshes and unloaded " + numUnload + " chunks in " + ((newTime - oldTime) / 1000f) + " seconds.");
        }

        for (World world : hostedWorlds) {
            for (Entity entity : world.getEntities()) {
                entity.onTick();
            }
        }
    }

    private void unloadExtraChunks() {
        for (World world : hostedWorlds) {
            Vec3i center = CoordConverter.globalToChunk(VecConverter.vec3iFromVec3f(boxle.getClient().getPlayer().getLocation(), VecPool.createVec3i()));
            ChunkTable chunks = world.getChunks();
            for (Chunk chunk : chunks.getAllChunks()) {
                Vec3i cLoc = chunk.getLocation();
                if (cLoc.x > center.x + renderDistanceH || cLoc.x < center.x - renderDistanceH || cLoc.y > center.y + renderDistanceV || cLoc.y < center.y - renderDistanceV || cLoc.z > center.z + renderDistanceH || cLoc.z < center.z - renderDistanceH) {
                    numUnload++;
                    rebuildChunks.remove(chunk);
                    world.unloadChunk(chunk);
                }
            }
            VecPool.free(center);
        }
    }

    private void rebuildNeighborChunks() {
        for (Chunk chunk : rebuildChunks) {
            rebuildChunks.remove(chunk);
            numChunks++;
            Vec3i cLoc = chunk.getLocation();
            ChunkNode node = new ChunkNode("chunk@" + cLoc.asCoords());
            VecPool.free(cLoc);
            buildChunk(chunk, node, false);
            ChunkNode oldNode = chunk.getChunkNode();
            if (oldNode.getParent() != null) {
                engine.removeNode(oldNode);
            }
            chunk.setChunkNode(node);
            engine.addNode(node);
            if (numChunks > config.maxLoadedChunksPerTick) {
                return;
            }
        }
    }

    private void rebuildChangedChunks() {
        EntityPlayer player = boxle.getClient().getPlayer();
        World world = player.getWorld();
        ChunkTable chunks = world.getChunks();
        Vec3i center = CoordConverter.globalToChunk(VecConverter.vec3iFromVec3f(player.getLocation(), VecPool.createVec3i()));
        if (!center.equals(lastPlayerCLoc)) {
            VecPool.free(lastPlayerCLoc);
            lastPlayerCLoc = center;
            spiral = new Spiral2i(VecPool.getVec2i(center.x, center.z));
        }
        GameConfig config = boxle.getGameConfig();
        while (numChunks < config.maxLoadedChunksPerTick) {
            spiral.next(spiralLoc);
            int sX = spiralLoc.x;
            int sY = spiralLoc.y;
            if (Math.abs(sX - center.x) >= renderDistanceH) {
                break;
            }
            for (int y = -renderDistanceV; y <= renderDistanceV; y++) {
                Vec3i newLoc = VecPool.getVec3i(sX, center.y + y, sY);
                Chunk chunk = chunks.getChunk(newLoc);
                if (chunk == null) {
                    chunk = world.loadOrGenerateChunk(newLoc);
                }
                if (chunk.isNeedsRebuild()) {
                    numChunks++;
                    rebuildChunks.remove(chunk); //make sure the chunk is not rendered twice
                    ChunkNode node = new ChunkNode("chunk@" + newLoc.asCoords());
                    buildChunk(chunk, node, true);
                    ChunkNode oldNode = chunk.getChunkNode();
                    if (oldNode.getParent() != null) {
                        engine.removeNode(chunk.getChunkNode());
                    }
                    chunk.setChunkNode(node);
                    engine.addNode(node);
                }
                VecPool.free(newLoc);
            }
        }
    }


    public void buildChunk(Chunk chunk, Node node, boolean notifyNeighbors) {
        chunk.setNeedsRebuild(false);

        Vec3i cLoc = chunk.getLocation();
        Vec3i gLoc = CoordConverter.chunkToGlobal(cLoc.duplicate());
        ChunkRenderer.buildChunkMesh(gLoc, chunk, node);
        /*
        BlockStorage storage = chunk.getBlocks();
        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                for (int z = 0; z < chunkSize; z++) {
                    Block block = storage.getBlock(x, y, z);
                    if (block != null && block.isRenderable()) {
                        BlockTex tex = block.getTextures();
                        if (isTransparent(x + 1, y, z, chunk)) {
                            addFace(node, tex, gLoc, BlockFace.RIGHT, x, y, z);
                        }
                        if (isTransparent(x - 1, y, z, chunk)) {
                            addFace(node, tex, gLoc, BlockFace.LEFT, x, y, z);
                        }
                        if (isTransparent(x, y + 1, z, chunk)) {
                            addFace(node, tex, gLoc, BlockFace.TOP, x, y, z);
                        }
                        if (isTransparent(x, y - 1, z, chunk)) {
                            addFace(node, tex, gLoc, BlockFace.BOTTOM, x, y, z);
                        }
                        if (isTransparent(x, y, z + 1, chunk)) {
                            addFace(node, tex, gLoc, BlockFace.FRONT, x, y, z);
                        }
                        if (isTransparent(x, y, z - 1, chunk)) {
                            addFace(node, tex, gLoc, BlockFace.BACK, x, y, z);
                        }
                    }
                }
            }
        }
        GeometryBatchFactory.optimize(node); //enhance!
        */
        if (notifyNeighbors) {
            if (config.notifyNeighborsMode >= 0) {
                ChunkTable chunks = chunk.getWorld().getChunks();
                //immediate neighbors
                notifyNeighbor(cLoc, 1, 0, 0, chunks);
                notifyNeighbor(cLoc, -1, 0, 0, chunks);
                notifyNeighbor(cLoc, 0, 1, 0, chunks);
                notifyNeighbor(cLoc, 0, -1, 0, chunks);
                notifyNeighbor(cLoc, 0, 0, 1, chunks);
                notifyNeighbor(cLoc, 0, 0, -1, chunks);
                if (config.notifyNeighborsMode >= 1) {
                    //edges
                    notifyNeighbor(cLoc, 1, 1, 0, chunks);
                    notifyNeighbor(cLoc, 1, -1, 0, chunks);
                    notifyNeighbor(cLoc, -1, 1, 0, chunks);
                    notifyNeighbor(cLoc, -1, -1, 0, chunks);
                    notifyNeighbor(cLoc, 1, 0, 1, chunks);
                    notifyNeighbor(cLoc, 1, 0, -1, chunks);
                    notifyNeighbor(cLoc, -1, 0, 1, chunks);
                    notifyNeighbor(cLoc, 1, 0, -1, chunks);
                    notifyNeighbor(cLoc, 0, 1, 1, chunks);
                    notifyNeighbor(cLoc, 0, -1, 1, chunks);
                    notifyNeighbor(cLoc, 0, 1, -1, chunks);
                    notifyNeighbor(cLoc, 0, -1, -1, chunks);
                    if (config.notifyNeighborsMode >= 2) {
                        //corners
                        notifyNeighbor(cLoc, 1, 1, 1, chunks);
                        notifyNeighbor(cLoc, 1, 1, -1, chunks);
                        notifyNeighbor(cLoc, -1, 1, 1, chunks);
                        notifyNeighbor(cLoc, -1, 1, -1, chunks);
                        notifyNeighbor(cLoc, 1, -1, 1, chunks);
                        notifyNeighbor(cLoc, 1, -1, -1, chunks);
                        notifyNeighbor(cLoc, -1, -1, 1, chunks);
                        notifyNeighbor(cLoc, -1, -1, -1, chunks);
                        if (config.notifyNeighborsMode >= 3) {
                            //2nd layer immediates
                            notifyNeighbor(cLoc, 2, 0, 0, chunks);
                            notifyNeighbor(cLoc, -2, 0, 0, chunks);
                            notifyNeighbor(cLoc, 0, 2, 0, chunks);
                            notifyNeighbor(cLoc, 0, -2, 0, chunks);
                            notifyNeighbor(cLoc, 0, 0, 2, chunks);
                            notifyNeighbor(cLoc, 0, 0, -2, chunks);
                        }
                    }
                }
            }
        }
        VecPool.free(cLoc);
        VecPool.free(gLoc);
    }

    private void notifyNeighbor(Vec3i cLoc, int x, int y, int z, ChunkTable chunks) {
        Chunk nChunk = chunks.getChunk(x + cLoc.x, y + cLoc.y, z + cLoc.z);
        if (nChunk != null && !rebuildChunks.contains(nChunk)) {
            rebuildChunks.add(nChunk);
            nChunk.setNeedsRebuild(false);
        }
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

    public World getDefaultWorld() {
        return defaultWorld;
    }

    public Set<World> getHostedWorlds() {
        return Collections.unmodifiableSet(hostedWorlds);
    }
}
