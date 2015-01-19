package com.eaw1805.battles.field.morale;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class is responsible for calculating the modifier for the rally morale
 * check of routing brigades.
 *
 * @author fragkakis
 */
public class RallyCalculator {

    private final FieldBattleProcessor fieldBattleProcessor;
    private static final Logger LOGGER = LoggerFactory.getLogger(RallyCalculator.class);

    public RallyCalculator(FieldBattleProcessor fieldBattleProcessor) {
        this.fieldBattleProcessor = fieldBattleProcessor;
    }

    /**
     * Computes the rally modifier of a rally brigade.
     *
     * @param brigade
     * @return
     */
    public int computeRallyModifier(Brigade brigade) {

        FieldBattleSector currentLocation = fieldBattleProcessor.getSector(brigade);
        int side = fieldBattleProcessor.findSide(brigade);

        int rallyModifier = 0;

        // if it is within 5 tiles (and has line of sight) of an unbroken friendly elite or a crack unit,
        // it receives a 10% bonus
        Set<FieldBattleSector> sectorsInRange5 = MapUtils.findSectorsInRadius(currentLocation, 5);
        Set<Brigade> alliesInRange5 = fieldBattleProcessor.findBrigadesOfSide(sectorsInRange5, side);
        if (!alliesInRange5.isEmpty()) {
            checkForCrackAndEliteAllies:
            for (Brigade ally : alliesInRange5) {
                if (!ally.isRouting()) {
                    for (Battalion allyBattalion : ally.getBattalions()) {
                        if (allyBattalion.getType().getCrack()
                                || allyBattalion.getType().getElite()) {
                            LOGGER.debug("There is a non-routing crack or elite ally within 5 tiles, receiving 10% rally bonus");
                            rallyModifier += 10;
                            break checkForCrackAndEliteAllies;
                        }
                    }
                }
            }
        }

        Set<FieldBattleSector> sectorsInRange10 = MapUtils.findSectorsInRadius(currentLocation, 10);
        int enemySide = side == 0 ? 1 : 0;
        Set<Brigade> enemiesInRange10 = fieldBattleProcessor.findBrigadesOfSide(sectorsInRange10, enemySide);

        // if no enemy is within 10 tiles, it receives a 10% bonus
        if (enemiesInRange10.isEmpty()) {
            LOGGER.debug("No enemies within 10 tiles, receiving 10% rally bonus");
            rallyModifier += 10;
        } else {
            // if an enemy non-routing cavalry unit is within 10 tiles, it receives a 10% penalty
            checkForEnemyCavalry:
            for (Brigade enemy : enemiesInRange10) {
                if (!enemy.isRouting()) {
                    for (Battalion enemyBattalion : enemy.getBattalions()) {
                        if (enemyBattalion.getType().isCavalry()) {
                            LOGGER.debug("There is a non-routing cavalry within 10 tiles, receiving 10% rally penalty");
                            rallyModifier -= 10;
                            break checkForEnemyCavalry;
                        }
                    }
                }
            }
        }

        return rallyModifier;
    }

}
