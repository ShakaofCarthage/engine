package com.eaw1805.battles.naval;

import com.eaw1805.battles.naval.result.RoundStat;
import com.eaw1805.data.dto.converters.ShipConverter;
import com.eaw1805.data.dto.web.fleet.ShipDTO;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.model.fleet.Ship;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Capturing of merchant ships.
 * All warships that are faster than merchant ships
 * (i.e. warships of the classes 1, 2 or 3) will try to capture enemy merchant ships.
 */
public class CaptureMerchantShips
        extends AbstractNavalBattleRound {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CaptureMerchantShips.class);

    /**
     * The statistics for each side at the end of the battle.
     */
    private final transient RoundStat finalStats;

    /**
     * Default constructor.
     *
     * @param caller   the processor requesting the execution of this round.
     * @param statsEnd the statistics at the end of the battle.
     */
    public CaptureMerchantShips(final NavalBattleProcessor caller,
                                final RoundStat statsEnd) {
        super(caller);
        setRound(ROUND_CAP);
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
        final List<Ship> lstShips[] = getParent().getSideShips();
        final List<ShipDTO> capShips[] = new ArrayList[2];

        switch (finalStats.getResult()) {
            case RoundStat.SIDE_A:
                // Side A won -- try to capture enemy merchant ships
                capShips[0] = ShipConverter.convert(captureMerchantShips(0), GoodManager.getInstance());
                capShips[1] = new ArrayList<ShipDTO>();
                break;

            case RoundStat.SIDE_B:
                // Side B won -- try to capture enemy merchant ships
                capShips[0] = new ArrayList<ShipDTO>();
                capShips[1] = ShipConverter.convert(captureMerchantShips(1), GoodManager.getInstance());
                break;

            case RoundStat.SIDE_NONE:
            default:
                // No one won -- stop here
                capShips[0] = new ArrayList<ShipDTO>();
                capShips[1] = new ArrayList<ShipDTO>();
                break;
        }

        // Produce statistics
        final RoundStat stat = new RoundStat(getRound(), lstShips);
        stat.setResult(finalStats.getResult());
        stat.setCapturedShips(capShips);
        return stat;
    }

    /**
     * Capture merchant ships.
     *
     * @param side the Side that is chasing.
     * @return list of ships that were captured.
     */
    private List<Ship> captureMerchantShips(final int side) {
        final List<Ship> capShips = new ArrayList<Ship>();

        // Calculate number of ships that can chase the enemy
        final int totLowClassShips = calcLowClassShips(side);
        if (totLowClassShips > 0) {

            // Calculate number of enemy merchant ships
            final int totHighClassShips = calcHighClassShips((side + 1) % 2);
            final List<Ship> merShips = calcMerchantShips((side + 1) % 2);
            final int totMerShips = merShips.size() - totHighClassShips * 2;

            // Check if any merchant ship can be captured
            if (totMerShips > 0) {
                // Capture merchant ships
                for (int pick = 0; pick < Math.min(totLowClassShips, totMerShips); pick++) {
                    // pick a random merchant ship
                    final int roll = getParent().getRandomGen().nextInt(merShips.size());

                    // select ship
                    final Ship capturedShip = merShips.get(roll);

                    LOGGER.debug("Merchant ship " + capturedShip.getName() + " [" + capturedShip.getShipId() + "] was captured");

                    // remove from list
                    merShips.remove(roll);

                    // apply damages
                    final int damage = getParent().getRandomGen().nextInt(10) + 10;
                    capturedShip.setCondition(capturedShip.getCondition() - damage);
                    capturedShip.setCapturedByNation(getParent().getSideShips()[side].get(0).getNation().getId());

                    // add to list
                    capturedShip.setCapturedByNation(capturedShip.getCapturedByNation() * -1);
                    capShips.add(capturedShip);
                }
            }
        }

        // Add Captured ships to winner's fleet
        if (!capShips.isEmpty()) {
            LOGGER.debug("Total of " + Integer.toString(capShips.size()) + " merchant ships were captured");

            final List<Ship> lstShips[] = getParent().getSideShips();
            for (final Ship capShip : capShips) {
                lstShips[side].add(capShip);
                lstShips[(side + 1) % 2].remove(capShip);
            }
            getParent().setSideShips(lstShips);
        }

        return capShips;
    }

    /**
     * Calculate total number of class 1, 2, 3 ships with more than 50% condition.
     * Each class 1 has a 50% chance of capturing an enemy merchant ship.
     * Each class 2 ship has a 25% chance of capturing an enemy merchant ship.
     * Each class 3 ship has a 5% chance of capturing an enemy merchant ship.
     *
     * @param side the side to check.
     * @return total number of class 1, 2, 3 ships.
     */
    private int calcLowClassShips(final int side) {
        int totLowClassShips = 0;
        for (final Ship ship : getParent().getSideShips()[side]) {
            if ((ship.getType().getShipClass() > 0)
                    && (ship.getType().getShipClass() <= 3)
                    && (ship.getCondition() > 50)
                    && (ship.getMarines() > 0)
                    && (ship.getCapturedByNation() == 0)) {

                final int roll = getParent().getRandomGen().nextInt(100) + 1;
                int target;
                switch (ship.getType().getShipClass()) {
                    case 1:
                        target = 65;
                        break;

                    case 2:
                        target = 45;
                        break;

                    case 3:
                    default:
                        target = 25;
                        break;
                }

                if (roll <= target) {
                    totLowClassShips++;
                }
            }
        }
        return totLowClassShips;
    }

    /**
     * Calculate total number of class 4, 5 ships.
     *
     * @param side the side to check.
     * @return total number of class 4, 5 ships.
     */
    private int calcHighClassShips(final int side) {
        int totHighClassShips = 0;
        for (final Ship ship : getParent().getSideShips()[side]) {
            if ((ship.getType().getShipClass() >= 4)
                    && (ship.getCondition() > 0)
                    && (ship.getMarines() > 0)
                    && (ship.getCapturedByNation() == 0)) {
                totHighClassShips++;
            }
        }
        return totHighClassShips;
    }

    /**
     * Calculate total number of merchant ships.
     *
     * @param side the side to check.
     * @return total number of merchant ships.
     */
    private List<Ship> calcMerchantShips(final int side) {
        final List<Ship> shipList = new ArrayList<Ship>();
        for (final Ship ship : getParent().getSideShips()[side]) {
            if ((ship.getType().getShipClass() == 0)
                    && (ship.getCondition() > 0)
                    && (ship.getMarines() > 0)
                    && (ship.getCapturedByNation() == 0)) {
                shipList.add(ship);
            }
        }
        return shipList;
    }
}
