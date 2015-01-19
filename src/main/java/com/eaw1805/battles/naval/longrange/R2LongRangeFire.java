package com.eaw1805.battles.naval.longrange;

import com.eaw1805.battles.naval.NavalBattleProcessor;

/**
 * Long-Range fire of all warships at 75% effectiveness.
 * All warships except class 1 will shoot at enemy ships.
 * This round will take place only at clear and rainy weather conditions.
 */
public class R2LongRangeFire
        extends SOLLongRangeFire {

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public R2LongRangeFire(final NavalBattleProcessor caller) {
        super(caller);
        setRoundModifier(75);
        setRound(ROUND_LR_1);
    }

    /**
     * Check weather to determine if this round will take place.
     *
     * @return true if the weather is CLEAR.
     */
    public boolean checkWeather() {
        return ((getParent().getWeather() == NavalBattleProcessor.WEATHER_CLEAR)
                || (getParent().getWeather() == NavalBattleProcessor.WEATHER_RAIN));
    }

    /**
     * The minimum ship class that can participate in this round.
     *
     * @return 2.
     */
    public int minimumShipClass() {
        return 2;
    }

}
