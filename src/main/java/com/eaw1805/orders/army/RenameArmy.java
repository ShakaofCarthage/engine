package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Rename Army.
 */
public class RenameArmy
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameArmy.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_ARMY;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameArmy(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameArmy instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int armyId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the Army
        final Army thisArmy = ArmyManager.getInstance().getByID(armyId);

        if (thisArmy == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("army not found");

            // Check ownership of Army
        } else if (thisArmy.getNation().getId() == getOrder().getNation().getId()) {
            thisArmy.setName(getOrder().getParameter2());
            ArmyManager.getInstance().update(thisArmy);

            getOrder().setResult(1);
            getOrder().setExplanation("army " + armyId + " changed name to " + thisArmy.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of army");
        }
    }
}
