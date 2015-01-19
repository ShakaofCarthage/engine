package com.eaw1805.orders.fleet;

import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Load Units during 2nd loading phase.
 */
public class LoadSecond
        extends LoadFirst {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(LoadSecond.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_LOAD_TROOPSS;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public LoadSecond(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("LoadSecond instantiated.");
    }
}
