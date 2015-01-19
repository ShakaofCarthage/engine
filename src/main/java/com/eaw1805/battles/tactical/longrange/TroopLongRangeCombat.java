package com.eaw1805.battles.tactical.longrange;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Long Range Combat round.
 */
public class TroopLongRangeCombat
        extends SkirmishLongRangeCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public TroopLongRangeCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_LONGRANGE);
    }

    /**
     * Calculate the attack power of fortresses.
     *
     * @return the combat points due to the cannons of the fort.
     */
    protected double getFortressAttackPoints() {
        return getParent().getFortAttackFactor()
                * terrainFactorAr()
                * (getParent().getRandomGen().nextInt(1) + 1);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    @Override
    protected boolean checkArmyType(final Battalion battalion) {
        return ((battalion.getType().isInfantry() && !battalion.getType().getFormationSk() && battalion.getType().getLongRange() > 0)
                || (battalion.getType().isCavalry() && !battalion.getType().getFormationSk() && battalion.getType().getLongRange() > 0)
                || battalion.getType().isArtillery()
                || battalion.getType().isMArtillery());
    }

    /**
     * The modified for dividing the combat points.
     *
     * @return modified for dividing the combat points.
     */
    protected double divisionModifier() {
        return 333d;
    }

}
