package com.eaw1805.events.army;

import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Removes corps without brigades.
 */
public class RemoveEmptyCorpsEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RemoveEmptyCorpsEvent.class);

    /**
     * The game to inspect.
     */
    private Game thisGame;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RemoveEmptyCorpsEvent(final EventProcessor myParent) {
        super(myParent);
        thisGame = getParent().getGameEngine().getGame();
        LOGGER.debug("RemoveEmptyCorpsEvent instantiated.");
    }

    /**
     * Alternative constructor.
     *
     * @param myGame the game to inspect.
     */
    public RemoveEmptyCorpsEvent(final Game myGame) {
        super(myGame);
        thisGame = myGame;
        LOGGER.debug("RemoveEmptyCorpsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Corp> corpsList = CorpManager.getInstance().listGame(thisGame);

        for (final Corp corp : corpsList) {
            // Check number of brigades
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(thisGame, corp.getCorpId());

            if (lstBrigades.isEmpty()) {
                // The Corp is empty
                // Check if a commander exists
                if (corp.getCommander() != null) {
                    corp.getCommander().setCorp(0);
                    corp.getCommander().setArmy(0);
                    CommanderManager.getInstance().update(corp.getCommander());

                    corp.setCommander(null);
                }

                report(thisGame, corp.getNation(), "corp.disband." + corp.getCorpId(), "Corps [" + corp.getName() + "] contained no brigades. Empty corps was disbanded.");
                newsSingle(thisGame, corp.getNation(), NEWS_MILITARY, "Corps " + corp.getName() + " was disbanded as it contained no brigades.");
                LOGGER.info("Corps [" + corp.getName() + "] of Nation [" + corp.getNation().getName() + "] contained no brigades. Empty corps was disbanded.");

                // Demolish army
                CorpManager.getInstance().delete(corp);
            }
        }

        LOGGER.info("RemoveEmptyCorpsEvent processed.");
    }
}
