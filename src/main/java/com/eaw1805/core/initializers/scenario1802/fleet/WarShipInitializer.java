package com.eaw1805.core.initializers.scenario1802.fleet;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
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
            "1,1,MLADENOW,1,38/33",
            "1,1,FM HILLER,1,38/33",
            "1,2,GALIZIEN,1,38/33",
            "1,2,TRAZEK,1,38/33",
            "1,2,DONAU,1,38/33",
            "1,2,ANTASIUS,1,38/33",
            "1,2,OB. STEYRER,1,38/33",
            "1,3,WIEN,1,38/33",
            "1,3,ERZH. KARL,1,38/33",
            "2,1,OLDENBURG,1,33/19",
            "2,1,BADEN,1,33/19",
            "2,1,WÜRTTEMBERG,1,33/19",
            "2,2,FRANKFURT,1,33/19",
            "2,3,WESTFALEN,1,33/19",
            "2,3,ALDEKERK,1,33/19",
            "2,3,BLUGGE,1,33/19",
            "2,4,SUNSBOCK,1,33/19",
            "2,4,KAPELLEN,1,33/19",
            "2,1,GELDERN,3,40/28",
            "2,1,MARIE-LOUISE,3,40/28",
            "2,2,HAMBURG,3,40/28",
            "2,3,WURZBURG,3,40/28",
            "3,1,JYLLAND,1,37/16",
            "3,1,INDFODSRETTEN,1,37/16",
            "3,1,HOLSTEEN,1,37/16",
            "3,2,NEYEBORG,1,37/16",
            "3,3,REDSBORG,1,37/16",
            "3,3,CH AMALIA,1,37/16",
            "3,3,SAELAND,1,37/16",
            "3,4,AGGERSHAUS,1,37/16",
            "3,4,PROVESTEENEN,1,37/16",
            "3,4,WAGREN,1,37/16",
            "3,1,HLAELPEREN,2,35/14",
            "3,2,ELVEN,2,35/14",
            "3,3,ALBORG,2,35/14",
            "3,3,CRONBORG,2,35/14",
            "4,2,SAN JUSTO,1,18/41",
            "4,2,CORDOBA,1,18/41",
            "4,3,VESPUCCIO,1,18/41",
            "4,3,SANCHEZ,1,18/41",
            "4,3,S.F. DE ASIS,1,18/41",
            "4,3,SAN OMBLIGOS,1,18/41",
            "4,3,SAN JUAN,1,18/41",
            "4,3,MONARCA,1,18/41",
            "4,4,SAN AUGUSTIN,1,18/41",
            "4,4,BAHAMA,1,18/41",
            "4,4,NEPTUNO,1,18/41",
            "4,4,SANTA ANA,1,18/41",
            "4,5,S.TRINIDAD,1,18/41",
            "4,1,MATRODIAS,1,7/34",
            "4,2,SAN ILDEFONSO,1,7/34",
            "4,3,ARGONAUTA,1,7/34",
            "4,3,SAN LEANDRO,1,7/34",
            "4,4,SAN JUSTO,1,7/34",
            "4,4,RAYO,1,7/34",
            "4,1,CADEZ,2,22/11",
            "4,1,LAREDO,2,22/11",
            "4,2,ESPANIA,2,22/11",
            "4,3,CISNEROS,2,22/11",
            "4,4,MONTANEZ,2,22/11",
            "4,2,LEONARDO,3,40/15",
            "4,2,PHILLIPE,3,40/15",
            "4,3,MONARCO,3,40/15",
            "4,2,CORDEZ,3,40/15",
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
            "5,2,TOURVILLE,3,18/6",
            "5,3,BERWICK,3,18/6",
            "5,3,ARGONAUTE,3,18/6",
            "5,4,FOUGUEUX,3,18/6",
            "5,4,AIGLE,3,18/6",
            "6,2,ARROW,1,13/19",
            "6,2,HOLLY,1,13/19",
            "6,2,DILIGENT,1,13/19",
            "6,3,ROSE,1,13/19",
            "6,4,DEFENCE,1,13/19",
            "6,1,LAVIUS,1,22/11",
            "6,3,ARDENT,1,22/11",
            "6,4,CRESSY,1,22/11",
            "6,4,ORION,1,22/11",
            "6,5,VICTORY,1,22/11",
            "6,2,ROMULUS,1,22/11",
            "6,1,AQUILON,1,23/20",
            "6,3,MARTIN,1,23/20",
            "6,3,AGAMEMNON,1,23/20",
            "6,4,LEVIATHAN,1,23/20",
            "6,4,ORION,1,23/20",
            "6,4,TONNANT,1,23/20",
            "6,5,BRITANNIA,1,23/20",
            "6,1,CIRCE,2,24/8",
            "6,1,HERMES,2,24/8",
            "6,2,DRAKE,2,24/8",
            "6,3,ISIS,2,24/8",
            "6,3,KING GEORGE,2,24/8",
            "6,4,SPARTIATE,2,24/8",
            "6,4,THUNDERER,2,24/8",
            "6,4,PRINCE OF WALES,2,24/8",
            "6,3,SUSSEX,1,38/53",
            "6,4,MINOTAUR,1,38/53",
            "6,4,NEPTUNE,1,38/53",
            "6,5,ROYAL SOVEREIGN,1,38/53",
            "6,2,GLORY,1,47/47",
            "6,2,DART,1,47/47",
            "6,3,CENTURION,1,47/47",
            "6,3,HERCULES,1,47/47",
            "6,4,AJAX,1,47/47",
            "6,1,AMAZON,1,9/46",
            "6,2,HOTSPUR,1,9/46",
            "6,3,ACTIVE,1,9/46",
            "6,3,AFRICA,1,9/46",
            "6,3,POLYPHEMUS,1,9/46",
            "6,4,CONQUEROR,1,9/46",
            "6,4,DREADNOUGHT,1,9/46",
            "6,2,BLANCHE,3,12/19",
            "6,4,GALLAND,3,12/19",
            "6,4,REVENGE,3,12/19",
            "6,2,DIADEM,3,6/10",
            "6,2,ALEMENE,3,6/10",
            "6,3,SPECULATOR,3,6/10",
            "6,4,SWIFTSURE,3,6/10",
            "6,4,ELEPHANT,3,6/10",
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
            "7,1,HELDIN,3,25/26",
            "7,2,JUPITER,3,25/26",
            "7,3,ATALANTA,3,25/26",
            "7,3,MARS,3,25/26",
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
            "9,1,COIMBRA,1,5/41",
            "9,1,BELEM,1,5/41",
            "9,2,SAO PAULO,1,5/41",
            "9,2,RIO DE JANEIRO,1,5/41",
            "9,3,PORTO,1,5/41",
            "9,4,COVILHA,1,5/41",
            "9,4,P. HENRIQE,1,5/41",
            "9,4,DON PEDRO,1,5/41",
            "9,1,SALVADOR,2,40/27",
            "9,3,BEJA,2,40/27",
            "9,3,FARO,2,40/27",
            "9,1,RECIFE,3,5/5",
            "9,2,COIMBRA,3,5/5",
            "9,3,LISSABON,3,5/5",
            "10,11,MECHNEDIN,1,21/50",
            "10,11,BEY EL DJAR,1,21/50",
            "10,11,KILIDIR,1,21/50",
            "10,11,MEDIZZA,1,21/50",
            "10,12,BEDR IL ZAFAR,1,21/50",
            "10,12,EFREZZIN,1,21/50",
            "10,12,BURSAKEZZ,1,21/50",
            "10,2,DENYWID,1,5/55",
            "10,2,SARASHENDR,1,5/55",
            "10,2,HADUK,1,5/55",
            "10,2,MASURDINE,1,5/55",
            "10,3,RAHBAT I-ALAN,1,5/55",
            "10,11,KARRABESH,1,5/55",
            "10,11,SADD IL BUKIR,1,5/55",
            "10,11,JUSHURIK,1,5/55",
            "10,11,KABBASERIN,1,5/55",
            "10,11,SOLUM EL DARUK,1,5/55",
            "10,11,FURASALEH,1,5/55",
            "10,11,DJURIRR,1,5/55",
            "10,11,NAJUDIRNA,1,5/55",
            "10,12,HADJISHIN,1,5/55",
            "10,12,KIRR IBN SAUD,1,5/55",
            "10,12,ABDUALLA,1,5/55",
            "10,11,BAHRIFUR,3,4/18",
            "10,11,FUZIR,3,4/18",
            "10,11,METEL,3,4/18",
            "10,11,HAFIRUK,3,4/18",
            "10,12,JIRHAREZ,3,4/18",
            "10,12,IBN EL FARUK,3,4/18",
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
            "11,4,DEMESTICO,1,38/43",
            "11,1,MILANO,3,23/16",
            "11,2,SIZILIA,3,23/16",
            "11,2,ANCORA,3,23/16",
            "11,4,CONDIECIERI,3,23/16",
            "12,1,SPANDAU,1,48/16",
            "12,1,KÖNIG,1,48/16",
            "12,1,DANZIG,1,48/16",
            "12,1,BRESLAU,1,48/16",
            "12,2,POTSDAM,1,48/16",
            "12,2,KURFÜRST,1,48/16",
            "13,1,KORSHUN,1,58/6",
            "13,1,OBLENISSIMOW,1,58/6",
            "13,1,ALPATOW,1,58/6",
            "13,1,MODLASEWSKI,1,58/6",
            "13,2,MAZNEW,1,58/6",
            "13,2,ARTEMJEW,1,58/6",
            "13,3,BATJUSCHKA,1,58/6",
            "13,3,IWANOWITSCH,1,58/6",
            "13,3,MITJAKTIN,1,58/6",
            "13,3,LEDOCHCHOWSKI,1,58/6",
            "13,3,KOLJA,1,58/6",
            "13,3,ALESCHA,1,58/6",
            "13,3,MARKUSCHIN,1,58/6",
            "13,4,KORSHIKOW,1,58/6",
            "13,4,KOLJA,1,58/6",
            "13,4,FÜRST REPNIN,1,58/6",
            "13,5,IMP. ALEXANDER,1,58/6",
            "13,1,GEORGI,1,63/31",
            "13,1,TOMASCHOW,1,63/31",
            "13,1,PESTREZOW,1,63/31",
            "13,1,SANJEJEW,1,63/31",
            "13,2,BURJANOW,1,63/31",
            "13,2,KISLODOWSK,1,63/31",
            "13,3,ALEXEJ,1,63/31",
            "13,3,PONOWIE,1,63/31",
            "13,3,SSLABIN,1,63/31",
            "13,4,CHARTSCHENKO,1,63/31",
            "13,4,SKATSHKOW,1,63/31",
            "13,4,TWARDY,1,63/31",
            "13,4,DIMITRI,1,63/31",
            "14,1,PROSTETTEN,1,43/10",
            "14,1,OELSON,1,43/10",
            "14,2,TJÖLDE,1,43/10",
            "14,2,RENDSBORG,1,43/10",
            "14,3,HELSINGJÖR,1,43/10",
            "14,4,WASA,1,43/10",
            "14,4,SJEALLA,1,43/10",
            "14,4,MALMÖ,1,43/10",
            "14,1,HOLDARBORG,3,23/18",
            "14,1,ARLANDA,3,23/18",
            "14,2,WAGRIEN,3,23/18",
            "14,3,CROENBORG,3,23/18",
            "14,3,TAMANIUM,3,23/18",
            "15,1,BAHRIS,1,58/40",
            "15,1,ALI BERIK,1,58/40",
            "15,2,RUDALLA,1,58/40",
            "15,2,BISHAREK,1,58/40",
            "15,3,KUSSAIR,1,58/40",
            "15,4,BEN HADEK,1,58/40",
            "15,4,MELIKE,1,58/40",
            "15,4,HADAR NUR,1,58/40",
            "15,5,HASAN,1,58/40",
            "15,11,TÖLGÖL,1,58/40",
            "15,12,MEDINE,1,58/40",
            "15,1,HUSSIEN FASUD,1,72/49",
            "15,3,DSHADAHLAH,1,72/49",
            "15,4,MAZUR,1,72/49",
            "15,11,OZOGÖL,1,72/49",
            "15,11,NESSIMEH,1,72/49",
            "16,1,BREZINKI,1,51/13",
            "16,1,MIRJANZ,1,51/13",
            "16,1,RADOMSK,1,51/13",
            "16,1,PODZIN,1,51/13",
            "16,2,SALDANZ,1,51/13",
            "16,2,PIDSULSKI,1,51/13",
            "16,2,ZULEWSKI,1,51/13",
            "16,3,WRALOC,1,51/13",
            "17,1,GUERRIERE,1,59/59",
            "17,1,LEONE,1,59/59",
            "17,2,FEVZ NUSRA,1,59/59",
            "17,2,KA'ID ZAFE,1,59/59",
            "17,3,TARIQ,1,59/59",
            "17,3,EL HORREYA,1,59/59",
            "17,3,DOMYAT,1,59/59",
            "17,3,TABA,1,59/59",
            "17,4,GHIUH REWAN,1,59/59",
            "17,11,FAHTI BAHRI,1,59/59",
            "17,11,BURJ ZAFER,1,59/59",
            "17,11,IHSANYA,1,59/59",
            "17,12,SURYA,1,59/59",
            "17,12,AL BURULLUS,1,59/59",
            "17,12,AL GADAR,1,59/59",
            "17,12,ABU EL GHOSN,1,59/59",
            "17,12,AL GADAR,1,59/59"
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
