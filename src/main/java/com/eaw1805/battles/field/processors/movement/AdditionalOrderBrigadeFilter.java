package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.ProcessorUtils;
import com.eaw1805.battles.field.utils.FieldBattleCollectionUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class picks the preferred brigades based on the additional order.
 *
 * @author fragkakis
 */
public class AdditionalOrderBrigadeFilter {

    /**
     * Returns the preferred enemy to move towards.
     *
     * @param brigade              the brigade
     * @param enemies              the enemies
     * @param pathCalculator       the path calculator
     * @param order                the given order
     * @param fieldBattleProcessor the field battle processor
     * @return
     */
    public Brigade getPreferredEnemyForMovement(Brigade brigade, Set<Brigade> enemies,
                                                BaseFieldBattlePathCalculator pathCalculator, Order order, FieldBattleProcessor fieldBattleProcessor) {

        Set<Brigade> preferredEnemies = filterEnemies(enemies, order);

        Brigade preferredEnemy = null;
        if (order.isTargetHighestHeadcount()) {
            preferredEnemy = getHighestHeadcount(preferredEnemies);
        } else {
            preferredEnemy = findClosestInMovementPoints(brigade, preferredEnemies, pathCalculator, fieldBattleProcessor);
        }

        return preferredEnemy;
    }

    /**
     * Returns the preferred enemy for long-range attack.
     *
     * @param brigade              the brigade
     * @param enemies              the enemies
     * @param order                the given order
     * @param fieldBattleProcessor the field battle processor
     * @return
     */
    public Brigade getPreferredEnemyForLongRangeAttack(Brigade brigade, Set<Brigade> enemies,
                                                       Order order, FieldBattleProcessor fieldBattleProcessor) {

        Set<Brigade> preferredEnemies = filterEnemies(enemies, order);

        Brigade preferredEnemy = null;
        if (order.isTargetHighestHeadcount()) {
            preferredEnemy = getHighestHeadcount(preferredEnemies);
        } else {
            preferredEnemy = findClosestInDistance(brigade, preferredEnemies, fieldBattleProcessor);
        }

        return preferredEnemy;
    }

    public Brigade findClosestInDistance(Brigade brigade, Set<Brigade> preferredEnemies, FieldBattleProcessor fieldBattleProcessor) {

        FieldBattleSector brigadeSector = fieldBattleProcessor.getSector(brigade);
        Set<FieldBattleSector> preferredSectors = fieldBattleProcessor.getSectors(preferredEnemies);
        Set<FieldBattleSector> closestSectors = MapUtils.findClosest(brigadeSector, preferredSectors);

        Brigade closestEnemy = null;
        if (!closestSectors.isEmpty()) {
            closestEnemy = fieldBattleProcessor.getBrigadeInSector(FieldBattleCollectionUtils.getRandom(closestSectors));
        }
        return closestEnemy;
    }

    /**
     * Return the preferred enemies in respect of nation, arm and formation.
     *
     * @param enemies the enemies
     * @param order   the order
     * @return the preferred enemies
     */
    public Set<Brigade> filterEnemies(Set<Brigade> enemies, Order order) {
        Set<Brigade> preferredEnemies = new HashSet<Brigade>(enemies);

        filterNations(preferredEnemies, order.getTargetNations());
        filterArm(preferredEnemies, order.getTargetArm());
        filterFormation(preferredEnemies, order.getTargetFormation());
        return preferredEnemies;
    }

    private void filterNations(Set<Brigade> preferredEnemies, Set<Nation> targetNations) {

        if (targetNations != null && !targetNations.isEmpty()) {

            Iterator<Brigade> it = preferredEnemies.iterator();
            while (it.hasNext()) {
                Brigade brigade = it.next();
                if (!targetNations.contains(brigade.getNation())) {
                    it.remove();
                }
            }
        }
    }

