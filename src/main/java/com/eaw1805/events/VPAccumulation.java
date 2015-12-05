package com.eaw1805.events;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accumulates VPs.
 */
public class VPAccumulation
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, ReportConstants, NationConstants, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(VPAccumulation.class);

    /**
     * The current game.
     */
    private final Game thisGame;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public VPAccumulation(final EventProcessor myParent) {
        super(myParent);
        thisGame = myParent.getGameEngine().getGame();
        LOGGER.debug("VPAccumulation instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Game initGame = GameManager.getInstance().getByID(-1);

        // Check alive nations
        final List<Nation> activeNations = getParent().getGameEngine().getAliveNations();
        for (Nation thisNation : activeNations) {
            // Count number of neutral sectors conquered by inspecting initial state of map
            final List<Sector> lstInitialSectors = SectorManager.getInstance().listByGameNation(initGame, thisNation);

            int initEurope = 0;

            // Calculate initial european and colonial positions
            // and construct set
            final Set<Position> sectorSet = new HashSet<Position>();
            for (final Sector initialSector : lstInitialSectors) {
                if (initialSector.getPosition().getRegion().getId() == EUROPE) {
                    initEurope++;
                }

                sectorSet.add(initialSector.getPosition());
            }

            // Very naive: Now examine this game's sectors
            final List<Sector> lstSectors = SectorManager.getInstance().listByGameNation(thisGame, thisNation);

            int curEurope = 0;
            int curColonial = 0;
            int curNeutral = 0;

            // Very naive check
            for (final Sector thisSector : lstSectors) {
                final Position initPos = (Position) thisSector.getPosition().clone();
                initPos.setGame(initGame);

                if (!sectorSet.contains(initPos)) {
                    // This is a conquered sector
                    if (thisSector.getPosition().getRegion().getId() == EUROPE) {
                        // Retrieve previous owner to check if was neutral
                        final Sector initSector = SectorManager.getInstance().getByPosition(initPos);
                        if (initSector.getNation().getId() == NATION_NEUTRAL) {
                            curNeutral++;
                        } else {
                            curEurope++;
                        }

                    } else {
                        curColonial++;
                    }
                }
            }

            // For every 40 non-neutral coordinates conquered beyond your home nation in europe
            final int batchEurope = curEurope / 40;
            if (batchEurope > 0) {
                // We gain 1 VP
                changeVP(thisGame, thisNation, batchEurope * CONQUER_40_EUROPE,
                        "For every 40 non-neutral coordinates conquered beyond your home nation in europe (" + curEurope + ")");
            }

            // For every 90 neutral coordinates conquered beyond your home nation in europe
            final int batchNeutral = curNeutral / 90;
            if (batchNeutral > 0) {
                // We gain 1 VP
                changeVP(thisGame, thisNation, batchNeutral * CONQUER_90_NEUTRAL,
                        "For every 90 neutral coordinates conquered beyond your home nation in europe (" + curNeutral + ")");
            }

            // For every 60 colonial coordinates conquered
            final int batchColonial = curColonial / 60;
            if (batchColonial > 0) {
                // We gain 1 VP
                changeVP(thisGame, thisNation, batchColonial * CONQUER_60_COLONIES,
                        "For every 60 colonial coordinates conquered (" + curColonial + ")");
            }

            // Check if Double coordinates (from starting coordinates) in Europe bonus
            if (curEurope > initEurope) {
                // once only
                final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(thisNation, thisGame, "double-coordinates");
                if (lstReports.isEmpty()) {
                    // We gain 8 VP
                    changeVP(thisGame, thisNation, DOUBLE_EUROPE,
                            "Double coordinates (from starting coordinates) in Europe");

                    newsGlobal(thisNation, NEWS_MILITARY, true,
                            "We have doubled our territories in Europe! Glory to our ruler.",
                            thisNation.getName() + " doubled their territories in Europe!");

                    report(thisNation, "double-coordinates", "1");
                }
            }

            // Check if Triple coordinates (from starting coordinates) in Europe bonus
            if (curEurope > initEurope * 2) {
                // once only
                final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(thisNation, thisGame, "triple-coordinates");
                if (lstReports.isEmpty()) {
                    // We gain 18 VP
                    changeVP(thisGame, thisNation, TRIPLE_EUROPE,
                            "Triple coordinates (from starting coordinates) in Europe");

                    newsGlobal(thisNation, NEWS_MILITARY, true,
                            "We have tripled our territories in Europe! Glory to our great ruler.",
                            thisNation.getName() + " tripled their territories in Europe!");

                    report(thisNation, "triple-coordinates", "1");
                }
            }
        }

        // Scenario 1808
        if (getGame().getScenarioId() == HibernateUtil.DB_S3) {

            // +1 per turn for each Trade city owned
            final Map<Nation, Integer> totCities = new HashMap<Nation, Integer>();
            totCities.put(NationManager.getInstance().getByID(NATION_SPAIN), 0);
            totCities.put(NationManager.getInstance().getByID(NATION_FRANCE), 0);
            totCities.put(NationManager.getInstance().getByID(NATION_GREATBRITAIN), 0);

            final List<TradeCity> lstCities = TradeCityManager.getInstance().listByGame(thisGame);
            for (TradeCity lstCity : lstCities) {
                final Nation cityOwner = lstCity.getNation();
                if (totCities.containsKey(lstCity.getNation())) {
                    final int curCityCounter = totCities.get(lstCity.getNation());
                    totCities.put(cityOwner, curCityCounter + 1);
                }
            }

            for (Map.Entry<Nation, Integer> entry : totCities.entrySet()) {
                changeVP(thisGame, entry.getKey(), CONTROL_TRADE_CITY * entry.getValue(),
                        "For each Trade City owned (" + entry.getValue() + ")");
            }

            // -2 per turn if capital under foreign occupation (Madrid, Lisbon, Bordeaux)
            // Lookup Madrid
            final Position posMadrid = new Position();
            posMadrid.setGame(thisGame);
            posMadrid.setRegion(RegionManager.getInstance().getByID(EUROPE));
            posMadrid.setX(23);
            posMadrid.setY(18);

            final Sector secMadrid = SectorManager.getInstance().getByPosition(posMadrid);
            if (secMadrid.getNation().getId() != NATION_SPAIN) {
                changeVP(thisGame, NationManager.getInstance().getByID(NATION_SPAIN), CONTROL_CAPITAL,
                        "Capital under foreign occupation");
            }

            // Lookup Lisbon
            final Position posLisbon = new Position();
            posLisbon.setGame(thisGame);
            posLisbon.setRegion(RegionManager.getInstance().getByID(EUROPE));
            posLisbon.setX(4);
            posLisbon.setY(25);

            final Sector secLisbon = SectorManager.getInstance().getByPosition(posLisbon);
            if (secLisbon.getNation().getId() != NATION_GREATBRITAIN) {
                changeVP(thisGame, NationManager.getInstance().getByID(NATION_GREATBRITAIN), CONTROL_CAPITAL,
                        "Capital under foreign occupation");
            }

            // Lookup Bordeaux
            final Position posBordeaux = new Position();
            posBordeaux.setGame(thisGame);
            posBordeaux.setRegion(RegionManager.getInstance().getByID(EUROPE));
            posBordeaux.setX(31);
            posBordeaux.setY(0);

            final Sector secBordeaux = SectorManager.getInstance().getByPosition(posBordeaux);
            if (secBordeaux.getNation().getId() != NATION_FRANCE) {
                changeVP(thisGame, NationManager.getInstance().getByID(NATION_FRANCE), CONTROL_CAPITAL,
                        "Capital under foreign occupation");
            }

        }
    }
}
