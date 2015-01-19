package com.eaw1805.core.initializers.scenario1802.army;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CommanderNameManager;
import com.eaw1805.data.managers.army.RankManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.CommanderName;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Initializes Commander objects.
 */
public class CommanderInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CommanderInitializer.class);

    /**
     * The date of the 4 records.
     * "Nation ID,Rank,X/Y,Region,StrC,ComC",
     */
    private static final String[] DATA = {
            "1,Field Marshal,41/29,1,5,31",
            "1,Lieutenant General,52/33,1,4,22",
            "1,Major-General,41/29,1,3,12",
            "1,Brigadier,38/33,1,2,8",
            "2,Lieutenant General,35/27,1,4,27",
            "2,Major-General,33/19,1,3,18",
            "2,Major-General,35/27,1,2,9",
            "3,Lieutenant General,37/16,1,4,21",
            "3,Major-General,31/12,1,3,16",
            "3,Brigadier,37/16,1,2,8",
            "4,Field Marshal,13/41,1,4,24",
            "4,Lieutenant General,07/34,1,3,19",
            "4,Major-General,09/15,2,3,11",
            "4,Brigadier,18/41,1,2,6",
            "5,Field Marshal,23/27,1,5,35",
            "5,Lieutenant General,27/36,1,4,27",
            "5,Major-General,23/27,1,3,12",
            "5,Major-General,13/4,2,3,11",
            "6,Field Marshal,23/20,1,4,30",
            "6,Lieutenant General,23/20,1,4,21",
            "6,Major-General,21/5,3,2,10",
            "6,Major-General,13/19,1,2,9",
            "7,Lieutenant General,28/20,1,4,22",
            "7,Major-General,28/20,1,3,20",
            "7,Brigadier,34/23,2,3,11",
            "7,Brigadier,25/26,3,2,10",
            "8,Lieutenant General,35/40,1,4,23",
            "8,Major-General,31/32,1,3,12",
            "8,Brigadier,35/40,1,3,11",
            "9,Lieutenant General,05/41,1,4,21",
            "9,Major-General,05/41,1,3,18",
            "9,Brigadier,40/27,2,3,14",
            "10,Lieutenant General,05/55,1,4,22",
            "10,Major-General,21/50,1,3,13",
            "10,Brigadier,21/50,1,2,9",
            "10,Brigadier,05/55,1,2,8",
            "11,Lieutenant General,38/43,1,4,26",
            "11,Major-General,38/59,1,3,17",
            "11,Brigadier,38/43,1,2,10",
            "12,Field Marshal,39/22,1,5,31",
            "12,Lieutenant General,48/16,1,4,22",
            "12,Major-General,39/22,1,3,18",
            "12,Major-General,48/16,1,3,14",
            "13,Field Marshal,58/06,1,5,31",
            "13,Lieutenant General,69/17,1,4,21",
            "13,Major-General,63/31,1,3,12",
            "13,Brigadier,58/06,1,2,6",
            "14,Lieutenant General,43/10,1,4,22",
            "14,Major-General,55/04,1,3,13",
            "14,Brigadier,43/10,1,2,10",
            "15,Lieutenant General,58/40,1,5,31",
            "15,Major-General,72/39,1,4,21",
            "15,Major-General,72/49,1,3,12",
            "15,Brigadier,58/40,1,2,7",
            "15,Brigadier,51/48,1,2,6",
            "16,Major-General,59/23,1,4,30",
            "16,Lieutenant General,52/20,1,3,16",
            "16,Major-General,52/20,1,3,14",
            "16,Major-General,51/13,1,2,8",
            "17,Lieutenant General,63/61,1,4,22",
            "17,Major-General,63/61,1,3,16",
            "17,Brigadier,63/61,1,2,10",
            "17,Brigadier,58/60,1,2,8"
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default constructor.
     */
    public CommanderInitializer() {
        super();
        LOGGER.debug("CommanderInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Commander> records = CommanderManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("CommanderInitializer invoked.");

        final Game game = GameManager.getInstance().getByID(-1);

        // 1st commander is the unknown commander
        CommanderManager.getInstance().addNegativeCommanderID();

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);

        // Initialize records
        int lastNationId = 0;
        List<CommanderName> names = CommanderNameManager.getInstance().list();
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Commander thisCommander = new Commander(); //NOPMD
            final StringTokenizer thisStk = new StringTokenizer(DATA[i], ","); // NOPMD

            final int nationId = Integer.parseInt(thisStk.nextToken()); // NOPMD
            boolean isSupreme = false;
            if (nationId != lastNationId) {
                names = CommanderNameManager.getInstance().listNation(NationManager.getInstance().getByID(nationId));
                lastNationId = nationId;
                isSupreme = true;
            }

            thisCommander.setNation(NationManager.getInstance().getByID(nationId));
            thisCommander.setCaptured(NationManager.getInstance().getByID(nationId));
            thisCommander.setRank(RankManager.getInstance().getByName(thisStk.nextToken()));

            // Find name from CommanderName list
            final CommanderName thisName = names.remove(0);
            thisCommander.setName(thisName.getName());
            thisCommander.setIntId(thisName.getPosition());

            // Fix position
            final Position thisPosition = new Position(); // NOPMD
            final StringTokenizer thisPositionStk = new StringTokenizer(thisStk.nextToken(), "/"); // NOPMD
            thisPosition.setX(Integer.parseInt(thisPositionStk.nextToken()) - 1);
            thisPosition.setY(Integer.parseInt(thisPositionStk.nextToken()) - 1);
            thisPosition.setRegion(RegionManager.getInstance().getByID(Integer.parseInt(thisStk.nextToken())));
            thisPosition.setGame(game);
            thisCommander.setPosition(thisPosition);

            thisCommander.setArmy(0);
            thisCommander.setCorp(0);
            thisCommander.setMps(80);
            thisCommander.setStrc(Integer.parseInt(thisStk.nextToken()));
            thisCommander.setComc(Integer.parseInt(thisStk.nextToken()));
            thisCommander.setDead(false);
            thisCommander.setPool(false);
            thisCommander.setSupreme(isSupreme);
            thisCommander.setSick(0);
            thisCommander.setCarrierInfo(emptyCarrierInfo);

            thisCommander.setCavalryLeader(false);
            thisCommander.setArtilleryLeader(false);
            thisCommander.setStoutDefender(false);
            thisCommander.setFearlessAttacker(false);
            thisCommander.setLegendaryCommander(false);

            if (isSupreme) {
                // The commander with the highest tactical rating acts as the Supreme Field Marshal with General Staff.
                // His strategic rating gets a +1 bonus and his tactical rating a +10 bonus.
                thisCommander.setStrc(thisCommander.getStrc() + 1);
                thisCommander.setComc(thisCommander.getComc() + 10);
            }

            CommanderManager.getInstance().add(thisCommander);
        }

        LOGGER.info("CommanderInitializer complete.");
    }

}

