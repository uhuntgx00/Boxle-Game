package net.acomputerdog.boxle.block.sim.util;

import net.acomputerdog.boxle.block.atom.Atom;
import net.acomputerdog.boxle.block.atom.types.value.PushBooleanAtom;
import net.acomputerdog.boxle.block.atom.types.value.PushFloatAtom;
import net.acomputerdog.boxle.block.atom.types.value.PushIntAtom;
import net.acomputerdog.boxle.block.atom.types.value.PushStringAtom;
import net.acomputerdog.boxle.block.block.Block;
import net.acomputerdog.boxle.block.registry.Atoms;
import net.acomputerdog.boxle.block.sim.program.Program;
import net.acomputerdog.boxle.block.sim.program.tree.InstructionBranch;
import net.acomputerdog.boxle.block.sim.program.tree.InstructionTree;
import net.acomputerdog.boxle.block.sim.sim.Sim;
import net.acomputerdog.boxle.block.sim.sim.SimResult;
import net.acomputerdog.boxle.block.sim.sim.state.SimState;
import net.acomputerdog.boxle.main.Boxle;
import net.acomputerdog.core.java.Patterns;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropLoader {
    private static final Map<String, Properties> propertyMap = new HashMap<>();

    public static Block loadAndCreateBlock(String name, InputStream in) {
        try {
            return createBlock(loadProp(name, in));
        } catch (Exception e) {
            System.err.println("Exception loading block: " + name);
            e.printStackTrace();
            throw new RuntimeException("Exception loading block", e);
        }
    }

    public static Properties loadProp(String name, InputStream in) {
        Properties prop = propertyMap.get(name);
        if (prop == null) {
            prop = new Properties();
            try {
                prop.load(in);
                try {
                    in.close();
                } catch (Exception ignored) {}
                prop.setProperty("prop_name", name);
            } catch (IOException e) {
                throw new RuntimeException("Exception reading property file!");
            }
            propertyMap.put(name, prop);
            Boxle.instance().LOGGER_MAIN.logDetail("Loaded block from prop: " + name);
        }
        return prop;
    }

    private static Properties getParent(Properties prop) {
        if (prop.containsKey("parent")) {
            String parentName = prop.getProperty("parent");
            Properties parentProp = getParent(loadProp(parentName, PropLoader.class.getResourceAsStream(parentName)));
            Properties temp = new Properties(parentProp);
            temp.putAll(prop);
            prop.putAll(temp);
        }
        return prop;
    }

    public static Block createBlock(Properties prop) {
        getParent(prop);
        Program program = new Program();
        program.setId(prop.getProperty("id"));
        program.setName(prop.getProperty("name"));
        InstructionTree tree = program.getInstructions();
        InstructionBranch root = tree.getStartInstruction();

        if (prop.containsKey("breakable")) addProperty(root, getBool(prop.getProperty("breakable")), Atoms.propertyBreakable);
        if (prop.containsKey("collidable")) addProperty(root, getBool(prop.getProperty("collidable")), Atoms.propertyCollidable);
        if (prop.containsKey("transparent")) addProperty(root, getBool(prop.getProperty("transparent")), Atoms.propertyTransparent);
        if (prop.containsKey("renderable")) addProperty(root, getBool(prop.getProperty("renderable")), Atoms.propertyRenderable);
        if (prop.containsKey("light_reduction"))
            addProperty(root, getInt(prop.getProperty("light_reduction")), Atoms.propertyLightReduction);
        if (prop.containsKey("light_output")) addProperty(root, getInt(prop.getProperty("light_output")), Atoms.propertyLightOutput);
        if (prop.containsKey("resistance")) addProperty(root, getFloat(prop.getProperty("resistance")), Atoms.propertyResistance);
        if (prop.containsKey("explosion_resistance"))
            addProperty(root, getFloat(prop.getProperty("explosion_resistance")), Atoms.propertyExplosionResistance);
        if (prop.containsKey("strength")) addProperty(root, getFloat(prop.getProperty("strength")), Atoms.propertyStregth);
        if (prop.containsKey("hardness")) addProperty(root, getFloat(prop.getProperty("hardness")), Atoms.propertyHardness);
        if (prop.containsKey("bounds")) {
            String[] boundStrings = prop.getProperty("bounds").split(Patterns.COMMA);
            if (boundStrings.length < 6) {
                Boxle.instance().LOGGER_MAIN.logWarning("Improperly formatted bounds: \"" + prop.getProperty("bounds") + "\".  Should be 6 comma-separated float values.");
            } else {
                InstructionBranch last = root;
                for (int index = 0; index < 6; index++) {
                    last = last.addOutput(getFloat(boundStrings[index]));
                }
                last.addOutput(Atoms.propertyBounds);
            }
        }
        if (prop.containsKey("tex")) {
            //TODO actually embed the textures
            String[] texPaths = prop.getProperty("tex").split(Patterns.COMMA);
            if (texPaths.length < 6) {
                Boxle.instance().LOGGER_MAIN.logWarning("Improperly formatted texture paths: \"" + prop.getProperty("tex") + "\".  Should be 6 comma-separated strings.");
            } else {
                InstructionBranch last = root;
                for (int index = 0; index < 6; index++) {
                    last = last.addOutput(getString(texPaths[index]));
                }
                last.addOutput(Atoms.propertyTex);
            }
        }

        try {
            Sim sim = new Sim(program);
            SimResult result = sim.startSim();
            SimState endState = result.getEndState();
            if (endState != SimState.FINISHED) {
                throw new RuntimeException("Sim finished unexpectedly with state " + endState.getState());
            }
            return result.getBlock();
        } catch (Exception e) {
            throw new RuntimeException("Caught exception simulating block!", e);
        }
    }

    private static void addProperty(InstructionBranch root, Atom value, Atom property) {
        root.addOutput(value).addOutput(property);
    }

    private static PushBooleanAtom getBool(String value) {
        return Boolean.parseBoolean(value) ? Atoms.valPushTrue : Atoms.valPushFalse;
    }

    private static PushIntAtom getInt(String value) {
        PushIntAtom atom = (PushIntAtom) Atoms.ATOMS.getFromId("VALUE.PUSH_INT_" + value);
        if (atom == null) {
            int atomVal = Integer.parseInt(value);
            atom = new PushIntAtom("Push " + atomVal + "i", atomVal);
            Atoms.ATOMS.register(atom);
        }
        return atom;
    }

    private static PushFloatAtom getFloat(String value) {
        PushFloatAtom atom = (PushFloatAtom) Atoms.ATOMS.getFromId("VALUE.PUSH_FLOAT_" + value);
        if (atom == null) {
            float atomVal = Float.parseFloat(value);
            atom = new PushFloatAtom("Push " + atomVal + "f", atomVal);
            Atoms.ATOMS.register(atom);
        }
        return atom;
    }

    private static PushStringAtom getString(String value) {
        PushStringAtom atom = (PushStringAtom) Atoms.ATOMS.getFromId("VALUE.PUSH_STRING_" + value);
        if (atom == null) {
            atom = new PushStringAtom("Push string " + value, value);
            Atoms.ATOMS.register(atom);
        }
        return atom;
    }
}