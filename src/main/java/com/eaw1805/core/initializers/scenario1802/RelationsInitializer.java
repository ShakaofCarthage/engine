package com.eaw1805.core.initializers.scenario1802;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the relations among nations.
 */
public class RelationsInitializer
        extends AbstractThreadedInitializer
        implements RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RelationsInitializer.class);

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NationsInitializer.TOTAL_RECORDS;

    /**
     * RelationsInitializer's default constructor.
     */
    public RelationsInitializer() {
        super();
        LOGGER.debug("RelationsInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        return (RelationsManager.getInstance().list().size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the GOODS table by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("RelationsInitializer invoked.");

        final Game scenario = GameManager.getInstance().getByID(-1);

        // Get list of all nations
        final List<Nation> nations = NationManager.getInstance().list();

        // Remove "Free" Nation
        nations.remove(0);

        // Initialize records
        for (final Nation thisNation : nations) {
            for (final Nation thatNation : nations) {
                if (thisNation.getId() == thatNation.getId()) {
                    continue;
                }
                final NationsRelation thisRel = new NationsRelation(); // NOPMD
                thisRel.setGame(scenario);
                thisRel.setNation(thisNation);
                thisRel.setTarget(thatNation);
                thisRel.setRelation(REL_TRADE);
                thisRel.setPrisoners(0);
                thisRel.setTurnCount(0);
                thisRel.setPeaceCount(0);
                thisRel.setSurrenderCount(0);
                RelationsManager.getInstance().add(thisRel);
            }
        }

        LOGGER.info("RelationsInitializer complete.");
    }

}
