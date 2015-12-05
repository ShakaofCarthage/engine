package com.eaw1805.core;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.GameConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.*;
import com.eaw1805.data.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

/**
 * Weekly notification of inactive positions.
 */
public class PositionChecker
        implements ReportConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(PositionChecker.class);

    private static final Map<String, String> STATS = new HashMap<String, String>();

    static {
        STATS.put("nation.vp", "Victory points");
        STATS.put("taxation", "Best game economy");
        STATS.put("population.size", "Largest population");
        STATS.put("sectors.size", "Largest country");
        STATS.put("armies.battalions.total", "Largest army");
        STATS.put("ships.total", "Largest navy");
        STATS.put("armies.kill", "Most casualties inflicted");
        STATS.put("armies.deaths", "Most casualties suffered");
        STATS.put("ships.sinks", "Most ship sinks/captures");
        STATS.put("ships.sinked", "Most ships sunk");
    }

    /**
     * Static Subjects for mail.
     */
    public static final String mailSubject[] = {
            "Everyone's chance to play",
            "Headless Countries need you!",
            "The Return of the King?",
            "The Patriot",
            "The Empire must strike back",
            "The Last of the Monarchs",
            "Master & Commander",
            "Waterloo will never happen",
            "Be a Braveheart"
    };

    /**
     * Static Bodies for mail.
     */
    public static final String mailBody[] = {
            "Grab your friends and join us for an Empires at War game.",
            "Dishonourable Heads of State abandoned their office to start a new career as plantation owners in the New World...  their countries now looking up to YOU for support.",
            "Your Highness, the time has come for you to step out and claim your heritage. Several Kingdoms await for their true King to come forward and lead them...",
            "The King has fled, the country is in flames. Who will rise to fight for your country's right to remain free in this world?",
            "For how long can an Empire suffer humiliation? Only until a true leader shows them how to fight back...",
            "When all else has failed, one must stand firm. In an era where heads roll and empires fall, a true leader stands his ground.",
            "Picking up the shattered remains of countries to carve Empires is a thing rare Leaders will accomplish. Will you be one of them?",
            "Give history a chance. If you look away at a time of need, others will take control of history instead of you. Can you live with that?",
            "And dying in your beds, many years from now, would you be willing to trade ALL the days, from this day to that, for one chance, just one chance, to come back here and tell our enemies that they may take our lives, but they'll never take... OUR KINGDOM!"
    };

    /**
     * Identify top 5 positions.
     *
     * @return the map with the data that will be passed to the jsp.
     */
    public LinkedList<LinkedList<Object>> getAllFreePlayedNations() {
        final LinkedList<LinkedList<Object>> nations = new LinkedList<LinkedList<Object>>();
        for (int db = HibernateUtil.DB_FIRST; db <= HibernateUtil.DB_LAST; db++) {
            HibernateUtil.connectEntityManagers(db);

            final Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(db);

            // Retrieve list of games
            final List<Game> games = GameManager.getInstance().list();
            games.remove(0); // remove "Scenario" entry

            //The free nations
            final HashMap<Game, List<Integer>> totalFreeNations = new HashMap<Game, List<Integer>>();

            //Iteration on Games
            for (final Game thisGame : games) {

                // Ignore games that have ended
                if (thisGame.getEnded()) {
                    continue;
                }

                // Ignore private games
                if (thisGame.isPrivateGame()) {
                    continue;
                }

                // check duration of game
                final double modifier;
                switch (thisGame.getType()) {
                    case GameConstants.DURATION_SHORT:
                        modifier = .7d;
                        break;

                    case GameConstants.DURATION_LONG:
                        modifier = 1.3d;
                        break;

                    case GameConstants.DURATION_NORMAL:
                    default:
                        modifier = 1d;
                }

                // Retrieve players for this game
                final List<UserGame> gameList = UserGameManager.getInstance().list(thisGame);

                // check if game is about to end
                double maxGoal = Double.MIN_VALUE;

                for (final UserGame userGame : gameList) {
                    final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(userGame.getNation(),
                            thisGame, thisGame.getTurn() - 1, N_VP);
                    final int currentVP = Integer.parseInt(thisReport.getValue());
                    final double currentGoal = 100d * currentVP / (userGame.getNation().getVpWin() * modifier);
                    maxGoal = Math.max(maxGoal, currentGoal);
                }

                // Ignore games that are about to end
                if (maxGoal >= 80d) {
                    continue;
                }

                final List<Integer> thisGameFreeNations = new ArrayList<Integer>();
                for (final UserGame userGame : gameList) {
                    // Check if nation is dead
                    if (userGame.isAlive()
                            && userGame.getGame().getTurn() > 0
                            && !userGame.isActive()) {

                        // Check if this position is entitled to special offer for 60 free credits
                        final List<UserGame> lstPrevPositions = UserGameManager.getInstance().listInActive(thisGame, userGame.getNation());
                        if (!lstPrevPositions.isEmpty()) {
                            if (thisGame.getTurn() - lstPrevPositions.get(0).getTurnDrop() >= 2) {
                                thisGameFreeNations.add(userGame.getNation().getId());
                            }
                        }
                    }
                }

                if (!thisGameFreeNations.isEmpty()) {
                    totalFreeNations.put(thisGame, thisGameFreeNations);
                }
            }

            final Set<LinkedList<Object>> nationVP = new TreeSet<LinkedList<Object>>(new Comparator<LinkedList<Object>>() {
                @Override
                public int compare(final LinkedList<Object> nation1, final LinkedList<Object> nation2) {
                    if (((Integer) nation2.getLast()).equals((Integer) nation1.getLast())) {
                        return -1;
                    } else {
                        return ((Integer) nation2.getLast()).compareTo((Integer) nation1.getLast());
                    }
                }
            });

            for (Game thisGame : totalFreeNations.keySet()) {
                // To produce the ranking of production we need to do it programmatically
                nationVP.addAll(reportNations(thisGame, ReportConstants.N_VP, totalFreeNations.get(thisGame)));
            }

            for (final LinkedList<Object> nation : nationVP) {
                final HashMap<String, Integer> unSortedMap = new HashMap<String, Integer>();
                unSortedMap.put(ReportConstants.N_VP, getPosition(ReportConstants.N_VP, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put("taxation", getPosition("taxation", (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put("population.size", getPosition("population.size", (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.E_SEC_SIZE_TOT, getPosition(ReportConstants.E_SEC_SIZE_TOT, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.A_TOT_BAT, getPosition(ReportConstants.A_TOT_BAT, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.S_TOT_SHIPS, getPositionFixed(ReportConstants.S_TOT_SHIPS, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.A_TOT_KILLS, getPosition(ReportConstants.A_TOT_KILLS, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.A_TOT_DEATHS, getPosition(ReportConstants.A_TOT_DEATHS, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.S_SINKS, getPosition(ReportConstants.S_SINKS, (Game) nation.get(0), (Nation) nation.get(1)));
                unSortedMap.put(ReportConstants.S_SINKED, getPosition(ReportConstants.S_SINKED, (Game) nation.get(0), (Nation) nation.get(1)));

                nation.addLast(sortByValue(unSortedMap));
                nation.addLast(UserGameManager.getInstance().list((Game) nation.get(0), (Nation) nation.get(1)).get(0));
                nations.addLast(nation);
            }

            thatTrans.commit();
        }

        return nations;
    }

    /**
     * Get the current calendar.
     *
     * @param thisGame the Game instance.
     * @return the calendar.
     */
    protected final Calendar calendar(final Game thisGame) {
        final Calendar thisCal = Calendar.getInstance();

        // Define starting date based on scenario.
        switch (thisGame.getScenarioId()) {
            case HibernateUtil.DB_S1:
                if (thisGame.getGameId() < 8) {
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

        thisCal.add(Calendar.MONTH, thisGame.getTurn());
        return thisCal;
    }

    protected final List<LinkedList<Object>> reportNations(final Game game, final String category, final List<Integer> nations) {
        final ArrayList<LinkedList<Object>> nationsMap = new ArrayList<LinkedList<Object>>();
        final List<Report> lstReports = ReportManager.getInstance().listByTurnKey(game, game.getTurn() - 1, category);
        for (final Report report : lstReports) {
            if (nations.contains(report.getNation().getId())) {
                final LinkedList<Object> nationReport = new LinkedList<Object>();
                nationReport.addFirst(GameManager.getInstance().getByID(game.getGameId()));
                nationReport.addLast(NationManager.getInstance().getByID(report.getNation().getId()));
                nationReport.addLast(Integer.parseInt(report.getValue()));
                nationsMap.add(nationReport);
            }
        }
        return nationsMap;
    }

    /**
     * Go through the list until you find the nation we are looking for.
     *
     * @param key        -- the key used to rank nations.
     * @param thisGame   -- the game.
     * @param thisNation -- the nation we are looking for.
     * @return the position of the nation on the link.
     */
    protected final int getPosition(final String key, final Game thisGame, final Nation thisNation) {
        final List<Nation> ranked = ReportManager.getInstance().rankNations(key, thisGame, thisGame.getTurn() - 1);

        if (ranked.isEmpty() || !ranked.contains(thisNation)) {
            return Integer.MAX_VALUE;
        }

        int rank = 1;
        for (final Nation nation : ranked) {
            if (nation.getId() == thisNation.getId()) {
                break;
            }
            rank++;
        }

        return rank;
    }

    /**
     * Go through the list until you find the nation we are looking for.
     *
     * @param key        -- the key used to rank nations.
     * @param thisGame   -- the game.
     * @param thisNation -- the nation we are looking for.
     * @return the position of the nation on the link.
     */
    protected final int getPositionFixed(final String key, final Game thisGame, final Nation thisNation) {
        final List<Nation> ranked = ReportManager.getInstance().rankNationsFixed(key, thisGame, thisGame.getTurn() - 1, 17);
        if (ranked.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int rank = 1;
        for (final Nation nation : ranked) {
            if (nation.getId() == thisNation.getId()) {
                break;
            }
            rank++;
        }

        return rank;
    }

    /**
     * Sort an <String, Integer> Map by value.
     *
     * @param map the unsorted map.
     * @return the sorted map.
     */
    private Map<String, String> sortByValue(final Map<String, Integer> map) {

        final LinkedList<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(map.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(final Map.Entry<String, Integer> o1, final Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        final LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

        for (final Map.Entry<String, Integer> thisEntry : list) {
            result.put(thisEntry.getKey(), convertRank(thisEntry.getValue()));
        }
        return result;
    }

    /**
     * Convert Rank from int to String.
     *
     * @param rank the input int
     * @return the String Rank
     */
    protected final String convertRank(final int rank) {
        final StringBuilder strBld = new StringBuilder();
        if (rank == 0 || rank == Integer.MAX_VALUE) {
            strBld.append("-");
        } else {
            strBld.append(rank);
            if (rank >= 10 && rank <= 14) {
                strBld.append("th");
            } else {
                switch ((rank % 10) + 1) {

                    case 2:
                        strBld.append("st");
                        break;

                    case 3:
                        strBld.append("nd");
                        break;

                    case 4:
                        strBld.append("rd");
                        break;

                    default:
                        strBld.append("th");
                        break;
                }
            }
        }

        return strBld.toString();
    }

    private String prepareMessage(final LinkedList<LinkedList<Object>> nations, final int topic) {
        // Prepare Email content
        final StringBuilder textContent = new StringBuilder();

        textContent.append(",\n\n");
        textContent.append(mailBody[topic]);
        textContent.append("\n\n");
        textContent.append("Take advantage of our special offer and pick-up one of the following positions to earn plenty of FREE credits:\n\n");

        // Produce List of free data
        for (LinkedList<Object> thisNationData : nations) {
            final Game thisGame = (Game) thisNationData.get(0);
            final Nation thisNation = (Nation) thisNationData.get(1);

            // Plain Text version
            textContent.append(thisNation.getName());
            textContent.append(" -- ");
            textContent.append("Game ");
            textContent.append(thisGame.getGameId());
            textContent.append(" (Scenario ");
            textContent.append(thisGame.getScenarioIdToString());
            textContent.append(")\n");

            // report most notable statistics
            textContent.append("Most notable achievements of the position:\n");
            int index = 1;
            final LinkedHashMap<String, String> statistics = (LinkedHashMap<String, String>) thisNationData.get(3);
            for (Map.Entry<String, String> entry : statistics.entrySet()) {
                textContent.append("  ");
                textContent.append(entry.getValue());
                textContent.append(" ");
                textContent.append(STATS.get(entry.getKey()));
                textContent.append("\n");
                index++;
                if (index > 3) {
                    break;
                }
            }

            textContent.append("*Pickup position & Start playing now*\n");
            textContent.append("  ");
            textContent.append("http://www.eaw1805.com/scenario/");
            textContent.append(thisGame.getScenarioIdToString());
            textContent.append("/game/");
            textContent.append(thisGame.getGameId());
            textContent.append("/pickup/");
            textContent.append(thisNation.getId());
            textContent.append("\n");

            textContent.append("More information on game:\n");
            textContent.append("  ");
            textContent.append("http://www.eaw1805.com/scenario/");
            textContent.append(thisGame.getScenarioIdToString());
            textContent.append("/game/");
            textContent.append(thisGame.getGameId());
            textContent.append("/info\n\n");
        }

        textContent.append("As soon as you pick-up one of the above positions you will receive 60 free credits, enough to play for a long time!\n\n");
        textContent.append("You will also receive a bonus of double Administrative & Command Points for this game turn.\n\n");
        textContent.append("If you have any questions or issues regarding your account, please don't hesitate to contact us at support@oplongames.com\n\n");
        textContent.append("Want to control which emails you receive from EaW 1805? Go to:\n");
        textContent.append("http://www.eaw1805.com/settings");
        textContent.append("\n\n");

        textContent.append("We hope that you enjoy playing Empires at War 1805,\nOplon Games");

        return textContent.toString();
    }

    public void testMails() {
        final LinkedList<LinkedList<Object>> nations = getAllFreePlayedNations();
        if (nations.isEmpty()) {
            LOGGER.info("No free positions found.");
            return;
        }

        // pick a subject/body based on current date
        final Calendar thisCal = Calendar.getInstance();
        final int topic = thisCal.get(Calendar.DAY_OF_YEAR) % mailSubject.length;

        // Prepare Email
        final String subject = mailSubject[topic];
        final String body = prepareMessage(nations, topic);

        final Transaction thisTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        User user = UserManager.getInstance().getByID(54);
        EmailManager.getInstance().sendUpdate(user.getEmail(), subject, "Dear " + user.getUsername() + body);

        user = UserManager.getInstance().getByID(59);
        EmailManager.getInstance().sendUpdate(user.getEmail(), subject, "Dear " + user.getUsername() + body);

        thisTrans.commit();
    }

    public void sendMails() {
        final LinkedList<LinkedList<Object>> nations = getAllFreePlayedNations();
        if (nations.isEmpty()) {
            LOGGER.info("No free positions found.");
            return;
        }

        // pick a subject/body based on current date
        final Calendar thisCal = Calendar.getInstance();
        final int topic = thisCal.get(Calendar.DAY_OF_YEAR) % mailSubject.length;
        final int weekOfYear = thisCal.get(Calendar.WEEK_OF_YEAR);
        final int week = thisCal.get(Calendar.WEEK_OF_MONTH);
        final int maxWeeknumber = thisCal.getActualMaximum(Calendar.WEEK_OF_MONTH);
        int totNotifications = 0;

        // Prepare Email
        final String subject = mailSubject[topic];
        final String body = prepareMessage(nations, topic);

        // Identify non-active users
        final Transaction thisTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        final List<User> lstUsers = UserManager.getInstance().list();
        thisTrans.commit();

        for (final User user : lstUsers) {
            if (user.getEnableNotifications()) {
                // check if user is inactive
                int totActivePos = 0;
                for (int db = HibernateUtil.DB_FIRST; db <= HibernateUtil.DB_LAST; db++) {
                    HibernateUtil.connectEntityManagers(db);
                    final Transaction nextTrans = HibernateUtil.getInstance().beginTransaction(db);
                    final List<UserGame> positions = UserGameManager.getInstance().listActive(user);
                    nextTrans.commit();

                    totActivePos += positions.size();
                }

                if (totActivePos == 0) {
                    // Send to each user only once per 2 months
                    if ((user.getUserId() % maxWeeknumber == week) && (user.getUserId() % 2 == weekOfYear % 2)) {
                        totNotifications++;
                        LOGGER.info("Informing " + user.getUsername() + "<" + user.getEmail() + ">");
                        EmailManager.getInstance().sendUpdate(user.getEmail(), subject, "Dear " + user.getUsername() + body);
                    }
                }
            }
        }

        LOGGER.info(totNotifications + " notifications sent");
    }

    /**
     * Simple execution.
     *
     * @param args no arguments needed here
     */
    public static void main(final String[] args) {
        // Introduce random delay
        final int delay = new Random().nextInt(2);

        LOGGER.info("Random delay of " + delay + " minutes");

        final Timer thisTimer = new Timer();

        thisTimer.schedule(new TimerTask() {
            public void run() {
                // Retrieve free positions and relevant data
                PositionChecker posChecker = new PositionChecker();
                posChecker.sendMails();
            }
        }, delay * 60 * 1000);

        // close sessions
        HibernateUtil.getInstance().closeSessions();
    }

}
