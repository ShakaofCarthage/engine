package com.eaw1805.battles.field.orders;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class determines the prevalence of orders.
 *
 * @author fragkakis
 */
public class FieldBattleOrderProcessor {

    private FieldBattleProcessor parent;

    /**
     * Constructor.
     *
     * @param parent the field battle processor
     */
    public FieldBattleOrderProcessor(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    /**
     * Determines the current order of a brigade depending on the existence or
     * not of an additional order, and whether the trigger condition has been
     * met.
     *
     * @param brigade the brigade
     * @return the current order
     */
    public Order findCurrentOrder(Brigade brigade) {

        Order basicOrder = brigade.getBasicOrder();
        Order additionalOrder = brigade.getAdditionalOrder();

        // No additional order
        if (additionalOrder == null
                || StringUtils.isEmpty(additionalOrder.getOrderType())) {
            return basicOrder;
        }

        // Battle round trigger
        int currentRound = parent.getCurrentRound();
        if (additionalOrder.getActivationRound() > 0
                && additionalOrder.getActivationRound() <= currentRound) {
            return additionalOrder;
        }

        // Head count trigger
        if (additionalOrder.getHeadCountThreshold() >= 0
                && ArmyUtils.findBrigadeHeadCount(brigade) <= additionalOrder.getHeadCountThreshold()) {
            return additionalOrder;
        }

        // Last destination has been reached
        if (additionalOrder.isLastDestinationReached()
                && OrderUtils.lastCheckpointReached(basicOrder)) {
            return additionalOrder;
        }

        // Strategic point - related triggers
        FieldBattleMap fbMap = parent.getFbMap();

        int ourSide = parent.findSide(brigade);
        int enemySide = ourSide == 0 ? 1 : 0;
        List<Nation> ourSideNations = parent.getBattleField().getSide(ourSide);
        List<Nation> enemySideNations = parent.getBattleField().getSide(enemySide);
        Set<FieldBattleSector> allOurStrategicPoints = MapUtils.findStrategicPoints(fbMap, ourSideNations);
        Set<FieldBattleSector> allEnemyStrategicPoints = MapUtils.findStrategicPoints(fbMap, enemySideNations);

        Set<FieldBattleSector> customStrategicPoints = getCustomOrderStrategicPoints(additionalOrder);

        // Enemy has captured a "custom" (user-specified) strategic point of ours
        if (additionalOrder.isEnemySideCapturedOwnStrategicPoint()
                && !customStrategicPoints.isEmpty()
                && atLeastOneStrategicPointHeldBySide(customStrategicPoints, enemySide)) {
            return additionalOrder;
        }

        // Enemy has captured any strategic point of ours
        if (additionalOrder.isEnemySideCapturedOwnStrategicPoint()
                && customStrategicPoints.isEmpty()
                && atLeastOneStrategicPointHeldBySide(allOurStrategicPoints, enemySide)) {
            return additionalOrder;
        }

        // Our side has captured a "custom" (user-specified) strategic point of the enemy
        if (additionalOrder.isOwnSideCapturedEnemyStrategicPoint()
                && !customStrategicPoints.isEmpty()
                && atLeastOneStrategicPointHeldBySide(customStrategicPoints, ourSide)) {
            return additionalOrder;
        }

        // Our side has captured any strategic point of the enemy
        if (additionalOrder.isOwnSideCapturedEnemyStrategicPoint()
                && customStrategicPoints.isEmpty()
                && atLeastOneStrategicPointHeldBySide(allEnemyStrategicPoints, ourSide)) {
            return additionalOrder;
        }

        return basicOrder;

    }

    /**
     * Puts together all the custom order strategic points into a set.
     *
     * @param order the order
     * @return the set of custom strategic points
     */
    private Set<FieldBattleSector> getCustomOrderStrategicPoints(Order order) {

        Set<FieldBattleSector> customStrategicPoints = new HashSet<FieldBattleSector>();

        if (order.getCustomStrategicPoint1().exists()) {
            customStrategicPoints.add(MapUtils.getSectorFromPosition(parent.getFbMap(), order.getCustomStrategicPoint1()));
        }
        if (order.getCustomStrategicPoint2().exists()) {
            customStrategicPoints.add(MapUtils.getSectorFromPosition(parent.getFbMap(), order.getCustomStrategicPoint2()));
        }
        if (order.getCustomStrategicPoint3().exists()) {
            customStrategicPoints.add(MapUtils.getSectorFromPosition(parent.getFbMap(), order.getCustomStrategicPoint3()));
        }

        return customStrategicPoints;
    }

    /**
     * Checks whether at least one of the specified strategic points is currently held by the specified side.
     *
     * @param strategicPointSectors the strategic points
     * @param side                  the side
     * @return true or false
     */
    private boolean atLeastOneStrategicPointHeldBySide(Collection<FieldBattleSector> strategicPointSectors, int side) {

        Collection<Nation> sideNations = parent.getBattleField().getSide(side);

        for (FieldBattleSector strategicPointSector : strategicPointSectors) {
            if (sideNations.contains(strategicPointSector.getCurrentHolder())) {
                return true;
            }
        }

        return false;
    }

}
