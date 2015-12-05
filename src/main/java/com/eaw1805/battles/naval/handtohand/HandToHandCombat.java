package com.eaw1805.battles.naval.handtohand;

import com.eaw1805.battles.naval.AbstractNavalBattleRound;
import com.eaw1805.battles.naval.NavalBattleProcessor;
import com.eaw1805.battles.naval.result.RoundStat;
import com.eaw1805.battles.naval.result.ShipPair;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.model.fleet.Ship;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executes a hand-to-hand combat round of naval battles.
 */
public class HandToHandCombat
        extends AbstractNavalBattleRound {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(HandToHandCombat.class);

    /**
     * Pair of ships for hand-to-hand combat.
     */
    private final transient Set<ShipPair> lstShipPairs;

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     * @param round  the round of the battle.
     */
    public HandToHandCombat(final NavalBattleProcessor caller,
                            final int round) {
        super(caller);
        setRound(round);
        lstShipPairs = caller.getShipPairs();
    }

    /**
     * Check weather to determine if this round will take place.
     *
     * @return true if the weather is CLEAR or RAINY.
     */
    public boolean checkWeather() {
        return (getParent().getWeather() != NavalBattleProcessor.WEATHER_STORM);
    }

    /**
     * Execute the round of the naval battle.
     *
     * @return the result of the round.
     */
    @Override
    public RoundStat process() {
        final List<Ship> lstShips[] = getParent().getSideShips();

        // Keep track of ships that have been boarded by 1 or 2 ships.
        final Set<Integer> boarded1 = new HashSet<Integer>();
        final Set<Integer> boarded2 = new HashSet<Integer>();

        // calculated boarded sets for each side separately
        for (int side = 0; side < 2; side++) {
            // iterate through each ship pair
            for (final ShipPair shipPair : lstShipPairs) {
                // check 1st ship
                if (boarded1.contains(shipPair.getShipA().getShipId())) {
                    boarded2.add(shipPair.getShipA().getShipId());

                } else {
                    boarded1.add(shipPair.getShipA().getShipId());
                }

                // check 2nd ship
                if (boarded1.contains(shipPair.getShipB().getShipId())) {
                    boarded2.add(shipPair.getShipB().getShipId());

                } else {
                    boarded1.add(shipPair.getShipB().getShipId());
                }
            }
        }

        // This round will take place only at clear weather conditions.
        if (checkWeather()) {

            // pick targets for each side separately
            for (int side = 0; side < 2; side++) {

                // iterate through each ship
                for (final Ship ship : lstShips[side]) {
                    final double marineCondition = 100d * ship.getMarines() / ship.getType().getCitizens();

                    // ships with a condition of 60% or more will choose two possible target ships
                    if ((ship.getType().getShipClass() > 0)
                            && (ship.getCondition() >= 60)
                            && (marineCondition >= 60)
                            && (ship.getCapturedByNation() == 0)) {

                        // Check if ship is already boarded (or has already boarded)
                        if ((boarded1.contains(ship.getShipId()))
                                && (boarded2.contains(ship.getShipId()))) {
                            continue;
                        }

                        int morale = ship.getNation().getMorale() + ship.getExp();

                        // Lack of Wine effect in Naval Battle
                        if (ship.getNoWine()) {
                            morale--;
                        }

                        final int roll = getParent().getRandomGen().nextInt(101) + 1;
                        if (roll <= 30 + (5 * morale)) {

                            // find target
                            final ShipPair spair = findTargetShip(ship, (side + 1) % 2, lstShipPairs, boarded1, boarded2);

                            // check that we did manage to find a target
                            if ((spair != null) && (!lstShipPairs.contains(spair))) {
                                // make sure that this pair is not already added
                                lstShipPairs.add(spair);
                            }
                        }
                    }
                }
            }
        }

        // hand-to-hand combats have 3 rounds
        for (int round = 1; round <= 3; round++) {

            // calculate hand-to-hand combat round for each pair
            for (final ShipPair shipPair : lstShipPairs) {
                if ((shipPair.getShipA().getCapturedByNation() == 0)
                        && (shipPair.getShipB().getCapturedByNation() == 0)) {
                    calculateCombatPoints(round, shipPair);
                }
            }

            // apply hand-to-hand combat round for each pair
            for (final ShipPair shipPair : lstShipPairs) {
                if ((shipPair.getShipA().getCapturedByNation() == 0)
                        && (shipPair.getShipB().getCapturedByNation() == 0)) {
                    applyLosses(round, shipPair);
                }
            }

            // check if either side will take hold of the enemy ship
            for (final ShipPair shipPair : lstShipPairs) {
                // Ships already captured are not considered here
                if ((shipPair.getShipA().getCapturedByNation() == 0)
                        && (shipPair.getShipB().getCapturedByNation() == 0)) {

                    if (shipPair.getShipA().getMarines() > 3 * shipPair.getShipB().getMarines()) {
                        // one side has more than three times the number of marines compared to its enemy
                        shipPair.getShipB().setCapturedByNation(shipPair.getShipA().getNation().getId());
                        shipPair.setCaptured(1, round, true);

                        // split marines in half between two ships
                        int totMarines = shipPair.getShipA().getMarines() + shipPair.getShipB().getMarines();
                        shipPair.getShipA().setMarines(totMarines / 2);
                        shipPair.getShipB().setMarines(totMarines / 2);

                        // Make sure that the experiences of the marines is carried over properly
                        shipPair.getShipB().setExp(shipPair.getShipA().getExp());

                        // Hand over to other side
                        final Ship capturedShip = shipPair.getShipB();
                        capturedShip.setCapturedByNation(shipPair.getShipB().getCapturedByNation() * -1);
                        lstShips[0].add(capturedShip);
                        lstShips[1].remove(capturedShip);

                        LOGGER.debug("Side B ship " + shipPair.getShipB().getName() + " [" + shipPair.getShipB().getShipId() + "] was captured by side A");
                        switch (capturedShip.getType().getShipClass()) {
                            case 5:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_SUNK_5, 1);
                                break;

                            case 4:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_SUNK_4, 1);
                                break;

                            case 3:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_SUNK_3, 1);
                                break;

                            case 2:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_SUNK_2, 1);
                                break;

                            case 1:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_SUNK_1, 1);
                                break;

                            case 0:
                            default:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_SUNK_0, 1);
                                break;
                        }


                    } else if (shipPair.getShipB().getMarines() > 3 * shipPair.getShipA().getMarines()) {
                        // one side has more than three times the number of marines compared to its enemy
                        shipPair.getShipA().setCapturedByNation(shipPair.getShipB().getNation().getId());

                        shipPair.setCaptured(0, round, true);

                        // split marines in half between two ships
                        int totMarines = shipPair.getShipA().getMarines() + shipPair.getShipB().getMarines();
                        shipPair.getShipA().setMarines(totMarines / 2);
                        shipPair.getShipB().setMarines(totMarines / 2);

                        // Make sure that the experiences of the marines is carried over properly
                        shipPair.getShipA().setExp(shipPair.getShipB().getExp());

                        // Hand over to other side
                        final Ship capturedShip = shipPair.getShipA();
                        capturedShip.setCapturedByNation(shipPair.getShipA().getCapturedByNation() * -1);
                        lstShips[1].add(capturedShip);
                        lstShips[0].remove(capturedShip);

                        LOGGER.debug("Side A ship " + shipPair.getShipB().getName() + " [" + shipPair.getShipB().getShipId() + "] was captured by side B");
                        switch (capturedShip.getType().getShipClass()) {
                            case 5:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_SUNK_5, 1);
                                break;

                            case 4:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_SUNK_4, 1);
                                break;

                            case 3:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_SUNK_3, 1);
                                break;

                            case 2:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_SUNK_2, 1);
                                break;

                            case 1:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_SUNK_1, 1);
                                break;

                            case 0:
                            default:
                                // keep track of ships sunk
                                changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_SUNK_0, 1);
                                break;
                        }

                    }
                }
            }
        }

        // persist changes to ship lists
        getParent().setSideShips(lstShips);

        // Produce statistics
        final RoundStat stat = new RoundStat(getRound(), lstShips);
        stat.setSidePairs(lstShipPairs);
        LOGGER.debug(lstShipPairs.size() + " pair of ships participated in hand-to-hand combat");

        // Removed pairs with captured ships so that they do not appear in following h2h round
        final Set<ShipPair> updatedList = new HashSet<ShipPair>();
        for (final ShipPair shipPair : lstShipPairs) {
            if (shipPair.getShipA().getCapturedByNation() == 0
                    && shipPair.getShipA().getCondition() > 0
                    && shipPair.getShipB().getCapturedByNation() == 0
                    && shipPair.getShipB().getCondition() > 0) {

                // there is a 25% chance of the ships separating
                final int roll = getParent().getRandomGen().nextInt(101) + 1;
                if (roll > 25) {
                    // keep them for next round
                    final ShipPair newSP = new ShipPair(shipPair.getShipA(), shipPair.getShipB());
                    updatedList.add(newSP);
                }
            }
        }
        getParent().setShipPairs(updatedList);

        return stat;
    }

    /**
     * apply hand-to-hand combat round losses for each pair.
     *
     * @param round    the round of the hand-to-hand combat.
     * @param shipPair the ship pair.
     */
    private void applyLosses(final int round, final ShipPair shipPair) {
        int losesA = shipPair.getLossMarines(0, round);
        shipPair.getShipA().calcCondition(shipPair.getLossTonnage(0));
        shipPair.getShipA().setMarines(shipPair.getShipA().getMarines() - losesA);

        // Make sure we do not end up with negative marines
        if (shipPair.getShipA().getMarines() < 0) {
            losesA -= shipPair.getShipA().getMarines();
            shipPair.getShipA().setMarines(0);
        }

        // Also keep track of marines killed in player's profile
        changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_KILLED_MAR, losesA);

        // Side B ship
        int losesB = shipPair.getLossMarines(1, round);
        shipPair.getShipB().calcCondition(shipPair.getLossTonnage(1));
        shipPair.getShipB().setMarines(shipPair.getShipB().getMarines() - shipPair.getLossMarines(1, round));

        // Make sure we do not end up with negative marines
        if (shipPair.getShipB().getMarines() < 0) {
            losesB -= shipPair.getShipB().getMarines();
            shipPair.getShipB().setMarines(0);
        }

        // Also keep track of marines killed in player's profile
        changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_KILLED_MAR, losesB);
    }

    /**
     * calculate hand-to-hand combat round for each pair.
     *
     * @param round    the round of the hand-to-hand combat.
     * @param shipPair the ship pair.
     */
    private void calculateCombatPoints(final int round, final ShipPair shipPair) {
        // calculate side A
        final Ship sideA = shipPair.getShipA();
        final double cpA = sideA.getMarines()
                * (getParent().getRandomGen().nextInt(11) + 10) / 10d
                / 8d;

        shipPair.setCombatPoints(0, round, (int) cpA);

        // calculate side B
        final Ship sideB = shipPair.getShipB();
        final double cpB = sideB.getMarines()
                * (getParent().getRandomGen().nextInt(11) + 10) / 10d
                / 8d;

        shipPair.setCombatPoints(1, round, (int) cpB);

        // calculate losses
        int marinesLostA = (int) cpA;
        if (marinesLostA > sideB.getMarines()) {
            marinesLostA = sideB.getMarines();
        }
        shipPair.setLossMarines(1, round, marinesLostA);

        int marinesLostB = (int) cpB;
        if (marinesLostB > sideA.getMarines()) {
            marinesLostB = sideA.getMarines();
        }
        shipPair.setLossMarines(0, round, marinesLostB);
    }

    /**
     * Find an enemy ship to fire upon.
     *
     * @param thisShip  the ship about to fire.
     * @param enemySide the side of the enemy fleet.
     * @param lstSP     the list of ship pairs selected during the course of the naval battle.
     * @param boarded1  the ships that have been boarded by an enemy ship.
     * @param boarded2  the ships that have been boarded by two enemy ships.
     * @return the target ship.
     */
    protected final ShipPair findTargetShip(final Ship thisShip,
                                            final int enemySide,
                                            final Set<ShipPair> lstSP,
                                            final Set<Integer> boarded1,
                                            final Set<Integer> boarded2) {
        // matched pair
        ShipPair existingShipPair = null;

        // Check if ship is already boarded --
        if (boarded1.contains(thisShip.getShipId())) {

            // locate pair
            for (final ShipPair shipPair : lstSP) {
                if ((shipPair.getShipA().getShipId() == thisShip.getShipId())
                        || (shipPair.getShipB().getShipId() == thisShip.getShipId())) {
                    return null;
                }
            }
        }

        // the target Ship
        Ship targetShip = null;

        // Try to find 1st targetShip of same class that is not board yet
        final Ship targetA1 = findTargetShipSameClassNotBoarded(thisShip, enemySide, boarded1);

        // Try to find 1st targetShip of same class
        final Ship targetA2 = findTargetShipSameClass(thisShip, enemySide);

        final int roll = getParent().getRandomGen().nextInt(101) + 1;

        // Check if 1st targetShip will be chosen
        if (roll <= 50) {
            if (targetA1 == null) {
                targetShip = targetA2;

            } else {
                targetShip = targetA1;
            }
        }

        // Make sure that targetShip ship is not already boarded by 2 ships
        if (targetShip != null) {
            if (boarded1.contains(targetShip.getShipId()) && boarded2.contains(targetShip.getShipId())) {
                targetShip = null;
            }
        }

        // Check if 2nd targetShip will be chosen
        if ((targetShip == null) && (roll <= 90)) {
            // Try to find 2nd targetShip of 1 class higher or lower that is not board yet
            final Ship targetB1 = findTargetShipDiffClassNotBoarded(thisShip, enemySide, boarded1);

            // Try to find 2nd targetShip of 1 class higher or lower
            final Ship targetB2 = findTargetShipDiffClass(thisShip, enemySide);

            if (targetB1 == null) {
                targetShip = targetB2;

            } else {
                targetShip = targetB1;
            }
        }

        // Mark targetShip ship as boarded
        if (targetShip != null) {
            if (boarded1.contains(targetShip.getShipId())) {
                if (boarded2.contains(targetShip.getShipId())) {
                    // Target ship already boarded by 2 ships
                    targetShip = null;

                } else {
                    // Target ship already boarded by 1 ship
                    boarded2.add(targetShip.getShipId());
                }

            } else {
                // Target ship not boarded yet
                boarded1.add(targetShip.getShipId());
            }
        }

        // if we still have a target
        if (targetShip != null) {
            if (enemySide == 1) {
                existingShipPair = new ShipPair(thisShip, targetShip);

            } else {
                existingShipPair = new ShipPair(targetShip, thisShip);
            }

            // update boarded set
            if (boarded1.contains(thisShip.getShipId())) {
                boarded2.add(thisShip.getShipId());

            } else {
                boarded1.add(thisShip.getShipId());
            }
        }

        return existingShipPair;
    }

    /**
     * Find an enemy ship of same class to approach for hand-to-hand.
     *
     * @param thisShip  the ship about to fire.
     * @param enemySide the side of the enemy fleet.
     * @return the target ship.
     */
    protected final Ship findTargetShipSameClass(final Ship thisShip, final int enemySide) {
        Ship targetShip = null;

        // Try to find an enemy ship of the same class.
        final List<Ship> shipList = getParent().getSideShipsSC(enemySide, thisShip.getType().getShipClass());
        final List<Ship> aliveShipList = new ArrayList<Ship>();

        // Remove from list any ship that is sunk
        for (final Ship ship : shipList) {
            if ((ship.getCondition() > 0)
                    && (ship.getCapturedByNation() == 0)) {
                aliveShipList.add(ship);
            }
        }

        // Check that at least 1 ship of the particular Ship Class is still sailing
        if (!aliveShipList.isEmpty()) {
            final int target = getParent().getRandomGen().nextInt(aliveShipList.size());
            targetShip = shipList.get(target);
        }

        return targetShip;
    }

    /**
     * Find an enemy ship of same class to approach for hand-to-hand.
     *
     * @param thisShip  the ship about to fire.
     * @param enemySide the side of the enemy fleet.
     * @param boarded   the ships that have been boarded by an enemy ship.
     * @return the target ship.
     */
    protected final Ship findTargetShipSameClassNotBoarded(final Ship thisShip,
                                                           final int enemySide,
                                                           final Set<Integer> boarded) {
        Ship targetShip = null;

        // Try to find an enemy ship of the same class.
        final List<Ship> shipList = getParent().getSideShipsSC(enemySide, thisShip.getType().getShipClass());
        final List<Ship> aliveShipList = new ArrayList<Ship>();

        // Remove from list any ship that is sunk
        for (final Ship ship : shipList) {
            if ((ship.getCondition() > 0)
                    && (ship.getCapturedByNation() == 0)
                    && (!boarded.contains(ship.getShipId()))) {
                aliveShipList.add(ship);
            }
        }

        // Check that at least 1 ship of the particular Ship Class is still sailing
        if (!aliveShipList.isEmpty()) {
            final int target = getParent().getRandomGen().nextInt(aliveShipList.size());
            targetShip = shipList.get(target);
        }

        return targetShip;
    }

    /**
     * Find an enemy ship of 1 class lower or higher to approach for hand-to-hand.
     * Class three warships can board class four and five warships.
     *
     * @param thisShip  the ship about to fire.
     * @param enemySide the side of the enemy fleet.
     * @return the target ship.
     */
    protected final Ship findTargetShipDiffClass(final Ship thisShip, final int enemySide) {
        Ship targetShip = null;
        final List<Ship> aliveShipList = new ArrayList<Ship>();
        int class1, class2;

        // Try to find an enemy ship of the higher class.
        switch (thisShip.getType().getShipClass()) {
            case 5:
                class1 = 4;
                class2 = -1;
                break;

            case 4:
            case 2:
                class1 = thisShip.getType().getShipClass() - 1;
                class2 = thisShip.getType().getShipClass() + 1;
                break;

            case 3:
                class1 = 4;
                class2 = 5;
                break;

            case 1:
                class1 = 2;
                class2 = -1;
                break;

            default:
                class1 = -1;
                class2 = -1;
        }

        // Find ship of first selection class
        if (class1 > 0) {
            final List<Ship> shipList = getParent().getSideShipsSC(enemySide, class1);

            // Remove from list any ship that is sunk
            for (final Ship ship : shipList) {
                if ((ship.getCondition() > 0)
                        && (ship.getCapturedByNation() == 0)) {
                    aliveShipList.add(ship);
                }
            }
        }

        // Find ship of second selection class
        if (class2 > 0) {
            final List<Ship> shipList = getParent().getSideShipsSC(enemySide, class2);

            // Remove from list any ship that is sunk
            for (final Ship ship : shipList) {
                if ((ship.getCondition() > 0)
                        && (ship.getCapturedByNation() == 0)) {
                    aliveShipList.add(ship);
                }
            }
        }

        // Check that at least 1 ship of the particular Ship Class is still sailing
        if (!aliveShipList.isEmpty()) {
            final int target = getParent().getRandomGen().nextInt(aliveShipList.size());
            targetShip = aliveShipList.get(target);
        }

        return targetShip;
    }

    /**
     * Find an enemy ship of 1 class lower or higher to approach for hand-to-hand.
     * Class three warships can board class four and five warships.
     *
     * @param thisShip  the ship about to fire.
     * @param enemySide the side of the enemy fleet.
     * @param boarded   the ships that have been boarded by an enemy ship.
     * @return the target ship.
     */
    protected final Ship findTargetShipDiffClassNotBoarded(final Ship thisShip,
                                                           final int enemySide,
                                                           final Set<Integer> boarded) {
        Ship targetShip = null;
        final List<Ship> aliveShipList = new ArrayList<Ship>();
        int class1, class2;

        // Try to find an enemy ship of the higher class.
        switch (thisShip.getType().getShipClass()) {
            case 5:
                class1 = 4;
                class2 = -1;
                break;

            case 4:
            case 2:
                class1 = thisShip.getType().getShipClass() - 1;
                class2 = thisShip.getType().getShipClass() + 1;
                break;

            case 3:
                class1 = 4;
                class2 = 5;
                break;

            case 1:
                class1 = 2;
                class2 = -1;
                break;

            default:
                class1 = -1;
                class2 = -1;
        }

        // Find ship of first selection class
        if (class1 > 0) {
            final List<Ship> shipList = getParent().getSideShipsSC(enemySide, class1);

            // Remove from list any ship that is sunk
            for (final Ship ship : shipList) {
                if ((ship.getCondition() > 0)
                        && (ship.getCapturedByNation() == 0)
                        && (!boarded.contains(ship.getShipId()))) {
                    aliveShipList.add(ship);
                }
            }
        }

        // Find ship of second selection class
        if (class2 > 0) {
            final List<Ship> shipList = getParent().getSideShipsSC(enemySide, class2);

            // Remove from list any ship that is sunk
            for (final Ship ship : shipList) {
                if ((ship.getCondition() > 0)
                        && (ship.getCapturedByNation() == 0)
                        && (!boarded.contains(ship.getShipId()))) {
                    aliveShipList.add(ship);
                }
            }
        }

        // Check that at least 1 ship of the particular Ship Class is still sailing
        if (!aliveShipList.isEmpty()) {
            final int target = getParent().getRandomGen().nextInt(aliveShipList.size());
            targetShip = aliveShipList.get(target);
        }

        return targetShip;
    }

}
