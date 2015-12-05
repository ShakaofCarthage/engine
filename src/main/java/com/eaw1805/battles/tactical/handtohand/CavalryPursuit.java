package com.eaw1805.battles.tactical.handtohand;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Battalion;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements Round 13: Pursuit.
 */
public class CavalryPursuit
        extends TroopHandToHandCombat {

    /**
     * the side that won the battle or -1 if undecided.
     */
    private final transient int winningSide;

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     * @param winner the side that won the battle or -1 if undecided.
     */
    public CavalryPursuit(final TacticalBattleProcessor caller, final int winner) {
        super(caller);
        super.setRound(ROUND_PURSUIT);
        winningSide = winner;
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
     * The modified for dividing the combat points.
     *
     * @return modified for dividing the combat points.
     */
    @Override
    protected double divisionModifier() {
        return 100d;
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    @Override
    public RoundStatistics process() {
        final double[] combatPoints = new double[2];
        final double[] lostSoldiers = new double[2];
        final double[][] statRound = new double[2][4];
        final Map<Nation, Integer> losses = new HashMap<Nation, Integer>();
        final int[] totBatts = new int[2];

        // check if each side has infantry and count the number of infantry units.
        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                // count battalions with at least 1 soldier
                if (battalion.getHeadcount() > 0) {
                    totBatts[side]++;
                }
            }
        }

        // Calculate combat points
        for (int side = 0; side < 2; side++) {
            combatPoints[side] = 0;
            if (side == winningSide) {
                for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                    if (checkArmyType(battalion) && !battalion.isFleeing()) {
                        double factor = 1d;
                        if (battalion.getType().getTroopSpecsLc()) {
                            // Light Cavalry inflict double pursuit casualties.
                            factor = 2d;

                        } else if (battalion.getType().getTroopSpecsCu()) {
                            // Cuirassier inflict half losses when pursuing.
                            factor = .5d;
                        }
                        combatPoints[side] += factor * calcCombatPoints(battalion);

                        battalion.setParticipated(true);

                    } else {
                        battalion.setParticipated(false);
                    }
                }

            } else {
                // losing side does not participate
                for (final Battalion battalion : getParent().getSideBattalions()[side]) {
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
                // apply losses
                int nationLoses = (int) (lostSoldiers[side] * fortBonus);

                // make sure we do not end up with negative headcount
                if (nationLoses > battalion.getHeadcount()) {
                    nationLoses = battalion.getHeadcount();
                }

                // update headcount and statistics
                battalion.setHeadcount(battalion.getHeadcount() - nationLoses);
                statRound[side][1] += nationLoses;

                // Store nation losses
                if (losses.containsKey(battalion.getBrigade().getNation())) {
                    nationLoses += losses.get(battalion.getBrigade().getNation());
                }

                losses.put(battalion.getBrigade().getNation(), nationLoses);
            }
        }

        // Distributed casualties as prisoners of war
        if (thisGame.getGameId() > 0) {
            for (int side = 0; side < 2; side++) {
                final int thisSideSize = getParent().getSideNation(side).size();
                final int otherSide = (side + 1) % 2;
                if (lostSoldiers[otherSide] > 0)
                    for (final Nation targetOwner : getParent().getSideNation(otherSide)) {
                        final int powRate = getParent().getRandomGen().nextInt(26) + 75;
                        final int totalPOW = losses.get(targetOwner) * powRate / 100;
                        final int powShare = totalPOW / thisSideSize;

                        // Set prisoners of war towards 1st side targetOwner
                        for (Nation thisOwner : getParent().getSideNation(side)) {
                            // Store new prisoners
                            final NationsRelation thisRel = RelationsManager.getInstance().getByNations(thisGame, thisOwner, targetOwner);
                            int existingPOW = thisRel.getPrisoners();
                            existingPOW += powShare;
                            thisRel.setPrisoners(existingPOW);
                            RelationsManager.getInstance().update(thisRel);

                            // Also keep track of prisoners in player's profile
                            getParent().changeProfile(thisGame, thisOwner, ProfileConstants.ENEMY_PRISONERS, powShare);
                        }
                    }
            }
        }

        return new RoundStatistics(getRound(), getParent().getSideBattalions(), statRound);
    }

}
