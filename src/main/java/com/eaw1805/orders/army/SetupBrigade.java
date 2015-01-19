package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.army.ArmyTypeManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Order - Setup Brigade.
 * ticket:25.
 */
public class SetupBrigade
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SetupBrigade.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_B_BATT;

    private final Map<Position, Sector> scenarioSectors = new HashMap<Position, Sector>();

    private final Game initGame;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public SetupBrigade(final OrderProcessor myParent) {
        super(myParent);
        initGame = GameManager.getInstance().getByID(-1);
        for (final Sector sector : SectorManager.getInstance().listByGame(initGame)) {
            scenarioSectors.put(sector.getPosition(), sector);
        }

        LOGGER.debug("SetupBrigade instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int sectorId = Integer.parseInt(getOrder().getParameter1());
        final int[] battTypes = new int[7];
        battTypes[0] = Integer.parseInt(getOrder().getParameter2());
        battTypes[1] = Integer.parseInt(getOrder().getParameter3());
        battTypes[2] = Integer.parseInt(getOrder().getParameter4());
        battTypes[3] = Integer.parseInt(getOrder().getParameter5());
        battTypes[4] = Integer.parseInt(getOrder().getParameter6());
        battTypes[5] = Integer.parseInt(getOrder().getParameter7());

        final String name = getOrder().getParameter9();

        final List<ArmyType> lstTypes = new ArrayList<ArmyType>();

        // Retrieve the Sector
        final Sector thisSector = SectorManager.getInstance().getByID(sectorId);

        if (thisSector == null) {
            getOrder().setResult(-12);
            getOrder().setExplanation("cannot find sector");
            return;
        }

        // Update order's region of effect
        getOrder().setRegion(thisSector.getPosition().getRegion());

        // Check ownership of source brigade
        if (thisSector.getNation().getId() == getOrder().getNation().getId()) {
            final Position thePos = (Position) thisSector.getPosition().clone();
            thePos.setGame(initGame);
            final int ownerId = getOrder().getNation().getId();
            final int regionId = thisSector.getPosition().getRegion().getId();

            // Check that sector has a Barrack
            if (thisSector.hasBarrack()) {

                // Check if enemy brigades are present
                if (enemyNotPresent(thisSector)) {
                    int money = 0;
                    int inPt = 0;
                    int horses = 0;
                    int people = 0;

                    // Double Costs custom game option
                    final int modifier = getGame().isDoubleCostsArmy() ? 2 : 1;

                    // Check if this is not home region
                    final int sphere = modifier * getSphere(thisSector, getOrder().getNation());

                    // Calculate total cost for battalion types requested
                    for (int slot = 0; slot < 6; slot++) {
                        final int thisBatType = battTypes[slot];
                        if (thisBatType > 0) {
                            // Retrieve Army type
                            final ArmyType battType = ArmyTypeManager.getInstance().getByIntID(thisBatType, getOrder().getNation());

                            // check if battalion type exists
                            if (battType == null) {
                                getOrder().setResult(-12);
                                getOrder().setExplanation("unknown battalion type");
                                money = -1;
                                break;

                            } else if ((slot < 4) && (battType.getCrack())) {
                                // Check if battalion type is allowed to be built
                                // Check if this is a crack type
                                getOrder().setResult(-1);
                                getOrder().setExplanation("cannot build crack battalion types in slots 1..4");
                                money = -1;
                                break;

                            } else if ((battType.getCrack()) && (getSphere(thisSector, getOrder().getNation()) == 3)) {
                                // Check if this is not home region
                                getOrder().setResult(-2);
                                getOrder().setExplanation("cannot build crack battalion types outside home region");
                                money = -1;
                                break;

                            } else if (battType.getElite()) {
                                // Check if this is an elite type
                                getOrder().setResult(-3);
                                getOrder().setExplanation("cannot build elite battalion types");
                                money = -1;
                                break;

                            } else if (battType.getRegion() != null && regionId != battType.getRegion().getId()) {
                                // check if the particular battalion type can be trained in the region where the brigade is
                                getOrder().setResult(-4);
                                getOrder().setExplanation("army type cannot be built on this region");
                                money = -1;
                                break;

                            } else {
                                lstTypes.add(battType);

                                // Calculate required amount of money
                                money += battType.getCost() * sphere;

                                // Calculate required amount of materials
                                inPt += battType.getIndPt() * sphere;

                                int headcount = 800;
                                if (battType.getNation().getId() == NationConstants.NATION_MOROCCO
                                        || battType.getNation().getId() == NationConstants.NATION_OTTOMAN
                                        || battType.getNation().getId() == NationConstants.NATION_EGYPT) {
                                    headcount = 1000;
                                }

                                people += headcount;

                                if (battType.needsHorse()) {
                                    horses += headcount;
                                }
                            }
                        }
                    }

                    if (money > 0) {
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

                                        // Setup new brigade
                                        final Brigade newBrig = new Brigade();
                                        newBrig.setPosition(thisSector.getPosition());
                                        final FieldBattlePosition fbPosition = new FieldBattlePosition();
                                        fbPosition.setX(0);
                                        fbPosition.setY(0);
                                        fbPosition.setPlaced(false);
                                        newBrig.setFieldBattlePosition(fbPosition);


                                        if (name.length() > 0) {
                                            newBrig.setName(name);

                                        } else {
                                            newBrig.setName("Brigade " + getOrder().getOrderId());
                                        }

                                        newBrig.setNation(getOrder().getNation());
                                        newBrig.setBattalions(new HashSet<Battalion>()); // NOPMD

                                        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
                                        emptyCarrierInfo.setCarrierType(0);
                                        emptyCarrierInfo.setCarrierId(0);

                                        // Setup battalions
                                        int order = 1;
                                        for (final ArmyType thisType : lstTypes) {
                                            final Battalion newBatt = new Battalion();
                                            newBatt.setType(thisType);
                                            newBatt.setExperience(1);

                                            int headcount = 800;
                                            if (thisType.getNation().getId() == NationConstants.NATION_MOROCCO
                                                    || thisType.getNation().getId() == NationConstants.NATION_OTTOMAN
                                                    || thisType.getNation().getId() == NationConstants.NATION_EGYPT) {
                                                headcount = 1000;
                                            }

                                            newBatt.setHeadcount(headcount);
                                            newBatt.setOrder(order++);
                                            newBatt.setCarrierInfo(emptyCarrierInfo);
                                            newBrig.getBattalions().add(newBatt);
                                        }

                                        // update movement points of brigade
                                        newBrig.updateMP();
                                        BrigadeManager.getInstance().add(newBrig);

                                        // Update goods used by order
                                        final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                        usedGoods.put(GOOD_MONEY, money);
                                        usedGoods.put(GOOD_INPT, inPt);
                                        usedGoods.put(GOOD_PEOPLE, people);
                                        usedGoods.put(GOOD_HORSE, horses);
                                        getOrder().setUsedGoodsQnt(usedGoods);

                                        getOrder().setResult(1);
                                        getOrder().setExplanation("Brigade [" + name + "] setup");

                                    } else {
                                        getOrder().setResult(-5);
                                        getOrder().setExplanation("not enough horses available at regional warehouse");
                                    }

                                } else {
                                    getOrder().setResult(-6);
                                    getOrder().setExplanation("not enough people available at regional warehouse");
                                }

                            } else {
                                getOrder().setResult(-7);
                                getOrder().setExplanation("not enough industrial points available at regional warehouse");
                            }

                        } else {
                            getOrder().setResult(-8);
                            getOrder().setExplanation("not enough money available at empire treasury");
                        }

                    }

                } else {
                    getOrder().setResult(-9);
                    getOrder().setExplanation("enemy forces located on the sector");
                }

            } else {
                getOrder().setResult(-10);
                getOrder().setExplanation("sector does not have a barrack");
            }
        } else {
            getOrder().setResult(-11);
            getOrder().setExplanation("not owner of sector");
        }
    }
}
