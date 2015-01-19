package com.eaw1805.battles.field.movement;

import com.eaw1805.algorithms.SimpleWeightedEdge;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import org.jgrapht.Graph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base abstract class for movement calculation. Requires extension for each of the sides to determine what is "backwards".
 *
 * @author fragkakis
 */
public class BaseFieldBattlePathCalculator implements Serializable {

    private static final long serialVersionUID = -2184257890504267531L;

    protected FieldBattleMap fbMap;
    private final int side;
    private final Set<FieldBattleSector> brigadeOccupiedSectors;

    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> infantryGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> infantryInColumnGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> infantryBackwardsAllowedGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> infantryBackwardsAllowedInColumnGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> cavalryGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> cavalryInColumnGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> cavalryBackwardsAllowedGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> cavalryBackwardsAllowedInColumnGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> artilleryGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> artilleryInColumnGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> artilleryBackwardsAllowedGraph;
    protected SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> artilleryBackwardsAllowedInColumnGraph;

    /**
     * Public constructor.
     *
     * @param fbMap                  the field battle map
     * @param side                   the side for which to calculate paths. Can be 0 or 1.
     * @param brigadeOccupiedSectors the sectors that are occupied by a brigade, and are impassable.
     */
    public BaseFieldBattlePathCalculator(FieldBattleMap fbMap, int side, Set<FieldBattleSector> brigadeOccupiedSectors) {
        this.fbMap = fbMap;
        this.side = side;
        this.brigadeOccupiedSectors = brigadeOccupiedSectors;

        infantryGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        infantryInColumnGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        infantryBackwardsAllowedGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        infantryBackwardsAllowedInColumnGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        cavalryGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        cavalryInColumnGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        cavalryBackwardsAllowedGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        cavalryBackwardsAllowedInColumnGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        artilleryGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        artilleryInColumnGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        artilleryBackwardsAllowedGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);
        artilleryBackwardsAllowedInColumnGraph = new SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge>(SimpleWeightedEdge.class);

