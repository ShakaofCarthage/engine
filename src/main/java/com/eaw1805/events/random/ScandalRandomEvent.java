package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Target country loses 1 VP.
 */
public class ScandalRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ScandalRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ScandalRandomEvent(final EventProcessor myParent) {
        super(myParent, 8, 1);
        LOGGER.debug("ScandalRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final List<Nation> lstNation = NationManager.getInstance().list();
        final Nation free = lstNation.remove(0); // remove free nation

        final List<Nation> finalList = new ArrayList<Nation>();
        for (final Nation nation : lstNation) {
            // Does not happen in countries with at least 1 relation to "war".
            final List<NationsRelation> lstRel = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), nation);
            boolean hasWar = false;
            for (final NationsRelation relation : lstRel) {
                if (relation.getRelation() == REL_WAR) {
                    hasWar = true;
                    break;
                }
            }

            if (!hasWar) {
                finalList.add(nation);
            }
        }

        if (!finalList.isEmpty()) {
            // Choose a nation at random
            java.util.Collections.shuffle(finalList);
            final Nation thisNation = finalList.get(0);

            // Decrease VPs
            changeVP(getParent().getGameEngine().getGame(), thisNation, VP_RE_SCAN, "Serious political scandal revealed");

            // Set flag
            report(free, RE_SCAN, Integer.toString(thisNation.getId()));
            report(thisNation, RE_SCAN, "1");
            LOGGER.info("ScandalRandomEvent will affect " + thisNation.getName() + " this turn.");

            // Add news entry
            newsSingle(thisNation, NEWS_POLITICAL, "A serious political scandal was revealed this month involving some prestigious ministers of our government.");

            LOGGER.info("ScandalRandomEvent processed.");
        }

        theTrans.commit();
    }

}
