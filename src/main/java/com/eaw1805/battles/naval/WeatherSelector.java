package com.eaw1805.battles.naval;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.model.map.Position;

import java.util.Calendar;
import java.util.Random;

/**
 * Determines the weather type based on the coordinates of the naval battle.
 */
public class WeatherSelector
        implements RegionConstants {

    private final transient Random randomGen;

    private final transient Calendar gameCalendar;

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public WeatherSelector(final GameEngine caller) {
        randomGen = caller.getRandomGen();
        gameCalendar = caller.calendar();
    }

    /**
     * The weather type depends on the position of the naval battle on the map and the season.
     *
     * @param thisPos the Position to check.
     * @return the weather type.
     */
    public int rollWeather(final Position thisPos) {
        int weather;
        switch (thisPos.getRegion().getId()) {
            case EUROPE:
                if (thisPos.getY() <= 10) {
                    weather = getEuropeHigh(gameCalendar);

                } else if ((thisPos.getY() >= 11) && (thisPos.getY() <= 35)) {
                    weather = getEuropeMed(gameCalendar);

                } else {
                    weather = getEuropeLow(gameCalendar);
                }
                break;

            case CARIBBEAN:
                weather = getEuropeMed(gameCalendar);
                break;

            case INDIES:
                weather = getEuropeMed(gameCalendar);
                break;

            case AFRICA:
                weather = getEuropeLow(gameCalendar);
                break;

            default:
                weather = NavalBattleProcessor.WEATHER_CLEAR;
        }

        return weather;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @param thisCal the calendar of the game/turn.
     * @return the climate factor.
     */
    private int getEuropeHigh(final Calendar thisCal) {
        final int month = thisCal.get(Calendar.MONTH);
        final int roll = randomGen.nextInt(101) + 1;
        int weather;
        switch (month) {
            case 0:
            case 1:
                // Jan - Feb
                if (roll <= 10) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 60) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 2:
                // Mar
                if (roll <= 20) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 60) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 3:
                // Apr
                if (roll <= 20) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 70) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 4:
                // May
                if (roll <= 20) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 80) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 5:
                // Jun
                if (roll <= 30) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 6:
            case 7:
            case 8:
                // Jul - Aug - Sep
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 9:
                // Oct
                if (roll <= 30) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 10:
                // Nov
                if (roll <= 20) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 80) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 11:
                // Dec
                if (roll <= 10) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 70) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            default:
                weather = NavalBattleProcessor.WEATHER_CLEAR;
        }
        return weather;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @param thisCal the calendar of the game/turn.
     * @return the climate factor.
     */
    private int getEuropeMed(final Calendar thisCal) {
        final int month = thisCal.get(Calendar.MONTH);
        final int roll = randomGen.nextInt(101) + 1;
        int weather;
        switch (month) {
            case 0:
            case 1:
                // Jan - Feb
                if (roll <= 20) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 70) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 2:
                // Mar
                if (roll <= 30) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 70) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 3:
                // Apr
                if (roll <= 40) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 80) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 4:
                // May
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 5:
                // Jun
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 6:
            case 7:
                // Jul - Aug
                if (roll <= 60) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 8:
                // Sep
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 9:
                // Oct
                if (roll <= 40) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 10:
                // Nov
                if (roll <= 30) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 11:
                // Dec
                if (roll <= 20) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 80) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            default:
                weather = NavalBattleProcessor.WEATHER_CLEAR;
        }
        return weather;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @param thisCal the calendar of the game/turn.
     * @return the climate factor.
     */
    private int getEuropeLow(final Calendar thisCal) {
        final int month = thisCal.get(Calendar.MONTH);
        final int roll = randomGen.nextInt(101) + 1;
        int weather;
        switch (month) {
            case 0:
            case 1:
                // Jan - Feb
                if (roll <= 40) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 2:
                // Mar
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 3:
                // Apr
                if (roll <= 60) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 4:
                // May
                if (roll <= 70) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 5:
                // Jun
                if (roll <= 80) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 6:
            case 7:
                // Jul - Aug
                if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 8:
                // Sep
                if (roll <= 80) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 9:
                // Oct
                if (roll <= 70) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 10:
                // Nov
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 100) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            case 11:
                // Dec
                if (roll <= 50) {
                    weather = NavalBattleProcessor.WEATHER_CLEAR;

                } else if (roll <= 90) {
                    weather = NavalBattleProcessor.WEATHER_RAIN;

                } else {
                    weather = NavalBattleProcessor.WEATHER_STORM;
                }
                break;

            default:
                weather = NavalBattleProcessor.WEATHER_CLEAR;
        }
        return weather;
    }

}
