package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.MilitaryCalculators;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Commander join Corp.
 */
public class CommanderJoinCorp
        extends CommanderJoinArmy
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CommanderJoinCorp.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_CORP_COM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CommanderJoinCorp(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("CommanderJoinCorp instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int commId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source brigade
        final Commander thisComm = CommanderManager.getInstance().getByID(commId);

        if (thisComm == null) {
            getOrder().setResult(-5);
            getOrder().setExplanation("cannot locate subject of order");

        } else if (thisComm.getDead()) {
            getOrder().setResult(-4);
            getOrder().setExplanation("commander is dead");

        } else if (thisComm.getNation().getId() == getOrder().getNation().getId()) {
            // Check ownership of commander
            final int corpId = Integer.parseInt(getOrder().getParameter2());

            if (corpId == 0) {
                // Remove from Corp
                if (thisComm.getCorp() == 0) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("commander not leading a corps");

                } else {
                    removeCommander(thisComm);
                    getOrder().setResult(1);
                    getOrder().setExplanation("commander " + thisComm.getName() + " removed from corps");
                }

            } else {
                final Corp thisCorp = CorpManager.getInstance().getByID(corpId);
                if (thisCorp == null) {
                    // Check if this is a newly created army
                    if (getParent().corpAssocExists(corpId)) {
                        // remove commander from army or corp
                        removeCommander(thisComm);

                        // this is a new corps
                        thisComm.setCorp(getParent().retrieveCorpAssoc(corpId));
                        CommanderManager.getInstance().update(thisComm);

                        final Corp thatCorp = CorpManager.getInstance().getByID(getParent().retrieveCorpAssoc(corpId));
                        thatCorp.setCommander(thisComm);
                        CorpManager.getInstance().update(thatCorp);

                        // check if this commander arrived from the pool
                        if (thisComm.getPool()) {
                            // if it is in the same continent then automatic
                            if (thisComm.getPosition().getRegion().getId() == thatCorp.getPosition().getRegion().getId()
                                    || getParent().getGame().isFastAppointmentOfCommanders()) {
                                thisComm.setPool(false);
                                thisComm.setInTransit(false);
                                thisComm.setTransit(0);
                                thisComm.setPosition((Position) thisCorp.getPosition().clone());

                            } else {
                                thisComm.setPool(true);
                                thisComm.setInTransit(true);
                                thisComm.setTransit(MilitaryCalculators.getTransitDistance(thisComm.getPosition().getRegion().getId(), thatCorp.getPosition().getRegion().getId()));
                            }
                            CommanderManager.getInstance().update(thisComm);

                        } else {
                            // set position of commander to match corps
                            thisComm.setPosition((Position) thatCorp.getPosition().clone());
                            CommanderManager.getInstance().update(thisComm);
                        }

                        getOrder().setResult(1);
                        getOrder().setExplanation("commander " + thisComm.getName() + " joined newly created corps");
                    }

                } else {
                    // check ownership of corp
                    if (thisCorp.getNation().getId() == thisComm.getNation().getId()) {

                        // Check location of corp
                        if (thisComm.getPosition().equals(thisCorp.getPosition()) || thisComm.getPool()) {
                            // remove commander
                            removeCommander(thisComm);

                            // remove previous commander (if any)
                            clearCorp(thisCorp);

                            // check if this commander arrived from the pool
                            if (thisComm.getPool()) {
                                // if it is in the same continent then automatic
                                if (thisComm.getPosition().getRegion().getId() == thisCorp.getPosition().getRegion().getId()
                                        || getParent().getGame().isFastAppointmentOfCommanders()) {
                                    thisComm.setPool(false);
                                    thisComm.setInTransit(false);
                                    thisComm.setTransit(0);
                                    thisComm.setPosition((Position) thisCorp.getPosition().clone());

                                } else {
                                    thisComm.setPool(true);
                                    thisComm.setInTransit(true);
                                    thisComm.setTransit(MilitaryCalculators.getTransitDistance(thisComm.getPosition().getRegion().getId(), thisCorp.getPosition().getRegion().getId()));
                                }

                            } else {
                                // set position of commander to match corps
                                thisComm.setPosition((Position) thisCorp.getPosition().clone());
                                CommanderManager.getInstance().update(thisComm);
                            }

                            // Located at the same sector
                            thisComm.setCorp(corpId);
                            CommanderManager.getInstance().update(thisComm);

                            thisCorp.setCommander(thisComm);
                            CorpManager.getInstance().update(thisCorp);

                            getOrder().setResult(1);
                            getOrder().setExplanation("commander " + thisComm.getName() + " joined corps " + thisCorp.getName());

                        } else {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("corps located at a different sector");
                        }
                    } else {
                        getOrder().setResult(-2);
                        getOrder().setExplanation("not owner of corps");
                    }
                }
            }
        } else {
            getOrder().setResult(-3);
            getOrder().setExplanation("not owner of commander");
        }
    }

}
