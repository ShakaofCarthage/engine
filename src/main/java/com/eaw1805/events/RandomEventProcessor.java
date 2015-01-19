package com.eaw1805.events;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.events.random.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Responsible for processing the random events of the turn.
 */
public final class RandomEventProcessor
        extends EventProcessor {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RandomEventProcessor.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public RandomEventProcessor(final GameEngine caller) {
        super(caller);
        LOGGER.debug("RandomEventProcessor instantiated.");
    }

    /**
     * Produce random events.
     */
    public final int process() {
        // Register events in the sequence that they will be processed
        final ArrayList<EventInterface> eventLst = new ArrayList<EventInterface>();

        // Scenario 1808: Spanish Guerilla, Spanish Nationalism, Famine
        if (getGame().getScenarioId() == HibernateUtil.DB_S3) {


        } else {
            eventLst.add(new ArrestedRandomEvent(this));
            eventLst.add(new BoomingRandomEvent(this));
            eventLst.add(new CorruptionRandomEvent(this));
            eventLst.add(new CrisisRandomEvent(this));
            eventLst.add(new CultureRandomEvent(this));
            eventLst.add(new DeathRandomEvent(this));
            eventLst.add(new DeficitRandomEvent(this));
            eventLst.add(new DesertionsRandomEvent(this));
            eventLst.add(new EnlightmentRandomEvent(this));
            eventLst.add(new HarvestRandomEvent(this));
            eventLst.add(new NationalismRandomEvent(this));
            eventLst.add(new PersiaRandomEvent(this));
            //eventLst.add(new PiratesRandomEvent(this));
            eventLst.add(new RaidRandomEvent(this));
            eventLst.add(new RevoltRandomEvent(this));
            eventLst.add(new ScandalRandomEvent(this));
            eventLst.add(new ShortageRandomEvent(this));
            eventLst.add(new SicknessRandomEvent(this));
            eventLst.add(new StrikeRandomEvent(this));
            eventLst.add(new SurplusRandomEvent(this));
            eventLst.add(new USRandomEvent(this));
        }

        // Process all Events sequentially
        for (final EventInterface event : eventLst) {
            event.process();
        }

        LOGGER.info("RandomEventProcessor completed.");
        return 0;
    }


}
