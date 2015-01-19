package com.eaw1805.orders.movement;

import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Implements the BaggageTrains Movement order.
 */
public class BaggageTrainMovement
        extends AbstractMovement<BaggageTrain>
        implements RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BaggageTrainMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_BTRAIN;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public BaggageTrainMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("BaggageTrainMovement instantiated.");
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected BaggageTrain getMobileUnit() {
        final int trainId = Integer.parseInt(getOrder().getParameter2());

        // Retrieve the BaggageTrain
        final BaggageTrain btrain = BaggageTrainManager.getInstance().getByID(trainId);

        // initialize volatile parameters
        if (btrain != null) {
            btrain.initializeVariables();
        }

        return btrain;
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final BaggageTrain theUnit) {
        return "BaggageTrain (" + theUnit.getName() + ")";
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final BaggageTrain theUnit) {
        return (int) (80d * theUnit.getCondition() / 100d);
    }

    /**
     * Get the number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of sectors.
     */
    protected int getMaxConquers(final BaggageTrain theUnit) {
        return 0;
    }

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected int getMaxNeutralConquers(final BaggageTrain theUnit) {
        return 0;
    }

    /**
     * Checks if the subject of this movement order is part of a hierarchy and depends on another unit.
     *
     * @param theUnit the entity subject to this movement order.
     * @return true if the unit is part of a hierarchy and its movement depends on another (higher-level) entity.
     */
    protected boolean isBounded(final BaggageTrain theUnit) {
        return false;
    }

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected void update(final BaggageTrain entity) {
        BaggageTrainManager.getInstance().update(entity);

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
    protected void attrition(final BaggageTrain entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        // initialize volatile parameters
        entity.initializeVariables();

        // Check if the entity is carrying units, and update their movement & engage counters too
        if (entity.getHasTroops()) {
            entity.updateMoveEngageCounters(willBattle);

            // Foreign baggage trains loaded with troops travelling across your nation are reported on your turn report
            if (sector.getNation().getId() != entity.getNation().getId()
                    && sector.getNation().getId() != NATION_NEUTRAL) {
                news(getParent().getGame(), sector.getNation(), entity.getNation(), NEWS_MILITARY, false, 0,
                        "A baggage train of " + entity.getNation().getName() + " transporting troops was spotted at " + sector.getPosition().toString());
            }
        }
    }

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected void crossStorm(final BaggageTrain entity, final Sector sector) {
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
    protected boolean canCross(final Nation owner, final Sector sector, final BaggageTrain entity) {
        // Check nation's owner (taking into account possible conquers)
        final Nation sectorOwner = getSectorOwner(sector);

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
                 * A baggage train containing troops can only enter or move over territory of an empire with trade
                 * relations or better towards the owner of the train as well as right of passage or better towards the
                 * owner of the loaded troops.
                 */
                if (entity.getHasTroops()) {
                    // Check if ship is carrying units from another nation
                    final List<Nation> otherNations = new ArrayList<Nation>();

                    for (Map.Entry<Integer, Integer> loadedUnit : entity.getStoredGoods().entrySet()) {
                        if (loadedUnit.getKey() > GoodConstants.GOOD_LAST) {
                            if (loadedUnit.getKey() >= ArmyConstants.BRIGADE * 1000 && loadedUnit.getKey() < (ArmyConstants.BRIGADE + 1) * 1000) {
                                final Brigade thisBrigade = BrigadeManager.getInstance().getByID(loadedUnit.getValue());
//                                if (thisBrigade.getNation().getId() != owner.getId()) {
                                // found another owner
                                otherNations.add(thisBrigade.getNation());
//                                }
                            }
                        }
                    }

                    // Check other owners relations with sector owner
                    for (final Nation nation : otherNations) {
                        // Check Sector's relations against owner.
                        if (sectorOwner.getId() != nation.getId()) {
                            final NationsRelation otherRelation = getByNations(sectorOwner, nation);

                            // Passage is required
                            if (otherRelation.getRelation() > REL_PASSAGE) {
                                return false;
                            }
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
    protected boolean canConquer(final BaggageTrain entity) {
        // Baggage Trains cannot conquer.
        return false;
    }

    /**
     * Calculate the tonnage of the unit (applies only for sea units).
     *
     * @param entity the entity that is moving.
     * @return the tonnage of the unit.
     */
    protected int calcPower(final BaggageTrain entity) {
        return 0;
    }

}