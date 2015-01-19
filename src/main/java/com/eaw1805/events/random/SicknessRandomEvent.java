package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Randomly chose a commander. He is out of action for 2 to 6 months..
 */
public class SicknessRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SicknessRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public SicknessRandomEvent(final EventProcessor myParent) {
        super(myParent, 8, 1);
        LOGGER.debug("SicknessRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);

        // Pick a nation randomly
        final Nation thisNation = pickNation();

        // Identify healthy commanders
        final List<Commander> lstCommander = CommanderManager.getInstance().listGameNation(getParent().getGameEngine().getGame(), thisNation);
        final List<Commander> finalList = new ArrayList<Commander>();
        for (final Commander commander : lstCommander) {
            if (commander.getSick() == 0 && commander.getDead() == false) {
                finalList.add(commander);
            }
        }

        // Randomly choose a commander
        if (!finalList.isEmpty()) {
            java.util.Collections.shuffle(finalList);
            final Commander sickCommander = finalList.get(0);

            // All ratings are divided by 3 is out of action for 2 to 6 months.
            sickCommander.setSick(getParent().getGameEngine().getRandomGen().nextInt(5) + 2);
            sickCommander.setStrc(sickCommander.getStrc() / 3);
            sickCommander.setComc(sickCommander.getComc() / 3);
            CommanderManager.getInstance().update(sickCommander);

            // Set flag
            report(free, RE_SICK, Integer.toString(thisNation.getId()));
            report(thisNation, RE_SICK, Integer.toString(sickCommander.getId()));
            LOGGER.info("SicknessRandomEvent will affect " + thisNation.getName() + " this turn.");

            // Add news entry
            newsSingle(thisNation, NEWS_MILITARY, "Our commander " + sickCommander.getName() + " was struck by a dangerous disease. He is receiving treatment from the military medics. His performance will greatly degrade until he gets well.");
        }

        theTrans.commit();
        LOGGER.info("SicknessRandomEvent processed.");
    }

}
