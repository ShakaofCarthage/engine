package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.processors.ProcessorUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Movement processor for the "Attack and reform" order.
 *
 * @author fragkakis
 */
public class AttackAndReformMovementProcessor extends BaseOffensiveOrderMovementProcessor {

    public AttackAndReformMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    protected FieldBattleSector findPreferredDestinationForEnemyGroup(Brigade brigade,
                                                                      BaseFieldBattlePathCalculator pathCalculator,
                                                                      FieldBattleSector currentLocation,
                                                                      Collection<Brigade> enemies, int remainingMps, Order order) {

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

            proceedTowardsSector(brigade, destination, remainingMps, pathCalculator, true);

            // and remember to return when done
            setLastCheckpointNotReached(order);
        }

        return destination;
    }

    private void setLastCheckpointNotReached(Order order) {
        if (order.getCheckpoint3() != null) {
            order.setReachedCheckpoint3(false);
        } else if (order.getCheckpoint2() != null) {
            order.setReachedCheckpoint2(false);
        } else {
            order.setReachedCheckpoint1(false);
        }
    }

}
