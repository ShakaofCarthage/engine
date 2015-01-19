package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Order - Demolish Army.
 * ticket:32.
 */
public class DemolishArmy
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DemolishArmy.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_D_ARMY;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DemolishArmy(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DemolishArmy instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int armyId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Corp
        final Army thisArmy = ArmyManager.getInstance().getByID(armyId);
        if (thisArmy == null) {
            getOrder().setResult(-1);
            getOrder().setExplanation("army does not exist");
        } else {
            // Check ownership
            if (thisArmy.getNation().getId() == getOrder().getNation().getId()) {

                // Retrieve the corps of the army
                final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), armyId);

                // Retrieve the commanders of the army
                final List<Commander> lstComms = CommanderManager.getInstance().listByArmy(getParent().getGame(), armyId);

                for (final Commander thisComm : lstComms) {
                    thisComm.setArmy(0);
                    CommanderManager.getInstance().update(thisComm);
                }

                for (final Corp thisCorp : lstCorps) {
                    thisCorp.setArmy(null);
                    CorpManager.getInstance().update(thisCorp);
                }

                getOrder().setResult(1);
                getOrder().setExplanation("demolished army " + armyId);

                ArmyManager.getInstance().delete(thisArmy);

            } else {
                getOrder().setResult(-2);
                getOrder().setExplanation("not owner of army");
            }
        }
    }
}
