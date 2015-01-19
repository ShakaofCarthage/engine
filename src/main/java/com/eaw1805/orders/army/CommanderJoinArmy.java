package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.MilitaryCalculators;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Commander join Army.
 * ticket:32.
 */
public class CommanderJoinArmy
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CommanderJoinArmy.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_ARMY_COM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CommanderJoinArmy(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("CommanderJoinArmy instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int commId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the commander
        final Commander thisComm = CommanderManager.getInstance().getByID(commId);

        if (thisComm == null) {
            getOrder().setResult(-5);
            getOrder().setExplanation("cannot locate subject of order");

        } else if (thisComm.getDead()) {
            getOrder().setResult(-4);
            getOrder().setExplanation("commander is dead");

        } else if (thisComm.getNation().getId() == getOrder().getNation().getId()) {
            // Check ownership of commander
            final int armyId = Integer.parseInt(getOrder().getParameter2());

            if (armyId == 0) {
                // Remove from army
                if (thisComm.getArmy() == 0) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("commander not leading an army");

                } else {
                    removeCommander(thisComm);
                    getOrder().setResult(1);
                    getOrder().setExplanation("commander " + thisComm.getName() + " removed from army");
                }

            } else {
                final Army thisArmy = ArmyManager.getInstance().getByID(armyId);
                if (thisArmy == null) {
                    // Check if this is a newly created army
                    if (getParent().armyAssocExists(armyId)) {
                        // remove commander from army or corp
                        removeCommander(thisComm);

                        // this is a new army
                        thisComm.setArmy(getParent().retrieveArmyAssoc(armyId));
                        CommanderManager.getInstance().update(thisComm);

                        final Army thatArmy = ArmyManager.getInstance().getByID(getParent().retrieveArmyAssoc(armyId));
                        thatArmy.setCommander(thisComm);
                        ArmyManager.getInstance().update(thatArmy);

                        // check if this commander arrived from the pool
                        if (thisComm.getPool()) {
                            // if it is in the same continent then automatic
                            if (thisComm.getPosition().getRegion().getId() == thatArmy.getPosition().getRegion().getId()
                                    || getParent().getGame().isFastAppointmentOfCommanders()) {
                                thisComm.setPool(false);
                                thisComm.setInTransit(false);
                                thisComm.setPosition((Position) thatArmy.getPosition().clone());

                            } else {
                                thisComm.setPool(true);
                                thisComm.setInTransit(true);
                                thisComm.setTransit(MilitaryCalculators.getTransitDistance(thisComm.getPosition().getRegion().getId(), thatArmy.getPosition().getRegion().getId()));
                            }
                            CommanderManager.getInstance().update(thisComm);

                        } else {
                            // set position of commander to match army
                            thisComm.setPosition((Position) thatArmy.getPosition().clone());
                            CommanderManager.getInstance().update(thisComm);
                        }

                        getOrder().setResult(1);
                        getOrder().setExplanation("commander " + thisComm.getName() + " joined newly created army");
                    }

                } else {
                    // check ownership of army
                    if (thisArmy.getNation().getId() == thisComm.getNation().getId()) {

                        // Check location of army
                        if (thisComm.getPosition().equals(thisArmy.getPosition()) || thisComm.getPool()) {
                            // remove commander
                            removeCommander(thisComm);

                            // remove previous commander (if any)
                            clearArmy(thisArmy);

                            // update commander's position
                            thisComm.setArmy(armyId);
                            CommanderManager.getInstance().update(thisComm);

                            // update army's position
                            thisArmy.setCommander(thisComm);
                            ArmyManager.getInstance().update(thisArmy);

                            // check if this commander arrived from the pool
                            if (thisComm.getPool()) {
                                // if it is in the same continent then automatic
                                if (thisComm.getPosition().getRegion().getId() == thisArmy.getPosition().getRegion().getId()
                                        || getParent().getGame().isFastAppointmentOfCommanders()) {
                                    thisComm.setPool(false);
                                    thisComm.setInTransit(false);
                                    thisComm.setTransit(0);
                                    thisComm.setPosition((Position) thisArmy.getPosition().clone());

                                } else {
                                    thisComm.setPool(true);
                                    thisComm.setInTransit(true);
                                    thisComm.setTransit(MilitaryCalculators.getTransitDistance(thisComm.getPosition().getRegion().getId(), thisArmy.getPosition().getRegion().getId()));
                                }
                                CommanderManager.getInstance().update(thisComm);

                            } else {
                                // set position of commander to match army
                                thisComm.setPosition((Position) thisArmy.getPosition().clone());
                                CommanderManager.getInstance().update(thisComm);
                            }

                            getOrder().setResult(1);
                            getOrder().setExplanation("commander " + thisComm.getName() + " joined army " + thisArmy.getName());

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
            getOrder().setExplanation("not owner of commander");
        }
    }

    /**
     * Remove commanders from army.
     *
     * @param thisArmy the army object.
     */
    protected void clearArmy(final Army thisArmy) {
        // check if commander exists
        if (thisArmy.getCommander() != null) {
            // update commander
            thisArmy.getCommander().setArmy(0);
            CommanderManager.getInstance().update(thisArmy.getCommander());

            // remove commander
            thisArmy.setCommander(null);

            // update entity
            ArmyManager.getInstance().update(thisArmy);
        }
    }

    /**
     * Remove commanders from corps.
     *
     * @param thisCorp the corps object.
     */
    protected void clearCorp(final Corp thisCorp) {
        // check if commander exists
        if (thisCorp.getCommander() != null) {
            // update commander
            thisCorp.getCommander().setCorp(0);
            CommanderManager.getInstance().update(thisCorp.getCommander());

            // remove commander
            thisCorp.setCommander(null);

            // update entity
            CorpManager.getInstance().update(thisCorp);
        }
    }

}

