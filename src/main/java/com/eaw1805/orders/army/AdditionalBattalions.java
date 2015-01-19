package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.ArmyTypeManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order - Setup Additional Battalions.
 * ticket:26.
 */
public class AdditionalBattalions
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(AdditionalBattalions.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_ADD_BATT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public AdditionalBattalions(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("AdditionalBattalions instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int brigadeId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source brigade
        final Brigade thisBrigade = BrigadeManager.getInstance().getByID(brigadeId);

        // Check ownership of source brigade
        if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();

            // Retrieve sector where brigade is positioned
            final Sector thisSector = SectorManager.getInstance().getByPosition(thisBrigade.getPosition());

            // Battalion must be positioned in owned sectors
            if (thisSector.getNation().getId() == ownerId) {
                final int regionId = thisSector.getPosition().getRegion().getId();

                // Check that sector has a Barrack
                if (thisSector.hasBarrack()) {

                    // Check if enemy brigades are present
                    if (enemyNotPresent(thisSector)) {

                        // check that maximum number of battalions is not reached
                        if (thisBrigade.getBattalions().size() < 7) {
                            final int battalionType = Integer.parseInt(getOrder().getParameter2());

                            // Retrieve Army type
                            final ArmyType battType = ArmyTypeManager.getInstance().getByIntID(battalionType, thisBrigade.getNation());

                            // Make sure that player is not trying to build elite troop type
                            if (battType.getElite()) {
                                report("AdditionalBattalions." + getOrder().getOrderId() + ".result", "failed - elite army types cannot be built");

                            } else {

                                // check if the particular battalion type can be trained in the region where the brigade is
                                if ((regionId == EUROPE) || (battType.canColonies())) {

                                    // Double Costs custom game option
                                    final int modifier = getGame().isDoubleCostsArmy() ? 2 : 1;

                                    // Check if this is not home region
                                    final int sphere = modifier * getSphere(thisSector, getOrder().getNation());

                                    // Calculate required amount of money
                                    final int money = battType.getCost() * sphere;

                                    // Calculate other materials
                                    final int inPt = battType.getIndPt() * sphere;

                                    // Determine the maximum headcount
                                    int people = 800;
                                    if (battType.getNation().getId() == NationConstants.NATION_MOROCCO
                                            || battType.getNation().getId() == NationConstants.NATION_OTTOMAN
                                            || battType.getNation().getId() == NationConstants.NATION_EGYPT) {
                                        people = 1000;
                                    }

                                    int horses = 0;
                                    if (battType.needsHorse()) {
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

                                                    // clean up slots
                                                    int order = 1;
                                                    for (Battalion oldBatt : thisBrigade.getBattalions()) {
                                                        oldBatt.setOrder(order++);
                                                    }

                                                    // Setup new battalion
                                                    final Battalion newBatt = new Battalion();
                                                    newBatt.setType(battType);
                                                    newBatt.setExperience(1);
                                                    newBatt.setHeadcount(people);
                                                    newBatt.setOrder(order);

                                                    final CarrierInfo emptyCarrierInfo = new CarrierInfo();
                                                    emptyCarrierInfo.setCarrierType(0);
                                                    emptyCarrierInfo.setCarrierId(0);
                                                    newBatt.setCarrierInfo(emptyCarrierInfo);

                                                    thisBrigade.getBattalions().add(newBatt);
                                                    BrigadeManager.getInstance().update(thisBrigade);

                                                    // Update goods used by order
                                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                                    usedGoods.put(GOOD_MONEY, money);
                                                    usedGoods.put(GOOD_INPT, inPt);
                                                    usedGoods.put(GOOD_PEOPLE, people);
                                                    usedGoods.put(GOOD_HORSE, horses);
                                                    getOrder().setUsedGoodsQnt(usedGoods);

                                                    getOrder().setResult(1);
                                                    getOrder().setExplanation("additional battalion trained for brigade " + thisBrigade.getName());

                                                } else {
                                                    getOrder().setResult(-1);
                                                    getOrder().setExplanation("not enough horses available at regional warehouse");
                                                }

                                            } else {
                                                getOrder().setResult(-2);
                                                getOrder().setExplanation("not enough people available at regional warehouse");
                                            }

                                        } else {
                                            getOrder().setResult(-3);
                                            getOrder().setExplanation("not enough industrial points available at regional warehouse");
                                        }

                                    } else {
                                        getOrder().setResult(-4);
                                        getOrder().setExplanation("not enough money available at empire treasury");
                                    }

                                } else {
                                    getOrder().setResult(-5);
                                    getOrder().setExplanation("army type cannot be built on colonies");
                                }
                            }

                        } else {
                            getOrder().setResult(-6);
                            getOrder().setExplanation("maximum number of battalions reached");
                        }

                    } else {
                        getOrder().setResult(-7);
                        getOrder().setExplanation("enemy forces located on the sector");
                    }

                } else {
                    getOrder().setResult(-8);
                    getOrder().setExplanation("sector does not have a barrack");
                }
            } else {
                getOrder().setResult(-9);
                getOrder().setExplanation("not owner of sector");
            }

        } else {
            getOrder().setResult(-10);
            getOrder().setExplanation("not owner of brigade");
        }
    }

}
