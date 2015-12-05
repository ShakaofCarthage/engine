package com.eaw1805.economy.trade;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TradeConstants;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.economy.Good;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles the trading of a turn.
 * Controls NPC trades and upkeep of trade cities.
 * ticket:36.
 */
public class TradeProcessor
        implements RegionConstants, GoodConstants, TradeConstants, OrderConstants, ArmyConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TradeProcessor.class);

    /**
     * The game processed.
     */
    private final transient Game game;

    /**
     * The random generator.
     */
    private final transient Random randomGen;

    /**
     * The phase of the trading.
     */
    private final int phase;

    /**
     * Default constructor.
     *
     * @param caller       the game engine that invoked us.
     * @param tradingPhase the phase of the trading.
     */
    public TradeProcessor(final GameEngine caller, final int tradingPhase) {
        game = caller.getGame();
        randomGen = caller.getRandomGen();
        phase = tradingPhase;
        LOGGER.debug("TradeProcessor instantiated for trading phase [" + tradingPhase + "].");
    }

    /**
     * Get the game object.
     *
     * @return the game object.
     */
    public final Game getGame() {
        return game;
    }

    /**
     * Get the random number generator.
     *
     * @return the random number generator.
     */
    public final Random getRandomGen() {
        return randomGen;
    }

    /**
     * Do trading upkeep.
     */
    public void process() {
        // aggregate amounts traded by players per trade city
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final Map<Integer, Map<Integer, Map<Integer, Integer>>> tradedValues = aggregatePlayerOrders();

        // aggregate city trade rates & wealth per trade city owner
        final Map<Region, Map<Nation, List<TradeCity>>> linkedCities = new HashMap<Region, Map<Nation, List<TradeCity>>>();

        // initialize maps
        final List<Region> lstRegions = RegionManager.getInstance().list();
        for (final Region region : lstRegions) {
            linkedCities.put(region, new HashMap<Nation, List<TradeCity>>());
        }

        // Retrieve list of goods
        final List<Good> lstGoods = GoodManager.getInstance().list();
        lstGoods.remove(GOOD_CP - 1); // Remove Command Points -- this is a virtual good
        lstGoods.remove(GOOD_AP - 1); // Remove Administrative Points -- this is a virtual good
        lstGoods.remove(GOOD_PEOPLE - 1); // Remove People -- they are not traded
        lstGoods.remove(GOOD_MONEY - 1); // Remove Money -- they are not traded

        // Retrieve trade cities
        final List<TradeCity> listCities = TradeCityManager.getInstance().listByGame(getGame());
        for (final TradeCity thisCity : listCities) {
            final TradeCity firstCity;

            // Keep track of linked city wealth (based on values traded per good)
            int cityWealth;

            // check if this is a linked city (based on owner)            
            final Region thisRegion = thisCity.getPosition().getRegion();
            final Nation thisNation = thisCity.getNation();
            // Make sure Neutral cities are not linked together
            if (thisNation.getId() == NATION_NEUTRAL) {
                firstCity = thisCity;

                // initialize city wealth
                cityWealth = getRandomGen().nextInt(2000000) + 500000;

            } else {
                if (linkedCities.get(thisRegion).containsKey(thisNation)) {
                    firstCity = linkedCities.get(thisRegion).get(thisNation).get(0);

                    // add city in the list
                    linkedCities.get(thisRegion).get(thisNation).add(thisCity);

                } else {
                    firstCity = thisCity;

                    // initialize list of cities for this owner
                    final List<TradeCity> cities = new ArrayList<TradeCity>();

                    // add this city
                    cities.add(thisCity);
                    linkedCities.get(thisRegion).put(thisNation, cities);
                }

                // initialize city wealth
                cityWealth = getRandomGen().nextInt(2000000) + 500000;
            }

            // access good ratings for central city
            final Map<Integer, Integer> rateGoods = firstCity.getGoodsTradeLvl();

            // access player orders for this city (not first city)
            final Map<Integer, Map<Integer, Integer>> playerOrdersPerGood;
            if (tradedValues.containsKey(thisCity.getCityId())) {
                playerOrdersPerGood = tradedValues.get(thisCity.getCityId());

            } else {
                // no player orders found
                playerOrdersPerGood = new HashMap<Integer, Map<Integer, Integer>>();
            }

            // Previous turn city wealth
            final int previousCityWealth = firstCity.getGoodsTradeLvl().get(GOOD_MONEY);

            LOGGER.info("TRADE City [" + thisCity.getName() + "] Previews Wealth [" + previousCityWealth + "] - New Wealth Random Factor [" + cityWealth + "]");

            // go through all the items
            for (final Good good : lstGoods) {
                // Possibility of NPC trading for this good
                final int npcAction = getNPCChance(rateGoods.get(good.getGoodId()));
                int npcValue = 0;
                if (npcAction != TRADE_NO) {
                    // Determine amount of NPC transactions
                    final int roll = getRandomGen().nextInt(11) + 1;
                    npcValue = (int) ((previousCityWealth * roll) / 100d);

                    cityWealth += npcValue / 3;

                    String action = "SELL";
                    if (npcAction == TRADE_BUY) {
                        npcValue *= -1;
                        action = "BUY";
                    }

                    LOGGER.info("TRADE City [" + thisCity.getName() + "] - NPC transaction for " + good.getName() + " " + action + " " + npcValue);
                }

                // Calculate total money exchanged  (alpha)
                int totValue = npcValue;

                // Find values traded by players
                if (playerOrdersPerGood.containsKey(good.getGoodId())) {
                    final Map<Integer, Integer> playerActions = playerOrdersPerGood.get(good.getGoodId());

                    if (playerActions.containsKey(TRADE_BUY)) {
                        totValue -= playerActions.get(TRADE_BUY);
                        cityWealth += playerActions.get(TRADE_BUY) / 2;
                        LOGGER.info("TRADE City [" + thisCity.getName() + "] - Player transaction(s) for " + good.getName() + " BUY " + playerActions.get(TRADE_BUY));
                    }

                    if (playerActions.containsKey(TRADE_SELL)) {
                        totValue += playerActions.get(TRADE_SELL);
                        cityWealth += playerActions.get(TRADE_SELL) / 2;
                        LOGGER.info("TRADE City [" + thisCity.getName() + "] - Player transaction(s) for " + good.getName() + " SELL " + playerActions.get(TRADE_SELL));
                    }
                }

                // Fix problem with negative values
                // (ticket: 941)
                if (rateGoods.get(good.getGoodId()) < 0) {
                    rateGoods.put(good.getGoodId(), TRADE_ES);
                }

                // Determine changes in good rate
                final int change = getGoodRateChange(totValue);
                if (change == TRADE_RATE_DEC && rateGoods.get(good.getGoodId()) > TRADE_ES) {
                    rateGoods.put(good.getGoodId(), rateGoods.get(good.getGoodId()) - 1);
                    LOGGER.info("TRADE City [" + thisCity.getName() + "] - Good " + good.getName() + " rate DECREASES");

                } else if (change == TRADE_RATE_INC && rateGoods.get(good.getGoodId()) < TRADE_HD) {
                    rateGoods.put(good.getGoodId(), rateGoods.get(good.getGoodId()) + 1);
                    LOGGER.info("TRADE City [" + thisCity.getName() + "] - Good " + good.getName() + " rate INCREASES");
                }
            }

            LOGGER.info("TRADE City [" + thisCity.getName() + "] New Wealth [" + cityWealth + "]");

            // Update trade city
            rateGoods.put(GOOD_MONEY, cityWealth);
            firstCity.setGoodsTradeLvl(rateGoods);
            TradeCityManager.getInstance().update(firstCity);
        }

        // Trade cities on the same map belonging to the same empire should have the same trade rates (demand level)
        // Iterate through regions
        for (final Map<Nation, List<TradeCity>> citiesPerNation : linkedCities.values()) {
            // Iterate through national entries found this region
            for (List<TradeCity> nationsCities : citiesPerNation.values()) {
                // Check if this nation has more than 1 trade cities in this region
                if (nationsCities.size() > 1) {
                    // calculate the share of each linked city
                    final TradeCity firstCity = nationsCities.get(0);

                    // make sure that neutral cities are not linked
                    if (firstCity.getNation().getId() != NATION_NEUTRAL) {
                        // retrieve linked trade rates
                        final Map<Integer, Integer> rateGoods = firstCity.getGoodsTradeLvl();

                        // Link the gold of all trade cities owned by this nation in this region
                        for (final TradeCity city : nationsCities) {
                            if (city.getCityId() == firstCity.getCityId()) {
                                continue;
                            }
                            LOGGER.info("Linking trade rates and wealth of " + city.getName() + " with " + firstCity.getName());

                            final Map<Integer, Integer> cityGoods = city.getGoodsTradeLvl();

                            for (final Good good : lstGoods) {
                                cityGoods.put(good.getGoodId(), rateGoods.get(good.getGoodId()));
                            }

                            cityGoods.put(GOOD_MONEY, rateGoods.get(GOOD_MONEY));
                            city.setGoodsTradeLvl(cityGoods);

                            // Update trade city
                            TradeCityManager.getInstance().update(city);
                        }
                    }
                }
            }
        }

        theTrans.commit();
    }

    /**
     * Determine if the good rate will increase, decrease or remain the same.
     *
     * @param alpha total amount of money traded.
     * @return increase, decrease or remain the same.
     */
    private int getGoodRateChange(final int alpha) {
        int chanceInc = 0, chanceSame = 100;
        if (alpha < -5000001) {
            chanceInc = 89;
            chanceSame = 10;

        } else if (alpha > -5000000 && alpha < -3000001) {
            chanceInc = 77;
            chanceSame = 20;

        } else if (alpha > -3000000 && alpha < -2000001) {
            chanceInc = 65;
            chanceSame = 30;

        } else if (alpha > -2000000 && alpha < -1000001) {
            chanceInc = 53;
            chanceSame = 40;

        } else if (alpha > -1000000 && alpha < -500001) {
            chanceInc = 42;
            chanceSame = 50;

        } else if (alpha > -500000 && alpha < -100001) {
            chanceInc = 31;
            chanceSame = 60;

        } else if (alpha > -100000 && alpha < 100001) {
            chanceInc = 15;
            chanceSame = 70;

        } else if (alpha > 100000 && alpha < 500000) {
            chanceInc = 9;
            chanceSame = 60;

        } else if (alpha > 500001 && alpha < 1000000) {
            chanceInc = 8;
            chanceSame = 50;

        } else if (alpha > 1000001 && alpha < 2000000) {
            chanceInc = 7;
            chanceSame = 40;

        } else if (alpha > 2000001 && alpha < 3000000) {
            chanceInc = 5;
            chanceSame = 30;

        } else if (alpha > 3000001 && alpha < 5000000) {
            chanceInc = 3;
            chanceSame = 20;

        } else if (alpha > 5000000) {
            chanceInc = 1;
            chanceSame = 10;
        }

        // Roll the chances
        final int decision;
        final int roll = getRandomGen().nextInt(101) + 1;
        if (roll <= chanceInc) {
            decision = TRADE_RATE_INC;

        } else if (roll <= chanceInc + chanceSame) {
            decision = TRADE_RATE_NO;

        } else {
            decision = TRADE_RATE_DEC;
        }

        return decision;
    }

    /**
     * Determine the behaviour of NPC trades for this good type.
     *
     * @param demandRate the good rating.
     * @return the NPC action.
     */
    private int getNPCChance(final int demandRate) {
        final int roll = getRandomGen().nextInt(101) + 1;
        int action;
        switch (demandRate) {
            case TRADE_ES:
                if (roll < 70) {
                    action = TRADE_BUY;

                } else if (roll < 90) {
                    action = TRADE_SELL;

                } else {
                    action = TRADE_NO;
                }
                break;

            case TRADE_S:
                if (roll < 50) {
                    action = TRADE_BUY;

                } else if (roll < 80) {
                    action = TRADE_SELL;

                } else {
                    action = TRADE_NO;
                }
                break;

            case TRADE_A:
                if (roll < 35) {
                    action = TRADE_BUY;

                } else if (roll < 70) {
                    action = TRADE_SELL;

                } else {
                    action = TRADE_NO;
                }
                break;

            case TRADE_D:
                if (roll < 30) {
                    action = TRADE_BUY;

                } else if (roll < 80) {
                    action = TRADE_SELL;

                } else {
                    action = TRADE_NO;
                }
                break;

            case TRADE_HD:
                if (roll < 20) {
                    action = TRADE_BUY;

                } else if (roll < 90) {
                    action = TRADE_SELL;

                } else {
                    action = TRADE_NO;
                }
                break;

            default:
                action = TRADE_NO;
        }

        return action;
    }

    /**
     * aggregate amounts traded by players per trade city.
     *
     * @return report the total value bought & sold per trade city and per good type.
     */
    private Map<Integer, Map<Integer, Map<Integer, Integer>>> aggregatePlayerOrders() {
        // Retrieve player trading orders depending on the phase.
        final List<PlayerOrder> lstOrders;
        if (phase == 1) {
            lstOrders = PlayerOrderManager.getInstance().listTradeFirstOrders(getGame());

        } else {
            lstOrders = PlayerOrderManager.getInstance().listTradeSecondOrders(getGame());
        }

        // aggregate amounts traded by all players per trade city, good-type
        // <Trade City ID>, <Good-Type>, <action-type: Buy/Sell>, <Total amount>
        final Map<Integer, Map<Integer, Map<Integer, Integer>>> tradedValues = new HashMap<Integer, Map<Integer, Map<Integer, Integer>>>();
        for (final PlayerOrder order : lstOrders) {
            final int sourceTPE = Integer.parseInt(order.getParameter1());
            final int sourceID = Integer.parseInt(order.getParameter2());
            final int targetTPE = Integer.parseInt(order.getParameter3());
            final int targetID = Integer.parseInt(order.getParameter4());

            // Identify good type
            final int tpe = Integer.parseInt(order.getParameter5());

            // Get total amount of money used in the transaction
            // Temp9 is updated by the order processor.
            int value = 0;
            if (order.getTemp9() != null) {
                value = Integer.parseInt(order.getTemp9());
            }

            // Determine the action type + the trade city
            int tradeCityId;
            int action;

            if (sourceTPE == TRADECITY) {
                tradeCityId = sourceID;
                action = TRADE_BUY;

            } else if (targetTPE == TRADECITY) {
                tradeCityId = targetID;
                action = TRADE_SELL;

            } else {
                LOGGER.error("Unknown trade command " + order.getOrderId());
                continue;
            }

            // Check if this is the first entry for this trade city
            final Map<Integer, Map<Integer, Integer>> valuePerGood;
            if (tradedValues.containsKey(tradeCityId)) {
                valuePerGood = tradedValues.get(tradeCityId);

            } else {
                valuePerGood = new HashMap<Integer, Map<Integer, Integer>>();
                tradedValues.put(tradeCityId, valuePerGood);
            }

            // Update entry for particular good type
            final Map<Integer, Integer> actionPerGood;
            if (valuePerGood.containsKey(tpe)) {
                actionPerGood = valuePerGood.get(tpe);

            } else {
                actionPerGood = new HashMap<Integer, Integer>();
                valuePerGood.put(tpe, actionPerGood);
            }

            // Check if there is an entry for particular type of transaction
            if (actionPerGood.containsKey(action)) {
                value += actionPerGood.get(action);
            }

            // Update store
            actionPerGood.put(action, value);
        }

        return tradedValues;
    }

}
