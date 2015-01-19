package com.eaw1805.battles.field.utils;

import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for the visualisation of the Field Battle map.
 * @author fragkakis
 *
 */
public class VisualisationUtils {

    public static void visualize(FieldBattleMap fbMap) {

        System.out.println(visualizeAsString(fbMap));
    }

    public static void visualize(FieldBattleMap fbMap,
                                 Set<FieldBattleSector> armyLocations) {

        System.out.println(visualizeAsString(fbMap, armyLocations));
    }

    @SuppressWarnings("unchecked")
    public static String visualizeAsString(FieldBattleMap fbMap) {
        return visualizeAsString(fbMap, Collections.EMPTY_SET);
    }

    public static String visualizeAsString(FieldBattleMap fbMap,
                                           Set<FieldBattleSector> armyLocations) {
        StringBuilder sb = new StringBuilder();

        appendMapData(fbMap, sb);

        // upper border
        for (int x = 0; x < fbMap.getSizeX() * 2 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");

        // content
        for (int y = 0; y < fbMap.getSizeY(); y++) {
            sb.append("+");
            for (int x = 0; x < fbMap.getSizeX(); x++) {
                if (x != 0) {
                    sb.append("|");
                }
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                sb.append(getSectorContent(sector, armyLocations, false));
            }
            sb.append("+");
            sb.append("\n");
        }

        // lower border
        for (int x = 0; x < fbMap.getSizeX() * 2 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static void appendMapData(FieldBattleMap fbMap, StringBuilder sb) {

        Set<FieldBattleSector> sectors = MapUtils.getAllSectors(fbMap);

        boolean river = false;
        boolean fortification = false;
        boolean lake = false;
        boolean road = false;
        boolean town = false;
        boolean village = false;
        boolean chateau = false;
        
        int forestCount = 0;

        for (FieldBattleSector sector : sectors) {
            if (sector.isMinorRiver() || sector.isMajorRiver()) {
                river = true;
            }
            if (sector.isFortificationInterior() || sector.hasSectorWall()) {
                fortification = true;
            }
            if (sector.isLake()) {
                lake = true;
            }
            if (sector.isRoad()) {
                road = true;
            }
            if (sector.hasSectorTown()) {
                town = true;
            }
            if (sector.hasSectorVillage()) {
                village = true;
            }
            if (sector.hasSectorChateau()) {
                chateau = true;
            }
            if(sector.isForest()) {
            	forestCount++;
            }
        }

        sb.append("Map size: ");
        sb.append(fbMap.getSizeX());
        sb.append("x");
        sb.append(fbMap.getSizeY());
        sb.append("\n");
        sb.append("River: ");
        sb.append(river ? "Yes" : "No");
        sb.append("\n");
        sb.append("Fortification: ");
        sb.append(fortification ? "Yes" : "No");
        sb.append("\n");
        sb.append("Lake: ");
        sb.append(lake ? "Yes" : "No");
        sb.append("\n");
        sb.append("Road: ");
        sb.append(road ? "Yes" : "No");
        sb.append("\n");
        sb.append("Town: ");
        sb.append(town ? "Yes" : "No");
        sb.append("\n");
        sb.append("Village: ");
        sb.append(village ? "Yes" : "No");
        sb.append("\n");
        sb.append("Chateau: ");
        sb.append(chateau ? "Yes" : "No");
        int mapSize = fbMap.getSizeX()*fbMap.getSizeY();
        sb.append("Forest: ").append(forestCount).append("/").append(mapSize).append("(").append(forestCount*100/mapSize).append("%)");
        sb.append("\n");
        sb.append("\n");

    }

    public static void visualizeAltitude(FieldBattleMap fbMap) {

        System.out.println(visualizeAltitudeAsString(fbMap));
    }

    public static String visualizeAltitudeAsString(FieldBattleMap fbMap) {
        StringBuilder sb = new StringBuilder();

        appendMapData(fbMap, sb);

        // upper border
        for (int x = 0; x < fbMap.getSizeX() * 2 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");

        // content
        for (int y = 0; y < fbMap.getSizeY(); y++) {
            sb.append("+");
            for (int x = 0; x < fbMap.getSizeX(); x++) {
                if (x != 0) {
                    sb.append("|");
                }
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                sb.append(sector.getAltitude());
            }
            sb.append("+");
            sb.append("\n");
        }

        // lower border
        for (int x = 0; x < fbMap.getSizeX() * 2 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static void visualizeEverything(FieldBattleMap fbMap) {

        System.out.println(visualizeEverythingAsString(fbMap,
                Collections.EMPTY_SET));
    }

    public static String visualizeEverythingAsString(FieldBattleMap fbMap,
                                                     Set<FieldBattleSector> armyLocations) {

        StringBuilder sb = new StringBuilder();

        appendMapData(fbMap, sb);

        // Ys
        sb.append("    ");
        for (int x = 0; x < fbMap.getSizeX(); x++) {
            sb.append(" " + String.format("%1$2s", x) + " ");
        }
        sb.append("\n");

        // upper border
        sb.append("   ");
        for (int x = 0; x < fbMap.getSizeX() * 4 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");

        // content
        for (int y = 0; y < fbMap.getSizeY(); y++) {

            // content and altitude
            sb.append(String.format("%1$2s", y));
            sb.append(" +");
            for (int x = 0; x < fbMap.getSizeX(); x++) {
                if (x != 0) {
                    sb.append("|");
                }
                FieldBattleSector sector = fbMap.getFieldBattleSector(x, y);
                sb.append(getSectorContent(sector, armyLocations, false));
                sb.append(" ");
                sb.append(sector.getAltitude());
            }
            sb.append("+");
            sb.append("\n");

            sb.append("   ");
            // horizontal separator
            if (y != fbMap.getSizeY() - 1) {
                sb.append("+");
                for (int x = 0; x < fbMap.getSizeX(); x++) {
                    if (x != 0) {
                        sb.append("|");
                    }
                    sb.append("---");
                }
                sb.append("+");
                sb.append("\n");
            }
        }

        // lower border
        sb.append("");
        for (int x = 0; x < fbMap.getSizeX() * 4 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String getSectorContent(FieldBattleSector sector,
                                           Set<FieldBattleSector> armyLocations, boolean showSetupAreas) {
        String sectorContent = null;
        if (armyLocations.contains(sector)) {
            sectorContent = "A";
        } else if (sector.isStrategicPoint()) {
            sectorContent = "*";
        } else if (sector.hasSectorEntrenchment()) {
        	sectorContent = "E";
        } else if (sector.hasSectorChateau()) {
            sectorContent = "C";
        } else if (sector.hasSectorVillage()) {
            sectorContent = "V";
        } else if (sector.hasSectorTown()) {
            sectorContent = "T";
        } else if (sector.hasSectorBridge()) {
            sectorContent = "B";
        } else if (sector.hasSectorWall()) {
        	sectorContent = "W";
        } else if (sector.isMinorRiver() || sector.isMajorRiver()) {
            sectorContent = "R";
        } else if (sector.isLake()) {
            sectorContent = "L";
        } else if (sector.isRoad()) {
            sectorContent = "r";
        } else if (sector.isForest()) {
            sectorContent = "F";
        } else if (sector.isBush()) {
            sectorContent = "b";
        } else if (showSetupAreas && sector.getNation() != null) {
            sectorContent = "" + sector.getNation().getId();
        } else {
            sectorContent = " ";
        }

        return sectorContent;
    }

    public static void visualizeDimensions(FieldBattleMap fbt) {

        StringBuilder sb = new StringBuilder();

        // upper border
        for (int x = 0; x < fbt.getSizeX() * 2 + 1; x++) {
            sb.append("+++++");
        }
        sb.append("\n");

        // content
        for (int y = 0; y < fbt.getSizeY(); y++) {
            sb.append("+");
            for (int x = 0; x < fbt.getSizeX(); x++) {
                if (x != 0) {
                    sb.append("|");
                }

                sb.append(String.format("%02d", x)).append(",")
                        .append(String.format("%02d", y));
            }
            sb.append("+");
            sb.append("\n");
        }

        // lower border
        for (int x = 0; x < fbt.getSizeX() * 2 + 1; x++) {
            sb.append("+++++");
        }
        sb.append("\n");

        System.out.println(sb.toString());
    }

    @SuppressWarnings("unchecked")
    public static String visualizeEverythingAsString(FieldBattleMap fbMap) {
        return visualizeEverythingAsString(fbMap, Collections.EMPTY_SET);
    }

    public static Set<FieldBattleSector> convertToArmySectors(FieldBattleMap fbMap, List<Brigade>[] sideBrigades) {
        Set<FieldBattleSector> sectors = new HashSet<FieldBattleSector>();

        for (Brigade brig : sideBrigades[0]) {
            sectors.add(fbMap.getFieldBattleSector(brig.getFieldBattlePosition().getX(), brig.getFieldBattlePosition().getY()));
        }

        for (Brigade brig : sideBrigades[1]) {
            sectors.add(fbMap.getFieldBattleSector(brig.getFieldBattlePosition().getX(), brig.getFieldBattlePosition().getY()));
        }

        return sectors;
    }

    public static String getHeadCountInformation(Brigade brigade) {
        StringBuilder sb = new StringBuilder();
        for (Battalion battalion : brigade.getBattalions()) {
            sb.append(battalion.getType().getName());
            sb.append(": ");
            sb.append(battalion.getHeadcount());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void visualizeBrigadesInfo(List<Brigade>[] sideBrigades) {
        StringBuilder sb = new StringBuilder();
        for (Brigade brig : sideBrigades[0]) {
            sb.append("Side: 0, brigade: " + brig + "\n");
            sb.append(getHeadCountInformation(brig));
        }
        for (Brigade brig : sideBrigades[1]) {
            sb.append("Side: 1, brigade: " + brig + "\n");
            sb.append(getHeadCountInformation(brig));
        }
        System.out.println(sb.toString());

    }

    /**
	 * Convenience method for visualisation.
	 * @param <T>
	 * @param grid the 2d array
	 * @param line the line as a list
	 */
	public static <T> void visualize(T[][] grid, List<T> line, boolean inverse) {
		
		T[][] possiblyInvertedGrid = null;
		
		if(inverse) {
			Arrays.copyOf(grid, grid.length-1);
			possiblyInvertedGrid = Arrays.copyOf(grid, grid.length);
			
			for(int x=0; x<grid.length; x++) {
				possiblyInvertedGrid[x] = Arrays.copyOf(grid[x], grid[x].length);
			}
			
			for(int x=0; x<grid.length; x++) {
				for(int y=0; y<grid[0].length; y++) {
					possiblyInvertedGrid[x][y] = grid[x][grid[0].length-y-1];
				}
			}
			
		} else {
			possiblyInvertedGrid = grid;
		}

		StringBuffer sb = new StringBuffer();
		// upper border
		for(int x=0; x<possiblyInvertedGrid.length*2 + 1; x++) {
			 sb.append("+");
        }
        sb.append("\n");
		
        // content
        for(int y=0; y<possiblyInvertedGrid[0].length; y++) {
			
			sb.append("+");
			for(int x=0; x<possiblyInvertedGrid.length; x++) {
				if (x != 0) {
                    sb.append("|");
                }
				sb.append(line.contains(possiblyInvertedGrid[x][y]) ? "X" : " ");
			}
			sb.append("+");
			sb.append("\n");
		}
		
		// lower border
		for(int x=0; x<possiblyInvertedGrid.length*2 + 1; x++) {
			sb.append("+");
		}
		sb.append("\n");
		
		System.out.println(sb.toString());
	}

	public static String visualizeStats(FieldBattleMap fbMap) {
		StringBuilder sb = new StringBuilder();
        sb.append("\n\nForest Statistics");
		
		Set<FieldBattleSector> sectors = MapUtils.getAllSectors(fbMap);
		int forestCount = 0;
		int bushCount = 0;
		
		for(FieldBattleSector sector : sectors) {
			if(sector.isForest()){
				forestCount++;
			} else if(sector.isBush()) {
				bushCount++;
			}
		}
		
        sb.append("Forest: " + forestCount + "/" + sectors.size() + " (" + forestCount * 100 / sectors.size() + "%)\n");
        sb.append("Bush: " + bushCount + "/" + sectors.size() + " (" + bushCount * 100 / sectors.size() + "%)\n\n");
        return sb.toString();
	}
}
