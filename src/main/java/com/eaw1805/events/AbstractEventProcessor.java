package com.eaw1805.events;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.orders.AbstractOrderProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Abstract implementation of an event processor.
 */
public abstract class AbstractEventProcessor
        extends AbstractOrderProcessor
        implements EventInterface, NewsConstants, ReportConstants, NationConstants, RegionConstants {

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public AbstractEventProcessor(final EventProcessor myParent) {
        super(myParent);
    }

    public AbstractEventProcessor(final Game game) {
        super(game);
    }

    /**
     * Get the random number generator.
     *
     * @return the random number generator.
     */
    public final Random getRandomGen() {
        return getParent().getGameEngine().getRandomGen();
    }

    /**
     * Add a report entry for this turn.
     *
     * @param game   the game of the report entry.
     * @param nation the owner of the report entry.
     * @param key    the key of the report entry.
     * @param value  the value of the report entry.
     */
    protected void report(final Game game, final Nation nation, final String key, final String value) {
        final Nation thisNation = NationManager.getInstance().getByID(nation.getId());
        final Report thisReport = new Report();
        thisReport.setGame(game);
        thisReport.setTurn(game.getTurn());
        thisReport.setNation(thisNation);
        thisReport.setKey(key);
        thisReport.setValue(value);
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Add a report entry for this turn.
     *
     * @param nation the owner of the report entry.
     * @param turn   the turn of the report entry.
     * @param key    the key of the report entry.
     * @param value  the value of the report entry.
     */
    protected void report(final Nation nation, final int turn, final String key, final String value) {
        final Nation thisNation = NationManager.getInstance().getByID(nation.getId());
        final Report thisReport = new Report();
        thisReport.setGame(getParent().getGameEngine().getGame());
        thisReport.setTurn(turn);
        thisReport.setNation(thisNation);
        thisReport.setKey(key);
        thisReport.setValue(value);
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Retrieve a report entry for this turn.
     *
     * @param owner the owner of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    public String getReport(final Nation owner, final String key) {
        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner,
                getParent().getGameEngine().getGame(), getParent().getGameEngine().getGame().getTurn() - 1, key);
        if (thisReport == null) {
            return "";
        } else {
            return thisReport.getValue();
        }
    }

    /**
     * Pick a random nation.
     *
     * @return a Nation object.
     */
    protected final Nation pickNation() {
        final List<Nation> aliveNations = getParent().getGameEngine().getAliveNations();
        // Choose a nation at random
        java.util.Collections.shuffle(aliveNations);
        return NationManager.getInstance().getByID(aliveNations.get(0).getId());
    }

    /**
     * Pick a random nation.
     *
     * @return a Nation object.
     */
    protected final Nation pickNation(final Nation exception) {
        final List<Nation> aliveNations = getParent().getGameEngine().getAliveNations();

        // Remove exception
        if (aliveNations.contains(exception)) {
            aliveNations.remove(exception);
        }

        // Choose a nation at random
        java.util.Collections.shuffle(aliveNations);
        return NationManager.getInstance().getByID(aliveNations.get(0).getId());
    }

    /**
     * Determine if each nation is still controlling it's capital.
     *
     * @return a map of nations to boolean values (true if the nation still controls its capitol).
     */
    protected Map<Integer, Boolean> checkCapitalOwners(final Game thisGame) {
        final Map<Integer, Boolean> stillInControlOfCapital = new HashMap<Integer, Boolean>();

        // Initialize map
        final List<Nation> nationList = NationManager.getInstance().list();
        nationList.remove(0); // Remove Free Nation
        for (final Nation nation : nationList) {
            stillInControlOfCapital.put(nation.getId(), false);
        }

        final Game initGame = GameManager.getInstance().getByID(-1);
        final List<TradeCity> initTradeCities = TradeCityManager.getInstance().listByGame(initGame);
        final List<TradeCity> lstTradeCities = TradeCityManager.getInstance().listByGame(thisGame);

        for (final TradeCity initCity : initTradeCities) {
            if (initCity.getPosition().getRegion().getId() != EUROPE) {
                continue;
            }

            for (final TradeCity city : lstTradeCities) {
                if (initCity.getPosition().getX() == city.getPosition().getX()
                        && initCity.getPosition().getY() == city.getPosition().getY()) {

                    final int originalOwnerID = initCity.getNation().getId();
                    final int currentOwnerID = city.getNation().getId();

                    // Different treatment for RUSSIA
                    if (originalOwnerID == NATION_RUSSIA) {
                        // Only Russia has 2 capitals
                        final boolean otherCapital = stillInControlOfCapital.get(originalOwnerID);
                        stillInControlOfCapital.put(originalOwnerID, (originalOwnerID == currentOwnerID || otherCapital));

                    } else {
                        // This is the original capital of the owner
                        stillInControlOfCapital.put(originalOwnerID, originalOwnerID == currentOwnerID);
                    }
                }
            }
        }

        return stillInControlOfCapital;
    }

}
