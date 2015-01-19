package com.eaw1805.core;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.battles.FieldBattleReportManager;
import com.eaw1805.data.managers.field.UserFieldBattleManager;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.UserFieldBattle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FieldBattleEngine {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(FieldBattleEngine.class);

    private static final String BACKUP_BASE = "empire-backup";
    private static final String BACKUP_FIRST = "first";
    private static final String BACKUP_LAST = "last";

    private static final String BACKUP_USER = "empire";
    private static final String BACKUP_PWD = "empire123";

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
        final String filename = BACKUP_BASE + "-s" + scenarioId + "-fb" + gameId + "-" + formatter.format(new java.util.Date()) + "-" + backupPrefix + ".sql";

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


    public final static void main(String[] args) {

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

        LOGGER.info("searching for fieldbattles to process");
        final Calendar now = Calendar.getInstance();
        HibernateUtil.connectEntityManagers(scenarioId);
        Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final List<FieldBattleReport> battles = FieldBattleReportManager.getInstance().list();
        thatTrans.commit();
        if (buildNumber != -1) {
            try {
                backupDB(scenarioId, gameId, BACKUP_FIRST);
            } catch (Exception e) {
                LOGGER.error("Failed to create backup first. " , e);
            }
        }

        for (FieldBattleReport battle : battles) {
            LOGGER.info("should? fieldbattle #" + battle.getBattleId() + " round : " + battle.getRound() + " date? " + battle.getNextProcessDate());
            if (battle.getNextProcessDate() != null && battle.getNextProcessDate().before(now.getTime()) && battle.getWinner() == -1) {
                //if is scenario game... never execute it.
                if (battle.isScenarioBattle()) {
                    LOGGER.info("skipping scenario battle " + battle.getBattleId());
                    continue;
                }

                if (battle.getStatus() != null && battle.getStatus().equalsIgnoreCase("Being processed")) {
                    LOGGER.info("skipping current battle because status = " + battle.getStatus());
                    continue;
                }

                if (battle.isGameEnded()) {
                    LOGGER.info("Game has ended... skipping battle");
                    continue;
                }

                LOGGER.info("processing fieldbattle #" + battle.getBattleId() + " round : " + battle.getRound());
                thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
                battle.setStatus("Being processed");
                FieldBattleReportManager.getInstance().update(battle);
                thatTrans.commit();

                final FieldBattleProcessor fieldBattleProcessor = new FieldBattleProcessor(true, scenarioId);
                if (battle.getRound() <= 0) {
                    fieldBattleProcessor.processFirstHalfRounds(battle.getBattleId());
                } else {
                    fieldBattleProcessor.processSecondHalfRounds(battle.getBattleId());
                }

                //update battle status.. retrieve it from database again to be sure we don't have old data in this instance
                thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
                FieldBattleReport report = FieldBattleReportManager.getInstance().getByID(battle.getBattleId());
                report.setStatus("ready");
                report.setNextProcessDate(null);//mark it as null so it will not be processed again...
                FieldBattleReportManager.getInstance().update(report);
                List<UserFieldBattle> usersField = UserFieldBattleManager.getInstance().listByBattleId(report.getBattleId());
                for (UserFieldBattle userField : usersField) {
                    userField.setReady(false);
                    UserFieldBattleManager.getInstance().update(userField);
                }
                thatTrans.commit();
                LOGGER.info("done processing fieldbattle #" + battle.getBattleId() + " round : " + battle.getRound());
            }
        }
        if (buildNumber != -1) {
            try {
                backupDB(scenarioId, gameId, BACKUP_LAST);
            } catch (Exception e) {
                LOGGER.error("Failed to create backup last. " , e);
            }
        }
    }
}
