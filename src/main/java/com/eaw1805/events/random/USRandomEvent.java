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
 * US and Great Britain go to war. Britain loses 5 VPs.
 */
public class USRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(USRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public USRandomEvent(final EventProcessor myParent) {
        super(myParent, 2, 1);
        LOGGER.debug("USRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // US and Great Britain go to war. Britain loses 5 VPs
        final Nation britain = NationManager.getInstance().getByID(NATION_GREATBRITAIN);

        // This event can only happen once per game
        final List<Report> lstReport = ReportManager.getInstance().listByOwnerKey(britain,
                getParent().getGameEngine().getGame(), RE_US);

        if (lstReport == null || lstReport.isEmpty()) {
            final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

            // Decrease VPs
            changeVP(getParent().getGameEngine().getGame(), britain, VP_RE_US, "Riots in America");

            // Set flag
            report(free, RE_US, Integer.toString(britain.getId()));
            report(britain, RE_US, "1");
            LOGGER.info("USRandomEvent will affect " + britain.getName() + " this turn.");

            // Add news entry
            newsGlobal(britain, NEWS_ECONOMY, true,
                    "The United States declared war to us! It appears that unresolved issues from the time of their independence are still a major concern to them. We can end this war with minimal military losses for us, but the political cost is already quite significant.",
                    "The United States declared war against Great Britain! It appears that unresolved issues from the time of their independence are still a major concern to them. ");
        }

        theTrans.commit();
        LOGGER.info("USRandomEvent processed.");
    }

}
