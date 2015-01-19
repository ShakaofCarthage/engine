package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that calculates clusters of {@link FieldBattleSector}s on a {@link FieldBattleMap}.
 *
 * @author fragkakis
 */
public class ClusterCalculator {

    /**
     * Returns a random cluster of comprising of a specific number of sectors.
     *
     * @param fbMap        the field battle map
     * @param sectorNumber the number of sectors
     * @return the set of sectors
     */
    public Set<FieldBattleSector> findRandomCluster(FieldBattleMap fbMap, int sectorNumber, ClusterTypeEnum type) {

        return findRandomClusterInRegion(fbMap, sectorNumber, 0, fbMap.getSizeX() - 1, 0, fbMap.getSizeY() - 1, type);
    }

    /**
     * Returns a random cluster of comprising of a specific number of sectors.
     * Method has is given a maximum number of efforts to try, which if exceeded an {@link EffortCountExceededException}.
     *
     * @param fbMap        the field battle map
     * @param sectorNumber the number of sectors
     * @param maxEfforts   maximum number of efforts to try before throwing {@link EffortCountExceededException}. If negative, it will be ignored.
     * @return the set of sectors
     */
    public Set<FieldBattleSector> findRandomCluster(FieldBattleMap fbMap, int sectorNumber, int maxEfforts, ClusterTypeEnum type) {

        return findRandomClusterInRegion(fbMap, sectorNumber, 0, fbMap.getSizeX() - 1, 0, fbMap.getSizeY() - 1, maxEfforts, type);
    }

