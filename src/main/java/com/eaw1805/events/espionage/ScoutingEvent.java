package com.eaw1805.events.espionage;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Map;

/**
 * Generate Scouting Reports.
 */
public class ScoutingEvent
        extends EspionageEvent
        implements EventInterface, NationConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ScoutingEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ScoutingEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("ScoutingEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Map<Nation, Map<Sector, BigInteger>> listLC = BattalionManager.getInstance().listLCBattalions(getParent().getGameEngine().getGame());
        for (final Map.Entry<Nation, Map<Sector, BigInteger>> entry : listLC.entrySet()) {
            for (final Map.Entry<Sector, BigInteger> sector : listLC.get(entry.getKey()).entrySet()) {
                // Every army that has at least 40 battalions of Light Cavalry (LC),
                // each with headcount of more than 500 men, will provide reports
                if (sector.getValue().intValue() >= 40) {

                    // determine maximum bound in the reports
                    final int error = 30 - (int) ((sector.getValue().intValue() - 40) / 4d);

                    // Report Battalions
                    final String reportBattalions = reportBattalions(entry.getKey(), sector.getKey().getPosition(), error);
                    report(entry.getKey(), "scout." + sector.getKey().getId() + ".reportBattalions", reportBattalions);
                    LOGGER.info("Scouts of Nation [" + entry.getKey().getName() + "] at "
                            + sector.getKey().getPosition().toString()
                            + " reports battalions [" + reportBattalions + "] error (+/-" + error + "%)");

                    // Report Brigade
                    final String reportBrigades = reportBrigades(entry.getKey(), sector.getKey().getPosition(), error);
                    report(entry.getKey(), "scout." + sector.getKey().getId() + ".reportBrigades", reportBrigades);
                    LOGGER.info("Scouts of Nation [" + entry.getKey().getName() + "] at "
                            + sector.getKey().getPosition().toString()
                            + " reports brigades [" + reportBrigades + "] error (+/-" + error + "%)");
                }
            }
        }
        LOGGER.info("ScoutingEvent processed.");
    }

}
