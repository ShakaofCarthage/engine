package com.eaw1805.battles.field.processors.longrange;

import com.eaw1805.battles.field.processors.LongRangeProcessor;
import com.eaw1805.battles.field.processors.RicochetCalculator;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FieldBattleTerrainEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DefaultLongRangeFireProcessor extends BaseLongRangeProcessor {

    private static final double LONG_RANGE_RANDOMNESS_MODIFIER_MIN = 1d;
    private static final double LONG_RANGE_RANDOMNESS_MODIFIER_MAX = 4d;
    private static final Map<ArmEnum, Map<FormationEnum, Double>> attackerTypeModifier = new HashMap<ArmEnum, Map<FormationEnum, Double>>();
    private static final Map<ArmEnum, Map<FormationEnum, Double>> targetTypeModifier = new HashMap<ArmEnum, Map<FormationEnum, Double>>();
    private static final Map<ArmEnum, Map<Integer, Double>> attackerDistanceModifier = new HashMap<ArmEnum, Map<Integer, Double>>();
    private static final Map<ArmEnum, Map<FieldBattleTerrainEnum, Double>> targetTerrainModifier = new HashMap<ArmEnum, Map<FieldBattleTerrainEnum, Double>>();
    private static final boolean FRIENDLY_FIRE_ALLOWED = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLongRangeFireProcessor.class);
    private static final double PONTOON_BRIDGE_ARTILLERY_MODIFIER = 0.25d;
    private static final double VILLAGE_TOWN_CHATEAU_WALL_ARTILLERY_MODIFIER = 0.5d;


    static {
        // shooting effectiveness
        attackerTypeModifier.put(ArmEnum.INFANTRY, new HashMap<FormationEnum, Double>());
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.COLUMN, 0.45D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.LINE, 0.80D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.SKIRMISH, 1.00D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.SQUARE, 0.25D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.FLEE, 0.00D);

        attackerTypeModifier.put(ArmEnum.CAVALRY, new HashMap<FormationEnum, Double>());
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.COLUMN, 0.40D);
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.LINE, 0.80D);
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.SKIRMISH, 1.00D);
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.SQUARE, 0.40D);		// Invalid formation, behaves as column
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.FLEE, 0.00D);

        attackerTypeModifier.put(ArmEnum.ARTILLERY, new HashMap<FormationEnum, Double>());
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.COLUMN, 1.00D);
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.LINE, 1.00D);		// Invalid formation, behaves as column
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.SKIRMISH, 1.00D);	// Invalid formation, behaves as column
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.SQUARE, 1.00D);	// Invalid formation, behaves as column
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.FLEE, 0.00D);

        // target damage received
        targetTypeModifier.put(ArmEnum.INFANTRY, new HashMap<FormationEnum, Double>());
        targetTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.COLUMN, 0.90D);
        targetTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.LINE, 0.70D);
        targetTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.SKIRMISH, 0.40D);
        targetTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.SQUARE, 1.00D);
        targetTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.FLEE, 1.00D);

        targetTypeModifier.put(ArmEnum.CAVALRY, new HashMap<FormationEnum, Double>());
        targetTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.COLUMN, 1.00D);
        targetTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.LINE, 0.75D);
        targetTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.SKIRMISH, 0.45D);
        targetTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.SQUARE, 1.00D);		// Invalid formation, behaves as column
        targetTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.FLEE, 0.70D);

        targetTypeModifier.put(ArmEnum.ARTILLERY, new HashMap<FormationEnum, Double>());
        targetTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.COLUMN, 0.80D);
        targetTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.LINE, 0.80D);		// Invalid formation, behaves as column
        targetTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.SKIRMISH, 0.80D);	// Invalid formation, behaves as column
        targetTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.SQUARE, 0.80D);		// Invalid formation, behaves as column
        targetTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.FLEE, 1.00D);

        // attacker distance modifier
        attackerDistanceModifier.put(ArmEnum.INFANTRY, new HashMap<Integer, Double>());
        attackerDistanceModifier.get(ArmEnum.INFANTRY).put(new Integer(1), 1.00D);
        attackerDistanceModifier.get(ArmEnum.INFANTRY).put(new Integer(2), 0.80D);
        attackerDistanceModifier.get(ArmEnum.INFANTRY).put(new Integer(3), 0.60D);
        attackerDistanceModifier.get(ArmEnum.INFANTRY).put(new Integer(4), 0.50D);
        attackerDistanceModifier.get(ArmEnum.INFANTRY).put(new Integer(5), 0.40D);
        attackerDistanceModifier.get(ArmEnum.INFANTRY).put(new Integer(6), 0.35D);

        attackerDistanceModifier.put(ArmEnum.CAVALRY, new HashMap<Integer, Double>());
        attackerDistanceModifier.get(ArmEnum.CAVALRY).put(new Integer(1), 1.00D);
        attackerDistanceModifier.get(ArmEnum.CAVALRY).put(new Integer(2), 0.75D);
        attackerDistanceModifier.get(ArmEnum.CAVALRY).put(new Integer(3), 0.50D);
        attackerDistanceModifier.get(ArmEnum.CAVALRY).put(new Integer(4), 0.40D);
        attackerDistanceModifier.get(ArmEnum.CAVALRY).put(new Integer(5), 0.30D);
        attackerDistanceModifier.get(ArmEnum.CAVALRY).put(new Integer(6), 0.25D);

        attackerDistanceModifier.put(ArmEnum.ARTILLERY, new HashMap<Integer, Double>());
        attackerDistanceModifier.get(ArmEnum.ARTILLERY).put(new Integer(1), 2.50D);
        attackerDistanceModifier.get(ArmEnum.ARTILLERY).put(new Integer(2), 2.00D);
        attackerDistanceModifier.get(ArmEnum.ARTILLERY).put(new Integer(3), 1.75D);
        attackerDistanceModifier.get(ArmEnum.ARTILLERY).put(new Integer(4), 1.50D);
        attackerDistanceModifier.get(ArmEnum.ARTILLERY).put(new Integer(5), 1.00D);
        attackerDistanceModifier.get(ArmEnum.ARTILLERY).put(new Integer(6), 0.75D);

        // target distance modifier
        targetTerrainModifier.put(ArmEnum.INFANTRY, new HashMap<FieldBattleTerrainEnum, Double>());
        targetTerrainModifier.get(ArmEnum.INFANTRY).put(FieldBattleTerrainEnum.CLEAR, 1.00D);
        targetTerrainModifier.get(ArmEnum.INFANTRY).put(FieldBattleTerrainEnum.FOREST, 0.60D);
        targetTerrainModifier.get(ArmEnum.INFANTRY).put(FieldBattleTerrainEnum.ROUGH, 0.70D);
        targetTerrainModifier.get(ArmEnum.INFANTRY).put(FieldBattleTerrainEnum.VILLAGE, 0.50D);
        targetTerrainModifier.get(ArmEnum.INFANTRY).put(FieldBattleTerrainEnum.RIVER, 1.25D);
        targetTerrainModifier.get(ArmEnum.INFANTRY).put(FieldBattleTerrainEnum.BRIDGE, 1.25D);

        targetTerrainModifier.put(ArmEnum.CAVALRY, new HashMap<FieldBattleTerrainEnum, Double>());
        targetTerrainModifier.get(ArmEnum.CAVALRY).put(FieldBattleTerrainEnum.CLEAR, 1.00D);
        targetTerrainModifier.get(ArmEnum.CAVALRY).put(FieldBattleTerrainEnum.FOREST, 0.70D);
        targetTerrainModifier.get(ArmEnum.CAVALRY).put(FieldBattleTerrainEnum.ROUGH, 0.90D);
        targetTerrainModifier.get(ArmEnum.CAVALRY).put(FieldBattleTerrainEnum.RIVER, 1.25D);
        targetTerrainModifier.get(ArmEnum.CAVALRY).put(FieldBattleTerrainEnum.BRIDGE, 0.90D);

        targetTerrainModifier.put(ArmEnum.ARTILLERY, new HashMap<FieldBattleTerrainEnum, Double>());
        targetTerrainModifier.get(ArmEnum.ARTILLERY).put(FieldBattleTerrainEnum.CLEAR, 1.00D);
        targetTerrainModifier.get(ArmEnum.ARTILLERY).put(FieldBattleTerrainEnum.ROUGH, 0.80D);
        targetTerrainModifier.get(ArmEnum.ARTILLERY).put(FieldBattleTerrainEnum.RIVER, 1.25D);
        targetTerrainModifier.get(ArmEnum.ARTILLERY).put(FieldBattleTerrainEnum.BRIDGE, 1.25D);

    }

    public DefaultLongRangeFireProcessor(LongRangeProcessor longRangeProcessor) {
        super(longRangeProcessor);
    }

    @Override
    public void process(Brigade brigade, Order order) {
        if (brigade.isRouting()) {
            // routing brigades don't fire from long range
            LOGGER.debug("{} is routing, skipping long range.", brigade);
            restartTargetShotCounterIfArtillery(brigade);
            return;
        } else if (brigade.getArmTypeEnum() == ArmEnum.ARTILLERY
                && (longRangeProcessor.getParent().getBrigadesThatMovedInTheCurrentHalfRound().contains(brigade)
                || longRangeProcessor.getParent().getBrigadesThatMovedInThePreviousHalfRound().contains(brigade))) {
            // Light and heavy artillery that has moved in the same or previous half round cannot have any long range combat.
            // TODO: use army type short name to check light or heavy artillery
            LOGGER.debug("{} is artillery and has moved in the previous 2 halfrounds, skipping long range.", brigade);
            restartTargetShotCounterIfArtillery(brigade);
            return;
        } else if (longRangeProcessor.getParent().isInMeleeCombat(brigade)) {
            LOGGER.debug("{} is in melee, skipping long range.", brigade);
            restartTargetShotCounterIfArtillery(brigade);
            return;
        }

        FieldBattleSector targetSector = findTargetSector(brigade, order);

        if (targetSector != null) {

            performLongRangeAttackOnBuildings(brigade, targetSector);

            int enemySide = longRangeProcessor.getParent().findSide(brigade) == 0 ? 1 : 0;
            Brigade brigadeInTargetSector = longRangeProcessor.getParent().getBrigadeInSector(targetSector);

            if (brigadeInTargetSector != null
                    && (FRIENDLY_FIRE_ALLOWED || longRangeProcessor.getParent().findSide(brigadeInTargetSector) == enemySide)) {
                performLongRangeAttack(brigade, brigadeInTargetSector);
            } else {
                LOGGER.debug("{} will does not have a target brigade for long range.", brigade);
            }
        }
    }

    private FieldBattleSector findTargetSector(Brigade brigade, Order order) {
        FieldBattleSector targetStructureSector = null;

        targetStructureSector = findTargetStructure(brigade, order);

        if (targetStructureSector != null) {
            return targetStructureSector;
        } else {
            Brigade target = findTarget(brigade, order);
            if (target != null) {
                return longRangeProcessor.getParent().getSector(target);
            } else {
                // no target structure, no enemy in range
                return null;
            }
        }
    }

    private FieldBattleSector findTargetStructure(Brigade brigade, Order order) {

        if (brigade.getArmTypeEnum() == ArmEnum.ARTILLERY
                && order.getOrderTypeEnum() == OrdersEnum.DEFEND_POSITION
                && order.getDefendPositionArtilleryTarget() != null) {

            FieldBattleSector artilleryTargetSector = MapUtils.getSectorFromPosition(longRangeProcessor.getParent().getFbMap(),
                    order.getDefendPositionArtilleryTarget());

            if (BuildingUtils.containsBuilding(artilleryTargetSector)) {
                FieldBattleSector currentPosition = longRangeProcessor.getParent().getSector(brigade);
                int distance = MapUtils.getSectorsDistance(currentPosition, artilleryTargetSector);

                if (distance <= getRange(brigade)) {
                    return artilleryTargetSector;
                }
            }
        }
        return null;
    }

    private void performLongRangeAttackOnBuildings(Brigade brigade, FieldBattleSector targetSector) {

        if (targetSector.hasStructure()) {
            if (ArmyUtils.countBattalionsOfArm(brigade, ArmEnum.ARTILLERY) > 0) {
                removeHitPointsFromStructure(targetSector, calculateArtilleryDamageOnBuilding(brigade, targetSector));
            }
        }
    }

    private void removeHitPointsFromStructure(FieldBattleSector targetSector, int damage) {

        if (targetSector.hasSectorBridge()) {
            int newHitPoints = Math.max(0, targetSector.getBridge() - damage);
            targetSector.setBridge(newHitPoints);
            LOGGER.debug("Artillery hits bridge. New hit points: {}", newHitPoints);

        } else if (targetSector.hasSectorChateau()) {
            int newHitPoints = Math.max(0, targetSector.getChateau() - damage);
            targetSector.setChateau(newHitPoints);
            LOGGER.debug("Artillery hits chateau. New hit points: {}", newHitPoints);

        } else if (targetSector.hasSectorEntrenchment()) {
            int newHitPoints = Math.max(0, targetSector.getEntrenchment() - damage);
            targetSector.setEntrenchment(newHitPoints);
            LOGGER.debug("Artillery hits entrenchment. New hit points: {}", newHitPoints);

        } else if (targetSector.hasSectorTown()) {
            int newHitPoints = Math.max(0, targetSector.getTown() - damage);
            targetSector.setTown(newHitPoints);
            LOGGER.debug("Artillery hits town. New hit points: {}", newHitPoints);

        } else if (targetSector.hasSectorVillage()) {
            int newHitPoints = Math.max(0, targetSector.getVillage() - damage);
            targetSector.setVillage(newHitPoints);
            LOGGER.debug("Artillery hits village. New hit points: {}", newHitPoints);

        } else if (targetSector.hasSectorWall()) {
            int newHitPoints = Math.max(0, targetSector.getWall() - damage);
            targetSector.setWall(newHitPoints);
            LOGGER.debug("Artillery hits wall. New hit points: {}", newHitPoints);
        }
    }

    private void performLongRangeAttack(Brigade attacker, Brigade target) {
        performLongRangeAttack(attacker, target, true);
    }

    private void performLongRangeAttack(Brigade attacker, Brigade target, boolean includeRandomnessModifier) {

        performAttack(attacker, target, includeRandomnessModifier, false);

        // ricochet
        if (containsArtilleryBattalions(attacker)) {
            findRicochetTargetsAndPerformAttack(attacker, target, includeRandomnessModifier);
        }
    }

    private void performAttack(Brigade attacker, Brigade target, boolean includeRandomnessModifier, boolean ricochetAttack) {
        int targetBrigadeHeadCount = 0;
        for (Battalion battalion : target.getBattalions()) {
            targetBrigadeHeadCount += battalion.getHeadcount();
        }

        FieldBattleSector attackerPosition = longRangeProcessor.getParent().getSector(attacker);
        FieldBattleSector targetPosition = longRangeProcessor.getParent().getSector(target);
        FormationEnum attackerFormation = attacker.getFormationEnum();
        FormationEnum targetFormation = target.getFormationEnum();
        int distance = MapUtils.getSectorsDistance(attackerPosition, targetPosition);

        LOGGER.debug("{} vs {}", new Object[]{attacker, target});
        double totalBrigadeToBrigadeDamage = 0;

        double brigadeVsBrigadeModifiers = 1;

        // if the brigade is in range of a commander it receives an experience bonus +1
        boolean attackerInfluencedByCommander = longRangeProcessor.getParent().getCommanderProcessor().influencedByCommander(attacker);
        int commanderExperienceBonus = attackerInfluencedByCommander ? 1 : 0;
        LOGGER.debug("Commander experience bonus: {}", commanderExperienceBonus);

        // If the firing brigade has moved during the same half round,
        // then its long range fire will have 75% effectiveness
        if (longRangeProcessor.getParent().getBrigadesThatMovedInTheCurrentHalfRound().contains(attacker)) {
            brigadeVsBrigadeModifiers *= 0.75;
        }

        if (attacker.getArmTypeEnum() == ArmEnum.ARTILLERY
                && longRangeProcessor.getParent().getCommanderProcessor().influencedByCommanderOfType(attacker, CommanderType.ARTILLERY_LEADER)) {
            // Artillery troops receive a 10% bonus when influenced by an artillery leader
            LOGGER.debug("{} is an artillery influenced by a commander that is an artillery leader, 10% bonus", attacker);
            brigadeVsBrigadeModifiers *= 1.10d;
        }

        for (Battalion attackerBattalion : attacker.getBattalions()) {

            ArmEnum attackerBattalionArm = ArmyUtils.findArm(attackerBattalion);
            // only artillery can inflict ricochet damage
            if (ricochetAttack && attackerBattalionArm != ArmEnum.ARTILLERY) {
                continue;
            }

            // if the battalion does not have the range to participate in the long range attack, skip it
            // Note: in ricochet damage, the ricochet target may be outside our range by 1
            if (!ricochetAttack && distance > attackerBattalion.getType().getLongRange()) {
                continue;
            }

            // Mounted artillery brigades that have moved in the same half round cannot have long range combat
            if (attackerBattalion.getType().getType().equalsIgnoreCase("Ma")
                    && longRangeProcessor.getParent().getBrigadesThatMovedInTheCurrentHalfRound().contains(attacker)) {
                restartTargetShotCounterIfArtillery(attacker);
                continue;
            }
            double battalionFactor = calculateBattalionFactor(includeRandomnessModifier, attackerFormation, distance,
                    commanderExperienceBonus, attackerBattalion, attackerBattalionArm);

            double battalionVsBrigadeModifiers = 1;

            // ricochet losses equal to 1/3 of the damage the enemy would receive if it was the direct target
            if (ricochetAttack) {
                battalionFactor /= 3;
            }

            // artillery troops bombarding the same point for >= 3 rounds get a 30% efficiency bonus
            if (attackerBattalionArm == ArmEnum.ARTILLERY) {
                Map<Brigade, Integer> previousAttacksCounter = longRangeProcessor.getArtilleryTargets().get(attacker);
                if (previousAttacksCounter != null) {
                    Integer numberOfConsecutiveHalfRounds = previousAttacksCounter.get(target);
                    if (numberOfConsecutiveHalfRounds != null && numberOfConsecutiveHalfRounds >= 3) {
                        battalionVsBrigadeModifiers *= 1.3;
                    }
                }
            }

            if (attackerBattalion.getType().getType().equalsIgnoreCase("Ma")
                    && longRangeProcessor.getParent().getBrigadesThatMovedInTheCurrentHalfRound().contains(attacker)) {
                // Mounted artillery brigades that have moved in the same half round cannot have long range combat
                restartTargetShotCounterIfArtillery(attackerBattalion);
                continue;
            } else if (attackerBattalion.getType().getType().equalsIgnoreCase("La")
                    || attackerBattalion.getType().getType().equalsIgnoreCase("Ha")
                    && (longRangeProcessor.getParent().getBrigadesThatMovedInTheCurrentHalfRound().contains(attacker)
                    || longRangeProcessor.getParent().getBrigadesThatMovedInThePreviousHalfRound().contains(attacker))) {
                // Light and heavy artillery that has moved in the same or previous half round cannot have any long range combat.
                restartTargetShotCounterIfArtillery(attacker);
                continue;
            }

            if (!ricochetAttack && attackerBattalionArm == ArmEnum.ARTILLERY) {
                maintainArtilleryTargets(attackerBattalion, target);
            }

            for (Battalion targetBattalion : target.getBattalions()) {

                ArmEnum targetBattalionArm = ArmyUtils.findArm(targetBattalion);

                double battalionLevelModifiers = 1;

                try {
                    battalionLevelModifiers *= (targetTypeModifier.get(targetBattalionArm).get(targetFormation) == null
                            ? 1D
                            : targetTypeModifier.get(targetBattalionArm).get(targetFormation))
                            * (targetTerrainModifier.get(targetBattalionArm).get(getTerrainType(targetPosition)) == null
                            ? 1D
                            : targetTerrainModifier.get(targetBattalionArm).get(getTerrainType(targetPosition)));
                } catch (NullPointerException e) {
                    LOGGER.error("Target arm: {}, target formation: {}, target position: {}", new Object[]{targetBattalionArm, targetFormation, targetPosition});
                }

                // Cuirassiers suffer 30% fewer casualties from long range fire
                if (targetBattalion.getType().getTroopSpecsCu()) {
                    battalionLevelModifiers *= 0.7d;
                }

                double battalionToBattalionFactor = battalionFactor
                        * targetBattalion.getHeadcount() / targetBrigadeHeadCount;

                double battalionToBattalionDamage = battalionToBattalionFactor * brigadeVsBrigadeModifiers *
                        battalionVsBrigadeModifiers * battalionLevelModifiers;
                totalBrigadeToBrigadeDamage += battalionToBattalionDamage;
                longRangeProcessor.increaseBattalionDamage(targetBattalion, battalionToBattalionDamage);

//        		LOGGER.debug("|-- battalion {} inflicting damage of {}(tot:{}) men to battalion {}", 
//        				new Object[]{attackerBattalion, battalionToBattalionDamage, longRangeProcessor.getDamageToInflict().get(targetBattalion), targetBattalion});
            }
//        	LOGGER.debug("|-- Total damage so far: {}", totalBrigadeToBrigadeDamage);
        }

        LOGGER.debug("TOTAL: {} inflicting {} damage of {} men to {}",
                new Object[]{attacker, ricochetAttack ? "ricochet" : "", totalBrigadeToBrigadeDamage, target});

        int side = longRangeProcessor.getParent().findSide(attacker);
        longRangeProcessor.getParent().getFieldBattleLog().logLongRangeAttack(side, attacker, target, ricochetAttack, totalBrigadeToBrigadeDamage);

    }

    private double calculateBattalionFactor(boolean includeRandomnessModifier, FormationEnum attackerFormation, int distance,
                                            int commanderExperienceBonus, Battalion attackerBattalion, ArmEnum attackerBattalionArm) {
    	double battalionFactor = 0d;
    	try {
        battalionFactor = attackerBattalion.getType().getLongCombat()
                * (attackerBattalion.getExperience() + commanderExperienceBonus)
                * attackerBattalion.getHeadcount()
                * attackerTypeModifier.get(attackerBattalionArm).get(attackerFormation)
                * attackerDistanceModifier.get(attackerBattalionArm).get(6 <= distance ? 6 : distance)
                / 2000;

    	} catch (NullPointerException e) {
    		throw e;
    	}
        
        if (includeRandomnessModifier) {
            battalionFactor *= MathUtils.generateRandomDoubleInRange(LONG_RANGE_RANDOMNESS_MODIFIER_MIN, LONG_RANGE_RANDOMNESS_MODIFIER_MAX);
        }
        return battalionFactor;
    }

    private boolean containsArtilleryBattalions(Brigade attacker) {
        for (Battalion battalion : attacker.getBattalions()) {
            if (ArmyUtils.findArm(battalion) == ArmEnum.ARTILLERY) {
                return true;
            }
        }
        return false;
    }

    private void findRicochetTargetsAndPerformAttack(Brigade attacker, Brigade target, boolean includeRandomnessModifier) {

        int ourSide = longRangeProcessor.getParent().getSideBrigades()[0].contains(attacker) ? 0 : 1;
        int enemySide = ourSide == 0 ? 1 : 0;
        FieldBattleSector attackerPosition = longRangeProcessor.getParent().getSector(attacker);
        FieldBattleSector targetPosition = longRangeProcessor.getParent().getSector(target);
        Set<FieldBattleSector> ricochetSectors = RicochetCalculator.findRicochetSectors(attackerPosition, targetPosition);

        Set<Brigade> ricochetTargers = longRangeProcessor.getParent().findBrigadesOfSide(ricochetSectors, enemySide);

        for (Brigade ricochetTarget : ricochetTargers) {
            performAttack(attacker, ricochetTarget, includeRandomnessModifier, true);
        }
    }

    private FieldBattleTerrainEnum getTerrainType(FieldBattleSector sector) {
        FieldBattleTerrainEnum terrainType = null;
        if (sector.hasSectorBridge()) {
            terrainType = FieldBattleTerrainEnum.BRIDGE;
        } else if (sector.isForest()) {
            terrainType = FieldBattleTerrainEnum.FOREST;
        } else if (sector.isMajorRiver() || sector.isMinorRiver() || sector.isLake()) {
            terrainType = FieldBattleTerrainEnum.RIVER;
        } else if (sector.isBush()) {
            terrainType = FieldBattleTerrainEnum.ROUGH;
        } else if (sector.hasSectorVillage() || sector.hasSectorChateau() || sector.hasSectorTown()) {
            terrainType = FieldBattleTerrainEnum.VILLAGE;
        } else {
            terrainType = FieldBattleTerrainEnum.CLEAR;
        }
        return terrainType;
    }

    private Brigade findTarget(Brigade brigade, Order order) {
        FieldBattleSector attackerPosition = longRangeProcessor.getParent().getSector(brigade);
        int brigadeSide = longRangeProcessor.getParent().findSide(brigade);
        int enemySide = brigadeSide == 0 ? 1 : 0;
        int range = getRange(brigade);

        Brigade target = null;

        Set<FieldBattleSector> radiusSectors = MapUtils.findSectorsInRadius(attackerPosition, range);
        Set<Brigade> enemiesWithinRange = longRangeProcessor.getParent().findBrigadesOfSide(radiusSectors, enemySide);

        // remove brigades that are participating in melee combat
        Iterator<Brigade> it = enemiesWithinRange.iterator();
        while (it.hasNext()) {
            Brigade enemy = it.next();
            if (longRangeProcessor.getParent().isInMeleeCombat(enemy)) {
                it.remove();
            }
        }

        Set<Brigade> visibleEnemiesWithinRange = new HashSet<Brigade>();
        for (Brigade enemyWithinRange : enemiesWithinRange) {
            if (longRangeProcessor.getParent().getFieldBattleVisibilityProcessor().visibleForLongRange(brigade, enemyWithinRange)) {
                visibleEnemiesWithinRange.add(enemyWithinRange);
            }
        }

        Brigade preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForLongRangeAttack(brigade,
                visibleEnemiesWithinRange, order, longRangeProcessor.getParent());
        if (preferredEnemy != null) {
            target = preferredEnemy;
        } else {
            target = additionalOrderBrigadeFilter.findClosestInDistance(brigade,
                    visibleEnemiesWithinRange, longRangeProcessor.getParent());
        }

        return target;
    }

    private void restartTargetShotCounterIfArtillery(Brigade brigade) {
        for (Battalion battalion : brigade.getBattalions()) {
            restartTargetShotCounterIfArtillery(battalion);
        }
    }

    private void restartTargetShotCounterIfArtillery(Battalion battalion) {
        ArmEnum battalionArm = ArmyUtils.findArm(battalion);
        if (battalionArm == ArmEnum.ARTILLERY) {
            maintainArtilleryTargets(battalion, null);
        }
    }

    private void maintainArtilleryTargets(Battalion artilleryBattalion, Brigade target) {

        Map<Brigade, Integer> targetShots = longRangeProcessor.getArtilleryTargets().get(artilleryBattalion);
        if (targetShots == null
                || !targetShots.keySet().contains(target)) {
            // second condition is for when target has changed
            Map<Brigade, Integer> initializeCurrentTargetShots = new HashMap<Brigade, Integer>();
            initializeCurrentTargetShots.put(target, 0);
            longRangeProcessor.getArtilleryTargets().put(artilleryBattalion, initializeCurrentTargetShots);
            targetShots = initializeCurrentTargetShots;
        }
        // increase by 1
        Integer previousShots = targetShots.get(target);
        targetShots.put(target, previousShots + 1);
    }

    private int getRange(Brigade brigade) {

        int effectiveRange = 0;
        for (Battalion battalion : brigade.getBattalions()) {

            int battalionRange = battalion.getType().getLongRange();
            effectiveRange = effectiveRange < battalionRange ? battalionRange : effectiveRange;

        }
        return effectiveRange;
    }

    private int calculateArtilleryDamageOnBuilding(Brigade brigade, FieldBattleSector targetSector) {

        FieldBattleSector attackerPosition = longRangeProcessor.getParent().getSector(brigade);
        int distance = MapUtils.getSectorsDistance(attackerPosition, targetSector);
        int commanderExperienceBonus = calculateCommanderExperienceBonus(brigade);

        double damage = 0d;
        for (Battalion battalion : brigade.getBattalions()) {
            if (battalion.getType().isArtillery()) {
                damage += calculateBattalionFactor(true, brigade.getFormationEnum(), distance, commanderExperienceBonus, battalion, ArmEnum.ARTILLERY);
            }
        }

        int oldHitPoints = 0;

        if (targetSector.hasSectorBridge()) {
            oldHitPoints = targetSector.getBridge();
            damage *= PONTOON_BRIDGE_ARTILLERY_MODIFIER;
        } else if (targetSector.hasSectorChateau()) {
            oldHitPoints = targetSector.getChateau();
            damage *= VILLAGE_TOWN_CHATEAU_WALL_ARTILLERY_MODIFIER;
        } else if (targetSector.hasSectorEntrenchment()) {
            oldHitPoints = targetSector.getEntrenchment();
            damage *= VILLAGE_TOWN_CHATEAU_WALL_ARTILLERY_MODIFIER;
        } else if (targetSector.hasSectorTown()) {
            oldHitPoints = targetSector.getTown();
            damage *= VILLAGE_TOWN_CHATEAU_WALL_ARTILLERY_MODIFIER;
        } else if (targetSector.hasSectorVillage()) {
            oldHitPoints = targetSector.getVillage();
            damage *= VILLAGE_TOWN_CHATEAU_WALL_ARTILLERY_MODIFIER;
        } else if (targetSector.hasSectorWall()) {
            oldHitPoints = targetSector.getWall();
            damage *= VILLAGE_TOWN_CHATEAU_WALL_ARTILLERY_MODIFIER;
        }

        longRangeProcessor.getParent().getFieldBattleLog().logStructureAffected(targetSector.getX(),
                targetSector.getY(), targetSector.getStructureType(), brigade, oldHitPoints, (int) -damage);

        return (int) damage;
    }

    private int calculateCommanderExperienceBonus(Brigade brigade) {
        boolean attackerInfluencedByCommander = longRangeProcessor.getParent().getCommanderProcessor().influencedByCommander(brigade);
        return attackerInfluencedByCommander ? 1 : 0;
    }

}
