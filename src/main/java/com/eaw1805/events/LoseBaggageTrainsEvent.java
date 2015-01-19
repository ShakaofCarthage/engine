package com.eaw1805.events;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements Lose Baggage Trains Events.
 */
public class LoseBaggageTrainsEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(LoseBaggageTrainsEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public LoseBaggageTrainsEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("LoseBaggageTrainsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        // Iterate through all baggage trains
        List<BaggageTrain> listBT = BaggageTrainManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        for (final BaggageTrain baggageTrain : listBT) {
            // examine condition of baggage train
            if (baggageTrain.getCondition() < 50) {
                // a baggage train with condition of 15% has a 35% chance of being lost.
                final int randNum = getRandomGen().nextInt(100) + 1;
                if (randNum < 50 - baggageTrain.getCondition()) {
                    report(baggageTrain.getNation(), "baggagetrain.loss", "Baggage Train [" + baggageTrain.getName() + "] was lost due to low condition");

                    LOGGER.info("Baggage Train [" + baggageTrain.getName() + "] was lost due to low condition");

                    // All loaded goods are lost. All loaded armies are automatically unloaded.
                    unloadUnits(baggageTrain);

                    // Remove train  from game
                    BaggageTrainManager.getInstance().delete(baggageTrain);
                }
            }
        }

        LOGGER.info("LoseBaggageTrainsEvent processed.");
    }

    /**
     * Unload all loaded units.
     *
     * @param thisTrain the train to unload.
     */
    public void unloadUnits(final BaggageTrain thisTrain) {
        // Locate sector
        final Sector thisSector = SectorManager.getInstance().getByPosition(thisTrain.getPosition());

        // keep track of units unloaded
        final List<Integer> slotsRemoved = new ArrayList<Integer>();

        // Check if a unit is loaded in the baggage train
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.SPY * 1000) {
                    // A spy is loaded
                    final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());

                    LOGGER.info(thisTrain.getNation().getName() +
                            " - baggage train [" + thisTrain.getName() + "] at " +
                            thisTrain.getPosition() +
                            " unloads SPY " +
                            thisSpy.getName() + "[" + thisSpy.getSpyId() + "/" + thisSpy.getName() + "]");

                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);

                    thisSpy.setCarrierInfo(thisCarrying);
                    SpyManager.getInstance().update(thisSpy);

                    slotsRemoved.add(entry.getKey());

                } else if (entry.getKey() >= ArmyConstants.COMMANDER * 1000) {
                    // A commander is loaded
                    final Commander thisCommander = CommanderManager.getInstance().getByID(entry.getValue());

                    LOGGER.info(thisTrain.getNation().getName() +
                            " - baggage train [" + thisTrain.getName() + "] at " +
                            thisTrain.getPosition() +
                            " unloads COMMANDER " +
                            thisCommander.getName() + "[" + thisCommander.getId() + "/" + thisCommander.getName() + "]");

                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);

                    thisCommander.setCarrierInfo(thisCarrying);

                    slotsRemoved.add(entry.getKey());

                    CommanderManager.getInstance().update(thisCommander);

                } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                    // A Brigade is loaded
                    final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());

                    LOGGER.info(thisTrain.getNation().getName() +
                            " - baggage train [" + thisTrain.getName() + "] at " +
                            thisTrain.getPosition() +
                            " unloads BRIGADE " +
                            thisBrigade.getName() + "[" + thisBrigade.getBrigadeId() + "/" + thisBrigade.getName() + "]");

                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);

                    // update info in each battalion of the brigade
                    for (final Battalion battalion : thisBrigade.getBattalions()) {
                        battalion.setCarrierInfo(thisCarrying);
                        BattalionManager.getInstance().update(battalion);
                    }

                    BrigadeManager.getInstance().update(thisBrigade);
                    slotsRemoved.add(entry.getKey());
                }
            }
        }

        // Remove unloaded slots
        for (final int slot : slotsRemoved) {
            thisTrain.getStoredGoods().remove(slot);
        }
    }

}
