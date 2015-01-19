package com.eaw1805.core;

import com.eaw1805.core.initializers.GameInitializer;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.List;

public class ReInitGameParts {
    private static final Logger LOGGER = LogManager.getLogger(ReInitGameParts.class);


    public void reInitAries(int scenarioId, int gameId) {

        Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);

        final Game game = GameManager.getInstance().getByID(gameId);
        if (game.getTurn() > 0) {
            LOGGER.warn("Game not at turn 0, procedure aborted");
            thatTrans.rollback();
            return;
        }
        final Game scenarioGame = GameManager.getInstance().getByID(-1);
        clearOldArmies(game);
        LOGGER.info("INITIALIZING ARMIES FOR GAME " + game.getGameId());
        GameInitializer initializer = new GameInitializer(null, gameId, scenarioId, -1  );
        final List<Nation> lstNations = NationManager.getInstance().list();
        final List<Region> lstRegions = RegionManager.getInstance().list();
        initializer.initializeStatistics(lstNations, lstRegions);
        initializer.initBrigade(game, scenarioGame);
        initializer.initCorps(game, scenarioGame);
        thatTrans.commit();
    }

    private void clearOldArmies(Game game) {
        LOGGER.info("DELETING OLD ARMIES FOR GAME " + game.getGameId());
        final List<Brigade> gameBrigades = BrigadeManager.getInstance().listByGame(game);
        //delete all old brigades...
        for (Brigade brigade : gameBrigades) {
            BrigadeManager.getInstance().delete(brigade);
        }
    }



    public static void main(String[] args) {
        // check arguments
        if (args.length < 2) {
            LOGGER.fatal("Engine arguments (gameId, scenarioId, basePath, buildNumber) are missing [" + args.length + "]");
            return;
        }
        int gameId = Integer.parseInt(args[0]);
        int scenarioId = Integer.parseInt(args[1]);
        HibernateUtil.connectEntityManagers(scenarioId);
        final ReInitGameParts reInitializer = new ReInitGameParts();
        reInitializer.reInitAries(scenarioId, gameId);

    }

}
