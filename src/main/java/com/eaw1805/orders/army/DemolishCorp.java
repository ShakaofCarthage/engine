package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Order - Demolish Corp.
 */
public class DemolishCorp
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DemolishCorp.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_D_CORP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DemolishCorp(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DemolishCorp instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int corpId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);
        if (thisCorp != null) {

            // Check ownership
            if (thisCorp.getNation().getId() == getOrder().getNation().getId()) {

                // Retrieve the brigades of the corp
                final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), corpId);

                // Retrieve the commanders of the corp
                final List<Commander> lstComms = CommanderManager.getInstance().listByCorp(getParent().getGame(), corpId);


                for (final Commander thisComm : lstComms) {
                    thisComm.setArmy(0);
                    CommanderManager.getInstance().update(thisComm);
                }

                for (final Brigade thisBrigade : lstBrigades) {
                    thisBrigade.setCorp(0);
                    BrigadeManager.getInstance().update(thisBrigade);
                }

                getOrder().setResult(1);
                getOrder().setExplanation("demolished corps " + thisCorp.getName());

                CorpManager.getInstance().delete(thisCorp);

            } else {
                getOrder().setResult(-1);
                getOrder().setExplanation("not owner of corps");
            }
        } else {
            getOrder().setResult(-2);
            getOrder().setExplanation("corps does not exist");
        }
    }
}
