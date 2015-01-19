package com.eaw1805.battles.field.processors.melee;

import com.eaw1805.battles.field.generation.calculators.RoadCalculator;
import com.eaw1805.battles.field.orders.OrderUtils;
import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Melee processor for the "Build pontoon bridge order".
 *
 * @author fragkakis
 */
public class BuildPontoonBridgeMeleeProcessor extends BasePioneerOrderMeleeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildPontoonBridgeMeleeProcessor.class);

    public BuildPontoonBridgeMeleeProcessor(MeleeProcessor meleeProcessor) {
        super(meleeProcessor);
    }

    @Override
    public void performOrderMeleeAction(Brigade brigade, Order order) {
        if (OrderUtils.lastCheckpointReached(order)) {
            // presumably we are next to the river
            int ourSide = meleeProcessor.getParent().findSide(brigade);
            FieldBattleSector currentLocation = meleeProcessor.getParent().getSector(brigade);
            FieldBattleSector forwardSector = MapUtils.findSectorForward(currentLocation, ourSide);
            if ((forwardSector.isMinorRiver() || forwardSector.isMajorRiver())
                    && !forwardSector.hasSectorBridge()) {

                double constructionPointsAdded = calculatePioneerHitPoints(brigade);

                int previousConstructionCounter = order.getConstructionCounter() == null ? 0 : order.getConstructionCounter();
                int currentConstructionCounter = previousConstructionCounter + (int) constructionPointsAdded;

                order.setConstructionCounter(currentConstructionCounter);
                LOGGER.debug("A pontoon bridge is being erected in {}. Current construction points {} (was {})",
                        new Object[]{forwardSector, currentConstructionCounter, previousConstructionCounter});

                meleeProcessor.getParent().getFieldBattleLog().logStructureAffected(forwardSector.getX(), forwardSector.getY(),
                        "bridge", brigade, forwardSector.getBridge(), (int) constructionPointsAdded);

                if (RoadCalculator.DEFAULT_BRIDGE_HIT_POINTS <= currentConstructionCounter) {
                    // the bridge has been built!
                    forwardSector.setBridge(RoadCalculator.DEFAULT_BRIDGE_HIT_POINTS);
                    // reset construction counter in case bridge is destroyed in the future, or if this is a major river
                    // in which case other bridge tiles need to be built
                    order.setConstructionCounter(0);
                    LOGGER.debug("A pontoon bridge has been erected in {}", forwardSector);
                }
            }
        }
    }

}
