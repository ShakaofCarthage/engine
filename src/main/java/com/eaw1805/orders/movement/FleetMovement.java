package com.eaw1805.orders.movement;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the Fleet Movement order.
 */
public class FleetMovement
        extends AbstractMovement<Fleet> {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(FleetMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_FLEET;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public FleetMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("FleetMovement instantiated.");
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected Fleet getMobileUnit() {
        final int fleetId = Integer.parseInt(getOrder().getParameter2());

        // Retrieve the fleet object
        final Fleet fleet;

        if (getParent().fleetAssocExists(fleetId)) {
            fleet = FleetManager.getInstance().getByID(getParent().retrieveFleetAssoc(fleetId));

        } else {
            fleet = FleetManager.getInstance().getByID(fleetId);
        }

        return fleet;
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final Fleet theUnit) {
        return "Fleet (" + theUnit.getName() + ")";
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Fleet theUnit) {
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), theUnit.getFleetId());
        int totShips = 0;
        int availMps = Integer.MAX_VALUE;
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            if (thisShip.getType().getShipClass() != 0 && thisShip.getType().getIntId() != 31) {
                totShips++;
            }

            availMps = Math.min(availMps, (int) (thisShip.getType().getMovementFactor() * thisShip.getCondition() / 100d));
        }

        // Check fleet size
        if (totShips > 20) {
            // Calculate penalty
            // It is possible for a fleet to contain more than 20 warships, however for every 2 ships above 20,
            // the movement allowance of the fleet is reduced by 1 movement point due to coordination limitations.
            final double divisor;

            // certain nations have superior seamanship
            // other nations have inferior seamanship
            switch (theUnit.getNation().getId()) {
                case NATION_GREATBRITAIN:
                case NATION_HOLLAND:
                case NATION_DENMARK:
                case NATION_PORTUGAL:
                    // superior seamanship
                    divisor = 3d;
                    break;

                case NATION_AUSTRIA:
                case NATION_PRUSSIA:
                case NATION_WARSAW:
                    // inferior seamanship
                    divisor = 1d;
                    break;

                default:
                    divisor = 2d;
            }

            final int penalty = (int) Math.ceil((totShips - 20d) / divisor);
            availMps -= penalty;
        }

        if (availMps == Integer.MAX_VALUE) {
            availMps = 0;
        }

        if (availMps < 0) {
            availMps = 0;
        }

        return availMps;
    }

    /**
     * Get the number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of sectors.
     */
    protected int getMaxConquers(final Fleet theUnit) {
        return 0;
    }

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected int getMaxNeutralConquers(final Fleet theUnit) {
        return 0;
    }

    /**
     * Checks if the subject of this movement order is part of a hierarchy and depends on another unit.
     *
     * @param theUnit the entity subject to this movement order.
     * @return true if the unit is part of a hierarchy and its movement depends on another (higher-level) entity.
     */
    protected boolean isBounded(final Fleet theUnit) {
        return false;
    }

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected void update(final Fleet entity) {
        FleetManager.getInstance().update(entity);

        // also update ships
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), entity.getFleetId());
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            thisShip.setPosition((Position) entity.getPosition().clone());
            ShipManager.getInstance().update(thisShip);

            // initialize volatile parameters
            thisShip.initializeVariables();

            // Check if the entity is carrying units, and update their position too
            if (thisShip.getHasCommander() || thisShip.getHasSpy() || thisShip.getHasTroops()) {
                thisShip.updatePosition((Position) entity.getPosition().clone());
            }
        }
    }

    /**
     * Get the type (land or sea) of the subject of this movement order.
     *
     * @return the type of the mobile unit.
     */
    protected int getType() {
        return TPE_SEA;
    }

    /**
     * Enforce attrition due to movement.
     *
     * @param entity     the entity that is moving.
     * @param sector     the Sector where it it crossed through.
     * @param isWinter   (unused).
     * @param willBattle if the unit will engage in a battle.
     */
    protected void attrition(final Fleet entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        int warshipCount = 0;
        int bigShipCount = 0;
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), entity.getFleetId());
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            if (thisShip.getType().getShipClass() > 0) {
                warshipCount++;

                if (thisShip.getType().getShipClass() > 3) {
                    bigShipCount++;
                }
            }

            // Check if passing through storms
            if (sector.getStorm() > 0) {
                // The ship will suffer an additional 0-4% attrition
                int rndNum = getParent().getRandomGen().nextInt(5);

                // Dhows and Corsairs suffer double attrition when crossing storms
                if (thisShip.getType().getIntId() == 6
                        || thisShip.getType().getIntId() == 7
                        || thisShip.getType().getIntId() == 11
                        || thisShip.getType().getIntId() == 12) {
                    rndNum *= 2;
                }

                thisShip.setCondition(thisShip.getCondition() - rndNum);
            }

            // raise flag
            thisShip.setHasMoved(true);
            ShipManager.getInstance().update(thisShip);

            // initialize volatile parameters
            thisShip.initializeVariables();

            // Check if the entity is carrying units, and update their movement & engage counters too
            if (thisShip.getHasTroops()) {
                thisShip.updateMoveEngageCounters(willBattle);
            }
        }

        // Check if this eligible for pirates attack
        if (bigShipCount == 0 && warshipCount <= 2 && sector.getTerrain().getId() == TERRAIN_O) {
            getParent().addPirateTarget(entity, sector);
        }
    }

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected void crossStorm(final Fleet entity, final Sector sector) {
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), entity.getFleetId());
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            if (thisShip.getType().getShipClass() >= 4) {
                // Class 4 & 5 ships are always safe from this possibility
                continue;
            }

            final int rndNum = getParent().getRandomGen().nextInt(101) + 1;
            if (rndNum <= 1) {
                // Ship was lost in the storm !!
                newsSingle(entity.getNation(), NEWS_MILITARY, thisShip.getType().getTypeId(), "The ship " + thisShip.getName() + " was lost in a storm at " + sector.getPosition());
                thisShip.setCondition(0);
                ShipManager.getInstance().update(thisShip);
            }
        }
    }

    /**
     * Determine if the unit can cross the particular sector due to ownership and relations.
     *
     * @param owner  the owner of the item about to move.
     * @param sector the sector to move over.
     * @param entity the entity examined.
     * @return true, if it can cross.
     */
    protected boolean canCross(final Nation owner, final Sector sector, final Fleet entity) {
        // Check nation's owner (taking into account possible conquers)
        final Nation sectorOwner = getSectorOwner(sector);

        // Fleets can move inside Neutral Shipyards,
        // or Shipyards owned by Friendly and Allied nations
        // or shipyards of nations with whom their owner has WAR
        // or shipyards of nations in the colonies with whom their owner has COLONIAL WAR
        if (!sector.hasShipyard()) {
            return false;
        }

        if (sectorOwner.getId() == owner.getId()) {
            // this is the owner of the sector.
            return true;

        } else if (sectorOwner.getId() == NATION_NEUTRAL) {
            // this is a neutral sector
            return true;

        } else {
            // Check if any items are loaded on the vessels
            boolean onlyMerchants = true;
            final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), entity.getFleetId());
            for (final Ship thisShip : lstShips) {
                if (thisShip.getCapturedByNation() != 0) {
                    continue;
                }

                if (thisShip.getType().getShipClass() > 0) {
                    onlyMerchants = false;
                }
            }

            // determine target relations
            int targetRelation = REL_PASSAGE;
            if (onlyMerchants) {
                targetRelation = REL_TRADE;
            }

            // Check Sector's relations against owner.
            final NationsRelation relation = getByNations(sectorOwner, owner);

            if (relation.getRelation() <= targetRelation) {
                // Check if fleet is carrying units from another nation
                final List<Nation> otherNations = new ArrayList<Nation>();

                // Check if any items are loaded on the vessels
                for (final Ship thisShip : lstShips) {
                    if (thisShip.getCapturedByNation() != 0) {
                        continue;
                    }

                    // initialize volatile parameters
                    thisShip.initializeVariables();

                    /**
                     * A ship containing troops can only enter or move over territory of an empire with trade
                     * relations or better towards the owner of the ship as well as right of passage or better towards the
                     * owner of the loaded troops.
                     */
                    if (thisShip.getHasTroops()) {
                        for (Map.Entry<Integer, Integer> loadedUnit : thisShip.getStoredGoods().entrySet()) {
                            if (loadedUnit.getKey() > GoodConstants.GOOD_LAST) {
                                if (loadedUnit.getKey() >= ArmyConstants.BRIGADE * 1000 && loadedUnit.getKey() < (ArmyConstants.BRIGADE + 1) * 1000) {
                                    final Brigade thisBrigade = BrigadeManager.getInstance().getByID(loadedUnit.getValue());
                                    if (thisBrigade.getNation().getId() != owner.getId()
                                            && thisBrigade.getNation().getId() != sectorOwner.getId()) {
                                        // found another owner
                                        otherNations.add(thisBrigade.getNation());
                                    }
                                }
                            }
                        }
                    }
                }

                // Check other owners relations with sector owner
                for (final Nation nation : otherNations) {
                    // Check Sector's relations against owner.
                    final NationsRelation otherRelation = getByNations(sectorOwner, nation);

                    // Passage is required
                    if (otherRelation.getRelation() > REL_PASSAGE) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }
    }

    /**
     * Determine if the unit can conquer enemy/neutral territory.
     *
     * @param entity the entity that is moving.
     * @return true, if it can conquer.
     */
    protected boolean canConquer(final Fleet entity) {
        // Fleets cannot conquer.
        return false;
    }

    /**
     * Calculate the tonnage of the unit (applies only for sea units).
     *
     * @param entity the entity that is moving.
     * @return the tonnage of the unit.
     */
    protected int calcPower(final Fleet entity) {
        int tonnage = 0;
        final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), entity.getFleetId());
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            if (thisShip.getType().getShipClass() > 0) {
                tonnage += thisShip.calcTonnage();

            } else {
                tonnage += 0.25d * thisShip.calcTonnage();
            }
        }

        return tonnage;
    }

}
