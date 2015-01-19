package com.eaw1805.battles.field.generation;

import com.eaw1805.algorithms.SimpleWeightedEdge;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for path generation on the {@link FieldBattleMap}. Makes
 * use of the JGraphT library, in particular the DijkstraShortestPath algorithm.
 *
 * @author fragkakis
 */
public abstract class BasePathGenerator {

    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> graph;
    protected FieldBattleMap fbMap;

    protected static final double IMPASSABLE = 100000000d;

    /**
     * Public constructor.
     *
     * @param fbMap
     */
    public BasePathGenerator(FieldBattleMap fbMap) {
        this.fbMap = fbMap;

        graph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);

        // add vertices
        for (int x = 0; x < fbMap.getSizeX(); x++) {
            for (int y = 0; y < fbMap.getSizeY(); y++) {

                graph.addVertex(fbMap.getFieldBattleSector(x, y));
            }
        }
    }

    /**
     * The public method that actually generates the path as a {@link List} of {@link FieldBattleSector}s.
     *
     * @param entrySector the entry (start) sector
     * @param exitSector  the exit (end) sector
     * @return the Set of sectors that comprise the path
     */
    public abstract List<FieldBattleSector> generate(FieldBattleSector entrySector, FieldBattleSector exitSector);


    /**
     * Utility method that uses the Dijkstra algorith to compute the shortest path between two sectors.
     *
     * @param startSector the start sector of the path
     * @param endSector   the end sector of the path
     * @return the path
     */
    protected List<FieldBattleSector> shortestPath(FieldBattleSector startSector, FieldBattleSector endSector) {

        List<FieldBattleSector> path = new ArrayList<FieldBattleSector>();
        List<SimpleWeightedEdge> edges = DijkstraShortestPath.findPathBetween(graph, startSector, endSector);

        path.add(startSector);
//		System.out.println("(" + startSector.getX() + "," + startSector.getY() + ")");
        for (SimpleWeightedEdge edge : edges) {
            FieldBattleSector source = (FieldBattleSector) edge.getSource();
            FieldBattleSector target = (FieldBattleSector) edge.getTarget();
//			System.out.println(source + " --> " + target + " with weight: " + edge.getWeight() + "), target neighbours: " + getNeighbours(target));
            path.add(path.get(path.size() - 1) == source ? target : source);
        }
        return path;

    }
}
