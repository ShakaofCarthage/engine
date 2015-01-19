package com.eaw1805.battles.field.processors;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.morale.RallyCalculator;
import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.movement.AttackAndReformMovementProcessor;
import com.eaw1805.battles.field.processors.movement.AttackEnemyStrategicPointsMovementProcessor;
import com.eaw1805.battles.field.processors.movement.BaseOrderMovementProcessor;
import com.eaw1805.battles.field.processors.movement.BuildPontoonBridgeMovementProcessor;
import com.eaw1805.battles.field.processors.movement.DefendPositionMovementProcessor;
import com.eaw1805.battles.field.processors.movement.DigEntrenchmentsMovementProcessor;
import com.eaw1805.battles.field.processors.movement.EngageIfInRangeMovementProcessor;
import com.eaw1805.battles.field.processors.movement.FollowDetachmentMovementProcessor;
import com.eaw1805.battles.field.processors.movement.MaintainDistanceMovementProcessor;
import com.eaw1805.battles.field.processors.movement.MoveToDestroyBridgeMovementProcessor;
import com.eaw1805.battles.field.processors.movement.MoveToDestroyFortificationsMovementProcessor;
import com.eaw1805.battles.field.processors.movement.MoveToEngageMovementProcessor;
import com.eaw1805.battles.field.processors.movement.MoveToFireMovementProcessor;
import com.eaw1805.battles.field.processors.movement.RecoverOwnStrategicPointsMovementProcessor;
import com.eaw1805.battles.field.processors.movement.RetreatMovementProcessor;
import com.eaw1805.battles.field.processors.movement.RoutingMovementProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The movement processor processes the movements of Brigades in the field battle.
 *
 * @author fragkakis
 */
