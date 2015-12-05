package com.eaw1805.battles.naval;

import com.eaw1805.battles.naval.result.RoundStat;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.dto.converters.ShipConverter;
import com.eaw1805.data.dto.web.fleet.ShipDTO;
import com.eaw1805.data.model.fleet.Ship;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Determine who won the naval battle.
 */
public class DetermineWinner
        extends AbstractNavalBattleRound {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DetermineWinner.class);

    /**
     * The statistics for each side at the start of the battle.
     */
    private final transient RoundStat initStats;

    /**
     * The statistics for each side at the end of the battle.
     */
    private final transient RoundStat finalStats;

    /**
     * Default constructor.
     *
     * @param caller    the processor requesting the execution of this round.
     * @param statsInit the statistics at the start of the battle.
     * @param statsEnd  the statistics at the end of the battle.
     */
    public DetermineWinner(final NavalBattleProcessor caller,
                           final RoundStat statsInit,
                           final RoundStat statsEnd) {
        super(caller);
        initStats = statsInit;
        finalStats = statsEnd;
    }

    /**
     * Execute the round of the naval battle.
     *
     * @return the result of the round.
     */
    @Override
    @SuppressWarnings("unchecked")
    public RoundStat process() {
        final RoundStat stat = new RoundStat(ROUND_DET, finalStats.getSideShips(), finalStats.getSideStat());
        final double[] tonnageInit = new double[2];
        final double[] marinesInit = new double[2];
        final double[] tonnageFinal = new double[2];
        final double[] marinesFinal = new double[2];
        final double[] losses = new double[2];

        // Calculate initial tonnageInit and marinesInit of each side
        for (int side = 0; side < 2; side++) {
            for (int sc = 1; sc < 6; sc++) {
                tonnageInit[side] += initStats.getSideStat(side).get(sc).getTonnage();
                marinesInit[side] += initStats.getSideStat(side).get(sc).getMarines();
                tonnageFinal[side] += finalStats.getSideStat(side).get(sc).getTonnage();
                marinesFinal[side] += finalStats.getSideStat(side).get(sc).getMarines();
            }
            final double tonLosses = (1d - tonnageFinal[side] / tonnageInit[side]) * 100d;
            final double marLosses = (1d - marinesFinal[side] / marinesInit[side]) * 100d;
            losses[side] = (tonLosses + marLosses) / 2d;
        }

        final List<Ship> lstShips[] = getParent().getSideShips();
        final int target[] = new int[3];
        if (losses[1] - losses[0] > 20d || (tonnageInit[0] > 0 && tonnageInit[1] == 0)) {
            // Side 1 has won
            LOGGER.debug("1st side won the naval battle");
            stat.setResult(RoundStat.SIDE_A);
            target[RoundStat.SIDE_A] = 6;
            target[RoundStat.SIDE_B] = 1;

        } else if (losses[0] - losses[1] > 20d || (tonnageInit[0] == 0 && tonnageInit[1] > 0)) {
            // Side 2 has won
            LOGGER.debug("2nd side won the naval battle");
            stat.setResult(RoundStat.SIDE_B);
            target[RoundStat.SIDE_A] = 1;
            target[RoundStat.SIDE_B] = 6;

        } else {
            // Undecided
            LOGGER.debug("Naval Battle was undecided");
            stat.setResult(RoundStat.SIDE_NONE);
            target[RoundStat.SIDE_A] = 3;
            target[RoundStat.SIDE_B] = 3;
        }

        // access ships
        final List<ShipDTO> raisedExp[] = new ArrayList[2];

        // Check if marines gain experience
        for (int side = 0; side < 2; side++) {
            raisedExp[side] = raiseExperience(lstShips[side], target[side + 1]);
        }

        // Keep track of ships that were upgraded
        stat.setCapturedShips(raisedExp);

        return stat;
    }

    private List<ShipDTO> raiseExperience(final List<Ship> lstShip, final int targetRoll) {
        final List<ShipDTO> raisedExp = new ArrayList<ShipDTO>();
        for (final Ship ship : lstShip) {
            if ((ship.getCondition() > 0)
                    && (ship.getMarines() > 0)) {
                final int roll = getParent().getRandomGen().nextInt(101) + 1;
                int thisTarget = targetRoll;

                // Maritime Bonus
                if (thisTarget == 6
                        && (ship.getNation().getId() == NationConstants.NATION_GREATBRITAIN
                        || ship.getNation().getId() == NationConstants.NATION_PORTUGAL)) {
                    thisTarget += 2;
                }

                if ((roll <= thisTarget) && (ship.getExp() < 2)) {
                    ship.setExp(ship.getExp() + 1);
                    raisedExp.add(ShipConverter.convert(ship, null));
                }
            }
        }

        return raisedExp;
    }
}
