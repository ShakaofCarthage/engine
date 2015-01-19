package com.eaw1805.orders.army;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Order - Dismiss Commander.
 * Ticket: 736.
 */
public class DismissCommander
        extends CommanderJoinArmy
        implements GoodConstants, RegionConstants, ArmyConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DismissCommander.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_DISS_COM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DismissCommander(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DismissCommander instantiated.");
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

        } else if (thisComm.getPool()) {
            getOrder().setResult(-2);
            getOrder().setExplanation("commander is already in the pool");

        } else if (thisComm.getNation().getId() == getOrder().getNation().getId()) {
            // Remove from any Corp or Army
            removeCommander(thisComm);

            if (thisComm.getCarrierInfo().getCarrierType() != 0) {
                // Remove from vessel
                switch (thisComm.getCarrierInfo().getCarrierType()) {
                    case FLEET:
                    case SHIP:
                        unloadCommanderFromShip(thisComm);
                        break;

                    case BAGGAGETRAIN:
                        unloadCommanderFromTrain(thisComm);
                        break;
                }

                // remove info from commander unit
                final CarrierInfo thisCarrying = new CarrierInfo();
                thisCarrying.setCarrierType(0);
                thisCarrying.setCarrierId(0);
                thisComm.setCarrierInfo(thisCarrying);
            }

            // if commander is captured, then simply remove from game
            if (thisComm.getCaptured().getId() != thisComm.getNation().getId()) {
                // Remove commander from play
                thisComm.setDead(true);

                // Report addition
                newsPair(thisComm.getNation(), thisComm.getCaptured(),
                        NEWS_MILITARY,
                        "The high command of our army decided that we are no longer interested in the fate of our commander " + thisComm.getName() + " captured by " + thisComm.getCaptured().getName(),
                        thisComm.getNation().getName() + " is no longer interested in the fate of their commander " + thisComm.getName() + ". We gave orders to hang the captured commander.");

            } else {

                // Move to Pool
                thisComm.setPool(true);
                thisComm.setInTransit(false);
                thisComm.setTransit(0);
            }

            CommanderManager.getInstance().update(thisComm);

            getOrder().setResult(1);
            getOrder().setExplanation("commander " + thisComm.getName() + " dismissed to pool");

        } else {
            getOrder().setResult(-3);
            getOrder().setExplanation("not owner of commander");
        }
    }

    /**
     * Unload a commander from a ship.
     *
     * @param thisCommander the commander to unload.
     */
    private void unloadCommanderFromShip(final Commander thisCommander) {
        // unload commander from ship
        final Ship thisShip = ShipManager.getInstance().getByID(thisCommander.getCarrierInfo().getCarrierId());
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.COMMANDER * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            if (storedGoods.get(thisSlot) == thisCommander.getId()) {
                break;
            }

            thisSlot++;
        }

        storedGoods.remove(thisSlot);
        ShipManager.getInstance().update(thisShip);
    }

    /**
     * Unload a commander from a baggage train.
     *
     * @param thisCommander the commander to unload.
     */
    private void unloadCommanderFromTrain(final Commander thisCommander) {
        // unload commander from baggage train
        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(thisCommander.getCarrierInfo().getCarrierId());
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.COMMANDER * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            if (storedGoods.get(thisSlot) == thisCommander.getId()) {
                break;
            }

            thisSlot++;
        }

        storedGoods.remove(thisSlot);
        BaggageTrainManager.getInstance().update(thisTrain);
    }

}
