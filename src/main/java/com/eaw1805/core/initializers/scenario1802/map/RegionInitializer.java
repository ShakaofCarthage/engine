package com.eaw1805.core.initializers.scenario1802.map;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the table with the region data.
 */
public class RegionInitializer extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(RegionInitializer.class);

    /**
     * The codes of the 4 records.
     */
    private static final char[] CODES =
            {'E', 'C', 'I', 'A'};

    /**
     * The names of the 4 records.
     */
    private static final String[] NAMES =
            {"Europe", "Caribbean", "Indies", "Africa"};

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NAMES.length;

    /**
     * Default constructor.
     */
    public RegionInitializer() {
        super();
        LOGGER.debug("RegionInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Region> records = RegionManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("RegionInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Region thisRegion = new Region(); //NOPMD
            thisRegion.setCode(CODES[i]);
            thisRegion.setName(NAMES[i]);
            RegionManager.getInstance().add(thisRegion);
        }

        LOGGER.info("RegionInitializer complete.");
    }
}
