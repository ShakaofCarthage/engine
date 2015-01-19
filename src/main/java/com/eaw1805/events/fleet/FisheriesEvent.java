package com.eaw1805.events.fleet;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.economy.Good;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Use Merchant ships as fishing boats to produce food.
 */
public class FisheriesEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, GoodConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(FisheriesEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public FisheriesEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("FisheriesEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Map<Nation, Set<Sector>> fisheries = new HashMap<Nation, Set<Sector>>();
        final int totFish[] = new int[NATION_LAST + 1];

        // Initialize map
        final List<Nation> lstNations = NationManager.getInstance().list();
        lstNations.remove(0); // Remove neutrals
        for (final Nation nation : lstNations) {
            fisheries.put(nation, new HashSet<Sector>());
            totFish[nation.getId()] = 0;
        }

        // Produce food from fisheries
        final Good food = GoodManager.getInstance().getByID(GOOD_FOOD);
        final List<Sector> seaSectors = SectorManager.getInstance().listSeaByGame(getParent().getGameEngine().getGame());
        for (final Sector thisSector : seaSectors) {
            // Check if any ships are stationed here
            final List<Ship> shipList = ShipManager.getInstance().listByGamePosition(thisSector.getPosition());
            if (!shipList.isEmpty()) {
                // Count number of merchant ships
                int totShips = 0;
                for (final Ship ship : shipList) {
                    if (ship.getType().getShipClass() == 0 || ship.getType().getIntId() == 31) {
                        totShips++;
                    }
                }

                if (totShips > 0) {
                    // Roll production and compute share
                    final int fishProduction = getRandomGen().nextInt(101) + 150;
                    final int share = fishProduction / totShips;
                    LOGGER.info("Fisheries in " + thisSector.getPosition() + " produced " + fishProduction + " food shared equally among " + totShips + " ship(s)");

                    // Load goods
                    for (final Ship ship : shipList) {
                        if (ship.getType().getShipClass() == 0 || ship.getType().getIntId() == 31) {
                            // Only merchant ships can fish
                            final int freeSpace = calcFreeSpace(ship);
                            int thisShare = share;
                            if (food.getWeightOfGood() * share > freeSpace) {
                                thisShare = freeSpace / food.getWeightOfGood();
                            }

                            if (thisShare > 0) {
                                // Load fish
                                fisheries.get(ship.getNation()).add(thisSector);
                                totFish[ship.getNation().getId()] += thisShare;

                                if (ship.getStoredGoods().containsKey(GOOD_FOOD)) {
                                    ship.getStoredGoods().put(GOOD_FOOD, thisShare + ship.getStoredGoods().get(GOOD_FOOD));

                                } else {
                                    ship.getStoredGoods().put(GOOD_FOOD, thisShare);
                                }

                                ShipManager.getInstance().update(ship);
                            }
                        }
                    }
                }
            }
        }

        // Report production
        for (final Nation nation : lstNations) {
            if (totFish[nation.getId()] > 0) {
                report(nation, "fishing", Integer.toString(totFish[nation.getId()]));

                final Set<Sector> sectors = fisheries.get(nation);
                final StringBuilder stringBuilder = new StringBuilder();
                for (final Sector sector : sectors) {
                    stringBuilder.append(sector.getPosition());
                    stringBuilder.append(", ");
                }
                final String sectorNames = stringBuilder.substring(0, stringBuilder.length() - 2);
                newsSingle(nation, NEWS_ECONOMY, "Our merchant ships produced " + totFish[nation.getId()] + " units of food while fishing at " + sectorNames);
            }
        }

        LOGGER.info("FisheriesEvent completed.");
    }


    /**
     * Calculate the free space in the ship.
     *
     * @param ship the ship to examine.
     * @return the free space in tonnage (for loading goods).
     */
    private int calcFreeSpace(final Ship ship) {
        List<Integer> unusedSlots = new ArrayList<Integer>();
        final int totCapacity = ship.getType().getLoadCapacity();
        int currentLoad = 0;
        for (final Map.Entry<Integer, Integer> entry : ship.getStoredGoods().entrySet()) {
            final int goodType = entry.getKey();
            final int goodQte = entry.getValue();

            if (goodType <= GoodConstants.GOOD_LAST) {
                final Good goodTPE = GoodManager.getInstance().getByID(goodType);
                currentLoad += goodTPE.getWeightOfGood() * goodQte;

            } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                // this is a unit loaded on the vessel
                // take into account weight of loaded unit

                // A Brigade is loaded
                final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());
                if (thisBrigade != null) {
                    // update info in each battalion of the brigade
                    for (final Battalion battalion : thisBrigade.getBattalions()) {
                        if (battalion.getCarrierInfo().getCarrierId() == ship.getShipId()) {
                            currentLoad += getBattalionWeight(battalion);
                        }
                    }
                } else {
                    // For some reason the brigade is not loaded but the pointer is still there
                    unusedSlots.add(entry.getValue());
                }
            }
        }

        // remove unused slots
        if (unusedSlots != null) {
            for (Integer slot : unusedSlots) {
                ship.getStoredGoods().remove(slot);
            }

            ShipManager.getInstance().update(ship);
        }

        return totCapacity - currentLoad;
    }

    /**
     * Method that returns the weight of a battalion
     *
     * @param battalion the battalion whose weight we want
     * @return the weight in tons of the battalion
     */
    public static int getBattalionWeight(final Battalion battalion) {
        double unitWeight;
        if (battalion.getType().isInfantry() || battalion.getType().isEngineer()) {
            unitWeight = 200d;

        } else if (battalion.getType().isCavalry()) {
            unitWeight = 400d;

        } else {
            unitWeight = 600d;
        }

        return (int) ((battalion.getHeadcount() * unitWeight) / 1000d);
    }


}