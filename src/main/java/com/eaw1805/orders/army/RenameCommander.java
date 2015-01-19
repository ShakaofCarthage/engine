package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Rename Commander.
 * ticket:40.
 */
public class RenameCommander
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameCommander.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_COMM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameCommander(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameCommander instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int commId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the commander
        final Commander thisComm = CommanderManager.getInstance().getByID(commId);

        if (thisComm == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("commander not found");

        } else if (thisComm.getDead()) {
            getOrder().setResult(-3);
            getOrder().setExplanation("commander is dead");

            // Check ownership of commander
        } else if (thisComm.getNation().getId() == getOrder().getNation().getId()) {
            thisComm.setName(getOrder().getParameter2());
            CommanderManager.getInstance().update(thisComm);

            getOrder().setResult(1);
            getOrder().setExplanation("commander " + commId + " changed name to " + thisComm.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of commander");
        }
    }
}
