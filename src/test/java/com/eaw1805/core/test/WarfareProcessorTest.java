package com.eaw1805.core.test;

import com.eaw1805.battles.WarfareProcessor;
import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.orders.OrderProcessor;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;

/**
 * This class is tester for the WarfareProcessorTest class which is responsible
 * for progressing the battles of the game.
 */
public class WarfareProcessorTest extends TestCase {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(WarfareProcessorTest.class);

    /**
     * Setup the Tester.
     */
    @Before
    public void setUp() {
        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
    }

    /**
     * Tests process sector.
     */
    @Test
    public void testSectorSelector() {
        // Make sure we have an active transaction
        final Session thatSession = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession();
        final Transaction thatTrans = thatSession.beginTransaction();

        try {
            GameEngine thisGE = new GameEngine(5, HibernateUtil.DB_S1, "", -1);
            thisGE.init();

            final WarfareProcessor wProc = new WarfareProcessor(thisGE, new OrderProcessor(thisGE));
            wProc.process();

        } catch (Exception ex) {
            LOGGER.fatal(ex.getMessage(), ex);
        }

        thatTrans.rollback();
    }

}