package com.eaw1805.events.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.fleet.ShipTypeManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.fleet.ShipType;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Completes the construction of ship classes 3-5.
 */
public class ConstructShipsEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ConstructShipsEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ConstructShipsEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("ConstructShipsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Game thisGame = getParent().getGameEngine().getGame();

        // Check for class-3 ships
        final List<PlayerOrder> orderList = PlayerOrderManager.getInstance().listByShipConstruction(thisGame, thisGame.getTurn());
        for (final PlayerOrder playerOrder : orderList) {
            final int sectorId = Integer.parseInt(playerOrder.getParameter1());

            // Retrieve the sector that we wish to build the ship
            final Sector thisSector = SectorManager.getInstance().getByID(sectorId);

            final int shipTypeId = Integer.parseInt(playerOrder.getParameter2());
            final String shipName = playerOrder.getParameter3();

            if (playerOrder.getTurn() == thisGame.getTurn() - 1) {
                // This is a class 3 or class 4 ship that started construction last turn
                // Or a Class 5 that started constructions before 2 turns.
                // This month the ship is constructed.

                // Retrieve the ship type
                final ShipType shipTPE = ShipTypeManager.getInstance().getByType(shipTypeId);

                // Make sure that the owner is the same
                if (thisSector.getNation().getId() == playerOrder.getNation().getId()) {
                    // Create new ship
                    Ship newShip = new Ship();
                    newShip.setNation(playerOrder.getNation());
                    newShip.setType(shipTPE);
                    newShip.setName(shipName);
                    newShip.setPosition(thisSector.getPosition());
                    newShip.setCondition(100);
                    newShip.setExp(0);
                    newShip.setMarines(shipTPE.getCitizens());
                    newShip.setJustConstructed(true);

                    final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>();
                    for (int goodID = GoodConstants.GOOD_FIRST; goodID <= GoodConstants.GOOD_COLONIAL; goodID++) {
                        qteGoods.put(goodID, 0);
                    }
                    newShip.setStoredGoods(qteGoods);

                    ShipManager.getInstance().add(newShip);

                    final String description = "The construction of ship '" + shipName + "' was completed at " + thisSector.getPosition().toString();

                    playerOrder.setResult(1);
                    playerOrder.setExplanation(playerOrder.getExplanation() + description);
                    PlayerOrderManager.getInstance().update(playerOrder);

                    newsSingle(playerOrder.getNation(), NEWS_MILITARY, newShip.getType().getTypeId(), description);

                } else {
                    final String description = "The construction of ship '" + shipName + "' was disrupted when we lost control of " + thisSector.getPosition().toString();

                    // The ship got destroyed
                    playerOrder.setResult(-playerOrder.getResult());
                    playerOrder.setExplanation(playerOrder.getExplanation() + description);
                    PlayerOrderManager.getInstance().update(playerOrder);

                    newsSingle(playerOrder.getNation(), NEWS_MILITARY, description);
                }
            } else if (playerOrder.getTurn() == thisGame.getTurn() - 2) {
                // Construction requires one more month.
                newsSingle(playerOrder.getNation(), NEWS_MILITARY, "The construction of ship '" + shipName + "' continues for one more month at " + thisSector.getPosition().toString());

            }
        }

        LOGGER.info("ConstructShipsEvent processed.");
    }

}
