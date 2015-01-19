package com.eaw1805.core.initializers.scenario1802.fleet;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.GoodConstants;
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
            "1,21,1,38/33",
            "1,21,1,38/33",
            "1,21,1,38/33",
            "1,22,1,38/33",
            "1,22,1,38/33",
            "1,23,1,38/33",
            "2,21,1,33/19",
            "2,21,1,33/19",
            "2,21,1,33/19",
            "2,23,1,33/19",
            "2,21,3,40/28",
            "2,21,3,40/28",
            "2,22,3,40/28",
            "3,21,1,37/16",
            "3,21,1,37/16",
            "3,22,1,37/16",
            "3,23,1,37/16",
            "3,22,2,35/14",
            "4,21,1,18/41",
            "4,22,1,18/41",
            "4,22,1,18/41",
            "4,23,1,18/41",
            "4,21,1,7/34",
            "4,23,1,7/34",
            "4,21,2,22/11",
            "4,21,2,22/11",
            "4,22,3,40/15",
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
            "5,21,3,18/6",
            "5,22,3,18/6",
            "6,22,1,13/19",
            "6,21,1,22/11",
            "6,21,1,22/11",
            "6,22,1,22/11",
            "6,23,1,22/11",
            "6,23,1,22/11",
            "6,21,1,23/20",
            "6,21,1,23/20",
            "6,21,1,23/20",
            "6,21,1,23/20",
            "6,31,1,23/20",
            "6,23,1,38/53",
            "6,23,1,38/53",
            "6,22,1,47/47",
            "6,22,1,9/46",
            "6,22,1,9/46",
            "6,23,1,9/46",
            "6,21,2,24/8",
            "6,21,2,24/8",
            "6,21,2,24/8",
            "6,22,2,24/8",
            "6,23,2,24/8",
            "6,31,3,6/10",
            "6,31,3,6/10",
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
            "7,21,3,25/26",
            "7,22,3,25/26",
            "7,22,3,25/26",
            "7,23,3,25/26",
            "8,21,1,35/40",
            "8,22,1,35/40",
            "8,23,1,35/40",
            "8,21,2,30/20",
            "8,22,2,30/20",
            "9,21,1,5/41",
            "9,22,1,5/41",
            "9,23,1,5/41",
            "9,21,2,40/27",
            "9,22,2,40/27",
            "9,21,3,5/5",
            "9,23,3,5/5",
            "10,24,1,21/50",
            "10,23,1,5/55",
            "10,24,1,5/55",
            "10,24,1,5/55",
            "10,24,1,5/55",
            "10,24,1,5/55",
            "10,25,1,5/55",
            "10,25,1,5/55",
            "10,24,3,4/18",
            "10,25,3,4/18",
            "11,21,1,38/43",
            "11,21,1,38/43",
            "11,22,1,38/43",
            "11,23,1,38/43",
            "11,21,3,23/16",
            "11,21,3,23/16",
            "11,22,3,23/16",
            "11,22,3,23/16",
            "12,21,1,48/16",
            "12,21,1,48/16",
            "12,21,1,48/16",
            "12,22,1,48/16",
            "12,23,1,48/16",
            "13,21,1,58/6",
            "13,21,1,58/6",
            "13,21,1,58/6",
            "13,21,1,58/6",
            "13,21,1,58/6",
            "13,23,1,58/6",
            "13,21,1,63/31",
            "13,21,1,63/31",
            "13,21,1,63/31",
            "13,22,1,63/31",
            "13,22,1,63/31",
            "13,23,1,63/31",
            "14,21,1,43/10",
            "14,21,1,43/10",
            "14,22,1,43/10",
            "14,23,1,43/10",
            "14,23,1,43/10",
            "14,21,3,23/18",
            "14,22,3,23/18",
            "14,22,3,23/18",
            "15,21,1,58/40",
            "15,23,1,58/40",
            "15,24,1,58/40",
            "15,24,1,58/40",
            "15,24,1,58/40",
            "15,25,1,58/40",
            "15,25,1,58/40",
            "15,22,1,72/49",
            "15,24,1,72/49",
            "15,25,1,72/49",
            "16,21,1,51/13",
            "16,21,1,51/13",
            "16,21,1,51/13",
            "16,22,1,51/13",
            "16,23,1,51/13",
            "17,21,1,59/59",
            "17,22,1,59/59",
            "17,24,1,59/59",
            "17,24,1,59/59",
            "17,24,1,59/59",
            "17,24,1,59/59",
            "17,25,1,59/59",
            "17,25,1,59/59",
            "17,25,1,59/59"
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