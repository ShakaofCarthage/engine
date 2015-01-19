package com.eaw1805.battles.field;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.detachment.FieldBattleDetachmentProcessor;
import com.eaw1805.battles.field.generation.MapBuilder;
import com.eaw1805.battles.field.morale.MoraleChecker;
import com.eaw1805.battles.field.orders.FieldBattleOrderProcessor;
import com.eaw1805.battles.field.processors.LongRangeProcessor;
import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderProcessor;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.battles.field.victory.PursuitProcessor;
import com.eaw1805.battles.field.victory.VictoryChecker;
import com.eaw1805.battles.field.visibility.FieldBattleVisibilityProcessor;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.FieldBattleReport.Stage;
import com.eaw1805.data.model.battles.field.FieldBattleHalfRoundStatistics;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import com.eaw1805.data.model.battles.field.log.HalfRoundLog;
import com.eaw1805.data.model.battles.field.log.IHalfRoundLog;
import com.eaw1805.data.model.battles.field.log.StructureStatus;
import com.eaw1805.data.model.map.Sector;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class is responsible for executing the field battles.
 *
 * @author fragkakis
 */
public class FieldBattleProcessor implements VPConstants, ReportConstants {

    /**
     * The BattleField where the battle will take place.
     */
    private BattleField battleField = null;

    /**
     * The brigades of each side, including dead and fled brigades.
     */
    private List<Brigade>[] initialSideBrigades;

    /**
     * The alive and in-battlefield brigades of each side.
     */
    private List<Brigade>[] sideBrigades;

    private BiMap<FieldBattleSector, Brigade> sectorsToBrigades = HashBiMap.create();

    private FieldBattleMap fbMap;

    private FieldBattleProcessorResourceLocator resourceLocator;

    private int currentRound;

    /**
     * This collection is maintained by by the {@link MovementProcessor} and the
     * {@link FieldBattleProcessor} and holds the brigades that have moved
     * during the current half round. It is later used by other processors (i.e.
     * {@link LongRangeFireProcessor}) for their own processing.
     */
    private Set<Brigade> brigadesThatMovedInTheCurrentHalfRound = new HashSet<Brigade>();

    /**
     * This collection is maintained by by the {@link MovementProcessor} and the
     * {@link FieldBattleProcessor} and holds the brigades that moved during the
     * previous half round. It is later used by other processors (i.e. {@link
     * LongRangeFireProcessor}) for their own processing.
     */
    private Set<Brigade> brigadesThatMovedInThePreviousHalfRound = new HashSet<Brigade>();

    /**
     * The Morale checker, it helps movement and attack processors perform the morale checks.
     */
    private MoraleChecker moraleChecker;

    /**
     * The commander processor maintains and provides information about the commanders.
     */
    private CommanderProcessor commanderProcessor;

    /**
     * The order processor resolves the priority of the orders.
     */
    private FieldBattleOrderProcessor fieldBattleOrderProcessor;

    /**
     * The field battle visibility processor.
     */
    private FieldBattleVisibilityProcessor fieldBattleVisibilityProcessor;

    /**
     * The field battle detachment processor.
     */
    private FieldBattleDetachmentProcessor fieldBattleDetachmentProcessor;

    /**
     * The victory checker.
     */
    private VictoryChecker victoryChecker;

    /**
     * The pursuit processor.
     */
    private PursuitProcessor pursuitProcessor;

    private IHalfRoundLog halfRoundLog;

    /**
     * Helps in debugging.
     */
    private boolean persistAfterProcess = true;

    private static final int TOTAL_ROUNDS_NUMBER = 24;

    private static final int HALF_ROUNDS_NUMBER = TOTAL_ROUNDS_NUMBER / 2;

    private static final int BEFORE_FIRST_HALF_ROUND = -1;

    private static final int NO_WINNER = -1;

    private static final Logger LOGGER = LogManager.getLogger(FieldBattleProcessor.class);

    public FieldBattleProcessor(final boolean persistAfterProcess, final int scenarioId) {
        resourceLocator = new FieldBattleProcessorResourceLocator(scenarioId);
        this.persistAfterProcess = persistAfterProcess;
    }

