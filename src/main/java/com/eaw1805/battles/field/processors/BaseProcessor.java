package com.eaw1805.battles.field.processors;

import com.eaw1805.battles.field.FieldBattleProcessor;

/**
 * Base class for all processors in a field battle round.
 *
 * @author fragkakis
 */
public abstract class BaseProcessor {

    private final FieldBattleProcessor parent;

    public BaseProcessor(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    /**
     * Performs initialization tasks for the processor. Override if appropriate.
     */
    public void initialize() {
        // override as appropriate
    }

    /**
     * The most important class of the processor. To be invoked once per side per round.
     *
     * @param side
     * @param round
     */
    public abstract void process(int side, int round);

    /**
     * Returns the parent field battle processor.
     *
     * @return
     */
    public FieldBattleProcessor getParent() {
        return parent;
    }
}
