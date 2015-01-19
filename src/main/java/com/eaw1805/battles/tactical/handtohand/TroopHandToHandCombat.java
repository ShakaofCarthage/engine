package com.eaw1805.battles.tactical.handtohand;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.battles.tactical.longrange.ArtilleryLongRangeCombat;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Hand-to-Hand Combat round.
 */
public class TroopHandToHandCombat
        extends ArtilleryLongRangeCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public TroopHandToHandCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_HANDCOMBAT);
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
    protected double divisionModifier() {
        return 250d;
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

    /**
     * Calculate the combat points for the battalion.
     *
     * @param battalion the firing unit.
     * @return the combat points.
     */
    @Override
    public double calcCombatPoints(final Battalion battalion) {
        int experience = battalion.getExperience();

        // Cavalry modifier
        double modifier = 1d;
        if (battalion.getType().isCavalry()) {
            modifier = 1.5d;
        }

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

        return (experience
                * modifier
                * Math.sqrt(battalion.getType().getHandCombat())
                * battalion.getHeadcount()
                * terrainFactor(battalion)
                * (getParent().getRandomGen().nextDouble() + 1))
                / divisionModifier();
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    public RoundStatistics process() {
        final int[] totPioneers = new int[2];
        final int[] totSkirmishers = new int[2];
        final double[] combatPoints = new double[2];
        final double[] lostSoldiers = new double[2];
        final double[][] statRound = new double[2][4];
        final int[] totBatts = new int[2];


        // check if each side has infantry and count the number of skirmisher units.
        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                // count the number of skirmishers
                if (battalion.getType().getFormationSk()) {
                    totSkirmishers[side]++;
                }
            }
        }

        // Calculate combat points
        for (int side = 0; side < 2; side++) {
            combatPoints[side] = 0;
            totBatts[side] = 0;
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (checkArmyType(battalion) && !battalion.isFleeing()) {
                    double thisCombatPoints = calcCombatPoints(battalion);

                    // if this is a cavalry unit, and opposing site has unmatched skirmisher, apply 200% bonus
                    if (battalion.getType().isCavalry() && totSkirmishers[(side + 1) % 2] > 0) {
                        totSkirmishers[(side + 1) % 2]--;
                        thisCombatPoints *= 3d;
                    }

                    combatPoints[side] += thisCombatPoints;
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

}
