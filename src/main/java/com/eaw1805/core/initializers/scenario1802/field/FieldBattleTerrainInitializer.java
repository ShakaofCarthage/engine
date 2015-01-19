package com.eaw1805.core.initializers.scenario1802.field;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.field.FieldBattleTerrainManager;
import com.eaw1805.data.model.battles.field.FieldBattleTerrain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the table with the terrain data.
 *
 * @author fragkakis
 */
public class FieldBattleTerrainInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(FieldBattleTerrainInitializer.class);


    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = FieldBattleTerrainEnum.values().length;

    /**
     * Default constructor.
     */
    public FieldBattleTerrainInitializer() {
        super();
        LOGGER.debug("FieldBattleTerrainInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<FieldBattleTerrain> records = FieldBattleTerrainManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("FieldBattleTerrainInitializer invoked.");

        // Initialize records
        for (FieldBattleTerrainEnum fbtEnum : FieldBattleTerrainEnum.values()) {
            final FieldBattleTerrain thisTerrain = new FieldBattleTerrain(); //NOPMD
            thisTerrain.setName(fbtEnum.getName());
            FieldBattleTerrainManager.getInstance().add(thisTerrain);
        }

        LOGGER.info("FieldBattleTerrainInitializer complete.");
    }

    /**
     * Enumeration of all the possible terrain types.
     *
     * @author fragkakis
     */
    public enum FieldBattleTerrainEnum {

        CLEAR_TERRAIN("Clean Terrain"), FOREST("Forest"), BUSHES("Bushes"), BUILDINGS("Buildings"), RIVER("River"), LAKE("Lake");

        private final String name;

        private FieldBattleTerrainEnum(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}
