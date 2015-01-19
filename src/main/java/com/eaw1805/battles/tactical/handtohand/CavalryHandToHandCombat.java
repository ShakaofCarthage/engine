package com.eaw1805.battles.tactical.handtohand;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.Battalion;

/**
 * Executes the Hand-to-Hand Combat round.
 */
public class CavalryHandToHandCombat
        extends TroopHandToHandCombat {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public CavalryHandToHandCombat(final TacticalBattleProcessor caller) {
        super(caller);
        super.setRound(ROUND_HANDCOMBAT_CAV);
    }

    /**
     * Examine battalion and determine if it should participate in this round.
     *
     * @param battalion the battalion to examine.
     * @return true if battalion will participate in this round.
     */
    @Override
    protected boolean checkArmyType(final Battalion battalion) {
        return (battalion.getType().isCavalry());
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    @Override
    public RoundStatistics process() {
        final int[] totCav = new int[2];
        final int[] totPioneers = new int[2];
        final int[] totSkirmishers = new int[2];
        final boolean[] hasInf = new boolean[2];
        final double[] combatPoints = new double[2];
        final double[] lostSoldiers = new double[2];
        final double[][] statRound = new double[2][4];
        final int[] totBatts = new int[2];

        // check if each side has infantry and count the number of pioneer units.
        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (battalion.getType().isInfantry()) {
                    hasInf[side] = true;

                    if (battalion.getType().isEngineer()) {
                        totPioneers[side]++;
                    }

                    // count the number of skirmishers
                    if (battalion.getType().getFormationSk()) {
                        totSkirmishers[side]++;
                    }
                }

                // count battalions with at least 1 soldier
                if (battalion.getHeadcount() > 0) {
                    totBatts[side]++;
                }
            }
        }

        // Calculate combat points
        for (int side = 0; side < 2; side++) {
            combatPoints[side] = 0;
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (checkArmyType(battalion) && !battalion.isFleeing()) {
                    double thisCombatPoints = calcCombatPoints(battalion);

                    battalion.setParticipated(true);

                    double factor = 1d;
                    if (hasInf[(side + 1) % 2] && battalion.getType().getTroopSpecsLr()) {
                        // Lancers Cavalry (Lr) inflict double casualties during the cavalry's
                        // hand-to-hand combat round, if the enemy has any infantry units in battle.
                        factor = 2d;
                    }

                    // if this is a cavalry unit, and opposing site has unmatched skirmisher, apply 200% bonus
                    if (battalion.getType().isCavalry() && totSkirmishers[(side + 1) % 2] > 0) {
                        totSkirmishers[(side + 1) % 2]--;
                        thisCombatPoints *= 3d;
                    }

                    combatPoints[side] += factor * thisCombatPoints;
                    totCav[side]++;

                } else {
                    battalion.setParticipated(false);
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

        final Game thisGame = getParent().getSideBattalions()[0].get(0).getBrigade().getPosition().getGame();

        // Custom Games: Fierce Battles (+25%)
        final double casModifier;
        if (thisGame.isFierceCasualties()) {
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

            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                double thisCasualties = lostSoldiers[side];

                // For every pioneer battalion participating in combat,
                // another infantry or artillery battalion (chosen randomly)
                // will get a 20% decrease of its losses during the fire and melee rounds.
                if ((battalion.getType().isInfantry() || battalion.getType().isArtillery()) && (totPioneers[side] > 0)) {
                    thisCasualties *= .8d;
                    totPioneers[side]--;
                }

                // Apply casualties
                thisCasualties *= fortBonus;

                // make sure we do not end up with negative headcount
                if (thisCasualties > battalion.getHeadcount()) {
                    thisCasualties = battalion.getHeadcount();
                }

                // update headcount and statistics
                battalion.setHeadcount(battalion.getHeadcount() - (int) thisCasualties);
                statRound[side][1] += (int) thisCasualties;

                // In the fourth morale check at round 10, a number of infantry battalions equal to the
                // number of cavalry units the enemy has, receive a -10 penalty.
                if ((battalion.getType().isInfantry() || battalion.getType().isArtillery()) && (totCav[(side + 1) % 2] > 0)) {
                    battalion.setAttackedByCav(true);
                    statRound[side][2]++;
                }

                totCav[(side + 1) % 2]--;
            }
        }

        return new RoundStatistics(getRound(), getParent().getSideBattalions(), statRound);
    }

}
