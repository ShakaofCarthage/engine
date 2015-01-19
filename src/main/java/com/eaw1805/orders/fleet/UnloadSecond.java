package com.eaw1805.orders.fleet;

import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unload units during the 2nd phase.
 */
public class UnloadSecond
        extends UnloadFirst {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(UnloadSecond.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_UNLOAD_TROOPSS;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public UnloadSecond(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("UnloadSecond instantiated.");
    }

}
