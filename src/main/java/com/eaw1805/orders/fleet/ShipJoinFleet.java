package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Ship Join Fleet.
 * ticket:199
 */
public class ShipJoinFleet
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ShipJoinFleet.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_ADDTO_FLT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ShipJoinFleet(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ShipJoinFleet instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int shipId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source brigade
        final Ship thisShip = ShipManager.getInstance().getByID(shipId);

        if (thisShip == null) {
            getOrder().setResult(-1);
            getOrder().setExplanation("cannot locate ship");
            return;
        }

        // Check ownership of source brigade
        if (thisShip.getNation().getId() == getOrder().getNation().getId()) {
            final int fleetId = Integer.parseInt(getOrder().getParameter2());

            if (fleetId == 0) {
                // Remove from fleet
                report("ShipJoinFleet." + getOrder().getOrderId() + ".result", "success - ship " + shipId + " removed from fleet " + thisShip.getFleet());
                thisShip.setFleet(fleetId);
                ShipManager.getInstance().update(thisShip);

            } else {
                // Check if this is a newly created fleet
                if (getParent().fleetAssocExists(fleetId)) {
                    // this is a new fleet
                    thisShip.setFleet(getParent().retrieveFleetAssoc(fleetId));
                    ShipManager.getInstance().update(thisShip);

                    getOrder().setResult(1);
                    getOrder().setExplanation("ship " + thisShip.getName() + " joined newly created fleet " + thisShip.getFleet());

                } else {
                    // Retrieve fleet
                    final Fleet thisFleet = FleetManager.getInstance().getByID(fleetId);

                    if (thisFleet.getPosition().equals(thisShip.getPosition())) {
                        thisShip.setFleet(fleetId);
                        ShipManager.getInstance().update(thisShip);

                        getOrder().setResult(1);
                        getOrder().setExplanation("ship " + thisShip.getName() + " joined newly created fleet " + thisFleet.getName());

                    } else {
                        getOrder().setResult(-1);
                        getOrder().setExplanation("fleet located at a different sector");
                    }
                }
            }
        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of ship");
        }

    }
}

