package com.eaw1805.battles.field.processors;

import com.eaw1805.battles.field.FieldBattleProcessor;

/**
 * Base class for all processors in a field battle round.
 *
 * @author fragkakis
 */
public abstract class BaseBothSidesProcessor extends BaseAttackProcessor {

    public BaseBothSidesProcessor(FieldBattleProcessor parent) {
        super(parent);
    }

    /**
     * The most important class of the processor. To be invoked once per side per round.
     *
     * @param side
     * @param round
     */
    public abstract void process(int round);

}
