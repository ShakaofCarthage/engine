package com.eaw1805.fieldmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TilesLoader {

    final Map<String, BufferedImage> nameToImage = new HashMap<String, BufferedImage>();
    final String basePath = "/var/www-eaw1805/images/field/";
//    final String basePath = "/var/www/images/field/";
    final String elevationPath = "map/elevation/";
    final String riverPath = "map/rivers/";
    final String roadPath = "map/roads/";
    final String shadingPath = "map/shading/";
    final String vegetationPath = "map/vegetation/";

    public TilesLoader() {

    }

    public TilesLoader loadTiles() throws IOException{
        loadElevationImages();
        loadRiverImages();
        loadRoadImages();
        loadShadingImages();
        loadVegetationImages();
        return this;
    }


    private void loadElevationImages() throws IOException {
        nameToImage.put("elevation1.png", ImageIO.read(new File(basePath + elevationPath + "elevation1.png")));
        nameToImage.put("elevation2/e_.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_.png")));
        nameToImage.put("elevation2/e_1.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_1.png")));
        nameToImage.put("elevation2/e_2.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_2.png")));
        nameToImage.put("elevation2/e_02.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_02.png")));
        nameToImage.put("elevation2/e_3.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_3.png")));
        nameToImage.put("elevation2/e_4.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_4.png")));
        nameToImage.put("elevation2/e_12.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_12.png")));
        nameToImage.put("elevation2/e_13.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_13.png")));
        nameToImage.put("elevation2/e_14.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_14.png")));
        nameToImage.put("elevation2/e_23.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_23.png")));
        nameToImage.put("elevation2/e_24.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_24.png")));
        nameToImage.put("elevation2/e_34.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_34.png")));
        nameToImage.put("elevation2/e_123.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_123.png")));
        nameToImage.put("elevation2/e_124.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_124.png")));
        nameToImage.put("elevation2/e_134.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_134.png")));
        nameToImage.put("elevation2/e_234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_234.png")));
        nameToImage.put("elevation2/e_1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_1234.png")));
        nameToImage.put("elevation2/e_B34.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_B34.png")));
        nameToImage.put("elevation2/e_B134.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_B134.png")));
        nameToImage.put("elevation2/e_B234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_B234.png")));
        nameToImage.put("elevation2/e_B1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_B1234.png")));
        nameToImage.put("elevation2/e_BL134.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_BL134.png")));
        nameToImage.put("elevation2/e_BL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_BL1234.png")));
        nameToImage.put("elevation2/e_L14.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_L14.png")));
        nameToImage.put("elevation2/e_L124.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_L124.png")));
        nameToImage.put("elevation2/e_L134.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_L134.png")));
        nameToImage.put("elevation2/e_L1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_L1234.png")));
        nameToImage.put("elevation2/e_R23.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_R23.png")));
        nameToImage.put("elevation2/e_R123.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_R123.png")));
        nameToImage.put("elevation2/e_R234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_R234.png")));
        nameToImage.put("elevation2/e_R1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_R1234.png")));
        nameToImage.put("elevation2/e_RB234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_RB234.png")));
        nameToImage.put("elevation2/e_RB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_RB1234.png")));
        nameToImage.put("elevation2/e_RBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_RBL1234.png")));
        nameToImage.put("elevation2/e_RL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_RL1234.png")));
        nameToImage.put("elevation2/e_T12.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_T12.png")));
        nameToImage.put("elevation2/e_T123.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_T123.png")));
        nameToImage.put("elevation2/e_T124.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_T124.png")));
        nameToImage.put("elevation2/e_T1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_T1234.png")));
        nameToImage.put("elevation2/e_TB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TB1234.png")));
        nameToImage.put("elevation2/e_TBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TBL1234.png")));
        nameToImage.put("elevation2/e_TL124.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TL124.png")));
        nameToImage.put("elevation2/e_TL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TL1234.png")));
        nameToImage.put("elevation2/e_TR123.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TR123.png")));
        nameToImage.put("elevation2/e_TR1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TR1234.png")));
        nameToImage.put("elevation2/e_TRB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TRB1234.png")));
        nameToImage.put("elevation2/e_TRL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_TRL1234.png")));
        nameToImage.put("elevation2/e_URBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation2/e_URBL1234.png")));

        nameToImage.put("elevation3/e_.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_.png")));
        nameToImage.put("elevation3/e_1.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_1.png")));
        nameToImage.put("elevation3/e_2.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_2.png")));
