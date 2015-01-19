package com.eaw1805.core.initializers.scenario1802.fleet;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.fleet.ShipTypeManager;
import com.eaw1805.data.model.fleet.ShipType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Initializes the ship types.
 */
public class ShipTypesInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ShipTypesInitializer.class);

    /**
     * The date of the 18 records.
     * "Type,Name,canIndies,Wood,EcPt,Tex,Citz,Cost,Main,LC,MF,SC"
     */
    private static final String[] DATA = {
            "1,22 Cannon Brig,1,500,180,30,250,100000,5000,100,50,1",
            "2,36 Cannon Corvette,1,750,375,45,350,250000,12500,200,45,2",
            "3,64 Cannon Frigate,0,1200,750,45,500,500000,25000,400,35,3",
            "4,84 Cannon Cruiser,0,2000,1250,50,800,900000,45000,500,25,4",
            "5,120 Cannon Ship-of-the-Line,0,3200,2000,60,1100,1600000,80000,600,20,5",
            "11,24 Cannon Small Corsair,0,500,200,40,350,150000,7500,100,60,1",
            "12,36 Cannon Large Corsair,0,800,450,50,500,300000,15000,250,50,2",
            "21,Small Merchant,1,700,75,30,75,200000,10000,250,50,0",
            "22,Medium Merchant,1,1400,150,40,125,350000,17500,500,40,0",
            "23,Large Merchant,0,2000,250,50,200,500000,25000,750,30,0",
            "24,Small Dhow,0,500,50,40,50,150000,7500,200,60,0",
            "25,Large Dhow,0,1000,125,50,100,300000,15000,400,50,0",
            "31,East Indiamen (28 cannon),0,1600,300,40,200,450000,22500,600,35,0"
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default constructor.
     */
    public ShipTypesInitializer() {
        super();
        LOGGER.debug("ShipTypesInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<ShipType> records = ShipTypeManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("ShipTypesInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final ShipType thisShipType = new ShipType(); //NOPMD
            final StringTokenizer thisStk = new StringTokenizer(DATA[i], ","); // NOPMD
            thisShipType.setIntId(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setName(thisStk.nextToken());
            thisShipType.setCanColonies(Integer.parseInt(thisStk.nextToken()) == 1);
            thisShipType.setWood(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setIndPt(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setFabrics(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setCitizens(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setCost(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setMaintenance(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setLoadCapacity(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setMovementFactor(Integer.parseInt(thisStk.nextToken()));
            thisShipType.setShipClass(Integer.parseInt(thisStk.nextToken()));
            ShipTypeManager.getInstance().add(thisShipType);
        }

        LOGGER.info("ShipTypesInitializer complete.");
    }

}
