package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.morale.MoraleChecker;
import com.eaw1805.battles.field.processors.commander.CommanderProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.victory.PursuitProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.map.Terrain;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PursuitProcessorTest {

    private PursuitProcessor pursuitProcessor;
    private CommanderProcessor commanderProcessor;
    private MoraleChecker moraleChecker;
    private FieldBattleProcessor fbProcessor;
    private Brigade brigade0;
    private Brigade brigade1;
    private Brigade brigade2;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        fbProcessor = mock(FieldBattleProcessor.class);
        commanderProcessor = mock(CommanderProcessor.class);
        moraleChecker = mock(MoraleChecker.class);

        Sector sector = new Sector();
        Terrain terrain = new Terrain();
        terrain.setTerrainFactor(1);
        sector.setTerrain(terrain);
        BattleField bf = new BattleField(sector);
        when(fbProcessor.getBattleField()).thenReturn(bf);


        when(fbProcessor.getCommanderProcessor()).thenReturn(commanderProcessor);
        when(fbProcessor.getMoraleChecker()).thenReturn(moraleChecker);

        when(commanderProcessor.sideHasAliveCommanderOfType(anyInt(), any(CommanderType.class))).thenReturn(true);
        when(moraleChecker.calculateArmyMorale(anyInt())).thenReturn(60);

        List<Brigade>[] sideBrigades = new List[2];
        ArrayList<Brigade> losingSideBrigades = new ArrayList<Brigade>();
        brigade0 = FieldBattleTestUtils.buildCavalryBrigade(6, 100, 5, true, false);
        brigade1 = FieldBattleTestUtils.buildCavalryBrigade(6, 100, 3, false, true);
        brigade2 = FieldBattleTestUtils.buildCavalryBrigade(6, 100, 7, true, false);
        losingSideBrigades.add(brigade0);
        losingSideBrigades.add(brigade1);
        losingSideBrigades.add(brigade2);

        sideBrigades[0] = losingSideBrigades;

        when(fbProcessor.getSideBrigades()).thenReturn(sideBrigades);

        pursuitProcessor = new PursuitProcessor(fbProcessor);
    }

    @Test
    public void calculatePursuitPoints() {
        assertEquals(13770, pursuitProcessor.calculatePursuitPoints(0, false));
    }

    @Test
    public void inflictDamage() {

        assertEquals(600, ArmyUtils.findBrigadeHeadCount(brigade0));
        assertEquals(600, ArmyUtils.findBrigadeHeadCount(brigade1));
        assertEquals(600, ArmyUtils.findBrigadeHeadCount(brigade2));

        pursuitProcessor.inflictDamage(300, 0);

        assertEquals(498, ArmyUtils.findBrigadeHeadCount(brigade0));
        assertEquals(498, ArmyUtils.findBrigadeHeadCount(brigade1));
        assertEquals(498, ArmyUtils.findBrigadeHeadCount(brigade2));
    }

}
