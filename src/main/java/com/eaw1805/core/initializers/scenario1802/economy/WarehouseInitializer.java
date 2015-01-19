package com.eaw1805.core.initializers.scenario1802.economy;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Initializes the table with the Warehouse data.
 */
public class WarehouseInitializer
        extends AbstractThreadedInitializer
        implements GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(WarehouseInitializer.class);

    /**
     * Initial state of warehouses.
     * "Nation,Region,Mny,Citz,Ecpt,Food,Stn,Wood,Ore,Zinc,Hors,Text,Wool,Gold,Wine,ColonialGoods,AP,CP"
     */
    private static final String[] DATA = {
            "1,1,10375432,57423,6102,14856,2431,3256,65,0,7325,216,134,34,45,0,20,20",
            "1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "1,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "1,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "2,1,16072676,78136,7715,9360,2655,5187,131,12,6899,215,113,82,22,0,20,20",
            "2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "2,3,1742135,11439,958,500,4522,934,43,11,3145,49,23,77,36,0,0,0",
            "2,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "3,1,9735889,62153,6089,10344,1732,3410,51,0,6524,203,134,16,80,0,20,20",
            "3,2,1154888,6122,387,295,299,699,9,8,1706,41,23,11,34,0,0,0",
            "3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "3,4,0,3985,136,114,84,211,3,1,431,11,7,4,9,0,0,0",
            "4,1,9878499,58214,6245,12480,1679,3246,68,9,6945,216,109,35,50,0,20,20",
            "4,2,1845144,18451,426,1850,679,689,34,16,1972,68,28,23,156,0,0,0",
            "4,3,1278439,5974,305,300,289,678,14,5,845,42,25,43,109,0,0,0",
            "4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "5,1,9875874,58004,6359,15516,1689,3156,60,8,7946,203,124,24,65,0,20,20",
            "5,2,1548735,12657,403,712,305,702,16,11,1743,38,28,22,74,0,0,0",
            "5,3,1574385,11566,422,740,378,846,24,14,961,23,21,26,66,0,0,0",
            "5,4,0,4650,188,211,116,214,9,3,512,7,9,6,11,0,0,0",
            "6,1,10087835,58167,6278,11664,1647,3958,48,11,6912,234,116,35,90,0,20,20",
            "6,2,1287577,9645,356,470,257,654,15,9,835,39,26,22,157,0,0,0",
            "6,3,1287544,12458,426,758,235,965,24,11,1687,46,28,25,139,0,0,0",
            "6,4,0,4088,156,212,88,404,9,4,650,32,11,6,18,0,0,0",
            "7,1,14644963,83879,7744,9336,2645,3410,89,9,7648,234,169,71,35,0,20,20",
            "7,2,1235612,10578,388,240,354,720,19,15,1420,39,21,16,46,0,0,0",
            "7,3,1125326,11312,402,350,299,864,24,9,1589,46,23,21,68,0,0,0",
            "7,4,0,6720,325,188,156,312,9,3,912,18,16,12,54,0,0,0",
            "8,1,14520035,84452,7715,9828,1546,4625,118,7,6598,208,123,74,25,0,20,20",
            "8,2,1187846,6248,399,200,278,814,23,9,1678,46,28,26,44,0,0,0",
            "8,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "8,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "9,1,14454265,79512,6324,9744,2015,3206,69,11,7129,143,132,64,35,0,20,20",
            "9,2,1435744,16948,378,889,305,823,23,8,1689,49,29,24,56,0,0,0",
            "9,3,1734188,11123,311,380,243,785,19,9,942,50,21,23,24,0,0,0",
            "9,4,0,6855,124,320,65,468,8,2,805,25,12,6,9,0,0,0",
            "10,1,9310175,59462,6099,12912,1649,3521,64,9,6891,231,101,25,90,0,20,20",
            "10,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "10,3,1301128,6245,399,200,236,751,13,8,1678,54,25,23,66,0,0,0",
            "10,4,0,680,35,24,0,6,0,0,219,0,0,0,1,0,0,0",
            "11,1,20763474,85122,7601,9396,2036,4625,119,11,6925,213,108,61,30,0,20,20",
            "11,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "11,3,1541088,8235,387,210,265,758,25,9,1678,48,23,17,51,0,0,0",
            "11,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "12,1,9935459,58156,6134,12408,1648,3058,39,9,6189,224,106,29,35,0,20,20",
            "12,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "12,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "12,4,0,1544,71,88,25,117,4,0,56,5,0,1,2,0,0,0",
            "13,1,9124512,58213,6221,17544,2108,1060,49,12,9429,221,108,34,195,0,20,20",
            "13,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "13,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "13,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "14,1,9455433,65249,6029,9948,1699,3029,39,11,6158,134,147,24,22,0,20,20",
            "14,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "14,3,1401259,9289,388,250,249,948,16,10,1678,41,25,16,68,0,0,0",
            "14,4,0,2750,113,89,55,260,3,2,365,11,4,3,5,0,0,0",
            "15,1,9715459,56479,6089,17640,1679,3125,45,12,6489,224,201,37,132,0,20,20",
            "15,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "15,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "15,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "16,1,13784388,84452,8689,11568,1942,3122,67,10,8851,132,134,41,41,0,20,20",
            "16,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "16,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "16,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "17,1,9715459,56479,6089,17640,1679,3125,45,12,6489,224,201,37,132,0,20,20",
            "17,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "17,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "17,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default WarehouseInitializer's constructor.
     */
    public WarehouseInitializer() {
        LOGGER.debug("WarehouseInitializer instantiated.");
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
        LOGGER.debug("WarehouseInitializer invoked.");
        final Game scenario = GameManager.getInstance().getByID(-1);
        final List<Nation> listOfNations = NationManager.getInstance().list();
        final List<Region> listOfRegions = RegionManager.getInstance().list();

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final StringTokenizer sTok = new StringTokenizer(DATA[i], ",");  // NOPMD

            final Warehouse thisWarehouse = new Warehouse(); //NOPMD
            thisWarehouse.setGame(scenario);
            thisWarehouse.setNation(listOfNations.get(Integer.parseInt(sTok.nextToken())));
            thisWarehouse.setRegion(listOfRegions.get(Integer.parseInt(sTok.nextToken()) - 1));

            final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
            for (int j = GOOD_FIRST; j <= GOOD_CP; j++) {
                qteGoods.put(j, Integer.parseInt(sTok.nextToken()));
            }
            thisWarehouse.setStoredGoodsQnt(qteGoods);

            WarehouseManager.getInstance().add(thisWarehouse);
        }

        LOGGER.info("WarehouseInitializer complete.");
    }

}
