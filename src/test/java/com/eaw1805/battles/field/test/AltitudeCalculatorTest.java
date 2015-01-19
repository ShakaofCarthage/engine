package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.MapBuilder;
import com.eaw1805.battles.field.generation.calculators.AltitudeCalculator;
import com.eaw1805.battles.field.generation.calculators.SetupAreaCalculator;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.map.Terrain;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AltitudeCalculatorTest {

    private AltitudeCalculator altitudeCalc;

    private FieldBattleMap fbMap;
    private BattleField mockBf;
    private Map<Nation, FieldBattleSetupArea> setupAreas;

    @Before
    public void setUp() {

        initialize();
    }

    private void initialize() {
        Terrain terrain = new Terrain();
        terrain.setId(TerrainConstants.TERRAIN_G);
        terrain.setName("chios");
        terrain.setMaxDensity(MathUtils.generateRandomIntInRange(7, 9));
        Sector sector = new Sector();
        sector.setTerrain(terrain);
        sector.setPopulation(0);
        mockBf = new BattleField(sector);

        Nation nation0 = new Nation();
        nation0.setId(0);
        mockBf.addNation(0, nation0);

        Nation nation1 = new Nation();
        nation1.setId(1);
        mockBf.addNation(0, nation1);

        Nation nation2 = new Nation();
        nation2.setId(2);
        mockBf.addNation(1, nation2);

        Nation nation3 = new Nation();
        nation3.setId(3);
        mockBf.addNation(1, nation3);

        MapBuilder mapBuilder = new MapBuilder(mockBf, FieldBattleTestUtils.initializeSideBrigades(300));
        fbMap = mapBuilder.buildMap();

        SetupAreaCalculator setupAreaCalc = new SetupAreaCalculator();
        setupAreas = setupAreaCalc.computeSetupAreas(mockBf, fbMap);

        altitudeCalc = new AltitudeCalculator(fbMap, setupAreas, mockBf);

    }


    @Test
    public void testCalculateAltitude() {
        for (int i = 0; i < 100; i++) {

            setUp();

            System.out.println("Field Battle #" + i);
            VisualisationUtils.visualize(fbMap);
            VisualisationUtils.visualizeAltitude(fbMap);
            System.out.println("\n\n\n");
        }
    }

    @Test
    public void testMinimap() {

        for (int i = 0; i < 100; i++) {
            FieldBattleMap fbMap = new FieldBattleMap(5, 5);

            altitudeCalc = new AltitudeCalculator(fbMap, new HashMap<Nation, FieldBattleSetupArea>(), mockBf);

//			fbMap.getFieldBattleSector(2, 2).setAltitude(3);
            altitudeCalc.getRemainingSectorsPerAltitude().put(1, 15);
            altitudeCalc.getRemainingSectorsPerAltitude().put(2, 10);
            altitudeCalc.getRemainingSectorsPerAltitude().put(3, 0);
            altitudeCalc.getRemainingSectorsPerAltitude().put(4, 0);

            altitudeCalc.getAltitudePercentages().put(3, 0);
            altitudeCalc.getAltitudePercentages().put(4, 0);

            altitudeCalc.calculateAltitude();

            VisualisationUtils.visualizeAltitude(fbMap);
        }
    }

}
