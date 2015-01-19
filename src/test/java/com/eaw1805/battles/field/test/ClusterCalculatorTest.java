package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.generation.calculators.ClusterCalculator;
import com.eaw1805.battles.field.generation.calculators.ClusterTypeEnum;
import com.eaw1805.battles.field.generation.calculators.MapDimensionsCalculator;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.map.Sector;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class ClusterCalculatorTest {

    private MapDimensionsCalculator mapDimensionsCalc;
    private ClusterCalculator clusterCalc;

    @Before
    public void setUp() {
        Sector sector = new Sector();
        sector.setPopulation(0);
        mapDimensionsCalc = new MapDimensionsCalculator();
        clusterCalc = new ClusterCalculator();
    }

    @Test
    public void testFindFreeArea() {

        for (int k = 0; k < 1000; k++) {

            List<List<Brigade>> sideBrigades = FieldBattleTestUtils.initializeSideBrigades(300);
            FieldBattleMap fbMap = mapDimensionsCalc.createEmptyMap(sideBrigades);

            for (int i = 5; i <= 30; i += 6) {
                Set<FieldBattleSector> sectors = clusterCalc.findRandomCluster(fbMap, i, ClusterTypeEnum.VILLAGE);
                for (FieldBattleSector sector : sectors) {
                    sector.setVillage(123);
                }
            }

            VisualisationUtils.visualize(fbMap);

        }

    }

    @Test
    public void testFindRandomClusterInRegion() {

        for (int i = 0; i <= 100; i++) {
            FieldBattleMap fbMap = new FieldBattleMap(15, 15);

            Set<FieldBattleSector> sectors = clusterCalc.findRandomClusterInRegion(fbMap, 7, 0, 4, 0, 4, 10, ClusterTypeEnum.TOWN);
            for (FieldBattleSector sector : sectors) {
                sector.setTown(23);
            }

            sectors = clusterCalc.findRandomClusterInRegion(fbMap, 7, 0, 5, fbMap.getSizeY() - 1 - 4, fbMap.getSizeY() - 1, 10, ClusterTypeEnum.TOWN);
            for (FieldBattleSector sector : sectors) {
                sector.setTown(34);
            }

            sectors = clusterCalc.findRandomClusterInRegion(fbMap, 7, fbMap.getSizeX() - 1 - 4, fbMap.getSizeX() - 1, 0, 4, 10, ClusterTypeEnum.TOWN);
            for (FieldBattleSector sector : sectors) {
                sector.setTown(45);
            }

            sectors = clusterCalc.findRandomClusterInRegion(fbMap, 7, fbMap.getSizeX() - 1 - 4, fbMap.getSizeX() - 1, fbMap.getSizeY() - 1 - 4, fbMap.getSizeY() - 1, 10, ClusterTypeEnum.TOWN);
            for (FieldBattleSector sector : sectors) {
                sector.setTown(56);
            }

            VisualisationUtils.visualize(fbMap);
        }
    }

    @Test
    public void testCalculateSquareSectors() {
        FieldBattleMap fbMap = new FieldBattleMap(15, 20);

        FieldBattleSector startingSector = fbMap.getFieldBattleSector(14, 19);
        Set<FieldBattleSector> cluster = clusterCalc.calculateSquareSectors(startingSector, 4);
        for (FieldBattleSector sector : cluster) {
            sector.setForest(true);
        }

        startingSector = fbMap.getFieldBattleSector(0, 0);
        cluster = clusterCalc.calculateSquareSectors(startingSector, 4);
        for (FieldBattleSector sector : cluster) {
            sector.setForest(true);
        }

        startingSector = fbMap.getFieldBattleSector(0, 19);
        cluster = clusterCalc.calculateSquareSectors(startingSector, 4);
        for (FieldBattleSector sector : cluster) {
            sector.setForest(true);
        }

        startingSector = fbMap.getFieldBattleSector(14, 0);
        cluster = clusterCalc.calculateSquareSectors(startingSector, 4);
        for (FieldBattleSector sector : cluster) {
            sector.setForest(true);
        }

        startingSector = fbMap.getFieldBattleSector(8, 10);
        cluster = clusterCalc.calculateSquareSectors(startingSector, 4);
        for (FieldBattleSector sector : cluster) {
            sector.setForest(true);
        }

        VisualisationUtils.visualize(fbMap);

    }
}
