package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Collection;
import java.util.Set;

/**
 * Movement processor for the "Move to engage" order.
 *
 * @author fragkakis
 */
public class MoveToEngageMovementProcessor extends BaseOffensiveOrderMovementProcessor {

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public MoveToEngageMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    protected FieldBattleSector findPreferredDestinationForEnemyGroup(Brigade brigade,
                                                                      BaseFieldBattlePathCalculator pathCalculator,
                                                                      FieldBattleSector currentLocation,
                                                                      Collection<Brigade> enemies, int remainingMps, Order order) {

        FieldBattleSector destination = null;
        for (Brigade enemy : enemies) {
            FieldBattleSector enemySector = movementProcessor.getParent().getSector(enemy);

            Set<FieldBattleSector> enemySectorNeighbours = MapUtils.getHorizontalAndVerticalNeighbours(enemySector);

            int nearestEnemyCost = Integer.MAX_VALUE;
            if (enemySectorNeighbours.contains(currentLocation)) {
                // we are already next to an enemy, no need to move
                destination = currentLocation;
                break;
            }
            for (FieldBattleSector enemySectorNeighbour : enemySectorNeighbours) {
                int enemySectorNeighbourCost = pathCalculator.findCost(currentLocation, enemySectorNeighbour,
                        brigade.getArmTypeEnum(), brigade.getFormationEnum(), true);
                if (enemySectorNeighbourCost < nearestEnemyCost) {
                    destination = enemySectorNeighbour;
                    nearestEnemyCost = enemySectorNeighbourCost;
                }
            }
        }
        return destination;
    }

}