//        nameToImage.put("elevation3/e_02.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_02.png")));
        nameToImage.put("elevation3/e_3.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_3.png")));
        nameToImage.put("elevation3/e_4.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_4.png")));
        nameToImage.put("elevation3/e_12.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_12.png")));
        nameToImage.put("elevation3/e_13.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_13.png")));
        nameToImage.put("elevation3/e_14.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_14.png")));
        nameToImage.put("elevation3/e_23.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_23.png")));
        nameToImage.put("elevation3/e_24.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_24.png")));
        nameToImage.put("elevation3/e_34.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_34.png")));
        nameToImage.put("elevation3/e_123.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_123.png")));
        nameToImage.put("elevation3/e_124.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_124.png")));
        nameToImage.put("elevation3/e_134.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_134.png")));
        nameToImage.put("elevation3/e_234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_234.png")));
        nameToImage.put("elevation3/e_1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_1234.png")));
        nameToImage.put("elevation3/e_B34.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_B34.png")));
        nameToImage.put("elevation3/e_B134.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_B134.png")));
        nameToImage.put("elevation3/e_B234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_B234.png")));
        nameToImage.put("elevation3/e_B1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_B1234.png")));
        nameToImage.put("elevation3/e_BL134.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_BL134.png")));
        nameToImage.put("elevation3/e_BL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_BL1234.png")));
        nameToImage.put("elevation3/e_L14.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_L14.png")));
        nameToImage.put("elevation3/e_L124.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_L124.png")));
        nameToImage.put("elevation3/e_L134.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_L134.png")));
        nameToImage.put("elevation3/e_L1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_L1234.png")));
        nameToImage.put("elevation3/e_R23.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_R23.png")));
        nameToImage.put("elevation3/e_R123.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_R123.png")));
        nameToImage.put("elevation3/e_R234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_R234.png")));
        nameToImage.put("elevation3/e_R1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_R1234.png")));
        nameToImage.put("elevation3/e_RB234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_RB234.png")));
        nameToImage.put("elevation3/e_RB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_RB1234.png")));
        nameToImage.put("elevation3/e_RBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_RBL1234.png")));
        nameToImage.put("elevation3/e_RL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_RL1234.png")));
        nameToImage.put("elevation3/e_T12.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_T12.png")));
        nameToImage.put("elevation3/e_T123.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_T123.png")));
        nameToImage.put("elevation3/e_T124.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_T124.png")));
        nameToImage.put("elevation3/e_T1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_T1234.png")));
        nameToImage.put("elevation3/e_TB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TB1234.png")));
        nameToImage.put("elevation3/e_TBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TBL1234.png")));
        nameToImage.put("elevation3/e_TL124.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TL124.png")));
        nameToImage.put("elevation3/e_TL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TL1234.png")));
        nameToImage.put("elevation3/e_TR123.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TR123.png")));
        nameToImage.put("elevation3/e_TR1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TR1234.png")));
        nameToImage.put("elevation3/e_TRB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TRB1234.png")));
        nameToImage.put("elevation3/e_TRL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_TRL1234.png")));
//        nameToImage.put("elevation3/e_URBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation3/e_URBL1234.png")));

        nameToImage.put("elevation4/e_.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_.png")));
        nameToImage.put("elevation4/e_1.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_1.png")));
        nameToImage.put("elevation4/e_2.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_2.png")));
