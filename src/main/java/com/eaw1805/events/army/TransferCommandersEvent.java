package com.eaw1805.events.army;

import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implements the movement of the commanders from the pool to assigned units.
 */
public class TransferCommandersEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TransferCommandersEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public TransferCommandersEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("TransferCommandersEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        LOGGER.info("TransferCommandersEvent processed.");

        // Iterate through nations of game
        final List<Nation> nationList = NationManager.getInstance().list();

        // Remove free nation
        nationList.remove(0);

        for (final Nation nation : nationList) {
            // Retrieve commanders for particular nation
            final List<Commander> commanderList = CommanderManager.getInstance().listGameNation(getParent().getGameEngine().getGame(), nation);
            for (final Commander commander : commanderList) {
                if (commander.getTransit() > 0) {
                    commander.setTransit(commander.getTransit() - 1);

                    // Identify assigned unit
                    String assignedUnit;
                    Position unitPos;
                    if (commander.getArmy() > 0) {
                        final Army assignedArmy = ArmyManager.getInstance().getByID(commander.getArmy());
                        unitPos = assignedArmy.getPosition();
                        if (assignedArmy.getName().contains("army")) {
                            assignedUnit = assignedArmy.getName();
                        } else {
                            assignedUnit = "army " + assignedArmy.getName();
                        }

                    } else if (commander.getCorp() > 0) {
                        final Corp assignedCorps = CorpManager.getInstance().getByID(commander.getCorp());
                        unitPos = assignedCorps.getPosition();
                        if (assignedCorps.getName().contains("corps")) {
                            assignedUnit = assignedCorps.getName();
                        } else {
                            assignedUnit = "corps " + assignedCorps.getName();
                        }

                    } else {
                        assignedUnit = "";
                        unitPos = commander.getPosition();
                    }

                    if (commander.getTransit() <= 1) {
                        commander.setTransit(0);
                        commander.setPosition(unitPos);
                        commander.setPool(false);
                        commander.setInTransit(false);

                        LOGGER.info("Commander arrived at target unit [Commander=" + commander.getId() + "]");
                        newsSingle(commander.getNation(), NEWS_MILITARY, commander.getName() + " arrived at assigned unit, " + assignedUnit + " at " + commander.getPosition().getRegion().getName());

                    } else {
                        LOGGER.info("Commander in transit to target unit [Commander=" + commander.getId() + "]");
                        newsSingle(commander.getNation(), NEWS_MILITARY, commander.getName() + " is still traveling to reach the assigned unit, " + assignedUnit + " at " + commander.getPosition().getRegion().getName());
                    }

                    CommanderManager.getInstance().update(commander);
                }
            }
        }
    }

}
