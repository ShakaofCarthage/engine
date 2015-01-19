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
 * Persia attacks Russia. Russia loses 5 VPs.
 */
public class PersiaRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(PersiaRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public PersiaRandomEvent(final EventProcessor myParent) {
        super(myParent, 2, 1);
        LOGGER.debug("PersiaRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(super.getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Persia attacks Russia. Russia loses 5 VPs
        final Nation russia = NationManager.getInstance().getByID(NATION_RUSSIA);

        // This event can only happen once per game
        final List<Report> lstReport = ReportManager.getInstance().listByOwnerKey(russia,
                getParent().getGameEngine().getGame(), RE_PERS);

        if (lstReport == null || lstReport.isEmpty()) {
            final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

            // Decrease VPs
            changeVP(getParent().getGameEngine().getGame(), russia, VP_RE_PERS, "Persia attacked us");

            // Set flag
            report(free, RE_PERS, Integer.toString(russia.getId()));
            report(russia, RE_PERS, "1");
            LOGGER.info("PersiaRandomEvent will affect " + russia.getName() + " this turn.");

            // Add news entry
            newsGlobal(russia, NEWS_POLITICAL, true,
                    "Persia has attacked us. A war is waged against our south eastern borders.",
                    "Persia has attacked Russia. A war is waged at the south eastern borders of Russia.");
        }

        theTrans.commit();
        LOGGER.info("PersiaRandomEvent processed.");
    }

}
