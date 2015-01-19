package com.eaw1805.battles.tactical.longrange;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Disengagement Long Range Combat round.
 */
public class DisengagementLongRangeCombat
        extends ArtilleryLongRangeCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public DisengagementLongRangeCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_HANDCOMBAT_LR);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    @Override
    protected boolean checkArmyType(final Battalion battalion) {
        return (battalion.getType().isArtillery() || (battalion.getType().isInfantry() && battalion.getType().getFormationSk()));
    }

    /**
     * The modified for dividing the combat points.
     *
     * @return modified for dividing the combat points.
     */
    @Override
    protected double divisionModifier() {
        return 600d;
    }

}
