package com.eaw1805.battles.naval.result;

import com.eaw1805.data.dto.converters.ShipConverter;
import com.eaw1805.data.dto.web.fleet.ShipDTO;
import com.eaw1805.data.dto.web.fleet.ShipTypeDTO;
import com.eaw1805.data.model.fleet.Ship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the statistics for a single round.
 */
public final class RoundStat
        implements Serializable {

    /**
     * Required by Serializable interface.
     */
    static final long serialVersionUID = 42L; //NOPMD

    /**
     * Side A won the battle.
     */
    public final static int SIDE_A = 1;

    /**
     * Side B won the battle.
     */
    public final static int SIDE_B = 2;

    /**
     * Undecided.
     */
    public final static int SIDE_NONE = 0;

    /**
     * The round.
     */
    private int round;

    /**
     * The ships of each side.
     */
    private List<ShipDTO>[] sideShips;

    /**
     * The ships statistics of each side.
     */
    private List<ShipClassStat>[] sideStat;

    /**
     * The pair of ships battled during this round.
     */
    private Set<ShipPairDTO> sidePairs;

    /**
     * The captured ships.
     */
    private List<ShipDTO>[] capturedShips;

    /**
     * The result of the battle.
     */
    private int result;

    /**
     * Default constructor.
     */
    public RoundStat() {
        // empty constructor for use by spring.
    }

    /**
     * Constructor used by naval processor.
     *
     * @param thisRound the round of the battle.
     * @param ships     the ships of each side.
     */
    @SuppressWarnings("unchecked")
    public RoundStat(final int thisRound,
                     final List<Ship>[] ships) {
        round = thisRound;

        sideShips = new ArrayList[2];
        sideStat = new ArrayList[2];
        for (int side = 0; side < 2; side++) {
            sideShips[side] = ShipConverter.convert(ships[side], null);
            sideStat[side] = calcSideStat(sideShips[side]);
        }
    }

    /**
     * Constructor used by naval processor.
     *
     * @param thisRound the round of the battle.
     * @param ships     the ships of each side.
     * @param stats     the statistics of each side.
     */
    @SuppressWarnings("unchecked")
    public RoundStat(final int thisRound,
                     final List<ShipDTO>[] ships,
                     final List<ShipClassStat>[] stats) {
        round = thisRound;
        sideShips = ships;
        sideStat = stats;
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
     * Get the ships of each side.
     *
     * @return the ships of each side.
     */
    public List<ShipDTO>[] getSideShips() {
        return sideShips;
    }

    /**
     * Get the ships of each side.
     *
     * @param side the side.
     * @return the ships of each side.
     */
    public List<ShipDTO> getSideShips(final int side) {
        return sideShips[side];
    }

    /**
     * Get the ships statistics of each side.
     *
     * @return the ships statistics of each side.
     */
    public List<ShipClassStat>[] getSideStat() {
        return sideStat;
    }

    /**
     * Get the ships statistics of each side.
     *
     * @param side the side.
     * @return the ships statistics of each side.
     */
    public List<ShipClassStat> getSideStat(final int side) {
        return sideStat[side];
    }

    /**
     * Calculate the statistics for the particular list of ships.
     *
     * @param ships the list of ships.
     * @return the statistics per ship class.
     */
    public List<ShipClassStat> calcSideStat(final List<ShipDTO> ships) {
        final List<ShipClassStat> lstStats = new ArrayList<ShipClassStat>();

        // Initiate the stats
        for (int sc = 0; sc < 6; sc++) {
            lstStats.add(new ShipClassStat(sc));
        }

        // Iterate through ships
        for (final ShipDTO ship : ships) {
            // Only take into account not captured ships
            if (ship.getCapturedByNation() == 0 && ship.getCondition() > 0) {
                final ShipTypeDTO thisType = ship.getType();

                // Increase stats for this ship class
                final ShipClassStat scStat = lstStats.get(thisType.getShipClass());
                scStat.incShips(1);
                scStat.incMarines(ship.getMarines());
                scStat.incTonnage(ship.calcTonnage());
                scStat.incCannons(thisType.getCannons());
            }
        }

        // Calculate grand totals
        final ShipClassStat scTotStat = new ShipClassStat(6);
        int shipClass = 0;
        for (final ShipClassStat scStat : lstStats) {
            if (shipClass > 0) {
                scTotStat.incShips(scStat.getShips());
                scTotStat.incMarines(scStat.getMarines());
                scTotStat.incTonnage(scStat.getTonnage());
                scTotStat.incCannons(scStat.getCannons());
            }
            shipClass++;
        }
        lstStats.add(scTotStat);

        // Calculate captured ships
        final ShipClassStat scCapturedStat = new ShipClassStat(7);
        for (final ShipDTO ship : ships) {
            // Only take into account captured ships
            if (ship.getCapturedByNation() < 0) {
                final ShipTypeDTO thisType = ship.getType();

                // Increase stats for this ship class
                scCapturedStat.incShips(1);
                scCapturedStat.incMarines(ship.getMarines());
                scCapturedStat.incTonnage(ship.calcTonnage());
                scCapturedStat.incCannons(thisType.getCannons());
            }
        }

        // only add this category if at least 1 ship was captured
        if (scCapturedStat.getShips() > 0) {
            lstStats.add(scCapturedStat);
        }

        // Calculate lost ships
        final ShipClassStat scLostStat = new ShipClassStat(8);
        for (final ShipDTO ship : ships) {
            // Only take into account lost ships
            if (ship.getCapturedByNation() > 0 || ship.getCondition() < 1) {
                final ShipTypeDTO thisType = ship.getType();

                // Increase stats for this ship class
                scLostStat.incShips(1);
                scLostStat.incMarines(ship.getMarines());
                scLostStat.incTonnage(ship.calcTonnage());
                scLostStat.incCannons(thisType.getCannons());
            }
        }

        // only add this category if at least 1 ship was lost
        if (scLostStat.getShips() > 0) {
            lstStats.add(scCapturedStat);
            lstStats.add(scLostStat);
        }

        return lstStats;
    }

    /**
     * Get the pairs of ships battled during this round.
     *
     * @return the pairs of ships.
     */
    public Set<ShipPairDTO> getSidePairs() {
        return sidePairs;
    }

    /**
     * Set the pairs of ships battled during this round.
     *
     * @param value the pairs of ships.
     */
    public void setSidePairs(final Set<ShipPair> value) {
        this.sidePairs = new HashSet<ShipPairDTO>();
        for (final ShipPair shipPair : value) {
            sidePairs.add(new ShipPairDTO(shipPair));
        }
    }

    /**
     * Get the result of the battle.
     *
     * @return the result of the battle.
     */
    public int getResult() {
        return result;
    }

    /**
     * Set the result of the battle.
     *
     * @param value the result of the battle.
     */
    public void setResult(final int value) {
        this.result = value;
    }

    /**
     * Get the list of captured ships.
     *
     * @return the list of captured ships.
     */
    public List<ShipDTO>[] getCapturedShips() {
        return capturedShips;
    }

    /**
     * Set the list of captured ships.
     *
     * @param value the list of captured ships.
     */
    public void setCapturedShips(final List<ShipDTO>[] value) {
        this.capturedShips = value;
    }
}
