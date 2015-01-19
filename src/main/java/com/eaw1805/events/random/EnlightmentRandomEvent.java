package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

/**
 * Target country made a significant scientific progress. They receive +2 VPs this turn.
 */
public class EnlightmentRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(EnlightmentRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public EnlightmentRandomEvent(final EventProcessor myParent) {
        super(myParent, 4, 1);
        LOGGER.debug("EnlightmentRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        final Nation thisNation = pickNation();

        // Increase VPs
        changeVP(getParent().getGameEngine().getGame(), thisNation, VP_RE_ENLI, "Scientists made major breakthrough");

        // Set flag
        report(free, RE_ENLI, Integer.toString(thisNation.getId()));
        report(thisNation, RE_ENLI, "1");
        LOGGER.info("EnlightmentRandomEvent will affect " + thisNation.getName() + " this turn.");

        // Add news entry
        newsGlobal(thisNation, NEWS_POLITICAL, true,
                "Our scientists have made a major breakthrough this month.",
                "The scientists of " + thisNation.getName() + " have made a major breakthrough this month.");

        theTrans.commit();
        LOGGER.info("EnlightmentRandomEvent processed.");
    }

}
