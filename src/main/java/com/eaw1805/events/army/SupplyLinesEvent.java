package com.eaw1805.events.army;

import com.eaw1805.algorithms.DistanceCalculator;
import com.eaw1805.algorithms.SupplyLinesConnectivity;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Engine;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implements Supply Lines Events.
 */
public class SupplyLinesEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SupplyLinesEvent.class);

    /**
     * If arctic zone suffers from severe winter.
     */
    private final transient boolean hasArctic;

    /**
     * If central zone suffers from severe winter.
     */
    private final transient boolean hasCentral;

    /**
     * If mediterranean zone suffers from severe winter.
     */
    private final transient boolean hasMediterranean;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public SupplyLinesEvent(final EventProcessor myParent) {
        super(myParent);

        final Nation freeNation = NationManager.getInstance().getByID(-1);
        hasArctic = getReport(freeNation, "winter.arctic").equals("1");
        hasCentral = getReport(freeNation, "winter.central").equals("1");
        hasMediterranean = getReport(freeNation, "winter.mediterranean").equals("1");

        LOGGER.debug("SupplyLinesEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final ExecutorService executorService = Executors.newFixedThreadPool(Engine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        // identify list of nations
        final List<Nation> lstNations;
        if (getParent().getGame().getScenarioId() == HibernateUtil.DB_FREE) {
            // Process only FRANCE
            lstNations = new ArrayList<Nation>();
            lstNations.add(NationManager.getInstance().getByID(NATION_FRANCE));

        } else {
            lstNations = NationManager.getInstance().list();
            lstNations.remove(0); // Remove FREE Nation
        }

        // compute supply lines
        for (final Nation nation : lstNations) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getParent().getGameEngine().getGame().getScenarioId());

                    // An instance of the distance calculator for each region.
                    final Map<Region, DistanceCalculator> distCalc = new HashMap<Region, DistanceCalculator>();
                    final List<Region> lstRegions = RegionManager.getInstance().list();
                    for (final Region thisRegion : lstRegions) {
                        distCalc.put(thisRegion, new DistanceCalculator(getParent().getGameEngine().getGame(), thisRegion, nation,
                                RelationsManager.getInstance(), SectorManager.getInstance(), BattalionManager.getInstance()));
                    }

                    final SupplyLinesConnectivity slc = new SupplyLinesConnectivity(getParent().getGameEngine(),
                            nation, distCalc, getParent().getGameEngine().getPatrolOrders(),
                            RelationsManager.getInstance(),
                            SectorManager.getInstance(),
                            RegionManager.getInstance(),
                            BarrackManager.getInstance(),
                            TradeCityManager.getInstance());

                    // Start by supplying the barracks
                    slc.setupSupplyLines();

                    // Examine all troops based on their location
                    final Map<Sector, BigInteger> lstBatt = BattalionManager.getInstance().countBattalions(getParent().getGameEngine().getGame(), nation, 1, false);
                    for (final Map.Entry<Sector, BigInteger> entry : lstBatt.entrySet()) {
                        // Check if this sector can be properly supplied or not.
                        final Sector thisSector = entry.getKey();
                        boolean inSupply;

                        // Exclude supply check for units in any home nation (of the same nation as the unit)
                        // controlled (by the owner of the unit) sectors.
                        inSupply = (thisSector.getPosition().getRegion().getId() == EUROPE && thisSector.getNation().getId() == nation.getId() && getSphere(thisSector, nation) == 1)
                                || slc.checkSupply(thisSector);

                        // calculate attrition due to starvation
                        int factor = 4;

                        // Check if this sector is affected by one of the winter effects
                        if (thisSector.getPosition().getRegion().getId() == EUROPE
                                && ((hasArctic && thisSector.getPosition().getY() <= 10)
                                || (hasCentral && thisSector.getPosition().getY() >= 11 && thisSector.getPosition().getY() <= 35)
                                || (hasMediterranean && thisSector.getPosition().getY() > 35))) {
                            factor *= 2;
                        }

                        int countNotSupplied = 0;

                        // Update supply status of battalions
                        final List<Battalion> lstBattalions = BattalionManager.getInstance().listByGamePosition(thisSector.getPosition(), nation);
                        for (final Battalion thisBattalion : lstBattalions) {
                            boolean thisBattSupply = inSupply;

                            // if the battalion is loaded on a ship then it is supplied directly from the warehouse
                            if (thisBattalion.getCarrierInfo() != null) {
                                if (thisBattalion.getCarrierInfo().getCarrierType() == ArmyConstants.SHIP) {
                                    thisBattSupply = true;
                                }
                            }

                            thisBattalion.setNotSupplied(!thisBattSupply);
                            BattalionManager.getInstance().update(thisBattalion);

                            if (!thisBattSupply) {
                                countNotSupplied++;
                                final int lostSoldiers = (int) (thisBattalion.getHeadcount() * factor / 100d);
                                thisBattalion.setHeadcount(thisBattalion.getHeadcount() - lostSoldiers);

                                if (thisBattalion.getHeadcount() < 0) {
                                    thisBattalion.setHeadcount(0);
                                }
                            }
                        }

                        // spread the news
                        if (countNotSupplied > 0) {
                            LOGGER.info("Battalions positioned at [" + thisSector.getPosition() + "] owned by " + nation.getName() + " are out of supply.");
                            newsSingle(nation, NewsConstants.NEWS_ECONOMY, "Our battalions at " + thisSector.getPosition() + " are not reachable by our supply lines.");
                        }
                    }

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

        LOGGER.info("SupplyLinesEvent processed.");
    }

}
