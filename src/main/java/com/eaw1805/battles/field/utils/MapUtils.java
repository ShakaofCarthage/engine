package com.eaw1805.battles.field.utils;

import com.eaw1805.battles.field.generation.calculators.ClusterTypeEnum;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is a utilities class for the {@link FieldBattleMap}.
 *
 * @author fragkakis
 */
public class MapUtils {

    public static Set<FieldBattleSector> findSectorsInArea(FieldBattleMap fbMap, int minX, int maxX, int minY, int maxY, boolean onlyEmpty) {

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();
        for (int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (onlyEmpty && !sector.isEmpty()) {
                    continue;
                }
                sectors.add(sector);
            }
        }

        return sectors;

    }

    public static Set<FieldBattleSector> findSectorsInRadius(FieldBattleSector startingSector, int radius) {

        FieldBattleMap fbMap = startingSector.getMap();
        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();
        int minX = startingSector.getX() - radius;
        int maxX = startingSector.getX() + radius;
        int minY = startingSector.getY() - radius;
        int maxY = startingSector.getY() + radius;

        for (int x = minX; x <= maxX; x++) {

            if (x < 0 || fbMap.getSizeX() - 1 < x) {
                continue;
            }
            for (int y = minY; y <= maxY; y++) {

                if (y < 0 || fbMap.getSizeY() - 1 < y) {
                    continue;
                }
                sectors.add(fbMap.getFieldBattleSector(x, y));
            }
        }

        return sectors;

    }

    public static Set<FieldBattleSector> findSectorsInRing(FieldBattleSector startingSector, int radius) {

        FieldBattleMap fbMap = startingSector.getMap();
        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();
        int minX = startingSector.getX() - radius;
        int maxX = startingSector.getX() + radius;
        int minY = startingSector.getY() - radius;
        int maxY = startingSector.getY() + radius;


        for (int x = minX; x <= maxX; x++) {

            if (x < 0 || fbMap.getSizeX() - 1 < x) {
                continue;
            }
            for (int y = minY; y <= maxY; y++) {

                if (y < 0 || fbMap.getSizeY() - 1 < y) {
                    continue;
                }
                sectors.add(fbMap.getFieldBattleSector(x, y));
            }
        }

        return sectors;

    }

    /**
     * Finds all sectors with a specific altitude
     *
     * @param fbMap    the field battle map
     * @param altitude the specified altitude
     * @return all the requested sectors
     */
    public static Set<FieldBattleSector> findSectorsWithAltitude(FieldBattleMap fbMap, int altitude) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {
            for (int y = 0; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.getAltitude() == altitude) {
                    sectors.add(sector);
                }
            }
        }
        return sectors;
    }

    /**
     * Find all the neighbouring sectors of a set of sectors, excluding that set.
     *
     * @param sectors the sectors
     * @return the neighbours
     */
    public static Set<FieldBattleSector> findNeighboursSectors(Set<FieldBattleSector> sectors) {
        Set<FieldBattleSector> neighbours = new HashSet<FieldBattleSector>();

        for (FieldBattleSector sector : sectors) {
            neighbours.addAll(getNeighbours(sector));
        }
        neighbours.removeAll(sectors);

        return neighbours;

    }
    
    /**
     * Find all the empty neighbouring sectors of a set of sectors, excluding that set.
     *
     * @param sectors the sectors
     * @return the neighbours
     */
    public static Set<FieldBattleSector> findEmptyNeighboursSectors(Set<FieldBattleSector> sectors) {
        Set<FieldBattleSector> neighbours = findNeighboursSectors(sectors);
        
        Iterator<FieldBattleSector> it = neighbours.iterator();
        
        while(it.hasNext()) {
        	if(!it.next().isEmpty()) {
        		it.remove();
        	}
        }

        return neighbours;

    }

    /**
     * Find all the neighbouring sectors of a set of sectors, excluding that set.
     *
     * @param sectors the sectors
     * @return the neighbours
     */
    public static Set<FieldBattleSector> findNeighbourSectorsWithNoAltitude(Set<FieldBattleSector> sectors) {
        Set<FieldBattleSector> neighboursWithNoAltitude = new HashSet<FieldBattleSector>();

        for (FieldBattleSector sector : sectors) {
            Set<FieldBattleSector> sectorNeighbours = getNeighbours(sector);
            for (FieldBattleSector sectorNeighbour : sectorNeighbours) {
                if (sectorNeighbour.getAltitude() == 0) {
                    neighboursWithNoAltitude.add(sectorNeighbour);
                }
            }
        }
        return neighboursWithNoAltitude;

    }

    public static int getSectorsDistance(FieldBattleSector sector1, FieldBattleSector sector2) {
        return Math.max(Math.abs(sector1.getX() - sector2.getX()), Math.abs(sector1.getY() - sector2.getY()));
    }

    /**
     * Returns all the sectors of the {@link FieldBattleMap}.
     *
     * @param fbMap the map
     * @return a set of all the sectors
     */
    public static Set<FieldBattleSector> getAllSectors(FieldBattleMap fbMap) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {

            for (int y = 0; y <= maxY; y++) {
                sectors.add(fbMap.getFieldBattleSector(x, y));
            }
        }

        return sectors;
    }

    /**
     * Returns all the empty sectors of the {@link FieldBattleMap}.
     *
     * @param fbMap the map
     * @return a set of all the empty sectors
     */
    public static Set<FieldBattleSector> getAllEmptySectors(FieldBattleMap fbMap) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {

            for (int y = 0; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.isEmpty()) {
                    sectors.add(sector);
                }
            }
        }

        return sectors;
    }

    /**
     * Returns all the the sectors of the {@link FieldBattleMap} with no altitude set.
     *
     * @param fbMap the map
     * @return a set of all the requested sectors
     */
    public static Set<FieldBattleSector> getAllSectorsWithNoAltitude(FieldBattleMap fbMap) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {

            for (int y = 0; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.getAltitude() == 0) {
                    sectors.add(sector);
                }
            }
        }

        return sectors;
    }

    public static Set<FieldBattleSector> getNeighbours(FieldBattleSector sector) {
        Set<FieldBattleSector> neighbours = new HashSet<FieldBattleSector>();
        for (int x = sector.getX() - 1; x <= sector.getX() + 1; x++) {

            if (x < 0 || sector.getMap().getSizeX() - 1 < x) {
                continue;
            }

            for (int y = sector.getY() - 1; y <= sector.getY() + 1; y++) {
                if (y < 0 || sector.getMap().getSizeY() - 1 < y) {
                    continue;
                }
                if (x == sector.getX() && y == sector.getY()) {
                    continue;
                }
                neighbours.add(sector.getMap().getFieldBattleSector(x, y));
            }
        }

        return neighbours;
    }

    /**
     * Returns the horizontal and vertical neighbours of a sector, NOT the diagonal neighbours.
     *
     * @param sector the sector
     * @return its neighbours
     */
    public static Set<FieldBattleSector> getHorizontalAndVerticalNeighbours(FieldBattleSector sector) {
        Set<FieldBattleSector> neighbours = new HashSet<FieldBattleSector>();
        for (int x = sector.getX() - 1; x <= sector.getX() + 1; x++) {

            if (x < 0 || sector.getMap().getSizeX() - 1 < x) {
                continue;
            }

            for (int y = sector.getY() - 1; y <= sector.getY() + 1; y++) {
                if (y < 0 || sector.getMap().getSizeY() - 1 < y) {
                    continue;
                }
                if (x == sector.getX() && y == sector.getY()) {
                    continue;
                }

                // only vertical and horizontal neighbours
                if (x == sector.getX() || y == sector.getY()) {
                    neighbours.add(sector.getMap().getFieldBattleSector(x, y));
                }
            }
        }

        return neighbours;
    }

    /**
     * Find all the neighbouring sectors (horizontally or vertically, NOT diagonally) of a set of sectors, excluding the sectors themselves.
     *
     * @param sectors the sectors
     * @return the horizontal and vertical neighbours
     */
    public static Set<FieldBattleSector> getHorizontalAndVerticalNeighbours(Collection<FieldBattleSector> sectors) {
        Set<FieldBattleSector> neighbours = new HashSet<FieldBattleSector>();

        for (FieldBattleSector sector : sectors) {
            neighbours.addAll(getHorizontalAndVerticalNeighbours(sector));
        }
        neighbours.removeAll(sectors);

        return neighbours;

    }

    public static boolean isSectorOfType(FieldBattleSector sector, ClusterTypeEnum type) {

        switch (type) {
            case FOREST:
                return sector.isForest();
            case WATER:
                return sector.isLake() || sector.isMinorRiver();
            case TOWN:
                return sector.hasSectorTown();
            case VILLAGE:
                return sector.hasSectorVillage();
            default:
                return false;

        }
    }

    /**
     * Returns all the river or lake sectors of the {@link FieldBattleMap}.
     *
     * @param fbMap the map
     * @return a set of all the river or lake sectors
     */
    public static Set<FieldBattleSector> getAllRiverOrLakeSectors(FieldBattleMap fbMap) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {

            for (int y = 0; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.isMinorRiver() || sector.isLake()) {
                    sectors.add(sector);
                }
            }
        }

        return sectors;
    }

    /**
     * Returns all the empty sectors of the {@link FieldBattleMap}.
     *
     * @param fbMap the map
     * @return a set of all the empty sectors
     */
    public static Set<FieldBattleSector> getAllRoadSectors(FieldBattleMap fbMap) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {

            for (int y = 0; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.isRoad()) {
                    sectors.add(sector);
                }
            }
        }

        return sectors;
    }

    public static FieldBattleSector getSectorFromPosition(FieldBattleMap fbMap, FieldBattlePosition fieldBattlePosition) {

        return fbMap.getFieldBattleSector(fieldBattlePosition.getX(), fieldBattlePosition.getY());
    }

    /**
     * Returns all the {@link FieldBattleSector}s that are strategic points originally belonging to the specified nations.
     *
     * @param fbMap   the field battle map
     * @param nations the nations. Leave empty to ignore nation.
     * @return the sectors that are strategic points
     */
    public static Set<FieldBattleSector> findStrategicPoints(FieldBattleMap fbMap, Collection<Nation> nations) {
        Set<FieldBattleSector> strategicPoints = new HashSet<FieldBattleSector>();

        Set<FieldBattleSector> allSectors = MapUtils.getAllSectors(fbMap);
        for (FieldBattleSector sector : allSectors) {
            if (sector.isStrategicPoint()) {
                if (!nations.isEmpty() && !nations.contains(sector.getNation())) {
                    continue;
                }
                strategicPoints.add(sector);
            }
        }

        return strategicPoints;
    }

    /**
     * Finds the closest sectors among a collection to a specified sector.
     * Frequently there will be 1 sector. If multiple sectors have the same
     * minimum distance, the returned set will contain all of them.
     *
     * @param sector           the starting sector
     * @param candidateSectors the candidate sectors
     * @return the closest sector(s)
     */
    public static Set<FieldBattleSector> findClosest(FieldBattleSector sector, Collection<FieldBattleSector> candidateSectors) {

        int minimumDistance = Integer.MAX_VALUE;
        Set<FieldBattleSector> closestSectors = new HashSet<FieldBattleSector>();

        for (FieldBattleSector candidateSector : candidateSectors) {
            int candidateDistance = MapUtils.getSectorsDistance(sector, candidateSector);

            if (candidateDistance < minimumDistance) {
                closestSectors.clear();
                minimumDistance = candidateDistance;
            }
            if (candidateDistance == minimumDistance) {
                closestSectors.add(candidateSector);
            }
        }

        return closestSectors;
    }

    /**
     * Finds all sectors that contain wall belong to any of a set of nations.
     *
     * @param fbMap   the field battle map
     * @param nations the nations that may own the wall
     * @return the sectors containing the walls
     */
    public static Set<FieldBattleSector> findWallSectorsForNations(FieldBattleMap fbMap, Collection<Nation> nations) {

        Set<FieldBattleSector> wallSectors = new HashSet<FieldBattleSector>();

        Set<FieldBattleSector> allSectors = getAllSectors(fbMap);
        for (FieldBattleSector sector : allSectors) {
            if (sector.hasSectorWall() && nations.contains(sector.getNation())) {
                wallSectors.add(sector);
            }
        }

        return wallSectors;
    }

    /**
     * Finds all sectors that contain wall belong to any of a set of nations.
     *
     * @param fbMap   the field battle map
     * @param nations the nations that may own the wall
     * @return the sectors containing the walls
     */
    public static Set<FieldBattleSector> findBridgeSectorsForNations(
            FieldBattleMap fbMap, Collection<Nation> nations) {
        Set<FieldBattleSector> pontoonBridgeSectors = new HashSet<FieldBattleSector>();

        Set<FieldBattleSector> allSectors = getAllSectors(fbMap);
        for (FieldBattleSector sector : allSectors) {
            if (sector.hasSectorBridge() && nations.contains(sector.getCurrentHolder())) {
                pontoonBridgeSectors.add(sector);
            }
        }

        return pontoonBridgeSectors;
    }

    /**
     * Returns the sector that is forward of the specified one.
     *
     * @param sector the sector
     * @param side   the side (to determine which way is forward)
     * @return the sector
     */
    public static FieldBattleSector findSectorForward(FieldBattleSector sector, int side) {
        int verticalDisplacement = side == 0 ? 1 : -1;
        return sector.getMap().getFieldBattleSector(sector.getX(), sector.getY() + verticalDisplacement);
    }

    /**
     * Filters a set of sectors and keeps only those that are sufficiently far away from another set of sectors.
     *
     * @param sectors         the sectors to filter
     * @param avoidSectors    the sectors to avoid
     * @param minimumDistance the minimum distance
     * @return the filtered set of sectors
     */
    public static Set<FieldBattleSector> filterSectorsByDistance(
            Collection<FieldBattleSector> sectors, Set<FieldBattleSector> avoidSectors, int minimumDistance) {

        Set<FieldBattleSector> filteredSectors = new HashSet<FieldBattleSector>();

        filterSector:
        for (FieldBattleSector sector : sectors) {
            for (FieldBattleSector avoidSector : avoidSectors) {
                if (MapUtils.getSectorsDistance(sector, avoidSector) < minimumDistance) {
                    continue filterSector;
                }
            }
            filteredSectors.add(sector);
        }

        return filteredSectors;
    }

    public static Set<FieldBattleSector> findStructures(FieldBattleMap fbMap) {
        Set<FieldBattleSector> structures = new HashSet<FieldBattleSector>();

        for (FieldBattleSector sector : getAllSectors(fbMap)) {
            if (sector.hasStructure()) {
                structures.add(sector);
            }
        }

        return structures;
    }

    /**
     * Orders a list of sectors by their distance to a specific sector of reference, closest first.
     * @param sectorOfReference the sector of reference.
     * @param sectors the sectors
     * @return the ordered list of sectors.
     */
	public static List<FieldBattleSector> orderByDistance(FieldBattleSector sectorOfReference, Collection<FieldBattleSector> sectors) {
		
		List<FieldBattleSector> sectorsOrderedByDistance = new ArrayList<FieldBattleSector>(sectors);
		
		Collections.sort(sectorsOrderedByDistance, new DistanceComparator(sectorOfReference));

		return sectorsOrderedByDistance;
	}
	
	private static class DistanceComparator implements Comparator<FieldBattleSector> {

		private FieldBattleSector sectorOfReference;
		public DistanceComparator(FieldBattleSector sector) {
			sectorOfReference = sector;
		}

		@Override
		public int compare(FieldBattleSector sector0, FieldBattleSector sector1) {
			return getSectorsDistance(sectorOfReference, sector0) - getSectorsDistance(sectorOfReference, sector1); 
		}
		
	}
	
	/**
     * Returns all the forest sectors of the {@link FieldBattleMap}.
     *
     * @param fbMap the map
     * @return a set of all the forest sectors
     */
    public static Set<FieldBattleSector> getAllForestSectors(FieldBattleMap fbMap) {

        int maxX = fbMap.getSizeX() - 1;
        int maxY = fbMap.getSizeY() - 1;

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (int x = 0; x <= maxX; x++) {
            for (int y = 0; y <= maxY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.isForest()) {
                    sectors.add(sector);
                }
            }
        }

        return sectors;
    }

}
