package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.calculators.FortCalculator;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FortCalculatorTest {

    private SetupAreaCalculator setupAreaCalc;
    private FortCalculator fortCalc;

    private FieldBattleMap fbMap;
    private BattleField mockBf;
    private List<Nation> side0;
    private List<Nation> side1;

    @Before
    public void setUp() {
        Sector sector = new Sector();
        sector.setPopulation(0);
        setupAreaCalc = new SetupAreaCalculator();

        initialize();
    }

    private void initialize() {
        fbMap = new FieldBattleMap(45, 40);
        mockBf = mock(BattleField.class);
        side0 = new ArrayList<Nation>();
        Nation nation0 = new Nation();
        nation0.setId(0);
        side0.add(nation0);
        side1 = new ArrayList<Nation>();
        Nation nation1 = new Nation();
        nation1.setId(1);
        side1.add(nation1);
    }

    @Test
    public void testAddFort_45x40_1vs1() {

        initialize();

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        fortCalc = new FortCalculator(fbMap, setupAreas);

        fortCalc.addFort(mockBf.getSide(0).get(0), 4, 4, 1000);
        fortCalc.addFort(mockBf.getSide(1).get(0), 4, 4, 1000);

        VisualisationUtils.visualize(fbMap);
    }

    @Test
    public void testAddRoad_50x45_2vs2() {

        initialize();

        Nation nation2 = new Nation();
        nation2.setId(2);
        side0.add(nation2);

        Nation nation3 = new Nation();
        nation3.setId(3);
        side1.add(nation3);

        fbMap = new FieldBattleMap(50, 45);

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        fortCalc = new FortCalculator(fbMap, setupAreas);
        fortCalc.addFort(mockBf.getSide(0).get(0), 8, 4, 1000);
        fortCalc.addFort(mockBf.getSide(0).get(1), 8, 6, 1000);
        fortCalc.addFort(mockBf.getSide(1).get(0), 8, 4, 1000);
        fortCalc.addFort(mockBf.getSide(1).get(1), 8, 6, 1000);

        VisualisationUtils.visualize(fbMap);
    }

    @Test
    public void testAddRoad_60x50_3vs3() {

        initialize();

        Nation nation2 = new Nation();
        nation2.setId(2);
        Nation nation3 = new Nation();
        nation3.setId(3);
        Nation nation4 = new Nation();
        nation4.setId(4);
        Nation nation5 = new Nation();
        nation5.setId(5);

        side1.add(nation2);
        side1.add(nation3);
        side0.add(nation4);
        side0.add(nation5);

        fbMap = new FieldBattleMap(60, 50);

        when(mockBf.getSide(0)).thenReturn(side0);
        when(mockBf.getSide(1)).thenReturn(side1);

        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        fortCalc = new FortCalculator(fbMap, setupAreas);
        fortCalc.addFort(mockBf.getSide(0).get(0), 8, 4, 1000);
        fortCalc.addFort(mockBf.getSide(0).get(1), 8, 6, 1000);
        fortCalc.addFort(mockBf.getSide(0).get(2), 10, 8, 1000);
        fortCalc.addFort(mockBf.getSide(1).get(0), 8, 4, 1000);
        fortCalc.addFort(mockBf.getSide(1).get(1), 8, 6, 1000);
        fortCalc.addFort(mockBf.getSide(1).get(2), 10, 8, 1000);

        VisualisationUtils.visualize(fbMap);
    }
}
