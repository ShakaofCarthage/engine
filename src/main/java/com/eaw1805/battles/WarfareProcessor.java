package com.eaw1805.battles;

import com.eaw1805.battles.naval.AbstractNavalBattleRound;
import com.eaw1805.battles.naval.NavalBattleProcessor;
import com.eaw1805.battles.naval.WeatherSelector;
import com.eaw1805.battles.naval.result.RoundStat;
import com.eaw1805.battles.tactical.AbstractTacticalBattleRound;
import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.battles.tactical.handtohand.TroopHandToHandCombat;
import com.eaw1805.battles.tactical.longrange.ArtilleryLongRangeCombat;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.battles.NavalBattleReportManager;
import com.eaw1805.data.managers.battles.TacticalBattleReportManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.NavalBattleReport;
import com.eaw1805.data.model.battles.TacticalBattleReport;
import com.eaw1805.data.model.comparators.NationSort;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.orders.PatrolOrderDetails;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Class responsible for scanning the map's sectors in order to find battling
 * armies in the same sector.
 */
public class WarfareProcessor
        implements RegionConstants, TerrainConstants, ProductionSiteConstants, ReportConstants, VPConstants,
        RelationConstants, NewsConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(WarfareProcessor.class);

    /**
     * The game processed.
     */
    private final transient Game game;

    /**
     * The random generator.
     */
    private final transient java.util.Random randomGen;

    /**
     * The weather selector.
     */
    private final transient WeatherSelector weatherSelector;

    /**
     * The active patrol orders.
     */
    private final transient Map<Integer, PatrolOrderDetails> patrolOrders;

    /**
     * the parent object.
     */
    private final transient OrderProcessor parent;

    /**
     * nation relations.
     */
    private static Map<Nation, Map<Nation, NationsRelation>> relationsMap;

    /**
     * Default constructor.
     *
     * @param engine the game engine instance.
     * @param caller the OrderProcessor that invoked us.
     */
    public WarfareProcessor(final GameEngine engine, final OrderProcessor caller) {
        parent = caller;
        game = caller.getGame();
        randomGen = caller.getRandomGen();
        weatherSelector = new WeatherSelector(engine);
        patrolOrders = caller.getPatrolOrders();

        // Retrieve nation relations
        final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        relationsMap = mapNationRelation(engine.getGame());
        trans.commit();

        LOGGER.debug("WarfareProcessor instantiated.");
    }

    /**
     * Get the game object.
     *
     * @return the game object.
     */
    public final Game getGame() {
        return game;
    }

    /**
     * Get the random number generator.
     *
     * @return the random number generator.
     */
    public final java.util.Random getRandomGen() {
        return randomGen;
    }

    /**
     * Get the Relations from the database that corresponds to the input
     * parameters.
     *
     * @param owner  the Owner of the Report object.
     * @param target the Target of the Report object.
     * @return an Entity object.
     */
    public NationsRelation getByNations(final Game game, final Nation owner, final Nation target) {
        return relationsMap.get(owner).get(target);
    }

    private Map<Nation, Map<Nation, NationsRelation>> mapNationRelation(final Game thisGame) {
        final Map<Nation, Map<Nation, NationsRelation>> mapRelations = new HashMap<Nation, Map<Nation, NationsRelation>>();
        final List<Nation> lstNations = NationManager.getInstance().list();
        for (final Nation nation : lstNations) {
            final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(thisGame, nation);
            final Map<Nation, NationsRelation> nationRelations = new HashMap<Nation, NationsRelation>();
            for (final NationsRelation relation : lstRelations) {
                nationRelations.put(relation.getTarget(), relation);
            }
            mapRelations.put(nation, nationRelations);
        }

        return mapRelations;
    }

    /**
     * Investigate sea sectors patrolled for possible naval battles.
     *
     * @return list of naval battles initiated.
     */
    protected List<BattleField> searchSeaPatrols() {
        final List<BattleField> battleFields = new ArrayList<BattleField>();

        for (final PatrolOrderDetails orderDetails : patrolOrders.values()) {
            if (orderDetails.getIntercept() && orderDetails.getTonnage() > 0) {
                final Sector sector = SectorManager.getInstance().getByPosition(orderDetails.getPosition());

                // Ignore land sectors
                if (sector.getTerrain().getId() != TERRAIN_O) {
                    continue;
                }

                // Identify owners in this sector
                final List<Nation> owners = ShipManager.getInstance().listOwners(sector.getPosition());
                LOGGER.debug(owners.size() + " nations gathered at " + sector.getPosition().toString());

                if (owners.size() >= 1) {
                    // This is an easy case
                    // Ignore off-map trade cities
                    if (sector.getPosition().getRegion().getId() == EUROPE &&
                            ((sector.getPosition().getX() == 0 && sector.getPosition().getY() == 2)
                                    || (sector.getPosition().getX() == 0 && sector.getPosition().getY() == 26))) {

                        LOGGER.debug("Naval patrol not allowed in off-map trade cities");

                    } else {
                        // Retrieve relations with foreign nation
                        final NationsRelation relation = getByNations(game, orderDetails.getNation(), owners.get(0));

                        // Retrieve reverse relations with foreign nation
                        final NationsRelation reverse = getByNations(game, owners.get(0), orderDetails.getNation());

                        // Check relations
                        if (relation.getRelation() == REL_COLONIAL_WAR || reverse.getRelation() == REL_COLONIAL_WAR) {
                            final BattleField bfield = new BattleField(sector);
                            bfield.addNation(0, owners.get(0));
                            bfield.addNation(1, orderDetails.getNation());
                            bfield.setPatrolForce(orderDetails.getShips());
                            bfield.setPatrol(true);
                            battleFields.add(bfield);
                            LOGGER.debug(bfield.toString());
                        }
                    }
                }
            }
        }

        return battleFields;
    }

    /**
     * Investigate specific sector and produce battles depending on relations between participants.
     *
     * @param sector       the sector to examine.
     * @param participants the armies located in the sector.
     * @return one or more battles initiated.
     */
    protected List<BattleField> searchSeaSector(final Sector sector, final List<Nation> participants) {
        final List<BattleField> battleFields = new ArrayList<BattleField>();
        final List<Nation> sideA = new ArrayList<Nation>();
        final List<Nation> sideB = new ArrayList<Nation>();

        // check that at least 1 nation has moved ships and can therefore initiate a battle
        boolean foundMovingUnit = false;
        final List<Ship> lstShips = ShipManager.getInstance().listByGamePosition(sector.getPosition());
        for (final Ship ship : lstShips) {
            if (ship.getHasMoved()) {
                foundMovingUnit = true;
                break;
            }
        }

        if (!foundMovingUnit) {
            participants.clear();
            return battleFields;
        }

        // find potential match
        pickSides(sector, participants, sideA, sideB, true);

        if (!sideB.isEmpty() && !sideA.isEmpty()) {
            final BattleField bfield = new BattleField(sector);

            for (Nation nation : sideA) {
                bfield.addNation(0, nation);
            }

            for (Nation nation : sideB) {
                bfield.addNation(1, nation);
            }

            battleFields.add(bfield);
            LOGGER.debug(bfield.toString());
        }

        return battleFields;
    }

    /**
     * Investigate specific sector and produce battles depending on relations between participants.
     *
     * @param sector       the sector to examine.
     * @param participants the armies located in the sector.
     * @return one or more battles initiated.
     */
    protected List<BattleField> searchSector(final Sector sector, final List<Nation> participants) {
        final List<BattleField> battleFields = new ArrayList<BattleField>();
        final List<Nation> sideA = new ArrayList<Nation>();
        final List<Nation> sideB = new ArrayList<Nation>();

        // check that at least 1 nation has moved troops and can therefore initiate a battle
        boolean foundMovingUnit = false;
        final List<Battalion> lstBattalions = BattalionManager.getInstance().listByGamePosition(sector.getPosition());
        for (final Battalion battalion : lstBattalions) {
            if (battalion.getHasMoved()) {
                foundMovingUnit = true;
                break;
            }
        }

        if (!foundMovingUnit) {
            participants.clear();
            return battleFields;
        }

        // find potential match
        pickSides(sector, participants, sideA, sideB, false);

        if (!sideB.isEmpty() && !sideA.isEmpty()) {
            final BattleField bfield = new BattleField(sector);

            for (Nation nation : sideA) {
                bfield.addNation(0, nation);
            }

            for (Nation nation : sideB) {
                bfield.addNation(1, nation);
            }

            battleFields.add(bfield);
            LOGGER.debug(bfield.toString());
        }

        return battleFields;
    }

    private void pickSides(final Sector sector, final List<Nation> participants,
                           final List<Nation> sideA,
                           final List<Nation> sideB,
                           final boolean isNaval) {
        boolean changeFoundA = false;
        boolean changeFoundB = false;

        // Check if the OWNER [A] of the tile has an army in the tile.
        if (participants.contains(sector.getNation())) {
            sideA.add(sector.getNation());
            participants.remove(sector.getNation());
            changeFoundA = true;

        } else if (sideA.isEmpty()) {
            // pick a random nation
            java.util.Collections.shuffle(participants);
            sideA.add(participants.remove(0));
            changeFoundA = true;

        } else {
            // Check if there is an army [C] that is ALLIED to the OWNER [A] and at WAR with the enemy [B] of step 1.
            // If yes, army [C] joins the battle.
            final Nation potentialAlly = pickAlly(sector.getPosition().getRegion(), sideA, sideB, participants, isNaval);
            if (potentialAlly != null) {
                sideA.add(potentialAlly);
                participants.remove(potentialAlly);
                changeFoundA = true;
            }
        }

        // If yes, check if there is an enemy of the owner [B] (WAR relation) present.
        final Nation potentialOpponent = pickOpponent(sector.getPosition().getRegion(), sideA, sideB, participants, isNaval);

        if (potentialOpponent != null) {
            sideB.add(potentialOpponent);
            participants.remove(potentialOpponent);
            changeFoundB = true;

        } else if (sideB.isEmpty() && participants.size() <= 1) {
            // did not find any opponent and no other potential opponent exist
            // stop check.
            return;
        }

        // If side A was not a good choice for finding opponents
        if (participants.size() >= 2
                && sideB.isEmpty()
                && changeFoundA && !changeFoundB) {
            // try with a new sideA
            sideA.clear();

            // Try again
            pickSides(sector, participants, sideA, sideB, isNaval);

        } else if (!participants.isEmpty() && (changeFoundA || changeFoundB)) {
            // recursive call
            // Try again
            pickSides(sector, participants, sideA, sideB, isNaval);
        }
    }

    /**
     * Search participants list for a nation that is at war with the side.
     *
     * @param region       the region the battle takes place.
     * @param otherSide    the nation leading the other side.
     * @param participants the owners of armies in the sector.
     * @param isNaval      if this is a sea or land battle.
     * @return an opponent nation.
     */
    private Nation pickOpponent(final Region region,
                                final List<Nation> otherSide,
                                final List<Nation> thisSide,
                                final List<Nation> participants,
                                final boolean isNaval) {
        for (final Nation nation : participants) {
            // Check if at war with other side
            if (checkOpponent(region, otherSide.get(0), nation, isNaval)) {
                boolean validOpponent = true;

                // In case the other side comprises with more than 1 nations, also check other nations as well
                for (Nation otherNation : otherSide) {
                    validOpponent &= checkOpponent(region, otherNation, nation, isNaval);
                }

                if (validOpponent) {
                    // make sure that it is also allie of other side
                    for (Nation thisNation : thisSide) {
                        validOpponent &= checkAlly(thisNation, nation);
                    }

                    if (validOpponent) {
                        return nation;
                    }
                }
            }
        }

        return null;
    }

    private boolean checkOpponent(final Region region, final Nation sideA, final Nation sideB, final boolean isNaval) {
        if (isNaval) {
            return checkOpponentSea(region, sideA, sideB);

        } else {
            return checkOpponentLand(region, sideA, sideB);
        }
    }

    private boolean checkOpponentLand(final Region region, final Nation sideA, final Nation sideB) {
        final NationsRelation relationAtoB = getByNations(game, sideA, sideB);
        final NationsRelation relationBtoA = getByNations(game, sideB, sideA);

        return (relationAtoB.getRelation() == REL_WAR)
                || (region.getId() != EUROPE && relationAtoB.getRelation() == REL_COLONIAL_WAR)
                || (relationBtoA.getRelation() == REL_WAR)
                || (region.getId() != EUROPE && relationBtoA.getRelation() == REL_COLONIAL_WAR);
    }

    private boolean checkOpponentSea(final Region region, final Nation sideA, final Nation sideB) {
        final NationsRelation relationAtoB = getByNations(game, sideA, sideB);
        final NationsRelation relationBtoA = getByNations(game, sideB, sideA);

        return (relationAtoB.getRelation() >= REL_COLONIAL_WAR)
                || (relationBtoA.getRelation() >= REL_COLONIAL_WAR);
    }

    /**
     * Search participants list for a nation that is at alliance with the side.
     *
     * @param thisSide     the nation leading the other side.
     * @param participants the owners of armies in the sector.
     * @return an allied nation.
     */
    private Nation pickAlly(final Region region,
                            final List<Nation> thisSide,
                            final List<Nation> otherSide,
                            final List<Nation> participants,
                            final boolean isNaval) {
        for (final Nation nation : participants) {
            // check if allies
            if (checkAlly(thisSide.get(0), nation)) {
                boolean validAlly = true;

                // In case the other side comprises with more than 1 nations, also check other nations as well
                for (Nation otherNation : thisSide) {
                    validAlly &= checkAlly(otherNation, nation);
                }

                if (validAlly) {
                    // Check if at war with other side
                    for (Nation thatNation : otherSide) {
                        validAlly &= checkOpponent(region, thatNation, nation, isNaval);
                    }

                    if (validAlly) {
                        return nation;
                    }
                }
            }
        }

        return null;
    }

    private boolean checkAlly(final Nation sideA, final Nation sideB) {
        final NationsRelation relationAtoB = getByNations(game, sideA, sideB);
        final NationsRelation relationBtoA = getByNations(game, sideA, sideB);

        return (relationAtoB.getRelation() == REL_ALLIANCE)
                && (relationBtoA.getRelation() == REL_ALLIANCE);
    }

    /**
     * Investigate land sectors for possible battles.
     *
     * @return list of battles initiated.
     */
    protected List<BattleField> searchSectors() {
        final List<BattleField> battleFields = new ArrayList<BattleField>();

        // Retrieve all sectors with more than 1 owner
        final List<Sector> sectorList = BrigadeManager.getInstance().listMultiOwners(game);

        // Iterate sectors one by one
        for (final Sector sector : sectorList) {
            // Ignore sea sectors
            if (sector.getTerrain().getId() == TERRAIN_O) {
                continue;
            }

            // Identify owners in this sector
            final List<Nation> owners = BrigadeManager.getInstance().listOwners(sector.getPosition());
            final List<Nation> validOwners = new ArrayList<Nation>();

            if (owners.size() > 1) {

                // Ignore off-map trade cities
                if (sector.getPosition().getRegion().getId() == EUROPE &&
                        ((sector.getPosition().getX() == 0 && sector.getPosition().getY() == 2)
                                || (sector.getPosition().getX() == 0 && sector.getPosition().getY() == 26))) {

                    LOGGER.debug("Battles not allowed in off-map trade cities");

                } else {

                    int totOwners = 0;
                    final StringBuilder strBuilder = new StringBuilder();
                    for (final Nation nation : owners) {
                        boolean foundBattalions = false;
                        final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByPositionNation(sector.getPosition(), nation);
                        for (final Brigade brigade : lstBrigades) {
                            for (final Battalion battalion : brigade.getBattalions()) {
                                // do not count overrun battalions
                                if (battalion.getHeadcount() > 0) {
                                    foundBattalions = true;
                                    break;
                                }
                            }
                        }

                        if (foundBattalions) {
                            totOwners++;
                            validOwners.add(nation);

                            strBuilder.append(" ");
                            strBuilder.append(nation.getCode());
                        }
                    }

                    // considering only standing forces
                    if (totOwners > 1) {
                        LOGGER.debug(owners.size() + " nations [" + strBuilder.substring(1) + "] gathered at " + sector.getPosition().toString());
                        battleFields.addAll(searchSector(sector, validOwners));
                    }
                }
            }
        }

        return battleFields;
    }

    /**
     * Investigate sea sectors for possible battles.
     *
     * @return list of battles initiated.
     */
    protected List<BattleField> searchSeaSectors() {
        final List<BattleField> battleFields = new ArrayList<BattleField>();

        // Retrieve all sectors with more than 1 owner
        final List<Sector> sectorList = ShipManager.getInstance().listMultiOwners(game);

        // Iterate sectors one by one
        for (final Sector sector : sectorList) {
            // Ignore land sectors
            if (sector.getTerrain().getId() != TERRAIN_O) {
                continue;
            }

            // Identify owners in this sector
            final List<Nation> owners = ShipManager.getInstance().listOwners(sector.getPosition());
            final List<Nation> validOwners = new ArrayList<Nation>();

            if (owners.size() > 1) {
                int totOwners = 0;
                final StringBuilder strBuilder = new StringBuilder();
                for (final Nation nation : owners) {
                    boolean foundShip = false;
                    final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(sector.getPosition(), nation);
                    for (final Ship ship : lstShips) {
                        // do not count overrun ships
                        // or merchant ships
                        if (ship.getCondition() > 0
                                && (ship.getType().getShipClass() > 0 || ship.getType().getIntId() == 31)) {
                            foundShip = true;
                            break;
                        }
                    }

                    if (foundShip) {
                        totOwners++;
                        validOwners.add(nation);

                        strBuilder.append(" ");
                        strBuilder.append(nation.getCode());
                    }
                }

                // considering only standing forces
                if (totOwners > 1) {
                    LOGGER.debug(owners.size() + " nations [" + strBuilder.substring(1) + "] gathered at " + sector.getPosition().toString());
                    battleFields.addAll(searchSeaSector(sector, validOwners));
                }
            }
        }

        return battleFields;
    }

    /**
     * Do battles.
     */
    public void process() {
        final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());

        // remove has lost counters from previous turn (movement is complete)
        BattalionManager.getInstance().removeHasLostFlag(getGame());

        // Search for sectors where a battle has to take place
        final List<BattleField> battleFields = searchSectors();

        // Iterate through sectors and execute battle
        for (final BattleField field : battleFields) {
            // Tactical Battle
            LOGGER.debug("Resolving Tactical Battle at " + field.getField().getPosition().toString());
            conductBattleTactical(field);
        }

        // Search for sectors where a naval battle has to take place
        final List<BattleField> seaFields = searchSeaSectors();
        seaFields.addAll(searchSeaPatrols());

        // Iterate through sectors and execute battle
        for (final BattleField field : seaFields) {
            // Naval Battle
            if (field.getPatrol()) {
                LOGGER.debug("Resolving Naval Patrol Battle at " + field.getField().getPosition().toString());
                conductPatrol(field);

            } else {
                LOGGER.debug("Resolving Naval Battle at " + field.getField().getPosition().toString());
                conductBattleNaval(field);
            }
        }

        trans.commit();
        LOGGER.info("WarfareProcessor completed.");
    }

    /**
     * Invoke the tactical battle processor.
     *
     * @param field the BattleField to execute.
     */
    private void conductBattleTactical(final BattleField field) {
        // Prepare the battalions of each side
        final List<Battalion> side1 = prepareBattalionList(field.getField().getPosition(), field.getSide(0));
        final List<Battalion> side2 = prepareBattalionList(field.getField().getPosition(), field.getSide(1));

        // Convert Lists to Sets
        final Set<Nation> side1set = new HashSet<Nation>();
        for (Nation nation : field.getSide(0)) {
            side1set.add(nation);
        }

        final Set<Nation> side2set = new HashSet<Nation>();
        for (Nation nation : field.getSide(1)) {
            side2set.add(nation);
        }

        // Locate the commanders of each side
        final Commander comm1 = prepareCommander(field.getField().getPosition(), field.getSide(0));
        final Commander comm2 = prepareCommander(field.getField().getPosition(), field.getSide(1));

        // Identify if the defender has a fort
        int thisFort = 0;
        String fort = "-";

        if ((field.getField().getProductionSite() != null)
                && (field.getField().getProductionSite().getId() > PS_BARRACKS)) {

            // It is not possible for defenders in a fortress to re-engage enemy outside a fortress,
            // but still fight behind the walls.
            boolean hasEngaged = false;
            for (final Battalion battalion : side1) {
                if (battalion.getHasEngagedBattle()) {
                    hasEngaged = true;
                    break;
                }
            }

            // If a movement order of the defended initiated this battle,
            // then the defenders will leave the fortress and fight outside.
            if (!hasEngaged) {
                thisFort = field.getField().getProductionSite().getId() - PS_BARRACKS;
                fort = field.getField().getProductionSite().getName();

                // If this is a huge fortress, make sure that it has been built properly
                if (field.getField().getProductionSite().getId() == PS_BARRACKS_FH && field.getField().getBuildProgress() > 0) {
                    thisFort = PS_BARRACKS_FL - PS_BARRACKS;
                    fort = field.getField().getProductionSite().getName() + " (under construction)";
                }
            } else {
                fort = "Defender moved outside";
            }
        }

        // setup battle processor
        final TacticalBattleProcessor tbp = new TacticalBattleProcessor(field.getField(), thisFort, side1, side2, comm1, comm2);

        // Compute total battalions per side involved
        final int sideTotBattalions[] = new int[2];
        final double sideTotCP[] = new double[2];
        sideTotBattalions[RoundStatistics.SIDE_A] = side1.size();
        sideTotBattalions[RoundStatistics.SIDE_B] = side2.size();
        sideTotCP[RoundStatistics.SIDE_A] = 0d;
        sideTotCP[RoundStatistics.SIDE_B] = 0d;

        // Compute total battalions per nation involved
        final HashMap<Nation, Integer> nationBattalions = new HashMap<Nation, Integer>();
        final HashMap<Nation, Double> nationCombatPoints = new HashMap<Nation, Double>();
        final HashMap<Nation, Double> nationContribution = new HashMap<Nation, Double>();
        final HashMap<Nation, Integer> nationSide = new HashMap<Nation, Integer>();
        final HashMap<Nation, Integer> nationInitSize = new HashMap<Nation, Integer>();

        // In order to compute the battle points we create a long range and hand-to-hand round processors
        final ArtilleryLongRangeCombat lrc = new ArtilleryLongRangeCombat(tbp);
        final TroopHandToHandCombat hhc = new TroopHandToHandCombat(tbp);

        // Compute statistics for side 1
        for (final Battalion battalion : side1) {
            int totBatts = 0;
            double totCP = 0;
            int totSize = 0;

            if (nationBattalions.containsKey(battalion.getBrigade().getNation())) {
                totBatts = nationBattalions.get(battalion.getBrigade().getNation());
                totCP = nationCombatPoints.get(battalion.getBrigade().getNation());
                totSize = nationInitSize.get(battalion.getBrigade().getNation());

            } else {
                nationSide.put(battalion.getBrigade().getNation(), RoundStatistics.SIDE_A);
            }

            // calculate battalion's CP once
            final double battalionCP = lrc.calcCombatPoints(battalion) + hhc.calcCombatPoints(battalion);

            nationBattalions.put(battalion.getBrigade().getNation(), totBatts + 1);
            nationCombatPoints.put(battalion.getBrigade().getNation(), totCP + battalionCP);
            sideTotCP[RoundStatistics.SIDE_A] += battalionCP;
            nationInitSize.put(battalion.getBrigade().getNation(), totSize + battalion.getHeadcount());
        }

        // Compute statistics for side 2
        for (final Battalion battalion : side2) {
            int totBatts = 0;
            double totCP = 0;
            int totSize = 0;

            if (nationBattalions.containsKey(battalion.getBrigade().getNation())) {
                totBatts = nationBattalions.get(battalion.getBrigade().getNation());
                totCP = nationCombatPoints.get(battalion.getBrigade().getNation());
                totSize = nationInitSize.get(battalion.getBrigade().getNation());

            } else {
                nationSide.put(battalion.getBrigade().getNation(), RoundStatistics.SIDE_B);
            }

            // calculate battalion's CP once
            final double battalionCP = lrc.calcCombatPoints(battalion) + hhc.calcCombatPoints(battalion);

            nationBattalions.put(battalion.getBrigade().getNation(), totBatts + 1);
            nationCombatPoints.put(battalion.getBrigade().getNation(), totCP + battalionCP);
            sideTotCP[RoundStatistics.SIDE_B] += battalionCP;
            nationInitSize.put(battalion.getBrigade().getNation(), totSize + battalion.getHeadcount());
        }

        // Compute contribution of each side to the casualties inflicted/received based on combat points
        for (Map.Entry<Nation, Double> participant : nationCombatPoints.entrySet()) {
            // Acquire total CP for this side
            final double totCP = sideTotCP[nationSide.get(participant.getKey())];
            nationContribution.put(participant.getKey(), participant.getValue() / totCP);

            LOGGER.info("Contribution of " + participant.getKey().getCode() + " <CP=" + participant.getValue() + ", sideCP=" + totCP + "] " + (100d * participant.getValue() / totCP) + "%");
        }

        // Execute processor
        final List<RoundStatistics> result = tbp.process();

        // Store results
        final TacticalBattleReport tbr = new TacticalBattleReport();
        tbr.setPosition(field.getField().getPosition());
        tbr.setFort(fort);
        tbr.setTurn(field.getField().getPosition().getGame().getTurn());
        tbr.setSide1(side1set);
        tbr.setSide2(side2set);
        tbr.setComm1(comm1);
        tbr.setComm2(comm2);
        tbr.setWinner(tbp.getWinner());

        // Save analytical results
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(result);
            os.close();
            zos.close();
            baos.close();

            tbr.setStats(baos.toByteArray());
        } catch (Exception ex) {
            LOGGER.fatal(ex);
        }

        // Store battle result
        TacticalBattleReportManager.getInstance().add(tbr);

        // Compute ending headcount per nation involved
        final HashMap<Nation, Integer> nationFinalSize = new HashMap<Nation, Integer>();

        // Persist changes for Side 1 battalions
        for (final Battalion battalion : side1) {
            int totSize = 0;

            if (nationFinalSize.containsKey(battalion.getBrigade().getNation())) {
                totSize = nationFinalSize.get(battalion.getBrigade().getNation());
            }

            nationFinalSize.put(battalion.getBrigade().getNation(), totSize + battalion.getHeadcount());

            BattalionManager.getInstance().update(battalion);
        }

        // Persist changes for Side 2 battalions
        for (final Battalion battalion : side2) {
            int totSize = 0;

            if (nationFinalSize.containsKey(battalion.getBrigade().getNation())) {
                totSize = nationFinalSize.get(battalion.getBrigade().getNation());
            }

            nationFinalSize.put(battalion.getBrigade().getNation(), totSize + battalion.getHeadcount());

            BattalionManager.getInstance().update(battalion);
        }

        // Persist changes for Side 1 commander
        if (comm1 != null && comm1.getId() > 0) {
            CommanderManager.getInstance().update(comm1);
        }

        // Persist changes for Side 2 commander
        if (comm2 != null && comm2.getId() > 0) {
            CommanderManager.getInstance().update(comm2);
        }

        // Report casualties inflicted and received
        for (final RoundStatistics roundStatistics : result) {
            if (roundStatistics.getRound() == AbstractTacticalBattleRound.ROUND_WINNER) {
                for (final Nation nation : side1set) {
                    int casInflictedThisTurn = retrieveReportAsInt(nation, getGame().getTurn(), A_TOT_KILLS);
                    final int casInflicted = retrieveReportRunningSum(nation, A_TOT_KILLS);
                    final double contributedKills = nationContribution.get(nation) * roundStatistics.getSideStat()[1][1];
                    final double contributedLoses = nationInitSize.get(nation) - nationFinalSize.get(nation);

                    LOGGER.info("Contribution of " + nation.getCode() + " [" + nationContribution.get(nation) * 100 + "%] kills=" + contributedKills + " / loses=" + contributedLoses);

                    /** @todo temporarily disabled
                    // Kill 300,000 enemy troops in battles
                    if (casInflicted < 300000 && casInflicted + contributedKills > 300000) {
                    parent.changeVP(getGame(), nation, KILL_300, "Kill 300,000 enemy troops in battles");

                    news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                    "Our capable commanders and our land forces have killed more than 300,000 enemy troops!");
                    }

                    // Kill 700,000 enemy troops in battles
                    if (casInflicted < 700000 && casInflicted + contributedKills > 700000) {
                    parent.changeVP(getGame(), nation, KILL_700, "Kill 700,000 enemy troops in battles");

                    news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                    "Our capable commanders and our land forces have killed more than 700,000 enemy troops!");
                    }

                    // Kill 1,500,000 enemy troops in battles
                    if (casInflicted < 1500000 && casInflicted + contributedKills > 1500000) {
                    parent.changeVP(getGame(), nation, KILL_1500, "Kill 1,500,000 enemy troops in battles");

                    news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                    "Our capable commanders and our land forces have killed more than 1,500,000 enemy troops!");
                    }
                     */

                    casInflictedThisTurn += contributedKills;
                    report(nation, A_TOT_KILLS, casInflictedThisTurn);

                    // change player's profile
                    parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_KILLED_ALL, (int) roundStatistics.getSideStat()[1][1]);

                    // check if achievements have been reached
                    parent.achievementsTroopsKilled(getGame(), nation);

                    int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), A_TOT_DEATHS);
                    casReceived += contributedLoses;
                    report(nation, A_TOT_DEATHS, casReceived);
                }

                for (final Nation nation : side2set) {
                    int casInflictedThisTurn = retrieveReportAsInt(nation, getGame().getTurn(), A_TOT_KILLS);
                    final int casInflicted = retrieveReportRunningSum(nation, A_TOT_KILLS);
                    final double contributedKills = nationContribution.get(nation) * roundStatistics.getSideStat()[0][1];
                    final double contributedLoses = nationInitSize.get(nation) - nationFinalSize.get(nation);

                    LOGGER.info("Contribution of " + nation.getCode() + " [" + nationContribution.get(nation) * 100 + "%] kills=" + contributedKills + " / loses=" + contributedLoses);

                    /** @todo temporarily disabled
                    // Kill 300,000 enemy troops in battles
                    if (casInflicted < 300000 && casInflicted + contributedKills > 300000) {
                    parent.changeVP(getGame(), nation, KILL_300, "Kill 300,000 enemy troops in battles");

                    news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                    "Our capable commanders and our land forces have killed more than 300,000 enemy troops!");
                    }

                    // Kill 700,000 enemy troops in battles
                    if (casInflicted < 700000 && casInflicted + contributedKills > 700000) {
                    parent.changeVP(getGame(), nation, KILL_700, "Kill 700,000 enemy troops in battles");

                    news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                    "Our capable commanders and our land forces have killed more than 700,000 enemy troops!");
                    }

                    // Kill 1,500,000 enemy troops in battles
                    if (casInflicted < 1500000 && casInflicted + contributedKills > 1500000) {
                    parent.changeVP(getGame(), nation, KILL_1500, "Kill 1,500,000 enemy troops in battles");

                    news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                    "Our capable commanders and our land forces have killed more than 1,500,000 enemy troops!");
                    }
                     */

                    casInflictedThisTurn += contributedKills;
                    report(nation, A_TOT_KILLS, casInflictedThisTurn);

                    // change player's profile
                    parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_KILLED_ALL, (int) roundStatistics.getSideStat()[0][1]);

                    // check if achievements have been reached
                    parent.achievementsTroopsKilled(getGame(), nation);

                    int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), A_TOT_DEATHS);
                    casReceived += contributedLoses;
                    report(nation, A_TOT_DEATHS, casReceived);
                }

            }
        }

        boolean isOwnerInvolved = false;
        boolean isOwnerAllyInvolvedSideB = false;

        // check if owner is in Side 1
        for (final Nation nation : side1set) {
            if (nation.getId() == field.getField().getNation().getId()) {
                isOwnerInvolved = true;
                break;
            } else {
                // check relations
                if (relationsMap.containsKey(nation)
                        && relationsMap.get(nation).containsKey(field.getField().getNation())
                        && relationsMap.get(nation).get(field.getField().getNation()).getRelation() == REL_ALLIANCE) {
                    isOwnerInvolved = true;
                    break;
                }
            }
        }

        if (!isOwnerInvolved) {
            // check if he is in side 2
            for (final Nation nation : side2set) {
                if (nation.getId() == field.getField().getNation().getId()) {
                    isOwnerAllyInvolvedSideB = true;
                    break;
                } else {
                    // check relations
                    if (relationsMap.containsKey(nation)
                            && relationsMap.get(nation).containsKey(field.getField().getNation())
                            && relationsMap.get(nation).get(field.getField().getNation()).getRelation() == REL_ALLIANCE) {
                        isOwnerAllyInvolvedSideB = true;
                        break;
                    }
                }
            }
        }


        // Check result of battle, hand-over sector and send news
        switch (tbp.getWinner()) {
            case RoundStatistics.SIDE_A: {

                if (isOwnerInvolved) {
                    LOGGER.debug("Sector " + field.getField().getPosition() + " owned by " + field.getField().getNation().getName() + " was defended successfully");
                    field.getField().setTempNation(field.getField().getNation());
                    SectorManager.getInstance().update(field.getField());
                    parent.getConqueredSectors().remove(field.getField());

                } else {
                    // Identify nation that will win the sector
                    Nation newOwner = null;

                    // choose the nation whose commander led the battle
                    if ((comm1 != null)
                            && (comm1.getId() > 0)) {
                        newOwner = comm1.getNation();
                    }

                    // Choose a nation randomly
                    if (newOwner == null) {
                        for (final Nation nation : side1set) {
                            final int roll = getRandomGen().nextInt(100) + 1;
                            if (roll < (100 * nationBattalions.get(nation) / sideTotBattalions[RoundStatistics.SIDE_A])) {
                                newOwner = nation;
                                break;
                            }
                        }
                    }

                    // In a very rare case where no-one was found...
                    if (newOwner == null) {
                        newOwner = side1set.iterator().next();
                    }

                    // Make sure that no other battle that involved the owner of the sector took place
                    LOGGER.debug("Sector " + field.getField().getPosition() + " owned by " + field.getField().getNation().getName() + " was conquered by " + newOwner.getName());
                    field.getField().setTempNation(newOwner);
                    SectorManager.getInstance().update(field.getField());
                    parent.getConqueredSectors().add(field.getField());
                }

                // Send News
                int newsId = 0;
                for (final Nation nation : side1set) {
                    final int thisNewsId = news(nation, field.getSide(RoundStatistics.SIDE_A).get(0),
                            NewsConstants.NEWS_MILITARY, newsId,
                            "We defeated the land forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at the tactical battle that took place at " + field.getField().getPosition().toString());
                    if (newsId == 0) {
                        newsId = thisNewsId;
                    }
                }

                for (final Nation nation : side2set) {
                    news(nation, field.getSide(RoundStatistics.SIDE_B).get(0),
                            NewsConstants.NEWS_MILITARY, newsId,
                            "Our forces were defeated by the land forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at the tactical battle that took place at " + field.getField().getPosition().toString());
                }

                break;
            }

            case RoundStatistics.SIDE_B: {

                if (isOwnerAllyInvolvedSideB) {
                    LOGGER.debug("Sector " + field.getField().getPosition() + " owned by " + field.getField().getNation().getName() + " was defended successfully");
                    field.getField().setTempNation(field.getField().getNation());
                    SectorManager.getInstance().update(field.getField());
                    parent.getConqueredSectors().remove(field.getField());

                } else {
                    // Identify nation that will win the sector
                    Nation newOwner = null;

                    // choose the nation whose commander led the battle
                    if ((comm2 != null)
                            && (comm2.getId() > 0)) {
                        newOwner = comm2.getNation();
                    }

                    // Choose a nation randomly
                    if (newOwner == null) {
                        newOwner = side2set.iterator().next();
                        for (final Nation nation : side2set) {
                            final int roll = getRandomGen().nextInt(100) + 1;
                            if (roll < (100 * nationBattalions.get(nation) / sideTotBattalions[RoundStatistics.SIDE_B])) {
                                newOwner = nation;
                                break;
                            }
                        }
                    }

                    LOGGER.debug("Sector " + field.getField().getPosition() + " owned by " + field.getField().getNation().getName() + " was conquered by " + newOwner.getName());
                    field.getField().setTempNation(newOwner);
                    SectorManager.getInstance().update(field.getField());
                    parent.getConqueredSectors().add(field.getField());
                }

                // Send News
                int newsId = 0;
                for (final Nation nation : side2set) {
                    final int thisNewsId = news(nation, field.getSide(RoundStatistics.SIDE_B).get(0),
                            NewsConstants.NEWS_MILITARY, newsId,
                            "We defeated the land forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at the tactical battle that took place at " + field.getField().getPosition().toString());
                    if (newsId == 0) {
                        newsId = thisNewsId;
                    }
                }

                for (final Nation nation : side1set) {
                    news(nation, field.getSide(RoundStatistics.SIDE_A).get(0),
                            NewsConstants.NEWS_MILITARY, newsId,
                            "Our forces were defeated by the land forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at the tactical battle that took place at " + field.getField().getPosition().toString());
                }

                break;
            }

            default:

                if (isOwnerInvolved) {
                    LOGGER.debug("Sector " + field.getField().getPosition() + " owned by " + field.getField().getNation().getName() + " was defended successfully");
                    field.getField().setTempNation(field.getField().getNation());
                    SectorManager.getInstance().update(field.getField());
                    parent.getConqueredSectors().remove(field.getField());
                }

                // Send News
                int newsId = 0;
                for (final Nation nation : side1set) {
                    final int thisNewsId = news(nation, nation,
                            NewsConstants.NEWS_MILITARY, newsId,
                            "Our forces battled against the land forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at " + field.getField().getPosition().toString() + ". The tactical battle was indecisive.");

                    if (newsId == 0) {
                        newsId = thisNewsId;
                    }
                }

                for (final Nation nation : side2set) {
                    news(nation, nation,
                            NewsConstants.NEWS_MILITARY, newsId,
                            "Our forces battled against the land forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at " + field.getField().getPosition().toString() + ". The tactical battle was indecisive.");
                }
        }
    }

    /**
     * Locate all the battalions for the given list of nations that are located at given x/y/region.
     *
     * @param position the position to look.
     * @param side     the list of nations.
     * @return a list of battalions.
     */
    private List<Battalion> prepareBattalionList(final Position position, final List<Nation> side) {
        final List<Battalion> battalions = new ArrayList<Battalion>();
        // Iterate through nations
        for (final Nation nation : side) {
            // Locate battalions at this sector
            final List<Brigade> brigadeList = BrigadeManager.getInstance().listByPositionNation(position, nation);

            // Go through all the brigades
            for (final Brigade brigade : brigadeList) {

                // Iterate through all the battalions of the brigade
                for (final Battalion battalion : brigade.getBattalions()) {
                    // do not consider overrun battalions
                    if (battalion.getHeadcount() > 0) {
                        battalion.setUnloaded(false);

                        // Check if this battalion is loaded
                        if (battalion.getCarrierInfo() != null
                                && battalion.getCarrierInfo().getCarrierType() != 0) {

                            switch (battalion.getCarrierInfo().getCarrierType()) {

                                case ArmyConstants.BAGGAGETRAIN:
                                    // unload unit from carrier
                                    final BaggageTrain thisTrain = BaggageTrainManager.getInstance().getByID(battalion.getCarrierInfo().getCarrierId());
                                    if (thisTrain != null) {
                                        final Map<Integer, Integer> storedGoods = thisTrain.getStoredGoods();
                                        int thisKey = 0;

                                        // Check if a unit is loaded in the carrier
                                        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                                if (entry.getKey() >= ArmyConstants.BRIGADE * 1000
                                                        && entry.getKey() < (ArmyConstants.BRIGADE + 1) * 1000
                                                        && entry.getValue() == battalion.getBrigade().getBrigadeId()) {
                                                    thisKey = entry.getKey();
                                                    break;
                                                }
                                            }
                                        }

                                        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == battalion.getBrigade().getBrigadeId()) {
                                            storedGoods.remove(thisKey);
                                        }

                                        BaggageTrainManager.getInstance().update(thisTrain);
                                    }
                                    break;

                                case ArmyConstants.SHIP:
                                default:
                                    // unload unit from carrier
                                    final Ship thisShip = ShipManager.getInstance().getByID(battalion.getCarrierInfo().getCarrierId());
                                    if (thisShip != null) {
                                        final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
                                        int thisKey = 0;

                                        // Check if a unit is loaded in the carrier
                                        for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                                            if (entry.getKey() > GoodConstants.GOOD_LAST) {
                                                if (entry.getKey() >= ArmyConstants.BRIGADE * 1000
                                                        && entry.getKey() < (ArmyConstants.BRIGADE + 1) * 1000
                                                        && entry.getValue() == battalion.getBrigade().getBrigadeId()) {
                                                    thisKey = entry.getKey();
                                                    break;
                                                }
                                            }
                                        }

                                        if (storedGoods.containsKey(thisKey) && storedGoods.get(thisKey) == battalion.getBrigade().getBrigadeId()) {
                                            storedGoods.remove(thisKey);
                                        }

                                        ShipManager.getInstance().update(thisShip);
                                    }
                                    break;
                            }

                            // Unload from transport
                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(0);
                            thisCarrying.setCarrierId(0);
                            battalion.setCarrierInfo(thisCarrying);
                            BattalionManager.getInstance().update(battalion);

                            // Battalion gets a penalty
                            battalion.setUnloaded(true);
                        }

                        battalions.add(battalion);
                    }
                }
            }
        }

        return battalions;
    }

    /**
     * Identify the side with the most battalions and select the commander to lead the battle.
     *
     * @param position the position to look.
     * @param side     the list of nations.
     * @return a Commander.
     */
    private Commander prepareCommander(final Position position, final List<Nation> side) {
        final List<Nation> nationList = new ArrayList<Nation>();
        final Map<Nation, Commander> commMap = new HashMap<Nation, Commander>();
        // Iterate through nations
        for (final Nation nation : side) {
            // Locate battalions at this sector
            final List<Brigade> brigadeList = BrigadeManager.getInstance().listByPositionNation(position, nation);

            final List<Commander> commList = CommanderManager.getInstance().listByPositionNation(position, nation);
            final List<Commander> aliveCommanders = new ArrayList<Commander>();
            // Make sure dead commanders and captured commanders are not in the list
            for (final Commander commander : commList) {
                if (!commander.getDead()
                        && !commander.getPool()
                        && !commander.getInTransit()
                        && commander.getCaptured().getId() == commander.getNation().getId()) {
                    aliveCommanders.add(commander);
                }
            }

            if (!aliveCommanders.isEmpty()) {
                // Pick highest ranking commander
                Commander leadCommander = aliveCommanders.get(0);
                for (final Commander commander : aliveCommanders) {
                    if (commander.getRank().getRankId() >= leadCommander.getRank().getRankId()
                            && commander.getComc() > leadCommander.getComc()) {
                        leadCommander = commander;
                    }
                }
                commMap.put(nation, leadCommander);

                // Go through all the brigades
                int batCount = 0;
                for (final Brigade brigade : brigadeList) {

                    // Iterate through all the battalions of the brigade
                    for (final Battalion battalion : brigade.getBattalions()) {
                        if (battalion.getHeadcount() > 400) {
                            batCount++;
                        }
                    }
                }
                nation.setSort(batCount);
                nationList.add(nation);
            }
        }

        final Commander thisCommander;
        if (nationList.isEmpty()) {
            thisCommander = CommanderManager.getInstance().getByID(-1);

        } else {

            // Sort nations based on number of battalions
            java.util.Collections.sort(nationList, new NationSort());

            // Pick the commander from the first nation
            thisCommander = commMap.get(nationList.get(0));
        }

        return thisCommander;
    }

    /**
     * Invoke the naval battle processor.
     *
     * @param field the BattleField to execute.
     */
    private void conductBattleNaval(final BattleField field) {
        // Identify if the weather of the battle
        int weather;
        if (field.getField().getStorm() > 0) {
            weather = NavalBattleProcessor.WEATHER_STORM;
        } else {
            weather = weatherSelector.rollWeather(field.getField().getPosition());
        }

        // Prepare the ships of each side
        final List<Ship> side1 = prepareShipList(field.getField().getPosition(), field.getSide(0));
        final List<Ship> side2 = prepareShipList(field.getField().getPosition(), field.getSide(1));

        // Count number of fleets involved in the battle (side 1)
        final Set<Integer> fleetSet1 = new HashSet<Integer>();
        for (final Ship ship : side1) {
            // Only consider ships in fleet
            if (ship.getFleet() > 0) {
                fleetSet1.add(ship.getFleet());
            }
        }
        final int cntFleetsSide1 = fleetSet1.size();

        // Count number of fleets involved in the battle (side 2)
        final Set<Integer> fleetSet2 = new HashSet<Integer>();
        for (final Ship ship : side2) {
            // Only consider ships in fleet
            if (ship.getFleet() > 0) {
                fleetSet2.add(ship.getFleet());
            }
        }
        final int cntFleetsSide2 = fleetSet2.size();

        // Determine if this is a large naval battle
        final boolean isLarge = (cntFleetsSide1 + cntFleetsSide2 > 5);

        // Execute naval processor
        final NavalBattleProcessor nbp = new NavalBattleProcessor(weather, side1, side2, false);
        final List<RoundStat> result = nbp.process();

        // Store results
        final NavalBattleReport nbr = new NavalBattleReport();
        nbr.setPosition(field.getField().getPosition());
        nbr.setTurn(field.getField().getPosition().getGame().getTurn());
        nbr.setWeather(weather);
        nbr.setPiracy(false);

        // Identify involved Nations
        final Set<Nation> side1set = new HashSet<Nation>();
        for (Nation side : field.getSide(0)) {
            side1set.add(side);
        }
        nbr.setSide1(side1set);

        // Identify involved Nations
        final Set<Nation> side2set = new HashSet<Nation>();
        for (Nation side : field.getSide(1)) {
            side2set.add(side);
        }
        nbr.setSide2(side2set);

        // Save winner
        if (result.get(AbstractNavalBattleRound.ROUND_DET).getResult() == RoundStat.SIDE_A) {
            nbr.setWinner(RoundStat.SIDE_A);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, field.getSide(RoundStatistics.SIDE_A).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "We defeated the naval forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at the naval battle that took place at " + field.getField().getPosition().toString());
                if (newsId == 0) {
                    newsId = thisNewsId;
                }
            }

            for (final Nation nation : side2set) {
                news(nation, field.getSide(RoundStatistics.SIDE_B).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our forces were defeated by the naval forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at the battle that took place at " + field.getField().getPosition().toString());
            }

        } else if (result.get(AbstractNavalBattleRound.ROUND_DET).getResult() == RoundStat.SIDE_B) {
            nbr.setWinner(RoundStat.SIDE_B);

            // Send News
            int newsId = 0;
            for (final Nation nation : side2set) {
                final int thisNewsId = news(nation, field.getSide(RoundStatistics.SIDE_B).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "We defeated the naval forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at the naval battle that took place at " + field.getField().getPosition().toString());
                if (newsId == 0) {
                    newsId = thisNewsId;
                }
            }

            for (final Nation nation : side1set) {
                news(nation, field.getSide(RoundStatistics.SIDE_A).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our forces were defeated by the naval forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at the naval battle that took place at " + field.getField().getPosition().toString());
            }

        } else {
            nbr.setWinner(0);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, nation,
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our forces battled against the naval forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at " + field.getField().getPosition().toString() + ". The naval battle was indecisive.");

                if (newsId == 0) {
                    newsId = thisNewsId;
                }
            }

            for (final Nation nation : side2set) {
                news(nation, nation,
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our forces battled against the naval forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at " + field.getField().getPosition().toString() + ". The naval battle was indecisive.");
            }
        }

        // Enable piracy bit
        nbr.setPiracy(field.getPiracy());

        // Save analytical results
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(result);
            os.close();
            zos.close();
            baos.close();

            nbr.setStats(baos.toByteArray());
        } catch (Exception ex) {
            LOGGER.fatal(ex);
        }

        // Update records
        for (final Ship ship : side1) {
            // make sure participated ships do not jump-off
            ship.setHasMoved(false);

            // East Indiamen should fight as class 2 ships
            if (ship.getType().getTypeId() == 13) {
                // return to normal class
                ship.getType().setShipClass(0);
            }

            ShipManager.getInstance().update(ship);
        }

        for (final Ship ship : side2) {
            // make sure participated ships do not jump-off
            ship.setHasMoved(false);

            // East Indiamen should fight as class 2 ships
            if (ship.getType().getTypeId() == 13) {
                // return to normal class
                ship.getType().setShipClass(0);
            }

            ShipManager.getInstance().update(ship);
        }

        NavalBattleReportManager.getInstance().add(nbr);

        // Report casualties inflicted and received
        int side1loses = 0;
        int side2loses = 0;
        for (final RoundStat roundStatistics : result) {
            if (roundStatistics.getRound() == AbstractNavalBattleRound.ROUND_FINAL) {

                // Retrieve Captured ships.
                if (roundStatistics.getSideStat()[0].size() > 7) {
                    // Side 1 captured ships of side 2 -- these are loses for side 2
                    side2loses += roundStatistics.getSideStat()[0].get(7).getShips();
                }

                if (roundStatistics.getSideStat()[1].size() > 7) {
                    // Side 2 captured ships of side 1 -- these are loses for side 1
                    side1loses += roundStatistics.getSideStat()[1].get(7).getShips();
                }

                // Retrieve Lost ships.
                if (roundStatistics.getSideStat()[0].size() > 8) {
                    side1loses += roundStatistics.getSideStat()[0].get(8).getShips();
                }

                if (roundStatistics.getSideStat()[1].size() > 8) {
                    side2loses += roundStatistics.getSideStat()[1].get(8).getShips();
                }

            }
        }

        // update player profiles
        parent.changeProfile(getGame(), side1set, ProfileConstants.BATTLES_NAVAL, 1);
        parent.changeProfile(getGame(), side2set, ProfileConstants.BATTLES_NAVAL, 1);

        for (final Nation nation : side1set) {
            int casInflictedThisTurn = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
            final int casInflicted = retrieveReportRunningSum(nation, S_SINKS);

            // Charge VPs
            if (isLarge) {
                final int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MAJOR_MAX);

                if (nbr.getWinner() == RoundStat.SIDE_A) {
                    parent.changeVP(getGame(), nation, NB_MAJOR_WIN * fleetsInvolved, "Won a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_WON, 1);

                    // check achievements
                    parent.achievementsNavalBattles(getGame(), nation);

                } else if (nbr.getWinner() == RoundStat.SIDE_B) {
                    parent.changeVP(getGame(), nation, NB_MAJOR_LOSE * fleetsInvolved, "Lost a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_LOST, 1);

                } else {
                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_DRAW, 1);
                }

            } else {
                final int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MINOR_MAX);

                if (nbr.getWinner() == RoundStat.SIDE_A) {
                    parent.changeVP(getGame(), nation, NB_MINOR_WIN * fleetsInvolved, "Won a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_WON, 1);

                    // check achievements
                    parent.achievementsNavalBattles(getGame(), nation);

                } else if (nbr.getWinner() == RoundStat.SIDE_B) {
                    parent.changeVP(getGame(), nation, NB_MINOR_LOSE * fleetsInvolved, "Lost a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_LOST, 1);

                } else {
                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_DRAW, 1);
                }
            }

            /** @todo temporarily disabled
            // Destroy/capture 30 enemy warships
            if (casInflicted < 30 && casInflicted + side2loses > 30) {
            parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

            news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
            "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");

            }

            // Destroy/capture 70 enemy warships
            if (casInflicted < 70 && casInflicted + side2loses > 70) {
            parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

            news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
            "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
            }

            // Destroy/capture 150 enemy warships
            if (casInflicted < 150 && casInflicted + side2loses > 150) {
            parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

            news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
            "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
            }
             */

            casInflictedThisTurn += side2loses;
            report(nation, S_SINKS, casInflictedThisTurn);

            // change player's profile
            parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side2loses);

            // check achievements
            parent.achievementsEnemyShips(getGame(), nation);
            parent.achievementsEnemyMerchants(getGame(), nation);

            int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
            casReceived += side1loses;
            report(nation, S_SINKED, casReceived);
        }

        for (final Nation nation : side2set) {
            int casInflictedThisTurn = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
            final int casInflicted = retrieveReportRunningSum(nation, S_SINKS);

            // Charge VPs
            if (isLarge) {
                final int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MAJOR_MAX);

                if (nbr.getWinner() == RoundStat.SIDE_B) {
                    parent.changeVP(getGame(), nation, NB_MAJOR_WIN * fleetsInvolved, "Won a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                } else if (nbr.getWinner() == RoundStat.SIDE_A) {
                    parent.changeVP(getGame(), nation, NB_MAJOR_LOSE * fleetsInvolved, "Lost a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                } else {
                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_DRAW, 1);
                }

            } else {
                final int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MINOR_MAX);

                if (nbr.getWinner() == RoundStat.SIDE_B) {
                    parent.changeVP(getGame(), nation, NB_MINOR_WIN * fleetsInvolved, "Won a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_WON, 1);

                    // check achievements
                    parent.achievementsNavalBattles(getGame(), nation);

                } else if (nbr.getWinner() == RoundStat.SIDE_A) {
                    parent.changeVP(getGame(), nation, NB_MINOR_LOSE * fleetsInvolved, "Lost a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_LOST, 1);

                } else {
                    // update player profiles
                    parent.changeProfile(getGame(), nation, ProfileConstants.BATTLES_NAVAL_DRAW, 1);
                }

            }

            /** @todo temporarily disabled
            // Destroy/capture 30 enemy warships
            if (casInflicted < 30 && casInflicted + side1loses > 30) {
            parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

            news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
            "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");
            }

            // Destroy/capture 70 enemy warships
            if (casInflicted < 70 && casInflicted + side1loses > 70) {
            parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

            news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
            "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
            }

            // Destroy/capture 150 enemy warships
            if (casInflicted < 150 && casInflicted + side1loses > 150) {
            parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

            news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
            "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
            }
             */

            casInflictedThisTurn += side1loses;
            report(nation, S_SINKS, casInflictedThisTurn);

            // change player's profile
            parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side1loses);

            // check achievements
            parent.achievementsEnemyShips(getGame(), nation);
            parent.achievementsEnemyMerchants(getGame(), nation);

            int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
            casReceived += side2loses;
            report(nation, S_SINKED, casReceived);
        }
    }

    /**
     * Invoke the naval battle processor to process a piracy battle.
     *
     * @param sector the sector where the piracy will take place.
     * @param side1  the list of ships attacked by pirates.
     * @param side2  the pirate ships.
     */
    public void conductPiracy(final Sector sector, final List<Ship> side1, final List<Ship> side2) {
        // Identify if the weather of the battle
        int weather;
        if (sector.getStorm() > 0) {
            weather = NavalBattleProcessor.WEATHER_STORM;
        } else {
            weather = weatherSelector.rollWeather(sector.getPosition());
        }

        // Identify involved Nations
        final Set<Nation> side1set = new HashSet<Nation>();
        for (Ship ship : side1) {
            if (ship.getNation().getId() > 0) {
                side1set.add(ship.getNation());
            }
        }

        final Set<Nation> side2set = new HashSet<Nation>();
        side2set.add(NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL));

        final NavalBattleProcessor nbp = new NavalBattleProcessor(weather, side1, side2, true);
        final List<RoundStat> result = nbp.process();

        // Store results
        final NavalBattleReport nbr = new NavalBattleReport();
        nbr.setPosition(sector.getPosition());
        nbr.setTurn(sector.getPosition().getGame().getTurn());
        nbr.setWeather(weather);
        nbr.setPiracy(true);
        nbr.setPatrol(false);
        nbr.setSide1(side1set);
        nbr.setSide2(side2set);

        // Save winner
        if (result.get(7).getResult() == RoundStat.SIDE_A) {
            nbr.setWinner(RoundStat.SIDE_A);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "We defeated the pirates at the naval battle that took place at " + sector.getPosition().toString());
                if (newsId == 0) {
                    newsId = thisNewsId;
                }
            }

        } else if (result.get(7).getResult() == RoundStat.SIDE_B) {
            nbr.setWinner(RoundStat.SIDE_B);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our naval forces were defeated by pirates at the naval battle that took place at " + sector.getPosition().toString());
                if (newsId == 0) {
                    newsId = thisNewsId;
                }
            }

        } else {
            nbr.setWinner(0);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "We encountered pirates at " + sector.getPosition().toString() + ". The naval battle was indecisive.");
                if (newsId == 0) {
                    newsId = thisNewsId;
                }
            }

        }

        // Save analytical results
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(result);
            os.close();
            zos.close();
            baos.close();

            nbr.setStats(baos.toByteArray());
        } catch (Exception ex) {
            LOGGER.fatal(ex);
        }

        // Update records
        Nation side1nation = null;
        int fleetId = 0;
        Position position = null;
        for (final Ship ship : side1) {
            if (ship.getNation().getId() > 0) {
                side1nation = ship.getNation();
                fleetId = ship.getFleet();
                position = ship.getPosition();
                break;
            }
        }
        int count = 0;
        for (final Ship ship : side1) {
            try {
                if (ship.getNation().getId() == 0) {

                    if (ship.getShipId() != 0) {//just be sure the ship has an appropriate id before trying to delete it.
                        LOGGER.debug("deleting ship : " + ship.getShipId() + " - nation : " + ship.getNation().getId() + ", " + ship.getNation().getName());
                        //just check the ship exists in the database
                        final Ship dbShip = ShipManager.getInstance().getByID(ship.getShipId());
                        if (dbShip != null) {
                            ShipManager.getInstance().delete(dbShip);
                        }
                    } else {
                        //add captured in database.
                        if (side1nation != null) {
                            count++;
                            LOGGER.debug("adding ship : " + ship.getShipId() + " - nation : " + ship.getNation().getId() + ", " + ship.getNation().getName());
                            ship.setNation(side1nation);
                            ship.setName("Captured Pirate Ship " + count);
                            ship.setFleet(fleetId);
                            ship.setCapturedByNation(0);
                            ship.setPosition((Position) position.clone());
                            ShipManager.getInstance().add(ship);
                            LOGGER.debug("added ship : " + ship.getShipId() + " - nation : " + ship.getNation().getId() + ", " + ship.getNation().getName());
                        }
                    }
                } else {
                    LOGGER.debug("updating ship : " + ship.getShipId() + " - nation : " + ship.getNation().getId() + ", " + ship.getNation().getName());
                    ShipManager.getInstance().update(ship);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to update attacked by pirates ship record (ship Id: " + ship.getShipId() + ", nation : " + ship.getNation().getName() + ")");
            }
        }
        //delete lost ships
        for (Ship ship : side2) {
            if (ship.getShipId() > 0) {
                LOGGER.debug("deleting captured by pirates ship : " + ship.getShipId() + " - nation : " + ship.getNation().getId() + ", " + ship.getNation().getName());
                //just check the ship exists in the database
                final Ship dbShip = ShipManager.getInstance().getByID(ship.getShipId());
                if (dbShip != null) {
                    ShipManager.getInstance().delete(dbShip);
                }
            }
        }

        NavalBattleReportManager.getInstance().add(nbr);
    }

    /**
     * Invoke the naval battle processor to process a patrol battle.
     *
     * @param field the BattleField to execute.
     */
    public void conductPatrol(final BattleField field) {
        // Identify if the weather of the battle
        int weather;
        if (field.getField().getStorm() > 0) {
            weather = NavalBattleProcessor.WEATHER_STORM;
        } else {
            weather = weatherSelector.rollWeather(field.getField().getPosition());
        }

        // Prepare the ships of each side
        final List<Ship> side1 = prepareShipList(field.getField().getPosition(), field.getSide(0));

        final NavalBattleProcessor nbp = new NavalBattleProcessor(weather, side1, field.getPatrolForce(), false);
        final List<RoundStat> result = nbp.process();

        // Store results
        final NavalBattleReport nbr = new NavalBattleReport();
        nbr.setPosition(field.getField().getPosition());
        nbr.setTurn(field.getField().getPosition().getGame().getTurn());
        nbr.setWeather(weather);
        nbr.setPiracy(false);
        nbr.setPatrol(true);

        // Count number of fleets involved in the battle (side 1)
        final Set<Integer> fleetSet1 = new HashSet<Integer>();

        // Identify involved Nations
        final Set<Nation> side1set = new HashSet<Nation>();
        for (final Ship ship : side1) {
            side1set.add(ship.getNation());

            // Only consider ships in fleet
            if (ship.getFleet() > 0) {
                fleetSet1.add(ship.getFleet());
            }
        }
        nbr.setSide1(side1set);

        final int cntFleetsSide1 = fleetSet1.size();

        // Count number of fleets involved in the battle (side 2)
        final Set<Integer> fleetSet2 = new HashSet<Integer>();

        final Set<Nation> side2set = new HashSet<Nation>();
        for (final Ship ship : field.getPatrolForce()) {
            side2set.add(ship.getNation());

            // Only consider ships in fleet
            if (ship.getFleet() > 0) {
                fleetSet2.add(ship.getFleet());
            }
        }
        nbr.setSide2(side2set);

        final int cntFleetsSide2 = fleetSet2.size();

        // Report casualties inflicted and received
        int side1loses = 0;
        int side2loses = 0;
        for (final RoundStat roundStatistics : result) {
            if (roundStatistics.getRound() == AbstractNavalBattleRound.ROUND_FINAL) {

                // Retrieve Captured ships.
                if (roundStatistics.getSideStat()[0].size() > 7) {
                    side1loses += roundStatistics.getSideStat()[0].get(7).getShips();
                }

                if (roundStatistics.getSideStat()[1].size() > 7) {
                    side2loses += roundStatistics.getSideStat()[1].get(7).getShips();
                }

                // Retrieve Lost ships.
                if (roundStatistics.getSideStat()[0].size() > 8) {
                    side1loses += roundStatistics.getSideStat()[0].get(8).getShips();
                }

                if (roundStatistics.getSideStat()[1].size() > 8) {
                    side2loses += roundStatistics.getSideStat()[1].get(8).getShips();
                }

            }
        }

        // Determine if this is a large naval battle
        final boolean isLarge = (cntFleetsSide1 + cntFleetsSide2 > 8);

        // Save winner
        if (result.get(7).getResult() == RoundStat.SIDE_A) {
            nbr.setWinner(RoundStat.SIDE_A);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, field.getSide(RoundStatistics.SIDE_A).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our patrolling forces defeated the naval forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at the naval battle that took place at " + field.getField().getPosition().toString());
                if (newsId == 0) {
                    newsId = thisNewsId;
                }

                int casInflictedThisTurn = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
                final int casInflicted = retrieveReportRunningSum(nation, S_SINKS);

                /** @todo temporarily disabled
                // Destroy/capture 30 enemy warships
                if (casInflicted < 30 && casInflicted + side2loses > 30) {
                parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");

                }

                // Destroy/capture 70 enemy warships
                if (casInflicted < 70 && casInflicted + side2loses > 70) {
                parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
                }

                // Destroy/capture 150 enemy warships
                if (casInflicted < 150 && casInflicted + side2loses > 150) {
                parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
                }
                 */

                casInflictedThisTurn += side2loses;
                report(nation, S_SINKS, casInflictedThisTurn);

                // change player's profile
                parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side2loses);

                int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
                casReceived += side1loses;
                report(nation, S_SINKED, casReceived);

                // Charge VPs
                if (isLarge) {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MAJOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MAJOR_WIN * fleetsInvolved, "Won a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                } else {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MINOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MINOR_WIN * fleetsInvolved, "Won a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());
                }
            }

            for (final Nation nation : side2set) {
                news(nation, field.getSide(RoundStatistics.SIDE_B).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our forces were defeated by the naval patrol of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at the battle that took place at " + field.getField().getPosition().toString());

                int casInflictedThisTurn = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
                final int casInflicted = retrieveReportRunningSum(nation, S_SINKS);

                /** @todo temporarily disabled
                // Destroy/capture 30 enemy warships
                if (casInflicted < 30 && casInflicted + side1loses > 30) {
                parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");
                }

                // Destroy/capture 70 enemy warships
                if (casInflicted < 70 && casInflicted + side1loses > 70) {
                parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
                }

                // Destroy/capture 150 enemy warships
                if (casInflicted < 150 && casInflicted + side1loses > 150) {
                parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
                }
                 */

                casInflictedThisTurn += side1loses;
                report(nation, S_SINKS, casInflictedThisTurn);

                // change player's profile
                parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side1loses);

                int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
                casReceived += side2loses;
                report(nation, S_SINKED, casReceived);

                // Charge VPs
                if (isLarge) {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MAJOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MAJOR_LOSE * fleetsInvolved, "Lost a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                } else {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MINOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MINOR_LOSE * fleetsInvolved, "Lost a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());
                }
            }

        } else if (result.get(7).getResult() == RoundStat.SIDE_B) {
            nbr.setWinner(RoundStat.SIDE_B);

            // Send News
            int newsId = 0;
            for (final Nation nation : side2set) {
                final int thisNewsId = news(nation, field.getSide(RoundStatistics.SIDE_B).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "We defeated the patrolling forces of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at the naval battle that took place at " + field.getField().getPosition().toString());
                if (newsId == 0) {
                    newsId = thisNewsId;
                }

                int casInflicted = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
                if (casInflicted == 0) {
                    casInflicted = retrieveReportAsInt(nation, getGame().getTurn() - 1, S_SINKS);
                }

                /** @todo temporarily disabled
                // Destroy/capture 30 enemy warships
                if (casInflicted < 30 && casInflicted + side1loses > 30) {
                parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");
                }

                // Destroy/capture 70 enemy warships
                if (casInflicted < 70 && casInflicted + side1loses > 70) {
                parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
                }

                // Destroy/capture 150 enemy warships
                if (casInflicted < 150 && casInflicted + side1loses > 150) {
                parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
                }
                 */

                casInflicted += side1loses;
                report(nation, S_SINKS, casInflicted);

                // change player's profile
                parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side1loses);


                int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
                casReceived += side2loses;
                report(nation, S_SINKED, casReceived);

                // Charge VPs
                if (isLarge) {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MAJOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MAJOR_LOSE * fleetsInvolved, "Lost a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                } else {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MINOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MINOR_LOSE * fleetsInvolved, "Lost a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());
                }
            }

            for (final Nation nation : side1set) {
                news(nation, field.getSide(RoundStatistics.SIDE_A).get(0),
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our patrolling forces were defeated by the naval forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at the naval battle that took place at " + field.getField().getPosition().toString());

                int casInflicted = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
                if (casInflicted == 0) {
                    casInflicted = retrieveReportAsInt(nation, getGame().getTurn() - 1, S_SINKS);
                }

                /** @todo temporarily disabled
                // Destroy/capture 30 enemy warships
                if (casInflicted < 30 && casInflicted + side2loses > 30) {
                parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");

                }

                // Destroy/capture 70 enemy warships
                if (casInflicted < 70 && casInflicted + side2loses > 70) {
                parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
                }

                // Destroy/capture 150 enemy warships
                if (casInflicted < 150 && casInflicted + side2loses > 150) {
                parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
                }
                 */

                casInflicted += side2loses;
                report(nation, S_SINKS, casInflicted);

                // change player's profile
                parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side2loses);

                int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
                casReceived += side1loses;
                report(nation, S_SINKED, casReceived);

                // Charge VPs
                if (isLarge) {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MAJOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MAJOR_WIN * fleetsInvolved, "Won a major naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());

                } else {
                    int fleetsInvolved = Math.min(cntFleetsSide1 + cntFleetsSide2, VPConstants.NB_MINOR_MAX);
                    if (fleetsInvolved < 1) {
                        fleetsInvolved = 1;
                    }

                    parent.changeVP(getGame(), nation, NB_MINOR_WIN * fleetsInvolved, "Won a minor naval battle (" + (cntFleetsSide1 + cntFleetsSide2) + " fleets involved) at " + field.getField().getPosition().toString());
                }
            }

        } else {
            nbr.setWinner(0);

            // Send News
            int newsId = 0;
            for (final Nation nation : side1set) {
                final int thisNewsId = news(nation, nation,
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our patrolling forces battled against the naval forces of " + field.getSide(RoundStatistics.SIDE_B).get(0).getName() + " at " + field.getField().getPosition().toString() + ". The naval battle was indecisive.");

                if (newsId == 0) {
                    newsId = thisNewsId;
                }

                int casInflicted = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
                if (casInflicted == 0) {
                    casInflicted = retrieveReportAsInt(nation, getGame().getTurn() - 1, S_SINKS);
                }

                /** @todo temporarily disabled
                // Destroy/capture 30 enemy warships
                if (casInflicted < 30 && casInflicted + side2loses > 30) {
                parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");

                }

                // Destroy/capture 70 enemy warships
                if (casInflicted < 70 && casInflicted + side2loses > 70) {
                parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
                }

                // Destroy/capture 150 enemy warships
                if (casInflicted < 150 && casInflicted + side2loses > 150) {
                parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
                }
                 */

                casInflicted += side2loses;
                report(nation, S_SINKS, casInflicted);

                // change player's profile
                parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side2loses);


                int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
                casReceived += side1loses;
                report(nation, S_SINKED, casReceived);
            }

            for (final Nation nation : side2set) {
                news(nation, nation,
                        NewsConstants.NEWS_MILITARY, newsId,
                        "Our forces battled against the naval patrol of " + field.getSide(RoundStatistics.SIDE_A).get(0).getName() + " at " + field.getField().getPosition().toString() + ". The naval battle was indecisive.");

                int casInflicted = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKS);
                if (casInflicted == 0) {
                    casInflicted = retrieveReportAsInt(nation, getGame().getTurn() - 1, S_SINKS);
                }

                /** @todo temporarily disabled
                // Destroy/capture 30 enemy warships
                if (casInflicted < 30 && casInflicted + side1loses > 30) {
                parent.changeVP(getGame(), nation, SUNK_30, "Destroy/capture 30 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 30 enemy warships!");
                }

                // Destroy/capture 70 enemy warships
                if (casInflicted < 70 && casInflicted + side1loses > 70) {
                parent.changeVP(getGame(), nation, SUNK_70, "Destroy/capture 70 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 70 enemy warships!");
                }

                // Destroy/capture 150 enemy warships
                if (casInflicted < 150 && casInflicted + side1loses > 150) {
                parent.changeVP(getGame(), nation, SUNK_150, "Destroy/capture 150 enemy warships");

                news(nation, nation, NewsConstants.NEWS_MILITARY, 0,
                "Our capable admirals and our naval forces have destroyed and captured more than 150 enemy warships!");
                }
                 */

                casInflicted += side1loses;
                report(nation, S_SINKS, casInflicted);

                // change player's profile
                parent.changeProfile(getGame(), nation, ProfileConstants.ENEMY_SUNK_ALL, side1loses);

                int casReceived = retrieveReportAsInt(nation, getGame().getTurn(), S_SINKED);
                casReceived += side2loses;
                report(nation, S_SINKED, casReceived);
            }
        }

        // Save analytical results
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(result);
            os.close();
            zos.close();
            baos.close();

            nbr.setStats(baos.toByteArray());
        } catch (Exception ex) {
            LOGGER.fatal(ex);
        }

        // Update records
        for (final Ship ship : side1) {
            ShipManager.getInstance().update(ship);
        }

        for (final Ship ship : field.getPatrolForce()) {
            ShipManager.getInstance().update(ship);
        }

        NavalBattleReportManager.getInstance().add(nbr);
    }

    /**
     * Locate all the ships for the given list of nations that are located at given x/y/region.
     *
     * @param position the position to look.
     * @param side     the list of nations.
     * @return a list of ships.
     */
    private List<Ship> prepareShipList(final Position position, final List<Nation> side) {
        final List<Ship> ships = new ArrayList<Ship>();
        // Iterate through nations
        for (final Nation nation : side) {
            // Locate ships at this sector
            final List<Ship> shipList = ShipManager.getInstance().listByPositionNation(position, nation);

            // Go through all the ships
            for (final Ship ship : shipList) {
                // Do not consider overrun ships
                if (ship.getCondition() > 0) {
                    ships.add(ship);
                }

                // East Indiamen should fight as class 2 ships
                if (ship.getType().getTypeId() == 13) {
                    ship.getType().setShipClass(2);
                }
            }
        }

        return ships;
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Nation nation, final Nation subject, final int type, final int baseNewsId, final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(game);
        thisNewsEntry.setTurn(game.getTurn());
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected int retrieveReportAsInt(final Nation owner, final int turn, final String key) {
        final String value = retrieveReport(owner, turn, key);

        // Check if string is empty
        if (value.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected int retrieveReportRunningSum(final Nation owner, final String key) {
        try {
            final List<Report> thisReport = ReportManager.getInstance().listByOwnerKey(owner, getGame(), key);
            if (thisReport.isEmpty()) {
                return 0;
            }

            int totValue = 0;
            for (Report report : thisReport) {
                totValue += Integer.parseInt(report.getValue());
            }
            return totValue;

        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected String retrieveReport(final Nation owner, final int turn, final String key) {
        try {
            final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getGame(), turn, key);
            if (thisReport == null) {
                return "";
            }
            return thisReport.getValue();

        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final String key, final int value) {
        report(owner, key, Integer.toString(value)); // NOPMD
    }

    /**
     * Add a report entry for this turn.
     *
     * @param owner the Owner of the report entry.
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    protected void report(final Nation owner, final String key, final String value) {
        // check if report already exists
        Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getGame(), getGame().getTurn(), key);
        if (thisReport == null) {
            thisReport = new Report();
            thisReport.setGame(getGame());
            thisReport.setTurn(getGame().getTurn());
            thisReport.setNation(owner);
            thisReport.setKey(key);
        }

        thisReport.setValue(value);
        ReportManager.getInstance().add(thisReport);
    }

}
