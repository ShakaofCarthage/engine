package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Set;

/**
 * Movement processor for the "Dig Entrenchments" order.
 *
 * @author fragkakis
 */
public class DigEntrenchmentsMovementProcessor extends BaseOrderMovementProcessor {

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public DigEntrenchmentsMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    protected void afterCheckpointsOrderSpecificMovement(Brigade brigade,
                                                         Set<FieldBattleSector> visibleEnemySectors,
                                                         Set<Brigade> visibleEnemies,
                                                         BaseFieldBattlePathCalculator pathCalculator, Order order,
                                                         int remainingMps) {

        // don't move, digging entrenchment
    }

}
