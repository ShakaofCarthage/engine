package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Selling in that trade city, increases profits by 20%. Buying from that trade city, costs 20% more.
 */
public class DeficitRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DeficitRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DeficitRandomEvent(final EventProcessor myParent) {
        super(myParent, 10, 4);
        LOGGER.debug("DeficitRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final List<TradeCity> lstTradeCities = TradeCityManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        final List<TradeCity> finalTradeCities = new ArrayList<TradeCity>();
        final Set<Integer> affectedTradeCities = new HashSet<Integer>();
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL); // retrieve free nation

        // Retrieve cities that are affected by a Deficit event
        final List<Report> lstReportDeficit = ReportManager.getInstance().listByOwnerTurnKey(free,
                getParent().getGameEngine().getGame(),
                getParent().getGameEngine().getGame().getTurn() + 1, RE_DEFI);

        for (final Report report : lstReportDeficit) {
            affectedTradeCities.add(Integer.parseInt(report.getValue()));
        }

        // Retrieve cities that are affected by a Surplus event
        final List<Report> lstReportSurp = ReportManager.getInstance().listByOwnerTurnKey(free,
                getParent().getGameEngine().getGame(),
                getParent().getGameEngine().getGame().getTurn() + 1, RE_SURP);

        for (final Report report : lstReportSurp) {
            affectedTradeCities.add(Integer.parseInt(report.getValue()));
        }

        // Generate list of trade cities that are not affected by any event
        for (final TradeCity city : lstTradeCities) {
            if (!affectedTradeCities.contains(city.getCityId())) {
                finalTradeCities.add(city);
            }
        }

        // Choose a nation at random
        java.util.Collections.shuffle(finalTradeCities);
        final TradeCity thisCity = finalTradeCities.get(0);

        // Set flag
        report(free, getParent().getGameEngine().getGame().getTurn() + 1, RE_SURP, Integer.toString(thisCity.getCityId()));
        LOGGER.info("DeficitRandomEvent will affect " + thisCity.getName() + " next turn.");

        // Determine owner of trade city
        final Sector thisSector = SectorManager.getInstance().getByPosition(thisCity.getPosition());
        final Nation owner = thisSector.getNation();

        // Add news entry
        newsGlobal(owner, NEWS_ECONOMY, false,
                "Our trade city " + thisCity.getName() + " is expected to experience deficit during next month.",
                "Trade city " + thisCity.getName() + " is expected to experience deficit during next month.");

        theTrans.commit();
        LOGGER.info("DeficitRandomEvent processed.");
    }

}
