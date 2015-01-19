package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.processors.ProcessorUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Movement processor for the "Engage if in range" order.
 *
 * @author fragkakis
 */
public class EngageIfInRangeMovementProcessor extends BaseOffensiveOrderMovementProcessor {

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public EngageIfInRangeMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    protected int beforeCheckpointsOrderMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies,
                                                 BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        FieldBattleSector destination = findTargetDestination(brigade, visibleEnemies,
                pathCalculator, order, remainingMps, currentLocation);

        if (destination != null && currentLocation != destination) {
            proceedTowardsSector(brigade, destination, remainingMps, pathCalculator, true);
            return -1;    // we don't want any another movement in this halfround
        } else {
            return remainingMps;
        }
    }

    @Override
    protected FieldBattleSector findPreferredDestinationForEnemyGroup(
            Brigade brigade, BaseFieldBattlePathCalculator pathCalculator,
            FieldBattleSector currentLocation, Collection<Brigade> enemies,
            int remainingMps, Order order) {

        FieldBattleSector destination = null;
        Map<Brigade, Set<FieldBattleSector>> enemySectorsWithinMps = new HashMap<Brigade, Set<FieldBattleSector>>();

        for (Brigade enemy : enemies) {

            FieldBattleSector enemySector = movementProcessor.getParent().getSector(enemy);

            Set<FieldBattleSector> enemySectorNeighbours = MapUtils.getHorizontalAndVerticalNeighbours(enemySector);
            if (enemySectorNeighbours.contains(currentLocation)) {
                // we are next to this enemy, so if we finally attack him, there is no reason to move to another adjacent sector
                enemySectorsWithinMps.put(enemy, new HashSet<FieldBattleSector>());
                enemySectorsWithinMps.get(enemy).add(currentLocation);
                continue;
            }

            for (FieldBattleSector enemySectorNeighbour : enemySectorNeighbours) {
                // gather all the neighbouring sectors of this enemy that are within our MPs
                int enemySectorNeighbourCost = pathCalculator.findCost(currentLocation, enemySectorNeighbour,
                        brigade.getArmTypeEnum(), brigade.getFormationEnum(), true);
                if (enemySectorNeighbourCost <= remainingMps) {

                    if (enemySectorsWithinMps.get(enemy) == null) {
                        enemySectorsWithinMps.put(enemy, new HashSet<FieldBattleSector>());
                    }
                    enemySectorsWithinMps.get(enemy).add(enemySectorNeighbour);
                }
            }
        }

        if (!enemySectorsWithinMps.isEmpty()) {
            Brigade randomEnemyWithinRange = ProcessorUtils.getRandom(enemySectorsWithinMps.keySet());
            destination = ProcessorUtils.getRandom(enemySectorsWithinMps.get(randomEnemyWithinRange));
        }

        if (destination != null) {
            // set the return point
            order.setCheckpoint1(new FieldBattlePosition(currentLocation.getX(), currentLocation.getY()));
            order.setReachedCheckpoint1(false);
            order.setCheckpoint2(null);
            order.setCheckpoint3(null);
        }

        return destination;
    }

}
