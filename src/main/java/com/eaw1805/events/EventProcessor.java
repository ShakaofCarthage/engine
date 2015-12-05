package com.eaw1805.events;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.events.army.LoseBattalionsEvent;
import com.eaw1805.events.army.LoseBrigadesEvent;
import com.eaw1805.events.army.NewCommandersEvent;
import com.eaw1805.events.army.NewSupremeCommanderEvent;
import com.eaw1805.events.army.RemoveEmptyArmiesEvent;
import com.eaw1805.events.army.RemoveEmptyCorpsEvent;
import com.eaw1805.events.army.SupplyLinesEvent;
import com.eaw1805.events.army.TransferCommandersEvent;
import com.eaw1805.events.army.WinterAttritionEvent;
import com.eaw1805.events.espionage.CatchSpiesEvent;
import com.eaw1805.events.espionage.EspionageEvent;
import com.eaw1805.events.espionage.GenerateSpiesEvent;
import com.eaw1805.events.espionage.ScoutingEvent;
import com.eaw1805.events.fleet.ConstructShipsEvent;
import com.eaw1805.events.fleet.DestroyShipsEvent;
import com.eaw1805.events.fleet.FisheriesEvent;
import com.eaw1805.events.fleet.JumpOffPointsEvent1804;
import com.eaw1805.events.fleet.JumpOffPointsEvent1805;
import com.eaw1805.events.fleet.RemoveEmptyFleetsEvent;
import com.eaw1805.events.fleet.StormEvent;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;

/**
 * Responsible for processing the events of the turn.
 */
public class EventProcessor
        extends OrderProcessor {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(EventProcessor.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public EventProcessor(final GameEngine caller) {
        super(caller);
        LOGGER.debug("EventProcessor instantiated.");
    }

    /**
     * Do economic turn.
     */
    public int process() {
        // Register events in the sequence that they will be processed
        final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final ArrayList<EventInterface> eventLst = new ArrayList<EventInterface>();
        if (!getGame().isAlwaysSummerWeather()) {
            eventLst.add(new StormEvent(this));
        }

        eventLst.add(new EpidemicsEvent(this));

        // Fleet & Trade related events
        if (!getGame().isFastShipConstruction()) {
            eventLst.add(new ConstructShipsEvent(this));
        }

        eventLst.add(new DestroyShipsEvent(this));
        eventLst.add(new RemoveEmptyFleetsEvent(this));

        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            eventLst.add(new JumpOffPointsEvent1805(this));

        } else if (getGame().getScenarioId() == HibernateUtil.DB_FREE) {
            eventLst.add(new JumpOffPointsEvent1804(this));
        }

        eventLst.add(new LoseBaggageTrainsEvent(this));
        eventLst.add(new FisheriesEvent(this));

        // Army related events
        eventLst.add(new WinterAttritionEvent(this));
        eventLst.add(new SupplyLinesEvent(this));
        eventLst.add(new LoseBattalionsEvent(this));
        eventLst.add(new LoseBrigadesEvent(this));
        eventLst.add(new RemoveEmptyCorpsEvent(this));
        eventLst.add(new RemoveEmptyArmiesEvent(this));

        // Commander related events
        eventLst.add(new NewCommandersEvent(this));
        eventLst.add(new NewSupremeCommanderEvent(this));
        eventLst.add(new TransferCommandersEvent(this));

        if (getGame().getScenarioId() > HibernateUtil.DB_MAIN) {

            // Rebellions
            eventLst.add(new RebellionEvent(this));
        }

        // Scenario 1808 events
        if (getGame().getScenarioId() == HibernateUtil.DB_S3) {

            // Guerilla
            eventLst.add(new GuerillaEvent(this));

            // Famine
            eventLst.add(new FamineEvent(this));

            // Additional Game Events
            eventLst.add(new HistoricEvent(this));
        }

        // Espionage related events
        eventLst.add(new CatchSpiesEvent(this));
        eventLst.add(new GenerateSpiesEvent(this));

        // Accumulate VPs
        eventLst.add(new VPAccumulation(this));

        // Report prisoners
        eventLst.add(new PrisonersOfWarEvent(this));

        if (getGame().getScenarioId() > HibernateUtil.DB_MAIN) {
            // Check dead nations
            eventLst.add(new CivilDisorderEvent(this));

            // Check for winners
            eventLst.add(new CheckWinners(this));
        }

        // Calculate Command & Administration points
        eventLst.add(new CommandAdminPoints(this));

        // Espionage related events
        eventLst.add(new EspionageEvent(this));
        eventLst.add(new ScoutingEvent(this));

        trans.commit();

        // Process all Events sequentially
        for (final EventInterface event : eventLst) {
            final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
            event.process();
            theTrans.commit();
        }

        LOGGER.info("EventProcessor completed.");
        return 0;
    }

}

