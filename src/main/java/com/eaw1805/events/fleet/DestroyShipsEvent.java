package com.eaw1805.events.fleet;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.WeightCalculators;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.Good;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implements Destroy/Scruttling Ships Events.
 */
public class DestroyShipsEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(DestroyShipsEvent.class);

    /**
     * The game to inspect.
     */
    private final transient Game thisGame;

    /**
     * The random generator.
     */
    private final transient Random randomGen;

    /**
     * The weight of the load of the fleets (goods + brigades).
     */
    private final transient Map<Integer, Integer> fleetLoad;

    /**
     * The capacity of the fleets (goods + brigades).
     */
    private final transient Map<Integer, Integer> fleetCapacity;

    /**
     * The size of the fleets (in number of ships).
     */
    private final transient Map<Integer, Integer> fleetSize;

    /**
     * The brigades loaded on a fleet that need reallocation.
     */
    private final transient Map<Integer, List<Brigade>> fleetBrigades;

    /**
     * Keep track if the ship where the brigades was loaded was captured or destroyed and by which nation (if captured).
     */
    private final transient Map<Integer, Nation> brigadeCaptured;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DestroyShipsEvent(final EventProcessor myParent) {
        super(myParent);
        thisGame = myParent.getGame();
        randomGen = myParent.getRandomGen();
        fleetLoad = new HashMap<Integer, Integer>();
        fleetCapacity = new HashMap<Integer, Integer>();
        fleetSize = new HashMap<Integer, Integer>();
        fleetBrigades = new HashMap<Integer, List<Brigade>>();
        brigadeCaptured = new HashMap<Integer, Nation>();
        LOGGER.debug("DestroyShipsEvent instantiated.");
    }

    /**
     * Default constructor.
     *
     * @param game the game to examine.
     */
    public DestroyShipsEvent(final Game game, final Random random) {
        super(game);
        thisGame = game;
        randomGen = random;
        fleetLoad = new HashMap<Integer, Integer>();
        fleetCapacity = new HashMap<Integer, Integer>();
        fleetSize = new HashMap<Integer, Integer>();
        fleetBrigades = new HashMap<Integer, List<Brigade>>();
        brigadeCaptured = new HashMap<Integer, Nation>();
        LOGGER.debug("DestroyShipsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Ship> shipList = ShipManager.getInstance().listByGame(thisGame);
        for (final Ship ship : shipList) {
            boolean isDestroyed = false;

            // A ship will be scuttled by its crew if it has less than 5 Mps
            if (ship.getType().getMovementFactor() * ship.getCondition() / 100d < 5d) {
                destroyShip(ship);
                isDestroyed = true;

            } else if (ship.getCondition() < 1) {
                destroyShip(ship);
                isDestroyed = true;

            } else if (ship.getCondition() < 50) {
                // If its condition has sunk below 50% with a possibility of
                double goal = (50 - ship.getCondition()) ^ 2;
                goal /= 25d;

                if (randomGen.nextInt(101) + 1 < goal) {
                    // Report destruction of ship.
                    destroyShip(ship);
                    isDestroyed = true;
                }
            }

            // Hand-over ships
            if (!isDestroyed && ship.getCapturedByNation() != 0) {
                final Nation newOwner = NationManager.getInstance().getByID(Math.abs(ship.getCapturedByNation()));
                ship.setNation(newOwner);
                ship.setCapturedByNation(0);

                // destroy any loaded unit
                destroyLoadedUnits(ship, true);

                ship.setFleet(0);

                ShipManager.getInstance().update(ship);
            }

            // Remove JustConstructed flag
            if (!isDestroyed && ship.getJustConstructed()) {
                ship.setJustConstructed(false);
                ShipManager.getInstance().update(ship);
            }
        }

        // Try to reallocate brigades whose ship sunk
        for (Map.Entry<Integer, List<Brigade>> entry : fleetBrigades.entrySet()) {
            final int fleetId = entry.getKey();
            final List<Brigade> brigadeList = entry.getValue();
            final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getGame(), fleetId);
            LOGGER.info("Reallocating " + brigadeList.size() + " brigades loaded on fleet " + fleetId);

            // check if fleet is totally destroyed
            if (lstShips.isEmpty()) {
                // destroy all brigades
                for (final Brigade brigade : brigadeList) {
                    // Report capture of spy.
                    if (brigadeCaptured.containsKey(brigade.getBrigadeId())) {
                        final Nation newNation = brigadeCaptured.get(brigade.getBrigadeId());
                        newsPair(brigade.getNation(), newNation, NEWS_POLITICAL,
                                "Our brigade '" + brigade.getName() + "' was disbanded as the ship where it was loaded was captured by '" + newNation.getName() + "'.",
                                "We disbanded '" + brigade.getName() + "', the brigade of " + brigade.getNation().getName() + "] when we captured the ship where it was loaded.");

                        LOGGER.info("Brigade [" + brigade.getName() + "] of Nation [" + brigade.getNation().getName() + "] was disbanded when the ship where it was loaded was captured by " + newNation.getName());

                    } else {
                        newsSingle(brigade.getNation(), NEWS_POLITICAL, "Our brigade '" + brigade.getName() + "' was disbanded when the ship where it was loaded sunk");
                        LOGGER.info("Brigade [" + brigade.getName() + "] of Nation [" + brigade.getNation().getName() + "] was disbanded when the ship where it was loaded sunk");
                    }

                    // Remove brigade from game
                    BrigadeManager.getInstance().delete(brigade);
                }

            } else {
                // there are other ships available
                // compute new size of fleet
                double newMaxLoad = 0;
                for (Ship ship : lstShips) {
                    // Get maximum load capacity (based on type)
                    newMaxLoad += ship.getType().getLoadCapacity();
                }

                // retrieve capacity required to hold all loaded units
                final double requiredCapacity = fleetLoad.get(fleetId);

                // Check if the new size of the fleet is enough to hold the troops
                if (requiredCapacity > newMaxLoad) {
                    final NumberFormat formatter = new DecimalFormat("#,###,###");

                    // Some troops will go down with the sunk/captured ships
                    final double remainingFraction = newMaxLoad / requiredCapacity;

                    LOGGER.info("Required Capacity=" + requiredCapacity + " / Available Capacity=" + newMaxLoad + " / Required reduction=" + remainingFraction);

                    // allocate all brigades in this ship
                    for (final Brigade brigade : brigadeList) {
                        int lostHeadcount = 0;

                        for (final Battalion battalion : brigade.getBattalions()) {
                            if (battalion.getHeadcount() > 0) {
                                final int newCount = (int) (battalion.getHeadcount() * remainingFraction);
                                lostHeadcount += (battalion.getHeadcount() - newCount);

                                battalion.setHeadcount(newCount);
                                BattalionManager.getInstance().update(battalion);
                            }
                        }

                        // Report capture of brigade.
                        final String lostMen = formatter.format(lostHeadcount);
                        if (brigadeCaptured.containsKey(brigade.getBrigadeId())) {
                            final Nation newNation = brigadeCaptured.get(brigade.getBrigadeId());
                            newsPair(brigade.getNation(), newNation, NEWS_POLITICAL,
                                    "Our brigade '" + brigade.getName() + "' lost " + lostMen + " men when the ship where the troops were loaded was captured by '" + newNation.getName() + "'.",
                                    "We killed " + lostMen + " men of '" + brigade.getName() + "', the brigade of " + brigade.getNation().getName() + "] when we captured the ship where the troops were loaded.");

                            LOGGER.info("Brigade [" + brigade.getName() + "] of Nation [" + brigade.getNation().getName() + "] lost " + lostMen + " men when the ship where they were loaded was captured by " + newNation.getName());

                        } else {
                            newsSingle(brigade.getNation(), NEWS_POLITICAL, "Our brigade '" + brigade.getName() + "' lost " + lostMen + " men when the ship where they were loaded sunk");
                            LOGGER.info("Brigade [" + brigade.getName() + "] of Nation [" + brigade.getNation().getName() + "] lost " + lostMen + " men when the ship where they were loaded sunk");
                        }
                    }
                }

                // Reposition units in the next available ship
                final Ship newShip = lstShips.get(0);
                final Map<Integer, Integer> storedGoods = newShip.getStoredGoods();

                // initialize slot id
                int nextSlot = ArmyConstants.BRIGADE * 1000;

                // allocate all brigades in this ship
                for (final Brigade brigade : brigadeList) {
                    LOGGER.info("Reallocating brigade [" + brigade.getName() + "] to ship [" + newShip.getName() + "]");

                    // find next available slot
                    while (storedGoods.containsKey(nextSlot)) {
                        nextSlot++;
                    }

                    storedGoods.put(nextSlot, brigade.getBrigadeId());

                    // Keep info in each battalion of the brigade
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(ArmyConstants.SHIP);
                    thisCarrying.setCarrierId(newShip.getShipId());

                    for (final Battalion battalion : brigade.getBattalions()) {
                        if (battalion.getHeadcount() > 0) {
                            battalion.setCarrierInfo(thisCarrying);
                            BattalionManager.getInstance().update(battalion);
                        }
                    }
                }

                ShipManager.getInstance().update(newShip);
            }
        }

        LOGGER.info("DestroyShipsEvent processed.");
    }

    public void destroyLoadedUnits(final Ship thisShip, final boolean isCaptured) {
        if (thisShip.getFleet() > 0) {
            if (!fleetBrigades.containsKey(thisShip.getFleet())) {
                // initialize fleet statistics
                computeFleet(thisShip.getFleet());
            }
        }

        // Check if a unit is loaded in the ship
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
        final List<Integer> unitsRemoved = new ArrayList<Integer>();
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                unitsRemoved.add(entry.getKey());
                if (entry.getKey() >= ArmyConstants.SPY * 1000) {
                    // A spy is loaded
                    final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());

                    // Report capture of spy.
                    if (isCaptured) {
                        report(thisSpy.getNation(), "spy.death", "Spy '" + thisSpy.getName() + "' was executed as the ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'");
                        report(thisShip.getNation(), "spy.death", "We executed spy '" + thisSpy.getName() + "' of Nation [" + thisSpy.getNation().getName() + "] when we captured ship '" + thisShip.getName() + "'");

                        newsPair(thisSpy.getNation(), thisSpy.getNation(), NEWS_POLITICAL,
                                "Our spy '" + thisSpy.getName() + "' as the ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'",
                                "We arrested '" + thisSpy.getName() + "', the spy of Nation [" + thisSpy.getNation().getName() + "] when we captured ship '" + thisShip.getName() + "'");

                        LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was executed by enemy forces at " + thisShip.getPosition().toString() + " when ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'");

                    } else {
                        report(thisSpy.getNation(), "spy.death", "Spy '" + thisSpy.getName() + "' was drown when the ship '" + thisShip.getName() + "' sunk.");
                        newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy '" + thisSpy.getName() + "' was drown when the ship '" + thisShip.getName() + "' sunk");
                        LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was drown when ship '" + thisShip.getName() + "' sunk");
                    }

                    // Remove spy from game
                    SpyManager.getInstance().delete(thisSpy);

                } else if (entry.getKey() >= ArmyConstants.COMMANDER * 1000) {
                    // A commander is loaded
                    final Commander thisCommander = CommanderManager.getInstance().getByID(entry.getValue());

                    // Report capture of commander.
                    if (isCaptured) {
                        newsPair(thisCommander.getNation(), thisShip.getNation(), NEWS_POLITICAL,
                                "Our commander '" + thisCommander.getName() + "' was executed as the ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'",
                                "We executed '" + thisCommander.getName() + "', a commander of Nation [" + thisCommander.getNation().getName() + "] when we captured ship '" + thisShip.getName() + "'");

                        LOGGER.info("Commander [" + thisCommander.getName() + "] of Nation [" + thisCommander.getNation().getName() + "] was executed by enemy forces at " + thisShip.getPosition().toString() + " when ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'");

                    } else {
                        newsSingle(thisCommander.getNation(), NEWS_POLITICAL, "Our commander '" + thisCommander.getName() + "' was drown when the ship '" + thisShip.getName() + "' sunk");
                        LOGGER.info("Commander [" + thisCommander.getName() + "] of Nation [" + thisCommander.getNation().getName() + "] was drown when ship '" + thisShip.getName() + "' sunk");
                    }

                    // remove commander from command
                    removeCommander(thisCommander);

                    // remove commander from game
                    thisCommander.setDead(true);

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    thisCommander.setCarrierInfo(thisCarrying);

                    CommanderManager.getInstance().update(thisCommander);

                } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                    // A Brigade is loaded
                    final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());

                    if (thisShip.getFleet() == 0) {
                        // This is a stand-alone ship
                        // Destroy the brigade immediately
                        if (isCaptured) {
                            newsPair(thisBrigade.getNation(), thisShip.getNation(), NEWS_POLITICAL,
                                    "Our brigade '" + thisBrigade.getName() + "' was disbanded as the ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'",
                                    "We disbanded '" + thisBrigade.getName() + "', the brigade of Nation [" + thisBrigade.getNation().getName() + "] when we captured ship '" + thisShip.getName() + "'");

                            LOGGER.info("Brigade [" + thisBrigade.getName() + "] of Nation [" + thisBrigade.getNation().getName() + "] was disbanded by enemy forces at " + thisShip.getPosition().toString() + " when ship '" + thisShip.getName() + "' was captured by '" + thisShip.getNation().getName() + "'");

                        } else {
                            newsSingle(thisBrigade.getNation(), NEWS_POLITICAL, "Our brigade '" + thisBrigade.getName() + "' was disbanded when the ship '" + thisShip.getName() + "' sunk");
                            LOGGER.info("Brigade [" + thisBrigade.getName() + "] of Nation [" + thisBrigade.getNation().getName() + "] was disbanded when ship '" + thisShip.getName() + "' sunk");
                        }

                        // Remove spy from game
                        BrigadeManager.getInstance().delete(thisBrigade);

                    } else {
                        if (isCaptured) {
                            brigadeCaptured.put(thisBrigade.getBrigadeId(), thisShip.getNation());
                        }
                    }
                }
            }
        }

        // remove loaded units
        if (!unitsRemoved.isEmpty()) {
            for (final int loadedUnitSlot : unitsRemoved) {
                thisShip.getStoredGoods().remove(loadedUnitSlot);
            }
        }
    }

    private void destroyShip(final Ship ship) {
        // Report destruction of ship.
        report(ship.getNation(), "ship.destruction", "Ship [" + ship.getName() + "] was scuttled by its crew at " + ship.getPosition().toString());
        newsSingle(ship.getNation(), NEWS_MILITARY, "Our class " + ship.getType().getShipClass() + " " + ship.getType().getName() + " " + ship.getName() + " was scuttled by its crew at " + ship.getPosition().toString());

        LOGGER.info("Ship [" + ship.getName() + "] of Nation [" + ship.getNation().getName() + "] was scuttled by its crew at " + ship.getPosition().toString());

        // destroy any loaded unit
        destroyLoadedUnits(ship, false);

        // Remove ship from game
        ShipManager.getInstance().delete(ship);
    }

    /**
     * Compute the weight of the goods loaded on a fleet.
     *
     * @param fleetId the fleet to check.
     */
    private void computeFleet(final int fleetId) {
        int maxLoad = 0;
        int loadedUnits = 0;
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getGame(), fleetId);
        for (Ship ship : lstShips) {
            loadedUnits += computeLoad(ship);

            // Get maximum load capacity (based on type)
            maxLoad += ship.getType().getLoadCapacity();
        }

        fleetLoad.put(fleetId, loadedUnits);
        fleetCapacity.put(fleetId, maxLoad);
        fleetSize.put(fleetId, lstShips.size());
    }

    /**
     * Compute the weight of the goods loaded on a ship.
     *
     * @param ship the ship to check.
     * @return the total weight.
     */
    private int computeLoad(final Ship ship) {
        // identify brigades loaded on ships of the fleet
        final List<Brigade> loadedBrigades;
        if (!fleetBrigades.containsKey(ship.getFleet())) {
            // Initialize variables
            loadedBrigades = new ArrayList<Brigade>();
            fleetBrigades.put(ship.getFleet(), loadedBrigades);

        } else {
            loadedBrigades = fleetBrigades.get(ship.getFleet());
        }

            // identify loaded units & goods
        int loadedUnits = 0;
        final List<Integer> unitsRemoved = new ArrayList<Integer>();
        for (Map.Entry<Integer, Integer> loadedUnit : ship.getStoredGoods().entrySet()) {
            final int key = loadedUnit.getKey();
            if (key <= GoodConstants.GOOD_LAST) {
                if (loadedUnit.getValue() > 0) {
                    // compute weight
                    final Good loadedGood = GoodManager.getInstance().getByID(key);
                    loadedUnits += loadedGood.getWeightOfGood() * loadedUnit.getValue();
                }

            } else {
                // Check type of unit
                if (loadedUnit.getKey() >= ArmyConstants.SPY * 1000) {
                    // no weight

                } else if (loadedUnit.getKey() >= ArmyConstants.COMMANDER * 1000) {
                    // no weight

                } else if (loadedUnit.getKey() >= ArmyConstants.BRIGADE * 1000) {
                    // locate brigade
                    final Brigade loadedBrigade = BrigadeManager.getInstance().getByID(loadedUnit.getValue());

                    // remove brigade from this ship, we will reallocate
                    unitsRemoved.add(key);

                    // add brigade for inspection after all ships have been sunk / destroyed
                    loadedBrigades.add(loadedBrigade);

                    // check which battalions are loaded on this particular ship
                    for (final Battalion battalion : loadedBrigade.getBattalions()) {
                        if (battalion.getCarrierInfo() != null
                                && battalion.getCarrierInfo().getCarrierType() == ArmyConstants.SHIP
                                && battalion.getCarrierInfo().getCarrierId() == ship.getShipId()) {
                            loadedUnits += WeightCalculators.getBattalionWeight(battalion);
                        }
                    }
                }
            }
        }

        // remove loaded units
        if (!unitsRemoved.isEmpty()) {
            for (final int loadedUnitSlot : unitsRemoved) {
                ship.getStoredGoods().remove(loadedUnitSlot);
            }
            ShipManager.getInstance().update(ship);
        }

        return loadedUnits;
    }

}
