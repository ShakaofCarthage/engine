package com.eaw1805.battles.tactical.morale;

import com.eaw1805.battles.tactical.AbstractTacticalBattleRound;
import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.model.army.Battalion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Performs the morale check in the battalions and marks the ones that
 * failed the check.
 */
public class MoraleCheckAttackedByCavalry
        extends AbstractTacticalBattleRound
        implements NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(MoraleCheckAttackedByCavalry.class);

    private final Map<Integer, RoundStatistics> statMap;

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public MoraleCheckAttackedByCavalry(final TacticalBattleProcessor caller, final Map<Integer, RoundStatistics> map) {
        super(caller, ROUND_MORALE_4);
        statMap = map;
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    @SuppressWarnings("unchecked")
    public RoundStatistics process() {
        final double[][] statRound = new double[2][4];

        // Retrieve initial headcount
        int[] headcount = new int[2];
        int[] totBattalions = new int[2];
        for (int side = 0; side < 2; side++) {
            headcount[side] += statMap.get(AbstractTacticalBattleRound.ROUND_INIT).getArmySizes()[side][TacticalBattleProcessor.TROOP_TOT][TacticalBattleProcessor.TPE_BATTSIZE];
            totBattalions[side] = statMap.get(AbstractTacticalBattleRound.ROUND_INIT).getSideBattalions()[side].size();
        }

        final int[] rounds = {AbstractTacticalBattleRound.ROUND_HANDCOMBAT_CAV};

        // Calculate total casualties
        int[] casualties = new int[2];
        for (final int round : rounds) {
            for (int side = 0; side < 2; side++) {
                if (statMap.containsKey(round)) {
                    casualties[side] += (int) statMap.get(round).getSideStat()[side][1];
                }
            }
        }

        // Compute total headcount for each side (exclude fleeing troops)
        int[] totTroops = new int[2];
        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (!battalion.isFleeing()) {
                    totTroops[side] += battalion.getHeadcount();
                }
            }
        }

        // Determine which side gains bonus due to numerical odds
        int[] bonus = new int[2];
        if (totTroops[0] > 3 * totTroops[1]) {
            bonus[0] = 5;
            bonus[1] = -5;

        } else if (totTroops[0] > 2 * totTroops[1]) {
            bonus[0] = 5;
            bonus[1] = 0;

        } else if (totTroops[1] > 3 * totTroops[0]) {
            bonus[0] = -5;
            bonus[1] = 5;

        } else if (totTroops[1] > 2 * totTroops[0]) {
            bonus[0] = 0;
            bonus[1] = 5;
        }

        // Make morale check
        for (int side = 0; side < 2; side++) {
            // 5% bonus for rally fleeing troops if side has a commander
            int commanderBonus = 0;
            if (getParent().getSideCommanders(side) != null
                    && !getParent().getSideCommanders(side).getDead()
                    && !getParent().getSideCommanders(side).getInTransit()) {
                commanderBonus = 5;
            }

            // Identify fleeing battalions and check if they will regain morale
            for (Battalion battalion : getParent().getSideBattalions()[side]) {
                if (battalion.isFleeing()) {
                    // keep track of statistics
                    battalion.setParticipated(true);
                    statRound[side][2]++;

                    int experience = battalion.getExperience();

                    // The Experience Factor of all colonial troops will be decreased by 1 when fighting in Europe.
                    if ((battalion.getType().canColonies()) && (getParent().getRegion().getId() == RegionConstants.EUROPE)) {
                        experience--;
                    }

                    // Home Region bonus.
                    if (battalion.getType().getHome() && getSphere(getParent().getField(), battalion.getBrigade().getNation()) == 1) {
                        experience++;
                    }

                    final int roll = getParent().getRandomGen().nextInt(100);

                    // 5% bonus if in home coordinate
                    int homeBonus = 0;
                    if (battalion.getBrigade().getNation().getId() == getParent().getField().getNation().getId()) {
                        homeBonus = 5;
                    }

                    // Try to rally
                    // Fleeing battalions have a 10% penalty.
                    if (roll < 50 + (experience * 5) - 10 + bonus[side] + homeBonus + commanderBonus) {
                        battalion.setFleeing(false);
                        statRound[side][1]++;
                    }

                } else {
                    battalion.setParticipated(false);
                }
            }

            LOGGER.debug("[" + side + "] init hc=" + headcount[side] + ", cas=" + casualties[side] + ", bonus=" + bonus[side]);

            // Make morale check
            int attackedByCav = 0;
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (battalion.getHeadcount() <= 0) {
                    battalion.setAttackedByCav(false);
                    battalion.setParticipated(false);
                    battalion.setFleeing(false);

                } else if (battalion.isAttackedByCav() && !battalion.isFleeing()) {
                    attackedByCav++;
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

                    // Fleeing battalions have an extra 10% penalty
                    double penalty = 10d;
                    if (battalion.isFleeing()) {
                        penalty += 10d;
                    }

                    if (!battalion.getType().getFormationSq()) {
                        penalty += 20d;
                        statRound[side][3]++;
                    }

                    final int roll = getParent().getRandomGen().nextInt(100);

                    // check if they will flee
                    if (roll >= 50 + (experience * 5) - penalty) {
                        battalion.setFleeing(true);
                        battalion.setParticipated(true);
                        statRound[side][0]++;
                    }
                }
            }

            LOGGER.debug("[" + side + "] rout units=" + statRound[side][2] + ", recovered=" + statRound[side][1] + ", attacked by cav=" + attackedByCav + ", newly rout=" + statRound[side][0]);
        }

        return new RoundStatistics(getRound(), getParent().getSideBattalions(), statRound);
    }

}
