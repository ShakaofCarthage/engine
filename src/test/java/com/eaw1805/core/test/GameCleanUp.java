package com.eaw1805.core.test;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.model.Game;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.io.File;
import java.util.Calendar;
import java.util.List;

/**
 * Clean up old solo games.
 */
public class GameCleanUp {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(GameCleanUp.class);

    private static final String BASE_PATH = "/srv/eaw1805";

    public GameCleanUp()
            throws Exception {
        cleanup();
    }

    public void cleanup()
            throws Exception {
        final List<Game> lstGames = GameManager.getInstance().list();
        // Remove scenario game
        lstGames.remove(0);
        for (final Game game : lstGames) {
            final Calendar nextProc = Calendar.getInstance();
            nextProc.setTime(game.getDateNextProc());

            final DateTime nextProcDate = new DateTime(nextProc.get(Calendar.YEAR),
                    nextProc.get(Calendar.MONTH) + 1,
                    nextProc.get(Calendar.DAY_OF_MONTH),
                    nextProc.get(Calendar.HOUR_OF_DAY),
                    nextProc.get(Calendar.MINUTE));

            final Hours hours = Hours.hoursBetween(new DateTime(), nextProcDate);

            if (hours.getHours() < -240) {
                // todo: check if player is active in another game, otherwise send questionnaire

                // This game is inactive for more than 10 days.
                LOGGER.warn("G" + game.getGameId() + " inactive for " + (hours.getHours() / 24) + " days");

                // Remove all DB records
                final Transaction delTrans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_FREE).getCurrentSession().beginTransaction();
                GameManager.getInstance().deleteGame(game.getGameId());
                delTrans.commit();

                // Remove files
                final StringBuilder baseName = new StringBuilder();
                baseName.append(BASE_PATH);
                baseName.append("/images/maps/s");
                baseName.append(game.getScenarioId());
                baseName.append("/");
                baseName.append(game.getGameId());

                final File gameDir = new File(baseName.toString());
                FileUtils.deleteDirectory(gameDir);

            } else {
                LOGGER.info("G" + game.getGameId() + " is active (last turn processing before " + hours.getHours() + " hours)");
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

        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_FREE);

        // Make sure we have an active transaction
        final Transaction thatTrans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_FREE).getCurrentSession().beginTransaction();
        new GameCleanUp();
    }
}