    private void filterArm(Set<Brigade> preferredEnemies, String targetArmStr) {

        if (targetArmStr != null && !targetArmStr.isEmpty()) {

            ArmEnum targetArm = ArmEnum.valueOf(targetArmStr);
            Iterator<Brigade> it = preferredEnemies.iterator();

            while (it.hasNext()) {
                Brigade brigade = it.next();
                if (targetArm != brigade.getArmTypeEnum()) {
                    it.remove();
                }
            }
        }
    }

    private void filterFormation(Set<Brigade> preferredEnemies, String targetFormationStr) {

        if (targetFormationStr != null && !targetFormationStr.isEmpty()) {

            FormationEnum targetFormation = FormationEnum.valueOf(targetFormationStr);
            Iterator<Brigade> it = preferredEnemies.iterator();

            while (it.hasNext()) {
                Brigade brigade = it.next();
                if (targetFormation != brigade.getFormationEnum()) {
                    it.remove();
                }
            }
        }
    }

    public Brigade getHighestHeadcount(Set<Brigade> enemies) {

        Brigade highestHeadcountBrigade = null;

        if (!enemies.isEmpty()) {

            Set<Brigade> highestHeadcountBrigades = new HashSet<Brigade>();
            int highestHeadCount = 0;

            Iterator<Brigade> it = enemies.iterator();

            while (it.hasNext()) {
                Brigade brigade = it.next();

                int headCount = calculateHeadCount(brigade);
                if (highestHeadCount < headCount) {
                    highestHeadCount = headCount;
                    highestHeadcountBrigades.clear();
                    highestHeadcountBrigades.add(brigade);
                } else if (highestHeadCount == headCount) {
                    highestHeadcountBrigades.add(brigade);
                }
            }

            highestHeadcountBrigade = ProcessorUtils.getRandom(highestHeadcountBrigades);
        }

        return highestHeadcountBrigade;
    }

    private int calculateHeadCount(Brigade brigade) {
        int headCount = 0;
        for (Battalion battalion : brigade.getBattalions()) {
            headCount += battalion.getHeadcount();
        }
        return headCount;
    }

    private Brigade findClosestInMovementPoints(Brigade brigade, Set<Brigade> enemies,
                                                BaseFieldBattlePathCalculator pathCalculator, FieldBattleProcessor fieldBattleProcessor) {

        Brigade closestInRangeBrigade = null;

        if (!enemies.isEmpty()) {

            FieldBattleSector currentLocation = fieldBattleProcessor.getSector(brigade);
            Set<Brigade> closestInRangeBrigades = new HashSet<Brigade>();
            int nearestEnemyCost = Integer.MAX_VALUE;

            for (Brigade enemy : enemies) {
                FieldBattleSector enemySector = fieldBattleProcessor.getSector(enemy);

                Set<FieldBattleSector> enemySectorNeighbours = MapUtils.getHorizontalAndVerticalNeighbours(enemySector);

                if (enemySectorNeighbours.contains(currentLocation)) {
                    // we are already next to an enemy, no need to move
                    if (nearestEnemyCost > 0) {
                        closestInRangeBrigades.clear();
                    }
                    nearestEnemyCost = 0;
                    closestInRangeBrigades.add(enemy);
                    continue;
                }

                for (FieldBattleSector enemySectorNeighbour : enemySectorNeighbours) {
                    int enemySectorNeighbourCost = pathCalculator.findCost(currentLocation, enemySectorNeighbour,
                            brigade.getArmTypeEnum(), brigade.getFormationEnum(), false);

                    if (enemySectorNeighbourCost < nearestEnemyCost) {
                        nearestEnemyCost = enemySectorNeighbourCost;
                        closestInRangeBrigades.clear();
                        closestInRangeBrigades.add(enemy);
                    } else if (enemySectorNeighbourCost == nearestEnemyCost) {
                        closestInRangeBrigades.add(enemy);
                    }
                }
            }

            closestInRangeBrigade = ProcessorUtils.getRandom(closestInRangeBrigades);
        }

        return closestInRangeBrigade;
    }
}
