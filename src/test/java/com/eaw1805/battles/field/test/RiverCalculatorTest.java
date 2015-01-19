package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.generation.calculators.MapDimensionsCalculator;
import com.eaw1805.battles.field.generation.calculators.RiverCalculator;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.map.Sector;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class RiverCalculatorTest {

    private MapDimensionsCalculator mapDimensionsCalc;
    private RiverCalculator riverCalc;

    @Before
    public void setUp() {
        Sector sector = new Sector();
        sector.setPopulation(0);
        mapDimensionsCalc = new MapDimensionsCalculator();
        riverCalc = new RiverCalculator();
    }


    @Test
    public void testAddMinorRiver() {

        for (int i = 0; i < 100; i++) {

            List<List<Brigade>> sideBrigade = FieldBattleTestUtils.initializeSideBrigades(300);

            FieldBattleMap fbMap = mapDimensionsCalc.createEmptyMap(sideBrigade);
            riverCalc.addMinorRiver(fbMap);

            VisualisationUtils.visualize(fbMap);
        }

    }

    @Test
    public void testAddMinorRiver50x45() {


        for (int i = 0; i < 100; i++) {

            FieldBattleMap fbMap = new FieldBattleMap(45, 40);
            riverCalc.addMinorRiver(fbMap);

            VisualisationUtils.visualize(fbMap);
        }

    }

    @Test
    public void testAddNonTraversingMinorRiver() {

        for (int i = 0; i < 100; i++) {

            FieldBattleMap fbMap = new FieldBattleMap(45, 40);
//			FieldBattleMap fbMap = new FieldBattleMap(50, 45);
//			FieldBattleMap fbMap = new FieldBattleMap(60, 50);
//			FieldBattleMap fbMap = new FieldBattleMap(65, 55);

            riverCalc.addNonTraversingMinorRiver(fbMap);

            VisualisationUtils.visualize(fbMap);
        }

    }

    @Test
    public void testAddMajorRiver() {

        for (int i = 0; i < 100; i++) {

//			FieldBattleMap fbMap = new FieldBattleMap(45, 40);
//			FieldBattleMap fbMap = new FieldBattleMap(50, 45);
//			FieldBattleMap fbMap = new FieldBattleMap(60, 50);
            FieldBattleMap fbMap = new FieldBattleMap(65, 55);

            riverCalc.addMajorRiver(fbMap);

            VisualisationUtils.visualize(fbMap);
        }

    }

}
