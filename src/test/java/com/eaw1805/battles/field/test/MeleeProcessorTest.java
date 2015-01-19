package com.eaw1805.battles.field.test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.FieldBattleProcessorResourceLocator;
import com.eaw1805.battles.field.orders.FieldBattleOrderProcessor;
import com.eaw1805.battles.field.test.FieldBattleTestUtils.TerrainConstantsEnum;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeleeProcessorTest {


    private FieldBattleProcessor fieldBattleProcessor;
    private BattleField bf;
    private FieldBattleMap fbMap;
    private List<List<Brigade>> sideBrigades;
    private FieldBattleProcessorResourceLocator resourceLocator = mock(FieldBattleProcessorResourceLocator.class);
    private Order basicOrder0;
    private Order basicOrder1;
    private Brigade brigade0;
    private Brigade brigade1;

    @SuppressWarnings("unchecked")
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

        brigade0 = FieldBattleTestUtils.initializeBrigade(0, 9, FormationEnum.COLUMN, 8, nation0);
        FieldBattleTestUtils.addBattalions(brigade0, 300, ArmEnum.INFANTRY, true);
        basicOrder0 = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.MOVE_TO_ENGAGE, FormationEnum.LINE);
        brigade0.setBasicOrder(basicOrder0);
        sideBrigades.get(0).add(brigade0);

        brigade1 = FieldBattleTestUtils.initializeBrigade(1, 9, FormationEnum.COLUMN, 8, nation1);
        FieldBattleTestUtils.addBattalions(brigade1, 300, ArmEnum.INFANTRY, false);
        basicOrder1 = FieldBattleTestUtils.initializeOrder(0, OrdersEnum.MOVE_TO_ENGAGE, FormationEnum.LINE);
        brigade1.setBasicOrder(basicOrder1);
        brigade1.setFieldBattleCommanderId(1);
        sideBrigades.get(1).add(brigade1);

        when(resourceLocator.getFieldBattleMap(anyInt())).thenReturn(fbMap);
        when(resourceLocator.getFieldBattleReport(anyInt())).thenReturn(new FieldBattleReport());
        when(resourceLocator.getCommanderById(anyInt())).thenReturn(new Commander());

        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);

        bf = FieldBattleTestUtils.prepareBattleField(TerrainConstantsEnum.PLAINS.getId(), 9, 3, 2, 2);
        fieldBattleProcessor.setBattleField(bf);
        fieldBattleProcessor.setFieldBattleOrderProcessor(new FieldBattleOrderProcessor(fieldBattleProcessor));
        fieldBattleProcessor.setFbMap(fbMap);

        List<Brigade>[] sideBrigadesArray = (List<Brigade>[]) Array.newInstance(List.class, 2);
        sideBrigadesArray[0] = sideBrigades.get(0);
        sideBrigadesArray[1] = sideBrigades.get(1);
        fieldBattleProcessor.setSideBrigades(sideBrigadesArray);

        fieldBattleProcessor.setResourceLocator(resourceLocator);
    }

    @Test
    public void defaultMeleeBattle() {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        brigade0.setArmTypeEnum(ArmEnum.CAVALRY);
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(15, 38));
        brigade1.setArmTypeEnum(ArmEnum.INFANTRY);
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void buildPontoonBridgeOnMinorRiver() {

        for (int x = 0; x < fbMap.getSizeX(); x++) {
            fbMap.getFieldBattleSector(x, 20).setMinorRiver(true);
        }
        basicOrder0.setCheckpoint1(new FieldBattlePosition(10, 19));
        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.BUILD_PONTOON_BRIDGE);

        basicOrder1.setCheckpoint1(new FieldBattlePosition(40, 38));
        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void buildPontoonBridgeOnMajorRiver() {

        for (int x = 0; x < fbMap.getSizeX(); x++) {
            fbMap.getFieldBattleSector(x, 19).setMajorRiver(true);
            fbMap.getFieldBattleSector(x, 20).setMajorRiver(true);
            fbMap.getFieldBattleSector(x, 21).setMajorRiver(true);
        }

        basicOrder0.setCheckpoint1(new FieldBattlePosition(10, 18));
        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.BUILD_PONTOON_BRIDGE);

        basicOrder1.setCheckpoint1(new FieldBattlePosition(40, 38));
        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void moveToDestroyBridgeOnMinorRiver() {

        for (int x = 0; x < fbMap.getSizeX(); x++) {
            fbMap.getFieldBattleSector(x, 20).setMinorRiver(true);
        }

        fbMap.getFieldBattleSector(15, 20).setBridge(18);

        basicOrder0.setCheckpoint1(new FieldBattlePosition(15, 19));
        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_DESTROY_BRIDGES);

        basicOrder1.setCheckpoint1(new FieldBattlePosition(40, 38));
        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void moveToDestroyBridgeOnMajorRiver() {

        for (int x = 0; x < fbMap.getSizeX(); x++) {
            fbMap.getFieldBattleSector(x, 19).setMajorRiver(true);
            fbMap.getFieldBattleSector(x, 20).setMajorRiver(true);
            fbMap.getFieldBattleSector(x, 21).setMajorRiver(true);
        }

        fbMap.getFieldBattleSector(15, 19).setBridge(18);
        fbMap.getFieldBattleSector(15, 20).setBridge(18);
        fbMap.getFieldBattleSector(15, 21).setBridge(18);

        basicOrder0.setCheckpoint1(new FieldBattlePosition(15, 18));
        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_DESTROY_BRIDGES);

        basicOrder1.setCheckpoint1(new FieldBattlePosition(40, 38));
        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }


    @Test
    public void moveToDestroyFortifications() {

        for (int x = 15; x <= 25; x++) {
            for (int y = 0; y <= 10; y++) {
                if (x == 15 || x == 25 || y == 10) {
                    fbMap.getFieldBattleSector(x, y).setWall(20);
                }
            }
        }

        brigade0.setFieldBattlePosition(new FieldBattlePosition(35, 5));
        basicOrder0.setCheckpoint1(new FieldBattlePosition(35, 35));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(30, 35));
        basicOrder1.setCheckpoint1(new FieldBattlePosition(20, 11));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_DESTROY_FORTIFICATIONS);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void moveToDestroyFortificationsOtherSide() {

        for (int x = 15; x <= 25; x++) {
            for (int y = fbMap.getSizeY() - 11; y <= fbMap.getSizeY() - 1; y++) {
                if (x == 15 || x == 25 || y == fbMap.getSizeY() - 11) {
                    fbMap.getFieldBattleSector(x, y).setWall(20);
                }
            }
        }

        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setCheckpoint1(new FieldBattlePosition(20, fbMap.getSizeY() - 12));
        basicOrder0.setOrderTypeEnum(OrdersEnum.MOVE_TO_DESTROY_FORTIFICATIONS);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        basicOrder1.setCheckpoint1(new FieldBattlePosition(40, 4));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

    @Test
    public void digEntrenchments() {

        basicOrder0.setCheckpoint1(new FieldBattlePosition(10, 19));
        brigade0.setFieldBattlePosition(new FieldBattlePosition(15, 2));
        basicOrder0.setOrderTypeEnum(OrdersEnum.DIG_ENTRENCHMENTS);

        basicOrder1.setCheckpoint1(new FieldBattlePosition(40, 38));
        brigade1.setFieldBattlePosition(new FieldBattlePosition(19, 38));
        basicOrder1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        fieldBattleProcessor.processFirstHalfRounds(1);
    }

}
