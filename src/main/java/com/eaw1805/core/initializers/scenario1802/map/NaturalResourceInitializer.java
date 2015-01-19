package com.eaw1805.core.initializers.scenario1802.map;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.core.initializers.scenario1802.NationsInitializer;
import com.eaw1805.data.managers.map.NaturalResourceManager;
import com.eaw1805.data.model.map.NaturalResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the table NATURAL_RESOURCES with the default natural resources.
 */
public class NaturalResourceInitializer extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(NationsInitializer.class);

    /**
     * The codes of the 7 records.
     */
    private static final char[] CODES =
            {'g', 'e', 'z', 'w', 'n', 'p', 'v', 'f', 'c'};

    /**
     * The names of the 7 records.
     */
    private static final String[] NAMES =
            {"precious metals", "ore", "gems", "wine", "foodstuff", "horse breeding",
                    "sheep breeding", "fish", "colonial goods"};

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NAMES.length;


    /**
     * Default NaturalResourceInitializer's constructor.
     */
    public NaturalResourceInitializer() {
        super();
        LOGGER.debug("NationsInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<NaturalResource> records =
                NaturalResourceManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("NaturalResourceInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final NaturalResource thisNaturalRes =
                    new NaturalResource(); //NOPMD
            thisNaturalRes.setCode(CODES[i]);
            thisNaturalRes.setName(NAMES[i]);
            thisNaturalRes.setFactor(0);
            NaturalResourceManager.getInstance().add(thisNaturalRes);
        }

        LOGGER.info("NaturalResourceInitializer complete.");
    }

}
