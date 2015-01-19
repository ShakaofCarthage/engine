package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper class that adds towns to a field battle map.
 *
 * @author fragkakis
 */
public class TownCalculator {

    private FieldBattleMap fbMap;
    private BattleField battleField;
    private int defendingSide;
    private List<FieldBattleSector> roadSectorsOfSetupAreasOfSide0;
    private List<FieldBattleSector> roadSectorsOfSetupAreasOfSide1;
    private static final int DEFAULT_TOWN_HIT_POINTS = 800;

    /**
     * Constructor
     *
     * @param fbMap       the field battle map
     * @param battleField the battlefield
     */
    public TownCalculator(FieldBattleMap fbMap, BattleField battleField) {
        this.fbMap = fbMap;
        this.battleField = battleField;

        defendingSide = getDefendingSide();

        roadSectorsOfSetupAreasOfSide0 = findRoadSectorsInSetupAreaForSide(0);
        roadSectorsOfSetupAreasOfSide1 = findRoadSectorsInSetupAreaForSide(1);
    }

    private List<FieldBattleSector> findRoadSectorsInSetupAreaForSide(int side) {

        List<FieldBattleSector> setupAreaRoadSectors = new ArrayList<FieldBattleSector>();

        for (int x = 0; x < fbMap.getSizeX(); x++) {
            for (int y = 0; y < fbMap.getSizeY(); y++) {

                FieldBattleSector sector = fbMap.getSectors()[x][y];
                if (sector.isRoad() &&
                        (
                                (side == 0 && 3 < sector.getY() && sector.getY() <= 10)
                                        ||
                                        (side == 1 && fbMap.getSizeY() - 1 - 10 < sector.getY() && sector.getY() < fbMap.getSizeY() - 1 - 3)
                        )
                        ) {
                    setupAreaRoadSectors.add(sector);
                }
            }
        }
        return setupAreaRoadSectors;
    }

    /**
     * Adds the specified number of towns to the terrain.
     *
     * @param fbMap         the map
     * @param numberOfTowns the number of towns
     */
    public void addTowns(int numberOfTowns) {

        switch (numberOfTowns) {
            case 1:
                addTownToRandomSide();
                break;
            case 2:
            default:
                addTownToSide(0);
                addTownToSide(1);
                break;
        }
    }

    private void addTownToRandomSide() {

        // defending side has 50% more chance that a town will appear in or closer to its setup area.
        // Over all: defending side: 67% chance, attacking side 33%
        boolean placeInDefendingSide = MathUtils.generateRandomIntInRange(1, 100) <= 67;

        int sideToPlaceTheTown = 0;

        if ((placeInDefendingSide && defendingSide == 0) || (!placeInDefendingSide) && defendingSide == 1) {
            sideToPlaceTheTown = 0;
        } else {
            sideToPlaceTheTown = 1;
        }

        Set<FieldBattleSector> townSectors;
        townSectors = addTownToSide(sideToPlaceTheTown);

        roadSectorsOfSetupAreasOfSide0.removeAll(townSectors);
        roadSectorsOfSetupAreasOfSide1.removeAll(townSectors);

    }

    private Set<FieldBattleSector> addTownToSide(int sideToPlaceTheTown) {
        Set<FieldBattleSector> townSectors;
        // 50% the town is a pararellogram
        if (MathUtils.generateRandomIntInRange(1, 100) <= 50) {
            townSectors = addSquareTown(sideToPlaceTheTown);
        } else {
            townSectors = addRandomTown(sideToPlaceTheTown);
        }

        for (FieldBattleSector sector : townSectors) {
            sector.setTown(DEFAULT_TOWN_HIT_POINTS);
        }

        return townSectors;
    }

    private Set<FieldBattleSector> addSquareTown(int side) {

        int townSizeX = 0;
        int townSizeY = 0;

        switch (battleField.getField().getPopulation()) {
            case 6:
                townSizeX = 5;
                townSizeY = 4;
                break;
            case 7:
                townSizeX = 6;
                townSizeY = 4;
                break;
            case 8:
                townSizeX = 7;
                townSizeY = 4;
                break;
            default:
            case 9:
                townSizeX = 6;
                townSizeY = 5;
                break;
        }

        ClusterCalculator calc = new ClusterCalculator();

        Set<FieldBattleSector> town = null;

        boolean townAdded = false;
        while (!townAdded) {

            if (side == 0) {
                town = calc.findSquareTownClusterInArea(fbMap, townSizeX, townSizeY, 0, fbMap.getSizeX() - 1, 0, 12);
            } else {
                town = calc.findSquareTownClusterInArea(fbMap, townSizeX, townSizeY, 0, fbMap.getSizeX() - 1, fbMap.getSizeY() - 1 - 12, fbMap.getSizeY() - 1);
            }

            townAdded = true;

        }

        return town;

    }

    /**
     * Adds a town to the terrain.
     *
     * @param fbMap the map
     */
    private Set<FieldBattleSector> addRandomTown(int side) {

        FieldBattleSector townCenter = findPossibleTownCenter(side);

        int townSize = calculateTownSize();
        ClusterCalculator calc = new ClusterCalculator();
        return calc.findRandomTownCluster(townCenter, townSize);
    }

    private int calculateTownSize() {

        int townSize = 0;

        switch (battleField.getField().getPopulation()) {
            case 6:
                townSize = MathUtils.generateRandomIntInRange(12, 16);
                break;
            case 7:
                townSize = MathUtils.generateRandomIntInRange(16, 20);
                break;
            case 8:
                townSize = MathUtils.generateRandomIntInRange(20, 24);
                break;
            default:
            case 9:
                townSize = MathUtils.generateRandomIntInRange(24, 30);

        }
        return townSize;
    }


    /**
     * Mock method to get defending side.
     *
     * @return
     */
    private int getDefendingSide() {
        // TODO Auto-generated method stub
        return MathUtils.generateRandomIntInRange(0, 1);
    }

    private FieldBattleSector findPossibleTownCenter(int side) {
        FieldBattleSector townCenter = null;

        if (side == 0) {
            townCenter = roadSectorsOfSetupAreasOfSide0.get(MathUtils.generateRandomIntInRange(0, roadSectorsOfSetupAreasOfSide0.size() - 1));
        } else {
            townCenter = roadSectorsOfSetupAreasOfSide1.get(MathUtils.generateRandomIntInRange(0, roadSectorsOfSetupAreasOfSide1.size() - 1));
        }
        return townCenter;
    }
}
