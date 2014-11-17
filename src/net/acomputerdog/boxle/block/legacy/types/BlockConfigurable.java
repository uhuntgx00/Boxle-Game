package net.acomputerdog.boxle.block.legacy.types;

import net.acomputerdog.boxle.block.block.Block;
import net.acomputerdog.boxle.main.Boxle;
import net.acomputerdog.boxle.math.aabb.AABBF;

/**
 * Abstract block subclass that adds methods and fields for values such as strength,, resitance, light value, etc.
 */
public abstract class BlockConfigurable extends Block {
    private boolean isBreakable = true;
    private float resistance = 1.0f;
    private float explosionResistance = 1.0f;
    private float strength = 100f;
    private float hardness = .1f;
    private boolean isCollidable = true;
    private boolean isTransparent = false;
    private byte lightReduction = (byte) 255;
    private byte lightOutput = 0;
    private boolean renderable = true;
    private AABBF bounds;

    /**
     * Creates a new Block
     *
     * @param name The name of this block.
     */
    public BlockConfigurable(String name, Boxle boxle) {
        super(name, boxle);
        bounds = super.getBlockBounds();
    }

    @Override
    public boolean isBreakable() {
        return isBreakable;
    }

    @Override
    public float getResistance() {
        return resistance;
    }

    @Override
    public float getExplosionResistance() {
        return explosionResistance;
    }

    @Override
    public float getStrength() {
        return strength;
    }

    @Override
    public float getHardness() {
        return hardness;
    }

    @Override
    public boolean isCollidable() {
        return isCollidable;
    }

    @Override
    public boolean isTransparent() {
        return isTransparent;
    }

    @Override
    public byte getLightReduction() {
        return lightReduction;
    }

    @Override
    public byte getLightOutput() {
        return lightOutput;
    }

    @Override
    public boolean isRenderable() {
        return renderable;
    }

    @Override
    public AABBF getBlockBounds() {
        return bounds;
    }

    public void setBreakable(boolean isBreakable) {
        this.isBreakable = isBreakable;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
    }

    public void setExplosionResistance(float explosionResistance) {
        this.explosionResistance = explosionResistance;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public void setHardness(float hardness) {
        this.hardness = hardness;
    }

    public void setCollidable(boolean collidable) {
        this.isCollidable = collidable;
    }

    public void setTransparent(boolean isTransparent) {
        this.isTransparent = isTransparent;
    }

    public void setLightReduction(byte lightReduction) {
        this.lightReduction = lightReduction;
    }

    public void setLightOutput(byte lightOutput) {
        this.lightOutput = lightOutput;
    }

    public void setRenderable(boolean renderable) {
        this.renderable = renderable;
    }

    public void setBounds(AABBF bounds) {
        this.bounds = bounds;
    }
}
