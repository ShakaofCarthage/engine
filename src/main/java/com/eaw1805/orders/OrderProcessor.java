package com.eaw1805.orders;

import com.eaw1805.battles.WarfareProcessor;
import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.battles.TacticalBattleReportManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.economy.GoodManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.fleet.FleetManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.fleet.ShipTypeManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.ProductionSiteManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.battles.TacticalBattleReport;
import com.eaw1805.data.model.comparators.NationWeight;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.economy.Good;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.fleet.Fleet;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.fleet.ShipType;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.orders.PatrolOrderDetails;
import com.eaw1805.economy.AbstractMaintenance;
import com.eaw1805.economy.trade.TradeProcessor;
import com.eaw1805.events.army.RemoveEmptyArmiesEvent;
import com.eaw1805.events.army.RemoveEmptyCorpsEvent;
import com.eaw1805.events.fleet.DestroyShipsEvent;
import com.eaw1805.orders.army.*;
import com.eaw1805.orders.economy.BuildBaggageTrain;
import com.eaw1805.orders.economy.ChangeTaxation;
import com.eaw1805.orders.economy.RepairBaggageTrain;
import com.eaw1805.orders.economy.ScuttleBaggageTrain;
import com.eaw1805.orders.economy.TransferFirst;
import com.eaw1805.orders.economy.TransferSecond;
import com.eaw1805.orders.fleet.BuildShip;
import com.eaw1805.orders.fleet.DemolishFleet;
import com.eaw1805.orders.fleet.HandOverShip;
import com.eaw1805.orders.fleet.LoadFirst;
import com.eaw1805.orders.fleet.LoadSecond;
import com.eaw1805.orders.fleet.RenameFleet;
import com.eaw1805.orders.fleet.RenameShip;
import com.eaw1805.orders.fleet.RepairFleet;
import com.eaw1805.orders.fleet.RepairShip;
import com.eaw1805.orders.fleet.ScuttleShip;
import com.eaw1805.orders.fleet.SetupFleet;
import com.eaw1805.orders.fleet.ShipJoinFleet;
import com.eaw1805.orders.fleet.UnloadFirst;
import com.eaw1805.orders.fleet.UnloadSecond;
import com.eaw1805.orders.map.BuildProductionSite;
import com.eaw1805.orders.map.DecreasePopDensity;
import com.eaw1805.orders.map.DemolishProductionSite;
import com.eaw1805.orders.map.HandOverTerritoryOrderProcessor;
import com.eaw1805.orders.map.IncreasePopDensity;
import com.eaw1805.orders.movement.ArmyForcedMovement;
import com.eaw1805.orders.movement.ArmyMovement;
import com.eaw1805.orders.movement.BaggageTrainMovement;
import com.eaw1805.orders.movement.BrigadeForcedMovement;
import com.eaw1805.orders.movement.BrigadeMovement;
import com.eaw1805.orders.movement.CommanderMovement;
import com.eaw1805.orders.movement.CorpForcedMovement;
import com.eaw1805.orders.movement.CorpMovement;
import com.eaw1805.orders.movement.FleetMovement;
import com.eaw1805.orders.movement.FleetPatrolMovement;
import com.eaw1805.orders.movement.MerchantShipMovement;
import com.eaw1805.orders.movement.ShipPatrolMovement;
import com.eaw1805.orders.movement.SpyMovement;
import com.eaw1805.orders.movement.WarShipMovement;
import com.eaw1805.orders.politics.PoliticsOrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for processing the orders of the turn.
 */
