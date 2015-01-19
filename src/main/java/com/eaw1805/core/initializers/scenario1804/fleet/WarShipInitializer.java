package com.eaw1805.core.initializers.scenario1804.fleet;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
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

import java.util.List;
import java.util.StringTokenizer;

/**
 * Initializes the war ships.
 */
public class WarShipInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(WarShipInitializer.class);

    /**
     * The date of the 322 records.
     * "NationID,Type,Name,region,New Pos"
     */
    private static final String[] DATA = {
            "4,1,CADEZ,2,22/11",
            "4,1,LAREDO,2,22/11",
            "4,2,ESPANIA,2,22/11",
            "4,3,CISNEROS,2,22/11",
            "4,4,MONTANEZ,2,22/11",
            "5,1,MONTANGE,1,15/26",
            "5,2,PELLETIER,1,15/26",
            "5,3,CONVENTION,1,15/26",
            "5,3,ACHILLE,1,15/26",
            "5,3,REDOUTABLE,1,15/26",
            "5,3,ALGERICAS,1,15/26",
            "5,4,DUGURAY-TROUIN,1,15/26",
            "5,4,LION,1,15/26",
            "5,4,SCIPION,1,15/26",
            "5,4,PLUTON,1,15/26",
            "5,4,BUCENTAURE,1,15/26",
            "5,4,FORMIDABLE,1,15/26",
            "5,5,IL DE FRANCE,1,15/26",
            "5,1,AMERICA,1,27/36",
            "5,3,JACOBIN,1,27/36",
            "5,4,HEROS,1,27/36",
            "5,4,MONT-BLANC,1,27/36",
            "5,4,NEPTUNE,1,27/36",
            "5,3,GASPARIN,2,28/12",
            "5,3,JUSTE,2,28/12",
            "5,3,SWIFTURE,2,28/12",
            "6,2,ARROW,1,13/19",
            "6,2,HOLLY,1,13/19",
            "6,2,DILIGENT,1,13/19",
            "6,3,ROSE,1,13/19",
            "6,4,DEFENCE,1,13/19",
            "6,1,AQUILON,1,23/20",
            "6,3,MARTIN,1,23/20",
            "6,3,AGAMEMNON,1,23/20",
            "6,4,LEVIATHAN,1,23/20",
            "6,4,ORION,1,23/20",
            "6,4,TONNANT,1,23/20",
            "6,5,BRITANNIA,1,23/20",
            "7,1,MONNIKENDAM,1,28/20",
            "7,1,LEYDEN,1,28/20",
            "7,2,WASSENER,1,28/20",
            "7,3,ALKMAAR,1,28/20",
            "7,3,TJERK HIDDES,1,28/20",
            "7,4,GELIJKHEID,1,28/20",
            "7,4,HAARLEM,1,28/20",
            "7,4,VRIJHEID,1,28/20",
            "7,4,HERCULES,1,28/20",
            "7,1,BATAVIER,2,34/23",
            "7,3,DELFT,2,34/23",
            "7,4,BRUTUS,2,34/23",
            "8,1,BULGASSI,1,35/40",
            "8,1,FRENZONI,1,35/40",
            "8,2,EUGENE,1,35/40",
            "8,2,BATTAGLIA,1,35/40",
            "8,2,VERSACE,1,35/40",
            "8,3,TORINO,1,35/40",
            "8,3,STRATOZZI,1,35/40",
            "8,3,ATTURIOS,1,35/40",
            "8,3,SEPTIMO,1,35/40",
            "8,3,PARILA,1,35/40",
            "8,3,IL SPECTACLE,1,35/40",
            "8,4,KURIO,1,35/40",
            "8,4,IL DIABOLO,1,35/40",
            "8,4,GUIESIEPPO,1,35/40",
            "8,1,VESPOLI,2,30/20",
            "8,1,MILANO,2,30/20",
            "8,2,ROMA,2,30/20",
            "11,1,VENEDIG,1,38/43",
            "11,1,VERONA,1,38/43",
            "11,1,LA CASA,1,38/43",
            "11,1,BASTICO,1,38/43",
            "11,2,POMPEJI,1,38/43",
            "11,3,NAPOLI,1,38/43",
            "11,3,SABRATHO,1,38/43",
            "11,3,ALPAGIO,1,38/43",
            "11,3,LA CESA,1,38/43",
            "11,3,NAPOLITA,1,38/43",
            "11,3,DOSTREGA,1,38/43",
            "11,4,PUPLIAS,1,38/43",
            "11,4,DEMESTICO,1,38/43"
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default constructor.
     */
    public WarShipInitializer() {
        super();
        LOGGER.debug("WarShipInitializer instantiated.");
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
        LOGGER.debug("WarShipInitializer invoked.");

        final Game game = GameManager.getInstance().getByID(-1);

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Ship thisShip = new Ship(); //NOPMD
            final StringTokenizer thisStk = new StringTokenizer(DATA[i], ","); // NOPMD

            final int nationId = Integer.parseInt(thisStk.nextToken()); // NOPMD
            thisShip.setNation(NationManager.getInstance().getByID(nationId));

            final int type = Integer.parseInt(thisStk.nextToken()); // NOPMD
            thisShip.setType(ShipTypeManager.getInstance().getByType(type));

            thisShip.setName(thisStk.nextToken());
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
            ShipManager.getInstance().add(thisShip);
        }

        LOGGER.info("WarShipInitializer complete.");
    }

}
