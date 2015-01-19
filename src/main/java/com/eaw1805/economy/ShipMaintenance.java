package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.comparators.ShipPosition;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.List;

/**
 * Responsible for processing the maintenance costs of ships.
 */
public class ShipMaintenance extends AbstractMaintenance {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(ShipMaintenance.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public ShipMaintenance(final GameEngine caller) {
        super(caller);

        LOGGER.debug("ShipMaintenance instantiated.");
    }

    /**
     * Maintenance Costs for Ships, Wine consumption.
     */
    public void maintainShips() {
        // array to store total marines per nation, region for wine consumption.
        final int[][] totMarines = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[] soberMarines = new int[NATION_LAST + 1];
        final int[] unpaidShips = new int[NATION_LAST + 1];
        final int[] totCostMoney = new int[NATION_LAST + 1];
        final int[][] totCostWine = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[] totShips = new int[NATION_LAST + 1];
        final int[] totCannons = new int[NATION_LAST + 1];
        final int[][] totShipsPerClass = new int[NATION_LAST + 1][6];

        // Double Costs custom game option
        final int doubleCosts = getGame().isDoubleCostsNavy() ? 2 : 1;

        // Retrieve ships
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Ship> lstShips = ShipManager.getInstance().listByGame(getGame());

        // Pay maintenance costs for all ships
        // in Europe paying first the ships that are closer to a trade city
        // and randomly in the colonies
        final Ship[] sortedList = new Ship[lstShips.size()];
        lstShips.toArray(sortedList);
        Arrays.sort(sortedList, new ShipPosition(getGame(), getRandomGen()));

        // Pay maintenance costs for all ships
        for (final Ship thisShip : sortedList) {
            final int nationId = thisShip.getNation().getId();
            final int regionId = thisShip.getPosition().getRegion().getId();

            // keep track of ship statistics
            totShipsPerClass[nationId][thisShip.getType().getShipClass()]++;

            final int cost = thisShip.getType().getMaintenance() * doubleCosts;

            // reduce money
            if (getTotGoods(nationId, EUROPE, GOOD_MONEY) >= cost) {
                // Update totals
                decTotGoods(nationId, EUROPE, GOOD_MONEY, cost);
                totCostMoney[nationId] += cost;

            } else {
                // The ship was not maintained properly
                // When ships maintenance is not paid, there a 5-15% chance of them deserting.
                final int roll = getRandomGen().nextInt(100) + 1;
                if (roll <= 15) {
                    // it is lost
                    newsSingle(thisShip.getNation(), NEWS_ECONOMY, "We did not have enough money to pay the monthly salaries for the sailors of " + thisShip.getName() + ". The sailors declared mutiny and took over our ship.");

                    // Increase counter of unpaid ships
                    unpaidShips[nationId]++;

                    // Report unpaid ship
                    reportUnpaidShip(thisShip, unpaidShips[nationId]);

                    LOGGER.info("Unpaid Ship [Ship=" + thisShip.getShipId() + "] deserting");

                    // remove ship
                    ShipManager.getInstance().delete(thisShip);

                    continue;

                } else {
                    // it is lost
                    newsSingle(thisShip.getNation(), NEWS_ECONOMY, "We did not have enough money to pay the monthly salaries for the sailors of " + thisShip.getName() + ". Our sailors decided to remain loyal to your commands.");

                    // Increase counter of unpaid ships
                    unpaidShips[nationId]++;

                    // Report unpaid ship
                    reportUnpaidShip(thisShip, unpaidShips[nationId]);

                    LOGGER.info("Unpaid Ship [Ship=" + thisShip.getShipId() + "] remain loyal");
                }
            }

            // you also need to pay for the marines
            // increase total number of marines for this nation, region
            totMarines[nationId][regionId] += thisShip.getMarines();

            // update statistics
            totShips[nationId]++;

            // report total cannons
            if (thisShip.getType().getShipClass() > 0) {
                // this is a war ship
                if (thisShip.getType().getTypeId() == 13) {
                    // east india men
                    totCannons[nationId] += 28;

                } else {
                    // all other war ships
                    final String cannonDescr = thisShip.getType().getName().substring(0, thisShip.getType().getName().indexOf(" "));
                    totCannons[nationId] += Integer.parseInt(cannonDescr);
                }


            }

            // Required wine
            final int neededWine = (int) Math.ceil(((double) totMarines[nationId][regionId]) / WINE_RATE);

            // Simple check if enough wine is available
            if (neededWine > getTotGoods(nationId, EUROPE, GOOD_WINE) + getTotGoods(nationId, CARIBBEAN, GOOD_WINE)
                    + getTotGoods(nationId, INDIES, GOOD_WINE) + getTotGoods(nationId, AFRICA, GOOD_WINE)) {
                // Probably this ship will not be supplied with wine
                thisShip.setNoWine(true);

            } else {
                thisShip.setNoWine(false);
            }

            ShipManager.getInstance().update(thisShip);
        }

        // Pay for wine for all marines
        final List<Nation> lstNations = getLstNations();
        lstNations.remove(0);
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            // iterate through the regions
            for (final Region regionObj : getLstRegions()) {
                final int region = regionObj.getId();
                if (totMarines[nation][region] > 0) {
                    // Required wine
                    final int neededWine = (int) Math.ceil(((double) totMarines[nation][region]) / WINE_RATE);

                    if (getTotGoods(nation, region, GOOD_WINE) >= neededWine) {
                        // Update totals
                        decTotGoods(nation, region, GOOD_WINE, neededWine);
                        totCostWine[nation][region] += neededWine;

                    } else {
                        // Update totals
                        totCostWine[nation][region] += getTotGoods(nation, region, GOOD_WINE);
                        int remainingAmount = neededWine - getTotGoods(nation, region, GOOD_WINE);
                        setTotGoods(nation, region, GOOD_WINE, 0);

                        // when drawing supply from a different warehouse, the wine consumption is increased by 25%.
                        remainingAmount *= 1.25d;

                        // try to provide wine from other warehouses
                        boolean foundWine = false;
                        for (int otherRegion = REGION_FIRST; otherRegion <= REGION_LAST; otherRegion++) {
                            if (getTotGoods(nation, otherRegion, GOOD_WINE) >= remainingAmount) {
                                // Update totals
                                decTotGoods(nation, otherRegion, GOOD_WINE, remainingAmount);
                                totCostWine[nation][otherRegion] += remainingAmount;
                                foundWine = true;
                                break;

                            } else if (getTotGoods(nation, otherRegion, GOOD_WINE) > 0) {
                                // Update totals
                                totCostWine[nation][otherRegion] += getTotGoods(nation, otherRegion, GOOD_WINE);
                                remainingAmount -= getTotGoods(nation, otherRegion, GOOD_WINE);
                                setTotGoods(nation, otherRegion, GOOD_WINE, 0);
                            }
                        }

                        if (!foundWine) {
                            // The nation needs to pay money to replace wine
                            soberMarines[nation] += remainingAmount;
                        }
                    }
                }
            }
        }

        // Pay sober marines (wine not found)
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            // Pay sober marines
            if (soberMarines[nation] > 0) {
                // deduct money
                decTotGoods(nation, EUROPE, GOOD_MONEY, soberMarines[nation] * STARVING_COST);
                if (getTotGoods(nation, EUROPE, GOOD_MONEY) < 0) {
                    setTotGoods(nation, EUROPE, GOOD_MONEY, 0);
                }

                // update costs
                totCostMoney[nation] += soberMarines[nation] * STARVING_COST;

                final Nation thisNation = NationManager.getInstance().getByID(nation);
                newsSingle(thisNation, NEWS_ECONOMY, "We did not supply our sailors with wine and " + (soberMarines[nation] * (int) WINE_RATE) + " marines remained sober. We had to pay them " + (soberMarines[nation] * STARVING_COST) + " money to keep them calm.");

                // Report sober marines
                reportSoberMarines(nationObj, soberMarines[nation] * (int) WINE_RATE, soberMarines[nation] * STARVING_COST);
            }
        }

