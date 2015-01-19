package com.eaw1805.core.initializers.test;

import com.eaw1805.core.initializers.scenario1802.field.FieldBattleTerrainInitializer;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.field.FieldBattleTerrainManager;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestFieldBattleTerrainInitializer {

    /**
     * Setup the Tester.
     */
    @Before
    public void setUp() {
        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
    }

    @Test
    public void testInitialize() {
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);

        // Make sure we have an active transaction
        final Session thatSession = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession();
        final Transaction thatTrans = thatSession.beginTransaction();

        FieldBattleTerrainManager manager = FieldBattleTerrainManager.getInstance();

        FieldBattleTerrainInitializer fbti = new FieldBattleTerrainInitializer();
        if (fbti.needsInitialization()) {
            fbti.initialize();
        }

        int currentNum = manager.list().size();
        assertEquals(6, currentNum);

        thatTrans.rollback();

    }

}
