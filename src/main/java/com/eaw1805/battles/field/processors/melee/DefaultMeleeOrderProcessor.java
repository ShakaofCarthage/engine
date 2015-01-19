package com.eaw1805.battles.field.processors.melee;

import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.Order;

public class DefaultMeleeOrderProcessor extends BaseOrderMeleeProcessor {
	
	public DefaultMeleeOrderProcessor(MeleeProcessor meleeProcessor) {
		super(meleeProcessor);
	}

	protected void performOrderMeleeAction(Brigade brigade, Order order) {
		// do nothing
	}

}
