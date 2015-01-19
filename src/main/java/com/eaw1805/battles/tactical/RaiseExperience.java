package com.eaw1805.battles.tactical;

import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.army.RankManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Commander;

/**
 * Each of the participating battalions has a chance of its EF raising.
 * Experience gained through battle cannot raise a battalion's experience
 * more than 2 higher than the maximum value given in the army table.
 */
public class RaiseExperience
        extends AbstractTacticalBattleRound
        implements VPConstants {

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
    public RaiseExperience(final TacticalBattleProcessor caller, final int winner) {
        super(caller, ROUND_AFTERMATH);
        winningSide = winner;
    }

    /**
     * Provide percentage for chances of increasing EF.
     *
     * @param side   the side under evaluation.
     * @param nation to determine militaristic bonus.
     * @return the percentage.
     */
    public final int chanceToRise(final int side, final Nation nation) {

        if (side == winningSide) {
            // Militaristic Bonus
            if (nation.getId() == NationConstants.NATION_RHINE
                    || nation.getId() == NationConstants.NATION_FRANCE
                    || nation.getId() == NationConstants.NATION_PRUSSIA) {
                return 7;
            }

            // Winner of battle
            return 5;

        } else if (winningSide == -1) {
            // Undecided
            return 2;

        } else {
            // Loser
            return 1;
        }
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    public RoundStatistics process() {
        final double[][] statRound = new double[2][4];

        // Examine each battalion and decided if they will rise in experience
        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                if (battalion.getHeadcount() > 0) {
                    final int chance = chanceToRise(side, battalion.getType().getNation());

                    if (getParent().getRandomGen().nextInt(100) <= chance) {
                        statRound[side][0]++;

                        // There is an upper bound on maximum experience that can be reached
                        if (battalion.getExperience() < battalion.getType().getMaxExp() + 2) {
                            battalion.setExperience(battalion.getExperience() + 1);
                            battalion.setParticipated(true);

                            // check if zero or negative experience reached
                            if (battalion.getExperience() < 1) {
                                battalion.setExperience(1);
                            }

                            // make sure we do not encounter an overflow
                            if (battalion.getExperience() > battalion.getType().getMaxExp() + 2) {
                                battalion.setExperience(battalion.getType().getMaxExp() + 2);
                            }

                        } else {
                            statRound[side][1]++;
                            battalion.setParticipated(false);
                        }

                    } else {
                        battalion.setParticipated(false);
                    }

                } else {
                    battalion.setParticipated(false);
                }
            }
        }


        //  The victorious commander's command capability will rise by 1 to 2 points.
        if (winningSide != RoundStatistics.SIDE_NONE) {
            final Commander comm = getParent().getSideCommanders(winningSide);
            if (comm != null) {
                final int skillGain = getParent().getRandomGen().nextInt(2) + 1;

                if (comm.getRank().getRankId() == 5
                        && comm.getComc() < comm.getRank().getMaxComC()
                        && comm.getComc() + skillGain >= comm.getRank().getMaxComC()) {

                    // Maximum level reached
                    comm.setComc(comm.getRank().getMaxComC());

                    // Give VPs
                    getParent().changeVP(comm.getPosition().getGame(), comm.getNation(), COMM_MAX_SKILL, comm.getName() + " makes it to max skill level");

                    // Add a news entry
                    getParent().news(comm.getPosition().getGame(), comm.getNation(), comm.getNation(), NEWS_MILITARY, 0, comm.getName() + " victoriously led our troops to glory for one more time. During his long lasting military service he has excelled his combat skills. He is among the most experienced generals of the known world!");

                } else if (comm.getRank().getRankId() < 5 && comm.getComc() >= comm.getRank().getMaxComC()) {
                    // proceed to next rank
                    comm.setRank(RankManager.getInstance().getByID(comm.getRank().getRankId() + 1));
                    comm.setStrc(comm.getRank().getStrC());
                    comm.setComc(comm.getComc() + skillGain);

                    // Add a news entry
                    getParent().news(comm.getPosition().getGame(), comm.getNation(), comm.getNation(), NEWS_MILITARY, 0, comm.getName() + " victoriously led our troops to glory. He has been promoted to the rank of " + comm.getRank().getName());

                } else {
                    comm.setComc(comm.getComc() + skillGain);

                    // Add a news entry
                    getParent().news(comm.getPosition().getGame(), comm.getNation(), comm.getNation(), NEWS_MILITARY, 0, comm.getName() + " victoriously led our troops to glory. The battle experience gave him important insights on how to lead his troops to combat. His combat skill was increased by " + skillGain + " points.");
                }
            }
        }

        return new RoundStatistics(getRound(), getParent().getSideBattalions(), statRound);
    }

}
