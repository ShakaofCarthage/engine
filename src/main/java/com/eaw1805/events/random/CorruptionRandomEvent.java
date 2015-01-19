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
 * Army maintenance costs increase by 25% this month.
 */
public class CorruptionRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CorruptionRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CorruptionRandomEvent(final EventProcessor myParent) {
        super(myParent, 4, 1);
        LOGGER.debug("CorruptionRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

        final Nation thisNation = pickNation();

        // Set flag
        report(free, RE_CORR, Integer.toString(thisNation.getId()));
        report(thisNation, RE_CORR, "1");
        LOGGER.info("CorruptionRandomEvent will affect " + thisNation.getName() + " this turn.");

        // Add news entry
        newsSingle(thisNation, NEWS_POLITICAL, "Our ministers report that ill-defined powers corrupt our military forces.");

        theTrans.commit();
        LOGGER.info("CorruptionRandomEvent processed.");
    }

}
