package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RenameFleet extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameFleet.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_FLT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameFleet(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameFleet instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int fleetId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the Army
        final Fleet thisFleet = FleetManager.getInstance().getByID(fleetId);

        if (thisFleet == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("fleet not found");

            // Check ownership of Army
        } else if (thisFleet.getNation().getId() == getOrder().getNation().getId()) {
            thisFleet.setName(getOrder().getParameter2());
            FleetManager.getInstance().update(thisFleet);

            getOrder().setResult(1);
            getOrder().setExplanation("fleet " + fleetId + " changed name to " + thisFleet.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of fleet");
        }
    }

}
