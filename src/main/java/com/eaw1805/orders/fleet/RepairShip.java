package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order - Repair Ship.
 * ticket:20.
 */
public class RepairShip
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RepairShip.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_R_SHP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RepairShip(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RepairShip instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int shipId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Ship
        final Ship thisShip = ShipManager.getInstance().getByID(shipId);

        // Double Costs custom game option
        final int doubleCosts = getGame().isDoubleCostsNavy() ? 2 : 1;

        if (thisShip != null) {
            boolean isAllied = false;
            if (thisShip.getNation().getId() != getOrder().getNation().getId()) {
                // Retrieve relation between two nations
                final NationsRelation relation = getByNations(getOrder().getNation(), thisShip.getNation());

                isAllied = (relation.getRelation() == REL_ALLIANCE);

                if (!isAllied) {
                    getOrder().setResult(-9);
                    getOrder().setExplanation("cannot repair non-allied ships");
                }
            }

            // Check ownership of ship
            if (isAllied || thisShip.getNation().getId() == getOrder().getNation().getId()) {
                // Retrieve the sector where we wish to repair the ship
                final Sector thisSector = SectorManager.getInstance().getByPosition(thisShip.getPosition());

                // Update order's region of effect
                getOrder().setRegion(thisSector.getPosition().getRegion());

                final int ownerId = getOrder().getNation().getId();
                final int regionId = thisSector.getPosition().getRegion().getId();

                // Make sure that it can be built at the colonies
                if (thisSector.hasShipyard()) {
                    final double modifier = doubleCosts * (100d - thisShip.getCondition()) / 100d;
                    final int reqMoney = (int) (thisShip.getType().getCost() * modifier);
                    final int reqCitizens = thisShip.getType().getCitizens() - thisShip.getMarines();
                    final int reqWood = (int) (thisShip.getType().getWood() * modifier);
                    final int reqFabrics = (int) (thisShip.getType().getFabrics() * modifier);
                    final int reqInPt = (int) (thisShip.getType().getIndPt() * modifier);

                    // Make sure that enough money are available
                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= reqMoney) {

                        // Make sure that enough citizens are available
                        if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= reqCitizens) {

                            // Make sure that enough Industrial Points are available
                            if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= reqInPt) {

                                // Make sure that enough Wood are available
                                if (getParent().getTotGoods(ownerId, regionId, GOOD_WOOD) >= reqWood) {

                                    // Make sure that enough Fabrics are available
                                    if (getParent().getTotGoods(ownerId, regionId, GOOD_FABRIC) >= reqFabrics) {

                                        // Reduce materials
                                        getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, reqMoney);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, reqCitizens);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_INPT, reqInPt);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_WOOD, reqWood);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_FABRIC, reqFabrics);

                                        // Update goods used by order
                                        final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                        usedGoods.put(GOOD_MONEY, reqMoney);
                                        usedGoods.put(GOOD_INPT, reqInPt);
                                        usedGoods.put(GOOD_PEOPLE, reqCitizens);
                                        usedGoods.put(GOOD_WOOD, reqWood);
                                        usedGoods.put(GOOD_FABRIC, reqFabrics);
                                        getOrder().setUsedGoodsQnt(usedGoods);

                                        // Repair ship
                                        thisShip.setCondition(100);
                                        thisShip.setMarines(thisShip.getType().getCitizens());
                                        ShipManager.getInstance().update(thisShip);

                                        // Report success
                                        getOrder().setResult(1);
                                        getOrder().setExplanation("repaired ship " + thisShip.getName());

                                    } else {
                                        getOrder().setResult(-1);
                                        getOrder().setExplanation("not enough fabric at regional warehouse");
                                    }

                                } else {
                                    getOrder().setResult(-2);
                                    getOrder().setExplanation("not enough wood at regional warehouse");
                                }

                            } else {
                                getOrder().setResult(-3);
                                getOrder().setExplanation("not enough industrial points at regional warehouse");
                            }

                        } else {
                            getOrder().setResult(-4);
                            getOrder().setExplanation("not enough citizens at regional warehouse");
                        }

                    } else {
                        getOrder().setResult(-5);
                        getOrder().setExplanation("not enough money at empire warehouse");
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
            getOrder().setExplanation("cannot locate ship");
        }
    }
}
