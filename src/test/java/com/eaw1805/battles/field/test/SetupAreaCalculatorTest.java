package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.calculators.SetupAreaCalculator;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;
import com.eaw1805.data.model.map.Sector;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetupAreaCalculatorTest {

    private SetupAreaCalculator setupAreaCalc;

    @Before
    public void setUp() {
        Sector sector = new Sector();
        sector.setPopulation(0);
        setupAreaCalc = new SetupAreaCalculator();
    }


    @Test
    public void testComputeSetupAreasForSide_45x40_1nationPerSide() {

        FieldBattleMap fbMap = new FieldBattleMap(45, 40);
        BattleField mockBf = mock(BattleField.class);
        List<Nation> side0 = new ArrayList<Nation>();
        Nation nation0 = new Nation();
        nation0.setId(0);
        side0.add(nation0);
        List<Nation> side1 = new ArrayList<Nation>();
        Nation nation1 = new Nation();
        nation1.setId(1);
        side1.add(nation1);

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        FieldBattleSetupArea setupArea0 = setupAreas.get(nation0);

        assertEquals(12, setupArea0.getStartX());
        assertEquals(31, setupArea0.getEndX());
        assertEquals(0, setupArea0.getStartY());
        assertEquals(9, setupArea0.getEndY());

        FieldBattleSetupArea setupArea1 = setupAreas.get(nation1);

        assertEquals(12, setupArea1.getStartX());
        assertEquals(31, setupArea1.getEndX());
        assertEquals(30, setupArea1.getStartY());
        assertEquals(39, setupArea1.getEndY());

        VisualisationUtils.visualize(fbMap);
        VisualisationUtils.visualizeDimensions(fbMap);

    }

    @Test
    public void testComputeSetupAreasForSide_45x40_2nationsPerSide() {

        FieldBattleMap fbMap = new FieldBattleMap(45, 40);
        BattleField mockBf = mock(BattleField.class);
        List<Nation> side0 = new ArrayList<Nation>();
        Nation nation0a = new Nation();
        Nation nation0b = new Nation();
        nation0a.setId(0);
        nation0b.setId(1);
        side0.add(nation0a);
        side0.add(nation0b);
        List<Nation> side1 = new ArrayList<Nation>();
        Nation nation1a = new Nation();
        Nation nation1b = new Nation();
        nation1a.setId(3);
        nation1b.setId(4);
        side1.add(nation1a);
        side1.add(nation1b);

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        FieldBattleSetupArea setupArea0a = setupAreas.get(nation0a);

        assertEquals(6, setupArea0a.getStartX());
        assertEquals(21, setupArea0a.getEndX());
        assertEquals(0, setupArea0a.getStartY());
        assertEquals(9, setupArea0a.getEndY());

        FieldBattleSetupArea setupArea0b = setupAreas.get(nation0b);

        assertEquals(22, setupArea0b.getStartX());
        assertEquals(37, setupArea0b.getEndX());
        assertEquals(0, setupArea0b.getStartY());
        assertEquals(9, setupArea0b.getEndY());

        FieldBattleSetupArea setupArea1a = setupAreas.get(nation1a);

        assertEquals(6, setupArea1a.getStartX());
        assertEquals(21, setupArea1a.getEndX());
        assertEquals(30, setupArea1a.getStartY());
        assertEquals(39, setupArea1a.getEndY());

        FieldBattleSetupArea setupArea1b = setupAreas.get(nation1b);

        assertEquals(22, setupArea1b.getStartX());
        assertEquals(37, setupArea1b.getEndX());
        assertEquals(30, setupArea1b.getStartY());
        assertEquals(39, setupArea1b.getEndY());

        VisualisationUtils.visualize(fbMap);

    }

    @Test
    public void testComputeSetupAreasForSide_45x40_3nationsPerSide() {

        FieldBattleMap fbMap = new FieldBattleMap(45, 40);
        BattleField mockBf = mock(BattleField.class);
        List<Nation> side0 = new ArrayList<Nation>();
        Nation nation0a = new Nation();
        Nation nation0b = new Nation();
        Nation nation0c = new Nation();
        nation0a.setId(0);
        nation0b.setId(1);
        nation0c.setId(2);
        side0.add(nation0a);
        side0.add(nation0b);
        side0.add(nation0c);
        List<Nation> side1 = new ArrayList<Nation>();
        Nation nation1a = new Nation();
        Nation nation1b = new Nation();
        Nation nation1c = new Nation();
        nation1a.setId(3);
        nation1b.setId(4);
        nation1c.setId(5);
        side1.add(nation1a);
        side1.add(nation1b);
        side1.add(nation1c);

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        FieldBattleSetupArea setupArea0a = setupAreas.get(nation0a);

        assertEquals(0, setupArea0a.getStartX());
        assertEquals(14, setupArea0a.getEndX());
        assertEquals(0, setupArea0a.getStartY());
        assertEquals(9, setupArea0a.getEndY());

        FieldBattleSetupArea setupArea0b = setupAreas.get(nation0b);

        assertEquals(15, setupArea0b.getStartX());
        assertEquals(29, setupArea0b.getEndX());
        assertEquals(0, setupArea0b.getStartY());
        assertEquals(9, setupArea0b.getEndY());


        FieldBattleSetupArea setupArea0c = setupAreas.get(nation0c);

        assertEquals(30, setupArea0c.getStartX());
        assertEquals(44, setupArea0c.getEndX());
        assertEquals(0, setupArea0c.getStartY());
        assertEquals(9, setupArea0c.getEndY());

        FieldBattleSetupArea setupArea1a = setupAreas.get(nation1a);

        assertEquals(0, setupArea1a.getStartX());
        assertEquals(14, setupArea1a.getEndX());
        assertEquals(30, setupArea1a.getStartY());
        assertEquals(39, setupArea1a.getEndY());

        FieldBattleSetupArea setupArea1b = setupAreas.get(nation1b);

        assertEquals(15, setupArea1b.getStartX());
        assertEquals(29, setupArea1b.getEndX());
        assertEquals(30, setupArea1b.getStartY());
        assertEquals(39, setupArea1b.getEndY());

        FieldBattleSetupArea setupArea1c = setupAreas.get(nation1c);

        assertEquals(30, setupArea1c.getStartX());
        assertEquals(44, setupArea1c.getEndX());
        assertEquals(30, setupArea1c.getStartY());
        assertEquals(39, setupArea1c.getEndY());

        VisualisationUtils.visualize(fbMap);

    }

    @Test
    public void testComputeSetupAreasForSide_50x45_3nationsPerSide() {

        FieldBattleMap fbMap = new FieldBattleMap(50, 45);
        BattleField mockBf = mock(BattleField.class);
        List<Nation> side0 = new ArrayList<Nation>();
        Nation nation0a = new Nation();
        Nation nation0b = new Nation();
        Nation nation0c = new Nation();
        nation0a.setId(0);
        nation0b.setId(1);
        nation0c.setId(2);
        side0.add(nation0a);
        side0.add(nation0b);
        side0.add(nation0c);
        List<Nation> side1 = new ArrayList<Nation>();
        Nation nation1a = new Nation();
        Nation nation1b = new Nation();
        Nation nation1c = new Nation();
        nation1a.setId(3);
        nation1b.setId(4);
        nation1c.setId(5);
        side1.add(nation1a);
        side1.add(nation1b);
        side1.add(nation1c);

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        FieldBattleSetupArea setupArea0a = setupAreas.get(nation0a);

        assertEquals(1, setupArea0a.getStartX());
        assertEquals(16, setupArea0a.getEndX());
        assertEquals(0, setupArea0a.getStartY());
        assertEquals(9, setupArea0a.getEndY());

        FieldBattleSetupArea setupArea0b = setupAreas.get(nation0b);

        assertEquals(17, setupArea0b.getStartX());
        assertEquals(32, setupArea0b.getEndX());
        assertEquals(0, setupArea0b.getStartY());
        assertEquals(9, setupArea0b.getEndY());


        FieldBattleSetupArea setupArea0c = setupAreas.get(nation0c);

        assertEquals(33, setupArea0c.getStartX());
        assertEquals(48, setupArea0c.getEndX());
        assertEquals(0, setupArea0c.getStartY());
        assertEquals(9, setupArea0c.getEndY());

        FieldBattleSetupArea setupArea1a = setupAreas.get(nation1a);

        assertEquals(1, setupArea1a.getStartX());
        assertEquals(16, setupArea1a.getEndX());
        assertEquals(35, setupArea1a.getStartY());
        assertEquals(44, setupArea1a.getEndY());

        FieldBattleSetupArea setupArea1b = setupAreas.get(nation1b);

        assertEquals(17, setupArea1b.getStartX());
        assertEquals(32, setupArea1b.getEndX());
        assertEquals(35, setupArea1b.getStartY());
        assertEquals(44, setupArea1b.getEndY());

        FieldBattleSetupArea setupArea1c = setupAreas.get(nation1c);

        assertEquals(33, setupArea1c.getStartX());
        assertEquals(48, setupArea1c.getEndX());
        assertEquals(35, setupArea1c.getStartY());
        assertEquals(44, setupArea1c.getEndY());

        VisualisationUtils.visualize(fbMap);

    }

}
