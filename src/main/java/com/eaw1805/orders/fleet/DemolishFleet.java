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

import java.util.List;

/**
 * Order - Demolish Fleet.
 * ticket:199.
 */
public class DemolishFleet
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DemolishFleet.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_D_FLT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DemolishFleet(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DemolishFleet instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int fleetId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve fleet
        final Fleet thisFleet = FleetManager.getInstance().getByID(fleetId);

        if (thisFleet != null) {

            // Check ownership
            if (thisFleet.getNation().getId() == getOrder().getNation().getId()) {

                // Retrieve the ships of the fleet
                final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getParent().getGame(), fleetId);

                if (!lstShips.isEmpty()) {
                    for (final Ship thisShip : lstShips) {
                        thisShip.setFleet(0);
                        ShipManager.getInstance().update(thisShip);
                    }
                }

                getOrder().setResult(1);
                getOrder().setExplanation("demolished fleet " + thisFleet.getName());

                // Delete fleet
                FleetManager.getInstance().delete(thisFleet);

            } else {
                getOrder().setResult(-2);
                getOrder().setExplanation("not owner of fleet");
            }

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("unknown fleet");
        }
    }
}
