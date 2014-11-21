package net.acomputerdog.boxle.block.sim.program.instruction.debug;

import net.acomputerdog.boxle.block.block.Block;
import net.acomputerdog.boxle.block.sim.program.Instruction;
import net.acomputerdog.boxle.block.sim.sim.Sim;
import net.acomputerdog.boxle.block.sim.sim.exec.SimException;
import net.acomputerdog.boxle.block.sim.stack.Stack;

public class InstructionPrintText extends Instruction {
    private final String text;

    public InstructionPrintText(String text) {
        super("DEBUG_PRINT_TEXT");
        this.text = text;
    }

    @Override
    public void execute(Sim sim, Stack stack, Block block) throws SimException {
        System.out.print(text);
    }
}
