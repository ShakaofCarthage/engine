package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Rename Ship.
 * ticket:40.
 */
public class RenameShip
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameShip.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_SHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameShip(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameShip instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int shipId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source ship
        final Ship thisShip = ShipManager.getInstance().getByID(shipId);

        if (thisShip == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("ship not found");

            // Check ownership of ship
        } else if (thisShip.getNation().getId() == getOrder().getNation().getId()) {
            thisShip.setName(getOrder().getParameter2());
            ShipManager.getInstance().update(thisShip);

            getOrder().setResult(1);
            getOrder().setExplanation("ship " + shipId + " changed name to " + thisShip.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of ship");
        }
    }
}