    /**
     * Generates the field battle terrain.
     *
     * @param battleId the id of the field battle
     */
    public FieldBattleReport processInitialization(final int battleId) {
        final FieldBattleReport fbReport = resourceLocator.getFieldBattleReport(battleId);
        initialize(fbReport);

        final List<List<Brigade>> sideBrigadesList = new ArrayList<List<Brigade>>();
        sideBrigadesList.add(sideBrigades[0]);
        sideBrigadesList.add(sideBrigades[1]);

        final MapBuilder tg = new MapBuilder(battleField, sideBrigadesList);
        final FieldBattleMap fbMap = tg.buildMap();
        fbMap.setBattleId(battleId);

        resourceLocator.save(fbMap);

        // field battle report
        appendHalfRoundStats(fbReport, new FieldBattleHalfRoundStatistics(BEFORE_FIRST_HALF_ROUND, null, fbMap, null, NO_WINNER));
        fbReport.setStageEnum(Stage.INITIALIZED);
        resourceLocator.updateFieldBattleReport(fbReport);

        return fbReport;
    }

    @SuppressWarnings("unchecked")
    private void initialize(final FieldBattleReport fbReport) {
        LOGGER.debug("Performing initialization for field battle " + fbReport.getBattleId());
        if (battleField == null) {
            final Sector sector = resourceLocator.getSectorByPosition(fbReport.getPosition());
            battleField = new BattleField(sector);

            final Set<Nation> side0 = fbReport.getSide1();
            final Set<Nation> side1 = fbReport.getSide2();

            for (Nation nation : side0) {
                battleField.addNation(0, nation);
            }

            for (Nation nation : side1) {
                battleField.addNation(1, nation);
            }
        }

        // fetch side brigades and keep track of brigades that have died or left the battle field
        if (sideBrigades == null) {
            initialSideBrigades = new List[2];
            sideBrigades = new List[2];

            for (int side = 0; side <= 1; side++) {
                initialSideBrigades[side] = new ArrayList<Brigade>();
                sideBrigades[side] = new ArrayList<Brigade>();
                for (Nation nation : battleField.getSide(side)) {
                    initialSideBrigades[side].addAll(resourceLocator.getBrigadesForPositionAndNation(battleField.getField().getPosition(), nation));
                }

                for (Brigade brigade : initialSideBrigades[side]) {
                    if (brigade.getFieldBattlePosition().exists()) {
                        sideBrigades[side].add(brigade);
                    }
                }
            }
        }

        // brigades that moved in previous halfround (for the 2nd set of halfrounds)
        // and brigades that left the field battle
        final List<String> brigadesThatMovedInPreviousHalfroundIds = Arrays.asList(StringUtils.split(
                fbReport.getBrigadesThatMovedInPreviousHalfround() == null ? "" : fbReport.getBrigadesThatMovedInPreviousHalfround(),
                ","));

        final Set<Brigade> allBrigades = new HashSet<Brigade>();
        allBrigades.addAll(sideBrigades[0]);
        allBrigades.addAll(sideBrigades[1]);

        for (final Brigade brigade : allBrigades) {
            if (brigadesThatMovedInPreviousHalfroundIds.contains(Integer.toString(brigade.getBrigadeId()))) {
                LOGGER.debug(brigade + " has moved in the previous halfround");
                brigadesThatMovedInThePreviousHalfRound.add(brigade);
            }
            OrderSanitizer.sanitizeOrder(brigade, brigade.getBasicOrder(), true);
            OrderSanitizer.sanitizeOrder(brigade, brigade.getAdditionalOrder(), false);
        }

        if (moraleChecker == null) {
            moraleChecker = new MoraleChecker(this);
            if (fbReport.getInitialMoraleContributions() != null) {
                try {
                    final Map<Integer, Double> initialMoraleContributions = (Map<Integer, Double>) new ObjectInputStream(
                            new ByteArrayInputStream(fbReport.getInitialMoraleContributions()))
                            .readObject();
                    LOGGER.debug("Retrieved from field battle report the following morale contributions for brigades: " + initialMoraleContributions);
                    moraleChecker.setBrigadeMoraleRelativeContributions(
                            initialMoraleContributions);

                } catch (Exception e) {
                    LOGGER.error("Error retrieving initial morale contributions from field battle report", e);
                }
            }
        }

        if (victoryChecker == null) {
            victoryChecker = new VictoryChecker(this);
        }

        if (pursuitProcessor == null) {
            pursuitProcessor = new PursuitProcessor(this);
        }

        halfRoundLog = new HalfRoundLog();
    }

