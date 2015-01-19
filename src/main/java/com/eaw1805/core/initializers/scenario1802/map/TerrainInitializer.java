package com.eaw1805.core.initializers.scenario1802.map;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.map.TerrainManager;
import com.eaw1805.data.model.map.Terrain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the table with the terrain data.
 */
public class TerrainInitializer extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(TerrainInitializer.class);

    /**
     * The codes of the 9 records.
     */
    private static final char[] CODES =
            {'B', 'D', 'G', 'H', 'K', 'Q', 'W', 'S', 'T', 'J', 'R', 'O', 'X'};

    /**
     * The names of the 9 records.
     */
    private static final String[] NAMES =
            {"Arable land", "Desert", "Mountains", "Hills", "Karst, stony steppe",
                    "Grassy prairie", "Wood/forest", "Swamp", "Taiga", "Jungle", "River",
                    "Ocean", "Impassable"};

    /**
     * The mps of the 11 records.
     */
    private static final int[] MPS = {4, 8, 12, 6, 7, 6, 8, 10, 8, 10, 6, 1, 0};

    /**
     * The mps during winter of the 11 records.
     */
    private static final int[] MPS_WINTER = {4, 8, 15, 8, 9, 7, 10, 8, 10, 10, 12, 1, 0};

    /**
     * The maximum density of the 11 records.
     */
    private static final int[] MAX_DENSITY = {9, 0, 2, 7, 4, 4, 3, 1, 1, 2, 0, 0, 0};

    /**
     * The attrition own of the 9 records.
     */
    private static final double[] ATTRITION_OWN =
            {.05, .25, .4, .1, .1, .05, .15, .5, .2, .6, .05, 0, 0};

    /**
     * The attrition foreign of the 9 records.
     */
    private static final double[] ATTRITION_FOREIGN =
            {.1, .5, .8, .2, .2, .1, .3, 1, .3, 1.2, .1, 0, 0};

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NAMES.length;

    /**
     * Default constructor.
     */
    public TerrainInitializer() {
        super();
        LOGGER.debug("TerrainInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Terrain> records = TerrainManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("TerrainInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Terrain thisTerrain = new Terrain(); //NOPMD
            thisTerrain.setCode(CODES[i]);
            thisTerrain.setName(NAMES[i]);
            thisTerrain.setMps(MPS[i]);
            thisTerrain.setMpsWinter(MPS_WINTER[i]);
            thisTerrain.setMaxDensity(MAX_DENSITY[i]);
            thisTerrain.setAttritionOwn(ATTRITION_OWN[i]);
            thisTerrain.setAttritionForeign(ATTRITION_FOREIGN[i]);
            TerrainManager.getInstance().add(thisTerrain);
        }

        LOGGER.info("TerrainInitializer complete.");
    }

}