public class OrderProcessor
        extends AbstractMaintenance
        implements OrderConstants, ProductionSiteConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(OrderProcessor.class);

    /**
     * PlayerOrder Processors place holder.
     */
    private final transient Map<Integer, OrderInterface> ORDER_PROCESSORS = new HashMap<Integer, OrderInterface>();

    /**
     * Used to associate DB Ids with UI Ids for newly setup Corps.
     */
    private final transient Map<Integer, Integer> mapCorpIds;

    /**
     * Used to associate DB Ids with UI Ids for newly setup Armies.
     */
    private final transient Map<Integer, Integer> mapArmyIds;

    /**
     * Used to associate DB Ids with UI Ids for newly setup Fleets.
     */
    private final transient Map<Integer, Integer> mapFleetIds;

    /**
     * Used to store all sectors that will be conquered at the end of the order processing.
     */
    private final transient Set<Sector> conqueredSectors;

    /**
     * Used to list all fleets that may attacked by pirates (i.e., with less than 2 warships).
     */
    private final transient Map<Fleet, Set<Sector>> piratesTargetFlt;

    /**
     * Used to list all the ships that may be attacked by pirates.
     */
    private final transient Map<Ship, Set<Sector>> piratesTargetShp;

    /**
     * Used to store all declarations of war so that target can call for allies.
     */
    private final transient Set<NationsRelation> callToAllies;

    /**
     * Keep track of the profits of each nation.
     */
    private final transient Map<Nation, Integer> tradeProfits;

    private final NumberFormat formatter = new DecimalFormat("#,###,###");

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public OrderProcessor(final GameEngine caller) {
        super(caller);

        // Setup hash maps.
        mapCorpIds = new HashMap<Integer, Integer>();
        mapArmyIds = new HashMap<Integer, Integer>();
        mapFleetIds = new HashMap<Integer, Integer>();
        conqueredSectors = new HashSet<Sector>();
        piratesTargetFlt = new HashMap<Fleet, Set<Sector>>();
        piratesTargetShp = new HashMap<Ship, Set<Sector>>();
        callToAllies = new HashSet<NationsRelation>();
        tradeProfits = new HashMap<Nation, Integer>();

        // Start transaction
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(caller.getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Brigades
        ORDER_PROCESSORS.put(AdditionalBattalions.ORDER_TYPE, new AdditionalBattalions(this));
        ORDER_PROCESSORS.put(BrigadeJoinCorp.ORDER_TYPE, new BrigadeJoinCorp(this));
        ORDER_PROCESSORS.put(DemolishBattalion.ORDER_TYPE, new DemolishBattalion(this));
        ORDER_PROCESSORS.put(DemolishBrigade.ORDER_TYPE, new DemolishBrigade(this));
        ORDER_PROCESSORS.put(ExchangeBattalions.ORDER_TYPE, new ExchangeBattalions(this));
        ORDER_PROCESSORS.put(IncreaseExperience.ORDER_TYPE, new IncreaseExperience(this));
        ORDER_PROCESSORS.put(IncreaseHeadcount.ORDER_TYPE, new IncreaseHeadcount(this));
        ORDER_PROCESSORS.put(MergeBattalions.ORDER_TYPE, new MergeBattalions(this));
        ORDER_PROCESSORS.put(RenameBrigade.ORDER_TYPE, new RenameBrigade(this));
        ORDER_PROCESSORS.put(SetupBrigade.ORDER_TYPE, new SetupBrigade(this));

        // Corps
        ORDER_PROCESSORS.put(CommanderJoinCorp.ORDER_TYPE, new CommanderJoinCorp(this));
        ORDER_PROCESSORS.put(CorpJoinArmy.ORDER_TYPE, new CorpJoinArmy(this));
        ORDER_PROCESSORS.put(DemolishCorp.ORDER_TYPE, new DemolishCorp(this));
        ORDER_PROCESSORS.put(RenameCorp.ORDER_TYPE, new RenameCorp(this));
        ORDER_PROCESSORS.put(SetupCorp.ORDER_TYPE, new SetupCorp(this));
        ORDER_PROCESSORS.put(IncreaseHeadcountCorps.ORDER_TYPE, new IncreaseHeadcountCorps(this));
        ORDER_PROCESSORS.put(IncreaseExperienceCorps.ORDER_TYPE, new IncreaseExperienceCorps(this));
        // Armies
        ORDER_PROCESSORS.put(CommanderJoinArmy.ORDER_TYPE, new CommanderJoinArmy(this));
        ORDER_PROCESSORS.put(DemolishArmy.ORDER_TYPE, new DemolishArmy(this));
        ORDER_PROCESSORS.put(RenameArmy.ORDER_TYPE, new RenameArmy(this));
        ORDER_PROCESSORS.put(SetupArmy.ORDER_TYPE, new SetupArmy(this));
        ORDER_PROCESSORS.put(IncreaseHeadcountArmy.ORDER_TYPE, new IncreaseHeadcountArmy(this));
        ORDER_PROCESSORS.put(IncreaseExperienceArmy.ORDER_TYPE, new IncreaseExperienceArmy(this));

        // Commanders
        ORDER_PROCESSORS.put(RenameCommander.ORDER_TYPE, new RenameCommander(this));
        ORDER_PROCESSORS.put(DismissCommander.ORDER_TYPE, new DismissCommander(this));
        ORDER_PROCESSORS.put(HireCommander.ORDER_TYPE, new HireCommander(this));
        ORDER_PROCESSORS.put(CommanderLeaveUnit.ORDER_TYPE, new CommanderLeaveUnit(this));

        // Fleets
        ORDER_PROCESSORS.put(BuildShip.ORDER_TYPE, new BuildShip(this));
        ORDER_PROCESSORS.put(RepairShip.ORDER_TYPE, new RepairShip(this));
        ORDER_PROCESSORS.put(ScuttleShip.ORDER_TYPE, new ScuttleShip(this));
        ORDER_PROCESSORS.put(ShipJoinFleet.ORDER_TYPE, new ShipJoinFleet(this));
        ORDER_PROCESSORS.put(DemolishFleet.ORDER_TYPE, new DemolishFleet(this));
        ORDER_PROCESSORS.put(RenameShip.ORDER_TYPE, new RenameShip(this));
        ORDER_PROCESSORS.put(SetupFleet.ORDER_TYPE, new SetupFleet(this));
        ORDER_PROCESSORS.put(RepairFleet.ORDER_TYPE, new RepairFleet(this));
        ORDER_PROCESSORS.put(LoadFirst.ORDER_TYPE, new LoadFirst(this));
        ORDER_PROCESSORS.put(LoadSecond.ORDER_TYPE, new LoadSecond(this));
        ORDER_PROCESSORS.put(UnloadFirst.ORDER_TYPE, new UnloadFirst(this));
        ORDER_PROCESSORS.put(UnloadSecond.ORDER_TYPE, new UnloadSecond(this));
        ORDER_PROCESSORS.put(RenameFleet.ORDER_TYPE, new RenameFleet(this));

        // Economy
        ORDER_PROCESSORS.put(ChangeTaxation.ORDER_TYPE, new ChangeTaxation(this));
        ORDER_PROCESSORS.put(BuildBaggageTrain.ORDER_TYPE, new BuildBaggageTrain(this));
        ORDER_PROCESSORS.put(RepairBaggageTrain.ORDER_TYPE, new RepairBaggageTrain(this));
        ORDER_PROCESSORS.put(ScuttleBaggageTrain.ORDER_TYPE, new ScuttleBaggageTrain(this));
        ORDER_PROCESSORS.put(TransferFirst.ORDER_TYPE, new TransferFirst(this));
        ORDER_PROCESSORS.put(TransferSecond.ORDER_TYPE, new TransferSecond(this));

        // Map
        ORDER_PROCESSORS.put(BuildProductionSite.ORDER_TYPE, new BuildProductionSite(this));
        ORDER_PROCESSORS.put(DemolishProductionSite.ORDER_TYPE, new DemolishProductionSite(this));
        ORDER_PROCESSORS.put(IncreasePopDensity.ORDER_TYPE, new IncreasePopDensity(this));
        ORDER_PROCESSORS.put(DecreasePopDensity.ORDER_TYPE, new DecreasePopDensity(this));
        ORDER_PROCESSORS.put(HandOverTerritoryOrderProcessor.ORDER_TYPE, new HandOverTerritoryOrderProcessor(this));
        ORDER_PROCESSORS.put(HandOverShip.ORDER_TYPE, new HandOverShip(this));
        ORDER_PROCESSORS.put(RenameBarrack.ORDER_TYPE, new RenameBarrack(this));

        // Movement
        ORDER_PROCESSORS.put(BrigadeMovement.ORDER_TYPE, new BrigadeMovement(this));
        ORDER_PROCESSORS.put(BrigadeForcedMovement.ORDER_TYPE, new BrigadeForcedMovement(this));
        ORDER_PROCESSORS.put(CorpMovement.ORDER_TYPE, new CorpMovement(this));
        ORDER_PROCESSORS.put(CorpForcedMovement.ORDER_TYPE, new CorpForcedMovement(this));
        ORDER_PROCESSORS.put(ArmyMovement.ORDER_TYPE, new ArmyMovement(this));
        ORDER_PROCESSORS.put(ArmyForcedMovement.ORDER_TYPE, new ArmyForcedMovement(this));
        ORDER_PROCESSORS.put(CommanderMovement.ORDER_TYPE, new CommanderMovement(this));
        ORDER_PROCESSORS.put(SpyMovement.ORDER_TYPE, new SpyMovement(this));
        ORDER_PROCESSORS.put(BaggageTrainMovement.ORDER_TYPE, new BaggageTrainMovement(this));
        ORDER_PROCESSORS.put(MerchantShipMovement.ORDER_TYPE, new MerchantShipMovement(this));
        ORDER_PROCESSORS.put(WarShipMovement.ORDER_TYPE, new WarShipMovement(this));
        ORDER_PROCESSORS.put(FleetMovement.ORDER_TYPE, new FleetMovement(this));
        ORDER_PROCESSORS.put(ShipPatrolMovement.ORDER_TYPE, new ShipPatrolMovement(this));
        ORDER_PROCESSORS.put(FleetPatrolMovement.ORDER_TYPE, new FleetPatrolMovement(this));

        // Politics
        ORDER_PROCESSORS.put(PoliticsOrderProcessor.ORDER_TYPE, new PoliticsOrderProcessor(this));

        theTrans.commit();
        LOGGER.info("OrderProcessor instantiated.");
    }

    /**
     * Process player orders.
     */
    public int process() {
        // Fetch list of orders for particular type
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        PlayerOrderManager.getInstance().resetOrderResult(getGame().getGameId(), getGame().getTurn());
        final List<PlayerOrder> lstOrders = decideOrderOfExecution(PlayerOrderManager.getInstance().listByGame(getGame()));

        boolean checkCorps = true;
        boolean processedBattles = false;
        boolean firstTradePhase = false;
        boolean secondTradePhase = false;
        int counter = 1;

        final Region defaultRegion = RegionManager.getInstance().getByID(RegionConstants.EUROPE);
        theTrans.commit();

        // Iterate through all orders issued for this turn
        for (final PlayerOrder thisOrder : lstOrders) {
            // after execution of brigades join corps check corps sizes
            if (checkCorps && thisOrder.getType() > ORDER_ADDTO_CORP) {
                checkCorps = false;
                Transaction cleanupTP = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
                checkCorpsSizes();
                cleanupTP.commit();
            }

            // execution of battle events within the player order sequence
            if (!processedBattles && thisOrder.getType() > ORDER_P_FLEET) {
                // Resolve Battles
                final WarfareProcessor thisWP = new WarfareProcessor(getGameEngine(), this);
                thisWP.process();
                processedBattles = true;

                // Check for piracy random event
                randomEventPirates(thisWP);
            }

            // execution of trading events within the player order sequence
            if (!firstTradePhase && thisOrder.getType() > ORDER_EXCHF) {
                // First do cleanup
                Transaction cleanupTP = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
                final RemoveEmptyArmiesEvent reaEvent = new RemoveEmptyArmiesEvent(getGame());
                reaEvent.process();

                final RemoveEmptyCorpsEvent recEvent = new RemoveEmptyCorpsEvent(getGame());
                recEvent.process();

                final DestroyShipsEvent dsEvent = new DestroyShipsEvent(getGame(), getRandomGen());
                dsEvent.process();
                cleanupTP.commit();

                // Resolve 1st Trade phase
                final TradeProcessor thisTP = new TradeProcessor(getGameEngine(), 1);
                thisTP.process();
                firstTradePhase = true;
            }

            // execution of trading events within the player order sequence
            if (!secondTradePhase && thisOrder.getType() > ORDER_EXCHS) {
                // Resolve 2nd Trade phase
                final TradeProcessor thisTP = new TradeProcessor(getGameEngine(), 2);
                thisTP.process();
                secondTradePhase = true;

                final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
                // Store trading statistics of players
                for (Map.Entry<Nation, Integer> entry : tradeProfits.entrySet()) {
                    if (entry.getValue() > 0) {
                        // keep statistics for trading
                        report(entry.getKey(), TRADE_TOT_MONEY, entry.getValue());

                        maxProfile(getGame(), entry.getKey(), ProfileConstants.TRADE_HIGHEST, entry.getValue());
                        // Add news entry
                        newsSingle(entry.getKey(), NEWS_ECONOMY,
                                "Our capable traders managed to bargain and make trade deals for total profits of " + formatter.format(entry.getValue()) + "&nbsp;<img src='http://static.eaw1805.com/images/goods/good-1.png' height=18 title='Money' border=0 style='vertical-align: middle;'>");
                    }
                }
                trans.commit();
            }

            // Fetch order processor
            final OrderInterface orderProc;

            if (thisOrder.getType() == ORDER_M_UNIT) {
                // Movement orders are grouped together
                // Corresponding order processor is indicated by 5th parameter
                final int subType = Integer.parseInt(thisOrder.getParameter5());
                orderProc = ORDER_PROCESSORS.get(subType);

            } else {
                // All other orders are executed properly by the corresponding processor
                orderProc = ORDER_PROCESSORS.get(thisOrder.getType());
            }

            if (orderProc == null) {
                LOGGER.error("Undefined order type [" + thisOrder.getType() + "]");
                continue;
            }

            // begin transaction
            final Session session = HibernateUtil.getInstance().getSessionFactory(getGame().getScenarioId()).getCurrentSession();
            final Transaction trans = session.beginTransaction();

            thisOrder.setRegion(defaultRegion);
            thisOrder.setProcOrder(counter++);

            orderProc.setOrder(thisOrder);
            orderProc.process();

            LOGGER.info(orderProc.getClass().getName() + " [OrderID:" + thisOrder.getOrderId()
                    + ", Pos=" + thisOrder.getProcOrder()
                    + ", Owner=" + thisOrder.getNation().getName()
                    + ", Result=" + thisOrder.getResultStr()
                    + ", Explanation=" + thisOrder.getExplanation() + "]");

            // Persist changes to db.
            PlayerOrderManager.getInstance().update(thisOrder);
            trans.commit();
            if (session.isOpen()) {
                session.close();
                LOGGER.error("Session not closed automatically");
            }
        }

        // execution of battle events without any player orders
        if (!processedBattles) {
            // Resolve Battles
            final WarfareProcessor thisWP = new WarfareProcessor(getGameEngine(), this);
            thisWP.process();

            // Check for piracy random event
            randomEventPirates(thisWP);
        }

        // reload relations map
        final Transaction reloadTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        ((PoliticsOrderProcessor) ORDER_PROCESSORS.get(PoliticsOrderProcessor.ORDER_TYPE)).reloadRelations();
        reloadTrans.commit();

        // hand over sectors
        handoverSectors();

        // Persist Successful Patrol Orders
        persistPatrolOrders();

        // Make sure that the trading processor is invoked at least once per turn
        if (!firstTradePhase && !secondTradePhase) {
            final TradeProcessor thisTP = new TradeProcessor(getGameEngine(), 1);
            thisTP.process();
        }

        // execution of call to allies within the player order sequence
        final Transaction anotherTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        respondToCallFromAllies();
        issueCallToAllies();
        anotherTrans.commit();

        // Save changes to the warehouses.
        saveData();

        LOGGER.info("OrderProcessor completed.");
        return counter;
    }

    /**
     * Enforce corps sizes.
     */
    private void checkCorpsSizes() {
        final List<Corp> lstCorps = CorpManager.getInstance().listGame(getGame());
        for (final Corp corps : lstCorps) {
            final boolean isMuslim = (corps.getNation().getId() == NATION_MOROCCO || corps.getNation().getId() == NATION_OTTOMAN || corps.getNation().getId() == NATION_EGYPT);
            final int limit = isMuslim ? 20 : 16;

            // check that corps has enough space
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getGame(), corps.getCorpId());
            if (lstBrigades.size() > limit) {
                int diff = lstBrigades.size() - limit;
                // drop excessive brigades
                for (final Brigade brigade : lstBrigades) {
                    brigade.setCorp(0);
                    BrigadeManager.getInstance().update(brigade);

                    corps.getBrigades().remove(brigade);

                    diff--;
                    if (diff <= 0) {
                        break;
                    }
                }

                CorpManager.getInstance().update(corps);
            }
        }
    }

    /**
     * Execute all call to allies based on the wars declared during this turn.
     */
    private void issueCallToAllies() {
        for (final NationsRelation declaration : callToAllies) {
            // Target will Call allies
            final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(getGame(), declaration.getTarget());
            for (final NationsRelation thisRelation : lstRelations) {
                if (thisRelation.getRelation() == REL_ALLIANCE) {
                    // Make sure ally is still alive
                    if (getGameEngine().isAlive(thisRelation.getTarget())) {
                        // Check if ally is already at war with nation
                        final NationsRelation theirRelation = RelationsManager.getInstance().getByNations(getGame(),
                                thisRelation.getTarget(), declaration.getNation());

                        if (theirRelation.getRelation() != REL_WAR
                                && theirRelation.getPeaceCount() == 0
                                && theirRelation.getSurrenderCount() == 0) {
                            newsGlobal(declaration.getTarget(), thisRelation.getTarget(), NEWS_POLITICAL,
                                    "We called our ally " + thisRelation.getTarget().getName() + " to join our war against " + declaration.getNation().getName(),
                                    declaration.getTarget().getName() + " called us to join the war against " + declaration.getNation().getName() + ". We have to decide this month if we will join forces or our alliance will break!",
                                    declaration.getTarget().getName() + " called their ally " + thisRelation.getTarget().getName() + " to join the war against " + declaration.getNation().getName() + ". " + thisRelation.getTarget().getName() + " will have to decide this month if they will join forces or the alliance will break!");

                            // Add an entry
                            report(thisRelation.getTarget(), "callallies." + declaration.getNation().getId(), Integer.toString(declaration.getTarget().getId()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Examine if calls from allies have been responsed properly, or otherwise break alliances.
     */
    private void respondToCallFromAllies() {
        // Retrieve all call to allies
        final List<Report> lstReports = ReportManager.getInstance().listByTurnKey(getGame(),
                getGame().getTurn() - 1,
                "callallies.");

        for (final Report report : lstReports) {
            // Identify target of call to allies
            final Nation target = report.getNation();

            // Identify enemy nation
            final String enemyId = report.getKey().substring(report.getKey().lastIndexOf(".") + 1);
            final Nation enemy = NationManager.getInstance().getByID(Integer.parseInt(enemyId));

            // Identify caller nation
            final Nation caller = NationManager.getInstance().getByID(Integer.parseInt(report.getValue()));

            // check that nation are still allies
            final NationsRelation allianceRelation = RelationsManager.getInstance().getByNations(getGame(),
                    target, caller);

            if (allianceRelation.getRelation() == REL_ALLIANCE) {
                // Make sure ally is still alive
                if (getGameEngine().isAlive(target)) {
                    // Check relations
                    final NationsRelation theirRelation = RelationsManager.getInstance().getByNations(getGame(),
                            target, enemy);
                    if (theirRelation.getRelation() != REL_WAR
                            && theirRelation.getPeaceCount() == 0
                            && theirRelation.getSurrenderCount() == 0) {

                        // Fixed alliances cannot break
                        if (allianceRelation.getFixed()) {

                            // Refusing a call to allies
                            newsGlobal(target, caller, NEWS_POLITICAL,
                                    "We refused the call of our ally " + caller.getName() + " to join war against " + enemy.getName() + ". Our alliance with " + caller.getName() + " is not affected !",
                                    target.getName() + " refused our call to join the war against " + enemy.getName() + ". Our alliance with " + target.getName() + " is not affected !",
                                    target.getName() + " refused the call of " + caller.getName() + " to join the war against " + enemy.getName() + ". The alliance of " + target.getName() + " with " + caller.getName() + " is still in full effect !");

                        } else {
                            // Refusing a call to allies
                            newsGlobal(target, caller, NEWS_POLITICAL,
                                    "We refused the call of our ally " + caller.getName() + " to join war against " + enemy.getName() + ". As a result, our alliance with " + caller.getName() + " is broken !",
                                    target.getName() + " refused our call to join the war against " + enemy.getName() + ". As a result, our alliance with " + target.getName() + " is broken !",
                                    target.getName() + " refused the call of " + caller.getName() + " to join the war against " + enemy.getName() + ". As a result, the alliance of " + target.getName() + " with " + caller.getName() + " is broken !");
                        }

                        // Target loses 6 VPs
                        changeVP(getGame(), target, POLITICS_WAR_CALL_REFUSE, "Refusing a call to Allies");

                        // Modify player's profile
                        changeProfile(getGame(), target, ProfileConstants.REFUSE_CALLALLIES, 1);

                        // Update achievements
                        achievementsRejectCall(getGame(), target);

                        // Fixed alliances cannot break
                        if (!allianceRelation.getFixed()) {
                            // Alliance is broken
                            final NationsRelation thisRelation = RelationsManager.getInstance().getByNations(getGame(),
                                    target, caller);
                            thisRelation.setRelation(REL_PASSAGE);
                            RelationsManager.getInstance().update(thisRelation);

                            final NationsRelation thisRelationReverse = RelationsManager.getInstance().getByNations(getGame(),
                                    caller, target);
                            thisRelationReverse.setRelation(REL_PASSAGE);
                            RelationsManager.getInstance().update(thisRelationReverse);

                            // Unload loaded units
                            unloadUnits(getGame(), target, caller);
                            unloadUnits(getGame(), caller, target);
                        }
                    }
                }
            }
        }
    }

    private List<PlayerOrder> decideOrderOfExecution(final List<PlayerOrder> lstOrders) {
        final List<List<PlayerOrder>> orderOfExecution = new ArrayList<List<PlayerOrder>>();
        final List<Nation> lstNation = getGameEngine().getAliveNations();

        // Iterate through orders
        int orderType = 0;
        int orderPosition = 0;
        List<PlayerOrder> subList = new ArrayList<PlayerOrder>();
        for (final PlayerOrder order : lstOrders) {
            // Check if we have finished with this set of orders
            if (order.getType() != orderType || order.getPosition() != orderPosition) {
                if (!subList.isEmpty()) {
                    // Roll dices for each nation
                    for (final Nation nation : lstNation) {
                        nation.setWeight(getRandomGen().nextInt(101) + nation.getId());
                    }

                    // Sort orders based on rolls
                    java.util.Collections.sort(subList, new NationWeight(lstNation));

                    orderOfExecution.add(subList);
                    subList = new ArrayList<PlayerOrder>();
                }

                orderType = order.getType();
                orderPosition = order.getPosition();
            }

            subList.add(order);
        }

        // Add last order
        if (!subList.isEmpty()) {
            // Roll dices for each nation
            for (final Nation nation : lstNation) {
                nation.setWeight(getRandomGen().nextInt(101) + nation.getId());
            }

            // Sort orders based on rolls
            java.util.Collections.sort(subList, new NationWeight(lstNation));

            orderOfExecution.add(subList);
        }

        LOGGER.info("Sequence of Orders Execution");
        final StringBuilder debugStr = new StringBuilder();

        // generate final list of orders
        final List<PlayerOrder> finalList = new ArrayList<PlayerOrder>();
        for (List<PlayerOrder> orderList : orderOfExecution) {
            debugStr.append("|");
            for (final PlayerOrder playerOrder : orderList) {
                debugStr.append(playerOrder.getNation().getCode());
                finalList.add(playerOrder);
            }
        }

        if (debugStr.length() > 0) {
            LOGGER.info(debugStr.substring(1));
        } else {
            LOGGER.info("No orders received (!)");
        }

        return finalList;
    }

    /**
     * Persist successful patrol orders.
     */
    private void persistPatrolOrders() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final Set<Nation> aliveNations = new HashSet<Nation>(getGameEngine().getAliveNations());
        for (final PatrolOrderDetails orderDetails : getPatrolOrders().values()) {
            if (orderDetails.getTonnage() > 0) {
                final PlayerOrder order = PlayerOrderManager.getInstance().getByID(orderDetails.getOrderId());
                final PlayerOrder newOrder = new PlayerOrder();

                // Only persist orders for alive nations
                if (aliveNations.contains(order.getNation())) {
                    newOrder.setType(order.getType());
                    newOrder.setPosition(1);
                    newOrder.setGame(order.getGame());
                    newOrder.setNation(order.getNation());
                    newOrder.setRegion(order.getRegion());
                    newOrder.setTurn(order.getTurn() + 1);
                    newOrder.setParameter1(order.getParameter1());
                    newOrder.setParameter2(order.getParameter2());
                    newOrder.setParameter3(order.getParameter3());
                    newOrder.setParameter4(order.getParameter4());
                    newOrder.setParameter5(order.getParameter5());
                    newOrder.setParameter6(order.getParameter6());
                    newOrder.setParameter7(order.getParameter7());
                    newOrder.setParameter8(order.getParameter8());
                    newOrder.setParameter9(order.getParameter9());
                    newOrder.setTemp1(order.getTemp1());
                    newOrder.setTemp2(order.getTemp2());
                    newOrder.setTemp3(order.getTemp3());
                    newOrder.setTemp4(order.getTemp4());
                    newOrder.setTemp5(order.getTemp5());
                    newOrder.setTemp6(order.getTemp6());
                    newOrder.setTemp7(order.getTemp7());
                    newOrder.setTemp8(order.getTemp8());
                    newOrder.setTemp9(order.getTemp9());
                    PlayerOrderManager.getInstance().add(newOrder);
                }
            }
        }
        theTrans.commit();
    }

    /**
     * Examine sectors and hand them over to conqueror
     */
    private void handoverSectors() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final Map<Integer, Integer> theCapitols = retrieveCapitalOwners();
        for (final Sector theSector : conqueredSectors) {
            final Sector sector = SectorManager.getInstance().getByID(theSector.getId());
            final Nation conquerer = sector.getTempNation();
            if (sector.getNation().getId() == NATION_NEUTRAL) {
                sector.setConqueredCounter(0);
                sector.setConquered(true);
                LOGGER.info("Neutral " + sector.getPosition() + " conquered by " + sector.getTempNation().getName());

            } else if (sector.getTempNation().getId() == sector.getNation().getId()) {
                sector.setConqueredCounter(0);
                LOGGER.info("Sector " + sector.getPosition() + " was defended and remained under the control of " + sector.getNation().getName());

            } else {
                // Check if this is the home region of the nation that conquered the sector
                final Position origPos = (Position) sector.getPosition().clone();
                origPos.setGame(GameManager.getInstance().getByID(-1));
                final Sector originalSector = SectorManager.getInstance().getByPosition(origPos);

                sector.setConquered(true);

                if (sector.getPosition().getRegion().getId() == EUROPE
                        && originalSector.getNation().getId() == sector.getTempNation().getId()) {
                    sector.setConqueredCounter(0);
                    LOGGER.info("Sector " + sector.getPosition() + " owned by " + sector.getNation().getName() + " was re-conquered by " + sector.getTempNation().getName() + " (home nation)");

                } else {
                    // Check if new owner is ally with home nation
                    final NationsRelation relationWithHome = RelationsManager.getInstance().getByNations(getGame(), originalSector.getNation(), sector.getTempNation());
                    if (sector.getPosition().getRegion().getId() == EUROPE
                            && relationWithHome != null
                            && relationWithHome.getRelation() == REL_ALLIANCE) {
                        // Ally is freeing the territory - not conquering
                        sector.setConqueredCounter(0);

                        LOGGER.info("Sector " + sector.getPosition() + " owned by " + sector.getNation().getName() + " was re-conquered by " + sector.getTempNation().getName() + " for the ally " + originalSector.getNation().getName() + " (home nation).");

                        sector.setTempNation(originalSector.getNation());

                    } else {
                        sector.setConqueredCounter(6);
                        LOGGER.info("Sector " + sector.getPosition() + " owned by " + sector.getNation().getName() + " was conquered by " + sector.getTempNation().getName());
                    }
                }
            }

            // check if this is a trade city
            final TradeCity tcity = TradeCityManager.getInstance().getByPosition(sector.getPosition());
            if (tcity != null) {
                // Check if this is a capitol
                if (tcity.getNation().getId() != NATION_NEUTRAL
                        && sector.getPosition().getRegion().getId() == EUROPE
                        && theCapitols.containsKey(tcity.getCityId())) {
                    if (theCapitols.get(tcity.getCityId()) == sector.getTempNation().getId()) {
                        // Plunder warehouse
                        final StringBuilder strMaterials = new StringBuilder();
                        if (tcity.getNation().getId() != NATION_NEUTRAL) {
                            plunderWarehouse(sector, strMaterials);

                            // Remove last comma
                            if (strMaterials.length() > 0) {
                                strMaterials.delete(strMaterials.length() - 2, strMaterials.length());

                            } else {
                                strMaterials.append(" nothing! The warehouses were empty !");
                            }
                        }

                        // This used to be the capital, we are regaining
                        if (conquerer.getId() == sector.getTempNation().getId()) {
                            changeVP(sector.getPosition().getGame(), sector.getTempNation(), RECONQUER_CAPITAL, "Regained our capital at " + sector.getPosition().toString());

                        } else {
                            changeVP(sector.getPosition().getGame(), sector.getTempNation(), RECONQUER_CAPITAL, "Freed the capital of our ally " + sector.getTempNation().getName() + " at " + sector.getPosition().toString());
                        }

                        changeVP(sector.getPosition().getGame(), sector.getNation(), LOSE_F_CAPITAL, "Lost Foreign Capital at " + sector.getPosition().toString());

                        // broadcasting news
                        if (conquerer.getId() == sector.getTempNation().getId()) {
                            newsGlobal(sector.getTempNation(), sector.getNation(), NEWS_MILITARY,
                                    "We regained our capital at " + sector.getPosition().toString() + " from " + sector.getNation().getName() + ". We managed to plunder from the enemy's warehouse the following materials: " + strMaterials.toString(),
                                    sector.getTempNation().getName() + " regained their capital at " + sector.getPosition().toString() + ". The enemy managed to plunder from our warehouse the following materials: " + strMaterials.toString(),
                                    sector.getTempNation().getName() + " regained their capital at " + sector.getPosition().toString() + " from " + sector.getNation().getName());

                        } else {
                            newsGlobal(conquerer, sector.getNation(), NEWS_MILITARY,
                                    "Freed the capital of our ally " + sector.getTempNation().getName() + " at " + sector.getPosition().toString() + " from " + sector.getNation().getName() + ". " + theSector.getTempNation().getName() + " managed to plunder from the enemy's warehouse the following materials: " + strMaterials.toString(),
                                    conquerer.getName() + " freed the capital of " + sector.getTempNation().getName() + " at " + sector.getPosition().toString() + ". The enemy managed to plunder from our warehouse the following materials: " + strMaterials.toString(),
                                    conquerer.getName() + " freed the capital of " + sector.getTempNation().getName() + " at " + sector.getPosition().toString() + " from " + sector.getNation().getName());

                            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, 0, "Our capital was freed by our ally " + conquerer.getName() + ". We managed to plunder from the enemy's warehouse the following materials: " + strMaterials.toString());

                            // award VPs for conquering multiple capitals
                            awardCapitalConquers(theCapitols, sector, conquerer);
                        }

                        LOGGER.info(sector.getTempNation().getName() + " regained " + sector.getName() + " at " + sector.getPosition().toString() + " from " + sector.getNation().getName());

                    } else {
                        // Check if this is the capitol of the player
                        if (theCapitols.get(tcity.getCityId()) == sector.getNation().getId()) {
                            // Plunder warehouse
                            final StringBuilder strMaterials = new StringBuilder();
                            if (tcity.getNation().getId() != NATION_NEUTRAL) {
                                plunderWarehouse(sector, strMaterials);

                                // Remove last comma
                                if (strMaterials.length() > 0) {
                                    strMaterials.delete(strMaterials.length() - 2, strMaterials.length());

                                } else {
                                    strMaterials.append(" nothing! The warehouses were empty !");
                                }
                            }

                            // This is a capitol
                            changeVP(sector.getPosition().getGame(), sector.getTempNation(), CONQUER_CAPITAL, "Conquered Capital of " + sector.getNation().getName() + " at " + sector.getPosition().toString());
                            changeVP(sector.getPosition().getGame(), sector.getNation(), LOSE_CAPITAL, "Lost our Capital at " + sector.getPosition().toString());

                            // broadcasting news
                            newsGlobal(sector.getTempNation(), sector.getNation(), NEWS_MILITARY,
                                    "We conquered the capital of " + sector.getNation().getName() + " at " + sector.getPosition().toString() + ". We managed to plunder from the enemy's warehouse the following materials: " + strMaterials.toString(),
                                    "We lost our capital at " + sector.getPosition().toString() + " to " + sector.getTempNation().getName() + ". The enemy managed to plunder from our warehouse the following materials: " + strMaterials.toString(),
                                    sector.getTempNation().getName() + " conquered the capital of " + sector.getNation().getName() + " at " + sector.getPosition().toString());

                            // Modify player's profile
                            changeProfile(getGame(), sector.getTempNation(), ProfileConstants.CAPITAL_CONQUERED, 1);

                            // Update achievements
                            achievementsConquerCapitals(getGame(), sector.getTempNation());

                            LOGGER.info(sector.getTempNation().getName() + " conquered " + sector.getName() + " at " + sector.getPosition().toString() + ", the capital of " + sector.getNation().getName());

                        } else {
                            // Plunder warehouse
                            final StringBuilder strMaterials = new StringBuilder();
                            if (tcity.getNation().getId() != NATION_NEUTRAL) {
                                plunderWarehouse(sector, strMaterials);

                                // Remove last comma
                                if (strMaterials.length() > 0) {
                                    strMaterials.delete(strMaterials.length() - 2, strMaterials.length());

                                } else {
                                    strMaterials.append(" nothing! The warehouses were empty !");
                                }
                            }

                            changeVP(sector.getPosition().getGame(), sector.getTempNation(), CONQUER_CAPITAL, "Conquered a foreign capital at " + sector.getPosition().toString());
                            changeVP(sector.getPosition().getGame(), sector.getNation(), LOSE_F_CAPITAL, "Lost Foreign Capital at " + sector.getPosition().toString());

                            // broadcasting news
                            newsGlobal(sector.getTempNation(), sector.getNation(), NEWS_MILITARY,
                                    "We conquered " + sector.getName() + " at " + sector.getPosition().toString() + " from " + sector.getNation().getName() + ". We managed to plunder from the enemy's warehouse the following materials: " + strMaterials.toString(),
                                    "We lost " + sector.getName() + " at " + sector.getPosition().toString() + " to " + sector.getTempNation().getName() + ". The enemy managed to plunder from our warehouse the following materials: " + strMaterials.toString(),
                                    sector.getTempNation().getName() + " conquered " + sector.getName() + " at " + sector.getPosition().toString());

                            // Modify player's profile
                            changeProfile(getGame(), sector.getTempNation(), ProfileConstants.CAPITAL_CONQUERED, 1);

                            // Update achievements
                            achievementsConquerCapitals(getGame(), sector.getTempNation());

                            LOGGER.info(sector.getTempNation().getName() + " conquered " + sector.getName() + " at " + sector.getPosition().toString() + " from " + sector.getNation().getName());
                        }

                        // award VPs for conquering multiple capitals
                        awardCapitalConquers(theCapitols, sector, sector.getTempNation());
                    }

                } else {
                    // Plunder warehouse
                    final StringBuilder strMaterials = new StringBuilder();
                    if (tcity.getNation().getId() != NATION_NEUTRAL) {
                        plunderWarehouse(sector, strMaterials);

                        // Remove last comma
                        if (strMaterials.length() > 0) {
                            strMaterials.delete(strMaterials.length() - 2, strMaterials.length());

                        } else {
                            strMaterials.append(" nothing! The warehouses were empty !");
                        }
                    }

                    // this is just a trade city
                    changeVP(sector.getPosition().getGame(), sector.getTempNation(), CONQUER_TCITY, "Conquered Trade city at " + sector.getPosition().toString());
                    changeVP(sector.getPosition().getGame(), sector.getNation(), LOSE_TCITY, "Lost Trade city at " + sector.getPosition().toString());

                    // broadcasting news
                    newsGlobal(sector.getTempNation(), sector.getNation(), NEWS_MILITARY,
                            "We conquered the trade city " + tcity.getName() + " at " + sector.getPosition().toString() + " from " + sector.getNation().getName() + ". We managed to plunder from the enemy's warehouse the following materials: " + strMaterials.toString(),
                            "We lost the trade city " + tcity.getName() + " at " + sector.getPosition().toString() + " to " + sector.getNation().getName() + ". The enemy managed to plunder from our warehouse the following materials: " + strMaterials.toString(),
                            sector.getTempNation().getName() + " conquered the trade city " + tcity.getName() + " of " + sector.getNation().getName() + " at " + sector.getPosition().toString());

                    // Modify player's profile
                    changeProfile(getGame(), sector.getTempNation(), ProfileConstants.TCITY_CONQUERED, 1);

                    LOGGER.info(sector.getTempNation().getName() + " conquered Trade city at " + sector.getPosition().toString());
                }
            }

            // Check if sector contains a barrack
            if (sector.hasBarrack()) {
                LOGGER.info("Sector " + sector.getPosition() + " maintains a barrack/shipyard");

                // check if this is the first barrack in the colonies
                final int regionId = sector.getPosition().getRegion().getId();
                if (regionId != EUROPE) {
                    boolean foundMore = false;
                    final List<Barrack> lstBarracks = BarrackManager.getInstance().listByGameNation(getGame(), sector.getTempNation());
                    for (final Barrack barrack : lstBarracks) {
                        if (barrack.getPosition().getRegion().getId() == regionId) {
                            foundMore = true;
                        }
                    }

                    if (!foundMore) {
                        // Start up a colony
                        final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(sector.getTempNation(), getGame(), "colony." + regionId);
                        if (lstReports.isEmpty()) {
                            // We gain 8 VP
                            changeVP(getGame(), sector.getTempNation(), STARTUP_COLONY,
                                    "Start up a Colony in " + sector.getPosition().getRegion().getName());

                            report(sector.getTempNation(), "colony." + regionId, "1");

                            // Modify player's profile
                            changeProfile(getGame(), sector.getTempNation(), ProfileConstants.STARTUP_COLONY, 1);

                            // Update achievements
                            achievementsSetupColonies(getGame(), sector.getTempNation());
                        }
                    }
                }

                // locate barrack
                final Barrack thisBarr = BarrackManager.getInstance().getByPosition(sector.getPosition());

                try {
                    // Capture of Ships under construction
                    // or move outside of the port
                    destroyOrMoveShips(sector);

                    // Capture trains or move outside of the barrack
                    destroyOrMoveTrains(sector);

                } catch (Exception ex) {
                    // something very weird appeared - normally it should never enter here
                    LOGGER.error(ex, ex);
                }

                // If a country conquers a fortress, we need to check the level of the fortress at the start of the turn
                // A possible battle may degrade the level
                if (sector.getProductionSite() != null && sector.getProductionSite().getId() >= PS_BARRACKS_FS) {
                    // get current level
                    int fortLevel = sector.getProductionSite().getId();
                    if (fortLevel == PS_BARRACKS_FH) {
                        // Check if a complex production site is under construction
                        if (sector.getBuildProgress() > 0) {
                            // construction is disrupted
                            sector.setBuildProgress(0);
                            sector.setProductionSite(ProductionSiteManager.getInstance().getByID(PS_BARRACKS_FL));
                            fortLevel = PS_BARRACKS_FL;
                        }

                    } else {
                        // Check if a battle occurred
                        final TacticalBattleReport field = TacticalBattleReportManager.getInstance().getByPositionTurn(sector.getPosition(), getGame().getTurn());
                        if (field != null) {
                            // Check level of fortress at start of battle
                            if (field.getFort().indexOf("Huge") >= 0) {
                                fortLevel = PS_BARRACKS_FH;
                                LOGGER.info("Sector maintained a huge fortress that was degraded due to a siege.");

                            } else if (field.getFort().indexOf("Large") >= 0) {
                                fortLevel = PS_BARRACKS_FL;
                                LOGGER.info("Sector maintained a large fortress that was degraded due to a siege.");
                            }
                        }
                    }

                    // if a country conquers a Huge fortress, it will win 4 victory points.
                    if (fortLevel == PS_BARRACKS_FH) {
                        changeVP(sector.getPosition().getGame(), sector.getTempNation(), CONQUER_HUGE, "Conquered Huge fortress at " + sector.getPosition().toString());
                        changeVP(sector.getPosition().getGame(), sector.getNation(), LOSE_HUGE, "Lost Huge fortress at " + sector.getPosition().toString());

                        // broadcasting news
                        newsGlobal(sector.getTempNation(), sector.getNation(), NEWS_MILITARY,
                                "We conquered the huge fortress of " + sector.getNation().getName() + " at " + sector.getPosition().toString(),
                                "We lost our huge fortress at " + sector.getPosition().toString() + " to " + sector.getTempNation().getName(),
                                sector.getTempNation().getName() + " conquered the huge fortress of " + sector.getNation().getName() + " at " + sector.getPosition().toString());

                        // Modify player's profile
                        changeProfile(getGame(), sector.getTempNation(), ProfileConstants.FORTRESS_CONQUERED, 1);

                        // Update achievements
                        achievementsConquerHuge(getGame(), sector.getTempNation());

                    } else if (fortLevel == PS_BARRACKS_FL) {
                        // if a country conquers a Large fortress, it will win 2 victory points.
                        changeVP(sector.getPosition().getGame(), sector.getTempNation(), CONQUER_LARGE, "Conquered Large fortress at " + sector.getPosition().toString());
                        changeVP(sector.getPosition().getGame(), sector.getNation(), LOSE_LARGE, "Lost Large fortress at " + sector.getPosition().toString());

                        // broadcasting news
                        newsGlobal(sector.getTempNation(), sector.getNation(), NEWS_MILITARY,
                                "We conquered the large fortress of " + sector.getNation().getName() + " at " + sector.getPosition().toString(),
                                "We lost our large fortress at " + sector.getPosition().toString() + " to " + sector.getTempNation().getName(),
                                sector.getTempNation().getName() + " conquered the large fortress of " + sector.getNation().getName() + " at " + sector.getPosition().toString());

                        // Modify player's profile
                        changeProfile(getGame(), sector.getTempNation(), ProfileConstants.FORTRESS_CONQUERED, 1);

                        // Update achievements
                        achievementsConquerHuge(getGame(), sector.getTempNation());
                    }
                }

                // handover barrack
                thisBarr.setNation(sector.getTempNation());
                BarrackManager.getInstance().update(thisBarr);
            }

            sector.setNation(sector.getTempNation());
            SectorManager.getInstance().update(sector);
        }
        theTrans.commit();
    }

    /**
     * Award VPs for conquering multiple foreign capitals.
     *
     * @param theCapitols the original owners of the capitals.
     * @param sector      the sector to check.
     * @param conqueror   the conquering nation.
     */
    private void awardCapitalConquers(final Map<Integer, Integer> theCapitols, final Sector sector, final Nation conqueror) {
        // Count number of capitals
        int capitalCnt = 0;
        final List<TradeCity> lstCapitals = TradeCityManager.getInstance().listByGame(getGame());
        for (final TradeCity capital : lstCapitals) {
            if (capital.getPosition().getRegion().getId() == EUROPE
                    && theCapitols.containsKey(capital.getCityId())
                    && theCapitols.get(capital.getCityId()) != conqueror.getId()
                    && (capital.getNation().getId() == conqueror.getId() || capital.getPosition().equals(sector.getPosition()))) {
                capitalCnt++;
            }
        }

        LOGGER.info(conqueror.getName() + " has conquered " + capitalCnt + " foreign capitals");

        // Conquer 2 enemy capitals -- once only
        if (capitalCnt >= 2) {
            final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(conqueror, getGame(), "vp.2-capitals");
            if (lstReports.isEmpty()) {
                // We gain 12 VP
                changeVP(getGame(), conqueror, CONQUER_2_CAPITALS,
                        "Conquered 2 enemy capitals");

                report(conqueror, "vp.2-capitals", 1);

                // broadcasting news
                newsGlobal(conqueror, NEWS_MILITARY,
                        "We have conquered two enemy capitals!",
                        conqueror.getName() + " conquered two enemy capitals!");

                LOGGER.info(conqueror.getName() + " gains VPs for conquering 2 foreign capitals");
            }
        }

        // Conquer 3 enemy capitals -- once only
        if (capitalCnt >= 3) {
            final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(conqueror, getGame(), "vp.3-capitals");
            if (lstReports.isEmpty()) {
                // We gain 24 VP
                changeVP(getGame(), conqueror, CONQUER_3_CAPITALS,
                        "Conquered 3 enemy capitals");

                report(conqueror, "vp.3-capitals", 1);

                // broadcasting news
                newsGlobal(conqueror, NEWS_MILITARY,
                        "We have conquered three enemy capitals!",
                        conqueror.getName() + " conquered three enemy capitals!");

                LOGGER.info(conqueror.getName() + " gains VPs for conquering 3 foreign capitals");
            }
        }

        // Conquer 4 enemy capitals -- once only
        if (capitalCnt >= 4) {
            final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(conqueror, getGame(), "vp.4-capitals");
            if (lstReports.isEmpty()) {
                // We gain 40 VP
                changeVP(getGame(), conqueror, CONQUER_4_CAPITALS,
                        "Conquered 4 enemy capitals");

                report(conqueror, "vp.4-capitals", 1);

                // broadcasting news
                newsGlobal(conqueror, NEWS_MILITARY,
                        "We have conquered four enemy capitals!",
                        conqueror.getName() + " conquered four enemy capitals!");

                LOGGER.info(conqueror.getName() + " gains VPs for conquering 4 foreign capitals");
            }
        }
    }

    /**
     * Plunder the enemy warehouse.
     *
     * @param sector       the sector that was conquered (trade city).
     * @param strMaterials the description of the materials plundered.
     */
    private void plunderWarehouse(Sector sector, StringBuilder strMaterials) {
        LOGGER.info("Trade City " + sector.getName() + "(" + sector.getPosition().toString() + ") plundered by " + sector.getTempNation().getName());

        double modifier = calcPlunderModifier(sector);

        // Scenario 1808: execute normal formula for every material, then divide by 2
        if (getGame().getScenarioId() == HibernateUtil.DB_S3) {
            modifier /= 2;
        }

        final int winnerId = sector.getTempNation().getId();
        final int loserId = sector.getNation().getId();
        final int regionId = sector.getPosition().getRegion().getId();

        // exchange materials
        final List<Good> lstGoods = GoodManager.getInstance().list();
        lstGoods.remove(GOOD_CP - 1);
        lstGoods.remove(GOOD_AP - 1);
        lstGoods.remove(GOOD_PEOPLE - 1);
        lstGoods.remove(GOOD_MONEY - 1);
        for (Good good : lstGoods) {
            final int gain = (int) (getTotGoods(loserId, regionId, good.getGoodId()) * modifier);
            LOGGER.info("Material " + good.getName() + ", Avail: " + getTotGoods(loserId, regionId, good.getGoodId()) + ", Plundered: " + gain);

            if (gain > 0) {
                strMaterials.append(formatter.format(gain));
                strMaterials.append("&nbsp;");
                strMaterials.append("<img width=\"16\" src='http://static.eaw1805.com/images/goods/good-");
                strMaterials.append(good.getGoodId());
                strMaterials.append(".png' style=\"padding-top: 1px; vertical-align: bottom;\" title=\"");
                strMaterials.append(good.getName());
                strMaterials.append("\">");
                strMaterials.append(", ");

                final int thisRegionId;
                if (good.getGoodId() == GOOD_MONEY) {
                    thisRegionId = EUROPE;
                } else {
                    thisRegionId = regionId;
                }

                // Do the transfer
                incTotGoods(winnerId, thisRegionId, good.getGoodId(), gain);
                decTotGoods(loserId, thisRegionId, good.getGoodId(), gain);

                // Report removal of goods
                final Report lostReport = ReportManager.getInstance().getByOwnerTurnKey(sector.getNation(),
                        sector.getPosition().getGame(),
                        sector.getPosition().getGame().getTurn(),
                        "fees.region." + thisRegionId + ".good." + good.getGoodId());

                if (lostReport != null) {
                    try {
                        lostReport.setValue(Integer.toString(Integer.parseInt(lostReport.getValue()) - gain));

                    } catch (Exception ex) {
                        LOGGER.error("Cannot parse report value", ex);
                        lostReport.setValue(Integer.toString(-gain));
                    }
                    ReportManager.getInstance().update(lostReport);

                } else {
                    final Report newReport = new Report();
                    newReport.setGame(sector.getPosition().getGame());
                    newReport.setTurn(sector.getPosition().getGame().getTurn());
                    newReport.setNation(sector.getNation());
                    newReport.setKey("fees.region." + thisRegionId + ".good." + good.getGoodId());
                    newReport.setValue(Integer.toString(-gain));
                    ReportManager.getInstance().add(newReport);
                }

                // Report addition of goods
                final Report gainsReport = ReportManager.getInstance().getByOwnerTurnKey(sector.getTempNation(),
                        sector.getPosition().getGame(),
                        sector.getPosition().getGame().getTurn(),
                        "fees.region." + thisRegionId + ".good." + good.getGoodId());

                if (gainsReport != null) {
                    try {
                        gainsReport.setValue(Integer.toString(Integer.parseInt(gainsReport.getValue()) + gain));

                    } catch (Exception ex) {
                        LOGGER.error("Cannot parse report value", ex);
                        gainsReport.setValue(Integer.toString(gain));
                    }
                    ReportManager.getInstance().update(gainsReport);

                } else {
                    final Report newReport = new Report();
                    newReport.setGame(sector.getPosition().getGame());
                    newReport.setTurn(sector.getPosition().getGame().getTurn());
                    newReport.setNation(sector.getTempNation());
                    newReport.setKey("fees.region." + thisRegionId + ".good." + good.getGoodId());
                    newReport.setValue(Integer.toString(gain));
                    ReportManager.getInstance().add(newReport);
                }
            }
        }
    }

    /**
     * Calculate plunder modifier when a trade city is conquered.
     *
     * @param sector the sector of the trade city.
     * @return the plunder modifier.
     */
    private double calcPlunderModifier(final Sector sector) {
        // Count number of cities
        final int totTradeCities = TradeCityManager.getInstance().countGameOwner(sector.getPosition().getGame(),
                sector.getNation(), sector.getPosition().getRegion());

        LOGGER.info(sector.getNation().getName() + " maintains " + totTradeCities + " trade cities in this region");

        final double modifier;
        if (sector.getPosition().getRegion().getId() == EUROPE) {
            if (getSphere(sector, sector.getNation()) == 1) {
                // Losing a Home Region Trade City: *1/(x+2)
                modifier = 1d / (totTradeCities + 3d);

                LOGGER.info("Losing a Home Region Trade City: *1/(x+2) -- " + modifier);

            } else {
                // Losing a non-Home nation Trade City in Europe: 1/(x+10)
                modifier = 1d / (totTradeCities + 10d);

                LOGGER.info("Losing a non-Home nation Trade City in Europe: 1/(x+10) -- " + modifier);
            }

        } else {
            // Losing a Trade City in the Colonies: 1/(x+5)
            modifier = 1d / (totTradeCities + 5d);

            LOGGER.info("Losing a Trade City in the Colonies: 1/(x+5) -- " + modifier);
        }

        return modifier;
    }

    /**
     * Unload all loaded units of the ex-allie.
     *
     * @param thisGame the game to inspect.
     * @param owner    the owner to check ships.
     * @param oldAlly  the ex-allie.
     */
    public void unloadUnits(final Game thisGame, final Nation owner, final Nation oldAlly) {
        final List<Ship> lstShips = ShipManager.getInstance().listGameNation(thisGame, owner);
        for (final Ship thisShip : lstShips) {
            // Locate sector
            final Sector thisSector = SectorManager.getInstance().getByPosition(thisShip.getPosition());

            // keep track of units unloaded
            final List<Integer> slotsRemoved = new ArrayList<Integer>();

            // Check if a unit is loaded in the ship
            final Map<Integer, Integer> storedGoods = thisShip.getStoredGoods();
            for (Map.Entry<Integer, Integer> entry : storedGoods.entrySet()) {
                if (entry.getKey() > GoodConstants.GOOD_LAST) {
                    if (entry.getKey() >= ArmyConstants.SPY * 1000) {
                        // A spy is loaded
                        final Spy thisSpy = SpyManager.getInstance().getByID(entry.getValue());

                        if (thisSpy == null) {
                            // Spy is dead
                            slotsRemoved.add(entry.getKey());

                        } else if (thisSpy.getNation().getId() == oldAlly.getId()) {

                            if (thisSector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                                // Remove spy
                                LOGGER.info(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " drowns ex-allied SPY " +
                                        thisSpy.getName() + "[" + thisSpy.getSpyId() + "/" + oldAlly.getName() + "]");

                                // Report death of spy.
                                report(thisSpy.getNation(), "spy.death", "Spy '" + thisSpy.getName() + "' was thrown off the ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when our alliance was broken.");
                                report(thisSector.getNation(), "spy.death", "We thrown spy '" + thisSpy.getName() + "' of Nation [" + thisSpy.getNation().getName() + "] to the water at " + thisSector.getPosition().toString() + " when our alliance was broken.");

                                newsPair(oldAlly, owner, NEWS_POLITICAL,
                                        "Our spy '" + thisSpy.getName() + "' was thrown off the ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when the alliance with " + owner.getName() + " was broken.",
                                        "We thrown '" + thisSpy.getName() + "', the spy of " + thisSpy.getNation().getName() + ", off our ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when the alliance with " + oldAlly.getName() + " was broken.");

                                // Remove spy from game
                                SpyManager.getInstance().delete(thisSpy);

                            } else {
                                LOGGER.info(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " unloads ex-allied SPY " +
                                        thisSpy.getName() + "[" + thisSpy.getSpyId() + "/" + oldAlly.getName() + "]");
                            }

                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(0);
                            thisCarrying.setCarrierId(0);

                            thisSpy.setCarrierInfo(thisCarrying);
                            SpyManager.getInstance().update(thisSpy);

                            slotsRemoved.add(entry.getKey());
                        }

                    } else if (entry.getKey() >= ArmyConstants.COMMANDER * 1000) {
                        // A commander is loaded
                        final Commander thisCommander = CommanderManager.getInstance().getByID(entry.getValue());

                        if (thisCommander == null) {
                            // Commander is dead
                            slotsRemoved.add(entry.getKey());

                        } else if (thisCommander.getNation().getId() == oldAlly.getId()) {

                            if (thisSector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                                // Remove commander
                                LOGGER.info(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " drowns ex-allied COMMANDER " +
                                        thisCommander.getName() + "[" + thisCommander.getId() + "/" + oldAlly.getName() + "]");

                                // Report death of commander.
                                newsPair(oldAlly, owner, NEWS_POLITICAL,
                                        "Our commander '" + thisCommander.getName() + "' was thrown off the ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when the alliance with " + owner.getName() + " was broken.",
                                        "We thrown '" + thisCommander.getName() + "', a commander of " + thisCommander.getNation().getName() + ", off our ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when the alliance with " + oldAlly.getName() + " was broken.");

                                // remove commander from command
                                removeCommander(thisCommander);

                                // Remove commander from game
                                thisCommander.setDead(true);

                            } else {
                                LOGGER.info(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " unloads ex-allied COMMANDER " +
                                        thisCommander.getName() + "[" + thisCommander.getId() + "/" + oldAlly.getName() + "]");
                            }

                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(0);
                            thisCarrying.setCarrierId(0);

                            thisCommander.setCarrierInfo(thisCarrying);

                            slotsRemoved.add(entry.getKey());

                            CommanderManager.getInstance().update(thisCommander);
                        }

                    } else if (entry.getKey() >= ArmyConstants.BRIGADE * 1000) {
                        // A Brigade is loaded
                        final Brigade thisBrigade = BrigadeManager.getInstance().getByID(entry.getValue());

                        if (thisBrigade == null) {
                            // Brigade is dead
                            slotsRemoved.add(entry.getKey());

                        } else if (thisBrigade.getNation().getId() == oldAlly.getId()) {

                            if (thisSector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                                // Remove spy
                                LOGGER.info(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " drowns ex-allied BRIGADE " +
                                        thisBrigade.getName() + "[" + thisBrigade.getBrigadeId() + "/" + oldAlly.getName() + "]");

                                // Report death of commander.
                                newsPair(oldAlly, owner, NEWS_POLITICAL,
                                        "Our brigade '" + thisBrigade.getName() + "' was thrown off the ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when the alliance with " + owner.getName() + " was broken.",
                                        "We thrown '" + thisBrigade.getName() + "', a brigade of Nation [" + thisBrigade.getNation().getName() + "], off our ship '" + thisShip.getName() + "' at " + thisSector.getPosition().toString() + " when the alliance with " + oldAlly.getName() + " was broken.");

                                // update info in each battalion of the brigade
                                for (final Battalion battalion : thisBrigade.getBattalions()) {
                                    battalion.setHeadcount(0);
                                    BattalionManager.getInstance().update(battalion);
                                }

                            } else {
                                LOGGER.info(thisShip.getNation().getName() +
                                        " - ship [" + thisShip.getName() + "] at " +
                                        thisShip.getPosition() +
                                        " unloads ex-allied BRIGADE " +
                                        thisBrigade.getName() + "[" + thisBrigade.getBrigadeId() + "/" + oldAlly.getName() + "]");
                            }

                            final CarrierInfo thisCarrying = new CarrierInfo();
                            thisCarrying.setCarrierType(0);
                            thisCarrying.setCarrierId(0);

                            // update info in each battalion of the brigade
                            for (final Battalion battalion : thisBrigade.getBattalions()) {
                                battalion.setCarrierInfo(thisCarrying);
                                BattalionManager.getInstance().update(battalion);
                            }

                            BrigadeManager.getInstance().update(thisBrigade);
                            slotsRemoved.add(entry.getKey());
                        }
                    }
                }
            }

            // Remove unloaded slots
            for (final int slot : slotsRemoved) {
                thisShip.getStoredGoods().remove(slot);
            }
        }
    }

    /**
     * Determine the nation each nation is still controlling it's capital.
     *
     * @return a map of nations to boolean values (true if the nation still controls its capitol).
     */
    private Map<Integer, Integer> retrieveCapitalOwners() {
        final Map<Integer, Integer> initialCapitols = new HashMap<Integer, Integer>();

        final Game initGame = GameManager.getInstance().getByID(-1);
        final List<TradeCity> initTradeCities = TradeCityManager.getInstance().listByGame(initGame);
        final List<TradeCity> lstTradeCities = TradeCityManager.getInstance().listByGame(getGame());

        for (final TradeCity initCity : initTradeCities) {
            // Retrieve owner of trade city when game starts
            if (initCity.getPosition().getRegion().getId() != EUROPE
                    || initCity.getNation().getId() == NATION_NEUTRAL) {
                continue;
            }

            for (final TradeCity city : lstTradeCities) {
                if (initCity.getPosition().getX() == city.getPosition().getX()
                        && initCity.getPosition().getY() == city.getPosition().getY()) {
                    initialCapitols.put(city.getCityId(), initCity.getNation().getId());
                }
            }
        }

        return initialCapitols;
    }

    private List<Ship> randomPirateForce(final Sector field) {
        // Raise pirate flag!
        final Nation pirates = new Nation();
        pirates.setId(0);
        pirates.setName("Pirates");

        // Roll ships
        final List<Ship> side = new ArrayList<Ship>();
        final int count = getGameEngine().getRandomGen().nextInt(6) + 1;
        for (int shipCnt = 0; shipCnt < count; shipCnt++) {
            final Ship pirateShip = new Ship();
            pirateShip.setNation(pirates);
            pirateShip.setPosition(field.getPosition());
            pirateShip.setCondition(100);
            pirateShip.setExp(1);
            pirateShip.setNoWine(false);

            ShipType thisType;
            final int roll = getGameEngine().getRandomGen().nextInt(101) + 1;
            if (roll < 40) {
                // Class 1
                thisType = ShipTypeManager.getInstance().getByID(1);

            } else if (roll < 80) {
                // class 2
                thisType = ShipTypeManager.getInstance().getByID(2);

            } else {
                // class 3
                thisType = ShipTypeManager.getInstance().getByID(3);
            }

            pirateShip.setType(thisType);
            pirateShip.setMarines(thisType.getCitizens());

            side.add(pirateShip);
        }

        return side;
    }

    private List<Ship> prepareShipList() {
        final List<Ship> ships = new ArrayList<Ship>();

        for (final Ship ship : piratesTargetShp.keySet()) {
            ships.add(ship);
        }

        return ships;
    }

    private List<Fleet> prepareFleetList() {
        final List<Fleet> fleets = new ArrayList<Fleet>();

        for (final Fleet fleet : piratesTargetFlt.keySet()) {
            fleets.add(fleet);
        }

        return fleets;
    }

    private Sector pickRandomSector(final Set<Sector> visitedSectors) {
        final List<Sector> sectors = new ArrayList<Sector>();

        for (final Sector sector : visitedSectors) {
            sectors.add(sector);
        }

        java.util.Collections.shuffle(sectors);
        return sectors.get(0);
    }

    /**
     * Handle the Random Event for pirates.
     *
     * @param thisWP an instance of the WarfareProcessor.
     */
    private void randomEventPirates(final WarfareProcessor thisWP) {
        final Transaction trans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        if (!piratesTargetFlt.isEmpty() || !piratesTargetShp.isEmpty()) {
            // setup lists
            final List<Ship> lstShips = prepareShipList();
            final List<Fleet> lstFleets = prepareFleetList();

            final int roll = getGameEngine().getRandomGen().nextInt(101) + 1;
            if (roll <= 20) {
                int count = getGameEngine().getRandomGen().nextInt(3) + 1;

                while ((count > 0) && (!lstShips.isEmpty() || !lstFleets.isEmpty())) {
                    // Prepare forces
                    final List<Ship> side1 = new ArrayList<Ship>();
                    Sector field = null;

                    if (piratesTargetFlt.isEmpty()) {
                        // pick a merchant ship
                        java.util.Collections.shuffle(lstShips);
                        final Ship thisShip = lstShips.remove(0);
                        side1.add(thisShip);

                        // pick a sector from the movement path
                        field = pickRandomSector(piratesTargetShp.get(thisShip));

                    } else if (piratesTargetShp.isEmpty()) {
                        // pick a fleet
                        java.util.Collections.shuffle(lstFleets);
                        final Fleet thisFleet = lstFleets.remove(0);
                        final List<Ship> shipList = ShipManager.getInstance().listByFleet(getGame(), thisFleet.getFleetId());
                        for (final Ship thisShip : shipList) {
                            side1.add(thisShip);
                        }

                        // pick a sector from the movement path
                        field = pickRandomSector(piratesTargetFlt.get(thisFleet));

                    } else {
                        final int choose = getGameEngine().getRandomGen().nextInt(3) + 1;
                        if (choose == 1 && (!lstShips.isEmpty() || lstFleets.isEmpty())) {
                            // pick a merchant ship
                            java.util.Collections.shuffle(lstShips);
                            final Ship thisShip = lstShips.remove(0);
                            side1.add(thisShip);

                            // pick a sector from the movement path
                            field = pickRandomSector(piratesTargetShp.get(thisShip));

                        } else if (choose == 1 && !lstFleets.isEmpty()) {
                            // pick a fleet
                            java.util.Collections.shuffle(lstFleets);
                            final Fleet thisFleet = lstFleets.remove(0);
                            final List<Ship> shipList = ShipManager.getInstance().listByFleet(getGame(), thisFleet.getFleetId());
                            for (final Ship thisShip : shipList) {
                                side1.add(thisShip);
                            }

                            // pick a sector from the movement path
                            field = pickRandomSector(piratesTargetFlt.get(thisFleet));
                        }
                    }

                    // Setup forces
                    if (field != null) {
                        final List<Ship> side2 = randomPirateForce(field);

                        LOGGER.info("Piracy Random Event at " + field.getPosition().toString() + " vs " + side1.get(0).getNation().getName());

                        // Execute battle with pirates
                        thisWP.conductPiracy(field, side1, side2);
                    }

                    count--;
                }
            }
        }
        trans.commit();
    }

    /**
     * When a barrack/shipyard where ships are being build is captured by enemy troops, the following may happen:
     * there is a 25% chance per ship being built there, that it will be captured and be available to the conqueror or
     * there is a 50% chance per ship being built there, that it will be destroyed and the captor will receive half
     * of Ind.Pts & Fabric that went into its construction (wood is burned and lost) or
     * there is a 25% chance per ship being built there, that it will be completely destroyed.
     *
     * @param sector the sector to inspect.
     */
    private void destroyOrMoveShips(final Sector sector) {
        int cntMoved = 0, cntCaptured = 0, cntBurnt = 0, cntDestroyed = 0;
        int totIndPt = 0, totFabrics = 0;
        final StringBuilder moved = new StringBuilder();
        final StringBuilder captured = new StringBuilder();
        final StringBuilder burnt = new StringBuilder();
        final StringBuilder destroyed = new StringBuilder();

        // Dummy order used for unloading units
        final ScuttleShip dummyOrder = new ScuttleShip(this);

        // In case we need to move ships outside the shipyard, select the sector where it will move
        final List<Sector> lstSectors = SectorManager.getInstance().listAdjacentSea(sector.getPosition());

        final Sector exitSector;

        if (lstSectors.isEmpty()) {
            LOGGER.info("No exit sector found at " + sector.getPosition());
            exitSector = null;

        } else {
            // Randomly select one
            java.util.Collections.shuffle(lstSectors);

            if (lstSectors.get(0).getId() == sector.getId()) {
                lstSectors.remove(0);

                if (lstSectors.isEmpty()) {
                    LOGGER.info("No exit sector found at " + sector.getPosition());
                    exitSector = null;

                } else {
                    exitSector = lstSectors.get(0);
                }

            } else {
                exitSector = lstSectors.get(0);
            }
        }

        // Capture Ships under construction
        final List<Ship> lstShips = ShipManager.getInstance().listByPositionNation(sector.getPosition(), sector.getNation());
        for (final Ship ship : lstShips) {
            // Check if this is a new ship
            if (ship.getJustConstructed() || exitSector == null) {
                // check if ship was loaded with units
                ship.initializeVariables();

                // Check if the entity is carrying units, and update their position too
                if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                    // Unload entities
                    dummyOrder.destroyLoadedUnits(ship, true);
                }

                // Apply rules to capture or destroy ships under construction
                final int roll = getRandomGen().nextInt(101) + 1;
                if (roll <= 25) {
                    // 25% chance per ship will be captured and be available to the conqueror
                    cntCaptured++;
                    captured.append(ship.getName());
                    captured.append(", ");
                    ship.setNation(sector.getTempNation());
                    ShipManager.getInstance().update(ship);

                } else if (roll <= 75) {
                    // 50% chance that it will be destroyed and the captor will receive half of Ind.Pts & textiles
                    cntBurnt++;

                    // calculate industrial points
                    final int indPt = (int) (ship.getType().getIndPt() * 0.5d);
                    totIndPt += indPt;
                    final int fabrics = (int) (ship.getType().getFabrics() * 0.5d);
                    totFabrics += fabrics;

                    burnt.append(ship.getName());
                    burnt.append(", ");

                    final int thisRegionId = ship.getPosition().getRegion().getId();

                    // Add goods to regional warehouse
                    incTotGoods(ship.getNation().getId(), thisRegionId, GOOD_INPT, indPt);
                    incTotGoods(ship.getNation().getId(), thisRegionId, GOOD_FABRIC, fabrics);

                    // Report addition of industrial points
                    final Report gainsReport = ReportManager.getInstance().getByOwnerTurnKey(sector.getTempNation(),
                            sector.getPosition().getGame(),
                            sector.getPosition().getGame().getTurn(),
                            "fees.region." + thisRegionId + ".good." + GOOD_INPT);

                    if (gainsReport != null) {
                        try {
                            gainsReport.setValue(Integer.toString(Integer.parseInt(gainsReport.getValue()) + indPt));

                        } catch (Exception ex) {
                            LOGGER.error("Cannot parse report value", ex);
                            gainsReport.setValue(Integer.toString(indPt));
                        }
                        ReportManager.getInstance().update(gainsReport);

                    } else {
                        final Report newReport = new Report();
                        newReport.setGame(sector.getPosition().getGame());
                        newReport.setTurn(sector.getPosition().getGame().getTurn());
                        newReport.setNation(sector.getTempNation());
                        newReport.setKey("fees.region." + thisRegionId + ".good." + GOOD_FABRIC);
                        newReport.setValue(Integer.toString(indPt));
                        ReportManager.getInstance().add(newReport);
                    }

                    // Report addition of fabrics
                    final Report fabricsReport = ReportManager.getInstance().getByOwnerTurnKey(sector.getTempNation(),
                            sector.getPosition().getGame(),
                            sector.getPosition().getGame().getTurn(),
                            "fees.region." + thisRegionId + ".good." + GOOD_FABRIC);

                    if (fabricsReport != null) {
                        try {
                            fabricsReport.setValue(Integer.toString(Integer.parseInt(gainsReport.getValue()) + fabrics));

                        } catch (Exception ex) {
                            LOGGER.error("Cannot parse report value", ex);
                            fabricsReport.setValue(Integer.toString(fabrics));
                        }
                        ReportManager.getInstance().update(fabricsReport);

                    } else {
                        final Report newReport = new Report();
                        newReport.setGame(sector.getPosition().getGame());
                        newReport.setTurn(sector.getPosition().getGame().getTurn());
                        newReport.setNation(sector.getTempNation());
                        newReport.setKey("fees.region." + thisRegionId + ".good." + GOOD_FABRIC);
                        newReport.setValue(Integer.toString(fabrics));
                        ReportManager.getInstance().add(newReport);
                    }

                    ship.setCondition(0);
                    ShipManager.getInstance().update(ship);

                } else {
                    // 25% chance that it will be completely destroyed
                    cntDestroyed++;
                    destroyed.append(ship.getName());
                    destroyed.append(", ");
                    ship.setCondition(0);
                    ship.setMarines(0);
                    ShipManager.getInstance().update(ship);

                    // Check if the entity is carrying units, and update their position too
                    if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                        dummyOrder.destroyLoadedUnits(ship, true);
                    }
                }

            } else {
                // Fleet must move out of shipyard
                cntMoved++;
                moved.append(ship.getName());
                moved.append(", ");

                ship.setPosition((Position) exitSector.getPosition().clone());
                ShipManager.getInstance().update(ship);

                // check also loaded units
                ship.initializeVariables();

                // Check if the entity is carrying units, and update their position too
                if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                    ship.updatePosition((Position) exitSector.getPosition().clone());
                }

                // Check if ship is in fleet
                if (ship.getFleet() != 0) {
                    // Also update fleet
                    final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                    thisFleet.setPosition(exitSector.getPosition());
                    FleetManager.getInstance().update(thisFleet);
                }
            }
        }

        // Report destruction
        if (cntCaptured > 0) {
            captured.delete(captured.length() - 2, captured.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our shipyard at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntCaptured + " ships (" + captured.toString() + ") were captured. ");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and captured "
                            + cntCaptured + " ships (" + captured.toString() + ").");
        }

        if (cntBurnt > 0) {
            burnt.delete(burnt.length() - 2, burnt.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our shipyard at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntCaptured + " ships (" + captured.toString() + ") were burnt. "
                            + "They managed to salvage " + totIndPt + " industrial points and "
                            + totFabrics + " fabrics.");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and burnt "
                            + cntCaptured + " ships (" + captured.toString() + ")."
                            + "We managed to salvage " + totIndPt + " industrial points and "
                            + totFabrics + " fabrics.");
        }

        if (cntDestroyed > 0) {
            destroyed.delete(destroyed.length() - 2, destroyed.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our shipyard at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntCaptured + " ships (" + captured.toString() + ") were destroyed.");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and destroyed "
                            + cntCaptured + " ships (" + captured.toString() + ").");
        }

        // Report move
        if (cntMoved > 0) {
            moved.delete(moved.length() - 2, moved.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our shipyard at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntMoved + " ships (" + moved.toString() + ") managed to escape to " + exitSector.getPosition() + ".");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and "
                            + cntMoved + " enemy ships (" + moved.toString() + ") managed to escape to " + exitSector.getPosition() + ".");
        }

        // Also check ships of enemy forces
        final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(getGame(), sector.getTempNation());
        for (final NationsRelation relation : lstRelations) {
            if (relation.getRelation() > REL_TRADE) {
                final StringBuilder movedEnemy = new StringBuilder();
                int movedShips = 0;
                // All ships of this nation must be moved away
                final List<Ship> lstShip = ShipManager.getInstance().listByPositionNation(sector.getPosition(), relation.getTarget());
                for (final Ship ship : lstShip) {
                    // Fleet must move out of shipyard
                    movedShips++;
                    movedEnemy.append(ship.getName());
                    movedEnemy.append(", ");

                    ship.setPosition((Position) exitSector.getPosition().clone());
                    ShipManager.getInstance().update(ship);

                    // check also loaded units
                    ship.initializeVariables();

                    // Check if the entity is carrying units, and update their position too
                    if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                        ship.updatePosition((Position) exitSector.getPosition().clone());
                    }

                    // Check if ship is in fleet
                    if (ship.getFleet() != 0) {
                        // Also update fleet
                        final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                        thisFleet.setPosition(exitSector.getPosition());
                        FleetManager.getInstance().update(thisFleet);
                    }
                }

                // Report move
                if (movedShips > 0) {
                    movedEnemy.delete(movedEnemy.length() - 2, movedEnemy.length() - 1);
                    final int newsId = news(relation.getTarget(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                            "The shipyard at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                                    + " and "
                                    + movedShips + " ships (" + movedEnemy.toString() + ") managed to escape to " + exitSector.getPosition() + ".");

                    news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                            "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                                    + " and "
                                    + movedShips + " ships of " + relation.getTarget().getName() + " (" + movedEnemy.toString() + ") managed to escape to " + exitSector.getPosition() + ".");
                }

            } else if (relation.getRelation() == REL_TRADE) {
                // Move Warships out of port
                final StringBuilder movedEnemy = new StringBuilder();
                int movedShips = 0;

                // Decide which fleets have to move outside the port
                final Set<Integer> moveFleets = new HashSet<Integer>();

                // All ships of this nation must be moved away
                final List<Ship> lstShip = ShipManager.getInstance().listByPositionNation(sector.getPosition(), relation.getTarget());
                for (final Ship ship : lstShip) {
                    // check also loaded units
                    ship.initializeVariables();

                    if (ship.getType().getShipClass() > 0
                            || ship.getHasTroops()) {
                        // This is a war ship, it has to be moved
                        if (ship.getFleet() > 0) {
                            // Fleet must move out of shipyard
                            moveFleets.add(ship.getFleet());

                        } else {
                            // Ship must move out of shipyard
                            movedShips++;
                            movedEnemy.append(ship.getName());
                            movedEnemy.append(", ");

                            ship.setPosition((Position) exitSector.getPosition().clone());
                            ShipManager.getInstance().update(ship);

                            // Check if the entity is carrying units, and update their position too
                            if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                                ship.updatePosition((Position) exitSector.getPosition().clone());
                            }

                            // Check if ship is in fleet
                            if (ship.getFleet() != 0) {
                                // Also update fleet
                                final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                                thisFleet.setPosition(exitSector.getPosition());
                                FleetManager.getInstance().update(thisFleet);
                            }
                        }
                    }
                }

                // Check if we need to move fleets as well
                for (int moveFleet : moveFleets) {
                    final List<Ship> fleetShips = ShipManager.getInstance().listByFleet(sector.getPosition().getGame(), moveFleet);
                    for (Ship ship : fleetShips) {
                        // Ship must move out of shipyard
                        movedShips++;
                        movedEnemy.append(ship.getName());
                        movedEnemy.append(", ");

                        ship.setPosition((Position) exitSector.getPosition().clone());
                        ShipManager.getInstance().update(ship);

                        // check also loaded units
                        ship.initializeVariables();

                        // Check if the entity is carrying units, and update their position too
                        if (ship.getHasCommander() || ship.getHasSpy() || ship.getHasTroops()) {
                            ship.updatePosition((Position) exitSector.getPosition().clone());
                        }

                        // Check if ship is in fleet
                        if (ship.getFleet() != 0) {
                            // Also update fleet
                            final Fleet thisFleet = FleetManager.getInstance().getByID(ship.getFleet());
                            thisFleet.setPosition(exitSector.getPosition());
                            FleetManager.getInstance().update(thisFleet);
                        }
                    }
                }

                // Report move
                if (movedShips > 0) {
                    movedEnemy.delete(movedEnemy.length() - 2, movedEnemy.length() - 1);
                    final int newsId = news(relation.getTarget(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                            "The shipyard at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                                    + " and "
                                    + movedShips + " ships (" + movedEnemy.toString() + ") managed to escape to " + exitSector.getPosition() + ".");

                    news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                            "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                                    + " and "
                                    + movedShips + " ships of " + relation.getTarget().getName() + " (" + movedEnemy.toString() + ") managed to escape to " + exitSector.getPosition() + ".");
                }
            }
        }
    }

    /**
     * When a barrack where trains are being stationed is captured by enemy troops, the following may happen:
     * there is a 25% chance per train being there, that it will be captured and be available to the conqueror or
     * there is a 75% chance per train being there, that it will be completely destroyed.
     *
     * @param sector the sector to inspect.
     */
    private void destroyOrMoveTrains(final Sector sector) {
        int cntMoved = 0, cntCaptured = 0, cntDestroyed = 0;
        final StringBuilder moved = new StringBuilder();
        final StringBuilder captured = new StringBuilder();
        final StringBuilder destroyed = new StringBuilder();

        // Dummy order used for unloading units
        final ScuttleShip dummyOrder = new ScuttleShip(this);

        // In case we need to move ships outside the shipyard, select the sector where it will move
        final List<Sector> lstSectors = SectorManager.getInstance().listAdjacentLand(sector.getPosition());

        final Sector exitSector;

        if (lstSectors.isEmpty()) {
            LOGGER.info("No exit sector found at " + sector.getPosition());
            exitSector = null;

        } else {
            // Randomly select one
            java.util.Collections.shuffle(lstSectors);

            if (lstSectors.get(0).getId() == sector.getId()) {
                lstSectors.remove(0);

                if (lstSectors.isEmpty()) {
                    LOGGER.info("No exit sector found at " + sector.getPosition());
                    exitSector = null;

                } else {
                    exitSector = lstSectors.get(0);
                }

            } else {
                exitSector = lstSectors.get(0);
            }
        }

        // Identify trains in barrack
        final List<BaggageTrain> lstTrains = BaggageTrainManager.getInstance().listByPositionNation(sector.getPosition(), sector.getNation());
        for (final BaggageTrain train : lstTrains) {
            final NationsRelation relation = RelationsManager.getInstance().getByNations(train.getPosition().getGame(), train.getNation(), sector.getTempNation());
            if (relation.getRelation() < REL_COLONIAL_WAR) {
                train.initializeVariables();
                continue;
            }
            if (exitSector == null) {

                // Check if the entity is carrying units, and update their position too
                if (train.getHasCommander() || train.getHasSpy() || train.getHasTroops()) {
                    // Unload entities
                    dummyOrder.destroyLoadedUnits(train);
                }

                // Apply rules to capture or destroy train under construction
                final int roll = getRandomGen().nextInt(101) + 1;
                if (roll <= 25) {
                    // 75% chance per train will be captured and be available to the conqueror
                    cntCaptured++;
                    captured.append(train.getName());
                    captured.append(", ");
                    train.setNation(sector.getTempNation());
                    BaggageTrainManager.getInstance().update(train);

                } else {
                    // 25% chance that it will be completely destroyed
                    cntDestroyed++;
                    destroyed.append(train.getName());
                    destroyed.append(", ");
                    train.setCondition(0);
                    BaggageTrainManager.getInstance().update(train);
                }

            } else {
                // Fleet must move out of shipyard
                cntMoved++;
                moved.append(train.getName());
                moved.append(", ");

                train.setPosition((Position) exitSector.getPosition().clone());
                BaggageTrainManager.getInstance().update(train);

                // check also loaded units
                train.initializeVariables();

                // Check if the entity is carrying units, and update their position too
                if (train.getHasCommander() || train.getHasSpy() || train.getHasTroops()) {
                    train.updatePosition((Position) exitSector.getPosition().clone());
                }
            }
        }

        // Report destruction
        if (cntCaptured > 0) {
            captured.delete(captured.length() - 2, captured.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our barrack at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntCaptured + " baggage trains (" + captured.toString() + ") were captured. ");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the barrack at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and captured "
                            + cntCaptured + " baggage trains (" + captured.toString() + ").");
        }

        if (cntDestroyed > 0) {
            destroyed.delete(destroyed.length() - 2, destroyed.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our barrack at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntCaptured + " baggage trains (" + captured.toString() + ") were destroyed.");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the barrack at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and destroyed "
                            + cntCaptured + " baggage trains (" + captured.toString() + ").");
        }

        // Report move
        if (cntMoved > 0) {
            moved.delete(moved.length() - 2, moved.length() - 1);
            final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                    "Our barrack at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                            + " and "
                            + cntMoved + " baggage trains (" + moved.toString() + ") managed to escape to " + exitSector.getPosition() + ".");

            news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                    "We conquered the barrack at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                            + " and "
                            + cntMoved + " enemy baggage trains (" + moved.toString() + ") managed to escape to " + exitSector.getPosition() + ".");
        }

        // Also check ships of enemy forces
        final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(getGame(), sector.getTempNation());
        for (final NationsRelation relation : lstRelations) {
            if (relation.getRelation() > REL_TRADE) {
                final StringBuilder movedEnemy = new StringBuilder();
                final StringBuilder capturedEnemy = new StringBuilder();
                final StringBuilder destroyedEnemy = new StringBuilder();
                int movedTrains = 0;
                int destroyedTrains = 0;
                int capturedTrains = 0;

                // All trains of this nation must be moved away
                final List<BaggageTrain> lstTrain = BaggageTrainManager.getInstance().listByPositionNation(sector.getPosition(), relation.getTarget());
                for (final BaggageTrain train : lstTrain) {

                    if (exitSector == null) {

                        // Check if the entity is carrying units, and update their position too
                        if (train.getHasCommander() || train.getHasSpy() || train.getHasTroops()) {
                            // Unload entities
                            dummyOrder.destroyLoadedUnits(train);
                        }

                        // Apply rules to capture or destroy train under construction
                        final int roll = getRandomGen().nextInt(101) + 1;
                        if (roll <= 25) {
                            // 75% chance per train will be captured and be available to the conqueror
                            capturedTrains++;
                            capturedEnemy.append(train.getName());
                            capturedEnemy.append(", ");
                            train.setNation(sector.getTempNation());
                            BaggageTrainManager.getInstance().update(train);

                        } else {
                            // 25% chance that it will be completely destroyed
                            destroyedTrains++;
                            destroyedEnemy.append(train.getName());
                            destroyedEnemy.append(", ");
                            train.setCondition(0);
                            BaggageTrainManager.getInstance().update(train);
                        }

                    } else {
                        // train must move out of shipyard
                        movedTrains++;
                        movedEnemy.append(train.getName());
                        movedEnemy.append(", ");

                        train.setPosition((Position) exitSector.getPosition().clone());
                        BaggageTrainManager.getInstance().update(train);

                        // check also loaded units
                        train.initializeVariables();

                        // Check if the entity is carrying units, and update their position too
                        if (train.getHasCommander() || train.getHasSpy() || train.getHasTroops()) {
                            train.updatePosition((Position) exitSector.getPosition().clone());
                        }
                    }
                }

                // Report destruction
                if (capturedTrains > 0) {
                    capturedEnemy.delete(capturedEnemy.length() - 2, capturedEnemy.length() - 1);
                    final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                            "The barrack at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                                    + " and "
                                    + capturedTrains + " baggage trains (" + captured.toString() + ") were captured. ");

                    news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                            "We conquered the barrack at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                                    + " and captured "
                                    + capturedTrains + " baggage trains of " + relation.getTarget().getName() + " (" + movedEnemy.toString() + ").");
                }

                if (destroyedTrains > 0) {
                    destroyedEnemy.delete(destroyedEnemy.length() - 2, destroyedEnemy.length() - 1);
                    final int newsId = news(sector.getNation(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                            "The barrack at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                                    + " and "
                                    + destroyedTrains + " baggage trains (" + captured.toString() + ") were destroyed.");

                    news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                            "We conquered the barrack at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                                    + " and destroyed "
                                    + destroyedTrains + " baggage trains of " + relation.getTarget().getName() + " (" + movedEnemy.toString() + ").");
                }

                // Report move
                if (movedTrains > 0) {
                    movedEnemy.delete(movedEnemy.length() - 2, movedEnemy.length() - 1);
                    final int newsId = news(relation.getTarget(), sector.getTempNation(), NEWS_MILITARY, false, 0,
                            "The barrack at " + sector.getPosition() + " was conquered by " + sector.getTempNation().getName()
                                    + " and "
                                    + cntMoved + " baggage trains (" + movedEnemy.toString() + ") managed to escape to " + exitSector.getPosition() + ".");

                    news(sector.getTempNation(), sector.getNation(), NEWS_MILITARY, false, newsId,
                            "We conquered the shipyard at " + sector.getPosition() + " owned by " + sector.getNation().getName()
                                    + " and "
                                    + cntMoved + " baggage trains of " + relation.getTarget().getName() + " (" + movedEnemy.toString() + ") managed to escape to " + exitSector.getPosition() + ".");
                }
            }
        }

    }

    /**
     * Add an association for corp Id.
     *
     * @param dbId the Id assigned by the DB.
     * @param uiId the Id assigned by the UI.
     */
    public final void associateCorpId(final int dbId, final int uiId) {
        mapCorpIds.put(uiId, dbId);
    }

    /**
     * Check if an association exists for a particular Corp Id.
     *
     * @param uiId the Id assigned by the UI.
     * @return true if an association exists.
     */
    public final boolean corpAssocExists(final int uiId) {
        return mapCorpIds.containsKey(uiId);
    }

    /**
     * Retrieve the association for a particular Corp Id.
     *
     * @param uiId the Id assigned by the UI.
     * @return the Id assigned by the DB.
     */
    public final int retrieveCorpAssoc(final int uiId) {
        return mapCorpIds.get(uiId);
    }

    /**
     * Add an association for Army Id.
     *
     * @param dbId the Id assigned by the DB.
     * @param uiId the Id assigned by the UI.
     */
    public final void associateArmyId(final int dbId, final int uiId) {
        mapArmyIds.put(uiId, dbId);
    }

    /**
     * Check if an association exists for a particular Army Id.
     *
     * @param uiId the Id assigned by the UI.
     * @return true if an association exists.
     */
    public final boolean armyAssocExists(final int uiId) {
        return mapArmyIds.containsKey(uiId);
    }

    /**
     * Retrieve the association for a particular Army Id.
     *
     * @param uiId the Id assigned by the UI.
     * @return the Id assigned by the DB.
     */
    public final int retrieveArmyAssoc(final int uiId) {
        return mapArmyIds.get(uiId);
    }

    /**
     * Add an association for Fleet Id.
     *
     * @param dbId the Id assigned by the DB.
     * @param uiId the Id assigned by the UI.
     */
    public final void associateFleetId(final int dbId, final int uiId) {
        mapFleetIds.put(uiId, dbId);
    }

    /**
     * Check if an association exists for a particular Army Id.
     *
     * @param uiId the Id assigned by the UI.
     * @return true if an association exists.
     */
    public final boolean fleetAssocExists(final int uiId) {
        return mapFleetIds.containsKey(uiId);
    }

    /**
     * Retrieve the association for a particular Fleet Id.
     *
     * @param uiId the Id assigned by the UI.
     * @return the Id assigned by the DB.
     */
    public final int retrieveFleetAssoc(final int uiId) {
        return mapFleetIds.get(uiId);
    }

    /**
     * Add a sector in the set of conquered sectors.
     *
     * @param thisSector a conquered sector.
     */
    public final void addConqueredSector(final Sector thisSector) {
        conqueredSectors.add(thisSector);
    }

    /**
     * Add a fleet as a potential target for pirates.
     *
     * @param thisFleet  the fleet to add.
     * @param thisSector the Sector where it was spotted.
     */
    public final void addPirateTarget(final Fleet thisFleet, final Sector thisSector) {
        Set<Sector> visitedSectors;
        if (piratesTargetFlt.containsKey(thisFleet)) {
            visitedSectors = piratesTargetFlt.get(thisFleet);

        } else {
            visitedSectors = new HashSet<Sector>();
            piratesTargetFlt.put(thisFleet, visitedSectors);
        }

        visitedSectors.add(thisSector);
    }

    /**
     * Add a ship as a potential target for pirates.
     *
     * @param thisShip   the ship to add.
     * @param thisSector the Sector where it was spotted.
     */
    public final void addPirateTarget(final Ship thisShip, final Sector thisSector) {
        Set<Sector> visitedSectors;
        if (piratesTargetShp.containsKey(thisShip)) {
            visitedSectors = piratesTargetShp.get(thisShip);

        } else {
            visitedSectors = new HashSet<Sector>();
            piratesTargetShp.put(thisShip, visitedSectors);
        }

        visitedSectors.add(thisSector);
    }

    /**
     * Get the active patrol orders.
     *
     * @return the active patrol orders.
     */
    public Map<Integer, PatrolOrderDetails> getPatrolOrders() {
        return getGameEngine().getPatrolOrders();
    }

    /**
     * Get the sectors that must be checked for hand-over.
     *
     * @return set of sectors.
     */
    public Set<Sector> getConqueredSectors() {
        return conqueredSectors;
    }

    /**
     * Add a declaration of war in the queue so that the call for allies is processed after all politics orders
     * are processed.
     *
     * @param warmonger the nation declaring the war.
     * @param target    the nation war was declared upon.
     */
    public final void addDeclaration(final Nation warmonger, final Nation target) {
        final NationsRelation declaration = new NationsRelation();
        declaration.setNation(warmonger);
        declaration.setTarget(target);
        callToAllies.add(declaration);
    }

    public Map<Nation, Integer> getTradeProfits() {
        return tradeProfits;
    }
}
