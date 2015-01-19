package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.generation.calculators.ChateauCalculator;
import com.eaw1805.battles.field.generation.calculators.MapDimensionsCalculator;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.map.Sector;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChateauCalculatorTest {

    private MapDimensionsCalculator mapDimensionsCalc;

    @Before
    public void setUp() {
        Sector sector = new Sector();
        sector.setPopulation(0);
        mapDimensionsCalc = new MapDimensionsCalculator();
    }


    @Test
    public void testAddChateaus() {

        List<List<Brigade>> sideBrigades = FieldBattleTestUtils.initializeSideBrigades(300);

        // check 3 chateaus added
        FieldBattleMap fbTerrain1 = mapDimensionsCalc.createEmptyMap(sideBrigades);
        ChateauCalculator chateauCalc = new ChateauCalculator();
        chateauCalc.addChateaus(fbTerrain1, 3);

        int chateauCount1 = 0;
        for (int x = 0; x < fbTerrain1.getSizeX(); x++) {
            for (int y = 0; y < fbTerrain1.getSizeY(); y++) {
                if (fbTerrain1.getFieldBattleSector(x, y).hasSectorChateau()) {
                    chateauCount1++;
                }
            }
        }
        VisualisationUtils.visualize(fbTerrain1);

        assertEquals("Unexpected chateau count.", 3, chateauCount1);

        // check 5 chateaus added
        FieldBattleMap fbTerrain2 = mapDimensionsCalc.createEmptyMap(sideBrigades);
        chateauCalc.addChateaus(fbTerrain2, 5);

        int chateauCount2 = 0;
        for (int x = 0; x < fbTerrain2.getSizeX(); x++) {
            for (int y = 0; y < fbTerrain2.getSizeY(); y++) {
                if (fbTerrain2.getFieldBattleSector(x, y).hasSectorChateau()) {
                    chateauCount2++;
                }
            }
        }

        VisualisationUtils.visualize(fbTerrain2);

        assertEquals("Unexpected chateau count.", 5, chateauCount2);


    }

}
