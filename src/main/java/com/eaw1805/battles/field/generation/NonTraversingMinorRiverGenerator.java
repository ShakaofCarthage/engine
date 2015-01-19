package com.eaw1805.battles.field.generation;

import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.List;

/**
 * This class is used for the generation of non-traversing minor rivers on the map.
 *
 * @author fragkakis
 */
public class NonTraversingMinorRiverGenerator
        extends MinorRiverGenerator {

    /**
     * Public constructor.
     *
     * @param fbMap
     */
    public NonTraversingMinorRiverGenerator(FieldBattleMap fbMap) {
        super(fbMap);
    }

    @Override
    public List<FieldBattleSector> generate(FieldBattleSector startSector, FieldBattleSector endSector) {

        addGraphEdgesForRiver(startSector, endSector, false);
        return shortestPath(startSector, endSector);

    }

}
