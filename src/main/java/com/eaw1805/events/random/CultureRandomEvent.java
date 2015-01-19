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
 * Target country is the motherland of a famous artist. Country receives +1 VPs this turn.
 */
public class CultureRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, VPConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CultureRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CultureRandomEvent(final EventProcessor myParent) {
        super(myParent, 5, 1);
        LOGGER.debug("CultureRandomEvent instantiated.");
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
            // Will only happen in countries with no "war" relation to anyone.
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
            final Nation thisNation = lstNation.get(0);

            // Decrease VPs
            changeVP(getParent().getGameEngine().getGame(), thisNation, VP_RE_CULT, "Artist gains worldwide recognition");

            // Set flag
            report(free, RE_CULT, Integer.toString(thisNation.getId()));
            report(thisNation, RE_CULT, "1");
            LOGGER.info("CultureRandomEvent will affect " + thisNation.getName() + " this turn.");

            // Add news entry
            newsSingle(thisNation, NEWS_POLITICAL, "Our very own artist gains worldwide recognition. Fellow countrymen feel very proud.");

            LOGGER.info("CultureRandomEvent processed.");
        }

        theTrans.commit();
        LOGGER.info("CultureRandomEvent processed.");
    }

}
