package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.List;
import java.util.Set;

/**
 * Movement processor for the "Maintain Distance" order.
 *
 * @author fragkakis
 */
public class MaintainDistanceMovementProcessor extends BaseBackwardsMovementProcessor {

    private static final int DEFAULT_DISTANCE = 7;

    public MaintainDistanceMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    public void afterCheckpointsOrderSpecificMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        int distanceToMaintain = order.getMaintainDistance() != null ? order.getMaintainDistance() : DEFAULT_DISTANCE;
        // find closest enemy
        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        int closestEnemyDistance = Integer.MAX_VALUE;
        FieldBattleSector closestEnemySector = null;
        for (FieldBattleSector visibleEnemySector : visibleEnemySectors) {
            int distance = MapUtils.getSectorsDistance(currentLocation, visibleEnemySector);
            if (distance < closestEnemyDistance) {
                closestEnemySector = visibleEnemySector;
                closestEnemyDistance = distance;
            }
        }

        if (closestEnemyDistance < distanceToMaintain) {
            // the enemy is near! move towards our setup area!

            FieldBattleSector exitPoint = findExitPoint(brigade, pathCalculator);
            if (exitPoint == currentLocation) {
                // no point in moving, we are already there
                return;
            }
            List<FieldBattleSector> exitPath = pathCalculator.findPath(currentLocation, exitPoint, brigade.getArmTypeEnum(), brigade.getFormationEnum(), true);

            FieldBattleSector maintainDistancePathSector = null;

            for (int i = 1; i <= exitPath.size() - 1; i++) {
                int pathSectorCost = pathCalculator.findCost(currentLocation, exitPath.get(i), brigade.getArmTypeEnum(), brigade.getFormationEnum(), true);
                maintainDistancePathSector = exitPath.get(i);

                if (remainingMps < pathSectorCost || distanceToMaintain < MapUtils.getSectorsDistance(closestEnemySector, exitPath.get(i))) {
                    maintainDistancePathSector = exitPath.get(i - 1);
                    break;
                }
            }

            if (maintainDistancePathSector != null) {
                proceedTowardsSector(brigade, maintainDistancePathSector, remainingMps, pathCalculator, true);
            }
        }

    }

}
