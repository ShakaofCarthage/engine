package com.eaw1805.core.initializers.test;

import com.eaw1805.core.initializers.scenario1802.field.FieldBattleMapExtraFeatureInitializer;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.field.FieldBattleMapExtraFeatureManager;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestFieldBattleMapExtraFeatureInitializer {

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

        FieldBattleMapExtraFeatureManager manager = FieldBattleMapExtraFeatureManager.getInstance();

        FieldBattleMapExtraFeatureInitializer initializer = new FieldBattleMapExtraFeatureInitializer();
        if (initializer.needsInitialization()) {
            initializer.initialize();
        }

        int currentNum = manager.list().size();
        assertEquals(5, currentNum);

        thatTrans.rollback();

    }

}
