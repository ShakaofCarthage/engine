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
 * Target countries have a "casus-belli" against each other for 1 turn.
 */
public class CrisisRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CrisisRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CrisisRandomEvent(final EventProcessor myParent) {
        super(myParent, 15, 2);
        LOGGER.debug("CrisisRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        // Choose a nation at random
        final Nation thisNation = pickNation();

        final List<Nation> finalList = new ArrayList<Nation>();

        // Does not target countries in "1" or "5" relations between them (War or Alliance).
        final List<NationsRelation> lstRel = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), thisNation);

        for (final NationsRelation relation : lstRel) {
            if (relation.getRelation() != REL_WAR
                    && relation.getRelation() != REL_ALLIANCE
                    && getParent().getGameEngine().isAlive(relation.getTarget())) {
                finalList.add(relation.getTarget());
            }
        }

        if (!finalList.isEmpty()) {
            // Choose a nation at random
            java.util.Collections.shuffle(finalList);
            final Nation thatNation = NationManager.getInstance().getByID(finalList.get(0).getId());

            // Set flag
            report(thisNation, getParent().getGameEngine().getGame().getTurn() + 1, RE_CRIS, Integer.toString(thatNation.getId()));
            report(thatNation, getParent().getGameEngine().getGame().getTurn() + 1, RE_CRIS, Integer.toString(thisNation.getId()));
            LOGGER.info("CrisisRandomEvent will affect " + thisNation.getName() + " this turn.");

            // Add news entry
            newsPair(thisNation, thatNation, NEWS_POLITICAL,
                    "Shocking behavior by the " + thatNation.getName() + " ambassador. Exposed improper affair with a member of our royal family. A direct insult to our nation. Your Highness, this gives us the casus belli should we declare war to them this month! VP reduction for a declaration of war will be halved.",
                    "Serious problems with our diplomatic relations with " + thisNation.getName() + ". False accusations by the " + thisNation.getName() + " royal family. Your Highness, they consider this as casus belli! VP reduction for a declaration of war will be halved.");

            LOGGER.info("CrisisRandomEvent processed.");
        }

        theTrans.commit();
    }

}
