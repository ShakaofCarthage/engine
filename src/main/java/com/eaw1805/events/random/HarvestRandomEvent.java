package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Calendar;

/**
 * Food production is doubled for this month.
 */
public class HarvestRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(HarvestRandomEvent.class);

    /**
     * Capture the game month.
     */
    private final int month;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public HarvestRandomEvent(final EventProcessor myParent) {
        super(myParent, 15, 3);

        // identify month
        final Calendar thisCal = getParent().getGameEngine().calendar();
        month = thisCal.get(Calendar.MONTH);
        LOGGER.debug("HarvestRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        // Can only happen in Europe between June to September
        if (month >= Calendar.JUNE && month <= Calendar.SEPTEMBER) {
            final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

            // Pick a nation randomly
            final Nation thisNation = pickNation();

            // Set flag
            report(free, RE_HARV, Integer.toString(thisNation.getId()));
            report(thisNation, RE_HARV, "1");
            LOGGER.info("HarvestRandomEvent will affect " + thisNation.getName() + " this turn.");

            // Add news entry
            newsSingle(thisNation, NEWS_ECONOMY, "Our ministers report that our estates had an excellent harvest.");

            LOGGER.info("HarvestRandomEvent processed.");
        }
        theTrans.commit();
    }

}
