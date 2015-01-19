package com.eaw1805.battles.field.processors.commander;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.utils.CommanderUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class processor commander operations
 *
 * @author fragkakis
 */
public class CommanderProcessor {

    private FieldBattleProcessor parent;

    private Map<Integer, Commander> overallCommanders = new HashMap<Integer, Commander>();
    private Map<Integer, Set<Commander>> sideCommanders = new HashMap<Integer, Set<Commander>>();
    private Map<Nation, Map<Integer, Commander>> nationCorpsCommanders = new HashMap<Nation, Map<Integer, Commander>>();
    private BiMap<Brigade, Commander> brigadeToCommander = HashBiMap.create();

    private static final Logger LOGGER = LoggerFactory.getLogger(CommanderProcessor.class);

    /**
     * Constructor.
     *
     * @param fieldBattleProcessor the field battle processor
     * @param sideBrigades the commander allocations of the field battle
     */
    public CommanderProcessor(FieldBattleProcessor fieldBattleProcessor, List<Brigade>[] sideBrigades) {
        this.parent = fieldBattleProcessor;
        sideCommanders.put(0, new HashSet<Commander>());
        sideCommanders.put(1, new HashSet<Commander>());

        registerCommandersForSide(0, sideBrigades[0]);
        registerCommandersForSide(1, sideBrigades[1]);
    }

    private void registerCommandersForSide(int i, List<Brigade> brigades) {

        for (Brigade brigade : brigades) {
            if (brigade.getFieldBattleCommanderId() != 0) {
                registerCommander(brigade, brigade.getFieldBattleCommanderId());
            }

            if (brigade.getFieldBattleOverallCommanderId() != 0) {
                registerOverallCommander(brigade, brigade.getFieldBattleOverallCommanderId());
            }
        }
    }

    private Commander registerCommander(Brigade brigade, int commanderId) {

        Commander commander = parent.getResourceLocator().getCommanderById(commanderId);
        LOGGER.debug("Commander {} marches with {}", new Object[]{commander.getName(), brigade});

        Integer brigadeSide = parent.findSide(brigade);
        sideCommanders.get(brigadeSide).add(commander);

        if (!nationCorpsCommanders.containsKey(brigade.getNation())) {
            nationCorpsCommanders.put(brigade.getNation(), new HashMap<Integer, Commander>());
        }

        nationCorpsCommanders.get(brigade.getNation()).put(brigade.getCorp(), commander);
        brigadeToCommander.put(brigade, commander);

        return commander;
    }

    private void registerOverallCommander(Brigade brigade, int commanderId) {

        Commander commander = registerCommander(brigade, commanderId);
        Integer brigadeSide = parent.findSide(brigade);

        overallCommanders.put(brigadeSide, commander);
    }

    /**
     * Checks whether a brigade is influenced by a commander. This is the case
     * is is within the influence field of a minor commander of the same corps
     * or an overall commander.
     *
     * @param brigade the brigade
     * @return true if the brigade is influenced, false otherwise
     */
    public boolean influencedByCommander(Brigade brigade) {
        return influencedByCommanderOfType(brigade, CommanderType.ANY);
    }

    /**
     * Checks whether a brigade is influenced by a commander of a specific type
     * (can be ANY). This is the case is is within the influence field of a
     * minor commander of the same corps or an overall commander.
     *
     * @param brigade       the brigade
     * @param commanderType the commander type.
     * @return true if the brigade is influenced, false otherwise
     */
    public boolean influencedByCommanderOfType(Brigade brigade, CommanderType commanderType) {

        Integer brigadeSide = parent.findSide(brigade);
        Commander overallCommander = overallCommanders.get(brigadeSide);
        Commander corpCommander = null;
        Map<Integer, Commander> corpsCommandersForNation = nationCorpsCommanders.get(brigade.getNation());
        if (corpsCommandersForNation != null) {
            corpCommander = nationCorpsCommanders.get(brigade.getNation()).get(brigade.getCorp());
        }

        boolean result = false;
        if (overallCommander != null
                && !overallCommander.getDead()
                && isCommanderOfType(overallCommander, commanderType)
                && brigadeInfluencedByCommander(brigade, overallCommander)) {
            result = true;

        } else if (corpCommander != null
                && !corpCommander.getDead()
                && isCommanderOfType(corpCommander, commanderType)
                && brigadeInfluencedByCommander(brigade, corpCommander)) {
            result = true;
        }

        return result;
    }

    /**
     * Checks if a commander is of the specified type
     *
     * @param commander
     * @param commanderType
     * @return
     */
    private boolean isCommanderOfType(Commander commander, CommanderType commanderType) {

        switch (commanderType) {
            case ANY:
                return true;
            case CAVALRY_LEADER:
                return commander.getCavalryLeader();
            case ARTILLERY_LEADER:
                return commander.getArtilleryLeader();
            case STOUT_DEFENDER:
                return commander.getStoutDefender();
            case FEARLESS_ATTACKER:
                return commander.getFearlessAttacker();
            case LEGENDARY_COMMANDER:
                return commander.getLegendaryCommander();
            default:
                // should never reach this
                throw new IllegalArgumentException("Commander type check not implemented for " + commanderType);
        }
    }


