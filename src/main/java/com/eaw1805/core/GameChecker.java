package com.eaw1805.core;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.EngineProcessManager;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Check if players have issued orders and send out notifications when deadline is approaching.
 */
public class GameChecker
        implements ReportConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(GameChecker.class);

    public void checkGames() {
        final List<Game> lstGames = GameManager.getInstance().listAlmostReady();
        for (final Game game : lstGames) {
            checkGame(game);
        }
    }

    /**
     * Get the current calendar.
     *
     * @return the calendar.
     */
    public Calendar calendar(final Game thisGame) {
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

    private void checkGame(final Game thisGame) {
        LOGGER.info("Preparing Notifications for Game " + thisGame.getGameId());

        final Calendar turnCal = calendar(thisGame);

        final String scenarioId;
        switch (thisGame.getScenarioId()) {

            case HibernateUtil.DB_FREE:
                scenarioId = "1804";
                break;

            case HibernateUtil.DB_S3:
                scenarioId = "1808";
                break;

            case HibernateUtil.DB_S2:
                scenarioId = "1805";
                break;

            case HibernateUtil.DB_S1:
            default:
                scenarioId = "1802";
                break;
        }

        // Construct the body of the mail
        final StringBuilder mailBody = new StringBuilder();
        mailBody.append("Game ");
        mailBody.append(thisGame.getGameId());
        mailBody.append(" / ");

        mailBody.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        mailBody.append(" ");
        mailBody.append(turnCal.get(Calendar.YEAR));
        if (thisGame.getDescription() != null && thisGame.getDescription().length() > 0) {
            mailBody.append(" (");
            mailBody.append(thisGame.getDescription());
            mailBody.append(")");
        }

        mailBody.append(" will be processed in the next ");

        final Calendar nextProc = Calendar.getInstance();
        nextProc.setTime(thisGame.getDateNextProc());

        final DateTime nextProcDate = new DateTime(nextProc.get(Calendar.YEAR),
                nextProc.get(Calendar.MONTH) + 1,
                nextProc.get(Calendar.DAY_OF_MONTH),
                nextProc.get(Calendar.HOUR_OF_DAY),
                nextProc.get(Calendar.MINUTE));

        final Hours hours = Hours.hoursBetween(new DateTime(), nextProcDate);

        mailBody.append(Math.abs(hours.getHours()) + 24);
        mailBody.append(" hours.\n\n");

        // retrieve active nations
        final Set<Nation> aliveNations = detectAliveNations(thisGame);

        // check if players have submitted orders
        final List<UserGame> nationPlayers = UserGameManager.getInstance().list(thisGame);
        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        for (final UserGame userGame : nationPlayers) {
            if (userGame.isActive()
                    && userGame.isAlive()
                    && aliveNations.contains(userGame.getNation())) {
                // Find out if the player has submitted any order
                final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByGameNation(thisGame,
                        userGame.getNation(), thisGame.getTurn());

                if (lstOrders.isEmpty()) {

                    final User user = UserManager.getInstance().getByID(userGame.getUserId());
                    if (!user.getEnableNotifications()) {
                        //if user doesn't have notifications enabled... then skip him.
                        continue;
                    }
                    // No order submitted yet
                    final StringBuilder mailText = new StringBuilder();
                    mailText.append("Dear ");
                    mailText.append(user.getUsername());
                    mailText.append(",\n\n");
                    mailText.append("Our records show that you have not issued any order for your ");
                    mailText.append(userGame.getNation().getName());
                    mailText.append(", for ");
                    mailText.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
                    mailText.append(" ");
                    mailText.append(turnCal.get(Calendar.YEAR));
                    mailText.append(".\n\n");
                    mailText.append(mailBody);
                    mailText.append("Your Empire will not survive on its own. Please consider this as a kind reminder to submit orders for your nation. Your people need you...\n\n");
                    mailText.append("You can access your position from the following link:\n\n");
                    mailText.append("http://www.eaw1805.com/play/scenario/");
                    mailText.append(thisGame.getScenarioIdToString());
                    mailText.append("/game/");
                    mailText.append(thisGame.getGameId());
                    mailText.append("/nation/");
                    mailText.append(userGame.getNation().getId());
                    mailText.append("\n\n");

                    // Check if the player has issued orders during the previous turn
                    if (userGame.getTurnPickUp() < thisGame.getTurn()) {
                        final List<PlayerOrder> lstOrdersPrev = PlayerOrderManager.getInstance().listByGameNation(thisGame,
                                userGame.getNation(), thisGame.getTurn() - 1);

                        if (lstOrdersPrev.isEmpty()) {
                            // This is the second consecutive turn without orders
                            mailText.append("*NOTE* You did not issue any orders for your Empire during the previous processing. ");
                            mailText.append("Please consider that having inactive positions is seriously affecting the balance of the game.\n\n");
                            mailText.append("If the are no orders for your position during the next processing as well, the position will automatically be dropped out by the engine. \n\n");
                        }
                    }

                    mailText.append("Check out the progress of the game via the following link:\n\n");
                    mailText.append("http://www.eaw1805.com/scenario/").append(thisGame.getScenarioIdToString()).append("/game/");
                    mailText.append(thisGame.getGameId());
                    mailText.append("/info");
                    mailText.append("\n\n");

                    mailText.append("If you are experiencing a technical problem, please contact us at support@eaw1805.com.\n\n");
                    mailText.append("Enjoy your game,\nOplon Games");

                    EmailManager.getInstance().sendNotification(user.getEmail(),
                            "Your Highness, we need your orders!", mailText.toString());

                }
            }
        }
        mainTrans.commit();
    }

    /**
     * Detect the nations that are still alive.
     */
    private Set<Nation> detectAliveNations(final Game thisGame) {
        final Set<Nation> aliveNations = new HashSet<Nation>();

        // Identify alive nations
        final List<Report> lstReports = ReportManager.getInstance().listByTurnKey(thisGame, thisGame.getTurn() - 1, N_ALIVE);
        for (final Report report : lstReports) {
            if ("1".endsWith(report.getValue())) {
                aliveNations.add(report.getNation());
            }
        }

        return aliveNations;
    }

    /**
     * Simple execution.
     *
     * @param args no arguments needed here
     */
    public static void main(final String[] args) {
        //PropertyConfigurator.configure((new GameEngine()).getClass().getClassLoader().getResource("log4j.properties"));

        // check arguments
        if (args.length < 1) {
            LOGGER.fatal("Notify arguments (scenarioId) are missing [" + args.length + "]");
            return;
        }

        // Retrieve scenarioId
        int scenarioId = HibernateUtil.DB_S1;
        try {
            scenarioId = Integer.parseInt(args[0]);

        } catch (Exception ex) {
            LOGGER.warn("Could not parse scenarioId");
        }

        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(scenarioId);

        final Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        new GameChecker().checkGames();
        thatTrans.commit();

        // Update the Engine Process Table
        final Transaction thisTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        EngineProcessManager.getInstance().updateNegativeProcID();
        thisTrans.commit();

        // close sessions
        HibernateUtil.getInstance().closeSessions();
    }
}
