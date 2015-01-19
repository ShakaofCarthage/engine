package com.eaw1805.battles.field.processors.melee;

import com.eaw1805.battles.field.processors.MeleeProcessor;
import com.eaw1805.battles.field.processors.ProcessorUtils;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.processors.movement.AdditionalOrderBrigadeFilter;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for processing the melee actions for a particular type of order.
 * Each subclass processes movement of a single order type. The default
 * behaviour is to attack in melee adjacent enemies. If there are no enemies,
 * specific orders may perform other melee tasks.
 *
 * @author fragkakis
 */
public abstract class BaseOrderMeleeProcessor {

    private static final double MELEE_RANDOMNESS_MODIFIER_MIN = 1.5d;
    private static final double MELEE_RANDOMNESS_MODIFIER_MAX = 3.5d;
    protected MeleeProcessor meleeProcessor;
    protected AdditionalOrderBrigadeFilter additionalOrderBrigadeFilter;
    private static final Map<ArmEnum, Map<FormationEnum, Double>> attackerTypeModifier = new HashMap<ArmEnum, Map<FormationEnum, Double>>();

    static {
        // shooting effectiveness
        attackerTypeModifier.put(ArmEnum.INFANTRY, new HashMap<FormationEnum, Double>());
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.COLUMN, 1.00D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.LINE, 0.60D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.SKIRMISH, 0.35D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.SQUARE, 0.25D);
        attackerTypeModifier.get(ArmEnum.INFANTRY).put(FormationEnum.FLEE, 0.10D);

