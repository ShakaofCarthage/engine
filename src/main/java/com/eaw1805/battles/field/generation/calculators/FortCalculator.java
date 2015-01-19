package com.eaw1805.battles.field.generation.calculators;

import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.FieldBattleSetupArea;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adds a fort to a field battle map.
 *
 * @author fragkakis
 */
public class FortCalculator {


    private FieldBattleMap fbMap;
    private Map<Nation, FieldBattleSetupArea> setupAreas;

    public FortCalculator(FieldBattleMap fbMap, Map<Nation, FieldBattleSetupArea> setupAreas) {

        this.fbMap = fbMap;
        this.setupAreas = setupAreas;
    }


    public void addFort(Nation nation, int fortSizeX, int fortSizeY, int hitPoints) {
        FieldBattleSetupArea setupArea = setupAreas.get(nation);

        if (setupArea.getStartY() == 0) {
            addFortToTopSetupArea(setupArea, fortSizeX, fortSizeY, hitPoints);
        } else {
            addFortToBottomSetupArea(setupArea, fortSizeX, fortSizeY, hitPoints);
        }
    }

    private void addFortToTopSetupArea(FieldBattleSetupArea setupArea,
                                       int fortSizeX, int fortSizeY, int hitPoints) {

        int fortLeftMarginX = (setupArea.getEndX() - setupArea.getStartX() - fortSizeX) / 2;

        int fortStartingX = setupArea.getStartX() + fortLeftMarginX;
        int fortEndingX = fortStartingX + fortSizeX;
        int fortEndingY = fortSizeY - 1;

        Set<FieldBattleSector> wallSectors = new HashSet<FieldBattleSector>();
        Set<FieldBattleSector> fortificationInternalSectors = new HashSet<FieldBattleSector>();

        for (int x = fortStartingX; x <= fortEndingX; x++) {
            for (int y = 0; y <= fortEndingY; y++) {
                FieldBattleSector fortificationSector = fbMap.getFieldBattleSector(x, y);
                if (x == fortStartingX || x == fortEndingX || y == fortEndingY) {
                    wallSectors.add(fortificationSector);
                } else {
                    fortificationInternalSectors.add(fortificationSector);
                }
            }
        }

        for (FieldBattleSector sector : wallSectors) {
            sector.setWall(hitPoints);
        }

        for (FieldBattleSector sector : fortificationInternalSectors) {
            sector.setFortificationInterior(true);
        }

        setupArea.setStartWallX(fortStartingX);
        setupArea.setEndWallX(fortEndingX);
        setupArea.setWallY(fortEndingY);
    }

    private void addFortToBottomSetupArea(FieldBattleSetupArea setupArea,
                                          int fortSizeX, int fortSizeY, int hitPoints) {

        int mapSizeY = setupArea.getFbt().getSizeY();

        int fortLeftMarginX = (setupArea.getEndX() - setupArea.getStartX() - fortSizeX) / 2;

        int fortStartingX = setupArea.getStartX() + fortLeftMarginX;
        int fortEndingX = fortStartingX + fortSizeX;
        int fortStartingY = mapSizeY - fortSizeY - 1;

        Set<FieldBattleSector> wallSectors = new HashSet<FieldBattleSector>();
        Set<FieldBattleSector> fortificationInternalSectors = new HashSet<FieldBattleSector>();

        for (int x = fortStartingX; x <= fortEndingX; x++) {
            for (int y = fortStartingY; y <= mapSizeY - 1; y++) {
                FieldBattleSector fortificationSector = fbMap.getFieldBattleSector(x, y);
                if (x == fortStartingX || x == fortEndingX || y == fortStartingY) {
                    wallSectors.add(fortificationSector);
                } else {
                    fortificationInternalSectors.add(fortificationSector);
                }
            }
        }

        for (FieldBattleSector sector : wallSectors) {
            sector.setWall(hitPoints);
        }

        for (FieldBattleSector sector : fortificationInternalSectors) {
            sector.setFortificationInterior(true);
        }

        setupArea.setStartWallX(fortStartingX);
        setupArea.setEndWallX(fortEndingX);
        setupArea.setWallY(fortStartingY);

    }

}
