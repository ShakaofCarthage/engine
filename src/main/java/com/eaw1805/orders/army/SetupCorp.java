package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.MilitaryCalculators;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Setup Corp.
 */
public class SetupCorp
        extends AbstractOrderProcessor
        implements GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CorpJoinArmy.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_B_CORP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public SetupCorp(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("SetupCorp instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int tmpCorpId = Integer.parseInt(getOrder().getParameter1());
        final String corpName = getOrder().getParameter3();
        final int posX = Integer.parseInt(getOrder().getParameter4());
        final int posY = Integer.parseInt(getOrder().getParameter5());
        final int posRegion = Integer.parseInt(getOrder().getParameter6());

        // Create position for new Corp
        final Position thisPosition = new Position();
        thisPosition.setGame(getOrder().getGame());
        thisPosition.setRegion(RegionManager.getInstance().getByID(posRegion));
        thisPosition.setX(posX);
        thisPosition.setY(posY);

        // Retrieve Commander
        final int commId = Integer.parseInt(getOrder().getParameter8());
        final Commander thisComm = CommanderManager.getInstance().getByID(commId);
        if (thisComm != null) {
            // Make sure that commander is in the same position with army
            // Except if the commander is in the pool
            if (!thisComm.getPosition().equals(thisPosition) && !thisComm.getPool()) {
                getOrder().setResult(-1);
                getOrder().setExplanation("commander is located at a different sector");
                return;
            }

            if (thisComm.getCorp() > 0) {
                // Remove commander from corp
                final Corp thisCorp = CorpManager.getInstance().getByID(thisComm.getCorp());
                if (thisCorp != null) {
                    thisCorp.setCommander(null);
                    CorpManager.getInstance().update(thisCorp);
                }

                thisComm.setCorp(0);
            }

            if (thisComm.getArmy() > 0) {
                // Remove commander from army
                final Army thisArmy = ArmyManager.getInstance().getByID(thisComm.getArmy());
                if (thisArmy != null) {
                    thisArmy.setCommander(null);
                    ArmyManager.getInstance().update(thisArmy);
                }

                thisComm.setArmy(0);
            }
        }

        // Create Corp
        final Corp newCorp = new Corp();
        newCorp.setName(corpName);
        newCorp.setPosition(thisPosition);
        newCorp.setMps(0);
        newCorp.setArmy(null);
        newCorp.setNation(getOrder().getNation());
        newCorp.setCommander(thisComm);

        // Add Corp
        CorpManager.getInstance().add(newCorp);

        // Update Commander
        if (thisComm != null) {
            thisComm.setCorp(newCorp.getCorpId());
            CommanderManager.getInstance().update(thisComm);

            // check if this commander arrived from the pool
            if (thisComm.getPool()) {
                // if it is in the same continent then automatic
                if (thisComm.getPosition().getRegion().getId() == newCorp.getPosition().getRegion().getId()
                        || getParent().getGame().isFastAppointmentOfCommanders()) {
                    thisComm.setPool(false);
                    thisComm.setInTransit(false);
                    thisComm.setTransit(0);
                } else {
                    thisComm.setPool(true);
                    thisComm.setInTransit(true);
                    thisComm.setTransit(MilitaryCalculators.getTransitDistance(thisComm.getPosition().getRegion().getId(), newCorp.getPosition().getRegion().getId()));
                }
                CommanderManager.getInstance().update(thisComm);

            } else {
                // set position of commander to match corps
                thisComm.setPosition((Position) newCorp.getPosition().clone());
                CommanderManager.getInstance().update(thisComm);
            }
        }

        getOrder().setResult(1);
        getOrder().setExplanation("New corps formed");

        // Associate ID assigned from DB with the one used by UI
        getParent().associateCorpId(newCorp.getCorpId(), tmpCorpId);
    }

}
