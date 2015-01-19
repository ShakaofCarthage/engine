package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.Set;

/**
 * Helper class that adds lakes to a field battle map.
 *
 * @author fragkakis
 */
public class LakeCalculator {

    private FieldBattleMap fbMap;

    public LakeCalculator(FieldBattleMap fbMap) {
        this.fbMap = fbMap;
    }


    /**
     * Adds a lake to the field battle map
     *
     * @param fbMap
     */
    public void addLake() {

        ClusterCalculator clusterCalc = new ClusterCalculator();

        Set<FieldBattleSector> sectors = clusterCalc.findRandomCluster(fbMap, MathUtils.generateRandomIntInRange(20, 50), ClusterTypeEnum.WATER);


        for (FieldBattleSector sector : sectors) {
            sector.setLake(true);
        }

    }

}