        // add vertices
        for (int x = 0; x < fbMap.getSizeX(); x++) {
            for (int y = 0; y < fbMap.getSizeY(); y++) {

                FieldBattleSector fieldBattleSector = fbMap.getFieldBattleSector(x, y);
                infantryGraph.addVertex(fieldBattleSector);
                infantryInColumnGraph.addVertex(fieldBattleSector);
                infantryBackwardsAllowedGraph.addVertex(fieldBattleSector);
                infantryBackwardsAllowedInColumnGraph.addVertex(fieldBattleSector);
                cavalryGraph.addVertex(fieldBattleSector);
                cavalryInColumnGraph.addVertex(fieldBattleSector);
                cavalryBackwardsAllowedGraph.addVertex(fieldBattleSector);
                cavalryBackwardsAllowedInColumnGraph.addVertex(fieldBattleSector);
                artilleryGraph.addVertex(fieldBattleSector);
                artilleryInColumnGraph.addVertex(fieldBattleSector);
                artilleryBackwardsAllowedGraph.addVertex(fieldBattleSector);
                artilleryBackwardsAllowedInColumnGraph.addVertex(fieldBattleSector);
            }
        }
        // add edges
        calculateEdges();
    }

    public List<FieldBattleSector> findPath(FieldBattleSector start, FieldBattleSector destination,
                                            ArmEnum arm, FormationEnum formation, boolean backwardsAllowed) {
        SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> graph = getGraphForArmAndFormationAndBackwardsMovement(arm, formation, backwardsAllowed);

        List<FieldBattleSector> path = shortestPath(graph, start, destination);

        return path;

    }

    public int findCost(FieldBattleSector start, FieldBattleSector destination,
                        ArmEnum arm, FormationEnum formation, boolean backwardsAllowed) {
        SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> graph = getGraphForArmAndFormationAndBackwardsMovement(arm, formation, backwardsAllowed);

        BellmanFordShortestPath<FieldBattleSector, SimpleWeightedEdge> bellman = new BellmanFordShortestPath<FieldBattleSector, SimpleWeightedEdge>(graph, start);
        return (int) bellman.getCost(destination);

    }

    public FieldBattleSector findClosest(FieldBattleSector start, Collection<FieldBattleSector> candidateDestinations,
                                         ArmEnum arm, FormationEnum formation, boolean backwardsAllowed) {
    	
    	// there cannot be closest than current position
    	if(candidateDestinations.contains(start)) {
    		return start;
    	}

        SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> graph = getGraphForArmAndFormationAndBackwardsMovement(arm, formation, backwardsAllowed);
        BellmanFordShortestPath<FieldBattleSector, SimpleWeightedEdge> bellman = new BellmanFordShortestPath<FieldBattleSector, SimpleWeightedEdge>(graph, start);

        double nearestDestinationCost = Integer.MAX_VALUE;
        FieldBattleSector destination = null;

        for (FieldBattleSector candidateDestination : candidateDestinations) {
            double enemySectorNeighbourCost = bellman.getCost(candidateDestination);
            if (enemySectorNeighbourCost < nearestDestinationCost) {
                destination = candidateDestination;
                nearestDestinationCost = enemySectorNeighbourCost;
            }
        }

        return destination;
    }

    private SimpleDirectedWeightedGraph<FieldBattleSector, SimpleWeightedEdge> getGraphForArmAndFormationAndBackwardsMovement(
            ArmEnum arm, FormationEnum formation, boolean backwardsAllowed) {

        switch (arm) {
            case ARTILLERY:
                switch (formation) {
                    case COLUMN:
                        return backwardsAllowed ? artilleryBackwardsAllowedInColumnGraph : artilleryInColumnGraph;
                    default:
                        return backwardsAllowed ? artilleryBackwardsAllowedGraph : artilleryGraph;
                }
            case CAVALRY:
                switch (formation) {
                    case COLUMN:
                        return backwardsAllowed ? cavalryBackwardsAllowedInColumnGraph : cavalryInColumnGraph;
                    default:
                        return backwardsAllowed ? cavalryBackwardsAllowedGraph : cavalryGraph;
                }
            case INFANTRY:
                switch (formation) {
                    case COLUMN:
                        return backwardsAllowed ? infantryBackwardsAllowedInColumnGraph : infantryInColumnGraph;
                    default:
                        return backwardsAllowed ? infantryBackwardsAllowedGraph : infantryGraph;
                }
            default:
                return null;
        }
    }

    private void calculateEdges() {

        Set<FieldBattleSector> allSectors = MapUtils.getAllSectors(fbMap);

        for (FieldBattleSector sector : allSectors) {
            addIncomingEdgesForSector(sector);
        }
    }

    private void addIncomingEdgesForSector(FieldBattleSector sector) {

        // sectors is not accessible, don't add incoming edges
        if (brigadeOccupiedSectors.contains(sector)) {
            return;
        }
        // wall sectors are not accessible
        if (sector.hasSectorWall()) {
            return;
        }

        boolean cavalryAllowed = true;
        boolean artilleryAllowed = true;
        double defaultInfantryBackwardsAllowedWeight = 0;
        double defaultCavalryBackwardsAllowedWeight = 0;
        double defaultArtilleryBackwardsAllowedWeight = 0;

        if (sector.isEmpty() || sector.isFortificationInterior() || sector.isRoad() || sector.hasSectorBridge()) {

            defaultInfantryBackwardsAllowedWeight = 2;
            defaultCavalryBackwardsAllowedWeight = 2;
            defaultArtilleryBackwardsAllowedWeight = 2;

        } else if (sector.isBush()) {
            defaultInfantryBackwardsAllowedWeight = 3;
            defaultCavalryBackwardsAllowedWeight = 4;
            defaultArtilleryBackwardsAllowedWeight = 6;

        } else if (sector.isForest()) {
            defaultInfantryBackwardsAllowedWeight = 4;
            defaultCavalryBackwardsAllowedWeight = 8;
            artilleryAllowed = false;

        } else if (sector.hasSectorChateau() || sector.hasSectorVillage() || sector.hasSectorTown()) {
            defaultInfantryBackwardsAllowedWeight = 4;
            cavalryAllowed = false;
            artilleryAllowed = false;

        } else if (sector.hasSectorEntrenchment()) {
            defaultInfantryBackwardsAllowedWeight = 4;
            defaultCavalryBackwardsAllowedWeight = 6;
            defaultArtilleryBackwardsAllowedWeight = 8;

        } else if (sector.hasSectorBridge()) {
            defaultInfantryBackwardsAllowedWeight = 3;
            defaultCavalryBackwardsAllowedWeight = 4;
            defaultArtilleryBackwardsAllowedWeight = 4;

        } else if (sector.isMinorRiver()) {
            defaultInfantryBackwardsAllowedWeight = 6;
            defaultCavalryBackwardsAllowedWeight = 10;
            defaultArtilleryBackwardsAllowedWeight = 12;

        } else if (sector.isMajorRiver()) {
            defaultInfantryBackwardsAllowedWeight = 99;
            defaultCavalryBackwardsAllowedWeight = 99;
            artilleryAllowed = false;

        }

        for (FieldBattleSector neighbour : MapUtils.getNeighbours(sector)) {

            int altitudeDifference = sector.getAltitude() - neighbour.getAltitude();

            double infantryBackwardsAllowedWeight = defaultInfantryBackwardsAllowedWeight + altitudeDifference;
            double cavalryBackwardsAllowedWeight = defaultCavalryBackwardsAllowedWeight + altitudeDifference;
            double artilleryBackwardsAllowedWeight = defaultArtilleryBackwardsAllowedWeight + altitudeDifference;

            if (neighbour.getX() == sector.getX()) {
                // favour straight lines
                infantryBackwardsAllowedWeight = infantryBackwardsAllowedWeight - 0.1;
                cavalryBackwardsAllowedWeight = cavalryBackwardsAllowedWeight - 0.1;
                artilleryBackwardsAllowedWeight = artilleryBackwardsAllowedWeight - 0.1;
            }

            double infantryBackwardsForbiddenWeight = -1;
            double cavalryBackwardsForbiddenWeight = -1;
            double artilleryBackwardsForbiddenWeight = -1;
            if (isBackwardsComparedTo(sector, neighbour)) {
                // don't go back
                infantryBackwardsForbiddenWeight = 1000;
                cavalryBackwardsForbiddenWeight = 1000;
                artilleryBackwardsForbiddenWeight = 1000;
            } else {
                infantryBackwardsForbiddenWeight = infantryBackwardsAllowedWeight;
                cavalryBackwardsForbiddenWeight = cavalryBackwardsAllowedWeight;
                artilleryBackwardsForbiddenWeight = artilleryBackwardsAllowedWeight;
            }

            infantryGraph.setEdgeWeight(infantryGraph.addEdge(neighbour, sector), infantryBackwardsForbiddenWeight);
            infantryBackwardsAllowedGraph.setEdgeWeight(infantryBackwardsAllowedGraph.addEdge(neighbour, sector), infantryBackwardsAllowedWeight);
            if(cavalryAllowed) {
	            cavalryGraph.setEdgeWeight(cavalryGraph.addEdge(neighbour, sector), cavalryBackwardsForbiddenWeight);
	            cavalryBackwardsAllowedGraph.setEdgeWeight(cavalryBackwardsAllowedGraph.addEdge(neighbour, sector), cavalryBackwardsAllowedWeight);
            }
            if(artilleryAllowed) {
	            artilleryGraph.setEdgeWeight(artilleryGraph.addEdge(neighbour, sector), artilleryBackwardsForbiddenWeight);
	            artilleryBackwardsAllowedGraph.setEdgeWeight(artilleryBackwardsAllowedGraph.addEdge(neighbour, sector), artilleryBackwardsAllowedWeight);
            }

            if (sector.isRoad() && neighbour.isRoad()) {
                infantryInColumnGraph.setEdgeWeight(infantryInColumnGraph.addEdge(neighbour, sector), infantryBackwardsForbiddenWeight - 1);
                infantryBackwardsAllowedInColumnGraph.setEdgeWeight(infantryBackwardsAllowedInColumnGraph.addEdge(neighbour, sector), infantryBackwardsAllowedWeight - 1);
                if(cavalryAllowed) {
	                cavalryInColumnGraph.setEdgeWeight(cavalryInColumnGraph.addEdge(neighbour, sector), cavalryBackwardsForbiddenWeight - 1);
	                cavalryBackwardsAllowedInColumnGraph.setEdgeWeight(cavalryBackwardsAllowedInColumnGraph.addEdge(neighbour, sector), cavalryBackwardsAllowedWeight - 1);
                }
                if(artilleryAllowed) {
	                artilleryInColumnGraph.setEdgeWeight(artilleryInColumnGraph.addEdge(neighbour, sector), artilleryBackwardsForbiddenWeight - 1);
	                artilleryBackwardsAllowedInColumnGraph.setEdgeWeight(artilleryBackwardsAllowedInColumnGraph.addEdge(neighbour, sector), artilleryBackwardsAllowedWeight - 1);
                }
            } else {
                infantryInColumnGraph.setEdgeWeight(infantryInColumnGraph.addEdge(neighbour, sector), infantryBackwardsForbiddenWeight);
                infantryBackwardsAllowedInColumnGraph.setEdgeWeight(infantryBackwardsAllowedInColumnGraph.addEdge(neighbour, sector), infantryBackwardsAllowedWeight);
                if(cavalryAllowed) {
	                cavalryInColumnGraph.setEdgeWeight(cavalryInColumnGraph.addEdge(neighbour, sector), cavalryBackwardsForbiddenWeight);
	                cavalryBackwardsAllowedInColumnGraph.setEdgeWeight(cavalryBackwardsAllowedInColumnGraph.addEdge(neighbour, sector), cavalryBackwardsAllowedWeight);
                }
                if(artilleryAllowed) {
	                artilleryInColumnGraph.setEdgeWeight(artilleryInColumnGraph.addEdge(neighbour, sector), artilleryBackwardsForbiddenWeight);
	                artilleryBackwardsAllowedInColumnGraph.setEdgeWeight(artilleryBackwardsAllowedInColumnGraph.addEdge(neighbour, sector), artilleryBackwardsAllowedWeight);
                }
            }
        }

    }

    private void removeIncomingEdgesFromSector(FieldBattleSector sector) {
        infantryGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(infantryGraph.incomingEdgesOf(sector)));
        infantryInColumnGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(infantryInColumnGraph.incomingEdgesOf(sector)));
        infantryBackwardsAllowedGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(infantryBackwardsAllowedGraph.incomingEdgesOf(sector)));
        infantryBackwardsAllowedInColumnGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(infantryBackwardsAllowedInColumnGraph.incomingEdgesOf(sector)));
        cavalryGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(cavalryGraph.incomingEdgesOf(sector)));
        cavalryInColumnGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(cavalryInColumnGraph.incomingEdgesOf(sector)));
        cavalryBackwardsAllowedGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(cavalryBackwardsAllowedGraph.incomingEdgesOf(sector)));
        cavalryBackwardsAllowedInColumnGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(cavalryBackwardsAllowedInColumnGraph.incomingEdgesOf(sector)));
        artilleryGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(artilleryGraph.incomingEdgesOf(sector)));
        artilleryInColumnGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(artilleryInColumnGraph.incomingEdgesOf(sector)));
        artilleryBackwardsAllowedGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(artilleryBackwardsAllowedGraph.incomingEdgesOf(sector)));
        artilleryBackwardsAllowedInColumnGraph.removeAllEdges(new HashSet<SimpleWeightedEdge>(artilleryBackwardsAllowedInColumnGraph.incomingEdgesOf(sector)));
    }


    /**
     * Utility method that uses the Dijkstra algorithm to compute the shortest path between two sectors.
     *
     * @param graph       the graph
     * @param startSector the start sector of the path
     * @param endSector   the end sector of the path
     * @return the path
     */
    private List<FieldBattleSector> shortestPath(Graph<FieldBattleSector, SimpleWeightedEdge> graph,
                                                 FieldBattleSector startSector, FieldBattleSector endSector) {

        List<SimpleWeightedEdge> edges = BellmanFordShortestPath.findPathBetween(graph, startSector, endSector);

        List<FieldBattleSector> path = null;
        if (edges != null) {
            path = new ArrayList<FieldBattleSector>();
            path.add(startSector);
            //		System.out.println("(" + startSector.getX() + "," + startSector.getY() + ")");
            for (SimpleWeightedEdge edge : edges) {
                FieldBattleSector source = (FieldBattleSector) edge.getSource();
                FieldBattleSector target = (FieldBattleSector) edge.getTarget();
                //			System.out.println(source + " --> " + target + " with weight: " + edge.getWeight() + "), target neighbours: " + getNeighbours(target));
                path.add(path.get(path.size() - 1) == source ? target : source);
            }
        }
        return path;

    }


    /**
     * Checks whether a sector is backwards in comparison to another sector.
     *
     * @param sector1 a sector
     * @param sector2 another sector
     * @return true if the first sector is backwards compared to the second sector
     */
    private boolean isBackwardsComparedTo(FieldBattleSector sector1, FieldBattleSector sector2) {
        if (side == 0) {
            return sector1.getY() < sector2.getY();
        } else {
            // side == 1
            return sector2.getY() < sector1.getY();
        }
    }

    public void makeSectorPassable(FieldBattleSector sector) {
        brigadeOccupiedSectors.remove(sector);
        addIncomingEdgesForSector(sector);
    }


    public void makeSectorImpassable(FieldBattleSector sector) {
        brigadeOccupiedSectors.add(sector);
        removeIncomingEdgesFromSector(sector);
    }

}
