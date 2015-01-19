package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Place Brigade in a Corp.
 * ticket:32.
 */
public class BrigadeJoinCorp
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BrigadeJoinCorp.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_ADDTO_CORP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BrigadeJoinCorp(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("BrigadeJoinCorp instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int brigadeId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source brigade
        final Brigade thisBrigade = BrigadeManager.getInstance().getByID(brigadeId);

        if (thisBrigade == null) {
            getOrder().setResult(-3);
            getOrder().setExplanation("cannot locate brigade");

        } else {
            // Check ownership of source brigade
            if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
                final int corpId = Integer.parseInt(getOrder().getParameter2());

                if (corpId == 0) {
                    // Remove from corps
                    thisBrigade.setCorp(corpId);
                    BrigadeManager.getInstance().update(thisBrigade);

                    getOrder().setResult(1);
                    getOrder().setExplanation("brigade " + brigadeId + " removed from corps " + thisBrigade.getCorp());

                } else {

                    // Check if this is a newly created corp
                    if (getParent().corpAssocExists(corpId)) {
                        // this is a new Corp
                        thisBrigade.setCorp(getParent().retrieveCorpAssoc(corpId));
                        BrigadeManager.getInstance().update(thisBrigade);

                        getOrder().setResult(1);
                        getOrder().setExplanation("brigade " + thisBrigade.getName() + " joined newly created corps " + thisBrigade.getCorp());

                    } else {
                        // Retrieve corp
                        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

                        if (thisCorp == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("corp does not exist");

                        } else {
                            if (thisCorp.getPosition().equals(thisBrigade.getPosition())) {
                                thisBrigade.setCorp(corpId);
                                BrigadeManager.getInstance().update(thisBrigade);

                                getOrder().setResult(1);
                                getOrder().setExplanation("brigade " + thisBrigade.getName() + " joined corps " + thisCorp.getName());
                            } else {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("corp located at a different sector");
                            }
                        }
                    }
                }

            } else {
                getOrder().setResult(-2);
                getOrder().setExplanation("not owner of brigade");
            }
        }
    }
}
