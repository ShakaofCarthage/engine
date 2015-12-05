package com.eaw1805.events.espionage;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implements Catch Spies Events.
 * <p/>
 * While situated at a hostile barrack (i.e. a barrack that belongs to a country that has 'war' relations
 * towards your country), the spy runs a risk of being revealed. The chance of your spy being revealed and
 * captured is 5% per turn.
 */
public class CatchSpiesEvent
        extends AbstractEventProcessor
        implements EventInterface, NationConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CatchSpiesEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CatchSpiesEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("CatchSpiesEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        // Iterate through all spies
        final List<Spy> lstSpies = SpyManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        for (final Spy thisSpy : lstSpies) {
            // Retrieve location of spy
            final Sector thisSector = SectorManager.getInstance().getByPosition(thisSpy.getPosition());

            // Check if spy is located in foreign barrack
            if (thisSector.hasBarrack()) {
                if (thisSector.getNation().getId() == thisSpy.getNation().getId()) {
                    // Nothing to do, continue with next spy

                } else if (thisSector.getNation().getId() == NATION_NEUTRAL) {
                    // Nothing to do, continue with next spy

                } else {
                    // Check Sector's relations against owner.
                    final NationsRelation relation = getByNations(thisSector.getNation(), thisSpy.getNation());

                    if (relation.getRelation() == REL_WAR) {
                        // The chance of your spy being revealed and captured is 5% per turn.
                        final int roll = getRandomGen().nextInt(101) + 1;
                        if (roll <= 5) {
                            // Report capture of spy.
                            report(thisSpy.getNation(), "spy.death", "Spy '" + thisSpy.getName() + "' was arrested by enemy forces at " + thisSector.getPosition().toString());
                            report(thisSector.getNation(), "spy.death", "We arrested spy '" + thisSpy.getName() + "' of " + thisSpy.getNation().getName() + " at " + thisSector.getPosition().toString());

                            newsPair(thisSpy.getNation(), thisSector.getNation(), NEWS_POLITICAL,
                                    "Our spy '" + thisSpy.getName() + "' was arrested by enemy forces at " + thisSector.getPosition().toString(),
                                    "We arrested '" + thisSpy.getName() + "', the spy of " + thisSpy.getNation().getName() + " at " + thisSector.getPosition().toString());

                            LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was captured by enemy forces at " + thisSector.getPosition().toString() + " owned by [" + thisSector.getNation().getName() + "]");

                            // Remove spy from game
                            SpyManager.getInstance().delete(thisSpy);

                            // continue with next spy
                            continue;
                        }
                    }
                }
            }

            // Update stationary counter of spy
            thisSpy.setStationary(thisSpy.getStationary() + 1);
            SpyManager.getInstance().update(thisSpy);
        }

        LOGGER.info("CatchSpiesEvent processed.");
    }
}