        theTrans.commit();

        // store data
        saveData();

        // Report maintenance costs
        theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            reportShipMaintenance(nationObj,
                    totMarines[nation][EUROPE] + totMarines[nation][CARIBBEAN] + totMarines[nation][INDIES] + totMarines[nation][AFRICA],
                    totCostMoney[nation],
                    totCostWine[nation],
                    unpaidShips[nation],
                    totShips[nation],
                    totCannons[nation],
                    totShipsPerClass[nation]);
        }
        theTrans.commit();

        LOGGER.info("ShipMaintenance completed.");
    }

    /**
     * Report the ship that was unpaid.
     *
     * @param thisShip    the Ship to be lost.
     * @param lostShipCnt the counter of the position in the list of unpaid ships.
     */
    private void reportUnpaidShip(final Ship thisShip, final int lostShipCnt) {
        report(thisShip.getNation(), S_TOT_UNPD_REP + lostShipCnt + S_TOT_UNPD_NAME, thisShip.getName());
        report(thisShip.getNation(), S_TOT_UNPD_REP + lostShipCnt + S_TOT_UNPD_TPE, thisShip.getType().getTypeId());
    }

    /**
     * Report sober marines.
     *
     * @param thisNation   the Nation with sober marines.
     * @param soberMarines the total number of sober marines.
     * @param payment      the money paid to compensate.
     */
    private void reportSoberMarines(final Nation thisNation,
                                    final int soberMarines,
                                    final int payment) {
        report(thisNation, S_SOBMRNS_QTE, soberMarines);
        report(thisNation, S_SOBMRNS_COST, payment);
    }

    /**
     * Report fleet maintenance costs.
     *
     * @param thisNation  the Nation.
     * @param marines     the total number of marines.
     * @param money       the money paid.
     * @param wine        the wine paid.
     * @param unpaidShips the total number of unpaid ships.
     * @param totShips    the total number of ships.
     * @param totCannons  the total number of cannons of the ships.
     */
    private void reportShipMaintenance(final Nation thisNation,
                                       final int marines,
                                       final int money,
                                       final int[] wine,
                                       final int unpaidShips,
                                       final int totShips,
                                       final int totCannons,
                                       final int[] totShipsPerClass) {
        report(thisNation, S_TOT_MARINES, marines);
        report(thisNation, S_TOT_MONEY, money);
        report(thisNation, S_TOT_UNPD, unpaidShips);
        report(thisNation, S_TOT_SHIPS, totShips);
        report(thisNation, S_TOT_CANNONS, totCannons);

        for (int region = REGION_FIRST; region <= REGION_LAST; region++) {
            report(thisNation, S_TOT_WINE + region, wine[region]);
        }

        for (int shipClass = 1; shipClass < 6; shipClass++) {
            maxProfile(getGame(), thisNation, ProfileConstants.FORCE_SHIP + shipClass, totShipsPerClass[shipClass]);
        }
    }

}
