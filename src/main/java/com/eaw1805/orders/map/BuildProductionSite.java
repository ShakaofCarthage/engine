package com.eaw1805.orders.map;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.AchievementConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.AchievementManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.ProductionSiteManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Achievement;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.ProductionSite;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the Build Production Site order.
 * ticket #17.
 */
public class BuildProductionSite
        extends AbstractOrderProcessor
        implements OrderInterface, GoodConstants, ProductionSiteConstants, RegionConstants, NaturalResourcesConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BuildProductionSite.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_B_PRODS;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BuildProductionSite(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("BuildProductionSite instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        // retrieve sector
        final int sectorID = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the sector that we wish to increase the population density
        final Sector thisSector = SectorManager.getInstance().getByID(sectorID);

        if (thisSector == null) {
            getOrder().setResult(-10);
            getOrder().setExplanation("cannot find sector");
            return;
        }

        // Update order's region of effect
        getOrder().setRegion(thisSector.getPosition().getRegion());

        // Make sure that the owner is the same
        if (thisSector.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();
            final int regionId = thisSector.getPosition().getRegion().getId();

            // retrieve production site type
            final int typeId = Integer.parseInt(getOrder().getParameter2());

            if ((thisSector.getProductionSite() == null)
                    || (thisSector.getProductionSite().getId() == 0)
                    || (thisSector.getProductionSite().getId() >= PS_BARRACKS && typeId > PS_BARRACKS)) {

                // Retrieve the production site
                final ProductionSite psite = ProductionSiteManager.getInstance().getByID(typeId);

                // Check that the terrain is appropriate
                if (psite.getTerrainsSuitable().indexOf(String.valueOf(thisSector.getTerrain().getCode())) >= 0) {

                    // Check that population size is appropriate
                    if ((thisSector.getPopulation() >= psite.getMinPopDensity()) && (thisSector.getPopulation() <= psite.getMaxPopDensity())) {

                        // Check if natural resource is present in this sector
                        if ((psite.getId() != PS_MINE && psite.getId() != PS_VINEYARD && psite.getId() != PS_PLANTATION)
                                || ((thisSector.getNaturalResource() != null) &&
                                (((psite.getId() == PS_MINE) && ((thisSector.getNaturalResource().getId() == NATRES_ORE) || (thisSector.getNaturalResource().getId() == NATRES_GEMS) || (thisSector.getNaturalResource().getId() == NATRES_METALS)))
                                        || ((psite.getId() == PS_VINEYARD) && (thisSector.getNaturalResource().getId() == NATRES_WINE))
                                        || ((psite.getId() == PS_PLANTATION) && (thisSector.getNaturalResource().getId() == NATRES_COLONIAL))
                                ))) {

                            // Check if production site is allowed in this region
                            // e.g., Plantation can only be built in the Colonies
                            // e.g., Mints only in Europe
                            if (((psite.getId() == PS_MINT) && (thisSector.getPosition().getRegion().getId() == EUROPE))
                                    || ((psite.getId() == PS_PLANTATION) && (thisSector.getPosition().getRegion().getId() != EUROPE))
                                    || ((psite.getId() != PS_MINT) && (psite.getId() != PS_PLANTATION))) {

                                // 2,000 men are needed to build all sites except :
                                // Mines & Factories & Barrack w/ small fortification (biult in one go) that need 4,000 men
                                int peopleRequired = 2000;
                                if ((psite.getId() == PS_MINE)
                                        || (psite.getId() == PS_FACTORY)) {
                                    peopleRequired = 4000;
                                }

                                // Check that required citizens are available at the warehouse
                                if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= peopleRequired) {

                                    // check the money
                                    if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= psite.getCost()) {

                                        // Check if enemy brigades are present
                                        if (enemyNotPresent(thisSector)) {
                                            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();

                                            if (psite.getId() == ProductionSiteConstants.PS_BARRACKS) {
                                                // check if this is the first barrack in the colonies
                                                if (regionId != EUROPE) {
                                                    boolean foundMore = false;
                                                    final List<Barrack> lstBarracks = BarrackManager.getInstance().listByGameNation(getOrder().getGame(), thisSector.getNation());
                                                    for (final Barrack barrack : lstBarracks) {
                                                        if (barrack.getPosition().getRegion().getId() == regionId) {
                                                            foundMore = true;
                                                        }
                                                    }

                                                    if (!foundMore) {
                                                        // Start up a colony
                                                        final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(thisSector.getNation(), getOrder().getGame(), "colony." + regionId);
                                                        if (lstReports.isEmpty()) {
                                                            // We gain 8 VP
                                                            changeVP(getOrder().getGame(), thisSector.getNation(), STARTUP_COLONY,
                                                                    "Start up a Colony in " + thisSector.getPosition().getRegion().getName());

                                                            report("colony." + regionId, "1");

                                                            // Modify player's profile
                                                            changeProfile(thisSector.getNation(), ProfileConstants.STARTUP_COLONY, 1);

                                                            // Update achievements
                                                            achievementsSetupColonies(getOrder().getGame(), thisSector.getNation());
                                                        }
                                                    }
                                                }

                                            } else if (psite.getId() == ProductionSiteConstants.PS_BARRACKS_FS) {
                                                // this is a fortification -- since you will need industrial points
                                                // and stone
                                                final int indPt = 500;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) <= indPt) {
                                                    getOrder().setResult(-10);
                                                    getOrder().setExplanation("not enough industrial points present at the regional warehouse");
                                                }

                                                int product = GOOD_STONE;
                                                String productName = "stone";
                                                if (regionId != EUROPE) {
                                                    product = GOOD_WOOD;
                                                    productName = "wood";
                                                }

                                                final int stone = 1500;
                                                if (getParent().getTotGoods(ownerId, regionId, product) <= stone) {
                                                    getOrder().setResult(-11);
                                                    getOrder().setExplanation("not enough " + productName + " present at the regional warehouse");
                                                }

                                                // deduct materials
                                                getParent().decTotGoods(ownerId, regionId, GOOD_INPT, indPt);
                                                getParent().decTotGoods(ownerId, regionId, product, stone);

                                                // report used goods
                                                usedGoods.put(GOOD_INPT, indPt);
                                                usedGoods.put(product, stone);

                                            } else if (psite.getId() == ProductionSiteConstants.PS_BARRACKS_FM) {
                                                // this is a fortification -- since you will need industrial points
                                                // and stone
                                                final int indPt = 1000;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) <= indPt) {
                                                    getOrder().setResult(-10);
                                                    getOrder().setExplanation("not enough industrial points present at the regional warehouse");
                                                }

                                                final int stone = 4000;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_STONE) <= stone) {
                                                    getOrder().setResult(-11);
                                                    getOrder().setExplanation("not enough stone present at the regional warehouse");
                                                }

                                                // deduct materials
                                                getParent().decTotGoods(ownerId, regionId, GOOD_INPT, indPt);
                                                getParent().decTotGoods(ownerId, regionId, GOOD_STONE, stone);

                                                // report used goods
                                                usedGoods.put(GOOD_INPT, indPt);
                                                usedGoods.put(GOOD_STONE, stone);

                                            } else if (psite.getId() == ProductionSiteConstants.PS_BARRACKS_FL) {
                                                // this is a fortification -- since you will need industrial points
                                                // and stone
                                                final int indPt = 5000;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) <= indPt) {
                                                    getOrder().setResult(-10);
                                                    getOrder().setExplanation("not enough industrial points present at the regional warehouse");
                                                }

                                                final int stone = 16000;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_STONE) <= stone) {
                                                    getOrder().setResult(-11);
                                                    getOrder().setExplanation("not enough stone present at the regional warehouse");
                                                }

                                                // deduct materials
                                                getParent().decTotGoods(ownerId, regionId, GOOD_INPT, indPt);
                                                getParent().decTotGoods(ownerId, regionId, GOOD_STONE, stone);

                                                // report used goods
                                                usedGoods.put(GOOD_INPT, indPt);
                                                usedGoods.put(GOOD_STONE, stone);

                                            } else if (psite.getId() == ProductionSiteConstants.PS_BARRACKS_FH) {
                                                // this is a fortification -- since you will need industrial points
                                                // and stone
                                                final int indPt = 15000;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) <= indPt) {
                                                    getOrder().setResult(-10);
                                                    getOrder().setExplanation("not enough industrial points present at the regional warehouse");
                                                }

                                                final int stone = 45000;
                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_STONE) <= stone) {
                                                    getOrder().setResult(-11);
                                                    getOrder().setExplanation("not enough stone present at the regional warehouse");
                                                }

                                                // deduct materials
                                                getParent().decTotGoods(ownerId, regionId, GOOD_INPT, indPt);
                                                getParent().decTotGoods(ownerId, regionId, GOOD_STONE, stone);

                                                // report used goods
                                                usedGoods.put(GOOD_INPT, indPt);
                                                usedGoods.put(GOOD_STONE, stone);

                                                if (!getParent().getGame().isFastFortressConstruction()) {
                                                    // Huge fortresses are not built in 1 turn.
                                                    // It takes 3 months to upgrade a large fortress to a huge one.
                                                    // All the goods for the construction are removed from the warehouse
                                                    // immediately, and if the construction process is cancelled for some
                                                    // reason (for example if an enemy force occupy that co-ordination)
                                                    // the goods are not 're-funded'.
                                                    thisSector.setBuildProgress(3);

                                                } else {
                                                    // We gain 15 VPs
                                                    changeVP(getParent().getGame(), thisSector.getNation(), BUILD_HUGE, "Built a huge fortress");

                                                    getParent().newsGlobal(thisSector.getNation(), NewsConstants.NEWS_MILITARY, thisSector.getProductionSite().getId(),
                                                            "We built a huge fortress at " + thisSector.getPosition().toString(),
                                                            thisSector.getNation().getName() + " built a huge fortress at " + thisSector.getPosition().toString());

                                                    // Modify player's profile
                                                    getParent().changeProfile(getParent().getGame(), thisSector.getNation(), ProfileConstants.FORTRESS_BUILT, 1);

                                                    // Update achievements
                                                    getParent().achievementsBuildHuge(getParent().getGame(), thisSector.getNation());
                                                }
                                            }

                                            // Reduce materials
                                            getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, peopleRequired);
                                            getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, psite.getCost());
                                            thisSector.setProductionSite(psite);
                                            SectorManager.getInstance().update(thisSector);

                                            // Update goods used by order
                                            usedGoods.put(GOOD_MONEY, psite.getCost());
                                            usedGoods.put(GOOD_PEOPLE, peopleRequired);
                                            getOrder().setUsedGoodsQnt(usedGoods);

                                            // Check if this is a barrack or a shipyard
                                            if (psite.getId() == ProductionSiteConstants.PS_BARRACKS) {

                                                // Also add Barrack
                                                final Barrack thisBarrack = new Barrack();
                                                thisBarrack.setNation(thisSector.getNation());
                                                thisBarrack.setPosition(thisSector.getPosition());
                                                thisBarrack.setName(thisSector.getName());
                                                BarrackManager.getInstance().add(thisBarrack);
                                            }

                                            // Add news entry for fortresses
                                            switch (psite.getId()) {
                                                case PS_BARRACKS_FS:
                                                    newsSingle(thisSector.getNation(), NewsConstants.NEWS_MILITARY, thisSector.getProductionSite().getId(),
                                                            "We built a small fortress at " + thisSector.getPosition().toString());
                                                    break;

                                                case PS_BARRACKS_FM:
                                                    newsSingle(thisSector.getNation(), NewsConstants.NEWS_MILITARY, thisSector.getProductionSite().getId(),
                                                            "We built a medium fortress at " + thisSector.getPosition().toString());
                                                    break;

                                                case PS_BARRACKS_FL:
                                                    newsSingle(thisSector.getNation(), NewsConstants.NEWS_MILITARY, thisSector.getProductionSite().getId(),
                                                            "We built a large fortress at " + thisSector.getPosition().toString());
                                                    break;

                                                case PS_BARRACKS_FH:
                                                    newsGlobal(thisSector.getNation(), NewsConstants.NEWS_MILITARY, true,
                                                            "We started the construction of a huge fortress at " + thisSector.getPosition().toString(),
                                                            thisSector.getNation().getName() + " is fortifying its holdings at " + thisSector.getPosition().toString());
                                                    break;
                                                default:
                                                    // no news
                                            }

                                            getOrder().setResult(1);
                                            getOrder().setExplanation("new production site (" + psite.getName() + ") built at sector "
                                                    + thisSector.getPosition().toString()
                                                    + " (" + thisSector.getPosition().getRegion().getName() + ")");

                                        } else {
                                            getOrder().setResult(-1);
                                            getOrder().setExplanation("enemy forces located on the sector");
                                        }

                                    } else {
                                        getOrder().setResult(-2);
                                        getOrder().setExplanation("not enough money at the empire warehouse");
                                    }

                                } else {
                                    getOrder().setResult(-3);
                                    getOrder().setExplanation("not enough citizens present at the regional warehouse");
                                }

                            } else {
                                getOrder().setResult(-4);
                                getOrder().setExplanation("production site cannot be built in this region");
                            }

                        } else {
                            getOrder().setResult(-5);
                            getOrder().setExplanation("sector does not have the required natural resource");
                        }

                    } else {
                        getOrder().setResult(-6);
                        getOrder().setExplanation("population density outside limits");
                    }

                } else {
                    getOrder().setResult(-7);
                    getOrder().setExplanation("incompatible terrain type");
                }
            } else {
                getOrder().setResult(-8);
                getOrder().setExplanation("production site already exists");
            }

        } else {
            getOrder().setResult(-9);
            getOrder().setExplanation("not owner of sector");
        }
    }

    /**
     * Check and set specific achievement.
     *
     * @param game   the Game to check.
     * @param nation the Nation to check.
     */
    public void achievementsSetupColonies(final Game game, final Nation nation) {
        final User owner = getParent().getUser(game, nation);
        final int totColonies = getParent().getProfile(game, owner, ProfileConstants.STARTUP_COLONY);

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

}
