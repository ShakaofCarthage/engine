package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.MapBuilder;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.beans.BrigadeManagerBean;
import com.eaw1805.data.managers.beans.GameManagerBean;
import com.eaw1805.data.managers.beans.NationManagerBean;
import com.eaw1805.data.managers.beans.SectorManagerBean;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WebClientTest {

    /**
     * Setup the Tester.
     */
    @Before
    public void setUp() {
        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
    }

    @Test
    public void webClientTest() {

        // Make sure we have an active transaction
        final Session thatSession = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession();
        final Transaction thatTrans = thatSession.beginTransaction();

        NationManagerBean nationManager = NationManager.getInstance();
        SectorManagerBean sectorManager = SectorManager.getInstance();
        GameManagerBean gameManager = GameManager.getInstance();
        BrigadeManagerBean brigadeManager = BrigadeManager.getInstance();

        final Nation thisNation1 = nationManager.getByID(17);
        final Nation thisNation2 = nationManager.getByID(15);

        final BattleField field = new BattleField(sectorManager.getByID(34));
        field.addNation(0, thisNation1);
        field.addNation(1, thisNation2);


        final Game thisGame = gameManager.getByID(3);
        final List<Brigade> brigades1 = brigadeManager.listByGameNation(thisGame, thisNation1);
        final List<Brigade> brigades2 = brigadeManager.listByGameNation(thisGame, thisNation2);
        final List<List<Brigade>> sides = new ArrayList<List<Brigade>>();
        sides.add(brigades1);
        sides.add(brigades2);
        int count1 = 0;
        for (Brigade brig : brigades1) {
            for (Battalion battalion : brig.getBattalions()) {
                count1 += battalion.getHeadcount();
            }
        }
        System.out.println("Battalions in side 1 : " + count1);
        int count2 = 0;
        for (Brigade brig : brigades2) {
            for (Battalion battalion : brig.getBattalions()) {
                count2 += battalion.getHeadcount();
            }
        }
        System.out.println("Battalions in side 2 : " + count2);
        final MapBuilder mapBuilder = new MapBuilder(field, sides);

        FieldBattleMap fbMap = mapBuilder.buildMap();
        VisualisationUtils.visualize(fbMap);

        thatTrans.rollback();
    }

}