//        nameToImage.put("elevation4/e_02.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_02.png")));
        nameToImage.put("elevation4/e_3.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_3.png")));
        nameToImage.put("elevation4/e_4.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_4.png")));
        nameToImage.put("elevation4/e_12.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_12.png")));
        nameToImage.put("elevation4/e_13.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_13.png")));
        nameToImage.put("elevation4/e_14.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_14.png")));
        nameToImage.put("elevation4/e_23.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_23.png")));
        nameToImage.put("elevation4/e_24.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_24.png")));
        nameToImage.put("elevation4/e_34.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_34.png")));
        nameToImage.put("elevation4/e_123.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_123.png")));
        nameToImage.put("elevation4/e_124.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_124.png")));
        nameToImage.put("elevation4/e_134.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_134.png")));
        nameToImage.put("elevation4/e_234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_234.png")));
        nameToImage.put("elevation4/e_1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_1234.png")));
        nameToImage.put("elevation4/e_B34.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_B34.png")));
        nameToImage.put("elevation4/e_B134.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_B134.png")));
        nameToImage.put("elevation4/e_B234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_B234.png")));
        nameToImage.put("elevation4/e_B1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_B1234.png")));
        nameToImage.put("elevation4/e_BL134.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_BL134.png")));
        nameToImage.put("elevation4/e_BL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_BL1234.png")));
        nameToImage.put("elevation4/e_L14.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_L14.png")));
        nameToImage.put("elevation4/e_L124.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_L124.png")));
        nameToImage.put("elevation4/e_L134.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_L134.png")));
        nameToImage.put("elevation4/e_L1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_L1234.png")));
        nameToImage.put("elevation4/e_R23.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_R23.png")));
        nameToImage.put("elevation4/e_R123.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_R123.png")));
        nameToImage.put("elevation4/e_R234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_R234.png")));
        nameToImage.put("elevation4/e_R1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_R1234.png")));
        nameToImage.put("elevation4/e_RB234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_RB234.png")));
        nameToImage.put("elevation4/e_RB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_RB1234.png")));
        nameToImage.put("elevation4/e_RBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_RBL1234.png")));
        nameToImage.put("elevation4/e_RL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_RL1234.png")));
        nameToImage.put("elevation4/e_T12.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_T12.png")));
        nameToImage.put("elevation4/e_T123.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_T123.png")));
        nameToImage.put("elevation4/e_T124.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_T124.png")));
        nameToImage.put("elevation4/e_T1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_T1234.png")));
        nameToImage.put("elevation4/e_TB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TB1234.png")));
        nameToImage.put("elevation4/e_TBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TBL1234.png")));
        nameToImage.put("elevation4/e_TL124.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TL124.png")));
        nameToImage.put("elevation4/e_TL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TL1234.png")));
        nameToImage.put("elevation4/e_TR123.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TR123.png")));
        nameToImage.put("elevation4/e_TR1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TR1234.png")));
        nameToImage.put("elevation4/e_TRB1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TRB1234.png")));
        nameToImage.put("elevation4/e_TRL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_TRL1234.png")));
