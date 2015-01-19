package com.eaw1805.orders.movement;

import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the Commanders Movement order.
 */
public class CommanderMovement
        extends AbstractMovement<Commander> {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CommanderMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_COMM;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CommanderMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("CommanderMovement instantiated.");
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected Commander getMobileUnit() {
        final int commId = Integer.parseInt(getOrder().getParameter2());

        // Retrieve the commander
        return CommanderManager.getInstance().getByID(commId);
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final Commander theUnit) {
        return "Commander (" + theUnit.getName() + ")";
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Commander theUnit) {
        if (theUnit.getCarrierInfo() != null && theUnit.getCarrierInfo().getCarrierId() != 0) {
            return 0;
        }

        return theUnit.getMps();
    }

    /**
     * Get the number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of sectors.
     */
    protected int getMaxConquers(final Commander theUnit) {
        return 0;
    }

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected int getMaxNeutralConquers(final Commander theUnit) {
        return 0;
    }

    /**
     * Checks if the subject of this movement order is part of a hierarchy and depends on another unit.
     *
     * @param theUnit the entity subject to this movement order.
     * @return true if the unit is part of a hierarchy and its movement depends on another (higher-level) entity.
     */
    protected boolean isBounded(final Commander theUnit) {
        return (theUnit.getArmy() + theUnit.getCorp() > 0);
    }

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected void update(final Commander entity) {
        CommanderManager.getInstance().update(entity);
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
    protected void attrition(final Commander entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        // Commanders do not suffer from attrition
    }

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected void crossStorm(final Commander entity, final Sector sector) {
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
    protected boolean canCross(final Nation owner, final Sector sector, final Commander entity) {
        // Commanders can move everywhere freely.
        return true;
    }

    /**
     * Determine if the unit can conquer enemy/neutral territory.
     *
     * @param entity the entity that is moving.
     * @return true, if it can conquer.
     */
    protected boolean canConquer(final Commander entity) {
        // Commanders cannot conquer.
        return false;
    }

    /**
     * Calculate the tonnage of the unit (applies only for sea units).
     *
     * @param entity the entity that is moving.
     * @return the tonnage of the unit.
     */
    protected int calcPower(final Commander entity) {
        return 0;
    }

}
