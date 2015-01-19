package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.data.model.army.Brigade;


/**
 * Movement processor for the "Attack enemy strategic points" order.
 * @author fragkakis
 *
 */
public class AttackEnemyStrategicPointsMovementProcessor extends BaseStrategicPointOrderMovementProcessor {

	/**
	 * Constructor.
	 * @param movementProcessor
	 */
	public AttackEnemyStrategicPointsMovementProcessor(MovementProcessor movementProcessor) {
		super(movementProcessor);
	}

	@Override
	protected int getTargetStrategicPointsSide(Brigade brigade) {
		int ourSide = findSide(brigade.getNation());
		return ourSide == 0 ? 1 : 0;
	}


}
