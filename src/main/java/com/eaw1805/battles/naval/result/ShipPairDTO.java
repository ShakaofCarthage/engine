package com.eaw1805.battles.naval.result;

import com.eaw1805.data.dto.converters.ShipConverter;
import com.eaw1805.data.dto.web.fleet.ShipDTO;

import java.io.Serializable;

/**
 * A pair of Ships.
 */
public class ShipPairDTO
        implements Serializable {

    /**
     * Required by Serializable interface.
     */
    static final long serialVersionUID = 42L; //NOPMD

    /**
     * The 1st ship of the pair.
     */
    private ShipDTO shipA;

    /**
     * The 2nd ship of the pair.
     */
    private ShipDTO shipB;

    /**
     * Combat points of each ship.
     */
    private int[] combatPoints;

    /**
     * Combat points of each ship for 2nd round.
     */
    private int[] combatPoints2;

    /**
     * Combat points of each ship for 3rd round.
     */
    private int[] combatPoints3;

    /**
     * Lost tonnage of each ship.
     */
    private int[] lossTonnage;

    /**
     * Lost marines of each ship.
     */
    private int[] lossMarines;

    /**
     * Lost marines of each ship for 2nd round.
     */
    private int[] lossMarines2;

    /**
     * Lost marines of each ship for 3rd round.
     */
    private int[] lossMarines3;

    /**
     * If the ship was captured during 1st round of hand-to-hand combat.
     */
    private boolean[] captured1;

    /**
     * If the ship was captured during 2nd round of hand-to-hand combat.
     */
    private boolean[] captured2;

    /**
     * If the ship was captured during 3rd round of hand-to-hand combat.
     */
    private boolean[] captured3;

    /**
     * Empty constructor.
     */
    public ShipPairDTO(final ShipPair shipPair) {
        shipA = ShipConverter.convert(shipPair.getShipA(), null);
        shipB = ShipConverter.convert(shipPair.getShipB(), null);
        combatPoints = shipPair.getCombatPoints();
        combatPoints2 = shipPair.getCombatPoints2();
        combatPoints3 = shipPair.getCombatPoints3();
        lossTonnage = shipPair.getLossTonnage();
        lossMarines = shipPair.getLossMarines();
        lossMarines2 = shipPair.getLossMarines2();
        lossMarines3 = shipPair.getLossMarines3();
        captured1 = shipPair.getCaptured1();
        captured2 = shipPair.getCaptured2();
        captured3 = shipPair.getCaptured3();
    }

    /**
     * Get the 1st ship of the pair.
     *
     * @return the 1st ship of the pair.
     */

    public ShipDTO getShipA() {
        return shipA;
    }

    /**
     * Get the 2nd ship of the pair.
     *
     * @return the 2nd ship of the pair.
     */
    public ShipDTO getShipB() {
        return shipB;
    }

    /**
     * Get the combat points of the ship.
     *
     * @return the combat points.
     */
    public int[] getCombatPoints() {
        return combatPoints.clone();
    }

    /**
     * Get the combat points of the ship for the 2nd round.
     *
     * @return the combat points.
     */
    public int[] getCombatPoints2() {
        return combatPoints2.clone();
    }

    /**
     * Get the combat points of the ship for the 3rd round.
     *
     * @return the combat points.
     */
    public int[] getCombatPoints3() {
        return combatPoints3.clone();
    }

    /**
     * Get the lost tonnage of the ship.
     *
     * @param side the side of the pair.
     * @return the lost tonnage of the ship.
     */
    public int getLossTonnage(final int side) {
        return lossTonnage[side];
    }

    /**
     * et the lost tonnage of the ship.
     *
     * @return the lost tonnage of the ship.
     */
    public int[] getLossTonnage() {
        return lossTonnage.clone();
    }

    /**
     * Get the lost marines of the ship.
     *
     * @param side  the side of the pair.
     * @param round the round of the battle (for hand-to-hand).
     * @return the lost marines of the ship.
     */
    public int getLossMarines(final int side, final int round) {
        int lostMarines;
        switch (round) {
            case 1:
                lostMarines = lossMarines[side];
                break;

            case 2:
                lostMarines = lossMarines2[side];
                break;

            case 3:
            default:
                lostMarines = lossMarines3[side];
                break;
        }
        return lostMarines;
    }

    /**
     * Set the lost marines of the ship.
     *
     * @param side  the side of the pair.
     * @param round the round of the battle (for hand-to-hand).
     * @param value the lost marines of the ship.
     */
    public void setLossMarines(final int side, final int round, final int value) {
        switch (round) {
            case 1:
                lossMarines[side] = value;
                break;

            case 2:
                lossMarines2[side] = value;
                break;

            case 3:
            default:
                lossMarines3[side] = value;
                break;
        }
    }

    /**
     * Get the lost marines of the ship.
     *
     * @return the lost marines of the ship.
     */
    public int[] getLossMarines() {
        return lossMarines.clone();
    }

    /**
     * Set the lost marines of the ship.
     *
     * @param value lost marines of the ship.
     */
    public void setLossMarines(final int[] value) {
        this.lossMarines = value.clone();
    }

    /**
     * Get the lost marines of the ship for 2nd round.
     *
     * @return the lost marines of the ship.
     */
    public int[] getLossMarines2() {
        return lossMarines2.clone();
    }

    /**
     * Set the lost marines of the ship for 2nd round.
     *
     * @param value lost marines of the ship.
     */
    public void setLossMarines2(final int[] value) {
        this.lossMarines2 = value.clone();
    }

    /**
     * Get the lost marines of the ship for 3rd round.
     *
     * @return the lost marines of the ship.
     */
    public int[] getLossMarines3() {
        return lossMarines3.clone();
    }

    /**
     * Set the lost marines of the ship for 3rd round.
     *
     * @param value lost marines of the ship.
     */
    public void setLossMarines3(final int[] value) {
        this.lossMarines3 = value.clone();
    }

    /**
     * Get the captured flags for each side after the 1st round.
     *
     * @return the captured flags for each side after the 1st round.
     */
    public boolean[] getCaptured1() {
        return captured1.clone();
    }

    /**
     * Set the captured flags for each side after the 1st round.
     *
     * @param value the captured flags for each side after the 1st round.
     */
    public void setCaptured1(final boolean[] value) {
        captured1 = value.clone();
    }

    /**
     * Get the captured flags for each side after the 2nd round.
     *
     * @return the captured flags for each side after the 2nd round.
     */
    public boolean[] getCaptured2() {
        return captured2.clone();
    }

    /**
     * Set the captured flags for each side after the 2nd round.
     *
     * @param value the captured flags for each side after the 2nd round.
     */
    public void setCaptured2(final boolean[] value) {
        captured2 = value.clone();
    }

    /**
     * Get the captured flags for each side after the 3rd round.
     *
     * @return the captured flags for each side after the 3rd round.
     */
    public boolean[] getCaptured3() {
        return captured3;
    }

}
