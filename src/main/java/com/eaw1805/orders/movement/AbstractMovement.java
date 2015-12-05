package com.eaw1805.orders.movement;

import com.eaw1805.data.constants.MovementConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.MapElement;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.orders.PatrolOrderDetails;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Generic implementation of movement order.
 */
public abstract class AbstractMovement<TypeOfUnit extends MapElement>
        extends AbstractOrderProcessor
        implements OrderInterface, MovementConstants, TerrainConstants, RegionConstants,
        ReportConstants, NationConstants, ProductionSiteConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(AbstractMovement.class);

    /**
     * Holds the items that have moved already.
     */
    protected static Set movedItems;

    protected static Map<Integer, Map<Integer, Set<Sector>>> nationsToSectorsConquered;

    /**
     * The sectors under patrol indexed to nations patrolling.
     */
    protected static final Map<Position, Set<Nation>> patrolledSectorsIdxNation = new HashMap<Position, Set<Nation>>();

    /**
     * The sectors under patrol indexed to patrol order.
     */
    protected static final Map<Position, Set<PatrolOrderDetails>> patrolledSectorsIdxOrder = new HashMap<Position, Set<PatrolOrderDetails>>();

    /**
     * The sectors that will initiate a land battle indexed to nations that can potentially participate.
     */
    protected static final Map<Position, Set<Nation>> landBattleSectorsIdxNation = new HashMap<Position, Set<Nation>>();

    /**
     * The sectors that will initiate a sea battle indexed to nations that can potentially participate.
     */
    protected static final Map<Position, Set<Nation>> seaBattleSectorsIdxNation = new HashMap<Position, Set<Nation>>();

    /**
     * If arctic zone suffers from severe winter.
     */
    private static boolean hasArctic;

    /**
     * If central zone suffers from severe winter.
     */
    private static boolean hasCentral;

    /**
     * If mediterranean zone suffers from severe winter.
     */
    private static boolean hasMediterranean;

    /**
     * Captures if the Random Event Desertion is in effect.
     */
    private static int desertion;

    /**
     * Captures if the Random Event Natives Raid is in effect.
     */
    private static int nativesRaid;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public AbstractMovement(final OrderProcessor myParent) {
        super(myParent);

        if (movedItems == null) {
            movedItems = new HashSet();
            nationsToSectorsConquered = new HashMap<Integer, Map<Integer, Set<Sector>>>();
            final Nation freeNation = NationManager.getInstance().getByID(NATION_NEUTRAL);
            hasArctic = getReport(freeNation, "winter.arctic").equals("1");
            hasCentral = getReport(freeNation, "winter.central").equals("1");
            hasMediterranean = getReport(freeNation, "winter.mediterranean").equals("1");

            // Random Event: Workers Strike, Natives Raid
            desertion = retrieveReportAsInt(freeNation, myParent.getGame().getTurn(), RE_DESE);
            nativesRaid = retrieveReportAsInt(freeNation, myParent.getGame().getTurn(), RE_RAID);
        }

        LOGGER.debug("AbstractMovement instantiated.");
    }

    /**
     * Add a sector that a land battle will take place with a nation involved.
     *
     * @param position the sector where the battle will be initiated.
     * @param nation   the nation that will be potentially involved.
     */
    protected final void addLandBattleSector(final Position position, final Nation nation) {
        Set<Nation> theSectors;
        if (landBattleSectorsIdxNation.containsKey(position)) {
            theSectors = landBattleSectorsIdxNation.get(position);
        } else {
            theSectors = new HashSet<Nation>();
            landBattleSectorsIdxNation.put(position, theSectors);
        }
        theSectors.add(nation);
    }

    /**
     * Add a sector that a sea battle will take place with a nation involved.
     *
     * @param position the sector where the battle will be initiated.
     * @param nation   the nation that will be potentially involved.
     */
    protected final void addSeaBattleSector(final Position position, final Nation nation) {
        Set<Nation> theSectors;
        if (seaBattleSectorsIdxNation.containsKey(position)) {
            theSectors = seaBattleSectorsIdxNation.get(position);
        } else {
            theSectors = new HashSet<Nation>();
            seaBattleSectorsIdxNation.put(position, theSectors);
        }
        theSectors.add(nation);
    }


    /**
     * Add a sector that is patrolled by a ship/fleet.
     *
     * @param sector the sector patrolled.
     * @param nation the nation conducting the patrol.
     */
    protected final void addPatrolSector(final Sector sector, final Nation nation) {
        Set<Nation> theSectors;
        if (patrolledSectorsIdxNation.containsKey(sector.getPosition())) {
            theSectors = patrolledSectorsIdxNation.get(sector.getPosition());

        } else {
            theSectors = new HashSet<Nation>();
            patrolledSectorsIdxNation.put(sector.getPosition(), theSectors);
        }
        theSectors.add(nation);
    }

    /**
     * Add a sector that is patrolled by a ship/fleet.
     *
     * @param sector the sector patrolled.
     * @param order  the patrol order.
     */
    protected final void addPatrolSector(final Sector sector, final PatrolOrderDetails order) {
        Set<PatrolOrderDetails> theSectors;
        if (patrolledSectorsIdxOrder.containsKey(sector.getPosition())) {
            theSectors = patrolledSectorsIdxOrder.get(sector.getPosition());

        } else {
            theSectors = new HashSet<PatrolOrderDetails>();
            patrolledSectorsIdxOrder.put(sector.getPosition(), theSectors);
        }
        theSectors.add(order);
    }

    /**
     * Add a sector that is patrolled by a ship/fleet.
     *
     * @param order the patrol order.
     */
    protected final void addPatrolOrder(final PatrolOrderDetails order) {
        getParent().getPatrolOrders().put(order.getOrderId(), order);
    }

    /**
     * Get the set of items that have already moved.
     *
     * @return the items that have already moved.
     */
    public final Set getMovedItems() {
        return movedItems;
    }

    /**
     * Checks if the particular item has moved.
     *
     * @param item the item to check.
     * @return true if the item has moved this turn.
     */
    private boolean hasNotMoved(final TypeOfUnit item) {
        return (!getMovedItems().contains(item));
    }

    /**
     * Checks if the particular nation is affected by the desertion random event.
     *
     * @param item the item to check.
     * @return true if the item is affected.
     */
    protected final boolean hasDesertion(final TypeOfUnit item) {
        return (desertion == item.getNation().getId());
    }

    /**
     * Checks if the particular army is affected by the natives raid random event.
     *
     * @param item the item to check.
     * @return true if the item is affected.
     */
    protected final boolean hasNativesRaid(final Army item) {
        return (nativesRaid == item.getArmyId());
    }

    /**
     * Check if the item is conquering this sector.
     *
     * @param item       the subject of the movement order.
     * @param thisSector the sector to check.
     * @return true if the item is conquering.
     */
    private boolean needConquer(final TypeOfUnit item, final Sector thisSector) {
        if ((item instanceof Spy)
                || (item instanceof Commander)
                || (item instanceof BaggageTrain)) {
            return false;
        }

        // Retrieve current owner of sector
        final Nation sectorOwner = getSectorOwner(thisSector);

        if (item.getNation().getId() == sectorOwner.getId()) {
            // check if this is the owner of the sector.
            return false;

        } else if (sectorOwner.getId() == NATION_NEUTRAL) {
            // Check if this is a neutral sector
            return false;

        } else {
            // Check Owner's relations against Sector's owner.
            final NationsRelation relation = getByNations(item.getNation(), sectorOwner);

            return ((relation.getRelation() == REL_WAR)
                    || (thisSector.getPosition().getRegion().getId() != EUROPE && relation.getRelation() >= REL_COLONIAL_WAR));
        }
    }

    /**
     * Check if the item is conquering this neutral sector.
     *
     * @param item       the subject of the movement order.
     * @param thisSector the sector to check.
     * @return true if the item is conquering neutral sector.
     */
    private boolean needNeutralConquer(final TypeOfUnit item, final Sector thisSector) {
        if ((item instanceof Spy)
                || (item instanceof Commander)
                || (item instanceof BaggageTrain)) {
            return false;
        }

        // Retrieve current owner of sector
        final Nation sectorOwner = getSectorOwner(thisSector);

        // check if this is the owner of the sector.
        return ((sectorOwner.getId() == NATION_NEUTRAL) && (thisSector.getTerrain().getId() != TERRAIN_O));
    }

    /**
     * Retrieve a report entry for this turn.
     *
     * @param owner the owner of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    public final String getReport(final Nation owner, final String key) {
        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getParent().getGame(), getParent().getGame().getTurn(), key);
        if (thisReport == null) {
            return "";
        } else {
            return thisReport.getValue();
        }
    }

    /**
     * Extract from the order the subject of this movement order.
     *
     * @return Retrieves the Entity related to the order.
     */
    protected abstract TypeOfUnit getMobileUnit();

    /**
     * Get the name of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the name of the unit.
     */
    protected abstract String getName(final TypeOfUnit theUnit);

    /**
     * Get the available movement points of the subject of this movement order.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the available movement points.
     */
    protected abstract int getMps(final TypeOfUnit theUnit);

    /**
     * Get the number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of sectors.
     */
    protected abstract int getMaxConquers(final TypeOfUnit theUnit);

    /**
     * Get the neutral number of sectors that it can conquer.
     *
     * @param theUnit the entity subject to this movement order.
     * @return the total number of neutral sectors.
     */
    protected abstract int getMaxNeutralConquers(final TypeOfUnit theUnit);

    /**
     * Checks if the subject of this movement order is part of a hierarchy and depends on another unit.
     *
     * @param theUnit the entity subject to this movement order.
     * @return true if the unit is part of a hierarchy and its movement depends on another (higher-level) entity.
     */
    protected abstract boolean isBounded(final TypeOfUnit theUnit);

    /**
     * updating an entry into the database, according to the input object it
     * receives.
     *
     * @param entity an Entity object that may be of every type of entity.
     */
    protected abstract void update(final TypeOfUnit entity);

    /**
     * Get the type (land or sea) of the subject of this movement order.
     *
     * @return the type of the mobile unit.
     */
    protected abstract int getType();

    /**
     * Enforce attrition due to movement.
     *
     * @param entity     the entity that is moving.
     * @param sector     the Sector where it it crossed through.
     * @param isWinter   if it is winter.
     * @param willBattle if the unit has initiated a battle.
     */
    protected abstract void attrition(final TypeOfUnit entity, final Sector sector, final boolean isWinter, boolean willBattle);

    /**
     * Test ship endurance due to movement through storms.
     *
     * @param entity the entity that is moving.
     * @param sector the Sector where it it crossed through.
     */
    protected abstract void crossStorm(final TypeOfUnit entity, final Sector sector);

    /**
     * Determine if the unit can cross the particular sector due to ownership and relations.
     *
     * @param owner  the owner of the item about to move.
     * @param sector the sector to move over.
     * @param entity the entity examined.
     * @return true, if it can cross.
     */
    protected abstract boolean canCross(final Nation owner, final Sector sector, final TypeOfUnit entity);

    /**
     * Determine if the unit can conquer enemy/neutral territory.
     *
     * @param entity the entity that is moving.
     * @return true, if it can conquer.
     */
    protected abstract boolean canConquer(final TypeOfUnit entity);

    /**
     * Calculate the power of the unit for applying the overrun rule.
     * In sea units it uses tonnage.
     * In land units it uses battalion count.
     *
     * @param entity the entity that is moving.
     * @return the power of the unit.
     */
    protected abstract int calcPower(final TypeOfUnit entity);

    /**
     * Determine if particular sector has sever winter effects.
     *
     * @param position the position to check.
     * @return true, if severe winter in effect.
     */
    protected final boolean isSevereWinter(final Position position) {
        switch (position.getRegion().getId()) {
            case EUROPE:
                if (position.getY() <= 10) {
                    return hasArctic;

                } else if ((position.getY() >= 11) && (position.getY() <= 35)) {
                    return hasCentral;

                } else {
                    return hasMediterranean;
                }

            case CARIBBEAN:
            case INDIES:
            case AFRICA:
            default:
                return false;
        }
    }

    /**
     * Identify the current owner of the sector.
     *
     * @param sector the sector to inspect.
     * @return the owner.
     */
    protected final Nation getSectorOwner(final Sector sector) {
        final Nation sectorOwner;
        if (sector.getTempNation() != null
                && sector.getTempNation().getId() != 0
                && sector.getTempNation().getId() != NATION_NEUTRAL) {
            sectorOwner = sector.getTempNation();

        } else {
            sectorOwner = sector.getNation();
        }

        return sectorOwner;
    }

    /**
     * Extract from the order the parameters related to the movement path.
     *
     * @return the concatenated parameters values.
     */
    private String getMovementPath() {
        final String movePath[] = new String[5];
        movePath[0] = getOrder().getParameter3();
        movePath[1] = getOrder().getParameter4();
        movePath[2] = getOrder().getParameter5();
        movePath[3] = getOrder().getParameter6();
        movePath[4] = getOrder().getParameter7();

        // Concatenate each movement path
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String thisPath : movePath) {
            if (thisPath == null) {
                // reached end of path
                break;

            } else {
                final String newPath = thisPath.replaceAll("!", "-");
                if (newPath.contains(":")) {
                    stringBuilder.append(newPath.replaceAll("--", "-"));
                }
            }
        }

        return stringBuilder.toString();
    }

    /**
     * De-serialization of movement sector path.
     *
     * @param theUnit      the entity subject to this movement order.
     * @param isEngageMove the entity is performing engage move.
     * @param movementPath a list of coordinates (formatted as "x:y") delimited by "-".
     * @return a list of Sectors that correspond to the coordinates.
     */
    private List<Sector> getPossibleSectors(final TypeOfUnit theUnit, final boolean isEngageMove, final String movementPath) {
        final List<Sector> lstSectors = new ArrayList<Sector>();
        final StringTokenizer stokSectors = new StringTokenizer(movementPath.replaceAll("!", "-"), "-");

        if (isEngageMove) {
            // patrol sector movement
            final Sector thisSector = SectorManager.getInstance().getByPosition(theUnit.getPosition());
            lstSectors.add(thisSector);
        }

        // if no other sectors given, stop here.
        if (!stokSectors.hasMoreTokens()) {
            return lstSectors;
        }

        // skip 1st sector, it is the position of the unit
        stokSectors.nextToken();

        // iterate through all other tokens
        while (stokSectors.hasMoreElements()) {
            final StringTokenizer stokCoords = new StringTokenizer(stokSectors.nextToken(), ":");
            final String coordX = stokCoords.nextToken();
            final String coordY = stokCoords.nextToken();

            final Position thisPos = (Position) theUnit.getPosition().clone();
            thisPos.setX(Integer.parseInt(coordX));
            thisPos.setY(Integer.parseInt(coordY));

            // Locate sector
            final Sector nextSector = SectorManager.getInstance().getByPosition(thisPos);

            if (nextSector == null) {
                break;
            }
            // Check if sector is compatible with type of the mobile unit
            if ((getType() == TPE_SEA
                    && (nextSector.getTerrain().getId() == TERRAIN_O
                    || (nextSector.getProductionSite() != null
                    && (nextSector.getProductionSite().getId() == PS_BARRACKS
                    || nextSector.getProductionSite().getId() == PS_BARRACKS_FS
                    || nextSector.getProductionSite().getId() == PS_BARRACKS_FM
                    || nextSector.getProductionSite().getId() == PS_BARRACKS_FL
                    || nextSector.getProductionSite().getId() == PS_BARRACKS_FH))
            ))
                    || (getType() == TPE_LAND
                    && nextSector.getTerrain().getId() != TERRAIN_O
                    && nextSector.getTerrain().getId() != TERRAIN_I)) {

                // passable sector

                // check if this is not a duplicate entry
                if (lstSectors.isEmpty()
                        || (lstSectors.get(lstSectors.size() - 1).getId() != nextSector.getId())) {
                    lstSectors.add(nextSector);
                }

            } else {
                // end movement here
                break;
            }
        }

        return lstSectors;
    }

    /**
     * Check if in this sector there exist foreign+enemy brigades.
     *
     * @param theUnit       the entity that is moving.
     * @param position      the position of the entity.
     * @param ignoreHasLost ignore the units that have lost.
     * @param movingOutside make sure overrun is properly evaluated - this is the force inside the castle that is moving outside.
     * @return true if enemy brigades are present.
     */
    private boolean checkForBattle(final TypeOfUnit theUnit,
                                   final Sector position,
                                   final boolean ignoreHasLost,
                                   final boolean movingOutside) {
        boolean found = false;
        final int totBattalions = calcNationBattalions(theUnit.getNation(), theUnit.getPosition(), false);

        if ((theUnit instanceof Spy)
                || (theUnit instanceof Commander)
                || (theUnit instanceof BaggageTrain)
                || (theUnit instanceof Ship)
                || (theUnit instanceof Fleet)) {
            return found;
        }

        // Identify minimum relation to be considered an enemy
        final int warRelation;
        if (theUnit.getPosition().getRegion().getId() != EUROPE) {
            warRelation = REL_COLONIAL_WAR;

        } else {
            warRelation = REL_WAR;
        }

        // Identify owners in this sector
        final List<Nation> owners = BrigadeManager.getInstance().listOwners(theUnit.getPosition());
        for (final Nation nation : owners) {
            // ignore own
            if (nation.getId() == theUnit.getNation().getId()) {
                continue;
            }

            // Retrieve relations with target owner
            final NationsRelation relation = getByNations(theUnit.getNation(), nation);

            // Check relations
            if (relation.getRelation() >= warRelation) {
                // Build nation names
                final StringBuilder unitOwner = new StringBuilder();
                unitOwner.append(theUnit.getNation().getName());
                unitOwner.append(", ");

                final StringBuilder otherOwner = new StringBuilder();
                otherOwner.append(nation.getName());
                otherOwner.append(", ");

                // Keep track of nations
                final List<Nation> unitNations = new ArrayList<Nation>();
                unitNations.add(theUnit.getNation());

                final List<Nation> otherNations = new ArrayList<Nation>();
                otherNations.add(nation);

                // calculate power of nations
                int totWarBattalions = calcNationBattalions(nation, theUnit.getPosition(), ignoreHasLost);
                int totAlliedBattalions = totBattalions;

                // Identify other nations that are allies and/or enemies
                for (final Nation thirdNation : owners) {

                    // ignore basic pair of nations
                    if (thirdNation.getId() == theUnit.getNation().getId()
                            || thirdNation.getId() == nation.getId()) {
                        continue;
                    }

                    // Retrieve relations of unit owner with third nation
                    final NationsRelation unitRelation = getByNations(theUnit.getNation(), thirdNation);

                    // Retrieve relations of enemy with third nation
                    final NationsRelation theirRelation = getByNations(nation, thirdNation);

                    // Check if this is our Ally and their Enemy
                    if (unitRelation.getRelation() == REL_ALLIANCE
                            && theirRelation.getRelation() >= warRelation) {

                        // calculate power of nation
                        totAlliedBattalions += calcNationBattalions(thirdNation, theUnit.getPosition(), false);

                        // Update participant names
                        unitOwner.append(thirdNation.getName());
                        unitOwner.append(", ");

                        // update participants list
                        unitNations.add(thirdNation);

                    } else if (unitRelation.getRelation() >= warRelation
                            && theirRelation.getRelation() == REL_ALLIANCE) {

                        // calculate power of nation
                        totWarBattalions += calcNationBattalions(thirdNation, theUnit.getPosition(), ignoreHasLost);

                        // Update participant names
                        otherOwner.append(thirdNation.getName());
                        otherOwner.append(", ");

                        // update participants list
                        otherNations.add(thirdNation);
                    }
                }

                // Check if we overrun opponent
                if (totWarBattalions > 0
                        && totWarBattalions < 50
                        && totAlliedBattalions >= totWarBattalions * 10
                        && (!position.hasFort() || movingOutside)) {

                    final String unitNation = unitOwner.delete(unitOwner.length() - 2, unitOwner.length()).toString();
                    final String otherNation = otherOwner.delete(otherOwner.length() - 2, otherOwner.length()).toString();

                    doLandOverrun(unitNation, unitNations, otherNations, theUnit.getPosition());

                    LOGGER.info("Land units of " + unitNation + " overrun forces of " + otherNation + " at " + theUnit.getPosition().toString());

                } else if (totAlliedBattalions > 0
                        && totAlliedBattalions < 50
                        && totWarBattalions >= totBattalions * 10
                        && (!position.hasFort() || movingOutside)) {

                    // enemy forces overrun ours
                    final String unitNation = unitOwner.delete(unitOwner.length() - 2, unitOwner.length()).toString();
                    final String otherNation = otherOwner.delete(otherOwner.length() - 2, otherOwner.length()).toString();

                    doLandOverrun(otherNation, otherNations, unitNations, theUnit.getPosition());

                    found = true;
                    LOGGER.info("Land units of " + otherNation + " overrun forces of " + unitNation + " at " + theUnit.getPosition().toString());

                } else if (totWarBattalions > 0 && totAlliedBattalions > 0) {
                    addLandBattleSector(position.getPosition(), theUnit.getNation());
                    addLandBattleSector(position.getPosition(), nation);
                    found = true;

                    LOGGER.info("Land units of " + unitOwner + " about to battle forces of " + otherOwner + " at " + theUnit.getPosition().toString());

                    // also involve allied nations
                    // Identify other nations that are allies and/or enemies
                    for (final Nation thirdNation : owners) {

                        // ignore basic pair of nations
                        if (thirdNation.getId() == theUnit.getNation().getId()
                                || thirdNation.getId() == nation.getId()) {
                            continue;
                        }

                        // Retrieve relations of unit owner with third nation
                        final NationsRelation unitRelation = getByNations(theUnit.getNation(), thirdNation);

                        // Retrieve relations of enemy with third nation
                        final NationsRelation theirRelation = getByNations(nation, thirdNation);

                        // Check if this is our Ally and their Enemy
                        if ((unitRelation.getRelation() == REL_ALLIANCE && theirRelation.getRelation() >= warRelation)
                                || (theirRelation.getRelation() == REL_ALLIANCE && unitRelation.getRelation() >= warRelation)) {

                            addLandBattleSector(position.getPosition(), thirdNation);
                        }
                    }
                }
            }
        }

        return found;
    }

    /**
     * Check if in this sector there exist foreign+enemy ships.
     *
     * @param theUnit the entity that is moving.
     * @return true if enemy ships are present.
     */
    private boolean checkForSeaBattle(final TypeOfUnit theUnit) {
        boolean found = false;
        final int totTonnage = calcNationTonnage(theUnit.getNation(), theUnit.getPosition());
        final int totWarShips = calcWarShips(theUnit.getNation(), theUnit.getPosition());

        if ((theUnit instanceof Spy)
                || (theUnit instanceof Commander)
                || (theUnit instanceof BaggageTrain)
                || (theUnit instanceof Brigade)
                || (theUnit instanceof Corp)
                || (theUnit instanceof Army)) {
            return found;
        }

        // Ignore land sectors
        final Sector thisSector = SectorManager.getInstance().getByPosition(theUnit.getPosition());
        if (thisSector.getTerrain().getId() != TERRAIN_O) {
            return found;
        }

        // Identify owners in this sector
        final List<Nation> owners = ShipManager.getInstance().listOwners(theUnit.getPosition());
        for (final Nation nation : owners) {
            // check owner
            if (nation.getId() == theUnit.getNation().getId()) {
                continue;
            }

            // Retrieve relations with foreign nation
            final NationsRelation relation = getByNations(theUnit.getNation(), nation);

            // Check relations
            if (relation.getRelation() >= REL_COLONIAL_WAR) {
                // Build nation names
                final StringBuilder unitOwner = new StringBuilder();
                unitOwner.append(theUnit.getNation().getName());
                unitOwner.append(", ");

                final StringBuilder otherOwner = new StringBuilder();
                otherOwner.append(nation.getName());
                otherOwner.append(", ");

                // Keep track of nations
                final List<Nation> unitNations = new ArrayList<Nation>();
                unitNations.add(theUnit.getNation());

                final List<Nation> otherNations = new ArrayList<Nation>();
                otherNations.add(nation);

                // calculate power of nations
                int totEnemyTonnage = calcNationTonnage(nation, theUnit.getPosition());
                int totEnemyShips = calcWarShips(nation, theUnit.getPosition());
                int totAlliedTonnage = totTonnage;
                int totAlliedShips = totWarShips;

                // Identify other nations that are allies and/or enemies
                for (final Nation thirdNation : owners) {

                    // ignore basic pair of nations
                    if (thirdNation.getId() == theUnit.getNation().getId()
                            || thirdNation.getId() == nation.getId()) {
                        continue;
                    }

                    // Retrieve relations of unit owner with third nation
                    final NationsRelation unitRelation = getByNations(theUnit.getNation(), thirdNation);

                    // Retrieve relations of enemy with third nation
                    final NationsRelation theirRelation = getByNations(nation, thirdNation);

                    // Check if this is our Ally and their Enemy
                    if (unitRelation.getRelation() == REL_ALLIANCE
                            && theirRelation.getRelation() >= REL_COLONIAL_WAR) {

                        // calculate power of nation
                        totAlliedTonnage += calcNationTonnage(thirdNation, theUnit.getPosition());
                        totAlliedShips += calcWarShips(thirdNation, theUnit.getPosition());

                        // Update participant names
                        unitOwner.append(thirdNation.getName());
                        unitOwner.append(", ");

                        // update participants list
                        unitNations.add(thirdNation);

                    } else if (unitRelation.getRelation() >= REL_COLONIAL_WAR
                            && theirRelation.getRelation() == REL_ALLIANCE) {

                        // calculate power of nation
                        totEnemyTonnage += calcNationTonnage(thirdNation, theUnit.getPosition());
                        totEnemyShips += calcWarShips(thirdNation, theUnit.getPosition());

                        // Update participant names
                        otherOwner.append(thirdNation.getName());
                        otherOwner.append(", ");

                        // update participants list
                        otherNations.add(thirdNation);
                    }
                }

                // Check if we overrun opponent
                if (totEnemyTonnage < 4000
                        && totAlliedTonnage >= totEnemyTonnage * 6
                        && totAlliedShips + totEnemyShips > 0) {

                    final String unitNation = unitOwner.delete(unitOwner.length() - 2, unitOwner.length()).toString();
                    final String otherNation = otherOwner.delete(otherOwner.length() - 2, otherOwner.length()).toString();

                    doSeaOverrun(unitNation, unitNations, otherNations, theUnit.getPosition());
                    LOGGER.info("Naval force of " + unitNation + " overrun forces of " + otherNation + " at " + theUnit.getPosition().toString());

                } else if (totAlliedTonnage < 4000
                        && totEnemyTonnage >= totAlliedTonnage * 6
                        && totAlliedShips + totEnemyShips > 0) {

                    // enemy forces overrun ours
                    final String unitNation = unitOwner.delete(unitOwner.length() - 2, unitOwner.length()).toString();
                    final String otherNation = otherOwner.delete(otherOwner.length() - 2, otherOwner.length()).toString();

                    found = true;
                    doSeaOverrun(otherNation, otherNations, unitNations, theUnit.getPosition());
                    LOGGER.info("Naval force of " + otherNation + " overrun forces of " + unitNation + " at " + theUnit.getPosition().toString());

                } else if (totAlliedShips > 0 && totEnemyShips == 0) {
                    doChaseMerchant(theUnit.getNation(), nation, theUnit.getPosition());

                    LOGGER.info("Naval force of " + theUnit.getNation().getName() + " chased the merchant ships of " + nation.getName() + " at " + theUnit.getPosition().toString());

                } else if (totAlliedShips == 0 && totEnemyShips > 0) {
                    doChaseMerchant(nation, theUnit.getNation(), theUnit.getPosition());

                    LOGGER.info("Naval force of " + nation.getName() + " chased the merchant ships of " + theUnit.getNation().getName() + " at " + theUnit.getPosition().toString());

                } else if (totAlliedShips + totEnemyShips > 0) {
                    found = true;
                    addSeaBattleSector(theUnit.getPosition(), theUnit.getNation());
                    addSeaBattleSector(theUnit.getPosition(), nation);

                    // also involve allied nations
                    // Identify other nations that are allies and/or enemies
                    for (final Nation thirdNation : owners) {

                        // ignore basic pair of nations
                        if (thirdNation.getId() == theUnit.getNation().getId()
                                || thirdNation.getId() == nation.getId()) {
                            continue;
                        }

                        // Retrieve relations of unit owner with third nation
                        final NationsRelation unitRelation = getByNations(theUnit.getNation(), thirdNation);

                        // Retrieve relations of enemy with third nation
                        final NationsRelation theirRelation = getByNations(nation, thirdNation);

                        // Check if this is our Ally and their Enemy
                        if ((unitRelation.getRelation() == REL_ALLIANCE && theirRelation.getRelation() >= REL_COLONIAL_WAR)
                                || (theirRelation.getRelation() == REL_ALLIANCE && unitRelation.getRelation() >= REL_COLONIAL_WAR)) {

                            addSeaBattleSector(theUnit.getPosition(), thirdNation);
                        }
                    }

                    break;
                }
            }
        }

        return found;
    }

    /**
     * Check if in this sector there exist foreign+enemy ships.
     *
     * @param theUnit the entity that is moving.
     * @return true if enemy ships are present.
     */
    private boolean checkForPatrolBattle(final TypeOfUnit theUnit) {
        boolean found = false;
        final int totTonnage = calcNationTonnage(theUnit.getNation(), theUnit.getPosition());

        // Ignore land sectors
        final Sector thisSector = SectorManager.getInstance().getByPosition(theUnit.getPosition());
        if (thisSector.getTerrain().getId() != TERRAIN_O) {
            return found;
        }

        boolean isLoadUnload = false;
        if (theUnit instanceof Fleet) {
            // if the enemy fleet is trying to unload troops (programmers: check if the moving fleet is carrying troops and has an "un-boarding order")
            final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listLoadUnloadOrders((Fleet) theUnit);
            isLoadUnload = !lstOrders.isEmpty();
        }

        // +10% if the patrolling fleet/ship's speed (i.e. maximum movement potential) is larger than the moving fleets.
        // (in other words, if the patrol is faster than the enemy).
        final int unitMps = getMps(theUnit);

        if (patrolledSectorsIdxNation.containsKey(theUnit.getPosition())) {

            // Identify owners in this sector
            final Set<Nation> owners = patrolledSectorsIdxNation.get(theUnit.getPosition());
            for (final Nation nation : owners) {
                // check owner
                if (nation.getId() == theUnit.getNation().getId()) {
                    continue;
                }

                // Retrieve relations of foreign nation against us
                final NationsRelation relation = getByNations(theUnit.getNation(), nation);

                // Check relations
                if (relation.getRelation() >= REL_COLONIAL_WAR) {
                    // Check for overrun
                    final int patrolTonnage = calcPatrolTonnage(nation, theUnit.getPosition(), unitMps, isLoadUnload);

                    // Check if we overrun opponent
                    if (patrolTonnage > 0 && totTonnage < 4000 && patrolTonnage >= totTonnage * 6) {
                        final List<Nation> unitNations = new ArrayList<Nation>();
                        unitNations.add(theUnit.getNation());

                        final List<Nation> targetNations = new ArrayList<Nation>();
                        targetNations.add(nation);

                        // Patrol overrun enemy forces
                        found = true;
                        doSeaOverrun(nation.getName(), targetNations, unitNations, theUnit.getPosition());
                        LOGGER.info("Patrolling naval force of " + nation.getName() + " overrun forces of " + theUnit.getNation().getName() + " at " + theUnit.getPosition().toString());

                    } else if (patrolTonnage > 0 && patrolTonnage < 4000 && totTonnage >= patrolTonnage * 6) {

                        // enemy forces overrun patrol
                        doPatrolOverrun(theUnit.getNation(), nation, theUnit.getPosition());
                        LOGGER.info("Naval force of " + theUnit.getNation().getName() + " overrun patrolling forces of " + nation.getName() + " at " + theUnit.getPosition().toString());

                    } else if (patrolTonnage > 0) {
                        found = true;
                        doIntercept(nation, theUnit.getPosition());
                        addLandBattleSector(theUnit.getPosition(), theUnit.getNation());
                        addLandBattleSector(theUnit.getPosition(), nation);
                        break;
                    }
                }
            }
        }

        return found;
    }

    /**
     * Calculate the power of the units positioned at the given sector.
     *
     * @param targetNation  the nation to check.
     * @param thatPosition  the position to check.
     * @param ignoreHasLost ignore the units that have lost.
     * @return the total power of the units.
     */
    private int calcNationBattalions(final Nation targetNation, final Position thatPosition, final boolean ignoreHasLost) {
        int totCount = 0;
        final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByPositionNation(thatPosition, targetNation);
        for (final Brigade brigade : lstBrigades) {
            for (final Battalion battalion : brigade.getBattalions()) {
                if (battalion.getHeadcount() > 0
                        && (!ignoreHasLost || !battalion.getHasLost())) {
                    totCount++;
                }
            }
        }
        return totCount;
    }

    /**
     * Calculate the power of the units positioned at the given sector.
     *
     * @param targetNation the nation to check.
     * @param thatPosition the position to check.
     * @return the total power of the units.
     */
    private int calcNationTonnage(final Nation targetNation, final Position thatPosition) {
        int tonnage = 0;
        final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(thatPosition, targetNation);
        for (final Ship thisShip : lstShips) {
            if (thisShip.getType().getTypeId() == 13) {
                //fix ship class for tonage calculation.
                thisShip.getType().setShipClass(2);
            }
            if (thisShip.getType().getShipClass() > 0) {
                tonnage += thisShip.calcTonnage();

            } else {
                tonnage += 0.25d * thisShip.calcTonnage();
            }
            if (thisShip.getType().getTypeId() == 13) {
                //fix ship class after tonage calculation.
                thisShip.getType().setShipClass(0);
            }
        }

        return tonnage;
    }

    /**
     * Calculate the total number of war ships positioned at the given sector.
     *
     * @param targetNation the nation to check.
     * @param thatPosition the position to check.
     * @return the total power of the units.
     */
    private int calcWarShips(final Nation targetNation, final Position thatPosition) {
        int warShips = 0;
        final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(thatPosition, targetNation);
        for (final Ship thisShip : lstShips) {
            if (thisShip.getType().getShipClass() > 0 || thisShip.getType().getIntId() == 31) {
                warShips++;
            }
        }

        return warShips;
    }

    /**
     * Calculate the power of the units positioned at the given sector.
     *
     * @param targetNation the nation to check.
     * @param thatPosition the position to check.
     * @param unitMps      the maximum movement potential of the moving item.
     * @param isLoadUnload if the target fleet is trying to load/unload troops.
     * @return the total power of the units.
     */
    private int calcPatrolTonnage(final Nation targetNation,
                                  final Position thatPosition,
                                  final int unitMps,
                                  final boolean isLoadUnload) {
        int tonnage = 0;

        // Determine if position is a coastal tile
        int rollBaseTarget = 0;

        // if the enemy fleet is trying to unload troops (programmers: check if the moving fleet is carrying troops and has an "un-boarding order")
        if (isLoadUnload) {
            rollBaseTarget += 30;
        }

        // -25% if the interception tile is an oceanic one (i.e. no coastal tile)
        final boolean isOceanic = !SectorManager.getInstance().checkNationCoastal(thatPosition, targetNation);
        if (isOceanic) {
            rollBaseTarget -= 25;

        } else {
            // -20% at coasts of the colonial maps.
            if (thatPosition.getRegion().getId() != EUROPE) {
                rollBaseTarget -= 20;
            }

            // +30% if interception take place at a coastal tile that is adjacent to a land tile owned by the patrolling fleet's country.
            final boolean isOwnCoastal = SectorManager.getInstance().checkNationCoastal(thatPosition, targetNation);
            if (isOwnCoastal) {
                rollBaseTarget += 30;
            }
        }

        // Examine patrols that go through this position
        final Set<PatrolOrderDetails> lstOrders = patrolledSectorsIdxOrder.get(thatPosition);
        for (final PatrolOrderDetails theOrder : lstOrders) {
            if (!theOrder.getIntercept() && theOrder.getTonnage() > 0 && theOrder.getNation().getId() == targetNation.getId()) {
                // Determine modifiers to roll Target
                int rollTarget = rollBaseTarget;

                // +3% per unspent movement point of the patrolling fleet.
                rollTarget += 3 * theOrder.getUnspentMP();

                // +10% if the patrolling fleet/ship's speed (i.e. maximum movement potential) is larger
                // than the moving fleets. (in other words, if the patrol is faster than the enemy).
                if (theOrder.getMaxMP() > unitMps) {
                    rollTarget += 10;
                }

                // The minimum chance of interception is 10%.
                rollTarget = Math.min(10, rollTarget);

                // Throw roll
                final int roll = getParent().getRandomGen().nextInt(101) + 1;
                if (roll < rollTarget) {
                    tonnage += theOrder.getTonnage();
                    theOrder.setTriggered(true);

                } else {
                    theOrder.setTriggered(false);
                }
            }
        }

        return tonnage;
    }

    /**
     * Do Patrol Intercept.
     *
     * @param targetNation the nation that intercepts.
     * @param thatPosition the position of interception.
     */
    private void doIntercept(final Nation targetNation, final Position thatPosition) {
        final Set<PatrolOrderDetails> lstOrders = patrolledSectorsIdxOrder.get(thatPosition);
        for (final PatrolOrderDetails theOrder : lstOrders) {
            if (theOrder.getTriggered() && theOrder.getTonnage() > 0 && theOrder.getNation().getId() == targetNation.getId()) {
                theOrder.setIntercept(true);
                theOrder.setPosition(thatPosition);
            }
        }
    }

    /**
     * Remove the forces that were overrun during the movement.
     *
     * @param strOverruners the names of the nations that are doing the overrun.
     * @param overrunners   the nations that are doing the overrun.
     * @param targetNations the nations that are being overrun.
     * @param thatPosition  the position of the overrun.
     */
    private void doLandOverrun(final String strOverruners, final List<Nation> overrunners,
                               final List<Nation> targetNations,
                               final Position thatPosition) {
        int totHeadcount = 0;
        int totBattalions = 0;

        for (final Nation nation : targetNations) {
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByPositionNation(thatPosition, nation);
            for (final Brigade thisBrigade : lstBrigades) {
                // all battalions will be destroyed
                for (final Battalion battalion : thisBrigade.getBattalions()) {
                    totBattalions++;
                    totHeadcount += battalion.getHeadcount();
                    battalion.setHeadcount(0);
                }

                BrigadeManager.getInstance().update(thisBrigade);
            }

            final StringBuilder targetStr = new StringBuilder();
            targetStr.append("The land forces of ");
            targetStr.append(strOverruners);
            targetStr.append(" overrun our forces at ");
            targetStr.append(thatPosition.toString());
            targetStr.append(". We lost ");
            targetStr.append(totBattalions);
            targetStr.append(" battalions with a total of ");
            targetStr.append(totHeadcount);
            targetStr.append(" soldiers.");

            final StringBuilder overunnerStr = new StringBuilder();
            overunnerStr.append("Our land force overrun the forces of ");
            overunnerStr.append(nation.getName());
            overunnerStr.append(" at ");
            overunnerStr.append(thatPosition.toString());
            overunnerStr.append(". We destroyed ");
            overunnerStr.append(totBattalions);
            overunnerStr.append(" battalions with a total of ");
            overunnerStr.append(totHeadcount);
            overunnerStr.append(" soldiers.");

            // Send news to destroyed nation
            final int baseNewsId = news(getParent().getGame(), nation, overrunners.get(0), NEWS_MILITARY, false, 0, targetStr.toString());

            // Send news to overrunners
            for (final Nation overrunner : overrunners) {
                news(getParent().getGame(), overrunner, nation, NEWS_MILITARY, false, baseNewsId, overunnerStr.toString());
            }
        }
    }

    /**
     * Remove the forces that were overrun during the movement.
     *
     * @param strOverruners the names of the nations that are doing the overrun.
     * @param overrunners   the nations that are doing the overrun.
     * @param targetNations the nations that are being overrun.
     * @param thatPosition  the position of the overrun.
     */
    private void doSeaOverrun(final String strOverruners, final List<Nation> overrunners,
                              final List<Nation> targetNations,
                              final Position thatPosition) {
        final StringBuilder strDestroyed = new StringBuilder();
        final StringBuilder strCaptured = new StringBuilder();
        final StringBuilder strRunaway = new StringBuilder();

        for (final Nation nation : targetNations) {
            int totShips = 0;
            final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(thatPosition, nation);
            for (final Ship thisShip : lstShips) {
                if (thisShip.getCapturedByNation() != 0 || thisShip.getCondition() < 1) {
                    continue;
                }

                totShips++;

                // -all warships of the smaller fleet will be sunk
                if (thisShip.getType().getShipClass() > 0) {
                    thisShip.setCondition(0);
                    ShipManager.getInstance().update(thisShip);

                    strDestroyed.append(thisShip.getName());
                    strDestroyed.append(" [Class ").append(thisShip.getType().getShipClass()).append("], ");

                } else {
                    // the merchant ships will be captured 33% of the time,
                    // destroyed 33% of the time,
                    // or will stay with their owner 33% of the time
                    final int roll = getParent().getRandomGen().nextInt(101) + 1;
                    if (roll < 33) {
                        // captured
                        // choose random overrunner
                        final int overrunnerId = getParent().getRandomGen().nextInt(overrunners.size());
                        thisShip.setCapturedByNation(overrunners.get(overrunnerId).getId());
                        ShipManager.getInstance().update(thisShip);

                        strCaptured.append(thisShip.getName());
                        strCaptured.append(", ");

                    } else if (roll < 66) {
                        // destroyed
                        thisShip.setCondition(0);
                        ShipManager.getInstance().update(thisShip);

                        strDestroyed.append(thisShip.getName());
                        strDestroyed.append(" [Merchant], ");

                    } else {
                        strRunaway.append(thisShip.getName());
                        strRunaway.append(", ");
                    }
                }
            }

            if (totShips > 0) {
                final StringBuilder targetStr = new StringBuilder();
                targetStr.append("The naval forces of ");
                targetStr.append(strOverruners);
                targetStr.append(" overrun our forces at ");
                targetStr.append(thatPosition.toString());
                targetStr.append(". ");

                final StringBuilder overunnerStr = new StringBuilder();
                overunnerStr.append("Our naval forces overrun the forces of ");
                overunnerStr.append(nation.getName());
                overunnerStr.append(" at ");
                overunnerStr.append(thatPosition.toString());
                overunnerStr.append(". ");

                if (strDestroyed.length() > 0) {
                    targetStr.append("Our ships ");
                    targetStr.append(strDestroyed.substring(0, strDestroyed.length() - 2));
                    targetStr.append(" were totally destroyed. ");

                    overunnerStr.append("We destroyed the ships ");
                    overunnerStr.append(strDestroyed.substring(0, strDestroyed.length() - 2));
                    overunnerStr.append(". ");
                }

                if (strCaptured.length() > 0) {
                    targetStr.append("Our ships ");
                    targetStr.append(strCaptured.substring(0, strCaptured.length() - 2));
                    targetStr.append(" were captured by the enemy naval forces. ");

                    overunnerStr.append("We captured the ships ");
                    overunnerStr.append(strCaptured.substring(0, strCaptured.length() - 2));
                    overunnerStr.append(". ");
                }

                if (strRunaway.length() > 0) {
                    targetStr.append("Our ships ");
                    targetStr.append(strRunaway.substring(0, strRunaway.length() - 2));
                    targetStr.append(" managed to sail away. ");

                    overunnerStr.append("We missed to capture the ships ");
                    overunnerStr.append(strRunaway.substring(0, strRunaway.length() - 2));
                    overunnerStr.append(". ");
                }

                // Send news to destroyed nation
                final int baseNewsId = news(getParent().getGame(), nation, overrunners.get(0), NEWS_MILITARY, false, 0, targetStr.toString());

                // Send news to overrunners
                for (final Nation overrunner : overrunners) {
                    news(getParent().getGame(), overrunner, nation, NEWS_MILITARY, false, baseNewsId, overunnerStr.toString());
                }
            }
        }
    }

    /**
     * Try to chase merchant ships during the movement.
     *
     * @param chaseNation  the nation that is doing the chase.
     * @param targetNation the nation that is being chased.
     * @param thatPosition the position of the chase.
     */
    private void doChaseMerchant(final Nation chaseNation, final Nation targetNation, final Position thatPosition) {
        final StringBuilder strDestroyed = new StringBuilder();
        final StringBuilder strCaptured = new StringBuilder();
        final StringBuilder strRunaway = new StringBuilder();

        final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(thatPosition, targetNation);
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            // the merchant ships will be captured 33% of the time,
            // destroyed 33% of the time,
            // or will stay with their owner 33% of the time
            final int roll = getParent().getRandomGen().nextInt(101) + 1;
            if (roll < 33) {
                // captured
                thisShip.setCapturedByNation(chaseNation.getId());
                ShipManager.getInstance().update(thisShip);

                strCaptured.append(thisShip.getName());
                strCaptured.append(", ");

            } else if (roll < 66) {
                // destroyed
                thisShip.setCondition(0);
                ShipManager.getInstance().update(thisShip);

                strDestroyed.append(thisShip.getName());
                strDestroyed.append(" [Merchant], ");

            } else {
                strRunaway.append(thisShip.getName());
                strRunaway.append(", ");
            }
        }

        final StringBuilder targetStr = new StringBuilder();
        targetStr.append("The naval force of ");
        targetStr.append(chaseNation.getName());
        targetStr.append(" chased our merchant ships at ");
        targetStr.append(thatPosition.toString());
        targetStr.append(". ");

        final StringBuilder overunnerStr = new StringBuilder();
        overunnerStr.append("Our naval force chased the merchant ships of ");
        overunnerStr.append(targetNation.getName());
        overunnerStr.append(" at ");
        overunnerStr.append(thatPosition.toString());
        overunnerStr.append(". ");

        if (strDestroyed.length() > 0) {
            targetStr.append("Our ships ");
            targetStr.append(strDestroyed.substring(0, strDestroyed.length() - 2));
            targetStr.append(" were totally destroyed. ");

            overunnerStr.append("We destroyed the ships ");
            overunnerStr.append(strDestroyed.substring(0, strDestroyed.length() - 2));
            overunnerStr.append(". ");
        }

        if (strCaptured.length() > 0) {
            targetStr.append("Our ships ");
            targetStr.append(strCaptured.substring(0, strCaptured.length() - 2));
            targetStr.append(" were captured by the enemy naval forces. ");

            overunnerStr.append("We captured the ships ");
            overunnerStr.append(strCaptured.substring(0, strCaptured.length() - 2));
            overunnerStr.append(". ");
        }

        if (strRunaway.length() > 0) {
            targetStr.append("Our ships ");
            targetStr.append(strRunaway.substring(0, strRunaway.length() - 2));
            targetStr.append(" managed to sail away. ");

            overunnerStr.append("We missed to capture the ships ");
            overunnerStr.append(strRunaway.substring(0, strRunaway.length() - 2));
            overunnerStr.append(". ");
        }

        newsPair(chaseNation, targetNation, NEWS_MILITARY, overunnerStr.toString(), targetStr.toString());
    }

    /**
     * Remove the patrolling forces that were overrun during the movement.
     *
     * @param overrunner   the nation that is doing the overrun.
     * @param targetNation the nation that is being overrun.
     * @param thatPosition the position of the overrun.
     */
    private void doPatrolOverrun(final Nation overrunner, final Nation targetNation, final Position thatPosition) {
        final StringBuilder strDestroyed = new StringBuilder();
        final StringBuilder strCaptured = new StringBuilder();
        final StringBuilder strRunaway = new StringBuilder();

        final List<Ship> lstShips = new ArrayList<Ship>();
        final Set<PatrolOrderDetails> lstOrders = patrolledSectorsIdxOrder.get(thatPosition);
        for (final PatrolOrderDetails theOrder : lstOrders) {
            if (theOrder.getTriggered() && theOrder.getTonnage() > 0 && theOrder.getNation().getId() == targetNation.getId()) {
                theOrder.setTonnage(0);
                lstShips.addAll(theOrder.getShips());
            }
        }

        int totShips = 0;
        for (final Ship thisShip : lstShips) {
            if (thisShip.getCapturedByNation() != 0) {
                continue;
            }

            // -all warships of the smaller fleet will be sunk
            if (thisShip.getType().getShipClass() > 0) {
                totShips++;

                thisShip.setCondition(0);
                ShipManager.getInstance().update(thisShip);

                strDestroyed.append(thisShip.getName());
                strDestroyed.append(" [Class ").append(thisShip.getType().getShipClass()).append("], ");

            } else {
                // the merchant ships will be captured 33% of the time,
                // destroyed 33% of the time,
                // or will stay with their owner 33% of the time
                final int roll = getParent().getRandomGen().nextInt(101) + 1;
                if (roll < 33) {
                    totShips++;

                    // captured
                    thisShip.setCapturedByNation(overrunner.getId());
                    ShipManager.getInstance().update(thisShip);

                    strCaptured.append(thisShip.getName());
                    strCaptured.append(", ");

                } else if (roll < 66) {
                    totShips++;

                    // destroyed
                    thisShip.setCondition(0);
                    ShipManager.getInstance().update(thisShip);

                    strDestroyed.append(thisShip.getName());
                    strDestroyed.append(" [Merchant], ");

                } else {
                    strRunaway.append(thisShip.getName());
                    strRunaway.append(", ");
                }
            }
        }

        if (totShips > 0) {
            final StringBuilder targetStr = new StringBuilder();
            targetStr.append("The patrolling naval force of ");
            targetStr.append(overrunner.getName());
            targetStr.append(" overrun our forces at ");
            targetStr.append(thatPosition.toString());
            targetStr.append(". ");

            final StringBuilder overunnerStr = new StringBuilder();
            overunnerStr.append("Our patrolling naval force overrun the forces of ");
            overunnerStr.append(targetNation.getName());
            overunnerStr.append(" at ");
            overunnerStr.append(thatPosition.toString());
            overunnerStr.append(". ");

            if (strDestroyed.length() > 0) {
                targetStr.append("Our ships ");
                targetStr.append(strDestroyed.substring(0, strDestroyed.length() - 2));
                targetStr.append(" were totally destroyed. ");

                overunnerStr.append("We destroyed the ships ");
                overunnerStr.append(strDestroyed.substring(0, strDestroyed.length() - 2));
                overunnerStr.append(". ");
            }

            if (strCaptured.length() > 0) {
                targetStr.append("Our ships ");
                targetStr.append(strCaptured.substring(0, strCaptured.length() - 2));
                targetStr.append(" were captured by the enemy naval forces. ");

                overunnerStr.append("We captured the ships ");
                overunnerStr.append(strCaptured.substring(0, strCaptured.length() - 2));
                overunnerStr.append(". ");
            }

            if (strRunaway.length() > 0) {
                targetStr.append("Our ships ");
                targetStr.append(strRunaway.substring(1, strRunaway.length() - 2));
                targetStr.append(" managed to sail away. ");

                overunnerStr.append("We missed to capture the ships ");
                overunnerStr.append(strRunaway.substring(1, strRunaway.length() - 2));
                overunnerStr.append(". ");
            }

            newsPair(overrunner, targetNation, NEWS_MILITARY, overunnerStr.toString(), targetStr.toString());
        }
    }

    /**
     * Process all sectors one by one until end of movement.
     *
     * @param theUnit       the entity subject to this movement order.
     * @param lstPosSectors a list of sectors that correspond to all the coordinates found in the movement order's parameters.
     */
    protected void performMovement(final TypeOfUnit theUnit, final List<Sector> lstPosSectors) {
        int availMps = getMps(theUnit);
        final int maxConquers = getMaxConquers(theUnit);
        final int maxNeutralConquers = getMaxNeutralConquers(theUnit);
        int conqueredSectors = 0;
        int conqueredNeutrals = 0;
        int sectorsMoved = 0;

        // Check if the position where the unit has been blocked for battle
        if ((theUnit instanceof Ship
                || theUnit instanceof Fleet)
                && (seaBattleSectorsIdxNation.containsKey(theUnit.getPosition()))) {

            // Check if nation is involved
            if (seaBattleSectorsIdxNation.get(theUnit.getPosition()).contains(theUnit.getNation())) {
                // end movement here
                getOrder().setResult(-3);
                getOrder().setExplanation("Movement stopped by hostile forces.");
                return;
            }

        } else if ((theUnit instanceof Brigade
                || theUnit instanceof Corp
                || theUnit instanceof Army
                || theUnit instanceof BaggageTrain)
                && (landBattleSectorsIdxNation.containsKey(theUnit.getPosition()))) {

            // Check if nation is involved
            if (landBattleSectorsIdxNation.get(theUnit.getPosition()).contains(theUnit.getNation())) {
                if (theUnit instanceof BaggageTrain) {
                    // only if loaded with troops
                    ((BaggageTrain) theUnit).initializeVariables();
                    // Check if the entity is carrying units, and update their movement & engage counters too
                    if (((BaggageTrain) theUnit).getHasTroops()) {
                        // end movement here
                        getOrder().setResult(-3);
                        getOrder().setExplanation("Movement stopped by hostile forces.");
                        return;
                    }

                } else {
                    // end movement here
                    getOrder().setResult(-3);
                    getOrder().setExplanation("Movement stopped by hostile forces.");
                    return;
                }
            }
        }

        // Check if this an army inside a fort and an enemy force waiting outside the fort
        if (theUnit instanceof Army
                || theUnit instanceof Corp
                || theUnit instanceof Brigade
                || theUnit instanceof BaggageTrain) {
            final Sector initSector = SectorManager.getInstance().getByPosition(theUnit.getPosition());
            if (initSector.hasFort()
                    && initSector.getNation().getId() == theUnit.getNation().getId()) {
                // update record
                update(theUnit);

                // check if enemy forces are in the sector
                final boolean willBattle = checkForBattle(theUnit, initSector, true, true);

                if (willBattle) {
                    // apply attrition
                    attrition(theUnit, initSector, false, willBattle);

                    // update record
                    update(theUnit);

                    // end movement here
                    getOrder().setExplanation("Enemy forces encountered while moving out of the fort.");
                    getOrder().setResult(sectorsMoved + 1);
                    return;
                }
            }
        }

        for (final Sector nextSector : lstPosSectors) {
            // Identify MP cost of sector based on the terrain type and the month
            final int mpCost = calcMPcost(theUnit, nextSector);

            // Check available movement points
            if (availMps >= mpCost) {

                // Allow to move if this is a SEA sector
                // OTHERWISE
                // Check ownership of sector and if item is allowed to move in this sector (relations)
                if ((getType() == TPE_SEA && nextSector.getTerrain().getId() == TERRAIN_O)
                        || canCross(theUnit.getNation(), nextSector, theUnit)) {

                    if (nationsToSectorsConquered.containsKey(getOrder().getNation().getId())) {
                        if (nationsToSectorsConquered.get(getOrder().getNation().getId()).get(1).contains(nextSector)) {
                            conqueredSectors++;
                        }
                        if (nationsToSectorsConquered.get(getOrder().getNation().getId()).get(0).contains(nextSector)) {
                            conqueredNeutrals++;
                        }
                    }

                    // check if sectors HAS TO BE conquered otherwise movement is not allowed -- total number of sectors conquered
                    final boolean isConquering = needConquer(theUnit, nextSector);

                    // check if conquering neutral sectors -- total number of neutral sectors conquered
                    final boolean isNeutralConquering = needNeutralConquer(theUnit, nextSector) && conqueredNeutrals < maxNeutralConquers;

                    // combines the above to checks to simplify if statements
                    final boolean willConquer = isConquering || isNeutralConquering;

                    // determine if the unit can conquer neutral or enemy territories
                    final boolean canConquer = canConquer(theUnit);

                    // 1. If it can move freely regardless of relations (commander, spy)
                    // 2. Forced march
                    if (!willConquer || canConquer) {

                        // Check tha maximum number of conquered sectors has not been reached
                        if (isConquering && canConquer && conqueredSectors >= maxConquers) {
                            // Movement stops here
                            availMps = 0;

                            // update record
                            update(theUnit);

                            // cannot conquer more sectors
                            getOrder().setResult(5);
                            final String existExplanation = getOrder().getExplanation();
                            if ((existExplanation != null) && (existExplanation.length() > 0)) {
                                getOrder().setExplanation(existExplanation + ", maximum number of sectors conquered reached.");
                                getOrder().setResult(sectorsMoved + 1);

                            } else {
                                getOrder().setExplanation("Maximum number of sectors conquered reached.");
                                getOrder().setResult(sectorsMoved + 1);
                            }

                        } else {

                            // move to next sector
                            availMps -= mpCost;

                            // everything OK, update position
                            sectorsMoved++;
                            theUnit.setPosition(nextSector.getPosition());

                            // update record
                            update(theUnit);

                            // check if enemy forces are located on the sector
                            final boolean spottedByPatrol = getType() == TPE_SEA && checkForPatrolBattle(theUnit);
                            final boolean willBattle = checkForBattle(theUnit, nextSector, false, false) || checkForSeaBattle(theUnit) || spottedByPatrol;

                            // apply attrition
                            attrition(theUnit, nextSector, false, willBattle);

                            // update record
                            update(theUnit);

                            // Check for overrun rule
                            if (willBattle) {
                                // Movement stops here
                                availMps = 0;

                                /**
                                 * @todo If the train meets an enemy army (either due to the train’s or the army’s movement)
                                 * and is loaded only with goods then there is an 80% chance of being captured along with
                                 * its goods and 20% of being destroyed.
                                 */
                                if (spottedByPatrol) {
                                    LOGGER.info("Patrolling forces were encountered at " + nextSector.getPosition().toString());

                                } else {
                                    LOGGER.info("Hostile forces were encountered at " + nextSector.getPosition().toString());
                                }

                            } else if (willConquer) {
                                if (!nationsToSectorsConquered.containsKey(getOrder().getNation().getId())) {
                                    nationsToSectorsConquered.put(getOrder().getNation().getId(), new HashMap<Integer, Set<Sector>>());
                                    nationsToSectorsConquered.put(getOrder().getNation().getId(), new HashMap<Integer, Set<Sector>>());
                                    nationsToSectorsConquered.get(getOrder().getNation().getId()).put(0, new HashSet<Sector>());
                                    nationsToSectorsConquered.get(getOrder().getNation().getId()).put(1, new HashSet<Sector>());

                                }

                                if (isConquering) {
                                    if (!nationsToSectorsConquered.get(getOrder().getNation().getId()).get(1).contains(nextSector)) {
                                        nationsToSectorsConquered.get(getOrder().getNation().getId()).get(1).add(nextSector);
                                        // increase counter + conquer Sector
                                        conqueredSectors++;
                                    }

                                } else {
                                    if (!nationsToSectorsConquered.get(getOrder().getNation().getId()).get(0).contains(nextSector)) {
                                        nationsToSectorsConquered.get(getOrder().getNation().getId()).get(0).add(nextSector);
                                        // increase counter + conquer Neutral sector
                                        conqueredNeutrals++;
                                    }
                                }

                                // Conquer sector
                                nextSector.setTempNation(theUnit.getNation());
                                SectorManager.getInstance().update(nextSector);

                                // Make sure the sector is truly conquered at the end, when all movement orders are processed.
                                getParent().addConqueredSector(nextSector);
                            }

                            // Check if entity is crossing a storm
                            if (nextSector.getStorm() > 0) {
                                crossStorm(theUnit, nextSector);
                            }

                            getOrder().setResult(sectorsMoved + 1);
                            final String existExplanation = getOrder().getExplanation();
                            if ((existExplanation != null) && (existExplanation.length() > 0)) {
                                if (isConquering) {
                                    getOrder().setExplanation(existExplanation + ", conquered " + nextSector.getPosition().toString());

                                } else if (isNeutralConquering) {
                                    getOrder().setExplanation(existExplanation + ", conquered neutral " + nextSector.getPosition().toString());

                                } else {
                                    getOrder().setExplanation(existExplanation + ", " + nextSector.getPosition().toString());
                                }

                            } else {
                                if (isConquering) {
                                    getOrder().setExplanation(getName(theUnit) + " conquered " + nextSector.getPosition().toString());

                                } else if (isNeutralConquering) {
                                    getOrder().setExplanation(getName(theUnit) + " conquered neutral " + nextSector.getPosition().toString());

                                } else {
                                    getOrder().setExplanation(getName(theUnit) + " moved to " + nextSector.getPosition().toString());
                                }
                            }

                            if (willBattle) {
                                // Movement stops here
                                getOrder().setExplanation(getOrder().getExplanation() + ". Encountered hostile forces. Movement stopped.");
                                break;
                            }
                        }

                    } else {
                        // Movement stops here
                        // update record
                        update(theUnit);

                        // cannot conquer sectors at all !
                        getOrder().setResult(sectorsMoved + 1);
                        final String existExplanation = getOrder().getExplanation();
                        if ((existExplanation != null) && (existExplanation.length() > 0)) {
                            getOrder().setExplanation(existExplanation + ", unit cannot conquer sectors.");

                        } else {
                            getOrder().setExplanation("Unit cannot conquer sectors.");
                        }

                        break;
                    }

                } else {
                    // update record
                    update(theUnit);

                    // Motion stops here
                    getOrder().setResult(sectorsMoved + 1);
                    final String existExplanation = getOrder().getExplanation();
                    if ((existExplanation != null) && (existExplanation.length() > 0)) {
                        getOrder().setExplanation(existExplanation + ". Cannot cross sector due to relations with owner of sector");

                    } else {
                        getOrder().setExplanation("Cannot cross sector due to relations with owner of sector");
                    }

                    break;
                }

            } else {
                // The potential list of sectors is too far away.
                getOrder().setResult(sectorsMoved + 1);
                final String existExplanation = getOrder().getExplanation();
                if ((existExplanation != null) && (existExplanation.length() > 0)) {
                    getOrder().setExplanation(existExplanation + ". Used up all the available movement points");

                } else {
                    getOrder().setExplanation("Used up all the available movement points");
                }
                break;
            }
        }

        // cannot conquer more sectors
        final String existExplanation = getOrder().getExplanation();
        if ((existExplanation == null) || (existExplanation.length() == 0)) {
            getOrder().setExplanation("Unit cannot move.");
            getOrder().setResult(6);
        }
    }

    /**
     * Calculate the MP required to move to the target sector.
     *
     * @param theUnit    the entity subject to this movement order.
     * @param nextSector the target sector to inspect.
     * @return the MP cost.
     */
    private int calcMPcost(final TypeOfUnit theUnit, final Sector nextSector) {
        int mpCost;
        if (theUnit instanceof Ship || theUnit instanceof Fleet) {
            mpCost = 1;

            // Check if sector is affected by storm
            if (nextSector.getStorm() > 0) {
                // It will cost one extra movement point per storm coordinate passing through
                mpCost++;
            }

            // make sure ship is not captured/overrun
            if (theUnit instanceof Ship) {
                final Ship theShip = (Ship) theUnit;
                if (theShip.getCondition() == 0
                        || theShip.getCapturedByNation() != 0) {
                    mpCost = Integer.MAX_VALUE;
                    LOGGER.info("Ship " + theShip.getName() + "/ID=" + theShip.getShipId() + " was just captured/overrun. Movement must stop");
                }
            } else {
                // make sure the fleet is not comprised of overrun/captured ships
                int nonCaptured = 0;
                final Fleet theFleet = (Fleet) theUnit;
                final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(), theFleet.getFleetId());
                for (Ship theShip : lstShips) {
                    if (theShip.getCondition() > 0
                            && theShip.getCapturedByNation() == 0) {
                        nonCaptured++;
                    }
                }

                // Check if fleet has at least 1 sheep
                if (nonCaptured == 0) {
                    mpCost = Integer.MAX_VALUE;
                    LOGGER.info("Fleet " + theFleet.getName() + "/ID=" + theFleet.getFleetId() + " contains only ships captured/overrun. Movement must stop");
                }
            }

        } else {
            if (isSevereWinter(nextSector.getPosition())) {
                mpCost = nextSector.getTerrain().getMpsWinter();

            } else {
                mpCost = nextSector.getTerrain().getMps();
            }

            // Land movement in the colonies uses double the amount of Mps.
            if (!getParent().getGame().isFullMpsAtColonies()
                    && nextSector.getTerrain().getId() != TERRAIN_O
                    && nextSector.getPosition().getRegion().getId() != EUROPE) {
                mpCost *= 2;
            }
        }
        return mpCost;
    }

    private int getNation(final TypeOfUnit theUnit) {
        int nation = theUnit.getNation().getId();

        if (theUnit instanceof Ship) {
            final Ship thisShip = (Ship) theUnit;
            if (thisShip.getCapturedByNation() != 0) {
                nation = thisShip.getCapturedByNation();
            }
        }

        return nation;
    }

    /**
     * Process this particular movement order.
     */
    @SuppressWarnings("unchecked")
    public void process() {
        // Retrieve item to move
        final TypeOfUnit theUnit = getMobileUnit();
        if (theUnit == null) {
            getOrder().setResult(-1);
            getOrder().setExplanation("cannot find subject of movement order");

        } else {
            // Check ownership
            if (getNation(theUnit) == getOrder().getNation().getId()) {

                // Check if unit has already moved
                if (hasNotMoved(theUnit)) {

                    // Check if this unit is depending on another unit (hierarchy)
                    if (isBounded(theUnit)) {
                        // Motion stops here
                        getOrder().setResult(-1);
                        getOrder().setExplanation(getName(theUnit) + " cannot move separately.");

                    } else {
                        // Extract order parameters
                        final String movePath = getMovementPath();

                        final boolean isEngageMove = (getOrder().getParameter4() != null
                                && getOrder().getParameter4().equals("1"));

                        performMovement(theUnit, getPossibleSectors(theUnit, isEngageMove, movePath));

                        if (getOrder().getExplanation() == null || getOrder().getExplanation().length() < 1) {
                            LOGGER.error("Movement without explanation.");
                        }

                        // Mark unit as moved
                        getMovedItems().add(theUnit);

                        // If unit is a ship or fleet then add attrition due to movement
                        if (theUnit instanceof Ship) {
                            final Ship theShip = (Ship) theUnit;

                            // The ship will suffer an attrition of 1-2%
                            final int rndNum = getParent().getRandomGen().nextInt(2) + 1;

                            theShip.setCondition(theShip.getCondition() - rndNum);
                            ShipManager.getInstance().update(theShip);

                        } else if (theUnit instanceof Fleet) {
                            final Fleet theFleet = (Fleet) theUnit;
                            final List<Ship> lstShips = ShipManager.getInstance().listByFleet(getOrder().getGame(),
                                    theFleet.getFleetId());

                            // The ship will suffer an attrition of 1-2%
                            final int rndNum = getParent().getRandomGen().nextInt(2) + 1;

                            for (final Ship theShip : lstShips) {
                                theShip.setCondition(theShip.getCondition() - rndNum);
                                ShipManager.getInstance().update(theShip);
                            }

                        } else if (theUnit instanceof BaggageTrain) {
                            final BaggageTrain theTrain = (BaggageTrain) theUnit;

                            int rndNum = getParent().getRandomGen().nextInt(2) + 1;

                            if (isSevereWinter(theTrain.getPosition())) {
                                // Here isWinter argument is used to signify if item is jumping to another region
                                rndNum++;
                            }

                            // If the baggage train carries any troops on it, its attrition losses are doubled.
                            if (theTrain.getHasTroops()) {
                                rndNum *= 2;
                            }

                            theTrain.setCondition(theTrain.getCondition() - rndNum);
                            BaggageTrainManager.getInstance().update(theTrain);
                        }
                    }

                } else {
                    getOrder().setResult(-2);
                    getOrder().setExplanation(getName(theUnit) + " has already moved");
                }

            } else {
                getOrder().setResult(-3);
                getOrder().setExplanation("not owner of unit");
            }
        }
    }

}

