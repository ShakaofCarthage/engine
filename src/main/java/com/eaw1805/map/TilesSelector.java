package com.eaw1805.map;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.dto.common.CoordinateDTO;
import com.eaw1805.data.dto.common.SectorDTO;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Functions that decide which tile to use when drawing a sector.
 */
public class TilesSelector
        implements TerrainConstants, ProductionSiteConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TilesSelector.class);

    public final static String DIR_BASE = "images/tiles/base/";
    public final static String DIR_COAST = "images/tiles/coast/";
    public final static String DIR_COAST_WINTER = "images/tiles/coastWinter/";
    public final static String DIR_DESERT = "images/tiles/desert/";
    public final static String DIR_ELEVATION = "images/tiles/elevation/";
    public final static String DIR_FOREST = "images/tiles/forest/";
    public final static String DIR_FOREST_WINTER = "images/tiles/forestWinter/";
    public final static String DIR_JUNGLE = "images/tiles/jungle/";
    public final static String DIR_STEPPE = "images/tiles/steppe/";
    public final static String DIR_STEPPE_WINTER = "images/tiles/steppeWinter/";
    public final static String DIR_SITES = "images/tiles/sites/";
    public final static String DIR_RESOURCES = "images/tiles/resources/";
    public final static String DIR_STORMS = "images/tiles/storms/";
    public final static String DIR_EVENTS = "images/tiles/";
    public final static String DIR_BORDERS = "images/tiles/borders/";
    public final static String DIR_BORDERS_FOW = "images/tiles/borders-fow/";

    public final static int LAYER_MOUNTAINS = 5;
    public final static int LAYER_HILLS = 1;
    public final static int LAYER_TCITY = TERRAIN_LAST;
    public final static int LAYER_PSITES = TERRAIN_LAST + 1;
    public final static int LAYER_FORT = TERRAIN_LAST + 2;
    public final static int LAYER_NATRES = TERRAIN_LAST + 3;
    public final static int LAYER_EPIDEMIC = TERRAIN_LAST + 4;
    public final static int LAYER_REBELLION = TERRAIN_LAST + 5;
    public final static int LAYER_CONQUER = TERRAIN_LAST + 6;
    public final static int LAYER_BORDERS = TERRAIN_LAST + 10;

    public final static int LAYER_STEPPE = 2;
    public final static int LAYER_DESERT = 3;
    public final static int LAYER_FOREST = TERRAIN_LAST - 1;
    public final static int LAYER_JUNGLE = TERRAIN_LAST - 2;
    public final static int LAYER_STORM = 4;

    /**
     * The sector information for each coordX,coordY.
     */
    private final transient SectorDTO[][] mapSector;

    /**
     * The list of additional tiles that are required as image overlays.
     */
    private final transient List<CoordinateDTO> sectorTiles;

    /**
     * The maximum X coordinate.
     */
    private final transient int maxX;

    /**
     * The maximum Y coordinate.
     */
    private final transient int maxY;

    /**
     * The minimumx X coordinate.
     */
    private final transient int minX;

    /**
     * The minimum Y coordinate.
     */
    private final transient int minY;

    /**
     * If arctic zone suffers from severe winter.
     */
    private final transient boolean hasArctic;

    /**
     * If central zone suffers from severe winter.
     */
    private final transient boolean hasCentral;

    /**
     * If mediterranean zone suffers from severe winter.
     */
    private final transient boolean hasMediterranean;

    private final int[] regionSizeX, regionSizeY;

    /**
     * Default constructor.
     *
     * @param sectors  a 2D array with the sectors of the map.
     * @param game     the game to examine.
     * @param thisXmax the last X coordinate to consider.
     * @param thisYmax the last Y coordinate to consider.
     * @param thisXmin the first X coordinate to consider.
     * @param thisYmin the first Y coordinate to consider.
     */
    public TilesSelector(final SectorDTO[][] sectors,
                         final Game game,
                         final int thisXmax, final int thisYmax,
                         final int thisXmin, final int thisYmin) {

        switch (game.getScenarioId()) {
            case HibernateUtil.DB_FREE:
                regionSizeX = RegionConstants.REGION_1804_SIZE_X;
                regionSizeY = RegionConstants.REGION_1804_SIZE_Y;
                break;

            case HibernateUtil.DB_S3:
                regionSizeX = RegionConstants.REGION_1808_SIZE_X;
                regionSizeY = RegionConstants.REGION_1808_SIZE_Y;
                break;

            case HibernateUtil.DB_S1:
            case HibernateUtil.DB_S2:
            default:
                regionSizeX = RegionConstants.REGION_1805_SIZE_X;
                regionSizeY = RegionConstants.REGION_1805_SIZE_Y;
                break;
        }

        mapSector = sectors;
        sectorTiles = new ArrayList<CoordinateDTO>();
        maxX = thisXmax;
        maxY = thisYmax;
        minX = thisXmin;
        minY = thisYmin;

        final Nation freeNation = NationManager.getInstance().getByID(-1);
        hasArctic = getReport(game, freeNation, "winter.arctic").equals("1");
        hasCentral = getReport(game, freeNation, "winter.central").equals("1");
        hasMediterranean = getReport(game, freeNation, "winter.mediterranean").equals("1");
    }

    /**
     * Retrieve a report entry for this turn.
     *
     * @param game  the game of the report entry.
     * @param owner the owner of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    public String getReport(final Game game, final Nation owner, final String key) {
        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, game, game.getTurn(), key);
        if (thisReport == null) {
            return "";
        } else {
            return thisReport.getValue();
        }
    }

    /**
     * Get overlay tiles.
     *
     * @return list of Coordinates with overlay tiles.
     */
    public List<CoordinateDTO> getSectorTiles() {
        return sectorTiles;
    }

    /**
     * Determine if it is winter for this particular sector.
     *
     * @param thisSector the sector to examine.
     * @return true if the sector should be painted with winter colors.
     */
    private boolean isWinter(final SectorDTO thisSector) {

        switch (thisSector.getRegionId()) {
            case EUROPE:
                if (thisSector.getY() <= 10) {
                    return hasArctic;

                } else if ((thisSector.getY() >= 11) && (thisSector.getY() <= 35)) {
                    return hasCentral;

                } else {
                    return hasMediterranean;
                }

            case CARIBBEAN:
            case INDIES:
            case AFRICA:
            default:
                return false;
        }
    }

    /**
     * Select the tile for the production site.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     */
    public void addPSiteTile(final int coordX,
                             final int coordY) {

        sectorTiles.add(new CoordinateDTO(coordX, coordY, imgProdSite(coordX, coordY), LAYER_PSITES));
    }

    /**
     * Select the tile for the trade city.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     */
    public void addTCityTile(final int coordX,
                             final int coordY) {

        sectorTiles.add(new CoordinateDTO(coordX, coordY, imgProdSite(coordX, coordY), LAYER_TCITY));
    }

    /**
     * Select the tile for the fortification.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     */
    public void addFortTile(final int coordX,
                            final int coordY) {

        final String filename;
        if (mapSector[coordX][coordY].getBuildProgress() > 0) {
            filename = DIR_SITES + "tprod-16-half.png";

        } else {
            filename = DIR_SITES + "fort0" + String.valueOf(mapSector[coordX][coordY].getProductionSiteId() - ProductionSiteConstants.PS_BARRACKS) + "bis.png";
        }

        sectorTiles.add(new CoordinateDTO(coordX, coordY, filename, LAYER_FORT));
    }

    /**
     * Select the tile for the natural resource.
     *
     * @param coordX   the X coordinate.
     * @param coordY   the Y coordinate.
     * @param natResId the Id of the natural resource.
     */
    public void addNatResTile(final int coordX,
                              final int coordY,
                              final int natResId) {

        String grayPrefix = "";
        if (!mapSector[coordX][coordY].getVisible()) {
            grayPrefix = "gray-";
        }

        final String filename = DIR_RESOURCES + grayPrefix + "resource-" + natResId + ".png";
        sectorTiles.add(new CoordinateDTO(coordX, coordY, filename, LAYER_NATRES));
    }

    /**
     * Select the tile for the epidemic.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     */
    public void addEpidemicTile(final int coordX,
                                final int coordY) {

        final String filename = DIR_EVENTS + "epidemic.png";
        sectorTiles.add(new CoordinateDTO(coordX, coordY, filename, LAYER_EPIDEMIC));
    }

    /**
     * Select the tile for the rebellion.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     */
    public void addRebellionTile(final int coordX,
                                 final int coordY) {

        final String filename = DIR_EVENTS + "rebellion.png";
        sectorTiles.add(new CoordinateDTO(coordX, coordY, filename, LAYER_REBELLION));
    }

    /**
     * Select the tile for the conquer.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     */
    public void addConquerTile(final int coordX,
                               final int coordY) {

        final String filename = DIR_EVENTS + "conquer.png";
        sectorTiles.add(new CoordinateDTO(coordX, coordY, filename, LAYER_CONQUER));
    }

    /**
     * Determine if the sector is neighboring a particular terrain type.
     *
     * @param coordX the X coordinate of the sector to look-up.
     * @param coordY the Y coordinate of the sector to look-up.
     * @param terId  the terrain type.
     * @return true if the sector is neighboring the particular terrain type, otherwise false.
     */
    private boolean isNeighboring(final int coordX,
                                  final int coordY,
                                  final int terId) {

        if ((coordX > minX)
                && (coordY > minY)
                && (mapSector[coordX - 1][coordY - 1].getTerrainId() == terId)) {
            return true;
        }
        if ((coordX > minX)
                && (mapSector[coordX - 1][coordY].getTerrainId() == terId)) {
            return true;
        }
        if ((coordX > minX)
                && (coordY < maxY - 1)
                && (mapSector[coordX - 1][coordY + 1].getTerrainId() == terId)) {
            return true;
        }
        if ((coordY > minY)
                && (mapSector[coordX][coordY - 1].getTerrainId() == terId)) {
            return true;
        }
        if ((coordY < maxY - 1)
                && (mapSector[coordX][coordY + 1].getTerrainId() == terId)) {
            return true;
        }
        if ((coordX < maxX - 1)
                && (coordY > minY)
                && (mapSector[coordX + 1][coordY - 1].getTerrainId() == terId)) {
            return true;
        }
        if ((coordX < maxX - 1)
                && (mapSector[coordX + 1][coordY].getTerrainId() == terId)) {
            return true;
        }
        if ((coordX < maxX - 1)
                && (coordY < maxY - 1)
                && (mapSector[coordX + 1][coordY + 1].getTerrainId() == terId)) {
            return true;
        }

        return false;
    }

    /**
     * Select the base-layer of the tile.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     * @return the name of the image.
     */
    public String getBaseTile(final int coordX,
                              final int coordY) {

        String grayPrefix = "";
        if (!mapSector[coordX][coordY].getVisible()) {
            grayPrefix = "gray-";
        }

        final int terrainId = mapSector[coordX][coordY].getTerrainId();
        switch (terrainId) {
            case TERRAIN_B: {

                if (isNeighboring(coordX, coordY, TERRAIN_K)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE_WINTER, grayPrefix + "steppew", TERRAIN_K),
                                LAYER_STEPPE));

                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE, grayPrefix + "steppe", TERRAIN_K),
                                LAYER_STEPPE));
                    }
                }

                if (isNeighboring(coordX, coordY, TERRAIN_D)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_DESERT, grayPrefix + "desert", TERRAIN_D),
                            LAYER_DESERT));
                }

                if (isNeighboring(coordX, coordY, TERRAIN_W)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST_WINTER, grayPrefix + "forestw", TERRAIN_W),
                                LAYER_FOREST));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST, grayPrefix + "forest", TERRAIN_W),
                                LAYER_FOREST));
                    }
                }

                if (isNeighboring(coordX, coordY, TERRAIN_J)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                            LAYER_JUNGLE));
                }

                if (mapSector[coordX][coordY].getStorm() == 0 && isNeighboring(coordX, coordY, TERRAIN_O)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getStormTileFromAdjacentTiles(coordX, coordY, DIR_STORMS, grayPrefix + "storm", TERRAIN_O),
                            LAYER_STORM));
                }

                if (isWinter(mapSector[coordX][coordY])) {
                    return DIR_BASE + grayPrefix + "arablew.png";

                } else {
                    return DIR_BASE + grayPrefix + "arable.png";
                }
            }

            case TERRAIN_D: {
                if (isNeighboring(coordX, coordY, TERRAIN_W)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST_WINTER, grayPrefix + "forestw", TERRAIN_W),
                                LAYER_FOREST));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST, grayPrefix + "forest", TERRAIN_W),
                                LAYER_FOREST));
                    }
                }

                if (isNeighboring(coordX, coordY, TERRAIN_J)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                            LAYER_JUNGLE));
                }

                return DIR_BASE + grayPrefix + "desert" + (((coordX + coordY + 1) % 2) + 1) + ".png";
            }

            case TERRAIN_I: {
                if (isNeighboring(coordX, coordY, TERRAIN_W)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST_WINTER, grayPrefix + "forestw", TERRAIN_W),
                                LAYER_FOREST));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST, grayPrefix + "forest", TERRAIN_W),
                                LAYER_FOREST));
                    }
                }

                if (isNeighboring(coordX, coordY, TERRAIN_J)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                            LAYER_JUNGLE));
                }

                if (isNeighboring(coordX, coordY, TERRAIN_K)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE_WINTER, grayPrefix + "steppew", TERRAIN_K),
                                LAYER_STEPPE));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE, grayPrefix + "steppe", TERRAIN_K),
                                LAYER_STEPPE));
                    }
                }

                // Check if this is a desert impassable
                if (coordY >= 58) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY, DIR_BASE + grayPrefix + "impassable" + (((coordX + coordY + 1) % 3) + 1) + ".png", LAYER_MOUNTAINS));
                    return DIR_BASE + grayPrefix + "desert" + (((coordX + coordY + 1) % 2) + 1) + ".png";
                }

                // This is a mountain impassable terrain
                sectorTiles.add(new CoordinateDTO(coordX, coordY, DIR_ELEVATION + grayPrefix + "ti0" + (((coordX + coordY + 1) % 3) + 1) + ".png", LAYER_MOUNTAINS));
                if (isWinter(mapSector[coordX][coordY])) {
                    return DIR_BASE + grayPrefix + "arablew.png";

                } else {
                    return DIR_BASE + grayPrefix + "arable.png";
                }
            }

            case TERRAIN_K: {
                if (isNeighboring(coordX, coordY, TERRAIN_D)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_DESERT, grayPrefix + "desert", TERRAIN_D),
                            LAYER_DESERT));
                }

                if (isNeighboring(coordX, coordY, TERRAIN_W)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST_WINTER, grayPrefix + "forestw", TERRAIN_W),
                                LAYER_FOREST));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST, grayPrefix + "forest", TERRAIN_W),
                                LAYER_FOREST));
                    }
                }

                if (isNeighboring(coordX, coordY, TERRAIN_J)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                            LAYER_JUNGLE));
                }

                if (isWinter(mapSector[coordX][coordY])) {
                    return DIR_BASE + grayPrefix + "steppew" + (((coordX + coordY + 1) % 2) + 1) + ".png";
                } else {
                    return DIR_BASE + grayPrefix + "steppe" + (((coordX + coordY + 1) % 2) + 1) + ".png";
                }
            }

            case TERRAIN_W: {
                if (isNeighboring(coordX, coordY, TERRAIN_J)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                            LAYER_JUNGLE));
                }

                if (isWinter(mapSector[coordX][coordY])) {
                    return DIR_BASE + grayPrefix + "forestw.png";
                } else {
                    return DIR_BASE + grayPrefix + "forest" + (((coordX + coordY + 1) % 2) + 1) + ".png";
                }
            }

            case TERRAIN_J: {
                return DIR_BASE + grayPrefix + "jungle" + (((coordX + coordY + 1) % 2) + 1) + ".png";
            }

            default: {

                // Decide base tile color
                if (isNeighboring(coordX, coordY, TERRAIN_D)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_DESERT, grayPrefix + "desert", TERRAIN_D),
                            LAYER_DESERT));

                } else if (isNeighboring(coordX, coordY, TERRAIN_K)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE_WINTER, grayPrefix + "steppew", TERRAIN_K),
                                LAYER_STEPPE));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE, grayPrefix + "steppe", TERRAIN_K),
                                LAYER_STEPPE));
                    }
                }

                // Decide Elevation type
                switch (terrainId) {
                    case TERRAIN_G: {

                        if (isWinter(mapSector[coordX][coordY])) {
                            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                    DIR_ELEVATION + grayPrefix + "tmw0" + (((coordX + coordY + 1) % 3) + 1) + ".png",
                                    LAYER_MOUNTAINS));

                        } else {
                            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                    DIR_ELEVATION + grayPrefix + "tm0" + (((coordX + coordY + 1) % 6) + 1) + ".png",
                                    LAYER_MOUNTAINS));
                        }
                        break;
                    }
                    case TERRAIN_H: {
                        if (isWinter(mapSector[coordX][coordY])) {
                            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                    DIR_ELEVATION + grayPrefix + "thw0" + (((coordX + coordY + 1) % 2) + 1) + ".png",
                                    LAYER_HILLS));

                        } else {
                            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                    DIR_ELEVATION + grayPrefix + "th0" + (((coordX + coordY + 1) % 6) + 1) + ".png",
                                    LAYER_HILLS));
                        }
                        break;
                    }
                    case TERRAIN_S: {
                        if (isWinter(mapSector[coordX][coordY])) {
                            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                    DIR_ELEVATION + grayPrefix + "tww0" + (((coordX + coordY + 1) % 2) + 1) + ".png",
                                    LAYER_HILLS));

                        } else {
                            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                    DIR_ELEVATION + grayPrefix + "tw0" + (((coordX + coordY + 1) % 3) + 1) + ".png",
                                    LAYER_HILLS));
                        }
                        break;
                    }
                }

                // Examine for overflow woods
                if (isNeighboring(coordX, coordY, TERRAIN_W)) {
                    if (isWinter(mapSector[coordX][coordY])) {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST_WINTER, grayPrefix + "forestw", TERRAIN_W),
                                LAYER_FOREST));
                    } else {
                        sectorTiles.add(new CoordinateDTO(coordX, coordY,
                                getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST, grayPrefix + "forest", TERRAIN_W),
                                LAYER_FOREST));
                    }
                }

                // Examine for overflow jungles
                if (isNeighboring(coordX, coordY, TERRAIN_J)) {
                    sectorTiles.add(new CoordinateDTO(coordX, coordY,
                            getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                            LAYER_JUNGLE));
                }

                if (isWinter(mapSector[coordX][coordY])) {
                    return DIR_BASE + grayPrefix + "arablew.png";
                } else {
                    return DIR_BASE + grayPrefix + "arable.png";
                }
            }
        }

    }

    /**
     * Select the base-layer of the tile.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     * @return the name of the image.
     */
    public String getOceanCoastTile(final int coordX,
                                    final int coordY) {

        String grayPrefix = "";
        if (!mapSector[coordX][coordY].getVisible()) {
            grayPrefix = "gray-";
        }

        if (isNeighboring(coordX, coordY, TERRAIN_W)) {
            if (isWinter(mapSector[coordX][coordY])) {
                sectorTiles.add(new CoordinateDTO(coordX, coordY,
                        getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST_WINTER, grayPrefix + "forestw", TERRAIN_W),
                        LAYER_FOREST));
            } else {
                sectorTiles.add(new CoordinateDTO(coordX, coordY,
                        getTileFromAdjacentTiles(coordX, coordY, DIR_FOREST, grayPrefix + "forest", TERRAIN_W),
                        LAYER_FOREST));
            }
        }

        if (isNeighboring(coordX, coordY, TERRAIN_J)) {
            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                    getTileFromAdjacentTiles(coordX, coordY, DIR_JUNGLE, grayPrefix + "jungle", TERRAIN_J),
                    LAYER_JUNGLE));
        }

        if (isNeighboring(coordX, coordY, TERRAIN_K)) {
            if (isWinter(mapSector[coordX][coordY])) {
                sectorTiles.add(new CoordinateDTO(coordX, coordY,
                        getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE_WINTER, grayPrefix + "steppew", TERRAIN_K),
                        LAYER_STEPPE));
            } else {
                sectorTiles.add(new CoordinateDTO(coordX, coordY,
                        getTileFromAdjacentTiles(coordX, coordY, DIR_STEPPE, grayPrefix + "steppe", TERRAIN_K),
                        LAYER_STEPPE));
            }
        }

        if (isNeighboring(coordX, coordY, TERRAIN_D)) {
            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                    getTileFromAdjacentTiles(coordX, coordY, DIR_DESERT, grayPrefix + "desert", TERRAIN_D),
                    LAYER_DESERT));
        }

        if (mapSector[coordX][coordY].getStorm() == 0 && isNeighboring(coordX, coordY, TERRAIN_O)) {
            sectorTiles.add(new CoordinateDTO(coordX, coordY,
                    getStormTileFromAdjacentTiles(coordX, coordY, DIR_STORMS, "storm", TERRAIN_O),
                    LAYER_STORM));
        }

        if (isWinter(mapSector[coordX][coordY])) {
            return getCoastTileFromAdjacentTiles(coordX, coordY, DIR_COAST_WINTER, "coastw", TERRAIN_O);

        } else {
            return getCoastTileFromAdjacentTiles(coordX, coordY, DIR_COAST, "coast", TERRAIN_O);
        }
    }

    /**
     * Determine the tile based on the neighboring terrain types.
     *
     * @param coordX        the X coordinate to check.
     * @param coordY        the Y coordinate to check.
     * @param dir           the main directory of the images.
     * @param starter       the prefix of the image filenames.
     * @param targetTerrain the terrain type that we look for.
     * @return the image filename.
     */
    private String getCoastTileFromAdjacentTiles(final int coordX,
                                                 final int coordY,
                                                 final String dir,
                                                 final String starter,
                                                 final int targetTerrain) {

        final int maxX = mapSector.length;
        final int maxY = mapSector[1].length;
        boolean checkUL = true, checkLD = true, checkDR = true, checkRU = true, found = false;
        final StringBuilder fileString = new StringBuilder(dir);
        fileString.append(starter);

        if ((coordY > minY)
                && (mapSector[coordX][coordY - 1].getTerrainId() != targetTerrain)) {
            fileString.append("Up");
            checkUL = false;
            checkRU = false;
            found = true;
        }

        if ((coordX > minX)
                && (mapSector[coordX - 1][coordY].getTerrainId() != targetTerrain)) {
            fileString.append("Left");
            checkUL = false;
            checkLD = false;
            found = true;
        }

        if ((coordY < maxY - 1)
                && (mapSector[coordX][coordY + 1].getTerrainId() != targetTerrain)) {
            fileString.append("Down");
            checkDR = false;
            checkLD = false;
            found = true;
        }

        if ((coordX < maxX - 1)
                && (mapSector[coordX + 1][coordY].getTerrainId() != targetTerrain)) {
            fileString.append("Right");
            checkDR = false;
            checkRU = false;
            found = true;
        }

        if ((coordX > minX)
                && (coordY > minY)
                && (checkUL && mapSector[coordX - 1][coordY - 1].getTerrainId() != targetTerrain)) {
            fileString.append("UL");
            found = true;
        }

        if ((coordX > minX)
                && (coordY < maxY - 1)
                && (checkLD && mapSector[coordX - 1][coordY + 1].getTerrainId() != targetTerrain)) {
            fileString.append("LD");
            found = true;
        }

        if ((coordX < maxX - 1)
                && (coordY < maxY - 1)
                && (checkDR && mapSector[coordX + 1][coordY + 1].getTerrainId() != targetTerrain)) {
            fileString.append("DR");
            found = true;
        }

        if ((coordX < maxX - 1)
                && (coordY > minY)
                && (checkRU && mapSector[coordX + 1][coordY - 1].getTerrainId() != targetTerrain)) {
            fileString.append("RU");
            found = true;
        }

        if (mapSector[coordX][coordY].getStorm() > 0) {
            sectorTiles.add(new CoordinateDTO(coordX, coordY, DIR_BASE + "storm.png", LAYER_STORM));
        }

        String path = "";
        if (found) {
            fileString.append(".png");
            path = fileString.toString();

        } else if (targetTerrain == TERRAIN_O) {
            // Check if a storm is affecting the sea sector
            if (mapSector[coordX][coordY].getStorm() == 0) {
                path = DIR_BASE + "ocean.png";

            } else {
                path = DIR_BASE + "storm.png";
            }
        }

        return path;
    }

    /**
     * Determine the tile based on the neighboring terrain types.
     *
     * @param coordX        the X coordinate to check.
     * @param coordY        the Y coordinate to check.
     * @param dir           the main directory of the images.
     * @param starter       the prefix of the image filenames.
     * @param targetTerrain the terrain type that we look for.
     * @return the image filename.
     */

    private String getTileFromAdjacentTiles(final int coordX,
                                            final int coordY,
                                            final String dir,
                                            final String starter,
                                            final int targetTerrain) {

        final int maxX = mapSector.length;
        final int maxY = mapSector[1].length;
        boolean checkUL = true, checkLD = true, checkDR = true, checkRU = true, found = false;
        final StringBuilder fileString = new StringBuilder(dir);
        fileString.append(starter);

        if ((coordY > minY)
                && (mapSector[coordX][coordY - 1].getTerrainId() == targetTerrain)) {
            fileString.append("Up");
            checkUL = false;
            checkRU = false;
            found = true;
        }
        if ((coordX > minX)
                && (mapSector[coordX - 1][coordY].getTerrainId() == targetTerrain)) {
            fileString.append("Left");
            checkUL = false;
            checkLD = false;
            found = true;
        }
        if ((coordY < maxY - 1)
                && (mapSector[coordX][coordY + 1].getTerrainId() == targetTerrain)) {
            fileString.append("Down");
            checkDR = false;
            checkLD = false;
            found = true;
        }
        if ((coordX < maxX - 1)
                && (mapSector[coordX + 1][coordY].getTerrainId() == targetTerrain)) {
            fileString.append("Right");
            checkDR = false;
            checkRU = false;
            found = true;
        }
        if ((coordX > minX && coordY > minY)
                && (checkUL && mapSector[coordX - 1][coordY - 1].getTerrainId() == targetTerrain)) {
            fileString.append("UL");
            found = true;
        }
        if ((coordX > minX && coordY < maxY - 1)
                && (checkLD && mapSector[coordX - 1][coordY + 1].getTerrainId() == targetTerrain)) {
            fileString.append("LD");
            found = true;
        }
        if ((coordX < maxX - 1 && coordY < maxY - 1)
                && (checkDR && mapSector[coordX + 1][coordY + 1].getTerrainId() == targetTerrain)) {
            fileString.append("DR");
            found = true;
        }
        if ((coordX < maxX - 1 && coordY > minY)
                && (checkRU && mapSector[coordX + 1][coordY - 1].getTerrainId() == targetTerrain)) {
            fileString.append("RU");
            found = true;
        }

        String path = "";
        if (found) {
            fileString.append(".png");
            path = fileString.toString();

        } else if (targetTerrain == TERRAIN_O) {
            // Check if a storm is affecting the sea sector
            if (mapSector[coordX][coordY].getStorm() == 0) {
                path = DIR_BASE + "ocean.png";

            } else {
                path = DIR_BASE + "storm.png";
            }
        }

        return path;
    }

    /**
     * Determine the tile based on the neighboring terrain types.
     *
     * @param coordX        the X coordinate to check.
     * @param coordY        the Y coordinate to check.
     * @param dir           the main directory of the images.
     * @param starter       the prefix of the image filenames.
     * @param targetTerrain the terrain type that we look for.
     * @return the image filename.
     */
    private String getStormTileFromAdjacentTiles(final int coordX,
                                                 final int coordY,
                                                 final String dir,
                                                 final String starter,
                                                 final int targetTerrain) {

        final int maxX = mapSector.length;
        final int maxY = mapSector[1].length;
        boolean checkUL = true, checkLD = true, checkDR = true, checkRU = true, found = false;
        final StringBuilder fileString = new StringBuilder(dir);
        fileString.append(starter);

        if ((coordY > minY)
                && (mapSector[coordX][coordY - 1].getTerrainId() == targetTerrain)
                && (mapSector[coordX][coordY - 1].getStorm() > 0)) {
            fileString.append("Up");
            checkUL = false;
            checkRU = false;
            found = true;
        }
        if ((coordX > minX)
                && (mapSector[coordX - 1][coordY].getTerrainId() == targetTerrain)
                && (mapSector[coordX - 1][coordY].getStorm() > 0)) {
            fileString.append("Left");
            checkUL = false;
            checkLD = false;
            found = true;
        }
        if ((coordY < maxY - 1)
                && (mapSector[coordX][coordY + 1].getTerrainId() == targetTerrain)
                && (mapSector[coordX][coordY + 1].getStorm() > 0)) {
            fileString.append("Down");
            checkDR = false;
            checkLD = false;
            found = true;
        }
        if ((coordX < maxX - 1)
                && (mapSector[coordX + 1][coordY].getTerrainId() == targetTerrain)
                && (mapSector[coordX + 1][coordY].getStorm() > 0)) {
            fileString.append("Right");
            checkDR = false;
            checkRU = false;
            found = true;
        }
        if ((coordX > minX && coordY > minY)
                && (checkUL && mapSector[coordX - 1][coordY - 1].getTerrainId() == targetTerrain)
                && (mapSector[coordX - 1][coordY - 1].getStorm() > 0)) {
            fileString.append("UL");
            found = true;
        }
        if ((coordX > minX && coordY < maxY - 1)
                && (checkLD && mapSector[coordX - 1][coordY + 1].getTerrainId() == targetTerrain)
                && (mapSector[coordX - 1][coordY + 1].getStorm() > 0)) {
            fileString.append("LD");
            found = true;
        }
        if ((coordX < maxX - 1 && coordY < maxY - 1)
                && (checkDR && mapSector[coordX + 1][coordY + 1].getTerrainId() == targetTerrain)
                && (mapSector[coordX + 1][coordY + 1].getStorm() > 0)) {
            fileString.append("DR");
            found = true;
        }
        if ((coordX < maxX - 1 && coordY > minY)
                && (checkRU && mapSector[coordX + 1][coordY - 1].getTerrainId() == targetTerrain)
                && (mapSector[coordX + 1][coordY - 1].getStorm() > 0)) {
            fileString.append("RU");
            found = true;
        }

        String path = "";
        if (found) {
            fileString.append(".png");
            path = fileString.toString();
        }

        return path;
    }

    /**
     * Get the filename of the production site image.
     *
     * @param coordX the coordX coordinate.
     * @param coordY the coordY coordinate.
     * @return the production site image.
     */
    private String imgProdSite(final int coordX, final int coordY) {
        String siteString;
        final boolean tCity = mapSector[coordX][coordY].getTradeCity();
        final char pSphere = mapSector[coordX][coordY].getPoliticalSphere();
        final int prodSite = mapSector[coordX][coordY].getProductionSiteId();
        final int population = mapSector[coordX][coordY].getPopulation();
        final int region = mapSector[coordX][coordY].getRegionId();

        String winterSuffix;
        if (isWinter(mapSector[coordX][coordY])) {
            winterSuffix = "w";
        } else {
            winterSuffix = "";
        }

        switch (prodSite) {

            case PS_BARRACKS:
            case PS_BARRACKS_FS:
            case PS_BARRACKS_FM:
            case PS_BARRACKS_FL:
            case PS_BARRACKS_FH:

                if (tCity) {
                    // Check region
                    switch (region) {
                        case CARIBBEAN:
                            // Caribbean all economy cities
                            siteString = "tcity02barracks.png";
                            break;

                        case AFRICA:
                            // Africa all economy cities
                            siteString = "tcoastcity05barracks.png";
                            break;

                        case INDIES:
                            // India : two types.
                            if (coordX < 19) {
                                // Trade cities 56/79, 62/88 and 68/75 use tcoastcity06barracks.
                                siteString = "tcoastcity06barracks.png";

                            } else {
                                // Trade cities 75/95, 82/88 and 83/75 use tcity03barracks
                                siteString = "tcity03barracks.png";
                            }
                            break;

                        case EUROPE:
                        default:
                            switch (pSphere) {

                                case 'Y':
                                case 'y':
                                case 'T':
                                case 't':
                                case 'M':
                                case 'm':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                    // All north Africa economy cities and Constaninopole
                                    siteString = "tcity04barracks.png";
                                    break;

                                default:

                                    siteString = "tcity" + winterSuffix + "01barracks.png";
                                    break;
                            }
                            break;
                    }

                } else {

                    if (population < 6) {
                        siteString = "sprod-" + prodSite + winterSuffix + ".png";

                    } else {
                        // All cities, anywhere on every map, if above 6 population
                        siteString = "ttown" + winterSuffix + "01barracks.png";
                    }
                }
                break;

            case PS_FACTORY:
            case PS_MILL:
            case PS_MINT:
            case PS_ESTATE:
            case PS_FARM_SHEEP:
            case PS_FARM_HORSE:
            case PS_LUMBERCAMP:
            case PS_QUARRY:
            case PS_MINE:
            case PS_VINEYARD:
            case PS_PLANTATION:
                siteString = "sprod-" + prodSite + winterSuffix + ".png";
                break;

            default:

                // Maybe a trade city in the sea (virtual)
                if (tCity) {
                    // Check region
                    switch (region) {
                        case CARIBBEAN:
                            // This is New York
                            siteString = "tcity" + winterSuffix + "01barracks.png";
                            break;

                        case AFRICA:
                            // Africa all economy cities
                            siteString = "tcoastcity05barracks.png";
                            break;

                        case INDIES:
                            // India : two types.
                            if (coordX < 19) {
                                // Trade cities 56/79, 62/88 and 68/75 use tcoastcity06barracks.
                                siteString = "tcoastcity06barracks.png";

                            } else {
                                // Trade cities 75/95, 82/88 and 83/75 use tcity03barracks
                                siteString = "tcity03barracks.png";
                            }
                            break;

                        case EUROPE:
                        default:
                            siteString = "tcity" + winterSuffix + "01barracks.png";
                            break;
                    }

                } else if (population < 6) {
                    siteString = "";

                } else if (population < 8) {
                    siteString = "ttown" + winterSuffix + "01.png";

                } else {
                    // Check region
                    switch (region) {
                        case CARIBBEAN:
                        case AFRICA:
                            siteString = "tcity02.png";
                            break;


                        case INDIES:
                            // India : two types.
                            if (coordX < 19) {
                                // Trade cities 56/79, 62/88 and 68/75 use tcoastcity06barracks.
                                siteString = "tcity02.png";

                            } else {
                                // Trade cities 75/95, 82/88 and 83/75 use tcity03barracks
                                siteString = "tcity03.png";
                            }
                            break;

                        case EUROPE:
                        default:
                            switch (pSphere) {

                                case 'T':
                                case 't':
                                case 'M':
                                case 'm':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                    // All north Africa economy cities and Constaninopole
                                    siteString = "tcity04.png";
                                    break;

                                default:

                                    siteString = "tcity" + winterSuffix + "01.png";
                                    break;
                            }
                            break;
                    }

                }
                break;

        }

        return DIR_SITES + siteString;
    }

    @SuppressWarnings("restriction")
    public void addBorderName(final boolean isVisible, final int coordX, final int coordY) {
        try {
            final StringBuilder tileImage = new StringBuilder();
            final int nationId = mapSector[coordX][coordY].getNationId();
            final int[] nationMap = new int[9];
            for (int neighboringTile = 0; neighboringTile < 9; neighboringTile++) {
                nationMap[neighboringTile] = -1;
            }

            if (coordY < mapSector[coordX].length - 1) {
                nationMap[3] = mapSector[coordX][coordY + 1].getNationId();
            }

            if (coordY > 0) {
                nationMap[7] = mapSector[coordX][coordY - 1].getNationId();
            }

            if (coordX > 0) {
                nationMap[1] = mapSector[coordX - 1][coordY].getNationId();
            }

            if (coordX > 0 && coordY < mapSector[coordX].length - 1) {
                nationMap[2] = mapSector[coordX - 1][coordY + 1].getNationId();
            }

            if (coordX > 0 && coordY > 0) {
                nationMap[0] = mapSector[coordX - 1][coordY - 1].getNationId();
            }

            if (coordX < mapSector.length - 1) {
                nationMap[5] = mapSector[coordX + 1][coordY].getNationId();
            }

            if (coordX < mapSector.length - 1 && coordY < mapSector[coordX].length - 1) {
                nationMap[4] = mapSector[coordX + 1][coordY + 1].getNationId();
            }

            if (coordX < mapSector.length - 1 && coordY > 0) {
                nationMap[6] = mapSector[coordX + 1][coordY - 1].getNationId();
            }

            if (nationMap[7] != nationId) {
                tileImage.append("Up");
            }

            if (nationMap[1] != nationId) {
                tileImage.append("Left");
            }

            if (nationMap[0] != nationId && nationMap[7] == nationId && nationMap[1] == nationId) {
                tileImage.append("UL");
            }

            if (nationMap[3] != nationId) {
                tileImage.append("Down");
            }

            if (nationMap[2] != nationId && nationMap[1] == nationId && nationMap[3] == nationId) {
                tileImage.append("DL");
            }

            if (nationMap[5] != nationId) {
                tileImage.append("Right");
            }

            if (nationMap[4] != nationId && nationMap[3] == nationId && nationMap[5] == nationId) {
                tileImage.append("DR");
            }

            if (nationMap[6] != nationId && nationMap[5] == nationId && nationMap[7] == nationId) {
                tileImage.append("UR");
            }

            final StringBuilder tile = new StringBuilder();

            if (isVisible) {
                // For visible tiles, we do not need to color sectors that do not have borders
                if (tileImage.length() < 1) {
                    return;
                }

                tile.append(DIR_BORDERS);

            } else {
                tile.append(DIR_BORDERS_FOW);
            }

            tile.append(nationId);
            tile.append("/");
            if (tileImage.length() < 1) {
                // For non-visible tiles, we have to put color all the tile
                tile.append("base");

            } else {
                tile.append(tileImage);
            }
            tile.append(".png");

            sectorTiles.add(new CoordinateDTO(coordX - 1, coordY, tile.toString(), LAYER_BORDERS));

        } catch (Exception ex) {
            LOGGER.error("Unable to encode border for " + coordX + "/" + coordY, ex);
        }
    }

}
