package com.eaw1805.map;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.dto.common.CoordinateDTO;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.beans.SectorManagerBean;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Draws maps.
 */
public class MapCreator
        implements RegionConstants, TerrainConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(MapCreator.class);

    /**
     * Directory name containing tiles.
     */
    private final transient String basePath;

    /**
     * The images used for drawing the map.
     */
    private final TileManager tileManager;

    public static final int TILE_SIZE = 64;

    public static final int TILE_ELEVATED = 104;

    public static final double ZOOM_FACTOR_RESOURCE = .2d;

    public static final double ZOOM_FACTOR_EVENT = .35d;

    private static final double COMPRESSION_LEVEL_TILE = .9d;

    private static final double COMPRESSION_LEVEL_NATION = .8d;

    private transient int offsetX = 0, offsetY = 0;

    private final transient double zoomFactor;

    private final transient int zoomTile;

    private final transient int zoomElevated;

    private final transient int zoomResource;

    private final transient int zoomEvent;

    /**
     * The maximum X coordinate.
     */
    private transient int maxX;

    /**
     * The maximum Y coordinate.
     */
    private transient int maxY;

    /**
     * The minimum X coordinate.
     */
    private transient int minX;

    /**
     * The minimum Y coordinate.
     */
    private transient int minY;

    private transient TilesLoader tilesLoader;

    private transient Image[][] mapImage;

    private transient Map<Point, Image> mapStorms;

    private transient Map<Point, Image> mapMountains;

    private transient Map<Point, Image> mapProdSites;

    private transient Map<Point, Image> mapForts;

    private transient Map<Point, Image> mapTradeCities;

    private transient Map<Point, Image> mapNatRes;

    private transient Map<Point, Image> mapEpidemic;

    private transient Map<Point, Image> mapRebellion;

    private transient Map<Point, Image> mapConquer;

    private transient Map<Integer, Image>[][] mapImageOver;

    private transient Map<Point, Image> mapBorders;

    private transient String[][] tileName;

    private transient String[][] tileElevName;

    private final transient List<Sector> lstSectors;

    /**
     * The nation to apply the fog-of-war rules for visibility.
     */
    private final transient Nation mapViewer;

    /**
     * The game.
     */
    private final transient Game game;

    /**
     * Display map information publicly available. (Apply full-fog-of-war rules).
     */
    private final transient boolean publicOnly;

    /**
     * Indicates if the national border will be drawn.
     */
    private final transient boolean showNationalBorders;

    /**
     * Indicates if the alternative national borders for foreign nations on the map..
     */
    private final transient boolean showFOWBorders;

    /**
     * Indicates if that fog-of-war will be used.
     */
    private final transient boolean deactivateFOW;

    private final transient Sector[][] arrSectors = new Sector[100][100];

    /**
     * Default constructor.
     *
     * @param thisGame         the game.
     * @param sectorList       the list of sectors.
     * @param viewer           the nation viewing the map.
     * @param path             the path where the images are stored.
     * @param zoom             the zoom factor (1d is original size).
     * @param useNationBorders display national border on the map.
     * @param useFOWborders    display alternative national borders for foreign nations on the map.
     * @param disableFOW       de-activate FOW rules.
     */
    public MapCreator(final Game thisGame,
                      final List<Sector> sectorList,
                      final Nation viewer,
                      final String path,
                      final double zoom,
                      final boolean useNationBorders,
                      final boolean useFOWborders,
                      final boolean disableFOW) {
        lstSectors = sectorList;
        mapViewer = viewer;
        basePath = path;
        game = thisGame;
        publicOnly = (viewer.getId() == NationConstants.NATION_NEUTRAL);
        showNationalBorders = useNationBorders;
        showFOWBorders = useFOWborders;
        deactivateFOW = disableFOW;

        zoomFactor = zoom;
        zoomTile = Math.max(3, (int) (TILE_SIZE * zoomFactor));
        zoomElevated = Math.max(5, (int) (TILE_ELEVATED * zoomFactor));
        zoomResource = Math.max(1, (int) (zoomTile * ZOOM_FACTOR_RESOURCE));
        zoomEvent = Math.max(1, (int) (zoomTile * ZOOM_FACTOR_EVENT));
        LOGGER.debug("Zoom factor " + zoomFactor + "(tile=" + zoomTile + "/elevated=" + zoomElevated + "/resource=" + zoomResource + "/event=" + zoomEvent + "/path=" + path + ")");

        tileManager = TileManager.getInstance(basePath, zoomFactor);

        loadMap();
        setupTiles();

        for (final Sector sector : sectorList) {
            arrSectors[sector.getPosition().getX()][sector.getPosition().getY()] = sector;
        }
    }

    /**
     * Load the tiles related to the given list of sectors.
     */
    private void loadMap() {
        tilesLoader = new TilesLoader(game, lstSectors, mapViewer, showFOWBorders, deactivateFOW);
    }

    @SuppressWarnings("unchecked")
    private void setupTiles() {

        maxX = tilesLoader.getMaxX();
        maxY = tilesLoader.getMaxY();
        minX = tilesLoader.getMinX();
        minY = tilesLoader.getMinY();

        mapImage = new Image[maxX + 1][maxY + 1];
        mapImageOver = new Map[maxX + 1][maxY + 1];
        mapBorders = new HashMap<Point, Image>();
        mapStorms = new HashMap<Point, Image>();
        mapMountains = new HashMap<Point, Image>();
        mapProdSites = new HashMap<Point, Image>();
        mapForts = new HashMap<Point, Image>();
        mapTradeCities = new HashMap<Point, Image>();
        mapNatRes = new HashMap<Point, Image>();
        mapEpidemic = new HashMap<Point, Image>();
        mapRebellion = new HashMap<Point, Image>();
        mapConquer = new HashMap<Point, Image>();
        tileName = new String[maxX + 1][maxY + 1];
        tileElevName = new String[maxX + 1][maxY + 1];

        // Setup base image & overlay placeholder
        for (int coordX = minX; coordX <= maxX; coordX++) {
            for (int coordY = minY; coordY <= maxY; coordY++) {
                mapImage[coordX][coordY] = tileManager.get(zoomFactor, tilesLoader.getSectorTile(coordX, coordY));

                mapImageOver[coordX][coordY] = new TreeMap();

                tileName[coordX][coordY] = tilesLoader.getSectorTile(coordX, coordY);
                tileElevName[coordX][coordY] = "";
            }
        }

        // Identify image overlays
        for (final CoordinateDTO coordinateDTO : tilesLoader.getSectorOverlayTiles()) {
            if (coordinateDTO.getPath().isEmpty()) {
                continue;
            }

            final int coordX = coordinateDTO.getX();
            final int coordY = coordinateDTO.getY();
            final int level = coordinateDTO.getElevation();
            final Image tileImg = tileManager.get(zoomFactor, coordinateDTO.getPath());

            if (tileImg == null) {
                LOGGER.error("Tile image '" + coordinateDTO.getPath() + "' not found for sector " + coordX + "/" + coordY);

            } else {
                if (level < TilesSelector.LAYER_BORDERS) {
                    tileName[coordX][coordY] += "-" + coordinateDTO.getPath();
                }

                switch (level) {
                    case TilesSelector.LAYER_HILLS:
                    case TilesSelector.LAYER_MOUNTAINS:
                        mapMountains.put(new Point(coordX, coordY), tileImg);
                        tileElevName[coordX][coordY] = coordinateDTO.getPath().replaceAll("images/tiles/", "").replaceAll("/", "_").replaceAll(".png", "");
                        break;

                    case TilesSelector.LAYER_PSITES:
                        mapProdSites.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_FORT:
                        mapForts.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_TCITY:
                        mapTradeCities.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_NATRES:
                        mapNatRes.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_EPIDEMIC:
                        mapEpidemic.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_REBELLION:
                        mapRebellion.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_CONQUER:
                        mapConquer.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_STORM:
                        mapStorms.put(new Point(coordX, coordY), tileImg);
                        break;

                    case TilesSelector.LAYER_BORDERS:
                        if (showNationalBorders) {
                            mapBorders.put(new Point(coordX, coordY), tileImg);
                        }
                        break;

                    default:
                        if (level < TilesSelector.LAYER_BORDERS) {
                            mapImageOver[coordX][coordY].put(level, tileImg);
                        }
                        break;
                }
            }
        }
    }

    /**
     * Draw all the tiles of the map.
     *
     * @return a byte array with the image.
     */
    public byte[] draw() {
        // initialize the output stream
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            ImageIO.write(drawMap(), "png", buffer);
        } catch (Exception ex) {
            LOGGER.fatal(ex);
        }

        return buffer.toByteArray();
    }

    /**
     * Draw all the tiles of the map.
     *
     * @param filename the name of the file to store the image.
     */
    public void draw(final String filename) {
        // Save the output
        try {
            final BufferedImage originalCanvas = drawMap();
            ImageIO.write(originalCanvas, "png", new File(filename));
            LOGGER.info(filename + " [size=" + (new File(filename)).length() + "]");

        } catch (IOException e) {
            LOGGER.fatal(e);
        }
    }

    /**
     * Draw all the tiles of the map.
     *
     * @return a buffered image.
     */
    private BufferedImage drawMap() {
        // setup canvas  TYPE_INT_ARGB / TYPE_BYTE_GRAY
        final BufferedImage canvas = new BufferedImage((1 + maxX - minX) * zoomTile, (1 + maxY - minY) * zoomTile,
                BufferedImage.TYPE_INT_ARGB);

        final Graphics2D gImg = canvas.createGraphics();

        LOGGER.debug("Image size is " + canvas.getWidth() + "/" + canvas.getHeight());

        int tilesBase = 0;
        int tilesOverlays = 0;
        int tilesElevation = 0;
        int tilesBorders = 0;
        int tilesStorm = 0;
        int tilesProdSites = 0;
        int tilesNatRes = 0;
        int tilesRebellions = 0;
        int tilesEpidemics = 0;
        int tilesConquer = 0;

        final String viewerToken;
        if (mapViewer.getId() == NationConstants.NATION_NEUTRAL) {
            viewerToken = "*";

        } else {
            viewerToken = "*" + mapViewer.getId() + "*";
        }

        // Draw base terrain
        for (int coordX = minX; coordX <= maxX; coordX++) {
            for (int coordY = minY; coordY <= maxY; coordY++) {

                if (mapImage[coordX][coordY] != null) {
                    final int posX = offsetX + (coordX - minX) * zoomTile;
                    final int posY = offsetY + (coordY - minY) * zoomTile;

                    if (publicOnly ||
                            ((deactivateFOW
                                    || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                                    || arrSectors[coordX][coordY].getFow().contains(viewerToken))
                                    && arrSectors[coordX][coordY].getTerrain().getId() != TERRAIN_O
                                    && !arrSectors[coordX][coordY].getImage().contains("coastAlt"))) {

                        gImg.drawImage(mapImage[coordX][coordY], posX, posY, null);
                        tilesBase++;
                    }
                }
            }
        }


        // Draw all other overlays
        for (int coordX = minX; coordX <= maxX; coordX++) {
            for (int coordY = minY; coordY <= maxY; coordY++) {
                if (mapImage[coordX][coordY] != null) {
                    final int posX = offsetX + (coordX - minX) * zoomTile;
                    final int posY = offsetY + (coordY - minY) * zoomTile;

                    if (publicOnly ||
                            ((deactivateFOW
                                    || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                                    || arrSectors[coordX][coordY].getFow().contains(viewerToken))
                                    && arrSectors[coordX][coordY].getTerrain().getId() != TERRAIN_O)) {

                        for (Map.Entry<Integer, Image> entry : mapImageOver[coordX][coordY].entrySet()) {
                            gImg.drawImage(entry.getValue(), posX, posY, null);
                            tilesOverlays++;
                        }
                    }
                }
            }
        }

        // Draw Storms
        for (Map.Entry<Point, Image> tile : mapStorms.entrySet()) {
            final int coordX = tile.getKey().x;
            final int coordY = tile.getKey().y;
            final int posX = (int) (offsetX + (coordX - minX) * zoomTile);
            final int posY = (int) (offsetY + (coordY - minY) * zoomTile);

            gImg.drawImage(tile.getValue(), posX, posY, null);
            tilesStorm++;
        }

        // Draw Mountains, Hills and swamp
        for (Map.Entry<Point, Image> tile : mapMountains.entrySet()) {
            final int coordX = tile.getKey().x;
            final int coordY = tile.getKey().y;
            final int posX = (int) (offsetX + (coordX - minX) * zoomTile - .25d * zoomTile);
            final int posY = (int) (offsetY + (coordY - minY) * zoomTile - .25d * zoomTile);

            if (publicOnly
                    || deactivateFOW
                    || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                    || arrSectors[coordX][coordY].getFow().contains(viewerToken)) {
                gImg.drawImage(tile.getValue(), posX, posY, null);
                tilesElevation++;
            }
        }


        // Draw Production sites
        if (!publicOnly) {
            for (Map.Entry<Point, Image> tile : mapProdSites.entrySet()) {
                final int coordX = tile.getKey().x;
                final int coordY = tile.getKey().y;
                final int posX = offsetX + (coordX - minX) * zoomTile;
                final int posY = offsetY + (coordY - minY) * zoomTile;

                if (deactivateFOW
                        || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                        || arrSectors[coordX][coordY].getFow().contains(viewerToken)) {
                    gImg.drawImage(tile.getValue(), posX, posY, null);
                    tilesProdSites++;
                }
            }

            // Draw Fortifications
            for (Map.Entry<Point, Image> tile : mapForts.entrySet()) {
                final int coordX = tile.getKey().x;
                final int coordY = tile.getKey().y;
                final int posX = (int) (offsetX + (coordX - minX) * zoomTile - .25d * zoomTile - .5d * zoomResource);
                final int posY = (int) (offsetY + (coordY - minY) * zoomTile - .25d * zoomTile);

                if (deactivateFOW
                        || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                        || arrSectors[coordX][coordY].getFow().contains(viewerToken)) {
                    gImg.drawImage(tile.getValue(), posX, posY, null);
                    tilesProdSites++;

                }
            }
        }

        // Draw trade cities
        for (Map.Entry<Point, Image> tile : mapTradeCities.entrySet()) {
            final int coordX = tile.getKey().x;
            final int coordY = tile.getKey().y;
            final int posX = (int) (offsetX + (coordX - minX) * zoomTile - .125d * zoomResource);
            final int posY = (int) (offsetY + (coordY - minY) * zoomTile + .25d * zoomResource);

            // Check for off-map trade cities
            if (arrSectors[coordX][coordY].getTerrain().getId() == TERRAIN_O) {
                final float opacity = 0.3f;
                int rule = AlphaComposite.SRC_OVER;

                final Composite comp = AlphaComposite.getInstance(rule, opacity);
                gImg.setComposite(comp);
                gImg.setColor(Color.green);
                gImg.fillRect(posX, posY, zoomTile, zoomTile);

                // Restore opacity
                final Composite restore = AlphaComposite.getInstance(rule, 1.0f);
                gImg.setComposite(restore);
            }

            gImg.drawImage(tile.getValue(), posX, posY, null);
            tilesProdSites++;
        }

        if (!publicOnly) {
            // Draw Natural Resources
            for (Map.Entry<Point, Image> tile : mapNatRes.entrySet()) {
                final int coordX = tile.getKey().x;
                final int coordY = tile.getKey().y;
                final int posX = offsetX + (coordX - minX) * zoomTile + zoomTile / 2;
                final int posY = offsetY + (coordY - minY) * zoomTile + zoomTile - zoomResource;

                if (deactivateFOW
                        || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                        || arrSectors[coordX][coordY].getFow().contains(viewerToken)
                        || arrSectors[coordX][coordY].getTerrain().getId() == TERRAIN_O) {
                    gImg.drawImage(tile.getValue(), posX, posY, null);
                    tilesNatRes++;
                }
            }

            // Draw Epidemics
            for (Map.Entry<Point, Image> tile : mapEpidemic.entrySet()) {
                final int coordX = tile.getKey().x;
                final int coordY = tile.getKey().y;
                final int posX = offsetX + (coordX - minX) * zoomTile + zoomTile / 2 - zoomEvent;
                final int posY = offsetY + (coordY - minY) * zoomTile + zoomEvent / 2;

                if (deactivateFOW
                        || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                        || arrSectors[coordX][coordY].getFow().contains(viewerToken)) {
                    gImg.drawImage(tile.getValue(), posX, posY, null);
                    tilesEpidemics++;
                }
            }

            // Draw Rebellions
            for (Map.Entry<Point, Image> tile : mapRebellion.entrySet()) {
                final int coordX = tile.getKey().x;
                final int coordY = tile.getKey().y;
                final int posX = offsetX + (coordX - minX) * zoomTile + zoomTile / 2 - zoomEvent;
                final int posY = (int) (offsetY + (coordY - minY) * zoomTile + zoomEvent / 2 + 0.5d * zoomEvent);

                if (deactivateFOW
                        || arrSectors[coordX][coordY].getNation().getId() == mapViewer.getId()
                        || arrSectors[coordX][coordY].getFow().contains(viewerToken)) {
                    gImg.drawImage(tile.getValue(), posX, posY, null);
                    tilesRebellions++;
                }
            }

            // Draw Conquer Counters
            for (Map.Entry<Point, Image> tile : mapConquer.entrySet()) {
                final int coordX = tile.getKey().x;
                final int coordY = tile.getKey().y;
                final int posX = offsetX + (coordX - minX) * zoomTile + zoomTile / 2;
                final int posY = offsetY + (coordY - minY) * zoomTile + zoomEvent / 2;

                gImg.drawImage(tile.getValue(), posX, posY, null);
                tilesConquer++;
            }
        }

        // Draw Borders
        if (showNationalBorders) {
            for (Map.Entry<Point, Image> tile : mapBorders.entrySet()) {
                final int coordX = tile.getKey().x + 1;
                final int coordY = tile.getKey().y;
                final int posX = offsetX + (coordX - minX) * zoomTile;
                final int posY = offsetY + (coordY - minY) * zoomTile;

                if (showFOWBorders) {
                    final float opacity;
                    if (deactivateFOW || arrSectors[coordX][coordY].getFow().contains(viewerToken)) {
                        opacity = 1f;

                    } else {
                        opacity = 0.6f;
                    }

                    int rule = AlphaComposite.SRC_OVER;
                    final Composite comp = AlphaComposite.getInstance(rule, opacity);
                    gImg.setComposite(comp);
                }

                gImg.drawImage(tile.getValue(), posX, posY, null);
                tilesBorders++;
            }
        }

        LOGGER.debug("Tiles base=" + tilesBase + "/overlays=" + tilesOverlays + "/borders=" + tilesBorders + "/storm=" + tilesStorm + "/elevation=" + tilesElevation + "/sites=" + tilesProdSites + "/natRes=" + tilesNatRes + "/epidemics=" + tilesEpidemics + "/rebellions=" + tilesRebellions + "/conquers=" + tilesConquer);
        return canvas;
    }

    public void drawTiles() {
        final Map<String, Point> uniqueTiles = new HashMap<String, Point>();
        final SectorManagerBean secMgr = SectorManager.getInstance();
        long totSize = 0;
        final BufferedImage mapImage = drawMap();

        final int width = mapImage.getWidth();
        final int height = mapImage.getHeight();
        final BufferedImage canvasMapGrey = new BufferedImage((int) (width * COMPRESSION_LEVEL_TILE), (int) (height * COMPRESSION_LEVEL_TILE), BufferedImage.TYPE_BYTE_GRAY);
        final Graphics gImgGrey = canvasMapGrey.createGraphics();
        gImgGrey.drawImage(mapImage, 0, 0, (int) (width * COMPRESSION_LEVEL_TILE), (int) (height * COMPRESSION_LEVEL_TILE), 0, 0, width, height, null);

        final BufferedImage mapImageGrey = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final Graphics mapGrey = mapImageGrey.createGraphics();
        mapGrey.drawImage(canvasMapGrey, 0, 0, width, height, 0, 0, (int) (width * COMPRESSION_LEVEL_TILE), (int) (height * COMPRESSION_LEVEL_TILE), null);

        // Shorten filenames and identify unique tiles
        for (final Sector thisSector : lstSectors) {
            final int coordX = thisSector.getPosition().getX();
            final int coordY = thisSector.getPosition().getY();
            tileName[coordX][coordY] = tileName[coordX][coordY].replaceAll("images/tiles/", "").replaceAll("/", "_").replaceAll(".png", "");

            // Check for neighboring hills, mountains and swamps
            tileName[coordX][coordY] += examineNeighboringElevation(coordX, coordY);

            if (!uniqueTiles.containsKey(tileName[coordX][coordY])) {
                uniqueTiles.put(tileName[coordX][coordY], new Point(coordX, coordY));
            }

            thisSector.setImage(tileName[coordX][coordY]);
            secMgr.update(thisSector);
        }

        for (final Map.Entry<String, Point> entry : uniqueTiles.entrySet()) {
            try {
                final File tile = new File(basePath + "images/tiles-generated/" + entry.getKey() + ".png");
                final File tileGrey = new File(basePath + "images/tiles-generated/fow-" + entry.getKey() + ".png");

                final int coordX = (int) entry.getValue().getX() * zoomTile;
                final int coordY = (int) entry.getValue().getY() * zoomTile;

                final BufferedImage canvas = new BufferedImage(zoomTile, zoomTile, BufferedImage.TYPE_INT_ARGB);
                final Graphics gImg = canvas.createGraphics();
                gImg.drawImage(mapImage, 0, 0, zoomTile, zoomTile, coordX, coordY, coordX + zoomTile, coordY + zoomTile, null);
                ImageIO.write(canvas, "png", tile);

                final BufferedImage canvasGrey = new BufferedImage(zoomTile, zoomTile, BufferedImage.TYPE_INT_ARGB);
                final Graphics gCanvasImgGrey = canvasGrey.createGraphics();
                gCanvasImgGrey.drawImage(mapImageGrey, 0, 0, zoomTile, zoomTile, coordX, coordY, coordX + zoomTile, coordY + zoomTile, null);
                ImageIO.write(canvasGrey, "png", tileGrey);

                totSize += tile.length();
            } catch (IOException e) {
                LOGGER.fatal(e);
            }
        }

        LOGGER.debug("Unique tiles=" + uniqueTiles.size() + " size=" + totSize);
    }

    private String examineNeighboringElevation(final int coordX, final int coordY) {
        String neighboring = "";

        if (coordX < tileName.length - 1 && tileElevName[coordX + 1][coordY].length() > 0) {
            neighboring = "-(E:" + tileElevName[coordX + 1][coordY] + ")";
        }

        if (coordX > 0 && tileElevName[coordX - 1][coordY].length() > 0) {
            neighboring += "-(W:" + tileElevName[coordX - 1][coordY] + ")";
        }

        if (coordY > 0 && tileElevName[coordX][coordY - 1].length() > 0) {
            neighboring += "-(N:" + tileElevName[coordX][coordY - 1] + ")";
        }

        if (coordX > 0 && coordY > 0 && tileElevName[coordX - 1][coordY - 1].length() > 0) {
            neighboring += "-(NW:" + tileElevName[coordX - 1][coordY - 1] + ")";
        }

        if (coordY < tileName[0].length - 1 && tileElevName[coordX][coordY + 1].length() > 0) {
            neighboring += "-(S:" + tileElevName[coordX][coordY + 1] + ")";
        }

        return neighboring;
    }

    /**
     * Main function.
     *
     * @param args String Argument
     */
    public static void main(final String[] args) {

        // check arguments
        if (args.length < 3) {
            LOGGER.fatal("fixMap arguments (gameId, scenarioId, basePath) are missing [" + args.length + "]");
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
        if (args[2].length() > 2) {
            basePath = args[2];
        } else {
            LOGGER.warn("Using default path: " + basePath);
        }

        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(scenarioId);

        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final Nation neutral = NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL);

        final Game game = GameManager.getInstance().getByID(gameId);

        final List<Nation> lstNations;
        final List<Region> lstRegion;
        switch (scenarioId) {
            case HibernateUtil.DB_FREE:
                lstNations = new ArrayList<Nation>();

                // add single nation
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));

                lstRegion = new ArrayList<Region>();
                lstRegion.add(RegionManager.getInstance().getByID(EUROPE));
                lstRegion.add(RegionManager.getInstance().getByID(CARIBBEAN));
                break;

            case HibernateUtil.DB_S3:
                lstNations = new ArrayList<Nation>();
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_SPAIN));
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN));

                lstRegion = RegionManager.getInstance().list();
                break;

            case HibernateUtil.DB_S1:
            case HibernateUtil.DB_S2:
            default:
                lstNations = NationManager.getInstance().list();
                                        lstNations.remove(0);

                lstRegion = RegionManager.getInstance().list();
                break;
        }

        LOGGER.info("Launching map drawing tasks");

        for (final Region region : lstRegion) {
            final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(game, region);

//            (new MapRegionExecutor(game, basePath, neutral, region, sectorList)).call();
//            LOGGER.info("Task (Region) submitted for execution [" + region.getId() + "]");
//
//            (new MapNationExecutor(game, basePath, lstNations.get(0), region, sectorList)).call();
//            LOGGER.info("Task (Region,Nation) submitted for execution [" + region.getId() + "]");

//
//            if (!game.isFogOfWar()) {
//                futures.add(executorService.submit(new MapNationExecutor(game, basePath, lstNations.get(0), region, sectorList)));
//                LOGGER.info("Task (Region,Nation) submitted for execution [" + region.getId() + "]");
//
//            } else {
//                for (final Nation nation : lstNations) {
//                    futures.add(executorService.submit(new MapNationExecutor(game, basePath, nation, region, sectorList)));
//                    LOGGER.info("Task (Region,Nation) submitted for execution [" + region.getId() + "/" + nation.getCode() + "]");
//                }
//            }

            //final MapCreator mapWindowTiles = new MapCreator(game, sectorList, neutral, basePath + "/", 1d, false);
            //mapWindowTiles.drawTiles();
            //mapWindowTiles.draw(basePath + "/scenario-geo-" + region.getId() + ".png");

            final MapCreator mapWindowTiles = new MapCreator(game, sectorList, neutral, basePath + "/", .1d, false, false, true);
            mapWindowTiles.draw(basePath + "/" + region.getId() + ".png");

//            final MapCreator mapWindowTilesLow = new MapCreator(game, sectorList, lstNations.get(1), basePath + "/", .7d, false, false, true);
//            mapWindowTilesLow.draw(basePath + "/land-" + region.getId() + "-lowres.png");
//
//            final MapCreator mapWindowTilesVLow = new MapCreator(game, sectorList, lstNations.get(1), basePath + "/", .5d, false, false, true);
//            mapWindowTilesVLow.draw(basePath + "/land-" + region.getId() + "-vlowres.png");
//
//            final MapCreator mapWindowTilesVVLow = new MapCreator(game, sectorList, lstNations.get(1), basePath + "/", .25d, false, false, true);
//            mapWindowTilesVVLow.draw(basePath + "/land-" + region.getId() + "-vvlowres.png");

//            // create scenario geo-images
//            final MapCreator mapWindowInfo = new MapCreator(game, sectorList, neutral, basePath + "/", .4d, true, false, false);
//            mapWindowInfo.draw(basePath + "/images/maps/scenario-" + region.getId() + ".png");
//
//            final MapCreator mapWindowGeo = new MapCreator(game, sectorList, neutral, basePath + "/", 1d, true, true, true);
//            mapWindowGeo.draw(basePath + "/images/maps/scenario-geo-" + region.getId() + ".png");

//            if (game.isFogOfWar()) {
//                for (Nation nation : lstNations) {
//                    final MapCreator mapWindow = new MapCreator(game, sectorList, nation, basePath + "/", 1d, false, false, true);
//                    mapWindow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + ".png");
//
//                    final MapCreator mapWindowLow = new MapCreator(game, sectorList, nation, basePath + "/", .7d, false, false, false);
//                    mapWindowLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-lowres.png");
//
//                    final MapCreator mapNationVLow = new MapCreator(game, sectorList, nation, basePath + "/", .5d, true, true, false);
//                    mapNationVLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-vlowres.png");
//
//                    final MapCreator mapNationVVLow = new MapCreator(game, sectorList, nation, basePath + "/", .25d, true, true, false);
//                    mapNationVVLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-vvlowres.png");
//
//                    final MapCreator mapNationBorder = new MapCreator(game, sectorList, nation, basePath + "/", 1d, true, true, false);
//                    mapNationBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border.png");
//
//                    final MapCreator mapNationLowBorder = new MapCreator(game, sectorList, nation, basePath + "/", .7d, true, true, false);
//                    mapNationLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border-lowres.png");
//
//                    final MapCreator mapNationVLowBorder = new MapCreator(game, sectorList, nation, basePath + "/", .5d, true, true, false);
//                    mapNationVLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border-vlowres.png");
//
//                    final MapCreator mapNationVVLowBorder = new MapCreator(game, sectorList, nation, basePath + "/", .25d, true, true, false);
//                    mapNationVVLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + nation.getId() + "-border-vvlowres.png");
//                }
//
//            } else {
//                final MapCreator mapWindow = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", 1d, false, false, true);
//                mapWindow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0.png");
//
//                final MapCreator mapNationLow = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", .7d, false, false, true);
//                mapNationLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-lowres.png");
//
//                final MapCreator mapNationVLow = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", .5d, false, false, true);
//                mapNationVLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-vlowres.png");
//
//                final MapCreator mapNationVVLow = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", .25d, false, false, true);
//                mapNationVVLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-vvlowres.png");
//
//                final MapCreator mapNationBorder = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", 1d, true, true, true);
//                mapNationBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-border.png");
//
//                final MapCreator mapNationLowBorder = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", .7d, true, true, true);
//                mapNationLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-border-lowres.png");
//
//                final MapCreator mapNationVLowBorder = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", .5d, true, true, true);
//                mapNationVLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-border-vlowres.png");
//
//                final MapCreator mapNationVVLowBorder = new MapCreator(game, sectorList, lstNations.get(0), basePath + "/", .25d, true, true, true);
//                mapNationVVLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-0-border-vvlowres.png");
//            }

//            final MapCreator mapWindow = new MapCreator(game, sectorList, neutral, basePath + "/", 1d, true, false, false);
//            mapWindow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + ".png");
//
//            final MapCreator mapWindowInfo = new MapCreator(game, sectorList, neutral, basePath + "/", 0.2d, true, false, false);
//            mapWindowInfo.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-info.png");
//
//            final MapCreator mapWindowSmall = new MapCreator(game, sectorList, neutral, basePath + "/", 0.1d, true, false, false);
//            mapWindowSmall.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-small.png");

        }

