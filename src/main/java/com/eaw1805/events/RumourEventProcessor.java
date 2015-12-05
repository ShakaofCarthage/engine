package com.eaw1805.events;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.Engine;
import com.eaw1805.data.model.Nation;
import com.eaw1805.events.rumours.ArmyPosition;
import com.eaw1805.events.rumours.BuildFortress;
import com.eaw1805.events.rumours.ForeignAid;
import com.eaw1805.events.rumours.NavalPosition;
import com.eaw1805.events.rumours.SpyPosition;
import com.eaw1805.events.rumours.Trading;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Responsible for producing rumours for this turn.
 */
public class RumourEventProcessor
        extends EventProcessor {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RumourEventProcessor.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public RumourEventProcessor(final GameEngine caller) {
        super(caller);
        LOGGER.debug("RumourEventProcessor instantiated.");
    }

    /**
     * Produce rumours.
     */
    public final int process() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        int totRumours = 0;
        final List<Nation> lstNation = getGameEngine().getAliveNations();
        theTrans.rollback();

        // Call Game specific pages.
        final ExecutorService executorService = Executors.newFixedThreadPool(Engine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (final Nation nation : lstNation) {
            // We have make 2 rolls during newsletter creation, ranging from 3 to 10
            for (int roll = 0; roll < 2; roll++) {
                final int totEntries = getGameEngine().getRandomGen().nextInt(9) + 3;
                LOGGER.info("RumourEventProcessor generating " + totEntries + " " + (roll == 0) + " rumours for " + nation.getName());

                // Construct rumour event
                final AbstractRumourEventProcessor rumourEventProcessor = chooseRumour(nation, (roll == 0));

                // produce rumours
                for (int rumour = 0; rumour < totEntries; rumour++) {
                    futures.add(executorService.submit(new Callable<Boolean>() {
                        public Boolean call() {
                            rumourEventProcessor.process();
                            return true;
                        }
                    }));
                }

                totRumours += totEntries;
            }
        }

        // wait for the execution all tasks
        try {
            // wait for all tasks to complete before continuing
            for (Future<Boolean> task : futures) {
                task.get();
            }

            executorService.shutdownNow();

        } catch (Exception ex) {
            LOGGER.error("Task execution interrupted", ex);
        }

        LOGGER.info("RumourEventProcessor completed.");
        return totRumours;
    }

    private AbstractRumourEventProcessor chooseRumour(final Nation target, final boolean isTrue) {
        final int roll = getGameEngine().getRandomGen().nextInt(11) + 1;
        switch (roll) {
            case 1:
            case 2:
            case 3:
                return new ArmyPosition(this, target, isTrue);

            case 4:
            case 5:
            case 6:
                return new NavalPosition(this, target, isTrue);

            case 7:
                return new SpyPosition(this, target, isTrue);

            case 8:
                return new ForeignAid(this, target, isTrue);

            case 9:
                return new Trading(this, target, isTrue);

            case 10:
            default:
                return new BuildFortress(this, target, isTrue);
        }
    }

}
