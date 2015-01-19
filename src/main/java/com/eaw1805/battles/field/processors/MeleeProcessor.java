package com.eaw1805.battles.field.processors;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.processors.melee.BaseOrderMeleeProcessor;
import com.eaw1805.battles.field.processors.melee.BuildPontoonBridgeMeleeProcessor;
import com.eaw1805.battles.field.processors.melee.DefaultMeleeOrderProcessor;
import com.eaw1805.battles.field.processors.melee.DigEntrenchmentsMeleeProcessor;
import com.eaw1805.battles.field.processors.melee.MoveToDestroyBridgeMeleeProcessor;
import com.eaw1805.battles.field.processors.melee.MoveToDestroyFortificationsMeleeProcessor;
import com.eaw1805.battles.field.processors.movement.BaseOrderMovementProcessor;
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
public class MeleeProcessor extends BaseBothSidesProcessor {

    /**
     * This map holds the number of brigades each target brigade has been attacked by.
     * It will be used for loss calculation.
     */
    private Map<Brigade, Integer> meleeAttackerCounts;

    private BaseOrderMeleeProcessor defaultMeleeAttackProcessor = new DefaultMeleeOrderProcessor(this);
    private BaseOrderMeleeProcessor buildPontoonBridgeProcessor = new BuildPontoonBridgeMeleeProcessor(this);
    private BaseOrderMeleeProcessor moveToDestroyBridgeProcessor = new MoveToDestroyBridgeMeleeProcessor(this);
    private BaseOrderMeleeProcessor moveToDestroyFortificationsProcessor = new MoveToDestroyFortificationsMeleeProcessor(this);
    private BaseOrderMeleeProcessor digEntrenchmentsProcessor = new DigEntrenchmentsMeleeProcessor(this);

    private static final Logger LOGGER = LoggerFactory.getLogger(MeleeProcessor.class);

    public MeleeProcessor(FieldBattleProcessor parent) {
        super(parent);
    }

    @Override
    public void process(int round) {

        damageToInflict = new HashMap<Battalion, Double>();
        meleeAttackerCounts = new HashMap<Brigade, Integer>();

        process(0, round);
        process(1, round);

        inflictDamageToSide(0);
        inflictDamageToSide(1);
    }

    @Override
    public void process(int side, int round) {

        LOGGER.debug("MELEE PROCESSOR: Processing round {} side {}", new Object[]{round, side});

        List<Brigade> sideBrigades = getParent().getSideBrigades()[side];

        for (Brigade brigade : sideBrigades) {
            LOGGER.debug("{}", brigade);
            Order order = getParent().findCurrentOrder(brigade);

            BaseOrderMeleeProcessor orderMeleeProcessor = getOrderMovementProcessorForOrder(order.getOrderTypeEnum());
            orderMeleeProcessor.process(brigade, order);
        }

    }

    /**
     * Returns the appropriate {@link BaseOrderMovementProcessor} for a type of order.
     *
     * @param orderType the order type
     * @return the order movement processor
     */
    private BaseOrderMeleeProcessor getOrderMovementProcessorForOrder(OrdersEnum orderType) {

        switch (orderType) {
            case DIG_ENTRENCHMENTS:
                return digEntrenchmentsProcessor;
            case MOVE_TO_DESTROY_FORTIFICATIONS:
                return moveToDestroyFortificationsProcessor;
            case MOVE_TO_DESTROY_BRIDGES:
                return moveToDestroyBridgeProcessor;
            case BUILD_PONTOON_BRIDGE:
                return buildPontoonBridgeProcessor;
            default:
                return defaultMeleeAttackProcessor;
        }
    }

    public int increaseTargetCount(Brigade brigade) {
        // increase the attack count for the current target by 1
        if (meleeAttackerCounts.get(brigade) == null) {
            meleeAttackerCounts.put(brigade, 0);
        }
        int newMeleeAttackerCount = meleeAttackerCounts.get(brigade) + 1;
        meleeAttackerCounts.put(brigade, newMeleeAttackerCount);
        return newMeleeAttackerCount;
    }

    public Map<Brigade, Integer> getMeleeAttackerCounts() {
        return meleeAttackerCounts;
    }

    @Override
    protected AttackType getAttackType() {
        return AttackType.MELEE;
    }

}
