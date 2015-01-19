package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.army.Army;
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
 * Increase the head count of all brigades that are part of an Army.
 */
public class IncreaseHeadcountArmy extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(IncreaseHeadcountArmy.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_INC_HEADCNT_ARMY;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public IncreaseHeadcountArmy(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("IncreaseHeadcount instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int armyId = Integer.parseInt(getOrder().getParameter1());
        final int newSoldiers = Integer.parseInt(getOrder().getParameter2());

        if (newSoldiers < 1) {
            getOrder().setResult(-5);
            getOrder().setExplanation("must order at least 1 soldier to train");
            return;
        }

        int gTotMoney = 0, gTotIndPt = 0, gTotPeople = 0, gTotHorses = 0;

        // Retrieve the army
        Army thisArmy = ArmyManager.getInstance().getByID(armyId);
        if (thisArmy == null && !getParent().armyAssocExists(armyId)) {

            getOrder().setResult(-6);
            getOrder().setExplanation("cannot locate Army");

        } else {
            // Check if this is a newly created army
            if (getParent().armyAssocExists(armyId)) {
                thisArmy = ArmyManager.getInstance().getByID(getParent().retrieveArmyAssoc(armyId));
            }

            // Check ownership of source brigade
            if (thisArmy.getNation().getId() == getOrder().getNation().getId()) {
                final int ownerId = getOrder().getNation().getId();

                // Retrieve sector where brigade is positioned
                final Sector thisSector = SectorManager.getInstance().getByPosition(thisArmy.getPosition());

                // Update order's region of effect
                getOrder().setRegion(thisSector.getPosition().getRegion());

                // Double Costs custom game option
                final int modifier = getGame().isDoubleCostsArmy() ? 2 : 1;

                // Determine if inside home region, sphere of influence, or outside
                final int sphere = modifier * getSphere(thisSector, getOrder().getNation());

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
                            for (Corp thisCorp : thisArmy.getCorps()) {
                                for (Brigade thisBrigade : thisCorp.getBrigades()) {
                                    for (final Battalion battalion : thisBrigade.getBattalions()) {
                                        // Determine the maximum headcount
                                        int headcount = 800;
                                        if (battalion.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                                                || battalion.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                                                || battalion.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                                            headcount = 1000;
                                        }

                                        // check if maximum has been reached
                                        if (battalion.getHeadcount() < headcount) {
                                            // calculate number of soldiers that will be trained
                                            int trainSoldiers = newSoldiers;

                                            // check if maximum size reached
                                            if (battalion.getHeadcount() + trainSoldiers > headcount) {
                                                trainSoldiers = headcount - battalion.getHeadcount();
                                            }

                                            // check if citizens are available at regional warehouse
                                            if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) < trainSoldiers) {
                                                trainSoldiers = getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE);
                                            }

                                            // Make sure that there are citizens to train
                                            if (trainSoldiers > 0) {

                                                // Calculate required amount of money
                                                final int money = sphere * trainSoldiers * battalion.getType().getCost() / headcount;
                                                final int inPt = sphere * trainSoldiers * battalion.getType().getIndPt() / headcount;
                                                int horses = 0;
                                                if (battalion.getType().needsHorse()) {
                                                    horses = trainSoldiers;
                                                }

                                                // check that enough money are available at play warehouse
                                                if (getParent().getTotGoods(ownerId, EUROPE, GOOD_MONEY) >= money) {

                                                    // check that enough industrial points are available at regional warehouse
                                                    if (getParent().getTotGoods(ownerId, regionId, GOOD_INPT) >= inPt) {

                                                        // check that enough industrial points are available at regional warehouse
                                                        if (getParent().getTotGoods(ownerId, regionId, GOOD_HORSE) >= horses) {

                                                            // Everything set -- do increase
                                                            getParent().decTotGoods(ownerId, EUROPE, GOOD_MONEY, money);
                                                            getParent().decTotGoods(ownerId, regionId, GOOD_INPT, inPt);
                                                            getParent().decTotGoods(ownerId, regionId, GOOD_HORSE, horses);
                                                            getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, trainSoldiers);

                                                            gTotMoney += money;
                                                            gTotIndPt += inPt;
                                                            gTotHorses += horses;
                                                            gTotPeople += trainSoldiers;

                                                            String experienceDrop = "";

                                                            // Check if experience level will be reduced
                                                            if (trainSoldiers > battalion.getHeadcount()) {
                                                                // drop by 2 since more than 100% increase
                                                                battalion.setExperience(battalion.getExperience() - 2);

                                                                experienceDrop = ", experience of battalion dropped by 2";

                                                            } else if (trainSoldiers > 0.5d * battalion.getHeadcount()) {
                                                                // drop by 1 since more than 50% increase
                                                                battalion.setExperience(battalion.getExperience() - 1);

                                                                experienceDrop = ", experience of battalion dropped by 1";
                                                            }

                                                            // check if zero or negative experience reached
                                                            if (battalion.getExperience() < 1) {
                                                                battalion.setExperience(1);
                                                            }

                                                            // make sure we do not encounter an overflow
                                                            if (battalion.getExperience() > battalion.getType().getMaxExp() + 2) {
                                                                battalion.setExperience(battalion.getType().getMaxExp() + 2);
                                                            }

                                                            // increase headcount
                                                            battalion.setHeadcount(battalion.getHeadcount() + trainSoldiers);
                                                            BattalionManager.getInstance().update(battalion);

                                                            // keep count of battalions that changed
                                                            totChanges++;

                                                            explStr.append("battalion ");
                                                            explStr.append(battalion.getOrder());
                                                            explStr.append(" headcount increased by ");
                                                            explStr.append(Integer.toString(trainSoldiers));
                                                            explStr.append(experienceDrop);
                                                            explStr.append(", ");

                                                        } else {
                                                            explStr.append("battalion ");
                                                            explStr.append(battalion.getOrder());
                                                            explStr.append(" headcount did not increase because not enough horses were available at regional warehouse, ");
                                                        }

                                                    } else {
                                                        explStr.append("battalion ");
                                                        explStr.append(battalion.getOrder());
                                                        explStr.append(" headcount did not increase because not enough industrial points were available at regional warehouse, ");
                                                    }

                                                } else {
                                                    explStr.append("battalion ");
                                                    explStr.append(battalion.getOrder());
                                                    explStr.append(" headcount did not increase because not enough money were available at empire's treasury, ");
                                                }

                                            } else {
                                                explStr.append("battalion ");
                                                explStr.append(battalion.getOrder());
                                                explStr.append(" headcount did not increase because not enough citizens were available at regional warehouse, ");
                                            }

                                        } else {
                                            explStr.append("battalion ");
                                            explStr.append(battalion.getOrder());
                                            explStr.append(" headcount did not increase because maximum size of 800 has been reached, ");
                                        }
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
                            }
                            getOrder().setExplanation(explStr.substring(0, explStr.length() - 2));

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
                getOrder().setExplanation("not owner of army");
            }
        }
    }

}
