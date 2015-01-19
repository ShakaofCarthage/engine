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

/**
 * target country receives 30% less citizens this turn. Event also cancels out "Nationalism" event.
 */
public class ShortageRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ShortageRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ShortageRandomEvent(final EventProcessor myParent) {
        super(myParent, 7, 2);
        LOGGER.debug("ShortageRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        final Nation thisNation = pickNation();

        // Set flag
        report(free, RE_SHOR, Integer.toString(thisNation.getId()));
        report(thisNation, RE_SHOR, "1");
        LOGGER.info("ShortageRandomEvent will affect " + thisNation.getName() + " this turn.");

        // Add news entry
        newsSingle(thisNation, NEWS_MILITARY, "Our ministers report that this month we are experiencing manpower shortage.");

        theTrans.commit();
        LOGGER.info("ShortageRandomEvent processed.");
    }

}
