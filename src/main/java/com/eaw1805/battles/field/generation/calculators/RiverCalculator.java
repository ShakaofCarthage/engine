package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.field.generation.BasePathGenerator;
import com.eaw1805.battles.field.generation.MajorRiverGenerator;
import com.eaw1805.battles.field.generation.MinorRiverGenerator;
import com.eaw1805.battles.field.generation.NonTraversingMinorRiverGenerator;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.List;
import java.util.Set;

/**
 * This is a helper class that adds rivers to a field battle map. It
 * conceptually separates the map into 4 quarters, and creates rivers from a
 * quarter to its opposite. Rivers only start from and end to the vertical borders of
 * of the map (from 1 to 3 or from 4 to 2).
 * <pre>
 * +++++++++
 * + 1 | 4 +
 * +-------+
 * + 2 | 3 +
 * +++++++++
 * </pre>
 *
 * @author fragkakis
 */
public class RiverCalculator {

    /**
     * Adds a non-traversing minor river (a 1-width river that horizontally traverses the 2/3 of the map).
     *
     * @param fbMap the field battle map
     */
    public void addNonTraversingMinorRiver(FieldBattleMap fbMap) {

        int entryQuarter = MathUtils.generateRandomIntInRange(1, 2);
        int exitQuarter = findMirrorQuarter(entryQuarter);

        FieldBattleSector entrySector = null;
        FieldBattleSector exitSector = null;

        if (MathUtils.generateRandomIntInRange(0, 1) == 1) {
            entrySector = findSectorInQuarter(fbMap, entryQuarter);
            exitSector = findBorderSectorInQuarter(fbMap, exitQuarter);
        } else {
            entrySector = findBorderSectorInQuarter(fbMap, entryQuarter);
            exitSector = findSectorInQuarter(fbMap, exitQuarter);
        }

        BasePathGenerator nonTraveringRiverGenerator = new NonTraversingMinorRiverGenerator(fbMap);
        List<FieldBattleSector> path = nonTraveringRiverGenerator.generate(entrySector, exitSector);

        for (FieldBattleSector sector : path) {
            sector.setMinorRiver(true);
        }

    }

    /**
     * Adds a minor river (1-width) that traverses horizontally the map.
     *
     * @param fbMap the field battle map
     */
    public void addMinorRiver(FieldBattleMap fbMap) {

        int entryQuarter = MathUtils.generateRandomIntInRange(1, 2);
        int exitQuarter = findMirrorQuarter(entryQuarter);

        FieldBattleSector entrySector = findBorderSectorInQuarter(fbMap, entryQuarter);
        FieldBattleSector exitSector = findBorderSectorInQuarter(fbMap, exitQuarter);

        BasePathGenerator riverGenerator = new MinorRiverGenerator(fbMap);
        List<FieldBattleSector> path = riverGenerator.generate(entrySector, exitSector);

        for (FieldBattleSector sector : path) {
            sector.setMinorRiver(true);
        }

    }

    /**
     * Adds a major river (2-3 width) that traverses horizontally the map.
     *
     * @param fbMap the field battle map
     */
    public void addMajorRiver(FieldBattleMap fbMap) {

        int entryQuarter = MathUtils.generateRandomIntInRange(1, 2);
        int exitQuarter = findMirrorQuarter(entryQuarter);

        FieldBattleSector entrySector = findBorderSectorInQuarter(fbMap, entryQuarter);
        FieldBattleSector exitSector = findBorderSectorInQuarter(fbMap, exitQuarter);

        MajorRiverGenerator majorRiverGenerator = new MajorRiverGenerator(fbMap);
        Set<FieldBattleSector> majorRiverSectors = majorRiverGenerator.generate(entrySector, exitSector);

        for (FieldBattleSector sector : majorRiverSectors) {
            sector.setMajorRiver(true);
        }

    }

    private FieldBattleSector findBorderSectorInQuarter(FieldBattleMap fbMap, int quarter) {
        switch (quarter) {
            case 1:
                return fbMap.getFieldBattleSector(0, MathUtils.generateRandomIntInRange(fbMap.getSizeY() / 2, fbMap.getSizeY() - 1 - 5));
            case 2:
                return fbMap.getFieldBattleSector(0, MathUtils.generateRandomIntInRange(5, fbMap.getSizeY() / 2 - 1));
            case 3:
                return fbMap.getFieldBattleSector(fbMap.getSizeX() - 1, MathUtils.generateRandomIntInRange(5, fbMap.getSizeY() / 2 - 1));
            default:
                // case 4
                return fbMap.getFieldBattleSector(fbMap.getSizeX() - 1, MathUtils.generateRandomIntInRange(fbMap.getSizeY() / 2, fbMap.getSizeY() - 1 - 5));
        }
    }

    /**
     * This method returns a sector in the first 1/3 of a quarter
     *
     * @param fbMap
     * @param quarter
     * @return
     */
    private FieldBattleSector findSectorInQuarter(FieldBattleMap fbMap, int quarter) {
        switch (quarter) {
            case 1:
                return fbMap.getFieldBattleSector(fbMap.getSizeX() / 3, MathUtils.generateRandomIntInRange(fbMap.getSizeY() / 2, fbMap.getSizeY() - 1 - 10));
            case 2:
                return fbMap.getFieldBattleSector(fbMap.getSizeX() / 3, MathUtils.generateRandomIntInRange(10, fbMap.getSizeY() / 2 - 1));
            case 3:
                return fbMap.getFieldBattleSector(fbMap.getSizeX() * 2 / 3 - 1, MathUtils.generateRandomIntInRange(10, fbMap.getSizeY() / 2 - 1));
            default:
                // case 4
                return fbMap.getFieldBattleSector(fbMap.getSizeX() * 2 / 3 - 1, MathUtils.generateRandomIntInRange(fbMap.getSizeY() / 2, fbMap.getSizeY() - 1 - 10));
        }
    }

    private int findMirrorQuarter(int entryQuarter) {
        switch (entryQuarter) {
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 1;
            default:
                // case 4
                return 2;
        }
    }

}
