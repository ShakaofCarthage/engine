package com.eaw1805.battles.tactical.longrange;

import com.eaw1805.battles.tactical.AbstractTacticalBattleRound;
import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Long Range Combat round for Heavy and Light artillery.
 */
public class ArtilleryLongRangeCombat
        extends AbstractTacticalBattleRound {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public ArtilleryLongRangeCombat(final TacticalBattleProcessor caller) {
        super(caller, ROUND_ARTILLERY_HLA);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    protected boolean checkArmyType(final Battalion battalion) {
        return battalion.getType().isArtillery();
    }

    /**
     * Calculate the attack power of fortresses.
     *
     * @return the combat points due to the cannons of the fort.
     */
    protected double getFortressAttackPoints() {
        // // Fortress artillery shoots at the enemy at round 1 (small fortress).
        return getParent().getFortAttackFactor()
                //* terrainFactorAr()
                * (getParent().getRandomGen().nextInt(10) + 1) / 10d;
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    public RoundStatistics process() {
        final int[] totPioneers = new int[2];
        final double[] combatPoints = new double[2];
        final double[] lostSoldiers = new double[2];
        final double[][] statRound = new double[2][4];
        final int[] totBatts = new int[2];

        // Calculate combat points
        for (int side = 0; side < 2; side++) {
            combatPoints[side] = 0;
            totBatts[side] = 0;
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (checkArmyType(battalion) && !battalion.isFleeing()) {
                    combatPoints[side] += calcCombatPoints(battalion);
                    battalion.setParticipated(true);

                } else {
                    battalion.setParticipated(false);
                }

                // count battalions with at least 1 soldier
                if (battalion.getHeadcount() > 0) {
                    totBatts[side]++;
                }

                // count the number of pioneer units
                if (battalion.getType().isEngineer()) {
                    totPioneers[side]++;
                }
            }

            if (combatPoints[side] < 0) {
                combatPoints[side] = 0;
            }

            statRound[side][0] = combatPoints[side];
            statRound[side][1] = 0;
            statRound[side][2] = 0;
            statRound[side][3] = 0;
        }

        // Defender may be behind a fort
        if (getParent().getFortress() > 0) {
            final double fortPoints = getFortressAttackPoints();

            combatPoints[0] += fortPoints;
            statRound[0][0] += fortPoints;
        }

        // Custom Games: Fierce Battles (+25%)
        final double casModifier;
        if (getParent().getField().getPosition().getGame().isFierceCasualties()) {
            casModifier = 1.25d;

        } else {
            casModifier = 1d;
        }

        // Distribute casualties
        for (int side = 0; side < 2; side++) {
            lostSoldiers[side] = casModifier * combatPoints[(side + 1) % 2] / totBatts[side];

            // determine the fort bonus for the defender.
            double fortBonus = 1d;
            if (side == 0) {
                fortBonus = getParent().getFortDefenceBonus();
            }

            // Go through all battalions
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                double bonus = 1d;

                if (battalion.getType().getTroopSpecsCu()) {
                    // Cuirassiers suffer 25% fewer casualties from long-range combat.
                    bonus = .75d;
                }

                // For every pioneer battalion participating in combat,
                // another infantry or artillery battalion (chosen randomly)
                // will get a 20% decrease of its losses during the fire and melee rounds.
                if ((battalion.getType().isInfantry() || battalion.getType().isArtillery()) && (totPioneers[side] > 0)) {
                    bonus *= .8d;
                    totPioneers[side]--;
                }

                // apply losses
                int actualLosses = (int) (lostSoldiers[side] * bonus * fortBonus);

                // make sure we do not end up with negative headcount
                if (battalion.getHeadcount() < actualLosses) {
                    actualLosses = battalion.getHeadcount();
                }

                // update headcount and statistics
                battalion.setHeadcount(battalion.getHeadcount() - actualLosses);
                statRound[side][1] += actualLosses;
            }
        }

        return new RoundStatistics(getRound(), getParent().getSideBattalions(), statRound);
    }

    /**
     * The modified for dividing the combat points.
     *
     * @return modified for dividing the combat points.
     */
    protected double divisionModifier() {
        return 500d;
    }

    /**
     * Calculate the combat points for the battalion.
     *
     * @param battalion the firing unit.
     * @return the combat points.
     */
    public double calcCombatPoints(final Battalion battalion) {
        int experience = battalion.getExperience();

        // The experience factor of some battalions is increased due to their command capability.
        if (battalion.getExpIncByComm()) {
            experience++;
        }

        // The Experience Factor of all colonial troops will be decreased by 1 when fighting in Europe.
        if ((battalion.getType().canColonies()) && (getParent().getRegion().getId() == RegionConstants.EUROPE)) {
            experience--;
        }

        // Home Region bonus.
        if (battalion.getType().getHome() && getSphere(getParent().getField(), battalion.getBrigade().getNation()) == 1) {
            experience++;
        }

        // Unload penalty
        if (battalion.getUnloaded()) {
            experience--;
        }

        // Cannot go lower than 1.
        if (experience < 1) {
            experience = 1;
        }

        return (experience
                * Math.sqrt(battalion.getType().getLongCombat() * battalion.getType().getLongRange())
                * battalion.getHeadcount()
                * terrainFactor(battalion)
                * (getParent().getRandomGen().nextDouble() + 1))
                / divisionModifier();
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @param battalion to examine for the particular terrain type.
     * @return the terrain factor affecting the combat points.
     */
    public double terrainFactor(final Battalion battalion) {
        if (getParent().getFortress() == 0 || getRound() > ROUND_ARTILLERY_HLA) {
            super.terrainFactor(battalion);
        }

        return 1d;
    }

}
