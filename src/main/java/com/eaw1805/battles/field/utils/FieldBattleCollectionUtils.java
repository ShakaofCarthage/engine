package com.eaw1805.battles.field.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for the field battle.
 * @author fragkakis
 *
 */
public class FieldBattleCollectionUtils {
	
	/**
	 * Returns a random element from a collection.
	 * @param collection the collection
	 * @return a random element
	 */
	public static <T> T getRandom(Collection<T> collection) {
		List<T> list = new ArrayList<T>(collection);
		return list.get(MathUtils.generateRandomIntInRange(0, collection.size()-1));
	}
	
	/**
	 * Returns at random a percentage of elements from a collection.
	 * @param collection the collection
	 * @param percentage the percentage of elements to return
	 * @return the random elements
	 */
	public static <T> Set<T> getRandomPercentage(Collection<T> collection, int percentage) {
		List<T> list = new ArrayList<T>(collection);
		Collections.shuffle(list);
		
		int returnNumber = collection.size() * percentage / 100;
		
		return new HashSet<T>(list.subList(0, returnNumber));
	}
	
	/**
	 * Returns at random a specific number of elements from a collection.
	 * @param collection the collection
	 * @param number the number of elements
	 * @return the random elements
	 */
	public static <T> Set<T> getRandomNumber(Collection<T> collection, int number) {
		List<T> list = new ArrayList<T>(collection);
		Collections.shuffle(list);
		
		return new HashSet<T>(list.subList(0, number));
	}

}