//        // wait for the execution all tasks
//        try {
//            // wait for all tasks to complete before continuing
//            for (Future<Boolean> task : futures) {
//                task.get();
//            }
//
//            executorService.shutdownNow();
//
//        } catch (Exception ex) {
//            LOGGER.error("Task execution interrupted", ex);
//        }

        LOGGER.info("All map drawing tasks completed.");

        theTrans.commit();

//        final List<Integer> gameIdList = new ArrayList<Integer>();
//        gameIdList.add(2);
//
//        for (int gameId : gameIdList) {
//            // Initialize transaction
//            final Session thisSession = HibernateUtil.getInstance().getSession();
//            thisSession.beginTransaction();
//
//            final Game thisGame = GameManager.getInstance().getByID(gameId);
//            for (int regionId = EUROPE; regionId <= AFRICA; regionId++) {
//                final Region thisRegion = RegionManager.getInstance().getByID(regionId);
//                final Nation neutral = NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL);
//                final Nation thisNation = NationManager.getInstance().getByID(NationConstants.NATION_RHINE);
//
//                // Initialize Sector list
//                final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(thisGame, thisRegion);
//
//                //final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(thisGame, thisRegion, 4, 0, 7, 2);

//            // create scenario geo-images
//            final MapCreator mapWindowInfo = new MapCreator(game, sectorList, neutral, basePath + "/", .4d, true, false, false);
//            mapWindowInfo.draw(basePath + "/images/maps/scenario-" + region.getId() + ".png");
//
//            final MapCreator mapWindow = new MapCreator(game, sectorList, neutral, basePath + "/", 1d, true, true, true);
//            mapWindow.draw(basePath + "/images/maps/scenario-geo-" + region.getId() + ".png");

//                //final MapCreator mapWindow = new MapCreator(thisGame, sectorList, thisNation, "/srv/eaw1805/", 1d, true);
//                //mapWindow.draw("mapWindow-" + thisRegion.getId() + "-" + 1 + ".png");
//
//                //final MapCreator mapWindowPublic = new MapCreator(thisGame, sectorList, neutral, "/srv/eaw1805/", 0.1d, true);
//                //mapWindowPublic.draw("scenario-" + thisRegion.getId() + ".png");
//
//                final MapCreator mapWindowTiles = new MapCreator(thisGame, sectorList, neutral, "/srv/eaw1805/", 1d, false);
//                mapWindowTiles.drawTiles();
//                //mapWindowTiles.reverseDrawMap();
//            }
//
//            // Finalize transaction
//            thisSession.getTransaction().commit();
//        }

        HibernateUtil.getInstance().closeSessions();
    }

}
