package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.BattleField;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a helper class that calculates the setup areas of a field battle map.
 *
 * @author fragkakis
 */
public class SetupAreaCalculator {

    /**
     * The setup areas of the field battle by nation.
     */
    private final Map<Nation, FieldBattleSetupArea> setupAreas = new HashMap<Nation, FieldBattleSetupArea>();

    public Map<Nation, FieldBattleSetupArea> computeSetupAreas(BattleField battleField, FieldBattleMap fbMap) {

        computeSetupAreasForSide(battleField, fbMap, 0);
        computeSetupAreasForSide(battleField, fbMap, 1);

        return setupAreas;

    }

    private void computeSetupAreasForSide(BattleField battleField, FieldBattleMap fbMap, int side) {
        int numberOfSideNations = battleField.getSide(side).size();

        int sizeX = 0;
        int sizeY = 10;

        switch (numberOfSideNations) {
            case 1:
                sizeX = 20;
                break;
            case 2:
                sizeX = 16;
                break;
            default:
                // practically this corresponds to 3 nations. Only give size 16 if there is space!
                if (fbMap.getSizeX() >= 16 * 3) {
                    sizeX = 16;
                } else {
                    sizeX = 15;
                }
        }

        int marginLeftX = (fbMap.getSizeX() - numberOfSideNations * sizeX) / 2; // needs to be integer

        for (int i = 0; i < numberOfSideNations; i++) {
            Nation nation = battleField.getSide(side).get(i);
            int startX = marginLeftX + i * sizeX; //
            int endX = startX + sizeX - 1; // -1 because startX is inclusive
            int startY = side == 0 ? 0 : fbMap.getSizeY() - sizeY;    // -1 because Y is 0-based
            int endY = startY + sizeY - 1;    // -1 because startY is inclusive

            FieldBattleSetupArea fbsa = new FieldBattleSetupArea(fbMap, nation, startX, endX, startY, endY);

            // Mark the sectors appropriately
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {

                    FieldBattleSector fbs = fbMap.getFieldBattleSector(x, y);
                    fbs.setNation(nation);
                }
            }

            setupAreas.put(nation, fbsa);
        }
    }
}