package com.eaw1805.battles.field.generation;

import com.eaw1805.algorithms.SimpleWeightedEdge;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.List;

/**
 * This class is used for the generation of minor rivers on the map.
 *
 * @author fragkakis
 */
public class MinorRiverGenerator
        extends BasePathGenerator {

    /**
     * Public constructor.
     *
     * @param fbMap
     */
    public MinorRiverGenerator(FieldBattleMap fbMap) {
        super(fbMap);
    }

    @Override
    public List<FieldBattleSector> generate(FieldBattleSector startSector, FieldBattleSector endSector) {

        addGraphEdgesForRiver(startSector, endSector, true);
        return shortestPath(startSector, endSector);

    }

    protected void addGraphEdgesForRiver(FieldBattleSector startSector, FieldBattleSector endSector, boolean addObstacles) {

        FieldBattleMap fbMap = startSector.getMap();

        int obstacle1Xmin = fbMap.getSizeX() * 3 / 10;
        int obstacle1Xmax = fbMap.getSizeX() * 4 / 10;
        int obstacle2Xmin = fbMap.getSizeX() * 7 / 10;
        int obstacle2Xmax = fbMap.getSizeX() * 8 / 10;

        int obstacle1Ymin = 0;
        int obstacle1Ymax = 0;
        int obstacle2Ymin = 0;
        int obstacle2Ymax = 0;

        if (startSector.getY() < endSector.getY()) {

            obstacle1Ymin = 0;
            obstacle1Ymax = startSector.getY() + MathUtils.generateRandomIntInRange(4, 7);
            obstacle2Ymin = endSector.getY() - MathUtils.generateRandomIntInRange(4, 7);
            obstacle2Ymax = fbMap.getSizeY();

        } else {
            obstacle1Ymin = startSector.getY() - MathUtils.generateRandomIntInRange(4, 7);
            obstacle1Ymax = fbMap.getSizeY();
            obstacle2Ymin = 0;
            obstacle2Ymax = endSector.getY() + MathUtils.generateRandomIntInRange(4, 7);
        }

        for (int x = 0; x < fbMap.getSizeX(); x++) {

            for (int y = 0; y < fbMap.getSizeY(); y++) {

                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);

                for (FieldBattleSector neighbour : MapUtils.getNeighbours(sector)) {

                    SimpleWeightedEdge edge = graph.addEdge(sector, neighbour);
                    double weight = 0;

                    if (neighbour.hasSectorWall()) {
                        // cannot pass through wall
                        weight = IMPASSABLE;
                    } else if (addObstacles && obstacle1Xmin < sector.getX() && sector.getX() < obstacle1Xmax && obstacle1Ymin <= sector.getY() && sector.getY() <= obstacle1Ymax) {
                        weight = IMPASSABLE;
                    } else if (addObstacles && obstacle2Xmin < sector.getX() && sector.getX() < obstacle2Xmax && obstacle2Ymin <= sector.getY() && sector.getY() <= obstacle2Ymax) {
                        weight = IMPASSABLE;
                    } else if (sector.getX() < neighbour.getX()) {
                        // default case
                        if (sector.getY() == neighbour.getY()) {
                            weight = Integer.valueOf(MathUtils.generateRandomIntInRange(1, 2)).doubleValue();
                        } else {
                            weight = Integer.valueOf(MathUtils.generateRandomIntInRange(1, 8)).doubleValue();
                        }

                    } else if (sector.getX() == neighbour.getX()) {
                        weight = Integer.valueOf(MathUtils.generateRandomIntInRange(1, 15)).doubleValue();

                    } else {
                        // avoid going backwards
                        weight = IMPASSABLE;
                    }
                    graph.setEdgeWeight(edge, weight);
//						System.out.println("Added edge " + sector + " - " + neighbour + " with weight: " + weight);
                }
            }
        }
    }

}
