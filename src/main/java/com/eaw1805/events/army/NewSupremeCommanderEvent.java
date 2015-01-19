package com.eaw1805.events.army;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.comparators.CommanderRank;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements New Supreme Commanders Events.
 */
public class NewSupremeCommanderEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(NewSupremeCommanderEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public NewSupremeCommanderEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("NewSupremeCommanderEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        LOGGER.info("NewSupremeCommanderEvent processed.");

        // Iterate through nations of game
        final List<Nation> nationList = NationManager.getInstance().list();

        // Remove free nation
        nationList.remove(0);

        for (final Nation nation : nationList) {
            // Retrieve commanders for particular nation
            final List<Commander> commanderList = CommanderManager.getInstance().listGameNationAlive(getParent().getGameEngine().getGame(), nation);
            final List<Commander> activeList = new ArrayList<Commander>();
            boolean found = false;
            for (final Commander commander : commanderList) {
                // if commander is not captured
                if (commander.getCaptured().getId() == commander.getNation().getId()) {
                    // keep him in the active list
                    activeList.add(commander);

                    if (commander.getSupreme()) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found && !activeList.isEmpty()) {
                // Supreme commander is lost,

                // Sort commanders first by region then by rank
                final Commander[] sortedList = new Commander[activeList.size()];
                activeList.toArray(sortedList);
                Arrays.sort(sortedList, new CommanderRank());

                // The second highest commander will be immediately promoted to Supreme Commander.
                final Commander newSupreme = sortedList[0];

                // His strategic rating gets a +1 bonus and his tactical rating a +10 bonus.
                newSupreme.setStrc(newSupreme.getStrc() + 1);
                newSupreme.setComc(newSupreme.getComc() + 10);
                newSupreme.setSupreme(true);

                // Militaristic Bonus
                if (nation.getId() == NationConstants.NATION_RHINE
                        || nation.getId() == NationConstants.NATION_FRANCE
                        || nation.getId() == NationConstants.NATION_PRUSSIA) {
                    newSupreme.setComc(newSupreme.getComc() + 1);
                }

                CommanderManager.getInstance().update(newSupreme);

                LOGGER.info("New Supreme Commander [Commander=" + newSupreme.getId() + "]");
                newsSingle(newSupreme.getNation(), NEWS_MILITARY, newSupreme.getName() + " is now acting as the Supreme Field Marshal with General Staff.");
            }
        }
    }

}
