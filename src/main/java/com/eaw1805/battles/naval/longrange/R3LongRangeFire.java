package com.eaw1805.battles.naval.longrange;

import com.eaw1805.battles.naval.NavalBattleProcessor;

/**
 * Long-Range fire of all warships at 100% effectiveness.
 * All warships will shoot at enemy ships.
 * This round will take place at all weather conditions.
 */
public class R3LongRangeFire
        extends SOLLongRangeFire {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public R3LongRangeFire(final NavalBattleProcessor caller) {
        super(caller);
        setRoundModifier(100);
        setRound(ROUND_LR_2);
    }

    /**
     * Check weather to determine if this round will take place.
     *
     * @return true if the weather is CLEAR.
     */
    public boolean checkWeather() {
        return true;
    }

    /**
     * The minimum ship class that can participate in this round.
     *
     * @return 1.
     */
    public int minimumShipClass() {
        return 1;
    }

}
