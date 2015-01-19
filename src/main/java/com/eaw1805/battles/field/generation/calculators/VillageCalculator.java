package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;

import java.util.Map;
import java.util.Set;

/**
 * Helper class that adds villages to a field battle map.
 *
 * @author fragkakis
 */
public class VillageCalculator {

    private FieldBattleMap fbMap;
    private Map<Nation, FieldBattleSetupArea> setupAreas;
    private BattleField battleField;
    private static final int DEFAULT_VILLAGE_HIT_POINTS = 600;

    public VillageCalculator(FieldBattleMap fbMap,
                             Map<Nation, FieldBattleSetupArea> setupAreas,
                             BattleField battleField) {
        this.fbMap = fbMap;
        this.setupAreas = setupAreas;
        this.battleField = battleField;
    }

    /**
     * Adds the villages to appear in the setup areas.
     *
     * @param fbMap the map
     */
    public void addSetupAreaVillages() {

        // side 0
        addSetulAreaVillageToSide(0);
        addSetulAreaVillageToSide(1);

    }

    private void addSetulAreaVillageToSide(int side) {

        int sideSize0 = battleField.getSide(side).size();
        Nation nationFromSide0 = battleField.getSide(side).get(MathUtils.generateRandomIntInRange(1, sideSize0) - 1);

        FieldBattleSetupArea setupAreaFromSide0 = setupAreas.get(nationFromSide0);

        ClusterCalculator clusterCalc = new ClusterCalculator();
        Set<FieldBattleSector> setupAreaVillage0 = clusterCalc
                .findRandomClusterInRegion(fbMap,
                        MathUtils.generateRandomIntInRange(4, 7),
                        setupAreaFromSide0.getStartX(),
                        setupAreaFromSide0.getEndX(),
                        setupAreaFromSide0.getStartY(),
                        setupAreaFromSide0.getEndY(),
                        ClusterTypeEnum.VILLAGE);

        for (FieldBattleSector sector : setupAreaVillage0) {
            sector.setVillage(DEFAULT_VILLAGE_HIT_POINTS);
        }
    }

    public void addOtherVillages(int numberOfVillages) {

        for (int i = 0; i < numberOfVillages; i++) {
            addVillage();
        }
    }

    /**
     * @param fbMap
     */
    private void addVillage() {

        int defendingSide = getDefendingSide();

        // defending side has 50% more chance that a village will appear in or closer to its setup area.
        // Over all: defending side: 67% chance, attacking side 33%
        boolean placeInDefendingSide = MathUtils.generateRandomIntInRange(1, 100) <= 67;

        int sideToPlaceTheVillage = 0;

        if ((placeInDefendingSide && defendingSide == 0) || (!placeInDefendingSide) && defendingSide == 1) {
            sideToPlaceTheVillage = 0;
        } else {
            sideToPlaceTheVillage = 1;
        }

        ClusterCalculator clusterCalc = new ClusterCalculator();

        Set<FieldBattleSector> sectors = null;

        if (sideToPlaceTheVillage == 0) {
            sectors = clusterCalc.findRandomClusterInRegion(fbMap, MathUtils.generateRandomIntInRange(4, 7), 0, fbMap.getSizeX() / 2, 0, fbMap.getSizeY() / 2, ClusterTypeEnum.VILLAGE);
        } else {
            sectors = clusterCalc.findRandomClusterInRegion(fbMap, MathUtils.generateRandomIntInRange(4, 7), fbMap.getSizeX() / 2, fbMap.getSizeX() - 1, fbMap.getSizeY() / 2, fbMap.getSizeY() - 1, ClusterTypeEnum.VILLAGE);
        }

        for (FieldBattleSector sector : sectors) {
            sector.setVillage(DEFAULT_VILLAGE_HIT_POINTS);
        }

    }

    private int getDefendingSide() {
        // TODO Auto-generated method stub
        return MathUtils.generateRandomIntInRange(0, 1);
    }
}
