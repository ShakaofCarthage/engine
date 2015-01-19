package com.eaw1805.fieldmap;

import com.eaw1805.data.dto.web.field.FieldBattleMapDTO;
import com.eaw1805.data.dto.web.field.FieldBattleSectorDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapConstructor {

    final List<ImagePart> images;
    final int TILE_WIDTH = 64, TILE_HEIGHT = 64;
    final FieldBattleMapDTO map;
    public List<ImagePart> getImages() {
        return images;
    }

    public MapConstructor(final FieldBattleMapDTO map) {

        this.map = map;
        images = new ArrayList<ImagePart>();
    }

    public MapConstructor constructMap() {
        final Random random = new Random();
        int sizeX = map.getSectors().length;
        int sizeY = map.getSectors()[0].length;


        for (int levelIndex = 1; levelIndex <=4; levelIndex++) {
            for (FieldBattleSectorDTO[] row : map.getSectors()) {
                for (FieldBattleSectorDTO sector : row) {
                    if (sector.getAltitude() == levelIndex) {
                        additionElevationImages(sector, map.getSectors());
                    }
                }
            }
        }

        for (FieldBattleSectorDTO[] row : map.getSectors()) {
            for (FieldBattleSectorDTO sector : row) {
                additionShadingImages(sector, map.getSectors());
            }
        }


        for (FieldBattleSectorDTO[] row : map.getSectors()) {
            for (FieldBattleSectorDTO sector : row) {
                final int x = getPointX(sector.getX());
                final int y = getPointY(sector.getY());

                if (sector.isRoad()) {
                    additionRoadImages(sector, map.getSectors());
//                    group.add(new Image(getPointX(sector.getX()), getPointX(sector.getY()), TILE_WIDTH, TILE_HEIGHT, host + "/images/field/road2.png"));
                }
                if (sector.isLake()) {
//                    additionLakeImages(sector, map.getSectors(), group);
                    images.add(new ImagePart("small-lake.png", x, y));
                }
                if (sector.isMinorRiver()) {
                    additionRiverImages(sector, map.getSectors());
//                    group.add(new Image(getPointX(sector.getX()), getPointX(sector.getY()), TILE_WIDTH, TILE_HEIGHT, host + "/images/field/riverlevel1.png"));
                }
                if (sector.isMajorRiver()) {
                    images.add(new ImagePart("river-large-center.png", x, y));
                }
                if (sector.isBush()) {
                    int rand = random.nextInt(4) + 1;
                    images.add(new ImagePart("Rough" + rand + ".png", x, y));
                }
                if (sector.isForest()) {
                    int rand = random.nextInt(4) + 1;
                    images.add(new ImagePart("Woods" + rand + ".png", x, y));
                }






            }
        }
        return this;
    }

    public void additionRiverImages(final FieldBattleSectorDTO sector, final FieldBattleSectorDTO[][] sectors) {
        boolean[][] riverSides = sector.getRiverDirections(sectors);

        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("rr_");

        if (riverSides[1][0]) {
            urlBuilder.append("T");
        }
        if (riverSides[2][1]) {
            urlBuilder.append("R");
        }
        if (riverSides[1][2]) {
            urlBuilder.append("B");
        }
        if (riverSides[0][1]) {
            urlBuilder.append("L");
        }
        if (riverSides[0][0]) {
            urlBuilder.append("1");
        }
        if (riverSides[2][0]) {
            urlBuilder.append("2");
        }
        if (riverSides[2][2]) {
            urlBuilder.append("3");
        }
        if (riverSides[0][2]) {
            urlBuilder.append("4");
        }

        urlBuilder.append(".png");

        images.add(new ImagePart(urlBuilder.toString(), getPointX(sector.getX()), getPointY(sector.getY())));
        //add corner images if any
        if (riverSides[0][0]) {
            images.add(new ImagePart("rr_2.png", getPointX(sector.getX() - 1), getPointX(sector.getY())));
            images.add(new ImagePart("rr_4.png", getPointX(sector.getX()), getPointX(sector.getY() - 1)));
        }
        if (riverSides[2][0]) {
            images.add(new ImagePart("rr_1.png", getPointX(sector.getX() + 1), getPointX(sector.getY())));
            images.add(new ImagePart("rr_3.png", getPointX(sector.getX()), getPointX(sector.getY() - 1)));
        }
        if (riverSides[0][2]) {
            images.add(new ImagePart("rr_3.png", getPointX(sector.getX() - 1), getPointX(sector.getY())));
            images.add(new ImagePart("rr_1.png", getPointX(sector.getX()), getPointX(sector.getY() + 1)));
        }
        if (riverSides[2][2]) {
            images.add(new ImagePart("rr_4.png", getPointX(sector.getX() + 1), getPointX(sector.getY())));
            images.add(new ImagePart("rr_2.png", getPointX(sector.getX()), getPointX(sector.getY() + 1)));
        }
    }

    public void additionRoadImages(final FieldBattleSectorDTO sector, final  FieldBattleSectorDTO[][] sectors) {
        boolean[][] riverSides = sector.getRoadDirections(sectors);

        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("r_");

        if (riverSides[1][0]) {
            urlBuilder.append("T");
        }
        if (riverSides[2][1]) {
            urlBuilder.append("R");
        }
        if (riverSides[1][2]) {
            urlBuilder.append("B");
        }
        if (riverSides[0][1]) {
            urlBuilder.append("L");
        }
        if (riverSides[0][0]) {
            urlBuilder.append("1");
        }
        if (riverSides[2][0]) {
            urlBuilder.append("2");
        }
        if (riverSides[2][2]) {
            urlBuilder.append("3");
        }
        if (riverSides[0][2]) {
            urlBuilder.append("4");
        }

        urlBuilder.append(".png");
        int x=getPointX(sector.getX());
        int y = getPointY(sector.getY());
        images.add(new ImagePart(urlBuilder.toString(), x, y));

        //add corner images if any
        if (riverSides[0][0]) {
            images.add(new ImagePart("r_2.png", getPointX(sector.getX() - 1), getPointX(sector.getY())));
            images.add(new ImagePart("r_4.png", getPointX(sector.getX()), getPointX(sector.getY() - 1)));
        }
        if (riverSides[2][0]) {
            images.add(new ImagePart("r_1.png", getPointX(sector.getX() + 1), getPointX(sector.getY())));
            images.add(new ImagePart("r_3.png", getPointX(sector.getX()), getPointX(sector.getY() - 1)));
        }
        if (riverSides[0][2]) {
            images.add(new ImagePart("r_3.png", getPointX(sector.getX() - 1), getPointX(sector.getY())));
            images.add(new ImagePart("r_1.png", getPointX(sector.getX()), getPointX(sector.getY() + 1)));

        }
        if (riverSides[2][2]) {
            images.add(new ImagePart("r_4.png", getPointX(sector.getX() + 1), getPointX(sector.getY())));
            images.add(new ImagePart("r_2.png", getPointX(sector.getX()), getPointX(sector.getY() + 1)));
        }
    }

    public void additionShadingImages(final FieldBattleSectorDTO sector, final FieldBattleSectorDTO[][] sectors) {
        int sizeX = sectors.length;
        int sizeY = sectors[0].length;
        int x = getPointX(sector.getX());
        int y = getPointY(sector.getY());
        int topLeftMultiplier = 1;
        int topRightMultiplier = 1;
        int bottomLeftMultiplier = 1;
        int bottomRightMultiplier = 1;
        int imgCodeTopLeft = 0;
        int imgCodeTopRight = 0;
        int imgCodeBottomLeft = 0;
        int imgCodeBottomRight = 0;
        if (sector.getX() > 0 && sectors[sector.getX() - 1][sector.getY()].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX() - 1][sector.getY()].getAltitude() < sector.getAltitude()) {
                topLeftMultiplier = -1;
                bottomLeftMultiplier = -1;
            }
            imgCodeTopLeft += 1;
            imgCodeBottomLeft += 4;
        }
        if (sector.getX() > 0 && sector.getY() > 0 && sectors[sector.getX() - 1][sector.getY() - 1].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX() - 1][sector.getY() - 1].getAltitude() < sector.getAltitude()) {
                topLeftMultiplier = -1;
            }
            imgCodeTopLeft += 10;
        }
        if (sector.getY() > 0 && sectors[sector.getX()][sector.getY() - 1].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX()][sector.getY() - 1].getAltitude() < sector.getAltitude()) {
                topLeftMultiplier = -1;
                topRightMultiplier = -1;
            }
            imgCodeTopLeft += 100;
            imgCodeTopRight += 200;
        }
        if (sector.getX() + 1 < sizeX && sector.getY() > 0 && sectors[sector.getX() + 1][sector.getY() - 1].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX() + 1][sector.getY() - 1].getAltitude() < sector.getAltitude()) {
                topRightMultiplier = -1;
            }
            imgCodeTopRight += 20;
        }
        if (sector.getX() + 1 < sizeX && sectors[sector.getX() + 1][sector.getY()].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX() + 1][sector.getY()].getAltitude() < sector.getAltitude()) {
                topRightMultiplier = -1;
                bottomRightMultiplier = -1;
            }
            imgCodeTopRight += 2;
            imgCodeBottomRight += 3;
        }
        if (sector.getX() + 1 < sizeX && sector.getY() + 1 < sizeY && sectors[sector.getX() + 1][sector.getY() + 1].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX() + 1][sector.getY() + 1].getAltitude() < sector.getAltitude()) {
                bottomRightMultiplier = -1;
            }
            imgCodeBottomRight += 30;
        }
        if (sector.getY() + 1 < sizeY && sectors[sector.getX()][sector.getY() + 1].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX()][sector.getY() + 1].getAltitude() < sector.getAltitude()) {
                bottomLeftMultiplier = -1;
                bottomRightMultiplier = -1;
            }
            imgCodeBottomLeft += 400;
            imgCodeBottomRight += 300;
        }
        if (sector.getX() > 0 && sector.getY() + 1 < sizeY && sectors[sector.getX() - 1][sector.getY() + 1].getAltitude() != sector.getAltitude()) {
            if (sectors[sector.getX() - 1][sector.getY() + 1].getAltitude() < sector.getAltitude()) {
                bottomLeftMultiplier = -1;
            }
            imgCodeBottomLeft += 40;
        }
        //fix codes to fit the elevation change
        imgCodeBottomLeft *= bottomLeftMultiplier;
        imgCodeBottomRight *=bottomRightMultiplier;
        imgCodeTopLeft *= topLeftMultiplier;
        imgCodeTopRight *= topRightMultiplier;
        if (imgCodeTopLeft != 0) {
            images.add(new ImagePart("s_" + imgCodeTopLeft + ".png", getPointX(sector.getX()), getPointX(sector.getY())));
        }
        if (imgCodeTopRight != 0) {
            images.add(new ImagePart("s_" + imgCodeTopRight + ".png", getPointX(sector.getX()) + TILE_WIDTH/2, getPointX(sector.getY())));
        }
        if (imgCodeBottomLeft != 0) {
            images.add(new ImagePart("s_" + imgCodeBottomLeft + ".png", getPointX(sector.getX()), getPointX(sector.getY()) + TILE_HEIGHT/2));
        }
        if (imgCodeBottomRight != 0) {
            images.add(new ImagePart("s_" + imgCodeBottomRight + ".png", getPointX(sector.getX()) + TILE_WIDTH/2, getPointX(sector.getY()) + TILE_HEIGHT/2));
        }

    }


    public void additionElevationImages(final FieldBattleSectorDTO sector, final FieldBattleSectorDTO[][] sectors) {
        int sizeX = sectors.length;
        int sizeY = sectors[0].length;
        int x = getPointX(sector.getX());
        int y = getPointY(sector.getY());
        if (sector.getAltitude() == 1) {
            images.add(new ImagePart("elevation1.png", x, y));
        } else {
            if (sector.getAltitude() == 2) {
                images.add(new ImagePart("elevation1.png", x, y));
            } else if (sector.getAltitude() == 3) {
                images.add(new ImagePart("elevation2/e_.png", x, y));
            } else if (sector.getAltitude() == 4) {
                images.add(new ImagePart("elevation3/e_.png", x, y));
            }
            String imgName = "e_";
            //for up right bottom left sides
            if (sector.getY() > 0 && sectors[sector.getX()][sector.getY() - 1].getAltitude() < sector.getAltitude()) {
                imgName += "T";
            }
            if (sector.getX() + 1 < sizeX && sectors[sector.getX() + 1][sector.getY()].getAltitude() < sector.getAltitude()) {
                imgName += "R";
            }
            if (sector.getY() + 1 < sizeY && sectors[sector.getX()][sector.getY() + 1].getAltitude() < sector.getAltitude()) {
                imgName += "B";
            }
            if (sector.getX() > 0 && sectors[sector.getX() - 1][sector.getY()].getAltitude() < sector.getAltitude()) {
                imgName+="L";
            }


            //for corner sides
            if ((imgName.contains("T") || imgName.contains("L")) || (sector.getX() > 0 && sector.getY() > 0 && sectors[sector.getX() - 1][sector.getY() - 1].getAltitude() < sector.getAltitude())) {
                imgName += "1";
            }
            if ((imgName.contains("T") || imgName.contains("R")) || (sector.getX() + 1 < sizeX && sector.getY() > 0 && sectors[sector.getX() + 1][sector.getY() - 1].getAltitude() < sector.getAltitude())) {
                imgName += "2";
            }
            if ((imgName.contains("R") || imgName.contains("B")) || (sector.getX() + 1 < sizeX && sector.getY() + 1 < sizeY && sectors[sector.getX() + 1][sector.getY() + 1].getAltitude() < sector.getAltitude())) {
                imgName += "3";
            }
            if ((imgName.contains("B") || imgName.contains("L")) || (sector.getX() > 0 && sector.getY() + 1 < sizeY && sectors[sector.getX() - 1][sector.getY() + 1].getAltitude() < sector.getAltitude())) {
                imgName += "4";
            }
            images.add(new ImagePart("elevation" + sector.getAltitude() + "/" + imgName + ".png", x, y));
        }
    }

    public int getPointX(final int x) {
        return x*64;
    }

    public int getPointY(final int y) {
        return y*64;
    }
}
