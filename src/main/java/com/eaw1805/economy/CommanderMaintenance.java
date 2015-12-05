package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.comparators.CommanderRank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.List;

/**
 * Responsible for processing the maintenance costs of commanders.
 */
public class CommanderMaintenance
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
    public CommanderMaintenance(final GameEngine caller) {
        super(caller);
        LOGGER.debug("CommanderMaintenance instantiated.");
    }

    /**
     * Commanders' pay.
     */
    public void maintainCommanders() {
        // array to store total marines per nation, region for wine consumption.
        final int[] unpaidCommanders = new int[NATION_LAST + 1];
        final int[] totCommanders = new int[NATION_LAST + 1];
        final int[] totCostMoney = new int[NATION_LAST + 1];

        // Retrieve commanders
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Commander> lstCommander = CommanderManager.getInstance().listByGame(getGame());

        // Remove unknown commander
        lstCommander.remove(0);

        // Pay maintenance costs for all commanders
        // pay highest ranking commanders first
        final Commander[] sortedList = new Commander[lstCommander.size()];
        lstCommander.toArray(sortedList);

        // Sort commanders first by region then by rank
        Arrays.sort(sortedList, new CommanderRank());

        int nationId;
        for (final Commander thisCommander : sortedList) {
            if (thisCommander.getDead()) {
                // Ignore dead commanders.
                continue;
            }

            nationId = thisCommander.getNation().getId();

            // reduce money
            if (getTotGoods(nationId, EUROPE, GOOD_MONEY) >= thisCommander.getRank().getSalary()) {
                // maintenance costs can be paid
                // Update totals
                decTotGoods(nationId, EUROPE, GOOD_MONEY, thisCommander.getRank().getSalary());
                totCostMoney[nationId] += thisCommander.getRank().getSalary();

                // update statistics
                totCommanders[nationId]++;

                // If commander is sick, reduce counter
                if (thisCommander.getSick() > 0) {
                    final int counter = thisCommander.getSick() - 1;
                    thisCommander.setSick(counter);
                    if (counter == 0) {
                        // Commander recovered from illness
                        // All ratings are divided by 3 is out of action for 2 to 6 months.
                        thisCommander.setStrc(thisCommander.getStrc() * 3);
                        if (thisCommander.getStrc() < 1) {
                            thisCommander.setStrc(1);
                        }

                        thisCommander.setComc(thisCommander.getComc() * 3);
                        if (thisCommander.getComc() < 1) {
                            thisCommander.setComc(1);
                        }

                        LOGGER.info("SicknessRandomEvent concluded for " + thisCommander.getNation().getName() + " this turn.");

                        // Add news entry
                        newsSingle(thisCommander.getNation(), NEWS_MILITARY, "Our commander " + thisCommander.getName() + " managed to fully recover this month. He is now back to full duty in iron strength.");

                        CommanderManager.getInstance().update(thisCommander);
                    }
                }

            } else {
                // The commander was not maintained properly                
                unpaidCommanders[nationId]++;

                // Report unpaid battalion
                reportUnpaidCommander(thisCommander, unpaidCommanders[nationId]);

                // Commanders will desert in not paid with a chance of 50%
                final int roll = getRandomGen().nextInt(101) + 1;
                if (roll > 50) {
                    LOGGER.info("Unpaid Commander [Commander=" + thisCommander.getId() + "] abandoned his post");
                    newsSingle(thisCommander.getNation(), NEWS_ECONOMY, "We did not have enough money to pay the monthly salary of " + thisCommander.getName() + ". He abandoned his post.");

                    // remove commander from command
                    removeCommander(thisCommander);

                    // remove commander from game
                    thisCommander.setDead(true);

                } else {
                    LOGGER.info("Unpaid Commander [Commander=" + thisCommander.getId() + "] but remains");
                    newsSingle(thisCommander.getNation(), NEWS_ECONOMY, "We did not have enough money to pay the monthly salary of " + thisCommander.getName() + ". He remains loyal and ready to serve your commands.");
                }
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
            reportCommandersMaintenance(nationObj,
                    totCostMoney[nation],
                    unpaidCommanders[nation],
                    totCommanders[nation]);
        }
        theTrans.commit();

        LOGGER.info("CommanderMaintenance completed.");
    }

    /**
     * Report the commander that was unpaid.
     *
     * @param thisCommander    the Brigade that lost the commanders.\
     * @param lostCommanderCnt the counter of the position in the list of unpaid commanders.
     */
    private void reportUnpaidCommander(final Commander thisCommander,
                                       final int lostCommanderCnt) {
        report(thisCommander.getNation(), "commanders.unpaid." + lostCommanderCnt + ".name", thisCommander.getName());
        report(thisCommander.getNation(), "commanders.unpaid." + lostCommanderCnt + ".rank", thisCommander.getRank().getName());
    }

    /**
     * Report commanders maintenance costs.
     *
     * @param thisNation       the Nation.
     * @param money            the money paid.
     * @param unpaidCommanders the total number of unpaid commanders.
     * @param totCommanders    the total number of commanders.
     */
    private void reportCommandersMaintenance(final Nation thisNation,
                                             final int money,
                                             final int unpaidCommanders,
                                             final int totCommanders) {
        report(thisNation, "commanders.totalMoney", money);
        report(thisNation, "commanders.unpaidCommanders", unpaidCommanders);
        report(thisNation, "commanders.totCommanders", totCommanders);
    }

}
