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
 * Target country receives an additional 30% tax income this month.
 */
public class BoomingRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BoomingRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BoomingRandomEvent(final EventProcessor myParent) {
        super(myParent, 4, 1);
        LOGGER.debug("BoomingRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

        final Nation thisNation = pickNation();

        // Set flag
        report(free, RE_BOOM, Integer.toString(thisNation.getId()));
        report(thisNation, RE_BOOM, "1");
        LOGGER.info("BoomingRandomEvent will affect " + thisNation.getName() + " this turn.");

        // Add news entry
        newsSingle(thisNation, NEWS_POLITICAL, "Our ministers report a booming economy for this month.");

        theTrans.commit();
        LOGGER.info("BoomingRandomEvent processed.");
    }

}
