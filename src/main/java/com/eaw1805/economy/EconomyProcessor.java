package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for processing sectors and production sites to produce the economy turn-around for the given turn.
 * Taxation
 * Population Growth
 * Food consumption
 * Wine consumption
 * Attrition of Workers
 * Maintenance Costs of Production Sites
 * Maintenance Costs for Ships
 * Soldiers' Pay
 */
public class EconomyProcessor {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(EconomyProcessor.class);

    /**
     * The game processed.
     */
    private final transient GameEngine gameEngine;

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public EconomyProcessor(final GameEngine caller) {
        gameEngine = caller;
        LOGGER.debug("EconomyProcessor instantiated.");
    }

    /**
     * Do economic turnaround.
     */
    public void process() {
        // Advance population, and compute income
        final SectorMaintenance advSectors = new SectorMaintenance(gameEngine);
        advSectors.advanceSectors();

        // Commanders' Pay
        final CommanderMaintenance mntCommanders = new CommanderMaintenance(gameEngine);
        mntCommanders.maintainCommanders();

        // Soldiers' Pay
        final ArmyMaintenance mntArmies = new ArmyMaintenance(gameEngine);
        mntArmies.maintainBrigades();

        // Maintenance Costs for Ships
        final ShipMaintenance mntShips = new ShipMaintenance(gameEngine);
        mntShips.maintainShips();

        // Maintenance Costs for Baggage Trains
        final BaggageTrainMaintenance mntBTrains = new BaggageTrainMaintenance(gameEngine);
        mntBTrains.maintainBaggageTrains();

        // Operate production sites
        final SectorMaintenance advProdSites = new SectorMaintenance(gameEngine);
        advProdSites.advanceProductionSites();

        // Feed the population.
        final SectorMaintenance mntSectors = new SectorMaintenance(gameEngine);
        mntSectors.maintainSectors();

        // Finally feed prisoners of war
        final PrisonersMaintenance mntPOW = new PrisonersMaintenance(gameEngine);
        mntPOW.maintainPrisoners();

        LOGGER.info("EconomyProcessor completed.");
    }

}
