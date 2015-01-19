package com.eaw1805.battles.naval.result;

import java.io.Serializable;

/**
 * Statistics for a particular ship class.
 */
public class ShipClassStat
        implements Serializable {

    /**
     * Required by Serializable interface.
     */
    static final long serialVersionUID = 42L; //NOPMD

    /**
     * The ship class.
     */
    private int shipClass;

    /**
     * Total number of ships.
     */
    private int ships;

    /**
     * Total number of cannons.
     */
    private int cannons;

    /**
     * Total number of tonnage.
     */
    private int tonnage;

    /**
     * Total number of marines.
     */
    private int marines;

    /**
     * Empty constructor.
     */
    public ShipClassStat() {
        // used by spring.
    }

    /**
     * Default constructor.
     *
     * @param thisSC The ship class.
     */
    public ShipClassStat(final int thisSC) {
        this.shipClass = thisSC;
        ships = 0;
        cannons = 0;
        tonnage = 0;
        marines = 0;
    }

    /**
     * Get The ship class.
     *
     * @return The ship class.
     */
    public int getShipClass() {
        return shipClass;
    }

    /**
     * Get the Total number of ships.
     *
     * @return Total number of ships.
     */
    public int getShips() {
        return ships;
    }

    /**
     * Increase the Total number of ships.
     *
     * @param value the extra number of ships.
     */
    public void incShips(final int value) {
        this.ships += value;
    }

    /**
     * Get the Total number of cannons.
     *
     * @return Total number of cannons.
     */
    public int getCannons() {
        return cannons;
    }

    /**
     * Increase the Total number of cannons.
     *
     * @param value the extra number of cannons.
     */
    public void incCannons(final int value) {
        this.cannons += value;
    }

    /**
     * Get the Total tonnage.
     *
     * @return Total tonnage.
     */
    public int getTonnage() {
        return tonnage;
    }

    /**
     * Increase the Total tonnage.
     *
     * @param value the extra tonnage.
     */
    public void incTonnage(final int value) {
        this.tonnage += value;
    }

    /**
     * Get the Total number of marines.
     *
     * @return Total number of marines.
     */
    public int getMarines() {
        return marines;
    }

    /**
     * Set the total number of marines.
     *
     * @param value the total number of marines.
     */
    public void setMarines(final int value) {
        this.marines = value;
    }

    /**
     * Increase the Total number of marines.
     *
     * @param value the additional number of marines.
     */
    public void incMarines(final int value) {
        this.marines += value;
    }
}
