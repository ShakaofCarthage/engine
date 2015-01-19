package com.eaw1805.core.initializers.scenario1802.economy;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.model.economy.Good;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Initializes the table with the goods data.
 */
public class GoodInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(GoodInitializer.class);

    /**
     * The names of the goods.
     */
    private static final String[] NAMES =
            {"Money", "Citizens",
                    "Industrial Points", "Food", "Stone", "Wood", "Ore", "Gems",
                    "Horse", "Fabric", "Wool", "Precious Metals", "Wine", "Colonial Goods",
                    "Administrative Points", "Command Points"};

    /**
     * The weight of the goods.
     */
    private static final int[] WEIGHT =
            {0, 10,
                    25, 1, 2, 2, 1, 10,
                    5, 1, 1, 1, 1, 1, 0, 0};

    /**
     * The good factor for each good.
     */
    private static final int[] GOOD_FACTOR =
            {0, 0,
                    17, 11, 2, 6, 250, 350,
                    5, 26, 3, 380, 70, 140, 0, 0};

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NAMES.length;

    /**
     * GoodInitializer's default constructor.
     */
    public GoodInitializer() {
        super();
        LOGGER.debug("GoodInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        return (GoodManager.getInstance().list().size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the GOODS table by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("GoodInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Good thisGood = new Good(); //NOPMD
            thisGood.setName(NAMES[i]);
            thisGood.setWeightOfGood(WEIGHT[i]);
            thisGood.setGoodFactor(GOOD_FACTOR[i]);
            GoodManager.getInstance().add(thisGood);
        }

        LOGGER.info("GoodInitializer complete.");
    }

}
