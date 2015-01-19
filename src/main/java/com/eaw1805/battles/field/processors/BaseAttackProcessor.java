package com.eaw1805.battles.field.processors;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Base class for attack processors.
 *
 * @author fragkakis
 */
public abstract class BaseAttackProcessor extends BaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAttackProcessor.class);
    /**
     * This map holds the damage to be inflicted to each battalion after 2 half round (a full round).
     */
    protected Map<Battalion, Double> damageToInflict;

    public BaseAttackProcessor(FieldBattleProcessor parent) {
        super(parent);
    }

    /**
     * Returns the {@link AttackType} of an attack processor. Must be implemented by concrete inheriting classes.
     *
     * @return the attack type
     */
    protected abstract AttackType getAttackType();

    protected void inflictDamageToSide(int side) {

        if (damageToInflict.isEmpty()) {
            // no damage to inflict, return
            return;
        }

        List<Brigade> sideBrigades0Copy = new ArrayList<Brigade>(getParent().getSideBrigades()[side]);

        for (Brigade brigade : sideBrigades0Copy) {

            int initialBrigadeHeadcount = ArmyUtils.findBrigadeHeadCount(brigade);

            HashSet<Battalion> battalionsCopy = new HashSet<Battalion>(brigade.getBattalions());

            for (Battalion battalion : battalionsCopy) {
                Double battalionDamage = damageToInflict.get(battalion);
                if (battalionDamage != null) {
                    battalion.setHeadcount((int) Math.round(battalion.getHeadcount() - battalionDamage));
                }

                if (battalion.getHeadcount() <= 0) {
                    getParent().markBattalionAsDead(battalion);
                }
            }

            handleCommanderDamage(brigade);

            if (brigade.getFieldBattlePosition().exists()
                    && !brigade.getBattalions().isEmpty()
                    && !brigade.isRouting()) {
                // perform morale check when significant losses
                int finalBrigadeHeadcount = ArmyUtils.findBrigadeHeadCount(brigade);

                int losses = initialBrigadeHeadcount - finalBrigadeHeadcount;
                double lossesPercentage = (double) (losses * 100) / (double) initialBrigadeHeadcount;

                int moraleModifier = 0;

                LOGGER.debug("{}: Losses percentage: {}", new Object[]{brigade, lossesPercentage});

                if (5d <= lossesPercentage && lossesPercentage < 7.5d) {
                    moraleModifier += -5;

                } else if (7.5d <= lossesPercentage && lossesPercentage < 10d) {
                    moraleModifier += -10;

                } else if (10d <= lossesPercentage && lossesPercentage < 15d) {
                    moraleModifier += -15;

                } else if (15d <= lossesPercentage) {
                    moraleModifier += -20;

                }

                if (0d <= lossesPercentage && lossesPercentage < 2.5d) {
                    // do nothing
                    LOGGER.debug("{}: Few or no losses, skipping morale check", brigade);

                } else {

                    // Defending target brigades that are influenced by a Stout Defender commander receive a +5% morale bonus
                    Order brigadeOrder = getParent().findCurrentOrder(brigade);
                    if (brigadeOrder.getOrderTypeEnum() == OrdersEnum.DEFEND_POSITION
                            && getParent().getCommanderProcessor().influencedByCommanderOfType(brigade, CommanderType.STOUT_DEFENDER)) {
                        LOGGER.trace("{} whose order is Defend Position and is influenced by a Stout Defender commander receives damage, +5% morale bonus",
                                new Object[]{brigade});
                        moraleModifier += 5;
                    }

                    getParent().getMoraleChecker().checkAndSetMorale(brigade, moraleModifier);
                }

            }
        }
    }

    private void handleCommanderDamage(Brigade brigade) {
        Commander commanderInBrigade = getParent().getCommanderProcessor().getCommanderInBrigade(brigade);
        if (commanderInBrigade != null) {
            // a commander is IN this brigade
            LOGGER.debug("{}, which is receiving damage, has a commander.", brigade);
            if (brigade.isRouting()) {

                // if brigade is routing, there is 2% change of getting hit and 2% change of getting captured
                int dice = MathUtils.generateRandomIntInRange(1, 100);
                LOGGER.debug("{} is routing, dice was {}", new Object[]{brigade, dice});
                if (1 <= dice && dice <= 2) {
                    getParent().getCommanderProcessor().commanderHasBeenHit(commanderInBrigade);
                } else if (3 <= dice && dice <= 4) {
                    getParent().getCommanderProcessor().commanderHasBeenCaptured(commanderInBrigade);
                }

            } else {

                switch (getAttackType()) {
                    case LONG_RANGE:
                        // 0.5% change of getting hit by long range damage
                        int diceLongRange = MathUtils.generateRandomIntInRange(1, 200);
                        LOGGER.debug("{} is receiving long range damage, dice is {}", new Object[]{brigade, diceLongRange});
                        if (diceLongRange == 1) {
                            getParent().getCommanderProcessor().commanderHasBeenHit(commanderInBrigade);
                        }
                        break;
                    case MELEE:
                    default:
                        // 1% change of getting hit by melee damage
                        int diceMelee = MathUtils.generateRandomIntInRange(1, 100);
                        LOGGER.debug("{} is receiving melee damage, dice is {}", new Object[]{brigade, diceMelee});
                        if (diceMelee == 1) {
                            getParent().getCommanderProcessor().commanderHasBeenHit(commanderInBrigade);
                        }
                        break;
                }
            }
        }
    }

    protected void inflictDamageToBattalion(Battalion battalion, int damage) {

        battalion.setHeadcount(battalion.getHeadcount() - damage);

        if (battalion.getHeadcount() <= 0) {
            getParent().markBattalionAsDead(battalion);
        }

    }

    public void increaseBattalionDamage(Battalion battalion, double additionalDamage) {
        if (damageToInflict.get(battalion) == null) {
            damageToInflict.put(battalion, 0D);
        }

        Double newBattalionDamage = damageToInflict.get(battalion) + additionalDamage;
        damageToInflict.put(battalion, newBattalionDamage);
    }

    public Map<Battalion, Double> getDamageToInflict() {
        return damageToInflict;
    }

    /**
     * Enumeration of attack types.
     *
     * @author fragkakis
     */
    protected static enum AttackType {
        MELEE,
        LONG_RANGE
    }

}
