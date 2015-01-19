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
 * Threaded execution of MapCreator for all maps related to a nation+region.
 */
public class MapRegionExecutor
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
    public MapRegionExecutor(final Game theGame,
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

        final MapCreator mapWindow = new MapCreator(game, lstSectors, viewer, basePath + "/", 1d, true, false, false);
        mapWindow.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + ".png");

        final MapCreator mapWindowInfo = new MapCreator(game, lstSectors, viewer, basePath + "/", .2d, true, false, false);
        mapWindowInfo.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-info.png");

        final MapCreator mapWindowSmall = new MapCreator(game, lstSectors, viewer, basePath + "/", .1d, true, false, false);
        mapWindowSmall.draw(basePath + "/images/maps/s" + game.getScenarioId() + "/" + game.getGameId() + "/map-" + game.getGameId() + "-" + game.getTurn() + "-" + region.getId() + "-small.png");

        theTrans.commit();
        return true;
    }
}
