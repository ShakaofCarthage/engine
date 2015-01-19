package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.BasePathGenerator;
import com.eaw1805.battles.field.generation.RoadGenerator;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;

import java.util.List;
import java.util.Map;

public class RoadCalculator {

    private final FieldBattleMap fbMap;
    private final Map<Nation, FieldBattleSetupArea> setupAreas;
    private final BattleField battleField;
    public static final int DEFAULT_BRIDGE_HIT_POINTS = 600;

    public RoadCalculator(FieldBattleMap fbMap,
                          Map<Nation, FieldBattleSetupArea> setupAreas,
                          BattleField battleField) {
        this.fbMap = fbMap;
        this.setupAreas = setupAreas;
        this.battleField = battleField;
    }

    public void addRoads() {

        int sideCount0 = battleField.getSide(0).size();
        int sideCount1 = battleField.getSide(1).size();

        int strongerSide = sideCount0 > sideCount1 ? 0 : 1;
        int weakerSide = sideCount0 > sideCount1 ? 1 : 0;

        int strongerSideCount = battleField.getSide(strongerSide).size();
        int weakerSideCount = battleField.getSide(weakerSide).size();

        // if 2 sides have equal nation count, shuffle who is the stronger and the waker
        if (strongerSideCount == weakerSideCount) {
            if (MathUtils.generateRandomIntInRange(1, 100) < 50) {
                strongerSide = MathUtils.generateRandomIntInRange(1, 2) % 2;
                weakerSide = strongerSide == 0 ? 1 : 0;
            }
        }

        int probability = MathUtils.generateRandomIntInRange(1, 100);

        // 1 vs 1
        if (weakerSideCount == 1 && strongerSideCount == 1) {
            Nation nation1 = battleField.getSide(weakerSide).get(0);
            Nation nation2 = battleField.getSide(strongerSide).get(0);

            drawRoadBetweenNations(nation1, nation2);

            // 1 vs 2
        } else if (weakerSideCount == 1 && strongerSideCount == 2) {

            Nation nation1 = battleField.getSide(weakerSide).get(0);

            Nation nation2 = battleField.getSide(strongerSide).get(0);
            Nation nation3 = battleField.getSide(strongerSide).get(1);

            if (probability <= 75) {

                Nation strongerSideNation = MathUtils.generateRandomIntInRange(1, 100) <= 50 ? nation2 : nation3;
                drawRoadBetweenNations(nation1, strongerSideNation);

            } else {

                Nation strongerSideNation = MathUtils.generateRandomIntInRange(1, 100) <= 50 ? nation2 : nation3;
                Nation strongerSideOtherNation = strongerSideNation == nation2 ? nation3 : nation2;

                List<FieldBattleSector> path = drawRoadBetweenNations(nation1, strongerSideNation);

                drawRoadBetweenNationAndSector(strongerSideOtherNation, path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3)));

            }


