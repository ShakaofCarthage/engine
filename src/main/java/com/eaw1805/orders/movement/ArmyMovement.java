package com.eaw1805.orders.movement;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implements the Army Movement order.
 */
public class ArmyMovement
        extends AbstractMovement<Army>
        implements RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ArmyMovement.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_M_ARMY;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ArmyMovement(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ArmyMovement instantiated.");
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected Army getMobileUnit() {
        final int armyId = Integer.parseInt(getOrder().getParameter2());

        if (getParent().armyAssocExists(armyId)) {
            return ArmyManager.getInstance().getByID(getParent().retrieveArmyAssoc(armyId));
        }

        // Retrieve the source brigade
        return ArmyManager.getInstance().getByID(armyId);
    }

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected String getName(final Army theUnit) {
        return "Army (" + theUnit.getName() + ")";
    }

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected int getMps(final Army theUnit) {
        // Retrieve all units
        int mps = Integer.MAX_VALUE;
        int commC = 0;
        if (theUnit.getCommander() != null) {
            mps = Math.min(mps, theUnit.getCommander().getMps());
            if (!theUnit.getCommander().getInTransit()
                    && !theUnit.getCommander().getDead()) {
                commC = theUnit.getCommander().getStrc();
            }
        }

        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), theUnit.getArmyId());
        for (final Corp theCorp : lstCorps) {
            commC--;
            if (theCorp.getCommander() != null) {
                mps = Math.min(mps, theCorp.getCommander().getMps());
            }

            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), theCorp.getCorpId());
            for (final Brigade brigade : lstBrigades) {
                for (Battalion movedItem : brigade.getBattalions()) {
                    if (movedItem.getHeadcount() > 0) {
                        mps = Math.min(mps, movedItem.getType().getMps());
                    }
                }
            }
        }

        // Examine commander's Strategic rating -- the number of Corps a commander can command.
        if (commC > 0) {
            commC = 0;
        }

        // If a commander leads an Army with more Corps that his Strategic Rating the movement points
        // potential of the Army will decrease by 2mp for every Corps above the maximum.
        mps += (commC * 2);

        if (mps < 0) {
            mps = 0;
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
    protected int getMaxConquers(final Army theUnit) {
        int totBrigades = 0;

        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), theUnit.getArmyId());
        for (final Corp theCorp : lstCorps) {
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), theCorp.getCorpId());

            // Troops that lose a battle cannot move over/capture enemy territory for one turn
            for (Brigade brigade : lstBrigades) {
                int headcount = 0;
                for (Battalion battalion : brigade.getBattalions()) {
                    if (battalion.getHasLost()) {
                        return 0;
                    }

                    headcount += battalion.getHeadcount();
                }

                if (headcount > 0) {
                    totBrigades++;
                }
            }
        }

        int maxSectors = 0;

        // Scenario 1808: Different rules apply
        if (getGame().getScenarioId() == HibernateUtil.DB_S3) {
            // You need a commander to conquer in Europe
            if (theUnit.getCommander() != null && !theUnit.getCommander().getInTransit() && !theUnit.getCommander().getDead()) {
                if (totBrigades >= 1 && totBrigades <= 3) {
                    maxSectors = 1;

                } else if (totBrigades >= 4 && totBrigades <= 9) {
                    maxSectors = 2;

                } else if (totBrigades >= 10) {
                    maxSectors = 3;
                }
            }

        } else {
            // All other scenarios follow standard rules

            // In Europe
            if (theUnit.getPosition().getRegion().getId() == EUROPE) {
                // You need a commander to conquer in Europe
                if (theUnit.getCommander() != null && !theUnit.getCommander().getInTransit() && !theUnit.getCommander().getDead()) {
                    if (totBrigades >= 5 && totBrigades <= 9) {
                        maxSectors = 1;

                    } else if (totBrigades >= 10 && totBrigades <= 14) {
                        maxSectors = 2;

                    } else if (totBrigades >= 15) {
                        maxSectors = 3;
                    }
                }

            } else {
                // In Colonies

                // Either you need a commander
                if (theUnit.getCommander() != null && !theUnit.getCommander().getInTransit()) {
                    if (totBrigades >= 4) {
                        maxSectors = 2;

                    } else if (totBrigades >= 1) {
                        maxSectors = 1;
                    }
                } else {
                    // You need KT troops
                    // Count KT troops
                    final int countKT = countTotalKT(theUnit);

                    // Needs 2 KT battalions to conquer 1 sector
                    if (totBrigades >= 4 && countKT >= 5) {
                        maxSectors = 2;

                    } else if (countKT >= 2) {
                        maxSectors = 1;
                    }
                }
            }
        }

        return maxSectors;
    }

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected int getMaxNeutralConquers(final Army theUnit) {
        int totBrigades = 0;
        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), theUnit.getArmyId());
        for (final Corp theCorp : lstCorps) {
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), theCorp.getCorpId());
            totBrigades += lstBrigades.size();

            // Troops that lose a battle cannot move over/capture enemy territory for one turn
            for (Brigade brigade : lstBrigades) {
                for (Battalion battalion : brigade.getBattalions()) {
                    if (battalion.getHasLost()) {
                        return 0;
                    }
                }
            }
        }

        int maxSectors = 0;
        if (theUnit.getPosition().getRegion().getId() == EUROPE) {
            // You need a commander to conquer in Europe
            if (theUnit.getCommander() != null && !theUnit.getCommander().getInTransit()) {
                if (totBrigades >= 1) {
                    maxSectors = 3;
                }
            }
        } else {
            // You need KT troops
            // Count KT troops
            final int countKT = countTotalKT(theUnit);

            // Needs 5 KT battalions to conquer 2 neutral sector
            if (countKT >= 5) {
                maxSectors = 2;

            } else if (theUnit.getCommander() != null && !theUnit.getCommander().getInTransit()) {
                // If you have a commander
                maxSectors = 2;

            } else if (countKT >= 2) {
                // Needs 2 KT battalions to conquer 1 neutral sector
                maxSectors = 1;
            }
        }

        return maxSectors;
    }

    /**
     * Count the number of KT battalions available in the Army.
     *
     * @param theUnit the Army.
     * @return the number of KT battalions.
     */
    private int countTotalKT(final Army theUnit) {
        int countKT = 0;
        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), theUnit.getArmyId());
        for (final Corp theCorp : lstCorps) {
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), theCorp.getCorpId());
            for (final Brigade brigade : lstBrigades) {
                for (Battalion movedItem : brigade.getBattalions()) {
                    if (movedItem.getType().getType().equals("Kt")
                            && movedItem.getHeadcount() > 0) {
                        countKT++;
                    }
                }
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
    protected boolean isBounded(final Army theUnit) {
        return false;
    }

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected void update(final Army entity) {
        ArmyManager.getInstance().update(entity);

        if (entity.getCommander() != null) {
            entity.getCommander().setPosition((Position) entity.getPosition().clone());
            CommanderManager.getInstance().update(entity.getCommander());
        }

        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), entity.getArmyId());
        for (final Corp theCorp : lstCorps) {
            theCorp.setPosition((Position) entity.getPosition().clone());
            CorpManager.getInstance().update(theCorp);

            if (theCorp.getCommander() != null) {
                theCorp.getCommander().setPosition((Position) theCorp.getPosition().clone());
                CommanderManager.getInstance().update(theCorp.getCommander());
            }

            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), theCorp.getCorpId());
            for (final Brigade brigade : lstBrigades) {
                brigade.setPosition((Position) entity.getPosition().clone());
                BrigadeManager.getInstance().update(brigade);
            }
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
     * @param willBattle if the unit will engage in battle.
     */
    protected void attrition(final Army entity, final Sector sector, final boolean isWinter, boolean willBattle) {
        double factorRandomEvent = 1d;

        // Random Event Natives Raid is in effect for this particular army
        if (hasNativesRaid(entity)) {
            factorRandomEvent = 5d;
        }

        // Apply casualties due to attrition to all corps, brigades & battalions
        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), entity.getArmyId());
        for (Corp thisCorp : lstCorps) {
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), thisCorp.getCorpId());
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

                    // Random Event Desertion is in effect
                    if (hasDesertion(entity)) {
                        factor += 4d;
                    }

                    // Take into account the Random Event RAID
                    factor *= factorRandomEvent;

                    // Reduce headcount
                    movedItem.setHeadcount((int) ((movedItem.getHeadcount() * (100d - factor)) / 100d));

                    // raise flag
                    movedItem.setHasMoved(true);
                    movedItem.setHasEngagedBattle(willBattle);
                }

                BrigadeManager.getInstance().update(thisBrigade);
            }
        }
    }

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected void crossStorm(final Army entity, final Sector sector) {
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
    protected boolean canCross(final Nation owner, final Sector sector, final Army entity) {
        // Check nation's owner (taking into account possible conquers)
        final Nation sectorOwner = getSectorOwner(sector);

        // Armies can move over Neutral Sectors
        // or sectors owned by Friendly and Allied nations.
        // or sectors of nations with whom their owner has WAR
        // or nations in the colonies with whom their owner has COLONIAL WAR
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

                return (ourRelation.getRelation() == REL_WAR
                        || (sector.getPosition().getRegion().getId() != EUROPE && ourRelation.getRelation() == REL_COLONIAL_WAR));
            }
        }
    }

    /**
     * Determine if the unit can conquer enemy/neutral territory.
     *
     * @param entity the entity that is moving.
     * @return true, if it can conquer.
     */
    protected boolean canConquer(final Army entity) {
        // Armies can conquer only if they have a commander
        // Armies in the colonies can conquer if they have KT battalions
        return (entity.getCommander() != null) || (entity.getPosition().getRegion().getId() != EUROPE && countTotalKT(entity) >= 2);
    }

    /**
     * Calculate the tonnage of the unit (applies only for sea units).
     *
     * @param entity the entity that is moving.
     * @return the tonnage of the unit.
     */
    protected int calcPower(final Army entity) {
        int totCount = 0;
        final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGame(), entity.getArmyId());
        for (Corp thisCorp : lstCorps) {
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGame(), thisCorp.getCorpId());
            for (Brigade thisBrigade : lstBrigades) {
                for (Battalion movedItem : thisBrigade.getBattalions()) {
                    if (movedItem.getHeadcount() >= 400) {
                        totCount++;
                    }
                }
            }
        }
        return totCount;
    }

}
