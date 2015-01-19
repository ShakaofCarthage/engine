package com.eaw1805.events;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.AchievementConstants;
import com.eaw1805.data.constants.GameConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.AchievementManager;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.model.Achievement;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Determines if a nation has reached the winning conditions and identifies possible runner-ups.
 */
public class CheckWinners
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, ReportConstants, NationConstants, VPConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CheckWinners.class);

    /**
     * The current game.
     */
    private final Game thisGame;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CheckWinners(final EventProcessor myParent) {
        super(myParent);
        thisGame = myParent.getGameEngine().getGame();
        LOGGER.debug("CheckWinners instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Set<Nation> winningNations = new HashSet<Nation>();
        final Set<Nation> coWinningNations = new HashSet<Nation>();
        final Set<Nation> runnerUpNations = new HashSet<Nation>();

        final StringBuilder winners = new StringBuilder();
        final StringBuilder coWinners = new StringBuilder();
        final StringBuilder runnerUp = new StringBuilder();

        final StringBuilder winnersID = new StringBuilder();
        final StringBuilder coWinnersID = new StringBuilder();
        final StringBuilder runnerUpID = new StringBuilder();

        final double modifier;
        switch (thisGame.getType()) {
            case GameConstants.DURATION_SHORT:
                modifier = .7d;
                break;

            case GameConstants.DURATION_LONG:
                modifier = 1.3d;
                break;

            case GameConstants.DURATION_NORMAL:
            default:
                modifier = 1d;
        }

        // Prepare special categories
        final List<List<Nation>> lstSpecialCategories = new ArrayList<List<Nation>>();
        final List<Integer> lstSpecialCategoriesLevel = new ArrayList<Integer>();

        // List of all nations
        final List<Nation> lstNation = NationManager.getInstance().list();

        // List special Muslim Nations
        final List<Nation> lstMuslim = new ArrayList<Nation>();
        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            lstMuslim.add(NationManager.getInstance().getByID((NATION_MOROCCO)));
            lstMuslim.add(NationManager.getInstance().getByID((NATION_OTTOMAN)));
            lstMuslim.add(NationManager.getInstance().getByID((NATION_EGYPT)));
        }
        lstSpecialCategories.add(lstMuslim);
        lstSpecialCategoriesLevel.add(NATION_LAST + 2);

        // List special Militaristic Nations
        final List<Nation> lstMilitaristic = new ArrayList<Nation>();
        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            lstMilitaristic.add(NationManager.getInstance().getByID((NATION_RHINE)));
            lstMilitaristic.add(NationManager.getInstance().getByID((NATION_FRANCE)));
            lstMilitaristic.add(NationManager.getInstance().getByID((NATION_PRUSSIA)));

        } else if (getGame().getScenarioId() == HibernateUtil.DB_S3
                || getGame().getScenarioId() == HibernateUtil.DB_FREE) {
            lstMilitaristic.add(NationManager.getInstance().getByID((NATION_FRANCE)));
        }
        lstSpecialCategories.add(lstMilitaristic);
        lstSpecialCategoriesLevel.add(NATION_LAST + 3);

        // List special Agricultural Nations
        final List<Nation> lstAgricultural = new ArrayList<Nation>();
        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_AUSTRIA)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_SPAIN)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_ITALY)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_MOROCCO)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_NAPLES)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_RUSSIA)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_OTTOMAN)));
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_WARSAW)));

        } else if (getGame().getScenarioId() == HibernateUtil.DB_S3) {
            lstAgricultural.add(NationManager.getInstance().getByID((NATION_SPAIN)));
        }
        lstSpecialCategories.add(lstAgricultural);
        lstSpecialCategoriesLevel.add(NATION_LAST + 4);

        // List special Industrious Nations
        final List<Nation> lstIndustrious = new ArrayList<Nation>();
        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            lstIndustrious.add(NationManager.getInstance().getByID((NATION_DENMARK)));
            lstIndustrious.add(NationManager.getInstance().getByID((NATION_SWEDEN)));
        }
        lstSpecialCategories.add(lstIndustrious);
        lstSpecialCategoriesLevel.add(NATION_LAST + 5);

        // List special Maritime Nations
        final List<Nation> lstMaritime = new ArrayList<Nation>();
        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            lstMaritime.add(NationManager.getInstance().getByID((NATION_GREATBRITAIN)));
            lstMaritime.add(NationManager.getInstance().getByID((NATION_PORTUGAL)));

        } else if (getGame().getScenarioId() == HibernateUtil.DB_S3) {
            lstMaritime.add(NationManager.getInstance().getByID((NATION_GREATBRITAIN)));
        }
        lstSpecialCategories.add(lstMaritime);
        lstSpecialCategoriesLevel.add(NATION_LAST + 6);

        // List special Merchantile Nations
        final List<Nation> lstMerchantile = new ArrayList<Nation>();
        if (getGame().getScenarioId() == HibernateUtil.DB_S1
                || getGame().getScenarioId() == HibernateUtil.DB_S2) {
            lstMerchantile.add(NationManager.getInstance().getByID((NATION_HOLLAND)));
            lstMerchantile.add(NationManager.getInstance().getByID((NATION_EGYPT)));
        }
        lstSpecialCategories.add(lstMerchantile);
        lstSpecialCategoriesLevel.add(NATION_LAST + 7);

        // remove free nation
        lstNation.remove(0);
        lstSpecialCategories.add(lstNation);
        lstSpecialCategoriesLevel.add(NATION_LAST + 1);

        // Check if any nation has crossed the VP limit
        final List<Nation> activeNations = getParent().getGameEngine().getAliveNations();
        for (Nation thisNation : activeNations) {

            final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(thisNation,
                    thisGame, thisGame.getTurn(), N_VP);

            if (thisReport != null) {
                final double targetVpWin = thisNation.getVpWin() * modifier;
                int currentVP = Integer.parseInt(thisReport.getValue());

                // Scenario 1808: GB and Spain have a combined VP pool.
                if (thisGame.getScenarioId() == HibernateUtil.DB_S3) {
                    if (thisNation.getId() == NATION_SPAIN) {
                        final Nation alliedNation = NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN);
                        final Report alliedReport = ReportManager.getInstance().getByOwnerTurnKey(thisNation,
                                thisGame, thisGame.getTurn(), N_VP);
                        if (alliedReport != null) {
                            currentVP += Integer.parseInt(alliedReport.getValue());
                        }

                    } else if (thisNation.getId() == NATION_GREATBRITAIN) {
                        final Nation alliedNation = NationManager.getInstance().getByID(NationConstants.NATION_SPAIN);
                        final Report alliedReport = ReportManager.getInstance().getByOwnerTurnKey(thisNation,
                                thisGame, thisGame.getTurn(), N_VP);
                        if (alliedReport != null) {
                            currentVP += Integer.parseInt(alliedReport.getValue());
                        }
                    }
                }

                if (currentVP >= targetVpWin) {
                    LOGGER.info(thisNation.getName() + " has reached winning VP limit !!! WINNER IDENTIFIED");
                    winningNations.add(thisNation);

                    winners.append(thisNation.getName());
                    winners.append(", ");

                    winnersID.append("*");
                    winnersID.append(thisNation.getId());

                    // Modify player's profile
                    changeProfile(thisNation, ProfileConstants.VPS, ProfileConstants.WINNER);

                    // check if the player has played for a full game
                    final User owner = getParent().getGameEngine().getUser(thisNation);
                    final List<UserGame> lstPlayers = UserGameManager.getInstance().list(getParent().getGame(), thisNation);
                    if (lstPlayers.size() == 1) {
                        // Only 1 player, award achievement
                        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                        if (!AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.PLAYNATION, thisNation.getId())) {
                            // Generate new entry
                            final Achievement entry = new Achievement();
                            entry.setUser(owner);
                            entry.setCategory(AchievementConstants.PLAYNATION);
                            entry.setLevel(thisNation.getId());
                            entry.setAnnounced(false);
                            entry.setFirstLoad(false);
                            entry.setDescription(AchievementConstants.PLAYNATION_STR[thisNation.getId()]);
                            entry.setVictoryPoints(0);
                            entry.setAchievementPoints(AchievementConstants.PLAYNATION_AP);
                            AchievementManager.getInstance().add(entry);


                            // Also check if player managed to win with all nations
                            boolean allNations = true;
                            for (Nation nation : lstNation) {
                                allNations &= AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.PLAYNATION, nation.getId());
                            }

                            if (allNations) {
                                // Generate new entry
                                final Achievement entryAll = new Achievement();
                                entryAll.setUser(owner);
                                entryAll.setCategory(AchievementConstants.PLAYNATION);
                                entryAll.setLevel(thisNation.getId());
                                entryAll.setAnnounced(false);
                                entryAll.setFirstLoad(false);
                                entryAll.setDescription(AchievementConstants.PLAYNATION_STR[NationConstants.NATION_LAST + 1]);
                                entryAll.setVictoryPoints(0);
                                entryAll.setAchievementPoints(AchievementConstants.PLAYNATION_AP_ALL);
                                AchievementManager.getInstance().add(entry);
                            }
                        }
                        mainTrans.commit();
                    }

                    // Check if the player has already won with this nation
                    final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                    if (!AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.WINNATION, thisNation.getId())) {
                        // Generate new entry
                        final Achievement entry = new Achievement();
                        entry.setUser(owner);
                        entry.setCategory(AchievementConstants.WINNATION);
                        entry.setLevel(thisNation.getId());
                        entry.setAnnounced(false);
                        entry.setFirstLoad(false);
                        entry.setDescription(AchievementConstants.WINNATION_STR[thisNation.getId()]);
                        entry.setVictoryPoints(0);
                        entry.setAchievementPoints(AchievementConstants.WINNATION_AP[thisNation.getId()]);
                        AchievementManager.getInstance().add(entry);

                        // Also check if player managed to win with all nations
                        for (int specialCat = 0; specialCat < lstSpecialCategories.size(); specialCat++) {
                            boolean allNations = true;
                            final List<Nation> lstSpecialNations = lstSpecialCategories.get(specialCat);
                            final int level = lstSpecialCategoriesLevel.get(specialCat);

                            for (Nation nation : lstSpecialNations) {
                                allNations &= AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.WINNATION, nation.getId());
                            }

                            if (allNations) {
                                // Generate new entry
                                final Achievement entryAll = new Achievement();
                                entryAll.setUser(owner);
                                entryAll.setCategory(AchievementConstants.WINNATION);
                                entryAll.setLevel(level);
                                entryAll.setAnnounced(false);
                                entryAll.setFirstLoad(false);
                                entryAll.setDescription(AchievementConstants.WINNATION_STR[level]);
                                entryAll.setVictoryPoints(0);
                                entryAll.setAchievementPoints(AchievementConstants.WINNATION_AP[level]);
                                AchievementManager.getInstance().add(entry);
                            }
                        }
                    }
                    mainTrans.commit();
                }
            }
        }

        if (winningNations.isEmpty()) {
            // No winners found, stop here
            return;
        }

        // If we have at least 1 winner, then we should also check for co-winners
        for (Nation thisNation : activeNations) {
            // ignore winning nations
            if (!winningNations.contains(thisNation)) {
                final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(thisNation,
                        thisGame, thisGame.getTurn(), N_VP);

                if (thisReport != null) {
                    final double targetVpCoWin = thisNation.getVpWin() * modifier * .95d;
                    final int currentVP = Integer.parseInt(thisReport.getValue());
                    if (currentVP >= targetVpCoWin) {
                        LOGGER.info(thisNation.getName() + " is a co-winner !!");
                        coWinningNations.add(thisNation);

                        coWinners.append(thisNation.getName());
                        coWinners.append(", ");

                        coWinnersID.append("*");
                        coWinnersID.append(thisNation.getId());

                        // Modify player's profile
                        changeProfile(thisNation, ProfileConstants.VPS, ProfileConstants.WINNER);

                        // check if the player has played for a full game
                        final User owner = getParent().getGameEngine().getUser(thisNation);
                        final List<UserGame> lstPlayers = UserGameManager.getInstance().list(getParent().getGame(), thisNation);
                        if (lstPlayers.size() == 1) {
                            // Only 1 player, award achievement
                            final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                            if (!AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.PLAYNATION, thisNation.getId())) {
                                // Generate new entry
                                final Achievement entry = new Achievement();
                                entry.setUser(owner);
                                entry.setCategory(AchievementConstants.PLAYNATION);
                                entry.setLevel(thisNation.getId());
                                entry.setAnnounced(false);
                                entry.setFirstLoad(false);
                                entry.setDescription(AchievementConstants.PLAYNATION_STR[thisNation.getId()]);
                                entry.setVictoryPoints(0);
                                entry.setAchievementPoints(AchievementConstants.PLAYNATION_AP);
                                AchievementManager.getInstance().add(entry);
                            }
                            mainTrans.commit();
                        }

                        // Check if the player has already won with this nation
                        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                        if (!AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.WINNATION, thisNation.getId())) {
                            // Generate new entry
                            final Achievement entry = new Achievement();
                            entry.setUser(owner);
                            entry.setCategory(AchievementConstants.WINNATION);
                            entry.setLevel(thisNation.getId());
                            entry.setAnnounced(false);
                            entry.setFirstLoad(false);
                            entry.setDescription(AchievementConstants.WINNATION_STR[thisNation.getId()]);
                            entry.setVictoryPoints(0);
                            entry.setAchievementPoints(AchievementConstants.WINNATION_AP[thisNation.getId()]);
                            AchievementManager.getInstance().add(entry);
                        }
                        mainTrans.commit();
                    }
                }
            }
        }

        // If we have at least 1 winner, then we should also check for runner-ups
        for (Nation thisNation : activeNations) {
            // ignore winning nations
            if (!winningNations.contains(thisNation) && !coWinningNations.contains(thisNation)) {
                final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(thisNation,
                        thisGame, thisGame.getTurn(), N_VP);

                if (thisReport != null) {
                    final double targetVpRunnerUp = thisNation.getVpWin() * modifier * .9d;
                    final int currentVP = Integer.parseInt(thisReport.getValue());
                    if (currentVP >= targetVpRunnerUp) {
                        LOGGER.info(thisNation.getName() + " is a runner-up !");
                        runnerUpNations.add(thisNation);

                        runnerUp.append(thisNation.getName());
                        runnerUp.append(", ");

                        runnerUpID.append("*");
                        runnerUpID.append(thisNation.getId());

                        // Modify player's profile
                        changeProfile(thisNation, ProfileConstants.VPS, ProfileConstants.RUNNERUP);

                        // check if the player has played for a full game
                        final List<UserGame> lstPlayers = UserGameManager.getInstance().list(getParent().getGame(), thisNation);
                        if (lstPlayers.size() == 1) {
                            // Only 1 player, award achievement
                            final User owner = getParent().getGameEngine().getUser(thisNation);
                            final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

                            if (!AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.PLAYNATION, thisNation.getId())) {
                                // Generate new entry
                                final Achievement entry = new Achievement();
                                entry.setUser(owner);
                                entry.setCategory(AchievementConstants.PLAYNATION);
                                entry.setLevel(thisNation.getId());
                                entry.setAnnounced(false);
                                entry.setFirstLoad(false);
                                entry.setDescription(AchievementConstants.PLAYNATION_STR[thisNation.getId()]);
                                entry.setVictoryPoints(0);
                                entry.setAchievementPoints(AchievementConstants.PLAYNATION_AP);
                                AchievementManager.getInstance().add(entry);
                            }
                            mainTrans.commit();
                        }
                    }
                }
            }
        }

        // Add news entries for winning nations
        winners.delete(winners.length() - 2, winners.length() - 1);
        if (winningNations.size() > 1) {
            winners.append(" have ");

        } else {
            winners.append(" has ");
        }
        winners.append(" achieved world domination !");

        newsGlobal(NationManager.getInstance().getByID(NATION_NEUTRAL),
                NewsConstants.NEWS_WORLD, true, winners.toString(), winners.toString());


        // add news entries for co-winners
        if (!coWinningNations.isEmpty()) {
            coWinners.delete(coWinners.length() - 2, coWinners.length() - 1);
            if (coWinningNations.size() > 1) {
                coWinners.append(" are ");
            } else {
                coWinners.append(" is ");
            }
            coWinners.append(" also very close to world domination !");

            newsGlobal(NationManager.getInstance().getByID(NATION_NEUTRAL),
                    NewsConstants.NEWS_WORLD, true, coWinners.toString(), coWinners.toString());

            coWinnersID.append("*");
        }

        // add news entries for runner-up nations
        if (!runnerUpNations.isEmpty()) {
            runnerUp.delete(runnerUp.length() - 2, runnerUp.length() - 1);
            if (runnerUpNations.size() > 1) {
                runnerUp.append(" are runners-up ");
            } else {
                runnerUp.append(" is runner-up ");
            }
            runnerUp.append("for world domination !");

            newsGlobal(NationManager.getInstance().getByID(NATION_NEUTRAL),
                    NewsConstants.NEWS_WORLD, true, runnerUp.toString(), runnerUp.toString());

            runnerUpID.append("*");
        }

        // identify survivors
        for (final Nation nation : activeNations) {
            if (!winningNations.contains(nation) && !coWinningNations.contains(nation) && !runnerUpNations.contains(nation)) {
                // Modify player's profile
                changeProfile(nation, ProfileConstants.VPS, ProfileConstants.SURVIVOR);

                // check if the player has played for a full game
                final List<UserGame> lstPlayers = UserGameManager.getInstance().list(getParent().getGame(), nation);
                if (lstPlayers.size() == 1) {
                    // Only 1 player, award achievement
                    final User owner = getParent().getGameEngine().getUser(nation);
                    final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

                    if (!AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.PLAYNATION, nation.getId())) {
                        // Generate new entry
                        final Achievement entry = new Achievement();
                        entry.setUser(owner);
                        entry.setCategory(AchievementConstants.PLAYNATION);
                        entry.setLevel(nation.getId());
                        entry.setAnnounced(false);
                        entry.setFirstLoad(false);
                        entry.setDescription(AchievementConstants.PLAYNATION_STR[nation.getId()]);
                        entry.setVictoryPoints(0);
                        entry.setAchievementPoints(AchievementConstants.PLAYNATION_AP);
                        AchievementManager.getInstance().add(entry);
                    }
                    mainTrans.commit();
                }
            }
        }

        // Update Game record
        winnersID.append("*");
        thisGame.setWinners(winnersID.toString());
        thisGame.setCoWinners(coWinnersID.toString());
        thisGame.setRunnerUp(runnerUpID.toString());
        thisGame.setEnded(true);
        GameManager.getInstance().update(thisGame);
    }
}