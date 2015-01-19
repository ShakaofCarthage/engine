package com.eaw1805.orders.movement;

import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.orders.PatrolOrderDetails;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements the Ship Patrol Movement order.
 */
public class ShipPatrolMovement
        extends MerchantShipMovement {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ShipPatrolMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_P_SHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ShipPatrolMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ShipPatrolMovement instantiated.");
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Ship theUnit) {
        return (int) (0.5d * super.getMps(theUnit));
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

            // Patrolling ships receive +50% attrition while on patrol duties.
            // (programmers: calculate attrition normally, as if the ships actually moved along the patrol path.
            // Include storm attrition. Then multiply by 50% extra).
            rndNum *= 1.5;

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
     * Process all sectors one by one until end of movement.
     *
     * @param theUnit       the entity subject to this movement order.
     * @param lstPosSectors a list of sectors that correspond to all the coordinates found in the movement order's parameters.
     */
    protected void performMovement(final Ship theUnit, final List<Sector> lstPosSectors) {
        int availMps = getMps(theUnit);

        final List<Ship> patrolShips = new ArrayList<Ship>();
        patrolShips.add(theUnit);

        final Set<Sector> patrolledSectors = new HashSet<Sector>();

        // Keep important little details
        final PatrolOrderDetails pod = new PatrolOrderDetails();
        pod.setOrderId(getOrder().getOrderId());
        pod.setNation(getOrder().getNation());
        pod.setShips(patrolShips);
        pod.setTonnage(calcPower(theUnit));
        pod.setMaxMP(availMps);
        pod.setIntercept(false);

        // Register PatrolOrder
        addPatrolOrder(pod);

        for (final Sector nextSector : lstPosSectors) {
            int mpCost = 1;

            // Check if sector is affected by storm
            if (nextSector.getStorm() > 0) {
                // It will cost one extra movement point per storm coordinate passing through
                mpCost++;
            }

            // Check available movement points
            if (availMps > mpCost) {

                // move to next sector
                availMps -= mpCost;

                // apply attrition
                attrition(theUnit, nextSector, false, false);

                // Check if entity is crossing a storm
                if (nextSector.getStorm() > 0) {
                    crossStorm(theUnit, nextSector);
                }

                if (theUnit.getCondition() > 0) {
                    // Update indexes
                    addPatrolSector(nextSector, theUnit.getNation());
                    addPatrolSector(nextSector, pod);

                    // Keep track of sector
                    patrolledSectors.add(nextSector);
                }

                // update record
                update(theUnit);

                getOrder().setResult(1);
                final String existExplanation = getOrder().getExplanation();
                if ((existExplanation != null) && (existExplanation.length() > 0)) {
                    getOrder().setExplanation(existExplanation + ", " + nextSector.getPosition().toString());
                } else {
                    getOrder().setExplanation(getName(theUnit) + " patrolling " + nextSector.getPosition().toString());
                }

            } else {
                // The potential list of sectors is too far away.
                getOrder().setResult(3);
                final String existExplanation = getOrder().getExplanation();
                if ((existExplanation != null) && (existExplanation.length() > 0)) {
                    getOrder().setExplanation(existExplanation + ". Used up all the available movement points");
                } else {
                    getOrder().setExplanation("Used up all the available movement points");
                }
                break;
            }
        }

        // Update details
        pod.setUnspentMP(availMps);
        pod.setPath(patrolledSectors);
    }

}
