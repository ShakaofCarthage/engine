package com.eaw1805.battles.tactical.longrange;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Long Range Combat round for Mounted artillery.
 */
public class MountedArtilleryLongRangeCombat
        extends ArtilleryLongRangeCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public MountedArtilleryLongRangeCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_ARTILLERY_MA);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    @Override
    protected boolean checkArmyType(final Battalion battalion) {
        return (battalion.getType().isMArtillery());
    }

    /**
     * Calculate the attack power of fortresses.
     * Fortress cannons do not participate in this combat round.
     *
     * @return 0.
     */
    protected double getFortressAttackPoints() {
        // Fortress artillery shoots at the enemy at round 1&2 (medium+ fortresses) at tactical battles.
        if (getParent().getFortAttackFactor() > 750d) {
            return getParent().getFortAttackFactor()
                    * terrainFactorAr()
                    * (getParent().getRandomGen().nextInt(2) + 1);
        } else {
            return 0d;
        }
    }

}
