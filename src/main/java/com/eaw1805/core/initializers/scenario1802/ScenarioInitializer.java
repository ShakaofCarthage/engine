package com.eaw1805.core.initializers.scenario1802;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.core.initializers.scenario1802.army.ArmyTypesInitializer;
import com.eaw1805.core.initializers.scenario1802.army.BrigadeInitializer;
import com.eaw1805.core.initializers.scenario1802.army.CommanderInitializer;
import com.eaw1805.core.initializers.scenario1802.army.CommanderNameInitializer;
import com.eaw1805.core.initializers.scenario1802.army.RankInitializer;
import com.eaw1805.core.initializers.scenario1802.army.SpyInitializer;
import com.eaw1805.core.initializers.scenario1802.economy.BaggageTrainInitializer;
import com.eaw1805.core.initializers.scenario1802.economy.GoodInitializer;
import com.eaw1805.core.initializers.scenario1802.economy.TradeInitializer;
import com.eaw1805.core.initializers.scenario1802.economy.WarehouseInitializer;
import com.eaw1805.core.initializers.scenario1802.field.FieldBattleMapExtraFeatureInitializer;
import com.eaw1805.core.initializers.scenario1802.field.FieldBattleTerrainInitializer;
import com.eaw1805.core.initializers.scenario1802.fleet.MerchantShipInitializer;
import com.eaw1805.core.initializers.scenario1802.fleet.ShipTypesInitializer;
import com.eaw1805.core.initializers.scenario1802.fleet.WarShipInitializer;
import com.eaw1805.core.initializers.scenario1802.map.NaturalResourceInitializer;
import com.eaw1805.core.initializers.scenario1802.map.ProductionSiteInitializer;
import com.eaw1805.core.initializers.scenario1802.map.RegionInitializer;
import com.eaw1805.core.initializers.scenario1802.map.SectorInitializer;
import com.eaw1805.core.initializers.scenario1802.map.TerrainInitializer;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.model.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

/**
 * Initializes a new scenario.
 */
public class ScenarioInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ScenarioInitializer.class);

    /**
     * The identity of the scenario object to initialize.
     */
    private transient int scenarioId;

    /**
     * Default constructor.
     */
    public ScenarioInitializer(final int scenario) {
        super();
        scenarioId = scenario;
        LOGGER.debug("ScenarioInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        final Game scenarioGame = GameManager.getInstance().getByID(-1);
        theTrans.rollback();
        return (scenarioGame == null);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() { // NOPMD
        LOGGER.debug("Initializing new scenario");

        // Insert Game Entry with ID = -1
        Transaction theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        GameManager.getInstance().addNegativeGameID(scenarioId);
        theTrans.commit();

        // Execute in parallel the following independent initializers
        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final RegionInitializer regionInit = new RegionInitializer();
        regionInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final NationsInitializer nationInit = new NationsInitializer();
        nationInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final GoodInitializer goodInit = new GoodInitializer();
        goodInit.run();
        theTrans.commit();

        // Execute in parallel the following complexly-dependent initializers
        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final WarehouseInitializer whouseInit = new WarehouseInitializer();
        whouseInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final TerrainInitializer terrainInit = new TerrainInitializer();
        terrainInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final NaturalResourceInitializer natResInit = new NaturalResourceInitializer();
        natResInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final ProductionSiteInitializer prodSiteInit = new ProductionSiteInitializer();
        prodSiteInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final RankInitializer rankInit = new RankInitializer();
        rankInit.run();
        theTrans.commit();

        // Execute in parallel the following simply-dependent initializers
        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final RelationsInitializer relInit = new RelationsInitializer();
        relInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final ArmyTypesInitializer armyTypesInit = new ArmyTypesInitializer();
        armyTypesInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final ShipTypesInitializer shipTypesInit = new ShipTypesInitializer();
        shipTypesInit.run();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final CommanderNameInitializer commNameInit = new CommanderNameInitializer();
        commNameInit.run();
        theTrans.commit();

        theTrans = HibernateUtil.getInstance().beginTransaction(scenarioId);
        final CommanderInitializer commInit = new CommanderInitializer();
        commInit.run();

        final BrigadeInitializer brigInit = new BrigadeInitializer();
        brigInit.run();

        final WarShipInitializer warShipInit = new WarShipInitializer();
        warShipInit.run();

        final MerchantShipInitializer merShipInit = new MerchantShipInitializer();
        merShipInit.run();

        final SectorInitializer secInit = new SectorInitializer();
        secInit.run();

        final SpyInitializer spyInit = new SpyInitializer();
        spyInit.run();

        final BaggageTrainInitializer btInit = new BaggageTrainInitializer();
        btInit.run();

        final TradeInitializer tInit = new TradeInitializer();
        tInit.run();

        final FieldBattleTerrainInitializer fbtInit = new FieldBattleTerrainInitializer();
        fbtInit.run();

        final FieldBattleMapExtraFeatureInitializer fbmefInit = new FieldBattleMapExtraFeatureInitializer();
        fbmefInit.run();

        theTrans.commit();
        LOGGER.info("Scenario initialized.");
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see java.lang.Thread#run()
     */
    public void run() {
        // Check if initialization of this entity is required
        if (needsInitialization()) {
            // perform initialization
            initialize();
        }
    }

}
