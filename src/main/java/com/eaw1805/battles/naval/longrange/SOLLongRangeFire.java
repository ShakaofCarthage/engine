package com.eaw1805.battles.naval.longrange;

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
 * Processes the 1st round of naval battle.
 * Long-Range fire of Ships-of-the-Line at 50% effectiveness.
 * SOLs only (class 5 warships) will shoot at enemy warships.
 * This round will take place only at clear weather conditions.
 */
public class SOLLongRangeFire
        extends AbstractNavalBattleRound {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SOLLongRangeFire.class);

    /**
     * Firing modifier for this round.
     */
    private int roundModifier;

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public SOLLongRangeFire(final NavalBattleProcessor caller) {
        super(caller);
        roundModifier = 50;
        setRound(ROUND_LR_SOL);
    }

    /**
     * Get the modifier for firing during this round.
     *
     * @return the modifier for firing during this round.
     */
    public int getRoundModifier() {
        return roundModifier;
    }

    /**
     * Set the modifier for firing during this round.
     *
     * @param value the modifier for firing during this round.
     */
    public void setRoundModifier(final int value) {
        this.roundModifier = value;
    }

    /**
     * Check weather to determine if this round will take place.
     *
     * @return true if the weather is CLEAR.
     */
    public boolean checkWeather() {
        return (getParent().getWeather() == NavalBattleProcessor.WEATHER_CLEAR);
    }

    /**
     * The minimum ship class that can participate in this round.
     *
     * @return 5.
     */
    public int minimumShipClass() {
        return 5;
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
        final List<Ship> lostShips[] = new List[2];
        final Set<ShipPair> lstSp = new HashSet<ShipPair>();

        // This round will take place only at clear weather conditions.
        if (checkWeather()) {

            // Calculate modifier based on weather
            final int fireModWeather = calcWeatherFireModifier(getParent().getWeather());

            // inspect each side separately
            for (int side = 0; side < 2; side++) {
                lostShips[side] = new ArrayList<Ship>();

                // iterate through each ship
                for (final Ship ship : lstShips[side]) {
                    if ((ship.getCondition() > 0)
                            && (ship.getMarines() > 0)
                            && (ship.getType().getShipClass() >= minimumShipClass())) {

                        // find target
                        final ShipPair spair = findTargetShip(ship, (side + 1) % 2, lstSp);

                        // check that we did manage to find a target
                        if (spair == null) {
                            continue;
                        }

                        // Get target ship
                        final Ship targetShip;
                        if ((side + 1) % 2 == 0) {
                            targetShip = spair.getShipA();

                        } else {
                            targetShip = spair.getShipB();
                        }

                        // calculate modifier based on target ship's class
                        final int fireModShipClass = calcTargetFireModifier(targetShip.getType().getShipClass());

                        // calculate fire efficiency
                        final double fireEfficiency = (fireModWeather / 100d)
                                * (fireModShipClass / 100d)
                                * (getRoundModifier() / 100d);

                        final int cannons = ship.getType().getCannons();
                        int morale = ship.getNation().getMorale() + ship.getExp();

                        // Lack of Wine effect in Naval Battle
                        if (ship.getNoWine()) {
                            morale--;
                        }

                        // calculate combat points
                        final double comPoints = cannons / 2d
                                * (getParent().getRandomGen().nextInt(21) + 10) / 10d
                                * Math.sqrt(morale)
                                * (ship.getCondition() / 100d)
                                * fireEfficiency;

                        spair.setCombatPoints(side, 1, (int) comPoints);

                        // calculate losses
                        final int lossMarines = (int) (comPoints / (3 * Math.sqrt(targetShip.getType().getShipClass())));
                        final int lossTonnage = (int) (comPoints / Math.sqrt(targetShip.getType().getShipClass()));

                        spair.setLossTonnage((side + 1) % 2, lossTonnage);
                        spair.setLossMarines((side + 1) % 2, 1, lossMarines);

                        lstSp.add(spair);
                    }
                }
            }
        }

        // apply losses
        for (final ShipPair shipPair : lstSp) {
            // Side A ship
            int losesA = shipPair.getLossMarines(0, 1);
            shipPair.getShipA().calcCondition(shipPair.getLossTonnage(0));
            shipPair.getShipA().setMarines(shipPair.getShipA().getMarines() - shipPair.getLossMarines(0, 1));

            // Make sure we do not end up with negative marines
            if (shipPair.getShipA().getMarines() < 0) {
                losesA -= shipPair.getShipA().getMarines();
                shipPair.getShipA().setMarines(0);
            }

            // Also keep track of marines killed in player's profile
            changeProfile(getParent().getGame(), shipPair.getShipB().getNation(), ProfileConstants.ENEMY_KILLED_MAR, losesA);

            // Check if Side A ship will sink
            final int rollA = getParent().getRandomGen().nextInt(100) + 1;
            if (shipPair.getShipA().getCondition() < 60 && rollA <= (60 - shipPair.getShipA().getCondition())) {
                // The ship has sunk
                final Ship sunkShip = shipPair.getShipA();
                sunkShip.setCapturedByNation(sunkShip.getNation().getId());
                sunkShip.setCondition(0);
                sunkShip.setMarines(0);
                lostShips[0].add(sunkShip);
                LOGGER.debug("Side A ship " + sunkShip.getName() + " [" + sunkShip.getShipId() + "] was sunk");

                switch (sunkShip.getType().getShipClass()) {
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

            // Side B ship
            int losesB = shipPair.getLossMarines(1, 1);
            shipPair.getShipB().calcCondition(shipPair.getLossTonnage(1));
            shipPair.getShipB().setMarines(shipPair.getShipB().getMarines() - shipPair.getLossMarines(1, 1));

            // Make sure we do not end up with negative marines
            if (shipPair.getShipB().getMarines() < 0) {
                losesB -= shipPair.getShipB().getMarines();
                shipPair.getShipB().setMarines(0);
            }

            // Also keep track of marines killed in player's profile
            changeProfile(getParent().getGame(), shipPair.getShipA().getNation(), ProfileConstants.ENEMY_KILLED_MAR, losesB);

            // Check if Side B ship will sink
            final int rollB = getParent().getRandomGen().nextInt(100) + 1;
            if (shipPair.getShipB().getCondition() < 60 && rollB <= (60 - shipPair.getShipB().getCondition())) {
                // The ship has sunk
                final Ship sunkShip = shipPair.getShipB();
                sunkShip.setCapturedByNation(sunkShip.getNation().getId());
                sunkShip.setCondition(0);
                sunkShip.setMarines(0);
                lostShips[1].add(sunkShip);
                LOGGER.debug("Side B ship " + sunkShip.getName() + " [" + sunkShip.getShipId() + "] was sunk");

                switch (sunkShip.getType().getShipClass()) {
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
            }
        }

        // Produce statistics
        final RoundStat stat = new RoundStat(getRound(), lstShips);
        stat.setSidePairs(lstSp);
        LOGGER.debug(lstSp.size() + " pair of ships exchanged long range fire");

        // Remove sunk ships
        for (int side = 0; side < 2; side++) {
            if (lostShips[side] != null) {
                for (Ship ship : lostShips[side]) {
                    lstShips[side].remove(ship);
                }
            }
        }

        return stat;
    }

    /**
     * Calculate the modifier for firing at target ship based on its ship class.
     *
     * @param targetSC the ship class of the target ship.
     * @return the modifier.
     */
    protected final int calcTargetFireModifier(final int targetSC) {
        int modifier;
        switch (targetSC) {
            case 5:
                modifier = 125;
                break;

            case 4:
                modifier = 110;
                break;

            case 3:
                modifier = 100;
                break;

            case 2:
                modifier = 75;
                break;

            case 1:
            default:
                modifier = 50;
                break;

        }

        return modifier;
    }

    /**
     * Calculate the modifier for firing at target ship based on the weather.
     *
     * @param weather the weather conditions.
     * @return the modifier.
     */
    protected final int calcWeatherFireModifier(final int weather) {
        int modifier;
        switch (weather) {
            case NavalBattleProcessor.WEATHER_CLEAR:
                modifier = 100;
                break;

            case NavalBattleProcessor.WEATHER_RAIN:
                modifier = 75;
                break;

            case NavalBattleProcessor.WEATHER_STORM:
            default:
                modifier = 50;
                break;

        }

        return modifier;
    }

    /**
     * Find an enemy ship to fire upon.
     *
     * @param thisShip  the ship about to fire.
     * @param enemySide the side of the enemy fleet.
     * @param lstSP     the list of ship pairs selected during the course of the naval battle.
     * @return the target ship.
     */
    protected final ShipPair findTargetShip(final Ship thisShip,
                                            final int enemySide,
                                            final Set<ShipPair> lstSP) {
        ShipPair spair = null;

        // Check if an enemy ship has picked this ship as a target
        for (final ShipPair shipPair : lstSP) {

            if ((enemySide == 1) && (shipPair.getShipA().getShipId() == thisShip.getShipId())) {
                spair = shipPair;
                break;
            }

            if ((enemySide == 0) && (shipPair.getShipB().getShipId() == thisShip.getShipId())) {
                spair = shipPair;
                break;
            }
        }

        // Check if no existing pair exist
        if (spair == null) {
            Ship targetShip;
            // Try to find an enemy ship of the same class.
            List<Ship> shipList = getParent().getSideShipsSC(enemySide, thisShip.getType().getShipClass());
            final List<Ship> aliveShipList = new ArrayList<Ship>();

            // Remove from list any ship that is sunk
            for (final Ship ship : shipList) {
                if (ship.getCondition() > 0) {
                    aliveShipList.add(ship);
                }
            }

            // Check that at least 1 ship of the particular Ship Class is still sailing
            if (aliveShipList.isEmpty()) {
                // Same ship class is empty
                // Try to find just 1 enemy ship
                shipList = getParent().getSideShips()[enemySide];

                // Remove from list any ship that is sunk
                for (final Ship ship : shipList) {
                    if ((ship.getCondition() > 0) && (ship.getType().getShipClass() > 0)) {
                        aliveShipList.add(ship);
                    }
                }

                // is there any ship alive?
                if (!aliveShipList.isEmpty()) {
                    final int target = getParent().getRandomGen().nextInt(aliveShipList.size());
                    targetShip = aliveShipList.get(target);

                    if (enemySide == 1) {
                        spair = new ShipPair(thisShip, targetShip);

                    } else {
                        spair = new ShipPair(targetShip, thisShip);
                    }
                }
            } else {
                final int target = getParent().getRandomGen().nextInt(aliveShipList.size());
                targetShip = aliveShipList.get(target);

                if (enemySide == 1) {
                    spair = new ShipPair(thisShip, targetShip);

                } else {
                    spair = new ShipPair(targetShip, thisShip);
                }

            }
        }

        return spair;
    }

}
