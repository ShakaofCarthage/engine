package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Rename Brigade.
 * ticket:40
 */
public class RenameBrigade
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameBrigade.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_BRIG;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameBrigade(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameBrigade instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int brigadeId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the brigade
        final Brigade thisBrigade = BrigadeManager.getInstance().getByID(brigadeId);

        if (thisBrigade == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("brigade not found");

            // Check ownership of brigade
        } else if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
            thisBrigade.setName(getOrder().getParameter2());
            BrigadeManager.getInstance().update(thisBrigade);

            getOrder().setResult(1);
            getOrder().setExplanation("brigade " + brigadeId + " changed name to " + thisBrigade.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of brigade");
        }
    }
}
