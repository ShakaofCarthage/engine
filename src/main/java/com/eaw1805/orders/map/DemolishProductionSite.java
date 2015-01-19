package com.eaw1805.orders.map;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the Build Production Site order.
 * ticket #41.
 */
public class DemolishProductionSite
        extends AbstractOrderProcessor
        implements OrderInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DemolishProductionSite.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_D_PRODS;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DemolishProductionSite(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DemolishProductionSite instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        // retrieve sector
        final int sectorID = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the sector that we wish to increase the population density
        final Sector thisSector = SectorManager.getInstance().getByID(sectorID);

        if (thisSector == null) {
            getOrder().setResult(-5);
            getOrder().setExplanation("cannot find sector");
            return;
        }

        // Update order's region of effect
        getOrder().setRegion(thisSector.getPosition().getRegion());

        // Make sure that the owner is the same
        if (thisSector.getNation().getId() == getOrder().getNation().getId()) {

            // Check if enemy brigades are present
            if (enemyNotPresent(thisSector)) {

                // Check if this is a trade city
                if (thisSector.getTradeCity()) {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("cannot demolish a trade city");

                } else {

                    if (thisSector.getProductionSite() == null) {
                        getOrder().setResult(-4);
                        getOrder().setExplanation("sector does not support a production site");

                    } else {

                        // Check if this is a barrack or a shipyard
                        if (thisSector.hasBarrack()) {

                            // If there are ships in the barrack, then move them out.
                            // In case we need to move ships outside the shipyard, select the sector where it will move
                            final List<Sector> lstSectors = SectorManager.getInstance().listAdjacentSea(thisSector.getPosition());
                            final Sector exitSector;
                            if (lstSectors.isEmpty()) {
                                LOGGER.info("No exit sector found at " + thisSector.getPosition());
                                exitSector = null;

                            } else {
                                // Randomly select one
                                java.util.Collections.shuffle(lstSectors);

                                if (lstSectors.get(0).getId() == thisSector.getId()) {
                                    lstSectors.remove(0);

                                    if (lstSectors.isEmpty()) {
                                        LOGGER.info("No exit sector found at " + thisSector.getPosition());
                                        exitSector = null;

                                    } else {
                                        exitSector = lstSectors.get(0);
                                    }

                                } else {
                                    exitSector = lstSectors.get(0);
                                }
                            }

                            final StringBuilder moved = new StringBuilder();
                            final StringBuilder destroyed = new StringBuilder();
                            int movedShips = 0;
                            int destroyedShips = 0;

                            // Capture Ships under construction
                            final List<Ship> lstShips = ShipManager.getInstance().listByGamePosition(thisSector.getPosition());
                            for (final Ship ship : lstShips) {
                                if (exitSector == null) {
                                    // destroy ship
                                    destroyedShips++;
                                    destroyed.append(ship.getName());
                                    destroyed.append(", ");

                                    // check also loaded units
                                    ship.initializeVariables();

                                    // Check if the entity is carrying units, and update their position too
                                    if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                                        // Unload entities
                                        destroyLoadedUnits(ship, true);
                                    }

                                    ship.setCondition(0);
                                    ship.setMarines(0);
                                    ShipManager.getInstance().update(ship);

                                } else {
                                    // Ship must move out of shipyard
                                    movedShips++;
                                    moved.append(ship.getName());
                                    moved.append(", ");

                                    // move ship
                                    ship.setPosition((Position) exitSector.getPosition().clone());
                                    ShipManager.getInstance().update(ship);

                                    // check also loaded units
                                    ship.initializeVariables();

                                    // Check if the entity is carrying units, and update their position too
                                    if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                                        ship.updatePosition((Position) exitSector.getPosition().clone());
                                    }

                                    // Check if ship is in fleet
                                    if (ship.getFleet() != 0) {
                                        // Also update fleet
                                        final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                                        thisFleet.setPosition(exitSector.getPosition());
                                        FleetManager.getInstance().update(thisFleet);
                                    }
                                }
                            }

                            if (destroyedShips > 0) {
                                destroyed.delete(destroyed.length() - 2, destroyed.length() - 1);
                                newsSingle(thisSector.getNation(), NEWS_MILITARY,
                                        "We demolished our shipyard at " + thisSector.getPosition() + " and "
                                                + destroyedShips + " ships (" + destroyed.toString() + ") were destroyed.");
                            }

                            if (movedShips > 0) {
                                moved.delete(moved.length() - 2, moved.length() - 1);
                                newsSingle(thisSector.getNation(), NEWS_MILITARY,
                                        "We demolished our shipyard at " + thisSector.getPosition() + " and "
                                                + movedShips + " ships (" + moved.toString() + ") moved out.");
                            }

                            // Also delete Barrack
                            final Barrack thisBarrack = BarrackManager.getInstance().getByPosition(thisSector.getPosition());
                            BarrackManager.getInstance().delete(thisBarrack);
                        }

                        // Reduce materials
                        getParent().decTotGoods(getOrder().getNation().getId(), EUROPE, GoodConstants.GOOD_MONEY, 5000);

                        // Update goods used by order
                        final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                        usedGoods.put(GoodConstants.GOOD_MONEY, 5000);
                        getOrder().setUsedGoodsQnt(usedGoods);

                        getOrder().setResult(1);
                        getOrder().setExplanation("demolished production site (" + thisSector.getProductionSite().getName() + ") at sector "
                                + thisSector.getPosition().toString());

                        thisSector.setProductionSite(null);
                        SectorManager.getInstance().update(thisSector);
                    }
                }
            } else {
                getOrder().setResult(-1);
                getOrder().setExplanation("enemy brigades located on the sector");
            }

        } else {
            getOrder().setResult(-2);
            getOrder().setExplanation("not owner of sector");
        }
    }

}

