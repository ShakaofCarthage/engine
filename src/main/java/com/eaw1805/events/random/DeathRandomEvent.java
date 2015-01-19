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
 * A personality of significant importance died. Country loses 1 VP.
 */
public class DeathRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DeathRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DeathRandomEvent(final EventProcessor myParent) {
        super(myParent, 4, 1);
        LOGGER.debug("DeathRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

        final Nation thisNation = pickNation();

        // Decrease VPs
        changeVP(getParent().getGameEngine().getGame(), thisNation, VP_RE_DEAT, "Significant personality dies");

        // Set flag
        report(free, RE_DEAT, Integer.toString(thisNation.getId()));
        report(thisNation, RE_DEAT, "1");
        LOGGER.info("DeathRandomEvent will affect " + thisNation.getName() + " this turn.");

        // Add news entry
        newsSingle(thisNation, NEWS_POLITICAL, "A personality of significant importance died this month. Our nation mourns the loss.");

        theTrans.commit();
        LOGGER.info("DeathRandomEvent processed.");
    }

}
