package com.eaw1805.core.initializers.scenario1808;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.model.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

/**
 * Initializes a new scenario.
 */
public class ScenarioInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ScenarioInitializer.class);

    /**
     * The identity of the scenario object to initialize.
     */
    private transient int scenarioId;

    /**
     * Default constructor.
     */
    public ScenarioInitializer(final int scenario) {
        super();
        scenarioId = scenario;
        LOGGER.debug("ScenarioInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        final Game scenarioGame = GameManager.getInstance().getByID(-1);
        theTrans.rollback();
        return (scenarioGame == null);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() { // NOPMD
        LOGGER.debug("Initializing new scenario");
        LOGGER.info("Initialization for scenario 1808 has not been implemented...");
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {
        // Check if initialization of this entity is required
        if (needsInitialization()) {
            // perform initialization
            initialize();
        }
    }

}
