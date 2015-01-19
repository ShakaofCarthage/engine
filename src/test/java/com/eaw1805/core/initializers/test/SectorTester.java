package com.eaw1805.core.initializers.test;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Generates static tables based on data found in SECTORS table.
 */
public class SectorTester
        implements RegionConstants {

    /**
     * Default constructor.
     */
    public SectorTester() {
        // do nothing
    }

    /**
     * Convert the sector data into the static table used by the sector initializer.
     *
     * @param regionId the region to extract.
     */
    public void extractPolitical(final int regionId) {
        final Game scenario = GameManager.getInstance().getByID(-1);
        final Region region = RegionManager.getInstance().getByID(regionId);
        final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(scenario, region);
        final StringBuffer sbuf = new StringBuffer();
        int positionY = 0;
        sbuf.append("\"");
        for (final Sector sector : sectorList) {
            if (sector.getPosition().getY() != positionY) {
                sbuf.deleteCharAt(sbuf.length() - 1);
                sbuf.append("\",\n\"");
                positionY = sector.getPosition().getY();
            }

            if (sector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                sbuf.append("  ,");

            } else {
                sbuf.append(sector.getNation().getCode());
                sbuf.append(sector.getPopulation());
                if (sector.getProductionSite() != null) {
                    if (sector.getTradeCity()) {
                        sbuf.append('$');
                    } else {
                        sbuf.append(sector.getProductionSite().getCode());
                    }
                }
                sbuf.append(",");
            }
        }
        sbuf.deleteCharAt(sbuf.length() - 1);
        sbuf.append("\"\n");

        System.out.println(sbuf.toString());
    }

    /**
     * Convert the sector data into the static table used by the sector initializer.
     *
     * @param regionId the region to extract.
     */
    public void extractRegional(final int regionId) {
        final Game scenario = GameManager.getInstance().getByID(-1);
        final Region region = RegionManager.getInstance().getByID(regionId);
        final List<Sector> sectorList = SectorManager.getInstance().listByGameRegion(scenario, region);
        final StringBuffer sbuf = new StringBuffer();
        int positionY = 0;
        sbuf.append("\"");
        for (final Sector sector : sectorList) {
            if (sector.getPosition().getY() != positionY) {
                sbuf.deleteCharAt(sbuf.length() - 1);
                sbuf.append("\",\n\"");
                positionY = sector.getPosition().getY();
            }

            if (sector.getTerrain().getId() == TerrainConstants.TERRAIN_O) {
                if (sector.getNaturalResource() != null && sector.getNaturalResource().getId() == NaturalResourcesConstants.NATRES_FISH) {
                    sbuf.append(" f,");
                } else {
                    sbuf.append("  ,");
                }

            } else {
                if (sector.getPoliticalSphere() == Character.UNASSIGNED) {
                    sbuf.append("?");
                } else {
                    // Simplification of political spheres
                    sbuf.append(String.valueOf(sector.getPoliticalSphere()).toUpperCase());
                }

                sbuf.append(sector.getTerrain().getCode());

                if (sector.getNaturalResource() != null) {
                    sbuf.append(sector.getNaturalResource().getCode());
                }
                sbuf.append(",");
            }
        }
        sbuf.deleteCharAt(sbuf.length() - 1);
        sbuf.append("\"\n");

        System.out.println(sbuf.toString());
    }


    /**
     * Default execution.
     *
     * @param args no particular importance.
     */
    public static void main(final String[] args) {
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);

        // Make sure we have an active transaction
        final Session thatSession = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession();
        final Transaction thatTrans = thatSession.beginTransaction();

        SectorTester stest = new SectorTester();
        stest.extractPolitical(EUROPE);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractRegional(EUROPE);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractPolitical(CARIBBEAN);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractRegional(CARIBBEAN);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractPolitical(INDIES);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractRegional(INDIES);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractPolitical(AFRICA);

        System.out.println("-------------------");
        System.out.println("-------------------");
        System.out.println("-------------------");

        stest.extractRegional(AFRICA);

        thatTrans.rollback();
    }

}
