package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.generation.calculators.ForestCalculator;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import org.junit.Before;
import org.junit.Test;

public class ForestCalculatorTest {

    private ForestCalculator forestCalc;
    private FieldBattleMap fbMap;

    @Before
    public void setUp() {

        fbMap = new FieldBattleMap(50, 45);

        forestCalc = new ForestCalculator(fbMap);
    }


    @Test
    public void testAddForestsBySectorsNumber() {

        forestCalc.addForestsByPercentage(80);

        VisualisationUtils.visualize(fbMap);

    }

}
