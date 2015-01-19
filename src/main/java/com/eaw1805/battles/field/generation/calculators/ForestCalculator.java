package com.eaw1805.battles.field.generation.calculators;

import com.google.common.collect.Sets;
import com.eaw1805.battles.field.utils.FieldBattleCollectionUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Helper class that adds forest to a field battle map.
 *
 * @author fragkakis
 */
public class ForestCalculator {

    private FieldBattleMap fbMap;

    public ForestCalculator(FieldBattleMap fbMap) {
        this.fbMap = fbMap;
    }

    /**
     * Adds forests of the specified size until they cover the specified percentage.
     *
     * @param targetForestSectors the number of sectors the forest must cover
     * @param minForestSize       the minimum forest size
     * @param maxForestSize       the maximum forest size
     */
	public void addForestsByPercentage(int percentage) {

		Set<FieldBattleSector> emptySectors = MapUtils.getAllEmptySectors(fbMap);
		int targetForestSectors = emptySectors.size() * percentage / 100; 
		
        Set<FieldBattleSector> forestSectors = findInitialForestSectors();
        
        
        Set<Set<FieldBattleSector>> forestClusters = convertToClusters(forestSectors);
        
        while (unionAll(forestClusters).size() < targetForestSectors) {
        	
        	Set<FieldBattleSector> forestCluster = FieldBattleCollectionUtils.getRandom(forestClusters);
    		Set<FieldBattleSector> emptyNeighbourSectors = MapUtils.findEmptyNeighboursSectors(forestCluster);
    		Set<FieldBattleSector> randomEmptyNeighbourSectorsCandidates = FieldBattleCollectionUtils.getRandomPercentage(emptyNeighbourSectors, 50);
        	
        	Set<FieldBattleSector> otherForestSectors = unionAll(forestClusters);
        	otherForestSectors.removeAll(forestCluster);
        	
        	Iterator<FieldBattleSector> it = randomEmptyNeighbourSectorsCandidates.iterator();
        	while(it.hasNext()) {
        		FieldBattleSector candidate = it.next();
        		Set<FieldBattleSector> neighbours = MapUtils.getNeighbours(candidate);
        		if(!Sets.intersection(otherForestSectors, neighbours).isEmpty()) {
        			it.remove();
        		}
        	}
        	
        	forestCluster.addAll(randomEmptyNeighbourSectorsCandidates);
        }
        
        makeForest(unionAll(forestClusters));
    }

	private Set<FieldBattleSector> unionAll(Set<Set<FieldBattleSector>> forestClusters) {
		
		Set<FieldBattleSector> union = new HashSet<FieldBattleSector>();
		for(Set<FieldBattleSector> forestCluster : forestClusters) {
			union.addAll(forestCluster);
		}
		return union;
	}

	private Set<Set<FieldBattleSector>> convertToClusters(Set<FieldBattleSector> forestSectors) {
		
		Set<Set<FieldBattleSector>> forestClusters = new HashSet<Set<FieldBattleSector>>();
		for(FieldBattleSector sector : forestSectors) {
			forestClusters.add(new HashSet<FieldBattleSector>(Arrays.asList(new FieldBattleSector[]{sector})));
		}
		return forestClusters;
	}

	private void makeForest(Set<FieldBattleSector> sectors) {
		for(FieldBattleSector sector : sectors) {
			if(sector.isEmpty()) {
				sector.setForest(true);
			}
		}
	}

	private Set<FieldBattleSector> findInitialForestSectors() {
		
		int stepX = fbMap.getSizeX()/7;
		int stepY = fbMap.getSizeY()/7;
		
		System.out.println(stepX);
		System.out.println(stepY);
		
		int numberOfForests = MathUtils.generateRandomIntInRange(6, 20);
		Set<FieldBattleSector> initialForestSectors = FieldBattleCollectionUtils.getRandomNumber(fbMap.getSectorsSet(), numberOfForests);
		
		Iterator<FieldBattleSector> it = initialForestSectors.iterator();
		
		while(it.hasNext()) {
			
			FieldBattleSector sector = it.next();
			if(!Sets.intersection(MapUtils.getNeighbours(sector), initialForestSectors).isEmpty()) {
				it.remove();
			}
			return initialForestSectors;
		}
		
		return initialForestSectors;
	}


}
