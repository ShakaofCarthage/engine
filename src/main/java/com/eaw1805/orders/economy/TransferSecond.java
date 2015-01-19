package com.eaw1805.orders.economy;

import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order for Loading Goods to Baggage Trains.
 * ticket #35.
 */
public class TransferSecond
        extends TransferFirst {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TransferSecond.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_EXCHS;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public TransferSecond(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("TransferSecond instantiated.");
    }
}
