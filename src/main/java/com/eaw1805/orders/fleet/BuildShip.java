package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.fleet.ShipTypeManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.fleet.ShipType;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order for Ship Construction.
 * ticket #19.
 */
public class BuildShip
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BuildShip.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_B_SHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BuildShip(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("BuildShip instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int sectorId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the sector that we wish to build the ship
        final Sector thisSector = SectorManager.getInstance().getByID(sectorId);

        if (thisSector == null) {
            getOrder().setResult(-11);
            getOrder().setExplanation("cannot find sector");
            return;
        }

        // Update order's region of effect
        getOrder().setRegion(thisSector.getPosition().getRegion());

        // Make sure that the owner is the same
        if (thisSector.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();
            final int regionId = thisSector.getPosition().getRegion().getId();
            final int shipTypeId = Integer.parseInt(getOrder().getParameter2());
            final String shipName = getOrder().getParameter3();

            // Retrieve the ship type
            final ShipType shipTPE = ShipTypeManager.getInstance().getByType(shipTypeId);

            // Make sure that it can be built at the colonies
            if ((regionId == EUROPE) || (shipTPE.getCanColonies())) {

                // Make sure Dhows are built only by Morocco, Ottoman, Egypt
                if (((shipTPE.getIntId() == 24)
                        || (shipTPE.getIntId() == 25)
                        || (shipTPE.getIntId() == 11)
                        || (shipTPE.getIntId() == 12))
                        && (getOrder().getNation().getId() != NationConstants.NATION_MOROCCO)
                        && (getOrder().getNation().getId() != NationConstants.NATION_OTTOMAN)
                        && (getOrder().getNation().getId() != NationConstants.NATION_EGYPT)
                        && (isAfrica(thisSector))) {

                    getOrder().setResult(-9);
                    getOrder().setExplanation("ship class cannot only be built by Morocco, Ottoman Empire and Egypt within African or Mediterranean territories");

                } else if ((shipTPE.getIntId() == 31)
                        && (getOrder().getNation().getId() != NationConstants.NATION_GREATBRITAIN)
                        && (getOrder().getNation().getId() != NationConstants.NATION_HOLLAND)
                        && (getSphere(thisSector, getOrder().getNation()) == 3)) {

                    // Make sure that Indiamen are only available to Great Britain and Holland
                    getOrder().setResult(-8);
                    getOrder().setExplanation("ship class cannot only be built by Great Britain and Holland within home region or sphere of influence");

                } else if ((shipTPE.getShipClass() >= 3 || shipTPE.getIntId() == 23)
                        && (getSphere(thisSector, getOrder().getNation()) == 3)) {

                    // Make sure that class 3-5 and large merchant ships
                    getOrder().setResult(-11);
                    getOrder().setExplanation("ship class cannot only be built in home region or within sphere of influence");

                } else if (thisSector.hasShipyard()) {
                    // make sure a shipyard is available

                    // Double Costs custom game option
                    final int doubleCosts = getGame().isDoubleCostsNavy() ? 2 : 1;

                    // Make sure that enough money are available
                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= shipTPE.getCost() * doubleCosts) {

                        // Make sure that enough citizens are available
                        if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= shipTPE.getCitizens()) {

                            // Make sure that enough Industrial Points are available
                            if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= shipTPE.getIndPt() * doubleCosts) {

                                // Make sure that enough Wood are available
                                if (getParent().getTotGoods(ownerId, regionId, GOOD_WOOD) >= shipTPE.getWood() * doubleCosts) {

                                    // Make sure that enough Fabrics are available
                                    if (getParent().getTotGoods(ownerId, regionId, GOOD_FABRIC) >= shipTPE.getFabrics() * doubleCosts) {

                                        // Reduce materials
                                        getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, shipTPE.getCost() * doubleCosts);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, shipTPE.getCitizens());
                                        getParent().decTotGoods(ownerId, regionId, GOOD_INPT, shipTPE.getIndPt() * doubleCosts);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_WOOD, shipTPE.getWood() * doubleCosts);
                                        getParent().decTotGoods(ownerId, regionId, GOOD_FABRIC, shipTPE.getFabrics() * doubleCosts);

                                        // Update goods used by order
                                        final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                        usedGoods.put(GOOD_MONEY, shipTPE.getCost() * doubleCosts);
                                        usedGoods.put(GOOD_INPT, shipTPE.getIndPt() * doubleCosts);
                                        usedGoods.put(GOOD_PEOPLE, shipTPE.getCitizens());
                                        usedGoods.put(GOOD_WOOD, shipTPE.getWood() * doubleCosts);
                                        usedGoods.put(GOOD_FABRIC, shipTPE.getFabrics() * doubleCosts);
                                        getOrder().setUsedGoodsQnt(usedGoods);

                                        if (shipTPE.getShipClass() < 3
                                                || getParent().getGame().isFastShipConstruction()) {

                                            // Create new ship
                                            final Ship newShip = new Ship();
                                            newShip.setNation(getOrder().getNation());
                                            newShip.setType(shipTPE);
                                            newShip.setName(shipName);
                                            newShip.setPosition(thisSector.getPosition());
                                            newShip.setCondition(100);
                                            newShip.setMarines(shipTPE.getCitizens());
                                            newShip.setJustConstructed(true);
                                            newShip.setExp(0);

                                            final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>();
                                            for (int goodID = GOOD_FIRST; goodID <= GOOD_COLONIAL; goodID++) {
                                                qteGoods.put(goodID, 0);
                                            }
                                            newShip.setStoredGoods(qteGoods);

                                            ShipManager.getInstance().add(newShip);

                                            getOrder().setResult(1);
                                            getOrder().setExplanation("new ship [" + shipName + "] of type " + shipTPE.getName() + " built at " + thisSector.getPosition().toString());

                                        } else {
                                            // delay build of ship
                                            newsSingle(getOrder().getNation(), NEWS_MILITARY,
                                                    "A new class " + shipTPE.getShipClass() + " ship is being built at " + thisSector.getPosition().toString());

                                            getOrder().setResult(shipTPE.getShipClass());
                                            getOrder().setExplanation("started to build ship [" + shipName + "] of type " + shipTPE.getName() + " at " + thisSector.getPosition().toString());
                                        }

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
                    getOrder().setExplanation("sector does not support a shipyard");
                }

            } else {
                getOrder().setResult(-7);
                getOrder().setExplanation("ship class cannot be built in the colonies");
            }

        } else {
            getOrder().setResult(-10);
            getOrder().setExplanation("not owner of sector");
        }
    }

    protected boolean isAfrica(final Sector sector) {
        return (sector.getPosition().getX() >= 0 && sector.getPosition().getX() <= 31 && sector.getPosition().getY() > 46)
                || (sector.getPosition().getX() >= 32 && sector.getPosition().getX() <= 44 && sector.getPosition().getY() > 52)
                || (sector.getPosition().getX() >= 45 && sector.getPosition().getX() <= 81 && sector.getPosition().getY() > 39);
    }

}
