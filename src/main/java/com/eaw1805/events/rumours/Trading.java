package com.eaw1805.events.rumours;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.TradeConstants;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractRumourEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces Trading rumours.
 */
public class Trading
        extends AbstractRumourEventProcessor
        implements EventInterface, RegionConstants, GoodConstants, TradeConstants, OrderConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(Trading.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     * @param isTrue   if the rumour is true.
     */
    public Trading(final EventProcessor myParent, final Nation target, final boolean isTrue) {
        super(myParent, target, isTrue);
        LOGGER.debug("Trading instantiated.");
    }

    /**
     * Process a true rumour.
     */
    protected final void produceTrue() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // aggregate amounts traded by players per trade city
        final Map<Integer, Set<Nation>> tradedNations = aggregatePlayerOrders();

        if (!tradedNations.isEmpty()) {
            // Pick a random trade city
            final List<Integer> lstCities = new ArrayList<Integer>();
            lstCities.addAll(tradedNations.keySet());
            java.util.Collections.shuffle(lstCities);

            final TradeCity theCity = TradeCityManager.getInstance().getByID(lstCities.get(0));
            final Sector thisSector = SectorManager.getInstance().getByPosition(theCity.getPosition());

            // construct list of traded nations
            final List<Nation> lstNations = new ArrayList<Nation>();
            lstNations.addAll(tradedNations.get(theCity.getCityId()));
            java.util.Collections.shuffle(lstNations);

            // Construct report
            String report;
            if (lstNations.size() == 1) {
                final Nation nation1 = lstNations.get(0);
                report = "A burst of activity this month in " + theCity.getName() + ", as " + nation1.getName() + " traded in its marketplace";

            } else {
                final Nation nation1 = lstNations.get(0);
                final Nation nation2 = lstNations.get(1);
                report = "A burst of activity this month in " + theCity.getName() + ", as " + nation1.getName() + " and " + nation2.getName() + " traded in its marketplace";
            }

            // produce news
            newsGlobal(thisSector.getNation(), NEWS_FOREIGN, false, report, report);
        }

        theTrans.commit();
        LOGGER.debug("Trading (true rumour) processed.");
    }

    /**
     * Process a false rumour.
     */
    protected final void produceFalse() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final List<Nation> aliveNations = getParent().getGameEngine().getAliveNations();
        aliveNations.remove(getNation());

        // Pick a random trade city
        final List<TradeCity> lstCities = TradeCityManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        java.util.Collections.shuffle(lstCities);
        int pos = 0;
        TradeCity theCity = lstCities.get(0);
        Sector thisSector = SectorManager.getInstance().getByPosition(theCity.getPosition());
        boolean isNeutral = !aliveNations.contains(thisSector.getNation());
        while (!isNeutral && pos < lstCities.size()) {
            theCity = lstCities.get(pos++);
            thisSector = SectorManager.getInstance().getByPosition(theCity.getPosition());
            isNeutral = !aliveNations.contains(thisSector.getNation());
        }

        if (isNeutral) {
            // Could not identify a non-neutral trade city
            theTrans.commit();
            LOGGER.info("Trading (false rumour) processed -- no non-neutral trade city could be found.");
            return;
        }

        // Retrieve relations
        final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), thisSector.getNation());

        // Select all relations above neutral
        final List<NationsRelation> finalList = new ArrayList<NationsRelation>();
        for (final NationsRelation thisRelation : lstRelations) {
            if (thisRelation.getRelation() <= REL_TRADE) {
                finalList.add(thisRelation);
            }
        }

        if (!finalList.isEmpty()) {
            // Pick random relation
            java.util.Collections.shuffle(finalList);

            // Construct report
            String report;
            if (finalList.size() == 1) {
                final Nation nation1 = finalList.get(0).getTarget();
                report = "A burst of activity this month in " + theCity.getName() + ", as " + nation1.getName() + " traded in its marketplace";

            } else {
                final Nation nation1 = finalList.get(0).getTarget();
                final Nation nation2 = finalList.get(1).getTarget();
                report = "A burst of activity this month in " + theCity.getName() + ", as " + nation1.getName() + " and " + nation2.getName() + " traded in its marketplace";
            }

            // produce news
            news(getNation(), thisSector.getNation(), NEWS_FOREIGN, false, 0, report);
        }

        theTrans.commit();
        LOGGER.debug("Trading (false rumour) processed.");
    }

    /**
     * aggregate amounts traded by players per trade city.
     *
     * @return report the total value bought & sold per trade city and per good type.
     */
    private Map<Integer, Set<Nation>> aggregatePlayerOrders() {
        // Retrieve player trading orders depending on the phase.
        final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listTradeFirstOrders(getParent().getGameEngine().getGame());
        lstOrders.addAll(PlayerOrderManager.getInstance().listTradeSecondOrders(getParent().getGameEngine().getGame()));

        // aggregate amounts traded by players per trade city
        final Map<Integer, Set<Nation>> tradedNations = new HashMap<Integer, Set<Nation>>();
        for (final PlayerOrder order : lstOrders) {
            if (order.getNation().getId() != getNation().getId()) {

                final int tradeCityId;
                if (Integer.parseInt(order.getParameter1()) == ArmyConstants.TRADECITY) {//if the trade city is the first trader
                    tradeCityId = Integer.parseInt(order.getParameter2());

                } else {//else if trade city is the second trader
                    tradeCityId = Integer.parseInt(order.getParameter4());
                }

                // Check if this is the first entry for this trade city
                final Set<Nation> cityNations;
                if (tradedNations.containsKey(tradeCityId)) {
                    cityNations = tradedNations.get(tradeCityId);

                } else {
                    cityNations = new HashSet<Nation>();
                    tradedNations.put(tradeCityId, cityNations);
                }

                cityNations.add(order.getNation());
            }
        }

        return tradedNations;
    }

}
