package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NavigationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Unload units during the 1st phase.
 */
public class UnloadFirst
        extends AbstractOrderProcessor
        implements ArmyConstants, RegionConstants, NavigationConstants, TerrainConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(UnloadFirst.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_UNLOAD_TROOPSF;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public UnloadFirst(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("UnloadFirst instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        // Check type of unit that will be unloaded
        final int vesselType = Integer.parseInt(getOrder().getParameter1());
        final int vesselId = Integer.parseInt(getOrder().getParameter2());
        final int unitType = Integer.parseInt(getOrder().getParameter3());
        final int unitId = Integer.parseInt(getOrder().getParameter4());
        final int direction = Integer.parseInt(getOrder().getParameter5());

        switch (unitType) {
            case SPY:
                final Spy thisSpy = SpyManager.getInstance().getByID(unitId);

                if (thisSpy == null) {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("spy not found");
                    return;
                }

                if (thisSpy.getCarrierInfo().getCarrierType() == 0) {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("unit not loaded");
                    return;
                }

                // determine the type of vessel that is carrying the unit
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

                        if (unloadSpyFromFleet(thisSpy, thisFleet, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Spy '" + thisSpy.getName() + "' unloaded from fleet '" + thisFleet.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("spy was not loaded on fleet or could not unload on sea.");
                        }

                        break;

                    case SHIP:
                        final Ship thisShip = ShipManager.getInstance().getByID(vesselId);
                        if (thisShip == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("ship not found");
                            return;
                        }

                        if (unloadSpyFromShip(thisSpy, thisShip, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Spy '" + thisSpy.getName() + "' unloaded from ship '" + thisShip.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("spy was not loaded on ship or could not unload on sea.");
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

                        if (unloadSpyFromTrain(thisSpy, thisTrain, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Spy '" + thisSpy.getName() + "' unloaded from baggage train '" + thisTrain.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("spy was not loaded on baggage train or could not unload on sea.");
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

                if (thisCommander.getCarrierInfo().getCarrierType() == 0) {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("unit not loaded");
                    return;
                }

                // determine the type of vessel that is carrying the unit
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

                        if (unloadCommanderFromFleet(thisCommander, thisFleet, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Commander '" + thisCommander.getName() + "' unloaded from fleet '" + thisFleet.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("commander was not loaded on fleet or could not unload on sea.");
                        }

                        break;

                    case SHIP:
                        final Ship thisShip = ShipManager.getInstance().getByID(vesselId);
                        if (thisShip == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("ship not found");
                            return;
                        }

                        if (unloadCommanderFromShip(thisCommander, thisShip, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Commander '" + thisCommander.getName() + "' unloaded from ship '" + thisShip.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("commander was not loaded on ship or could not unload on sea.");
                        }

                        break;

                    case BAGGAGETRAIN:
                    default:
                        final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(vesselId);
                        if (thisTrain == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("baggage train not found or could not unload on sea");
                            return;
                        }

                        if (unloadCommanderFromTrain(thisCommander, thisTrain, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Commander '" + thisCommander.getName() + "' unloaded from baggage train '" + thisTrain.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("commander was not loaded on baggage train or could not unload on sea.");
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

                // Make sure that at least 1 battalion is loaded
                // In some cases counters are messed up - probably due to merging of brigades
                // Fix for ticket:1670
                boolean loadedFound = false;
                for (final Battalion battalion : thisBrigade.getBattalions()) {
                    if (battalion.getCarrierInfo().getCarrierType() != 0) {
                        loadedFound = true;
                        break;
                    }
                }

                if (!loadedFound) {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("unit not loaded");
                    return;
                }

                // determine the type of vessel that is carrying the unit
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

                        if (!canUnload(thisBrigade.getNation().getId(), getCoordsByDirection(direction, thisFleet.getPosition()))) {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("brigade was not allowed to unload due to foreign relations.");

                        } else if (unloadBrigadeFromFleet(thisBrigade, thisFleet, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Brigade '" + thisBrigade.getName() + "' unloaded from fleet '" + thisFleet.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("brigade was not loaded on fleet or could not unload on sea.");
                        }

                        break;

                    case SHIP:
                        final Ship thisShip = ShipManager.getInstance().getByID(vesselId);
                        if (thisShip == null) {
                            getOrder().setResult(-1);
                            getOrder().setExplanation("ship not found");
                            return;
                        }

                        if (!canUnload(thisBrigade.getNation().getId(), getCoordsByDirection(direction, thisShip.getPosition()))) {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("brigade was not allowed to unload due to foreign relations.");

                        } else if (unloadBrigadeFromShip(thisBrigade, thisShip, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Brigade '" + thisBrigade.getName() + "' unloaded from ship '" + thisShip.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("brigade was not loaded on ship or could not unload on sea.");
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

                        if (!canUnload(thisBrigade.getNation().getId(), getCoordsByDirection(direction, thisTrain.getPosition()))) {
                            getOrder().setResult(-3);
                            getOrder().setExplanation("brigade was not allowed to unload due to foreign relations.");

                        } else if (unloadBrigadeFromTrain(thisBrigade, thisTrain, direction)) {
                            getOrder().setResult(1);
                            getOrder().setExplanation("Brigade '" + thisBrigade.getName() + "' unloaded from baggage train '" + thisTrain.getName() + "'");

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("brigade was not loaded on baggage train or could not unload on sea.");
                        }

                        break;
                }

                break;

            default:
                // do nothing
        }
    }

    /**
     * Unload a Spy from fleet.
     *
     * @param thisSpy   the Spy to unload.
     * @param thisFleet the fleet to use.
     * @param direction the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadSpyFromFleet(final Spy thisSpy, final Fleet thisFleet, final int direction) {
        final Ship thisShip = ShipManager.getInstance().getByID(thisSpy.getCarrierInfo().getCarrierId());
        if (thisShip == null) {
            return false;
        }

        if (thisShip.getFleet() != thisFleet.getFleetId()) {
            return false;
        }

        // unload from ship
        return unloadSpyFromShip(thisSpy, thisShip, direction);
    }

    /**
     * Unload a spy from a ship.
     *
     * @param thisSpy   the commander to unload.
     * @param thisShip  the ship to use.
     * @param direction the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadSpyFromShip(final Spy thisSpy, final Ship thisShip, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisShip.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector == null) {
            return false;
        }
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        // unload unit from carrier
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
        int thisKey = 0;

        // Check if a unit is loaded in the ship        
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.SPY * 1000
                        && entry.getKey() < (ArmyConstants.SPY + 1) * 1000
                        && entry.getValue() == thisSpy.getSpyId()) {
                    thisKey = entry.getKey();
                    break;
                }
            }
        }

        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == thisSpy.getSpyId()) {
            storedGoods.remove(thisKey);
        }
        ShipManager.getInstance().update(thisShip);

        // remove info from spy unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(0);
        thisCarrying.setCarrierId(0);
        thisSpy.setCarrierInfo(thisCarrying);

        // set new position of spy
        thisSpy.setPosition(landingPosition);

        SpyManager.getInstance().update(thisSpy);

        return true;
    }

    /**
     * Unload a spy from a baggage train.
     *
     * @param thisSpy   the commander to unload.
     * @param thisTrain the baggage train to use.
     * @param direction the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadSpyFromTrain(final Spy thisSpy, final BaggageTrain thisTrain, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisTrain.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector == null) {
            return false;
        }
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        // unload unit from carrier
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
        int thisKey = 0;

        // Check if a unit is loaded in the carrier
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.SPY * 1000
                        && entry.getKey() < (ArmyConstants.SPY + 1) * 1000
                        && entry.getValue() == thisSpy.getSpyId()) {
                    thisKey = entry.getKey();
                    break;
                }
            }
        }

        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == thisSpy.getSpyId()) {
            storedGoods.remove(thisKey);
        }

        BaggageTrainManager.getInstance().update(thisTrain);

        // remove info from spy unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(0);
        thisCarrying.setCarrierId(0);
        thisSpy.setCarrierInfo(thisCarrying);

        // set new position of spy
        thisSpy.setPosition(landingPosition);

        SpyManager.getInstance().update(thisSpy);

        return true;
    }

    /**
     * Unload a Commander from fleet.
     *
     * @param thisCommander the Commander to unload.
     * @param thisFleet     the fleet to use.
     * @param direction     the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadCommanderFromFleet(final Commander thisCommander, final Fleet thisFleet, final int direction) {
        final Ship thisShip = ShipManager.getInstance().getByID(thisCommander.getCarrierInfo().getCarrierId());
        if (thisShip == null) {
            return false;
        }
        if (thisShip.getFleet() != thisFleet.getFleetId()) {
            return false;
        }

        // unload from ship
        return unloadCommanderFromShip(thisCommander, thisShip, direction);
    }

    /**
     * Unload a commander from a ship.
     *
     * @param thisCommander the commander to unload.
     * @param thisShip      the ship to use.
     * @param direction     the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadCommanderFromShip(final Commander thisCommander, final Ship thisShip, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisShip.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector == null) {
            return false;
        }
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        // unload unit from carrier
        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
        int thisKey = 0;

        // Check if a unit is loaded in the carrier
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.COMMANDER * 1000
                        && entry.getKey() < (ArmyConstants.COMMANDER + 1) * 1000
                        && entry.getValue() == thisCommander.getId()) {
                    thisKey = entry.getKey();
                    break;
                }
            }
        }

        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == thisCommander.getId()) {
            storedGoods.remove(thisKey);
        }

        ShipManager.getInstance().update(thisShip);

        // remove info from commander unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(0);
        thisCarrying.setCarrierId(0);
        thisCommander.setCarrierInfo(thisCarrying);

        // set new position of commander
        thisCommander.setPosition(landingPosition);

        CommanderManager.getInstance().update(thisCommander);

        return true;
    }

    /**
     * Unload a commander from a baggage train.
     *
     * @param thisCommander the commander to unload.
     * @param thisTrain     the baggage train to use.
     * @param direction     the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadCommanderFromTrain(final Commander thisCommander, final BaggageTrain thisTrain, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisTrain.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector == null) {
            return false;
        }
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        // unload unit from carrier
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
        int thisKey = 0;

        // Check if a unit is loaded in the carrier
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.COMMANDER * 1000
                        && entry.getKey() < (ArmyConstants.COMMANDER + 1) * 1000
                        && entry.getValue() == thisCommander.getId()) {
                    thisKey = entry.getKey();
                    break;
                }
            }
        }

        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == thisCommander.getId()) {
            storedGoods.remove(thisKey);
        }

        BaggageTrainManager.getInstance().update(thisTrain);

        // remove info from commander unit
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(0);
        thisCarrying.setCarrierId(0);
        thisCommander.setCarrierInfo(thisCarrying);

        // set new position of commander
        thisCommander.setPosition(getCoordsByDirection(direction, thisTrain.getPosition()));

        CommanderManager.getInstance().update(thisCommander);

        return true;
    }

    /**
     * Unload a Brigade from fleet.
     *
     * @param thisBrigade the Brigade to unload.
     * @param thisFleet   the fleet to use.
     * @param direction   the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadBrigadeFromFleet(final Brigade thisBrigade, final Fleet thisFleet, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisFleet.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector == null) {
            return false;
        }
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        for (final Battalion battalion : thisBrigade.getBattalions()) {
            // unload from ship
            unloadBattalionFromShip(battalion);
        }

        // set new position of brigade
        thisBrigade.setPosition(landingPosition);
        BrigadeManager.getInstance().update(thisBrigade);

        return true;
    }

    /**
     * Unload a brigade from a ship.
     *
     * @param thisBrigade the brigade to unload.
     * @param thisShip    the ship to use.
     * @param direction   the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadBrigadeFromShip(final Brigade thisBrigade, final Ship thisShip, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisShip.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        for (final Battalion battalion : thisBrigade.getBattalions()) {
            // unload from ship
            unloadBattalionFromShip(battalion);
        }

        // set new position of brigade
        thisBrigade.setPosition(landingPosition);
        BrigadeManager.getInstance().update(thisBrigade);

        return true;
    }

    /**
     * Unload a Battalion from a ship.
     *
     * @param thisBattalion the Battalion to unload.
     * @return the result of the operation.
     */
    private boolean unloadBattalionFromShip(final Battalion thisBattalion) {
        // unload unit from carrier
        final Ship thisShip = ShipManager.getInstance().getByID(thisBattalion.getCarrierInfo().getCarrierId());
        if (thisShip != null) {
            final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
            int thisKey = 0;

            // Check if a unit is loaded in the carrier
            for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                if (entry.getKey() > GoodConstants.GOOD_LAST) {
                    if (entry.getKey() >= ArmyConstants.BRIGADE * 1000
                            && entry.getKey() < (ArmyConstants.BRIGADE + 1) * 1000
                            && entry.getValue() == thisBattalion.getBrigade().getBrigadeId()) {
                        thisKey = entry.getKey();
                        break;
                    }
                }
            }

            if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == thisBattalion.getBrigade().getBrigadeId()) {
                storedGoods.remove(thisKey);
            }

            ShipManager.getInstance().update(thisShip);
        }

        // remove info from battalion
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(0);
        thisCarrying.setCarrierId(0);
        thisBattalion.setCarrierInfo(thisCarrying);
        BattalionManager.getInstance().update(thisBattalion);

        return true;
    }

    /**
     * Unload a brigade from a baggage train.
     *
     * @param thisBrigade the brigade to unload.
     * @param thisTrain   the baggage train to use.
     * @param direction   the direction to disembark the unit.
     * @return the result of the operation.
     */
    private boolean unloadBrigadeFromTrain(final Brigade thisBrigade, final BaggageTrain thisTrain, final int direction) {
        // Check destination
        final Position landingPosition = getCoordsByDirection(direction, thisTrain.getPosition());
        final Sector landingSector = SectorManager.getInstance().getByPosition(landingPosition);
        if (landingSector == null) {
            return false;
        }
        if (landingSector.getTerrain().getId() == TERRAIN_O || landingSector.getTerrain().getId() == TERRAIN_I) {
            return false;
        }

        // unload unit from carrier
        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
        int thisKey = 0;

        // Check if a unit is loaded in the carrier
        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                if (entry.getKey() >= ArmyConstants.BRIGADE * 1000
                        && entry.getKey() < (ArmyConstants.BRIGADE + 1) * 1000
                        && entry.getValue() == thisBrigade.getBrigadeId()) {
                    thisKey = entry.getKey();
                    break;
                }
            }
        }

        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == thisBrigade.getBrigadeId()) {
            storedGoods.remove(thisKey);
        }

        BaggageTrainManager.getInstance().update(thisTrain);

        // remove info from battalions of brigade
        final CarrierInfo thisCarrying = new CarrierInfo();
        thisCarrying.setCarrierType(0);
        thisCarrying.setCarrierId(0);

        for (final Battalion battalion : thisBrigade.getBattalions()) {
            battalion.setCarrierInfo(thisCarrying);
            BattalionManager.getInstance().update(battalion);
        }

        // set new position of brigade
        thisBrigade.setPosition(getCoordsByDirection(direction, thisTrain.getPosition()));
        BrigadeManager.getInstance().update(thisBrigade);

        return true;
    }

    /**
     * Method that returns the xCoord,yCoord coordinates by the direction
     *
     * @param direction the target direction.
     * @param original  the original position of the carrying vessel.
     * @return a new position.
     */
    protected Position getCoordsByDirection(final int direction, final Position original) {
        final Position newPos = (Position) original.clone();

        switch (direction) {
            case NORTH:
                newPos.setX(original.getX());
                newPos.setY(original.getY() - 1);
                break;

            case SOUTH:
                newPos.setX(original.getX());
                newPos.setY(original.getY() + 1);
                break;

            case EAST:
                newPos.setX(original.getX() + 1);
                newPos.setY(original.getY());
                break;

            case WEST:
                newPos.setX(original.getX() - 1);
                newPos.setY(original.getY());
                break;

            default:
                newPos.setX(original.getX());
                newPos.setY(original.getY());
                break;
        }

        return newPos;
    }

    /**
     * Check if unit can unload to specific sector based on the owner of the sector.
     *
     * @param ownerId         the nation that is trying to unload.
     * @param landingPosition the sector where the troops will be unloaded.
     * @return true, if the nation can unload its troops on the given sector, otherwise false.
     */
    private boolean canUnload(final int ownerId, final Position landingPosition) {
        final Sector sector = SectorManager.getInstance().getByPosition(landingPosition);
        if (sector == null) {
            return true;
        }
        if (sector.getTerrain().getId() == TERRAIN_O || sector.getTerrain().getId() == TERRAIN_I) {
            // we do not check for Sea sectors.
            return true;
        }

        // Ships can move over Neutral Sectors
        // or sectors owned by Friendly and Allied nations.
        if (getSectorOwner(sector).getId() == NATION_NEUTRAL) {
            // this is a neutral sector
            return true;

        } else if (getSectorOwner(sector).getId() == ownerId) {
            // this is the owner of the sector.
            return true;

        } else {
            // Check Sector's relations against owner.
            final NationsRelation relation = RelationsManager.getInstance().getByNations(sector.getPosition().getGame(),
                    getSectorOwner(sector).getId(), ownerId);

            return relation.getRelation() <= REL_PASSAGE
                    || relation.getRelation() == REL_WAR
                    || (sector.getPosition().getRegion().getId() != EUROPE && relation.getRelation() >= REL_COLONIAL_WAR);
        }
    }

    /**
     * Identify the current owner of the sector.
     *
     * @param sector the sector to inspect.
     * @return the owner.
     */
    protected final Nation getSectorOwner(final Sector sector) {
        final Nation sectorOwner;
        if (sector.getTempNation() != null
                && sector.getTempNation().getId() != 0
                && sector.getTempNation().getId() != NATION_NEUTRAL) {
            sectorOwner = sector.getTempNation();

        } else {
            sectorOwner = sector.getNation();
        }

        return sectorOwner;
    }

}
