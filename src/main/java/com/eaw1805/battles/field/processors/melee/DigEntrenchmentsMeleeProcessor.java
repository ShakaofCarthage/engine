package com.eaw1805.battles.field.processors.melee;

import com.eaw1805.battles.field.orders.OrderUtils;
import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Melee processor for the "Dig entrenchments order".
 *
 * @author fragkakis
 */
public class DigEntrenchmentsMeleeProcessor extends BasePioneerOrderMeleeProcessor {

    public static final int ENTRENCHMENT_HIT_POINTS = 300;
    private static final Logger LOGGER = LoggerFactory.getLogger(DigEntrenchmentsMeleeProcessor.class);

    public DigEntrenchmentsMeleeProcessor(MeleeProcessor meleeProcessor) {
        super(meleeProcessor);
    }

    @Override
    protected void performOrderMeleeAction(Brigade brigade, Order order) {
        if (OrderUtils.lastCheckpointReached(order)) {
            FieldBattleSector currentLocation = meleeProcessor.getParent().getSector(brigade);
            if (currentLocation.isBuildable()) {

                double constructionPointsAdded = calculatePioneerHitPoints(brigade);

                int previousConstructionCounter = order.getConstructionCounter() == null ? 0 : order.getConstructionCounter();
                int currentConstructionCounter = previousConstructionCounter + (int) constructionPointsAdded;

                order.setConstructionCounter(currentConstructionCounter);
                LOGGER.debug("An entrenchment is being dug in {}. Current construction points {} (was {})",
                        new Object[]{currentLocation, currentConstructionCounter, previousConstructionCounter});

                meleeProcessor.getParent().getFieldBattleLog().logStructureAffected(currentLocation.getX(), currentLocation.getY(),
                        "entrenchment", brigade, currentLocation.getEntrenchment(), (int) constructionPointsAdded);

                if (ENTRENCHMENT_HIT_POINTS <= currentConstructionCounter) {
                    // the entrenchment has been dug!
                    currentLocation.setBridge(ENTRENCHMENT_HIT_POINTS);
                    // reset construction counter in case entrenchment is destroyed in the future
                    order.setConstructionCounter(0);
                    LOGGER.debug("An entrenchment has been dug in {}", currentLocation);
                }
            }
        }
    }

}
