package com.eaw1805.core.initializers.scenario1804.fleet;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.fleet.ShipTypeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Initializes the merchant ships.
 */
public class MerchantShipInitializer
        extends AbstractThreadedInitializer
        implements GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(MerchantShipInitializer.class);

    /**
     * The date of the 156 records.
     * "NationID,Type,region,New Pos"
     */
    private static final String[] DATA = {
            "4,21,1,18/41",
            "4,22,1,18/41",
            "4,22,1,18/41",
            "4,23,1,18/41",
            "4,21,2,22/11",
            "4,21,2,22/11",
            "5,21,1,15/26",
            "5,21,1,15/26",
            "5,22,1,15/26",
            "5,22,1,15/26",
            "5,23,1,15/26",
            "5,23,1,15/26",
            "5,21,1,27/36",
            "5,22,1,27/36",
            "5,21,2,28/12",
            "5,23,2,28/12",
            "6,22,1,13/19",
            "6,21,1,23/20",
            "6,21,1,23/20",
            "6,21,1,23/20",
            "6,21,1,23/20",
            "6,31,1,23/20",
            "6,21,2,24/8",
            "6,21,2,24/8",
            "6,21,2,24/8",
            "6,22,2,24/8",
            "6,23,2,24/8",
            "7,21,1,28/20",
            "7,21,1,28/20",
            "7,22,1,28/20",
            "7,22,1,28/20",
            "7,22,1,28/20",
            "7,23,1,28/20",
            "7,23,1,28/20",
            "7,23,1,28/20",
            "7,21,2,34/23",
            "7,21,2,34/23",
            "8,21,1,35/40",
            "8,22,1,35/40",
            "8,23,1,35/40",
            "8,21,2,30/20",
            "8,22,2,30/20",
            "11,21,1,38/43",
            "11,21,1,38/43",
            "11,22,1,38/43",
            "11,23,1,38/43",
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default constructor.
     */
    public MerchantShipInitializer() {
        super();
        LOGGER.debug("MerchantShipInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Ship> records = ShipManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("MerchantShipInitializer invoked.");

        final Game game = GameManager.getInstance().getByID(-1);

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Ship thisShip = new Ship(); //NOPMD
            final StringTokenizer thisStk = new StringTokenizer(DATA[i], ","); // NOPMD

            final int nationId = Integer.parseInt(thisStk.nextToken()); // NOPMD
            thisShip.setNation(NationManager.getInstance().getByID(nationId));

            final int type = Integer.parseInt(thisStk.nextToken()); // NOPMD
            thisShip.setType(ShipTypeManager.getInstance().getByType(type));

            thisShip.setName(thisShip.getType().getName());
            final Position thisPosition = new Position(); // NOPMD
            thisPosition.setRegion(RegionManager.getInstance().getByID(Integer.parseInt(thisStk.nextToken())));
            final StringTokenizer thisPositionStk = new StringTokenizer(thisStk.nextToken(), "/"); // NOPMD
            thisPosition.setX(Integer.parseInt(thisPositionStk.nextToken()) - 1);
            thisPosition.setY(Integer.parseInt(thisPositionStk.nextToken()) - 1);
            // Adjust position due to smaller European map
            if (thisPosition.getRegion().getId() == RegionConstants.EUROPE) {
                thisPosition.setX(thisPosition.getX() - 12);
                thisPosition.setY(thisPosition.getY() - 14);
            }

            thisPosition.setGame(game);
            thisShip.setPosition(thisPosition);

            thisShip.setFleet(0);
            thisShip.setCondition(100);
            thisShip.setMarines(thisShip.getType().getCitizens());
            thisShip.setExp(1);
            thisShip.setCapturedByNation(0);
            thisShip.setNoWine(false);
            thisShip.setNavalBattle(false);

            final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
            for (int goodID = GOOD_FIRST; goodID <= GOOD_COLONIAL; goodID++) {
                qteGoods.put(goodID, 0);
            }
            thisShip.setStoredGoods(qteGoods);

            ShipManager.getInstance().add(thisShip);
        }

        LOGGER.info("MerchantShipInitializer complete.");
    }

}
