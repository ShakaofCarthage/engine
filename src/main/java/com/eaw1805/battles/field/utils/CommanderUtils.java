package com.eaw1805.battles.field.utils;

import com.eaw1805.data.model.army.Commander;

/**
 * Utility class for the commanders.
 * @author fragkakis
 *
 */
public class CommanderUtils {

	/**
	 * Returns the influence radius of a commander.
	 * @param commander the commander
	 * @return the radius of influence
	 */
	public static int getCommanderInfluenceRadius(Commander commander) {
		return (int) Math.round(Math.sqrt(commander.getStrc()));
	}
}
