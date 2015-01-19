package com.eaw1805.core.initializers;

import com.eaw1805.algorithms.FogOfWarInspector;
import com.eaw1805.core.EmailManager;
import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.GameConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.TradeConstants;
import com.eaw1805.data.managers.EngineProcessManager;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.EngineProcess;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.map.MapCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.quartz.CronExpression;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Initializes a new game.
 */
public class GameInitializer
        extends AbstractThreadedInitializer
        implements RegionConstants, GoodConstants, TradeConstants, GameConstants, ReportConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(GameInitializer.class);

    /**
     * The instance of the game engine.
     */
    private final transient GameEngine caller;

    /**
     * The identity of the game object to initialize.
     */
    private transient int gameId;

    /**
     * The identity of the scenario object to initialize.
     */
    private transient int scenarioId;

    /**
     * The execution number.
     */
    private transient int execID;

    /**
     * Total sectors of each nation per region.
     */
    private transient int[][] totSectors;

    /**
     * Total population of each nation per region.
     */
    private transient int[][] totPopulation;

    /**
     * Total maintenance cost of army per nation.
     */
    private transient int[] totArmy;

    /**
     * Total maintenance cost of ships per nation.
     */
    private transient int[] totNavy;

    /**
     * Total goods of each nation per region (i.e. per warehouse).
     */
    private transient int[][][] totGoods;

    /**
     * GameInitializer's constructor that sets the gameId.
     *
     * @param identity the identity of the game object to initialize.
     * @param parent   the instance of the game engine that invoked us.
     */
    public GameInitializer(final GameEngine parent, final int identity, final int scenario, final int execution) {
        super();
        gameId = identity;
        execID = execution;
        scenarioId = scenario;
        caller = parent;

        LOGGER.debug("GameInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        final Game scenarioGame = GameManager.getInstance().getByID(gameId);
        theTrans.rollback();
        return (scenarioGame == null
                || scenarioGame.getTurn() < -1
                || GAME_CREATE.equals(scenarioGame.getStatus()));
    }

    public void initializeStatistics(List<Nation> lstNations, List<Region> lstRegions) {
        // Initialize statistics.

        totSectors = new int[NationConstants.NATION_LAST + 1][RegionConstants.REGION_LAST + 1];
        totPopulation = new int[NationConstants.NATION_LAST + 1][RegionConstants.REGION_LAST + 1];
        totGoods = new int[NationConstants.NATION_LAST + 1][RegionConstants.REGION_LAST + 1][GoodConstants.GOOD_CP + 1];
        totArmy = new int[NationConstants.NATION_LAST + 1];
        totNavy = new int[NationConstants.NATION_LAST + 1];
    }

    /**
     * Initializes the database for the game with gameId.
     */
    public void initialize() {
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);

        // Check if a game instance is already available
        final Game newGame;
        if (GameManager.getInstance().getByID(gameId) == null) {
            final Game scenarioGame = GameManager.getInstance().getByID(-1);

            // Construct new game instance.
            newGame = new Game();
            newGame.setGameId(gameId);
            newGame.setScenarioId(scenarioId);
            newGame.setTurn(-1);
            newGame.setEnded(false);
            newGame.setWinners("");
            newGame.setSchedule(scenarioGame.getSchedule());
            newGame.setDiscount(0);
            newGame.setStatus(GAME_PROC);
            newGame.setForumId(2);
            newGame.setUserId(2);
            newGame.setCronSchedule("");
            newGame.setCronScheduleDescr("");
            newGame.setType(GameConstants.DURATION_NORMAL);
            newGame.setFogOfWar(scenarioGame.isFogOfWar());
            newGame.setRandomEvents(scenarioGame.isRandomEvents());
            newGame.setFieldBattle(scenarioGame.isFieldBattle());
            newGame.setBoostedTaxation(scenarioGame.isBoostedTaxation());
            newGame.setBoostedProduction(scenarioGame.isBoostedProduction());
            newGame.setFastPopulationGrowth(scenarioGame.isFastPopulationGrowth());
            newGame.setBoostedCAPoints(scenarioGame.isBoostedCAPoints());
            newGame.setFierceCasualties(scenarioGame.isFierceCasualties());
            newGame.setFastAppointmentOfCommanders(scenarioGame.isFastAppointmentOfCommanders());
            newGame.setExtendedArrivalOfCommanders(scenarioGame.isExtendedArrivalOfCommanders());
            newGame.setFullMpsAtColonies(scenarioGame.isFullMpsAtColonies());
            newGame.setAlwaysSummerWeather(scenarioGame.isAlwaysSummerWeather());
            newGame.setFastShipConstruction(scenarioGame.isFastShipConstruction());
            newGame.setExtendedEspionage(scenarioGame.isExtendedEspionage());
            newGame.setFastFortressConstruction(scenarioGame.isFastFortressConstruction());
            newGame.setRumorsEnabled(scenarioGame.isRumorsEnabled());

            // Store object to DB.
            GameManager.getInstance().add(newGame);

            gameId = newGame.getGameId();
        } else {
            newGame = GameManager.getInstance().getByID(gameId);
            newGame.setStatus(GAME_PROC);
            newGame.setTurn(-1);

            // Store object to DB.
            GameManager.getInstance().update(newGame);
        }

        LOGGER.info("Initializing new Game [" + gameId + "]");

        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        final EngineProcess thisProc = new EngineProcess();
        thisProc.setGameId(gameId);
        thisProc.setScenarioId(scenarioId);
        thisProc.setTurn(-1);
        thisProc.setDateStart(new Date());
        thisProc.setDuration(-1);
        thisProc.setBuildNumber(execID);
        thisProc.setProcessName(ManagementFactory.getRuntimeMXBean().getName());
        EngineProcessManager.getInstance().add(thisProc);
        final long tsStart = System.currentTimeMillis();
        theTrans.commit();

        // Copy data from Game ID 0 to new Game ID
        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final Game scenarioGame = GameManager.getInstance().getByID(-1);
        final List<Nation> lstNations = NationManager.getInstance().list();
        final List<Region> lstRegions = RegionManager.getInstance().list();
        initializeStatistics(lstNations, lstRegions);

        // Warehouses
        initWarehouse(newGame, scenarioGame);

        // Nation Relations
        initNationsRelation(newGame, scenarioGame);

        // Sector
        initSector(newGame, scenarioGame);

        // Barracks
        initBarracks(newGame, scenarioGame);

        // Commander
        initCommander(newGame, scenarioGame);

        // Brigade
        initBrigade(newGame, scenarioGame);

        // Corps
        initCorps(newGame, scenarioGame);

        // Spy
        initSpy(newGame, scenarioGame);

        // Ship
        initShip(newGame, scenarioGame);

        // fleets
        initFleets(newGame, scenarioGame);

        // Baggage Trains
        initBaggageTrain(newGame, scenarioGame);

        // Trade Cities
        initTradeCities(newGame, scenarioGame);

        // Report initial statistics
        for (final Nation thisNation : lstNations) {
            if (thisNation.getId() > 0) {
                reportInitStatistics(newGame, thisNation, totSectors[thisNation.getId()],
                        totPopulation[thisNation.getId()],
                        totGoods[thisNation.getId()],
                        totArmy[thisNation.getId()],
                        totNavy[thisNation.getId()]);
            } else {
                reportInitStatistics(newGame, thisNation, totSectors[0], totPopulation[0], totGoods[0], 0, 0);
            }
        }

        // Report initial weather
        final Nation freeNation = NationManager.getInstance().getByID(-1);
        report(newGame, freeNation, "winter.arctic", 0);
        report(newGame, freeNation, "winter.central", 0);
        report(newGame, freeNation, "winter.mediterranean", 0);

        // create user-game entries
        if (scenarioId > HibernateUtil.DB_MAIN) {
            lstNations.remove(0);
            for (final Nation nation : lstNations) {
                if (UserGameManager.getInstance().list(newGame, nation).isEmpty()) {
                    //if not has been already initialized
                    final UserGame entry = new UserGame();
                    entry.setCurrent(true);
                    entry.setHasWon(false);
                    entry.setActive(false);
                    entry.setAlive(true);
                    entry.setNation(nation);
                    entry.setCost(nation.getCost());
                    entry.setUserId(2); // Admin
                    entry.setAccepted(true); // true for admin
                    entry.setGame(newGame);
                    entry.setOffer(0);
                    entry.setTurnFirstLoad(true);
                    UserGameManager.getInstance().add(entry);
                }
            }
        }

        // Update timestamps
        newGame.setDateStart(new Date());
        newGame.setDateLastProc(new Date());
        newGame.setTurn(0);
        newGame.setStatus(GAME_WAIT);

        // Produce images
        produceImages(newGame, caller.getBasePath());

        // Solo games should be ready for process
        if (scenarioId < HibernateUtil.DB_MAIN) {
            newGame.setStatus(GAME_READY);
        }

        // Identify date of next processing
        final Calendar nextTurn = Calendar.getInstance();
        nextTurn.set(Calendar.HOUR_OF_DAY, 0);
        nextTurn.set(Calendar.MINUTE, 0);
        nextTurn.set(Calendar.SECOND, 0);

        if (newGame.getSchedule() > 0) {
            // Day-based periodic schedule
            nextTurn.add(Calendar.DATE, newGame.getSchedule());

        } else {
            // Custom schedule
            try {
                final CronExpression cexp = new CronExpression(newGame.getCronSchedule());
                nextTurn.setTime(cexp.getNextValidTimeAfter(new Date()));

            } catch (Exception ex) {
                LOGGER.error(ex);
                LOGGER.info("Setting next processing date after 7 days.");
                nextTurn.add(Calendar.DATE, 7);
            }
        }
        newGame.setDateNextProc(nextTurn.getTime());

        GameManager.getInstance().update(newGame);
        caller.setGame(newGame);

        // Send mail to user if this is a solo game
        final int userId;
        if (scenarioId < HibernateUtil.DB_MAIN) {
            // retrieve userID of game
            final List<UserGame> lstUsers = UserGameManager.getInstance().list(newGame);
            if (lstUsers.isEmpty()) {
                userId = 2;

            } else {
                userId = lstUsers.get(0).getUserId();
            }

        } else {
            userId = 2;
        }

        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        thisProc.setDuration((int) ((System.currentTimeMillis() - tsStart) / 1000d));
        EngineProcessManager.getInstance().update(thisProc);

        // Send mail to user if this is a solo game
        if (scenarioId < HibernateUtil.DB_MAIN) {
            // retrieve user
            final User thisUser = UserManager.getInstance().getByID(userId);

            // Send mail to user
            sendMail(newGame, thisUser);

            LOGGER.info("New Solo Game [" + gameId + "] initialized.");

        } else {
            LOGGER.info("New Game [" + gameId + "] initialized.");
        }

        theTrans.commit();
    }

    private void sendMail(final Game game, final User user) {
        // Construct the 1st attachment
        final String filePath1 = caller.getBasePath() + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.EUROPE + "-small.png";

        // Construct the 3rd attachment
        final String filePath3 = caller.getBasePath() + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.CARIBBEAN + "-small.png";

        // Construct the body of the mail
        final StringBuilder mailBody = new StringBuilder();
        mailBody.append("Dear ");
        mailBody.append(user.getUsername());
        mailBody.append(",\n\n");
        mailBody.append("Your solo game is ready!\n\n");

        mailBody.append("You can play your game from your EaW home:\n\n");
        mailBody.append("http://www.eaw1805.com/games\n\n");

        mailBody.append("Click on the play button enter the game - or follow the link:\n\n");

        mailBody.append("http://www.eaw1805.com/play/scenario/1804/game/");
        mailBody.append(game.getGameId());
        mailBody.append("/nation/5\n\n");

        mailBody.append("You can find valuable information about the game in the Quick Start Guide:\n\n");
        mailBody.append("http://www.eaw1805.com/help/introduction\n\n");

        mailBody.append("Detailed information about the game rules and the mechanisms of Empires at War 1805 are available in the Players' Handbook:\n\n");
        mailBody.append("http://www.eaw1805.com/handbook\n\n");

        mailBody.append("For questions about the rules and the user interface, we suggest that you use the forums where all players post their comments along with replies from the GMs:\n\n");
        mailBody.append("http://forum.eaw1805.com\n\n");

        // Closing
        mailBody.append("Enjoy your game,\nOplon Games");

        // Send mails to all active players.
        EmailManager.getInstance().sendMail1804(user, "[EaW1805] Solo-Game created",
                mailBody.toString(), filePath1, filePath3);

        // Send mail to administrators
        EmailManager.getInstance().sendEmail("admin@eaw1805.com", "[EaW1805] New Solo-Game for " + user.getUsername(), mailBody.toString(),
                filePath1, null, filePath3, null);
    }

    /**
     * Initializes the warehouses.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initWarehouse(final Game newGame, final Game scenarioGame) {
        final List<Warehouse> listWH = WarehouseManager.getInstance().listByGame(scenarioGame);
        for (final Warehouse thisWH : listWH) {
            initWarehouse(newGame, thisWH);
        }
    }

    /**
     * Initialize the warehouse objects.
     *
     * @param newGame the game.
     * @param thisWH  the new warehouse object.
     */
    private void initWarehouse(final Game newGame, final Warehouse thisWH) {
        final Warehouse newWH = new Warehouse();
        newWH.setGame(newGame);
        newWH.setNation(thisWH.getNation());
        newWH.setRegion(thisWH.getRegion());
        final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
        for (Integer key : thisWH.getStoredGoodsQnt().keySet()) {
            qteGoods.put(key, thisWH.getStoredGoodsQnt().get(key));

            // Update statistics
            totGoods[thisWH.getNation().getId()][thisWH.getRegion().getId()][key] = thisWH.getStoredGoodsQnt().get(key);
        }
        newWH.setStoredGoodsQnt(qteGoods);
        WarehouseManager.getInstance().add(newWH);
    }

    /**
     * Initializes the nations relations.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initNationsRelation(final Game newGame, final Game scenarioGame) {
        final List<NationsRelation> listNR = RelationsManager.getInstance().listByGame(scenarioGame);
        for (final NationsRelation thisNR : listNR) {
            initNationRelation(newGame, thisNR);
        }
    }

    /**
     * Initialze the particular nations relation.
     *
     * @param newGame the game object.
     * @param thisNR  the new object.
     */
    private void initNationRelation(final Game newGame, final NationsRelation thisNR) {
        final NationsRelation newNR = new NationsRelation();
        newNR.setGame(newGame);
        newNR.setNation(thisNR.getNation());
        newNR.setTarget(thisNR.getTarget());
        newNR.setRelation(thisNR.getRelation());
        newNR.setPrisoners(thisNR.getPrisoners());
        RelationsManager.getInstance().add(newNR);
    }

    /**
     * Initializes the sectors.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initSector(final Game newGame, final Game scenarioGame) {
        final List<Sector> listSec = SectorManager.getInstance().listByGame(scenarioGame);
        for (final Sector thisSec : listSec) {
            initSector(newGame, thisSec);

            // Update statistics
            if (thisSec.getNation().getId() > 0) {
                totSectors[thisSec.getNation().getId()][thisSec.getPosition().getRegion().getId()]++;
                totPopulation[thisSec.getNation().getId()][thisSec.getPosition().getRegion().getId()] += thisSec.populationCount();

            } else {
                totSectors[0][thisSec.getPosition().getRegion().getId()]++;
                totPopulation[0][thisSec.getPosition().getRegion().getId()] += thisSec.populationCount();
            }
        }
    }

    /**
     * Initialize this particular sector.
     *
     * @param newGame the object of the new game.
     * @param thisSec the new sector object.
     */
    private void initSector(final Game newGame, final Sector thisSec) {
        final Sector newSec = new Sector();
        newSec.setNation(thisSec.getNation());
        newSec.setConqueredCounter(thisSec.getConqueredCounter());
        newSec.setBuildProgress(thisSec.getBuildProgress());
        newSec.setNaturalResource(thisSec.getNaturalResource());
        newSec.setPopulation(thisSec.getPopulation());
        newSec.setPoliticalSphere(thisSec.getPoliticalSphere());
        newSec.setProductionSite(thisSec.getProductionSite());
        newSec.setTradeCity(thisSec.getTradeCity());
        newSec.setEpidemic(thisSec.getEpidemic());
        newSec.setRebelled(thisSec.getRebelled());
        newSec.setPayed(thisSec.getPayed());
        newSec.setConquered(thisSec.getConquered());
        newSec.setTerrain(thisSec.getTerrain());
        newSec.setName(thisSec.getName());
        newSec.setFow("*");
        newSec.setImage("");
        newSec.setImageGeo(thisSec.getImageGeo());

        final Position newPos = new Position(); // NOPMD
        newPos.setX(thisSec.getPosition().getX());
        newPos.setY(thisSec.getPosition().getY());
        newPos.setRegion(thisSec.getPosition().getRegion());
        newPos.setGame(newGame);
        newSec.setPosition(newPos);

        SectorManager.getInstance().add(newSec);
    }

    /**
     * Initializes the barracks.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initBarracks(final Game newGame, final Game scenarioGame) {
        final List<Barrack> listBar = BarrackManager.getInstance().listByGame(scenarioGame);
        for (final Barrack barrack : listBar) {
            initBarrack(newGame, barrack);
        }
    }

    /**
     * Initialize this barrack object.
     *
     * @param newGame the game object.
     * @param thisBar the new barrack object.
     */
    private void initBarrack(final Game newGame, final Barrack thisBar) {
        final Barrack newBar = new Barrack();
        newBar.setNation(thisBar.getNation());
        newBar.setNotSupplied(false);

        final Position newPos = new Position(); // NOPMD
        newPos.setX(thisBar.getPosition().getX());
        newPos.setY(thisBar.getPosition().getY());
        newPos.setRegion(thisBar.getPosition().getRegion());
        newPos.setGame(newGame);
        newBar.setPosition(newPos);

        BarrackManager.getInstance().add(newBar);
    }

    /**
     * Initializes the commanders.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initCommander(final Game newGame, final Game scenarioGame) {
        final List<Commander> listCOM = CommanderManager.getInstance().listByGame(scenarioGame);
        for (final Commander thisCOM : listCOM) {
            initCommander(newGame, thisCOM);
        }
    }

    /**
     * Initialize this commander object.
     *
     * @param newGame the game object.
     * @param thisCOM the new commander object.
     */
    private void initCommander(final Game newGame, final Commander thisCOM) {
        final Commander newCOM = new Commander();
        newCOM.setNation(thisCOM.getNation());
        newCOM.setCaptured(thisCOM.getNation());
        newCOM.setComc(thisCOM.getComc());
        newCOM.setStrc(thisCOM.getStrc());
        newCOM.setCorp(0);
        newCOM.setMps(thisCOM.getMps());
        newCOM.setName(thisCOM.getName());
        newCOM.setRank(thisCOM.getRank());
        newCOM.setIntId(thisCOM.getIntId());
        newCOM.setDead(thisCOM.getDead());
        newCOM.setPool(thisCOM.getPool());
        newCOM.setSupreme(thisCOM.getSupreme());
        newCOM.setSick(thisCOM.getSick());

        final Position newPos = new Position();
        newPos.setX(thisCOM.getPosition().getX());
        newPos.setY(thisCOM.getPosition().getY());
        newPos.setRegion(thisCOM.getPosition().getRegion());
        newPos.setGame(newGame);
        newCOM.setPosition(newPos);

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);
        newCOM.setCarrierInfo(emptyCarrierInfo);

        newCOM.setCavalryLeader(thisCOM.getCavalryLeader());
        newCOM.setArtilleryLeader(thisCOM.getArtilleryLeader());
        newCOM.setStoutDefender(thisCOM.getStoutDefender());
        newCOM.setFearlessAttacker(thisCOM.getFearlessAttacker());
        newCOM.setLegendaryCommander(thisCOM.getLegendaryCommander());

        CommanderManager.getInstance().add(newCOM);
    }

    public void initCorps(final Game newGame, final Game scenarioGame) {
        final List<Corp> listCR = CorpManager.getInstance().listFreeByGame(scenarioGame);
        for (Corp corp : listCR) {
            Corp newCorp = new Corp();
            newCorp.setMps(corp.getMps());
            if (corp.getCommander() != null) {
                newCorp.setCommander(CommanderManager.getInstance().getByGameNationIntId(newGame, corp.getNation(), corp.getCommander().getIntId()).get(0));
            }
            newCorp.setNation(corp.getNation());
            newCorp.setName(corp.getName());
            Position newPos = (Position) corp.getPosition().clone();
            newPos.setGame(newGame);
            newCorp.setPosition(newPos);
            newCorp.setArmy(null);
            newCorp.setBrigades(new HashSet<Brigade>());
            CorpManager.getInstance().add(newCorp);
            int corpId = newCorp.getCorpId();
            for (Brigade thisBR : corp.getBrigades()) {
                final Brigade newBR = new Brigade();
                newBR.setNation(thisBR.getNation());
                newBR.setCorp(corpId);
                newBR.setMps(thisBR.getMps());
                newBR.setName(thisBR.getName());
                newBR.setPosition((Position) newPos.clone());

                final FieldBattlePosition fbPos = new FieldBattlePosition();
                fbPos.setPlaced(false);
                fbPos.setX(0);
                fbPos.setY(0);
                newBR.setFieldBattlePosition(fbPos);

                final HashSet<Battalion> setBat = new HashSet<Battalion>();
                for (Battalion thisBat : thisBR.getBattalions()) {
                    initBattalion(setBat, thisBat);

                    // update statistics
                    totArmy[thisBR.getNation().getId()] += thisBat.getType().getCost();
                }
                newBR.setBattalions(setBat);
                newBR.setFromInit(true);
                BrigadeManager.getInstance().add(newBR);
                newCorp.getBrigades().add(newBR);
            }
            //be sure to update the commander to the correct army/corp ids
            if (newCorp.getCommander() != null) {
                newCorp.getCommander().setArmy(0);
                newCorp.getCommander().setCorp(corpId);
                CommanderManager.getInstance().update(newCorp.getCommander());
            }
            CorpManager.getInstance().update(newCorp);
        }
    }

    /**
     * Initializes the brigades.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    public void initBrigade(final Game newGame, final Game scenarioGame) {
        final List<Brigade> listBR = BrigadeManager.getInstance().listFreeByGame(scenarioGame);
        for (final Brigade thisBR : listBR) {
            initBrigade(newGame, thisBR);
        }
    }

    /**
     * Initialize the particular brigade object.
     *
     * @param newGame the game object.
     * @param thisBR  the brigade object.
     */
    private void initBrigade(final Game newGame, final Brigade thisBR) {
        final Brigade newBR = new Brigade();
        newBR.setNation(thisBR.getNation());
        newBR.setCorp(thisBR.getCorp());
        newBR.setMps(thisBR.getMps());
        newBR.setName(thisBR.getName());

        final Position newPos = new Position();
        newPos.setX(thisBR.getPosition().getX());
        newPos.setY(thisBR.getPosition().getY());
        newPos.setRegion(thisBR.getPosition().getRegion());
        newPos.setGame(newGame);
        newBR.setPosition(newPos);

        final FieldBattlePosition fbPos = new FieldBattlePosition();
        fbPos.setPlaced(false);
        fbPos.setX(0);
        fbPos.setY(0);
        newBR.setFieldBattlePosition(fbPos);

        final HashSet<Battalion> setBat = new HashSet<Battalion>();
        for (Battalion thisBat : thisBR.getBattalions()) {
            initBattalion(setBat, thisBat);

            // update statistics
            totArmy[thisBR.getNation().getId()] += thisBat.getType().getCost();
        }
        newBR.setBattalions(setBat);
        newBR.setFromInit(true);
        newBR.setArmType("");
        newBR.setFormation("");

        BrigadeManager.getInstance().add(newBR);
    }

    /**
     * Initialize the particular battalion object.
     *
     * @param setBat  the battalion set.
     * @param thisBat the particular battalion.
     */
    private void initBattalion(final Set<Battalion> setBat, final Battalion thisBat) {
        final Battalion newBat = new Battalion();
        newBat.setType(thisBat.getType());
        newBat.setExperience(thisBat.getExperience());
        newBat.setHeadcount(thisBat.getHeadcount());
        newBat.setOrder(thisBat.getOrder());
        newBat.setHasMoved(thisBat.getHasMoved());
        newBat.setNotSupplied(thisBat.getNotSupplied());
        newBat.setHasLost(thisBat.getHasLost());

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);
        newBat.setCarrierInfo(emptyCarrierInfo);

        setBat.add(newBat);
    }

    /**
     * Initializes the spies.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initSpy(final Game newGame, final Game scenarioGame) {
        final List<Spy> listSpies = SpyManager.getInstance().listByGame(scenarioGame);
        for (final Spy thisSpy : listSpies) {
            initSpy(newGame, thisSpy);
        }
    }

    /**
     * Initialize this spy object.
     *
     * @param newGame the game object.
     * @param thisSpy the new spy object.
     */
    private void initSpy(final Game newGame, final Spy thisSpy) {
        final Spy newSPY = new Spy();
        newSPY.setNation(thisSpy.getNation());
        newSPY.setName(thisSpy.getName());

        final Position newPos = new Position();
        newPos.setX(thisSpy.getPosition().getX());
        newPos.setY(thisSpy.getPosition().getY());
        newPos.setRegion(thisSpy.getPosition().getRegion());
        newPos.setGame(newGame);
        newSPY.setPosition(newPos);

        newSPY.setColonial(thisSpy.getColonial());
        newSPY.setStationary(0);
        newSPY.setReportBattalions("");
        newSPY.setReportBrigades("");
        newSPY.setReportShips("");
        newSPY.setReportTrade("");
        newSPY.setReportRelations(thisSpy.getNation().getId());

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);
        newSPY.setCarrierInfo(emptyCarrierInfo);

        SpyManager.getInstance().add(newSPY);
    }

    /**
     * Initializes the ships.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initShip(final Game newGame, final Game scenarioGame) {
        final List<Ship> listSP = ShipManager.getInstance().listFreeByGame(scenarioGame);
        for (final Ship thisSP : listSP) {
            final Ship newSP = new Ship(); // NOPMD
            newSP.setNation(thisSP.getNation());
            newSP.setCondition(thisSP.getCondition());
            newSP.setFleet(thisSP.getFleet());
            newSP.setMarines(thisSP.getMarines());
            newSP.setExp(thisSP.getExp());
            newSP.setName(thisSP.getName());
            newSP.setType(thisSP.getType());
            newSP.setNoWine(thisSP.getNoWine());
            newSP.setNavalBattle(thisSP.getNavalBattle());

            final Position newPos = new Position(); // NOPMD
            newPos.setX(thisSP.getPosition().getX());
            newPos.setY(thisSP.getPosition().getY());
            newPos.setRegion(thisSP.getPosition().getRegion());
            newPos.setGame(newGame);
            newSP.setPosition(newPos);

            if (thisSP.getType().getShipClass() == 0) {
                final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
                for (Integer key : thisSP.getStoredGoods().keySet()) {
                    qteGoods.put(key, 0);
                }
                newSP.setStoredGoods(qteGoods);
            }

            ShipManager.getInstance().add(newSP);

            // update statistics
            totNavy[thisSP.getNation().getId()] += thisSP.getType().getCost();
        }
    }

    private void initFleets(final Game newGame, final Game scenarioGame) {
        final List<Fleet> listFL = FleetManager.getInstance().listByGame(scenarioGame);
        for (Fleet fleet : listFL) {
            Fleet newFl = new Fleet();
            newFl.setName(fleet.getName());
            newFl.setNation(fleet.getNation());
            Position newPos = (Position) fleet.getPosition().clone();
            newPos.setGame(newGame);
            newFl.setPosition(newPos);
            newFl.setMps(fleet.getMps());
            FleetManager.getInstance().add(newFl);
            int fleetId = newFl.getFleetId();
            //add ships for fleets
            final List<Ship> listSP = ShipManager.getInstance().listByFleet(scenarioGame, fleet.getFleetId());
            for (Ship thisSP : listSP) {
                final Ship newSP = new Ship(); // NOPMD
                newSP.setNation(thisSP.getNation());
                newSP.setCondition(thisSP.getCondition());
                newSP.setFleet(fleetId);
                newSP.setMarines(thisSP.getMarines());
                newSP.setExp(thisSP.getExp());
                newSP.setName(thisSP.getName());
                newSP.setType(thisSP.getType());
                newSP.setNoWine(thisSP.getNoWine());
                newSP.setNavalBattle(thisSP.getNavalBattle());


                newSP.setPosition((Position) newPos.clone());

                if (thisSP.getType().getShipClass() == 0) {
                    final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
                    for (Integer key : thisSP.getStoredGoods().keySet()) {
                        qteGoods.put(key, 0);
                    }
                    newSP.setStoredGoods(qteGoods);
                }

                ShipManager.getInstance().add(newSP);

                // update statistics
                totNavy[thisSP.getNation().getId()] += thisSP.getType().getCost();

            }
        }
    }

    /**
     * Initializes the Baggage Trains.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initBaggageTrain(final Game newGame, final Game scenarioGame) {
        final List<BaggageTrain> lstBTrains = BaggageTrainManager.getInstance().listByGame(scenarioGame);
        for (final BaggageTrain thisTrain : lstBTrains) {
            initBaggageTrain(newGame, thisTrain);
        }
    }

    /**
     * Initialize this Baggage Train object.
     *
     * @param newGame   the game object.
     * @param thisTrain the new Baggage Train object.
     */
    private void initBaggageTrain(final Game newGame, final BaggageTrain thisTrain) {
        final BaggageTrain thisBaggageTrain = new BaggageTrain(); //NOPMD
        thisBaggageTrain.setNation(thisTrain.getNation());
        thisBaggageTrain.setName(thisTrain.getName());
        thisBaggageTrain.setCondition(thisTrain.getCondition());

        final Position newPos = new Position(); // NOPMD
        newPos.setX(thisTrain.getPosition().getX());
        newPos.setY(thisTrain.getPosition().getY());
        newPos.setRegion(thisTrain.getPosition().getRegion());
        newPos.setGame(newGame);
        thisBaggageTrain.setPosition(newPos);

        final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
        for (Integer key : thisTrain.getStoredGoods().keySet()) {
            qteGoods.put(key, 0);
        }
        thisBaggageTrain.setStoredGoods(qteGoods);

        BaggageTrainManager.getInstance().add(thisBaggageTrain);
    }


    /**
     * Initializes the trade Cities.
     *
     * @param newGame      the object of the new Game.
     * @param scenarioGame the object of the scenario Game.
     */
    private void initTradeCities(final Game newGame, final Game scenarioGame) {
        final List<TradeCity> listTCities = TradeCityManager.getInstance().listByGame(scenarioGame);
        for (final TradeCity thisCity : listTCities) {
            initTradeCity(newGame, thisCity);
        }
    }

    /**
     * Initialize this trade city object.
     *
     * @param newGame  the game object.
     * @param thisCity the new trade city object.
     */
    private void initTradeCity(final Game newGame, final TradeCity thisCity) {
        final TradeCity newCity = new TradeCity();
        newCity.setName(thisCity.getName());

        final Position newPos = new Position(); // NOPMD
        newPos.setX(thisCity.getPosition().getX());
        newPos.setY(thisCity.getPosition().getY());
        newPos.setRegion(thisCity.getPosition().getRegion());
        newPos.setGame(newGame);
        newCity.setPosition(newPos);

        final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
        for (Integer key : thisCity.getGoodsTradeLvl().keySet()) {
            qteGoods.put(key, getTradeRate());
        }

        // Set initial rates based on region
        if (thisCity.getPosition().getRegion().getId() == EUROPE) {
            qteGoods.put(GOOD_MONEY, 500000 + (caller.getRandomGen().nextInt(4) + 1) * 500000 + caller.getRandomGen().nextInt(100000));

            qteGoods.put(GOOD_INPT, getTradeRate());
            qteGoods.put(GOOD_FOOD, getTradeRate());
            qteGoods.put(GOOD_STONE, getTradeRate());
            qteGoods.put(GOOD_WOOD, getTradeRate());
            qteGoods.put(GOOD_ORE, getTradeRate());
            qteGoods.put(GOOD_GEMS, getTradeRate());
            qteGoods.put(GOOD_HORSE, getTradeRate());
            qteGoods.put(GOOD_WOOL, getTradeRate());
            qteGoods.put(GOOD_FABRIC, getTradeRate());
            qteGoods.put(GOOD_PRECIOUS, getTradeRate());
            qteGoods.put(GOOD_WINE, getTradeRate());
            qteGoods.put(GOOD_COLONIAL, getTradeRate());

        } else {
            // The colonies
            qteGoods.put(GOOD_MONEY, 500000 + (caller.getRandomGen().nextInt(3) + 1) * 500000 + caller.getRandomGen().nextInt(100000));

            qteGoods.put(GOOD_INPT, getTradeRate());
            qteGoods.put(GOOD_FOOD, getTradeRate());
            qteGoods.put(GOOD_STONE, getTradeRate());
            qteGoods.put(GOOD_WOOD, getTradeRate());
            qteGoods.put(GOOD_ORE, getTradeRate());
            qteGoods.put(GOOD_GEMS, getTradeRate());
            qteGoods.put(GOOD_HORSE, getTradeRate());
            qteGoods.put(GOOD_WOOL, getTradeRate());
            qteGoods.put(GOOD_FABRIC, getTradeRate());
            qteGoods.put(GOOD_PRECIOUS, getTradeRate());
            qteGoods.put(GOOD_WINE, getTradeRate());
            qteGoods.put(GOOD_COLONIAL, getTradeRate());
        }

        newCity.setGoodsTradeLvl(qteGoods);

        TradeCityManager.getInstance().add(newCity);
    }

    private int getTradeRate() {
        int surplus;
        int rate = caller.getRandomGen().nextInt(12) + 1;
        if (rate == 1) {
            surplus = 1;
        } else if (rate <= 4) {
            surplus = 2;
        } else if (rate <= 8) {
            surplus = 3;
        } else if (rate <= 11) {
            surplus = 4;
        } else {
            surplus = 5;
        }
        return surplus;

    }

    /**
     * Add a report entry for this turn.
     *
     * @param game  the object of the new Game.
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Game game, final Nation owner, final String key, final int value) {
        final Report thisReport = new Report();
        thisReport.setGame(game);
        thisReport.setTurn(game.getTurn());
        thisReport.setNation(owner);
        thisReport.setKey(key);
        thisReport.setValue(Integer.toString(value)); // NOPMD
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Report warehouses.
     *
     * @param thisGame   the Game.
     * @param thisNation the Nation.
     * @param sectors    the total sectors per region.
     * @param population the total population size per region.
     * @param goods      the available resources per region.
     * @param army       the starting cost of the army.
     * @param navy       the starting cost of the navy.
     */
    private void reportInitStatistics(final Game thisGame,
                                      final Nation thisNation,
                                      final int[] sectors,
                                      final int[] population,
                                      final int[][] goods,
                                      final int army,
                                      final int navy) {

        // Add region-independent reports
        report(thisGame, thisNation, N_ALIVE, 1);
        report(thisGame, thisNation, N_VP, thisNation.getVpInit());

        report(thisGame, thisNation, A_TOT_MONEY, army);
        report(thisGame, thisNation, S_TOT_MONEY, navy);

        // iterate through the regions
        for (int region = REGION_FIRST; region < sectors.length; region++) {
            report(thisGame, thisNation, E_SEC_SIZE + region, sectors[region]);
            report(thisGame, thisNation, E_POP_SIZE + region, population[region]);

            if (region != EUROPE && thisNation.getId() > 0 && sectors[region] > 0) {
                // Report colony
                report(thisGame, thisNation, "colony." + region, 1);
            }

            // report goods only for nations, not for unconquered areas
            if (thisNation.getId() > 0) {
                for (int good = GOOD_FIRST; good <= GOOD_LAST; good++) {
                    report(thisGame, thisNation, W_REGION + region + W_GOOD + good, goods[region][good]);
                }
            }
        }
    }

    /**
     * Produce maps and tiles for this game/turn.
     *
     * @param thisGame the Game.
     * @param basePath the base path for storing images.
     */
    private void produceImages(final Game thisGame, final String basePath) {
        // Create the directory of this game
        final File gameDir = new File(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId());
        if (gameDir.exists()) {
            final boolean result = deleteDir(gameDir);
            if (result) {
                LOGGER.info("Game directory for maps already exists. Directory deleted.");
            } else {
                LOGGER.error("Game directory for maps already exists. Failed to delete directory.");
            }
        }

        // Create directory
        final boolean success = gameDir.mkdir();
        if (success) {
            LOGGER.info("Game directory for maps created. ");
        } else {
            LOGGER.fatal("Failed to create game directory for maps.");
        }

        final Nation neutral = NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL);
        final Map<Nation, Map<Sector, BigInteger>> listLC = BattalionManager.getInstance().listLCBattalions(thisGame);

        final List<Region> lstRegion;
        final List<Nation> lstNations;
        switch (scenarioId) {
            case HibernateUtil.DB_FREE:
                // add single nation
                lstNations = new ArrayList<Nation>();
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));

                // add two regions
                lstRegion = new ArrayList<Region>();
                lstRegion.add(RegionManager.getInstance().getByID(EUROPE));
                lstRegion.add(RegionManager.getInstance().getByID(CARIBBEAN));
                break;

            case HibernateUtil.DB_S3:
                lstNations = new ArrayList<Nation>();
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_SPAIN));
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN));

                lstRegion = new ArrayList<Region>();
                lstRegion.add(RegionManager.getInstance().getByID(RegionConstants.EUROPE));
                break;

            case HibernateUtil.DB_S1:
            case HibernateUtil.DB_S2:
            default:
                // all nations
                lstNations = NationManager.getInstance().list();
                lstNations.remove(0);

                // all regions
                lstRegion = RegionManager.getInstance().list();
                break;
        }

        for (final Region region : lstRegion) {
            final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(thisGame, region);
            final Map<Integer, Sector> sectorMap = new HashMap<Integer, Sector>();

            final MapCreator mapWindow = new MapCreator(thisGame, sectorList, neutral, basePath + "/", 1d, true, false, false);
            mapWindow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-0-" + region.getId() + ".png");

            final MapCreator mapWindowInfo = new MapCreator(thisGame, sectorList, neutral, basePath + "/", 0.2d, true, false, false);
            mapWindowInfo.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-0-" + region.getId() + "-info.png");

            final MapCreator mapWindowSmall = new MapCreator(thisGame, sectorList, neutral, basePath + "/", 0.1d, true, false, false);
            mapWindowSmall.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-0-" + region.getId() + "-small.png");

            if (!thisGame.isFogOfWar()) {
                final MapCreator mapNation = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", 1d, false, false, !thisGame.isFogOfWar());
                mapNation.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0.png");

                final MapCreator mapNationLow = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", .7d, false, false, !thisGame.isFogOfWar());
                mapNationLow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-lowres.png");

                final MapCreator mapNationVLow = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", .5d, false, false, !thisGame.isFogOfWar());
                mapNationVLow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-vlowres.png");

                final MapCreator mapNationVVLow = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", .25d, false, false, !thisGame.isFogOfWar());
                mapNationVVLow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-vvlowres.png");

                final MapCreator mapNationBorder = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", 1d, true, true, !thisGame.isFogOfWar());
                mapNationBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-border.png");

                final MapCreator mapNationLowBorder = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", .7d, true, true, !thisGame.isFogOfWar());
                mapNationLowBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-border-lowres.png");

                final MapCreator mapNationVLowBorder = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", .5d, true, true, !thisGame.isFogOfWar());
                mapNationVLowBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-border-vlowres.png");

                final MapCreator mapNationVVLowBorder = new MapCreator(thisGame, sectorList, lstNations.get(0), basePath + "/", .25d, true, true, !thisGame.isFogOfWar());
                mapNationVVLowBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-0-border-vvlowres.png");

            } else {

                // Apply fog of war rules.
                for (final Nation nation : lstNations) {
                    LOGGER.debug("Applying fog-of-war rules for " + nation.getName() + "/" + region.getName());

                    // Produce separate list of nations
                    final List<Nation> localList = NationManager.getInstance().list();
                    localList.remove(0); // Remove neutrals

                    final String token = "*" + Integer.toString(nation.getId()) + "*";

                    // Update sectors with FOW visibility
                    final FogOfWarInspector fOWsg = new FogOfWarInspector(thisGame, region, nation, localList, listLC);
                    for (final Sector sector : fOWsg.getVisibleSectors()) {
                        if (sector != null) {
                            if (!sectorMap.containsKey(sector.getId())) {
                                sectorMap.put(sector.getId(), sector);
                            }

                            if (!sector.getFow().contains(token)) {
                                sector.setFow(sector.getFow() + token);
                            }
                        }
                    }

                    final MapCreator mapNation = new MapCreator(thisGame, sectorList, nation, basePath + "/", 1d, false, false, !thisGame.isFogOfWar());
                    mapNation.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-0-" + region.getId() + "-" + nation.getId() + ".png");

                    final MapCreator mapNationLow = new MapCreator(thisGame, sectorList, nation, basePath + "/", .7d, false, false, !thisGame.isFogOfWar());
                    mapNationLow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-0-" + region.getId() + "-" + nation.getId() + "-lowres.png");

                    final MapCreator mapNationVLow = new MapCreator(thisGame, sectorList, nation, basePath + "/", .5d, false, false, !thisGame.isFogOfWar());
                    mapNationVLow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-vlowres.png");

                    final MapCreator mapNationVVLow = new MapCreator(thisGame, sectorList, nation, basePath + "/", .25d, false, false, !thisGame.isFogOfWar());
                    mapNationVVLow.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-vvlowres.png");

                    final MapCreator mapNationBorder = new MapCreator(thisGame, sectorList, nation, basePath + "/", 1d, true, true, !thisGame.isFogOfWar());
                    mapNationBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border.png");

                    final MapCreator mapNationLowBorder = new MapCreator(thisGame, sectorList, nation, basePath + "/", .7d, true, true, !thisGame.isFogOfWar());
                    mapNationLowBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border-lowres.png");

                    final MapCreator mapNationVLowBorder = new MapCreator(thisGame, sectorList, nation, basePath + "/", .5d, true, true, !thisGame.isFogOfWar());
                    mapNationVLowBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border-vlowres.png");

                    final MapCreator mapNationVVLowBorder = new MapCreator(thisGame, sectorList, nation, basePath + "/", .25d, true, true, !thisGame.isFogOfWar());
                    mapNationVVLowBorder.draw(basePath + "/images/maps/s" + thisGame.getScenarioId() + "/" + thisGame.getGameId() + "/map-" + thisGame.getGameId() + "-" + thisGame.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border-vvlowres.png");

                }

                // Update Sector information
                for (final Sector sector : sectorMap.values()) {
                    SectorManager.getInstance().update(sector);
                }
            }
        }
    }

    /**
     * Deletes all files and sub directories under dir.
     * If a deletion fails, the method stops attempting to delete and returns false.
     *
     * @param dir the directory to delete.
     * @return true if all deletions were successful.
     */

    private boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            for (final String aChildren : dir.list()) {
                if (!deleteDir(new File(dir, aChildren))) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

}
