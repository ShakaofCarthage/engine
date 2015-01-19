package com.eaw1805.battles.field.test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.morale.MoraleChecker;
import com.eaw1805.battles.field.orders.FieldBattleOrderProcessor;
import com.eaw1805.battles.field.processors.LongRangeProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.test.FieldBattleTestUtils.TerrainConstantsEnum;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LongRangeFireProcessorTest {

    private FieldBattleProcessor fieldBattleProcessor;
    private LongRangeProcessor longRangeProcessor;
    private Brigade brigade0;
    private BattleField bf;
    private Brigade brigade1;
    private Brigade brigade2;


    @Before
    @SuppressWarnings("unchecked")
    public void setup() {

        FieldBattleMap fbMap = new FieldBattleMap(45, 40);

        List<Brigade>[] sideBrigadesArray = (List<Brigade>[]) Array.newInstance(List.class, 2);

        List<Brigade> sideBrigades0 = new ArrayList<Brigade>();
        brigade0 = new Brigade();
        brigade0.setBattalions(new HashSet<Battalion>());
        sideBrigades0.add(brigade0);
        Order basicOrder0 = new Order();
        basicOrder0.setActivationRound(0);
        basicOrder0.setCheckpoint1(new FieldBattlePosition(20, 40));
        basicOrder0.setOrderTypeEnum(OrdersEnum.ENGAGE_IF_IN_RANGE);
        brigade0.setBasicOrder(basicOrder0);
        brigade0.setFormationEnum(FormationEnum.LINE);
        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 19));
        FieldBattleTestUtils.addBattalions(brigade0, 100, ArmEnum.INFANTRY, false);
        brigade0.setArmTypeEnum(ArmyUtils.findArm(brigade0));
        brigade0.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);

        List<Brigade> sideBrigades1 = new ArrayList<Brigade>();
        brigade1 = new Brigade();
        brigade1.setBattalions(new HashSet<Battalion>());
        sideBrigades1.add(brigade1);
        Order basicOrder1 = new Order();
        basicOrder1.setActivationRound(0);
        basicOrder1.setCheckpoint1(new FieldBattlePosition(20, 0));
        basicOrder1.setOrderTypeEnum(OrdersEnum.ENGAGE_IF_IN_RANGE);
        brigade1.setBasicOrder(basicOrder1);
        brigade1.setFormationEnum(FormationEnum.LINE);
        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, 21));
        FieldBattleTestUtils.addBattalions(brigade1, 100, ArmEnum.INFANTRY, false);
        brigade1.setArmTypeEnum(ArmyUtils.findArm(brigade1));
        brigade1.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);

        brigade2 = new Brigade();
        brigade2.setBattalions(new HashSet<Battalion>());
        sideBrigades1.add(brigade2);
        Order basicOrder2 = new Order();
        basicOrder2.setActivationRound(0);
        basicOrder2.setCheckpoint1(new FieldBattlePosition(20, 0));
        basicOrder2.setOrderTypeEnum(OrdersEnum.ENGAGE_IF_IN_RANGE);
        brigade2.setBasicOrder(basicOrder2);
        brigade2.setFormationEnum(FormationEnum.LINE);
        brigade2.setFieldBattlePosition(new FieldBattlePosition(20, 22));
        FieldBattleTestUtils.addBattalions(brigade2, 100, ArmEnum.INFANTRY, false);
        brigade2.setArmTypeEnum(ArmyUtils.findArm(brigade2));
        brigade2.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);


        sideBrigadesArray[0] = sideBrigades0;
        sideBrigadesArray[1] = sideBrigades1;


        bf = FieldBattleTestUtils.prepareBattleField(TerrainConstantsEnum.PLAINS.getId(), 9, 3, 2, 2);
        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
        fieldBattleProcessor.setBattleField(bf);
        fieldBattleProcessor.setFieldBattleOrderProcessor(new FieldBattleOrderProcessor(fieldBattleProcessor));
        fieldBattleProcessor.setFbMap(fbMap);
        fieldBattleProcessor.setSideBrigades(sideBrigadesArray);
        fieldBattleProcessor.setMoraleChecker(new MoraleChecker(fieldBattleProcessor));
        fieldBattleProcessor.getMoraleChecker().calculateInitialSideMoralesIfRequired();

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade2.getFieldBattlePosition().getX(), brigade2.getFieldBattlePosition().getY()), brigade2);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        CommanderProcessor commanderProcessorMock = mock(CommanderProcessor.class);
        when(commanderProcessorMock.influencedByCommander(any(Brigade.class))).thenReturn(false);
        when(commanderProcessorMock.influencedByCommanderOfType(any(Brigade.class), any(CommanderType.class))).thenReturn(false);
        fieldBattleProcessor.setCommanderProcessor(commanderProcessorMock);

        longRangeProcessor = new LongRangeProcessor(fieldBattleProcessor);
    }

    @Test
    public void testProcess() throws IOException {

        System.out.println(VisualisationUtils.getHeadCountInformation(brigade0));
        System.out.println(VisualisationUtils.getHeadCountInformation(brigade1));
        System.out.println(VisualisationUtils.getHeadCountInformation(brigade2));

        for (int round = 1; round < 50; round++) {

            for (int side = 0; side <= 1; side++) {

                longRangeProcessor.process(side, round);
                System.out.println("Brigade 0\n");
                System.out.println(VisualisationUtils.getHeadCountInformation(brigade0));
                System.out.println("\n");
                System.out.println("Brigade 1\n");
                System.out.println(VisualisationUtils.getHeadCountInformation(brigade1));
                System.out.println("\n");
                System.out.println("Brigade 2\n");
                System.out.println(VisualisationUtils.getHeadCountInformation(brigade2));

            }

        }

    }

    @Test
    public void testProcess_1Inf_2Art() {
        brigade0.setFormationEnum(FormationEnum.COLUMN);
        brigade0.getBattalions().clear();

        ArmyType infantryArmyType = new ArmyType();
        infantryArmyType.setName("in");
        infantryArmyType.setType("in");
        infantryArmyType.setLongRange(8);
        infantryArmyType.setLongCombat(7);


        ArmyType artilleryArmyType = new ArmyType();
        artilleryArmyType.setName("Ar");
        artilleryArmyType.setType("Ar");
        artilleryArmyType.setLongRange(8);
        artilleryArmyType.setLongCombat(7);

        Battalion battalion1 = new Battalion();
        battalion1.setType(infantryArmyType);
        battalion1.setHeadcount(800);
        battalion1.setExperience(3);
        brigade0.getBattalions().add(battalion1);

        Battalion battalion2 = new Battalion();
        battalion2.setType(artilleryArmyType);
        battalion2.setHeadcount(800);
        battalion2.setExperience(3);
        brigade0.getBattalions().add(battalion2);

        Battalion battalion3 = new Battalion();
        battalion3.setType(artilleryArmyType);
        battalion3.setHeadcount(800);
        battalion3.setExperience(3);
        brigade0.getBattalions().add(battalion3);

        brigade0.setArmTypeEnum(ArmyUtils.findArm(brigade0));

        printBrigadesStatus();

        for (int round = 1; round < 50; round++) {

            for (int side = 0; side <= 1; side++) {

                longRangeProcessor.process(side, round);
                printBrigadesStatus();

            }

        }

    }

    @Test
    public void testProcess_1Inf_2Art_WithRicochet() {
        brigade0.setFormationEnum(FormationEnum.COLUMN);
        brigade0.getBattalions().clear();

        ArmyType infantryArmyType = new ArmyType();
        infantryArmyType.setName("in");
        infantryArmyType.setType("in");
        infantryArmyType.setLongRange(8);
        infantryArmyType.setLongCombat(7);


        ArmyType artilleryArmyType = new ArmyType();
        artilleryArmyType.setName("Ar");
        artilleryArmyType.setType("Ar");
        artilleryArmyType.setLongRange(8);
        artilleryArmyType.setLongCombat(7);

        Battalion battalion1 = new Battalion();
        battalion1.setType(infantryArmyType);
        battalion1.setHeadcount(800);
        battalion1.setExperience(3);
        brigade0.getBattalions().add(battalion1);

        Battalion battalion2 = new Battalion();
        battalion2.setType(artilleryArmyType);
        battalion2.setHeadcount(800);
        battalion2.setExperience(3);
        brigade0.getBattalions().add(battalion2);

        Battalion battalion3 = new Battalion();
        battalion3.setType(artilleryArmyType);
        battalion3.setHeadcount(800);
        battalion3.setExperience(3);
        brigade0.getBattalions().add(battalion3);

        brigade0.setArmTypeEnum(ArmyUtils.findArm(brigade0));

        printBrigadesStatus();

        for (int round = 1; round < 50; round++) {

            for (int side = 0; side <= 1; side++) {

                longRangeProcessor.process(side, round);
                printBrigadesStatus();

            }

        }

    }

    private void printBrigadesStatus() {
        System.out.println("Brigade 0\n");
        System.out.println(VisualisationUtils.getHeadCountInformation(brigade0));
        System.out.println("\n");
        System.out.println("Brigade 1\n");
        System.out.println(VisualisationUtils.getHeadCountInformation(brigade1));
        System.out.println("\n");
        System.out.println("Brigade 2\n");
        System.out.println(VisualisationUtils.getHeadCountInformation(brigade2));
    }

}
