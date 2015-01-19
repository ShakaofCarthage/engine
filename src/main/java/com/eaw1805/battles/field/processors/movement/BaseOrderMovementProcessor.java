package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.orders.OrderUtils;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.FieldBattleCollectionUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for processing movement for a particular type of order. Each subclass processes movement of a single order type.
 *
 * @author fragkakis
 */
public abstract class BaseOrderMovementProcessor {

    protected MovementProcessor movementProcessor;
    protected AdditionalOrderBrigadeFilter additionalOrderBrigadeFilter;
    /**
     * This is the sector on which a brigade will stop advancing in case it
     * fails a morale check when advancing next to an enemy.
     */
    protected FieldBattleSector retreatPointOnMoraleCheckFail;
    private final static Logger LOGGER = LoggerFactory.getLogger(BaseOrderMovementProcessor.class);
    private static final Map<ArmEnum, Map<ArmEnum, Integer>> ATTACK_MORALE_MODIFIERS;

    static {
        ATTACK_MORALE_MODIFIERS = new HashMap<ArmEnum, Map<ArmEnum, Integer>>();

        ATTACK_MORALE_MODIFIERS.put(ArmEnum.INFANTRY, new HashMap<ArmEnum, Integer>());
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.INFANTRY).put(ArmEnum.INFANTRY, 0);
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.INFANTRY).put(ArmEnum.CAVALRY, -20);
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.INFANTRY).put(ArmEnum.ARTILLERY, -10);

        ATTACK_MORALE_MODIFIERS.put(ArmEnum.CAVALRY, new HashMap<ArmEnum, Integer>());
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.CAVALRY).put(ArmEnum.INFANTRY, 10);
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.CAVALRY).put(ArmEnum.CAVALRY, 10);
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.CAVALRY).put(ArmEnum.ARTILLERY, 0);

        ATTACK_MORALE_MODIFIERS.put(ArmEnum.ARTILLERY, new HashMap<ArmEnum, Integer>());
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.ARTILLERY).put(ArmEnum.INFANTRY, 0);
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.ARTILLERY).put(ArmEnum.CAVALRY, 0);
        ATTACK_MORALE_MODIFIERS.get(ArmEnum.ARTILLERY).put(ArmEnum.ARTILLERY, 0);

    }

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public BaseOrderMovementProcessor(MovementProcessor movementProcessor) {
        this.movementProcessor = movementProcessor;
        additionalOrderBrigadeFilter = new AdditionalOrderBrigadeFilter();
    }

    /**
     * The top-level method of the movement processor. Moves a brigade across
     * the battle field, depending on the order it has been given. By default,
     * it moves the brigade along its checkpoints, and then performs the
     * order-specific movement. Override if appropriate.
     *
     * @param brigade             the brigade to be moved.
     * @param visibleEnemySectors the sectors of visible enemies
     * @param visibleEnemies      the visible enemies
     * @param pathCalculator      the path calculator
     * @param order               the order of the brigade
     */
    public void move(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order) {

        retreatPointOnMoraleCheckFail = null;

        FieldBattleMap fbMap = movementProcessor.getParent().getFbMap();
        int remainingMps = findMps(brigade);

        remainingMps = beforeCheckpointsOrderMovement(brigade, visibleEnemySectors, visibleEnemies, pathCalculator, order, remainingMps);

        remainingMps = changeFormationIfAppropriate(brigade, order, remainingMps);

        if (remainingMps > 0 && !OrderUtils.lastCheckpointReached(order)) {
            remainingMps = proceedTowardsLastCheckpoint(fbMap, brigade, pathCalculator, order, remainingMps);
        }
        if (remainingMps > 0 && OrderUtils.lastCheckpointReached(order)) {
            afterCheckpointsOrderSpecificMovement(brigade, visibleEnemySectors, visibleEnemies, pathCalculator, order, remainingMps);
        }
        // the order may have resulted in the brigade having left the field battle (i.e. retreat). In that case, no movement can take place
        if (brigade.getFieldBattlePosition().exists()) {
            FieldBattleSector positionAfterMove = MapUtils.getSectorFromPosition(fbMap, brigade.getFieldBattlePosition());

            // When moving adjacently to an enemy, perform a morale check. If
            // the check fails, stay 1 tile away. If the check passes, move
            // normally next to the enemy. This does not apply for artilleries,
            // as they will never attempt to move next to an enemy.
            if (brigade.getArmTypeEnum() != ArmEnum.ARTILLERY) {
                int enemySide = findSide(brigade.getNation()) == 0 ? 1 : 0;
                Set<FieldBattleSector> neighbours = MapUtils.getHorizontalAndVerticalNeighbours(positionAfterMove);
                Set<Brigade> neighbouringEnemies = movementProcessor.getParent().findBrigadesOfSide(neighbours, enemySide);
                if (!neighbouringEnemies.isEmpty()) {
                    Brigade randomNeighbourEnemy = FieldBattleCollectionUtils.getRandom(neighbouringEnemies);

                    int moraleModifier = ATTACK_MORALE_MODIFIERS.get(brigade.getArmTypeEnum()).get(randomNeighbourEnemy.getArmTypeEnum());

                    // attacking brigades that are influenced by "Fearless attacker" commanders receive
                    // a +5% bonus in morale checks
                    if (order.getOrderTypeEnum().isAttackOrder()
                            && movementProcessor.getParent().getCommanderProcessor().influencedByCommanderOfType(brigade,
                            CommanderType.FEARLESS_ATTACKER)) {
                        LOGGER.trace("{} is influenced by a Fearless attacker commander and is performing an attack order, +5% morale check bonus", brigade);
                        moraleModifier += 5;
                    }

                    LOGGER.debug("{} is {} and is attacking {} which is {}, {}% morale check bonus",
                            new Object[]{brigade, brigade.getArmTypeEnum(), randomNeighbourEnemy, randomNeighbourEnemy.getArmTypeEnum(), moraleModifier});

                    boolean moraleCheckResult = movementProcessor.getParent().getMoraleChecker().checkMorale(brigade, moraleModifier);

                    if (!moraleCheckResult && retreatPointOnMoraleCheckFail != null) {
                        positionAfterMove = retreatPointOnMoraleCheckFail;
                        brigade.setFieldBattlePosition(new FieldBattlePosition(retreatPointOnMoraleCheckFail.getX(), retreatPointOnMoraleCheckFail.getY()));
                    }
                }

            } else {

            }

            movementProcessor.moveBrigade(brigade, positionAfterMove, pathCalculator);
        }
    }

    protected int findMps(Brigade brigade) {
        int totalMps = 0;
        for (Battalion battalion : brigade.getBattalions()) {
            totalMps += battalion.getType().getSps();
        }
        int mps = totalMps / brigade.getBattalions().size();
        LOGGER.debug("Movement points: {}", mps);
        return mps;
    }

    /**
     * If a formation change is required, this takes up all the halfround.
     *
     * @param brigade      the brigade
     * @param order        the order
     * @param remainingMps the remaining movement points
     * @return the new remaining movement points
     */
    private int changeFormationIfAppropriate(Brigade brigade, Order order, int remainingMps) {

        /**
         * Check for first half round and set formation
         */
        if (brigade.getFormation() == null) {
            brigade.setFormationEnum(order.getFormationEnum());
        } else {

            FormationEnum orderFormation = order.getFormationEnum();
            FormationEnum brigadeCurrentFormation = brigade.getFormationEnum();

            if (brigadeCurrentFormation != orderFormation) {

                /**
                 * In case this is an Infantry in Square, don't change the formation
                 * if there is a non-routing Cavalry enemy in radius 3.
                 */
                if (brigade.getArmTypeEnum() == ArmEnum.INFANTRY
                        && brigadeCurrentFormation == FormationEnum.SQUARE) {

                    int ourSide = movementProcessor.getParent().findSide(brigade);
                    int enemySide = ourSide == 0 ? 1 : 0;

                    FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
                    Set<FieldBattleSector> sectorsInRadius3 = MapUtils.findSectorsInRadius(currentLocation, 3);
                    Set<Brigade> enemiesInRadius3 = movementProcessor.getParent().findBrigadesOfSide(sectorsInRadius3, enemySide);

                    if (ArmyUtils.containsUnbrokenBrigadeOfArm(enemiesInRadius3, ArmEnum.CAVALRY)) {
                        return remainingMps;
                    }
                }

                // Changing the formation takes up the whole halfround.
                LOGGER.debug("{}: Changed formation from {} to {}, no further movement possible",
                        new Object[]{brigade, brigadeCurrentFormation, orderFormation});
                brigade.setFormationEnum(orderFormation);
                remainingMps = -1;
            }
        }

        return remainingMps;
    }

    /**
     * Abstract method that contains the order-specific movement rules that must
     * take place BEFORE any checkpoint-related movement has taken place.
     * Default implementation does not perform any movement. Must be overridden
     * if necessary.
     *
     * @param brigade             the brigade to be moved.
     * @param visibleEnemySectors the sectors of visible enemies
     * @param visibleEnemies      the visible enemies
     * @param pathCalculator      the path calculator
     * @param order               the order of the brigade
     * @param remainingMps        the remaining movement points
     * @return the remaining movement points
     */
    protected int beforeCheckpointsOrderMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors, Set<Brigade> visibleEnemies,
                                                 BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {
        // do nothing
        return remainingMps;
    }

    /**
     * Abstract method that contains the order-specific movement rules that must
     * take place AFTER the last checkpoint has been reached. Must be implemented.
     *
     * @param brigade             the brigade to be moved.
     * @param visibleEnemySectors the sectors of visible enemies
     * @param visibleEnemies      the visible enemies
     * @param pathCalculator      the path calculator
     * @param order               the order of the brigade
     * @param remainingMps        the remaining movement points
     */
    protected abstract void afterCheckpointsOrderSpecificMovement(Brigade brigade, Set<FieldBattleSector> visibleEnemySectors,
                                                                  Set<Brigade> visibleEnemies, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps);

    protected int proceedTowardsLastCheckpoint(FieldBattleMap fbMap, Brigade brigade, BaseFieldBattlePathCalculator pathCalculator, Order order, int remainingMps) {

        FieldBattleSector positionAfterMove = MapUtils.getSectorFromPosition(fbMap, brigade.getFieldBattlePosition());

        while (remainingMps > 0) {

            FieldBattlePosition nextCheckpoint = OrderUtils.nextCheckpoint(order);
            FieldBattleSector nextCheckpointSector = MapUtils.getSectorFromPosition(fbMap, nextCheckpoint);

            // we consider backwards movement allowed when still moving towards last checkpoint
            remainingMps = proceedTowardsSector(brigade, nextCheckpointSector, remainingMps, pathCalculator, true);

            positionAfterMove = MapUtils.getSectorFromPosition(fbMap, brigade.getFieldBattlePosition());

            if (positionAfterMove == nextCheckpointSector) {
                // checkpoint reached, mark it as such and return;
                markCheckpointAsReached(order, positionAfterMove);
                if (OrderUtils.lastCheckpointReached(order)) {
                    break;
                }
            } else {
                // even if there are remaining MPs, they apparently were not enough to get us to the next checkpoint, so stop.
                break;
            }
        }

        return remainingMps;
    }

    private void markCheckpointAsReached(Order order, FieldBattleSector positionAfterMove) {

        FieldBattlePosition checkpoint1 = order.getCheckpoint1();
        FieldBattlePosition checkpoint2 = order.getCheckpoint2();
        FieldBattlePosition checkpoint3 = order.getCheckpoint3();

        if (checkpoint1.exists()
                && checkpoint1.getX() == positionAfterMove.getX()
                && checkpoint1.getY() == positionAfterMove.getY()
                && !order.isReachedCheckpoint1()) {
            order.setReachedCheckpoint1(true);
        } else if (checkpoint2.exists()
                && checkpoint2.getX() == positionAfterMove.getX()
                && checkpoint2.getY() == positionAfterMove.getY()
                && !order.isReachedCheckpoint2()) {
            order.setReachedCheckpoint2(true);
        } else if (checkpoint3.exists()
                && checkpoint3.getX() == positionAfterMove.getX()
                && checkpoint3.getY() == positionAfterMove.getY()
                && !order.isReachedCheckpoint3()) {
            order.setReachedCheckpoint3(true);
        } else {
            throw new IllegalArgumentException("Sector " + positionAfterMove + " does not correspond to any of the checkpoints "
                    + checkpoint1 + ", " + checkpoint2 + ", " + checkpoint3);
        }
    }

    /**
     * This method handles the movement towards a sector.
     *
     * @param brigade
     * @param sector
     * @param remainingMps
     * @param pathCalculator
     * @param backwardsAllowed
     * @return
     */
    protected int proceedTowardsSector(Brigade brigade, FieldBattleSector sector, int remainingMps, BaseFieldBattlePathCalculator pathCalculator, boolean backwardsAllowed) {

        FieldBattleSector currentLocation = MapUtils.getSectorFromPosition(sector.getMap(), brigade.getFieldBattlePosition());

        if (currentLocation == sector) {
            // brigade is already there, return its Movement Points as they are
            return remainingMps;
        }

        int ourSide = movementProcessor.getParent().findSide(brigade);
        int enemySide = ourSide == 0 ? 1 : 0;

        List<FieldBattleSector> path = pathCalculator.findPath(currentLocation, sector, brigade.getArmTypeEnum(), brigade.getFormationEnum(), backwardsAllowed);

        /**
         * If there is no path to the destination
         */
        if (path == null || path.size() == 0) {
        	Set<FieldBattleSector> neighbours = MapUtils.getNeighbours(sector);
        	if(neighbours.contains(currentLocation)) {
        		// we are at a neighbouring sector, stay there
        	} else {
        		// move to the closest neighbouring sector
        		List<FieldBattleSector> neighboursClosestFirst = MapUtils.orderByDistance(currentLocation, neighbours);
	        	for(FieldBattleSector neighbourClosestFirst : neighboursClosestFirst) {
	        		List<FieldBattleSector> neighbourPath = pathCalculator.findPath(currentLocation, neighbourClosestFirst, brigade.getArmTypeEnum(), 
	        				brigade.getFormationEnum(), backwardsAllowed);
	        		if(neighbourPath!=null && !neighbourPath.isEmpty()) {
	        			return proceedTowardsSector(brigade, neighbourClosestFirst, remainingMps, pathCalculator, backwardsAllowed);
	        		}
	        	}
        	}
        	
            remainingMps = -1;
        } else {

            FieldBattleSector destinationWithinMps = null;

            for (int i = 1; i < path.size(); i++) {

                retreatPointOnMoraleCheckFail = path.get(i - 1);

                FieldBattleSector pathSector = path.get(i);

                // artillery brigades will keep a distance of 2 tiles from any enemy, and won't move any longer
                // this is the only case where destinationWithinMps may end up null
                if (brigade.getArmTypeEnum() == ArmEnum.ARTILLERY) {
                    boolean nextToEnemy = sectorIsNextToEnemies(pathSector, enemySide);
                    if (nextToEnemy) {
                        remainingMps = -1;
                        break;
                    }
                }

                int cost = pathCalculator.findCost(currentLocation, pathSector,
                        brigade.getArmTypeEnum(), brigade.getFormationEnum(), backwardsAllowed);

                // for the 1st sector in the path, don't care about the cost
                if (i > 1 && cost > remainingMps) {
                    break;
                } else {
                    destinationWithinMps = pathSector;
                }
            }

            // in case of artillery closing in on enemies, the destination may be null 
            if (destinationWithinMps != null) {
                remainingMps = remainingMps - pathCalculator.findCost(currentLocation, destinationWithinMps,
                        brigade.getArmTypeEnum(), brigade.getFormationEnum(), backwardsAllowed);

                // for any strategic points in the path, mark them as owned
                for (FieldBattleSector pathSector : path.subList(0, path.indexOf(destinationWithinMps) + 1)) {
                    if (pathSector.isStrategicPoint()) {
                        pathSector.setCurrentHolder(brigade.getNation());
                    }
                }

                brigade.getFieldBattlePosition().setX(destinationWithinMps.getX());
                brigade.getFieldBattlePosition().setY(destinationWithinMps.getY());
            }

        }
        return remainingMps;
    }

    private boolean sectorIsNextToEnemies(FieldBattleSector sector, int enemySide) {

        Set<FieldBattleSector> neighbourSectors = MapUtils.getNeighbours(sector);
        Set<Brigade> neighbourEnemies = movementProcessor.getParent().findBrigadesOfSide(neighbourSectors, enemySide);
        if (neighbourEnemies.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Finds the side of a nation.
     *
     * @param nation the nation
     * @return the side number (0 or 1)
     */
    protected int findSide(Nation nation) {
        return movementProcessor.getParent().getBattleField().getSide(0).contains(nation) ? 0 : 1;
    }
}
