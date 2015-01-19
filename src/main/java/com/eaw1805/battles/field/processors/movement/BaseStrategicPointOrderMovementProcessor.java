package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for order movement processors of strategic point-related orders.
 *
 * @author fragkakis
 */
public abstract class BaseStrategicPointOrderMovementProcessor extends BaseOrderMovementProcessor {

    public BaseStrategicPointOrderMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    public void afterCheckpointsOrderSpecificMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        FieldBattleMap fbMap = currentLocation.getMap();
        int ourSide = findSide(brigade.getNation());
        int enemySide = ourSide == 0 ? 1 : 0;
        List<Nation> enemySideNations = movementProcessor.getParent().getBattleField().getSide(enemySide);

        int targetStrategicPointsSide = getTargetStrategicPointsSide(brigade);
        List<Nation> targetSideNations = movementProcessor.getParent().getBattleField().getSide(targetStrategicPointsSide);
        Set<FieldBattleSector> targetStrategicPoints = MapUtils.findStrategicPoints(fbMap, new HashSet<Nation>(targetSideNations));

        while (remainingMps > 0) {

            FieldBattleSector strategicPoint = null;

            if (order.getStrategicPoint1() != null
            		&& order.getStrategicPoint1().exists()
                    && strategicPointHeldBy(fbMap, order.getStrategicPoint1(), enemySideNations)) {
                strategicPoint = MapUtils.getSectorFromPosition(fbMap, order.getStrategicPoint1());

            } else if (order.getStrategicPoint2() != null 
            		&& order.getStrategicPoint2().exists()
                    && strategicPointHeldBy(fbMap, order.getStrategicPoint2(), enemySideNations)) {
                strategicPoint = MapUtils.getSectorFromPosition(fbMap, order.getStrategicPoint2());

            } else if (order.getStrategicPoint3() != null 
            		&& order.getStrategicPoint3().exists()
                    && strategicPointHeldBy(fbMap, order.getStrategicPoint3(), enemySideNations)) {
                strategicPoint = MapUtils.getSectorFromPosition(fbMap, order.getStrategicPoint3());
            }

            if (strategicPoint == null) {
                // all top-priority strategic points are held by us, let's check if all relevant
                // strategic points are held by us
                Set<FieldBattleSector> ourLostStrategicPoints = strategicPointsHeldBy(fbMap,
                        targetStrategicPoints, enemySideNations);

                if (ourLostStrategicPoints != null && !ourLostStrategicPoints.isEmpty()) {
                    Set<FieldBattleSector> closestLostAlliedStrategicPoints = MapUtils.findClosest(currentLocation, ourLostStrategicPoints);
                    // in case there are multiple, pick one in random
                    FieldBattleSector closestLostAlliedStrategicPoint = closestLostAlliedStrategicPoints.iterator().next();

                    strategicPoint = closestLostAlliedStrategicPoint;
                }
            }

            if (strategicPoint != null) {
                remainingMps = proceedTowardsSector(brigade, strategicPoint, remainingMps, pathCalculator, true);
            } else {
                break;
            }
        }

    }

    /**
     * Returns the side of which the strategic points we want to recover. Must be implemented.
     *
     * @param brigade the brigade under move
     * @return the target side (0 or 1)
     */
    protected abstract int getTargetStrategicPointsSide(Brigade brigade);

    /**
     * Checks whether a strategic point is held by any one of the specified nations.
     *
     * @param fbMap                  the field battle map
     * @param strategicPointPosition the strategic point position
     * @param sideNations            a list of nations
     * @return true if the specified strategic point is held by any one of the specified nations, false otherwise
     */
    private boolean strategicPointHeldBy(FieldBattleMap fbMap,
                                         FieldBattlePosition strategicPointPosition, List<Nation> sideNations) {

        FieldBattleSector strategicPoint = MapUtils.getSectorFromPosition(fbMap, strategicPointPosition);

        return sideNations.contains(strategicPoint.getCurrentHolder());

    }

    /**
     * Filters a set of strategic point sectors by their current holder.
     *
     * @param fbMap           the field battle map
     * @param strategicPoints a set of strategic points
     * @param sideNations     a list of nations
     * @return the subset of strategic point sectors that are held by any nation in the specified ones
     */
    private Set<FieldBattleSector> strategicPointsHeldBy(FieldBattleMap fbMap,
                                                         Set<FieldBattleSector> strategicPoints, List<Nation> sideNations) {

        Set<FieldBattleSector> sideStrategicPoints = new HashSet<FieldBattleSector>();
        for (FieldBattleSector strategicPoint : strategicPoints) {
            if (sideNations.contains(strategicPoint.getCurrentHolder())) {
                sideStrategicPoints.add(strategicPoint);
            }
        }

        return sideStrategicPoints;

    }


}
