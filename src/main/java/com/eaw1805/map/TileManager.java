package com.eaw1805.map;

import com.eaw1805.data.constants.NationConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manage image tiles.
 */
public class TileManager {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TileManager.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static TileManager ourInstance = null;

    /**
     * Directory name containing tiles.
     */
    private final transient String basePath;

    /**
     * The images used for drawing the map.
     */
    private final transient Map<Double, Map<String, Image>> tiles = new HashMap<Double, Map<String, Image>>();

    private final transient Set<Double> zoomLevels = new HashSet<Double>();

    private TileManager(final String path) {
        // initialize manager
        basePath = path;
    }

    /**
     * AchievementManager is loaded on the first execution of AchievementManager.getInstance()
     * or the first access to AchievementManager.ourInstance, not before.
     *
     * @return ourInstance
     */
    public static TileManager getInstance() {
        synchronized (TileManager.class) {
            if (ourInstance == null) {
                ourInstance = new TileManager("/srv/eaw1805");
            }
        }
        return ourInstance;
    }

    /**
     * TileManager is loaded on the first execution of TileManager.getInstance()
     * or the first access to TileManager.ourInstance, not before.
     *
     * @return ourInstance
     */
    public static TileManager getInstance(final String thePath, final double zoom) {
        synchronized (TileManager.class) {
            if (ourInstance == null) {
                ourInstance = new TileManager(thePath);
            }

            if (!ourInstance.zoomLevels.contains(zoom)) {
                ourInstance.loadTiles(zoom);
                ourInstance.zoomLevels.add(zoom);
            }
        }

        return ourInstance;
    }

    public Image get(final double zoom, final String key) {
        return tiles.get(zoom).get(key);
    }

    private void loadTiles(final double zoomFactor) {
        final int zoomTile = Math.max(3, (int) (MapCreator.TILE_SIZE * zoomFactor));
        final int zoomElevated = Math.max(5, (int) (MapCreator.TILE_ELEVATED * zoomFactor));
        final int zoomResource = Math.max(1, (int) (zoomTile * MapCreator.ZOOM_FACTOR_RESOURCE));
        final int zoomEvent = Math.max(1, (int) (zoomTile * MapCreator.ZOOM_FACTOR_EVENT));

        final Map<String, Image> zoomTiles = new HashMap<String, Image>();
        tiles.put(zoomFactor, zoomTiles);

        // Load base tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_BASE)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_BASE + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_BASE + file, tmpImage);
        }

        // Load coastline tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_COAST)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_COAST + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_COAST + file, tmpImage);
        }

        // Load coastline tiles for Winter
        for (final String file : locateFiles(basePath + TilesSelector.DIR_COAST_WINTER)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_COAST_WINTER + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_COAST_WINTER + file, tmpImage);
        }

        // Load desert tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_DESERT)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_DESERT + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_DESERT + file, tmpImage);
        }

        // Load mountains & hills tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_ELEVATION)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_ELEVATION + "/" + file, zoomElevated);
            zoomTiles.put(TilesSelector.DIR_ELEVATION + file, tmpImage);
        }

        // Load forest tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_FOREST)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_FOREST + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_FOREST + file, tmpImage);
        }

        // Load forest winter tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_FOREST_WINTER)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_FOREST_WINTER + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_FOREST_WINTER + file, tmpImage);
        }

        // Load jungle tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_JUNGLE)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_JUNGLE + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_JUNGLE + file, tmpImage);
        }

        // Load steppe tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_STEPPE)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_STEPPE + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_STEPPE + file, tmpImage);
        }

        // Load steppe tiles for Winter
        for (final String file : locateFiles(basePath + TilesSelector.DIR_STEPPE_WINTER)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_STEPPE_WINTER + "/" + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_STEPPE_WINTER + file, tmpImage);
        }

        // Load production site tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_SITES)) {
            if (file.contains("fort") || file.contains("tprod-16-half")) {
                final Image tmpImage = loadImage(basePath + TilesSelector.DIR_SITES + "/" + file, zoomElevated);
                zoomTiles.put(TilesSelector.DIR_SITES + file, tmpImage);

            } else {
                final Image tmpImage = loadImage(basePath + TilesSelector.DIR_SITES + "/" + file, zoomTile);
                zoomTiles.put(TilesSelector.DIR_SITES + file, tmpImage);
            }
        }

        // Load storm tiles
        for (final String file : locateFiles(basePath + TilesSelector.DIR_STORMS)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_STORMS + file, zoomTile);
            zoomTiles.put(TilesSelector.DIR_STORMS + file, tmpImage);
        }

        // Load national borders
        for (int nation = NationConstants.NATION_FIRST; nation <= NationConstants.NATION_LAST; nation++) {
            for (final String file : locateFiles(basePath + TilesSelector.DIR_BORDERS + nation + "/")) {
                final Image tmpImage = loadImage(basePath + TilesSelector.DIR_BORDERS + nation + "/" + file, zoomTile);
                zoomTiles.put(TilesSelector.DIR_BORDERS + nation + "/" + file, tmpImage);
            }

            for (final String file : locateFiles(basePath + TilesSelector.DIR_BORDERS_FOW + nation + "/")) {
                final Image tmpImageFow = loadImage(basePath + TilesSelector.DIR_BORDERS_FOW + nation + "/" + file, zoomTile);
                zoomTiles.put(TilesSelector.DIR_BORDERS_FOW + nation + "/" + file, tmpImageFow);
            }
        }

        // Load natural resources
        for (final String file : locateFiles(basePath + TilesSelector.DIR_RESOURCES)) {
            final Image tmpImage = loadImage(basePath + TilesSelector.DIR_RESOURCES + "/" + file, zoomResource);
            zoomTiles.put(TilesSelector.DIR_RESOURCES + file, tmpImage);
        }

        // Load borders & events
        {
            final Image tmpImage1 = loadImage(basePath + TilesSelector.DIR_EVENTS + "epidemic.png", zoomEvent);
            zoomTiles.put(TilesSelector.DIR_EVENTS + "epidemic.png", tmpImage1);

            final Image tmpImage2 = loadImage(basePath + TilesSelector.DIR_EVENTS + "rebellion.png", zoomEvent);
            zoomTiles.put(TilesSelector.DIR_EVENTS + "rebellion.png", tmpImage2);

            final Image tmpImage3 = loadImage(basePath + TilesSelector.DIR_EVENTS + "conquer.png", zoomEvent);
            zoomTiles.put(TilesSelector.DIR_EVENTS + "conquer.png", tmpImage3);
        }
    }

    /**
     * Locate all tiles.
     *
     * @param dirName name of directory to examine.
     * @return collection of file names.
     */
    private Collection<String> locateFiles(final String dirName) {
        final ArrayList<String> allFiles = new ArrayList<String>();

        // get a list of all files in a directory
        final File dir = new File(dirName);
        if (dir.exists()) {
            final String[] files = dir.list();
            for (final String file : files) {
                // filter text documents
                if (file.endsWith(".jpg") || file.endsWith(".png")) {
                    allFiles.add(file);
                }
            }
        }

        return allFiles;
    }

    private Image loadImage(final String path, final int size) {
        Image img = null;
        try {
            img = ImageIO.read(new File(path)).getScaledInstance(size, size, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            LOGGER.fatal("Unable to load " + path, e);
        }
        return img;
    }

}
