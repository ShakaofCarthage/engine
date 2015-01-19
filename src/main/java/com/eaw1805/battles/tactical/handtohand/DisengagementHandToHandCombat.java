package com.eaw1805.battles.tactical.handtohand;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Hand-to-Hand Combat round.
 */
public class DisengagementHandToHandCombat
        extends TroopHandToHandCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public DisengagementHandToHandCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_HANDCOMBAT_D);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    @Override
    protected boolean checkArmyType(final Battalion battalion) {
        return true;
    }

    /**
     * The modified for dividing the combat points.
     *
     * @return modified for dividing the combat points.
     */
    @Override
    protected double divisionModifier() {
        return 500d;
    }

}
