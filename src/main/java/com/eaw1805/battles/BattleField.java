package com.eaw1805.battles;

import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes the nations of each side and the sector where a battle will take place.
 */
public class BattleField {

    /**
     * The nations of each side.
     */
    private transient List<Nation> side[];

    /**
     * The sector where the battle will take place;
     */
    private transient Sector field;

    /**
     * The battle involves act of piracy.
     */
    private transient boolean piracy;

    /**
     * The battle is a result of a patrol.
     */
    private transient boolean patrol;

    /**
     * The patrolling force.
     */
    private transient List<Ship> patrolForce;

    /**
     * Default constructor
     *
     * @param position the sector where the battle will take place.
     */
    @SuppressWarnings("unchecked")
    public BattleField(final Sector position) {
        side = new List[2];
        side[0] = new ArrayList<Nation>();
        side[1] = new ArrayList<Nation>();
        field = position;
        piracy = false;
        patrol = false;
    }

    /**
     * Get the sector where the battle will take place.
     *
     * @return the sector where the battle will take place.
     */
    public Sector getField() {
        return field;
    }

    /**
     * Get the side of the battle field.
     *
     * @param thisSide the side to retrieve.
     * @return a list of nations
     */
    public List<Nation> getSide(final int thisSide) {
        return side[thisSide];
    }

    /**
     * Add a nation to one of the sides of the battle field.
     *
     * @param thisSide   the side to add to.
     * @param thisNation the nation to add.
     */
    public void addNation(final int thisSide, final Nation thisNation) {
        side[thisSide].add(thisNation);
    }

    /**
     * Get if the battle involves act of piracy.
     *
     * @return if the battle involves act of piracy.
     */
    public boolean getPiracy() {
        return piracy;
    }

    /**
     * Set if the battle involves act of piracy.
     *
     * @param value true, if the battle involves act of piracy.
     */
    public void setPiracy(final boolean value) {
        this.piracy = value;
    }

    /**
     * Get if the battle is a result of a patrol.
     *
     * @return if the battle is a result of a patrol.
     */
    public boolean getPatrol() {
        return patrol;
    }

    /**
     * Set if the battle is a result of a patrol.
     *
     * @param value if the battle is a result of a patrol.
     */
    public void setPatrol(final boolean value) {
        this.patrol = value;
    }

    /**
     * Get the patrolling force.
     *
     * @return the patrolling force.
     */
    public List<Ship> getPatrolForce() {
        return patrolForce;
    }

    /**
     * Set the patrolling force.
     *
     * @param value the patrolling force.
     */
    public void setPatrolForce(final List<Ship> value) {
        this.patrolForce = value;
    }

    @Override
    public String toString() {
        final StringBuilder strBld = new StringBuilder();
        strBld.append("Battle Field at ");
        strBld.append(field.getPosition().toString());
        strBld.append(" Side A=[");
        for (Nation nationList : side[0]) {
            strBld.append(nationList.getCode());
        }
        strBld.append("] Side B=[");
        for (Nation nationList : side[1]) {
            strBld.append(nationList.getCode());
        }
        strBld.append("]");

        if (piracy) {
            strBld.append(" PIRATES!");
        }

        if (patrol) {
            strBld.append(" PATROL!");
        }

        return strBld.toString();
    }
}
