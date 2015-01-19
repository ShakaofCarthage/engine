package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.comparators.BrigadePosition;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for processing the maintenance costs of armies.
 */
public class ArmyMaintenance
        extends AbstractMaintenance {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(ArmyMaintenance.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public ArmyMaintenance(final GameEngine caller) {
        super(caller);

        LOGGER.debug("ArmyMaintenance instantiated.");
    }

    /**
     * Soldiers' pay.
     * Battalions that are not supplied will lose 5 - 15% of their soldiers due to desertion.
     */
    @SuppressWarnings("unchecked")
    public void maintainBrigades() {
        // array to store total marines per nation, region for wine consumption.
        final int[][] totSoldiers = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[] starvingSoldiers = new int[NATION_LAST + 1];
        final int[][] desertedSoldiers = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[] unpaidBattalions = new int[NATION_LAST + 1];
        final int[] unpaidSoldiers = new int[NATION_LAST + 1];
        final int[] totCostMoney = new int[NATION_LAST + 1];
        final int[][] totCostFood = new int[NATION_LAST + 1][REGION_LAST + 1];
        final int[] totBattalions = new int[NATION_LAST + 1];
        final String[] strRegion = new String[REGION_LAST + 1];

        final Map<String, Integer>[] totTroops = new Map[NATION_LAST + 1];
        for (int i = 0; i < NATION_LAST + 1; i++) {
            totTroops[i] = new HashMap<String, Integer>(); // NOPMD
        }

        // Double Costs custom game option
        final int doubleCosts = getGame().isDoubleCostsArmy() ? 2 : 1;

        // Retrieve brigades
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());

        final List<Region> lstRegion = RegionManager.getInstance().list();
        for (final Region region : lstRegion) {
            strRegion[region.getId()] = region.getName();
        }

        // Pay maintenance costs for all brigades
        // in Europe paying first the brigades that are closer to a trade city
        // and randomly in the colonies
        final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByGame(getGame());
        final Brigade[] sortedList = new Brigade[lstBrigades.size()];
        lstBrigades.toArray(sortedList);
        Arrays.sort(sortedList, new BrigadePosition(getGame(), getRandomGen()));

        // Random Event: Corrupted Military
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        final int corruptedMilitary = retrieveReportAsInt(free, getGame().getTurn(), RE_CORR);

        // Pay maintenance costs for all brigades
        for (final Brigade thisBrigade : sortedList) {
            final int nationId = thisBrigade.getNation().getId();
            final int regionId = thisBrigade.getPosition().getRegion().getId();
            // pay maintenance costs for all battalions
            for (final Battalion thisBattalion : thisBrigade.getBattalions()) {
                // Determine the maximum headcount
                int headcount = 800;
                if (thisBattalion.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                        || thisBattalion.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                        || thisBattalion.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                    headcount = 1000;
                }

                // Calculate maintenance cost
                int cost = thisBattalion.getType().getMaint()
                        * doubleCosts
                        * thisBattalion.getHeadcount() / headcount; // NOPMD

                // Random Event: Corrupted Economy
                if (nationId == corruptedMilitary) {
                    cost *= 1.25d;
                }

                // reduce money
                if (getTotGoods(nationId, EUROPE, GOOD_MONEY) >= cost) {

                    // Update totals
                    decTotGoods(nationId, EUROPE, GOOD_MONEY, cost);
                    totCostMoney[nationId] += cost;

                } else {
                    // soldiers desert
                    unpaidBattalions[nationId]++;

                    // Battalions that are not paid will lose 5 - 15% of their soldiers.
                    int modifier = 5;
                    int range = 11;
                    // Special (reduced) attrition in case of lack of food for French & Russian troops is negated, section 2.10.1
                    if (thisBrigade.getNation().getId() == NATION_FRANCE
                            || thisBrigade.getNation().getId() == NATION_RUSSIA) {
                        modifier = 1;
                        range = 5;

                    } else if (thisBrigade.getPosition().getRegion().getId() != EUROPE) {
                        // Battalions are not supplied in the colonies, they will lose 10-20%.
                        modifier = 10;
                    }
                    final int roll = getRandomGen().nextInt(range) + modifier;
                    final int lostSoldiers = (int) (thisBattalion.getHeadcount() * roll / 100d);

                    // remove soldiers
                    thisBattalion.setHeadcount(thisBattalion.getHeadcount() - lostSoldiers);
                    if (thisBattalion.getHeadcount() < 0) {
                        thisBattalion.setHeadcount(0);
                    }

                    // Report unpaid battalion
                    unpaidSoldiers[nationId] += lostSoldiers;
                    reportUnpaidBattalion(thisBrigade, thisBattalion, unpaidBattalions[nationId], lostSoldiers);

                    BattalionManager.getInstance().update(thisBattalion);

                    LOGGER.info("Unpaid Battalion [Owner=" + thisBrigade.getNation().getName()
                            + "/Brigade=" + thisBrigade.getBrigadeId()
                            + "/Battalion=" + thisBattalion.getId()
                            + "]");
                }

                // increase total number of soldiers for this nation, region
                totSoldiers[nationId][regionId] += thisBattalion.getHeadcount();

                // update statistics
                totBattalions[nationId]++;

                // report total soldiers per category
                if (!totTroops[nationId].containsKey(thisBattalion.getType().getType())) {
                    totTroops[nationId].put(thisBattalion.getType().getType(), 0);
                }

                final int curStat = totTroops[nationId].get(thisBattalion.getType().getType());
                totTroops[nationId].put(thisBattalion.getType().getType(), curStat + thisBattalion.getHeadcount());
            }
        }

        // Feed all brigades by nation and region
        final List<Nation> lstNations = getLstNations();
        lstNations.remove(0);
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            for (final Region regionObj : lstRegion) {
                final int region = regionObj.getId();
                // calculated needed food
                final int neededFood = (int) Math.ceil(((double) totSoldiers[nation][region]) / FOOD_RATE_SOL);
                if (getTotGoods(nation, region, GOOD_FOOD) >= neededFood) {
                    // Update totals
                    decTotGoods(nation, region, GOOD_FOOD, neededFood);
                    totCostFood[nation][region] += neededFood;

                } else {

                    // The nation needs to pay money to replace food
                    final int notFound = neededFood - getTotGoods(nation, region, GOOD_FOOD);
                    int starving = (int) (notFound * FOOD_RATE_SOL);
                    totCostFood[nation][region] += getTotGoods(nation, region, GOOD_FOOD);
                    setTotGoods(nation, region, GOOD_FOOD, 0);
                    starvingSoldiers[nation] += starving;

                    // Retrieve battalions for this nation and region to reduce headcount due to missing food
                    final List<Brigade> lstStarving = BrigadeManager.getInstance().listGameNationRegion(getGame(),
                            NationManager.getInstance().getByID(nation),
                            RegionManager.getInstance().getByID(region));

                    // Pay maintenance costs for all brigades
                    // in Europe paying first the brigades that are closer to a trade city
                    // and randomly in the colonies
                    final Brigade[] sortedStarvingList = new Brigade[lstStarving.size()];
                    lstStarving.toArray(sortedStarvingList);
                    if (!lstStarving.isEmpty()) {
                        Arrays.sort(sortedStarvingList, new BrigadePosition(getGame(), getRandomGen()));
                    }

                    // calculate attrition due to starvation
                    final int randomDeduction = calcStarvationAttrition(nation, region);

                    for (final Brigade thisBrigade : sortedStarvingList) {
                        if (starving > 0) {
                            // reduce headcount for all battalions
                            for (final Battalion thisBattalion : thisBrigade.getBattalions()) {
                                if (starving > 0) {
                                    final int lostSoldiers = (int) (thisBattalion.getHeadcount() * randomDeduction / 100d);

                                    // reduce starving troops
                                    starving -= thisBattalion.getHeadcount();

                                    thisBattalion.setHeadcount(thisBattalion.getHeadcount() - lostSoldiers);

                                    if (thisBattalion.getHeadcount() < 0) {
                                        thisBattalion.setHeadcount(0);
                                    }

                                    thisBattalion.setNotSupplied(true);

                                    BattalionManager.getInstance().update(thisBattalion);

                                    desertedSoldiers[nation][region] += lostSoldiers;

                                    // decrease total number of soldiers for this nation, region
                                    totSoldiers[nation][region] -= lostSoldiers;

                                    // report total soldiers per category
                                    if (!totTroops[nation].containsKey(thisBattalion.getType().getType())) {
                                        totTroops[nation].put(thisBattalion.getType().getType(), 0);
                                    }

                                    final int curStat = totTroops[nation].get(thisBattalion.getType().getType());
                                    totTroops[nation].put(thisBattalion.getType().getType(), curStat - lostSoldiers);

                                    LOGGER.info("Starving Battalion [Owner=" + thisBrigade.getNation().getName()
                                            + "/Brigade=" + thisBrigade.getBrigadeId()
                                            + "/Battalion=" + thisBattalion.getId()
                                            + "] missing food=" + notFound + " lost soldiers=" + lostSoldiers);
                                }
                            }
                        }
                    }
                }
            }
        }

        theTrans.commit();

        // store data
        saveData();

        // Report maintenance costs
        theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            if (unpaidSoldiers[nation] > 0) {
                // The brigade was not maintained properly
                newsSingle(nationObj, NEWS_ECONOMY, "We did not have enough money to pay the salaries of all the soldiers. " + unpaidSoldiers[nation] + " soldiers abandoned their duties.");
            }

            for (final Region regionObj : lstRegion) {
                final int region = regionObj.getId();
                if (desertedSoldiers[nation][region] > 0) {
                    // The brigade was not feed properly
                    newsSingle(nationObj, NEWS_ECONOMY, "We did not supply our battalions in " + strRegion[region] + " with food this month. " + desertedSoldiers[nation][region] + " soldiers starved to death.");
                }
            }

            reportArmyMaintenance(nationObj,
                    totSoldiers[nation],
                    totCostMoney[nation],
                    totCostFood[nation],
                    starvingSoldiers[nation],
                    desertedSoldiers[nation],
                    unpaidBattalions[nation],
                    totBattalions[nation],
                    totTroops[nation]);
        }
        theTrans.commit();

        LOGGER.info("ArmyMaintenance completed.");
    }

    /**
     * Calculate random attrition factor to not supplied troops.
     *
     * @param nation the owner of the battalions.
     * @param region the region of the battalions.
     * @return the loss in percentage.
     */
    private int calcStarvationAttrition(final int nation, final int region) {
        // Calculate attrition rate for not supplied battalions
        int randomDeduction;
        if (region == EUROPE) {
            // Battalions that are not supplied will lose 5 - 15% of their soldiers due to desertion and hunger.
            randomDeduction = getRandomGen().nextInt(11) + 5;

            if (nation == NATION_FRANCE || nation == NATION_RUSSIA) {
                randomDeduction = getRandomGen().nextInt(5) + 1;
            }

        } else {
            // If Battalions are not supplied in the colonies, they will lose 10-20% of their soldiers.
            randomDeduction = getRandomGen().nextInt(11) + 10;
        }
        return randomDeduction;
    }

    /**
     * Report the battalion that was unpaid.
     *
     * @param thisBrigade      the Brigade that lost the battalion.
     * @param thisBattalion    the Battalion to be lost.
     * @param lostBattalionCnt the counter of the position in the list of unpaid battalions.
     * @param lostSoldiers     the number of soldiers lost.
     */
    private void reportUnpaidBattalion(final Brigade thisBrigade,
                                       final Battalion thisBattalion,
                                       final int lostBattalionCnt,
                                       final int lostSoldiers) {
        report(thisBrigade.getNation(), A_TOT_BAT_UNPD + lostBattalionCnt + A_BRIGADE, thisBrigade.getName());
        report(thisBrigade.getNation(), A_TOT_BAT_UNPD + lostBattalionCnt + A_TYPE, thisBattalion.getType().getId());
        report(thisBrigade.getNation(), A_TOT_BAT_UNPD + lostBattalionCnt + A_UNPAID, lostSoldiers);
    }

    /**
     * Report army maintenance costs.
     *
     * @param thisNation       the Nation.
     * @param soldiers         the total number of soldiers per region.
     * @param money            the money paid.
     * @param food             the food paid.
     * @param starvingSoldiers the total number of starving soldiers.
     * @param deserted         the number that deserted.*
     * @param unpaidSoldiers   the total number of unpaid battalions.
     * @param totBattalions    the total number of battalions.
     * @param totTroops        the total number of solders per category.
     */
    private void reportArmyMaintenance(final Nation thisNation,
                                       final int[] soldiers,
                                       final int money,
                                       final int[] food,
                                       final int starvingSoldiers,
                                       final int[] deserted,
                                       final int unpaidSoldiers,
                                       final int totBattalions,
                                       final Map<String, Integer> totTroops) {
        report(thisNation, A_TOT_SOLDIERS, (soldiers[EUROPE] + soldiers[CARIBBEAN] + soldiers[INDIES] + soldiers[AFRICA]));

        int totDeserted = 0;


        for (final Region regionObj : getLstRegions()) {
            final int region = regionObj.getId();
            totDeserted += deserted[region];
            report(thisNation, A_TOT_SLDR_REG + region, soldiers[region]);
            report(thisNation, A_TOT_FOOD + region, food[region]);
        }

        report(thisNation, A_TOT_MONEY, money);
        report(thisNation, A_TOT_STARV_QTE, starvingSoldiers);
        report(thisNation, A_TOT_STARV_DES, totDeserted);
        report(thisNation, A_TOT_UNPAID, unpaidSoldiers);
        report(thisNation, A_TOT_BAT, totBattalions);

        for (final Map.Entry<String, Integer> entry : totTroops.entrySet()) {
            report(thisNation, A_TOT_SLDR_TPE + entry.getKey(), entry.getValue());

            // keep track of maximum battalions built ever
            if (entry.getKey().equals("In") || entry.getKey().equals("Kt") || entry.getKey().equals("Co")) {
                maxProfile(getGame(), thisNation, ProfileConstants.FORCE_INFANTRY, entry.getValue());

            } else if (entry.getKey().equals("Ca") || entry.getKey().equals("CC") || entry.getKey().equals("MC")) {
                maxProfile(getGame(), thisNation, ProfileConstants.FORCE_CAVALRY, entry.getValue());

            } else {
                maxProfile(getGame(), thisNation, ProfileConstants.FORCE_ARTILLERY, entry.getValue());
            }
        }

        // keep track of maximum battalions built ever
        maxProfile(getGame(), thisNation, ProfileConstants.BATT_HIGHEST, totBattalions);
    }

}
