package com.eaw1805.battles.tactical.result;

import java.io.Serializable;

/**
 * Keeps track of the statistics of one side of the battle.
 */
public class SideStat
        implements Serializable {

    /**
     * Required by Serializable interface.
     */
    static final long serialVersionUID = 42L; //NOPMD

    /**
     * The side of the battle.
     */
    private int side;

    /**
     * The combat points of the side.
     */
    private double combatPoints;

    /**
     * The casualties received.
     */
    private double casualties;

    /**
     * The casualties inflicted.
     */
    private double casualtiesInflicted;

    /**
     * Empty constructor.
     */
    public SideStat() {
        // used by spring.
    }

    /**
     * Get the side of the battle.
     *
     * @return the side of the battle.
     */
    public int getSide() {
        return side;
    }

    /**
     * Set the side of the battle.
     *
     * @param value the side of the battle.
     */
    public void setSide(final int value) {
        side = value;
    }

    /**
     * Get the combat points of the side.
     *
     * @return the combat points of the side.
     */
    public double getCombatPoints() {
        return combatPoints;
    }

    /**
     * Set the combat points of the side.
     *
     * @param value the combat points of the side.
     */
    public void setCombatPoints(final double value) {
        this.combatPoints = value;
    }

    /**
     * Get the casualties received.
     *
     * @return the casualties received.
     */
    public double getCasualties() {
        return casualties;
    }

    /**
     * Set the casualties received.
     *
     * @param value the casualties received.
     */
    public void setCasualties(final double value) {
        casualties = value;
    }

    /**
     * Get the casualties inflicted.
     *
     * @return the casualties inflicted.
     */
    public double getCasualtiesInflicted() {
        return casualtiesInflicted;
    }

    /**
     * Set the casualties inflicted.
     *
     * @param value the casualties inflicted.
     */
    public void setCasualtiesInflicted(final double value) {
        casualtiesInflicted = value;
    }

}
