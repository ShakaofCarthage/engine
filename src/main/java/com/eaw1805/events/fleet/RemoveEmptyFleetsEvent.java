package com.eaw1805.events.fleet;

import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


/**
 * Removes armies without corps.
 */
public class RemoveEmptyFleetsEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RemoveEmptyFleetsEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RemoveEmptyFleetsEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("RemoveEmptyFleetsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Fleet> fleetList = FleetManager.getInstance().listGame(getParent().getGameEngine().getGame());

        for (final Fleet fleet : fleetList) {
            // Check number of ships
            final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getParent().getGameEngine().getGame(), fleet.getFleetId());

            if (lstShips.isEmpty()) {
                // The fleet is empty

                report(fleet.getNation(), "fleet.disband." + fleet.getFleetId(), "Fleet [" + fleet.getName() + "] contained no ships. Empty fleet was disbanded.");
                newsSingle(fleet.getNation(), NEWS_MILITARY, "Fleet " + fleet.getName() + " was disbanded as it contained no ships.");
                LOGGER.info("Fleet [" + fleet.getName() + "] of Nation [" + fleet.getNation().getName() + "] contained no ships. Empty fleet was disbanded.");

                // Demolish army
                FleetManager.getInstance().delete(fleet);
            }
        }

        LOGGER.info("RemoveEmptyFleetsEvent processed.");
    }
}
