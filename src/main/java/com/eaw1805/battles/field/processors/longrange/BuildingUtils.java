package com.eaw1805.battles.field.processors.longrange;

import com.eaw1805.data.model.battles.field.FieldBattleSector;

public class BuildingUtils {
	
	public static boolean containsBuilding(FieldBattleSector sector) {
		
		return sector.getBridge() > 0
				|| sector.getChateau() > 0
				|| sector.getTown() > 0
				|| sector.getVillage() > 0
				|| sector.getWall() > 0
				|| sector.getEntrenchment() > 0;
	}
	
	

}
