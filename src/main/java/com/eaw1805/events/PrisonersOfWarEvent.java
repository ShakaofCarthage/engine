package com.eaw1805.events;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Reports Prisoners of War.
 */
public class PrisonersOfWarEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, NationConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(PrisonersOfWarEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public PrisonersOfWarEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("PrisonersOfWarEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final NumberFormat formatter = new DecimalFormat("#,###,###");
        final List<Nation> activeNations = getParent().getGameEngine().getAliveNations();
        for (Nation thisNation : activeNations) {
            if (thisNation.getId() == NATION_NEUTRAL) {
                continue;
            }

            final List<NationsRelation> allRelations = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), thisNation);
            for (final NationsRelation relation : allRelations) {
                final StringBuilder descrPOW = new StringBuilder();
                final StringBuilder descrPOWOther = new StringBuilder();

                if (relation.getPrisoners() > 0) {
                    // Each month, a random number of prisoners (5-10%) die from hardships, bad nutrition etc.
                    final int deathRate = getRandomGen().nextInt(5) + 2;
                    final int deadPOW = relation.getPrisoners() * deathRate / 100;
                    relation.setPrisoners(relation.getPrisoners() - deadPOW);
                    if (relation.getPrisoners() < 0) {
                        relation.setPrisoners(0);
                    }

                    if (relation.getPrisoners() > 0) {
                        descrPOW.append(formatter.format(relation.getPrisoners()));
                        descrPOW.append(" prisoners from ");
                        descrPOW.append(relation.getTarget().getName());
                        descrPOW.append(" (");
                        descrPOW.append(formatter.format(deadPOW));
                        descrPOW.append(" died this month due to hardships and bad nutrition)");

                        descrPOWOther.append(thisNation.getName());
                        descrPOWOther.append(" holds ");
                        descrPOWOther.append(formatter.format(relation.getPrisoners()));
                        descrPOWOther.append(" of our soldiers as prisoners (");
                        descrPOWOther.append(formatter.format(deadPOW));
                        descrPOWOther.append(" died this month due to hardships and bad nutrition)");

                        newsPair(thisNation, relation.getTarget(), NEWS_POLITICAL,
                                descrPOW.toString(), descrPOWOther.toString());
                    }
                }

                // keep history
                relation.setTurnCount(relation.getTurnCount() + 1);

                // Check if there was a piece treaty
                if (relation.getPeaceCount() > 0) {

                    if (relation.getPeaceCount() == 3) {
                        // After 3 months change relation to trade
                        relation.setRelation(REL_TRADE);

                        // Increase counter
                        relation.setPeaceCount(4);

                        // Remove ships from port
                        removeShips(getParent().getGameEngine().getGame(), thisNation, relation.getTarget());
                        removeShips(getParent().getGameEngine().getGame(), relation.getTarget(), thisNation);

                    } else if (relation.getPeaceCount() > 12) {
                        // After 12 months stop counting
                        relation.setPeaceCount(0);

                    } else {
                        // Increase counter
                        relation.setPeaceCount(relation.getPeaceCount() + 1);
                    }
                }

                // Check if there was a surrender
                if (relation.getSurrenderCount() > 0) {

                    if (relation.getSurrenderCount() == 3) {
                        // After 3 months change relation to trade
                        relation.setRelation(REL_TRADE);

                        // Increase counter
                        relation.setSurrenderCount(4);

                        // Remove ships from port
                        removeShips(getParent().getGameEngine().getGame(), thisNation, relation.getTarget());
                        removeShips(getParent().getGameEngine().getGame(), relation.getTarget(), thisNation);

                    } else if (relation.getSurrenderCount() > 12) {
                        // After 12 months stop counting
                        relation.setSurrenderCount(0);

                    } else {
                        // increase counter
                        relation.setSurrenderCount(relation.getSurrenderCount() + 1);
                    }
                }

                RelationsManager.getInstance().update(relation);
            }
        }
    }

    /**
     * Move ships out of enemy controlled shipyards.
     *
     * @param thisGame the game to inspect.
     * @param owner    the nation declared war.
     * @param newEnemy the owner to check ships.
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
