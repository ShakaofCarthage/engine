package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.generation.calculators.MapDimensionsCalculator;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EmptyMapCalculatorTest {

    private MapDimensionsCalculator mapDimensionsCalc;

    @Before
    public void setUp() {
        mapDimensionsCalc = new MapDimensionsCalculator();
    }


    @Test
    public void testCreateEmptyTerrain() {

        List<List<Brigade>> sideBrigades = new ArrayList<List<Brigade>>();

        FieldBattleMap fbt = null;

        // create 300 battalions
        sideBrigades = FieldBattleTestUtils.initializeSideBrigades(300);
        fbt = mapDimensionsCalc.createEmptyMap(sideBrigades);

        assertEquals(45, fbt.getSizeX());
        assertEquals(40, fbt.getSizeY());

        // create 400 battalions
        sideBrigades = FieldBattleTestUtils.initializeSideBrigades(400);
        fbt = mapDimensionsCalc.createEmptyMap(sideBrigades);

        assertEquals(50, fbt.getSizeX());
        assertEquals(45, fbt.getSizeY());

        // create 650 battalions
        sideBrigades = FieldBattleTestUtils.initializeSideBrigades(650);
        fbt = mapDimensionsCalc.createEmptyMap(sideBrigades);

        assertEquals(60, fbt.getSizeX());
        assertEquals(50, fbt.getSizeY());

        // create 800 battalions
        sideBrigades = FieldBattleTestUtils.initializeSideBrigades(800);
        fbt = mapDimensionsCalc.createEmptyMap(sideBrigades);

        assertEquals(65, fbt.getSizeX());
        assertEquals(55, fbt.getSizeY());

    }

}