            // 2 vs 2
        } else if (weakerSideCount == 2 && strongerSideCount == 2) {


            if (probability <= 50) {
                // 50%
                Nation nationA = battleField.getSide(weakerSide).get(0);
                Nation nationB = battleField.getSide(weakerSide).get(1);

                Nation nationC = battleField.getSide(strongerSide).get(0);
                Nation nationD = battleField.getSide(strongerSide).get(1);

                drawRoadBetweenNations(nationA, nationC);
                drawRoadBetweenNations(nationB, nationD);

            } else if (50 < probability && probability <= 75) {

                // 25%
                Nation nationA = MathUtils.generateRandomIntInRange(1, 100) <= 50 ?
                        battleField.getSide(strongerSide).get(0) : battleField.getSide(strongerSide).get(1);
                // get the opposite nation
                int nationAIndex = battleField.getSide(strongerSide).indexOf(nationA);
                Nation nationC = battleField.getSide(weakerSide).get(nationAIndex);

                List<FieldBattleSector> path = drawRoadBetweenNations(nationA, nationC);

                int nationDIndex = (nationAIndex + 1) % 2;

                Nation nationD = MathUtils.generateRandomIntInRange(1, 100) <= 50 ?
                        battleField.getSide(strongerSide).get(nationDIndex) :
                        battleField.getSide(weakerSide).get(nationDIndex);

                drawRoadBetweenNationAndSector(nationD, path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3)));

            } else {

                // 25%
                Nation nationA = battleField.getSide(strongerSide).get(0);
                Nation nationB = battleField.getSide(strongerSide).get(1);
                Nation nationC = battleField.getSide(weakerSide).get(0);
                Nation nationD = battleField.getSide(weakerSide).get(1);

                List<FieldBattleSector> path = drawRoadBetweenNations(nationA, nationD);

                FieldBattleSector midPathPoint = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));

                drawRoadBetweenNationAndSector(nationC, midPathPoint);
                drawRoadBetweenNationAndSector(nationB, midPathPoint);
            }


        } else if (weakerSideCount == 1 && strongerSideCount == 3) {

            Nation nationD = battleField.getSide(weakerSide).get(0);
            Nation nationB = battleField.getSide(strongerSide).get(1);
            int indexC = MathUtils.generateRandomIntInRange(1, 2) % 2 == 0 ? 0 : 2;
            int indexA = indexC == 0 ? 2 : 0;
            Nation nationC = battleField.getSide(strongerSide).get(indexC);
            Nation nationA = battleField.getSide(strongerSide).get(indexA);

            List<FieldBattleSector> path = drawRoadBetweenNations(nationB, nationD);

            if (probability <= 50) {
                // 50%
                // no other road
            } else {

                FieldBattleSector midPathPoint = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                drawRoadBetweenNationAndSector(nationC, midPathPoint);

                if (MathUtils.generateRandomIntInRange(1, 100) <= 50) {
                    FieldBattleSector midPathPoint2 = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                    drawRoadBetweenNationAndSector(nationA, midPathPoint2);
                }
            }

        } else if (weakerSideCount == 2 && strongerSideCount == 3) {

            Nation nationA = battleField.getSide(strongerSide).get(0);
            Nation nationB = battleField.getSide(strongerSide).get(1);
            Nation nationC = battleField.getSide(strongerSide).get(2);
            Nation nationD = battleField.getSide(weakerSide).get(0);
            Nation nationE = battleField.getSide(weakerSide).get(1);

            if (probability <= 50) {
                // 50%
                drawRoadBetweenNations(nationA, nationD);
                drawRoadBetweenNations(nationB, nationE);

            } else if (50 < probability && probability <= 75) {
                // 25%
                drawRoadBetweenNations(nationA, nationD);
                List<FieldBattleSector> path = drawRoadBetweenNations(nationB, nationE);

                FieldBattleSector midPathPoint = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                drawRoadBetweenNationAndSector(nationC, midPathPoint);


            } else {
                // 25%
                List<FieldBattleSector> path = drawRoadBetweenNations(nationB, nationE);
                FieldBattleSector midPathPointD = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                drawRoadBetweenNationAndSector(nationD, midPathPointD);
                FieldBattleSector midPathPointC = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                drawRoadBetweenNationAndSector(nationC, midPathPointC);
            }

        } else if (weakerSideCount == 3 && strongerSideCount == 3) {

            Nation nationA = battleField.getSide(strongerSide).get(0);
            Nation nationB = battleField.getSide(strongerSide).get(1);
            Nation nationC = battleField.getSide(strongerSide).get(2);
            Nation nationD = battleField.getSide(weakerSide).get(0);
            Nation nationE = battleField.getSide(weakerSide).get(1);
            Nation nationZ = battleField.getSide(weakerSide).get(2);


            if (probability <= 25) {
                // 25%
                drawRoadBetweenNations(nationB, nationE);

            } else if (25 < probability && probability <= 50) {
                // 25%
                drawRoadBetweenNations(nationB, nationE);
                drawRoadBetweenNations(nationC, nationZ);

            } else if (50 < probability && probability <= 75) {
                List<FieldBattleSector> path = drawRoadBetweenNations(nationB, nationE);
                FieldBattleSector midPathPoint = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                drawRoadBetweenNationAndSector(nationC, midPathPoint);

            } else if (75 < probability) {

                List<FieldBattleSector> path = drawRoadBetweenNations(nationB, nationE);
                FieldBattleSector midPathPoint = path.get(MathUtils.generateRandomIntInRange(path.size() / 3, path.size() * 2 / 3));
                drawRoadBetweenNationAndSector(nationC, midPathPoint);

                drawRoadBetweenNations(nationA, nationD);

            }
        }

    }

    private List<FieldBattleSector> drawRoadBetweenNations(Nation nation1, Nation nation2) {

        FieldBattleSetupArea setupArea1 = setupAreas.get(nation1);
        FieldBattleSetupArea setupArea2 = setupAreas.get(nation2);

        BasePathGenerator roadGenerator = new RoadGenerator(fbMap);

        FieldBattleSetupArea topSetupArea = setupArea1.isTop() ? setupArea1 : setupArea2;
        FieldBattleSetupArea bottomSetupArea = setupArea1.isTop() ? setupArea2 : setupArea1;

        // start sector
        FieldBattleSector startSector = findExitRoadSectorForSetupArea(topSetupArea);
        FieldBattleSector endSector = findExitRoadSectorForSetupArea(bottomSetupArea);

        // generate road
        List<FieldBattleSector> path = roadGenerator.generate(startSector, endSector);

        for (FieldBattleSector sector : path) {
            if (sector.isMinorRiver()) {
                sector.setBridge(DEFAULT_BRIDGE_HIT_POINTS);
            } else {
                sector.setRoad(true);
            }
        }

        return path;

    }


    private void drawRoadBetweenNationAndSector(Nation nation, FieldBattleSector endSector) {

        FieldBattleSetupArea setupArea = setupAreas.get(nation);

        BasePathGenerator roadGenerator = new RoadGenerator(fbMap);

        FieldBattleSector startSector = findExitRoadSectorForSetupArea(setupArea);

        List<FieldBattleSector> path = roadGenerator.generate(startSector, endSector);

        for (FieldBattleSector sector : path) {
            if (sector.isMinorRiver()) {
                sector.setBridge(DEFAULT_BRIDGE_HIT_POINTS);
            } else {
                sector.setRoad(true);
            }
        }

    }

    private FieldBattleSector findExitRoadSectorForSetupArea(
            FieldBattleSetupArea setupArea) {
        FieldBattleSector sector;
        int startSectorY = setupArea.isTop() ? 0 : fbMap.getSizeY() - 1;
        int startSectorMinX = -1;
        int startSectorMaxX = -1;

        if (setupArea.getStartWallX() >= 0) {
            // there is wall, start road outside it
            if (MathUtils.generateRandomIntInRange(1, 100) <= 50) {
                // left side of wall
                startSectorMinX = setupArea.getStartX() + 1;
                startSectorMaxX = setupArea.getStartWallX() - 1;
            } else {
                // right side of wall
                startSectorMinX = setupArea.getEndWallX() + 1;
                startSectorMaxX = setupArea.getEndX() - 1;
            }
        } else {
            // no wall
            startSectorMinX = setupArea.getStartX() + 3;
            startSectorMaxX = setupArea.getEndX() - 3;
        }

        sector = fbMap.getFieldBattleSector(MathUtils.generateRandomIntInRange(startSectorMinX, startSectorMaxX), startSectorY);
        return sector;
    }

}
