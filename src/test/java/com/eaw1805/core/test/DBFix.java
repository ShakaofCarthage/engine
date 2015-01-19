package com.eaw1805.core.test;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fixes the database.
 */
public class DBFix
        implements RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DBFix.class);

    public void fixCommanders() {
        final List<Commander> lstCommanders = CommanderManager.getInstance().list();
        for (final Commander commander : lstCommanders) {
            if (commander.getCorp() != 0) {
                // retrieve corps
                final Corp corp = CorpManager.getInstance().getByID(commander.getCorp());

                if (corp.getCommander() == null || corp.getCommander().getId() != commander.getId()) {
                    LOGGER.debug(commander.getNation().getName() +
                            " - commander [" + commander.getName() + "] at " +
                            commander.getPosition() +
                            " is not properly leading CORPS [" + corp.getCorpId() + "/" + corp.getPosition().getGame().getGameId() + "]");

                    corp.setCommander(commander);
                    CorpManager.getInstance().update(corp);

                    commander.setPosition((Position) corp.getPosition().clone());
                    CommanderManager.getInstance().update(commander);
                }

                if (!corp.getPosition().equals(commander.getPosition())) {
                    LOGGER.debug(commander.getNation().getName() +
                            " - commander [" + commander.getName() + "] at " +
                            commander.getPosition() +
                            " is misplaced from CORPS [" + corp.getCorpId() + "/" + corp.getPosition().getGame().getGameId() + "]");

                    commander.setPosition((Position) corp.getPosition().clone());
                    CommanderManager.getInstance().update(commander);
                }
            }

            if (commander.getArmy() != 0) {
                // retrieve army
                final Army army = ArmyManager.getInstance().getByID(commander.getArmy());

                if (army.getCommander() == null || army.getCommander().getId() != commander.getId()) {
                    LOGGER.debug(commander.getNation().getName() +
                            " - commander [" + commander.getName() + "] at " +
                            commander.getPosition() +
                            " is not properly leading ARMY [" + army.getArmyId() + "/" + army.getPosition().getGame().getGameId() + "]");

                    army.setCommander(commander);
                    ArmyManager.getInstance().update(army);

                    commander.setPosition((Position) army.getPosition().clone());
                    CommanderManager.getInstance().update(commander);
                }

                if (!army.getPosition().equals(commander.getPosition())) {
                    LOGGER.debug(commander.getNation().getName() +
                            " - commander [" + commander.getName() + "] at " +
                            commander.getPosition() +
                            " is misplaced from ARMY [" + army.getArmyId() + "/" + army.getPosition().getGame().getGameId() + "]");

                    commander.setPosition((Position) army.getPosition().clone());
                    CommanderManager.getInstance().update(commander);
                }
            }
        }
    }

    public void fixLoadedCommanders() {
        final List<Commander> lstCommanders = CommanderManager.getInstance().list();
        for (final Commander commander : lstCommanders) {
            if (commander.getCarrierInfo() != null
                    && commander.getCarrierInfo().getCarrierId() != 0) {
                // Try to locate object
                if (commander.getCarrierInfo().getCarrierType() == ArmyConstants.SHIP) {
                    // Retrieve unit
                    final Ship thisShip = ShipManager.getInstance().getByID(commander.getCarrierInfo().getCarrierId());

                    if (thisShip == null) {

                        LOGGER.debug(commander.getNation().getName() +
                                " - commander [" + commander.getName() + "] at " +
                                commander.getPosition() +
                                " is misreported as loaded to a SHIP [" + commander.getId() + "/" + commander.getPosition().getGame().getGameId() + "]");

                        // Remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        commander.setCarrierInfo(thisCarrying);

                        // Move to Pool
                        commander.setPool(true);
                        commander.setInTransit(false);
                        commander.setTransit(0);

                        CommanderManager.getInstance().update(commander);
                    }

                } else if (commander.getCarrierInfo().getCarrierType() == ArmyConstants.BAGGAGETRAIN) {
                    // Retrieve unit
                    final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(commander.getCarrierInfo().getCarrierId());

                    if (thisTrain == null) {

                        LOGGER.debug(commander.getNation().getName() +
                                " - commander [" + commander.getName() + "] at " +
                                commander.getPosition() +
                                " is misreported as loaded to a BAGGAGETRAIN [" + commander.getId() + "/" + commander.getPosition().getGame().getGameId() + "]");

                        // Remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        commander.setCarrierInfo(thisCarrying);

                        // Move to Pool
                        commander.setPool(true);
                        commander.setInTransit(false);
                        commander.setTransit(0);

                        CommanderManager.getInstance().update(commander);
                    }
                }
            }
        }
    }

    public void fixLoadedSpy() {
        final List<Spy> lstSpies = SpyManager.getInstance().list();
        for (final Spy spy : lstSpies) {
            if (spy.getCarrierInfo() != null
                    && spy.getCarrierInfo().getCarrierId() != 0) {
                // Try to locate object
                if (spy.getCarrierInfo().getCarrierType() == ArmyConstants.SHIP) {
                    // Retrieve unit
                    final Ship thisShip = ShipManager.getInstance().getByID(spy.getCarrierInfo().getCarrierId());

                    if (thisShip == null) {

                        LOGGER.debug(spy.getNation().getName() +
                                " - spy [" + spy.getName() + "] at " +
                                spy.getPosition() +
                                " is misreported as loaded to a SHIP [" + spy.getSpyId() + "/" + spy.getPosition().getGame().getGameId() + "]");

                        // Remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        spy.setCarrierInfo(thisCarrying);

                        // Identify sector
                        final Sector thisSector = SectorManager.getInstance().getByPosition(spy.getPosition());

                        if (thisSector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                            final List<Sector> lstSector = SectorManager.getInstance().listByGameRegionNation(spy.getPosition().getGame(),
                                    spy.getPosition().getRegion(), spy.getNation());
                            if (!lstSector.isEmpty()) {
                                spy.setPosition(lstSector.get(0).getPosition());

                            } else {
                                final Region europe = RegionManager.getInstance().getByID(RegionConstants.EUROPE);
                                final List<Sector> lstSectorsEurope = SectorManager.getInstance().listByGameRegionNation(spy.getPosition().getGame(),
                                        europe, spy.getNation());

                                spy.setPosition(lstSectorsEurope.get(0).getPosition());
                            }
                        }

                        SpyManager.getInstance().update(spy);
                    }

                } else if (spy.getCarrierInfo().getCarrierType() == ArmyConstants.BAGGAGETRAIN) {
                    // Retrieve unit
                    final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(spy.getCarrierInfo().getCarrierId());

                    if (thisTrain == null) {

                        LOGGER.debug(spy.getNation().getName() +
                                " - spy [" + spy.getName() + "] at " +
                                spy.getPosition() +
                                " is misreported as loaded to a BAGGAGETRAIN [" + spy.getSpyId() + "/" + spy.getPosition().getGame().getGameId() + "]");

                        // Remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        spy.setCarrierInfo(thisCarrying);

                        // Identify sector
                        final Sector thisSector = SectorManager.getInstance().getByPosition(spy.getPosition());

                        if (thisSector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                            final List<Sector> lstSector = SectorManager.getInstance().listByGameRegionNation(spy.getPosition().getGame(),
                                    spy.getPosition().getRegion(), spy.getNation());
                            if (!lstSector.isEmpty()) {
                                spy.setPosition(lstSector.get(0).getPosition());

                            } else {
                                final Region europe = RegionManager.getInstance().getByID(RegionConstants.EUROPE);
                                final List<Sector> lstSectorsEurope = SectorManager.getInstance().listByGameRegionNation(spy.getPosition().getGame(),
                                        europe, spy.getNation());

                                spy.setPosition(lstSectorsEurope.get(0).getPosition());
                            }
                        }

                        SpyManager.getInstance().update(spy);
                    }
                }
            }
        }
    }

    public void fixLoadedUnits() {
        final List<Ship> lstShips = ShipManager.getInstance().list();
        for (final Ship thisShip : lstShips) {
            final List<Integer> removedSlots = new ArrayList<Integer>();

            // Check if a unit is loaded in the ship
            final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
            for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                if (entry.getKey() > GoodConstants.GOOD_LAST) {
                    if (entry.getKey() >= ArmyConstants.SPY * 1000) {
                        // A spy is loaded                        
                        final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());

                        LOGGER.debug(thisShip.getNation().getName() +
                                " - ship [" + thisShip.getName() + "] at " +
                                thisShip.getPosition() +
                                " is loaded with a SPY " +
                                thisSpy.getName() + "[" + thisSpy.getSpyId() + "/" + thisSpy.getPosition().getGame().getGameId() + "]");

                        if (!thisSpy.getPosition().equals(thisShip.getPosition())) {
                            LOGGER.debug("Position mismatch! Fixing position of loaded unit");
                            thisSpy.setPosition(thisShip.getPosition());
                            SpyManager.getInstance().update(thisSpy);
                        }

                        if (thisSpy.getCarrierInfo().getCarrierType() != ArmyConstants.SHIP
                                && thisSpy.getCarrierInfo().getCarrierId() != thisShip.getShipId()) {
                            LOGGER.debug("Carrier Info mismatch! Fixing carrier info of loaded unit");

                            // Keep info in each battalion of the brigade
                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(ArmyConstants.SHIP);
                            thisCarrying.setCarrierId(thisShip.getShipId());

                            thisSpy.setCarrierInfo(thisCarrying);
                            SpyManager.getInstance().update(thisSpy);
                        }


                    } else if (entry.getKey() >= ArmyConstants.COMMANDER * 1000) {
                        // A commander is loaded
                        final Commander thisCommander = CommanderManager.getInstance().getByID(entry.getValue());

                        LOGGER.debug(thisShip.getNation().getName() +
                                " - ship [" + thisShip.getName() + "] at " +
                                thisShip.getPosition() +
                                " is loaded with a COMMANDER " +
                                thisCommander.getName() + "[" + thisCommander.getId() + "/" + thisCommander.getPosition().getGame().getGameId() + "]");

                        if (!thisCommander.getPosition().equals(thisShip.getPosition())) {
                            LOGGER.debug("Position mismatch! Fixing position of loaded unit");
                            thisCommander.setPosition(thisShip.getPosition());
                            CommanderManager.getInstance().update(thisCommander);
                        }

                        if (thisCommander.getCarrierInfo().getCarrierType() != ArmyConstants.SHIP
                                && thisCommander.getCarrierInfo().getCarrierId() != thisShip.getShipId()) {
                            LOGGER.debug("Carrier Info mismatch! Fixing carrier info of loaded unit");

                            // Keep info in each battalion of the brigade
                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(ArmyConstants.SHIP);
                            thisCarrying.setCarrierId(thisShip.getShipId());

                            thisCommander.setCarrierInfo(thisCarrying);
                            CommanderManager.getInstance().update(thisCommander);
                        }

                    } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                        // A Brigade is loaded
                        final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());

                        if (thisBrigade == null) {
                            LOGGER.debug(thisShip.getNation().getName() +
                                    " - ship [" + thisShip.getName() + "/" + thisShip.getShipId() + "] at " +
                                    thisShip.getPosition() +
                                    " is loaded with a UNKNOWN BRIGADE !" +
                                    "[" + entry.getKey() + "/" + thisShip.getPosition().getGame().getGameId() + "]");

                            removedSlots.add(entry.getKey());

                        } else {

                            LOGGER.debug(thisShip.getNation().getName() +
                                    " - ship [" + thisShip.getName() + "] at " +
                                    thisShip.getPosition() +
                                    " is loaded with a BRIGADE " +
                                    thisBrigade.getName() + "[" + thisBrigade.getBrigadeId() + "/" + thisBrigade.getPosition().getGame().getGameId() + "]");

                            if (!thisBrigade.getPosition().equals(thisShip.getPosition())) {
                                LOGGER.debug("Position mismatch! Fixing position of loaded unit");
                                thisBrigade.setPosition(thisShip.getPosition());
                                BrigadeManager.getInstance().update(thisBrigade);
                            }

                            for (final Battalion battalion : thisBrigade.getBattalions()) {

                                if (battalion.getCarrierInfo().getCarrierType() != ArmyConstants.SHIP
                                        && battalion.getCarrierInfo().getCarrierId() != thisShip.getShipId()) {
                                    LOGGER.debug("Carrier Info mismatch! Fixing carrier info of loaded unit");

                                    // Keep info in each battalion of the brigade
                                    final CarrierInfo thisCarrying = new CarrierInfo();
                                    thisCarrying.setCarrierType(ArmyConstants.SHIP);
                                    thisCarrying.setCarrierId(thisShip.getShipId());

                                    battalion.setCarrierInfo(thisCarrying);
                                    BattalionManager.getInstance().update(battalion);
                                }
                            }
                        }
                    }
                }
            }

            for (int removedSlot : removedSlots) {
                thisShip.getStoredGoods().remove(removedSlot);
            }
            ShipManager.getInstance().update(thisShip);
        }
    }

    public void fixFleetPositions() {
        final List<Fleet> fleets = FleetManager.getInstance().list();
        for (final Fleet fleet : fleets) {
            // Retrieve ships
            final List<Ship> ships = ShipManager.getInstance().listByFleet(fleet.getPosition().getGame(), fleet.getFleetId());
            for (final Ship ship : ships) {
                if (!ship.getPosition().equals(fleet.getPosition())) {
                    LOGGER.error("Ship " + ship.getShipId() + "/" + ship.getPosition().getGame().getGameId() + " misplaced from Fleet " + fleet.getFleetId());
                    ship.setPosition(fleet.getPosition());
                    ShipManager.getInstance().update(ship);
                }

                if (ship.getNation().getId() != fleet.getNation().getId()) {
                    LOGGER.error("Ship " + ship.getShipId() + "/" + ship.getPosition().getGame().getGameId() + " belongs to enemy fleet " + fleet.getFleetId());
                    ship.setFleet(0);
                    ShipManager.getInstance().update(ship);
                }
            }
        }
    }

    public void fixTradeCities() {
        final List<TradeCity> tcities = TradeCityManager.getInstance().list();
        for (final TradeCity city : tcities) {
            final Map<Integer, Integer> tradeRates = city.getGoodsTradeLvl();
            for (final Integer goodId : tradeRates.keySet()) {
                if (tradeRates.get(goodId) < 1) {
                    LOGGER.error("Trade City " + city.getName() + " on Good " + goodId + " has value " + tradeRates.get(goodId));
                    tradeRates.put(goodId, 1);
                }
            }
            city.setGoodsTradeLvl(tradeRates);
            TradeCityManager.getInstance().update(city);
        }
    }

    public void fixArmyPositions() {
        final List<Army> armies = ArmyManager.getInstance().list();
        for (final Army army : armies) {
            // Retrieve corps
            final List<Corp> corps = CorpManager.getInstance().listByArmy(army.getPosition().getGame(), army.getArmyId());
            for (final Corp corp : corps) {
                if (!corp.getPosition().equals(army.getPosition())) {
                    LOGGER.error("Corps " + corp.getCorpId() + " misplaced from Army " + army.getArmyId());
                    corp.setPosition(army.getPosition());
                    CorpManager.getInstance().update(corp);
                }
            }
        }
    }

    public void fixCorpsPositions() {
        final List<Corp> corps = CorpManager.getInstance().list();
        for (final Corp corp : corps) {
            // Retrieve brigades
            final List<Brigade> brigades = BrigadeManager.getInstance().listByCorp(corp.getPosition().getGame(), corp.getCorpId());
            for (final Brigade brigade : brigades) {
                if (!brigade.getPosition().equals(corp.getPosition())) {
                    LOGGER.error("Brigade " + brigade.getBrigadeId() + " misplaced from Corps " + corp.getCorpId());
                    brigade.setPosition(corp.getPosition());
                    BrigadeManager.getInstance().update(brigade);
                }
            }
        }
    }

    public void fixRelations() {
        final List<NationsRelation> relationList = RelationsManager.getInstance().list();
        for (final NationsRelation relation : relationList) {
            // Look-up reverse relation
            NationsRelation reverse = RelationsManager.getInstance().getByNations(relation.getGame(), relation.getTarget(), relation.getNation());

            if (relation.getRelation() > reverse.getRelation()) {
                relation.setRelation(reverse.getRelation());
                RelationsManager.getInstance().update(relation);

                LOGGER.error("Fixing relations between " + relation.getNation().getName() + " and " + relation.getTarget().getName());

            } else if (relation.getRelation() < reverse.getRelation()) {
                reverse.setRelation(relation.getRelation());
                RelationsManager.getInstance().update(reverse);

                LOGGER.error("Fixing relations between " + reverse.getNation().getName() + " and " + reverse.getTarget().getName());
            }

            if (relation.getTurnCount() > reverse.getTurnCount()) {
                reverse.setTurnCount(relation.getTurnCount());
                RelationsManager.getInstance().update(reverse);

                LOGGER.error("Fixing relations turn-counter between " + relation.getNation().getName() + " and " + relation.getTarget().getName());

            } else if (relation.getTurnCount() < reverse.getTurnCount()) {
                relation.setTurnCount(reverse.getTurnCount());
                RelationsManager.getInstance().update(relation);

                LOGGER.error("Fixing relations turn-counter between " + relation.getNation().getName() + " and " + relation.getTarget().getName());
            }
        }
    }

    public void fixBattalionSlots() {
        final List<Brigade> brigadeList = BrigadeManager.getInstance().list();
        for (final Brigade thisBrigade : brigadeList) {
            int slot = 1;
            for (final Battalion battalion : thisBrigade.getBattalions()) {
                battalion.setOrder(slot);
                slot++;
                BattalionManager.getInstance().update(battalion);
            }
            LOGGER.debug(thisBrigade.getBrigadeId() + "\t" + (slot - 1));
            BrigadeManager.getInstance().update(thisBrigade);
        }
    }

    public void fixUnloadedBattalions() {
        final List<Brigade> brigadeList = BrigadeManager.getInstance().list();
        for (final Brigade thisBrigade : brigadeList) {
            for (final Battalion battalion : thisBrigade.getBattalions()) {
                // Check if battalion is loaded
                if (battalion.getCarrierInfo().getCarrierId() != 0) {
                    boolean found = false;
                    switch (battalion.getCarrierInfo().getCarrierType()) {
                        case ArmyConstants.SHIP: {
                            final Ship thisShip = ShipManager.getInstance().getByID(battalion.getCarrierInfo().getCarrierId());
                            if (thisShip != null) {
                                // Check if a unit is loaded in the ship
                                final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
                                for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                    if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                        if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                                            if (entry.getValue() == battalion.getBrigade().getBrigadeId()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (!found) {
                                LOGGER.debug(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " is NOT loaded with BRIGADE " +
                                        thisBrigade.getName() + "[" + thisBrigade.getBrigadeId() + "/" + thisBrigade.getPosition().getGame().getGameId() + "] at " +
                                        thisBrigade.getPosition());

                                // Remove carrier info
                                final CarrierInfo thisCarrying = new CarrierInfo();
                                thisCarrying.setCarrierType(0);
                                thisCarrying.setCarrierId(0);
                                battalion.setCarrierInfo(thisCarrying);

                                BattalionManager.getInstance().update(battalion);
                            }
                            break;
                        }

                        case ArmyConstants.BAGGAGETRAIN: {
                            final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(battalion.getCarrierInfo().getCarrierId());

                            if (thisTrain != null) {
                                // Check if a unit is loaded in the train
                                final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
                                for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                    if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                        if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                                            if (entry.getValue() == battalion.getBrigade().getBrigadeId()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (!found) {
                                LOGGER.error("Brigade " + battalion.getBrigade().getBrigadeId() + " misreported as loaded on Baggage Train " + battalion.getCarrierInfo().getCarrierId());
                            }

                            break;
                        }

                        default:
                            LOGGER.error("Brigade " + battalion.getBrigade().getBrigadeId() + " loaded on Unknown carrier type");
                    }
                }
            }
        }
    }

    /**
     * Simple execution.
     *
     * @param args no arguments needed here
     */
    public static void main(final String[] args) {

        // check arguments
        if (args.length != 3) {
            LOGGER.fatal("DBfix arguments (gameId, scenarioId, basePath) are missing");
            return;
        }

        // Retrieve gameId
        int gameId = 0;
        try {
            gameId = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            LOGGER.warn("Could not parse gameId");
        }

        // Retrieve scenarioId
        int scenarioId = 0;
        try {
            scenarioId = Integer.parseInt(args[1]);

        } catch (Exception ex) {
            LOGGER.warn("Could not parse scenarioId");
        }

        String basePath = "/srv/eaw1805";
        if (args[2].length() > 2) {
            basePath = args[2];
        } else {
            LOGGER.warn("Using default path: " + basePath);
        }


        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(scenarioId);

        final DBFix thisFix = new DBFix();

        // Make sure we have an active transaction
        final Transaction thatTrans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        thisFix.fixRelations();
        thisFix.fixFleetPositions();
        thisFix.fixLoadedUnits();
        thisFix.fixLoadedCommanders();
        thisFix.fixLoadedSpy();
        thisFix.fixUnloadedBattalions();
        thisFix.fixCommanders();
        thatTrans.commit();
    }

}
