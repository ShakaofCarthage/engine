package com.eaw1805.battles.field.processors.melee;

import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;

/**
 * Base class for the pioneer orders.
 * @author fragkakis
 *
 */
public abstract class BasePioneerOrderMeleeProcessor extends BaseOrderMeleeProcessor {
	
	public BasePioneerOrderMeleeProcessor(MeleeProcessor meleeProcessor) {
		super(meleeProcessor);
	}

	protected double calculatePioneerHitPoints(Brigade brigade) {
		double constructionPointsAdded = 0d;

		for(Battalion battalion : brigade.getBattalions()) {
			if(battalion.getType().isEngineer()) {
				constructionPointsAdded += battalion.getHeadcount() * 0.01 * Math.sqrt(battalion.getExperience());
			}
		}
		return constructionPointsAdded;
	}
}
