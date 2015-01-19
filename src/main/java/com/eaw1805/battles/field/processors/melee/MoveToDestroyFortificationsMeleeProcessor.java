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
 * Melee processor for the "Move to destroy fortifications".
 *
 * @author fragkakis
 */
public class MoveToDestroyFortificationsMeleeProcessor extends BasePioneerOrderMeleeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveToDestroyFortificationsMeleeProcessor.class);

    public MoveToDestroyFortificationsMeleeProcessor(MeleeProcessor meleeProcessor) {
        super(meleeProcessor);
    }

    @Override
    protected void performOrderMeleeAction(Brigade brigade, Order order) {
        if (OrderUtils.lastCheckpointReached(order)) {
            // presumably we are next to the fortification to be destroyed
            int ourSide = findSide(brigade.getNation());
            FieldBattleSector currentLocation = meleeProcessor.getParent().getSector(brigade);
            FieldBattleSector forwardSector = MapUtils.findSectorForward(currentLocation, ourSide);
            if (forwardSector.hasSectorWall()) {

                double hitPointsRemoved = calculatePioneerHitPoints(brigade);
                int previousHitPoints = forwardSector.getWall();
                int currentHitpoints = previousHitPoints - (int) hitPointsRemoved;
                currentHitpoints = currentHitpoints > 0 ? currentHitpoints : 0;

                forwardSector.setWall(currentHitpoints);

                meleeProcessor.getParent().getFieldBattleLog().logStructureAffected(forwardSector.getX(), forwardSector.getY(),
                        "wall", brigade, currentLocation.getBridge(), (int) -hitPointsRemoved);

                if (currentHitpoints > 0) {
                    LOGGER.debug("A fortification wall is being destroyed in {}. Current hit points {} (was {})",
                            new Object[]{forwardSector, currentHitpoints, previousHitPoints});
                } else {
                    LOGGER.debug("The fortification wall has been destroyed in {}", forwardSector);
                }
            }
        }
    }

}
