package com.eaw1805.battles.field.processors.melee;

import com.eaw1805.battles.field.orders.OrderUtils;
import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Melee processor for the "Move to destroy bridge".
 *
 * @author fragkakis
 */
public class MoveToDestroyBridgeMeleeProcessor extends BasePioneerOrderMeleeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveToDestroyBridgeMeleeProcessor.class);

    public MoveToDestroyBridgeMeleeProcessor(MeleeProcessor meleeProcessor) {
        super(meleeProcessor);
    }

    @Override
    protected void performOrderMeleeAction(Brigade brigade, Order order) {
        if (OrderUtils.lastCheckpointReached(order)) {
            // presumably we are next to the bridge to be destroyed
            int ourSide = findSide(brigade.getNation());
            FieldBattleSector currentLocation = meleeProcessor.getParent().getSector(brigade);
            FieldBattleSector forwardSector = MapUtils.findSectorForward(currentLocation, ourSide);
            if ((forwardSector.isMinorRiver() || forwardSector.isMajorRiver())
                    && forwardSector.hasSectorBridge()) {

                double hitPointsRemoved = calculatePioneerHitPoints(brigade);
                int previousHitPoints = forwardSector.getBridge();
                int currentHitpoints = previousHitPoints - (int) hitPointsRemoved;
                currentHitpoints = currentHitpoints > 0 ? currentHitpoints : 0;

                forwardSector.setBridge(currentHitpoints);

                meleeProcessor.getParent().getFieldBattleLog().logStructureAffected(forwardSector.getX(), forwardSector.getY(),
                        "bridge", brigade, currentLocation.getBridge(), (int) -hitPointsRemoved);

                if (currentHitpoints > 0) {
                    LOGGER.debug("A pontoon bridge is being destroyed in {}. Current hit points {} (was {})",
                            new Object[]{forwardSector, currentHitpoints, previousHitPoints});
                } else {
                    LOGGER.debug("The pontoon bridge has been destroyed in {}", forwardSector);
                }
            }
        }
    }

}