    private boolean brigadeInfluencedByCommander(Brigade brigade, Commander commander) {

        if (commander == null || commander.getDead()) {
            return false;

        } else {

            FieldBattleSector brigadeLocation = parent.getSectorsToBrigades().inverse().get(brigade);

            Brigade commanderBrigade = brigadeToCommander.inverse().get(commander);
            if (commanderBrigade == null) {
                return false;
            }
            FieldBattleSector commanderLocation = parent.getSectorsToBrigades().inverse().get(commanderBrigade);
            if (commanderLocation == null) {
                return false;
            }
            int distance = MapUtils.getSectorsDistance(brigadeLocation, commanderLocation);

            int influenceRadius = CommanderUtils.getCommanderInfluenceRadius(commander);

            return distance <= influenceRadius;
        }
    }

    /**
     * Checks whether a commander is in a brigade.
     *
     * @param brigade the brigade
     * @return true if the brigade contains a commander, false otherwise
     */
    public Commander getCommanderInBrigade(Brigade brigade) {
        Commander commander = brigadeToCommander.get(brigade);
        if (commander == null || commander.getDead()) {
            commander = null;
        }
        return commander;
    }

    /**
     * Handles the event of a commander being hit.
     *
     * @param commander the commander
     */
    public void commanderHasBeenHit(Commander commander) {
        // if a commander gets hit, there is a 75% chance of getting injured
        // and a 25% chance of getting killed
        if (MathUtils.generateRandomIntInRange(1, 100) <= 75) {
            commanderHasBeenInjured(commander);
        } else {
            commanderHasBeenKilled(commander);
        }
    }

    /**
     * Handles the event of a captured commander.
     *
     * @param commander the commander
     */
    public void commanderHasBeenCaptured(Commander commander) {
        LOGGER.debug("Commander has been captured.");
        commander.setDead(true);
        performCommanderEventMoraleCheck(commander);
        // TODO: What else must be done?
    }

    /**
     * Handles the event of a killed commander.
     *
     * @param commander the commander
     */
    public void commanderHasBeenKilled(Commander commander) {
        LOGGER.debug("Commander has been killed.");
        commander.setDead(true);
        performCommanderEventMoraleCheck(commander);
    }

    /**
     * Handles the event of an injured commander.
     *
     * @param commander the commander
     */
    private void commanderHasBeenInjured(Commander commander) {
        LOGGER.debug("Commander has been injured.");
        commander.setSick(4);
        // TODO: How many months? What else must be done?
        performCommanderEventMoraleCheck(commander);
    }

    /**
     * It performs a morale check on all non-routing allies in a radius of 10
     * around the affected commander.
     *
     * @param commander
     */
    private void performCommanderEventMoraleCheck(Commander commander) {
        LOGGER.debug("Performing morale check in radius 10 from commander event.");
        Brigade commanderBrigade = brigadeToCommander.inverse().get(commander);
        FieldBattleSector commanderPosition = parent.getSector(commanderBrigade);
        Set<FieldBattleSector> sectorsInRadius10 = MapUtils.findSectorsInRadius(commanderPosition, 10);
        int side = parent.findSide(commanderBrigade);

        Set<Brigade> alliesInRadius10 = parent.findBrigadesOfSide(sectorsInRadius10, side);
        for (Brigade ally : alliesInRadius10) {
            // don't perform morale check for brigades that are already routing
            if (ally.isRouting()) {
                LOGGER.debug("Skipping ally that is routing.");
                continue;
            }
            // if this was not an overall commander, skip the morale check for
            // brigades of different corps.
            if (!overallCommanders.values().contains(commander)
                    && ally.getCorp() != commanderBrigade.getCorp()) {
                LOGGER.debug("Commander was not minor commander of corp {}, skipping ally of corp {}.",
                        new Object[]{commanderBrigade.getCorp(), ally.getCorp()});
                continue;
            }
            parent.getMoraleChecker().checkAndSetMorale(ally, -10);
        }

    }

    /**
     * Checks whether a side has at least 1 commander of the specified type.
     *
     * @param side          the side
     * @param commanderType the type of commander
     * @return true or false
     */
    public boolean sideHasAliveCommanderOfType(int side, CommanderType commanderType) {

        for (Commander commander : sideCommanders.get(side)) {
            if (!commander.getDead() && isCommanderOfType(commander, commanderType)) {
                return true;
            }
        }
        return false;
    }

    public Map<Integer, Set<Commander>> getSideCommanders() {
        return sideCommanders;
    }
}
