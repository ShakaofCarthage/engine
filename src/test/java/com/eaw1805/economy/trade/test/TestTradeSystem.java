package com.eaw1805.economy.trade.test;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.TradeCalculations;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Test functionality for trading system.
 */
public class TestTradeSystem {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TestTradeSystem.class);

    public static void main(String[] argc) {
        final int[] testQTE = {1, 10, 100, 1000};

        final int totWealth = 2093897;

        // BUY, Ind.Points, Average
        for (final int qte : testQTE) {
            final int valueBuy = TradeCalculations.getBuyGoodCost(20, 3, qte, false, false, false);
            final int valueSell = TradeCalculations.getSellGoodCost(20, 3, qte, false, false, false);
            LOGGER.info("Ind.Points (good factor=20), Demand (rate = 3) - QTE = " + qte + ", BUY VALUE=" + valueBuy + ", SELL VALUE=" + valueSell);
        }
        int maxBuy = TradeCalculations.getMaxBuyQTE(GoodConstants.GOOD_INPT, 20, 3, totWealth, false, false, false, true);
        int maxSell = TradeCalculations.getMaxSellQTE(20, 3, totWealth, false, false, false, true);
        LOGGER.info("Reverse Wealth = " + totWealth + " MAX BUY QTE = " + maxBuy + ", MAX SELL QTE=" + maxSell);

        // BUY, Food, Excessive Demand
        for (final int qte : testQTE) {
            final int valueBuy = TradeCalculations.getBuyGoodCost(15, 5, qte, false, false, false);
            final int valueSell = TradeCalculations.getSellGoodCost(15, 5, qte, false, false, false);
            LOGGER.info("Food (good factor=15), Excessive Surplus (rate = 5) - QTE = " + qte + ", BUY VALUE=" + valueBuy + ", SELL VALUE=" + valueSell);
        }

        maxBuy = TradeCalculations.getMaxBuyQTE(GoodConstants.GOOD_FOOD, 15, 5, totWealth, false, false, false, true);
        maxSell = TradeCalculations.getMaxSellQTE(15, 5, totWealth, false, false, false, true);
        LOGGER.info("Reverse Wealth = " + totWealth + " MAX BUY QTE = " + maxBuy + ", MAX SELL QTE=" + maxSell);

        // BUY, Stone, Surplus
        for (final int qte : testQTE) {
            final int valueBuy = TradeCalculations.getBuyGoodCost(5, 2, qte, false, false, false);
            final int valueSell = TradeCalculations.getSellGoodCost(5, 2, qte, false, false, false);
            LOGGER.info("Stone (good factor=5), Demand (rate = 2) - QTE = " + qte + ", BUY VALUE=" + valueBuy + ", SELL VALUE=" + valueSell);
        }

        maxBuy = TradeCalculations.getMaxBuyQTE(GoodConstants.GOOD_STONE, 5, 2, totWealth, false, false, false, true);
        maxSell = TradeCalculations.getMaxSellQTE(5, 2, totWealth, false, false, false, true);
        LOGGER.info("Reverse Wealth = " + totWealth + " MAX BUY QTE = " + maxBuy + ", MAX SELL QTE=" + maxSell);

    }

}
