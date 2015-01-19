package com.eaw1805.events.army;

import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Removes armies without corps.
 */
public class RemoveEmptyArmiesEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RemoveEmptyArmiesEvent.class);

    /**
     * The game to inspect.
     */
    private Game thisGame;
    
    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RemoveEmptyArmiesEvent(final EventProcessor myParent) {
        super(myParent);
        thisGame = getParent().getGameEngine().getGame();
        LOGGER.debug("RemoveEmptyArmiesEvent instantiated.");
    }

    /**
     * Alternative constructor.
     *
     * @param myGame the game to inspect.
     */
    public RemoveEmptyArmiesEvent(final Game myGame) {
        super(myGame);
        thisGame = myGame;
        LOGGER.debug("RemoveEmptyArmiesEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Army> armyList = ArmyManager.getInstance().listGame(thisGame);

        for (final Army army : armyList) {
            // Check number of corps
            final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(thisGame, army.getArmyId());

            if (lstCorps.isEmpty()) {
                // The army is empty
                // Check if a commander exists
                if (army.getCommander() != null) {
                    army.getCommander().setCorp(0);
                    army.getCommander().setArmy(0);
                    CommanderManager.getInstance().update(army.getCommander());
                }

                report(thisGame, army.getNation(), "army.disband." + army.getArmyId(), "Army [" + army.getName() + "] contained no corps. Empty army was disbanded.");
                newsSingle(thisGame, army.getNation(), NEWS_MILITARY, "Army " + army.getName() + " was disbanded as it contained no corps.");
                LOGGER.info("Army [" + army.getName() + "] of Nation [" + army.getNation().getName() + "] contained no corps. Empty army was disbanded.");

                // Demolish army
                ArmyManager.getInstance().delete(army);
            }
        }

        LOGGER.info("RemoveEmptyArmiesEvent processed.");
    }
}
