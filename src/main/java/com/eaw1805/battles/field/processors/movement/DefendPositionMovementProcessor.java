package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Set;

/**
 * Movement processor for the "Defend Position" order.
 *
 * @author fragkakis
 */
public class DefendPositionMovementProcessor extends BaseOrderMovementProcessor {

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public DefendPositionMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    public void afterCheckpointsOrderSpecificMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        // don't move, defending position.

    }

}
