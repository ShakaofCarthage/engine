package com.eaw1805.battles.field.generation;

import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used for the generation of major rivers on the map.
 *
 * @author fragkakis
 */
public class MajorRiverGenerator {

    private MinorRiverGenerator minorRiverGenerator;

    /**
     * Public constructor.
     *
     * @param fbMap
     */
    public MajorRiverGenerator(FieldBattleMap fbMap) {
        minorRiverGenerator = new MinorRiverGenerator(fbMap);
    }

    public Set<FieldBattleSector> generate(FieldBattleSector entrySector, FieldBattleSector exitSector) {
        Set<FieldBattleSector> majorRiverSectors = new HashSet<FieldBattleSector>();

        List<FieldBattleSector> path = minorRiverGenerator.generate(entrySector, exitSector);
        for (FieldBattleSector sector : path) {
            majorRiverSectors.add(sector);
            majorRiverSectors.addAll(getLowerNeighbours(sector));
        }

        return majorRiverSectors;
    }

    private Set<FieldBattleSector> getLowerNeighbours(FieldBattleSector sector) {
        Set<FieldBattleSector> neighbours = new HashSet<FieldBattleSector>();
        for (int x = sector.getX() - 1; x <= sector.getX() + 1; x++) {

            if (x < 0 || sector.getMap().getSizeX() - 1 < x) {
                continue;
            }

            for (int y = sector.getY() - 1; y <= sector.getY(); y++) {
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

}
