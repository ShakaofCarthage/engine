package com.eaw1805.core;

import com.eaw1805.algorithms.FogOfWarInspector;
import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.core.initializers.GameInitializer;
import com.eaw1805.core.initializers.scenario1802.ScenarioInitializer;
import com.eaw1805.core.payment.PaymentManager;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GameConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.EngineProcessManager;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Engine;
import com.eaw1805.data.model.EngineProcess;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.comparators.NationId;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.orders.PatrolOrderDetails;
import com.eaw1805.economy.EconomyProcessor;
import com.eaw1805.events.EventProcessor;
import com.eaw1805.events.RandomEventProcessor;
import com.eaw1805.events.RumourEventProcessor;
import com.eaw1805.map.MapNationExecutor;
import com.eaw1805.map.MapRegionExecutor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.quartz.CronExpression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The entry point for processing a turn.
 */
public final class GameEngine
        implements Engine, GameConstants, ReportConstants {

    private static final String BACKUP_BASE = "empire-backup";
    private static final String BACKUP_FIRST = "first";
    private static final String BACKUP_LAST = "last";

    private static final String BACKUP_DB = "empire-s1";
    private static final String BACKUP_USER = "empire";
    private static final String BACKUP_PWD = "empire123";

    /**
     * The URL of the JENKINS server.
     */
    private static final String JENKINS =
            "http://gamekeeper.oplongames.com:8098/jenkins";


    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(GameEngine.class);

    /**
     * VP placeholder.
     */
    public static final String VP_PLACEHOLDER = "VPREPORT";

    /**
     * Identification number of game to process.
     */
    private transient int gameID;

    /**
     * Identification number of the scenario.
     */
    private transient int scenarioID;

    /**
     * The game object fetched from the database.
     */
    private transient Game game;

    /**
     * The execution number.
     */
    private transient int execID;

    /**
     * The random generator.
     */
    private final transient Random randomGen;

    /**
     * Directory name containing tiles.
     */
    private final transient String basePath;

    /**
     * List of alive nations.
     */
    private final List<Nation> aliveNations;

    /**
     * List of alive nations.
     */
    private final Set<Integer> aliveNationsId;

    /**
     * Map alive & active nations with users.
     */
    private final Map<Integer, User> userNations;

    /**
     * The active patrol orders.
     */
    private final transient Map<Integer, PatrolOrderDetails> patrolOrders;

    /**
     * Empty constructor.
     */
    public GameEngine() {
        // used to load LOG4J properties
        basePath = "/srv/eaw1805";
        randomGen = new Random();
        scenarioID = HibernateUtil.DB_S1;

        // Detect alive nations
        aliveNations = new ArrayList<Nation>();
        aliveNationsId = new HashSet<Integer>();
        userNations = new HashMap<Integer, User>();

        // Setup patrol orders holder
        patrolOrders = new HashMap<Integer, PatrolOrderDetails>();
    }

    /**
     * Default constructor.
     *
     * @param identity   the Game ID to process.
     * @param path       the path where the images are stored.
     * @param execNumber the execution number.
     */
    public GameEngine(final int identity, final int scenario, final String path, final int execNumber) {
        gameID = identity;
        scenarioID = scenario;
        basePath = path;
        execID = execNumber;
        randomGen = new Random();
        randomGen.setSeed(gameID * 1000);

        // Detect alive nations
        aliveNations = new ArrayList<Nation>();
        aliveNationsId = new HashSet<Integer>();
        userNations = new HashMap<Integer, User>();

        // Setup patrol orders holder
        patrolOrders = new HashMap<Integer, PatrolOrderDetails>();

        LOGGER.debug("GameEngine instantiated.");
    }

    /**
     * Get the random number generator.
     *
     * @return the random number generator.
     */
    public Random getRandomGen() {
        return randomGen;
    }

    /**
     * Engine name.
     *
     * @return the name of the engine.
     */
    @Override
    public String getName() {
        return "GameEngine";
    }

    /**
     * Get the active patrol orders.
     *
     * @return the active patrol orders.
     */
    public Map<Integer, PatrolOrderDetails> getPatrolOrders() {
        return patrolOrders;
    }

    /**
     * Initializes game properties.
     *
     * @throws InvalidGameIdentifier case the game ID is not valid.
     */
    public void init() throws InvalidGameIdentifier {
        // Check if game is exists
        if (getGameID() < 1) {
            LOGGER.error("Game ID not valid.");
            throw new InvalidGameIdentifier();

        } else {
            // Check if Scenario is properly initialized
            final AbstractThreadedInitializer scInit;
            switch (scenarioID) {
                case HibernateUtil.DB_FREE:
                    scInit = new com.eaw1805.core.initializers.scenario1804.ScenarioInitializer(scenarioID);
                    break;

                case HibernateUtil.DB_S3:
                    scInit = new com.eaw1805.core.initializers.scenario1808.ScenarioInitializer(scenarioID);
                    break;

                case HibernateUtil.DB_S2:
                    scInit = new com.eaw1805.core.initializers.scenario1805.ScenarioInitializer(scenarioID);
                    break;

                case HibernateUtil.DB_S1:
                default:
                    scInit = new ScenarioInitializer(scenarioID);
                    break;
            }
            scInit.run();

            // Check if game is properly initialized
            final GameInitializer gInit = new GameInitializer(this, gameID, scenarioID, execID);
            gInit.run();

            // If this is not a new game we need to retrieve the record
            if (getGame() == null) {
                final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
                setGame(GameManager.getInstance().getByID(gameID));
                theTrans.commit();
            }

            // Clear Web site caches
            if (execID > 0) {
                final WebsiteCacheManager client = new WebsiteCacheManager(false);

                try {
                    client.connect();

                } catch (final IOException e) {
                    LOGGER.error(e);

                } catch (final InterruptedException e) {
                    LOGGER.error(e);

                } catch (final Exception e) {
                    LOGGER.error("general exception while evicting caches", e);
                }

                try {
                    client.evictCache(WebsiteCacheManager.CACHE_NAME);
                    client.evictCache(WebsiteCacheManager.USER_CACHE_NAME);
                    client.evictCache(WebsiteCacheManager.GAME_CACHE_NAME + "-" + gameID);

                    for (int nation = NationConstants.NATION_FIRST; nation <= NationConstants.NATION_LAST; nation++) {
                        client.evictCache(WebsiteCacheManager.CLIENT_CACHE_NAME + "-" + gameID + "-" + nation);
                    }
                } catch (final IOException e) {
                    LOGGER.error(e);

                } catch (final Exception e) {
                    LOGGER.error("general exception while evicting caches", e);
                }
            }

            LOGGER.info("Game Initialized [" + getGame().getGameId() + "].");
        }
    }

    /**
     * Start processing the game round.
     */
    public void process() {
        // Make sure we have an active transaction
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        // Fetch game object from db
        setGame(GameManager.getInstance().getByID(getGameID()));
        randomGen.setSeed(gameID * 1000 + getGame().getTurn());

        LOGGER.info("Processing Game [" + getGameID() + "] Turn [" + getGame().getTurn() + "].");

        // Game Calendar before processing
        final Calendar thatCal = calendar();

        final EngineProcess thisProc = new EngineProcess();
        thisProc.setGameId(getGameID());
        thisProc.setTurn(getGame().getTurn());
        thisProc.setDateStart(new Date());
        thisProc.setDuration(-1);
        thisProc.setProcessName(ManagementFactory.getRuntimeMXBean().getName());
        thisProc.setBuildNumber(execID);
        thisProc.setScenarioId(scenarioID);
        final Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        EngineProcessManager.getInstance().add(thisProc);
        final long tsStart = System.currentTimeMillis();
        thatTrans.commit();

        game.setStatus(GAME_PROC);
        GameManager.getInstance().update(game);
        theTrans.commit();

        // Clear Web site caches
        if (execID > 0 && scenarioID > 0) {
            final WebsiteCacheManager client = new WebsiteCacheManager(false);

            try {
                client.connect();

            } catch (final IOException e) {
                LOGGER.error(e);

            } catch (final InterruptedException e) {
                LOGGER.error(e);

            } catch (final Exception e) {
                LOGGER.error("Failed to evict cache", e);
            }

            try {
                client.evictCache(WebsiteCacheManager.GAME_CACHE_NAME + "-" + gameID);
                client.callGameSpecificPages(game, false);

            } catch (final IOException e) {
                LOGGER.error(e);

            } catch (final Exception e) {
                LOGGER.error("Failed to evict cache", e);
            }

            try {
                client.callGameSpecificPages(game, false);

            } catch (final IOException e) {
                LOGGER.error(e);

            } catch (final Exception e) {
                LOGGER.error("Failed to evict cache", e);
            }

            // Set the session factories to all stores
            HibernateUtil.connectEntityManagers(scenarioID);
        }

        // Remove movement flags from units (from previous turn)
        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        BattalionManager.getInstance().removeHasMovedFlag(getGame());
        ShipManager.getInstance().removeHasMovedFlag(getGame());

        // Remove old conquer counters
        SectorManager.getInstance().removeConquer(getGame());
        theTrans.commit();

        // Identify alive nations
        detectAliveNations();

        // Carry VPs from previous turn after applying small random losses.
        reportVP();

        // Remove Dead Commanders
        removeDeadCommanders();

        // Resolve Simulated Battles

        // Process Random Events
        if (scenarioID > HibernateUtil.DB_MAIN
                && game.isRandomEvents()) {
            final RandomEventProcessor thisREP = new RandomEventProcessor(this);
            thisREP.process();
        }

        // Process Orders
        final OrderProcessor thisOP = new OrderProcessor(this);
        final int totalOrders = thisOP.process();

        // Process Events
        final EventProcessor thisVP = new EventProcessor(this);
        thisVP.process();

        // Economic turnaround
        final EconomyProcessor thisEP = new EconomyProcessor(this);
        thisEP.process();

        // Process Rumours
        if (scenarioID > HibernateUtil.DB_MAIN
                && game.isRumorsEnabled()) {
            final RumourEventProcessor thisRUM = new RumourEventProcessor(this);
            thisRUM.process();
        }

        // Report warehouse at end of turn
        reportWarehouses();

        // Update Turn counter
        increaseTurnCounter();

        // Determine Severe winter
        if (scenarioID == HibernateUtil.DB_S3) {
            determineSevereWinter1808();

        } else {
            determineSevereWinter();
        }

        // apply fog of war rules
        if (game.isFogOfWar()) {
            fogOfWar();
        }

        if (execID > 0) {
            // Produce images
            produceImages();
        }

        // Register processing
        final Calendar nextCal = registerProcess(thisProc, tsStart);

        // Register processing on Twitter and send out mails
        if (execID > 0) {
            switch (scenarioID) {

                case HibernateUtil.DB_S3:
                    makeTweet(nextCal, thatCal, totalOrders);

                    // Send out mails to users
                    sendMails1808(nextCal, thatCal, totalOrders);

                    break;

                case HibernateUtil.DB_S2:
                case HibernateUtil.DB_S1:
                    makeTweet(nextCal, thatCal, totalOrders);

                    // Send out mails to users
                    sendMails1805(nextCal, thatCal, totalOrders);

                    break;

                case HibernateUtil.DB_FREE:

                    // retrieve userID of game
                    Transaction dbTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_FREE);
                    final List<UserGame> lstUsers = UserGameManager.getInstance().list(getGame());
                    final int userId = lstUsers.get(0).getUserId();
                    dbTrans.commit();

                    // retrieve user
                    dbTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                    final User thisUser = UserManager.getInstance().getByID(userId);
                    //be sure to update last process date for this user.
                    //this counts for the questionnaire.
                    thisUser.setLastProcDate(new Date());
                    dbTrans.commit();

                    // Update user profile
                    thisVP.changeProfile(thisUser, ProfileConstants.TURNS_PLAYED_SOLO, 1);

                    // send out mail to single user
                    sendMail1804(thatCal, thisUser);

                    break;

                default:
                    // do nothing
            }
        }

        if (scenarioID > HibernateUtil.DB_MAIN) {
            // Remove newsletter flags and make charges
            detectAliveNations();

            // Clear Web site caches
            if (execID > 0) {
                PaymentManager.getInstance().updateAccounts(this, thatCal);

                final WebsiteCacheManager client = new WebsiteCacheManager(false);

                try {
                    client.connect();

                } catch (final IOException e) {
                    LOGGER.error(e);

                } catch (final InterruptedException e) {
                    LOGGER.error(e);

                } catch (final Exception e) {
                    LOGGER.error("general exception while evicting caches", e);
                }

                try {
                    client.evictCache(WebsiteCacheManager.CACHE_NAME);
                    client.evictCache(WebsiteCacheManager.USER_CACHE_NAME);
                    client.evictCache(WebsiteCacheManager.GAME_CACHE_NAME + "-" + gameID);

                    for (int nation = NationConstants.NATION_FIRST; nation <= NationConstants.NATION_LAST; nation++) {
                        client.evictCache(WebsiteCacheManager.CLIENT_CACHE_NAME + "-" + gameID + "-" + nation);
                    }
                } catch (final IOException e) {
                    LOGGER.error(e);

                } catch (final Exception e) {
                    LOGGER.error("general exception while evicting caches", e);
                }

                try {
                    client.callGameSpecificPages(game, true);

                } catch (final IOException e) {
                    LOGGER.error(e);

                } catch (final Exception e) {
                    LOGGER.error("general exception while reloading caches", e);
                }
            }
        }

        LOGGER.info("Processed Game [" + getGameID() + "] Turn [" + (getGame().getTurn() - 1) + "].");
    }

    private void sendMail1804(final Calendar turnCal, final User user) {
        // Construct the 1st attachment
        final String filePath1 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.EUROPE + "-small.png";

        // Construct the 3rd attachment
        final String filePath3 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.CARIBBEAN + "-small.png";

        // Construct the subject of the mail
        final StringBuilder mailSubject = new StringBuilder();
        mailSubject.append("[EaW1805] Solo Game (");
        mailSubject.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        mailSubject.append(" ");
        mailSubject.append(turnCal.get(Calendar.YEAR));
        mailSubject.append(") processed");

        // Construct the body of the mail
        final StringBuilder mailBody = new StringBuilder();
        mailBody.append("Dear ");
        mailBody.append(user.getUsername());
        mailBody.append(",\n\n");
        mailBody.append("Your solo game was just processed!\n\n");

        mailBody.append("You can play your game from your EaW home:\n\n");
        mailBody.append("http://www.eaw1805.com/games\n\n");

        mailBody.append("Click on the play button enter the game - or follow the link:\n\n");

        mailBody.append("http://www.eaw1805.com/play/scenario/1804/game/");
        mailBody.append(game.getGameId());
        mailBody.append("/nation/5\n\n");

        mailBody.append("You can find valuable information about the game in the Quick Start Guide:\n\n");
        mailBody.append("http://www.eaw1805.com/help/introduction\n\n");

        mailBody.append("We have prepared a set of video tutorials to help you get useful insights on the game mechanics:\n\n");
        mailBody.append("http://www.eaw1805.com/help\n\n");

        mailBody.append("Detailed information about the game rules and the mechanisms of Empires at War 1805 are available in the Players' Handbook:\n\n");
        mailBody.append("http://www.eaw1805.com/handbook\n\n");

        // Closing
        mailBody.append("Enjoy your game,\nOplon Games");

        // Send mails to all active players.
        EmailManager.getInstance().sendMail1804(user, mailSubject.toString(),
                mailBody.toString(), filePath1, filePath3);

        // Send mail to administrators
        EmailManager.getInstance().sendEmail("admin@eaw1805.com", "[EaW1805] Process of Solo-Game for " + user.getUsername(), mailBody.toString(),
                filePath1, null, filePath3, null);
    }

    /**
     * Send mails to active players.
     *
     * @param nextCal     the next processing.
     * @param turnCal     the turn processed.
     * @param totalOrders total orders processed.
     */
    private void sendMails1805(final Calendar nextCal, final Calendar turnCal, final int totalOrders) {
        // Construct the 1st attachment
        final String filePath1 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.EUROPE + "-small.png";

        // Construct the 2nd attachment
        final String filePath2 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.AFRICA + "-small.png";

        // Construct the 3rd attachment
        final String filePath3 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.CARIBBEAN + "-small.png";

        // Construct the 4th attachment
        final String filePath4 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.INDIES + "-small.png";

        // Construct the subject of the mail
        final StringBuilder mailSubject = new StringBuilder();
        mailSubject.append("[EaW1805] Game ");
        mailSubject.append(getGame().getGameId());
        mailSubject.append(" / ");
        mailSubject.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        mailSubject.append(" ");
        mailSubject.append(turnCal.get(Calendar.YEAR));
        if (getGame().getDescription() != null && getGame().getDescription().length() > 0) {
            mailSubject.append(" (");
            mailSubject.append(getGame().getDescription());
            mailSubject.append(")");
        }
        mailSubject.append(" processed");

        // Construct the body of the mail
        final StringBuilder mailBody = new StringBuilder();
        mailBody.append("Game ");
        mailBody.append(getGame().getGameId());
        mailBody.append(" / ");

        mailBody.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        mailBody.append(" ");
        mailBody.append(turnCal.get(Calendar.YEAR));
        if (getGame().getDescription() != null && getGame().getDescription().length() > 0) {
            mailBody.append(" (");
            mailBody.append(getGame().getDescription());
            mailBody.append(")");
        }
        mailBody.append(" was just processed.\n\nIt included ");
        mailBody.append(totalOrders);
        mailBody.append(" player orders.\n\n");

        // Open DB transaction since need to check player orders + construct VP list for each player separately
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        final List<News> lstNewsEntries = NewsManager.getInstance().listGame(getGame(), getGame().getTurn() - 1);
        if (!lstNewsEntries.isEmpty()) {
            // sending out global news as part of the e-mail
            mailBody.append("News from around the world: \n\n");

            for (final News entry : lstNewsEntries) {
                mailBody.append("o ");
                mailBody.append(entry.getText());
                mailBody.append("\n\n");
            }
        }

        // Add placeholder for VPs
        mailBody.append(VP_PLACEHOLDER);

        // Add link to game info page
        mailBody.append("Check out the progress of the game via the following link:\n\n");
        mailBody.append("http://www.eaw1805.com/scenario/").append(getGame().getScenarioIdToString()).append("/game/");
        mailBody.append(getGame().getGameId());
        mailBody.append("/info");
        mailBody.append("\n\n");

        // Report new deadline
        mailBody.append("Next processing on ");

        mailBody.append(nextCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH));
        mailBody.append(" ");
        mailBody.append(nextCal.get(Calendar.YEAR));
        mailBody.append("-");
        if (nextCal.get(Calendar.MONTH) + 1 < 10) {
            mailBody.append("0");
        }
        mailBody.append(nextCal.get(Calendar.MONTH) + 1);
        mailBody.append("-");
        if (nextCal.get(Calendar.DAY_OF_MONTH) < 10) {
            mailBody.append("0");
        }
        mailBody.append(nextCal.get(Calendar.DAY_OF_MONTH));
        mailBody.append(".\n\n");

        // Add reminder for players that did not issue any order
        final StringBuilder mailBodyNoOrders = new StringBuilder();
        mailBodyNoOrders.append(mailBody);

        mailBodyNoOrders.append("*NOTE* You did not issue any orders for your Empire. ");
        mailBodyNoOrders.append("Your Empire will not survive on its own. Your people need you...\n\n");
        mailBodyNoOrders.append("If you give no orders for your position during the next processing as well, the position will automatically be dropped out by the engine. Please consider that having positions without orders is affecting the balance of the game.\n\n");
        mailBodyNoOrders.append("If you are experiencing a technical problem, please contact us at support@eaw1805.com.\n\n");

        // Closing
        mailBodyNoOrders.append("Enjoy your game,\nOplon Games");
        mailBody.append("Enjoy your game,\nOplon Games");


        // Send mails to all active players.
        EmailManager.getInstance().sendMail(getGame(), mailSubject.toString(),
                mailBody.toString(), mailBodyNoOrders.toString(),
                filePath1, filePath2, filePath3, filePath4);

        // Remove VP Placeholder
        final int location = mailBody.indexOf(VP_PLACEHOLDER);
        final StringBuilder adminBody = mailBody.replace(location, location + VP_PLACEHOLDER.length(), "");

        // Notify the admins on the log file of the processing
        adminBody.append("\n\nThe engine log is available here:\n\n");
        adminBody.append("http://gamekeeper.oplongames.com:8098/jenkins/job/Engine (Scenario ").append(getGame().getScenarioIdToString()).append(")/");
        adminBody.append(execID);
        adminBody.append("/console");

        // Send mail to administrators
        EmailManager.getInstance().sendEmail("engine@eaw1805.com", mailSubject.toString(), adminBody.toString(),
                filePath1, filePath2, filePath3, filePath4);

        theTrans.commit();
    }

    /**
     * Send mails to active players.
     *
     * @param nextCal     the next processing.
     * @param turnCal     the turn processed.
     * @param totalOrders total orders processed.
     */
    private void sendMails1808(final Calendar nextCal, final Calendar turnCal, final int totalOrders) {
        // Construct the 1st attachment
        final String filePath1 = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.EUROPE + "-small.png";

        // Construct the subject of the mail
        final StringBuilder mailSubject = new StringBuilder();
        mailSubject.append("[EaW1805] Game ");
        mailSubject.append(getGame().getGameId());
        mailSubject.append(" / ");
        mailSubject.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        mailSubject.append(" ");
        mailSubject.append(turnCal.get(Calendar.YEAR));
        if (getGame().getDescription() != null && getGame().getDescription().length() > 0) {
            mailSubject.append(" (");
            mailSubject.append(getGame().getDescription());
            mailSubject.append(")");
        }
        mailSubject.append(" processed");

        // Construct the body of the mail
        final StringBuilder mailBody = new StringBuilder();
        mailBody.append("Game ");
        mailBody.append(getGame().getGameId());
        mailBody.append(" / ");

        mailBody.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        mailBody.append(" ");
        mailBody.append(turnCal.get(Calendar.YEAR));
        if (getGame().getDescription() != null && getGame().getDescription().length() > 0) {
            mailBody.append(" (");
            mailBody.append(getGame().getDescription());
            mailBody.append(")");
        }
        mailBody.append(" was just processed.\n\nIt included ");
        mailBody.append(totalOrders);
        mailBody.append(" player orders.\n\n");

        // Open DB transaction since need to check player orders + construct VP list for each player separately
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        final List<News> lstNewsEntries = NewsManager.getInstance().listGame(getGame(), getGame().getTurn() - 1);
        if (!lstNewsEntries.isEmpty()) {
            // sending out global news as part of the e-mail
            mailBody.append("News from around the world: \n\n");

            for (final News entry : lstNewsEntries) {
                mailBody.append("o ");
                mailBody.append(entry.getText());
                mailBody.append("\n\n");
            }
        }

        // Add placeholder for VPs
        mailBody.append(VP_PLACEHOLDER);

        // Add link to game info page
        mailBody.append("Check out the progress of the game via the following link:\n\n");
        mailBody.append("http://www.eaw1805.com/scenario/").append(getGame().getScenarioIdToString()).append("/game/");
        mailBody.append(getGame().getGameId());
        mailBody.append("/info");
        mailBody.append("\n\n");

        // Report new deadline
        mailBody.append("Next processing on ");

        mailBody.append(nextCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH));
        mailBody.append(" ");
        mailBody.append(nextCal.get(Calendar.YEAR));
        mailBody.append("-");
        if (nextCal.get(Calendar.MONTH) + 1 < 10) {
            mailBody.append("0");
        }
        mailBody.append(nextCal.get(Calendar.MONTH) + 1);
        mailBody.append("-");
        if (nextCal.get(Calendar.DAY_OF_MONTH) < 10) {
            mailBody.append("0");
        }
        mailBody.append(nextCal.get(Calendar.DAY_OF_MONTH));
        mailBody.append(".\n\n");

        // Add reminder for players that did not issue any order
        final StringBuilder mailBodyNoOrders = new StringBuilder();
        mailBodyNoOrders.append(mailBody);

        mailBodyNoOrders.append("*NOTE* You did not issue any orders for your Empire. ");
        mailBodyNoOrders.append("Your Empire will not survive on its own. Your people need you...\n\n");
        mailBodyNoOrders.append("If you give no orders for your position during the next processing as well, the position will automatically be dropped out by the engine. Please consider that having positions without orders is affecting the balance of the game.\n\n");
        mailBodyNoOrders.append("If you are experiencing a technical problem, please contact us at support@eaw1805.com.\n\n");

        // Closing
        mailBodyNoOrders.append("Enjoy your game,\nOplon Games");
        mailBody.append("Enjoy your game,\nOplon Games");


        // Send mails to all active players.
        EmailManager.getInstance().sendMail(getGame(), mailSubject.toString(),
                mailBody.toString(), mailBodyNoOrders.toString(),
                filePath1, null, null, null);

        // Remove VP Placeholder
        final int location = mailBody.indexOf(VP_PLACEHOLDER);
        final StringBuilder adminBody = mailBody.replace(location, location + VP_PLACEHOLDER.length(), "");

        // Notify the admins on the log file of the processing
        adminBody.append("\n\nThe engine log is available here:\n\n");
        adminBody.append("http://gamekeeper.oplongames.com:8098/jenkins/job/Engine (Scenario ").append(getGame().getScenarioIdToString()).append(")/");
        adminBody.append(execID);
        adminBody.append("/console");

        // Send mail to administrators
        EmailManager.getInstance().sendEmail("engine@eaw1805.com", mailSubject.toString(), adminBody.toString(),
                filePath1, null, null, null);

        theTrans.commit();
    }

    /**
     * Make post to twitter.
     *
     * @param nextCal     the next processing.
     * @param turnCal     the turn processed.
     * @param totalOrders total orders processed.
     */
    private void makeTweet(final Calendar nextCal, final Calendar turnCal, final int totalOrders) {
        final String filePath = basePath + "/images/maps/s"
                + game.getScenarioId() + "/"
                + game.getGameId()
                + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + RegionConstants.EUROPE + "-small.png";

        final StringBuilder finalTweet = new StringBuilder();
        finalTweet.append("Game ");
        finalTweet.append(getGame().getGameId());
        finalTweet.append(" / ");

        finalTweet.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        finalTweet.append(" ");
        finalTweet.append(turnCal.get(Calendar.YEAR));
        if (getGame().getDescription() != null && getGame().getDescription().length() > 0) {
            finalTweet.append(" (");
            finalTweet.append(getGame().getDescription());
            finalTweet.append(")");
        }

        finalTweet.append(" included ");
        finalTweet.append(totalOrders);
        finalTweet.append(" player orders.");
//        finalTweet.append(" player orders. Next processing on ");
//
//        finalTweet.append(nextCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH));
//        finalTweet.append(" ");
//        finalTweet.append(nextCal.get(Calendar.YEAR));
//        finalTweet.append("-");
//        if (nextCal.get(Calendar.MONTH) + 1 < 10) {
//            finalTweet.append("0");
//        }
//        finalTweet.append(nextCal.get(Calendar.MONTH) + 1);
//        finalTweet.append("-");
//        if (nextCal.get(Calendar.DAY_OF_MONTH) < 10) {
//            finalTweet.append("0");
//        }
//        finalTweet.append(nextCal.get(Calendar.DAY_OF_MONTH));
//        finalTweet.append(".");

        TwitterManager.getInstance().tweetImage(finalTweet.toString(), filePath);
    }

    /**
     * Get the ID of the game.
     *
     * @return the unique identifier of the game.
     */
    public int getGameID() {
        return gameID;
    }

    /**
     * Game object of the turn processed.
     *
     * @return the game object.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Update the thisGame object of the turn processed.
     *
     * @param thisGame the new thisGame instance.
     */
    public void setGame(final Game thisGame) {
        this.game = thisGame;
        gameID = thisGame.getGameId();
    }

    /**
     * Get the base path for executing the engine and produce file output.
     *
     * @return the base path.
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * Get the current calendar.
     *
     * @return the calendar.
     */
    public Calendar calendar() {
        final Calendar thisCal = Calendar.getInstance();

        // Define starting date based on scenario.
        switch (getGame().getScenarioId()) {
            case HibernateUtil.DB_S1:
                if (getGameID() < 8) {
                    thisCal.set(1805, Calendar.JANUARY, 1);

                } else {
                    thisCal.set(1802, Calendar.APRIL, 1);
                }
                break;

            case HibernateUtil.DB_S2:
                thisCal.set(1805, Calendar.JANUARY, 1);
                break;

            case HibernateUtil.DB_S3:
                thisCal.set(1808, Calendar.SEPTEMBER, 1);
                break;

            case HibernateUtil.DB_FREE:
            default:
                thisCal.set(1804, Calendar.JANUARY, 1);
                break;

        }

        thisCal.add(Calendar.MONTH, getGame().getTurn());

        return thisCal;
    }

    /**
     * Backup the database.
     *
     * @param backupPrefix the filename prefix to add.
     * @throws Exception an error occurred.
     */
    public static void backupDB(final int scenarioId, final int gameId, final String backupPrefix)
            throws Exception {


        final String backupDB;
        switch (scenarioId) {
            case HibernateUtil.DB_FREE:
                backupDB = "empire-free";
                break;

            case HibernateUtil.DB_S3:
                backupDB = "empire-s3";
                break;

            case HibernateUtil.DB_S2:
                backupDB = "empire-s2";
                break;

            case HibernateUtil.DB_S1:
            default:
                backupDB = "empire-s1";
                break;
        }

        final StringBuilder ignoreTables = new StringBuilder();
        ignoreTables.append("--ignore-table=").append(backupDB).append(".game_settings ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".orders ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".orders_goods ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".users_games ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".watch_games ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_armytypes ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_commander_names ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".TBL_FIELD_BATTLE_MAP_EXTRA_FEATURES ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".TBL_FIELD_BATTLE_TERRAINS ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_goods ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_nations ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_naturalresources ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_productionsites ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_ranks ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_regions ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_shiptypes ");
        ignoreTables.append("--ignore-table=").append(backupDB).append(".tbl_terrains ");

        // Device filename
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ENGLISH);
        final String filename = BACKUP_BASE + "-s" + scenarioId + "-g" + gameId + "-" + formatter.format(new java.util.Date()) + "-" + backupPrefix + ".sql";

        // Execute Shell Command
        final String executeCmd = "mysqldump -u " + BACKUP_USER + " --password=" + BACKUP_PWD + " " + backupDB + " " + ignoreTables.toString() + " -r " + filename;
        final Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
        final int processComplete = runtimeProcess.waitFor();
        if (processComplete == 0) {
            LOGGER.info("Backup taken successfully");

            final String compressCmd = "bzip2 -z " + filename;
            final Process execCompress = Runtime.getRuntime().exec(compressCmd);
            final int compressComplete = execCompress.waitFor();
            if (compressComplete == 0) {
                LOGGER.info("Backup compressed successfully");

                // Move log file
                final File backupFrom = new File(filename + ".bz2");

                final String backupToFilename = BACKUP_BASE + "-s" + scenarioId + "-" + backupPrefix + ".sql";
                final File backupTo = new File(backupToFilename);
                copyFile(backupFrom, backupTo);

            } else {
                LOGGER.error("Could not compress mysql backup");
            }
        } else {
            LOGGER.error("Could not take mysql backup");
        }
    }

    public static void restoreDB()
            throws Exception {
        // Decompress backup file
        final String decompressCmd = "bzip2 -d " + BACKUP_LAST + ".bz2";
        final Process execDeCompress = Runtime.getRuntime().exec(decompressCmd);
        final int compressDeComplete = execDeCompress.waitFor();
        if (compressDeComplete == 0) {
            LOGGER.info("Backup decompressed successfully");

        } else {
            LOGGER.error("Could not decompress mysql backup");
        }

        // Execute Shell Command
        final String[] executeCmd = new String[]{"mysql", "--user=" + BACKUP_USER, "--password=" + BACKUP_PWD, BACKUP_DB, "-e", "source " + BACKUP_LAST};
        final Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
        final int processComplete = runtimeProcess.waitFor();
        if (processComplete == 0) {
            LOGGER.info("Restore completed successfully");

        } else {
            LOGGER.error("Could not restore mysql");
        }

        // Decompress backup file
        final String compressCmd = "bzip2 -z " + BACKUP_LAST;
        final Process execCompress = Runtime.getRuntime().exec(compressCmd);
        final int compressComplete = execCompress.waitFor();
        if (compressComplete == 0) {
            LOGGER.info("Backup re-compressed successfully");

        } else {
            LOGGER.error("Could not re-compress mysql backup");
        }
    }

    public static void copyFile(final File f1, final File f2) {
        try {
            final InputStream in = new FileInputStream(f1);

            //For Overwrite the file.
            final OutputStream out = new FileOutputStream(f2);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            LOGGER.debug("Backup file set to last.");

        } catch (FileNotFoundException ex) {
            LOGGER.error(ex.getMessage() + " in the specified directory.");

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Detect the nations that are still alive.
     */
    private void detectAliveNations() {
        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        aliveNations.clear();
        aliveNationsId.clear();
        userNations.clear();

        // Identify alive nations
        final List<Report> lstReports = ReportManager.getInstance().listByTurnKey(getGame(), getGame().getTurn() - 1, N_ALIVE);
        for (final Report report : lstReports) {
            if ("1".endsWith(report.getValue())) {
                aliveNations.add(report.getNation());
                aliveNationsId.add(report.getNation().getId());
            }
        }
        Collections.sort(aliveNations, new NationId());

        // Load users playing each nation
        final List<UserGame> lstUserGame = UserGameManager.getInstance().list(getGame());
        for (final UserGame userGame : lstUserGame) {
            if (userGame.isActive() && userGame.isAlive()) {
                userNations.put(userGame.getNation().getId(), UserManager.getInstance().getByID(userGame.getUserId()));
            }
        }

        // For all other active positions use admin account
        final User admin = UserManager.getInstance().getByID(2);
        final List<Nation> lstNation = NationManager.getInstance().list();
        for (final Nation nation : lstNation) {
            if (!userNations.containsKey(nation.getId())) {
                userNations.put(nation.getId(), admin);
            }
        }

        theTrans.commit();
        mainTrans.commit();
    }

    /**
     * Evaluates if the particular nation is alive.
     *
     * @param owner the nation to check.
     * @return true, if it is still alive.
     */
    public boolean isAlive(final Nation owner) {
        return aliveNationsId.contains(owner.getId());
    }

    /**
     * Access the user playing the nation.
     *
     * @param owner the nation to check.
     * @return the user object.
     */
    public User getUser(final Nation owner) {
        return userNations.get(owner.getId());
    }

    /**
     * Get list of alive nations.
     *
     * @return a list of alive nations.
     */
    public List<Nation> getAliveNations() {
        final List<Nation> newList = new ArrayList<Nation>();
        for (final Nation nation : aliveNations) {
            newList.add(NationManager.getInstance().getByID(nation.getId()));
        }
        return newList;
    }

    private Calendar registerProcess(final EngineProcess thisProc, final long tsStart) {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        game.setDateLastProc(new Date());
        game.setStatus(GAME_READY);

        // Identify date of next processing
        final Calendar nextTurn = Calendar.getInstance();
        nextTurn.set(Calendar.HOUR_OF_DAY, 0);
        nextTurn.set(Calendar.MINUTE, 0);
        nextTurn.set(Calendar.SECOND, 0);

        if (game.getSchedule() > 0) {
            // Day-based periodic schedule
            nextTurn.add(Calendar.DATE, game.getSchedule() - 1);

        } else {
            // Custom schedule
            try {
                final CronExpression cexp = new CronExpression(game.getCronSchedule());
                nextTurn.setTime(cexp.getNextValidTimeAfter(new Date()));

            } catch (Exception ex) {
                LOGGER.error(ex);
                LOGGER.info("Setting next processing date after 7 days.");
                nextTurn.add(Calendar.DATE, 7);
            }
        }

        game.setDateNextProc(nextTurn.getTime());
        GameManager.getInstance().update(getGame());
        theTrans.commit();

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        thisProc.setDuration((int) ((System.currentTimeMillis() - tsStart) / 1000d));
        EngineProcessManager.getInstance().update(thisProc);
        mainTrans.commit();

        return nextTurn;
    }

    private void increaseTurnCounter() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        game.setTurn(getGame().getTurn() + 1);
        theTrans.commit();
    }

    /**
     * Report the warehouses of each nation.
     */
    private void reportWarehouses() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        final List<Warehouse> listWH = WarehouseManager.getInstance().listByGame(getGame());
        for (final Warehouse thisWH : listWH) {
            for (Map.Entry<Integer, Integer> entry : thisWH.getStoredGoodsQnt().entrySet()) {
                report(thisWH.getNation(), W_REGION + thisWH.getRegion().getId() + W_GOOD + entry.getKey(), entry.getValue());
            }
        }
        theTrans.commit();
    }

    /**
     * Removes any dead commander.
     */
    private void removeDeadCommanders() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        final List<Commander> listCom = CommanderManager.getInstance().listByGame(getGame());
        for (final Commander commander : listCom) {
            if (commander.getDead()) {
                LOGGER.debug("Removing Dead Commander " + commander.getName() + " (" + commander.getNation().getName() + "," + commander.getPosition().toString() + ") from service");

                if (commander.getArmy() != 0) {
                    removeFromArmy(commander.getArmy());
                    commander.setArmy(0);
                }

                if (commander.getCorp() != 0) {
                    removeFromCorp(commander.getCorp());
                    commander.setCorp(0);
                }

                // check if commander is loaded
                if (commander.getCarrierInfo().getCarrierId() != 0) {
                    if (commander.getCarrierInfo().getCarrierType() == ArmyConstants.SHIP) {
                        final Ship thisShip = ShipManager.getInstance().getByID(commander.getCarrierInfo().getCarrierId());
                        if (thisShip != null) {
                            // unload unit from carrier
                            final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
                            int thisKey = 0;

                            // Check if a unit is loaded in the carrier
                            for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                    if (entry.getKey() >= ArmyConstants.COMMANDER * 1000
                                            && entry.getKey() < (ArmyConstants.COMMANDER + 1) * 1000
                                            && entry.getValue() == commander.getId()) {
                                        thisKey = entry.getKey();
                                        break;
                                    }
                                }
                            }

                            if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == commander.getId()) {
                                storedGoods.remove(thisKey);
                            }

                            ShipManager.getInstance().update(thisShip);
                        }

                    } else if (commander.getCarrierInfo().getCarrierType() == ArmyConstants.BAGGAGETRAIN) {
                        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(commander.getCarrierInfo().getCarrierId());
                        if (thisTrain != null) {
                            // unload unit from carrier
                            final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
                            int thisKey = 0;

                            // Check if a unit is loaded in the carrier
                            for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                    if (entry.getKey() >= ArmyConstants.COMMANDER * 1000
                                            && entry.getKey() < (ArmyConstants.COMMANDER + 1) * 1000
                                            && entry.getValue() == commander.getId()) {
                                        thisKey = entry.getKey();
                                        break;
                                    }
                                }
                            }

                            if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == commander.getId()) {
                                storedGoods.remove(thisKey);
                            }

                            BaggageTrainManager.getInstance().update(thisTrain);
                        }
                    }

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    commander.setCarrierInfo(thisCarrying);
                }

                CommanderManager.getInstance().update(commander);
            }
        }
        theTrans.commit();
    }

    protected void removeFromArmy(final int armyId) {
        // Retrieve army
        final Army thisArmy = ArmyManager.getInstance().getByID(armyId);

        if (thisArmy != null) {

            // remove commander
            thisArmy.setCommander(null);

            // update entity
            ArmyManager.getInstance().update(thisArmy);
        }
    }

    protected void removeFromCorp(final int corpId) {
        // Retrieve corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp != null) {

            // remove commander
            thisCorp.setCommander(null);

            // update entity
            CorpManager.getInstance().update(thisCorp);
        }
    }

    /**
     * Transfer the VPs of the previous turn to this one.
     * Apply VP loss.
     * Each month every major power loses a % of their victory points,
     * randomly rolled between 1%, 2%, 3% or 4%. (25% chance for each).
     * The same roll applied all countries for each turn.
     * Results are rounded DOWN
     */
    private void reportVP() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        //final int loss = 100 - (getRandomGen().nextInt(4) + 1);

        final List<Nation> listNation = NationManager.getInstance().list();
        listNation.remove(0); // Remove FREE nation.
        for (final Nation nation : listNation) {
            final int lastTurnVPs = Integer.parseInt(getReport(nation, game.getTurn() - 1, N_VP));
            //final int thisTurnBaseVPs = (int) Math.floor(lastTurnVPs * loss / 100d);
            report(nation, game.getTurn(), N_VP, lastTurnVPs);
        }
        theTrans.commit();
    }

    /**
     * Determine if the next game turn will suffer from severe winter conditions.
     */
    private void determineSevereWinter() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        // identify month
        final Calendar thisCal = calendar();

        final int rollArctic = getRandomGen().nextInt(100) + 1;
        boolean hasArctic;
        final int rollCentral = getRandomGen().nextInt(100) + 1;
        boolean hasCentral;
        final int rollMediterranean = getRandomGen().nextInt(100) + 1;
        boolean hasMediterranean;

        switch (thisCal.get(Calendar.MONTH)) {

            case Calendar.OCTOBER:
                hasArctic = (rollArctic <= 17);
                hasCentral = (rollCentral <= 0);
                hasMediterranean = (rollMediterranean <= 0);
                break;

            case Calendar.NOVEMBER:
                hasArctic = (rollArctic <= 50);
                hasCentral = (rollCentral <= 17);
                hasMediterranean = (rollMediterranean <= 0);
                break;

            case Calendar.DECEMBER:
                hasArctic = (rollArctic <= 100);
                hasCentral = (rollCentral <= 50);
                hasMediterranean = (rollMediterranean <= 17);
                break;

            case Calendar.JANUARY:
                hasArctic = (rollArctic <= 100);
                hasCentral = (rollCentral <= 83);
                hasMediterranean = (rollMediterranean <= 50);
                break;

            case Calendar.FEBRUARY:
                hasArctic = (rollArctic <= 67);
                hasCentral = (rollCentral <= 67);
                hasMediterranean = (rollMediterranean <= 33);
                break;

            case Calendar.MARCH:
                hasArctic = (rollArctic <= 33);
                hasCentral = (rollCentral <= 33);
                hasMediterranean = (rollMediterranean <= 17);
                break;

            case Calendar.APRIL:
                hasArctic = (rollArctic <= 17);
                hasCentral = (rollCentral <= 0);
                hasMediterranean = (rollMediterranean <= 0);
                break;

            default:
                hasArctic = false;
                hasCentral = false;
                hasMediterranean = false;
        }

        final Nation freeNation = NationManager.getInstance().getByID(-1);
        report(freeNation, "winter.arctic", hasArctic);
        report(freeNation, "winter.central", hasCentral);
        report(freeNation, "winter.mediterranean", hasMediterranean);

        final List<Nation> lstNations = NationManager.getInstance().list();
        if (hasArctic) {
            int firstNews = 0;
            for (final Nation lstNation : lstNations) {
                final int thisNews = news(lstNation, lstNation, getGame().getTurn() - 1, NewsConstants.NEWS_WORLD, firstNews, "Arctic zone is experiencing severe winter this month.");
                if (firstNews == 0) {
                    firstNews = thisNews;
                }
            }
        }

        if (hasCentral) {
            int firstNews = 0;
            for (final Nation lstNation : lstNations) {
                final int thisNews = news(lstNation, lstNation, getGame().getTurn() - 1, NewsConstants.NEWS_WORLD, firstNews, "Central European zone is experiencing severe winter this month.");
                if (firstNews == 0) {
                    firstNews = thisNews;
                }
            }
        }

        if (hasMediterranean) {
            int firstNews = 0;
            for (final Nation lstNation : lstNations) {
                final int thisNews = news(lstNation, lstNation, getGame().getTurn() - 1, NewsConstants.NEWS_WORLD, firstNews, "Mediterranean is experiencing severe winter this month.");
                if (firstNews == 0) {
                    firstNews = thisNews;
                }
            }
        }

        theTrans.commit();
    }

    /**
     * Determine if the next game turn will suffer from severe winter conditions.
     */
    private void determineSevereWinter1808() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        // identify month
        final Calendar thisCal = calendar();

        final int rollMediterranean = getRandomGen().nextInt(100) + 1;
        boolean hasMediterranean;

        switch (thisCal.get(Calendar.MONTH)) {

            case Calendar.DECEMBER:
                hasMediterranean = (rollMediterranean <= 50);
                break;

            case Calendar.JANUARY:
                hasMediterranean = (rollMediterranean <= 70);
                break;

            case Calendar.FEBRUARY:
                hasMediterranean = (rollMediterranean <= 50);
                break;

            default:
                hasMediterranean = false;
        }

        final Nation freeNation = NationManager.getInstance().getByID(-1);
        report(freeNation, "winter.arctic", hasMediterranean);
        report(freeNation, "winter.central", hasMediterranean);
        report(freeNation, "winter.mediterranean", hasMediterranean);

        final List<Nation> lstNations = NationManager.getInstance().list();
        if (hasMediterranean) {
            int firstNews = 0;
            for (final Nation lstNation : lstNations) {
                final int thisNews = news(lstNation, lstNation, getGame().getTurn() - 1, NewsConstants.NEWS_WORLD, firstNews, "We are experiencing severe winter this month.");
                if (firstNews == 0) {
                    firstNews = thisNews;
                }
            }
        }

        theTrans.commit();
    }

    /**
     * Produce maps and tiles for each nation for this game/turn.
     */
    private void fogOfWar() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

        // Remove existing FOW flags
        SectorManager.getInstance().removeFOW(game);

        final Map<Nation, Map<Sector, BigInteger>> listLC = BattalionManager.getInstance().listLCBattalions(getGame());
        final List<Nation> lstNations;
        final List<Region> lstRegion;

        switch (game.getScenarioId()) {
            case HibernateUtil.DB_FREE:
                lstNations = new ArrayList<Nation>();
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));

                lstRegion = new ArrayList<Region>();
                lstRegion.add(RegionManager.getInstance().getByID(RegionConstants.EUROPE));
                lstRegion.add(RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN));
                break;

            case HibernateUtil.DB_MAIN:
            default:
                lstNations = getAliveNations();
                lstRegion = RegionManager.getInstance().list();
                break;
        }

        theTrans.commit();

        // Call Game specific pages.
        final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (final Region region : lstRegion) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    final Map<Integer, Sector> sectorMap = new HashMap<Integer, Sector>();
                    final Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);

                    for (final Nation nation : lstNations) {
                        LOGGER.debug("Applying fog-of-war rules for " + nation.getName() + "/" + region.getName());

                        // Produce separate list of nations
                        final List<Nation> localList = NationManager.getInstance().list();
                        localList.remove(0); // Remove neutrals

                        final String token = "*" + Integer.toString(nation.getId()) + "*";

                        // Update sectors with FOW visibility
                        final FogOfWarInspector fOWsg = new FogOfWarInspector(getGame(), region, nation, localList, listLC);
                        for (final Sector sector : fOWsg.getVisibleSectors()) {
                            if (sector != null) {
                                final Sector thatSector;
                                if (sectorMap.containsKey(sector.getId())) {
                                    thatSector = sectorMap.get(sector.getId());

                                } else {
                                    sectorMap.put(sector.getId(), sector);
                                    thatSector = sector;
                                }

                                if (!thatSector.getFow().contains(token)) {
                                    thatSector.setFow(thatSector.getFow() + token);
                                }
                            }
                        }
                    }

                    // Update Sector information
                    for (final Sector sector : sectorMap.values()) {
                        SectorManager.getInstance().update(sector);
                    }

                    thatTrans.commit();
                    return true;
                }
            }));
        }

        // wait for the execution all tasks
        try {
            // wait for all tasks to complete before continuing
            for (Future<Boolean> task : futures) {
                task.get();
            }

            executorService.shutdownNow();

        } catch (Exception ex) {
            LOGGER.error("Task execution interrupted", ex);
        }
    }

    /**
     * Produce maps and tiles for this game/turn.
     */
    private void produceImages() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioID);
        final Nation neutral = NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL);

        final List<Nation> lstNations;
        switch (scenarioID) {
            case HibernateUtil.DB_FREE:
                lstNations = new ArrayList<Nation>();

                // add single nation
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                break;

            case HibernateUtil.DB_S3:
                lstNations = new ArrayList<Nation>();
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_SPAIN));
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN));
                break;

            case HibernateUtil.DB_S1:
            case HibernateUtil.DB_S2:
            default:
                lstNations = NationManager.getInstance().list();
                lstNations.remove(0);
                break;
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(GameEngine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        final List<Region> lstRegion = RegionManager.getInstance().list();
        for (final Region region : lstRegion) {
            final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(game, region);

            futures.add(executorService.submit(new MapRegionExecutor(game, basePath, neutral, region, sectorList)));
            LOGGER.info("Task (Region) submitted for execution [" + region.getId() + "]");

            if (!game.isFogOfWar()) {
                futures.add(executorService.submit(new MapNationExecutor(game, basePath, lstNations.get(0), region, sectorList)));
                LOGGER.info("Task (Region,Nation) submitted for execution [" + region.getId() + "]");

            } else {
                for (final Nation nation : lstNations) {
                    futures.add(executorService.submit(new MapNationExecutor(game, basePath, nation, region, sectorList)));
                    LOGGER.info("Task (Region,Nation) submitted for execution [" + region.getId() + "/" + nation.getCode() + "]");
                }
            }
        }

        // wait for the execution all tasks
        try {
            // wait for all tasks to complete before continuing
            for (Future<Boolean> task : futures) {
                task.get();
            }

            executorService.shutdownNow();

        } catch (Exception ex) {
            LOGGER.error("Task execution interrupted", ex);
        }

        theTrans.commit();
    }

    /**
     * Retrieve a report entry for this turn.
     *
     * @param owner the owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    public String getReport(final Nation owner, final int turn, final String key) {
        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getGame(), turn, key);
        if (thisReport == null) {
            return "0";
        } else {
            return thisReport.getValue();
        }
    }

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final String key, final boolean value) {
        if (value) {
            report(owner, game.getTurn(), key, "1");

        } else {
            report(owner, game.getTurn(), key, "0");
        }
    }

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final String key, final int value) {
        report(owner, game.getTurn(), key, Integer.toString(value)); // NOPMD
    }

    /**
     * Add a report entry for another turn.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final int turn, final String key, final int value) {
        report(owner, turn, key, Integer.toString(value)); // @NOPMD
    }

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final int turn, final String key, final String value) {
        final Report thisReport = new Report();
        thisReport.setGame(getGame());
        thisReport.setTurn(turn);
        thisReport.setNation(owner);
        thisReport.setKey(key);
        thisReport.setValue(value);
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param isGlobal     the message is global to all visitors.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Nation nation,
                       final Nation subject,
                       final int type,
                       final int baseNewsId,
                       final boolean isGlobal,
                       final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(getGame());
        thisNewsEntry.setTurn(getGame().getTurn());
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setGlobal(isGlobal);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param turnId       the turn to report the news.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Nation nation, final Nation subject, final int turnId, final int type, final int baseNewsId, final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(getGame());
        thisNewsEntry.setTurn(turnId);
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setGlobal(false);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
    }

    /**
     * Returns the html code of the specific article.
     *
     * @param scenarioId the scenario to build.
     */
    public void getBuild(final int scenarioId) {
        // Construct Build URL
        final StringBuilder url = new StringBuilder();
        url.append(JENKINS);
        url.append("/job/");

        switch (scenarioId) {
            case HibernateUtil.DB_FREE:
                url.append("Engine%20(Scenario%201804)");
                break;

            case HibernateUtil.DB_S3:
                url.append("Engine%20(Scenario%201808)");
                break;

            case HibernateUtil.DB_S2:
                url.append("Engine%20(Scenario%201805)");
                break;

            case HibernateUtil.DB_S1:
            default:
                url.append("Engine%20(Scenario%201802)");
        }

        url.append("/build?token=eawBUILDtoken999888");

        try {
            // Create your httpclient
            final DefaultHttpClient client = new DefaultHttpClient();

            // set max timeout parameters
            final HttpParams params = client.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 10000);
            HttpConnectionParams.setSoTimeout(params, 10000);

            // Then provide the right credentials
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials("engine", "eng123eaw"));

            // Generate BASIC scheme object and stick it to the execution context
            final BasicScheme basicAuth = new BasicScheme();
            final BasicHttpContext context = new BasicHttpContext();
            context.setAttribute("preemptive-auth", basicAuth);

            // Add as the first (because of the zero) request interceptor
            // It will first intercept the request and preemptively initialize the authentication scheme if there is not
            client.addRequestInterceptor(new PreemptiveAuth(), 0);

            LOGGER.info("Posting request to JENKINS [" + url.toString() + "]");
            HttpGet get = new HttpGet(url.toString());

            // Execute your request with the given context
            HttpResponse response = client.execute(get, context);
            final HttpEntity entity = response.getEntity();
            //EntityUtils.consume(entity);

        } catch (final IOException e) {
            LOGGER.error("Error while contacting JENKINS", e);
        }
    }

    /**
     * Preemptive authentication interceptor
     */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        /*
           * (non-Javadoc)
           *
           * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.HttpRequest,
           * org.apache.http.protocol.HttpContext)
           */
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            // Get the AuthState
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost
                            .getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(creds);
                }
            }

        }

    }

    /**
     * Simple execution.
     *
     * @param args no arguments needed here
     */
    public static void main(final String[] args)
            throws Exception {

        // check arguments
        if (args.length < 3) {
            LOGGER.fatal("Engine arguments (gameId, scenarioId, basePath, buildNumber) are missing [" + args.length + "]");
            return;
        }

        // Retrieve gameId
        int gameId = 0;
        try {
            gameId = Integer.parseInt(args[0]);

        } catch (Exception ex) {
            LOGGER.warn("Could not parse gameId");
        }

        // Check if this is a restore command
        if (gameId == -2) {
            LOGGER.info("Attempting to restore database.");
            try {
                restoreDB();
            } catch (Exception ex) {
                LOGGER.fatal("Restore failed", ex);
            }
            return;
        }

        // Retrieve scenarioId
        int scenarioId = 0;
        try {
            scenarioId = Integer.parseInt(args[1]);

        } catch (Exception ex) {
            LOGGER.warn("Could not parse scenarioId");
        }

        String basePath = "/srv/eaw1805";
        if (args.length > 2) {
            basePath = args[2];
        } else {
            LOGGER.warn("Using default path: " + basePath);
        }

        // Check if we have a build number
        int buildNumber = -1;
        if (args.length > 3) {
            try {
                buildNumber = Integer.parseInt(args[3]);

            } catch (Exception ex) {
                LOGGER.warn("No BUILD_NUMBER provided");
            }
        } else {
            LOGGER.warn("No BUILD_NUMBER provided");
        }

        // Set the session factories to all stores
        int nextGameId = 1;
        if (gameId == 0) {//if it is new game creation... calculate its new id before anything else...
            for (int db = HibernateUtil.DB_FIRST; db <= HibernateUtil.DB_LAST; db++) {
                HibernateUtil.connectEntityManagers(db);
                Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(db);
                final List<Game> list = GameManager.getInstance().list();
                if (!list.isEmpty()) {
                    final Game lastGame = list.get(list.size() - 1);
                    if (nextGameId < lastGame.getGameId() + 1) {
                        nextGameId = lastGame.getGameId() + 1;
                    }
                }

                thatTrans.commit();
            }
        }

        HibernateUtil.connectEntityManagers(scenarioId);

        // check if another instance of the engine is active
        Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final List<Game> underProcess = GameManager.getInstance().listProcessed();
        thatTrans.rollback();
        if (gameId == -1 && !underProcess.isEmpty()) {
            // Send mail
            EmailManager.getInstance().sendContact("dim1988g@hotmail.com", "dimitris", "Last instance of the engine exited abnormally.", "Last instance of the engine exited abnormally.");
            EmailManager.getInstance().sendContact("ichatz@gmail.com", "ichatz", "Last instance of the engine exited abnormally.", "Last instance of the engine exited abnormally.");

            LOGGER.fatal("Last instance of the engine exited abnormally.");
            return;
        }

        // Make sure we have an active transaction
        thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);

        int turnId = 0;
        boolean setupGame = false;
        switch (gameId) {
            case -1:
                // Locate next ready game
                final List<Game> listReady = GameManager.getInstance().listReady();
                if (listReady.isEmpty()) {
                    LOGGER.info("No Games ready to be processed");

                    // Update the Engine Process Table
                    final Transaction thisTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                    EngineProcessManager.getInstance().updateNegativeProcID();
                    thisTrans.commit();
                    return;

                } else {
                    // Retrieve game info
                    final Game nextGame = listReady.get(0);
                    gameId = nextGame.getGameId();
                    LOGGER.info("Inspecting Game " + gameId);

                    if (nextGame.getTurn() < 0) {
                        setupGame = true;
                        LOGGER.info("Initializing Game " + gameId);

                    } else {
                        turnId = nextGame.getTurn();
                        LOGGER.info("Processing Game " + gameId);
                    }
                }
                break;

            case 0:
                // Create a new game
                gameId = nextGameId;
                turnId = -1;
                setupGame = true;

                LOGGER.info("Create new Game");
                break;

            case -1000:
                final List<Game> games = GameManager.getInstance().listUnderConstruction();
                if (!games.isEmpty()) {
                    gameId = games.get(0).getGameId();
                    turnId = -1;
                    setupGame = true;
                    LOGGER.fatal("Game " + gameId + " pending creation.");

                } else {
                    LOGGER.fatal("No games for construction.");
                    thatTrans.rollback();
                    return;
                }

                break;

            default:
                Game game = GameManager.getInstance().getByID(gameId);
                if (GameConstants.GAME_CREATE.equals(game.getStatus())) {
                    gameId = game.getGameId();
                    turnId = -1;
                    setupGame = true;
                    LOGGER.fatal("Game pending creation.");
                }
                // Process the requested game

        }

        thatTrans.commit();

        // Update the Engine Process Table
        thatTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        EngineProcessManager.getInstance().updateNegativeProcID();
        thatTrans.commit();

        try {
            // If this is production system, backup data before processing
            if (buildNumber > 0 && scenarioId >= HibernateUtil.DB_S1) {
                // do not backup solo games
                backupDB(scenarioId, gameId, BACKUP_FIRST);
            }

            final GameEngine thisGE = new GameEngine(gameId, scenarioId, basePath, buildNumber);
            thisGE.init();

            if (!setupGame) {
                thisGE.process();
            }

            // Move log file
            final File logFileFrom = new File(basePath + "/engine/empire-engine.log");
            final File logFileTo = new File(basePath + "/log/empire-engine-s" + scenarioId + "-" + gameId + "-" + turnId + ".log");
            copyFile(logFileFrom, logFileTo);
            final boolean result = logFileFrom.delete();
            if (!result) {
                LOGGER.error("Could not archive log file");
            }

            // If this is production system, backup data after processing
            if (buildNumber > 0) {
                // do not backup solo games
                if (scenarioId >= HibernateUtil.DB_S1) {
                    backupDB(scenarioId, gameId, BACKUP_LAST);
                }

                // Re-exec game engine in case more than 1 game are scheduled for the same day.
                thisGE.getBuild(scenarioId);
            }

        } catch (Exception ex) {
            LOGGER.fatal(ex.getMessage(), ex);
        }

        // close sessions
        HibernateUtil.getInstance().closeSessions();
    }

}
