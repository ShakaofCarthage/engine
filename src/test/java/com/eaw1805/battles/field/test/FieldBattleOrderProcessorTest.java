package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.orders.FieldBattleOrderProcessor;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FieldBattleOrderProcessorTest {

    private FieldBattleProcessor fbProcessorMock;
    private FieldBattleOrderProcessor fieldBattleOrderProcessor;
    private BattleField battleFieldMock;
    private Order basicOrder;
    private Order additionalOrder;
    private Brigade brigade;
    private Nation nation0;
    private Nation nation1;
    private FieldBattleMap fbMap;


    @Before
    public void setUp() {
        brigade = new Brigade();
        FieldBattleTestUtils.addBattalions(brigade, 10, ArmEnum.INFANTRY, false);

        basicOrder = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.ATTACK_AND_REFORM, FormationEnum.COLUMN);
        brigade.setBasicOrder(basicOrder);
        additionalOrder = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.ATTACK_AND_REFORM, FormationEnum.COLUMN);
        brigade.setAdditionalOrder(additionalOrder);
        brigade.setNation(nation0);

        fbProcessorMock = mock(FieldBattleProcessor.class);
        battleFieldMock = mock(BattleField.class);
        when(fbProcessorMock.getBattleField()).thenReturn(battleFieldMock);

        fieldBattleOrderProcessor = new FieldBattleOrderProcessor(fbProcessorMock);

        nation0 = new Nation();
        nation0.setId(0);
        nation1 = new Nation();
        nation1.setId(1);

        when(battleFieldMock.getSide(0)).thenReturn(Arrays.asList(new Nation[]{nation0}));
        when(battleFieldMock.getSide(1)).thenReturn(Arrays.asList(new Nation[]{nation1}));

        fbMap = new FieldBattleMap(10, 10);
        fbMap.getFieldBattleSector(0, 0).setNation(nation0);
        fbMap.getFieldBattleSector(1, 1).setNation(nation0);
        fbMap.getFieldBattleSector(2, 2).setNation(nation1);
        fbMap.getFieldBattleSector(3, 3).setNation(nation1);
        when(fbProcessorMock.getFbMap()).thenReturn(fbMap);
    }

    @Test
    public void testNoAdditionalOrder() {
        brigade.setAdditionalOrder(null);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);
    }

    @Test
    public void testRoundTrigger() {
        when(fbProcessorMock.getCurrentRound()).thenReturn(17);

        additionalOrder.setActivationRound(18);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);

        additionalOrder.setActivationRound(17);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        additionalOrder.setActivationRound(10);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);
    }

    @Test
    public void testHeadCountTrigger() {

        additionalOrder.setHeadCountThreshold(200);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        additionalOrder.setHeadCountThreshold(59);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);
    }

    @Test
    public void testLastDestinationReachedTrigger() {

        additionalOrder.setLastDestinationReached(true);

        basicOrder.setCheckpoint1(new FieldBattlePosition(5, 5));
        basicOrder.setReachedCheckpoint1(true);
        basicOrder.setCheckpoint2(new FieldBattlePosition(6, 6));
        basicOrder.setReachedCheckpoint2(true);
        basicOrder.setCheckpoint3(new FieldBattlePosition(7, 7));
        basicOrder.setReachedCheckpoint3(true);

        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        basicOrder.setReachedCheckpoint3(false);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);
    }

    @Test
    public void testEnemySideCapturedOwnStrategicPoint() {

        additionalOrder.setEnemySideCapturedOwnStrategicPoint(true);

        fbMap.getFieldBattleSector(0, 0).setCurrentHolder(nation1);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        fbMap.getFieldBattleSector(0, 0).setCurrentHolder(nation0);
        fbMap.getFieldBattleSector(1, 1).setCurrentHolder(nation1);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        fbMap.getFieldBattleSector(1, 1).setCurrentHolder(nation0);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);
    }

    @Test
    public void testEnemySideCapturedCustomOwnStrategicPoint() {

        additionalOrder.setEnemySideCapturedOwnStrategicPoint(true);
        additionalOrder.setCustomStrategicPoint1(new FieldBattlePosition(0, 0));

        fbMap.getFieldBattleSector(0, 0).setCurrentHolder(nation1);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        fbMap.getFieldBattleSector(0, 0).setCurrentHolder(nation0);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);

        fbMap.getFieldBattleSector(1, 1).setCurrentHolder(nation1);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);

    }

    @Test
    public void testOwnSideCapturedEnemyStrategicPoint() {

        additionalOrder.setOwnSideCapturedEnemyStrategicPoint(true);

        fbMap.getFieldBattleSector(2, 2).setCurrentHolder(nation0);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        fbMap.getFieldBattleSector(2, 2).setCurrentHolder(nation1);
        fbMap.getFieldBattleSector(3, 3).setCurrentHolder(nation0);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        fbMap.getFieldBattleSector(3, 3).setCurrentHolder(nation1);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);
    }

    @Test
    public void testOwnSideCapturedCustomEnemyStrategicPoint() {

        additionalOrder.setOwnSideCapturedEnemyStrategicPoint(true);
        additionalOrder.setCustomStrategicPoint1(new FieldBattlePosition(2, 2));

        fbMap.getFieldBattleSector(2, 2).setCurrentHolder(nation0);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), additionalOrder);

        fbMap.getFieldBattleSector(2, 2).setCurrentHolder(nation1);
        fbMap.getFieldBattleSector(3, 3).setCurrentHolder(nation0);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);

        fbMap.getFieldBattleSector(3, 3).setCurrentHolder(nation1);
        assertSame(fieldBattleOrderProcessor.findCurrentOrder(brigade), basicOrder);
    }


}
