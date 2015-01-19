package com.eaw1805.battles.field.generation;

import com.eaw1805.algorithms.SimpleWeightedEdge;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used for the generation of roads on the map.
 *
 * @author fragkakis
 */
public class RoadGenerator
        extends BasePathGenerator {

    /**
     * Public constructor.
     *
     * @param fbMap
     */
    public RoadGenerator(FieldBattleMap fbMap) {
        super(fbMap);
    }

    @Override
    public List<FieldBattleSector> generate(FieldBattleSector startSector, FieldBattleSector endSector) {

        boolean ascending = endSector.getY() - startSector.getY() > 0;

        addGraphEdgesForRoad(fbMap, endSector, ascending);
        return shortestPath(startSector, endSector);

    }

    private void addGraphEdgesForRoad(FieldBattleMap fbMap, FieldBattleSector endSector, boolean ascending) {

        for (int x = 0; x < fbMap.getSizeX(); x++) {

            for (int y = 0; y < fbMap.getSizeY(); y++) {

                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);

                for (FieldBattleSector neighbour : getNeighbours(sector)) {

                    SimpleWeightedEdge edge = graph.addEdge(sector, neighbour);
                    double weight = 0;

                    if (!sector.isMinorRiver() && !neighbour.isMinorRiver()) {

                        if (neighbour.hasSectorWall()) {
                            // cannot pass through wall
                            weight = IMPASSABLE;
                        } else if (neighbour.isLake()) {
                            // cannot cross lake
                            weight = IMPASSABLE;
                        } else if (endSector != null && neighbour.isRoad() && neighbour != endSector) {
                            // cannot intersect with road, unless this is the end sector
                            weight = IMPASSABLE;
                        } else if (endSector != null && !neighbour.isRoad()
                                && getRoadNeighbours(neighbour).size() > 0
                                && !getRoadNeighbours(neighbour).contains(endSector)) {
                            // cannot go to a neighbour or road, unless it is next to the end sector
                            weight = IMPASSABLE;
                        } else if (ascending && sector.getY() < neighbour.getY()
                                || !ascending && sector.getY() > neighbour.getY()) {
                            // default case
                            if (sector.getX() == neighbour.getX()) {
                                // vertical: optimal case
                                weight = Integer.valueOf(MathUtils.generateRandomIntInRange(1, 2)).doubleValue();
                            } else {
                                // diagonal
                                if ((ascending && fbMap.getFieldBattleSector(sector.getX(), sector.getY() + 1).isMinorRiver())
                                        || (!ascending && fbMap.getFieldBattleSector(sector.getX(), sector.getY() - 1).isMinorRiver())) {
                                    // cannot diagonally cross a river
                                    weight = IMPASSABLE;
                                } else {
                                    weight = Integer.valueOf(MathUtils.generateRandomIntInRange(1, 12)).doubleValue();
                                }
                            }

                        } else if (sector.getY() == neighbour.getY()) {
                            weight = Integer.valueOf(MathUtils.generateRandomIntInRange(1, 30)).doubleValue();

                        } else {
                            // avoid going backwards
                            weight = IMPASSABLE;
                        }

                    } else {

                        if (neighbour.getX() != sector.getX()) {
                            // only cross a river vertically
                            weight = IMPASSABLE;
                        } else {
                            weight = 1;
                        }
                    }

                    if (neighbour.isMajorRiver() || neighbour.isMinorRiver()) {
                        // this makes bridges have the minimum possible width
                        weight += 5;
                    }
                    graph.setEdgeWeight(edge, weight);
//						System.out.println("Added edge " + sector + " - " + neighbour + " with weight: " + weight);
                }
            }
        }
    }

    private Set<FieldBattleSector> getNeighbours(FieldBattleSector sector) {
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

    private Set<FieldBattleSector> getRoadNeighbours(FieldBattleSector sector) {

        Set<FieldBattleSector> roadNeightbours = new HashSet<FieldBattleSector>();

        for (FieldBattleSector neighbour : getNeighbours(sector)) {
            if (neighbour.isRoad()) {
                roadNeightbours.add(neighbour);
            }
        }

        return roadNeightbours;
    }

}
