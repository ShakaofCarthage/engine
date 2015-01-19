package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

/**
 * Helper class that adds chateaus to a field battle map.
 * @author fragkakis
 *
 */
public class ChateauCalculator {
	
	public static final int DEFAULT_CHATEAU_HIT_POINTS = 600;

	/**
	 * Adds the specified number of chateaus to the map.
	 *
	 * @param fbMap            the map
	 * @param chateausNumber the number of chateaus to add
	 */
	public void addChateaus(FieldBattleMap fbMap, int chateausNumber) {
		
		for (int i = 0; i < chateausNumber; i++) {
			addChateau(fbMap);
		}
	}
	
	/**
	 * Adds a single chateau to a random sector in a field battle terrain.
	 *
	 * @param fbMap the map
	 */
	private void addChateau(FieldBattleMap fbMap) {
		boolean chateauAdded = false;

		while (!chateauAdded) {
			int randomX = MathUtils.generateRandomIntInRange(0, fbMap.getSizeX() - 1);
			int randomY = MathUtils.generateRandomIntInRange(0, fbMap.getSizeY() - 1);
			FieldBattleSector sector = fbMap.getFieldBattleSector(randomX, randomY);
			if (sector.isEmpty()) {
				sector.setChateau(DEFAULT_CHATEAU_HIT_POINTS);
				chateauAdded = true;
			}
		}
	}

}
