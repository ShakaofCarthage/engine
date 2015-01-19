package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.FieldBattleProcessorResourceLocator;
import com.eaw1805.battles.field.test.FieldBattleTestUtils.TerrainConstantsEnum;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldBattleProcessorTest {


    private FieldBattleProcessor fieldBattleProcessor;
    private BattleField bf;
    private FieldBattleMap fbMap;
    private List<List<Brigade>> sideBrigades;
    private FieldBattleProcessorResourceLocator resourceLocator = mock(FieldBattleProcessorResourceLocator.class);

    @Before
    public void setup() {

        fbMap = new FieldBattleMap(45, 40);

        sideBrigades = new ArrayList<List<Brigade>>();
        sideBrigades.add(new ArrayList<Brigade>());
        sideBrigades.add(new ArrayList<Brigade>());

        when(resourceLocator.getFieldBattleReport(anyInt())).thenReturn(new FieldBattleReport());

        List<Brigade> sideBrigades0 = new ArrayList<Brigade>();
        Brigade brigade0 = new Brigade();
        brigade0.setBattalions(new HashSet<Battalion>());
        sideBrigades0.add(brigade0);
        Order basicOrder0 = new Order();
        basicOrder0.setActivationRound(0);
        basicOrder0.setFormationEnum(FormationEnum.COLUMN);
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);
//        basicOrder0.setCheckpoint1(new FieldBattlePosition(20, 39));
        brigade0.setBasicOrder(basicOrder0);
        brigade0.setFormationEnum(FormationEnum.COLUMN);
        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        addBattalions(brigade0, 800);
        brigade0.setArmTypeEnum(ArmyUtils.findArm(brigade0));
        brigade0.setMps(8);
        sideBrigades.get(0).add(brigade0);

        List<Brigade> sideBrigades1 = new ArrayList<Brigade>();
        Brigade brigade1 = new Brigade();
        brigade1.setBattalions(new HashSet<Battalion>());
        sideBrigades1.add(brigade1);
        Order basicOrder1 = new Order();
        basicOrder1.setActivationRound(0);
        basicOrder1.setFormationEnum(FormationEnum.COLUMN);
        basicOrder1.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);
        basicOrder1.setCheckpoint1(new FieldBattlePosition(35, 30));
        brigade1.setBasicOrder(basicOrder1);
        brigade1.setFormationEnum(FormationEnum.COLUMN);
        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        addBattalions(brigade1, 300);
        brigade1.setArmTypeEnum(ArmyUtils.findArm(brigade1));
        brigade1.setMps(8);
        sideBrigades.get(1).add(brigade1);

        Brigade brigade2 = new Brigade();
        brigade2.setBattalions(new HashSet<Battalion>());
        sideBrigades1.add(brigade2);
        Order basicOrder2 = new Order();
        basicOrder2.setActivationRound(0);
        basicOrder2.setFormationEnum(FormationEnum.COLUMN);
        basicOrder2.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);
        basicOrder2.setCheckpoint1(new FieldBattlePosition(36, 31));
        brigade2.setBasicOrder(basicOrder2);
        brigade2.setFormationEnum(FormationEnum.COLUMN);
        brigade2.setFieldBattlePosition(new FieldBattlePosition(20, 39));
        addBattalions(brigade2, 300);
        brigade2.setArmTypeEnum(ArmyUtils.findArm(brigade2));
        brigade2.setMps(8);
        sideBrigades.get(1).add(brigade2);

        Brigade brigade3 = new Brigade();
        brigade3.setBattalions(new HashSet<Battalion>());
        sideBrigades1.add(brigade3);
        Order basicOrder3 = new Order();
        basicOrder3.setActivationRound(0);
        basicOrder3.setFormationEnum(FormationEnum.COLUMN);
        basicOrder3.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);
        basicOrder3.setCheckpoint1(new FieldBattlePosition(37, 32));
        brigade3.setBasicOrder(basicOrder3);
        brigade3.setFormationEnum(FormationEnum.COLUMN);
        brigade3.setFieldBattlePosition(new FieldBattlePosition(19, 39));
        addBattalions(brigade3, 300);
        brigade3.setArmTypeEnum(ArmyUtils.findArm(brigade3));
        brigade3.setMps(8);
        sideBrigades.get(1).add(brigade3);

        bf = FieldBattleTestUtils.prepareBattleField(TerrainConstantsEnum.PLAINS.getId(), 9, 3, 2, 2);
        when(resourceLocator.getFieldBattleMap(anyInt())).thenReturn(fbMap);

    }


    @Test
    @SuppressWarnings("unchecked")
    public void processFirstHalfRoundsTest() {

        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
        fieldBattleProcessor.setBattleField(bf);
        List<Brigade>[] sideBrigadesArray = (List<Brigade>[]) Array.newInstance(List.class, 2);
        sideBrigadesArray[0] = sideBrigades.get(0);
        sideBrigadesArray[1] = sideBrigades.get(1);
        fieldBattleProcessor.setSideBrigades(sideBrigadesArray);
        fieldBattleProcessor.setResourceLocator(resourceLocator);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }


    private void addBattalions(Brigade brigade, Integer battalionHeadCount) {
        for (int i = 0; i < 6; i++) {
            Battalion battalion = new Battalion();

            battalion.setHeadcount(battalionHeadCount == null ? MathUtils.generateRandomIntInRange(300, 800) : battalionHeadCount);
            ArmyType armyType = new ArmyType();
            armyType.setName("ar");
            armyType.setType("ar");
            armyType.setLongRange(8);
            armyType.setLongCombat(7);
            armyType.setHandCombat(8);
            battalion.setType(armyType);
            battalion.setExperience(3);
            brigade.getBattalions().add(battalion);
        }
    }
}