public class MovementProcessor extends BaseProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MovementProcessor.class);

    private final BaseOrderMovementProcessor engageIfInRangeMovementProcessor = new EngageIfInRangeMovementProcessor(this);
    private final BaseOrderMovementProcessor moveToEngageMovementProcessor = new MoveToEngageMovementProcessor(this);
    private final BaseOrderMovementProcessor attackAndReformMovementProcessor = new AttackAndReformMovementProcessor(this);
    private final BaseOrderMovementProcessor moveToFireMovementProcessor = new MoveToFireMovementProcessor(this);
    private final BaseOrderMovementProcessor defendPositionMovementProcessor = new DefendPositionMovementProcessor(this);
    private final BaseOrderMovementProcessor maintainDistanceMovementProcessor = new MaintainDistanceMovementProcessor(this);
    private final BaseOrderMovementProcessor retreatMovementProcessor = new RetreatMovementProcessor(this);
    private final BaseOrderMovementProcessor attackEnemyStrategicPointsMovementProcessor = new AttackEnemyStrategicPointsMovementProcessor(this);
    private final BaseOrderMovementProcessor recoverOwnStrategicPointsMovementProcessor = new RecoverOwnStrategicPointsMovementProcessor(this);
    private final BaseOrderMovementProcessor digEntrenchmentsMovementProcessor = new DigEntrenchmentsMovementProcessor(this);
    private final BaseOrderMovementProcessor moveToDestroyFortificationsMovementProcessor = new MoveToDestroyFortificationsMovementProcessor(this);
    private final BaseOrderMovementProcessor moveToDestroyBridgeMovementProcessor = new MoveToDestroyBridgeMovementProcessor(this);
    private final BaseOrderMovementProcessor buildPontoonBridgeMovementProcessor = new BuildPontoonBridgeMovementProcessor(this);
    private final BaseOrderMovementProcessor routingMovementProcessor = new RoutingMovementProcessor(this);
    private final BaseOrderMovementProcessor followDetachmentMovementProcessor = new FollowDetachmentMovementProcessor(this);

    private static final BrigadeFrontToBackComparator BRIGADE_FRONT_TO_BACK_COMPARATOR_SIDE_0 = new BrigadeFrontToBackComparator(0);
    private static final BrigadeFrontToBackComparator BRIGADE_FRONT_TO_BACK_COMPARATOR_SIDE_1 = new BrigadeFrontToBackComparator(1);

    private final RallyCalculator rallyChecker = new RallyCalculator(this.getParent());

    public MovementProcessor(FieldBattleProcessor parent) {
        super(parent);
    }

    @Override
    public void process(int side, int round) {


        LOGGER.debug("MOVEMENT PROCESSOR: Processing round {} side {}", new Object[]{round, side});
        FieldBattleMap fbMap = getParent().getFbMap();

        BaseFieldBattlePathCalculator pathCalculator = initializePathCalculatorForSide(fbMap, side);

        List<Brigade> sideBrigades = getParent().getSideBrigades()[side];
        List<Brigade> sideBrigadesOrderedFrontToBack = orderBrigadesFrontToBack(sideBrigades, side);

        for (Brigade brigade : sideBrigadesOrderedFrontToBack) {
            LOGGER.debug("{}", brigade);

            // skip brigades that have left the field battle
            if (!brigade.getFieldBattlePosition().exists()) {
                continue;
            }
            // routing units perform morale check to rally
            if (brigade.isRouting()) {
                LOGGER.debug("{} is routing, performing morale check to rally", brigade);
                int rallyModifier = rallyChecker.computeRallyModifier(brigade);
                MoraleStatusEnum moraleStatusEnum = getParent().getMoraleChecker().checkAndSetMorale(brigade, rallyModifier);
                getParent().getFieldBattleLog().logRallyOutcome(side, moraleStatusEnum, brigade);

                LOGGER.debug("{}: new morale status is {}", new Object[]{brigade, moraleStatusEnum});
            }
            // skip brigades that are in melee combat
            if (getParent().isInMeleeCombat(brigade)) {
                LOGGER.debug("{} is in melee combat, cannot move, skipping movement", brigade);
                continue;
            }

            Order order = getParent().findCurrentOrder(brigade);
            BaseOrderMovementProcessor orderMovementProcessor;

            if (brigade.isRouting()) {
                orderMovementProcessor = routingMovementProcessor;
            } else {
                orderMovementProcessor = getOrderMovementProcessorForOrder(order.getOrderTypeEnum());
            }

            Set<Brigade> visibleEnemies = getVisibleEnemies(brigade);
            Set<FieldBattleSector> visibleEnemySectors = getParent().getSectors(visibleEnemies);
            if (orderMovementProcessor != null) {
                orderMovementProcessor.move(brigade, visibleEnemySectors, visibleEnemies, pathCalculator, order);
            }
        }
    }

    private List<Brigade> orderBrigadesFrontToBack(List<Brigade> sideBrigades, int side) {

        // leaders(front-to-back) + followers (front-to-back) + others (front-to-back)
        BrigadeFrontToBackComparator frontToBackComparator = side == 0 ? BRIGADE_FRONT_TO_BACK_COMPARATOR_SIDE_0 : BRIGADE_FRONT_TO_BACK_COMPARATOR_SIDE_1;

        List<Brigade> leaders = new ArrayList<Brigade>(getParent().getFieldBattleDetachmentProcessor().getAllLeaders());
        Collections.sort(leaders, frontToBackComparator);

        List<Brigade> followers = new ArrayList<Brigade>(getParent().getFieldBattleDetachmentProcessor().getAllFollowers());
        Collections.sort(followers, frontToBackComparator);

        List<Brigade> others = new ArrayList<Brigade>(sideBrigades);
        others.removeAll(leaders);
        others.removeAll(followers);
        Collections.sort(others, frontToBackComparator);

        List<Brigade> frontToBackBrigades = new ArrayList<Brigade>(leaders);
        frontToBackBrigades.addAll(followers);
        frontToBackBrigades.addAll(others);

        return frontToBackBrigades;
    }

    /**
     * Comparator for brigades that orders based on their position in a field
     * battle. Orders brigades in front-to-back, left-to-right way. The
     * front-to-back ordering is side-specific.
     *
     * @author fragkakis
     */
    public static class BrigadeFrontToBackComparator implements Comparator<Brigade> {

        private int multiplier;

        public BrigadeFrontToBackComparator(int side) {
            multiplier = side == 0 ? 1 : -1;
        }

        @Override
        public int compare(Brigade o1, Brigade o2) {
            int result = multiplier * (o2.getFieldBattlePosition().getY() - o1.getFieldBattlePosition().getY());
            if (result == 0) {
                result = o1.getFieldBattlePosition().getX() - o2.getFieldBattlePosition().getX();
            }
            return result;
        }

    }

    /**
     * Returns the appropriate {@link BaseOrderMovementProcessor} for a type of order.
     *
     * @param orderType the order type
     * @return the order movement processor
     */
    private BaseOrderMovementProcessor getOrderMovementProcessorForOrder(OrdersEnum orderType) {

        switch (orderType) {
            case ENGAGE_IF_IN_RANGE:
                return engageIfInRangeMovementProcessor;
            case MOVE_TO_ENGAGE:
                return moveToEngageMovementProcessor;
            case ATTACK_AND_REFORM:
                return attackAndReformMovementProcessor;
            case MOVE_TO_FIRE:
                return moveToFireMovementProcessor;
            case DEFEND_POSITION:
                return defendPositionMovementProcessor;
            case MAINTAIN_DISTANCE:
                return maintainDistanceMovementProcessor;
            case RETREAT:
                return retreatMovementProcessor;
            case ATTACK_ENEMY_STRATEGIC_POINTS:
                return attackEnemyStrategicPointsMovementProcessor;
            case RECOVER_OWN_STRATEGIC_POINTS:
                return recoverOwnStrategicPointsMovementProcessor;
            case DIG_ENTRENCHMENTS:
                return digEntrenchmentsMovementProcessor;
            case MOVE_TO_DESTROY_FORTIFICATIONS:
                return moveToDestroyFortificationsMovementProcessor;
            case MOVE_TO_DESTROY_BRIDGES:
                return moveToDestroyBridgeMovementProcessor;
            case BUILD_PONTOON_BRIDGE:
                return buildPontoonBridgeMovementProcessor;
            case FOLLOW_DETACHMENT:
                return followDetachmentMovementProcessor;
            default:
                return null;
        }
    }

    /**
     * Returns the sectors in which visible enemies stand.
     *
     * @param side the side
     * @return the enemy sectors
     */
    private Set<Brigade> getVisibleEnemies(Brigade brigade) {

        int enemySide = getParent().findSide(brigade) == 0 ? 1 : 0;
        List<Brigade> enemies = getParent().getSideBrigades()[enemySide];
        Set<Brigade> visibleEnemies = new HashSet<Brigade>();

        for (Brigade enemy : enemies) {
            if (getParent().getFieldBattleVisibilityProcessor().visible(brigade, enemy)) {
                visibleEnemies.add(enemy);
            }
        }

        return visibleEnemies;
    }

    private Set<FieldBattleSector> getSideSectors(int side) {

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        List<Brigade> enemyBrigades = getParent().getSideBrigades()[side];

        for (Brigade brigade : enemyBrigades) {
            // check that brigade has not left the field battle
            FieldBattleSector sector = getParent().getSector(brigade);
            if (sector != null) {
                sectors.add(sector);
            }
        }

        return sectors;
    }

    /**
     * Initializes the path calculator for a side.
     *
     * @param fbMap            the field battle map
     * @param side             the side
     * @param enemySideSectors the sectors on which enemies stand
     * @return the path calculator
     */
    private BaseFieldBattlePathCalculator initializePathCalculatorForSide(FieldBattleMap fbMap, int side) {
        Set<FieldBattleSector> blockedSectors = new HashSet<FieldBattleSector>();
        blockedSectors.addAll(getSideSectors(0));
        blockedSectors.addAll(getSideSectors(1));
        LOGGER.debug("Blocked sectors: {}", blockedSectors);
        return new BaseFieldBattlePathCalculator(fbMap, side, blockedSectors);
    }

    /**
     * Moves a brigade to the specified location, and updates the path locator to specify the location as impassable.
     *
     * @param brigade        the brigade
     * @param destination    the destination
     * @param pathCalculator the path calculator
     */
    public void moveBrigade(Brigade brigade, FieldBattleSector destination, BaseFieldBattlePathCalculator pathCalculator) {

        FieldBattleProcessor parent = getParent();
        FieldBattleSector currentSector = parent.getSector(brigade);
        LOGGER.debug("{}: Moving from {} to {}", new Object[]{brigade, currentSector, destination});

        if (currentSector != destination) {
            if (parent.getBrigadeInSector(destination) != null) {
                throw new IllegalArgumentException("Attempting to move brigade to non-empty destination");
            }

            parent.getSectorsToBrigades().remove(currentSector);
            parent.getSectorsToBrigades().put(destination, brigade);

            // maintain parent's collections on who has moved at this halfround
            parent.getBrigadesThatMovedInTheCurrentHalfRound().add(brigade);

            // maintain path calculator, make new destination impassable
            pathCalculator.makeSectorPassable(currentSector);
            pathCalculator.makeSectorImpassable(destination);

            int side = parent.findSide(brigade);
            parent.getFieldBattleLog().logBrigadeMovement(side, brigade, currentSector, destination);
        }
    }

}
