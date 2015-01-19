package com.eaw1805.battles.naval.result;

import com.eaw1805.data.model.fleet.Ship;

/**
 * A pair of Ships.
 */
public class ShipPair {

    /**
     * Required by Serializable interface.
     */
    static final long serialVersionUID = 42L; //NOPMD

    /**
     * The 1st ship of the pair.
     */
    private Ship shipA;

    /**
     * The 2nd ship of the pair.
     */
    private Ship shipB;

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
    public ShipPair() {
        combatPoints = new int[2];
        combatPoints2 = new int[2];
        combatPoints3 = new int[2];
        lossTonnage = new int[2];
        lossMarines = new int[2];
        lossMarines2 = new int[2];
        lossMarines3 = new int[2];
        captured1 = new boolean[2];
        captured2 = new boolean[2];
        captured3 = new boolean[2];
    }

    /**
     * Simple constructor.
     *
     * @param shipA the 1st ship of the pair.
     * @param shipB the 2nd ship of the pair.
     */
    public ShipPair(final Ship shipA, final Ship shipB) {
        this.shipA = shipA;
        this.shipB = shipB;
        combatPoints = new int[2];
        combatPoints2 = new int[2];
        combatPoints3 = new int[2];
        lossTonnage = new int[2];
        lossMarines = new int[2];
        lossMarines2 = new int[2];
        lossMarines3 = new int[2];
        captured1 = new boolean[2];
        captured2 = new boolean[2];
        captured3 = new boolean[2];
    }

    /**
     * Get the 1st ship of the pair.
     *
     * @return the 1st ship of the pair.
     */
    public Ship getShipA() {
        return shipA;
    }

    /**
     * Set the 1st ship of the pair.
     *
     * @param value the 1st ship of the pair.
     */
    public void setShipA(final Ship value) {
        this.shipA = value;
    }

    /**
     * Get the 2nd ship of the pair.
     *
     * @return the 2nd ship of the pair.
     */
    public Ship getShipB() {
        return shipB;
    }

    /**
     * Set the 2nd ship of the pair.
     *
     * @param value the 2nd ship of the pair.
     */
    public void setShipB(final Ship value) {
        this.shipB = value;
    }

    /**
     * Set the combat points of the ship.
     *
     * @param side  the side of the pair.
     * @param round the round of the battle (for hand-to-hand).
     * @param value the combat points of the ship.
     */
    public void setCombatPoints(final int side, final int round, final int value) {
        switch (round) {
            case 1:
                combatPoints[side] = value;
                break;

            case 2:
                combatPoints2[side] = value;
                break;

            case 3:
            default:
                combatPoints3[side] = value;
                break;
        }
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
     * Set the combat points of the ship.
     *
     * @param value the combat points.
     */
    public void setCombatPoints(final int[] value) {
        this.combatPoints = value.clone();
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
     * Set the combat points of the ship for the 2nd round.
     *
     * @param value the combat points.
     */
    public void setCombatPoints2(final int[] value) {
        this.combatPoints2 = value.clone();
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
     * Set the combat points of the ship for the 3rd round.
     *
     * @param value the combat points.
     */
    public void setCombatPoints3(final int[] value) {
        this.combatPoints3 = value.clone();
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
     * Set the lost tonnage of the ship.
     *
     * @param side  the side of the pair.
     * @param value the lost tonnage of the ship.
     */
    public void setLossTonnage(final int side, final int value) {
        this.lossTonnage[side] = value;
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
     * Set the lost tonnage of the ship.
     *
     * @param value the lost tonnage of the ship.
     */
    public void setLossTonnage(final int[] value) {
        this.lossTonnage = value.clone();
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
        return captured3.clone();
    }

    /**
     * Set the captured flags for each side after the 3rd round.
     *
     * @param value the captured flags for each side after the 3rd round.
     */
    public void setCaptured3(final boolean[] value) {
        captured3 = value.clone();
    }

    /**
     * Set the captured flags for each side after the hand-to-hand round.
     *
     * @param side  the side of the pair.
     * @param round the round of the battle (for hand-to-hand).
     * @param value true if the ship was captured.
     */
    public void setCaptured(final int side, final int round, final boolean value) {
        switch (round) {
            case 1:
                captured1[side] = value;
                break;

            case 2:
                captured2[side] = value;
                break;

            case 3:
            default:
                captured3[side] = value;
                break;
        }
    }
}
