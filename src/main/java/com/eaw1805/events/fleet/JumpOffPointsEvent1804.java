package com.eaw1805.events.fleet;


import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transfer Fleets and Ships across regions via the jump-off points.
 */
public class JumpOffPointsEvent1804
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(JumpOffPointsEvent1805.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public JumpOffPointsEvent1804(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("JumpOffPointsEvent1805 instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        // At the end of movement order 1, all unengaged ships move off the map.
        final List<Fleet> fleetList = FleetManager.getInstance().listGame(getParent().getGameEngine().getGame());
        for (final Fleet fleet : fleetList) {
            final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getParent().getGameEngine().getGame(), fleet.getFleetId());
            if (lstShips.isEmpty()
                    || !lstShips.get(0).getHasMoved()) {//if fleet has moved we don't want it to jump anywhere
                continue;
            }

            // Check jump-off points for ships
            final Position jumpOffSector = getJumpOff(fleet.getPosition());
            if (jumpOffSector != null) {
                // Check if ship has participated in a naval battle this turn
                boolean participatedInNaval = false;

                for (final Ship thisShip : lstShips) {
                    if (thisShip.getNavalBattle()) {
                        participatedInNaval = true;
                        break;
                    }

                    // initialize non-volatile parameters
                    thisShip.initializeVariables();
                }

                if (!participatedInNaval) {

                    // everything OK, update position
                    fleet.setPosition(jumpOffSector);

                    // apply attrition 1%
                    for (final Ship thisShip : lstShips) {
                        thisShip.setPosition(jumpOffSector);

                        // apply attrition 1%
                        thisShip.setCondition(thisShip.getCondition() - 1);

                        // Check if a spy is loaded on this ship
                        if (thisShip.getHasSpy()) {
                            final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
                            final List<Integer> unitsRemoved = new ArrayList<Integer>();

                            for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                    if (entry.getKey() >= ArmyConstants.SPY * 1000) {

                                        unitsRemoved.add(entry.getKey());

                                        // Kill spy
                                        final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());
                                        report(thisSpy.getNation(), "spy.death", "Spy '" + thisSpy.getName() + "' died while the ship that was carrying him moved to another continent.");
                                        newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy '" + thisSpy.getName() + "' died while the ship that was carrying him moved to another continent.");

                                        // Remove spy from game
                                        SpyManager.getInstance().delete(thisSpy);
                                    }
                                }
                            }

                            // remove slots
                            for (final int loadedUnitSlot : unitsRemoved) {
                                thisShip.getStoredGoods().remove(loadedUnitSlot);
                            }
                        }

                        // Check if ship is carrying citizens from Colonies to Europe
                        if (jumpOffSector.getRegion().getId() == EUROPE) {
                            if (thisShip.getStoredGoods() != null
                                    && thisShip.getStoredGoods().containsKey(GoodConstants.GOOD_PEOPLE)
                                    && thisShip.getStoredGoods().get(GoodConstants.GOOD_PEOPLE) > 0) {
                                // Kill citizens
                                newsSingle(thisShip.getNation(), NEWS_POLITICAL, "Colonial citizens on board '" + thisShip.getName() + "' died from a disease while the ship that was carrying them moved to Europe.");
                                thisShip.getStoredGoods().put(GoodConstants.GOOD_PEOPLE, 0);
                            }
                        }

                        // Check if the entity is carrying units, and update their position too
                        if (thisShip.getHasCommander() || thisShip.getHasSpy() || thisShip.getHasTroops()) {
                            thisShip.updatePosition((Position) jumpOffSector.clone());
                        }

                        ShipManager.getInstance().update(thisShip);
                    }

                    FleetManager.getInstance().update(fleet);
                }
            }
        }

        final List<Ship> shipList = ShipManager.getInstance().listFreeByGame(getParent().getGameEngine().getGame());
        for (final Ship ship : shipList) {
            if (!ship.getHasMoved()) {//if ship has moved we don't want it to jump anywhere
                continue;
            }
            // Check jump-off points for ships
            final Position jumpOffSector = getJumpOff(ship.getPosition());
            if (jumpOffSector != null) {
                // Check if ship has participated in a naval battle this turn
                if (!ship.getNavalBattle()) {
                    // everything OK, update position
                    ship.setPosition(jumpOffSector);

                    // apply attrition 1%
                    ship.setCondition(ship.getCondition() - 1);

                    // initialize non-volatile parameters
                    ship.initializeVariables();

                    // Check if a spy is loaded on this ship
                    if (ship.getHasSpy()) {
                        final Map<Integer, Integer> storedGoods = ship.getStoredGoods();
                        final List<Integer> unitsRemoved = new ArrayList<Integer>();

                        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                if (entry.getKey() >= ArmyConstants.SPY * 1000) {

                                    unitsRemoved.add(entry.getKey());

                                    // Kill spy
                                    final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());
                                    report(thisSpy.getNation(), "spy.death", "Spy '" + thisSpy.getName() + "' died while the ship that was carrying him moved to another continent.");
                                    newsSingle(thisSpy.getNation(), NEWS_POLITICAL, "Our spy '" + thisSpy.getName() + "' died while the ship that was carrying him moved to another continent.");

                                    // Remove spy from game
                                    SpyManager.getInstance().delete(thisSpy);
                                }
                            }
                        }

                        // remove slots
                        for (final int loadedUnitSlot : unitsRemoved) {
                            ship.getStoredGoods().remove(loadedUnitSlot);
                        }
                    }

                    // Check if ship is carrying citizens from Colonies to Europe
                    if (jumpOffSector.getRegion().getId() == EUROPE) {
                        if (ship.getStoredGoods() != null
                                && ship.getStoredGoods().containsKey(GoodConstants.GOOD_PEOPLE)
                                && ship.getStoredGoods().get(GoodConstants.GOOD_PEOPLE) > 0) {
                            // Kill citizens
                            newsSingle(ship.getNation(), NEWS_POLITICAL, "Colonial citizens on board '" + ship.getName() + "' died from a disease while the ship that was carrying them moved to Europe.");
                            ship.getStoredGoods().put(GoodConstants.GOOD_PEOPLE, 0);
                        }
                    }

                    // Check if the entity is carrying units, and update their position too
                    if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                        ship.updatePosition((Position) jumpOffSector.clone());
                    }

                    ShipManager.getInstance().update(ship);
                }
            }
        }

        // Remove all flags
        ShipManager.getInstance().removeNavalFlag(getParent().getGameEngine().getGame());

        LOGGER.info("JumpOffPointsEvent1805 processed.");
    }

    /**
     * Check if this sector is a jump-off point and retrieve it in case it is.
     *
     * @param thisPos the position to check.
     * @return if it is a jump-off point, the corresponding position at the other end.
     */

    private Position getJumpOff(final Position thisPos) {
        Position jumpOffSector = null;
        switch (thisPos.getRegion().getId()) {
            case EUROPE:
                jumpOffSector = getJumpOffEurope(thisPos);
                break;

            case CARIBBEAN:
                jumpOffSector = getJumpOffCaribbean(thisPos);
                break;

            default:
                // do nothing
        }

        return jumpOffSector;
    }

    /**
     * Check if this sector is a jump-off point and retrieve it in case it is.
     *
     * @param thisPos the position to check.
     * @return if it is a jump-off point, the corresponding position at the other end.
     */
    private Position getJumpOffCaribbean(final Position thisPos) {
        Position jumpOffSector = null;
        if (thisPos.getX() == 39 && (thisPos.getY() >= 7 && thisPos.getY() <= 10)) {
            // Jump to Europe
            final Position thatPos = new Position();
            thatPos.setRegion(RegionManager.getInstance().getByID(EUROPE));
            thatPos.setGame(thisPos.getGame());
            thatPos.setX(0);
            thatPos.setY(thisPos.getY());
            jumpOffSector = thatPos;

        }
        return jumpOffSector;
    }

    /**
     * Check if this sector is a jump-off point and retrieve it in case it is.
     *
     * @param thisPos the position to check.
     * @return if it is a jump-off point, the corresponding position at the other end.
     */
    private Position getJumpOffEurope(final Position thisPos) {
        Position jumpOffSector = null;
        if (thisPos.getX() == 0 && (thisPos.getY() >= 7 && thisPos.getY() <= 10)) {
            // Jump to Caribbean
            final Position thatPos = new Position();
            thatPos.setRegion(RegionManager.getInstance().getByID(CARIBBEAN));
            thatPos.setGame(thisPos.getGame());
            thatPos.setX(39);
            thatPos.setY(thisPos.getY());
            jumpOffSector = thatPos;
        }
        return jumpOffSector;
    }

}
