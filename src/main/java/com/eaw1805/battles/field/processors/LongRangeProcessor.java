package com.eaw1805.battles.field.processors;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.processors.longrange.BaseLongRangeProcessor;
import com.eaw1805.battles.field.processors.longrange.DefaultLongRangeFireProcessor;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The melee processor processes the melee actions.
 *
 * @author fragkakis
 */
public class LongRangeProcessor extends BaseAttackProcessor {

    public LongRangeProcessor(FieldBattleProcessor parent) {
        super(parent);
    }

    private final BaseLongRangeProcessor defaultLongRangeFireProcessor = new DefaultLongRangeFireProcessor(this);
    /**
     * This map stores the number of consecutive half rounds for which an artillery battalion has been hitting the same target.
     */
    private Map<Battalion, Map<Brigade, Integer>> artilleryTargets = new HashMap<Battalion, Map<Brigade, Integer>>();

    private static final Logger LOGGER = LoggerFactory.getLogger(LongRangeProcessor.class);


    @Override
    public void process(int side, int round) {

        LOGGER.debug("LONG RANGE PROCESSOR: Processing round {} side {}", new Object[]{round, side});

        damageToInflict = new HashMap<Battalion, Double>();

        List<Brigade> sideBrigades = getParent().getSideBrigades()[side];

        for (Brigade brigade : sideBrigades) {
            LOGGER.debug("{}", brigade);
            Order order = getParent().findCurrentOrder(brigade);

            BaseLongRangeProcessor longRangeProcessor = getLongRangeProcessorForOrder(order.getOrderTypeEnum());
            longRangeProcessor.process(brigade, order);
        }

        int enemySide = side == 0 ? 1 : 0;

        inflictDamageToSide(enemySide);
    }

    private BaseLongRangeProcessor getLongRangeProcessorForOrder(
            OrdersEnum orderTypeEnum) {

        switch (orderTypeEnum) {
            case DEFEND_POSITION:
            default:
                return defaultLongRangeFireProcessor;
        }
    }

    public void increaseBattalionDamage(Battalion battalion, double additionalDamage) {
        if (damageToInflict.get(battalion) == null) {
            damageToInflict.put(battalion, 0D);
        }

        Double newBattalionDamage = damageToInflict.get(battalion) + additionalDamage;
        damageToInflict.put(battalion, newBattalionDamage);
    }

    public Map<Battalion, Map<Brigade, Integer>> getArtilleryTargets() {
        return artilleryTargets;
    }

    @Override
    protected AttackType getAttackType() {
        return AttackType.LONG_RANGE;
    }

}
