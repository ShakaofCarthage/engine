package com.eaw1805.core.test;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.io.File;
import java.util.List;

/**
 * Clean up old and unused map images.
 */
public class ImageCleanUp {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ImageCleanUp.class);

    public static String basePath;

    public ImageCleanUp() {
        cleanup();
    }

    public void cleanup() {
        final List<Game> lstGames = GameManager.getInstance().list();
        for (final Game game : lstGames) {
            cleanup(game);
        }
    }

    public void cleanup(final Game game) {
        LOGGER.debug("Cleaning up G" + game.getGameId());

        final StringBuilder baseName = new StringBuilder();
        baseName.append(basePath);
        baseName.append("/images/maps/s");
        baseName.append(game.getScenarioId());
        baseName.append("/");
        baseName.append(game.getGameId());
        baseName.append("/map-");
        baseName.append(game.getGameId());
        baseName.append("-");

        final List<Region> lstRegions = RegionManager.getInstance().list();
        final List<Nation> lstNations = NationManager.getInstance().list();

        for (int turn = 0; turn < game.getTurn(); turn++) {
            for (Region region : lstRegions) {
                for (Nation nation : lstNations) {
                    final StringBuilder filename = new StringBuilder();
                    filename.append(baseName);
                    filename.append(turn);
                    filename.append("-");
                    filename.append(region.getId());
                    filename.append("-");
                    filename.append(nation.getId());

                    try {
                        final File lowRes = new File(filename + ".png");
                        if (!lowRes.delete()) {
                            LOGGER.error("unable to delete file " + lowRes.toString());
                        }

                    } catch (Exception ex) {
                        LOGGER.error("unable to delete file " + filename.toString(), ex);
                    }

                    try {
                        final File lowRes = new File(filename + "-lowres.png");
                        if (!lowRes.delete()) {
                            LOGGER.error("unable to delete file " + lowRes.toString());
                        }

                    } catch (Exception ex) {
                        LOGGER.error("unable to delete file " + filename.toString(), ex);
                    }

                    try {
                        final File lowRes = new File(filename + "-vlowres.png");
                        if (!lowRes.delete()) {
                            LOGGER.error("unable to delete file " + lowRes.toString());
                        }

                    } catch (Exception ex) {
                        LOGGER.error("unable to delete file " + filename.toString(), ex);
                    }

                    try {
                        final File lowRes = new File(filename + "-border.png");
                        if (!lowRes.delete()) {
                            LOGGER.error("unable to delete file " + lowRes.toString());
                        }

                    } catch (Exception ex) {
                        LOGGER.error("unable to delete file " + filename.toString(), ex);
                    }

                    try {
                        final File lowRes = new File(filename + "-border-lowres.png");
                        if (!lowRes.delete()) {
                            LOGGER.error("unable to delete file " + lowRes.toString());
                        }

                    } catch (Exception ex) {
                        LOGGER.error("unable to delete file " + filename.toString(), ex);
                    }

                    try {
                        final File lowRes = new File(filename + "-border-vlowres.png");
                        if (!lowRes.delete()) {
                            LOGGER.error("unable to delete file " + lowRes.toString());
                        }

                    } catch (Exception ex) {
                        LOGGER.error("unable to delete file " + filename.toString(), ex);
                    }

                }
            }
        }
    }

    /**
     * Simple execution.
     *
     * @param args no arguments needed here
     */
    public static void main(final String[] args) {

        // check arguments
        if (args.length != 2) {
            LOGGER.fatal("ImageCleanUp arguments (scenarioId, basePath) are missing");
            return;
        }

        // Retrieve scenarioId
        int scenarioId = 0;
        try {
            scenarioId = Integer.parseInt(args[0]);

        } catch (Exception ex) {
            LOGGER.warn("Could not parse scenarioId");
        }

        basePath = "/srv/eaw1805";
        if (args[1].length() > 1) {
            basePath = args[1];
        } else {
            LOGGER.warn("Using default path: " + basePath);
        }

        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(scenarioId);

        // Make sure we have an active transaction
        final Transaction thatTrans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        new ImageCleanUp();
        thatTrans.commit();
    }

}
