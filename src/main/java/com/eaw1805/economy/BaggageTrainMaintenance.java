package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.comparators.BaggageTrainPosition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.List;

/**
 * Responsible for processing the maintenance costs of baggage trains.
 */
public class BaggageTrainMaintenance
        extends AbstractMaintenance {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(CommanderMaintenance.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public BaggageTrainMaintenance(final GameEngine caller) {
        super(caller);
        LOGGER.debug("BaggageTrainMaintenance instantiated.");
    }

    /**
     * Commanders' pay.
     */
    public void maintainBaggageTrains() {
        // array to store total marines per nation, region for wine consumption.
        final int[] unpaidBaggageTrains = new int[NATION_LAST + 1];
        final int[] totBaggageTrains = new int[NATION_LAST + 1];
        final int[] totCostMoney = new int[NATION_LAST + 1];

        // Retrieve baggage trains
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<BaggageTrain> lstTrains = BaggageTrainManager.getInstance().listByGame(getGame());

        // Pay maintenance costs for all baggage trains
        // in Europe paying first the baggage trains that are closer to a trade city
        // and randomly in the colonies
        final BaggageTrain[] sortedList = new BaggageTrain[lstTrains.size()];
        lstTrains.toArray(sortedList);

        // Sort baggage trains first by region then by rank
        Arrays.sort(sortedList, new BaggageTrainPosition(getGame(), getRandomGen()));

        // Pay maintenance costs for all baggage trains
        int nationId;
        for (final BaggageTrain thisTrain : lstTrains) {
            nationId = thisTrain.getNation().getId();

            if (thisTrain.getCondition() < 0) {
                // Remove destroyed baggage trains.
                newsSingle(thisTrain.getNation(), NEWS_ECONOMY, "Baggage train " + thisTrain.getName() + " was destroyed.");

                BaggageTrainManager.getInstance().delete(thisTrain);
                continue;
            }

            // reduce money
            if (getTotGoods(nationId, EUROPE, GOOD_MONEY) >= 15000) {
                // maintenance costs can be paid
                // Update totals
                decTotGoods(nationId, EUROPE, GOOD_MONEY, 15000);
                totCostMoney[nationId] += 15000;

                // update statistics
                totBaggageTrains[nationId]++;

            } else {
                // The train was not maintained properly
                unpaidBaggageTrains[nationId]++;

                // Report unpaid train
                reportUnpaidBaggageTrain(thisTrain, unpaidBaggageTrains[nationId]);

                LOGGER.info("Unpaid Baggage Train [BTrain=" + thisTrain.getBaggageTrainId() + "]");

                newsSingle(thisTrain.getNation(), NEWS_ECONOMY, "We did not have enough money to pay the monthly maintenance costs of " + thisTrain.getName() + " baggage train.");

                // remove baggage train
                BaggageTrainManager.getInstance().delete(thisTrain);
            }
        }

        theTrans.commit();

        // store data
        saveData();

        // Report maintenance costs
        theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Nation> lstNations = getLstNations();
        lstNations.remove(0);
        for (Nation nationObj : lstNations) {
            final int nation = nationObj.getId();
            reportBaggageTrainMaintenance(nationObj,
                    totCostMoney[nation],
                    unpaidBaggageTrains[nation],
                    totBaggageTrains[nation]);
        }
        theTrans.commit();

        LOGGER.info("BaggageTrainMaintenance completed.");
    }

    /**
     * Report the baggage train that was unpaid.
     *
     * @param thisTrain    the Brigade that lost the commanders.
     * @param lostTrainCnt the counter of the position in the list of unpaid trains.
     */
    private void reportUnpaidBaggageTrain(final BaggageTrain thisTrain,
                                          final int lostTrainCnt) {
        report(thisTrain.getNation(), "baggagetrain.unpaid." + lostTrainCnt + ".name", thisTrain.getName());
    }

    /**
     * Report baggage train maintenance costs.
     *
     * @param thisNation   the Nation.
     * @param money        the money paid.
     * @param unpaidTrains the total number of unpaid commanders.
     * @param totTrains    the total number of commanders.
     */
    private void reportBaggageTrainMaintenance(final Nation thisNation,
                                               final int money,
                                               final int unpaidTrains,
                                               final int totTrains) {
        report(thisNation, "baggagetrain.totalMoney", money);
        report(thisNation, "baggagetrain.unpaidTrains", unpaidTrains);
        report(thisNation, "baggagetrain.totTrains", totTrains);
    }

}
