package com.eaw1805.fieldmap;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.dto.converters.FieldBattleMapConverter;
import com.eaw1805.data.dto.web.field.FieldBattleMapDTO;
import com.eaw1805.data.managers.field.FieldBattleMapManager;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import org.hibernate.Transaction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Class to create a field battle map image using tiles.
 */
public class CreateMap {

    private static final String TILES_DIR = "tiles";
    private static final String INPUT_IMG = "input.jpg";
    private static final String OUTPUT_IMG = "output.png";
    public static final int TILE_WIDTH = 32;
    public static final int TILE_HEIGHT = 24;
    private static final int TILE_SCALE = 8;
    private static final boolean IS_BW = false;
    private static final int THREADS = 2;

    private static void log(String msg) {
        System.out.println(msg);
    }

    int x = 0;
    int y = 0;
    int width;
    int height;

    public CreateMap(final FieldBattleMapDTO map, final int scenarioId) throws Exception {
        width = map.getSectors().length * 64;
        height = map.getSectors()[0].length * 64;
        final Map<String, BufferedImage> tiles = new TilesLoader().loadTiles().getTiles();
        final List<ImagePart> images = new MapConstructor(map).constructMap().getImages();


        //construct the basic map
        BufferedImage result = new BufferedImage(
                width, height, //work these out
                BufferedImage.TYPE_INT_RGB);
        Graphics g = result.getGraphics();

        for (final ImagePart image : images) {
            if (tiles.containsKey(image.getCode())) {
                BufferedImage bi = tiles.get(image.getCode());
                g.drawImage(bi, image.getX(), image.getY(), null);
            }

        }
        //write map image to file
        ImageIO.write(result, "jpg", new File("/srv/eaw1805/images/fieldmaps/s" + scenarioId + "/m" + map.getBattleId() + ".jpg"));
//        ImageIO.write(result,"jpg",new File("/home/karavias/m" + map.getBattleId() + ".jpg"));

        //find pixel size for 210 pixels max
        double zoomFactorX = (double) 225 * 1.0 / width;
        double zoomFactorY = (double) 225 * 1.0 / height;
        final int thisTileSize;
        if (zoomFactorX < zoomFactorY) {
            thisTileSize = (int) (64 * zoomFactorX);
        } else {
            thisTileSize = (int) (64 * zoomFactorY);
        }
        int miniMapWidth = map.getSectors().length * thisTileSize;
        int miniMapHeight = map.getSectors()[0].length * thisTileSize;

        //construct the mini map
        BufferedImage resultMini = new BufferedImage(
                miniMapWidth, miniMapHeight, //work these out
                BufferedImage.TYPE_INT_RGB);
        Graphics gMini = resultMini.getGraphics();

        for (final ImagePart image : images) {
            if (tiles.containsKey(image.getCode())) {
                BufferedImage bi = resize(tiles.get(image.getCode()), thisTileSize, thisTileSize);
                gMini.drawImage(bi, (image.getX() / 64) * thisTileSize, (image.getY() / 64) * thisTileSize, null);
            }

        }
        //write map image to file
        ImageIO.write(resultMini, "jpg", new File("/srv/eaw1805/images/fieldmaps/s" + scenarioId +  "/mm" + map.getBattleId() + ".jpg"));

    }

    public BufferedImage resize(BufferedImage img, int aWidth, int aHeight) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage dimg = new BufferedImage(aWidth, aHeight, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, aWidth, aHeight, 0, 0, width, height, null);
        g.dispose();
        return dimg;
    }


    public static void main(final String[] args) {
        int battleId = 15;
        int scenarioId = 1;


        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(scenarioId);

        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();

        final FieldBattleMap map = FieldBattleMapManager.getInstance().getByBattleID(battleId);

        theTrans.rollback();

        try {
            new CreateMap(FieldBattleMapConverter.convert(map), scenarioId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
