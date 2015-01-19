package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Setup Fleet.
 */
public class SetupFleet
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SetupFleet.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_B_FLT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public SetupFleet(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("SetupFleet instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int tmpFleetId = Integer.parseInt(getOrder().getParameter1());
        final String fleetName = getOrder().getParameter2();
        final int posX = Integer.parseInt(getOrder().getParameter4());
        final int posY = Integer.parseInt(getOrder().getParameter5());
        final int posRegion = Integer.parseInt(getOrder().getParameter6());

        // Create position for new Fleet
        final Position thisPosition = new Position();
        thisPosition.setGame(getOrder().getGame());
        thisPosition.setRegion(RegionManager.getInstance().getByID(posRegion));
        thisPosition.setX(posX);
        thisPosition.setY(posY);

        // Create Fleet
        final Fleet newFleet = new Fleet();
        newFleet.setName(fleetName);
        newFleet.setPosition(thisPosition);
        newFleet.setMps(0);
        newFleet.setNation(getOrder().getNation());

        // Add Fleet
        FleetManager.getInstance().add(newFleet);

        getOrder().setResult(1);
        getOrder().setExplanation("new fleet " + newFleet.getName() + " created at " + newFleet.getPosition());

        // Associate ID assigned from DB with the one used by UI
        getParent().associateFleetId(newFleet.getFleetId(), tmpFleetId);
    }
}
