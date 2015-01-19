package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order: Repair Fleet
 */
public class RepairFleet
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RepairFleet.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_R_FLT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RepairFleet(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RepairFleet instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int fleetId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Fleet
        final Fleet thisFleet;
        if (getParent().fleetAssocExists(fleetId)) {
            // this is a new fleet
            thisFleet = FleetManager.getInstance().getByID(getParent().retrieveFleetAssoc(fleetId));

        } else {
            thisFleet = FleetManager.getInstance().getByID(fleetId);
        }


        if (thisFleet != null) {
            boolean isAllied = false;
            if (thisFleet.getNation().getId() != getOrder().getNation().getId()) {
                // Retrieve relation between two nations
                final NationsRelation relation = getByNations(getOrder().getNation(), thisFleet.getNation());

                isAllied = (relation.getRelation() == REL_ALLIANCE);

                if (!isAllied) {
                    getOrder().setResult(-9);
                    getOrder().setExplanation("cannot repair non-allied fleets");
                }
            }

            // Check ownership of ship
            if (isAllied || thisFleet.getNation().getId() == getOrder().getNation().getId()) {
                // Retrieve the sector where we wish to repair the ship
                final Sector thisSector = SectorManager.getInstance().getByPosition(thisFleet.getPosition());

                // Update order's region of effect
                getOrder().setRegion(thisSector.getPosition().getRegion());

                final int ownerId = getOrder().getNation().getId();
                final int regionId = thisSector.getPosition().getRegion().getId();

                int totMoney = 0;
                int totIndPt = 0;
                int totWood = 0;
                int totFabrics = 0;
                int totCitizens = 0;

                // Double Costs custom game option
                final int doubleCosts = getGame().isDoubleCostsNavy() ? 2 : 1;

                // Make sure that it can be built at the colonies
                if (thisSector.hasShipyard()) {
                    // Retrieve ships
                    final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), thisFleet.getFleetId());
                    boolean allRepaired = true;
                    boolean someRepaired = false;
                    for (final Ship thisShip : lstShips) {
                        final double modifier = doubleCosts * (100d - thisShip.getCondition()) / 100d;
                        final int reqMoney = (int) (thisShip.getType().getCost() * modifier);
                        final int reqCitizens = thisShip.getType().getCitizens() - thisShip.getMarines();
                        final int reqWood = (int) (thisShip.getType().getWood() * modifier);
                        final int reqFabrics = (int) (thisShip.getType().getFabrics() * modifier);
                        final int reqInPt = (int) (thisShip.getType().getIndPt() * modifier);

                        // Make sure that all materials are available
                        if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= reqMoney
                                && getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= reqCitizens
                                && getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= reqInPt
                                && getParent().getTotGoods(ownerId, regionId, GOOD_WOOD) >= reqWood
                                && getParent().getTotGoods(ownerId, regionId, GOOD_FABRIC) >= reqFabrics) {

                            // Reduce materials
                            getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, reqMoney);
                            getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, reqCitizens);
                            getParent().decTotGoods(ownerId, regionId, GOOD_INPT, reqInPt);
                            getParent().decTotGoods(ownerId, regionId, GOOD_WOOD, reqWood);
                            getParent().decTotGoods(ownerId, regionId, GOOD_FABRIC, reqFabrics);

                            totMoney += reqMoney;
                            totIndPt += reqInPt;
                            totWood += reqWood;
                            totFabrics += reqFabrics;
                            totCitizens += reqCitizens;

                            // Repair ship
                            thisShip.setCondition(100);
                            thisShip.setMarines(thisShip.getType().getCitizens());
                            ShipManager.getInstance().update(thisShip);

                            // Report success
                            getOrder().setResult(1);
                            getOrder().setExplanation("repaired fleet " + thisFleet.getName());
                            someRepaired = true;

                        } else {
                            allRepaired = false;
                        }
                    }

                    // Update goods used by order
                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                    usedGoods.put(GOOD_MONEY, totMoney);
                    usedGoods.put(GOOD_INPT, totIndPt);
                    usedGoods.put(GOOD_PEOPLE, totCitizens);
                    usedGoods.put(GOOD_WOOD, totWood);
                    usedGoods.put(GOOD_FABRIC, totFabrics);
                    getOrder().setUsedGoodsQnt(usedGoods);

                    if (!someRepaired) {
                        getOrder().setResult(-1);
                        getOrder().setExplanation("not enough materials available at regional warehouse to repair fleet");

                    } else if (!allRepaired) {
                        getOrder().setExplanation("repaired some ships of the fleet " + thisFleet.getName() + ". Not enough materials available at regional warehouse to repair all ships of the fleet.");
                    }

                } else {
                    getOrder().setResult(-6);
                    getOrder().setExplanation("sector does not have a shipyard");
                }

            } else {
                getOrder().setResult(-7);
                getOrder().setExplanation("not owner of sector");
            }
        } else {
            getOrder().setResult(-8);
            getOrder().setExplanation("cannot locate fleet");
        }
    }
}
