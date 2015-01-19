package com.eaw1805.battles.tactical;

import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.map.Sector;

/**
 * Constitutes a common ancestor for all round processors for tactical battles.
 */
public abstract class AbstractTacticalBattleRound
        implements TerrainConstants, ReportConstants, NewsConstants {

    /**
     * Round 0: Maneuvering of troops.
     */
    public final static int ROUND_INIT = 0;

    /**
     * Round 1: Artillery long-range combat (Heavy and Light artillery only).
     */
    public final static int ROUND_ARTILLERY_HLA = 1;

    /**
     * Round 2: Mounted Artillery long-range combat (Mounted artillery only).
     */
    public final static int ROUND_ARTILLERY_MA = 2;

    /**
     * Round 3: Morale Check +20%.
     */
    public final static int ROUND_MORALE_1 = 3;

    /**
     * Round 4: Skirmishers long-range combat (Sk units with Range of 7).
     */
    public final static int ROUND_LONGRANGE_SK = 4;

    /**
     * Round 5: Troop long-range combat (all units with Range of 3-6, all artillery).
     */
    public final static int ROUND_LONGRANGE = 5;

    /**
     * Round 6: Morale Check +10%.
     */
    public final static int ROUND_MORALE_2 = 6;

    /**
     * Round 7: Hand-to-Hand combat (all units)
     */
    public final static int ROUND_HANDCOMBAT = 7;

    /**
     * Round 8: Morale Check.
     */
    public final static int ROUND_MORALE_3 = 8;

    /**
     * Round 9: Cavalry Hand-to-Hand combat (cavalry only).
     */
    public final static int ROUND_HANDCOMBAT_CAV = 9;

    /**
     * Round 10: Morale Check (only for units attacked by cavalry).
     */
    public final static int ROUND_MORALE_4 = 10;

    /**
     * Round 11: Disengagement Hand-to-Hand combat.
     */
    public final static int ROUND_HANDCOMBAT_D = 11;

    /**
     * Round 12: Disengagement long-range combat (Artillery & Skirmish units only).
     */
    public final static int ROUND_HANDCOMBAT_LR = 12;

    /**
     * End battle: Determination of Winner.
     */
    public final static int ROUND_WINNER = 13;

    /**
     * Round 13 : Pursuit.
     */
    public final static int ROUND_PURSUIT = 14;

    /**
     * Aftermath: Raise Experience.
     */
    public final static int ROUND_AFTERMATH = 15;

    /**
     * Aftermath: Capture Commanders
     */
    public final static int ROUND_CAPTURE_COMM = 16;

    /**
     * The parent engine.
     */
    private final transient TacticalBattleProcessor parent;

    /**
     * The round.
     */
    private int round;

    /**
     * Default constructor.
     *
     * @param caller    the processor requesting the execution of this round.
     * @param thisRound the round of the battle.
     */
    public AbstractTacticalBattleRound(final TacticalBattleProcessor caller, final int thisRound) {
        parent = caller;
        round = thisRound;
    }

    /**
     * Get the parent engine.
     *
     * @return instance of the parent engine.
     */
    protected TacticalBattleProcessor getParent() {
        return parent;
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
     * @param value the round of the naval battle.
     */
    public void setRound(final int value) {
        round = value;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @param battalion to examine for the particular terrain type.
     * @return the terrain factor affecting the combat points.
     */
    public double terrainFactor(final Battalion battalion) {

        if (battalion.getType().isInfantry()) {
            if (battalion.getType().getFormationSk()) {
                return terrainFactorSk();

            } else {
                return terrainFactorInf();
            }

        } else if (battalion.getType().isArtillery()) {
            return terrainFactorAr();

        } else if (battalion.getType().isMArtillery()) {
            return terrainFactorMAr();

        } else if (battalion.getType().isCavalry()) {
            if (battalion.getType().getTroopSpecsLc()) {
                return terrainFactorLCav();

            } else {
                return terrainFactorCav();
            }
        }

        return 1d;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @return the terrain factor affecting the combat points.
     */
    double terrainFactorInf() {
        double value;
        switch (getParent().getField().getTerrain().getId()) {
            case TERRAIN_B:
            case TERRAIN_Q:
                value = 1d;
                break;

            case TERRAIN_H:
            case TERRAIN_K:
            case TERRAIN_T:
                value = .8d;
                break;

            case TERRAIN_W:
            case TERRAIN_G:
            case TERRAIN_D:
                value = .5d;
                break;

            case TERRAIN_J:
                value = .4d;
                break;

            case TERRAIN_S:
            default:
                value = .45d;
                break;
        }
        return value;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @return the terrain factor affecting the combat points.
     */
    double terrainFactorSk() {
        double value;
        switch (getParent().getField().getTerrain().getId()) {
            case TERRAIN_B:
            case TERRAIN_Q:
            case TERRAIN_H:
            case TERRAIN_K:
            case TERRAIN_T:
                value = 1d;
                break;

            case TERRAIN_W:
            case TERRAIN_G:
            case TERRAIN_D:
                value = .75d;
                break;

            case TERRAIN_J:
                value = .7d;
                break;

            case TERRAIN_S:
            default:
                value = .65d;
                break;
        }

        return value;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @return the terrain factor affecting the combat points.
     */
    public final double terrainFactorAr() {
        double value;
        switch (getParent().getField().getTerrain().getId()) {
            case TERRAIN_B:
            case TERRAIN_Q:
                value = 1d;
                break;

            case TERRAIN_H:
            case TERRAIN_K:
            case TERRAIN_T:
                value = .8d;
                break;

            case TERRAIN_W:
                value = .35d;
                break;

            case TERRAIN_G:
            case TERRAIN_D:
                value = .5d;
                break;

            case TERRAIN_J:
                value = .15d;
                break;

            case TERRAIN_S:
            default:
                value = .25d;
                break;
        }
        return value;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @return the terrain factor affecting the combat points.
     */
    protected double terrainFactorMAr() {
        double value;
        switch (getParent().getField().getTerrain().getId()) {
            case TERRAIN_B:
            case TERRAIN_Q:
                value = 1d;
                break;

            case TERRAIN_H:
            case TERRAIN_K:
            case TERRAIN_T:
                value = .9d;
                break;

            case TERRAIN_W:
                value = .5d;
                break;

            case TERRAIN_G:
            case TERRAIN_D:
                value = .75d;
                break;

            case TERRAIN_J:
                value = .15d;
                break;

            case TERRAIN_S:
            default:
                value = .25d;
                break;
        }
        return value;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @return the terrain factor affecting the combat points.
     */
    double terrainFactorLCav() {
        double value;
        switch (getParent().getField().getTerrain().getId()) {
            case TERRAIN_B:
            case TERRAIN_Q:
            case TERRAIN_H:
            case TERRAIN_K:
            case TERRAIN_T:
                value = 1d;
                break;

            case TERRAIN_W:
                value = .6d;
                break;

            case TERRAIN_G:
            case TERRAIN_D:
                value = .5d;
                break;

            case TERRAIN_J:
                value = .25d;
                break;

            case TERRAIN_S:
            default:
                value = .35d;
                break;
        }
        return value;
    }

    /**
     * Determine the effect of the terrain on the combat points produced by each participating battalion.
     *
     * @return the terrain factor affecting the combat points.
     */
    double terrainFactorCav() {
        double value;
        switch (getParent().getField().getTerrain().getId()) {
            case TERRAIN_B:
            case TERRAIN_Q:
                value = 1d;
                break;

            case TERRAIN_H:
            case TERRAIN_K:
            case TERRAIN_T:
                value = .8d;
                break;

            case TERRAIN_W:
                value = .35d;
                break;

            case TERRAIN_G:
            case TERRAIN_D:
                value = .5d;
                break;

            case TERRAIN_J:
                value = .15d;
                break;

            case TERRAIN_S:
            default:
                value = .25d;
                break;
        }
        return value;
    }

    /**
     * Identify if sector is a home region, inside sphere of influence, or outside of the receiving nation.
     *
     * @param sector   the sector to examine.
     * @param receiver the receiving nation.
     * @return 1 if home region, 2 if in sphere of influence, 3 if outside.
     */
    protected final int getSphere(final Sector sector, final Nation receiver) {
        final char thisNationCodeLower = String.valueOf(receiver.getCode()).toLowerCase().charAt(0);
        final char thisSectorCodeLower = String.valueOf(sector.getPoliticalSphere()).toLowerCase().charAt(0);
        int sphere = 1;

        // x2 and x3 are used only for European units if they are within SOI or outside SOI
        if (sector.getPosition().getRegion().getId() != RegionConstants.EUROPE) {
            return 1;
        }

        // Check if this is not home region
        if (thisNationCodeLower != thisSectorCodeLower) {
            sphere = 2;

            // Check if this is outside sphere of influence
            if (receiver.getSphereOfInfluence().toLowerCase().indexOf(thisSectorCodeLower) < 0) {
                sphere = 3;
            }
        }

        return sphere;
    }

}