        attackerTypeModifier.put(ArmEnum.CAVALRY, new HashMap<FormationEnum, Double>());
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.COLUMN, 1.00D);
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.LINE, 0.60D);
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.SKIRMISH, 0.35D);
        attackerTypeModifier.get(ArmEnum.CAVALRY).put(FormationEnum.FLEE, 0.25D);

        attackerTypeModifier.put(ArmEnum.ARTILLERY, new HashMap<FormationEnum, Double>());
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.COLUMN, 1.00D);
        attackerTypeModifier.get(ArmEnum.ARTILLERY).put(FormationEnum.FLEE, 0.10D);
    }


    private final static Logger LOGGER = LoggerFactory.getLogger(BaseOrderMeleeProcessor.class);

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public BaseOrderMeleeProcessor(MeleeProcessor meleeProcessor) {
        this.meleeProcessor = meleeProcessor;
        additionalOrderBrigadeFilter = new AdditionalOrderBrigadeFilter();
    }

    /**
     * Finds the side of a nation.
     *
     * @param nation the nation
     * @return the side number (0 or 1)
     */
    protected int findSide(Nation nation) {
        return meleeProcessor.getParent().getBattleField().getSide(0).contains(nation) ? 0 : 1;
    }

    public void process(Brigade brigade, Order order) {

        if (brigade.isRouting()) {
            LOGGER.debug("{} is routing, skipping melee.", brigade);
            return;
        }

        int side = meleeProcessor.getParent().getSideBrigades()[0].contains(brigade) ? 0 : 1;
        int enemySide = side == 0 ? 1 : 0;

        Brigade target = findTarget(enemySide, additionalOrderBrigadeFilter, brigade, order);

        if (target != null) {
            performMeleeAttack(brigade, target);
        } else {
            performOrderMeleeAction(brigade, order);
        }
    }

    /**
     * This method must be overridden to define order-specific melee behaviour.
     */
    protected abstract void performOrderMeleeAction(Brigade brigade, Order order);

    private Brigade findTarget(int enemySide, AdditionalOrderBrigadeFilter additionalOrderBrigadeFilter,
                               Brigade brigade, Order order) {
        FieldBattleSector currentLocation = meleeProcessor.getParent().getSector(brigade);

        Set<FieldBattleSector> neighbours = MapUtils.getHorizontalAndVerticalNeighbours(currentLocation);
        Set<Brigade> enemiesInNeighbours = meleeProcessor.getParent().findBrigadesOfSide(neighbours, enemySide);
        Brigade targetEnemy = null;

        if (!enemiesInNeighbours.isEmpty()) {
            LOGGER.debug("{} has {} adjacent enemies, it will participate in melee!", new Object[]{brigade, enemiesInNeighbours.size()});
            Set<Brigade> preferredEnemiesInNeighbours = additionalOrderBrigadeFilter.filterEnemies(enemiesInNeighbours, order);
            targetEnemy = !preferredEnemiesInNeighbours.isEmpty() ?
                    ProcessorUtils.getRandom(preferredEnemiesInNeighbours) :
                    ProcessorUtils.getRandom(enemiesInNeighbours);
        } else {
            LOGGER.debug("{} doesn't have adjacent enemies, it will not participate in melee", new Object[]{brigade});
        }

        return targetEnemy;
    }

    private void performMeleeAttack(Brigade attacker, Brigade target) {
        performMeleeAttack(attacker, target, true);
    }

    private void performMeleeAttack(Brigade attacker, Brigade target, boolean includeRandomnessModifier) {

        // If cavalry attacks infantry, the infantry does a morale check at
        // -20%, and if it passes it forms a square (if it can).
        if (attacker.getArmTypeEnum() == ArmEnum.CAVALRY
                && target.getArmTypeEnum() == ArmEnum.INFANTRY
                && !target.isRouting()) {

            int moraleModifier = -20;

            // Defending target brigades that are influenced by a Stout Defender commander receive a +5% morale bonus
            Order targetOrder = meleeProcessor.getParent().findCurrentOrder(target);
            if (targetOrder.getOrderTypeEnum() == OrdersEnum.DEFEND_POSITION
                    && meleeProcessor.getParent().getCommanderProcessor().influencedByCommanderOfType(target, CommanderType.STOUT_DEFENDER)) {
                LOGGER.trace("{} is attacking {}, whose order is Defend Position and is influenced by a Stout Defender commander, target receives +5% morale bonus",
                        new Object[]{attacker, target});
                moraleModifier += 5;
            }

            MoraleStatusEnum moraleStatusAfterMoraleCheck = meleeProcessor.getParent().getMoraleChecker().checkAndSetMorale(target, moraleModifier);
            if (moraleStatusAfterMoraleCheck == MoraleStatusEnum.NORMAL
                    || moraleStatusAfterMoraleCheck == MoraleStatusEnum.DISORDER) {

                if (ArmyUtils.canFormSquare(target)) {
                    target.setFormationEnum(FormationEnum.SQUARE);
                }
            }
            LOGGER.debug("Cavalry attacks infantry, infantry performed a morale check with final status: {} and formation: {}",
                    new Object[]{moraleStatusAfterMoraleCheck, target.getFormation()});
        }

        FieldBattleSector attackerPosition = meleeProcessor.getParent().getSector(attacker);
        FieldBattleSector targetPosition = meleeProcessor.getParent().getSector(target);
        FormationEnum attackerFormation = attacker.getFormationEnum();

        double totalBrigadeToBrigadeDamage = 0;

        int targetBrigadeHeadCount = 0;
        for (Battalion battalion : target.getBattalions()) {
            targetBrigadeHeadCount += battalion.getHeadcount();
        }

        double brigadeVsBrigadeModifiers = 1;

        // Altitude difference: if one brigade is on higher terrain than another, it has 10% more effectiveness
        if (attackerPosition.getAltitude() > targetPosition.getAltitude()) {
            brigadeVsBrigadeModifiers *= 1.10d;
        }

        int attackCount = meleeProcessor.increaseTargetCount(target);
        LOGGER.trace("Attack count is {}", attackCount);
        if (attackCount == 2 || attackCount == 3) {
            LOGGER.trace("{} is the 2nd or 3rd brigade to attack target, 20% bonus damage", attacker);
            brigadeVsBrigadeModifiers *= 1.20d;
        } else if (attackCount == 4) {
            LOGGER.trace("{} is the 4th brigade to attack target, 20% bonus damage", attacker);
            brigadeVsBrigadeModifiers *= 1.50d;
        }

        if (attacker.getArmTypeEnum() == ArmEnum.CAVALRY
                && meleeProcessor.getParent().getCommanderProcessor().influencedByCommanderOfType(attacker, CommanderType.CAVALRY_LEADER)) {
            // Cavalry troops receive a 10% bonus when influenced by a cavalry leader
            LOGGER.debug("{} is a cavalry influenced by a commander that is a cavalry leader, 10% bonus", attacker);
            brigadeVsBrigadeModifiers *= 1.10d;
        }

        // if the brigade is in range of a commander it receives an experience bonus +1
        int commanderExperienceBonus = calculateCommanderExperienceBonus(attacker);
        LOGGER.debug("Commander experience bonus: {}", commanderExperienceBonus);

        LOGGER.debug("{} vs {}", new Object[]{attacker, target});
        for (Battalion attackerBattalion : attacker.getBattalions()) {

            ArmEnum attackerBattalionArm = ArmyUtils.findArm(attackerBattalion);

            double battalionFactor = calculateBattalionFactor(includeRandomnessModifier, attackerFormation, commanderExperienceBonus,
                    attackerBattalion, attackerBattalionArm);

            double battalionVsBrigadeModifiers = 1;

            // Cavalry attacking square
            if (attackerBattalionArm == ArmEnum.CAVALRY && target.getFormationEnum() == FormationEnum.SQUARE) {
                if (attackerBattalion.getType().getTroopSpecsLc()) {
                    // Lancers cavalry attacking square has 60% less effectiveness
                    battalionVsBrigadeModifiers = battalionVsBrigadeModifiers * 0.4d;
                } else {
                    // All other cases, cavalry attacking square has 90% less effectiveness
                    battalionVsBrigadeModifiers = battalionVsBrigadeModifiers * 0.1d;
                }
            }

            for (Battalion targetBattalion : target.getBattalions()) {

                double battalionToBattalionFactor = battalionFactor
                        * targetBattalion.getHeadcount() / targetBrigadeHeadCount;


                ArmEnum targetBattalionArm = ArmyUtils.findArm(targetBattalion);

                double battalionLevelModifiers = 1;

                // Light cavalry has 35% less effectiveness against infantry in forests
                if (attackerBattalion.getType().getTroopSpecsLc() && targetBattalionArm == ArmEnum.INFANTRY && targetPosition.isForest()) {
                    battalionLevelModifiers *= 0.65d;
                }

                // Light cavalry has 20% less effectiveness against infantry in bushes
                if (attackerBattalion.getType().getTroopSpecsLc() && targetBattalionArm == ArmEnum.INFANTRY && targetPosition.isBush()) {
                    battalionLevelModifiers *= 0.80d;
                }

                // Heavy cavalry has 40% less effectiveness against infantry in bushes
                if (attackerBattalion.getType().getTroopSpecsCu() && targetBattalionArm == ArmEnum.INFANTRY && targetPosition.isBush()) {
                    battalionLevelModifiers *= 0.60d;
                }

                // Heavy cavalry has 20% less effectiveness against light cavalry
                if (attackerBattalion.getType().getTroopSpecsCu() && targetBattalion.getType().getTroopSpecsLc()) {
                    battalionLevelModifiers *= 0.80d;
                }

                // Cavalry has 60% less effectiveness vs infantry or artillery in entrenchments
                if (attackerBattalionArm == ArmEnum.CAVALRY && targetPosition.hasSectorEntrenchment()
                        && (targetBattalionArm == ArmEnum.INFANTRY || targetBattalionArm == ArmEnum.ARTILLERY)) {
                    battalionLevelModifiers *= 0.40d;
                }

                // Infantry has 50% less effectiveness vs infantry or artillery in entrenchments
                if (attackerBattalionArm == ArmEnum.INFANTRY && targetPosition.hasSectorEntrenchment()
                        && (targetBattalionArm == ArmEnum.INFANTRY || targetBattalionArm == ArmEnum.ARTILLERY)) {
                    battalionLevelModifiers *= 0.50d;
                }

                // Infantry has 50% less effectiveness vs infantry or artillery in entrenchments
                if (attackerBattalionArm == ArmEnum.INFANTRY && targetPosition.hasSectorEntrenchment()
                        && (targetBattalionArm == ArmEnum.INFANTRY || targetBattalionArm == ArmEnum.ARTILLERY)) {
                    battalionLevelModifiers *= 0.50d;
                }

                double battalionToBattalionDamage = battalionToBattalionFactor * brigadeVsBrigadeModifiers *
                        battalionVsBrigadeModifiers * battalionLevelModifiers;

                totalBrigadeToBrigadeDamage += battalionToBattalionDamage;
                meleeProcessor.increaseBattalionDamage(targetBattalion, battalionToBattalionDamage);


//        		LOGGER.debug("|-- battalion {} inflicting damage of {}(tot:{}) men to battalion {}", 
//        				new Object[]{attackerBattalion, battalionToBattalionDamage, damageToInflict.get(targetBattalion), targetBattalion});
            }
//        	LOGGER.debug("|-- Total damage so far: {}", totalBrigadeToBrigadeDamage);

        }

        LOGGER.debug("TOTAL: {} inflicting damage of {} men to {}",
                new Object[]{attacker, totalBrigadeToBrigadeDamage, target});

        int side = meleeProcessor.getParent().findSide(attacker);
        meleeProcessor.getParent().getFieldBattleLog().logMeleeAttack(side, attacker, target, totalBrigadeToBrigadeDamage);

    }

    protected int calculateCommanderExperienceBonus(Brigade attacker) {
        boolean attackerInfluencedByCommander = meleeProcessor.getParent().getCommanderProcessor().influencedByCommander(attacker);
        return attackerInfluencedByCommander ? 1 : 0;
    }

    protected double calculateBattalionFactor(boolean includeRandomnessModifier,
                                              FormationEnum attackerFormation, int commanderExperienceBonus,
                                              Battalion attackerBattalion, ArmEnum attackerBattalionArm) {
        double battalionFactor = attackerBattalion.getType().getHandCombat()
                * (attackerBattalion.getExperience() + commanderExperienceBonus)
                * attackerBattalion.getHeadcount()
                * (attackerTypeModifier.get(attackerBattalionArm).get(attackerFormation) == null
                ? 1D
                : attackerTypeModifier.get(attackerBattalionArm).get(attackerFormation))
                / 1500;

        if (includeRandomnessModifier) {
            battalionFactor *= MathUtils.generateRandomDoubleInRange(MELEE_RANDOMNESS_MODIFIER_MIN, MELEE_RANDOMNESS_MODIFIER_MAX);
        }
        return battalionFactor;
    }
}
