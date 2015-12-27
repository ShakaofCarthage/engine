package com.eaw1805.battles.field.morale;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class implements the morale checks during a field battle. It holds the
 * brigade morales, their contributions to the total army morale and also
 * performs morale checks.
 *
 * @author fragkakis
 */

/* Adding implementation to deal with morale for multiple groups of battalions in a field battle */
public class MoraleChecker {


    private final FieldBattleProcessor parent;
    private Map<Integer, Double> brigadeMoraleRelativeContributions = new HashMap<Integer, Double>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MoraleChecker.class);

    public MoraleChecker(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    public MoraleChecker(FieldBattleProcessor parent, Map<Integer, Double> brigadeMoraleRelativeContributions) {
        this.parent = parent;
        this.brigadeMoraleRelativeContributions = brigadeMoraleRelativeContributions;
    }

    /**
     * Initialization method that calculates initial morales for each side.
     */
    public void calculateInitialSideMoralesIfRequired() {
        if (brigadeMoraleRelativeContributions == null
                || brigadeMoraleRelativeContributions.isEmpty()) {
            calculateInitialSideMorale(0);
            calculateInitialSideMorale(1);
        }
    }


    /**
     * Initialization method that calculates initial morales for a single side.
     */
    public void calculateInitialSideMorale(int side) {

        List<Brigade> initialSideBrigades = parent.getInitialSideBrigades()[side];

        Map<Integer, Double> sideBrigadeMoraleWeights = new HashMap<Integer, Double>();

        double totalBrigadeMoralesTimesHeadcount = 0;

        for (Brigade brigade : initialSideBrigades) {

            int brigadeMorale = calculateBrigadeMorale(brigade);

            double brigadeHeadcount = ArmyUtils.findBrigadeHeadCount(brigade);

            double brigadeMoraleTimesHeadcount = brigadeMorale * brigadeHeadcount;
            sideBrigadeMoraleWeights.put(brigade.getBrigadeId(), brigadeMoraleTimesHeadcount);

            totalBrigadeMoralesTimesHeadcount += brigadeMoraleTimesHeadcount;
        }

        for (Entry<Integer, Double> entry : sideBrigadeMoraleWeights.entrySet()) {
            double brigadeMoraleTimesHeadcount = entry.getValue();
            sideBrigadeMoraleWeights.put(entry.getKey(), brigadeMoraleTimesHeadcount * 100 / totalBrigadeMoralesTimesHeadcount);
        }

        brigadeMoraleRelativeContributions.putAll(sideBrigadeMoraleWeights);
    }

    /**
     * Calculates the morale for a brigade. This equals the mean battalion
     * morale (experience), weighed by the battalions' headcount.
     *
     * @param brigade the brigade
     * @return the morale of the brigade
     */
    public int calculateBrigadeMorale(Brigade brigade) {

        double totalBattalionsMoralesTimesHeadcount = 0;
        double totalBrigadeHeadcount = 0;

        for (Battalion battalion : brigade.getBattalions()) {
            totalBattalionsMoralesTimesHeadcount += battalion.getExperience() * battalion.getHeadcount();
            totalBrigadeHeadcount += battalion.getHeadcount();
        }

        int brigadeMorale = (int) Math.round(totalBattalionsMoralesTimesHeadcount / totalBrigadeHeadcount);
        return brigadeMorale;
    }

    /**
     * Performs morale check and sets new morale status to brigade with no modifier.
     *
     * @param brigade the brigade
     * @return the new morale status
     */
    public MoraleStatusEnum checkAndSetMorale(Brigade brigade) {
        return checkAndSetMorale(brigade, 0);
    }

    /**
     * Performs morale check and sets new morale status to brigade.
     *
     * @param brigade  the brigade
     * @param modifier the modifier to apply
     * @return the new morale status
     */
    public MoraleStatusEnum checkAndSetMorale(Brigade brigade, int modifier) {

        return checkAndOptionallySetMorale(brigade, modifier, true);
    }

    /**
     * Performs morale check and sets new morale status to brigade.
     *
     * @param brigade the brigade
     * @return the result of the morale check
     */
    public boolean checkMorale(Brigade brigade) {
        return checkMorale(brigade, 0);
    }

    /**
     * Performs morale check without changing the morale status of the brigade.
     *
     * @param brigade  the brigade
     * @param modifier the modifier to apply
     * @return the result of the morale check
     */
    public boolean checkMorale(Brigade brigade, int modifier) {

        return checkAndOptionallySetMorale(brigade, modifier, false) == MoraleStatusEnum.NORMAL;
    }

    /**
     * Performs morale check and optionally sets new morale status to brigade.
     *
     * @param brigade   the brigade
     * @param modifier  the modifier to apply
     * @param setMorale if true, set the new morale status
     * @return the new morale status
     */
    private MoraleStatusEnum checkAndOptionallySetMorale(Brigade brigade, int modifier, boolean setMorale) {

        // Brigades with a Legendary Commander cannot fail a morale check
        Commander brigadeCommander = parent.getCommanderProcessor().getCommanderInBrigade(brigade);
        if (brigadeCommander != null && brigadeCommander.getLegendaryCommander()) {
            return MoraleStatusEnum.NORMAL;
        }

        int moraleThreshold = calculateMoraleThreshold(brigade);

        // all units within a "Legendary Commander" leader influence receive an
        // extra 5% morale bonus on top of any other
        if (parent.getCommanderProcessor().influencedByCommanderOfType(brigade, CommanderType.LEGENDARY_COMMANDER)) {
            modifier += 5;
        }

        int moraleThresholdPlusModifier = moraleThreshold + modifier;
        LOGGER.debug("{}: Performing morale check. Morale threshold: {}, modifier: {}, final threshold: {}",
                new Object[]{brigade, moraleThreshold, modifier, moraleThresholdPlusModifier});

        int dice = MathUtils.generateRandomIntInRange(1, 100);

        MoraleStatusEnum moraleStatus = null;
        if (1 <= dice && dice <= moraleThresholdPlusModifier) {
            moraleStatus = MoraleStatusEnum.NORMAL;

        } else if (moraleThresholdPlusModifier < dice && dice <= moraleThresholdPlusModifier + 5) {
            moraleStatus = MoraleStatusEnum.DISORDER;

        } else {
            moraleStatus = MoraleStatusEnum.ROUTING;
        }

        if (setMorale) {
            LOGGER.debug("Dice showed {}, so new morale status is {}", new Object[]{dice, moraleStatus});
            brigade.setMoraleStatusEnum(moraleStatus);
            if (moraleStatus == MoraleStatusEnum.ROUTING) {
                brigade.setFormationEnum(FormationEnum.FLEE);
            }
        } else {
            LOGGER.debug("Dice showed {}, morale check outcome: {}", new Object[]{dice, moraleStatus == MoraleStatusEnum.NORMAL});
        }

        return moraleStatus;
    }

    /**
     * Calculates the morale threshold for a brigade.
     *
     * @param brigade the brigade
     * @return the morale threshold
     */
    public int calculateMoraleThreshold(Brigade brigade) {

        // if the brigade is in range of a commander it receives an experience bonus +1
        boolean attackerInfluencedByCommander = parent.getCommanderProcessor().influencedByCommander(brigade);
        int commanderExperienceBonus = attackerInfluencedByCommander ? 1 : 0;
        LOGGER.trace("Commander experience bonus: {}", commanderExperienceBonus);

        int side = parent.findSide(brigade);
        return 60 + (calculateBrigadeMorale(brigade) + commanderExperienceBonus) * 5 - (100 - calculateArmyMorale(side)) / 2;
    }

    public int calculateArmyMorale(int side) {
        double armyMorale = 0;
        for (Brigade brigade : parent.getSideBrigades()[side]) {
            if (!brigade.isRouting()) {
                if (brigadeMoraleRelativeContributions.containsKey(brigade.getBrigadeId())) {
                    armyMorale += brigadeMoraleRelativeContributions.get(brigade.getBrigadeId());
                }
            }
        }

        // An army with a legendary commander has a +5% morale bonus (will initially be 105%)
        if (parent.getCommanderProcessor().sideHasAliveCommanderOfType(side, CommanderType.LEGENDARY_COMMANDER)) {
            armyMorale += 5;
        }

        return (int) Math.round(armyMorale);
    }

    public Map<Integer, Double> getBrigadeMoraleRelativeContributions() {
        return brigadeMoraleRelativeContributions;
    }

    public void setBrigadeMoraleRelativeContributions(Map<Integer, Double> brigadeMoraleRelativeContributions) {
        this.brigadeMoraleRelativeContributions = brigadeMoraleRelativeContributions;
    }

}
