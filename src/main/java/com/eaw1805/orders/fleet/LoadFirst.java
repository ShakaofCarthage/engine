package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.WeightCalculators;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.Good;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Load Units during 1st loading phase.
 */
public class LoadFirst
        extends AbstractOrderProcessor
        implements ArmyConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(LoadFirst.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_LOAD_TROOPSF;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public LoadFirst(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("LoadFirst instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        // Check type of unit that will be loaded
        final int vesselType = Integer.parseInt(getOrder().getParameter1());
        final int vesselId = Integer.parseInt(getOrder().getParameter2());
        final int unitType = Integer.parseInt(getOrder().getParameter3());
        final int unitId = Integer.parseInt(getOrder().getParameter4());

        switch (unitType) {
            case SPY:
                final Spy thisSpy = SpyManager.getInstance().getByID(unitId);

                if (thisSpy == null) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("spy not found");
                    return;
                }

                if (thisSpy.getCarrierInfo().getCarrierType() != 0) {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("unit already loaded");
                    return;
                }

                // determine the type of vessel that will carry the unit
                switch (vesselType) {
                    case FLEET:
                        final Fleet thisFleet;
                        // Check if this is a newly created fleet
                        if (getParent().fleetAssocExists(vesselId)) {
                            // this is a new fleet
                            thisFleet = FleetManager.getInstance().getByID(getParent().retrieveFleetAssoc(vesselId));

                        } else {
                            thisFleet = FleetManager.getInstance().getByID(vesselId);
                        }

                        if (thisFleet == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("fleet not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisSpy.getPosition().equals(thisFleet.getPosition())
                                && thisSpy.getPosition().distance(thisFleet.getPosition()) > 1) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("fleet not within loading reach.");

                        } else if (loadSpyOnFleet(thisSpy, thisFleet)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Spy '" + thisSpy.getName() + "' loaded on fleet '" + thisFleet.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("fleet does not have enough space for spy to embark.");
                        }

                        break;

                    case SHIP:
                        final Ship thisShip = ShipManager.getInstance().getByID(vesselId);
                        if (thisShip == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("ship not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisSpy.getPosition().equals(thisShip.getPosition())
                                && thisSpy.getPosition().distance(thisShip.getPosition()) > 1) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("ship not within loading reach.");

                        } else if (loadSpyOnShip(thisSpy, thisShip)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Spy '" + thisSpy.getName() + "' loaded on ship '" + thisShip.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("ship does not have enough space for spy to embark.");
                        }

                        break;

                    case BAGGAGETRAIN:
                    default:
                        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(vesselId);
                        if (thisTrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("baggage train not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisSpy.getPosition().equals(thisTrain.getPosition())
                                && thisSpy.getPosition().distance(thisTrain.getPosition()) > 1) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("baggage train not within loading reach.");

                        } else if (loadSpyOnTrain(thisSpy, thisTrain)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Spy '" + thisSpy.getName() + "' loaded on baggage train '" + thisTrain.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("baggage train does not have enough space for spy to embark.");
                        }

                        break;
                }

                break;

            case COMMANDER:
                final Commander thisCommander = CommanderManager.getInstance().getByID(unitId);

                if (thisCommander == null) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("commander not found");
                    return;
                }

                if (thisCommander.getCarrierInfo().getCarrierType() != 0) {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("unit already loaded");
                    return;
                }

                // determine the type of vessel that will carry the unit
                switch (vesselType) {
                    case FLEET:
                        final Fleet thisFleet;
                        // Check if this is a newly created fleet
                        if (getParent().fleetAssocExists(vesselId)) {
                            // this is a new fleet
                            thisFleet = FleetManager.getInstance().getByID(getParent().retrieveFleetAssoc(vesselId));

                        } else {
                            thisFleet = FleetManager.getInstance().getByID(vesselId);
                        }

                        if (thisFleet == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("fleet not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisCommander.getPosition().equals(thisFleet.getPosition())
                                && thisCommander.getPosition().distance(thisFleet.getPosition()) > 1) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("fleet not within loading reach.");

                        } else if (loadCommanderOnFleet(thisCommander, thisFleet)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Commander '" + thisCommander.getName() + "' loaded on fleet '" + thisFleet.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("fleet does not have enough space for commander to embark.");
                        }

                        break;

                    case SHIP:
                        final Ship thisShip = ShipManager.getInstance().getByID(vesselId);
                        if (thisShip == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("ship not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisCommander.getPosition().equals(thisShip.getPosition())
                                && thisCommander.getPosition().distance(thisShip.getPosition()) > 1) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("ship not within loading reach.");

                        } else if (loadCommanderOnShip(thisCommander, thisShip)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Commander '" + thisCommander.getName() + "' loaded on ship '" + thisShip.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("ship does not have enough space for commander to embark.");
                        }

                        break;

                    case BAGGAGETRAIN:
                    default:
                        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(vesselId);
                        if (thisTrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("baggage train not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisCommander.getPosition().equals(thisTrain.getPosition())
                                && thisCommander.getPosition().distance(thisTrain.getPosition()) > 1) {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("baggage train not within loading reach.");

                        } else if (loadCommanderOnTrain(thisCommander, thisTrain)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Commander '" + thisCommander.getName() + "' loaded on baggage train '" + thisTrain.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("baggage train does not have enough space for commander to embark.");
                        }

                        break;
                }

                break;

            case BRIGADE:
                final Brigade thisBrigade = BrigadeManager.getInstance().getByID(unitId);

                if (thisBrigade == null) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("brigade not found");
                    return;
                }

                int totHeadcount = 0;
                for (final Battalion battalion : thisBrigade.getBattalions()) {
                    if (battalion.getCarrierInfo().getCarrierType() != 0) {
                        getOrder().setResult(-3);
                        getOrder().setExplanation("unit already loaded");
                        return;
                    }

                    totHeadcount += battalion.getHeadcount();
                }

                if (totHeadcount < 1) {
                    getOrder().setResult(-4);
                    getOrder().setExplanation("unit is empty");
                    return;
                }

                // determine the type of vessel that will carry the unit
                switch (vesselType) {
                    case FLEET:
                        final Fleet thisFleet;
                        // Check if this is a newly created fleet
                        if (getParent().fleetAssocExists(vesselId)) {
                            // this is a new fleet
                            thisFleet = FleetManager.getInstance().getByID(getParent().retrieveFleetAssoc(vesselId));

                        } else {
                            thisFleet = FleetManager.getInstance().getByID(vesselId);
                        }

                        if (thisFleet == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("fleet not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisBrigade.getPosition().equals(thisFleet.getPosition())
                                && thisBrigade.getPosition().distance(thisFleet.getPosition()) > 1) {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("Brigade not within loading reach.");

                        } else if (loadBrigadeOnFleet(thisBrigade, thisFleet)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Brigade '" + thisBrigade.getName() + "' loaded on fleet '" + thisFleet.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("fleet does not have enough space for brigade to embark.");
                        }

                        break;

                    case SHIP:
                        final Ship thisShip = ShipManager.getInstance().getByID(vesselId);
                        if (thisShip == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("ship not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisBrigade.getPosition().equals(thisShip.getPosition())
                                && thisBrigade.getPosition().distance(thisShip.getPosition()) > 1) {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("Brigade not within loading reach.");

                        } else if (loadBrigadeOnShip(thisBrigade, thisShip)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Brigade '" + thisBrigade.getName() + "' loaded on ship '" + thisShip.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("ship does not have enough space for brigade to embark.");
                        }

                        break;

                    case BAGGAGETRAIN:
                    default:
                        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(vesselId);
                        if (thisTrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("baggage train not found");
                            return;
                        }

                        // Check that two entities are on the same location
                        if (!thisBrigade.getPosition().equals(thisTrain.getPosition())
                                && thisBrigade.getPosition().distance(thisTrain.getPosition()) > 1) {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("baggage train not within loading reach.");

                        } else if (loadBrigadeOnTrain(thisBrigade, thisTrain)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Brigade '" + thisBrigade.getName() + "' loaded on baggage train '" + thisTrain.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("baggage train does not have enough space for brigade to embark.");
                        }

                        break;
                }

                break;

            default:
                // do nothing
        }
    }

    /**
     * Load a spy on a fleet.
     *
     * @param thisSpy   the spy to load.
     * @param thisFleet the fleet to use.
     * @return the result of the operation.
     */
    private boolean loadSpyOnFleet(final Spy thisSpy, final Fleet thisFleet) {
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), thisFleet.getFleetId());
        if (lstShips.isEmpty()) {
            return false;
        }

        // load on first ship
        return loadSpyOnShip(thisSpy, lstShips.get(0));
    }

    /**
     * Load a spy on a ship.
     *
     * @param thisSpy  the spy to load.
     * @param thisShip the ship to use.
     * @return the result of the operation.
     */
    private boolean loadSpyOnShip(final Spy thisSpy, final Ship thisShip) {
        // load spy on ship
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.SPY * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            thisSlot++;
        }

        storedGoods.put(thisSlot, thisSpy.getSpyId());
        ShipManager.getInstance().update(thisShip);

        // Keep info in spy unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(ArmyConstants.SHIP);
        thisCarrying.setCarrierId(thisShip.getShipId());
        thisSpy.setCarrierInfo(thisCarrying);
        SpyManager.getInstance().update(thisSpy);

        return true;
    }

    /**
     * Load a spy on a baggage train.
     *
     * @param thisSpy   the spy to load.
     * @param thisTrain the baggage train to use.
     * @return the result of the operation.
     */
    private boolean loadSpyOnTrain(final Spy thisSpy, final BaggageTrain thisTrain) {
        // load spy on ship
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.SPY * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            thisSlot++;
        }

        storedGoods.put(thisSlot, thisSpy.getSpyId());
        BaggageTrainManager.getInstance().update(thisTrain);

        // Keep info in spy unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(ArmyConstants.BAGGAGETRAIN);
        thisCarrying.setCarrierId(thisTrain.getBaggageTrainId());
        thisSpy.setCarrierInfo(thisCarrying);
        SpyManager.getInstance().update(thisSpy);

        return true;
    }

    /**
     * Load a Commander on a fleet.
     *
     * @param thisCommander the Commander to load.
     * @param thisFleet     the fleet to use.
     * @return the result of the operation.
     */
    private boolean loadCommanderOnFleet(final Commander thisCommander, final Fleet thisFleet) {
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), thisFleet.getFleetId());
        if (lstShips.isEmpty()) {
            return false;
        }

        // load on first ship
        return loadCommanderOnShip(thisCommander, lstShips.get(0));
    }

    /**
     * Load a commander on a ship.
     *
     * @param thisCommander the commander to load.
     * @param thisShip      the ship to use.
     * @return the result of the operation.
     */
    private boolean loadCommanderOnShip(final Commander thisCommander, final Ship thisShip) {
        // load commander on ship
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.COMMANDER * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            thisSlot++;
        }

        storedGoods.put(thisSlot, thisCommander.getId());
        ShipManager.getInstance().update(thisShip);

        // Remove from command of any corps or army
        removeCommander(thisCommander);

        // Keep info in commander unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(ArmyConstants.SHIP);
        thisCarrying.setCarrierId(thisShip.getShipId());
        thisCommander.setCarrierInfo(thisCarrying);
        thisCommander.setPool(false);
        CommanderManager.getInstance().update(thisCommander);

        return true;
    }

    /**
     * Load a commander on a baggage train.
     *
     * @param thisCommander the commander to load.
     * @param thisTrain     the baggage train to use.
     * @return the result of the operation.
     */
    private boolean loadCommanderOnTrain(final Commander thisCommander, final BaggageTrain thisTrain) {
        // load commander on baggage train
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.COMMANDER * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            thisSlot++;
        }

        storedGoods.put(thisSlot, thisCommander.getId());
        BaggageTrainManager.getInstance().update(thisTrain);

        // Remove from command of any corps or army
        removeCommander(thisCommander);

        // Keep info in commander unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(ArmyConstants.BAGGAGETRAIN);
        thisCarrying.setCarrierId(thisTrain.getBaggageTrainId());
        thisCommander.setCarrierInfo(thisCarrying);
        thisCommander.setPool(false);
        CommanderManager.getInstance().update(thisCommander);

        return true;
    }

    /**
     * Remove commander from army or corps.
     *
     * @param thisComm the commander object.
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
        //be sure to update commander
        CommanderManager.getInstance().update(thisComm);
    }

    /**
     * Remove commander from army.
     *
     * @param armyId the army ID.
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
     * Remove commander from corps.
     *
     * @param corpId the corps id.
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
     * Load a brigade on a fleet.
     *
     * @param thisBrigade the brigade to load.
     * @param thisFleet   the fleet to use.
     * @return the result of the operation.
     */
    private boolean loadBrigadeOnFleet(final Brigade thisBrigade, final Fleet thisFleet) {
        // Check available space
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), thisFleet.getFleetId());
        if (lstShips.isEmpty()) {
            return false;
        }

        // load on first ship
        return loadBrigadeOnShip(thisBrigade, lstShips.get(0));
    }

    /**
     * Load a brigade on a ship.
     *
     * @param thisBrigade the brigade to load.
     * @param thisShip    the ship to use.
     * @return the result of the operation.
     */
    private boolean loadBrigadeOnShip(final Brigade thisBrigade, final Ship thisShip) {
        // load brigade on ship
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.BRIGADE * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            thisSlot++;
        }

        storedGoods.put(thisSlot, thisBrigade.getBrigadeId());
        ShipManager.getInstance().update(thisShip);

        // Remove from corps
        thisBrigade.setCorp(0);

        // Keep info in each battalion of the brigade
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(ArmyConstants.SHIP);
        thisCarrying.setCarrierId(thisShip.getShipId());

        for (final Battalion battalion : thisBrigade.getBattalions()) {
            if (battalion.getHeadcount() > 0) {
                battalion.setCarrierInfo(thisCarrying);
                BattalionManager.getInstance().update(battalion);
            }
        }

        return true;
    }

    /**
     * Load a brigade on a baggage train.
     *
     * @param thisBrigade the brigade to load.
     * @param thisTrain   the baggage train to use.
     * @return the result of the operation.
     */
    private boolean loadBrigadeOnTrain(final Brigade thisBrigade, final BaggageTrain thisTrain) {
        // load brigade on baggage train
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();

        // find next available slot
        int thisSlot = ArmyConstants.BRIGADE * 1000;
        while (storedGoods.containsKey(thisSlot)) {
            thisSlot++;
        }

        storedGoods.put(thisSlot, thisBrigade.getBrigadeId());
        BaggageTrainManager.getInstance().update(thisTrain);

        // Remove from corps
        thisBrigade.setCorp(0);

        // Keep info in each battalion of the brigade
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(ArmyConstants.BAGGAGETRAIN);
        thisCarrying.setCarrierId(thisTrain.getBaggageTrainId());

        for (final Battalion battalion : thisBrigade.getBattalions()) {
            if (battalion.getHeadcount() > 0) {
                battalion.setCarrierInfo(thisCarrying);
                BattalionManager.getInstance().update(battalion);
            }
        }

        return true;
    }

    /**
     * Compute available space of given ship based on already loaded goods and units.
     *
     * @param thisShip the ship to check.
     * @return available space.
     */
    private int getShipFreeSpace(final Ship thisShip) {
        // Get maximum load capacity (based on type)
        final int maxLoad = thisShip.getType().getLoadCapacity();

        // identify loaded units & goods
        int loadedUnits = 0;
        for (Map.Entry<Integer, Integer> loadedUnit : thisShip.getStoredGoods().entrySet()) {
            final int key = loadedUnit.getKey();
            if (key <= GoodConstants.GOOD_LAST) {
                if (loadedUnit.getValue() > 0) {
                    // compute weight
                    final Good loadedGood = GoodManager.getInstance().getByID(key);
                    loadedUnits += loadedGood.getWeightOfGood() * loadedUnit.getValue();
                }

            } else {
                // Check type of unit
                if (loadedUnit.getKey() >= ArmyConstants.SPY * 1000) {
                    // no weight

                } else if (loadedUnit.getKey() >= ArmyConstants.COMMANDER * 1000) {
                    // no weight

                } else if (loadedUnit.getKey() >= ArmyConstants.BRIGADE * 1000) {
                    // locate brigade
                    final Brigade loadedBrigade = BrigadeManager.getInstance().getByID(loadedUnit.getValue());

                    // check which battalions are loaded on this particular ship
                    for (final Battalion battalion : loadedBrigade.getBattalions()) {
                        if (battalion.getCarrierInfo() != null
                                && battalion.getCarrierInfo().getCarrierType() == ArmyConstants.SHIP
                                && battalion.getCarrierInfo().getCarrierId() == thisShip.getShipId()) {
                            loadedUnits += WeightCalculators.getBattalionWeight(battalion);
                        }
                    }
                }
            }
        }

        return maxLoad - loadedUnits;
    }

}