//        nameToImage.put("elevation4/e_URBL1234.png", ImageIO.read(new File(basePath + elevationPath + "elevation4/e_URBL1234.png")));
    }

    private void loadRiverImages() throws IOException {
        nameToImage.put("rr_1.png", ImageIO.read(new File(basePath + riverPath + "r_1.png")));
        nameToImage.put("rr_2.png", ImageIO.read(new File(basePath + riverPath + "r_2.png")));
        nameToImage.put("rr_3.png", ImageIO.read(new File(basePath + riverPath + "r_3.png")));
        nameToImage.put("rr_4.png", ImageIO.read(new File(basePath + riverPath + "r_4.png")));
        nameToImage.put("rr_13.png", ImageIO.read(new File(basePath + riverPath + "r_13.png")));
        nameToImage.put("rr_24.png", ImageIO.read(new File(basePath + riverPath + "r_24.png")));
        nameToImage.put("rr_B1.png", ImageIO.read(new File(basePath + riverPath + "r_B1.png")));
        nameToImage.put("rr_B2.png", ImageIO.read(new File(basePath + riverPath + "r_B2.png")));
        nameToImage.put("rr_BL.png", ImageIO.read(new File(basePath + riverPath + "r_BL.png")));
        nameToImage.put("rr_L2.png", ImageIO.read(new File(basePath + riverPath + "r_L2.png")));
        nameToImage.put("rr_L3.png", ImageIO.read(new File(basePath + riverPath + "r_L3.png")));
        nameToImage.put("rr_R1.png", ImageIO.read(new File(basePath + riverPath + "r_R1.png")));
        nameToImage.put("rr_R4.png", ImageIO.read(new File(basePath + riverPath + "r_R4.png")));
        nameToImage.put("rr_RB.png", ImageIO.read(new File(basePath + riverPath + "r_RB.png")));
        nameToImage.put("rr_RL.png", ImageIO.read(new File(basePath + riverPath + "r_RL.png")));
        nameToImage.put("rr_T3.png", ImageIO.read(new File(basePath + riverPath + "r_T3.png")));
        nameToImage.put("rr_T4.png", ImageIO.read(new File(basePath + riverPath + "r_T4.png")));
        nameToImage.put("rr_TB.png", ImageIO.read(new File(basePath + riverPath + "r_TB.png")));
        nameToImage.put("rr_TL.png", ImageIO.read(new File(basePath + riverPath + "r_TL.png")));
        nameToImage.put("rr_TR.png", ImageIO.read(new File(basePath + riverPath + "r_TR.png")));
        nameToImage.put("rr_12.png", ImageIO.read(new File(basePath + riverPath + "r_12.png")));
        nameToImage.put("rr_14.png", ImageIO.read(new File(basePath + riverPath + "r_14.png")));
        nameToImage.put("rr_23.png", ImageIO.read(new File(basePath + riverPath + "r_23.png")));
        nameToImage.put("rr_34.png", ImageIO.read(new File(basePath + riverPath + "r_34.png")));
    }

    private void loadRoadImages() throws IOException {
        nameToImage.put("r_1.png", ImageIO.read(new File(basePath + roadPath + "r_1.png")));
        nameToImage.put("r_2.png", ImageIO.read(new File(basePath + roadPath + "r_2.png")));
        nameToImage.put("r_3.png", ImageIO.read(new File(basePath + roadPath + "r_3.png")));
        nameToImage.put("r_4.png", ImageIO.read(new File(basePath + roadPath + "r_4.png")));
        nameToImage.put("r_12.png", ImageIO.read(new File(basePath + roadPath + "r_12.png")));
        nameToImage.put("r_13.png", ImageIO.read(new File(basePath + roadPath + "r_13.png")));
        nameToImage.put("r_14.png", ImageIO.read(new File(basePath + roadPath + "r_14.png")));
        nameToImage.put("r_23.png", ImageIO.read(new File(basePath + roadPath + "r_23.png")));
        nameToImage.put("r_24.png", ImageIO.read(new File(basePath + roadPath + "r_24.png")));
        nameToImage.put("r_34.png", ImageIO.read(new File(basePath + roadPath + "r_34.png")));
        nameToImage.put("r_1234.png", ImageIO.read(new File(basePath + roadPath + "r_1234.png")));
        nameToImage.put("r_B.png", ImageIO.read(new File(basePath + roadPath + "r_B.png")));
        nameToImage.put("r_B1.png", ImageIO.read(new File(basePath + roadPath + "r_B1.png")));
        nameToImage.put("r_B2.png", ImageIO.read(new File(basePath + roadPath + "r_B2.png")));
        nameToImage.put("r_B12.png", ImageIO.read(new File(basePath + roadPath + "r_B12.png")));
        nameToImage.put("r_BL.png", ImageIO.read(new File(basePath + roadPath + "r_BL.png")));
        nameToImage.put("r_BL2.png", ImageIO.read(new File(basePath + roadPath + "r_BL2.png")));
        nameToImage.put("r_L.png", ImageIO.read(new File(basePath + roadPath + "r_L.png")));
        nameToImage.put("r_L2.png", ImageIO.read(new File(basePath + roadPath + "r_L2.png")));
        nameToImage.put("r_L3.png", ImageIO.read(new File(basePath + roadPath + "r_L3.png")));
        nameToImage.put("r_L23.png", ImageIO.read(new File(basePath + roadPath + "r_L23.png")));
        nameToImage.put("r_R.png", ImageIO.read(new File(basePath + roadPath + "r_R.png")));
        nameToImage.put("r_R1.png", ImageIO.read(new File(basePath + roadPath + "r_R1.png")));
        nameToImage.put("r_R4.png", ImageIO.read(new File(basePath + roadPath + "r_R4.png")));
        nameToImage.put("r_R14.png", ImageIO.read(new File(basePath + roadPath + "r_R14.png")));
        nameToImage.put("r_RB.png", ImageIO.read(new File(basePath + roadPath + "r_RB.png")));
        nameToImage.put("r_RB1.png", ImageIO.read(new File(basePath + roadPath + "r_RB1.png")));
        nameToImage.put("r_RBL.png", ImageIO.read(new File(basePath + roadPath + "r_RBL.png")));
        nameToImage.put("r_RL.png", ImageIO.read(new File(basePath + roadPath + "r_RL.png")));
        nameToImage.put("r_T.png", ImageIO.read(new File(basePath + roadPath + "r_T.png")));
        nameToImage.put("r_T3.png", ImageIO.read(new File(basePath + roadPath + "r_T3.png")));
        nameToImage.put("r_T4.png", ImageIO.read(new File(basePath + roadPath + "r_T4.png")));
        nameToImage.put("r_T34.png", ImageIO.read(new File(basePath + roadPath + "r_T34.png")));
        nameToImage.put("r_TB.png", ImageIO.read(new File(basePath + roadPath + "r_TB.png")));
        nameToImage.put("r_TBL.png", ImageIO.read(new File(basePath + roadPath + "r_TBL.png")));
        nameToImage.put("r_TL.png", ImageIO.read(new File(basePath + roadPath + "r_TL.png")));
        nameToImage.put("r_TL3.png", ImageIO.read(new File(basePath + roadPath + "r_TL3.png")));
        nameToImage.put("r_TR.png", ImageIO.read(new File(basePath + roadPath + "r_TR.png")));
        nameToImage.put("r_TR4.png", ImageIO.read(new File(basePath + roadPath + "r_TR4.png")));
        nameToImage.put("r_TRB.png", ImageIO.read(new File(basePath + roadPath + "r_TRB.png")));
        nameToImage.put("r_TRBL.png", ImageIO.read(new File(basePath + roadPath + "r_TRBL.png")));
        nameToImage.put("r_TRL.png", ImageIO.read(new File(basePath + roadPath + "r_TRL.png")));
        nameToImage.put("r_RL1.png", ImageIO.read(new File(basePath + roadPath + "r_RL1.png")));
        nameToImage.put("r_RL2.png", ImageIO.read(new File(basePath + roadPath + "r_RL2.png")));
        nameToImage.put("r_RL3.png", ImageIO.read(new File(basePath + roadPath + "r_RL3.png")));
        nameToImage.put("r_RL4.png", ImageIO.read(new File(basePath + roadPath + "r_RL4.png")));
        nameToImage.put("r_RL14.png", ImageIO.read(new File(basePath + roadPath + "r_RL14.png")));
        nameToImage.put("r_RL23.png", ImageIO.read(new File(basePath + roadPath + "r_RL23.png")));
        nameToImage.put("r_TB1.png", ImageIO.read(new File(basePath + roadPath + "r_TB1.png")));
        nameToImage.put("r_TB2.png", ImageIO.read(new File(basePath + roadPath + "r_TB2.png")));
        nameToImage.put("r_TB12.png", ImageIO.read(new File(basePath + roadPath + "r_TB12.png")));
        nameToImage.put("r_TB3.png", ImageIO.read(new File(basePath + roadPath + "r_TB3.png")));
        nameToImage.put("r_TB4.png", ImageIO.read(new File(basePath + roadPath + "r_TB4.png")));
        nameToImage.put("r_TB34.png", ImageIO.read(new File(basePath + roadPath + "r_TB34.png")));
    }

    private void loadShadingImages() throws IOException {
        nameToImage.put("s_1.png", ImageIO.read(new File(basePath + shadingPath + "s_1.png")));
        nameToImage.put("s_2.png", ImageIO.read(new File(basePath + shadingPath + "s_2.png")));
        nameToImage.put("s_3.png", ImageIO.read(new File(basePath + shadingPath + "s_3.png")));
        nameToImage.put("s_4.png", ImageIO.read(new File(basePath + shadingPath + "s_4.png")));
        nameToImage.put("s_10.png", ImageIO.read(new File(basePath + shadingPath + "s_10.png")));
        nameToImage.put("s_11.png", ImageIO.read(new File(basePath + shadingPath + "s_11.png")));
        nameToImage.put("s_20.png", ImageIO.read(new File(basePath + shadingPath + "s_20.png")));
        nameToImage.put("s_22.png", ImageIO.read(new File(basePath + shadingPath + "s_22.png")));
        nameToImage.put("s_30.png", ImageIO.read(new File(basePath + shadingPath + "s_30.png")));
        nameToImage.put("s_33.png", ImageIO.read(new File(basePath + shadingPath + "s_33.png")));
        nameToImage.put("s_40.png", ImageIO.read(new File(basePath + shadingPath + "s_40.png")));
        nameToImage.put("s_44.png", ImageIO.read(new File(basePath + shadingPath + "s_44.png")));
        nameToImage.put("s_100.png", ImageIO.read(new File(basePath + shadingPath + "s_100.png")));
        nameToImage.put("s_101.png", ImageIO.read(new File(basePath + shadingPath + "s_101.png")));
        nameToImage.put("s_110.png", ImageIO.read(new File(basePath + shadingPath + "s_110.png")));
        nameToImage.put("s_111.png", ImageIO.read(new File(basePath + shadingPath + "s_111.png")));
        nameToImage.put("s_200.png", ImageIO.read(new File(basePath + shadingPath + "s_200.png")));
        nameToImage.put("s_202.png", ImageIO.read(new File(basePath + shadingPath + "s_202.png")));
        nameToImage.put("s_220.png", ImageIO.read(new File(basePath + shadingPath + "s_220.png")));
        nameToImage.put("s_222.png", ImageIO.read(new File(basePath + shadingPath + "s_222.png")));
        nameToImage.put("s_300.png", ImageIO.read(new File(basePath + shadingPath + "s_300.png")));
        nameToImage.put("s_303.png", ImageIO.read(new File(basePath + shadingPath + "s_303.png")));
        nameToImage.put("s_330.png", ImageIO.read(new File(basePath + shadingPath + "s_330.png")));
        nameToImage.put("s_333.png", ImageIO.read(new File(basePath + shadingPath + "s_333.png")));
        nameToImage.put("s_400.png", ImageIO.read(new File(basePath + shadingPath + "s_400.png")));
        nameToImage.put("s_404.png", ImageIO.read(new File(basePath + shadingPath + "s_404.png")));
        nameToImage.put("s_440.png", ImageIO.read(new File(basePath + shadingPath + "s_440.png")));
        nameToImage.put("s_444.png", ImageIO.read(new File(basePath + shadingPath + "s_444.png")));
        nameToImage.put("s_-1.png", ImageIO.read(new File(basePath + shadingPath + "s_-1.png")));
        nameToImage.put("s_-2.png", ImageIO.read(new File(basePath + shadingPath + "s_-2.png")));
        nameToImage.put("s_-3.png", ImageIO.read(new File(basePath + shadingPath + "s_-3.png")));
        nameToImage.put("s_-4.png", ImageIO.read(new File(basePath + shadingPath + "s_-4.png")));
        nameToImage.put("s_-10.png", ImageIO.read(new File(basePath + shadingPath + "s_-10.png")));
        nameToImage.put("s_-11.png", ImageIO.read(new File(basePath + shadingPath + "s_-11.png")));
        nameToImage.put("s_-20.png", ImageIO.read(new File(basePath + shadingPath + "s_-20.png")));
        nameToImage.put("s_-22.png", ImageIO.read(new File(basePath + shadingPath + "s_-22.png")));
        nameToImage.put("s_-30.png", ImageIO.read(new File(basePath + shadingPath + "s_-30.png")));
        nameToImage.put("s_-33.png", ImageIO.read(new File(basePath + shadingPath + "s_-33.png")));
        nameToImage.put("s_-40.png", ImageIO.read(new File(basePath + shadingPath + "s_-40.png")));
        nameToImage.put("s_-44.png", ImageIO.read(new File(basePath + shadingPath + "s_-44.png")));
        nameToImage.put("s_-100.png", ImageIO.read(new File(basePath + shadingPath + "s_-100.png")));
        nameToImage.put("s_-101.png", ImageIO.read(new File(basePath + shadingPath + "s_-101.png")));
        nameToImage.put("s_-110.png", ImageIO.read(new File(basePath + shadingPath + "s_-110.png")));
        nameToImage.put("s_-111.png", ImageIO.read(new File(basePath + shadingPath + "s_-111.png")));
        nameToImage.put("s_-200.png", ImageIO.read(new File(basePath + shadingPath + "s_-200.png")));
        nameToImage.put("s_-202.png", ImageIO.read(new File(basePath + shadingPath + "s_-202.png")));
        nameToImage.put("s_-220.png", ImageIO.read(new File(basePath + shadingPath + "s_-220.png")));
        nameToImage.put("s_-222.png", ImageIO.read(new File(basePath + shadingPath + "s_-222.png")));
        nameToImage.put("s_-300.png", ImageIO.read(new File(basePath + shadingPath + "s_-300.png")));
        nameToImage.put("s_-303.png", ImageIO.read(new File(basePath + shadingPath + "s_-303.png")));
        nameToImage.put("s_-330.png", ImageIO.read(new File(basePath + shadingPath + "s_-330.png")));
        nameToImage.put("s_-333.png", ImageIO.read(new File(basePath + shadingPath + "s_-333.png")));
        nameToImage.put("s_-400.png", ImageIO.read(new File(basePath + shadingPath + "s_-400.png")));
        nameToImage.put("s_-404.png", ImageIO.read(new File(basePath + shadingPath + "s_-404.png")));
        nameToImage.put("s_-440.png", ImageIO.read(new File(basePath + shadingPath + "s_-440.png")));
        nameToImage.put("s_-444.png", ImageIO.read(new File(basePath + shadingPath + "s_-444.png")));
    }

    private void loadVegetationImages() throws IOException {
        nameToImage.put("Rough1.png", ImageIO.read(new File(basePath + vegetationPath +  "Rough1.png" )));
        nameToImage.put("Rough2.png", ImageIO.read(new File(basePath + vegetationPath +  "Rough2.png" )));
        nameToImage.put("Rough3.png", ImageIO.read(new File(basePath + vegetationPath +  "Rough3.png" )));
        nameToImage.put("Rough4.png", ImageIO.read(new File(basePath + vegetationPath +  "Rough4.png" )));
        nameToImage.put("Woods1.png", ImageIO.read(new File(basePath + vegetationPath +  "Woods1.png" )));
        nameToImage.put("Woods2.png", ImageIO.read(new File(basePath + vegetationPath +  "Woods2.png" )));
        nameToImage.put("Woods3.png", ImageIO.read(new File(basePath + vegetationPath +  "Woods3.png" )));
        nameToImage.put("Woods4.png", ImageIO.read(new File(basePath + vegetationPath +  "Woods4.png" )));
    }

    public Map<String, BufferedImage> getTiles() {
        return nameToImage;
    }

}
