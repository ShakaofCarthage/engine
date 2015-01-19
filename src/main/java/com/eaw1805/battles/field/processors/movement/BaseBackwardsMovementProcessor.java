package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Base class for backwards movement processing.
 *
 * @author fragkakis
 */
public abstract class BaseBackwardsMovementProcessor extends BaseOrderMovementProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(BaseBackwardsMovementProcessor.class);

    public BaseBackwardsMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    public void afterCheckpointsOrderSpecificMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        FieldBattleSector exitPoint = findExitPoint(brigade, pathCalculator);

        if (currentLocation == exitPoint) {
            movementProcessor.getParent().markBrigadeAsLeftTheBattlefield(brigade);
        } else {
            proceedTowardsSector(brigade, exitPoint, remainingMps, pathCalculator, true);

            FieldBattlePosition newPosition = brigade.getFieldBattlePosition();
            if (currentLocation.getMap().getFieldBattleSector(newPosition.getX(), newPosition.getY()) == exitPoint) {
                LOGGER.debug("Brigade {} left the battlefield at {}", new Object[]{brigade, exitPoint});
                movementProcessor.getParent().markBrigadeAsLeftTheBattlefield(brigade);
            }
        }
    }

    /**
     * Finds the exit point for a brigade.
     *
     * @param brigade        the brigade
     * @param pathCalculator the path calculator
     * @return the exit point
     */
    protected FieldBattleSector findExitPoint(Brigade brigade, BaseFieldBattlePathCalculator pathCalculator) {
        int side = findSide(brigade.getNation());
        
        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        FieldBattleMap fbMap = currentLocation.getMap();
        int targetY = side == 0 ? 0 : currentLocation.getMap().getSizeY() - 1;
        
        Set<FieldBattleSector> lastLineSectors = MapUtils.findSectorsInArea(fbMap, 0, fbMap.getSizeX()-1, targetY, targetY, false);
        FieldBattleSector exitPoint = pathCalculator.findClosest(currentLocation, lastLineSectors, brigade.getArmTypeEnum(), brigade.getFormationEnum(), true);
        
        return exitPoint;
    }

}
