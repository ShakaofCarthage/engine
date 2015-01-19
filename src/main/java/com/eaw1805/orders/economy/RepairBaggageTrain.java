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
 * Order - Repair Ship.
 * ticket:20.
 */
public class RepairBaggageTrain
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RepairBaggageTrain.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_R_BTRAIN;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RepairBaggageTrain(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("RepairBaggageTrain instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int trainId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Ship
        final BaggageTrain thisBTrain = BaggageTrainManager.getInstance().getByID(trainId);

        // Check ownership of ship
        if (thisBTrain.getNation().getId() == getOrder().getNation().getId()) {
            // Retrieve the sector where we wish to repair the ship
            final Sector thisSector = SectorManager.getInstance().getByPosition(thisBTrain.getPosition());

            // Update order's region of effect
            getOrder().setRegion(thisSector.getPosition().getRegion());

            final int ownerId = getOrder().getNation().getId();
            final int regionId = thisSector.getPosition().getRegion().getId();

            // Make sure that it can be built at the colonies
            if (thisSector.hasBarrack()) {
                final double modifier = (100d - thisBTrain.getCondition()) / 100d;
                final int reqMoney = (int) (300000 * modifier);
                final int reqHorse = (int) (2000 * modifier);
                final int reqWood = (int) (1000 * modifier);
                final int reqCitizens = (int) (1000 * modifier);
                final int reqIndPt = (int) (500 * modifier);

                // Make sure that enough money are available
                if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= reqMoney) {

                    // Make sure that enough citizens are available
                    if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= reqCitizens) {

                        // Make sure that enough Industrial Points are available
                        if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= reqHorse) {

                            // Make sure that enough Wood are available
                            if (getParent().getTotGoods(ownerId, regionId, GOOD_WOOD) >= reqWood) {

                                // Make sure that enough Industrial Points are available
                                if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= reqIndPt) {

                                    // Reduce materials
                                    getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, reqMoney);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, reqCitizens);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_HORSE, reqHorse);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_WOOD, reqWood);
                                    getParent().decTotGoods(ownerId, regionId, GOOD_INPT, reqIndPt);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(GOOD_MONEY, reqMoney);
                                    usedGoods.put(GOOD_WOOD, reqWood);
                                    usedGoods.put(GOOD_PEOPLE, reqCitizens);
                                    usedGoods.put(GOOD_HORSE, reqHorse);
                                    usedGoods.put(GOOD_INPT, reqIndPt);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    // Repair ship
                                    thisBTrain.setCondition(100);
                                    BaggageTrainManager.getInstance().update(thisBTrain);

                                    // Report success
                                    getOrder().setResult(1);
                                    getOrder().setExplanation("repaired baggage train " + thisBTrain.getName());

                                } else {
                                    getOrder().setResult(-8);
                                    getOrder().setExplanation("not enough industrial points at regional warehouse");
                                }

                            } else {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not enough wood at regional warehouse");
                            }

                        } else {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("not enough horses at regional warehouse");
                        }

                    } else {
                        getOrder().setResult(-4);
                        getOrder().setExplanation("not enough citizens at regional warehouse");
                    }

                } else {
                    getOrder().setResult(-5);
                    getOrder().setExplanation("not enough money at empire warehouse");
                }

            } else {
                getOrder().setResult(-6);
                getOrder().setExplanation("sector does not have a barrack");
            }

        } else {
            getOrder().setResult(-7);
            getOrder().setExplanation("not owner of sector");
        }
    }
}

