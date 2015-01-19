package com.eaw1805.orders.economy;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order for Removing Baggage Trains.
 */
public class ScuttleBaggageTrain
        extends AbstractOrderProcessor
        implements GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ScuttleBaggageTrain.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_SCUTTLE_BTRAIN;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ScuttleBaggageTrain(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ScuttleBaggageTrain instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int trainId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the train that we wish to destroy
        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(trainId);

        if (thisTrain == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("cannot locate subject of order");

        } else {

            // Check ownership of train
            if (thisTrain.getNation().getId() == getOrder().getNation().getId()) {
                final int ownerId = getOrder().getNation().getId();
                final int regionId = thisTrain.getPosition().getRegion().getId();

                // Calculate amount of Wood that will be recovered from the destruction
                final int wood = (int) (1500 * thisTrain.getCondition() / 200d);

                // Calculate amount of Horses that will be recovered from the destruction
                final int horses = (int) (2000 * thisTrain.getCondition() / 200d);

                // Add to the corresponding regional warehouse
                getParent().incTotGoods(ownerId, regionId, GOOD_WOOD, wood);
                getParent().incTotGoods(ownerId, regionId, GOOD_HORSE, horses);

                // Update goods used by order
                final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                usedGoods.put(GOOD_WOOD, wood * -1);
                usedGoods.put(GOOD_HORSE, horses * -1);
                getOrder().setUsedGoodsQnt(usedGoods);

                // Update order's region of effect
                getOrder().setRegion(thisTrain.getPosition().getRegion());

                getOrder().setResult(1);
                getOrder().setExplanation("baggage train " + thisTrain.getName() + " scuttled - " + wood + " Wood, " + horses + " Horses salvaged.");

                thisTrain.initializeVariables();

                // Check if the entity is carrying units, and update their position too
                if (thisTrain.getHasCommander() || thisTrain.getHasSpy() || thisTrain.getHasTroops()) {
                    // Unload entities
                    destroyLoadedUnits(thisTrain);
                }

                // remove ship
                BaggageTrainManager.getInstance().delete(thisTrain);

            } else {
                getOrder().setResult(-1);
                getOrder().setExplanation("not owner of train");
            }
        }
    }
}