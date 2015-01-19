package com.eaw1805.battles.tactical.result;

import com.eaw1805.battles.tactical.TacticalBattleProcessor;
import com.eaw1805.data.dto.converters.BattalionConverter;
import com.eaw1805.data.dto.web.army.BattalionDTO;
import com.eaw1805.data.model.army.Battalion;

import java.io.Serializable;
import java.util.List;

/**
 * Keeps track of the statistics of a single tactical battle round.
 */
public class RoundStatistics
        implements Serializable {

    /**
     * Required by Serializable interface.
     */
    static final long serialVersionUID = 42L; //NOPMD

    /**
     * Side A won the battle.
     */
    public final static int SIDE_A = 0;

    /**
     * Side B won the battle.
     */
    public final static int SIDE_B = 1;

    /**
     * Undecided.
     */
    public final static int SIDE_NONE = -1;

    /**
     * The round.
     */
    private int round;

    /**
     * The battalions of each side.
     */
    private List<BattalionDTO>[] sideBattalions;

    /**
     * The statistics for a tactical battle round for each side.
     */
    private double[][] sideStat;

    /**
     * Statistics for the army sizes of each side.
     */
    private final int[][][] armySizes;

    /**
     * Default constructor.
     */
    public RoundStatistics() {
        // empty constructor for use by spring.
        armySizes = new int[2][5][4];
    }

    /**
     * Normal constructor used by tactical battle processor.
     *
     * @param thisRound  the round of the battle.
     * @param battalions the battalions that participate in the battle round.
     * @param stats      combat points and casualties resulted in this round.
     */
    @SuppressWarnings("unchecked")
    public RoundStatistics(final int thisRound,
                           final List<Battalion>[] battalions,
                           final double[][] stats) {
        round = thisRound;
        sideBattalions = new List[2];
        for (int side = 0; side < 2; side++) {
            sideBattalions[side] = BattalionConverter.convert(battalions[side]);
        }

        sideStat = stats.clone();
        armySizes = calcArmySizes(battalions);
    }

    /**
     * Get the round of the naval battle.
     *
     * @return the round.
     */
    public int getRound() {
        return round;
    }

    /**
     * Set the round of the naval battle.
     *
     * @param round the round of the naval battle.
     */
    public void setRound(final int round) {
        this.round = round;
    }

    /**
     * Get the battalions of each side.
     *
     * @return the battalions of each side.
     */
    public List<BattalionDTO>[] getSideBattalions() {
        return sideBattalions;
    }

    /**
     * The army sizes at the end of the round.
     *
     * @return army sizes at the end of the round.
     */
    public int[][][] getArmySizes() {
        return armySizes.clone();
    }

    /**
     * Combat points and casualties resulted in this round.
     *
     * @return combat points and casualties resulted in this round.
     */
    public double[][] getSideStat() {
        return sideStat;
    }

    /**
     * Calculate the size of the army per troop type.
     *
     * @param battalions the battalions that participate in the battle round.
     * @return army sizes for each side per troop type.
     */
    public int[][][] calcArmySizes(final List<Battalion>[] battalions) {
        final int[][][] armyTypes = new int[2][5][4];

        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : battalions[side]) {
                if (battalion.getHeadcount() >= 0) {
                    if (battalion.getType().isInfantry()) {
                        armyTypes[side][TacticalBattleProcessor.TROOP_INF][TacticalBattleProcessor.TPE_BATTCNT]++;
                        armyTypes[side][TacticalBattleProcessor.TROOP_INF][TacticalBattleProcessor.TPE_BATTSIZE] += battalion.getHeadcount();

                        // check if battalion is fleeing
                        if (battalion.isFleeing()) {
                            armyTypes[side][TacticalBattleProcessor.TROOP_INF][TacticalBattleProcessor.TPE_FL_BATTCNT]++;
                            armyTypes[side][TacticalBattleProcessor.TROOP_INF][TacticalBattleProcessor.TPE_FL_BATTSIZE] += battalion.getHeadcount();
                        }

                    } else if (battalion.getType().isEngineer()) {
                        armyTypes[side][TacticalBattleProcessor.TROOP_ENG][TacticalBattleProcessor.TPE_BATTCNT]++;
                        armyTypes[side][TacticalBattleProcessor.TROOP_ENG][TacticalBattleProcessor.TPE_BATTSIZE] += battalion.getHeadcount();

                        // check if battalion is fleeing
                        if (battalion.isFleeing()) {
                            armyTypes[side][TacticalBattleProcessor.TROOP_ENG][TacticalBattleProcessor.TPE_FL_BATTCNT]++;
                            armyTypes[side][TacticalBattleProcessor.TROOP_ENG][TacticalBattleProcessor.TPE_FL_BATTSIZE] += battalion.getHeadcount();
                        }

                    } else if (battalion.getType().isCavalry()) {
                        armyTypes[side][TacticalBattleProcessor.TROOP_CAV][TacticalBattleProcessor.TPE_BATTCNT]++;
                        armyTypes[side][TacticalBattleProcessor.TROOP_CAV][TacticalBattleProcessor.TPE_BATTSIZE] += battalion.getHeadcount();

                        // check if battalion is fleeing
                        if (battalion.isFleeing()) {
                            armyTypes[side][TacticalBattleProcessor.TROOP_CAV][TacticalBattleProcessor.TPE_FL_BATTCNT]++;
                            armyTypes[side][TacticalBattleProcessor.TROOP_CAV][TacticalBattleProcessor.TPE_FL_BATTSIZE] += battalion.getHeadcount();
                        }

                    } else if (battalion.getType().isArtillery() || battalion.getType().isMArtillery()) {
                        armyTypes[side][TacticalBattleProcessor.TROOP_ART][TacticalBattleProcessor.TPE_BATTCNT]++;
                        armyTypes[side][TacticalBattleProcessor.TROOP_ART][TacticalBattleProcessor.TPE_BATTSIZE] += battalion.getHeadcount();

                        // check if battalion is fleeing
                        if (battalion.isFleeing()) {
                            armyTypes[side][TacticalBattleProcessor.TROOP_ART][TacticalBattleProcessor.TPE_FL_BATTCNT]++;
                            armyTypes[side][TacticalBattleProcessor.TROOP_ART][TacticalBattleProcessor.TPE_FL_BATTSIZE] += battalion.getHeadcount();
                        }
                    }

                    armyTypes[side][TacticalBattleProcessor.TROOP_TOT][TacticalBattleProcessor.TPE_BATTCNT]++;
                    armyTypes[side][TacticalBattleProcessor.TROOP_TOT][TacticalBattleProcessor.TPE_BATTSIZE] += battalion.getHeadcount();

                    // check if battalion is fleeing
                    if (battalion.isFleeing()) {
                        armyTypes[side][TacticalBattleProcessor.TROOP_TOT][TacticalBattleProcessor.TPE_FL_BATTCNT]++;
                        armyTypes[side][TacticalBattleProcessor.TROOP_TOT][TacticalBattleProcessor.TPE_FL_BATTSIZE] += battalion.getHeadcount();
                    }
                }
            }
        }

        return armyTypes;
    }

}
