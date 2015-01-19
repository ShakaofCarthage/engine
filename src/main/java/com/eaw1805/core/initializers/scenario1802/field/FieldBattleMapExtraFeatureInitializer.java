package com.eaw1805.core.initializers.scenario1802.field;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.field.FieldBattleMapExtraFeatureManager;
import com.eaw1805.data.model.battles.field.FieldBattleMapExtraFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the table with the extra features of the field battle terrain.
 *
 * @author fragkakis
 */
public class FieldBattleMapExtraFeatureInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(FieldBattleMapExtraFeatureInitializer.class);


    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = FieldBattleMapExtraFeatureEnum.values().length;

    /**
     * Default constructor.
     */
    public FieldBattleMapExtraFeatureInitializer() {
        super();
        LOGGER.debug("FieldBattleMapExtraFeatureInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<FieldBattleMapExtraFeature> records = FieldBattleMapExtraFeatureManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("FieldBattleMapExtraFeatureInitializer invoked.");

        // Initialize records
        for (FieldBattleMapExtraFeatureEnum mefEnum : FieldBattleMapExtraFeatureEnum.values()) {
            final FieldBattleMapExtraFeature thisTerrain = new FieldBattleMapExtraFeature(); //NOPMD
            thisTerrain.setName(mefEnum.getName());
            FieldBattleMapExtraFeatureManager.getInstance().add(thisTerrain);
        }

        LOGGER.info("FieldBattleMapExtraFeatureInitializer complete.");
    }

    /**
     * Enumeration of all the possible extra features a field battle map may have.
     *
     * @author fragkakis
     */
    public enum FieldBattleMapExtraFeatureEnum {
        BRIDGE("Bridge"), ROAD("Road"), FORTRESS_WALL("Fortress Wall"), PONTOON_BRIDGE("Pontoon Bridge"), ENTRENCHMENT("Entrenchment");

        private final String name;

        private FieldBattleMapExtraFeatureEnum(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}
