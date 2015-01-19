package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.WarfareProcessor;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.FieldBattleProcessorResourceLocator;
import com.eaw1805.battles.field.orders.FieldBattleOrderProcessor;
import com.eaw1805.battles.field.test.FieldBattleTestUtils.TerrainConstantsEnum;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.battles.FieldBattleReportManager;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.orders.OrderProcessor;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FieldBattleProcessorIntegrationTest {


    private FieldBattleProcessor fieldBattleProcessor;
    private BattleField bf;
    private List<List<Brigade>> sideBrigades;
    private FieldBattleProcessorResourceLocator resourceLocator;
    //    private int turn = (int) new Date().getTime();
    private int turn = 111;


    private static class MyWarfareProcessor extends WarfareProcessor {
        public MyWarfareProcessor(GameEngine engine, OrderProcessor caller) {
            super(engine, caller);
        }

        @Override
        public List<BattleField> searchSectors() {
            return super.searchSectors();
        }

    }

    ;

    @Before
    public void setup() throws Exception {


        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
        GameEngine thisGE = new GameEngine(4, HibernateUtil.DB_S1, "", -1);
        thisGE.init();

        MyWarfareProcessor wfProc = new MyWarfareProcessor(thisGE, new OrderProcessor(thisGE));

        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
        Transaction trans2 = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession().beginTransaction();
        List<BattleField> battleFields = wfProc.searchSectors();

        bf = battleFields.get(0);

        List<Brigade> sideBrigades0 = BrigadeManager.getInstance().listByPositionNation(bf.getField().getPosition(), bf.getSide(0).get(0));
        List<Brigade> sideBrigades1 = BrigadeManager.getInstance().listByPositionNation(bf.getField().getPosition(), bf.getSide(1).get(0));

        sideBrigades = new ArrayList<List<Brigade>>();
        sideBrigades.add(sideBrigades0);
        sideBrigades.add(sideBrigades1);

        trans2.rollback();

        resourceLocator = new FieldBattleProcessorResourceLocator(HibernateUtil.DB_S1);
    }


    @Test
    public void initializeTest() {

        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);

        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);

        FieldBattleReport fbReport = new FieldBattleReport();
        fbReport.setPosition(bf.getField().getPosition());
        fbReport.setTurn(turn);
        saveFieldBattleReport(fbReport);

        FieldBattleReport retrievedFbReport = retrieveFieldBattleReport(bf.getField().getPosition(), turn);

        fieldBattleProcessor.processInitialization(retrievedFbReport.getBattleId());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void processFirstHalfRounds() {

        FieldBattleReport fbReport = retrieveFieldBattleReport(bf.getField().getPosition(), turn);

        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);

        bf = FieldBattleTestUtils.prepareBattleField(TerrainConstantsEnum.PLAINS.getId(), 9, 3, 2, 2);
        fieldBattleProcessor.setBattleField(bf);
        fieldBattleProcessor.setFieldBattleOrderProcessor(new FieldBattleOrderProcessor(fieldBattleProcessor));

        List<Brigade>[] sideBrigadesArray = (List<Brigade>[]) Array.newInstance(List.class, 2);
        sideBrigadesArray[0] = sideBrigades.get(0);
        sideBrigadesArray[1] = sideBrigades.get(1);
        fieldBattleProcessor.setSideBrigades(sideBrigadesArray);

        emulateUserInput(fbReport.getBattleId());

        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
        fieldBattleProcessor.processFirstHalfRounds(fbReport.getBattleId());

    }


    private void emulateUserInput(int battleId) {

        placeBrigades(sideBrigades, 0, battleId);
        placeBrigades(sideBrigades, 1, battleId);

        placeOrders(sideBrigades, 0, battleId);
        placeOrders(sideBrigades, 1, battleId);
    }


    private void placeBrigades(List<List<Brigade>> sideBrigades, int side, int battleId) {

        FieldBattleMap fbMap = resourceLocator.getFieldBattleMap(battleId);
        Set<FieldBattlePosition> fbPositions = new HashSet<FieldBattlePosition>();
        int startingY = side == 0 ? 0 : fbMap.getSizeY() - 10;

        Transaction trans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession().beginTransaction();
        for (Brigade brigade : sideBrigades.get(side)) {
            while (true) {

                FieldBattlePosition fbPosition = new FieldBattlePosition(MathUtils.generateRandomIntInRange(0, fbMap.getSizeX() - 1),
                        MathUtils.generateRandomIntInRange(startingY, startingY + 9));
                fbPosition.setPlaced(true);
                FieldBattleSector fbSector = MapUtils.getSectorFromPosition(fbMap, fbPosition);
                if (!fbSector.isEmpty() || fbPositions.contains(fbPosition)) {
                    continue;
                } else {
                    brigade.setFieldBattlePosition(fbPosition);
                    BrigadeManager.getInstance().update(brigade);
                    fbPositions.add(fbPosition);
                    break;
                }
            }
        }
        trans.commit();
    }

    private void placeOrders(List<List<Brigade>> sideBrigades, int side, int battleId) {

        Transaction trans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession().beginTransaction();
        for (Brigade brigade : sideBrigades.get(side)) {

            Order order = new Order();
            order.setCheckpoint1(new FieldBattlePosition(-1, -1));
            order.setCheckpoint2(new FieldBattlePosition(-1, -1));
            order.setCheckpoint3(new FieldBattlePosition(-1, -1));
            order.setStrategicPoint1(new FieldBattlePosition(-1, -1));
            order.setStrategicPoint2(new FieldBattlePosition(-1, -1));
            order.setStrategicPoint3(new FieldBattlePosition(-1, -1));
            order.setCustomStrategicPoint1(new FieldBattlePosition(-1, -1));
            order.setCustomStrategicPoint2(new FieldBattlePosition(-1, -1));
            order.setCustomStrategicPoint3(new FieldBattlePosition(-1, -1));
            order.setOrderTypeEnum(OrdersEnum.MOVE_TO_ENGAGE);
            order.setFormationEnum(FormationEnum.COLUMN);
            brigade.setBasicOrder(order);
            brigade.setFormationEnum(order.getFormationEnum());
            BrigadeManager.getInstance().update(brigade);
        }
        trans.commit();
    }


    private void saveFieldBattleReport(FieldBattleReport fbReport) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession().beginTransaction();
        FieldBattleReportManager.getInstance().add(fbReport);
        trans.commit();
    }

    private FieldBattleReport retrieveFieldBattleReport(Position position, int turn) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession().beginTransaction();
        FieldBattleReport fbReport = FieldBattleReportManager.getInstance().listPositionTurn(position, turn);
        trans.commit();
        return fbReport;
    }

}
