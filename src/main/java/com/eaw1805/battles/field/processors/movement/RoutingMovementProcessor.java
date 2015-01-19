package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Movement processor for the "Route" order/state.
 *
 * @author fragkakis
 */
public class RoutingMovementProcessor extends BaseBackwardsMovementProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(RoutingMovementProcessor.class);

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public RoutingMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    public void move(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors,
                     Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order) {

        FieldBattleMap fbMap = movementProcessor.getParent().getFbMap();

        int remainingMps = findMps(brigade);

        if (remainingMps > 0) {
            afterCheckpointsOrderSpecificMovement(brigade, visibleEnemySectors, visibleEnemies, pathCalculator, order, remainingMps);
        }
        // the order may have resulted in the brigade having left the field battle (i.e. retreat). In that case, no movement can take place
        if (brigade.getFieldBattlePosition().exists()) {
            FieldBattleSector positionAfterMove = MapUtils.getSectorFromPosition(fbMap, brigade.getFieldBattlePosition());
            movementProcessor.moveBrigade(brigade, positionAfterMove, pathCalculator);
        }
    }
}
