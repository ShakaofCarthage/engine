package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AltitudeCalculator
        extends ClusterCalculator {

    private static final int MAX_EFFORTS = 100;

    private FieldBattleMap fbMap;
    private Map<Nation, FieldBattleSetupArea> setupAreas;

    private Map<Integer, Integer> altitudePercentages = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> targetSectorsPerAltitude = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> remainingSectorsPerAltitude = new HashMap<Integer, Integer>();

    public AltitudeCalculator(FieldBattleMap fbMap, Map<Nation,
            FieldBattleSetupArea> setupAreas,
                              BattleField battleField) {
        this.fbMap = fbMap;
        this.setupAreas = setupAreas;

        switch (battleField.getField().getTerrain().getId()) {
            case TerrainConstants.TERRAIN_B:
            default:
                // plains (default)
                altitudePercentages.put(1, 70);
                altitudePercentages.put(2, 25);
                altitudePercentages.put(3, 5);
                altitudePercentages.put(4, 0);
                break;
            case TerrainConstants.TERRAIN_H:
                // hills
                altitudePercentages.put(1, 40);
                altitudePercentages.put(2, 35);
                altitudePercentages.put(3, 25);
                altitudePercentages.put(4, 0);
                break;
            case TerrainConstants.TERRAIN_W:
                // forest
                altitudePercentages.put(1, 60);
                altitudePercentages.put(2, 30);
                altitudePercentages.put(3, 10);
                altitudePercentages.put(4, 0);
                break;
            case TerrainConstants.TERRAIN_G:
                // mountain
                altitudePercentages.put(1, 30);
                altitudePercentages.put(2, 30);
                altitudePercentages.put(3, 25);
                altitudePercentages.put(4, 15);
                break;
            case TerrainConstants.TERRAIN_S:
                // swamp
                altitudePercentages.put(1, 80);
                altitudePercentages.put(2, 20);
                altitudePercentages.put(3, 0);
                altitudePercentages.put(4, 0);
                break;
            case TerrainConstants.TERRAIN_D:
            case TerrainConstants.TERRAIN_J:
                // desert, jungle
                altitudePercentages.put(1, 70);
                altitudePercentages.put(2, 30);
                altitudePercentages.put(3, 0);
                altitudePercentages.put(4, 0);
                break;
        }

        int totalNumberOfSectors = fbMap.getSizeX() * fbMap.getSizeY();
        targetSectorsPerAltitude.put(1, totalNumberOfSectors * altitudePercentages.get(1) / 100);
        targetSectorsPerAltitude.put(2, totalNumberOfSectors * altitudePercentages.get(2) / 100);
        targetSectorsPerAltitude.put(3, totalNumberOfSectors * altitudePercentages.get(3) / 100);
        targetSectorsPerAltitude.put(4, totalNumberOfSectors * altitudePercentages.get(4) / 100);

        remainingSectorsPerAltitude.putAll(targetSectorsPerAltitude);

    }

    public void calculateAltitude() {

        // rivers
        calculateRiversAndLakesAltitude();

        // peaks
        if (altitudePercentages.get(4) > 0 || altitudePercentages.get(3) > 0) {
            calculatePeaks();
        } else {
            // set a random sector to altitude 2 for the algorithm to find a starting point
            List<FieldBattleSector> noAltitudeSectors = new ArrayList<FieldBattleSector>(MapUtils.getAllSectorsWithNoAltitude(fbMap));
            int startingPointCount = 0;
            while (startingPointCount < 10) {
                FieldBattleSector startingPointSector = noAltitudeSectors.get(MathUtils.generateRandomIntInRange(0, noAltitudeSectors.size() - 1));
                if (sectorAppropriateForAltitude(startingPointSector, 2)) {
                    startingPointSector.setAltitude(2);
                    remainingSectorsPerAltitude.put(2, remainingSectorsPerAltitude.get(2) - 1);
                    startingPointCount++;
                }
            }
        }


        int highestAltitude = calculateHighestAltitude();
        int startingAltitude = highestAltitude > 2 ? highestAltitude - 1 : highestAltitude;

        for (int altitude = startingAltitude; altitude > 0; altitude--) {
            placeAltitude(altitude);
        }
    }

    public void placeAltitude(int altitude) {

        Set<FieldBattleSector> higherSectors = MapUtils.findSectorsWithAltitude(fbMap, altitude + 1);

        Set<FieldBattleSector> startingSectors = new HashSet<FieldBattleSector>();
        startingSectors.addAll(placeAltitudeSectors(altitude, higherSectors, false));

        // add already placed altitude sectors
        startingSectors.addAll(MapUtils.findSectorsWithAltitude(fbMap, altitude));
        if (remainingSectorsPerAltitude.get(altitude) > 0) {

            while (remainingSectorsPerAltitude.get(altitude) > 0 && MapUtils.getAllSectorsWithNoAltitude(fbMap).size() > 0) {
                startingSectors = placeAltitudeSectors(altitude, startingSectors, true);
            }
        }
    }

    private Set<FieldBattleSector> placeAltitudeSectors(int altitude, Set<FieldBattleSector> startingSectors, boolean stopOnSectorLimit) {

        int remainingSectorsForAltitude = remainingSectorsPerAltitude.get(altitude);

        Set<FieldBattleSector> candidateSectors = MapUtils.findNeighbourSectorsWithNoAltitude(startingSectors);
//		FieldBattleVisualizer.visualize(fbMap);
//		FieldBattleVisualizer.visualizeAltitude(fbMap);
        Set<FieldBattleSector> altitudeSectors = new HashSet<FieldBattleSector>();

        for (FieldBattleSector sector : candidateSectors) {
            if (sectorAppropriateForAltitude(sector, altitude)) {
                sector.setAltitude(altitude);
                altitudeSectors.add(sector);
                remainingSectorsForAltitude--;
                if (stopOnSectorLimit && remainingSectorsForAltitude == 0) {
                    break;
                }
            }
        }
        remainingSectorsPerAltitude.put(altitude, remainingSectorsForAltitude);
        return altitudeSectors;
    }

    private void calculatePeaks() {

        int peakAltitude = calculateHighestAltitude();

        for (FieldBattleSetupArea fbsa : setupAreas.values()) {
            int setupAreaPeakSize = MathUtils.generateRandomIntInRange(10, 20);
            try {
                placePeakInSetupArea(fbsa, setupAreaPeakSize, peakAltitude);
            } catch (EffortCountExceededException e) {
                System.out.println("Effort count exceeded while placing setup area peak of size " + setupAreaPeakSize + ". Retrying.");
            }
        }

        // remaining peaks
        Integer remainingPeakSectors = remainingSectorsPerAltitude.get(peakAltitude);
        while (remainingSectorsPerAltitude.get(peakAltitude) > 0) {
            int ramdomPeakSize = 0;
            if (remainingPeakSectors < 20) {
                ramdomPeakSize = remainingPeakSectors;
            } else {
                ramdomPeakSize = MathUtils.generateRandomIntInRange(5, 20);
            }
            try {
                placeRamdomPeak(ramdomPeakSize, peakAltitude);
            } catch (EffortCountExceededException e) {
                System.out.println("Effort count exceeded while placing peak of size " + ramdomPeakSize + ". Retrying.");
            }
        }
    }

    /**
     * Calculates the peak altitude, depending on the percentages of the altitudes the map must have.
     *
     * @return the peak altitude (3 or 4) or 0 if there are no peaks
     */
    private int calculateHighestAltitude() {
        int peakAltitude = altitudePercentages.get(4) > 0 ? 4 :
                altitudePercentages.get(3) > 0 ? 3 :
                        altitudePercentages.get(2) > 0 ? 2 :
                                1;
        return peakAltitude;
    }

    private void placePeakInSetupArea(FieldBattleSetupArea fbsa, int peakSize, int peakAltitude) {
        Set<FieldBattleSector> peakSectors = findPeakClusterInRegion(fbMap, peakSize,
                fbsa.getStartX(), fbsa.getEndX(), fbsa.getStartY(), fbsa.getEndY(), MAX_EFFORTS, peakAltitude);

        for (FieldBattleSector sector : peakSectors) {
            sector.setAltitude(peakAltitude);
        }

        remainingSectorsPerAltitude.put(peakAltitude, remainingSectorsPerAltitude.get(peakAltitude) - peakSize);
    }

    private void placeRamdomPeak(int peakSize, int peakAltitude) {

        Set<FieldBattleSector> peakSectors = findPeakClusterInRegion(fbMap, peakSize,
                0, fbMap.getSizeX() - 1, 0, fbMap.getSizeY() - 1, MAX_EFFORTS, peakAltitude);

        for (FieldBattleSector sector : peakSectors) {
            sector.setAltitude(peakAltitude);
        }

        remainingSectorsPerAltitude.put(peakAltitude, remainingSectorsPerAltitude.get(peakAltitude) - peakSize);
    }

    private void calculateRiversAndLakesAltitude() {
        Set<FieldBattleSector> riverOrLakeSectors = MapUtils.getAllRiverOrLakeSectors(fbMap);
        for (FieldBattleSector sector : riverOrLakeSectors) {
            sector.setAltitude(1);
        }

        // for water bank tiles that are adjacent to 2 or more water tiles, also set the altitude to 1
        // (this was a solution for the diagonal river in valley rendering problem)
        Set<FieldBattleSector> waterBanks = MapUtils.findNeighboursSectors(riverOrLakeSectors);
        Set<FieldBattleSector> waterBanksWithAltitude1 = new HashSet<FieldBattleSector>();

        for (FieldBattleSector waterBank : waterBanks) {
            Set<FieldBattleSector> waterBankNeighbours = MapUtils.getHorizontalAndVerticalNeighbours(waterBank);

            int waterNeighbourCount = 0;
            for (FieldBattleSector waterBankNeighbour : waterBankNeighbours) {
                if (waterBankNeighbour.isLake()
                        || waterBankNeighbour.isMinorRiver()
                        || waterBankNeighbour.isMajorRiver()) {
                    waterNeighbourCount++;
                }
                if (waterNeighbourCount == 2) {
                    waterBank.setAltitude(1);
                    waterBanksWithAltitude1.add(waterBank);
                    break;
                }
            }
        }

        Integer remaining = remainingSectorsPerAltitude.get(1);
        remainingSectorsPerAltitude.put(1, remaining - riverOrLakeSectors.size() - waterBanksWithAltitude1.size());
    }


    /**
     * Returns a random cluster of comprising of a specific number of sectors, enclosed in a specific rectangular region on the map.
     * Method has is given a maximum number of efforts to try, which if exceeded an {@link EffortCountExceededException}.
     *
     * @param fbMap        the field battle map
     * @param sectorNumber the number of sectors
     * @param minX         the starting X of the region
     * @param maxX         the ending X of the region
     * @param minY         the starting Y of the region
     * @param maxY         the ending Y of the region
     * @param maxEfforts   maximum number of efforts to try before throwing {@link EffortCountExceededException}. If negative, it will be ignored.
     * @return the set of sectors
     */
    private Set<FieldBattleSector> findPeakClusterInRegion(FieldBattleMap fbMap, int sectorNumber, int minX,
                                                           int maxX, int minY, int maxY, int maxEfforts, int peakAltitude) throws EffortCountExceededException {

        List<FieldBattleSector> clusterSectors = new ArrayList<FieldBattleSector>();

        // calculate the size of the square to contain the area
        int squareSideSize = calculateSquareSizeFittingCluster(sectorNumber);

        int effortCount = 0;
        while (clusterSectors.isEmpty()) {

            if (maxEfforts > 0 && effortCount > maxEfforts) {
                VisualisationUtils.visualize(fbMap);
                VisualisationUtils.visualizeAltitude(fbMap);
                throw new EffortCountExceededException();
            } else {
                effortCount++;
            }

            FieldBattleSector startingSector = fbMap.getFieldBattleSector(MathUtils.generateRandomIntInRange(minX, maxX),
                    MathUtils.generateRandomIntInRange(minY, maxY));

            if (!sectorAppropriateForPeak(startingSector, peakAltitude)) {
                continue;
            }

            Set<FieldBattleSector> squareSectors = calculateSquareSectors(startingSector, squareSideSize);

            clusterSectors.add(startingSector);

            addNeighbourToCluster:
            for (int neighbourEffortCount = 0; clusterSectors.size() < sectorNumber; neighbourEffortCount++) {

                if (neighbourEffortCount > 500) {
                    clusterSectors.clear();
                    break;
                }

                FieldBattleSector randomClusterSector = clusterSectors.get(MathUtils.generateRandomIntInRange(0, clusterSectors.size() - 1));

                Set<FieldBattleSector> neighbours = MapUtils.getNeighbours(randomClusterSector);
                FieldBattleSector neighbour = new ArrayList<FieldBattleSector>(neighbours).get(MathUtils.generateRandomIntInRange(0, neighbours.size() - 1));

                if (clusterSectors.contains(neighbour) || !squareSectors.contains(neighbour) || !sectorAppropriateForPeak(neighbour, peakAltitude)) {
                    continue addNeighbourToCluster;
                } else {
                    clusterSectors.add(neighbour);
                }
            }

        }

        return new HashSet<FieldBattleSector>(clusterSectors);
    }

    private boolean sectorAppropriateForPeak(FieldBattleSector sector, int peakAltitude) {
        boolean result = false;
        result = sector.getAltitude() == 0
                && !sector.hasSectorWall()
                && !sector.isFortificationInterior()
                && !sector.hasSectorChateau()
                && !sector.hasSectorVillage()
                && !sector.hasSectorTown()
                && !sector.isRoad()
                && !sector.isMinorRiver()
                && !sector.isLake();

        if (result) {
            // peaks must not be adjacent, so check if its neighbours are next to another peak
            for (FieldBattleSector neighboursNeighbour : MapUtils.getNeighbours(sector)) {
                if (neighboursNeighbour.getAltitude() == peakAltitude) {
                    result = false;
                    break;
                }
            }
        }

        // altitude 4 peaks cannot be adjacent to cities, roads or forts
        if (result && peakAltitude == 4) {
            for (FieldBattleSector neighbour : MapUtils.getNeighbours(sector)) {
                if (neighbour.isRoad()
                        || neighbour.hasSectorTown()
                        || neighbour.isFortificationInterior()
                        || neighbour.hasSectorWall()) {
                    result = false;
                    break;
                }
            }
        }

        if (result) {
            result = sectorAppropriateForAltitude(sector, peakAltitude);
        }

        return result;
    }

    private boolean sectorAppropriateForAltitude(FieldBattleSector sector, int altitude) {

        Set<FieldBattleSector> neightboursNeighbours = MapUtils.findSectorsInRadius(sector, 4);
        for (FieldBattleSector neighboursNeighbour : neightboursNeighbours) {
            // if altitude difference with a neighbour's neighbour is more that what can be covered by their distance, sector is not appropriate
            if (neighboursNeighbour.getAltitude() != 0
                    && MapUtils.getSectorsDistance(sector, neighboursNeighbour) < Math.abs(altitude - neighboursNeighbour.getAltitude())) {
                return false;
            }
        }

        return true;
    }

    public FieldBattleMap getFbMap() {
        return fbMap;
    }

    public void setFbMap(FieldBattleMap fbMap) {
        this.fbMap = fbMap;
    }

    public Map<Integer, Integer> getRemainingSectorsPerAltitude() {
        return remainingSectorsPerAltitude;
    }

    public void setRemainingSectorsPerAltitude(
            Map<Integer, Integer> remainingSectorsPerAltitude) {
        this.remainingSectorsPerAltitude = remainingSectorsPerAltitude;
    }

    public Map<Integer, Integer> getAltitudePercentages() {
        return altitudePercentages;
    }

}
