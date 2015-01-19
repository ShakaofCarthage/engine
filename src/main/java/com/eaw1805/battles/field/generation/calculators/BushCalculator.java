package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.Set;

/**
 * Helper class that adds bush to a field battle map.
 *
 * @author fragkakis
 */
public class BushCalculator {

    private FieldBattleMap fbMap;

    public BushCalculator(FieldBattleMap fbMap) {
        this.fbMap = fbMap;
    }

    /**
     * Adds forests of the specified size until they cover the specified percentage.
     *
     * @param targetBushSectors the number of sectors the bush must cover
     * @param minForestSize     the minimum forest size
     * @param maxForestSize     the maximum forest size
     */
    public void addBushBySectorsNumber(int targetBushSectors, int minBushSize, int maxBushSize) {

        int bushCount = 0;

        while (bushCount < targetBushSectors) {
            int bushSize = MathUtils.generateRandomIntInRange(minBushSize, maxBushSize);
            try {
                addBush(bushSize);
            } catch (EffortCountExceededException e) {
                continue;
            }
            bushCount += bushSize;
        }

    }

    /**
     * Adds a forest to the field battle map
     *
     * @param fbMap
     */
    private void addBush(int forestSize) {

        ClusterCalculator clusterCalc = new ClusterCalculator();

        Set<FieldBattleSector> sectors = clusterCalc.findRandomCluster(fbMap, forestSize, 10, ClusterTypeEnum.BUSH);

        for (FieldBattleSector sector : sectors) {
            if (sector.isEmpty()) {
                sector.setBush(true);
            }
        }

    }

}
