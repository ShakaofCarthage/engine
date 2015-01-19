package com.eaw1805.orders;

import com.eaw1805.data.constants.AchievementConstants;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of an order processor.
 */
public abstract class AbstractOrderProcessor
        implements OrderInterface, OrderConstants, NewsConstants, ReportConstants, VPConstants,
        RelationConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(AbstractOrderProcessor.class);

    /**
     * The object containing the player order.
     */
    private PlayerOrder order;

    /**
     * The parent object.
     */
    private final transient OrderProcessor parent;

    private final transient Game game;

    /**
     * nation relations.
     */
    private static Map<Nation, Map<Nation, NationsRelation>> relationsMap;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public AbstractOrderProcessor(final OrderProcessor myParent) {
        parent = myParent;
        game = myParent.getGame();
        if (relationsMap == null) {
            // Retrieve nation relations
            relationsMap = mapNationRelation(getParent().getGame());
        }
    }

    /**
     * Default constructor.
     *
     * @param myGame the parent object that invoked us.
     */
    public AbstractOrderProcessor(final Game myGame) {
        game = myGame;
        parent = null;
        if (relationsMap == null) {
            // Retrieve nation relations
            relationsMap = mapNationRelation(getParent().getGame());
        }
    }

    /**
     * Returns the order object.
     *
     * @return the order object.
     */
    public final PlayerOrder getOrder() {
        return order;
    }

    /**
     * Sets the particular thisOrder.
     *
     * @param thisOrder the new PlayerOrder instance.
     */
    public final void setOrder(final PlayerOrder thisOrder) {
        this.order = thisOrder;
    }

    /**
     * Τhe parent object that invoked us.
     *
     * @return the parent object that invoked us.
     */
    public OrderProcessor getParent() {
        return parent;
    }

    public Game getGame() {
        return game;
    }

    public void reloadRelations() {
        relationsMap.clear();

        // Retrieve nation relations
        relationsMap = mapNationRelation(getParent().getGame());
    }

    /**
     * Get the Relations from the database that corresponds to the input
     * parameters.
     *
     * @param owner  the Owner of the Report object.
     * @param target the Target of the Report object.
     * @return an Entity object.
     */
    public NationsRelation getByNations(final Nation owner, final Nation target) {
        return relationsMap.get(owner).get(target);
    }

    private Map<Nation, Map<Nation, NationsRelation>> mapNationRelation(final Game thisGame) {
        final Map<Nation, Map<Nation, NationsRelation>> mapRelations = new HashMap<Nation, Map<Nation, NationsRelation>>();
        final List<Nation> lstNations = NationManager.getInstance().list();
        for (final Nation nation : lstNations) {
            final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(thisGame, nation);
            final Map<Nation, NationsRelation> nationRelations = new HashMap<Nation, NationsRelation>();
            for (final NationsRelation relation : lstRelations) {
                nationRelations.put(relation.getTarget(), relation);
            }
            mapRelations.put(nation, nationRelations);
        }

        return mapRelations;
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param isGlobal     if the news entry will appear to public.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected final int news(final Nation nation,
                             final Nation subject,
                             final int type,
                             final boolean isGlobal,
                             final int baseNewsId,
                             final String announcement) {

        return news(game, nation, subject, type, isGlobal, baseNewsId, announcement);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param game         the game of the news entry.
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param isGlobal     if the news entry will appear to public.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected final int news(final Game game,
                             final Nation nation,
                             final Nation subject,
                             final int type,
                             final boolean isGlobal,
                             final int baseNewsId,
                             final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(game);
        thisNewsEntry.setTurn(game.getTurn());
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
     * Add a news entry for this turn.
     *
     * @param nation          the owner of the news entry.
     * @param type            the type of the news entry.
     * @param isGlobal        the entry will appear on the public news.
     * @param announcement    the value of the news entry to appear for the nation.
     * @param announcementAll the value of the news entry to appear for all others.
     */
    protected void newsGlobal(final Nation nation,
                              final int type,
                              final boolean isGlobal,
                              final String announcement,
                              final String announcementAll) {

        final int baseNewsId = news(game, nation, nation, type, false, 0, announcement);

        final List<Nation> lstNations = getParent().getGameEngine().getAliveNations();

        boolean global = isGlobal;
        for (final Nation thirdNation : lstNations) {
            if (thirdNation.getId() == nation.getId()) {
                continue;
            }

            news(game, thirdNation, nation, type, global, baseNewsId, announcementAll);

            global &= false;
        }
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

        final int baseNewsId = news(game, nation, subject, type, false, 0, announcementNation);
        news(game, subject, nation, type, false, baseNewsId, announcementSubject);

        boolean isGlobal = true;
        final List<Nation> lstNations = getParent().getGameEngine().getAliveNations();
        for (final Nation thirdNation : lstNations) {
            if (thirdNation.getId() == nation.getId()
                    || thirdNation.getId() == subject.getId()) {
                continue;
            }

            news(game, thirdNation, nation, type, isGlobal, baseNewsId, announcementAll);

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

        final int baseNewsId = news(game, nation, subject, type, false, 0, announcementNation);
        news(game, subject, nation, type, false, baseNewsId, announcementSubject);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param game               the game of the news entry.
     * @param nation             the owner of the news entry.
     * @param announcementNation the value of the news entry to appear for the nation.
     */
    protected void newsSingle(final Game game,
                              final Nation nation,
                              final int type,
                              final String announcementNation) {

        news(game, nation, nation, type, false, 0, announcementNation);
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

        news(game, nation, nation, type, false, 0, announcementNation);
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation             the owner of the news entry.
     * @param additionalId       an additional identifier specific to this news entry.
     * @param announcementNation the value of the news entry to appear for the nation.
     */
    protected void newsSingle(final Nation nation,
                              final int type,
                              final int additionalId,
                              final String announcementNation) {

        news(game, nation, nation, type, false, additionalId, announcementNation);
    }

    /**
     * Add a report entry for this turn.
     *
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected final void report(final String key, final String value) {

        final Report thisReport = new Report();
        thisReport.setGame(game);
        thisReport.setTurn(game.getTurn());
        thisReport.setNation(order.getNation());
        thisReport.setKey(key);
        thisReport.setValue(value);
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Add a report entry for this turn.
     *
     * @param target the target of the report entry.
     * @param key    the key of the report entry.
     * @param value  the value of the report entry.
     */
    protected final void report(final Nation target, final String key, final String value) {
        final Report thisReport = new Report();
        thisReport.setGame(game);
        thisReport.setTurn(game.getTurn());
        thisReport.setNation(target);
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
    protected final int retrieveReportAsInt(final Nation owner, final int turn, final String key) {
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
    protected final String retrieveReport(final Nation owner, final int turn, final String key) {
        try {
            final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getParent().getGame(), turn, key);
            if (thisReport == null) {
                return "";
            }
            return thisReport.getValue();

        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Increase/Decrease the VPs of a nation.
     *
     * @param game        the game instance.
     * @param owner       the Nation to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    protected final void changeVP(final Game game,
                                  final Nation owner,
                                  final int increase,
                                  final String description) {
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
            news(game, owner, owner, NEWS_VP, false, increase, description);

            // Modify player's profile
            changeProfile(owner, ProfileConstants.VPS, increase);

            // Report VP addition in player's achievements list
            parent.achievement(game, owner, AchievementConstants.VPS, AchievementConstants.LEVEL_1, description, 0, increase);
        }
    }


    /**
     * Increase/Decrease a profile attribute for the player of the nation.
     *
     * @param owner    the Nation to change the profile of the player.
     * @param key      the profile key of the player.
     * @param increase the increase or decrease in the profile entry.
     */
    public final void changeProfile(final Nation owner, final String key, final int increase) {
        getParent().changeProfile(getParent().getGame(), owner, key, increase);
    }

    /**
     * Check if in this sector there exist foreign+enemy brigades.
     *
     * @param thisSector the sector to check.
     * @return true if enemy brigades are present.
     */
    protected final boolean enemyNotPresent(final Sector thisSector) {
        boolean notFound = true;

        // Trade cities and large and huge fortresses are exempt from this rule.
        if (thisSector.getProductionSite() != null
                && thisSector.getProductionSite().getId() >= ProductionSiteConstants.PS_BARRACKS_FL) {
            return true;
        }

        final TradeCity city = TradeCityManager.getInstance().getByPosition(thisSector.getPosition());
        if (city != null) {
            return true;
        }

        // Retrieve all brigades at the particular Sector
        final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByPosition(thisSector.getPosition());
        for (final Brigade thisBrigade : lstBrigades) {
            // check owner
            if (thisBrigade.getNation().getId() == thisSector.getNation().getId()) {
                continue;
            }

            // Retrieve relations with foreign nation
            final NationsRelation relation = RelationsManager.getInstance().getByNations(thisSector.getPosition().getGame(),
                    thisSector.getNation(),
                    thisBrigade.getNation());

            // Check relations
            if (relation.getRelation() == REL_WAR
                    || (relation.getRelation() == REL_COLONIAL_WAR && thisSector.getPosition().getRegion().getId() != RegionConstants.EUROPE)) {
                return false;
            }
        }

        return notFound;
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
     * Remove commander from army.
     *
     * @param armyId the army ID.
     */
    protected void removeFromArmy(final int armyId, final int commanderId) {
        // Retrieve army
        final Army thisArmy = ArmyManager.getInstance().getByID(armyId);

        if (thisArmy != null) {
            //check that the commander in the army is the same (it could be changed previously by an assign order
            if (thisArmy.getCommander() != null && thisArmy.getCommander().getId() != commanderId) {
                return;
            }

            // remove commander
            thisArmy.setCommander(null);

            // update entity
            ArmyManager.getInstance().update(thisArmy);
        }
    }

    /**
     * Remove commander from corps.
     *
     * @param corpId the corps id.
     */
    protected void removeFromCorp(final int corpId, final int commanderId) {
        // Retrieve corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp != null) {
            //check that the commander in the corps is the same (it could be changed previously by an assign order
            if (thisCorp.getCommander() != null && thisCorp.getCommander().getId() != commanderId) {
                return;
            }

            // remove commander
            thisCorp.setCommander(null);

            // update entity
            CorpManager.getInstance().update(thisCorp);
        }
    }

    /**
     * Remove commander from army or corps.
     *
     * @param thisComm the commander object.
     */
    protected void removeCommander(final Commander thisComm) {
        if (thisComm.getArmy() != 0) {
            removeFromArmy(thisComm.getArmy(), thisComm.getId());

            thisComm.setArmy(0);
        }

        if (thisComm.getCorp() != 0) {
            removeFromCorp(thisComm.getCorp(), thisComm.getId());

            thisComm.setCorp(0);
        }
        //be sure to update commander.
        CommanderManager.getInstance().update(thisComm);
    }

    public void destroyLoadedUnits(final Ship thisShip, final boolean isInLand) {
        // Check if a unit is loaded in the ship
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.SPY * 1000) {
                    // A spy is loaded
                    final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());

                    // Report capture of spy.
                    if (isInLand) {
                        newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy '" + thisSpy.getName() + "' was unloaded as the ship '" + thisShip.getName() + "' was scuttled by its crew");
                        LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was unloaded when ship '" + thisShip.getName() + "' was scuttled by its crew");

                        // remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        thisSpy.setCarrierInfo(thisCarrying);

                        SpyManager.getInstance().update(thisSpy);

                    } else {
                        newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy '" + thisSpy.getName() + "' was drown when the ship '" + thisShip.getName() + "' was scuttled by its crew");
                        LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was drown when ship '" + thisShip.getName() + "' was scuttled by its crew");

                        // Remove spy from game
                        SpyManager.getInstance().delete(thisSpy);
                    }

                } else if (entry.getKey() >= ArmyConstants.COMMANDER * 1000) {
                    // A commander is loaded
                    final Commander thisCommander = CommanderManager.getInstance().getByID(entry.getValue());

                    // Report capture of commander.
                    if (isInLand) {
                        newsSingle(thisCommander.getNation(), NEWS_POLITICAL, "Our commander '" + thisCommander.getName() + "' was unloaded as the ship '" + thisShip.getName() + "' was scuttled by its crew");
                        LOGGER.info("Commander [" + thisCommander.getName() + "] of Nation [" + thisCommander.getNation().getName() + "] was unloaded at " + thisShip.getPosition().toString() + " when ship '" + thisShip.getName() + "' was scuttled by its crew");

                    } else {
                        newsSingle(thisCommander.getNation(), NEWS_POLITICAL, "Our commander '" + thisCommander.getName() + "' was drown when the ship '" + thisShip.getName() + "' sunk");
                        LOGGER.info("Commander [" + thisCommander.getName() + "] of Nation [" + thisCommander.getNation().getName() + "] was drown when ship '" + thisShip.getName() + "' sunk");

                        // remove commander from command
                        removeCommander(thisCommander);

                        // remove commander from game
                        thisCommander.setDead(true);
                    }

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    thisCommander.setCarrierInfo(thisCarrying);

                    CommanderManager.getInstance().update(thisCommander);

                } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                    // A Brigade is loaded
                    final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());

                    // Report capture of spy.
                    if (isInLand) {
                        newsSingle(thisBrigade.getNation(), NEWS_POLITICAL, "Our brigade '" + thisBrigade.getName() + "' was unloaded as the ship '" + thisShip.getName() + "' was scuttled by its crew.");
                        LOGGER.info("Brigade [" + thisBrigade.getName() + "] of Nation [" + thisBrigade.getNation().getName() + "] was unloaded as the ship '" + thisShip.getName() + "' was scuttled by its crew.");

                        for (final Battalion battalion : thisBrigade.getBattalions()) {

                            // remove carrier info
                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(0);
                            thisCarrying.setCarrierId(0);

                            battalion.setCarrierInfo(thisCarrying);
                            BattalionManager.getInstance().update(battalion);
                        }

                        BrigadeManager.getInstance().update(thisBrigade);

                    } else {
                        newsSingle(thisBrigade.getNation(), NEWS_POLITICAL, "Our brigade '" + thisBrigade.getName() + "' was disbanded when the ship '" + thisShip.getName() + "' was scuttled by its crew");
                        LOGGER.info("Brigade [" + thisBrigade.getName() + "] of Nation [" + thisBrigade.getNation().getName() + "] was disbanded when ship '" + thisShip.getName() + "' was scuttled by its crew");

                        // Remove brigade from game
                        BrigadeManager.getInstance().delete(thisBrigade);
                    }
                }
            }
        }
    }

    public void destroyLoadedUnits(final BaggageTrain thisTrain) {
        // Check if a unit is loaded in the ship
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.SPY * 1000) {
                    // A spy is loaded
                    final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());

                    // Report capture of spy.
                    newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy '" + thisSpy.getName() + "' was unloaded as the baggage train '" + thisTrain.getName() + "' was scuttled by its crew");
                    LOGGER.info("Spy [" + thisSpy.getName() + "] of Nation [" + thisSpy.getNation().getName() + "] was unloaded when the baggage train '" + thisTrain.getName() + "' was scuttled by its crew");

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    thisSpy.setCarrierInfo(thisCarrying);

                    SpyManager.getInstance().update(thisSpy);

                } else if (entry.getKey() >= ArmyConstants.COMMANDER * 1000) {
                    // A commander is loaded
                    final Commander thisCommander = CommanderManager.getInstance().getByID(entry.getValue());

                    // Report capture of commander.
                    newsSingle(thisCommander.getNation(), NEWS_POLITICAL, "Our commander '" + thisCommander.getName() + "' was unloaded as the the baggage train '" + thisTrain.getName() + "' was scuttled by its crew");
                    LOGGER.info("Commander [" + thisCommander.getName() + "] of Nation [" + thisCommander.getNation().getName() + "] was unloaded at " + thisTrain.getPosition().toString() + " when the the baggage train '" + thisTrain.getName() + "' was scuttled by its crew");

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    thisCommander.setCarrierInfo(thisCarrying);

                    CommanderManager.getInstance().update(thisCommander);

                } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                    // A Brigade is loaded
                    final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());

                    // Report capture of spy.
                    newsSingle(thisBrigade.getNation(), NEWS_POLITICAL, "Our brigade '" + thisBrigade.getName() + "' was unloaded as the the baggage train '" + thisTrain.getName() + "' was scuttled by its crew.");
                    LOGGER.info("Brigade [" + thisBrigade.getName() + "] of Nation [" + thisBrigade.getNation().getName() + "] was unloaded as the the baggage train '" + thisTrain.getName() + "' was scuttled by its crew.");

                    for (final Battalion battalion : thisBrigade.getBattalions()) {

                        // remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);

                        battalion.setCarrierInfo(thisCarrying);
                        BattalionManager.getInstance().update(battalion);
                    }

                    BrigadeManager.getInstance().update(thisBrigade);
                }
            }
        }
    }

    public Game getMyGame() {
        return game;
    }

}
