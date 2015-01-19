package com.eaw1805.orders.economy;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order for Baggage Train Construction.
 */
public class BuildBaggageTrain
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BuildBaggageTrain.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_B_BTRAIN;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BuildBaggageTrain(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("BuildBaggageTrain instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int sectorId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the sector that we wish to build the ship
        final Sector thisSector = SectorManager.getInstance().getByID(sectorId);

        // Update order's region of effect
        getOrder().setRegion(thisSector.getPosition().getRegion());

        // Make sure that the owner is the same
        if (thisSector.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();
            final int regionId = thisSector.getPosition().getRegion().getId();
            final String trainName = getOrder().getParameter3();

            // make sure a shipyard is available
            if (thisSector.hasBarrack()) {

                // Make sure that enough money are available
                if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= 300000) {

                    // Make sure that enough citizens are available
                    if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= 1000) {

                        // Make sure that enough Horses are available
                        if (getParent().getTotGoods(ownerId, regionId, GOOD_HORSE) >= 2000) {

                            // Make sure that enough Wood are available
                            if (getParent().getTotGoods(ownerId, regionId, GOOD_WOOD) >= 1500) {

                                // Make sure that enough Industrial Points are available
                                if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= 500) {

                                    // Reduce materials
                                    getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, 300000);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, 1000);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_HORSE, 2000);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_WOOD, 1500);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_INPT, 500);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(GOOD_MONEY, 300000);
                                    usedGoods.put(GOOD_WOOD, 1500);
                                    usedGoods.put(GOOD_PEOPLE, 1000);
                                    usedGoods.put(GOOD_HORSE, 2000);
                                    usedGoods.put(GOOD_INPT, 500);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    // Create new baggage train
                                    final BaggageTrain newBTrain = new BaggageTrain();
                                    newBTrain.setNation(getOrder().getNation());
                                    newBTrain.setName(trainName);
                                    newBTrain.setPosition(thisSector.getPosition());
                                    newBTrain.setCondition(100);

                                    final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>();
                                    for (int goodID = GOOD_FIRST; goodID <= GOOD_COLONIAL; goodID++) {
                                        qteGoods.put(goodID, 0);
                                    }
                                    newBTrain.setStoredGoods(qteGoods);

                                    BaggageTrainManager.getInstance().add(newBTrain);

                                    getOrder().setResult(1);
                                    getOrder().setExplanation("new baggage train [" + trainName + "] built at " + thisSector.getPosition().toString());

                                } else {
                                    getOrder().setResult(-7);
                                    getOrder().setExplanation("not enough industrial points at regional warehouse");
                                }

                            } else {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("not enough wood at regional warehouse");
                            }

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("not enough horses at regional warehouse");
                        }

                    } else {
                        getOrder().setResult(-3);
                        getOrder().setExplanation("not enough citizens at regional warehouse");
                    }

                } else {
                    getOrder().setResult(-4);
                    getOrder().setExplanation("not enough money at empire warehouse");
                }

            } else {
                getOrder().setResult(-5);
                getOrder().setExplanation("sector does not support a barrack");
            }

        } else {
            getOrder().setResult(-6);
            getOrder().setExplanation("not owner of sector");
        }
    }
}
