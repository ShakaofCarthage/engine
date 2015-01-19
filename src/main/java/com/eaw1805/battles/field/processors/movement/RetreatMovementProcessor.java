package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.processors.MovementProcessor;

/**
 * Movement processor for the "Retreat" order.
 *
 * @author fragkakis
 */
public class RetreatMovementProcessor extends BaseBackwardsMovementProcessor {

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public RetreatMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

}
