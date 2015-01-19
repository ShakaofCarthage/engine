package com.eaw1805.orders.movement;

import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the Brigade Movement order.
 */
public class BrigadeMovement
        extends AbstractMovement<Brigade> {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BrigadeMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_BRIG;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BrigadeMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("BrigadeMovement instantiated.");
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected Brigade getMobileUnit() {
        final int brigadeId = Integer.parseInt(getOrder().getParameter2());

        // Retrieve the source brigade
        return BrigadeManager.getInstance().getByID(brigadeId);
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final Brigade theUnit) {
        return "Brigade (" + theUnit.getName() + ")";
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Brigade theUnit) {
        int mps = Integer.MAX_VALUE;
        for (Battalion movedItem : theUnit.getBattalions()) {
            if (movedItem.getCarrierInfo() != null && movedItem.getCarrierInfo().getCarrierId() != 0) {
                mps = 0;

            } else if (movedItem.getHeadcount() > 0) {
                mps = Math.min(mps, movedItem.getType().getMps());
            }
        }

        if (mps == Integer.MAX_VALUE) {
            mps = 0;
        }

        return mps;
    }

    /**
     * Get the number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of sectors.
     */
    protected int getMaxConquers(final Brigade theUnit) {
        if (theUnit.getPosition().getRegion().getId() != EUROPE) {
            // Troops that lose a battle cannot move over/capture enemy territory for one turn
            for (Battalion battalion : theUnit.getBattalions()) {
                if (battalion.getHasLost()) {
                    return 0;
                }
            }

            // You need KT troops
            // Count KT troops
            final int countKT = countTotalKT(theUnit);

            // Needs 2 KT battalions to conquer 1 sector
            if (countKT >= 2) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected int getMaxNeutralConquers(final Brigade theUnit) {
        int maxSectors = 0;
        if (theUnit.getPosition().getRegion().getId() != EUROPE) {
            // Troops that lose a battle cannot move over/capture enemy territory for one turn
            for (Battalion battalion : theUnit.getBattalions()) {
                if (battalion.getHasLost()) {
                    return 0;
                }
            }

            // You need KT troops
            // Count KT troops
            final int countKT = countTotalKT(theUnit);

            // Needs 5 KT battalions to conquer 2 neutral sector
            if (countKT >= 5) {
                maxSectors = 2;

            } else if (countKT >= 2) {
                // Needs 2 KT battalions to conquer 1 neutral sector
                maxSectors = 1;
            }
        }

        return maxSectors;
    }

    /**
     * Count the number of KT battalions available in the Corp.
     *
     * @param theUnit the Corp.
     * @return the number of KT battalions.
     */
    private int countTotalKT(final Brigade theUnit) {
        int countKT = 0;
        for (Battalion movedItem : theUnit.getBattalions()) {
            if (movedItem.getType().getType().equals("Kt")
                    && movedItem.getHeadcount() > 0) {
                countKT++;
            }
        }

        return countKT;
    }

    /**
     * Checks if the subject of this movement order is part of a hierarchy and depends on another unit.
     *
     * @param theUnit the entity subject to this movement order.
     * @return true if the unit is part of a hierarchy and its movement depends on another (higher-level) entity.
     */
    protected boolean isBounded(final Brigade theUnit) {
        return (theUnit.getCorp() != null && theUnit.getCorp() > 0);
    }

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected void update(final Brigade entity) {
        BrigadeManager.getInstance().update(entity);
    }

    /**
     * Get the type (land or sea) of the subject of this movement order.
     *
     * @return the type of the mobile unit.
     */
    protected int getType() {
        return TPE_LAND;
    }

    /**
     * Enforce attrition due to movement.
     *
     * @param entity     the entity that is moving.
     * @param sector     the Sector where it it crossed through.
     * @param isWinter   if it is winter.
     * @param willBattle if the unit will engage in a battle.
     */
    protected void attrition(final Brigade entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        // Apply casualties due to attrition to all battalions
        for (Battalion movedItem : entity.getBattalions()) {
            // Determine attrition factor based on terrain type and ownership of sector
            double factor;

            if (sector.getNation().getId() == entity.getNation().getId()) {
                factor = sector.getTerrain().getAttritionOwn();

            } else {
                factor = sector.getTerrain().getAttritionForeign();
            }

            // Attrition in the Colonies doubles for European troops only (excluding Kt)
            if (sector.getPosition().getRegion().getId() != EUROPE) {
                if ((!movedItem.getType().canColonies()) && (!movedItem.getType().getType().equals("Kt"))) {
                    factor *= 2d;
                }
            }

            // Random Event Desertion is in effect
            if (hasDesertion(entity)) {
                factor += 4d;
            }

            // Reduce headcount
            movedItem.setHeadcount((int) ((movedItem.getHeadcount() * (100d - factor)) / 100d));

            // raise flag
            movedItem.setHasMoved(true);
            movedItem.setHasEngagedBattle(willBattle);
        }

        BrigadeManager.getInstance().update(entity);
    }

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected void crossStorm(final Brigade entity, final Sector sector) {
        // Not applicable.
    }

    /**
     * Determine if the unit can cross the particular sector due to ownership and relations.
     *
     * @param owner  the owner of the item about to move.
     * @param sector the sector to move over.
     * @param entity the entity examined.
     * @return true, if it can cross.
     */
    protected boolean canCross(final Nation owner, final Sector sector, final Brigade entity) {
        // Check nation's owner (taking into account possible conquers)
        final Nation sectorOwner = getSectorOwner(sector);

        // Brigades can move over Neutral Sectors
        // or sectors owned by Friendly and Allied nations.
        if (sectorOwner.getId() == owner.getId()) {
            // this is the owner of the sector.
            return true;

        } else if (sectorOwner.getId() == NATION_NEUTRAL) {
            // this is a neutral sector
            return true;

        } else {
            // Check Sector's relations against owner.
            final NationsRelation relation = getByNations(sectorOwner, owner);

            if (relation.getRelation() <= REL_PASSAGE) {
                return true;

            } else {
                // Need to check if we have war against sector's owner
                final NationsRelation ourRelation = getByNations(owner, sectorOwner);

                return (sector.getPosition().getRegion().getId() != EUROPE && ourRelation.getRelation() >= REL_COLONIAL_WAR);
            }
        }
    }

    /**
     * Determine if the unit can conquer enemy/neutral territory.
     *
     * @param entity the entity that is moving.
     * @return true, if it can conquer.
     */
    protected boolean canConquer(final Brigade entity) {
        // Brigades can conquer only outside EUROPE.
        return (entity.getPosition().getRegion().getId() != EUROPE && countTotalKT(entity) >= 2);
    }

    /**
     * Calculate the tonnage of the unit (applies only for sea units).
     *
     * @param entity the entity that is moving.
     * @return the tonnage of the unit.
     */
    protected int calcPower(final Brigade entity) {
        int totCount = 0;
        for (Battalion movedItem : entity.getBattalions()) {
            if (movedItem.getHeadcount() >= 400) {
                totCount++;
            }
        }
        return totCount;
    }
}