    public FieldBattleReport processFirstHalfRounds(final int battleId) {
        final FieldBattleReport fbReport = processHalfRounds(battleId);
        if (persistAfterProcess) {
            updateDatabase();
        }

        return fbReport;
    }

    public FieldBattleReport processSecondHalfRounds(final int battleId) {
        final FieldBattleReport fbReport = processHalfRounds(battleId);
        cleanUpFieldBattleMetadata();    // field battle has ended, winner or not
        if (persistAfterProcess) {
            updateDatabase();
        }

        return fbReport;
    }

    private FieldBattleReport processHalfRounds(final int battleId) {
        final FieldBattleReport fbReport = resourceLocator.getFieldBattleReport(battleId);
        initialize(fbReport);

        final List<byte[]> halfRoundStats = new ArrayList<byte[]>();

        moraleChecker.calculateInitialSideMoralesIfRequired();

        // bring brigades and the field battle map
        fbMap = resourceLocator.getFieldBattleMap(battleId);
        commanderProcessor = new CommanderProcessor(this, sideBrigades);
        fieldBattleOrderProcessor = new FieldBattleOrderProcessor(this);
        fieldBattleVisibilityProcessor = new FieldBattleVisibilityProcessor(this);
        fieldBattleDetachmentProcessor = new FieldBattleDetachmentProcessor(this);

        // populate collections holding the positions and the orders
        for (Brigade brigade : sideBrigades[0]) {
            sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade.getFieldBattlePosition().getX(), brigade.getFieldBattlePosition().getY()), brigade);
        }

        for (Brigade brigade : sideBrigades[1]) {
            sectorsToBrigades.put(fbMap.getFieldBattleSector(brigade.getFieldBattlePosition().getX(), brigade.getFieldBattlePosition().getY()), brigade);
        }

        findBrigadeArms(sideBrigades[0]);
        findBrigadeArms(sideBrigades[1]);

        setMoraleStatusesToNormal(sideBrigades[0]);
        setMoraleStatusesToNormal(sideBrigades[1]);

        final MovementProcessor movementProcessor = new MovementProcessor(this);
        movementProcessor.initialize();

        final LongRangeProcessor longRangeProcessor = new LongRangeProcessor(this);
        MeleeProcessor meleeProcessor = new MeleeProcessor(this);

        final int initialRound = fbReport.getRound() + 1;
        final int finalRound = fbReport.getRound() + 1 + HALF_ROUNDS_NUMBER;
        if (initialRound == 0) {
            // amend first round stats, add brigade positions
            fbReport.setStats(null);
            appendHalfRoundStats(fbReport, new FieldBattleHalfRoundStatistics(BEFORE_FIRST_HALF_ROUND, sideBrigades, fbMap, null, NO_WINNER));
        }

        for (int round = initialRound; round < finalRound; round++) {
            this.currentRound = round;

            // first half round
            resetHalfRoundStatistics();
            VisualisationUtils.visualize(fbMap, VisualisationUtils.convertToArmySectors(fbMap, sideBrigades));
            LOGGER.debug(sectorsToBrigades.toString());
            fieldBattleDetachmentProcessor.calculateCurrentDetachmentsForSide(0);
            movementProcessor.process(0, round);
            longRangeProcessor.process(1, round);
            longRangeProcessor.process(0, round);
            meleeProcessor.process(round);
            prepareMovementDataForNextHalfRound();
            keepHalfRoundStatistics(halfRoundStats, round * 2, NO_WINNER);

            // second half round
            resetHalfRoundStatistics();
            VisualisationUtils.visualize(fbMap, VisualisationUtils.convertToArmySectors(fbMap, sideBrigades));
            LOGGER.debug(sectorsToBrigades.toString());
            fieldBattleDetachmentProcessor.calculateCurrentDetachmentsForSide(1);
            movementProcessor.process(1, round);
            longRangeProcessor.process(0, round);
            longRangeProcessor.process(1, round);
            meleeProcessor.process(round);
            prepareMovementDataForNextHalfRound();

            // check for winners
            int fieldBattleWinner = victoryChecker.fieldBattleWinner();
            if (fieldBattleWinner >= 0) {
                pursuitProcessor.performPursuit(fieldBattleWinner);
            }

            keepHalfRoundStatistics(halfRoundStats, round * 2 + 1, fieldBattleWinner);
            fbReport.setWinner(fieldBattleWinner);
            fbReport.setRound(round);

            if (fieldBattleWinner >= 0) {
                cleanUpFieldBattleMetadata();
                break;
            }
        }

        // field battle report
        appendHalfRoundStats(fbReport, halfRoundStats);
        fbReport.setInitialMoraleContributions(convertToByteArray((Serializable) moraleChecker.getBrigadeMoraleRelativeContributions()));
        fbReport.setBrigadesThatMovedInPreviousHalfround(joinBrigadeIds(brigadesThatMovedInThePreviousHalfRound));
        fbReport.setStageEnum(Stage.HALF_ROUNDS);
        if (persistAfterProcess) {
            resourceLocator.updateFieldBattleReport(fbReport);
        }

        return fbReport;
    }

    private void resetHalfRoundStatistics() {
        halfRoundLog.clear();

        final Set<FieldBattleSector> structureSectors = MapUtils.findStructures(fbMap);
        for (FieldBattleSector sector : structureSectors) {
            final StructureStatus structureStatus = new StructureStatus(sector.getX(), sector.getY(), sector.getStructureType(),
                    sector.getStructureHitPoints());
            halfRoundLog.registerStructureStatus(structureStatus);
        }
    }

    private void keepHalfRoundStatistics(final List<byte[]> halfRoundStats,
                                         final int halfRound,
                                         final int fieldBattleWinner) {
        final FieldBattleHalfRoundStatistics stats = new FieldBattleHalfRoundStatistics(halfRound, sideBrigades, fbMap, halfRoundLog, fieldBattleWinner);
        halfRoundLog.logMoraleForSide(moraleChecker.calculateArmyMorale(0), 0);
        halfRoundLog.logMoraleForSide(moraleChecker.calculateArmyMorale(1), 1);
        halfRoundStats.add(convertToByteArray(stats));
    }

    private String joinBrigadeIds(final Set<Brigade> brigades) {
        final Set<Integer> brigadeIds = new HashSet<Integer>();
        for (Brigade brigade : brigades) {
            brigadeIds.add(brigade.getBrigadeId());
        }

        return StringUtils.join(brigadeIds, ",");
    }

    private void cleanUpFieldBattleMetadata() {
        // TODO
    }

    private void updateDatabase() {
        // update brigades
        resourceLocator.updateBrigades(initialSideBrigades);

        // update map
        resourceLocator.updateMap(fbMap);

        // update commanders
        final Set<Commander> commanders = new HashSet<Commander>();
        commanders.addAll(commanderProcessor.getSideCommanders().get(0));
        commanders.addAll(commanderProcessor.getSideCommanders().get(1));
        resourceLocator.updateCommanders(commanders);
    }

    private void prepareMovementDataForNextHalfRound() {
        brigadesThatMovedInThePreviousHalfRound = brigadesThatMovedInTheCurrentHalfRound;
        brigadesThatMovedInTheCurrentHalfRound = new HashSet<Brigade>();
    }

    private void findBrigadeArms(final List<Brigade> brigadeList) {
        for (Brigade brigade : brigadeList) {
            brigade.updateMP();
            brigade.setArmTypeEnum(ArmyUtils.findArm(brigade));
        }
    }

    private void setMoraleStatusesToNormal(final List<Brigade> brigadeList) {
        for (Brigade brigade : brigadeList) {
            brigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
        }
    }

    public Order findCurrentOrder(final Brigade brigade) {
        return fieldBattleOrderProcessor.findCurrentOrder(brigade);
    }

    /**
     * Checks whether a brigade is in melee combat. This is the case when the brigade
     * is not fleeing (routing) AND it has a neighbouring enemy brigade.
     *
     * @param brigade the brigade to check
     * @return true if the brigade is in melee combat, false otherwise
     */
    public boolean isInMeleeCombat(final Brigade brigade) {
        boolean isInMeleeCombat = false;

        // if the brigade is fleeing, it is not in melee combat
        if (!brigade.isRouting()) {
            final int side = findSide(brigade);
            final FieldBattleSector brigadePosition = getSector(brigade);
            final Set<FieldBattleSector> neighbours = MapUtils.getHorizontalAndVerticalNeighbours(brigadePosition);
            final Set<Brigade> neibouringEnemies = findBrigadesOfSide(neighbours, side == 0 ? 1 : 0);

            isInMeleeCombat = !neibouringEnemies.isEmpty();
        }

        return isInMeleeCombat;
    }

    public Set<Brigade> findBrigadesOfSide(final Set<FieldBattleSector> sectors, final int side) {
        final List<Brigade> thisSideBrigades = sideBrigades[side];

        final Set<Brigade> sideBrigadesInSectors = new HashSet<Brigade>();
        for (final FieldBattleSector sector : sectors) {
            final Brigade brigade = getBrigadeInSector(sector);

            if (brigade != null && thisSideBrigades.contains(brigade)) {
                sideBrigadesInSectors.add(brigade);
            }
        }

        return sideBrigadesInSectors;
    }

    public List<Brigade>[] getSideBrigades() {
        return sideBrigades;
    }

    public FieldBattleMap getFbMap() {
        return fbMap;
    }

    public Brigade getBrigadeInSector(final FieldBattleSector sector) {
        return sectorsToBrigades.get(sector);
    }

    public FieldBattleSector getSector(final Brigade brigade) {
        return sectorsToBrigades.inverse().get(brigade);
    }

    public BiMap<FieldBattleSector, Brigade> getSectorsToBrigades() {
        return sectorsToBrigades;
    }

    public void setSectorsToBrigades(final BiMap<FieldBattleSector, Brigade> sectorsToBrigades) {
        this.sectorsToBrigades = sectorsToBrigades;
    }

    public BattleField getBattleField() {
        return battleField;
    }

    public void setFbMap(final FieldBattleMap fbMap) {
        this.fbMap = fbMap;
    }

    public void setSideBrigades(final List<Brigade>[] sideBrigades) {
        this.sideBrigades = sideBrigades;
    }

    public Set<Brigade> getBrigadesThatMovedInTheCurrentHalfRound() {
        return brigadesThatMovedInTheCurrentHalfRound;
    }

    public void setBrigadesThatMovedInTheCurrentHalfRound(final Set<Brigade> brigadesThatMovedInTheCurrentHalfRound) {
        this.brigadesThatMovedInTheCurrentHalfRound = brigadesThatMovedInTheCurrentHalfRound;
    }

    public Set<Brigade> getBrigadesThatMovedInThePreviousHalfRound() {
        return brigadesThatMovedInThePreviousHalfRound;
    }

    public void setBrigadesThatMovedInThePreviousHalfRound(final Set<Brigade> brigadesThatMovedInThePreviousHalfRound) {
        this.brigadesThatMovedInThePreviousHalfRound = brigadesThatMovedInThePreviousHalfRound;
    }

    /**
     * Setter for testing purposes
     *
     * @param resourceLocator
     */
    public void setResourceLocator(final FieldBattleProcessorResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
    }

    public int findSide(Brigade brigade) {
        return initialSideBrigades[0].contains(brigade) ? 0 : 1;
    }

    public MoraleChecker getMoraleChecker() {
        return moraleChecker;
    }

    public void markBattalionAsDead(final Battalion battalion) {
        LOGGER.debug("Battalion " + battalion + " is dead.");
        Brigade brigade = battalion.getBrigade();
        brigade.getBattalions().remove(battalion);

        if (brigade.getBattalions().isEmpty()) {
            markBrigadeAsDead(brigade);
        }

        battalion.setBrigade(null);
    }

    /**
     * Marks a brigade as dead.
     *
     * @param brigade the brigade
     */
    public void markBrigadeAsDead(final Brigade brigade) {
        LOGGER.debug("Brigade " + brigade + " is dead.");
        fieldBattleDetachmentProcessor.removeBrigadeFromDetachments(brigade);

        brigade.getFieldBattlePosition().setX(-1);
        brigade.getFieldBattlePosition().setY(-1);

        Commander commanderInBrigade = commanderProcessor.getCommanderInBrigade(brigade);
        if (commanderInBrigade != null && !commanderInBrigade.getDead()) {
            commanderProcessor.commanderHasBeenKilled(commanderInBrigade);
        }

        int side = findSide(brigade);
        sideBrigades[side].remove(brigade);
        sectorsToBrigades.inverse().remove(brigade);

    }

    /**
     * Marks a brigade as having left the battlefield.
     *
     * @param brigade the brigade
     */
    public void markBrigadeAsLeftTheBattlefield(final Brigade brigade) {
        LOGGER.debug("Brigade " + brigade + " has left the battlefield.");
        fieldBattleDetachmentProcessor.removeBrigadeFromDetachments(brigade);

        brigade.getFieldBattlePosition().setX(-1);
        brigade.getFieldBattlePosition().setY(-1);

        int side = findSide(brigade);
        sideBrigades[side].remove(brigade);

        // remove from map;
        sectorsToBrigades.inverse().remove(brigade);

        halfRoundLog.logBrigadeLeftBattlefield(brigade);
    }

    public void setMoraleChecker(final MoraleChecker moraleChecker) {
        this.moraleChecker = moraleChecker;
    }

    public CommanderProcessor getCommanderProcessor() {
        return commanderProcessor;
    }

    public void setCommanderProcessor(final CommanderProcessor commanderProcessor) {
        this.commanderProcessor = commanderProcessor;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public static byte[] convertToByteArray(Serializable object) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(object);
            os.close();
            baos.close();

            return baos.toByteArray();

        } catch (IOException e) {
            LOGGER.error("Error in serialization", e);
            return null;
        }
    }

    public void setFieldBattleOrderProcessor(final FieldBattleOrderProcessor fieldBattleOrderProcessor) {
        this.fieldBattleOrderProcessor = fieldBattleOrderProcessor;
    }

    public FieldBattleProcessorResourceLocator getResourceLocator() {
        return resourceLocator;
    }

    public static void appendHalfRoundStats(final FieldBattleReport fbReport,
                                            final FieldBattleHalfRoundStatistics halfRoundsStats) {
        List<byte[]> statsCollection = new ArrayList<byte[]>();
        statsCollection.add(convertToByteArray(halfRoundsStats));
        appendHalfRoundStats(fbReport, statsCollection);
    }

    @SuppressWarnings("unchecked")
    public static void appendHalfRoundStats(final FieldBattleReport fbReport,
                                            final List<byte[]> halfRoundsStatsCollection) {
        try {
            List<byte[]> fbStats = null;
            if (fbReport.getStats() != null) {
                final ByteArrayInputStream bais = new ByteArrayInputStream(fbReport.getStats());
                final GZIPInputStream zis = new GZIPInputStream(bais);
                final ObjectInputStream is = new ObjectInputStream(zis);
                fbStats = (List<byte[]>) is.readObject();
                is.close();
                zis.close();
                bais.close();
            } else {
                fbStats = new ArrayList<byte[]>();
            }

            fbStats.addAll(halfRoundsStatsCollection);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(fbStats);
            os.close();
            zos.close();
            baos.close();

            fbReport.setStats(baos.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setBattleField(final BattleField battleField) {
        this.battleField = battleField;
    }

    public List<Brigade>[] getInitialSideBrigades() {
        return initialSideBrigades;
    }

    public void setInitialSideBrigades(final List<Brigade>[] initialSideBrigades) {
        this.initialSideBrigades = initialSideBrigades;
    }

    public IHalfRoundLog getFieldBattleLog() {
        return halfRoundLog;
    }

    public FieldBattleVisibilityProcessor getFieldBattleVisibilityProcessor() {
        return fieldBattleVisibilityProcessor;
    }

    public void setFieldBattleVisibilityProcessor(final FieldBattleVisibilityProcessor fieldBattleVisibilityProcessor) {
        this.fieldBattleVisibilityProcessor = fieldBattleVisibilityProcessor;
    }

    public Set<FieldBattleSector> getSectors(final Set<Brigade> brigades) {
        final Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();
        for (Brigade brigade : brigades) {
            final FieldBattleSector sector = getSector(brigade);
            if (sector != null) {
                sectors.add(sector);
            }
        }
        return sectors;
    }

    public FieldBattleDetachmentProcessor getFieldBattleDetachmentProcessor() {
        return fieldBattleDetachmentProcessor;
    }

    public void setFieldBattleDetachmentProcessor(final FieldBattleDetachmentProcessor fieldBattleDetachmentProcessor) {
        this.fieldBattleDetachmentProcessor = fieldBattleDetachmentProcessor;
    }

    public void setHalfRoundLog(final IHalfRoundLog halfRoundLog) {
        this.halfRoundLog = halfRoundLog;
    }
}
