package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;

import java.util.List;

/**
 * This is a helper class that calculates the dimensions a field battle
 * map should have, and creates empty such maps.
 *
 * @author fragkakis
 */
public class MapDimensionsCalculator {

    /**
     * Creates a terrain of the appropriate size, depending on the total number of battalions (irrespective of size)
     *
     * @param sideBrigades list of lists of battalions (2 lists, 1 for each side)
     * @return the field battle terrain
     */
    public FieldBattleMap createEmptyMap(List<List<Brigade>> sideBrigades) {

        int battalionCount = 0;

        for (int side = 0; side <= 1; side++) {

            List<Brigade> brigades = sideBrigades.get(side);

            for (Brigade brigade : brigades) {
                battalionCount += brigade.getBattalions().size();
            }
        }

        int sizeX = 0;
        int sizeY = 0;

        if (0 <= battalionCount && battalionCount <= 360) {
            sizeX = 45;
            sizeY = 40;
        } else if (361 <= battalionCount && battalionCount <= 600) {
            sizeX = 50;
            sizeY = 45;
        } else if (601 <= battalionCount && battalionCount <= 720) {
            sizeX = 60;
            sizeY = 50;
        } else if (721 <= battalionCount) {
            sizeX = 65;
            sizeY = 55;
        }

        FieldBattleMap fbMap = new FieldBattleMap(sizeX, sizeY);

        return fbMap;
    }

}
