package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.map.comparators.SectorComparator;
import com.eaw1805.economy.production.AbstractProductionSite;
import com.eaw1805.economy.production.ProductionSiteFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for processing sectors and advancing the population.
 */
public class SectorMaintenance
        extends AbstractMaintenance
        implements ProductionSiteConstants, ReportConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SectorMaintenance.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public SectorMaintenance(final GameEngine caller) {
        super(caller);
        LOGGER.debug("SectorMaintenance instantiated.");
    }

    /**
     * Taxation, Population Growth, Production Sites produce goods.
     */
    public void advanceSectors() {
        LOGGER.info("Advance Sectors: Taxation and Population Growth.");

        // array to store total marines per nation, region for wine consumption.
        final int[][] empSize = new int[NATION_LAST + 1][REGION_LAST + 1];
        final double[][] incLow = new double[NATION_LAST + 1][REGION_LAST + 1];
        final double[][] incHigh = new double[NATION_LAST + 1][REGION_LAST + 1];
        final int[][] totSectors = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[][] totPopulation = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[][] popType = new int[NATION_LAST + 1][3];
        final int[][] totPopIncrease = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[] totTaxation = new int[NATION_LAST + 1];

        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());

        // Calculate rates
        calculateIncRates(empSize, incLow, incHigh);

        // Retrieve taxation orders
        final int[] taxOrders = new int[NATION_LAST + 1];
        final int[] taxSpendGoods = new int[NATION_LAST + 1];
        final List<PlayerOrder> lstTaxOrders = PlayerOrderManager.getInstance().listTaxOrders(getGame(), getGame().getTurn());
        for (PlayerOrder taxOrder : lstTaxOrders) {
            taxOrders[taxOrder.getNation().getId()] = Integer.parseInt(taxOrder.getParameter1());

            if (taxOrder.getParameter2() != null && taxOrder.getParameter2().equals("1")) {
                taxSpendGoods[taxOrder.getNation().getId()] = 1;
                LOGGER.info("Nation [" + taxOrder.getNation().getName() + "] issued Spend Goods to improve Taxation");

            } else {
                LOGGER.info("Nation [" + taxOrder.getNation().getName() + "] issued Taxation Order [" + taxOrder.getParameter1() + "]");
            }
        }

        // Random Event: Booming Economy, Manpower shortage
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        final int boomingEconomy = retrieveReportAsInt(free, getGame().getTurn(), RE_BOOM);
        final int manpowerShortage = retrieveReportAsInt(free, getGame().getTurn(), RE_SHOR);
        final int nationalism = retrieveReportAsInt(free, getGame().getTurn(), RE_NATI);

        //Retrieve all the sectors for the specific game id.
        final List<Sector> lstSectors = SectorManager.getInstance().listByGame(getGame());
        for (final Sector thisSector : lstSectors) {
            final int regionId = thisSector.getPosition().getRegion().getId();
            final int nationId = thisSector.getNation().getId();

            // Skip free sectors
            if (nationId < 1) {
                continue;
            }

            // Sanity check
            if (incHigh[nationId][regionId] <= 0) {
                LOGGER.error("Negative incHigh[" + nationId + "][" + regionId + "] = " + incHigh[nationId][regionId] + " for SectorID " + thisSector.getId());
                incHigh[nationId][regionId] = 0.1;
            }

            if (incLow[nationId][regionId] <= 0) {
                LOGGER.error("Negative incLow[" + nationId + "][" + regionId + "] = " + incLow[nationId][regionId] + " for SectorID " + thisSector.getId());
                incLow[nationId][regionId] = 0;
            }

            // Sanity check
            if (incHigh[nationId][regionId] - incLow[nationId][regionId] <= 0) {
                LOGGER.error("Negative incHigh[" + nationId + "][" + regionId + "] - incLow[][] for SectorID " + thisSector.getId());
                LOGGER.error("         incHigh[" + nationId + "][" + regionId + "] = " + incHigh[nationId][regionId]);
                LOGGER.error("         incLow[" + nationId + "][" + regionId + "] = " + incLow[nationId][regionId]);
                incHigh[nationId][regionId] = 0.1;
                incLow[nationId][regionId] = 0;
            }

            // Determine rate of population growth
            double incRate = incLow[nationId][regionId] +
                    (getRandomGen().nextInt((int) ((incHigh[nationId][regionId] - incLow[nationId][regionId]) * 100d)) + 1)
                            / 100d;

            // Check type of sector (if not home, if not in political sphere)
            incRate *= calculatePopIncRate(thisSector);

            final int popCount = thisSector.populationCount();

            // keep track of population size per type
            if (thisSector.getPosition().getRegion().getId() == EUROPE) {
                // the population type: 0 - home region, 1 - sphere of influence, 2 - other.
                final int thisType = getSphere(thisSector, thisSector.getNation()) - 1;
                popType[nationId][thisType] += popCount;
            }

            // Calculate increase
            int increase = (int) (popCount * incRate / 100d);

            // Calculate taxation
            int taxes = thisSector.populationCount() * calculateTaxRate(thisSector);

            // Check if sector was conquered recently
            if (thisSector.getConqueredCounter() > 0) {
                taxes *= (thisSector.taxDecrease() / 100d);

                // reduce conquer counter
                thisSector.setConqueredCounter(thisSector.getConqueredCounter() - 1);
            }

            // Check if a complex production site is under construction
            if (thisSector.getBuildProgress() > 0) {
                // reduce construction counter
                thisSector.setBuildProgress(thisSector.getBuildProgress() - 1);

                // check if huge fortress is finally built
                if (thisSector.getBuildProgress() == 0) {
                    // We gain 15 VPs
                    changeVP(getGame(), thisSector.getNation(), BUILD_HUGE, "Built a huge fortress");

                    newsGlobal(thisSector.getNation(), NewsConstants.NEWS_MILITARY, thisSector.getProductionSite().getId(),
                            "We built a huge fortress at " + thisSector.getPosition().toString(),
                            thisSector.getNation().getName() + " built a huge fortress at " + thisSector.getPosition().toString());

                    // Modify player's profile
                    changeProfile(getGame(), thisSector.getNation(), ProfileConstants.FORTRESS_BUILT, 1);

                    // Update achievements
                    achievementsBuildHuge(getGame(), thisSector.getNation());
                }
            }

            // check taxation orders
            if (thisSector.getPosition().getRegion().getId() == EUROPE) {
                switch (taxOrders[nationId]) {
                    case OrderConstants.TAX_HARSH:
                        taxes *= 1.25d;
                        increase *= .5d;
                        break;

                    case OrderConstants.TAX_LOW:
                        taxes *= 0.5d;
                        increase *= 1.25d;
                        break;

                    default:
                        // Spend colonial goods
                        if (taxSpendGoods[nationId] == 1) {
                            taxes *= 1.2d;
                        }
                }
            }

            // Random Event: Booming Economy
            if (nationId == boomingEconomy) {
                taxes *= 1.3d;
            }

            // Custom game: Boosted Taxation (+25% coins)
            if (getGame().isBoostedTaxation()) {
                taxes *= 1.25d;
            }

            // Random Event: Manpower shortage
            if (nationId == manpowerShortage) {
                increase *= 0.7d;
            }

            // Random Event: Nationalism
            if (nationId == nationalism) {
                increase *= 1.3d;
            }

            // Custom game: Fast-growing population (+25% citizens arriving)
            if (getGame().isFastPopulationGrowth()) {
                increase *= 1.25d;
            }

            // Increase people at warehouse
            incTotGoods(nationId, regionId, GOOD_PEOPLE, increase);

            // Increase total taxation
            incTotGoods(nationId, EUROPE, GOOD_MONEY, taxes);

            // Keep track of statistics
            totSectors[nationId][regionId] += 1;
            totPopIncrease[nationId][regionId] += increase;
            totPopulation[nationId][regionId] += thisSector.populationCount();
            totTaxation[nationId] += taxes;
        }

        theTrans.commit();

        saveData();

        // Report advancement and warehouses
        final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final NumberFormat formatter = new DecimalFormat("#,###,###");
        final List<Nation> lstNations = getLstNations();
        if (getGame().getScenarioId() != HibernateUtil.DB_FREE) {
            if (lstNations.get(0).getId() == NATION_NEUTRAL) {
                lstNations.remove(0);
            }
        }

        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            reportAdvanceSector(nationObj,
                    totSectors[nation],
                    totPopulation[nation],
                    popType[nation],
                    totPopIncrease[nation],
                    totTaxation[nation]);

            // Produce news
            newsSingle(nationObj, NEWS_ECONOMY, formatter.format(totTaxation[nation]) + " money were collected from our citizens");
            newsSingle(nationObj, NEWS_ECONOMY, formatter.format(totPopIncrease[nation][EUROPE]) + " new citizens arrived at our empire at Europe");

            // Check if this is the Largest income per turn ever for the particular player
            maxProfile(getGame(), nationObj, ProfileConstants.INCOME_HIGHEST, totTaxation[nation]);
        }
        trans.commit();

        LOGGER.info("Advance Sectors completed.");
    }

    /**
     * Production Sites maintenance, and production of goods.
     */
    public void advanceProductionSites() {
        LOGGER.info("Advance Sectors: Taxation and Population Growth.");
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());

        final int[] maintenance = new int[NATION_LAST + 1];
        final ProductionSiteFactory prodFactory = new ProductionSiteFactory(this);

        //Retrieve all the sectors for the specific game id.
        final List<Sector> lstSectors = SectorManager.getInstance().listByGame(getGame());

        // First iteration -- first order production sites
        LOGGER.info("Production sites 1st pass: pay maintenance costs + produce 1st order goods");
        for (final Sector thisSector : lstSectors) {
            if (thisSector.getNation().getId() < 1) {
                continue;
            }

            // Process production site
            if (thisSector.getProductionSite() != null) {
                final int nationId = thisSector.getNation().getId();
                final int cost = thisSector.getProductionSite().getMaintenanceCost();
                if (getTotGoods(nationId, EUROPE, GOOD_MONEY) >= cost) {
                    maintenance[nationId] += cost;
                    decTotGoods(nationId, EUROPE, GOOD_MONEY, cost);
                    thisSector.setPayed(true);

                } else {
                    // Production site not properly maintained
                    thisSector.setPayed(false);

                    newsSingle(thisSector.getNation(), NEWS_ECONOMY, thisSector.getProductionSite().getId(), "Not enough money available at national treasury to maintain the " + thisSector.getProductionSite().getName() + " of " + thisSector.getPosition().toString() + ". The production site will not be operated until the salaries of the workers are payed.");

                    LOGGER.info("Sector [" + thisSector.getPosition().toString()
                            + "] owned by " + thisSector.getNation().getName() + " lvl=" + thisSector.getPopulation() + " NOT MAINTAINED");
                }
            }

            // Process production site
            if (thisSector.getProductionSite() != null && thisSector.getPayed()) {
                if ((thisSector.getProductionSite().getId() != PS_MILL)
                        && (thisSector.getProductionSite().getId() != PS_MINT)
                        && (thisSector.getProductionSite().getId() != PS_FACTORY)) {
                    final AbstractProductionSite prodSite = prodFactory.construct(thisSector); // NOPMD
                    prodSite.advance();
                }

            } else if (!thisSector.getPayed()) {
                // Unpaid production site
                LOGGER.info("Unpaid production site at " + thisSector.getPosition().toString());
            }
        }

        // Second iteration -- process mint & mill
        LOGGER.info("Production sites 2nd pass: produce 2nd order goods");
        for (final Sector thisSector : lstSectors) {
            // Process production site
            if (thisSector.getProductionSite() != null && thisSector.getPayed()) {
                if ((thisSector.getProductionSite().getId() == PS_MILL) || (thisSector.getProductionSite().getId() == PS_MINT)) {
                    final AbstractProductionSite prodSite = prodFactory.construct(thisSector); // NOPMD
                    prodSite.advance();
                }

            } else if (!thisSector.getPayed()) {
                // Unpaid production site
                LOGGER.info("Unpaid production site at " + thisSector.getPosition().toString());
            }
        }

        // Report random rolls for estates
        LOGGER.info("Estate RF [" + getStrRandomFactor().toString() + "]");

        // Third iteration -- process factories
        LOGGER.info("Production sites 2nd pass: produce 3rd order goods");
        for (final Sector thisSector : lstSectors) {
            // Process production site
            if (thisSector.getProductionSite() != null && thisSector.getPayed()) {
                if (thisSector.getProductionSite().getId() == PS_FACTORY) {
                    final AbstractProductionSite prodSite = prodFactory.construct(thisSector); // NOPMD
                    prodSite.advance();
                }

            } else if (!thisSector.getPayed()) {
                // Unpaid production site
                LOGGER.info("Unpaid production site at " + thisSector.getPosition().toString());
            }

            SectorManager.getInstance().update(thisSector);
        }

        theTrans.commit();

        // Warehouse statistics.
        saveData();

        // Report advancement and warehouses
        final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Nation> lstNations = getLstNations();
        if (lstNations.get(0).getId() == NATION_NEUTRAL) {
            lstNations.remove(0);
        }
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            reportProduction(nationObj,
                    getProdGoods(nation),
                    maintenance[nation]);
        }
        trans.commit();

        LOGGER.info("Production Sites advancement completed.");
    }

    /**
     * Food consumption,
     */
    public void maintainSectors() {
        LOGGER.info("Sector Maintenance started.");

        final int[][] totCostFood = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[][] starvingPop = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[][] reducedPop = new int[NATION_LAST + 1][REGION_LAST + 1];

        // Retrieve all the sectors for the specific game id.
        // Food consumption of Civilians -- in 3 phases (Home, Sphere, Outside) sorted by distance from nearest Trade City.
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Sector> lstSectors = SectorManager.getInstance().listByGame(getGame());

        // Pay maintenance costs for all sectors
        // in Europe paying first the sectors that are closer to a trade city
        // and randomly in the colonies
        final Sector[] sortedList = new Sector[lstSectors.size()];
        lstSectors.toArray(sortedList);
        java.util.Arrays.sort(sortedList, new SectorComparator(getGame(), getRandomGen()));

        // Initialize summary
        final Map<Nation, Set<Sector>> summaryNoEffect = new HashMap<Nation, Set<Sector>>();
        final Map<Nation, Set<Sector>> summaryReduced = new HashMap<Nation, Set<Sector>>();
        final List<Nation> lstNations = getLstNations();
        if (lstNations.get(0).getId() == NATION_NEUTRAL) {
            lstNations.remove(0);
        }
        for (final Nation nation : lstNations) {
            summaryNoEffect.put(nation, new HashSet<Sector>());
            summaryReduced.put(nation, new HashSet<Sector>());
        }

        for (final Sector thisSector : sortedList) {
            final int regionId = thisSector.getPosition().getRegion().getId();
            final int nationId = thisSector.getNation().getId();
            // Skip free sectors
            if (nationId < 1 || !summaryReduced.containsKey(thisSector.getNation())) {
                continue;
            }

            // calculate required food
            final int requiredFood = (int) (thisSector.populationCount() / FOOD_RATE_CIV);
            if (getTotGoods(nationId, regionId, GOOD_FOOD) >= requiredFood) {
                // Reduce food
                decTotGoods(nationId, regionId, GOOD_FOOD, requiredFood);
                totCostFood[nationId][regionId] += requiredFood;

            } else {
                // Could not provide food
                starvingPop[nationId][regionId]++;

                // Try to reduce sector
                if (thisSector.getPopulation() > 0) {
                    // There is a 50% chance that the sector will be reduced by 1 level
                    final int randomDeduction = getRandomGen().nextInt(100);
                    if (randomDeduction >= 50) {
                        reducedPop[nationId][regionId]++;
                        thisSector.setPopulation(thisSector.getPopulation() - 1);

                        // update summary
                        summaryReduced.get(thisSector.getNation()).add(thisSector);

                        LOGGER.info("Starving Sector [" + thisSector.getPosition().toString()
                                + "] owned by " + thisSector.getNation().getName() + " lvl=" + thisSector.getPopulation() + " rnd=" + randomDeduction + " -- population decreased by 1");

                    } else {
                        // update summary
                        summaryNoEffect.get(thisSector.getNation()).add(thisSector);

                        LOGGER.info("Starving Sector [" + thisSector.getPosition().toString()
                                + "] owned by " + thisSector.getNation().getName() + " lvl=" + thisSector.getPopulation() + " rnd=" + randomDeduction + " -- no population decrease");
                    }

                } else {
                    // update summary
                    summaryNoEffect.get(thisSector.getNation()).add(thisSector);

                    LOGGER.info("Starving Sector [" + thisSector.getPosition().toString()
                            + "] owned by " + thisSector.getNation().getName() + " lvl=" + thisSector.getPopulation() + " SKIP");
                }
            }

            SectorManager.getInstance().update(thisSector);
        }

        // Report effects
        for (final Nation nation : lstNations) {
            if (!summaryNoEffect.get(nation).isEmpty()) {
                final StringBuilder strBuilderNoEffect = new StringBuilder();
                if (summaryNoEffect.get(nation).size() == 1) {
                    strBuilderNoEffect.append("Famine hit sector ");

                } else {
                    strBuilderNoEffect.append("Famine hit sectors ");
                }

                for (final Sector sector : summaryNoEffect.get(nation)) {
                    strBuilderNoEffect.append(sector.getPosition().toString());
                    strBuilderNoEffect.append(", ");
                }

                final String sectorNamesNoEffect = strBuilderNoEffect.substring(0, strBuilderNoEffect.length() - 2);
                newsSingle(nation, NEWS_ECONOMY, sectorNamesNoEffect + ". Our people managed to recover without loses.");
            }

            if (!summaryReduced.get(nation).isEmpty()) {
                final StringBuilder strBuilderReduced = new StringBuilder();
                if (summaryReduced.get(nation).size() == 1) {
                    strBuilderReduced.append("Famine hit sector ");

                } else {
                    strBuilderReduced.append("Famine hit sectors ");
                }

                for (final Sector sector : summaryReduced.get(nation)) {
                    strBuilderReduced.append(sector.getPosition().toString());
                    strBuilderReduced.append(", ");
                }

                final String sectorNamesReduced = strBuilderReduced.substring(0, strBuilderReduced.length() - 2);
                newsSingle(nation, NEWS_ECONOMY, sectorNamesReduced + " and people starved to death.");
            }
        }

        theTrans.commit();

        // Warehouse statistics.
        saveData();

        // Report advancement and warehouses
        theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            reportMaintainSector(nationObj,
                    totCostFood[nation],
                    starvingPop[nation],
                    reducedPop[nation]);
        }
        theTrans.commit();
        LOGGER.info("SectorMaintenance completed.");
    }

    /**
     * Based on population sizes it calculates the rates for population increase.
     *
     * @param empSize the calculated population sizes per region.
     * @param incLow  the low value for the increase rate per region.
     * @param doubles the high value for the increase rate per region.
     */
    private void calculateIncRates(int[][] empSize, double[][] incLow, double[][] doubles) {
        // retrieve population size from previous turn
        int nationId;
        final List<Nation> lstNations = getLstNations();
        if (lstNations.get(0).getId() == NATION_NEUTRAL) {
            lstNations.remove(0);
        }
        for (final Nation thisNation : lstNations) {
            nationId = thisNation.getId();
            if (nationId < 0) {
                nationId = 0;
            }

            // Calculate Lows and Highs of population increase for this turn for Europe
            empSize[nationId][EUROPE] = retrieveReportAsInt(thisNation, getGame().getTurn() - 1, "population.size.region.1");
            if (empSize[nationId][EUROPE] < 1000000) {
                incLow[nationId][EUROPE] = 2.5d;
                doubles[nationId][EUROPE] = 4d;

            } else {
                final double limit = (empSize[nationId][EUROPE] - 1000000d) / 1500000d;

                incLow[nationId][EUROPE] = 2.5d - limit;
                doubles[nationId][EUROPE] = 4d - limit;

                // The calculated rate will not sink under 1.0% to 2% for Europe
                if (incLow[nationId][EUROPE] < 1d) {
                    incLow[nationId][EUROPE] = 1.0d;
                }
                if (doubles[nationId][EUROPE] < 2d) {
                    doubles[nationId][EUROPE] = 2d;
                }
            }

            // Colonies
            for (final Region regionObj : getLstRegions()) {
                final int regionId = regionObj.getId();
                empSize[nationId][regionId] = retrieveReportAsInt(thisNation, getGame().getTurn() - 1, "population.size.region." + regionId);

                // Colonies
                if (empSize[nationId][regionId] < 500000d) {

                    incLow[nationId][regionId] = 3.5d;
                    doubles[nationId][regionId] = 4.5d;

                } else {
                    final double limit = (empSize[nationId][regionId] - 500000d) / 1000000d;

                    incLow[nationId][regionId] = 3.5d - limit;
                    doubles[nationId][regionId] = 4.5d - limit;

                    // The calculated rate will not sink under 1.5% to 2.5% for Colonies
                    if (incLow[nationId][regionId] < 1.5d) {
                        incLow[nationId][regionId] = 1.5d;
                    }
                    if (doubles[nationId][regionId] < 2.5d) {
                        doubles[nationId][regionId] = 2.5d;
                    }
                }
            }
        }
    }

    /**
     * Identify taxation rate.
     *
     * @param sector the sector to examine.
     * @return the taxation rate.
     */
    private int calculateTaxRate(final Sector sector) {
        final int baseTax;
        final int sphere = getSphere(sector, sector.getNation());

        // Check if this is a colonial sector
        if (sector.getPosition().getRegion().getId() != EUROPE) {
            baseTax = 3;

        } else {
            // check sphere
            switch (sphere) {
                case 1:
                    baseTax = sector.getNation().getTaxRate();
                    break;

                case 2:
                    baseTax = 4;
                    break;

                case 3:
                default:
                    baseTax = 3;
                    break;
            }
        }

        return baseTax;
    }

    /**
     * Identify population increase rate based on Table p.18 / v0.7.
     * sphere of influence give 50%, outside sphere gives 20%.
     *
     * @param sector the sector to examine.
     * @return the population increase rate.
     */
    private double calculatePopIncRate(final Sector sector) {
        final double baseRate;
        final int sphere = getSphere(sector, sector.getNation());

        // check sphere
        switch (sphere) {
            // home + colonial
            case 1:
                baseRate = 1d;
                break;

            // sphere of influence
            case 2:
                baseRate = .5d;
                break;

            // foreign population
            case 3:
            default:
                baseRate = .2d;
                break;
        }

        return baseRate;
    }

    /**
     * Report advancement of sectors.
     *
     * @param thisNation the Nation.
     * @param sectors    the total number of sectors per region.
     * @param population the total population size per region.
     * @param popType    the total european population per type.
     * @param increase   the population increase per region.
     * @param taxation   the taxation collected.
     */
    private void reportAdvanceSector(final Nation thisNation,
                                     final int[] sectors,
                                     final int[] population,
                                     final int[] popType,
                                     final int[] increase,
                                     final int taxation) {
        // iterate through the regions
        for (final Region regionObj : getLstRegions()) {
            final int region = regionObj.getId();
            report(thisNation, E_SEC_SIZE + region, sectors[region]);
            report(thisNation, E_POP_SIZE + region, population[region]);
            report(thisNation, E_POP_INC + region, increase[region]);
        }

        for (int type = 0; type <= 2; type++) {
            report(thisNation, E_POP_TYPE + type, popType[type]);
        }

        report(thisNation, "taxation", taxation);
    }

    /**
     * Report advancement of sectors.
     *
     * @param thisNation  the Nation.
     * @param goods       the available resources per region.
     * @param maintenance the maintenance of the sites.
     */
    private void reportProduction(final Nation thisNation,
                                  final int[][] goods,
                                  final int maintenance) {
        // iterate through the regions
        for (final Region regionObj : getLstRegions()) {
            final int region = regionObj.getId();
            // report goods only for nations, not for unconquered areas
            if (thisNation.getId() > 0) {
                for (int good = GOOD_FIRST; good <= GOOD_LAST; good++) {
                    report(thisNation, "goods.region." + region + ".good." + good, goods[region][good]);
                }
            }
        }
        report(thisNation, "production.maintenance", maintenance);
    }

    /**
     * Report maintenance of sectors.
     *
     * @param thisNation the Nation.
     * @param foodCost   the total cost in food per region.
     * @param starving   the sectors that were not fed properly.
     * @param reduced    the sectors that were reduced.
     */
    private void reportMaintainSector(final Nation thisNation,
                                      final int[] foodCost,
                                      final int[] starving,
                                      final int[] reduced) {
        // iterate through the regions
        for (final Region regionObj : getLstRegions()) {
            final int region = regionObj.getId();
            report(thisNation, E_POP_FOOD + region, foodCost[region]);
            report(thisNation, E_POP_STRV + region, starving[region]);
            report(thisNation, E_POP_RDC + region, reduced[region]);
        }
    }


}
