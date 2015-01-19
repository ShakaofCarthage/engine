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
 * Target countries -10% Industrial Points production this turn
 * (Programmers - please remember to reduce accordingly by 10% the expense of materials needed to produce IP).
 */
public class StrikeRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(StrikeRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public StrikeRandomEvent(final EventProcessor myParent) {
        super(myParent, 5, 1);
        LOGGER.debug("StrikeRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(super.getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        final Nation thisNation = pickNation();

        // Set flag
        report(free, RE_STRI, Integer.toString(thisNation.getId()));
        report(thisNation, RE_STRI, "1");
        LOGGER.info("StrikeRandomEvent will affect " + thisNation.getName() + " this turn.");

        // Add news entry
        newsSingle(thisNation, NEWS_POLITICAL, "Our ministers report that our factories are dealing with workers on strike.");

        theTrans.commit();
        LOGGER.info("StrikeRandomEvent processed.");
    }

}