    /**
     * Returns a random cluster of comprising of a specific number of sectors, enclosed in a specific rectangular region on the map.
     *
     * @param fbMap        the field battle map
     * @param sectorNumber the number of sectors
     * @param minX         the starting X of the region
     * @param maxX         the ending X of the region
     * @param minY         the starting Y of the region
     * @param maxY         the ending Y of the region
     * @return the set of sectors
     */
    public Set<FieldBattleSector> findRandomClusterInRegion(FieldBattleMap fbMap, int sectorNumber, int minX, int maxX, int minY, int maxY, ClusterTypeEnum type) {

        return findRandomClusterInRegion(fbMap, sectorNumber, minX, maxX, minY, maxY, -1, type);
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
    public Set<FieldBattleSector> findRandomClusterInRegion(FieldBattleMap fbMap, int sectorNumber, int minX,
                                                            int maxX, int minY, int maxY, int maxEfforts, ClusterTypeEnum type) throws EffortCountExceededException {

        List<FieldBattleSector> clusterSectors = new ArrayList<FieldBattleSector>();

        // calculate the size of the square to contain the area
        int squareSideSize = calculateSquareSizeFittingCluster(sectorNumber);

        int effortCount = 0;
        while (clusterSectors.isEmpty()) {

            if (maxEfforts > 0 && effortCount > maxEfforts) {
                throw new EffortCountExceededException();
            } else {
                effortCount++;
            }

            FieldBattleSector startingSector = fbMap.getFieldBattleSector(MathUtils.generateRandomIntInRange(minX, maxX),
                    MathUtils.generateRandomIntInRange(minY, maxY));

            if (!startingSector.isEmpty() && !startingSector.isRoad()) {
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

                if (clusterSectors.contains(neighbour) || !squareSectors.contains(neighbour) ||
                        (!neighbour.isEmpty() && !neighbour.isRoad())) {
                    continue addNeighbourToCluster;
                } else {
                    for (FieldBattleSector neighboursNeighbour : MapUtils.getNeighbours(neighbour)) {
                        if (MapUtils.isSectorOfType(neighboursNeighbour, type)) {
                            continue addNeighbourToCluster;
                        }
                    }
                    clusterSectors.add(neighbour);
                }
            }

        }

        return new HashSet<FieldBattleSector>(clusterSectors);
    }

    public Set<FieldBattleSector> calculateSquareSectors(FieldBattleSector startingSector, int squareSideSize) {

        int mapMaxX = startingSector.getMap().getSizeX() - 1;
        int mapMaxY = startingSector.getMap().getSizeY() - 1;

        int startingSectorX = startingSector.getX();
        int startingSectorY = startingSector.getY();

        int squareStartX = startingSectorX - squareSideSize / 2;
        int squareEndX = startingSectorX + squareSideSize / 2 - 1;
        int squareStartY = startingSectorY - squareSideSize / 2;
        int squareEndY = startingSectorY + squareSideSize / 2 - 1;

        int horizontalDisplace = 0;
        if (squareStartX < 0) {
            horizontalDisplace = Math.abs(squareStartX);
        } else if (mapMaxX < squareEndX) {
            horizontalDisplace = mapMaxX - squareEndX;
        }

        int verticalDisplace = 0;
        if (squareStartY < 0) {
            verticalDisplace = Math.abs(squareStartY);
        } else if (mapMaxY < squareEndY) {
            verticalDisplace = mapMaxY - squareEndY;
        }

        // displace as appropriate
        squareStartX = squareStartX + horizontalDisplace;
        squareEndX = squareEndX + horizontalDisplace;
        squareStartY = squareStartY + verticalDisplace;
        squareEndY = squareEndY + verticalDisplace;

        return MapUtils.findSectorsInArea(startingSector.getMap(), squareStartX, squareEndX, squareStartY, squareEndY, false);
    }

    protected int calculateSquareSizeFittingCluster(int clusterSize) {
        int squareSideSize = 1;
        for (int i = 3; true; i += 2) {
            if (i * i >= clusterSize) {
                squareSideSize = i + (i / 3 < 2 ? 2 : i / 3);
                break;
            }
            squareSideSize++;
        }
        return squareSideSize;
    }

    private Set<FieldBattleSector> findSectors(FieldBattleMap fbMap, FieldBattleSector startingBattleSector, int sectorNumber, int squareSideSize) {

        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        if (startingBattleSector.isEmpty() || startingBattleSector.isRoad() || startingBattleSector.isMinorRiver()) {

            boolean[][] villageMask = generateMaskInSquare(sectorNumber, squareSideSize);

            // place cluster on terrain
            sectors = getSectorsCorrespondingToMask(fbMap,
                    squareSideSize, startingBattleSector.getX(), startingBattleSector.getY(),
                    villageMask);

            for (FieldBattleSector sector : sectors) {
                if (!sector.isEmpty() && !sector.isRoad()) {
                    return new HashSet<FieldBattleSector>();
                }
            }
        }

        return sectors;

    }

    private Set<FieldBattleSector> getSectorsCorrespondingToMask(
            FieldBattleMap fbMap, int squareSideSize, int startingPointX,
            int startingPointY, boolean[][] villageMask) {
        Set<FieldBattleSector> sectors;
        sectors = new HashSet<FieldBattleSector>();

        for (int relativeX = 0; relativeX < squareSideSize; relativeX++) {
            for (int relativeY = 0; relativeY < squareSideSize; relativeY++) {
                if (villageMask[relativeX][relativeY]) {
                    FieldBattleSector fieldBattleSector = fbMap.getFieldBattleSector(startingPointX - squareSideSize / 2 + relativeX,
                            startingPointY - squareSideSize / 2 + relativeY);
                    if (!fieldBattleSector.isRoad()) {
                        sectors.add(fieldBattleSector);
                    }
                }
            }
        }
        return sectors;
    }

    private boolean[][] generateMaskInSquare(int sectorNumber,
                                             int squareSideSize) {
        boolean[][] villageMask = new boolean[squareSideSize][squareSideSize];

        int randomDisplacerX = MathUtils.generateRandomIntInRange(-1, 1);
        int randomDisplacerY = MathUtils.generateRandomIntInRange(-1, 1);
        // occupy center sector as village part
        if (squareSideSize % 2 == 1) {
            villageMask[squareSideSize / 2 + randomDisplacerX][squareSideSize / 2 + randomDisplacerY] = true;
        } else {
            int centerDisplacerX = MathUtils.generateRandomIntInRange(0, 1);
            int centerDisplacerY = MathUtils.generateRandomIntInRange(0, 1);

            villageMask[squareSideSize / 2 - centerDisplacerX + randomDisplacerX][squareSideSize / 2 - centerDisplacerY + randomDisplacerY] = true;

        }

        // create a random "mask" of the sectors to occupy
        for (int i = 0; i < sectorNumber - 1; i++) {

            boolean villagePartPlaced = false;
            while (!villagePartPlaced) {

                int villagePartRelativeX = MathUtils.generateRandomIntInRange(0, squareSideSize - 1);
                int villagePartRelativeY = MathUtils.generateRandomIntInRange(0, squareSideSize - 1);

                if (!villageMask[villagePartRelativeX][villagePartRelativeY]
                        && (
                        (villagePartRelativeX + 1 <= squareSideSize - 1 &&
                                villageMask[villagePartRelativeX + 1][villagePartRelativeY])
                                || (villagePartRelativeX - 1 >= 0 &&
                                villageMask[villagePartRelativeX - 1][villagePartRelativeY])
                                || (villagePartRelativeY + 1 <= squareSideSize - 1 &&
                                villageMask[villagePartRelativeX][villagePartRelativeY + 1])
                                || (villagePartRelativeY - 1 >= 0 &&
                                villageMask[villagePartRelativeX][villagePartRelativeY - 1])
                )) {

                    villageMask[villagePartRelativeX][villagePartRelativeY] = true;
                    villagePartPlaced = true;
                }
            }
        }
        return villageMask;
    }

    /**
     * Finds a square cluster that can be used for a town (may contain roads, rivers), enclosed in a specific area.
     *
     * @param fbMap       the field battle map
     * @param squareSizeX the horizontal size of the cluster
     * @param squareSizeY the vertical size of the cluster
     * @param minX        the starting X of the region
     * @param maxX        the ending X of the region
     * @param minY        the starting Y of the region
     * @param maxY        the ending Y of the region
     * @return the set of sectors
     */
    public Set<FieldBattleSector> findSquareTownClusterInArea(FieldBattleMap fbMap, int squareSizeX, int squareSizeY, int minX, int maxX, int minY, int maxY) {

        boolean areaFound = false;
        Set<FieldBattleSector> sectors = null;

        List<FieldBattleSector> roadSectorsInArea = findRoadSectorsInArea(fbMap, minX + squareSizeX / 2, maxX - squareSizeX / 2, minY + squareSizeY / 2, maxY - squareSizeY / 2);

        int tryCount = 0;
        findArea:
        while (!areaFound) {

            sectors = new HashSet<FieldBattleSector>();

            FieldBattleSector townCenter = roadSectorsInArea.get(MathUtils.generateRandomIntInRange(0, roadSectorsInArea.size() - 1));

            int squareStartX = townCenter.getX() - squareSizeX / 2;
            int squareStartY = townCenter.getY() - squareSizeY / 2;

            for (int x = squareStartX; x < squareStartX + squareSizeX; x++) {

                for (int y = squareStartY; y < squareStartY + squareSizeY; y++) {

                    FieldBattleSector townSector = townCenter.getMap().getFieldBattleSector(x, y);
                    if (townSector.isEmpty()) {
                        sectors.add(townSector);
                    } else {
                        // there are cases where a non-empty sector (i.e. river, road is inside the town), o skip it
                        if (townSector.isMinorRiver() || townSector.isRoad()) {
                            continue;
                        } else {
                            tryCount++;
                            if (tryCount > 15) {
                                VisualisationUtils.visualize(fbMap);
                                throw new RuntimeException("Something is wrong");
                            }
                            continue findArea;
                        }
                    }
                }
            }
            areaFound = true;
        }

        return sectors;
    }

    private List<FieldBattleSector> findRoadSectorsInArea(FieldBattleMap fbMap, int minX, int maxX, int minY, int maxY) {

        List<FieldBattleSector> roadSectorsInArea = new ArrayList<FieldBattleSector>();

        for (int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {

                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.isRoad()) {
                    roadSectorsInArea.add(sector);
                }
            }
        }

        return roadSectorsInArea;
    }


    /**
     * Finds a random cluster that can be used for a town (may contain roads, rivers).
     *
     * @param townCenter the town center (always a road sector)
     * @param townSize   the size of the town in sectors
     * @return the sectors
     */
    public Set<FieldBattleSector> findRandomTownCluster(FieldBattleSector townCenter,
                                                        int townSize) {

        // calculate the size of the square to contain the area
        int squareSideSize = 0;
        for (int i = 1; true; i++) {
            if (i * i >= townSize) {
                squareSideSize = i + 1;
                break;
            }
        }

        return findSectors(townCenter.getMap(), townCenter, townSize, squareSideSize);

    }


}
