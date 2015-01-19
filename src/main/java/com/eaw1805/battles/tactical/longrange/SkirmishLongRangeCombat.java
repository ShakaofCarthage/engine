package com.eaw1805.battles.tactical.longrange;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.data.model.army.Battalion;

/**
 * All skirmishing units with range > 7 fire.
 */
public class SkirmishLongRangeCombat
        extends ArtilleryLongRangeCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public SkirmishLongRangeCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_LONGRANGE_SK);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    @Override
    protected boolean checkArmyType(final Battalion battalion) {
        return battalion.getType().getFormationSk();
    }

    /**
     * Calculate the attack power of fortresses.
     * Fortress cannons do not participate in this combat round.
     *
     * @return 0.
     */
    protected double getFortressAttackPoints() {
        return 0d;
    }

}

