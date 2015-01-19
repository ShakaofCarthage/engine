package com.eaw1805.battles.field.victory;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * This class handles the pursuit of the losing side after a field battle.
 *
 * @author fragkakis
 */
public class PursuitProcessor {

    private FieldBattleProcessor parent;

    private static final Logger LOGGER = LoggerFactory.getLogger(PursuitProcessor.class);

    /**
     * Constructor.
     *
     * @param parent
     */
    public PursuitProcessor(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    /**
     * This is the central method of the field battle.
     *
     * @param winningSide the winning side
     */
    public void performPursuit(int winningSide) {

        int totalPursuitPoints = calculatePursuitPoints(winningSide, true);

        int losingSide = winningSide == 0 ? 1 : 0;

        inflictDamage(totalPursuitPoints, losingSide);

    }

    /**
     * Calculates the pursuit points of the winning side (how many men it will kill in pursuit).
     *
     * @param winningSide       the winning side
     * @param includeRandomness true or false to include randomness (false for testing purposes)
     * @return the pursuit points
     */
    public int calculatePursuitPoints(int winningSide, boolean includeRandomness) {

        List<Brigade> winningSideBrigades = parent.getSideBrigades()[winningSide];

        double cavalryLeaderPresentBonus = parent.getCommanderProcessor().sideHasAliveCommanderOfType(winningSide, CommanderType.CAVALRY_LEADER) ? 1.5d : 1d;
        int sideMorale = parent.getMoraleChecker().calculateArmyMorale(winningSide);
        int terrainFactor = parent.getBattleField().getField().getTerrain().getTerrainFactor();

        double totalPursuitPoints = 0d;

        for (Brigade brigade : winningSideBrigades) {

            // TODO: better check for dead brigades
            for (Battalion battalion : brigade.getBattalions()) {

                if (ArmyUtils.findArm(battalion) == ArmEnum.CAVALRY) {

                    totalPursuitPoints +=
                            battalion.getExperience()
                                    * battalion.getHeadcount()
                                    * (includeRandomness ? MathUtils.generateRandomIntInRange(1, 2) : 1)
                                    * (battalion.getType().getTroopSpecsLc() ? 2 : 1)
                                    * (battalion.getType().getTroopSpecsCu() ? 0.5d : 1d);
                }
            }
        }

        totalPursuitPoints *= terrainFactor;
        totalPursuitPoints *= (sideMorale / 100d);
        totalPursuitPoints *= cavalryLeaderPresentBonus;

        return (int) Math.round(totalPursuitPoints);
    }

    /**
     * Inflicts damage on the losing side. It distributes the casualties among the battalions based on their headcount.
     *
     * @param totalPursuitCasualties the number of casualties.
     * @param losingSide             the losing side.
     */
    public void inflictDamage(int totalPursuitCasualties, int losingSide) {

        Collection<Brigade> losingSideBrigades = parent.getSideBrigades()[losingSide];

        int losingSideHeadCount = ArmyUtils.findHeadCount(losingSideBrigades);

        double totalWeight = 0d;

        for (Brigade brigade : losingSideBrigades) {

            for (Battalion battalion : brigade.getBattalions()) {

                if (battalion.getHeadcount() <= 0) {
                    continue;
                }


                int initialBattalionHeadcount = battalion.getHeadcount();
                double battalionWeight = (double) initialBattalionHeadcount / (double) losingSideHeadCount;

                totalWeight += battalionWeight;

                int battalionCasualties = (int) Math.round(totalPursuitCasualties * battalionWeight);

                battalion.setHeadcount(initialBattalionHeadcount - battalionCasualties);

                if (battalion.getHeadcount() <= 0) {
                    parent.markBattalionAsDead(battalion);
                }

                LOGGER.debug("{} had {} men, lost {}, current headcount {}",
                        new Object[]{battalion, initialBattalionHeadcount, battalionCasualties, battalion.getHeadcount()});
            }
        }

        LOGGER.debug("Total weight is {} (should be around 1)", totalWeight);

    }

}
