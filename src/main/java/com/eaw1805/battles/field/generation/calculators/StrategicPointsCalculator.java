package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.battles.field.utils.FieldBattleCollectionUtils;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates where strategic points should go.
 *
 * @author fragkakis
 */
public class StrategicPointsCalculator {

    private static final int STRATEGIC_POINT_MINIMUM_DISTANCE = 5;
    private final FieldBattleMap fbMap;
    private final Map<Nation, FieldBattleSetupArea> setupAreas;

    public StrategicPointsCalculator(FieldBattleMap fbMap,
                                     Map<Nation, FieldBattleSetupArea> setupAreas) {
        this.fbMap = fbMap;
        this.setupAreas = setupAreas;
    }

    public void addStrategicPoints() {
        for (Nation nation : setupAreas.keySet()) {
            FieldBattleSetupArea setupArea = setupAreas.get(nation);
            // road exit-points
            addRoadExitPoints(nation, setupArea);
            //strategic buildings
            addStrategicBuilding(nation, setupArea);
            //strategic peak
            addStrategicPeak(nation, setupArea);
            //strategic fortress
            addStrategicPointIfThereIsFortress(nation, setupArea);
        }

        addNeutralStrategicPoints();
    }

	private void addRoadExitPoints(Nation nation, FieldBattleSetupArea setupArea) {

        int startX = setupArea.getStartX();
        int endX = setupArea.getEndX();

        int roadY = setupArea.isTop() ? 0 : fbMap.getSizeY() - 1;

        for (int x = startX; x <= endX; x++) {
            FieldBattleSector sector = fbMap.getFieldBattleSector(x, roadY);
            if (sector.isRoad()) {
                setStrategicPoint(sector, nation);
            }
        }
    }

    private void addStrategicBuilding(Nation nation, FieldBattleSetupArea setupArea) {

        int startX = setupArea.getStartX();
        int endX = setupArea.getEndX();
        int startY = setupArea.getStartY();
        int endY = setupArea.getEndY();

        List<FieldBattleSector> setupAreaBuildings = new ArrayList<FieldBattleSector>();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.hasSectorChateau()
                        || sector.hasSectorTown()
                        || sector.hasSectorVillage()) {
                    setupAreaBuildings.add(sector);
                }
            }
        }
        if (!setupAreaBuildings.isEmpty()) {

            FieldBattleSector buildingStrategicPoint = tryToPickStrategicPointWithMinDistance(setupAreaBuildings, STRATEGIC_POINT_MINIMUM_DISTANCE);

            setStrategicPoint(buildingStrategicPoint, nation);
        }
    }
    
    private void addStrategicPointIfThereIsFortress(Nation nation, FieldBattleSetupArea setupArea) {
		if(setupArea.getStartWallX() != -1) {
			
			int fortInteriorStartX = setupArea.getStartWallX() + 1;
			int fortInteriorEndX = setupArea.getEndWallX() - 1;
			int fortInteriorStartY;
			int fortInteriorEndY;
			
			if(setupArea.isTop()) {
				fortInteriorStartY = 0;
				fortInteriorEndY = setupArea.getWallY()-1;
			} else {
				fortInteriorStartY = setupArea.getWallY()+1;
				fortInteriorEndY = fbMap.getSizeY()-1;
			}
			
			
			Set<FieldBattleSector> fortInterior = MapUtils.findSectorsInArea(
					fbMap, fortInteriorStartX, fortInteriorEndX,
					fortInteriorStartY, fortInteriorEndY, false);
			
			FieldBattleSector fortStrategicPoint = tryToPickStrategicPointWithMinDistance(fortInterior, STRATEGIC_POINT_MINIMUM_DISTANCE);

            setStrategicPoint(fortStrategicPoint, nation);
		}
		
	}

    private FieldBattleSector tryToPickStrategicPointWithMinDistance(Collection<FieldBattleSector> strategicPointsCandidates, int minimumDistance) {

        Set<FieldBattleSector> strategicPointsSoFar = MapUtils.findStrategicPoints(fbMap, new HashSet<Nation>());

        FieldBattleSector buildingStrategicPoint = null;
        Set<FieldBattleSector> candidatesRespectingMinDistance = MapUtils.filterSectorsByDistance(strategicPointsCandidates, strategicPointsSoFar, minimumDistance);
        if (!candidatesRespectingMinDistance.isEmpty()) {
            buildingStrategicPoint = FieldBattleCollectionUtils.getRandom(candidatesRespectingMinDistance);
        } else {
            buildingStrategicPoint = FieldBattleCollectionUtils.getRandom(strategicPointsCandidates);
        }
        return buildingStrategicPoint;
    }

    private void addStrategicPeak(Nation nation, FieldBattleSetupArea setupArea) {
        int startX = setupArea.getStartX();
        int endX = setupArea.getEndX();
        int startY = setupArea.getStartY();
        int endY = setupArea.getEndY();

        List<FieldBattleSector> peaksAltitude3 = new ArrayList<FieldBattleSector>();
        List<FieldBattleSector> peaksAltitude4 = new ArrayList<FieldBattleSector>();
        List<FieldBattleSector> peaksAltitude2 = new ArrayList<FieldBattleSector>();
        List<FieldBattleSector> peaksAltitude1 = new ArrayList<FieldBattleSector>();

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {

                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                if (sector.getAltitude() == 4) {
                    peaksAltitude4.add(sector);
                } else if (sector.getAltitude() == 3) {
                    peaksAltitude3.add(sector);
                } else if (sector.getAltitude() == 2) {
                    peaksAltitude2.add(sector);
                } else if (sector.getAltitude() == 1) {
                    peaksAltitude1.add(sector);
                }
            }
        }

        FieldBattleSector strategicPeakSector = !peaksAltitude4.isEmpty() ? tryToPickStrategicPointWithMinDistance(peaksAltitude4, STRATEGIC_POINT_MINIMUM_DISTANCE) :
                !peaksAltitude3.isEmpty() ? tryToPickStrategicPointWithMinDistance(peaksAltitude3, STRATEGIC_POINT_MINIMUM_DISTANCE) :
                        !peaksAltitude2.isEmpty() ? tryToPickStrategicPointWithMinDistance(peaksAltitude2, STRATEGIC_POINT_MINIMUM_DISTANCE) :
                                tryToPickStrategicPointWithMinDistance(peaksAltitude1, STRATEGIC_POINT_MINIMUM_DISTANCE);

        setStrategicPoint(strategicPeakSector, nation);

    }

    private void addNeutralStrategicPoints() {
        int neutralStrategicPointsCount = MathUtils.generateRandomIntInRange(0, 2);

        for (int i = 0; i <= neutralStrategicPointsCount; i++) {
            int strategicPointX = MathUtils.generateRandomIntInRange(0, fbMap.getSizeX() - 1);

            int heightDistance = MathUtils.generateRandomIntInRange(0, 4);
            int strategicPointY = MathUtils.generateRandomIntInRange(fbMap.getSizeY() / 2 - heightDistance,
                    fbMap.getSizeY() / 2 + heightDistance);

            FieldBattleSector neutralStrategicSector = fbMap.getFieldBattleSector(strategicPointX, strategicPointY);

            if (!neutralStrategicSector.isLake() && !neutralStrategicSector.isMajorRiver() && !neutralStrategicSector.isMinorRiver()) {
                setStrategicPoint(neutralStrategicSector, null);
            }
        }
    }

    private void setStrategicPoint(FieldBattleSector sector, Nation nation) {
        sector.setStrategicPoint(true);
        sector.setNation(nation);
        sector.setCurrentHolder(nation);
    }

}
