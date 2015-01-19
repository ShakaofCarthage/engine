package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for order movement processors for offensive orders.
 *
 * @author fragkakis
 */
public abstract class BaseOffensiveOrderMovementProcessor extends
        BaseOrderMovementProcessor {

    public BaseOffensiveOrderMovementProcessor(
            MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    public void afterCheckpointsOrderSpecificMovement(Brigade brigade,
                                                      Set<FieldBattleSector> visibleEnemySectors,
                                                      Set<Brigade> visibleEnemies,
                                                      BaseFieldBattlePathCalculator pathCalculator, Order order,
                                                      int remainingMps) {

        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        FieldBattleSector destination = findTargetDestination(brigade, visibleEnemies,
                pathCalculator, order, remainingMps, currentLocation);

        if (destination != null && currentLocation != destination) {
            proceedTowardsSector(brigade, destination, remainingMps, pathCalculator, true);
        }
    }

    protected FieldBattleSector findTargetDestination(Brigade brigade,
                                                      Set<Brigade> visibleEnemies,
                                                      BaseFieldBattlePathCalculator pathCalculator, Order order,
                                                      int remainingMps, FieldBattleSector currentLocation) {
        FieldBattleSector destination = null;
        // try preferred enemies
        Brigade preferredVisibleEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(brigade, visibleEnemies, pathCalculator, order, movementProcessor.getParent());
        if (preferredVisibleEnemy != null) {
            destination = findPreferredDestinationForEnemy(brigade, pathCalculator, currentLocation, preferredVisibleEnemy, remainingMps, order);
        }

        if (destination == null) {
            // try non-preferred enemies
            Set<Brigade> notPreferredVisibleEnemies = new HashSet<Brigade>(visibleEnemies);
            notPreferredVisibleEnemies.remove(preferredVisibleEnemy);
            destination = findPreferredDestinationForEnemyGroup(brigade, pathCalculator, currentLocation, notPreferredVisibleEnemies, remainingMps, order);
        }
        return destination;
    }

    private FieldBattleSector findPreferredDestinationForEnemy(Brigade brigade,
                                                               BaseFieldBattlePathCalculator pathCalculator,
                                                               FieldBattleSector currentLocation, Brigade preferredVisibleEnemy,
                                                               int remainingMps, Order order) {

        Set<Brigade> preferredVisibleEnemySet = new HashSet<Brigade>();
        preferredVisibleEnemySet.add(preferredVisibleEnemy);
        return findPreferredDestinationForEnemyGroup(brigade, pathCalculator, currentLocation,
                preferredVisibleEnemySet, remainingMps, order);
    }

    protected abstract FieldBattleSector findPreferredDestinationForEnemyGroup(Brigade brigade,
                                                                               BaseFieldBattlePathCalculator pathCalculator,
                                                                               FieldBattleSector currentLocation,
                                                                               Collection<Brigade> enemies, int remainingMps, Order order);
}
