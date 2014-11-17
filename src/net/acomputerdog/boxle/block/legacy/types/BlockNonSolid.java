package net.acomputerdog.boxle.block.legacy.types;

import net.acomputerdog.boxle.main.Boxle;

/**
 * Superclass for non-solid blocks
 */
public abstract class BlockNonSolid extends BlockConfigurable {
    /**
     * Creates a new Block
     *
     * @param name The name of this block.
     */
    public BlockNonSolid(String name, Boxle boxle) {
        super(name, boxle);
        super.setCollidable(false);
        super.setTransparent(true);
        super.setLightReduction((byte) 0);
    }
}