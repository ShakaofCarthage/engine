package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Order - Corp join Army.
 */
public class CorpJoinArmy
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CorpJoinArmy.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_ADDTO_ARMY;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CorpJoinArmy(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("CorpJoinArmy instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int corpId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp == null) {
            // this is a newly formed corp
            // Check if this is a newly created corp
            if (getParent().corpAssocExists(corpId)) {
                // this is a new Corp
                final Corp thatCorp = CorpManager.getInstance().getByID(getParent().retrieveCorpAssoc(corpId));
                int armyId = Integer.parseInt(getOrder().getParameter2());
                if (armyId == 0) {
                    if (thatCorp.getArmy() == 0) {
                        getOrder().setResult(-61);
                        getOrder().setExplanation("corp not part of an army");
                        return;

                    } else {
                        final Army thisArmy = ArmyManager.getInstance().getByID(thatCorp.getArmy());
                        if (thisArmy == null) {
                            thatCorp.setArmy(null);
                            CorpManager.getInstance().update(thisCorp);

                            getOrder().setResult(1);
                            getOrder().setExplanation("corps " + thatCorp.getName() + " removed from army");
                            return;

                        } else {
                            thatCorp.setArmy(null);
                            CorpManager.getInstance().update(thisCorp);

                            getOrder().setResult(1);
                            getOrder().setExplanation("corps " + thatCorp.getName() + " removed from army " + thisArmy.getName());
                            return;
                        }
                    }
                } else if (getParent().armyAssocExists(armyId)) {
                    armyId = getParent().retrieveArmyAssoc(armyId);

                } else {
                    final Army thisArmy = ArmyManager.getInstance().getByID(armyId);
                    if (thisArmy == null) {
                        getOrder().setResult(-5);
                        getOrder().setExplanation("cannot find army");
                        return;
                    }
                }

                final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getOrder().getGame(), armyId);
                if (lstCorps.size() >= 5) {
                    getOrder().setResult(-5);
                    getOrder().setExplanation("army reached maximum number of corps");

                } else {

                    thatCorp.setArmy(armyId);
                    CorpManager.getInstance().update(thatCorp);

                    getOrder().setResult(1);
                    getOrder().setExplanation("corps " + thatCorp.getName() + " joined army " + armyId);
                }
            }

        } else {

            // Check ownership of corp
            if (thisCorp.getNation().getId() == getOrder().getNation().getId()) {
                final int armyId = Integer.parseInt(getOrder().getParameter2());

                if (armyId == 0) {
                    if (thisCorp.getArmy() == null || thisCorp.getArmy() == 0) {
                        getOrder().setResult(-4);
                        getOrder().setExplanation("corps not part of an army");

                    } else {
                        final Army thisArmy = ArmyManager.getInstance().getByID(thisCorp.getArmy());
                        if (thisArmy == null) {
                            thisCorp.setArmy(null);
                            CorpManager.getInstance().update(thisCorp);

                            getOrder().setResult(1);
                            getOrder().setExplanation("corps " + thisCorp.getName() + " removed from army");

                        } else {
                            thisCorp.setArmy(null);
                            CorpManager.getInstance().update(thisCorp);

                            getOrder().setResult(1);
                            getOrder().setExplanation("corps " + thisCorp.getName() + " removed from army " + thisArmy.getName());
                        }
                    }

                } else {
                    final Army thisArmy = ArmyManager.getInstance().getByID(armyId);
                    if (thisArmy == null) {

                        // Check if this is a newly created army
                        if (getParent().armyAssocExists(armyId)) {
                            // this is a new army
                            final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getOrder().getGame(), armyId);
                            if (lstCorps.size() >= 5) {
                                getOrder().setResult(-5);
                                getOrder().setExplanation("army reached maximum number of corps");

                            } else {

                                thisCorp.setArmy(getParent().retrieveArmyAssoc(armyId));
                                CorpManager.getInstance().update(thisCorp);

                                getOrder().setResult(1);
                                getOrder().setExplanation("corps " + thisCorp.getName() + " joined newly created army");
                            }
                        }

                    } else {

                        // check ownership of army
                        if (thisArmy.getNation().getId() == thisCorp.getNation().getId()) {
                            // Check location of army
                            if (thisCorp.getPosition().equals(thisArmy.getPosition())) {

                                final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getOrder().getGame(), armyId);
                                if (lstCorps.size() >= 5) {
                                    getOrder().setResult(-5);
                                    getOrder().setExplanation("army reached maximum number of corps");

                                } else {

                                    // Located at the same sector
                                    thisCorp.setArmy(armyId);
                                    CorpManager.getInstance().update(thisCorp);

                                    getOrder().setResult(1);
                                    getOrder().setExplanation("corps " + thisCorp.getName() + " joined army " + thisArmy.getName());
                                }
                            } else {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("army located at a different sector");
                            }

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("not owner of army");
                        }
                    }
                }

            } else {
                getOrder().setResult(-3);
                getOrder().setExplanation("not owner of corps");
            }
        }
    }
}
