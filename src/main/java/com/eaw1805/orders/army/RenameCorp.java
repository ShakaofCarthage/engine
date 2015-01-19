package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Rename Corp.
 */
public class RenameCorp
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RenameCorp.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_REN_CORP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RenameCorp(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RenameCorp instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int corpId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the Corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("corp not found");

            // Check ownership of Corp
        } else if (thisCorp.getNation().getId() == getOrder().getNation().getId()) {

            thisCorp.setName(getOrder().getParameter2());
            CorpManager.getInstance().update(thisCorp);

            getOrder().setResult(1);
            getOrder().setExplanation("corps " + corpId + " changed name to " + thisCorp.getName());

        } else {
            getOrder().setResult(-1);
            getOrder().setExplanation("not owner of corps");
        }
    }

}
