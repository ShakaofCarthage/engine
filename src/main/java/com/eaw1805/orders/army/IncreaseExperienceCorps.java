package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GameConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.army.ArmyTypeManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Increase the experience of all brigades beloning in a Corps.
 */
public class IncreaseExperienceCorps
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(IncreaseExperienceCorps.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_INC_EXP_CORPS;

    /**
     * Keep duration of game modifier.
     */
    private double modifier;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public IncreaseExperienceCorps(final OrderProcessor myParent) {
        super(myParent);

        // check duration of game
        switch (myParent.getGame().getType()) {
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

        LOGGER.debug("IncreaseExperience instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int corpId = Integer.parseInt(getOrder().getParameter1());

        final boolean isUpgradeCrack = (getOrder().getParameter2() != null) && (getOrder().getParameter2().equals("1"));

        int gTotMoney = 0, gTotIndPt = 0, gTotPeople = 0, gTotHorses = 0;

        // Retrieve the source brigade
        Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp == null && !getParent().corpAssocExists(corpId)) {
            getOrder().setResult(-6);
            getOrder().setExplanation("cannot locate corps");

        } else {
            // Check if this is a newly created army
            if (getParent().corpAssocExists(corpId)) {
                thisCorp = CorpManager.getInstance().getByID(getParent().retrieveCorpAssoc(corpId));
            }

            // Check ownership of source brigade
            if (thisCorp.getNation().getId() == getOrder().getNation().getId()) {
                final int ownerId = getOrder().getNation().getId();

                // Retrieve sector where brigade is positioned
                final Sector thisSector = SectorManager.getInstance().getByPosition(thisCorp.getPosition());

                // Update order's region of effect
                getOrder().setRegion(thisSector.getPosition().getRegion());

                // Battalion must be positioned in owned sectors
                if (thisSector.getNation().getId() == ownerId) {
                    final int regionId = thisSector.getPosition().getRegion().getId();

                    // Check that sector has a Barrack
                    if (thisSector.hasBarrack()) {

                        // Check if enemy brigades are present
                        if (enemyNotPresent(thisSector)) {

                            final StringBuilder explStr = new StringBuilder();
                            int totChanges = 0;

                            // iterate through battalions
                            for (Brigade thisBrigade : thisCorp.getBrigades()) {
                                for (final Battalion battalion : thisBrigade.getBattalions()) {

                                    // check if this is an upgrade to crack elite
                                    if (isUpgradeCrack) {

                                        // check if this is a crack unit
                                        if (battalion.getType().getCrack()) {

                                            // check if this battalion can be upgraded
                                            if (battalion.getType().getUpgradeEliteTo() > 0) {

                                                // check if it has reached maximum exp level
                                                if (battalion.getExperience() > battalion.getType().getMaxExp()) {

                                                    // retrieve nation's VPs in case order is about elite/crack-elite troops with VPs limitation
                                                    final int lastTurnVPs = Integer.parseInt(retrieveReport(getOrder().getNation(), getParent().getGame().getTurn() - 1, ReportConstants.N_VP));

                                                    // Crack Elite Availability is a function of VPs
                                                    if (battalion.getType().getVps() <= (100 * lastTurnVPs / (getOrder().getNation().getVpWin() * modifier))) {

                                                        // locate new type
                                                        final ArmyType newType = ArmyTypeManager.getInstance().getByIntID(battalion.getType().getUpgradeEliteTo(), getOrder().getNation());
                                                        final int money = newType.getCost();
                                                        final int inPt = newType.getIndPt();

                                                        // Determine the maximum headcount
                                                        int people = 800;
                                                        if (battalion.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                                                                || battalion.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                                                                || battalion.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                                                            people = 1000;
                                                        }

                                                        int horses = 0;
                                                        if (newType.needsHorse()) {
                                                            horses = people;
                                                        }

                                                        // check that enough money are available at play warehouse
                                                        if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= money) {

                                                            // check that enough materials are available at regional warehouse
                                                            if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= inPt) {

                                                                // check that enough materials are available at regional warehouse
                                                                if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= people) {

                                                                    // check that enough materials are available at regional warehouse
                                                                    if (getParent().getTotGoods(ownerId, regionId, GOOD_HORSE) >= horses) {

                                                                        // Everything set -- do increase
                                                                        getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, money);
                                                                        getParent().decTotGoods(ownerId, regionId, GOOD_INPT, inPt);
                                                                        getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, people);
                                                                        getParent().decTotGoods(ownerId, regionId, GOOD_HORSE, horses);

                                                                        battalion.setType(newType);
                                                                        BattalionManager.getInstance().update(battalion);

                                                                        totChanges++;

                                                                        explStr.append("battalion ");
                                                                        explStr.append(battalion.getOrder());
                                                                        explStr.append(" upgraded to Crack Elite [");
                                                                        explStr.append(newType.getName());
                                                                        explStr.append("], ");

                                                                        gTotMoney += money;
                                                                        gTotIndPt += inPt;
                                                                        gTotHorses += horses;
                                                                        gTotPeople += people;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // check if maximum has been reached
                                    } else if (battalion.getExperience() < battalion.getType().getMaxExp()) {

                                        // Determine the maximum headcount
                                        int headcount = 800;
                                        if (battalion.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                                                || battalion.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                                                || battalion.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                                            headcount = 1000;
                                        }

                                        // Calculate required amount of money
                                        final int money = (int) (battalion.getHeadcount() * battalion.getType().getCost() / (10d * headcount));
                                        final int inPt = (int) (battalion.getHeadcount() * battalion.getType().getIndPt() / (10d * headcount));

                                        // check that enough money are available at play warehouse
                                        if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= money) {

                                            // check that enough materials are available at regional warehouse
                                            if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= inPt) {

                                                // Everything set -- do increase
                                                getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, money);
                                                getParent().decTotGoods(ownerId, regionId, GOOD_INPT, inPt);

                                                // make sure we do not encounter an overflow
                                                if (battalion.getExperience() < 1) {
                                                    battalion.setExperience(1);
                                                }

                                                battalion.setExperience(battalion.getExperience() + 1);
                                                BattalionManager.getInstance().update(battalion);

                                                gTotMoney += money;
                                                gTotIndPt += inPt;

                                                totChanges++;

                                                explStr.append("battalion ");
                                                explStr.append(battalion.getOrder());
                                                explStr.append(" experience increased, ");

                                            } else {
                                                explStr.append("battalion ");
                                                explStr.append(battalion.getOrder());
                                                explStr.append(" experience not increased as not enough industrial points were available at regional warehouse, ");
                                            }

                                        } else {
                                            explStr.append("battalion ");
                                            explStr.append(battalion.getOrder());
                                            explStr.append(" experience not increased as not enough money available at empire's treasury, ");
                                        }

                                    } else {
                                        explStr.append("battalion ");
                                        explStr.append(battalion.getOrder());
                                        explStr.append(" experience not increased as maximum experience is already reached, ");
                                    }
                                }
                            }

                            // Check if at least 1 battalion was affected by the order
                            if (totChanges > 0) {
                                getOrder().setResult(totChanges);

                                // Update goods used by order
                                final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                usedGoods.put(GOOD_MONEY, gTotMoney);
                                usedGoods.put(GOOD_INPT, gTotIndPt);
                                usedGoods.put(GOOD_PEOPLE, gTotPeople);
                                usedGoods.put(GOOD_HORSE, gTotHorses);
                                getOrder().setUsedGoodsQnt(usedGoods);

                            } else {
                                getOrder().setResult(-1);

                                if (isUpgradeCrack) {
                                    explStr.append("brigade does not include any battalion eligible to get upgraded to Crack Elite  ");
                                } else {
                                    explStr.append("no battalion was allowed to increase experience  ");
                                }
                            }
                            if (explStr.length() > 0) {
                                getOrder().setExplanation(explStr.substring(0, explStr.length() - 2));
                            }

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("enemy forces located on the sector");
                        }

                    } else {
                        getOrder().setResult(-3);
                        getOrder().setExplanation("sector does not have a barrack");
                    }
                } else {
                    getOrder().setResult(-4);
                    getOrder().setExplanation("not owner of sector");
                }

            } else {
                getOrder().setResult(-5);
                getOrder().setExplanation("not owner of brigade");
            }
        }
    }
}
