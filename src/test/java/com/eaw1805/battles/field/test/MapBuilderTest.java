package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.generation.MapBuilder;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapBuilderTest {

    //    @Test(timeout = 1500 * 60)
    @Test
    public void testBuildMap() throws IOException {

        int[] terrainTypes = new int[]{TerrainConstants.TERRAIN_S, TerrainConstants.TERRAIN_W};
        int[] terrainMaxDensities = new int[]{9};
        int[] battalionNumbers = new int[]{300, 400, 700, 1000};
        int[] fortSizes = new int[]{-1, ProductionSiteConstants.PS_BARRACKS_FS, ProductionSiteConstants.PS_BARRACKS_FM,
                ProductionSiteConstants.PS_BARRACKS_FL, ProductionSiteConstants.PS_BARRACKS_FH};

        Map<Integer, Integer> versus = new HashMap<Integer, Integer>();
        versus.put(1, 1);
        versus.put(2, 2);

        String today = new SimpleDateFormat("ddMMM-hhmm").format(new Date());
        File todaysDir = new File("/Users/deadlock/Desktop/" + today);
        todaysDir.mkdir();

        for (int terrainType : terrainTypes) {
            for (int terrainMaxDensity : terrainMaxDensities) {
                for (int battalionNumber : battalionNumbers) {
                    for (int fortSize : fortSizes) {

                        for (Entry<Integer, Integer> vs : versus.entrySet()) {

                            String fileName = "terrain" + FieldBattleTestUtils.TerrainConstantsEnum.fromId(terrainType).getName() + "_maxdens"
                                    + terrainMaxDensity + "_battnum" + battalionNumber
                                    + "_fort" + getFortNotation(fortSize) + "_" + vs.getKey() + "vs" + vs.getValue() + ".txt";
                            System.out.println(fileName);

                            FileWriter fw = new FileWriter(todaysDir + "/" + fileName);

                            MapBuilder mapBuilder = FieldBattleTestUtils.prepareMapBuilder(terrainType, terrainMaxDensity, battalionNumber, fortSize, vs.getKey(), vs.getValue());
                            mapBuilder.buildMap();

                            for (int i = 0; i < 1; i++) {

                                FieldBattleMap fbMap = mapBuilder.buildMap();
                                fw.write("Field Battle #" + i + "\n");
                                fw.write(VisualisationUtils.visualizeAsString(fbMap));
                                fw.write(VisualisationUtils.visualizeAltitudeAsString(fbMap));
                                fw.write(VisualisationUtils.visualizeEverythingAsString(fbMap));
                                fw.write("\n\n\n");

                            }

                            fw.close();
                        }
                    }
                }
            }
        }

    }


    private String getFortNotation(int fortSize) {

        switch (fortSize) {
            case ProductionSiteConstants.PS_BARRACKS_FS:
                return "S";
            case ProductionSiteConstants.PS_BARRACKS_FM:
                return "M";
            case ProductionSiteConstants.PS_BARRACKS_FL:
                return "L";
            case ProductionSiteConstants.PS_BARRACKS_FH:
                return "H";
            default:
                return "N";
        }
    }
}
