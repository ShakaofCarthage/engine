package com.eaw1805.orders.fleet;

import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Handing over Ships or Territory.
 * ticket #39.
 */
public class HandOverShip
        extends AbstractOrderProcessor
        implements OrderInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(HandOverShip.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_HOVER_SHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public HandOverShip(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("HandOverShip instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int shipId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Ship
        final Ship thisShip = ShipManager.getInstance().getByID(shipId);

        if (thisShip == null) {
            getOrder().setResult(-1);
            getOrder().setExplanation("cannot find entity");

        } else {

            // Check ownership of ship
            if (thisShip.getNation().getId() == getOrder().getNation().getId()) {
                // retrieve
                final int targetId = Integer.parseInt(getOrder().getParameter2());

                // Retrieve target nation
                final Nation target = NationManager.getInstance().getByID(targetId);

                // Retrieve relation between two nations
                final NationsRelation relation = getByNations(getOrder().getNation(), target);

                if (relation.getRelation() == REL_ALLIANCE) {

                    // To do so you must send the ships to an allied shipyard before giving the command.
                    final Sector thisSector = SectorManager.getInstance().getByPosition(thisShip.getPosition());
                    if (thisSector.getNation().getId() == target.getId() && thisSector.hasShipyard()) {
                        thisShip.setNation(target);
                        thisShip.setFleet(0);
                        ShipManager.getInstance().update(thisShip);

                        // Check if ship has loaded goods
                        String loaded = "";
                        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
                        for (final Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                            if (entry.getValue() > 0) {
                                entry.setValue(0);
                                loaded = ". Loaded goods were lost during the transaction.";
                            }
                        }
                        ShipManager.getInstance().update(thisShip);

                        newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                "We handed over our ship " + thisShip.getName() + " to " + target.getName(),
                                getOrder().getNation().getName() + " handed over the ship " + thisShip.getName() + " to us.");

                        // For each type 4 or 5 class ship handed over (-1 per ship)
                        if (thisShip.getType().getShipClass() == 4) {
                            changeVP(getOrder().getGame(), getOrder().getNation(), SHIP_HANDOVER_4, "Handover class-4 ship '" + thisShip.getName() + "' to " + target.getName());

                        } else if (thisShip.getType().getShipClass() == 5) {
                            changeVP(getOrder().getGame(), getOrder().getNation(), SHIP_HANDOVER_5, "Handover class-5 ship '" + thisShip.getName() + "' to " + target.getName());
                        }

                        getOrder().setResult(1);
                        getOrder().setExplanation("handed over " + thisShip.getName() + " to " + target.getName() + loaded);

                    } else {
                        getOrder().setResult(-4);
                        getOrder().setExplanation("ship must be located in an allied shipyard");
                    }

                } else {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("cannot hand over ship to a non-allied nation");
                }

            } else {
                getOrder().setResult(-2);
                getOrder().setExplanation("not owner of ship");
            }
        }
    }
}
