package com.eaw1805.battles.field.processors.longrange;

import com.eaw1805.battles.field.processors.LongRangeProcessor;
import com.eaw1805.battles.field.processors.movement.AdditionalOrderBrigadeFilter;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.Order;

/**
 * Base class for processing the melee actions for a particular type of order.
 * Each subclass processes movement of a single order type. The default
 * behaviour is to attack in melee adjacent enemies. If there are no enemies,
 * specific orders may perform other melee tasks.
 * 
 * @author fragkakis
 */
public abstract class BaseLongRangeProcessor {

    protected LongRangeProcessor longRangeProcessor;
    protected AdditionalOrderBrigadeFilter additionalOrderBrigadeFilter;

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public BaseLongRangeProcessor(LongRangeProcessor longRangeProcessor) {
        this.longRangeProcessor = longRangeProcessor;
        additionalOrderBrigadeFilter = new AdditionalOrderBrigadeFilter();
    }

    /**
     * Finds the side of a nation.
     *
     * @param nation the nation
     * @return the side number (0 or 1)
     */
    protected int findSide(Nation nation) {
        return longRangeProcessor.getParent().getBattleField().getSide(0).contains(nation) ? 0 : 1;
    }

	public abstract void process(Brigade brigade, Order order);

}
