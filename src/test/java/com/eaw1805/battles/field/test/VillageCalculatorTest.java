package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.calculators.SetupAreaCalculator;
import com.eaw1805.battles.field.generation.calculators.VillageCalculator;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.map.Terrain;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class VillageCalculatorTest {

    private VillageCalculator villageCalc;
    private BattleField bf;
    private FieldBattleMap fbMap;

    @Before
    public void setUp() {

        Terrain terrain = new Terrain();
        terrain.setId(TerrainConstants.TERRAIN_S);
        terrain.setName("chios");
        Sector sector = new Sector();
        sector.setTerrain(terrain);
        sector.setPopulation(0);

        bf = new BattleField(sector);

        Nation nation0 = new Nation();
        nation0.setId(0);
        bf.addNation(0, nation0);

        Nation nation1 = new Nation();
        nation1.setId(1);
        bf.addNation(0, nation1);

        Nation nation2 = new Nation();
        nation2.setId(2);
        bf.addNation(1, nation2);

        fbMap = new FieldBattleMap(50, 45);

        // Setup areas
        SetupAreaCalculator setupAreaCalc = new SetupAreaCalculator();
        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(bf, fbMap);

        villageCalc = new VillageCalculator(fbMap, setupAreas, bf);
    }


    @Test
    public void testAddVillages() {

        villageCalc.addSetupAreaVillages();

        VisualisationUtils.visualize(fbMap);

    }

    @Test
    public void testAddOtherVillages() {

        villageCalc.addOtherVillages(30);

        VisualisationUtils.visualize(fbMap);

    }

}
