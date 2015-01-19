package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Hire Commander.
 */
public class HireCommander
        extends CommanderJoinArmy
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(HireCommander.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_HIRE_COM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public HireCommander(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("HireCommander instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        LOGGER.debug("Hire Commander commands are ignored");
    }
}
