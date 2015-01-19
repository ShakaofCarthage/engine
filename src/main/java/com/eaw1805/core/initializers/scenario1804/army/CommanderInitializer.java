package com.eaw1805.core.initializers.scenario1804.army;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.RegionConstants;
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
            "1,Major-General,41/29,1,3,12",
            "1,Brigadier,38/33,1,2,8",
            "2,Lieutenant General,35/27,1,4,27",
            "2,Major-General,33/19,1,3,18",
            "2,Major-General,35/27,1,2,9",
            "3,Lieutenant General,37/16,1,4,21",
            "3,Brigadier,37/16,1,2,8",
            "4,Field Marshal,13/41,1,4,24",
            "4,Brigadier,18/41,1,2,6",
            "5,Field Marshal,23/27,1,5,35",
            "5,Lieutenant General,27/36,1,4,27",
            "5,Major-General,23/27,1,3,12",
            "5,Major-General,13/4,2,3,11",
            "6,Field Marshal,23/20,1,4,30",
            "6,Lieutenant General,23/20,1,4,21",
            "7,Lieutenant General,28/20,1,4,22",
            "7,Major-General,28/20,1,3,20",
            "8,Lieutenant General,35/40,1,4,23",
            "8,Brigadier,35/40,1,3,11"
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

            // Adjust position due to smaller European map
            if (thisPosition.getRegion().getId() == RegionConstants.EUROPE) {
                thisPosition.setX(thisPosition.getX() - 12);
                thisPosition.setY(thisPosition.getY() - 14);
            }

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
