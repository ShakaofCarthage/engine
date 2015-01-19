package com.eaw1805.events;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

/**
 * Examine if a country has died.
 */
public class CivilDisorderEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, ReportConstants, NationConstants, RelationConstants, VPConstants, ProfileConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CivilDisorderEvent.class);

    /**
     * The current game.
     */
    private final Game thisGame;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CivilDisorderEvent(final EventProcessor myParent) {
        super(myParent);
        thisGame = myParent.getGameEngine().getGame();
        LOGGER.debug("CivilDisorderEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Nation> nationList = NationManager.getInstance().list();
        nationList.remove(0); // Remove Free Nation

        // Look-up capitals, original owners and current owners
        Map<Integer, Boolean> capitalsInControl = checkCapitalOwners(thisGame);

        // Go through all nations one by one
        for (final Nation nation : nationList) {
            // Check if nation was already dead from previous turn
            final int lastReport = retrieveReportAsInt(nation, thisGame.getTurn() - 1, N_ALIVE);
            if (lastReport == 1) {

                // Retrieve home population size
                final int popEurope = getHomePopulation(nation);

                // An empire is considered to have surrendered when its home population is reduced to below 100,000 citizens,
                // and its capital is occupied.
                final boolean stillControlsCapital = capitalsInControl.get(nation.getId());
                if (!stillControlsCapital && popEurope < 100000) {
                    // Look-up armies per region
                    boolean hasBigArmyInHome = false;

                    // If an army of more than 100 battalions (with a minimum headcount of 400) is still present in a
                    // home region coordinate, then the country does not surrender.
                    final Map<Sector, BigInteger> lstBatt = BattalionManager.getInstance().countBattalions(thisGame, nation, 400, false);
                    for (final Map.Entry<Sector, BigInteger> entry : lstBatt.entrySet()) {
                        // Check if this entry is a home region for the particular nation
                        final Sector thisSector = entry.getKey();
                        hasBigArmyInHome |= (thisSector.getPosition().getRegion().getId() == EUROPE
                                && getSphere(thisSector, nation) == 1
                                && entry.getValue().intValue() >= 100);
                    }

                    if (!hasBigArmyInHome) {
                        // This is a dead nation
                        report(nation, N_ALIVE, 0);

                        LOGGER.info(nation.getName() + " enters civil disorder");
                        newsGlobal(nation, NEWS_MILITARY, true,
                                "Our nation enters civil disorder !",
                                nation.getName() + " enters civil disorder !");

                        // Modify player's profile for playing the empire until Civil Disorder
                        changeProfile(nation, ProfileConstants.VPS, FIGHT_UNTIL_CD);

                        for (final Nation thisNation : nationList) {

                            // check if nation was at war with dead empire for more than 8 months
                            if (thisNation.getId() != nation.getId()) {
                                // Retrieve relations
                                final NationsRelation thisRelation = getByNations(thisNation, nation);
                                final NationsRelation thatRelation = getByNations(nation, thisNation);

                                // Check if an alliance was established
                                if (thisRelation.getRelation() == REL_ALLIANCE) {
                                    // Reduce relation to Passage
                                    thisRelation.setRelation(REL_PASSAGE);
                                    RelationsManager.getInstance().update(thisRelation);

                                    thatRelation.setRelation(REL_PASSAGE);
                                    RelationsManager.getInstance().update(thatRelation);

                                    newsGlobal(nation, thisNation, NEWS_POLITICAL,
                                            "Our alliance with " + thisNation.getName() + " is no longer after entering civil disorder.",
                                            "Our alliance with " + nation.getName() + " is no longer valid after " + nation.getName() + " entered civil disorder.",
                                            "The alliance between " + nation.getName() + " and " + thisNation.getName() + " is no longer valid after " + nation.getName() + " entered civil disorder.");
                                }

                                // check if a war was going on
                                if (thisRelation.getRelation() == REL_WAR && thisRelation.getTurnCount() >= 8) {
                                    // Award VPs for winning this battle
                                    changeVP(thisGame, thisNation, ENEMY_ENTERS_CD, nation.getName() + " enters civil disorder");

                                    // Modify player's profile
                                    changeProfile(thisNation, ProfileConstants.EMPIRES_DESTROYED, 1);
                                }

                                // Release any prisoners held
                                if (thatRelation.getPrisoners() > 0) {
                                    final NumberFormat formatter = new DecimalFormat("#,###,###");
                                    newsGlobal(nation, thisNation, NEWS_POLITICAL,
                                            "We released " + formatter.format(thatRelation.getPrisoners()) + " prisoners of " + thisNation.getName(),
                                            nation.getName() + " released " + formatter.format(thatRelation.getPrisoners()) + " of our soldiers held prisoners of war.",
                                            nation.getName() + " released " + formatter.format(thatRelation.getPrisoners()) + " of soldiers of " + thisNation.getName() + " held prisoners of war.");

                                    // Prisoners go directly to warehouse
                                    final Warehouse europe = WarehouseManager.getInstance().getByNationRegion(thisGame, thisNation, RegionManager.getInstance().getByID(EUROPE));
                                    europe.getStoredGoodsQnt().put(GoodConstants.GOOD_PEOPLE, europe.getStoredGoodsQnt().get(GoodConstants.GOOD_PEOPLE) + thatRelation.getPrisoners());
                                    WarehouseManager.getInstance().update(europe);

                                    // Report transfer
                                    final Report feesReport = ReportManager.getInstance().getByOwnerTurnKey(thisNation, thisGame,
                                            thisGame.getTurn(), "fees.region." + EUROPE + ".good." + GoodConstants.GOOD_PEOPLE);
                                    if (feesReport != null) {
                                        try {
                                            feesReport.setValue(Integer.toString(Integer.parseInt(feesReport.getValue()) + thatRelation.getPrisoners()));

                                        } catch (Exception ex) {
                                            LOGGER.error("Cannot parse report value", ex);
                                            feesReport.setValue(Integer.toString(thatRelation.getPrisoners()));
                                        }
                                        ReportManager.getInstance().update(feesReport);

                                    } else {
                                        final Report newReport = new Report();
                                        newReport.setGame(thisGame);
                                        newReport.setTurn(thisGame.getTurn());
                                        newReport.setNation(thisNation);
                                        newReport.setKey("fees.region." + EUROPE + ".good." + GoodConstants.GOOD_PEOPLE);
                                        newReport.setValue(Integer.toString(thatRelation.getPrisoners()));
                                        ReportManager.getInstance().add(newReport);
                                    }

                                    thatRelation.setPrisoners(0);
                                    RelationsManager.getInstance().update(thatRelation);
                                }

                                // Check if winning nation has prisoners of dead nation
                                if (thisRelation.getPrisoners() > 0) {
                                    // just release them to save food
                                    thisRelation.setPrisoners(0);
                                    RelationsManager.getInstance().update(thisRelation);
                                }

                                // Return captured commanders
                                final List<Commander> lstCommanders = CommanderManager.getInstance().listGameNation(getParent().getGame(), thisNation);
                                for (Commander commander : lstCommanders) {
                                    if (commander.getCaptured().getId() == nation.getId()) {
                                        // Release commander
                                        commander.setCaptured(commander.getNation());
                                        commander.setPool(true);
                                        CommanderManager.getInstance().update(commander);

                                        newsGlobal(nation, thisNation, NEWS_POLITICAL,
                                                "We released captured commander " + commander.getName() + " of " + thisNation.getName(),
                                                nation.getName() + " released captured commander " + commander.getName() + ".",
                                                nation.getName() + " released captured commander " + commander.getName() + " of " + thisNation.getName() + ".");
                                    }
                                }
                            }
                        }

                    } else {
                        // This nation is still alive
                        LOGGER.info(nation.getName() + " has fewer than 100000 people in EUROPE, lost the capitol, but an army of > 100 battalions in home region");
                        report(nation, N_ALIVE, 1);
                    }

                } else {
                    // This nation is still alive
                    report(nation, N_ALIVE, 1);
                }
            } else {
                LOGGER.info(nation.getName() + " is a dead empire");

                // This is a dead nation
                report(nation, N_ALIVE, 0);
            }
        }

        LOGGER.info("CivilDisorderEvent processed.");
    }

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final String key, final int value) {
        report(owner, key, Integer.toString(value)); // NOPMD
    }

    protected int getHomePopulation(final Nation owner) {
        final List<Sector> lstSectors = SectorManager.getInstance().listHomeSectors(thisGame, owner);
        int totPop = 0;
        for (final Sector sector : lstSectors) {
            totPop += sector.populationCount();
        }

        return totPop;
    }

}
