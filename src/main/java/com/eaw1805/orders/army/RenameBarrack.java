package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Rename barracks.
 */
public class RenameBarrack
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameBarrack.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_BARRACK;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameBarrack(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameBarrack instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int barrackId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the barrack
        final Barrack thisBarrack = BarrackManager.getInstance().getByID(barrackId);

        if (thisBarrack == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("thisBarrack not found");

            // Check ownership of brigade
        } else if (thisBarrack.getNation().getId() == getOrder().getNation().getId()) {
            thisBarrack.setName(getOrder().getParameter2());
            BarrackManager.getInstance().update(thisBarrack);

            getOrder().setResult(1);
            getOrder().setExplanation("Barrack " + thisBarrack.getPosition() + " changed name to " + thisBarrack.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of barrack");
        }
    }
}

