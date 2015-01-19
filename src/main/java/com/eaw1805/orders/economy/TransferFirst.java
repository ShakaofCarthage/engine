package com.eaw1805.orders.economy;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TradeCalculations;
import com.eaw1805.data.constants.TradeConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.Good;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.fleet.Ship;
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
import java.util.Map;

/**
 * Order for Loading Goods to Baggage Trains.
 * ticket #35.
 */
public class TransferFirst
        extends AbstractOrderProcessor
        implements OrderInterface, GoodConstants, RegionConstants, ArmyConstants, TradeConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TransferFirst.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_EXCHF;

    /**
     * Captures if the Random Event Trade Deficit is in effect.
     */
    private final transient int tradeDeficit;

    /**
     * Captures if the Random Event Trade Surplus is in effect.
     */
    private final transient int tradeSurplus;

    /**
     * Keep track of the nation that is first to trade with each trade city.
     */
    private final transient Map<Integer, Integer> tradeFirst;

    private final NumberFormat formatter = new DecimalFormat("#,###,###");

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public TransferFirst(final OrderProcessor myParent) {
        super(myParent);

        tradeFirst = new HashMap<Integer, Integer>();

        // Random Event: Workers Strike, Natives Raid
        final Nation freeNation = NationManager.getInstance().getByID(NATION_NEUTRAL);
        tradeDeficit = retrieveReportAsInt(freeNation, myParent.getGame().getTurn(), RE_DEFI);
        tradeSurplus = retrieveReportAsInt(freeNation, myParent.getGame().getTurn(), RE_SURP);

        LOGGER.debug("TransferFirst instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int sourceTPE = Integer.parseInt(getOrder().getParameter1());
        final int sourceID = Integer.parseInt(getOrder().getParameter2());
        final int targetTPE = Integer.parseInt(getOrder().getParameter3());
        final int targetID = Integer.parseInt(getOrder().getParameter4());
        final int tpe = Integer.parseInt(getOrder().getParameter5());
        int quantity = Integer.parseInt(getOrder().getParameter6());

        if (tpe == 0) {
            getOrder().setResult(-1);
            getOrder().setExplanation("invalid good type");
            return;
        }

        if (quantity < 0) {
            getOrder().setResult(-2);
            getOrder().setExplanation("invalid quantity");
            return;
        }

        switch (targetTPE) {
            case BARRACK:

                switch (sourceTPE) {
                    case BAGGAGETRAIN: {
                        // Transfer from Baggage train to Warehouse
                        // Retrieve Baggage Train
                        final BaggageTrain btrain = BaggageTrainManager.getInstance().getByID(sourceID);
                        if (btrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("cannot locate entity");

                        } else {
                            // Check that baggage train is indeed in a barrack
                            final Sector thisPos = SectorManager.getInstance().getByPosition(btrain.getPosition());
                            if (!thisPos.hasBarrack()) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");

                            } else if (targetID == NATION_NEUTRAL) {
                                // Check ownership
                                getOrder().setResult(-5);
                                getOrder().setExplanation("cannot unload goods to neutrals");

                            } else {

                                // Retrieve baggage train materials
                                final Map<Integer, Integer> storedGoods = btrain.getStoredGoods();

                                // locate regional warehouse
                                final int regionId;
                                if (tpe == GOOD_MONEY) {
                                    regionId = EUROPE;

                                } else {
                                    regionId = btrain.getPosition().getRegion().getId();
                                }

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (storedGoods.get(tpe) < quantity) {
                                    quantity = storedGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (storedGoods.get(tpe) >= quantity && quantity > 0) {
                                    // Apply transfer fees
                                    final int transferredQuantity = applyTransferFees(tpe, quantity,
                                            NationManager.getInstance().getByID(targetID),
                                            btrain.getPosition().getRegion());

                                    // Increase warehouse materials
                                    getParent().incTotGoods(targetID, regionId, tpe, transferredQuantity);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(tpe, -1 * quantity);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    getOrder().setRegion(RegionManager.getInstance().getByID(regionId));

                                    // Reduce baggage train materials
                                    storedGoods.put(tpe, storedGoods.get(tpe) - quantity);
                                    BaggageTrainManager.getInstance().update(btrain);

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);

                                    // Check if this is foreign aid
                                    if (targetID != btrain.getNation().getId()) {
                                        // Add news entry
                                        newsPair(getOrder().getNation(), NationManager.getInstance().getByID(targetID),
                                                NEWS_ECONOMY,
                                                "We delivered " + formatter.format(transferredQuantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> as foreign aid to " + NationManager.getInstance().getByID(targetID).getName(),
                                                "We received foreign aid of " + formatter.format(transferredQuantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> from " + getOrder().getNation().getName());

                                        if (tpe == GOOD_MONEY) {
                                            final int vpCharge = quantity / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        NationManager.getInstance().getByID(targetID),
                                                        vpCharge * TRANS_MONEY,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-1.png' title='Money' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }

                                        } else {
                                            final int worth = quantity * thisGood.getGoodFactor();
                                            final int vpCharge = worth / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        NationManager.getInstance().getByID(targetID),
                                                        vpCharge * TRANS_GOODS,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' title='" + thisGood.getName() + "' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        }

                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + btrain.getName() + " to the warehouse of " + NationManager.getInstance().getByID(targetID).getName() + getOrder().getExplanation());
                                    } else {
                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + btrain.getName());
                                    }

                                    getOrder().setTemp8(Integer.toString(quantity));
                                    getOrder().setResult(1);

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in baggage train");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                        }
                        break;
                    }

                    case SHIP:
                    default: {
                        // Transfer from Ship to Warehouse
                        // Retrieve Ship
                        final Ship mship = ShipManager.getInstance().getByID(sourceID);
                        if (mship == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("cannot locate entity");

                        } else if (mship.getNation().getId() != getOrder().getNation().getId()
                                || (mship.getCapturedByNation() != 0)) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("not owner of entity");

                        } else {
                            // Check that baggage train is indeed in a barrack
                            final Sector thisPos = SectorManager.getInstance().getByPosition(mship.getPosition());
                            if (!thisPos.hasBarrack()) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");

                                // Check ownership
                            } else if (targetID == NATION_NEUTRAL) {
                                getOrder().setResult(-5);
                                getOrder().setExplanation("cannot unload goods to neutrals");

                            } else {

                                // locate regional warehouse
                                final int regionId;
                                if (tpe == GOOD_MONEY) {
                                    regionId = EUROPE;

                                } else {
                                    regionId = mship.getPosition().getRegion().getId();
                                }

                                // Retrieve ship materials
                                final Map<Integer, Integer> storedGoods = mship.getStoredGoods();

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (storedGoods.get(tpe) < quantity) {
                                    quantity = storedGoods.get(tpe);
                                }

                                final Good thisGood = GoodManager.getInstance().getByID(tpe);

                                // Make sure that enough goods are available
                                if (storedGoods.get(tpe) >= quantity && quantity > 0) {
                                    // Apply transfer fees
                                    final int transferredQuantity = applyTransferFees(tpe, quantity,
                                            NationManager.getInstance().getByID(targetID),
                                            mship.getPosition().getRegion());

                                    // Increase warehouse materials
                                    getParent().incTotGoods(targetID, regionId, tpe, transferredQuantity);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(tpe, -1 * quantity);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    getOrder().setRegion(RegionManager.getInstance().getByID(regionId));

                                    // Reduce ship materials
                                    storedGoods.put(tpe, storedGoods.get(tpe) - quantity);
                                    ShipManager.getInstance().update(mship);

                                    // Check if this is foreign aid
                                    if (targetID != mship.getNation().getId()) {
                                        // Add news entry
                                        newsPair(getOrder().getNation(), NationManager.getInstance().getByID(targetID),
                                                NEWS_ECONOMY,
                                                "We delivered " + formatter.format(transferredQuantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> as foreign aid to " + NationManager.getInstance().getByID(targetID).getName(),
                                                "We received foreign aid of " + formatter.format(transferredQuantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> from " + getOrder().getNation().getName());

                                        if (tpe == GOOD_MONEY) {
                                            final int vpCharge = quantity / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        NationManager.getInstance().getByID(targetID),
                                                        vpCharge * TRANS_MONEY,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-1.png' title='Money' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }

                                        } else {
                                            final int worth = quantity * thisGood.getGoodFactor();
                                            final int vpCharge = worth / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        NationManager.getInstance().getByID(targetID),
                                                        vpCharge * TRANS_GOODS,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' title='" + thisGood.getName() + "' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        }

                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + mship.getName() + " to the warehouse of " + NationManager.getInstance().getByID(targetID).getName() + getOrder().getExplanation());

                                    } else {
                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + mship.getName());
                                    }

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in merchant ship");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                        }
                        break;
                    }

                }
                break;

            case WAREHOUSE:

                switch (sourceTPE) {
                    case TRADECITY: {
                        // Buy Goods from Trade City
                        final int money = buyWarehouseFromTradeCity(targetID, tpe, quantity, sourceID);
                        if (money < 1) {
                            getOrder().setTemp8(Integer.toString(0));
                            getOrder().setTemp9("0");
                            getOrder().setResult(-1);
                            getOrder().setExplanation("not enough money available");

                        } else {
                            getOrder().setTemp9(Integer.toString(money));
                            getOrder().setResult(1);
                            getOrder().setExplanation("goods bought from trade city");
                        }
                        break;
                    }

                    case BAGGAGETRAIN: {
                        // Transfer from Baggage train to Warehouse
                        // Retrieve Baggage Train
                        final BaggageTrain btrain = BaggageTrainManager.getInstance().getByID(sourceID);
                        if (btrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("cannot locate entity");
                            getOrder().setTemp8(Integer.toString(0));
                            getOrder().setTemp9(Integer.toString(0));

                        } else if (btrain.getNation().getId() != getOrder().getNation().getId()) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("not owner of entity");
                            getOrder().setTemp8(Integer.toString(0));
                            getOrder().setTemp9(Integer.toString(0));

                        } else {
                            final Sector thisSector = SectorManager.getInstance().getByPosition(btrain.getPosition());
                            final int ownerId = thisSector.getNation().getId();

                            // Check that baggage train is indeed in a barrack
                            if (!thisSector.hasBarrack()) {
                                getOrder().setResult(-6);
                                getOrder().setTemp8(Integer.toString(0));
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setExplanation("transport is in wrong sector");

                            } else if (ownerId == NATION_NEUTRAL) {
                                getOrder().setResult(-5);
                                getOrder().setTemp8(Integer.toString(0));
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setExplanation("cannot unload goods to neutrals");

                            } else {

                                // Retrieve baggage train materials
                                final Map<Integer, Integer> storedGoods = btrain.getStoredGoods();

                                // locate regional warehouse
                                final int regionId;
                                if (tpe == GOOD_MONEY) {
                                    regionId = EUROPE;

                                } else {
                                    regionId = btrain.getPosition().getRegion().getId();
                                }

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (storedGoods.get(tpe) < quantity) {
                                    quantity = storedGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (storedGoods.get(tpe) >= quantity && quantity > 0) {
                                    // Increase warehouse materials
                                    getParent().incTotGoods(ownerId, regionId, tpe, quantity);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(tpe, -1 * quantity);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    getOrder().setRegion(RegionManager.getInstance().getByID(regionId));

                                    // Reduce baggage train materials
                                    storedGoods.put(tpe, storedGoods.get(tpe) - quantity);
                                    BaggageTrainManager.getInstance().update(btrain);

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);

                                    // Check if this is foreign aid
                                    if (ownerId != btrain.getNation().getId()) {
                                        // Add news entry
                                        newsPair(btrain.getNation(), thisSector.getNation(),
                                                NEWS_ECONOMY,
                                                "We delivered " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> as foreign aid to " + thisSector.getNation().getName(),
                                                "We received foreign aid of " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> from " + btrain.getNation().getName());

                                        if (tpe == GOOD_MONEY) {
                                            final int vpCharge = quantity / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        thisSector.getNation(),
                                                        vpCharge * TRANS_MONEY,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-1.png' title='Money' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }

                                        } else {
                                            final int worth = quantity * thisGood.getGoodFactor();
                                            final int vpCharge = worth / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        thisSector.getNation(),
                                                        vpCharge * TRANS_GOODS,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' title='" + thisGood.getName() + "' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        }

                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + btrain.getName() + " to the warehouse of " + thisSector.getNation().getName() + getOrder().getExplanation());

                                    } else {
                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + btrain.getName());
                                    }

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in baggage train");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }

                        }
                        break;
                    }

                    case SHIP:
                    default: {
                        // Transfer from Ship to Warehouse
                        // Retrieve Baggage Train
                        final Ship mship = ShipManager.getInstance().getByID(sourceID);
                        if (mship == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("cannot locate entity");
                            getOrder().setTemp8(Integer.toString(0));
                            getOrder().setTemp9(Integer.toString(0));

                        } else if (mship.getNation().getId() != getOrder().getNation().getId()
                                || (mship.getCapturedByNation() != 0)) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("not owner of entity");
                            getOrder().setTemp8(Integer.toString(0));
                            getOrder().setTemp9(Integer.toString(0));

                        } else {

                            // Use sector's owner ID -- in case we are unloading at another nation's warehouse
                            final Sector thisSector = SectorManager.getInstance().getByPosition(mship.getPosition());
                            final int ownerId = thisSector.getNation().getId();

                            // Check that ship is indeed in a barrack
                            if (!thisSector.hasBarrack()) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp8(Integer.toString(0));
                                getOrder().setTemp9(Integer.toString(0));

                            } else if (ownerId == NATION_NEUTRAL) {
                                getOrder().setResult(-5);
                                getOrder().setExplanation("cannot unload goods to neutrals");
                                getOrder().setTemp8(Integer.toString(0));
                                getOrder().setTemp9(Integer.toString(0));

                            } else {

                                // locate regional warehouse
                                final int regionId;
                                if (tpe == GOOD_MONEY) {
                                    regionId = EUROPE;

                                } else {
                                    regionId = mship.getPosition().getRegion().getId();
                                }

                                // Retrieve ship materials
                                final Map<Integer, Integer> storedGoods = mship.getStoredGoods();

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (storedGoods.get(tpe) < quantity) {
                                    quantity = storedGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (storedGoods.get(tpe) >= quantity && quantity > 0) {
                                    // Increase warehouse materials
                                    getParent().incTotGoods(ownerId, regionId, tpe, quantity);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(tpe, -1 * quantity);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    getOrder().setRegion(RegionManager.getInstance().getByID(regionId));

                                    // Reduce ship materials
                                    storedGoods.put(tpe, storedGoods.get(tpe) - quantity);
                                    ShipManager.getInstance().update(mship);

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);

                                    // Check if this is foreign aid
                                    if (ownerId != mship.getNation().getId()) {
                                        // Add news entry
                                        newsPair(mship.getNation(), thisSector.getNation(),
                                                NEWS_ECONOMY,
                                                "We delivered " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> as foreign aid to " + thisSector.getNation().getName(),
                                                "We received foreign aid of " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> from " + mship.getNation().getName());

                                        if (tpe == GOOD_MONEY) {
                                            final int vpCharge = quantity / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        thisSector.getNation(),
                                                        vpCharge * TRANS_MONEY,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-1.png' title='Money' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }

                                        } else {
                                            final int worth = quantity * thisGood.getGoodFactor();
                                            final int vpCharge = worth / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        thisSector.getNation(),
                                                        vpCharge * TRANS_GOODS,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' title='" + thisGood.getName() + "' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        }

                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + mship.getName() + " to the warehouse of " + thisSector.getNation().getName() + getOrder().getExplanation());

                                    } else {
                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> unloaded from " + mship.getName());
                                    }

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in merchant ship");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                        }
                        break;
                    }

                }
                break;


            case TRADECITY: {

                switch (sourceTPE) {
                    case WAREHOUSE: {
                        // Sell Goods To Trade City
                        final int money = sellWarehouseToTradeCity(sourceID, tpe, quantity, targetID);
                        if (money > 0) {
                            getOrder().setTemp9(Integer.toString(money));
                            getOrder().setTemp8(Integer.toString(quantity));
                            getOrder().setResult(1);

                        } else {
                            getOrder().setResult(-4);
                            getOrder().setExplanation("not enough materials are available in warehouse");
                            getOrder().setTemp9(Integer.toString(0));
                            getOrder().setTemp8(Integer.toString(0));
                        }
                        break;
                    }

                    case BAGGAGETRAIN: {
                        // Baggage Train Sell Goods to Trade City
                        // Retrieve Baggage Train
                        final BaggageTrain btrain = BaggageTrainManager.getInstance().getByID(sourceID);
                        if (btrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("cannot locate entity");
                            getOrder().setTemp9(Integer.toString(0));
                            getOrder().setTemp8(Integer.toString(0));

                        } else {
                            final TradeCity city = TradeCityManager.getInstance().getByPosition(btrain.getPosition());

                            // Check that ship is indeed in a barrack
                            if (city == null) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is not in a trade city");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (city.getCityId() != targetID) {
                                // Check if this is the correct trade city
                                getOrder().setResult(-7);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                                // Check ownership
                            } else if (btrain.getNation().getId() == getOrder().getNation().getId()) {
                                final int money = sellBaggageTrainToTradeCity(tpe, quantity, btrain, targetID);
                                if (money > 0) {
                                    getOrder().setTemp9(Integer.toString(money));
                                    getOrder().setResult(1);

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in baggage train");
                                    getOrder().setTemp9(Integer.toString(0));
                                    getOrder().setTemp8(Integer.toString(0));
                                }

                            } else {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not owner of entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));
                            }
                        }
                        break;
                    }

                    case SHIP:
                    default: {
                        // Ship Sell Goods to Trade City
                        // Retrieve Merchant Ship
                        final Ship mship = ShipManager.getInstance().getByID(sourceID);
                        if (mship == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("cannot locate entity");
                            getOrder().setTemp9(Integer.toString(0));
                            getOrder().setTemp8(Integer.toString(0));

                        } else if (mship.getNation().getId() != getOrder().getNation().getId()
                                || (mship.getCapturedByNation() != 0)) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("not owner of entity");
                            getOrder().setTemp9(Integer.toString(0));
                            getOrder().setTemp8(Integer.toString(0));

                        } else {
                            final TradeCity city = TradeCityManager.getInstance().getByPosition(mship.getPosition());

                            // Check that ship is indeed in a city
                            if (city == null) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is not in a trade city");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (city.getCityId() != targetID) {
                                // Check if this is the correct trade city
                                getOrder().setResult(-7);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (mship.getNation().getId() == getOrder().getNation().getId()
                                    && (mship.getCapturedByNation() == 0)) {
                                // Check ownership
                                final int money = sellShipToTradeCity(tpe, quantity, mship, targetID);
                                if (money > 0) {
                                    getOrder().setTemp9(Integer.toString(money));
                                    getOrder().setResult(1);

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in merchant ship");
                                }

                            } else {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not owner of entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));
                            }
                        }
                        break;
                    }
                }

                break;
            }

            case BAGGAGETRAIN: {
                // Retrieve Baggage Train
                final BaggageTrain btrain = BaggageTrainManager.getInstance().getByID(targetID);
                if (btrain == null) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("cannot locate entity");
                    getOrder().setTemp9(Integer.toString(0));
                    getOrder().setTemp8(Integer.toString(0));

                } else {

                    switch (sourceTPE) {
                        case BAGGAGETRAIN: {
                            // Transfer from ship to baggage train

                            // Retrieve Ship
                            final BaggageTrain btrainSource = BaggageTrainManager.getInstance().getByID(sourceID);
                            if (btrainSource == null) {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("cannot locate entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (btrainSource.getNation().getId() != getOrder().getNation().getId()) {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not owner of entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (!btrainSource.getPosition().equals(btrain.getPosition())) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {
                                // Retrieve train materials
                                final Map<Integer, Integer> trainGoods = btrain.getStoredGoods();

                                // Retrieve 2nd train materials
                                final Map<Integer, Integer> sourceGoods = btrainSource.getStoredGoods();

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (sourceGoods.get(tpe) < quantity) {
                                    quantity = sourceGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (sourceGoods.get(tpe) >= quantity && quantity > 0) {

                                    getOrder().setRegion(btrainSource.getPosition().getRegion());

                                    // Reduce 2nd train materials
                                    sourceGoods.put(tpe, sourceGoods.get(tpe) - quantity);
                                    BaggageTrainManager.getInstance().update(btrainSource);

                                    // increase baggage train materials
                                    trainGoods.put(tpe, trainGoods.get(tpe) + quantity);
                                    BaggageTrainManager.getInstance().update(btrain);

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));
                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);
                                    getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> transferred from " + btrainSource.getName() + " to " + btrain.getName());

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in baggage train");
                                    getOrder().setTemp9(Integer.toString(0));
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;
                        }

                        case SHIP: {
                            // Transfer from ship to baggage train

                            // Retrieve Ship
                            final Ship mship = ShipManager.getInstance().getByID(sourceID);
                            if (mship == null) {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("cannot locate entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (mship.getNation().getId() != getOrder().getNation().getId()
                                    || (mship.getCapturedByNation() != 0)) {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not owner of entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (!mship.getPosition().equals(btrain.getPosition())) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {
                                // Retrieve ship materials
                                final Map<Integer, Integer> trainGoods = btrain.getStoredGoods();

                                // Retrieve ship materials
                                final Map<Integer, Integer> shipGoods = mship.getStoredGoods();

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (shipGoods.get(tpe) < quantity) {
                                    quantity = shipGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (shipGoods.get(tpe) >= quantity && quantity > 0) {

                                    getOrder().setRegion(mship.getPosition().getRegion());

                                    // Reduce ship materials
                                    shipGoods.put(tpe, shipGoods.get(tpe) - quantity);
                                    ShipManager.getInstance().update(mship);

                                    // increase baggage train materials
                                    trainGoods.put(tpe, trainGoods.get(tpe) + quantity);
                                    BaggageTrainManager.getInstance().update(btrain);

                                    getOrder().setResult(1);
                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);
                                    getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> transferred from " + mship.getName() + " to " + btrain.getName());
                                    getOrder().setTemp8(Integer.toString(quantity));

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in ship");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;
                        }

                        case WAREHOUSE:
                            // Load from warehouse
                            final Sector thisSector = SectorManager.getInstance().getByPosition(btrain.getPosition());
                            final int ownerId = thisSector.getNation().getId();

                            // Check that ship is indeed in a barrack
                            if (!thisSector.hasBarrack()) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (ownerId == NATION_NEUTRAL) {
                                getOrder().setResult(-5);
                                getOrder().setExplanation("cannot load goods from neutrals");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {

                                // locate regional warehouse
                                final int regionId;
                                if (tpe == GOOD_MONEY) {
                                    regionId = EUROPE;
                                } else {
                                    regionId = btrain.getPosition().getRegion().getId();
                                }

                                // Make sure that enough goods are available
                                if (getParent().getTotGoods(ownerId, regionId, tpe) >= quantity) {
                                    // Reduce warehouse materials
                                    getParent().decTotGoods(ownerId, regionId, tpe, quantity);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(tpe, quantity);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    getOrder().setRegion(RegionManager.getInstance().getByID(regionId));

                                    // Retrieve baggage train materials
                                    final Map<Integer, Integer> storedGoods = btrain.getStoredGoods();

                                    // Increase baggage train materials
                                    storedGoods.put(tpe, quantity + storedGoods.get(tpe));
                                    BaggageTrainManager.getInstance().update(btrain);

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);

                                    // Check if this is foreign aid
                                    if (ownerId != btrain.getNation().getId()) {
                                        // Add news entry
                                        newsPair(getOrder().getNation(), btrain.getNation(),
                                                NEWS_ECONOMY,
                                                "We delivered " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> as foreign aid to " + btrain.getNation().getName(),
                                                "We received foreign aid of " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> from " + getOrder().getNation().getName());

                                        if (tpe == GOOD_MONEY) {
                                            final int vpCharge = quantity / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        btrain.getNation(),
                                                        vpCharge * TRANS_MONEY,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-1.png' title='Money' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        } else {
                                            final int worth = quantity * thisGood.getGoodFactor();
                                            final int vpCharge = worth / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        btrain.getNation(),
                                                        vpCharge * TRANS_GOODS,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' title='" + thisGood.getName() + "' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        }

                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> loaded to " + btrain.getName() + " owned by " + btrain.getNation().getName() + getOrder().getExplanation());

                                    } else {
                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> loaded to " + btrain.getName());
                                    }

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                } else {
                                    getOrder().setResult(-5);
                                    getOrder().setExplanation("not enough materials are available at warehouse");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;

                        case TRADECITY:
                        default:
                            final TradeCity thisCity = TradeCityManager.getInstance().getByPosition(btrain.getPosition());

                            // Check that ship is indeed in a city
                            if (thisCity == null) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is not in a trade city");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (thisCity.getCityId() != sourceID) {
                                // Check if this is the correct trade city
                                getOrder().setResult(-7);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {
                                // Baggage Train buys goods from trade city
                                final int money = buyBaggageTrainFromTradeCity(tpe, quantity, btrain, sourceID);

                                if (money == Integer.MIN_VALUE) {
                                    getOrder().setTemp8(Integer.toString(0));
                                    getOrder().setTemp9("0");
                                    getOrder().setResult(-1);
                                    getOrder().setExplanation("unable to trade with trade city");

                                } else if (money < 1) {
                                    getOrder().setTemp8(Integer.toString(0));
                                    getOrder().setTemp9("0");
                                    getOrder().setResult(-1);
                                    getOrder().setExplanation("not enough money available");

                                } else {
                                    getOrder().setTemp9(Integer.toString(money));
                                    getOrder().setResult(1);
                                }
                            }
                            break;
                    }
                }
                break;
            }

            case SHIP:
            default:
                // Retrieve Ship
                final Ship mship = ShipManager.getInstance().getByID(targetID);
                if (mship == null) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("cannot locate entity");

                } else {
                    switch (sourceTPE) {
                        case SHIP: {
                            // Transfer from ship to ship

                            // Retrieve Ship
                            final Ship shipSource = ShipManager.getInstance().getByID(sourceID);
                            if (shipSource == null) {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("cannot locate entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (shipSource.getNation().getId() != getOrder().getNation().getId()
                                    || (shipSource.getCapturedByNation() != 0)) {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not owner of entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (!mship.getPosition().equals(shipSource.getPosition())) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {
                                // Retrieve train materials
                                final Map<Integer, Integer> sourceGoods = shipSource.getStoredGoods();

                                // Retrieve ship materials
                                final Map<Integer, Integer> shipGoods = mship.getStoredGoods();

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (sourceGoods.get(tpe) < quantity) {
                                    quantity = sourceGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (sourceGoods.get(tpe) >= quantity && quantity > 0) {

                                    getOrder().setRegion(mship.getPosition().getRegion());

                                    // decrease ship materials
                                    sourceGoods.put(tpe, sourceGoods.get(tpe) - quantity);
                                    ShipManager.getInstance().update(shipSource);

                                    // increase ship materials
                                    shipGoods.put(tpe, shipGoods.get(tpe) + quantity);
                                    ShipManager.getInstance().update(mship);

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);
                                    getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> transferred from " + shipSource.getName() + " to " + mship.getName());

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in ship");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;
                        }

                        case BAGGAGETRAIN: {
                            // Transfer from baggage train to ship

                            // Retrieve baggage train
                            final BaggageTrain btrain = BaggageTrainManager.getInstance().getByID(sourceID);
                            if (btrain == null) {
                                getOrder().setResult(-1);
                                getOrder().setExplanation("cannot locate entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (btrain.getNation().getId() != getOrder().getNation().getId()) {
                                getOrder().setResult(-2);
                                getOrder().setExplanation("not owner of entity");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (!mship.getPosition().equals(btrain.getPosition())) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {
                                // Retrieve train materials
                                final Map<Integer, Integer> trainGoods = btrain.getStoredGoods();

                                // Retrieve ship materials
                                final Map<Integer, Integer> shipGoods = mship.getStoredGoods();

                                // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
                                // or sell to a TCity quantity larger than the available,
                                // it will empty the transport rather than fail the order
                                if (trainGoods.get(tpe) < quantity) {
                                    quantity = trainGoods.get(tpe);
                                }

                                // Make sure that enough goods are available
                                if (trainGoods.get(tpe) >= quantity && quantity > 0) {

                                    getOrder().setRegion(mship.getPosition().getRegion());

                                    // decrease baggage train materials
                                    trainGoods.put(tpe, trainGoods.get(tpe) - quantity);
                                    BaggageTrainManager.getInstance().update(btrain);

                                    // increase ship materials
                                    shipGoods.put(tpe, shipGoods.get(tpe) + quantity);
                                    ShipManager.getInstance().update(mship);

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);
                                    getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> transferred from " + btrain.getName() + " to " + mship.getName());

                                } else {
                                    getOrder().setResult(-4);
                                    getOrder().setExplanation("not enough materials are available in baggage train");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;
                        }

                        case BARRACK:
                        case WAREHOUSE: {
                            // Load from warehouse
                            final Sector thisSector = SectorManager.getInstance().getByPosition(mship.getPosition());
                            final int ownerId = thisSector.getNation().getId();
                            if (!thisSector.hasBarrack()) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (ownerId == NATION_NEUTRAL) {
                                getOrder().setResult(-5);
                                getOrder().setExplanation("cannot load goods from neutrals");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {

                                // locate regional warehouse
                                final int regionId;
                                if (tpe == GOOD_MONEY) {
                                    regionId = EUROPE;
                                } else {
                                    regionId = mship.getPosition().getRegion().getId();
                                }

                                // Make sure that enough goods are available
                                if (getParent().getTotGoods(ownerId, regionId, tpe) >= quantity) {
                                    // Reduce warehouse materials
                                    getParent().decTotGoods(ownerId, regionId, tpe, quantity);

                                    // Update goods used by order
                                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                                    usedGoods.put(tpe, quantity);
                                    getOrder().setUsedGoodsQnt(usedGoods);

                                    getOrder().setRegion(RegionManager.getInstance().getByID(regionId));

                                    // Increase ship materials
                                    final Map<Integer, Integer> storedGoods = mship.getStoredGoods();
                                    storedGoods.put(tpe, quantity + storedGoods.get(tpe));
                                    ShipManager.getInstance().update(mship);

                                    final Good thisGood = GoodManager.getInstance().getByID(tpe);

                                    // Check if this is foreign aid
                                    if (ownerId != mship.getNation().getId()) {
                                        // Add news entry
                                        newsPair(getOrder().getNation(), mship.getNation(),
                                                NEWS_ECONOMY,
                                                "We delivered " + formatter.format(quantity) + " <img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> as foreign aid to " + mship.getNation().getName(),
                                                "We received foreign aid of " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + tpe + ".png' style=\"padding-top: 1px; vertical-align: bottom;\"> from " + getOrder().getNation().getName());

                                        if (tpe == GOOD_MONEY) {
                                            final int vpCharge = quantity / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        mship.getNation(),
                                                        vpCharge * TRANS_MONEY,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-1.png' title='Money' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        } else {
                                            final int worth = quantity * thisGood.getGoodFactor();
                                            final int vpCharge = worth / 2000000;
                                            if (vpCharge > 0) {
                                                // We lose -1 VP per 2000000
                                                changeVP(getOrder().getGame(),
                                                        mship.getNation(),
                                                        vpCharge * TRANS_GOODS,
                                                        "Received " + formatter.format(quantity) + "&nbsp;<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' title='" + thisGood.getName() + "' style=\"padding-top: 1px; vertical-align: bottom;\">");
                                            }
                                        }

                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> loaded to " + mship.getName() + " owned by " + mship.getNation().getName() + getOrder().getExplanation());

                                    } else {
                                        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> loaded to " + mship.getName());
                                    }

                                    getOrder().setResult(1);
                                    getOrder().setTemp8(Integer.toString(quantity));

                                } else {
                                    getOrder().setResult(-5);
                                    getOrder().setExplanation("not enough materials are available at warehouse");
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;
                        }

                        case TRADECITY:
                        default: {
                            // Check that ship is indeed in a barrack
                            final TradeCity thisCity = TradeCityManager.getInstance().getByPosition(mship.getPosition());
                            if (thisCity == null) {
                                getOrder().setResult(-6);
                                getOrder().setExplanation("transport is not in a trade city");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else if (thisCity.getCityId() != sourceID) {
                                // Check if this is the correct trade city
                                getOrder().setResult(-7);
                                getOrder().setExplanation("transport is in wrong sector");
                                getOrder().setTemp9(Integer.toString(0));
                                getOrder().setTemp8(Integer.toString(0));

                            } else {
                                // Ship buys goods from trade city
                                final int money = buyShipFromTradeCity(tpe, quantity, mship, sourceID);
                                if (money > 0) {
                                    getOrder().setResult(1);

                                } else {
                                    getOrder().setResult(-1);
                                    getOrder().setExplanation("not enough money");
                                    getOrder().setTemp9(Integer.toString(0));
                                    getOrder().setTemp8(Integer.toString(0));
                                }
                            }
                            break;
                        }
                    }
                }
                break;
        }
    }

    /**
     * Apply and report transfer fees.
     *
     * @param tpe          the type of goods transfered.
     * @param quantityInit the total quantity.
     * @param thatNation   the receiving nation of the transaction.
     * @param thatRegion   the region where the transaction takes place.
     * @return the final amount of transfer.
     */
    private int applyTransferFees(final int tpe, final int quantityInit, final Nation thatNation, final Region thatRegion) {
        // Transfer fees
        final int transFee;
        int quantity = quantityInit;

        // Depending on Relations it varies
        final NationsRelation thatRelation = RelationsManager.getInstance()
                .getByNations(getOrder().getGame(), thatNation, getOrder().getNation());
        switch (thatRelation.getRelation()) {
            case REL_ALLIANCE:
                transFee = 0;
                getOrder().setExplanation("&nbsp; No Transfer fees applied. ");
                break;

            case REL_PASSAGE:
                transFee = (int) (quantity * 0.025d);
                getOrder().setExplanation("&nbsp; Transfer fees of 2.5% applied. ");
                break;

            case REL_TRADE:
                transFee = (int) (quantity * 0.05d);
                getOrder().setExplanation("&nbsp; Transfer fees of 5% applied. ");
                break;

            default:
                getOrder().setExplanation("&nbsp; Transfer fees of 100% applied. ");
                transFee = quantity;
                break;
        }

        // Reduce transfer fees
        quantity -= transFee;

        // Report transfer
        final Report feesReport = ReportManager.getInstance().getByOwnerTurnKey(thatNation, getOrder().getGame(),
                getOrder().getTurn(), "fees.region." + thatRegion.getId() + ".good." + tpe);
        if (feesReport != null) {
            try {
                feesReport.setValue(Integer.toString(Integer.parseInt(feesReport.getValue()) + quantity));

            } catch (Exception ex) {
                LOGGER.error("Cannot parse report value", ex);
                feesReport.setValue(Integer.toString(quantity));
            }
            ReportManager.getInstance().update(feesReport);

        } else {
            final Report newReport = new Report();
            newReport.setGame(getOrder().getGame());
            newReport.setTurn(getOrder().getTurn());
            newReport.setNation(thatNation);
            newReport.setKey("fees.region." + thatRegion.getId() + ".good." + tpe);
            newReport.setValue(Integer.toString(quantity));
            ReportManager.getInstance().add(newReport);
        }

        getOrder().setTemp7(Integer.toString(transFee));
        return quantity;
    }

    /**
     * Merchant ship sells goods to Trade city.
     *
     * @param tpe          the type of goods to sell.
     * @param quantityInit the quantity to sell.
     * @param mship        the ship that will do the sale.
     * @param tradeCityId  the trade city Id.
     * @return the amount of money acquired for the sale.
     */
    private int sellShipToTradeCity(final int tpe, final int quantityInit, final Ship mship, final int tradeCityId) {
        int quantity = quantityInit;

        // Retrieve Trade City
        final TradeCity tcity = TradeCityManager.getInstance().getByID(tradeCityId);

        // Retrieve Merchant Ship materials
        final Map<Integer, Integer> storedGoods = mship.getStoredGoods();

        // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
        // or sell to a TCity quantity larger than the available,
        // it will empty the transport rather than fail the order
        if (storedGoods.containsKey(tpe) && storedGoods.get(tpe) < quantity) {
            quantity = storedGoods.get(tpe);
        }

        // Make sure that enough goods are available
        if (storedGoods.containsKey(tpe) && storedGoods.get(tpe) >= quantity && quantity > 0) {
            // Access good factor
            final Good thisGood = GoodManager.getInstance().getByID(tpe);

            // determine if this is the first transaction
            final boolean isFirst;
            if (tradeFirst.containsKey(tradeCityId)) {
                isFirst = (tradeFirst.get(tradeCityId) == mship.getNation().getId());
                LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - is this Player first? " + isFirst);

            } else {
                isFirst = true;
                tradeFirst.put(tradeCityId, mship.getNation().getId());
                LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - FIRST Player to trade");
            }

            // Access good rate
            final Map<Integer, Integer> rateGoods = tcity.getGoodsTradeLvl();

            // Determine good rate taking into account the special Trade Cities Traits
            final double goodRate = TradeCalculations.determineSellTradeRate(tcity.getPosition().getRegion().getId(),
                    tcity.getName(), thisGood.getGoodId(),
                    rateGoods.get(tpe));

            // determine maximum quantity supported by trade city
            final int maxQuantity = TradeCalculations.getMaxSellQTE(thisGood.getGoodFactor(),
                    goodRate,
                    rateGoods.get(GOOD_MONEY),
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus),
                    isFirst);

            // Check if city can support the quantity
            quantity = Math.min(quantity, maxQuantity);

            // Calculate money received from transaction
            int money = TradeCalculations.getSellGoodCost(thisGood.getGoodFactor(),
                    goodRate,
                    quantity,
                    (mship.getNation().getId() == NATION_EGYPT || mship.getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus));

            LOGGER.info("SELL City [" + tcity.getName() + "] from Ship [" + mship.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);

            if (tcity.getNation().getId() != NATION_NEUTRAL && tcity.getNation().getId() != getOrder().getNation().getId()) {
                // Transaction fees
                final int transFee;

                // Depending on Relations it varies
                final NationsRelation thatRelation = RelationsManager.getInstance()
                        .getByNations(getOrder().getGame(),
                                tcity.getNation(), getOrder().getNation());
                switch (thatRelation.getRelation()) {
                    case REL_ALLIANCE:
                        transFee = 0;
                        getOrder().setExplanation("No Transaction fees applied. ");
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level ALLIANCE / Fee Rate = 0%");
                        break;

                    case REL_PASSAGE:
                        transFee = (int) (money * 0.025d);
                        getOrder().setExplanation("Transaction fees of 2.5% applied. ");
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level PASSAGE / Fee Rate = 2.5%");
                        break;

                    case REL_TRADE:
                        transFee = (int) (money * 0.05d);
                        getOrder().setExplanation("Transaction fees of 5% applied. ");
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level TRADE / Fee Rate = 5%");
                        break;

                    default:
                        transFee = money;
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level WAR / Fee Rate = 100%");
                        break;
                }

                // Reduce transfer fees
                money -= transFee;

                // Fees are received by the city owner
                // Increase/Decrease warehouse materials
                getParent().incTotGoods(tcity.getNation().getId(), EUROPE, GOOD_MONEY, transFee);

                // Report transfer
                final Report feesReport = ReportManager.getInstance().getByOwnerTurnKey(tcity.getNation(), getOrder().getGame(),
                        getOrder().getTurn(), "fees.region." + tcity.getPosition().getRegion().getId() + ".good.1");
                if (feesReport != null) {
                    try {
                        feesReport.setValue(Integer.toString(Integer.parseInt(feesReport.getValue()) + transFee));

                    } catch (Exception ex) {
                        LOGGER.error("Cannot parse report value", ex);
                        feesReport.setValue(Integer.toString(quantity));
                    }
                    ReportManager.getInstance().update(feesReport);

                } else {
                    final Report newReport = new Report();
                    newReport.setGame(getOrder().getGame());
                    newReport.setTurn(getOrder().getTurn());
                    newReport.setNation(tcity.getNation());
                    newReport.setKey("fees.region." + tcity.getPosition().getRegion().getId() + ".good.1");
                    newReport.setValue(Integer.toString(transFee));
                    ReportManager.getInstance().add(newReport);
                }
            }

            // Increase/Decrease Merchant Ship materials
            storedGoods.put(GOOD_MONEY, storedGoods.get(GOOD_MONEY) + money);
            storedGoods.put(tpe, storedGoods.get(tpe) - quantity);
            ShipManager.getInstance().update(mship);

            // Keep track of total profit for this nation
            if (getParent().getTradeProfits().containsKey(getOrder().getNation())) {
                getParent().getTradeProfits().put(getOrder().getNation(), getParent().getTradeProfits().get(getOrder().getNation()) + money);
            } else {
                getParent().getTradeProfits().put(getOrder().getNation(), money);
            }

            getOrder().setTemp8(Integer.toString(quantity));
            getOrder().setRegion(RegionManager.getInstance().getByID(mship.getPosition().getRegion().getId()));
            getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> bought from trade city for " + formatter.format(Math.abs(money)) + " <img title='Money' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-1.png' width='12'>" + getOrder().getExplanation());
            return money;

        } else {
            return -1;
        }
    }

    /**
     * Baggage Train sells goods to Trade city.
     *
     * @param tpe          the type of goods to sell.
     * @param quantityInit the quantity to sell.
     * @param btrain       the baggage train that will do the sale.
     * @param tradeCityId  the trade city Id.
     * @return the amount of money acquired for the sale.
     */
    private int sellBaggageTrainToTradeCity(final int tpe, final int quantityInit, final BaggageTrain btrain, final int tradeCityId) {
        int quantity = quantityInit;

        // Retrieve Trade City
        final TradeCity tcity = TradeCityManager.getInstance().getByID(tradeCityId);

        // Retrieve baggage train materials
        final Map<Integer, Integer> storedGoods = btrain.getStoredGoods();

        // ticket:1398 when a Ship/BTrain tries to transfer to a Warehouse/Ship/Btrain
        // or sell to a TCity quantity larger than the available,
        // it will empty the transport rather than fail the order
        if (storedGoods.containsKey(tpe) && storedGoods.get(tpe) < quantity) {
            quantity = storedGoods.get(tpe);
        }

        // Make sure that enough goods are available
        if (storedGoods.containsKey(tpe) && storedGoods.get(tpe) >= quantity && quantity > 0) {
            // Access good factor
            final Good thisGood = GoodManager.getInstance().getByID(tpe);

            // determine if this is the first transaction
            final boolean isFirst;
            if (tradeFirst.containsKey(tradeCityId)) {
                isFirst = (tradeFirst.get(tradeCityId) == btrain.getNation().getId());
                LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - is this Player first? " + isFirst);

            } else {
                isFirst = true;
                tradeFirst.put(tradeCityId, btrain.getNation().getId());
                LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - FIRST Player to trade");
            }

            // Access good rate
            final Map<Integer, Integer> rateGoods = tcity.getGoodsTradeLvl();

            // Determine good rate taking into account the special Trade Cities Traits
            final double goodRate = TradeCalculations.determineSellTradeRate(tcity.getPosition().getRegion().getId(),
                    tcity.getName(), thisGood.getGoodId(),
                    rateGoods.get(tpe));

            // determine maximum quantity supported by trade city
            final int maxQuantity = TradeCalculations.getMaxSellQTE(thisGood.getGoodFactor(),
                    goodRate,
                    rateGoods.get(GOOD_MONEY),
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus),
                    isFirst);

            // Check if city can support the quantity
            quantity = Math.min(quantity, maxQuantity);

            // Calculate money received from transaction
            int money = TradeCalculations.getSellGoodCost(thisGood.getGoodFactor(),
                    goodRate,
                    quantity,
                    (btrain.getNation().getId() == NATION_EGYPT || btrain.getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus));

            LOGGER.info("SELL City [" + tcity.getName() + "] from Train [" + btrain.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);

            if (tcity.getNation().getId() != NATION_NEUTRAL && tcity.getNation().getId() != getOrder().getNation().getId()) {
                // Transaction fees
                final int transFee;

                // Depending on Relations it varies
                final NationsRelation thatRelation = RelationsManager.getInstance()
                        .getByNations(getOrder().getGame(),
                                tcity.getNation(), getOrder().getNation());
                switch (thatRelation.getRelation()) {
                    case REL_ALLIANCE:
                        transFee = 0;
                        getOrder().setExplanation("No Transaction fees applied. ");
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level ALLIANCE / Fee Rate = 0%");
                        break;

                    case REL_PASSAGE:
                        transFee = (int) (money * 0.025d);
                        getOrder().setExplanation("Transaction fees of 2.5% applied. ");
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level PASSAGE / Fee Rate = 2.5%");
                        break;

                    case REL_TRADE:
                        transFee = (int) (money * 0.05d);
                        getOrder().setExplanation("Transaction fees of 5% applied. ");
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level TRADE / Fee Rate = 5%");
                        break;

                    default:
                        transFee = money;
                        LOGGER.info("Transaction Fee " + transFee + " to City Owner [" + tcity.getNation().getName() + "] Relations Level WAR / Fee Rate = 100%");
                        break;
                }

                // Reduce transfer fees
                money -= transFee;

                // Fees are received by the city owner
                // Increase/Decrease warehouse materials
                getParent().incTotGoods(tcity.getNation().getId(), EUROPE, GOOD_MONEY, transFee);

                // Report transfer
                final Report feesReport = ReportManager.getInstance().getByOwnerTurnKey(tcity.getNation(), getOrder().getGame(),
                        getOrder().getTurn(), "fees.region." + tcity.getPosition().getRegion().getId() + ".good.1");

                if (feesReport != null) {
                    try {
                        feesReport.setValue(Integer.toString(Integer.parseInt(feesReport.getValue()) + transFee));

                    } catch (Exception ex) {
                        LOGGER.error("Cannot parse report value", ex);
                        feesReport.setValue(Integer.toString(quantity));
                    }
                    ReportManager.getInstance().update(feesReport);

                } else {
                    final Report newReport = new Report();
                    newReport.setGame(getOrder().getGame());
                    newReport.setTurn(getOrder().getTurn());
                    newReport.setNation(tcity.getNation());
                    newReport.setKey("fees.region." + tcity.getPosition().getRegion().getId() + ".good.1");
                    newReport.setValue(Integer.toString(transFee));
                    ReportManager.getInstance().add(newReport);
                }
            }

            // Increase/Decrease baggage train materials
            storedGoods.put(GOOD_MONEY, storedGoods.get(GOOD_MONEY) + money);
            storedGoods.put(tpe, storedGoods.get(tpe) - quantity);
            BaggageTrainManager.getInstance().update(btrain);

            // Keep track of total profit for this nation
            if (getParent().getTradeProfits().containsKey(getOrder().getNation())) {
                getParent().getTradeProfits().put(getOrder().getNation(), getParent().getTradeProfits().get(getOrder().getNation()) + money);
            } else {
                getParent().getTradeProfits().put(getOrder().getNation(), money);
            }

            getOrder().setTemp8(Integer.toString(quantity));
            getOrder().setRegion(RegionManager.getInstance().getByID(btrain.getPosition().getRegion().getId()));

            getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> bought from trade city for " + formatter.format(Math.abs(money)) + " <img title='Money' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-1.png' width='12'>" + getOrder().getExplanation());
            return money;

        } else {
            return -1;
        }
    }

    /**
     * Warehouse sells goods to Trade City.
     *
     * @param whouseID     the warehouse ID.
     * @param tpe          the good type to sell.
     * @param quantityInit the quantity to sell.
     * @param tradeCityId  the trade city ID.
     * @return the amount of money required for the transaction.
     */
    private int sellWarehouseToTradeCity(final int whouseID, final int tpe, final int quantityInit, final int tradeCityId) {
        int quantity = quantityInit;

        // Retrieve Trade City
        final TradeCity tcity = TradeCityManager.getInstance().getByID(tradeCityId);

        // Retrieve warehouse materials
        final Warehouse whouse = WarehouseManager.getInstance().getByID(whouseID);

        // Make sure that enough goods are available
        if (tpe > 0 && quantity > 0 && getParent().getTotGoods(whouse.getNation().getId(), whouse.getRegion().getId(), tpe) >= quantity) {
            // Access good factor
            final Good thisGood = GoodManager.getInstance().getByID(tpe);

            // determine if this is the first transaction
            final boolean isFirst;
            if (tradeFirst.containsKey(tradeCityId)) {
                isFirst = (tradeFirst.get(tradeCityId) == whouse.getNation().getId());
                LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - is this Player first? " + isFirst);

            } else {
                isFirst = true;
                tradeFirst.put(tradeCityId, whouse.getNation().getId());
                LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - FIRST Player to trade");
            }

            // Access good rate
            final Map<Integer, Integer> rateGoods = tcity.getGoodsTradeLvl();

            // Determine good rate taking into account the special Trade Cities Traits
            final double goodRate = TradeCalculations.determineSellTradeRate(tcity.getPosition().getRegion().getId(),
                    tcity.getName(), thisGood.getGoodId(),
                    rateGoods.get(tpe));

            // determine maximum quantity supported by trade city
            final int maxQuantity = TradeCalculations.getMaxSellQTE(thisGood.getGoodFactor(),
                    goodRate,
                    rateGoods.get(GOOD_MONEY),
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus),
                    isFirst);

            // Check if city can support the quantity
            quantity = Math.min(quantity, maxQuantity);

            // Calculate money received from transaction
            int money = TradeCalculations.getSellGoodCost(thisGood.getGoodFactor(),
                    goodRate,
                    quantity,
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus));

            LOGGER.info("SELL City [" + tcity.getName() + "] from Warehouse [" + whouse.getNation().getName() + "] - " + thisGood.getName() + " (" + tpe + ") [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);

            // Increase/Decrease warehouse materials
            getParent().decTotGoods(whouse.getNation().getId(), whouse.getRegion().getId(), tpe, quantity);
            getParent().incTotGoods(whouse.getNation().getId(), EUROPE, GOOD_MONEY, money);

            // Update goods used by order
            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
            usedGoods.put(tpe, quantity);
            usedGoods.put(GOOD_MONEY, -1 * money);
            getOrder().setUsedGoodsQnt(usedGoods);

            // Keep track of total profit for this nation
            if (getParent().getTradeProfits().containsKey(getOrder().getNation())) {
                getParent().getTradeProfits().put(getOrder().getNation(), getParent().getTradeProfits().get(getOrder().getNation()) + money);
            } else {
                getParent().getTradeProfits().put(getOrder().getNation(), money);
            }

            getOrder().setTemp8(Integer.toString(quantity));
            getOrder().setRegion(whouse.getRegion());
            getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> sold to trade city for " + formatter.format(Math.abs(money)) + " <img title='Money' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-1.png' width='12'>");
            return money;

        } else {
            return -1;
        }
    }

    /**
     * Merchant Ship buys goods from Trade city.
     *
     * @param tpe          the type of goods to buy.
     * @param quantityInit the quantity to buy.
     * @param mship        the ship that will do the purchase.
     * @param tradeCityId  the trade city Id.
     * @return the amount of money required for the purchase.
     */
    private int buyShipFromTradeCity(final int tpe, final int quantityInit, final Ship mship, final int tradeCityId) {
        int quantity = quantityInit;

        // Retrieve Trade City
        final TradeCity tcity = TradeCityManager.getInstance().getByID(tradeCityId);

        // Access good factor
        final Good thisGood = GoodManager.getInstance().getByID(tpe);

        // Access good rate
        final Map<Integer, Integer> rateGoods = tcity.getGoodsTradeLvl();

        // Determine good rate taking into account the special Trade Cities Traits
        final double goodRate = TradeCalculations.determineBuyTradeRate(tcity.getName(), thisGood.getGoodId(),
                rateGoods.get(tpe));

        // determine if this is the first transaction
        final boolean isFirst;
        if (tradeFirst.containsKey(tradeCityId)) {
            isFirst = (tradeFirst.get(tradeCityId) == mship.getNation().getId());
            LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - is this Player first? " + isFirst);

        } else {
            isFirst = true;
            tradeFirst.put(tradeCityId, mship.getNation().getId());
            LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - FIRST Player to trade");
        }

        // determine maximum quantity supported by trade city
        final int maxQuantity = TradeCalculations.getMaxBuyQTE(thisGood.getGoodId(),
                thisGood.getGoodFactor(),
                goodRate,
                rateGoods.get(GOOD_MONEY),
                (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                (tradeCityId == tradeDeficit),
                (tradeCityId == tradeSurplus),
                isFirst);

        // Check if city can support the quantity
        quantity = Math.min(quantity, maxQuantity);

        // Calculate money required for the transaction
        int money = TradeCalculations.getBuyGoodCost(thisGood.getGoodFactor(),
                goodRate,
                quantity,
                (mship.getNation().getId() == NATION_EGYPT || mship.getNation().getId() == NATION_HOLLAND),
                (tradeCityId == tradeDeficit),
                (tradeCityId == tradeSurplus));

        LOGGER.info("BUY City [" + tcity.getName() + "] to Ship [" + mship.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);

        // Retrieve Merchant Ship materials
        final Map<Integer, Integer> storedGoods = mship.getStoredGoods();

        // You cannot buy more than the Merchant Ship contains.
        if (money > storedGoods.get(GOOD_MONEY)) {
            quantity = TradeCalculations.getMaxBuyQTE(-1,
                    thisGood.getGoodFactor(),
                    goodRate,
                    storedGoods.get(GOOD_MONEY),
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus),
                    isFirst);

            money = TradeCalculations.getBuyGoodCost(thisGood.getGoodFactor(),
                    goodRate,
                    quantity,
                    (mship.getNation().getId() == NATION_EGYPT || mship.getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus));

            LOGGER.info("NOT ENOUGH MONEY -- BUY City [" + tcity.getName() + "] to Ship [" + mship.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);
        }

        // Increase/Decrease Merchant Ship materials
        storedGoods.put(GOOD_MONEY, storedGoods.get(GOOD_MONEY) - money);
        storedGoods.put(tpe, storedGoods.get(tpe) + quantity);
        ShipManager.getInstance().update(mship);

        getOrder().setRegion(mship.getPosition().getRegion());
        getOrder().setTemp8(Integer.toString(quantity));
        getOrder().setTemp9(Integer.toString(money));
        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> bought from trade city for " + formatter.format(Math.abs(money)) + " <img title='Money' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-1.png' width='12'>");
        return money;
    }

    /**
     * Baggage Train buys goods from Trade city.
     *
     * @param tpe          the type of goods to buy.
     * @param quantityInit the quantity to buy.
     * @param btrain       the baggage train that will do the purchase.
     * @param tradeCityId  the trade city Id.
     * @return the amount of money required for the purchase.
     */
    private int buyBaggageTrainFromTradeCity(final int tpe, final int quantityInit, final BaggageTrain btrain, final int tradeCityId) {
        int quantity = quantityInit;

        // Retrieve Trade City
        final TradeCity tcity = TradeCityManager.getInstance().getByID(tradeCityId);

        if (tcity == null) {
            // trade city not found
            return Integer.MIN_VALUE;
        }

        // Access good factor
        final Good thisGood = GoodManager.getInstance().getByID(tpe);

        // Access good rate
        final Map<Integer, Integer> rateGoods = tcity.getGoodsTradeLvl();

        // Determine good rate taking into account the special Trade Cities Traits
        final double goodRate = TradeCalculations.determineBuyTradeRate(tcity.getName(), thisGood.getGoodId(),
                rateGoods.get(tpe));

        // determine if this is the first transaction
        final boolean isFirst;
        if (tradeFirst.containsKey(tradeCityId)) {
            isFirst = (tradeFirst.get(tradeCityId) == btrain.getNation().getId());
            LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - is this Player first? " + isFirst);

        } else {
            isFirst = true;
            tradeFirst.put(tradeCityId, btrain.getNation().getId());
            LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - FIRST Player to trade");
        }

        // determine maximum quantity supported by trade city
        final int maxQuantity = TradeCalculations.getMaxBuyQTE(thisGood.getGoodId(),
                thisGood.getGoodFactor(),
                goodRate,
                rateGoods.get(GOOD_MONEY),
                (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                (tradeCityId == tradeDeficit),
                (tradeCityId == tradeSurplus),
                isFirst);

        // Check if city can support the quantity
        quantity = Math.min(quantity, maxQuantity);

        // Calculate money required for the transaction
        int money = TradeCalculations.getBuyGoodCost(thisGood.getGoodFactor(),
                goodRate,
                quantity,
                (btrain.getNation().getId() == NATION_EGYPT || btrain.getNation().getId() == NATION_HOLLAND),
                (tradeCityId == tradeDeficit),
                (tradeCityId == tradeSurplus));

        LOGGER.info("BUY City [" + tcity.getName() + "] to Train [" + btrain.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);

        // Retrieve baggage train materials
        final Map<Integer, Integer> storedGoods = btrain.getStoredGoods();

        // You cannot buy more than the baggage train contains.
        if (money > storedGoods.get(GOOD_MONEY)) {
            quantity = TradeCalculations.getMaxBuyQTE(-1,
                    thisGood.getGoodFactor(),
                    goodRate,
                    storedGoods.get(GOOD_MONEY),
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus),
                    isFirst);

            money = TradeCalculations.getBuyGoodCost(thisGood.getGoodFactor(),
                    goodRate,
                    quantity,
                    (btrain.getNation().getId() == NATION_EGYPT || btrain.getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus));

            LOGGER.info("NOT ENOUGH MONEY -- BUY City [" + tcity.getName() + "] to Train [" + btrain.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);
        }

        // Increase/Decrease baggage train materials
        storedGoods.put(GOOD_MONEY, storedGoods.get(GOOD_MONEY) - money);
        storedGoods.put(tpe, storedGoods.get(tpe) + quantity);
        BaggageTrainManager.getInstance().update(btrain);

        getOrder().setTemp8(Integer.toString(quantity));
        getOrder().setRegion(btrain.getPosition().getRegion());
        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> bought from trade city for " + formatter.format(Math.abs(money)) + " <img title='Money' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-1.png' width='12'>");
        return money;
    }

    /**
     * Warehouse buys goods from Trade City.
     *
     * @param whouseID     the warehouse ID.
     * @param tpe          the good type to buy.
     * @param quantityInit the quantity to buy.
     * @param tradeCityId  the trade city ID.
     * @return the amount of money gained from transaction.
     */
    private int buyWarehouseFromTradeCity(final int whouseID, final int tpe, final int quantityInit, final int tradeCityId) {
        int quantity = quantityInit;

        // Retrieve Trade City
        final TradeCity tcity = TradeCityManager.getInstance().getByID(tradeCityId);

        // Access good factor
        final Good thisGood = GoodManager.getInstance().getByID(tpe);

        // Access good rate
        final Map<Integer, Integer> rateGoods = tcity.getGoodsTradeLvl();

        // Determine good rate taking into account the special Trade Cities Traits
        final double goodRate = TradeCalculations.determineBuyTradeRate(tcity.getName(), thisGood.getGoodId(),
                rateGoods.get(tpe));

        // Retrieve warehouse materials
        //since you need the warehouse to check if there are available money... we need the european warehouse..
        final Warehouse whouse = WarehouseManager.getInstance().getByID(whouseID);

        // determine if this is the first transaction
        final boolean isFirst;
        if (tradeFirst.containsKey(tradeCityId)) {
            isFirst = (tradeFirst.get(tradeCityId) == whouse.getNation().getId());
            LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - is this Player first? " + isFirst);

        } else {
            isFirst = true;
            tradeFirst.put(tradeCityId, whouse.getNation().getId());
            LOGGER.info("Trade First with City [" + tradeCityId + "] is Nation [" + tradeFirst.get(tradeCityId) + "] - FIRST Player to trade");
        }

        // determine maximum quantity supported by trade city
        final int maxQuantity = TradeCalculations.getMaxBuyQTE(thisGood.getGoodId(),
                thisGood.getGoodFactor(),
                goodRate,
                rateGoods.get(GOOD_MONEY),
                (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                (tradeCityId == tradeDeficit),
                (tradeCityId == tradeSurplus),
                isFirst);

        // Check if city can support the quantity
        quantity = Math.min(quantity, maxQuantity);

        // Calculate money required for the transaction
        int money = TradeCalculations.getBuyGoodCost(thisGood.getGoodFactor(),
                goodRate,
                quantity,
                (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                (tradeCityId == tradeDeficit),
                (tradeCityId == tradeSurplus));

        LOGGER.info("BUY City [" + tcity.getName() + "] to Warehouse [" + whouse.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);

        // You cannot pay more than available.
        if (money > getParent().getTotGoods(getOrder().getNation().getId(), EUROPE, GOOD_MONEY)) {
            quantity = TradeCalculations.getMaxBuyQTE(-1,
                    thisGood.getGoodFactor(),
                    goodRate,
                    getParent().getTotGoods(getOrder().getNation().getId(), EUROPE, GOOD_MONEY),
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus),
                    isFirst);

            money = TradeCalculations.getBuyGoodCost(thisGood.getGoodFactor(),
                    goodRate,
                    quantity,
                    (getOrder().getNation().getId() == NATION_EGYPT || getOrder().getNation().getId() == NATION_HOLLAND),
                    (tradeCityId == tradeDeficit),
                    (tradeCityId == tradeSurplus));

            LOGGER.info("NOT ENOUGH MONEY -- BUY City [" + tcity.getName() + "] to Warehouse [" + whouse.getNation().getName() + "] - " + thisGood.getName() + " [GF=" + thisGood.getGoodFactor() + ", TR=" + goodRate + "] QTE=" + quantity + " VALUE=" + money);
        }

        // Increase warehouse materials
        getParent().incTotGoods(whouse.getNation().getId(), whouse.getRegion().getId(), tpe, quantity);
        getParent().decTotGoods(whouse.getNation().getId(), EUROPE, GOOD_MONEY, money);

        // Update goods used by order
        final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
        usedGoods.put(tpe, -1 * quantity);
        usedGoods.put(GOOD_MONEY, money);
        getOrder().setUsedGoodsQnt(usedGoods);

        getOrder().setRegion(whouse.getRegion());
        getOrder().setTemp8(Integer.toString(quantity));
        getOrder().setExplanation(formatter.format(Math.abs(quantity)) + " <img title='" + thisGood.getName() + "' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-" + thisGood.getGoodId() + ".png' width='12'> bought from trade city for " + formatter.format(Math.abs(money)) + " <img title='Money' style='padding-top: 1px; vertical-align: bottom;' src='http://static.eaw1805.com/images/goods/good-1.png' width='12'>");
        return money;
    }

}
