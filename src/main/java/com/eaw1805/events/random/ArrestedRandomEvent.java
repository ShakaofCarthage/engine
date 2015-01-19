package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Randomly chose a spy from the game. He is removed from the game.
 */
public class ArrestedRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ArrestedRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ArrestedRandomEvent(final EventProcessor myParent) {
        super(myParent, 4, 1);
        LOGGER.debug("ArrestedRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Pick a random spy
        final List<Spy> lstSpies = SpyManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        java.util.Collections.shuffle(lstSpies);
        final Spy thisSpy = lstSpies.get(0);
        if (thisSpy != null) {
            // Report random event
            report(thisSpy.getNation(), "spy.death", "Spy [" + thisSpy.getName() + "] was arrested during an operation and neutralized.");
            newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy " + thisSpy.getName() + " was arrested during an operation and neutralized.");
            LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was arrested during an operation and neutralized.");

            // Remove spy from game
            SpyManager.getInstance().delete(thisSpy);
        }

        theTrans.commit();
        LOGGER.info("ArrestedRandomEvent processed.");
    }

}
