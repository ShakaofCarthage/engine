package com.eaw1805.battles.field.melee;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderProcessor;
import com.eaw1805.battles.field.processors.melee.BuildPontoonBridgeMeleeProcessor;
import com.eaw1805.battles.field.test.FieldBattleTestUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildPontoonBridgeMeleeProcessorTest {

    private BuildPontoonBridgeMeleeProcessor buildPontoonBridgeMeleeProcessor;
    private Brigade brigade;
    private Order order;


    @Before
    public void setUp() throws Exception {
        order = new Order();
        order.setConstructionCounter(0);
        Nation nation = new Nation();
        brigade = FieldBattleTestUtils.initializeBrigade(0, 0, FormationEnum.COLUMN, 7, nation);
        FieldBattleTestUtils.addBattalions(brigade, 800, ArmEnum.INFANTRY, true);

        CommanderProcessor commanderProcessor = mock(CommanderProcessor.class);
        when(commanderProcessor.influencedByCommander(brigade)).thenReturn(false);
        FieldBattleProcessor fieldBattleProcessor = mock(FieldBattleProcessor.class);
        when(fieldBattleProcessor.getCommanderProcessor()).thenReturn(commanderProcessor);
        MeleeProcessor meleeProcessor = new MeleeProcessor(fieldBattleProcessor);
        buildPontoonBridgeMeleeProcessor = new BuildPontoonBridgeMeleeProcessor(meleeProcessor);

        when(fieldBattleProcessor.findSide(brigade)).thenReturn(0);
        FieldBattleMap fbMap = new FieldBattleMap(10, 10);
        fbMap.getFieldBattleSector(5, 6).setMinorRiver(true);
        when(fieldBattleProcessor.getSector(brigade)).thenReturn(fbMap.getFieldBattleSector(5, 5));

    }

    @Test
    public void testPerformOrderMeleeAction() {

        buildPontoonBridgeMeleeProcessor.performOrderMeleeAction(brigade, order);

        assertTrue(order.getConstructionCounter() > 0);
    }

}
