package com.eaw1805.map;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.dto.common.CoordinateDTO;
import com.eaw1805.data.dto.common.SectorDTO;
import com.eaw1805.data.dto.converters.SectorConverter;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads the tiles of a map.
 */
public class TilesLoader
        implements TerrainConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TilesLoader.class);

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
     * The color of each x,y coordinate. Selection is based on color of sector's owner.
     */
    private final transient Color[][] mapNation;

    /**
     * The base image assigned to each coordinate.
     */
    private final transient String[][] sectorTiles;

    /**
     * The list of additional tiles that are required as image overlays.
     */
    private final transient List<CoordinateDTO> sectorOverlayTiles;

    /**
     * Indicates if that fog-of-war will be used.
     */
    private final transient boolean showFOWBorders;

    /**
     * Indicates if that fog-of-war will be used.
     */
    private final transient boolean deactivateFOW;

    /**
     * Default constructor.
     *
     * @param game          the game (for calculating winter).
     * @param sectorList    list of sectors to draw.
     * @param viewer        the nation viewing the map.
     * @param useFOWborders display alternative national borders for foreign nations on the map.
     * @param disableFOW    de-activate FOW rules.
     */
    public TilesLoader(final Game game,
                       final List<Sector> sectorList,
                       final Nation viewer,
                       final boolean useFOWborders,
                       final boolean disableFOW) {
        showFOWBorders = useFOWborders;
        deactivateFOW = disableFOW;
        final List<SectorDTO> sectors = new ArrayList<SectorDTO>();
        final Set<SectorDTO> sectorsVisible = new HashSet<SectorDTO>();
        int tempXmax = 0;
        int tempYmax = 0;
        int tempXmin = Integer.MAX_VALUE;
        int tempYmin = Integer.MAX_VALUE;

        final String viewerToken;
        if (viewer.getId() == NationConstants.NATION_NEUTRAL) {
            viewerToken = "*";
        } else {
            viewerToken = "*" + viewer.getId() + "*";
        }

        for (final Sector value : sectorList) {
            final SectorDTO convSector = SectorConverter.convert(value);
            sectors.add(convSector);
            tempXmax = Math.max(tempXmax, value.getPosition().getX());
            tempYmax = Math.max(tempYmax, value.getPosition().getY());
            tempXmin = Math.min(tempXmin, value.getPosition().getX());
            tempYmin = Math.min(tempYmin, value.getPosition().getY());

            if (value.getTerrain().getId() == TERRAIN_O
                    || value.getNation().getId() == viewer.getId()
                    || (value.getFow() != null && value.getFow().contains(viewerToken))
                    || deactivateFOW) {
                convSector.setVisible(true);
                sectorsVisible.add(convSector);
            }
        }

        LOGGER.debug("Loaded " + sectors.size() + " sectors.");
        LOGGER.debug("Map size is " + (tempXmin + 1) + "/" + (tempYmin + 1) + "..." + (tempXmax + 1) + "/" + (tempYmax + 1));

        maxX = tempXmax;
        maxY = tempYmax;
        minX = tempXmin;
        minY = tempYmin;

        final SectorDTO[][] mapSector = new SectorDTO[maxX + 1][maxY + 1];
        mapNation = new Color[maxX + 1][maxY + 1];
        sectorTiles = new String[maxX + 1][maxY + 1];
        final CoordinateDTO[][][] sectorOverlays = new CoordinateDTO[maxX + 1][maxY + 1][TilesSelector.LAYER_BORDERS];

        // Iterate through all sectors
        for (SectorDTO sector : sectors) {
            final int coordX = sector.getX();
            final int coordY = sector.getY();
            mapSector[coordX][coordY] = sector;
            if (sector.getNationId() > 0) {
                mapNation[coordX][coordY] = Color.decode("#" + sector.getNationDTO().getColor().toUpperCase());

            } else {
                mapNation[coordX][coordY] = null;
            }
        }

        // Setup tiles selector
        final TilesSelector tilesSelector = new TilesSelector(mapSector, game, maxX, maxY, minX, minY);

        // Identify base image
        for (int coordX = minX; coordX <= maxX; coordX++) {
            for (int coordY = minY; coordY <= maxY; coordY++) {
                final SectorDTO thisSector = mapSector[coordX][coordY];
                sectorOverlays[coordX][coordY][0] = new CoordinateDTO();

                if (thisSector.getTerrainId() == TERRAIN_O) {
                    sectorTiles[coordX][coordY] = tilesSelector.getOceanCoastTile(coordX, coordY);

                    // Check for off-map trade cities
                    if (thisSector.getTradeCity()) {
                        tilesSelector.addTCityTile(coordX, coordY);
                    }
                } else {
                    sectorTiles[coordX][coordY] = tilesSelector.getBaseTile(coordX, coordY);
                }

                if (deactivateFOW || sectorsVisible.contains(thisSector)) {
                    if ((thisSector.getProductionSiteId() > 0) || (thisSector.getPopulation() > 6)) {
                        if (thisSector.getTradeCity()) {
                            tilesSelector.addTCityTile(coordX, coordY);

                        } else {
                            tilesSelector.addPSiteTile(coordX, coordY);
                        }

                        // Add fortifications
                        if (thisSector.getProductionSiteId() > ProductionSiteConstants.PS_BARRACKS) {
                            tilesSelector.addFortTile(coordX, coordY);
                        }
                    }
                }

                if (thisSector.getNatResId() > 0) {
                    tilesSelector.addNatResTile(coordX, coordY, thisSector.getNatResId());
                }

                if (thisSector.getEpidemic()) {
                    tilesSelector.addEpidemicTile(coordX, coordY);
                }

                if (thisSector.getRebelled()) {
                    tilesSelector.addRebellionTile(coordX, coordY);
                }

                if (thisSector.getConquered()) {
                    tilesSelector.addConquerTile(coordX, coordY);
                }

                if (thisSector.getNationId() != NationConstants.NATION_NEUTRAL) {
                    tilesSelector.addBorderName(deactivateFOW || (showFOWBorders && sectorsVisible.contains(thisSector)), coordX, coordY);
                }
            }
        }

        // Get result of tile selection process
        sectorOverlayTiles = tilesSelector.getSectorTiles();
    }

    /**
     * Get the maximum X coordinate of the map.
     *
     * @return the maximum X coordinate of the map.
     */
    public int getMaxX() {
        return maxX;
    }

    /**
     * Get the maximum Y coordinate of the map.
     *
     * @return the maximum Y coordinate of the map.
     */
    public int getMaxY() {
        return maxY;
    }

    /**
     * Get the minimum X coordinate of the map.
     *
     * @return the minimum X coordinate of the map.
     */
    public int getMinX() {
        return minX;
    }

    /**
     * Get the minimum Y coordinate of the map.
     *
     * @return the minimum Y coordinate of the map.
     */
    public int getMinY() {
        return minY;
    }

    /**
     * Get the tile tint color.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     * @return the RGB color for tinting the coordinate.
     */
    public Color getTintColor(final int coordX, final int coordY) {
        return mapNation[coordX][coordY];
    }

    /**
     * Get the base tiles.
     *
     * @param coordX the X coordinate.
     * @param coordY the Y coordinate.
     * @return the filename of the base tile.
     */
    public String getSectorTile(final int coordX, final int coordY) {
        return sectorTiles[coordX][coordY];
    }

    /**
     * Get overlay tiles.
     *
     * @return list of Coordinates with overlay tiles.
     */
    public List<CoordinateDTO> getSectorOverlayTiles() {
        return sectorOverlayTiles;
    }

}
