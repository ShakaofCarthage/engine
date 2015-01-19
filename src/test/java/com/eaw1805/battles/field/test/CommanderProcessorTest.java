package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.FieldBattleProcessorResourceLocator;
import com.eaw1805.battles.field.test.FieldBattleTestUtils.TerrainConstantsEnum;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommanderProcessorTest {


    private FieldBattleProcessor fieldBattleProcessor;
    private BattleField bf;
    private FieldBattleMap fbMap;
    private List<List<Brigade>> sideBrigades;
    private FieldBattleProcessorResourceLocator resourceLocator = mock(FieldBattleProcessorResourceLocator.class);
    private Order basicOrder0;
    private Order basicOrder1;
    private Order basicOrder5;
    private Brigade brigade0;
    private Brigade brigade1;
    private Brigade brigade5;
    private Commander commander1;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void setup() {

        fbMap = new FieldBattleMap(45, 40);

        sideBrigades = new ArrayList<List<Brigade>>();
        sideBrigades.add(new ArrayList<Brigade>());
        sideBrigades.add(new ArrayList<Brigade>());

        Nation nation0 = new Nation();
        nation0.setId(0);
        Nation nation1 = new Nation();
        nation1.setId(1);

        List<Brigade> sideBrigades0 = new ArrayList<Brigade>();
        List<Brigade> sideBrigades1 = new ArrayList<Brigade>();

        brigade0 = FieldBattleTestUtils.initializeBrigade(0, 9, FormationEnum.COLUMN, 8, nation0);
        FieldBattleTestUtils.addBattalions(brigade0, 300, ArmEnum.INFANTRY, false);
        basicOrder0 = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.MOVE_TO_ENGAGE, FormationEnum.LINE);
        brigade0.setBasicOrder(basicOrder0);
        sideBrigades.get(0).add(brigade0);

        brigade1 = FieldBattleTestUtils.initializeBrigade(1, 9, FormationEnum.COLUMN, 8, nation0);
        FieldBattleTestUtils.addBattalions(brigade1, 300, ArmEnum.INFANTRY, false);
        basicOrder1 = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.MOVE_TO_ENGAGE, FormationEnum.LINE);
        brigade1.setBasicOrder(basicOrder1);
        brigade1.setFieldBattleCommanderId(1);
        sideBrigades.get(0).add(brigade1);

        brigade5 = FieldBattleTestUtils.initializeBrigade(5, 6, FormationEnum.COLUMN, 8, nation1);
        FieldBattleTestUtils.addBattalions(brigade5, 800, ArmEnum.INFANTRY, false);
        basicOrder5 = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.MOVE_TO_ENGAGE, FormationEnum.LINE);
        brigade5.setBasicOrder(basicOrder5);
        sideBrigades.get(1).add(brigade5);

        bf = FieldBattleTestUtils.prepareBattleField(TerrainConstantsEnum.PLAINS.getId(), 9, 3, 2, 2);
        bf.getSide(0).clear();
        bf.getSide(0).add(nation0);
        bf.getSide(1).clear();
        bf.getSide(1).add(nation1);
        when(resourceLocator.getFieldBattleMap(anyInt())).thenReturn(fbMap);

        commander1 = new Commander();
        commander1.setStrc(25);
        when(resourceLocator.getCommanderById(1)).thenReturn(commander1);

        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
        fieldBattleProcessor.setBattleField(bf);
        List[] sideBrigadesArray = new List[2];
        sideBrigadesArray[0] = sideBrigades0;
        sideBrigadesArray[1] = sideBrigades1;
        fieldBattleProcessor.setSideBrigades(sideBrigadesArray);

        fieldBattleProcessor.setResourceLocator(resourceLocator);

        when(resourceLocator.getFieldBattleReport(anyInt())).thenReturn(new FieldBattleReport());

    }

    @Test
    public void defaultMeleeBattle() {

        commander1.setFearlessAttacker(true);

        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(30, 2));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade5.setFieldBattlePosition(new FieldBattlePosition(15, 38));
        basicOrder5.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void cavalryMeleeBattle() {

        commander1.setCavalryLeader(true);

        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);
        FieldBattleTestUtils.addBattalions(brigade0, 300, ArmEnum.CAVALRY, false);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(30, 2));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade5.setFieldBattlePosition(new FieldBattlePosition(15, 38));
        basicOrder5.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void artilleryBattle() {

        commander1.setArtilleryLeader(true);

        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);
        basicOrder0.setFormationEnum(FormationEnum.COLUMN);
        FieldBattleTestUtils.addBattalions(brigade0, 300, ArmEnum.ARTILLERY, false);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(30, 2));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);

        brigade5.setFieldBattlePosition(new FieldBattlePosition(15, 38));
        basicOrder5.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);

        fieldBattleProcessor.processFirstHalfRounds(1);

    }

    @Test
    public void stoutDefenderBattle() {

        commander1.setStoutDefender(true);

        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(30, 2));
        basicOrder1.setCheckpoint1(new FieldBattlePosition(15, 1));
        basicOrder1.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);

        brigade5.setFieldBattlePosition(new FieldBattlePosition(15, 38));
        FieldBattleTestUtils.addBattalions(brigade5, 800, ArmEnum.CAVALRY, false);
        basicOrder5.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);

    }

}
