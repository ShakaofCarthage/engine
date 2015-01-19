package com.eaw1805.events.random;

import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Target fleet/ship not in shipyard with 2 warships or less is attacked by 1 to 5 pirate ships
 * (Pirate Ships are 40% class 1, 40% class 2, 20% class 3). Colonial seas are checked first, then European.
 * Fleets of merchants ships alone can always be attacked regardless of numbers. Fleets of 4 warships or more
 * will never be attacked. Pirates do not interrupt movement, even though piracy occurs in one random sea tile
 * of the player's fleet movement path. If no such fleet at sea, re-roll the event.
 */
public class PiratesRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(PiratesRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public PiratesRandomEvent(final EventProcessor myParent) {
        super(myParent, 20, 2);
        LOGGER.debug("PiratesRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        LOGGER.info("PiratesRandomEvent processed.");
    }

}
