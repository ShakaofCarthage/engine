package com.eaw1805.core.initializers.scenario1802.army;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.army.RankManager;
import com.eaw1805.data.model.army.Rank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


/**
 * Initializes the table with the commander ranks.
 */
public class RankInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RankInitializer.class);

    /**
     * The names of the 5 records.
     */
    private static final String[] NAMES = {"Colonel", "Brigadier", "Major-General", "Lieutenant General", "Field Marshal"};

    /**
     * The minimum commander capability of the 5 records.
     */
    private static final int[] MIN_COMC = {1, 6, 11, 21, 31};

    /**
     * The maximum commander capability of the 5 records.
     */
    private static final int[] MAX_COMC = {5, 10, 20, 30, 40};

    /**
     * The strategic capability of the 5 records.
     */
    private static final int[] STRC = {1, 2, 3, 4, 5};

    /**
     * The salary of the 5 records.
     */
    private static final int[] SALARY = {10000, 15000, 25000, 45000, 70000};

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NAMES.length;

    /**
     * Default constructor.
     */
    public RankInitializer() {
        super();
        LOGGER.debug("RankInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Rank> records = RankManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("RankInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Rank thisRank = new Rank(); //NOPMD
            thisRank.setName(NAMES[i]);
            thisRank.setMinComC(MIN_COMC[i]);
            thisRank.setMaxComC(MAX_COMC[i]);
            thisRank.setStrC(STRC[i]);
            thisRank.setSalary(SALARY[i]);
            RankManager.getInstance().add(thisRank);
        }

        LOGGER.info("RankInitializer complete.");
    }

}
