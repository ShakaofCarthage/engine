package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.detachment.FieldBattleDetachmentProcessor;
import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.DetachmentPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Movement processor for the "Follow Detachment" order.
 *
 * @author fragkakis
 */
public class FollowDetachmentMovementProcessor extends BaseOrderMovementProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(FieldBattleDetachmentProcessor.class);
	
    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public FollowDetachmentMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    public void move(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors,
                     Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order) {

        Brigade leader = movementProcessor.getParent().getFieldBattleDetachmentProcessor().getLeader(brigade);
        if(leader==null) {
        	// leader has died or left the battlefield, no more move
        	LOG.debug("Could not find leader for {}, skipping movement", brigade);
        	return;
        }
        
        FieldBattleSector leaderSector = movementProcessor.getParent().getSector(leader);

        DetachmentPosition detachmentPosition = order.getDetachmentPositionEnum();

        // temporarily set order information depending on order
        int targetX = leaderSector.getX() + detachmentPosition.getDisplacementX();
        int targetY = leaderSector.getY() + detachmentPosition.getDisplacementY();
        FieldBattleMap fbMap = movementProcessor.getParent().getFbMap();
        if (0 <= targetX
                && targetX < fbMap.getSizeX()
                && 0 <= targetY
                && targetY < fbMap.getSizeY()) {
            // target sector exists
            order.getCheckpoint1().setX(targetX);
            order.getCheckpoint1().setY(targetY);
        } else {
            // target sector outside map, set leader position as target
            order.getCheckpoint1().setX(leaderSector.getX());
            order.getCheckpoint1().setY(leaderSector.getY());
        }
        order.setReachedCheckpoint1(false);
//    	order.setOrderTypeEnum(leaderOrder.getOrderTypeEnum());

        super.move(brigade, visibleEnemySectors, visibleEnemies, pathCalculator, order);

        // reset order information
        order.getCheckpoint1().setX(-1);
        order.getCheckpoint1().setY(-1);
        order.setReachedCheckpoint1(false);
//    	order.setOrderTypeEnum(OrdersEnum.FOLLOW_DETACHMENT);
    }

    @Override
    public void afterCheckpointsOrderSpecificMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        // don't move, following detachment.
    }

}
