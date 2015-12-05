package com.eaw1805.orders.politics;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the Politics PlayerOrder.
 * ticket #17.
 */
public class PoliticsOrderProcessor
        extends AbstractOrderProcessor
        implements OrderInterface, RelationConstants, VPConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(PoliticsOrderProcessor.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_POLITICS;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public PoliticsOrderProcessor(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("PoliticsOrderProcessor instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        // retrieve 
        final int targetId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve winner nation
        final Nation target = NationManager.getInstance().getByID(targetId);

        // Retrieve relation between two nations
        final NationsRelation relation = RelationsManager.getInstance().getByNations(getOrder().getGame(), getOrder().getNation(), target);

        // retrieve change in relation (and flip)
        final int targetRelation = Integer.parseInt(getOrder().getParameter2());

        // Check if we have crossed the 3rd turn
        if ((getParent().getGame().getScenarioId() == HibernateUtil.DB_S1)
                && (getParent().getGame().getTurn() < 3)
                && (targetRelation > REL_COLONIAL_WAR)) {
            getOrder().setResult(-1);
            getOrder().setExplanation("cannot attack another Empire during the initial truce period");

        } else {

            // Check if this is an Advanced Politics order
            // Requirement: Already at War and an order to change it to War Relation.
            if (targetRelation == REL_WAR
                    && relation.getRelation() == REL_WAR) {
                // Look 3rd parameter
                final int advancedOption = Integer.parseInt(getOrder().getParameter3());

                // For advanced option to work properly we need a combination from both players so that they work properly
                // Check previous turns
                final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByRelations(getOrder().getGame(), target, getOrder().getNation());

                switch (advancedOption) {
                    case ACCEPT_SURR:
                        // We need the other player to accept our Surrender
                        // Check previous turns
                        if (lstOrders.isEmpty()) {
                            newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                    "Our attempt to accept surrender from " + target.getName() + " failed.",
                                    getOrder().getNation().getName() + " was expecting our surrender this month.");

                            getOrder().setResult(-8);
                            getOrder().setExplanation("offer to accept surrender was denied");

                        } else {
                            final PlayerOrder thatOrder = lstOrders.get(0);
                            final int thatTargetRelation = Integer.parseInt(thatOrder.getParameter2());
                            final int thatAdvancedOption = Integer.parseInt(thatOrder.getParameter3());
                            if (thatTargetRelation == REL_WAR && thatAdvancedOption == OFFER_SURR) {
                                surrenderAccept(target, relation, thatOrder);

                                getOrder().setResult(1);
                                getOrder().setExplanation(getOrder().getNation().getName() + " changed relation with " + target.getName() + " to " + getNameRelation(REL_PASSAGE) + " for 3 months.");

                            } else {
                                newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                        "Our attempt to surrender to " + target.getName() + " was turned down.",
                                        getOrder().getNation().getName() + " tried to surrender to us this month but we turned down the offer.");

                                getOrder().setResult(-9);
                                getOrder().setExplanation("offer to accept surrender was denied");
                            }
                        }
                        break;

                    case OFFER_SURR:
                        // We need the other player to accept our Surrender
                        // Check previous turns
                        if (lstOrders.isEmpty()) {
                            newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                    "Our attempt to surrender to " + target.getName() + " was turned down.",
                                    getOrder().getNation().getName() + " tried to surrender to us this month but we turned down the offer.");

                            getOrder().setResult(-6);
                            getOrder().setExplanation("offer for surrender was denied");

                        } else {
                            final PlayerOrder thatOrder = lstOrders.get(0);
                            final int thatTargetRelation = Integer.parseInt(thatOrder.getParameter2());
                            final int thatAdvancedOption = Integer.parseInt(thatOrder.getParameter3());
                            if (thatTargetRelation == REL_WAR && thatAdvancedOption == ACCEPT_SURR) {
                                surrenderOffer(target, relation, thatOrder);

                                getOrder().setResult(1);
                                getOrder().setExplanation(getOrder().getNation().getName() + " changed relation with " + target.getName() + " to " + getNameRelation(REL_PASSAGE) + " for 3 months.");

                            } else {
                                newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                        "Our attempt to surrender to " + target.getName() + " was turned down.",
                                        getOrder().getNation().getName() + " tried to surrender to us this month but we turned down the offer.");

                                getOrder().setResult(-7);
                                getOrder().setExplanation("offer for surrender was denied");
                            }
                        }

                        break;

                    case MAKE_PEACE:
                        // We need both to press the Make Peace button to work properly
                        // Check previous turns
                        if (lstOrders.isEmpty()) {
                            newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                    "We offered peace to " + target.getName() + " but it was turned down.",
                                    "We turned down the peace offer of " + getOrder().getNation().getName());

                            getOrder().setResult(-4);
                            getOrder().setExplanation("offer for piece was turned down");

                        } else {
                            final PlayerOrder thatOrder = lstOrders.get(0);
                            final int thatTargetRelation = Integer.parseInt(thatOrder.getParameter2());
                            final int thatAdvancedOption = Integer.parseInt(thatOrder.getParameter3());
                            if (thatTargetRelation == REL_WAR && thatAdvancedOption == MAKE_PEACE) {
                                peaceMake(target, relation, thatOrder);

                                getOrder().setResult(1);
                                getOrder().setExplanation(getOrder().getNation().getName() + " changed relation with " + target.getName() + " to " + getNameRelation(REL_PASSAGE) + " for 3 months.");

                            } else {
                                newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                        "We offered peace to " + target.getName() + " but it was turned down.",
                                        "We turned down the peace offer of " + getOrder().getNation().getName());

                                getOrder().setResult(-5);
                                getOrder().setExplanation("offer for peace was turned down");
                            }
                        }
                        break;

                    default:
                        newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                                "We are already at war with " + target.getName() + ".",
                                getOrder().getNation().getName() + " was about to declare war upon us, but we did it first.");

                        getOrder().setResult(-4);
                        getOrder().setExplanation("already at war");
                }

            } else if (Math.abs(relation.getRelation() - targetRelation) <= 2) {
                // Changes can go up to distance 2 from previous
                if (targetRelation > relation.getRelation()) {

                    if (targetRelation == REL_WAR) {
                        // declare war
                        warDeclare(target, relation, targetRelation);

                    } else {
                        // just reduce relations
                        reduceRelations(target, relation, targetRelation);
                    }

                    getOrder().setResult(1);
                    getOrder().setExplanation(getOrder().getNation().getName() + " changed relation with " + target.getName() + " to " + getNameRelation(targetRelation));

                } else {
                    improveRelations(target, relation, targetRelation);
                }

            } else {
                // Final check - if this is a response to call for allies
                if (targetRelation == REL_WAR) {
                    // Check if we declare war due to a call to Allies
                    final List<Report> lstReports = ReportManager.getInstance().listByOwnerTurnKey(getOrder().getNation(),
                            getOrder().getGame(),
                            getOrder().getTurn() - 1,
                            "callallies." + targetId);

                    if (!lstReports.isEmpty()) {
                        // declare war
                        warDeclare(target, relation, targetRelation);

                    } else {
                        // Not allowed
                        getOrder().setResult(-2);
                        getOrder().setExplanation("cannot change foreign relationships by more than a factor of two within a month");
                    }

                } else {
                    // Not allowed
                    getOrder().setResult(-2);
                    getOrder().setExplanation("cannot change foreign relationships by more than a factor of two within a month");
                }
            }
        }
    }

    private void improveRelations(final Nation target, final NationsRelation relation, final int targetRelation) {
        // We need both to press the same button to work properly
        final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByRelations(getOrder().getGame(), target, getOrder().getNation());
        if (lstOrders.isEmpty()) {
            // No button pressed from the other side.

            if (targetRelation == REL_ALLIANCE) {
                newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                        "We offered an alliance treaty to " + target.getName() + " but it was turned down.",
                        "We turned down the alliance treaty of " + getOrder().getNation().getName());

                getOrder().setResult(-3);
                getOrder().setExplanation("offer for alliance was turned down");

            } else {
                newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                        "We offered to improve our relations with " + target.getName() + " to " + getNameRelation(targetRelation) + " but it was turned down.",
                        "We turned down the offer of " + getOrder().getNation().getName() + " to improve our relations to " + getNameRelation(targetRelation));

                getOrder().setResult(-3);
                getOrder().setExplanation("offer to improve relations to " + getNameRelation(targetRelation) + " was turned down");
            }

        } else {
            final PlayerOrder thatOrder = lstOrders.get(0);
            final int thatTargetRelation = Integer.parseInt(thatOrder.getParameter2());
            if (thatTargetRelation == targetRelation) {
                // Both sides pressed the same button.

                if (thatTargetRelation == REL_ALLIANCE) {
                    if (thatOrder.getResult() == 0) {
                        newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                                "We signed an alliance with " + target.getName(),
                                "We signed an alliance with " + getOrder().getNation().getName(),
                                getOrder().getNation().getName() + " signed an alliance with " + target.getName());
                    }

                    // We gain 4 VPs
                    changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_ALLIANCE, "Signed an alliance with " + target.getName());

                    // Modify player's profile
                    changeProfile(getOrder().getNation(), ProfileConstants.ALLIANCES_MADE, 1);

                    // Update achievements
                    getParent().achievementsAlliance(getOrder().getGame(), getOrder().getNation());
                }

                relation.setPeaceCount(0);
                relation.setSurrenderCount(0);
                relation.setTurnCount(-1);
                relation.setRelation(targetRelation);
                RelationsManager.getInstance().update(relation);

                getOrder().setResult(1);
                getOrder().setExplanation(getOrder().getNation().getName() + " changed relation with " + target.getName() + " to " + getNameRelation(targetRelation));

            } else {
                // Must press the same button.
                if (targetRelation == REL_ALLIANCE) {
                    newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                            "We offered an alliance treaty to " + target.getName() + " but it was turned down.",
                            "We turned down the alliance treaty of " + getOrder().getNation().getName());

                    getOrder().setResult(-3);
                    getOrder().setExplanation("offer for alliance was turned down");

                } else {
                    newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                            "We offered to improve our relations with " + target.getName() + " to " + getNameRelation(targetRelation) + " but it was turned down.",
                            "We turned down the offer of " + getOrder().getNation().getName() + " to improve our relations to " + getNameRelation(targetRelation));

                    getOrder().setResult(-3);
                    getOrder().setExplanation("offer to improve relations to " + getNameRelation(targetRelation) + " was turned down");
                }
            }
        }
    }

    private void surrenderAccept(final Nation target, final NationsRelation relation, final PlayerOrder thatOrder) {
        newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                target.getName() + " surrendered to us!",
                "We surrendered to " + getOrder().getNation().getName(),
                target.getName() + " surrendered to " + getOrder().getNation().getName());

        // Return prisoners of war
        if (relation.getPrisoners() > 0) {
            final NumberFormat formatter = new DecimalFormat("#,###,###");
            newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                    "We released " + formatter.format(relation.getPrisoners()) + " prisoners of " + target.getName(),
                    getOrder().getNation().getName() + " released " + formatter.format(relation.getPrisoners()) + " of our soldiers held prisoners of war.",
                    getOrder().getNation().getName() + " released " + formatter.format(relation.getPrisoners()) + " of soldiers of " + target.getName() + " held prisoners of war.");

            // Prisoners go directly to warehouse
            getParent().incTotGoods(target.getId(), EUROPE, GoodConstants.GOOD_PEOPLE, relation.getPrisoners());

            // Update goods used by other order
            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
            usedGoods.put(GoodConstants.GOOD_PEOPLE, -1 * relation.getPrisoners());
            thatOrder.setUsedGoodsQnt(usedGoods);
            PlayerOrderManager.getInstance().update(thatOrder);

            relation.setPrisoners(0);
        }

        relation.setTurnCount(-1);
        relation.setSurrenderCount(1);
        relation.setPeaceCount(0);
        relation.setRelation(REL_PASSAGE);
        RelationsManager.getInstance().update(relation);

        // Return captured commanders
        final List<Commander> lstCommanders = CommanderManager.getInstance().listGameNation(getParent().getGame(), target);
        for (Commander commander : lstCommanders) {
            if (commander.getCaptured().getId() == getOrder().getNation().getId()) {
                // Release commander
                commander.setCaptured(commander.getNation());
                commander.setPool(true);
                CommanderManager.getInstance().update(commander);

                newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                        "We released captured commander " + commander.getName() + " of " + target.getName(),
                        getOrder().getNation().getName() + " released captured commander " + commander.getName() + ".",
                        getOrder().getNation().getName() + " released captured commander " + commander.getName() + " of " + target.getName() + ".");
            }
        }

        // We gain 30 VPs
        changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_SURRENDER_ACCEPT, "Accepting surrender of " + target.getName());

        // Modify player's profile
        changeProfile(getOrder().getNation(), ProfileConstants.SURRENDERS_ACCEPTED, 1);

        // Update achievements
        getParent().achievementsSurrenders(getOrder().getGame(), getOrder().getNation());

        // Check if an ally is still at war with power that just surrendered
        final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(getOrder().getGame(), target);
        for (final NationsRelation thisRelation : lstRelations) {
            if (thisRelation.getRelation() == REL_WAR
                    && thisRelation.getTarget().getId() != getOrder().getNation().getId()) {
                // Check relation with winner of order
                final NationsRelation theirRelation = RelationsManager.getInstance().getByNations(getOrder().getGame(),
                        thisRelation.getTarget(), getOrder().getNation());
                if (theirRelation.getRelation() == REL_ALLIANCE) {
                    // This is an ally ...
                    LOGGER.warn("CHECK multiple surrenders");

                    // check if the ally is also accepting surrender from player
                    final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByRelations(getOrder().getGame(), thisRelation.getTarget(), target);
                    if (lstOrders.isEmpty()) {

                        // We lose 4 VP per ally
                        changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_SURRENDER_ACCEPT_OTHERS, "Accepting surrender of " + target.getName() + " when our ally " + thisRelation.getTarget().getName() + " still at war with same power");
                    }
                }
            }
        }
    }

    private void surrenderOffer(final Nation winner, final NationsRelation relation, final PlayerOrder thatOrder) {
        // Return all sectors conquered by nation surrendering
        returnHomeRegionSectors(getOrder().getNation(), winner);

        // Return the capital of nation surrendering if it was captured by the nation accepting the surrender
        returnCapital(getOrder().getNation(), winner);

        // Return prisoners of war
        if (relation.getPrisoners() > 0) {
            final NumberFormat formatter = new DecimalFormat("#,###,###");
            newsGlobal(getOrder().getNation(), winner, NEWS_POLITICAL,
                    "We released " + formatter.format(relation.getPrisoners()) + " prisoners of " + winner.getName(),
                    getOrder().getNation().getName() + " released " + formatter.format(relation.getPrisoners()) + " of our soldiers held prisoners of war.",
                    getOrder().getNation().getName() + " released " + formatter.format(relation.getPrisoners()) + " of soldiers of " + winner.getName() + " held prisoners of war.");

            // Prisoners go directly to warehouse
            getParent().incTotGoods(winner.getId(), EUROPE, GoodConstants.GOOD_PEOPLE, relation.getPrisoners());

            // Update goods used by other order
            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
            usedGoods.put(GoodConstants.GOOD_PEOPLE, -1 * relation.getPrisoners());
            thatOrder.setUsedGoodsQnt(usedGoods);
            PlayerOrderManager.getInstance().update(thatOrder);

            relation.setPrisoners(0);
        }

        // Return captured commanders
        final List<Commander> lstCommanders = CommanderManager.getInstance().listGameNation(getParent().getGame(), winner);
        for (Commander commander : lstCommanders) {
            if (commander.getCaptured().getId() == getOrder().getNation().getId()) {
                // Release commander
                commander.setCaptured(commander.getNation());
                commander.setPool(true);
                CommanderManager.getInstance().update(commander);

                newsGlobal(getOrder().getNation(), winner, NEWS_POLITICAL,
                        "We released captured commander " + commander.getName() + " of " + winner.getName(),
                        getOrder().getNation().getName() + " released captured commander " + commander.getName() + ".",
                        getOrder().getNation().getName() + " released captured commander " + commander.getName() + " of " + winner.getName() + ".");
            }
        }

        relation.setTurnCount(-1);
        relation.setSurrenderCount(1);
        relation.setPeaceCount(0);
        relation.setRelation(REL_PASSAGE);
        RelationsManager.getInstance().update(relation);

        // We lose 20 VPs
        changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_SURRENDER, "Surrender to " + winner.getName());

        // Modify player's profile
        changeProfile(getOrder().getNation(), ProfileConstants.SURRENDERS_MADE, 1);
    }

    private void peaceMake(final Nation target, final NationsRelation relation, final PlayerOrder thatOrder) {

        // Check reverse relation to identify if this is the 1st player order or the 2nd player order
        // Retrieve relation between two nations
        final NationsRelation reverse = RelationsManager.getInstance().getByNations(getOrder().getGame(), target, getOrder().getNation());
        if (reverse.getRelation() == REL_PASSAGE) {
            // Announce only once
            newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                    "We signed a peace treaty with " + target.getName(),
                    "We signed a peace treaty with " + getOrder().getNation().getName(),
                    getOrder().getNation().getName() + " signed a peace treaty with " + target.getName());
        }

        // Return all sectors conquered
        returnHomeRegionSectors(getOrder().getNation(), target);

        // Return prisoners of war
        if (relation.getPrisoners() > 0) {
            final NumberFormat formatter = new DecimalFormat("#,###,###");
            newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                    "We released " + formatter.format(relation.getPrisoners()) + " prisoners of " + target.getName(),
                    getOrder().getNation().getName() + " released " + formatter.format(relation.getPrisoners()) + " of our soldiers held prisoners of war.",
                    getOrder().getNation().getName() + " released " + formatter.format(relation.getPrisoners()) + " of soldiers of " + target.getName() + " held prisoners of war.");

            // Prisoners go directly to warehouse
            getParent().incTotGoods(target.getId(), EUROPE, GoodConstants.GOOD_PEOPLE, relation.getPrisoners());

            // Update goods used by other order
            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
            usedGoods.put(GoodConstants.GOOD_PEOPLE, -1 * relation.getPrisoners());
            thatOrder.setUsedGoodsQnt(usedGoods);
            PlayerOrderManager.getInstance().update(thatOrder);

            relation.setPrisoners(0);
        }

        // Return captured commanders
        final List<Commander> lstCommanders = CommanderManager.getInstance().listGameNation(getParent().getGame(), target);
        for (Commander commander : lstCommanders) {
            if (commander.getCaptured().getId() == getOrder().getNation().getId()) {
                // Release commander
                commander.setCaptured(commander.getNation());
                commander.setPool(true);
                CommanderManager.getInstance().update(commander);

                newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                        "We released captured commander " + commander.getName() + " of " + target.getName(),
                        getOrder().getNation().getName() + " released captured commander " + commander.getName() + ".",
                        getOrder().getNation().getName() + " released captured commander " + commander.getName() + " of " + target.getName() + ".");
            }
        }

        relation.setTurnCount(-1);
        relation.setPeaceCount(1);
        relation.setSurrenderCount(0);
        relation.setRelation(REL_PASSAGE);
        RelationsManager.getInstance().update(relation);

        // We gain 6 VPs
        changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_PEACE, "Making peace with " + target.getName());

        // Modify player's profile
        changeProfile(getOrder().getNation(), ProfileConstants.PEACE_MADE, 1);
    }

    private void warDeclare(final Nation target, final NationsRelation relation, final int targetRelation) {
        final int targetId = target.getId();

        // Check if we declare war due to a call to Allies
        final List<Report> lstReports = ReportManager.getInstance().listByOwnerTurnKey(getOrder().getNation(),
                getOrder().getGame(),
                getOrder().getTurn() - 1,
                "callallies." + targetId);

        if (lstReports.isEmpty()) {
            // This is not a call to allies

            // Check if there is a casus belli in effect
            final List<Report> lstCasus = ReportManager.getInstance().listByOwnerTurnKey(getOrder().getNation(),
                    getOrder().getGame(),
                    getOrder().getTurn(),
                    RE_CRIS);

            boolean foundCasusBelli = false;
            for (final Report casus : lstCasus) {
                if (Integer.parseInt(casus.getValue()) == targetId) {
                    foundCasusBelli = true;
                    break;
                }
            }

            if (!foundCasusBelli) {
                // We lose 12 VPs
                changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_WAR, "Declaring war to " + target.getName());

                newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                        "We declared war to " + target.getName(),
                        getOrder().getNation().getName() + " declared war upon us!",
                        getOrder().getNation().getName() + " declared war against " + target.getName());

            } else {
                // We lose 6 VPs
                changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_WAR / 2, "Declaring war to " + target.getName() + " due to casus belli!");

                newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                        "We declared war to " + target.getName() + " due to casus belli!",
                        getOrder().getNation().getName() + " declared war upon us due to casus belli!",
                        getOrder().getNation().getName() + " declared war against " + target.getName() + " due to casus belli!");
            }

            // Modify player's profile
            changeProfile(getOrder().getNation(), ProfileConstants.WARS_DECLARED, 1);

            // Update achievements
            getParent().achievementsWar(getOrder().getGame(), getOrder().getNation());

        } else {
            // Declaring War due to a call to Allies
            // We lose 6 VPs
            if (relation.getTurnCount() > 0) {
                // We lose 6 VPs
                changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_WAR_CALL, "Declaring war to " + target.getName() + " due to a call to Allies");

                // Modify player's profile
                changeProfile(getOrder().getNation(), ProfileConstants.RESPOND_CALLALLIES, 1);

                // Update achievements
                getParent().achievementsAcceptCall(getOrder().getGame(), getOrder().getNation());
            }

            // Retrieve ally that called us to war
            final Nation allyNation = NationManager.getInstance().getByID(Integer.parseInt(lstReports.get(0).getValue()));

            newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                    "We declared war to " + target.getName() + " as a response to a call by our ally " + allyNation.getName(),
                    getOrder().getNation().getName() + " declared war upon us after a call from allied nation " + allyNation.getName(),
                    getOrder().getNation().getName() + " declared war against " + target.getName() + " after a call from allied nation " + allyNation.getName());
        }

        // Modify player's profile
        changeProfile(target, ProfileConstants.WARS_RECEIVED, 1);

        // Retrieve reverse relation
        final NationsRelation reverseRelation = RelationsManager.getInstance().getByNations(getOrder().getGame(), target, getOrder().getNation());
        reverseRelation.setRelation(REL_WAR);
        reverseRelation.setTurnCount(-1);
        reverseRelation.setPeaceCount(0);
        reverseRelation.setSurrenderCount(0);
        RelationsManager.getInstance().update(reverseRelation);

        relation.setRelation(REL_WAR);
        relation.setTurnCount(-1);
        relation.setPeaceCount(0);
        relation.setSurrenderCount(0);
        RelationsManager.getInstance().update(relation);

        // Target will Call allies
        getParent().addDeclaration(getOrder().getNation(), target);

        // Unload loaded units
        getParent().unloadUnits(getOrder().getGame(), getOrder().getNation(), target);
        getParent().unloadUnits(getOrder().getGame(), target, getOrder().getNation());

        // Remove ships from shipyards
        removeEnemyShips(getOrder().getGame(), getOrder().getNation(), target);
        removeEnemyShips(getOrder().getGame(), target, getOrder().getNation());

        // Remove trains from barracks
        removeEnemyTrains(getOrder().getGame(), getOrder().getNation(), target);
        removeEnemyTrains(getOrder().getGame(), target, getOrder().getNation());
    }

    private void reduceRelations(final Nation target, final NationsRelation relation, final int targetRelation) {
        // Check if prior relation was alliance, and we break alliance
        if (relation.getRelation() == REL_ALLIANCE) {
            newsGlobal(getOrder().getNation(), target, NEWS_POLITICAL,
                    "We broke the alliance with " + target.getName(),
                    getOrder().getNation().getName() + " broke our alliance!",
                    getOrder().getNation().getName() + " broke the alliance with " + target.getName());

            // We lose -5 VPs
            changeVP(getOrder().getGame(), getOrder().getNation(), POLITICS_ALLIANCE_BREAK, "Broke the alliance with " + target.getName());

            // Unload loaded units
            getParent().unloadUnits(getOrder().getGame(), getOrder().getNation(), target);
            getParent().unloadUnits(getOrder().getGame(), target, getOrder().getNation());

        } else {
            newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                    "We reduced our relations with " + target.getName() + " to " + getNameRelation(targetRelation) + ".",
                    "Our relations with " + getOrder().getNation().getName() + " where reduced to " + getNameRelation(targetRelation) + ".");
        }

        // Retrieve reverse relation
        final NationsRelation reverseRelation = RelationsManager.getInstance().getByNations(getOrder().getGame(), target, getOrder().getNation());
        reverseRelation.setTurnCount(-1);
        reverseRelation.setRelation(targetRelation);
        reverseRelation.setPeaceCount(0);
        reverseRelation.setSurrenderCount(0);
        RelationsManager.getInstance().update(reverseRelation);

        relation.setTurnCount(-1);
        relation.setRelation(targetRelation);
        relation.setPeaceCount(0);
        relation.setSurrenderCount(0);
        RelationsManager.getInstance().update(relation);

        if (relation.getRelation() >= REL_COLONIAL_WAR) {
            // Remove ships from shipyards
            removeEnemyShips(getOrder().getGame(), getOrder().getNation(), target);
            removeEnemyShips(getOrder().getGame(), target, getOrder().getNation());

            // Remove trains from barracks
            removeEnemyTrains(getOrder().getGame(), getOrder().getNation(), target);
            removeEnemyTrains(getOrder().getGame(), target, getOrder().getNation());

        } else if (relation.getRelation() >= REL_TRADE) {
            // Remove loaded ships & war ships from  shipyards
            removeShips(getOrder().getGame(), getOrder().getNation(), target);
            removeShips(getOrder().getGame(), target, getOrder().getNation());
        }
    }

    /**
     * Move ships out of enemy controlled shipyards.
     *
     * @param thisGame the game to inspect.
     * @param owner    the nation declared war.
     * @param newEnemy the winner to check ships.
     */
    private void removeEnemyShips(final Game thisGame, final Nation owner, final Nation newEnemy) {
        final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameNation(thisGame, owner);
        for (final Sector sector : lstBarracks) {
            // Identify ships in shipyard
            final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(sector.getPosition(), newEnemy);

            if (lstShips.isEmpty()) {
                continue;
            }

            // In case we need to move ships outside the shipyard, select the sector where it will move
            final List<Sector> lstSectors = SectorManager.getInstance().listAdjacentSea(sector.getPosition());

            final Sector exitSector;
            int cntShips = 0;
            final StringBuilder moved = new StringBuilder();

            if (lstSectors.isEmpty()) {
                LOGGER.info("No exit sector found at " + sector.getPosition());
                exitSector = null;

            } else {
                // Randomly select one
                java.util.Collections.shuffle(lstSectors);

                if (lstSectors.get(0).getId() == sector.getId()) {
                    lstSectors.remove(0);

                    if (lstSectors.isEmpty()) {
                        LOGGER.info("No exit sector found at " + sector.getPosition());
                        exitSector = null;

                    } else {
                        exitSector = lstSectors.get(0);
                    }

                } else {
                    exitSector = lstSectors.get(0);
                }
            }

            // Move ship out of port
            for (final Ship ship : lstShips) {
                cntShips++;
                moved.append(ship.getName());
                moved.append(", ");

                ship.setPosition((Position) exitSector.getPosition().clone());
                ShipManager.getInstance().update(ship);

                // check also loaded units
                ship.initializeVariables();

                // Check if the entity is carrying units, and update their position too
                if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                    ship.updatePosition((Position) exitSector.getPosition().clone());
                }

                // Check if ship is in fleet
                if (ship.getFleet() != 0) {
                    // Also update fleet
                    final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                    thisFleet.setPosition((Position) exitSector.getPosition().clone());
                    FleetManager.getInstance().update(thisFleet);
                }
            }

            moved.delete(moved.length() - 2, moved.length() - 1);

            // Send out news
            final int newsId = news(owner, newEnemy, NEWS_MILITARY, false, 0,
                    "Our shipyard at " + sector.getPosition() + " hosted ships owned by " + newEnemy.getName()
                            + ". " + cntShips
                            + " ships (" + moved.toString() + ") moved out of our port. ");

            news(newEnemy, owner, NEWS_MILITARY, false, newsId,
                    "The shipyard at " + sector.getPosition() + " owned by " + owner.getName()
                            + " is no longer friendly for our ships. "
                            + cntShips + " ships (" + moved.toString() + ") moved out of the port.");
        }
    }

    /**
     * Move trains out of enemy controlled shipyards.
     *
     * @param thisGame the game to inspect.
     * @param owner    the nation declared war.
     * @param newEnemy the winner to check ships.
     */
    private void removeEnemyTrains(final Game thisGame, final Nation owner, final Nation newEnemy) {
        final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameNation(thisGame, owner);
        for (final Sector sector : lstBarracks) {
            // Identify trains in barrack
            final List<BaggageTrain> lstTrains = BaggageTrainManager.getInstance().listByPositionNation(sector.getPosition(), newEnemy);

            if (lstTrains.isEmpty()) {
                continue;
            }

            // In case we need to move ships outside the barrack, select the sector where it will move
            final List<Sector> lstSectors = SectorManager.getInstance().listAdjacentLand(sector.getPosition());

            final Sector exitSector;
            int cntShips = 0;
            final StringBuilder moved = new StringBuilder();

            if (lstSectors.isEmpty()) {
                LOGGER.info("No exit sector found at " + sector.getPosition());
                exitSector = null;

            } else {
                // Randomly select one
                java.util.Collections.shuffle(lstSectors);

                if (lstSectors.get(0).getId() == sector.getId()) {
                    lstSectors.remove(0);

                    if (lstSectors.isEmpty()) {
                        LOGGER.info("No exit sector found at " + sector.getPosition());
                        exitSector = null;

                    } else {
                        exitSector = lstSectors.get(0);
                    }

                } else {
                    exitSector = lstSectors.get(0);
                }
            }

            // Move ship out of port
            for (final BaggageTrain train : lstTrains) {
                cntShips++;
                moved.append(train.getName());
                moved.append(", ");

                train.setPosition(exitSector.getPosition());
                BaggageTrainManager.getInstance().update(train);
            }

            moved.delete(moved.length() - 2, moved.length() - 1);

            // Send out news
            final int newsId = news(owner, newEnemy, NEWS_MILITARY, false, 0,
                    "Our barrack at " + sector.getPosition() + " hosted baggage trains owned by " + newEnemy.getName()
                            + ". " + cntShips
                            + " baggage trains (" + moved.toString() + ") moved out of our barrack. ");

            news(newEnemy, owner, NEWS_MILITARY, false, newsId,
                    "The barrack at " + sector.getPosition() + " owned by " + owner.getName()
                            + " is no longer friendly for our baggage trains. "
                            + cntShips + " baggage trains (" + moved.toString() + ") moved out of the barrack.");
        }
    }

    /**
     * Inspect home region sectors and those conquered are returned.
     * Captured home territories of the winning empire are returned to the winner,
     * however no home territories of the surrendering empire are returned (Capitals are always returned)
     *
     * @param loser     the home nation.
     * @param winner the winner nation.
     */
    private void returnHomeRegionSectors(final Nation loser, final Nation winner) {
        final Game thisGame = getOrder().getGame();
        final Game initGame = GameManager.getInstance().getByID(-1);
        final Region europe = RegionManager.getInstance().getByID(EUROPE);
        final List<Sector> initSectors = SectorManager.getInstance().listByGameRegionNation(initGame, europe, winner);
        for (final Sector sector : initSectors) {
            final Position thisPos = (Position) sector.getPosition().clone();
            thisPos.setGame(thisGame);

            if (sector.getId() == 79916
                    || sector.getId() == 79917
                    || sector.getId() == 79919) {
                LOGGER.debug("check this");
            }

            // Lookup sector in current game
            final Sector thisSector = SectorManager.getInstance().getByPosition(thisPos);

            if (thisSector.getNation().getId() == loser.getId()
                    || thisSector.getTempNation().getId() == loser.getId()) {
                // Return sector to winner
                thisSector.setNation(winner);
                thisSector.setTempNation(winner);
                thisSector.setConqueredCounter(0);
                SectorManager.getInstance().update(thisSector);

                // check if sector has barracks
                if (thisSector.getProductionSite() != null && thisSector.getProductionSite().getId() >= ProductionSiteConstants.PS_BARRACKS) {
                    // also hand-over barrack
                    final Barrack barrack = BarrackManager.getInstance().getByPosition(thisSector.getPosition());
                    barrack.setNation(winner);
                    BarrackManager.getInstance().update(barrack);
                }
            }
        }
    }

    /**
     * If the Capital of the winner is controlled by the winner, then it is returned to the winner.
     *
     * @param loser     the home nation.
     * @param winner the winner nation.
     */
    private void returnCapital(final Nation loser, final Nation winner) {
        final Game initGame = GameManager.getInstance().getByID(-1);
        final List<TradeCity> initTradeCities = TradeCityManager.getInstance().listByGame(initGame);

        for (final TradeCity initCity : initTradeCities) {
            // Retrieve winner of trade city when game starts
            if (initCity.getPosition().getRegion().getId() == EUROPE
                    && initCity.getNation().getId() == loser.getId()) {

                final Position thisPos = (Position) initCity.getPosition().clone();
                thisPos.setGame(getOrder().getGame());

                // Lookup sector in current game
                final Sector thisSector = SectorManager.getInstance().getByPosition(thisPos);

                if (thisSector.getNation() == winner
                        || thisSector.getTempNation() == winner) {
                    // Return sector to winner
                    thisSector.setNation(loser);
                    thisSector.setTempNation(loser);
                    thisSector.setConqueredCounter(0);
                    SectorManager.getInstance().update(thisSector);

                    // check if sector has barracks
                    if (thisSector.getProductionSite() != null && thisSector.getProductionSite().getId() >= ProductionSiteConstants.PS_BARRACKS) {
                        // also hand-over barrack
                        final Barrack barrack = BarrackManager.getInstance().getByPosition(thisSector.getPosition());
                        barrack.setNation(loser);
                        BarrackManager.getInstance().update(barrack);
                    }
                }
            }
        }
    }

    private String getNameRelation(final int relation) {
        switch (relation) {
            case REL_ALLIANCE:
                return "Alliance";

            case REL_PASSAGE:
                return "Right of passage";

            case REL_TRADE:
                return "Trading";

            case REL_COLONIAL_WAR:
                return "Colonial War";

            case REL_WAR:
                return "War";

            default:
                break;
        }
        return "unknown";
    }

    /**
     * Move ships out of enemy controlled shipyards.
     *
     * @param thisGame the game to inspect.
     * @param owner    the nation declared war.
     * @param newEnemy the winner to check ships.
     */
    private void removeShips(final Game thisGame, final Nation owner, final Nation newEnemy) {
        final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameNation(thisGame, owner);
        for (final Sector sector : lstBarracks) {
            // Identify ships in shipyard
            final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(sector.getPosition(), newEnemy);

            if (lstShips.isEmpty()) {
                continue;
            }

            // In case we need to move ships outside the shipyard, select the sector where it will move
            final List<Sector> lstSectors = SectorManager.getInstance().listAdjacentSea(sector.getPosition());

            final Sector exitSector;
            if (lstSectors.isEmpty()) {
                LOGGER.info("No exit sector found at " + sector.getPosition());
                exitSector = null;

            } else {
                // Randomly select one
                java.util.Collections.shuffle(lstSectors);

                if (lstSectors.get(0).getId() == sector.getId()) {
                    lstSectors.remove(0);

                    if (lstSectors.isEmpty()) {
                        LOGGER.info("No exit sector found at " + sector.getPosition());
                        exitSector = null;

                    } else {
                        exitSector = lstSectors.get(0);
                    }

                } else {
                    exitSector = lstSectors.get(0);
                }
            }

            // Move Warships out of port
            final StringBuilder movedEnemy = new StringBuilder();
            int movedShips = 0;

            // Decide which fleets have to move outside the port
            final Set<Integer> moveFleets = new HashSet<Integer>();

            // All ships of this nation must be moved away
            final List<Ship> lstShip = ShipManager.getInstance().listByPositionNation(sector.getPosition(), newEnemy);
            for (final Ship ship : lstShip) {
                // check also loaded units
                ship.initializeVariables();

                if (ship.getType().getShipClass() > 0
                        || ship.getHasTroops()) {
                    // This is a war ship, it has to be moved
                    if (ship.getFleet() > 0) {
                        // Fleet must move out of shipyard
                        moveFleets.add(ship.getFleet());

                    } else {
                        // Ship must move out of shipyard
                        movedShips++;
                        movedEnemy.append(ship.getName());
                        movedEnemy.append(", ");

                        ship.setPosition((Position) exitSector.getPosition().clone());
                        ShipManager.getInstance().update(ship);

                        // Check if the entity is carrying units, and update their position too
                        if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                            ship.updatePosition((Position) exitSector.getPosition().clone());
                        }

                        // Check if ship is in fleet
                        if (ship.getFleet() != 0) {
                            // Also update fleet
                            final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                            thisFleet.setPosition(exitSector.getPosition());
                            FleetManager.getInstance().update(thisFleet);
                        }
                    }
                }
            }

            // Check if we need to move fleets as well
            for (int moveFleet : moveFleets) {
                final List<Ship> fleetShips = ShipManager.getInstance().listByFleet(sector.getPosition().getGame(), moveFleet);
                for (Ship ship : fleetShips) {
                    // Ship must move out of shipyard
                    movedShips++;
                    movedEnemy.append(ship.getName());
                    movedEnemy.append(", ");

                    ship.setPosition((Position) exitSector.getPosition().clone());
                    ShipManager.getInstance().update(ship);

                    // check also loaded units
                    ship.initializeVariables();

                    // Check if the entity is carrying units, and update their position too
                    if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                        ship.updatePosition((Position) exitSector.getPosition().clone());
                    }

                    // Check if ship is in fleet
                    if (ship.getFleet() != 0) {
                        // Also update fleet
                        final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                        thisFleet.setPosition(exitSector.getPosition());
                        FleetManager.getInstance().update(thisFleet);
                    }
                }
            }

            // Report move
            if (movedShips > 0) {
                movedEnemy.delete(movedEnemy.length() - 2, movedEnemy.length() - 1);
                newsPair(owner, newEnemy, NEWS_MILITARY,
                        "Our relations with " + newEnemy.getName() + " changed to Trade. " +
                                movedShips + " ships (" + movedEnemy.toString() + ") were forced out of our port at " + exitSector.getPosition() + ".",
                        "Our relations with " + owner.getName() + " changed to Trade. " +
                                movedShips + " ships (" + movedEnemy.toString() + ") moved out of the port at " + exitSector.getPosition() + ".");
            }
        }
    }
}
