package com.eaw1805.battles.field.test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.detachment.FieldBattleDetachmentProcessor;
import com.eaw1805.battles.field.morale.MoraleChecker;
import com.eaw1805.battles.field.orders.FieldBattleOrderProcessor;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderProcessor;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.battles.field.visibility.FieldBattleVisibilityProcessor;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.DetachmentPosition;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import com.eaw1805.data.model.battles.field.log.IHalfRoundLog;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MovementProcessorTest {

    private FieldBattleProcessor fieldBattleProcessor;
    private MovementProcessor movementProcessor;
    private FieldBattleVisibilityProcessor fieldBattleVisibilityProcessor;
    private Order order0;
    private Order order1;
    private Brigade brigade0;
    private Brigade brigade1;
    private Nation nation0;
    private Nation nation1;
    private FieldBattleMap fbMap;


    @Before
    @SuppressWarnings("unchecked")
    public void setup() {

        fbMap = new FieldBattleMap(45, 40);

        List<Brigade>[] sideBrigadesArray = (List<Brigade>[]) Array.newInstance(List.class, 2);

        List<Brigade> sideBrigades0 = new ArrayList<Brigade>();
        nation0 = new Nation();
        nation0.setId(0);
        brigade0 = FieldBattleTestUtils.initializeBrigade(0, 0, FormationEnum.COLUMN, 5, nation0);
        sideBrigades0.add(brigade0);
        order0 = new Order();
        order0.setActivationRound(0);
        order0.setFormationEnum(FormationEnum.LINE);
        brigade0.setBasicOrder(order0);
        FieldBattleTestUtils.addBattalions(brigade0, 200, ArmEnum.INFANTRY, false);

        List<Brigade> sideBrigades1 = new ArrayList<Brigade>();
        nation1 = new Nation();
        nation1.setId(1);
        brigade1 = FieldBattleTestUtils.initializeBrigade(0, 0, FormationEnum.COLUMN, 5, nation1);
        sideBrigades1.add(brigade1);
        order1 = new Order();
        order1.setActivationRound(0);
        order1.setFormationEnum(FormationEnum.LINE);
        brigade1.setBasicOrder(order1);
        FieldBattleTestUtils.addBattalions(brigade1, 200, ArmEnum.INFANTRY, false);


        sideBrigadesArray[0] = sideBrigades0;
        sideBrigadesArray[1] = sideBrigades1;


        BattleField bf = new BattleField(null);
        bf.addNation(0, nation0);
        bf.addNation(1, nation1);
        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
        fieldBattleProcessor.setBattleField(bf);
        fieldBattleProcessor.setSideBrigades(sideBrigadesArray);
        fieldBattleProcessor.setInitialSideBrigades(sideBrigadesArray);
        fieldBattleProcessor.setFieldBattleOrderProcessor(new FieldBattleOrderProcessor(fieldBattleProcessor));
        fieldBattleProcessor.setFbMap(fbMap);
        fieldBattleProcessor.setMoraleChecker(new MoraleChecker(fieldBattleProcessor));
        fieldBattleVisibilityProcessor = mock(FieldBattleVisibilityProcessor.class);
        when(fieldBattleVisibilityProcessor.visible(any(Brigade.class), any(Brigade.class))).thenReturn(true);
        fieldBattleProcessor.setFieldBattleVisibilityProcessor(fieldBattleVisibilityProcessor);
        fieldBattleProcessor.setHalfRoundLog(mock(IHalfRoundLog.class));


        brigade0.setFieldBattlePosition(new FieldBattlePosition(18, 0));
        order0.setCheckpoint1(new FieldBattlePosition(18, fbMap.getSizeY() - 1));

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 1));
        order1.setCheckpoint1(new FieldBattlePosition(20, 0));

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        fieldBattleProcessor.getMoraleChecker().calculateInitialSideMoralesIfRequired();
        movementProcessor = new MovementProcessor(fieldBattleProcessor);
        fieldBattleProcessor.setCommanderProcessor(new CommanderProcessor(fieldBattleProcessor, (List<Brigade>[]) new ArrayList<?>[]{new ArrayList<Brigade>(), new ArrayList<Brigade>()}));

    }

    @Test
    public void testEngageIfInRange() throws IOException {

        brigade0.setArmTypeEnum(ArmEnum.INFANTRY);
        order0.setOrderTypeEnum(OrdersEnum.ENGAGE_IF_IN_RANGE);
        brigade1.setArmTypeEnum(ArmEnum.INFANTRY);
        order1.setOrderTypeEnum(OrdersEnum.ENGAGE_IF_IN_RANGE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 15; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testAttackAndReform() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(25, 5));
        order0.setCheckpoint1(new FieldBattlePosition(25, 10));
        order0.setOrderTypeEnum(OrdersEnum.ATTACK_AND_REFORM);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 1));
        order1.setCheckpoint1(new FieldBattlePosition(5, 14));
        order1.setCheckpoint2(new FieldBattlePosition(40, 14));
        order1.setCheckpoint3(new FieldBattlePosition(25, 14));
        order1.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }


    @Test
    public void testMoveToEngage() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(0, 5));
        order0.setCheckpoint1(new FieldBattlePosition(fbMap.getSizeX() - 1, 5));
        order0.setOrderTypeEnum(OrdersEnum.ATTACK_AND_REFORM);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 1));
        order1.setCheckpoint1(null);
        order1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 20; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }


    @Test
    public void testMoveToFire() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(0, 5));
        order0.setCheckpoint1(new FieldBattlePosition(fbMap.getSizeX() - 1, 5));
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(fbMap.getSizeX() - 1, fbMap.getSizeY() - 6));
        order1.setCheckpoint1(new FieldBattlePosition(0, fbMap.getSizeY() - 6));
        order1.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testDefendPosition() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(fbMap.getSizeX() - 2, 5));
        order0.setCheckpoint1(new FieldBattlePosition(20, 5));
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(10, fbMap.getSizeY() - 6));
        order1.setCheckpoint1(new FieldBattlePosition(20, fbMap.getSizeY() - 6));
        order1.setCheckpoint2(new FieldBattlePosition(10, fbMap.getSizeY() - 10));
        order1.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testMaintainDistance() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, 20));
        order1.setCheckpoint1(null);
        order1.setOrderTypeEnum(OrdersEnum.MAINTAIN_DISTANCE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testRetreat() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, 20));
        order1.setCheckpoint1(null);
        order1.setOrderTypeEnum(OrdersEnum.RETREAT);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testAttackEnemyStrategicPoints() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setCheckpoint1(null);
        order1.setOrderTypeEnum(OrdersEnum.ATTACK_ENEMY_STRATEGIC_POINTS);

        fbMap.getFieldBattleSector(25, 0).setStrategicPoint(true);
        fbMap.getFieldBattleSector(25, 0).setNation(brigade0.getNation());

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testMoveToDestroyFortifications() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setCheckpoint1(null);
        order1.setOrderTypeEnum(OrdersEnum.MOVE_TO_DESTROY_FORTIFICATIONS);

        fbMap.getFieldBattleSector(18, 6).setWall(1);
        fbMap.getFieldBattleSector(19, 6).setWall(1);
        fbMap.getFieldBattleSector(20, 6).setWall(1);
        fbMap.getFieldBattleSector(21, 6).setWall(1);
        fbMap.getFieldBattleSector(22, 6).setWall(1);
        fbMap.getFieldBattleSector(18, 6).setNation(nation0);
        fbMap.getFieldBattleSector(19, 6).setNation(nation0);
        fbMap.getFieldBattleSector(20, 6).setNation(nation0);
        fbMap.getFieldBattleSector(21, 6).setNation(nation0);
        fbMap.getFieldBattleSector(22, 6).setNation(nation0);

        fbMap.getFieldBattleSector(25, 0).setStrategicPoint(true);
        fbMap.getFieldBattleSector(25, 0).setNation(brigade0.getNation());

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testMoveToDestroyBridges() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setCheckpoint1(new FieldBattlePosition(5, 21));
        order1.setOrderTypeEnum(OrdersEnum.MOVE_TO_DESTROY_BRIDGES);

        for (int x = 0; x < fbMap.getSizeX() - 1; x++) {
            fbMap.getFieldBattleSector(x, 20).setMinorRiver(true);
        }

        fbMap.getFieldBattleSector(5, 20).setBridge(1);
        fbMap.getFieldBattleSector(5, 20).setCurrentHolder(nation0);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testBuildPontoonBridge() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(new FieldBattlePosition(5, 30));
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setCheckpoint1(new FieldBattlePosition(5, 21));
        order1.setOrderTypeEnum(OrdersEnum.BUILD_PONTOON_BRIDGE);

        for (int x = 0; x < fbMap.getSizeX() - 1; x++) {
            fbMap.getFieldBattleSector(x, 20).setMinorRiver(true);
        }

        fbMap.getFieldBattleSector(5, 20).setBridge(1);
        fbMap.getFieldBattleSector(5, 20).setCurrentHolder(nation0);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testRecoverOwnStrategicPoints() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setCheckpoint1(null);
        order1.setStrategicPoint1(new FieldBattlePosition(30, 0));
        order1.setOrderTypeEnum(OrdersEnum.RECOVER_OWN_STRATEGIC_POINTS);

        fbMap.getFieldBattleSector(15, 30).setStrategicPoint(true);
        fbMap.getFieldBattleSector(15, 30).setNation(brigade1.getNation());
        fbMap.getFieldBattleSector(15, 30).setCurrentHolder(brigade0.getNation());

        fbMap.getFieldBattleSector(35, 20).setStrategicPoint(true);
        fbMap.getFieldBattleSector(35, 20).setNation(brigade1.getNation());
        fbMap.getFieldBattleSector(35, 20).setCurrentHolder(brigade0.getNation());

        fbMap.getFieldBattleSector(25, 10).setStrategicPoint(true);
        fbMap.getFieldBattleSector(25, 10).setNation(brigade1.getNation());
        fbMap.getFieldBattleSector(25, 10).setCurrentHolder(brigade0.getNation());

        fbMap.getFieldBattleSector(30, 0).setStrategicPoint(true);
        fbMap.getFieldBattleSector(30, 0).setNation(brigade1.getNation());
        fbMap.getFieldBattleSector(30, 0).setCurrentHolder(brigade0.getNation());

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

    @Test
    public void testRouting() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setCheckpoint1(null);
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setCheckpoint1(null);
        order1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            if (round == 7) {
                brigade1.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
            } else if (round == 10) {
                brigade1.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
            }

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }


    @Test
    public void testFollowDetachment() throws IOException {

        brigade0.setFieldBattlePosition(new FieldBattlePosition(20, 5));
        order0.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        Brigade brigade0A = FieldBattleTestUtils.initializeBrigade(10, 0, FormationEnum.COLUMN, 5, nation0);
        FieldBattleTestUtils.addBattalions(brigade0A, 400, ArmEnum.INFANTRY, false);
        brigade0A.setFieldBattlePosition(new FieldBattlePosition(18, 5));
        fieldBattleProcessor.getSideBrigades()[0].add(brigade0A);
        Order order0A = new Order();
        order0A.setCheckpoint1(new FieldBattlePosition(-1, -1));
        order0A.setOrderTypeEnum(OrdersEnum.FOLLOW_DETACHMENT);
        order0A.setDetachmentPosition(DetachmentPosition.LEFT);
        order0A.setActivationRound(0);
        order0A.setFormationEnum(FormationEnum.LINE);
        brigade0A.setBasicOrder(order0A);
        FieldBattleTestUtils.addBattalions(brigade0, 200, ArmEnum.INFANTRY, false);

        FieldBattleDetachmentProcessor fbdpMock = mock(FieldBattleDetachmentProcessor.class);
        when(fbdpMock.getLeader(brigade0A)).thenReturn(brigade0);
        when(fbdpMock.getAllLeaders()).thenReturn((Set<Brigade>) new HashSet<Brigade>(Arrays.asList(new Brigade[]{brigade0})));
        when(fbdpMock.getAllFollowers()).thenReturn((Set<Brigade>) new HashSet<Brigade>(Arrays.asList(new Brigade[]{brigade0A})));
        fieldBattleProcessor.setFieldBattleDetachmentProcessor(fbdpMock);

        brigade1.setFieldBattlePosition(new FieldBattlePosition(20, fbMap.getSizeY() - 5));
        order1.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);

        BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0.getFieldBattlePosition().getX(), brigade0.getFieldBattlePosition().getY()), brigade0);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade0A.getFieldBattlePosition().getX(), brigade0A.getFieldBattlePosition().getY()), brigade0A);
        sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade1.getFieldBattlePosition().getX(), brigade1.getFieldBattlePosition().getY()), brigade1);
        fieldBattleProcessor.setSectorsToBrigades(sectorsToBrigades);

        for (int round = 1; round < 40; round++) {

            for (int side = 0; side <= 1; side++) {

                VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
                movementProcessor.process(side, round);
            }

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }


    @Test
    public void testMoveThroughEnemies() throws IOException {

        order0.setOrderTypeEnum(OrdersEnum.ATTACK_AND_REFORM);
        order1.setOrderTypeEnum(OrdersEnum.ATTACK_AND_REFORM);

        for (int x = 0; x < fieldBattleProcessor.getFbMap().getSizeX(); x++) {
            if (x == 5) {
                continue;
            }
            Brigade brig = new Brigade();
            brig.setArmTypeEnum(ArmEnum.INFANTRY);
            brig.setFieldBattlePosition(new FieldBattlePosition(x, 20));
            fieldBattleProcessor.getSectorsToBrigades().put(fieldBattleProcessor.getFbMap().getFieldBattleSector(x, 20), brig);

            fieldBattleProcessor.getSideBrigades()[1].add(brig);
        }

        movementProcessor = new MovementProcessor(fieldBattleProcessor);

        for (int round = 1; round < 20; round++) {


            VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
            movementProcessor.process(0, round);

        }
        VisualisationUtils.visualize(movementProcessor.getParent().getFbMap(), fieldBattleProcessor.getSectorsToBrigades().keySet());
    }

}
