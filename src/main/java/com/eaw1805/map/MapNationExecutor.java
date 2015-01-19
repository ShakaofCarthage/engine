package com.eaw1805.map;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.hibernate.Transaction;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Threaded execution of a MapCreator for all maps related to a region,
 */
public class MapNationExecutor
        implements Callable<Boolean> {

    /**
     * The game object fetched from the database.
     */
    private final Game game;

    /**
     * Directory name containing tiles.
     */
    private final String basePath;

    private final Nation viewer;

    private final Region region;

    private final List<Sector> lstSectors;

    /**
     * Default Constructor.
     *
     * @param theViewer  the viewer.
     * @param theRegion  the region to construct.
     * @param sectorList the list of sectors to draw.
     */
    public MapNationExecutor(final Game theGame,
                             final String thePath,
                             final Nation theViewer,
                             final Region theRegion,
                             final List<Sector> sectorList) {
        game = theGame;
        basePath = thePath;
        viewer = theViewer;
        region = theRegion;
        lstSectors = sectorList;
    }

    @Override
    public Boolean call() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(game.getScenarioId()).getCurrentSession().beginTransaction();

        int viewerId = viewer.getId();
        if (!game.isFogOfWar()) {
            viewerId = 0;
        }

        final MapCreator mapviewer = new MapCreator(game, lstSectors, viewer, basePath + "/", 1d, false, false, !game.isFogOfWar());
        mapviewer.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + ".png");

        final MapCreator mapviewerLow = new MapCreator(game, lstSectors, viewer, basePath + "/", .7d, false, false, !game.isFogOfWar());
        mapviewerLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-lowres.png");

        final MapCreator mapviewerVLow = new MapCreator(game, lstSectors, viewer, basePath + "/", .5d, false, false, !game.isFogOfWar());
        mapviewerVLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-vlowres.png");

        final MapCreator mapviewerVVLow = new MapCreator(game, lstSectors, viewer, basePath + "/", .25d, false, false, !game.isFogOfWar());
        mapviewerVVLow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-vvlowres.png");

        final MapCreator mapviewerBorder = new MapCreator(game, lstSectors, viewer, basePath + "/", 1d, true, true, !game.isFogOfWar());
        mapviewerBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-border.png");

        final MapCreator mapviewerLowBorder = new MapCreator(game, lstSectors, viewer, basePath + "/", .7d, true, true, !game.isFogOfWar());
        mapviewerLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-border-lowres.png");

        final MapCreator mapviewerVLowBorder = new MapCreator(game, lstSectors, viewer, basePath + "/", .5d, true, true, !game.isFogOfWar());
        mapviewerVLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-border-vlowres.png");

        final MapCreator mapviewerVVLowBorder = new MapCreator(game, lstSectors, viewer, basePath + "/", .25d, true, true, !game.isFogOfWar());
        mapviewerVVLowBorder.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-" + viewerId + "-border-vvlowres.png");

        theTrans.commit();

        return true;
    }
}
