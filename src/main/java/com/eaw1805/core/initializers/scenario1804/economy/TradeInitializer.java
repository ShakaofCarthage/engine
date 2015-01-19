package com.eaw1805.core.initializers.scenario1804.economy;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TradeConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Initializes the table with the trade cities data.
 */
public class TradeInitializer
        extends AbstractThreadedInitializer
        implements GoodConstants, TradeConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(TradeInitializer.class);

    /**
     * Initial state of warehouses.
     * "Region,New Pos,Name",
     */
    private static final String[] DATA = {
            "1,28/20,Amsterdam",
            "1,39/22,Berlin",
            "1,37/16,Copenhagen",
            "1,23/20,London",
            "1,13/41,Madrid",
            "1,35/27,Munich",
            "1,38/43,Naples",
            "1,23/27,Paris",
            "1,35/40,Rome",
            "1,30/49,Tunis",
            "1,41/29,Vienna",
            "2,40/27,Belem",
            "2,26/23,Cartagena",
            "2,35/14,Martinique",
            "2,13/4,New Orleans",
            "2,22/11,Santiago de Cuba",
            "2,9/15,Vera Cruz"
    };


    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default TradeInitializer's constructor.
     */
    public TradeInitializer() {
        LOGGER.debug("TradeInitializer instantiated.");
    }

    /**
     * t
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        return (WarehouseManager.getInstance().list().size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("TradeInitializer invoked.");
        final Game scenario = GameManager.getInstance().getByID(-1);
        final List<Region> listOfRegions = RegionManager.getInstance().list();

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final StringTokenizer sTok = new StringTokenizer(DATA[i], ",");  // NOPMD

            final TradeCity thisCity = new TradeCity(); //NOPMD

            final Position thisPosition = new Position(); //NOPMD
            thisPosition.setGame(scenario);
            thisPosition.setRegion(listOfRegions.get(Integer.parseInt(sTok.nextToken()) - 1));
            final StringTokenizer thisPositionStk = new StringTokenizer(sTok.nextToken(), "/"); // NOPMD
            thisPosition.setX(Integer.parseInt(thisPositionStk.nextToken()) - 1);
            thisPosition.setY(Integer.parseInt(thisPositionStk.nextToken()) - 1);
            // Adjust position due to smaller European map
            if (thisPosition.getRegion().getId() == RegionConstants.EUROPE) {
                thisPosition.setX(thisPosition.getX() - 12);
                thisPosition.setY(thisPosition.getY() - 14);
            }
            thisCity.setPosition(thisPosition);

            thisCity.setName(sTok.nextToken());

            final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
            for (int goodID = GOOD_INPT; goodID <= GOOD_COLONIAL; goodID++) {
                qteGoods.put(goodID, TRADE_A);
            }
            qteGoods.put(GOOD_MONEY, 1000);
            thisCity.setGoodsTradeLvl(qteGoods);

            TradeCityManager.getInstance().add(thisCity);
        }

        LOGGER.info("TradeInitializer complete.");
    }

}
