package com.eaw1805.events.espionage;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Engine;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Generates the espionage reports for spies.
 */
public class EspionageEvent
        extends AbstractEventProcessor
        implements EventInterface, NationConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(EspionageEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public EspionageEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("EspionageEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public void process() {
        final ExecutorService executorService = Executors.newFixedThreadPool(Engine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        // identify list of nations
        final List<Spy> lstSpies;
        if (getParent().getGame().getScenarioId() == HibernateUtil.DB_FREE) {
            // Process only FRANCE
            lstSpies = SpyManager.getInstance().listGameNation(getParent().getGameEngine().getGame(),
                    NationManager.getInstance().getByID(NATION_FRANCE));

        } else {
            lstSpies = SpyManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        }

        // Iterate through all spies
        for (final Spy thisSpy : lstSpies) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getParent().getGameEngine().getGame().getScenarioId());

                    // Locate sector where spy is located
                    final Sector location = SectorManager.getInstance().getByPosition(thisSpy.getPosition());

                    // determine maximum bound in the reports
                    int error = 10;
                    if (thisSpy.getStationary() > 0) {
                        error = 5;
                    }

                    // Report relations
                    if (location.hasBarrack()) {
                        thisSpy.setReportRelations(location.getNation().getId());
                        LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] at "
                                + thisSpy.getPosition().toString()
                                + " reports foreign relations for Nation [" + location.getNation().getName() + "]");
                    } else {
                        thisSpy.setReportRelations(thisSpy.getNation().getId());
                    }
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".reportRelations", Integer.toString(thisSpy.getReportRelations()));

                    // Report trade
                    final TradeCity thisCity = TradeCityManager.getInstance().getByPosition(location.getPosition());
                    if (thisCity != null) {
                        thisSpy.setReportTrade(reportTrade(thisCity));
                        LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] at "
                                + thisSpy.getPosition().toString()
                                + " reports trading at [" + thisCity.getName() + "] " + thisSpy.getReportTrade());
                    }
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".reportTrade", thisSpy.getReportTrade());

                    // Report battalions
                    thisSpy.setReportBattalions(reportBattalions(thisSpy.getNation(), thisSpy.getPosition(), error));
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".reportBattalions", thisSpy.getReportBattalions());
                    LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] at "
                            + thisSpy.getPosition().toString()
                            + " reports battalions [" + thisSpy.getReportBattalions() + "] error (+/- " + error + "%)");

                    // Report brigades of 8 neighboring sectors
                    thisSpy.setReportBrigades(reportBrigades(thisSpy.getNation(), thisSpy.getPosition(), error));
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".reportBrigades", thisSpy.getReportBrigades());
                    LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] at "
                            + thisSpy.getPosition().toString()
                            + " reports brigades [" + thisSpy.getReportBrigades() + "] error (+/- " + error + "%)");

                    // Report warships
                    thisSpy.setReportShips(reportShips(thisSpy.getNation(), thisSpy.getPosition()));
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".reportShips", thisSpy.getReportShips());
                    LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] at "
                            + thisSpy.getPosition().toString()
                            + " reports ships [" + thisSpy.getReportShips() + "] error (+/- 0%)");

                    // Report nearby ships of 8 neighboring sectors
                    thisSpy.setReportNearbyShips(reportNearbyShips(thisSpy.getNation(), thisSpy.getPosition(), error));
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".reportNearbyShips", thisSpy.getReportNearbyShips());
                    LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] at "
                            + thisSpy.getPosition().toString()
                            + " reports nearby ships [" + thisSpy.getReportNearbyShips() + "] error (+/- " + error + "%)");

                    // Increase stationary counter
                    thisSpy.setStationary(thisSpy.getStationary() + 1);
                    report(thisSpy.getNation(), "spy." + thisSpy.getSpyId() + ".stationary", Integer.toString(thisSpy.getStationary()));

                    // Save entity
                    SpyManager.getInstance().update(thisSpy);

                    theTrans.commit();
                    return true;
                }
            }));
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
    }

    /**
     * If a spy is in a trade city, he will also report on the amount of trading that took place last month
     * (no trading, little trading, substantial trading or heavy trading).
     *
     * @param city the city where the spy is located.
     * @return the report.
     */
    protected String reportTrade(final TradeCity city) {
        final StringBuilder strBld = new StringBuilder();
        final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listTradeOrders(city);
        int totMoney = 0;
        for (final PlayerOrder order : lstOrders) {
            final int value = Integer.parseInt(order.getTemp9());
            totMoney += value;
        }

        if (totMoney == 0) {
            strBld.append("no trading");

        } else if (totMoney < 500000) {
            strBld.append("little trading");

        } else if (totMoney < 2000000) {
            strBld.append("substantial trading");

        } else {
            strBld.append("heavy trading");
        }

        return strBld.toString();
    }

    /**
     * Number of brigades on the 8 neighboring co-ordinates. (the report will be +/- 10% accurate).
     *
     * @param viewer       owner of spy.
     * @param thisPosition location of spy.
     * @param error        accuracy of report.
     * @return the report
     */
    protected String reportBrigades(final Nation viewer, final Position thisPosition, final int error) {
        final Map<Nation, BigInteger> countMap = BrigadeManager.getInstance().countBrigadesByOwner(thisPosition);
        final StringBuilder strBld = new StringBuilder();
        for (final Map.Entry<Nation, BigInteger> entry : countMap.entrySet()) {
            if (entry.getKey().getId() != viewer.getId()) {
                strBld.append(entry.getKey().getName());
                strBld.append(":");

                // add error in report
                final int roll = getRandomGen().nextInt(1 + Math.abs(error * 2)) - error;
                final int value = (int) (((100d + roll) / 100d) * entry.getValue().intValue());

                strBld.append(value);

                strBld.append("|");
            }
        }

        // Check that we have found at least 1 entry to report
        if (strBld.length() > 0) {
            return strBld.substring(0, strBld.length() - 1);
        }

        return "";
    }

    /**
     * number of battalions on the current position and their owner.
     * The report will be +/-10% accurate (i.e. 400 bats may be reported as being 360 to 440).
     *
     * @param viewer       the nation owning the spy.
     * @param thisPosition location of spy.
     * @param error        accuracy of report.
     * @return the report.
     */
    protected String reportBattalions(final Nation viewer, final Position thisPosition, final int error) {
        final Map<Nation, BigInteger> countMap = BattalionManager.getInstance().countBattalionsByOwner(thisPosition);
        final StringBuilder strBld = new StringBuilder();
        for (final Map.Entry<Nation, BigInteger> entry : countMap.entrySet()) {
            if (entry.getKey().getId() != viewer.getId()) {
                strBld.append(entry.getKey().getName());
                strBld.append(":");

                // add error in report
                final int roll = getRandomGen().nextInt(1 + Math.abs(error * 2)) - error;
                final int value = (int) (((100d + roll) / 100d) * entry.getValue().intValue());

                strBld.append(value);

                strBld.append("|");
            }
        }

        // Check that we have found at least 1 entry to report
        if (strBld.length() > 0) {
            return strBld.substring(0, strBld.length() - 1);
        }

        return "";
    }

    /**
     * number of warships and merchant ships on the current position and their owner.
     * This report will be accurate (it is easy to count the ships in a port).
     * Furthermore, the spy will report whether there are any troops loaded on the ships,
     * but not their exact number
     * (example: A French fleet of 21 warships, including 4 Ships of the Line, is stationed at Marseilles.
     * The ships have troops loaded on them).
     *
     * @param viewer       owner of spy.
     * @param thisPosition location of spy.
     * @return the report.
     */
    private String reportShips(final Nation viewer, final Position thisPosition) {
        final Map<Nation, BigInteger> countMMap = ShipManager.getInstance().countShipsByOwner(thisPosition, true);
        final Map<Nation, BigInteger> countWMap = ShipManager.getInstance().countShipsByOwner(thisPosition, false);
        final StringBuilder strBld = new StringBuilder();

        // First report nations with war ships and look-up if they also have merchant ships
        for (final Map.Entry<Nation, BigInteger> entry : countWMap.entrySet()) {
            if (entry.getKey().getId() != viewer.getId()) {
                strBld.append(entry.getKey().getName());
                strBld.append(":w=");
                strBld.append(entry.getValue());

                // check if merchant are available as well
                if (countMMap.containsKey(entry.getKey())) {
                    strBld.append(":m=");
                    strBld.append(countMMap.get(entry.getKey()));

                    // Remove entry
                    countMMap.remove(entry.getKey());
                }

                strBld.append("|");
            }
        }

        // now report all remaining nations with merchant ships that do not have any war ship
        for (final Map.Entry<Nation, BigInteger> entry : countMMap.entrySet()) {
            if (entry.getKey().getId() != viewer.getId()) {
                strBld.append(entry.getKey().getName());
                strBld.append(":w=0:m=");
                strBld.append(entry.getValue());
                strBld.append("|");
            }
        }

        // Check that we have found at least 1 entry to report
        if (strBld.length() > 0) {
            return strBld.substring(0, strBld.length() - 1);
        }

        return "";
    }

    /**
     * number of ships on the 8 neighboring co-ordinates. (the report will be +/- 10% accurate)
     *
     * @param viewer       owner of spy.
     * @param thisPosition location of spy.
     * @param error        accuracy of report.
     * @return the report.
     */
    protected String reportNearbyShips(final Nation viewer, final Position thisPosition, final int error) {
        final Map<Nation, BigInteger> countMap = ShipManager.getInstance().countNearbyShipsByOwner(thisPosition);
        final StringBuilder strBld = new StringBuilder();
        for (final Map.Entry<Nation, BigInteger> entry : countMap.entrySet()) {
            if (entry.getKey().getId() != viewer.getId()) {
                strBld.append(entry.getKey().getName());
                strBld.append(":");

                // add error in report
                final int roll = getRandomGen().nextInt(1 + error * 2) - error;
                final int value = (int) (((100d + roll) / 100d) * entry.getValue().intValue());

                strBld.append(value);

                strBld.append("|");
            }
        }

        // Check that we have found at least 1 entry to report
        if (strBld.length() > 0) {
            return strBld.substring(0, strBld.length() - 1);
        }

        return "";
    }

}
