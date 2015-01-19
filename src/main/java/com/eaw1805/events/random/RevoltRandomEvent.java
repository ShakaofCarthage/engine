package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Royalists are revolting. France loses 5 VPs.
 */
public class RevoltRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RevoltRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RevoltRandomEvent(final EventProcessor myParent) {
        super(myParent, 2, 1);
        LOGGER.debug("RevoltRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Royalists are revolting. France loses 5 VPs
        final Nation france = NationManager.getInstance().getByID(NATION_FRANCE);

        // This event can only happen once per game
        final List<Report> lstReport = ReportManager.getInstance().listByOwnerKey(france,
                getParent().getGameEngine().getGame(), RE_REVO);

        if (lstReport == null || lstReport.isEmpty()) {
            final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

            // Decrease VPs
            changeVP(getParent().getGameEngine().getGame(), france, VP_RE_REVO, "Royalists revolting against government");

            // Set flag
            report(free, RE_REVO, Integer.toString(france.getId()));
            report(france, RE_REVO, "1");
            LOGGER.info("RevoltRandomEvent will affect " + france.getName() + " this turn.");

            // Add news entry
            newsGlobal(france, NEWS_POLITICAL, true,
                    "Royalists are revolting against our government. Our cities are dealing with civil unrest.",
                    "French royalists are revolting against their government. All major cities across the nation are dealing with civil unrest.");
        }

        theTrans.commit();
        LOGGER.info("RevoltRandomEvent processed.");
    }

}
