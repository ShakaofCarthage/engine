package com.eaw1805.orders.economy;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order for changing the taxation.
 */
public class ChangeTaxation
        extends AbstractOrderProcessor
        implements OrderInterface, ReportConstants, RegionConstants, GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ChangeTaxation.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_TAXATION;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ChangeTaxation(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ChangeTaxation instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int ownerId = getOrder().getNation().getId();
        final int type = Integer.parseInt(getOrder().getParameter1());

        // Update order's region of effect
        getOrder().setRegion(RegionManager.getInstance().getByID(EUROPE));

        // Check if particular order is issued for more than 2 turns.
        switch (type) {
            case TAX_HARSH:
                // Check previous turns
                final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByTaxOrders(getOrder().getGame(), getOrder().getNation(), getOrder().getTurn());
                if (lstOrders.size() > 1) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("cannot use harsh taxation for more than two consecutive months");

                } else {
                    getOrder().setResult(1);
                    getOrder().setExplanation("ordered harsh taxation for this month");
                    newsSingle(getOrder().getNation(), NEWS_ECONOMY, "Harsh taxation was ordered this month. Our people are not very happy but will do their best.");
                }
                break;

            case TAX_LOW:
                getOrder().setResult(1);
                getOrder().setExplanation("ordered low taxation for this month");
                newsSingle(getOrder().getNation(), NEWS_ECONOMY, "Low taxation was ordered this month. Our people are pleased and relieved.");
                break;

            default:
                final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();

                // Spend colonial goods to improve taxation
                if (getOrder().getParameter2() != null && getOrder().getParameter2().equals("1")) {
                    boolean spendGems = false;
                    if (getOrder().getParameter3().equals("1")) {
                        spendGems = true;
                    }

                    // Retrieve population size
                    final int popEurope = retrieveReportAsInt(getOrder().getNation(), getParent().getGame().getTurn() - 1, E_POP_SIZE + EUROPE);

                    // Check necessary quantities
                    // 1 tone of Colonial Goods is spent per 10,000 population,
                    int reqColonialGoods = (int) Math.ceil(popEurope / 10000d);
                    int reqGems = 0;

                    // If you have Gems in your warehouse, 1 gem per 100,000 inhabitants
                    // can be used to reduce colonial goods expenditure by 30%.
                    if (spendGems) {
                        reqGems = (int) Math.ceil(popEurope / 100000d);
                        reqColonialGoods *= 0.70d;
                    }

                    // Make sure that enough colonial goods are available
                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_COLONIAL) >= reqColonialGoods) {

                        // Make sure that enough gems are available
                        if (getParent().getTotGoods(ownerId, EUROPE, GOOD_GEMS) >= reqGems) {

                            // Spend materials
                            getParent().decTotGoods(ownerId, EUROPE, GOOD_COLONIAL, reqColonialGoods);
                            getParent().decTotGoods(ownerId, EUROPE, GOOD_GEMS, reqGems);

                            // Update goods used by order
                            usedGoods.put(GOOD_COLONIAL, reqColonialGoods);
                            usedGoods.put(GOOD_GEMS, reqGems);

                            getOrder().setResult(1);
                            if (reqGems == 0) {
                                getOrder().setExplanation("colonial goods distributed to the general population. ");
                                newsSingle(getOrder().getNation(), NEWS_ECONOMY, "We distributed colonial goods to the general population. Our people are happy with their government and will do more this month.");

                            } else {
                                getOrder().setExplanation("colonial goods and gems distributed to the general population. ");
                                newsSingle(getOrder().getNation(), NEWS_ECONOMY, "We distributed colonial goods and precious gems to the general population. Our people are very happy with their government and will do their best this month.");
                            }

                        } else {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("not enough gems at regional warehouse. ");
                        }
                    } else {
                        getOrder().setResult(-2);
                        getOrder().setExplanation("not enough colonial goods at regional warehouse. ");
                    }
                }

                // Spend Industrial points to gain VP
                if (getOrder().getParameter4() != null && getOrder().getParameter4().equals("1")) {

                    // per 10,000 Industrial points spend on populace
                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_INPT) > 10000) {
                        // Spend materials
                        getParent().decTotGoods(ownerId, EUROPE, GOOD_INPT, 10000);

                        // Update goods used by order
                        usedGoods.put(GOOD_INPT, 10000);

                        getOrder().setResult(1);
                        getOrder().setExplanation(getOrder().getExplanation() + "10,000 industrial points spent on populace. ");

                        changeVP(getOrder().getGame(), getOrder().getNation(), SPEND_INDPT, "10,000 industrial points spent on populace");

                    } else {
                        getOrder().setResult(-1);
                        getOrder().setExplanation(getOrder().getExplanation() + "Not enough industrial points available in warehouse. ");
                    }
                }

                // Spend Industrial points to gain VP
                if (getOrder().getParameter5() != null && getOrder().getParameter5().equals("1")) {

                    // Per 10,000,000 Pounds spend on populace
                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) > 10000000) {
                        // Spend materials
                        getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, 10000000);

                        // Update goods used by order
                        usedGoods.put(GOOD_MONEY, 10000000);

                        getOrder().setResult(1);
                        getOrder().setExplanation(getOrder().getExplanation() + "10,000,000 money spent on populace. ");

                        changeVP(getOrder().getGame(), getOrder().getNation(), SPEND_MONEY, "10,000,000 money spent on populace");

                    } else {
                        getOrder().setResult(-1);
                        getOrder().setExplanation(getOrder().getExplanation() + "Not enough money available in warehouse. ");
                    }
                }

                // Spend Colonial Goods to gain VP
                if (getOrder().getParameter6() != null && getOrder().getParameter6().equals("1")) {

                    // Per 500 goods spend on populace
                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_COLONIAL) > 500) {
                        // Spend materials
                        getParent().decTotGoods(ownerId, EUROPE, GOOD_COLONIAL, 500);

                        // Update goods used by order
                        usedGoods.put(GOOD_COLONIAL, 500);

                        getOrder().setResult(1);
                        getOrder().setExplanation(getOrder().getExplanation() + "500 colonial goods spent on populace. ");

                        // per 500 Colonial Goods spend on populace
                        changeVP(getOrder().getGame(), getOrder().getNation(), SPEND_COLONIAL, "500 colonial goods spent on populace");

                    } else {
                        getOrder().setResult(-1);
                        getOrder().setExplanation(getOrder().getExplanation() + "Not enough colonial goods available in warehouse. ");
                    }
                }

                getOrder().setUsedGoodsQnt(usedGoods);
        }
    }
}