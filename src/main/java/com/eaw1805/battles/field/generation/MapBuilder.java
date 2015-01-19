package com.eaw1805.battles.field.generation;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.generation.calculators.AltitudeCalculator;
import com.eaw1805.battles.field.generation.calculators.BushCalculator;
import com.eaw1805.battles.field.generation.calculators.ChateauCalculator;
import com.eaw1805.battles.field.generation.calculators.ForestCalculator;
import com.eaw1805.battles.field.generation.calculators.FortCalculator;
import com.eaw1805.battles.field.generation.calculators.LakeCalculator;
import com.eaw1805.battles.field.generation.calculators.MapDimensionsCalculator;
import com.eaw1805.battles.field.generation.calculators.RiverCalculator;
import com.eaw1805.battles.field.generation.calculators.RoadCalculator;
import com.eaw1805.battles.field.generation.calculators.SetupAreaCalculator;
import com.eaw1805.battles.field.generation.calculators.StrategicPointsCalculator;
import com.eaw1805.battles.field.generation.calculators.TownCalculator;
import com.eaw1805.battles.field.generation.calculators.VillageCalculator;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;
import com.eaw1805.data.model.map.ProductionSite;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the top-level class for creating maps for the field battle.
 *
 * @author fragkakis
 */
public class MapBuilder
        implements ProductionSiteConstants {

    /**
     * The BattleField where the battle will take place.
     */
    private final BattleField battleField;
    /**
     * The battalions of each side.
     */
    private final transient List<List<Brigade>> sideBrigades;

    public MapBuilder(BattleField battleField, List<List<Brigade>> sideBrigades) {

        this.sideBrigades = sideBrigades;
        this.battleField = battleField;

    }

    public FieldBattleMap buildMap() {

        MapDimensionsCalculator emptyMapCalc = new MapDimensionsCalculator();
        FieldBattleMap fbMap = emptyMapCalc.createEmptyMap(sideBrigades);
        // Setup areas
        SetupAreaCalculator setupAreaCalc = new SetupAreaCalculator();
        Map<Nation, FieldBattleSetupArea> setupAreas = setupAreaCalc.computeSetupAreas(battleField, fbMap);
        // Fortification
        addFortifications(fbMap, setupAreas);
        // Rivers
        addRivers(fbMap);
        // Lakes
        addLakes(fbMap);
        // Roads
        addRoads(fbMap, setupAreas);
        // Chateaus, villages, towns
        addTowns(fbMap, setupAreas);
        addChateaus(fbMap, setupAreas);
        addVillages(fbMap, setupAreas);
        // forests
        addForestsAndBush(fbMap);
        // altitude
        addAltitude(fbMap, setupAreas);
        // strategic points
        addStrategicPoints(fbMap, setupAreas);

        return fbMap;
    }

    private void addFortifications(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {

        FortCalculator fortCalc = new FortCalculator(fbMap, setupAreas);
        Nation defendingNation = battleField.getField().getNation();

        ProductionSite productionSite = battleField.getField().getProductionSite();
        if (productionSite != null) {
            switch (productionSite.getId()) {
                case PS_BARRACKS_FS:
                    fortCalc.addFort(defendingNation, 4, 4, 600);
                    break;
                case PS_BARRACKS_FM:
                    fortCalc.addFort(defendingNation, 8, 4, 800);
                    break;
                case PS_BARRACKS_FL:
                    fortCalc.addFort(defendingNation, 8, 6, 1000);
                    break;
                case PS_BARRACKS_FH:
                    fortCalc.addFort(defendingNation, 10, 8, 1200);
                    break;
                default:
                    // do nothing
            }
        }
    }

    private void addForestsAndBush(FieldBattleMap fbMap) {
        ForestCalculator forestCalc = new ForestCalculator(fbMap);
        BushCalculator bushCalc = new BushCalculator(fbMap);

        Set<FieldBattleSector> emptySectors = MapUtils.getAllEmptySectors(fbMap);
        int emptySectorsNumber = emptySectors.size();
        int targetBushSectors = 0;

        switch (battleField.getField().getTerrain().getId()) {

            case TerrainConstants.TERRAIN_B:
                // Plains
                // Forest: 15%, 3-12
                forestCalc.addForestsByPercentage(15);
                // Bush: 25%, 3-12
                targetBushSectors = 25 * emptySectorsNumber / 100;
                bushCalc.addBushBySectorsNumber(targetBushSectors, 3, 12);
                break;
            case TerrainConstants.TERRAIN_H:
                // Hills
                // Forest: 15%, 5-15
                forestCalc.addForestsByPercentage(15);
                // Bush: 25%, 3-12
                targetBushSectors = 25 * emptySectorsNumber / 100;
                bushCalc.addBushBySectorsNumber(targetBushSectors, 3, 12);
                break;
            case TerrainConstants.TERRAIN_G:
                // Maintains
                // Forest: 20%, 5-15
                forestCalc.addForestsByPercentage(20);
                // Bush: 60%, 3-12
                targetBushSectors = 60 * emptySectorsNumber / 100;
                bushCalc.addBushBySectorsNumber(targetBushSectors, 2, 5);
                break;
            case TerrainConstants.TERRAIN_S:
                // Swamp
                // Forest: 40%, 5-15
                forestCalc.addForestsByPercentage(40);
                // Bush: 40%, 2-5
                targetBushSectors = 40 * emptySectorsNumber / 100;
                bushCalc.addBushBySectorsNumber(targetBushSectors, 2, 5);
                break;
            case TerrainConstants.TERRAIN_W:
            case TerrainConstants.TERRAIN_J:
                // Forest (Woods), Jungle:
            	forestCalc.addForestsByPercentage(40);
                // Bush: 20%, 2-5
                targetBushSectors = 20 * emptySectorsNumber / 100;
                bushCalc.addBushBySectorsNumber(targetBushSectors, 2, 5);
                break;
            default:
            case TerrainConstants.TERRAIN_D:
                // Desert: 0%
                break;

        }
    }

    private void addLakes(FieldBattleMap fbMap) {

        LakeCalculator lakeCalc = new LakeCalculator(fbMap);
        switch (battleField.getField().getTerrain().getId()) {

            case TerrainConstants.TERRAIN_S:
                // Swamp: 40% for first, 20% for second
                if (Math.random() * 100 <= 40) {
                    lakeCalc.addLake();
                    if (Math.random() * 100 <= 20) {
                        lakeCalc.addLake();
                    }
                }
                break;
            case TerrainConstants.TERRAIN_D:
                // Desert: no river
                break;
            default:
                // Any: 5%
                if (Math.random() * 100 <= 5) {
                    lakeCalc.addLake();
                }

        }

        // TODO: Adjacent to lake tile: 60%

    }

    private void addRivers(FieldBattleMap fbMap) {

        boolean addedMajorRiver = addMajorRiverIfAppropriate(fbMap);
        if (!addedMajorRiver) {
            addMinorRiverIfAppropriate(fbMap);
        }
    }

    private boolean addMajorRiverIfAppropriate(FieldBattleMap fbMap) {

        boolean addMajorRiver = false;
        RiverCalculator riverCalculator = new RiverCalculator();

        if (battleField.getField().isRiverEast()
                || battleField.getField().isRiverNorth()
                || battleField.getField().isRiverSouth()
                || battleField.getField().isRiverWest()) {
            // Major River: 80%
            if (Math.random() * 100 <= 80) {
                addMajorRiver = true;
            }
        }

        if (!addMajorRiver) {
            // Major River: 2%
            if (Math.random() * 100 <= 2) {
                addMajorRiver = true;
            }
        }

        if (addMajorRiver) {
            riverCalculator.addMajorRiver(fbMap);
        }
        return addMajorRiver;
    }

    private void addMinorRiverIfAppropriate(FieldBattleMap fbMap) {

        RiverCalculator riverCalculator = new RiverCalculator();
        switch (battleField.getField().getTerrain().getId()) {

            case TerrainConstants.TERRAIN_G:
                // Mountain: 20%
                if (Math.random() * 100 <= 20) {
                    riverCalculator.addMinorRiver(fbMap);
                }
                break;
            case TerrainConstants.TERRAIN_S:
                // Swamp: 90% for first, 50% for second
                if (Math.random() * 100 <= 90) {
                    riverCalculator.addMinorRiver(fbMap);
                    // TODO: No second rivers for now
//					if(Math.random() * 100 <= 50) {
//						riverCalculator.addMinorRiver(fbMap);
//					}
                }
                break;
            case TerrainConstants.TERRAIN_D:
                // Desert: no river
                break;
            default:
                // Any: 10%
                if (Math.random() * 100 <= 10) {
                    riverCalculator.addMinorRiver(fbMap);
                }

        }
    }

    private void addRoads(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {

        RoadCalculator roadCalculator = new RoadCalculator(fbMap, setupAreas, battleField);

        int roadProbability = MathUtils.generateRandomIntInRange(1, 100);
        int populationDensity = battleField.getField().getTerrain().getMaxDensity();

        switch (populationDensity) {

            case 0:
                if (roadProbability <= 25) {
                    roadCalculator.addRoads();
                }
                break;
            case 1:
                if (roadProbability <= 50) {
                    roadCalculator.addRoads();
                }
                break;
            case 2:
                if (roadProbability <= 25) {
                    roadCalculator.addRoads();
                }
                break;
            case 3:
                if (roadProbability <= 95) {
                    roadCalculator.addRoads();
                }
                break;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                roadCalculator.addRoads();
        }
    }

    private void addChateaus(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {

        int populationDensity = battleField.getField().getTerrain().getMaxDensity();

        ChateauCalculator chateauCalc = new ChateauCalculator();

        switch (populationDensity) {

            case 1:
            case 2:
                chateauCalc.addChateaus(fbMap, MathUtils.generateRandomIntInRange(2, 4));
                break;
            case 3:
            case 5:
                chateauCalc.addChateaus(fbMap, MathUtils.generateRandomIntInRange(1, 3));
                break;
            case 6:
            case 7:
                chateauCalc.addChateaus(fbMap, MathUtils.generateRandomIntInRange(0, 2));
                break;
            case 8:
            case 9:
                chateauCalc.addChateaus(fbMap, MathUtils.generateRandomIntInRange(0, 1));
                break;
            default:
            case 0:
                chateauCalc.addChateaus(fbMap, MathUtils.generateRandomIntInRange(1, 3));
                break;
        }

    }

    private void addVillages(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {

        int populationDensity = battleField.getField().getTerrain().getMaxDensity();

        VillageCalculator villageCalc = new VillageCalculator(fbMap, setupAreas, battleField);

        switch (populationDensity) {

            case 1:
            case 2:
                villageCalc.addSetupAreaVillages();
                break;
            case 3:
            case 5:
                villageCalc.addSetupAreaVillages();
                villageCalc.addOtherVillages(MathUtils.generateRandomIntInRange(1, 4));
                break;
            case 6:
            case 7:
                villageCalc.addSetupAreaVillages();
                villageCalc.addOtherVillages(MathUtils.generateRandomIntInRange(1, 3));
                break;
            case 8:
            case 9:
                villageCalc.addSetupAreaVillages();
                villageCalc.addOtherVillages(MathUtils.generateRandomIntInRange(1, 2));
                break;
            default:
            case 0:
                villageCalc.addSetupAreaVillages();
                break;
        }

    }

    private void addTowns(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {

        // TODO: Check if this is OK
        for (FieldBattleSetupArea setupArea : setupAreas.values()) {
            if (setupArea.getStartX() >= 0) {
                // there is fortress in the map, no towns
                return;
            }
        }

        int populationDensity = battleField.getField().getTerrain().getMaxDensity();

        TownCalculator townCalc = new TownCalculator(fbMap, battleField);

        switch (populationDensity) {

            case 6:
            case 7:
                townCalc.addTowns(MathUtils.generateRandomIntInRange(1, 2));
                break;
            case 8:
            case 9:
                townCalc.addTowns(MathUtils.generateRandomIntInRange(1, 2));
                break;
            default:
        }

    }

    private void addAltitude(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {
        AltitudeCalculator altitudeCalc = new AltitudeCalculator(fbMap, setupAreas, battleField);
        altitudeCalc.calculateAltitude();
    }

    private void addStrategicPoints(FieldBattleMap fbMap,
                                    Map<Nation, FieldBattleSetupArea> setupAreas) {
        StrategicPointsCalculator strategicPointsCalc = new StrategicPointsCalculator(fbMap, setupAreas);
        strategicPointsCalc.addStrategicPoints();

    }

}
