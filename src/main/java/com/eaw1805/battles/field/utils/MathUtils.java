package com.eaw1805.battles.field.utils;


/**
 * Utility class with mathematical functions.
 * @author fragkakis
 *
 */
public class MathUtils {
	
	public static int generateRandomIntInRange(int min, int max) {
		return min + (int) (Math.random() * ((max - min) + 1));
	}
	
	public static double generateRandomDoubleInRange(double min, double max) {
		return min + (max - min) * Math.random();
	}

}
