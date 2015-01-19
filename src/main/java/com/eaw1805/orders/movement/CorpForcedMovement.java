package com.eaw1805.orders.movement;

import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implements the Corp Forced-March Movement order.
 */
public class CorpForcedMovement
        extends CorpMovement {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CorpForcedMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_FM_CORP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CorpForcedMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("CorpForcedMovement instantiated.");
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final Corp theUnit) {
        return "Corps (" + theUnit.getName() + ", Forced-March)";
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Corp theUnit) {
        // Retrieve all units
        int mps = Integer.MAX_VALUE;
        if (theUnit.getCommander() != null) {
            mps = Math.min(mps, theUnit.getCommander().getMps());
        }

        final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), theUnit.getCorpId());
        for (final Brigade brigade : lstBrigades) {
            for (Battalion movedItem : brigade.getBattalions()) {
                if (movedItem.getHeadcount() > 0) {
                    mps = Math.min(mps, (int) (movedItem.getType().getMps() * 1.5d));
                }
            }
        }

        if (mps == Integer.MAX_VALUE) {
            mps = 0;
        }

        return mps;
    }

    /**
     * Enforce attrition due to movement.
     *
     * @param entity     the entity that is moving.
     * @param sector     the Sector where it it crossed through.
     * @param isWinter   if it is winter.
     * @param willBattle if the unit will engage in a battle.
     */
    protected void attrition(final Corp entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        final int raisedMps = getMps(entity);

        // Apply casualties due to attrition to all brigades & battalions
        final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), entity.getCorpId());
        for (Brigade thisBrigade : lstBrigades) {
            for (Battalion movedItem : thisBrigade.getBattalions()) {
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

                // Battalions within a brigade whose normal MPs are within the raised number of movement points
                // will suffer normal attrition.
                if (movedItem.getType().getMps() < raisedMps) {
                    // Forced march will triple attrition
                    factor *= 3d;
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

            BrigadeManager.getInstance().update(thisBrigade);
        }
    }

    /**
     * Determine if the unit can cross the particular sector due to ownership and relations.
     *
     * @param owner  the owner of the item about to move.
     * @param sector the sector to move over.
     * @param entity the entity about to move.
     * @return true, if it can cross.
     */
    protected boolean canCross(final Nation owner, final Sector sector, final Corp entity) {
        // Check nation's owner (taking into account possible conquers)
        final Nation sectorOwner = getSectorOwner(sector);

        // A forced march is only possible within your own empire or through an ally (relation level 5).
        if (sectorOwner.getId() == owner.getId()) {
            // this is the owner of the sector.
            return true;

        } else if (sectorOwner.getId() == NATION_NEUTRAL) {
            // this is a neutral sector
            return false;

        } else {
            // Check Sector's relations against owner.
            final NationsRelation relation = getByNations(sectorOwner, owner);

            return (relation.getRelation() == REL_ALLIANCE);
        }
    }

}
