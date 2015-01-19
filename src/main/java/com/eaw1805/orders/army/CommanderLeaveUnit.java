package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Remove commander from army or corps.
 */
public class CommanderLeaveUnit
 extends CommanderJoinArmy
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CommanderLeaveUnit.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_LEAVE_COM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CommanderLeaveUnit(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("CommanderLeaveUnit instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int commId = Integer.parseInt(getOrder().getParameter2());

        // Retrieve the commander
        final Commander thisComm = CommanderManager.getInstance().getByID(commId);

        if (thisComm == null) {
            getOrder().setResult(-5);
            getOrder().setExplanation("cannot locate subject of order");

        } else if (thisComm.getDead()) {
            getOrder().setResult(-4);
            getOrder().setExplanation("commander is dead");

        } else if (thisComm.getNation().getId() == getOrder().getNation().getId()) {
            // Remove commander from army or corps
            removeCommander(thisComm);

            getOrder().setResult(1);
            getOrder().setExplanation("Commander removed from command.");

        } else {
            getOrder().setResult(-3);
            getOrder().setExplanation("not owner of commander");
        }
    }
}
