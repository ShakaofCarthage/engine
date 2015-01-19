package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.AchievementConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.MaintenanceConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.AchievementManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.ProfileManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Achievement;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.Profile;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Provides basic utilities for the economy processors.
 */
public class AbstractMaintenance
        implements GoodConstants, RegionConstants, NationConstants, MaintenanceConstants, ReportConstants, NewsConstants, VPConstants {

    /**
     * The game processed.
     */
    private final transient GameEngine gameEngine;

    /**
     * Warehouse data.
     */
    private transient int[][][] totGoods;

    /**
     * Production data.
     */
    private transient int[][][] producedGoods;

    /**
     * List of nations.
     */
    private transient List<Nation> lstNations;

    /**
     * List of regions.
     */
    private transient List<Region> lstRegions;

    private transient StringBuilder strRandomFactor;

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public AbstractMaintenance(final GameEngine caller) {
        gameEngine = caller;
        strRandomFactor = new StringBuilder();
        loadData();
    }

    /**
     * Get the game engine.
     *
     * @return the game engine.
     */
    public GameEngine getGameEngine() {
        return gameEngine;
    }

    /**
     * Get the game object.
     *
     * @return the game object.
     */
    public final Game getGame() {
        return gameEngine.getGame();
    }

    /**
     * Get the random number generator.
     *
     * @return the random number generator.
     */
    public final Random getRandomGen() {
        return gameEngine.getRandomGen();
    }

    /**
     * Get the available quantities per region and type for specific nation.
     *
     * @param nationId the nation.
     * @return the available quantities per region and type.
     */
    public final int[][] getTotGoods(final int nationId) {
        return totGoods[nationId];
    }

    /**
     * Get the available quantities per nation and region.
     *
     * @param nationId the nation.
     * @param regionId the region.
     * @param goodType the good type.
     * @return the available quantities per nation and region.
     */
    public final int getTotGoods(final int nationId, final int regionId, final int goodType) {
        return totGoods[nationId][regionId][goodType];
    }

    /**
     * Set the available quantities per nation and region.
     *
     * @param nationId the nation.
     * @param regionId the region.
     * @param goodType the good type.
     * @param value    the new quantity.
     */
    public final void setTotGoods(final int nationId, final int regionId, final int goodType, final int value) {
        totGoods[nationId][regionId][goodType] = value;
    }

    /**
     * Increase the available quantities per nation and region.
     *
     * @param nationId the nation.
     * @param regionId the region.
     * @param goodType the good type.
     * @param value    the new quantity.
     */
    public final void incTotGoods(final int nationId, final int regionId, final int goodType, final int value) {
        totGoods[nationId][regionId][goodType] += value;
    }

    /**
     * Decrease the available quantities per nation and region.
     *
     * @param nationId the nation.
     * @param regionId the region.
     * @param goodType the good type.
     * @param value    the new quantity.
     */
    public final void decTotGoods(final int nationId, final int regionId, final int goodType, final int value) {
        totGoods[nationId][regionId][goodType] -= value;
    }

    /**
     * Get the quantities produced per region and type for specific nation.
     *
     * @param nationId the nation.
     * @return the quantities produced per region and type.
     */
    public final int[][] getProdGoods(final int nationId) {
        return producedGoods[nationId];
    }

    /**
     * Increase the produced quantities per nation and region.
     *
     * @param nationId the nation.
     * @param regionId the region.
     * @param goodType the good type.
     * @param value    the new quantity.
     */
    public final void incProdGoods(final int nationId, final int regionId, final int goodType, final int value) {
        totGoods[nationId][regionId][goodType] += value;
        producedGoods[nationId][regionId][goodType] += value;
    }

    /**
     * Decrease the quantities produced per nation and region.
     *
     * @param nationId the nation.
     * @param regionId the region.
     * @param goodType the good type.
     * @param value    the new quantity.
     */
    public final void decProdGoods(final int nationId, final int regionId, final int goodType, final int value) {
        totGoods[nationId][regionId][goodType] -= value;
        producedGoods[nationId][regionId][goodType] -= value;
    }

    /**
     * Get the list of nations.
     *
     * @return the list of nations.
     */
    public final List<Nation> getLstNations() {
        return lstNations;
    }

    /**
     * Get the list of regions.
     *
     * @return the list of regions.
     */
    public final List<Region> getLstRegions() {
        return lstRegions;
    }

    /**
     * Load warehouse data.
     */
    protected final void loadData() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());

        switch(getGame().getScenarioId()) {
            case HibernateUtil.DB_FREE:
                lstNations = new ArrayList<Nation>();
                lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                break;

            case HibernateUtil.DB_S3:
            case HibernateUtil.DB_S2:
            case HibernateUtil.DB_S1:
            default:
                lstNations = NationManager.getInstance().list();
                break;
        }

        lstRegions = RegionManager.getInstance().list();
        totGoods = new int[NATION_LAST + 1][REGION_LAST + 1][GOOD_CP + 1];
        producedGoods = new int[NATION_LAST + 1][REGION_LAST + 1][GOOD_CP + 1];
        final List<Warehouse> listWH = WarehouseManager.getInstance().listByGame(getGame());
        for (final Warehouse thisWH : listWH) {
            for (Integer key : thisWH.getStoredGoodsQnt().keySet()) {
                totGoods[thisWH.getNation().getId()][thisWH.getRegion().getId()][key] = thisWH.getStoredGoodsQnt().get(key);
                producedGoods[thisWH.getNation().getId()][thisWH.getRegion().getId()][key] = 0;
            }
        }
        theTrans.commit();
    }

    /**
     * Save warehouse data.
     */
    protected final void saveData() {
        // Update Warehouses
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Warehouse> listWH = WarehouseManager.getInstance().listByGame(getGame());
        for (final Warehouse thisWH : listWH) {
            for (Integer key : thisWH.getStoredGoodsQnt().keySet()) {
                // Update statistics
                thisWH.getStoredGoodsQnt().put(key, totGoods[thisWH.getNation().getId()][thisWH.getRegion().getId()][key]);
            }
            WarehouseManager.getInstance().update(thisWH);
        }
        theTrans.commit();
    }


    /**
     * Add a news entry for this turn.
     *
     * @param nation              the owner of the news entry.
     * @param subject             the subject of the news entry.
     * @param announcementNation  the value of the news entry to appear for the nation.
     * @param announcementSubject the value of the news entry to appear for the subject.
     * @param announcementAll     the value of the news entry to appear for all others.
     */
    protected void newsGlobal(final Nation nation,
                              final Nation subject,
                              final int type,
                              final String announcementNation,
                              final String announcementSubject,
                              final String announcementAll) {

        final int baseNewsId = news(nation, subject, type, false, 0, announcementNation);
        news(subject, nation, type, false, baseNewsId, announcementSubject);

        boolean isGlobal = true;
        final List<Nation> lstNations = getGameEngine().getAliveNations();
        for (final Nation thirdNation : lstNations) {
            if (thirdNation.getId() == nation.getId()
                    || thirdNation.getId() == subject.getId()) {
                continue;
            }

            news(thirdNation, nation, type, isGlobal, baseNewsId, announcementAll);

            isGlobal &= false;
        }
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation          the owner of the news entry.
     * @param announcement    the value of the news entry to appear for the nation.
     * @param announcementAll the value of the news entry to appear for all others.
     */
    protected void newsGlobal(final Nation nation,
                              final int type,
                              final String announcement,
                              final String announcementAll) {

        final int baseNewsId = news(nation, nation, type, false, 0, announcement);

        final List<Nation> lstNations = getGameEngine().getAliveNations();

        boolean isGlobal = true;
        for (final Nation thirdNation : lstNations) {
            if (thirdNation.getId() == nation.getId()) {
                continue;
            }

            news(thirdNation, nation, type, isGlobal, baseNewsId, announcementAll);

            isGlobal &= false;
        }
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation          the owner of the news entry.
     * @param announcement    the value of the news entry to appear for the nation.
     * @param announcementAll the value of the news entry to appear for all others.
     */
    public void newsGlobal(final Nation nation,
                           final int type,
                           final int addId,
                           final String announcement,
                           final String announcementAll) {

        final int baseNewsId = news(nation, nation, type, false, addId, announcement);

        final List<Nation> lstNations = getGameEngine().getAliveNations();

        boolean isGlobal = true;
        for (final Nation thirdNation : lstNations) {
            if (thirdNation.getId() == nation.getId()) {
                continue;
            }

            news(thirdNation, nation, type, isGlobal, addId, announcementAll);

            isGlobal &= false;
        }
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation              the owner of the news entry.
     * @param subject             the subject of the news entry.
     * @param announcementNation  the value of the news entry to appear for the nation.
     * @param announcementSubject the value of the news entry to appear for the subject.
     */
    protected void newsPair(final Nation nation,
                            final Nation subject,
                            final int type,
                            final String announcementNation,
                            final String announcementSubject) {

        final int baseNewsId = news(nation, subject, type, false, 0, announcementNation);
        news(subject, nation, type, false, baseNewsId, announcementSubject);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation             the owner of the news entry.
     * @param announcementNation the value of the news entry to appear for the nation.
     */
    protected void newsSingle(final Nation nation,
                              final int type,
                              final String announcementNation) {

        news(nation, nation, type, false, 0, announcementNation);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation             the owner of the news entry.
     * @param announcementNation the value of the news entry to appear for the nation.
     */
    protected void newsSingle(final Nation nation,
                              final int type,
                              final int addId,
                              final String announcementNation) {

        news(nation, nation, type, false, addId, announcementNation);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Nation nation, final Nation subject, final int type, final boolean isGlobal, final int baseNewsId, final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(getGame());
        thisNewsEntry.setTurn(getGame().getTurn());
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setGlobal(isGlobal);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
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

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final String key, final String value) {
        final Report thisReport = new Report();
        thisReport.setGame(getGame());
        thisReport.setTurn(getGame().getTurn());
        thisReport.setNation(owner);
        thisReport.setKey(key);
        thisReport.setValue(value);
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected int retrieveReportAsInt(final Nation owner, final int turn, final String key) {
        final String value = retrieveReport(owner, turn, key);

        // Check if string is empty
        if (value.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected String retrieveReport(final Nation owner, final int turn, final String key) {
        try {
            final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getGame(), turn, key);
            if (thisReport == null) {
                return "";
            }
            return thisReport.getValue();

        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Add a news entry for this turn.
     *
     * @param game         the game of the news entry.
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Game game, final Nation nation, final Nation subject, final int type, final int baseNewsId, final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(game);
        thisNewsEntry.setTurn(game.getTurn());
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
    }

    /**
     * Add an achievement entry for the particular player.
     *
     * @param game        the game related to the achievement.
     * @param owner       the nation related to the achievement.
     * @param category    the achievement category.
     * @param level       the achievement level.
     * @param description the description of the achievement.
     * @param vp          the vps related to the achievement.
     * @param ap          the achievement points related to the achievement.
     */
    public void achievement(final Game game, final Nation owner,
                            final int category, final int level,
                            final String description,
                            final int vp, final int ap) {
        // Ignore Free Scenario
        if (game.getGameId() < 0 || game.getScenarioId() <= HibernateUtil.DB_MAIN || owner.getId() <= 0) {
            return;
        }

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve user for particular nation
        User user = getGameEngine().getUser(owner);

        if (user == null) {
            // Check this
            user = UserManager.getInstance().getByID(2);
        }

        // Generate new entry
        final Achievement entry = new Achievement();
        entry.setUser(user);
        entry.setCategory(category);
        entry.setLevel(level);
        entry.setAnnounced(false);
        entry.setFirstLoad(false);
        entry.setDescription(description);
        entry.setVictoryPoints(vp);
        entry.setAchievementPoints(ap);

        AchievementManager.getInstance().add(entry);

        mainTrans.commit();
    }

    /**
     * Increase/Decrease the VPs of a nation.
     *
     * @param game        the game instance.
     * @param owner       the Nation to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    public final void changeVP(final Game game, final Nation owner,
                               final int increase, final String description) {
        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner,
                game, game.getTurn(), N_VP);

        if (thisReport != null) {
            final int currentVP = Integer.parseInt(thisReport.getValue());

            // Make sure we do not end up with negative VP
            if (currentVP + increase > 0) {
                thisReport.setValue(Integer.toString(currentVP + increase));

            } else {
                thisReport.setValue("0");
            }

            ReportManager.getInstance().update(thisReport);

            // Report addition
            news(game, owner, owner, NEWS_VP, increase, description);

            // Modify player's profile
            changeProfile(game, owner, ProfileConstants.VPS, increase);

            // Report VP addition in player's achievements list
            achievement(game, owner, AchievementConstants.VPS, AchievementConstants.LEVEL_1, description, 0, increase);
        }
    }

    /**
     * Increase/Decrease the VPs of a side of nation.
     *
     * @param game        the game instance.
     * @param nations     the set of Nations to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    protected final void changeVP(final Game game, final Set<Nation> nations,
                                  final int increase, final String description) {
        for (final Nation nation : nations) {
            changeVP(game, nation, increase, description);
        }
    }

    /**
     * Get the string builder appending the random rolls for estates.
     *
     * @return the string builder.
     */
    public StringBuilder getStrRandomFactor() {
        return strRandomFactor;
    }

    /**
     * Increase/Decrease a profile attribute for the player of the nation.
     *
     * @param game     the game instance.
     * @param nations  the set of Nation to change the profile of the corresponding players.
     * @param key      the profile key of the player.
     * @param increase the increase or decrease in the profile entry.
     */
    public final void changeProfile(final Game game, final Set<Nation> nations, final String key, final int increase) {
        for (final Nation nation : nations) {
            changeProfile(game, nation, key, increase);
        }
    }

    /**
     * Increase/Decrease a profile attribute for the user.
     *
     * @param user     the user instance.
     * @param key      the profile key of the player.
     * @param increase the increase or decrease in the profile entry.
     */
    public final void changeProfile(final User user, final String key, final int increase) {
        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve profile entry
        final Profile entry = ProfileManager.getInstance().getByOwnerKey(user, key);

        // If the entry does not exist, create
        if (entry == null) {
            final Profile newEntry = new Profile();
            newEntry.setUser(user);
            newEntry.setKey(key);

            // Make sure players do not end up with negative VPs
            if (!key.equalsIgnoreCase(ProfileConstants.VPS) || increase > 0) {
                newEntry.setValue(increase);
            } else {
                newEntry.setValue(0);
            }

            ProfileManager.getInstance().add(newEntry);

        } else {

            // Make sure players do not end up with negative VPs
            if (!key.equalsIgnoreCase(ProfileConstants.VPS) || entry.getValue() + increase > 0) {
                entry.setValue(entry.getValue() + increase);

            } else {
                entry.setValue(0);
            }

            ProfileManager.getInstance().update(entry);
        }

        mainTrans.commit();
    }

    /**
     * Increase/Decrease a profile attribute for the player of the nation.
     *
     * @param game     the game instance.
     * @param owner    the Nation to change the profile of the player.
     * @param key      the profile key of the player.
     * @param increase the increase or decrease in the profile entry.
     */
    public final void changeProfile(final Game game, final Nation owner, final String key, final int increase) {
        // Ignore Free Scenario
        if (game.getGameId() < 0 || game.getScenarioId() <= HibernateUtil.DB_MAIN || owner.getId() <= 0) {
            return;
        }

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve user for particular nation
        User user = getGameEngine().getUser(owner);

        if (user == null) {
            // Check this
            user = UserManager.getInstance().getByID(2);
        }

        // Retrieve profile entry
        final Profile entry = ProfileManager.getInstance().getByOwnerKey(user, key);

        // If the entry does not exist, create
        if (entry == null) {
            final Profile newEntry = new Profile();
            newEntry.setUser(user);
            newEntry.setKey(key);

            // Make sure players do not end up with negative VPs
            if (!key.equalsIgnoreCase(ProfileConstants.VPS) || increase > 0) {
                newEntry.setValue(increase);
            } else {
                newEntry.setValue(0);
            }

            ProfileManager.getInstance().add(newEntry);

        } else {

            // Make sure players do not end up with negative VPs
            if (!key.equalsIgnoreCase(ProfileConstants.VPS) || entry.getValue() + increase > 0) {
                entry.setValue(entry.getValue() + increase);

            } else {
                entry.setValue(0);
            }

            ProfileManager.getInstance().update(entry);
        }

        mainTrans.commit();
    }

    /**
     * Set the maximum to a profile attribute for the player of the nation.
     *
     * @param game  the game instance.
     * @param owner the Nation to change the profile of the player.
     * @param key   the profile key of the player.
     * @param value the max value candidate for the profile entry.
     */
    public final void maxProfile(final Game game, final Nation owner, final String key, final int value) {
        // Ignore Free Scenario
        if (game.getScenarioId() <= HibernateUtil.DB_MAIN) {
            return;
        }

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve user for particular nation
        User user = getGameEngine().getUser(owner);

        if (user == null) {
            // Check this
            user = UserManager.getInstance().getByID(2);
        }

        // Retrieve profile entry
        final Profile entry = ProfileManager.getInstance().getByOwnerKey(user, key);

        // If the entry does not exist, create
        if (entry == null) {
            final Profile newEntry = new Profile();
            newEntry.setUser(user);
            newEntry.setKey(key);
            newEntry.setValue(value);
            ProfileManager.getInstance().add(newEntry);

        } else {
            entry.setValue(Math.max(entry.getValue(), value));
            ProfileManager.getInstance().update(entry);
        }

        mainTrans.commit();
    }

    /**
     * Get the player of the nation.
     *
     * @param game  the game instance.
     * @param owner the Nation to change the profile of the player.
     * @return the User assigned to this nation.
     */
    public final User getUser(final Game game, final Nation owner) {
        // Ignore Free Scenario
        if (game.getScenarioId() <= HibernateUtil.DB_MAIN) {
            return null;
        }

        // Retrieve user for particular nation
        final List<UserGame> lstEntries = UserGameManager.getInstance().list(game, owner);
        User user;

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        if (lstEntries.isEmpty()) {
            // retrieve admin
            user = UserManager.getInstance().getByID(2);

        } else {
            // retrieve user
            user = UserManager.getInstance().getByID(lstEntries.get(0).getUserId());

            if (user == null) {
                // Check this
                user = UserManager.getInstance().getByID(2);
            }
        }

        mainTrans.commit();

        return user;
    }

    /**
     * Get the profile attribute for the player of the nation.
     *
     * @param game the game instance.
     * @param user the Player to change the profile.
     * @param key  the profile key of the player.
     * @return the value for the profile entry.
     */
    public final int getProfile(final Game game, final User user, final String key) {
        // Ignore Free Scenario
        if (game.getScenarioId() <= HibernateUtil.DB_MAIN) {
            return 0;
        }

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve profile entry
        final Profile entry = ProfileManager.getInstance().getByOwnerKey(user, key);

        mainTrans.commit();

        // If the entry does not exist, create
        if (entry == null) {
            return 0;

        } else {
            return entry.getValue();
        }
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsNavalBattles(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.BATTLES_NAVAL_WON);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.NAVAL_L_MIN; level <= AchievementConstants.NAVAL_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.NAVAL_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.NAVAL, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.NAVAL);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.NAVAL_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.NAVAL_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsWar(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.WARS_DECLARED);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.WAR_L_MIN; level <= AchievementConstants.WAR_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.WAR_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.WAR, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.WAR);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.WAR_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.WAR_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsAlliance(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.ALLIANCES_MADE);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.ALLIANCE_L_MIN; level <= AchievementConstants.ALLIANCE_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.ALLIANCE_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.ALLIANCE, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.ALLIANCE);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.ALLIANCE_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.ALLIANCE_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsSurrenders(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.SURRENDERS_ACCEPTED);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.SURRENDERS_L_MIN; level <= AchievementConstants.SURRENDERS_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.SURRENDERS_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.SURRENDERS, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.SURRENDERS);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.SURRENDERS_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.SURRENDERS_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsAcceptCall(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.RESPOND_CALLALLIES);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.CALLACCEPT_L_MIN; level <= AchievementConstants.CALLACCEPT_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.CALLACCEPT_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.CALLACCEPT, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.CALLACCEPT);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.CALLACCEPT_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.CALLACCEPT_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsRejectCall(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.REFUSE_CALLALLIES);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.CALLREJECT_L_MIN; level <= AchievementConstants.CALLREJECT_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.CALLREJECT_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.CALLREJECT, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.CALLREJECT);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.CALLREJECT_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.CALLREJECT_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsBuildHuge(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totWonBattles = getProfile(game, owner, ProfileConstants.FORTRESS_BUILT);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.BUILDHUGE_L_MIN; level <= AchievementConstants.BUILDHUGE_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.BUILDHUGE_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.BUILDHUGE, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.BUILDHUGE);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.BUILDHUGE_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.BUILDHUGE_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsConquerHuge(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totFortresses = getProfile(game, owner, ProfileConstants.FORTRESS_CONQUERED);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.CONQUERHUGE_L_MIN; level <= AchievementConstants.CONQUERHUGE_L_MAX; level++) {
            if (totFortresses >= AchievementConstants.CONQUERHUGE_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.CONQUERHUGE, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.CONQUERHUGE);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.CONQUERHUGE_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.CONQUERHUGE_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsSetupColonies(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totColonies = getProfile(game, owner, ProfileConstants.STARTUP_COLONY);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.SETUPCOLONIES_L_MIN; level <= AchievementConstants.SETUPCOLONIES_L_MAX; level++) {
            if (totColonies >= AchievementConstants.SETUPCOLONIES_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.SETUPCOLONIES, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.SETUPCOLONIES);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.SETUPCOLONIES_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.SETUPCOLONIES_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsConquerCapitals(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totCapitals = getProfile(game, owner, ProfileConstants.CAPITAL_CONQUERED);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.CONQUERCAPITALS_L_MIN; level <= AchievementConstants.CONQUERCAPITALS_L_MAX; level++) {
            if (totCapitals >= AchievementConstants.CONQUERCAPITALS_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.CONQUERCAPITALS, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.CONQUERCAPITALS);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.CONQUERCAPITALS_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.CONQUERCAPITALS_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsTroopsKilled(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totTroopsKilled = getProfile(game, owner, ProfileConstants.ENEMY_KILLED_ALL);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.TROOPKILLS_L_MIN; level <= AchievementConstants.TROOPKILLS_L_MAX; level++) {
            if (totTroopsKilled >= AchievementConstants.TROOPKILLS_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.TROOPKILLS, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.TROOPKILLS);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.TROOPKILLS_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.TROOPKILLS_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsEnemyShips(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        int totSunk = getProfile(game, owner, ProfileConstants.ENEMY_SUNK_1);
        totSunk += getProfile(game, owner, ProfileConstants.ENEMY_SUNK_2);
        totSunk += getProfile(game, owner, ProfileConstants.ENEMY_SUNK_3);
        totSunk += getProfile(game, owner, ProfileConstants.ENEMY_SUNK_4);
        totSunk += getProfile(game, owner, ProfileConstants.ENEMY_SUNK_5);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.ENEMYMERCHANTS_L_MIN; level <= AchievementConstants.ENEMYMERCHANTS_L_MAX; level++) {
            if (totSunk >= AchievementConstants.ENEMYMERCHANTS_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.ENEMYMERCHANTS, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.ENEMYMERCHANTS);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.ENEMYMERCHANTS_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.ENEMYMERCHANTS_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsEnemyMerchants(final Game game, final Nation nation) {
        final User owner = getUser(game, nation);
        final int totSunk = getProfile(game, owner, ProfileConstants.ENEMY_SUNK_0);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.ENEMYMERCHANTS_L_MIN; level <= AchievementConstants.ENEMYMERCHANTS_L_MAX; level++) {
            if (totSunk >= AchievementConstants.ENEMYMERCHANTS_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.ENEMYMERCHANTS, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.ENEMYMERCHANTS);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.ENEMYMERCHANTS_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.ENEMYMERCHANTS_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Identify if sector is a home region, inside sphere of influence, or outside of the receiving nation.
     *
     * @param sector   the sector to examine.
     * @param receiver the receiving nation.
     * @return 1 if home region, 2 if in sphere of influence, 3 if outside.
     */
    protected final int getSphere(final Sector sector, final Nation receiver) {
        final char thisNationCodeLower = String.valueOf(receiver.getCode()).toLowerCase().charAt(0);
        final char thisSectorCodeLower = String.valueOf(sector.getPoliticalSphere()).toLowerCase().charAt(0);
        int sphere = 1;

        // Τα χ2 και x3 ισχύουν μόνο για τα Ευρωπαικά αν χτίζονται στο SoI ή εκτός SoI
        if (sector.getPosition().getRegion().getId() != RegionConstants.EUROPE) {
            return 1;
        }

        // Check if this is not home region
        if (thisNationCodeLower != thisSectorCodeLower) {
            sphere = 2;

            // Check if this is outside sphere of influence
            if (receiver.getSphereOfInfluence().toLowerCase().indexOf(thisSectorCodeLower) < 0) {
                sphere = 3;
            }
        }

        return sphere;
    }

    /**
     * Strip Army from any commander in charge.
     *
     * @param armyId the id of the unit.
     */
    protected void removeFromArmy(final int armyId) {
        // Retrieve army
        final Army thisArmy = ArmyManager.getInstance().getByID(armyId);

        if (thisArmy != null) {

            // remove commander
            thisArmy.setCommander(null);

            // update entity
            ArmyManager.getInstance().update(thisArmy);
        }
    }

    /**
     * Strip Corps from any commander in charge.
     *
     * @param corpId the id of the unit.
     */
    protected void removeFromCorp(final int corpId) {
        // Retrieve corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp != null) {

            // remove commander
            thisCorp.setCommander(null);

            // update entity
            CorpManager.getInstance().update(thisCorp);
        }
    }

    /**
     * Strip commander from any army/corps he is in charge
     *
     * @param thisComm the commander.
     */
    protected void removeCommander(final Commander thisComm) {
        if (thisComm.getArmy() != 0) {
            removeFromArmy(thisComm.getArmy());
            thisComm.setArmy(0);
        }

        if (thisComm.getCorp() != 0) {
            removeFromCorp(thisComm.getCorp());
            thisComm.setCorp(0);
        }
        //be sure to update commander.
        CommanderManager.getInstance().update(thisComm);
    }

}
