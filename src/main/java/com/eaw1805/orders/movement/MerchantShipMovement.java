package com.eaw1805.orders.movement;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Brigade;
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
 * Implements the Merchant Ship Movement order.
 */
public class MerchantShipMovement
        extends AbstractMovement<Ship> {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(MerchantShipMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_MSHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public MerchantShipMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("MerchantShipMovement instantiated.");
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected Ship getMobileUnit() {
        final int shipId = Integer.parseInt(getOrder().getParameter2());

        // retrieve ship
        final Ship thisShip = ShipManager.getInstance().getByID(shipId);

        if (thisShip == null) {
            LOGGER.info("Unable to retrieve ship (" + shipId + ")");
            return null;
        }

        // initialize volatile parameters
        thisShip.initializeVariables();

        return thisShip;
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final Ship theUnit) {
        // Check if this is a merchant ship.
        if (theUnit.getType().getShipClass() == 0 || theUnit.getType().getIntId() == 31) {
            return "Merchant Ship (" + theUnit.getName() + ")";
        } else {
            return "War Ship (" + theUnit.getName() + ")";
        }
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Ship theUnit) {
        return (int) (theUnit.getType().getMovementFactor() * theUnit.getCondition() / 100d);
    }

    /**
     * Get the number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of sectors.
     */
    protected int getMaxConquers(final Ship theUnit) {
        return 0;
    }

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected int getMaxNeutralConquers(final Ship theUnit) {
        return 0;
    }

    /**
     * Checks if the subject of this movement order is part of a hierarchy and depends on another unit.
     *
     * @param theUnit the entity subject to this movement order.
     * @return true if the unit is part of a hierarchy and its movement depends on another (higher-level) entity.
     */
    protected boolean isBounded(final Ship theUnit) {
        return (theUnit.getFleet() > 0);
    }

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected void update(final Ship entity) {
        ShipManager.getInstance().update(entity);

        // Check if the entity is carrying units, and update their position too
        if (entity.getHasCommander() || entity.getHasSpy() || entity.getHasTroops()) {
            entity.updatePosition((Position) entity.getPosition().clone());
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
    protected void attrition(final Ship entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        // Check if this eligible for pirates attack
        if (entity.getType().getShipClass() < 4 && sector.getTerrain().getId() == TERRAIN_O) {
            getParent().addPirateTarget(entity, sector);
        }

        // Check if passing through storms
        if (sector.getStorm() > 0) {
            // The ship will suffer an additional 0-4% attrition
            int rndNum = getParent().getRandomGen().nextInt(4);

            // Dhows and Corsairs suffer double attrition when crossing storms
            if (entity.getType().getIntId() == 6
                    || entity.getType().getIntId() == 7
                    || entity.getType().getIntId() == 11
                    || entity.getType().getIntId() == 12) {
                rndNum *= 2;
            }

            entity.setCondition(entity.getCondition() - rndNum);
        }

        // raise flag
        entity.setHasMoved(true);
        ShipManager.getInstance().update(entity);

        // initialize volatile parameters
        entity.initializeVariables();

        // Check if the entity is carrying units, and update their movement & engage counters too
        if (entity.getHasTroops()) {
            entity.updateMoveEngageCounters(willBattle);
        }
    }

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected void crossStorm(final Ship entity, final Sector sector) {
        if (entity.getType().getShipClass() >= 4) {
            // Class 4 & 5 ships are always safe from this possibility
            return;
        }

        final int rndNum = getParent().getRandomGen().nextInt(100) + 1;
        if (rndNum <= 1) {
            // Ship was lost in the storm !!
            newsSingle(entity.getNation(), NEWS_MILITARY, entity.getType().getTypeId(), "The " + getName(entity) + " was lost in a storm at " + sector.getPosition());
            entity.setCondition(0);
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
    protected boolean canCross(final Nation owner, final Sector sector, final Ship entity) {
        // Check nation's owner (taking into account possible conquers)
        final Nation sectorOwner = getSectorOwner(sector);

        // War Ships can move inside Neutral Shipyards,
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

            // Check Sector's relations against owner.
            final NationsRelation relation = getByNations(sectorOwner, owner);

            if (relation.getRelation() <= REL_TRADE) {

                /**
                 * A ship containing troops can only enter or move over territory of an empire with trade
                 * relations or better towards the owner of the ship as well as right of passage or better towards the
                 * owner of the loaded troops.
                 */
                if (entity.getHasTroops()) {
                    // Check if ship is carrying units from another nation
                    final List<Nation> otherNations = new ArrayList<Nation>();

                    for (Map.Entry<Integer, Integer> loadedUnit : entity.getStoredGoods().entrySet()) {
                        if (loadedUnit.getKey() > GoodConstants.GOOD_LAST) {
                            if (loadedUnit.getKey() >= ArmyConstants.BRIGADE * 1000 && loadedUnit.getKey() < (ArmyConstants.BRIGADE + 1) * 1000) {
                                final Brigade thisBrigade = BrigadeManager.getInstance().getByID(loadedUnit.getValue());
                                if (thisBrigade.getNation().getId() != owner.getId()) {
                                    // found another owner
                                    otherNations.add(thisBrigade.getNation());
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
    protected boolean canConquer(final Ship entity) {
        // Merchant Ships cannot conquer.
        return false;
    }

    /**
     * Calculate the tonnage of the unit (applies only for sea units).
     *
     * @param entity the entity that is moving.
     * @return the tonnage of the unit.
     */
    protected int calcPower(final Ship entity) {
        return entity.calcTonnage();
    }

}
