package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.RelationConstants;
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
 * Massive army desertions. Target countries get an extra 4% attricion rate to all their armies (even if in barracks) due to desertions.
 */
public class DesertionsRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DesertionsRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DesertionsRandomEvent(final EventProcessor myParent) {
        super(myParent, 4, 2);
        LOGGER.debug("DesertionsRandomEvent instantiated.");
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
            // Cannot happen to countries that have no 'War' relations with anyone.
            final List<NationsRelation> lstRel = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), nation);
            boolean hasWar = false;
            for (final NationsRelation relation : lstRel) {
                if (relation.getRelation() == REL_WAR) {
                    hasWar = true;
                    break;
                }
            }

            if (hasWar) {
                finalList.add(nation);
            }
        }

        if (!finalList.isEmpty()) {

            // Choose a nation at random
            java.util.Collections.shuffle(finalList);
            final Nation thisNation = lstNation.get(0);

            // Set flag
            report(free, RE_DESE, Integer.toString(thisNation.getId()));
            report(thisNation, RE_DESE, "1");
            LOGGER.info("DesertionsRandomEvent will affect " + thisNation.getName() + " this turn.");

            // Add news entry
            newsSingle(thisNation, NEWS_MILITARY, "Our ministers report massive army desertions.");

            LOGGER.info("DesertionsRandomEvent processed.");
        }

        theTrans.commit();
    }

}
