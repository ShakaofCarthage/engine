package com.eaw1805.orders.movement;

import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the War Ship Movement order.
 */
public class WarShipMovement
        extends MerchantShipMovement {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(WarShipMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_SHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public WarShipMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("WarShipMovement instantiated.");
    }

}
