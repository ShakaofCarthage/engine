package com.eaw1805.core.initializers.scenario1802.map;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.NaturalResourceManager;
import com.eaw1805.data.managers.map.ProductionSiteManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.managers.map.TerrainManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.NaturalResource;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.ProductionSite;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.map.Terrain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.StringTokenizer;

/**
 * Initializes the Political Maps (all Regions).
 */
public class SectorInitializer
        extends AbstractThreadedInitializer
        implements RegionConstants, GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SectorInitializer.class);

    /**
     * Total number of records for default MAP table.
     */
    private static final int TOTAL_RECORDS = 7600;

    /**
     * SectorInitializer's default constructor.
     */
    public SectorInitializer() {
        super();
        LOGGER.debug("SectorInitializer instantiated.");
    }

    /**
     * Checks if the MAP table is properly initialized.
     *
     * @return true if the MAP table needs initialization.
     */
    public boolean needsInitialization() {
        return (SectorManager.getInstance().list().size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the MAP table of the database by populating it with the
     * default values for the corresponding records.
     */
    public void initialize() {
        LOGGER.debug("SectorInitializer invoked.");

        LOGGER.debug("Initializing Europe Map.");
        initMap(POLITICAL_MAP_EUROPE, REGIONAL_MAP_EUROPE, RegionManager.getInstance().getByID(EUROPE));

        LOGGER.debug("Initializing Caribbean Map.");
        initMap(POLITICAL_MAP_CARIBBEAN, REGIONAL_MAP_CARIBBEAN, RegionManager.getInstance().getByID(CARIBBEAN));

        LOGGER.debug("Initializing Indies Map.");
        initMap(POLITICAL_MAP_INDIES, REGIONAL_MAP_INDIES, RegionManager.getInstance().getByID(INDIES));

        LOGGER.debug("Initializing Africa Map.");
        initMap(POLITICAL_MAP_AFRICA, REGIONAL_MAP_AFRICA, RegionManager.getInstance().getByID(AFRICA));

        LOGGER.info("SectorInitializer complete.");
    }

    /**
     * Helper function for processing the political and regional maps.
     *
     * @param mapPolitical the array containing the Political map.
     * @param mapRegional  the array containing the Regional map.
     * @param region       the Region of the mapPolitical.
     */
    private void initMap(final String[] mapPolitical, final String[] mapRegional, final Region region) {
        final Game scenario = GameManager.getInstance().getByID(-1);
        final Nation freeNation = NationManager.getInstance().getByID(NationConstants.NATION_NEUTRAL);
        final Terrain sea = TerrainManager.getInstance().getByID(TerrainConstants.TERRAIN_O);

        // Iterate through y-Coordinates
        for (int yCoord = 0; yCoord < mapPolitical.length; yCoord++) {
            final StringTokenizer sTokPolitical = new StringTokenizer(mapPolitical[yCoord], ",");  // NOPMD
            final StringTokenizer sTokRegional = new StringTokenizer(mapRegional[yCoord], ",");  // NOPMD

            int xCoord = 0;
            // Iterate through x-Coordinates
            while (sTokPolitical.hasMoreTokens()) {
                final String tokenPolitical = sTokPolitical.nextToken(); // NOPMD
                final String tokenRegional = sTokRegional.nextToken(); // NOPMD
                final Sector sector = new Sector(); // NOPMD

                final Position thisPos = new Position(); // NOPMD
                thisPos.setX(xCoord);
                thisPos.setY(yCoord);
                thisPos.setRegion(region);
                thisPos.setGame(scenario);
                sector.setPosition(thisPos);

                sector.setConqueredCounter(0);
                sector.setPayed(true);
                sector.setConquered(false);
                sector.setBuildProgress(0);
                sector.setFow("");
                sector.setImage("");
                sector.setName("");

                // Check Political Token
                if ((tokenPolitical.length() == 1) || (tokenPolitical.equals("  "))) {
                    // Sector is sea
                    sector.setNation(freeNation);
                    sector.setPopulation(0);

                } else {
                    if (tokenPolitical.charAt(0) == '?') {
                        // This is a free sector
                        sector.setNation(freeNation);
                    } else {
                        // This is owned by a nation
                        final Nation thisNation = NationManager.getInstance().getByCode(tokenPolitical.charAt(0)); // NOPMD
                        sector.setNation(thisNation);
                    }

                    // set initial population size                    
                    sector.setPopulation(Integer.parseInt(String.valueOf(tokenPolitical.charAt(1))));

                    if (tokenPolitical.length() == 3) {
                        final ProductionSite prodSite;

                        // Check if this is a trade city
                        if (tokenPolitical.charAt(2) == '$') {
                            // EUROPE: A level 2 Fortress is available in this sector
                            // Colonies: No fortress
                            // exception: London = 1, Constantinople = 3, Cartagena = 0
                            if (region.getId() == EUROPE && xCoord == 1 && yCoord == 1) {
                                // london
                                prodSite = ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FS); // NOPMD

                            } else if (region.getId() == EUROPE && xCoord == 2 && yCoord == 1) {
                                // Constantinople
                                prodSite = ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FL); // NOPMD

                            } else if (region.getId() != EUROPE && xCoord == 3 && yCoord == 1) {
                                // Cartagena
                                prodSite = ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FL); // NOPMD

                            } else if (region.getId() != EUROPE) {
                                prodSite = ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS); // NOPMD

                            } else {
                                prodSite = ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FM); // NOPMD
                            }
                            sector.setTradeCity(true);

                        } else {
                            // A production site is available in this sector
                            prodSite = ProductionSiteManager.getInstance().getByCode(tokenPolitical.charAt(2)); // NOPMD
                        }

                        sector.setProductionSite(prodSite);

                        // Check if this is a barrack or a shipyard
                        if ((prodSite.getId() == ProductionSiteConstants.PS_BARRACKS)
                                || (prodSite.getId() == ProductionSiteConstants.PS_BARRACKS_FS)
                                || (prodSite.getId() == ProductionSiteConstants.PS_BARRACKS_FM)
                                || (prodSite.getId() == ProductionSiteConstants.PS_BARRACKS_FL)
                                || (prodSite.getId() == ProductionSiteConstants.PS_BARRACKS_FH)) {

                            // Also add Barrack
                            final Barrack thisBarrack = new Barrack();
                            thisBarrack.setNation(sector.getNation());
                            thisBarrack.setPosition((Position) sector.getPosition().clone());
                            thisBarrack.setNotSupplied(false);
                            BarrackManager.getInstance().add(thisBarrack);
                        }
                    }
                }

                // Check Regional Token
                if ((tokenRegional.length() == 1) || (tokenRegional.equals("  "))) {
                    // Sector is sea
                    sector.setTerrain(sea);

                } else if ((tokenRegional.length() == 1) || (tokenRegional.equals(" f"))) {
                    // Sector is sea
                    sector.setTerrain(sea);

                    // Set also the fishery
                    final NaturalResource natResource = NaturalResourceManager.getInstance().getByID(NaturalResourcesConstants.NATRES_FISH);
                    sector.setNaturalResource(natResource);

                } else {
                    sector.setPoliticalSphere(tokenRegional.charAt(0));

                    final Terrain thisTerrain = TerrainManager.getInstance().getByCode(tokenRegional.charAt(1)); // NOPMD
                    sector.setTerrain(thisTerrain);

                    // Sea sectors have 0 population
                    if ((thisTerrain.getId() == 12) && (sector.getPopulation() > 0)) {
                        sector.setPopulation(0);
                        LOGGER.error("Mismatch of sector reported in political & regional maps.");
                    }

                    if (tokenRegional.length() == 3) {
                        // A Natural Resource is available in this sector
                        final NaturalResource natResource = NaturalResourceManager.getInstance().getByCode(tokenRegional.charAt(2)); // NOPMD
                        sector.setNaturalResource(natResource);
                    }
                }

                xCoord++;
                SectorManager.getInstance().add(sector);
            }
        }

    }

    /**
     *
     */
    private static final String[] POLITICAL_MAP_EUROPE = { // NOPMD
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D2,  ,D1,D1,D1,D1,S1,S1,S1,S1,S2,  ,S0,S0,S0,S1,  ,S1,  ,S1,S0,  ,S1,S0,S0,  ,?0,?0,?1,?0,?0,?0,?0,  ,  ,?1,  ,?0,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D0,D2,D1,D1,D1,D1,S1,S1,S2,S1,S3,S1,S0,S1,S2,S4,  ,S0,  ,S1,S0,S0,S0,S1,S0,R0,S0,  ,  ,  ,?0,?0,?0,?0,?0,  ,  ,?0,?0,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1,D1,D1,D1,D1,D1,D2,S1,S1,S1,S2,S1,S0,S0,S1,S0,  ,  ,  ,  ,  ,  ,S0,S0,R1,  ,R0,R0,  ,R0,  ,?0,?0,?0,?0,?0,  ,  ,?0,?0,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D2,D1,D2,D3,  ,D1,S2,S1,S1,S1,S1,S1,S0,S1,S3,  ,  ,  ,  ,  ,  ,S0,S0,S1&,R1,R0,R0,R0,R0,  ,  ,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,  ,  ,  ,  ,  ,  ,  ,D2,  ,D1,D0,D1,D1,D0,D2,S1,S1,S1,S2,S1,S1,S0,  ,  ,  ,  ,  ,  ,S0,  ,  ,  ,  ,R0,R0,  ,R0,  ,  ,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?1,?0,?0,?1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1,D1,D1,D1,D1,D1,D2,S1,S1,S1,S1,S1,S2,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,R5$,R0,R0,R0,  ,R0,?0,?0,?0,?0,R0,?1,?0,?0,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G2,G1,  ,  ,  ,  ,  ,  ,  ,  ,D4,D2,D1,D2,  ,D1,D1,D1,S1,S1,S1,S2,S4,S0,  ,  ,S1,  ,  ,  ,  ,  ,  ,  ,  ,R1,R0,R0,R0,R0,R0,R1,R0,?0,?0,R0,R0,R0,R2,?0,?0,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,  ,G1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,D0,D1,D1,D1,D1,D1,D1,D2,  ,  ,S1,S1,S1,S2,S1,S4,  ,S0,  ,  ,  ,  ,  ,  ,  ,R1,R0,R0,S0,R0,R0,R0,R0,R1,R0,?1,R1,R0,R0,R0,R0,?1,?0,?1,?0,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G3,G1,G1,G0,G3,  ,  ,  ,  ,  ,  ,D1,D1,D1,D1,D2,D1,D2,D1,  ,S3,S1,S2,S1,S1,S0,S0,  ,  ,  ,  ,S0,  ,  ,R1,R1,R0,R0,R0,R2,R1,R2,R2,R0,R2,R1,R1,R0,R1,R0,R1,R0,R0,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G2,G1,G0,G2,G2,G0,G1,  ,  ,  ,  ,  ,  ,D1,D3,D1,D1,D0,D3,  ,D5,S2,S3,  ,S2,S1,S6$,S1,  ,  ,  ,  ,  ,  ,  ,R1,R2,R2,R1,  ,R1,R0,R1,R0,R0,R0,R0,R0,R1,R0,R0,R0,R0,R1,R0,R0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G0,  ,G0,G0,  ,G1&,  ,  ,  ,  ,  ,  ,  ,D4,D1,D1,D4,  ,  ,  ,D1,S1,S1,S3,  ,S2,  ,  ,  ,  ,  ,  ,S1,S1,  ,R3,R1,R1,R1,  ,R1,R0,R2,  ,R0,R0,R0,R0,R0,R0,R1,R0,R1,R1,R0,R1,R1,?1,?0,R1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,  ,G0,G0,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,D2&,D2,  ,  ,  ,  ,S1,S1,S1,S2,S1,  ,  ,  ,S1,  ,  ,  ,  ,S1,  ,  ,  ,R1,R1,R1,R1,R1,R3,R0,R0,R1,R0,R1,R0,R0,R0,R0,R1,R0,R0,R0,?1,?0,R0,?0,R0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,  ,G2,G1,G2,  ,  ,G1,G1,G3,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1,  ,S1,S1,S1,S1,  ,S1,  ,  ,S1,  ,  ,  ,  ,  ,W1&,  ,  ,R1,R1,R1,R0,W0,W1,W1,R1,  ,R1,R0,R0,R0,R1,R0,R0,R1,R1,R0,R1,R0,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,G1,G0,G3,G1,  ,  ,  ,  ,G1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D0,D2,  ,  ,S1,S2,S1,S2,  ,  ,  ,  ,  ,  ,  ,W2,W2,W1,  ,  ,W3,W1,R2,R1,R0,W0,W0,R0,R0,R0,R0,R0,R0,R0,R0,R0,R0,R0,R0,R2,?1,?0,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G0,G3,G1,G2,  ,  ,  ,  ,G1,G1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1,D3,D1,  ,  ,  ,S4,S1,  ,  ,  ,  ,  ,  ,  ,P1,P1,W1,W1,W3,W4,W0,W1,W1,R1,R3,W0,R0,R0,R0,R0,R0,R1,R0,R0,R2,R1,R1,R1,R0,?1,?0,?1,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,G1,G1,G1,  ,  ,  ,  ,G1,G1,G3,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,D4,D1,  ,D1,D7$,  ,  ,  ,  ,S2,  ,  ,  ,  ,  ,P3&,P1,P1,W1,W1,W1,W1,W1,W1,W1,R2,W1,W0,R0,R0,R2,R1,R0,R1,R1,R0,R1,R2,R1,R0,?0,?0,?1,?0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G2,G1,G1,G2,  ,  ,  ,G1,G1,G1,G1,G4,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1&,  ,  ,D2,  ,  ,  ,  ,  ,  ,  ,  ,  ,P5,P2,P2,P2,P1,W1,W1,W4,W1,W2,W3,W1,W1,W1,W1,R1,R0,R0,R2,R0,R0,R0,R5$,R1,R0,R1,R1,?0,?1,?0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,G0,G2,G1,G1,G4,  ,  ,  ,G0,G1,G2,G2,G1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D2,D1,  ,  ,  ,S1,  ,  ,  ,  ,  ,  ,  ,P1,P2,P1,P1,P1,W1,W1,W1,W2,W1,W1,W2,W0,W1,W1,R1,R1,R1,R1,R2,R2,R0,R1,R0,R0,R1,R1,?1,?0,?0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,G3,G4,G2,G1,G1&,  ,  ,  ,G1,G0,G1,G1,G4,G1,G2,G1,  ,  ,  ,  ,H1,H1,H2,B2,B2&,B6,D3,D2,  ,  ,  ,  ,P3,P2,P2,P4,  ,P2,P1,P1,P1,P1,W1,W1,W1,W4,W1,W1,W1,W1,W0,W1,W1,W0,R2,R1,R0,R0,R0,R0,R2,R0,R0,R2,R4,R0,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,G3,G1,G1,G2,G7$,  ,  ,  ,  ,H6$,  ,H3,H2,H2,B1,B2,B2,D1,D1,P1,P2,P4,P2,P1,P2,P2,P2,P1,P1,P1,W2,W2,W1,W5$,W0,W2,W1,W1,W3,W3,W1,W1,W1,W0,R0,R0,R1,R0,R2,R1,R0,R0,R1,R0,R0,R0,?1,?0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G2,G1,G1,  ,G1,G1,G1,G3,G1,  ,  ,  ,H6,H1,H1,H1,H4,B1,B1,B3,B3,P1,P2,P1,P1,P1,P2,P1,P2,P1,P1,P3,P1,W1,W2,W1,W0,W1,W0,W4,W3,W1,W0,W2,W1,W1,R1,R2,R0,R1,R1,R1,R1,R1,R1,R1,R1,R1,?0,?0,R0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H1,H1,H1,H1,H2,B2,B1,B1,B1,P1,P1,P1,P6$,P1,P1,P0,P1,P4,P1,P0,P1,P2,P0,W2,W4,W0,W1,W0,W1,W1,W0,W1,W2,W1,R1,R2,R1,R0,R0,R1,R0,R1,R2,R4,R0,R1,R0,R0,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F0,H4,H1,H1,H3,H2,H3,H1,B3,B0,B2,P3,P4,P1,P1,P1,P1,P1,P1,P2,P1,P1,P1,P2,P1,P1,W1,W1,W1,W1,W1,W0,W1,W1,W2&,W1,R1,R0,R0,R3,R0,R1,R1,R1,R1,R1,R1,R0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F2,F5,  ,  ,  ,F1,F1,H1,H2,H1,H2,H2,H1,H3,B1,B1,B1,B3,B2,P4,P1,P1,P1,P1,P1,P1,P3,P0,P2,P2,P1,P1,P1,W1,W1,W1,W4,W0,W1,W1,W1,W1,W1,R1,R0,R0,R1,R1,R1,R1,R1,R1,R0,R0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F1,F2,  ,F1,F1,F1,F1,F5,F1,F1,F2,H1,H1,H4,H2,H1,H1,B3,B3,B1,B1,B2,B2,P2,P1,P2,P1,P1,A2,P1,P2,P4,P3,P2,P2,P2,P1,W1,W2,W1,W0,W2,W3,W1,W1,R1,R1,R1,R1,R1,R1,R2,R1,R1,R0,R0,R0,R1,?1,?0,?1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F3&,F2,F1,F1,F2,F2,F2,F1,F1,F0,F1,F1,H3,H3,H5,H3,B1,H3,B1,B1,B2,B4,B3,B1,A1,A1,A1,A3,A0,A1,A0,P0,P1,P1,P1,P1,P1,W1,W1,W1,W2,W1,W1,W1,W0,R1,R1,R1,R1,R1,R3,R1,R1,R1,R1,R1,R0,R0,R1,?1,?0,?1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F2,F1,F1,F1,F1,F2,F8$,F1,F1,F2,F1,H4,H1,H1&,H4,B5,B2,B3,B6$,B2,B1,A2,A1,A1,A0,A0,A0,A1,A1,A1,A2,A1,P0,P0,A1,A0,W1,A1,A1,A1,R1,R1,R2,R1,R1,R1,R1,R1,R1,R1,R1,R2,R3,  ,R4,R0,?1,R0,?0,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F1,F1,F2,F1,F1,F2,F1,F1,F2,F3,F2,F2,H1,B2,B2,B2,B3,B2,B1,A1,A1,A2,A6,A1,A4,A1,A1,A1,A1,A1,A1,A1,A1,A0,A0,A2,A1,A1,R1,R1,R2,R1,R1,R1,R1,R2,R1,R2,?1,  ,  ,  ,R0,R0,R0,?1,?0,?0,?1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F2,F1,F0,F1,F2,F4,F1,F1,F1,F3,F1,B2,B1,B1,  ,B2,B1,A1,A0,A0,A2,A2,A7$,A1,A1,A1,A1,A1,A1,A1,A0,A0,A1,A1,A1,A4,A1,R2,R1,R1,R0,R2,R2,R1,  ,R1,R1,  ,  ,  ,R1,R0,R0,R1,?1,?0,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F1,F1,F0,F1,F2,F4,F2,F1,F1,F1,F1,B4,I2,I2,I2,I1,A1,A1,A1,A2,A1,A1,A3,A1,A0,A1,A2,A3,A1,A1,A1,A1,A1,A1,A1,A1,A1,T1,R1,R1,R1,  ,  ,  ,  ,R1,  ,  ,  ,?0,?1,R0,?0,R1,R0,?1,?1,?0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F1,F4,F1,F1,F2,F1,F2,F1,F1,F0,F3,  ,I2,I1,I1,I1,I1,I1,A1,I0,A2,A0,A1,A1,A2,A1,A1,A2,A1,A1,A2,A1,A1,A1,A1,A1,A1,A1,T1,R1,R1,  ,  ,  ,  ,R1&,R1,R1,R0,  ,?1,?1,?1,?1,?0,?0,?1,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,E4,E0,E0,E1,  ,  ,  ,  ,  ,  ,F1,F1,F1,F1,F1,F2,F1,F3,F1,F1,F0,I1,I1,I2&,I2,I4,I2,I1,I2,A1,I1,I1,I2,A0,A1,A1,A1,A2,A1,A0,A0,A0,A0,A1,A0,A1,A1,T1,T1,T3,R2,  ,  ,  ,  ,  ,R4,R1,  ,  ,  ,  ,  ,?0,?1,?1,?1,?1,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,E0,E2,E1,E1,E1,E1,E1,  ,  ,  ,F2,F1,F1,F2,F1,F1,F1,F1,F1,F1,I1,I5,I1,I1,I1,I2,I2,I2,  ,  ,A3&,  ,I2,A1,A1,A2,A1,A3,A2,A0,A1,A1,A1,A1,A2&,A1,A1,T2,T1,T1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,E1&,E0,E0,E0,E0,E0,E1,E1,E1,E0,E3,F1,F2,F2,F0,F2,F1,F1,F1,F1,F2,I2,I2,I2,I4,I1,I2,I2,I2,  ,  ,  ,  ,I5,I0,A1,A2,A1,A0,A0,A2,A4,A2,A2,A1,A1,A1,T1,T1,T1,T1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,K1,K3,K2,K2,E0,E1,E1,E1,E2,E0,E1,F1,F1,F1,F1,F0,F0,F1,F2,F0,F1,F1,I2,  ,  ,  ,I3,I2,I0,I2,  ,  ,  ,  ,I2,I2,A1,A0,A2,A0,A1,A1,A1,A1,A0,A1,T1,A1,T1,T1,T2,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,K6,K6,K2,K1,E0,E4,E0,E1,E1,E1,E1,E1,E0,F2,F1,F1,F1,F1,F3,  ,  ,F5&,F2,  ,  ,  ,  ,I2,I1,I1,I1,I2,  ,  ,  ,  ,I0,A5,A1,A0,A0,A1,A1,A2,A2,A4,A1,T1,T0,T1,T1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,K3,K3,K1,K2,E1,E1,E1,E1,E1,E1,E2,E1,E1,E0,E0,E0,F0,F0,F1,  ,  ,  ,  ,  ,  ,  ,  ,I1,I1,I1,I2,N1,  ,  ,  ,  ,  ,  ,  ,A2,A1,A0,A1,A1,A1,T2,T0,T0,T1,T0,T1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,K2,K1,K2,K3,E1,E3,E1,E2,E1,E1,E2,E1,E1,E0,E0,E0,F0,F0,F5,  ,  ,  ,  ,  ,I1,  ,  ,I1,I5,I1,N2,N1,  ,  ,  ,  ,  ,  ,  ,A2,A1,A0,A2,T1,T1,T2,T0,T0,T1,T1,T1,  ,  ,  ,  ,  ,  ,  ,  ,  ,T0,T0,T1,  ,  ,  ,  ,?0,  ,  ,?1,?1,  ,?0,?0,?0,?0",
            "  ,  ,  ,  ,K5,K1,K3,K1,E1,E3,E2,E2,E1,E2,E3,E1,E1,E1,E1,E1,F0,F0,F1,  ,  ,  ,  ,  ,  ,I2,  ,  ,  ,I1,I2,I2,N3,N2,N1,  ,  ,  ,  ,  ,  ,A0,A0,T1,A1,T1,T0,T1,T2,T3,T1,T0,T0,  ,  ,  ,  ,  ,  ,T1,T1,T1,T0,T1,T1,T1,T4,T1&,?1,?1,?1,?0,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,K2,K3,K2,K1,K2,E1,E0,E3,E1,E1,E5,E1,E1,E1,E1,E4,  ,  ,  ,  ,  ,  ,  ,  ,  ,I2,  ,  ,  ,  ,I6$,I2,N0,N1,N1,N3,  ,  ,  ,  ,  ,T1,T0,A1,T1,T1,T1,T2,T1,T1,T2,T1,T4,T7$,  ,T0,T0,T2,T1,T0,T0,T0,T0,T2,T1,T1,T1,T3,T1,?1,?1,?1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,K7$,K1,K1,K2,K1,E1,E3,E1,E7$,E1,E3,E1,E1,E1&,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,I3,N1,N2,N2,N3,N4,  ,  ,  ,  ,T2,T0,T0,T1,T1,T1,T1,T2,T1,T1,T1,  ,  ,T4,T1,T1,T0,T1,T0,T0,T1,T0,T0,T0,T0,T1,T0,T0,?1,?1,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,K1,K1,K2,K2,K3,E3,E1,E1,E0,E1,E1,E2,E3,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N3,N2,  ,  ,  ,  ,  ,  ,N6,N2,N1,N2,N1,N3,N1,  ,  ,T1,T1,T0,T1,T1,T0,T4,  ,  ,  ,T0,  ,T1,T1,T0,T0,T0,T1,T0,T2,T1,T0,T1,T0,T1,T2,T0,T3,?0,?0,?0,?1,  ,?0,?0,?0,?0",
            "  ,  ,  ,  ,K6,K2,K1,K4,E2,E4,E2,E1,E2,E1,E0,E1,E1,E1,  ,  ,  ,E0,  ,G0,  ,  ,  ,  ,N2,N1,  ,  ,  ,  ,  ,  ,  ,N6$,N1,N1,N0,N1,N1,N1,  ,T1,T1,T1,T1,T1,T1,  ,  ,  ,  ,  ,T2,T1,T1,T1,T1,T0,T0,T3,T1,T1,  ,T0,T1,T0,T0,T1,T1,T1,T1,?0,  ,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,E3,E1,E1,E1,E1,E1,E1,E1,E1,E4,  ,  ,  ,  ,E1,  ,  ,  ,  ,  ,  ,N1&,N2,  ,  ,  ,  ,  ,  ,  ,  ,N1,N2,N2,  ,  ,  ,  ,  ,  ,T0,T1,T0,T1,T1,  ,  ,  ,  ,T1,T1,T0,T2,T0,T0,T1,T0,T0,T1,T1,T1,T1,T1,T0,T0,T1,T2,T1,T1,?1,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,E2,E1,E0,E1,E1,E3,E1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,N0,  ,  ,  ,  ,G1,  ,T1,T1,T0,T1,T1,  ,  ,T1,  ,T1,T1,T0,T0,T3,T0,T1,T1,T0,T1,T0,T2,T0,T1,T1,T1,T1,T0,?0,?0,?0,  ,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,G2&,  ,  ,  ,E0,E0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N3,N2,  ,  ,  ,  ,  ,T0,T1,T2,T1,T2,T1,  ,T1,  ,T4,T1,T1,T2,T1,T0,T2,T0,T1,T0,T0,T1,  ,  ,  ,T1,T1,T0,T0,T1,?1,T0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,  ,  ,  ,I1,  ,G0&,  ,T1,T1,T0,T4,  ,  ,  ,  ,  ,T1,T0,T1,T1,T0,T0,T1,T1,T3,T0,  ,  ,  ,  ,T3,T1,T1,T1,T0,T0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,  ,M4,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,N3,N1,N1&,N5,  ,  ,  ,  ,  ,  ,  ,  ,T0,T0,  ,T1&,  ,T0,  ,  ,  ,T0,  ,  ,  ,  ,T0,  ,  ,  ,  ,  ,  ,  ,  ,  ,T1,T1,T1,T0,T0,?0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,  ,  ,  ,M5,M1,M1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,  ,?3$,  ,  ,  ,  ,  ,N2,N1,N2,N1,  ,  ,  ,  ,  ,  ,  ,  ,T1,  ,  ,T0,T3,  ,  ,  ,  ,  ,  ,T1,  ,  ,  ,  ,  ,  ,  ,T0,T1,  ,  ,  ,T2&,T0,T1,T0,T0,T0,T0,?0,?0,?0,?0",
            "  ,  ,  ,  ,M1,M1,M1,M1,M1,M1,  ,  ,  ,  ,M6,M1,M1,  ,M4,M2,M3&,  ,  ,  ,  ,M1,?1,?0,?1,?1,?1,  ,  ,  ,  ,  ,N0,N1,N1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,T2,T1,T0,  ,  ,  ,  ,T0,T1,T1,T0,?1,T1,?0,?0,?0,?0,?0",
            "  ,  ,  ,M1,M0,M1,M1,M0,M2,M1,M1,M1,M1,M1,M2,M1,M1,M3,M1,M1,M1,M1,M1,M1,M3,M0,?1,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,N2,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?0,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,T1,T1,T1,T0,T2,T2,?0,?0,?0,?0,?0",
            "  ,  ,  ,M2,M2,M0,M0,M0,M1,M1,M1,M5,M1,M1,M1,M1,M1,M1,M0,M0,M0,M0,M0,M1,M0,M1,?0,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,T1,?1,T0,?0,T0,?0,?0,?0,?0",
            "  ,  ,  ,M3,M1,M1,M1,M0,M0,M0,M1,M1,M1,M0,M0,M0,M0,M3,M0,M0,M2,M1,M1,M1,M1,M2,?0,?1,?1,?1,?0,  ,  ,  ,  ,  ,  ,G3&,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,T1,?1,?1,?0,?0,T0,?0,?0,?0,?0",
            "  ,  ,  ,  ,M1,M1,M1,M0,M1,M1,M0,M2,M1,M0,M0,M0,M0,M0,M0,M0,M1,M0,M0,M0,M1,?0,?0,?0,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,?0,?0,T0,?0,?0,?0,?0,?0",
            "  ,  ,  ,  ,M5$,M2,M3,M0,M0,M0,M0,M0,M0,M0,M1,M0,M1,M0,M0,M0,?1,?1,M0,?0,?0,M0,?0,?1,?1,?1,  ,  ,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,T2,?0,T1,?0,T0,?0,?0,?0,?0",
            "  ,  ,  ,M2,M0,M4,M0,M0,M0,M2,M0,M2,M0,M0,M0,?0,M0,M0,M0,?0,?0,?1,?1,?1,M0,?0,?0,?0,?0,?1,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,  ,?0,T0,?0,?1,?0,?0,?1,?0,?0",
            "  ,  ,M1,M0,M0,M1,M1,M2,M2,M0,M0,M0,M0,M1,?0,?0,M0,M0,M0,?0,?0,?0,?1,?0,?0,?0,?1,?0,?0,?1,?1,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,?1,?0,?0,?0,?0,?0,?0,?0,?0",
            "  ,  ,M1,M0,M2,M0,M0,M1,M1,M0,?0,?0,M0,?0,?1,?1,?0,M0,?0,?0,?1,?0,?0,?0,?1,?0,?0,?0,?1,?0,?1,?0,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,?1,?0,?0,?0,?1,?0,?0,?0,?0,?0",
            "  ,M0,M0,M1,M1,M2,M2,M0,M0,M0,?0,?1,?0,?0,?0,?1,?0,?0,?1,?1,?1,?0,?1,?1,M0,?0,?0,?0,?1,?1,?1,?0,?1,?1,?1,?1,N2,N2&,N5,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,Y1,Y1,Y1,Y1,  ,  ,  ,  ,?0,?1,?1,?1,?0,?0,?1,?1,?1,?1,?0,?0,?1,?1,?0",
            "M0,M0,M0,M0,M3,M0,M0,M0,?0,?0,?1,?0,M1,?0,?0,?0,?0,?0,?1,?1,?1,?0,?0,?0,?1,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,N1,N1,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,?1,?0,Y1,Y2,Y1,Y1,Y6$,Y1,Y4,Y1,Y1,Y1,?1,?1,?1,?1,?1,?0,?1,?0,?0,?0,?0,?1,T1,?0,?0,?0,?1,?0,?0",
            "M0,M0,M0,M1,M0,M1,M1,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,?1,?1,?0,?0,?0,?0,?1,?0,?0,?1,?1,?1,?0,N2,?0,?1,  ,  ,  ,  ,  ,?1,?0,?1,?1,?0,?1,?0,Y1,Y2,Y1,Y4,Y1,Y3,Y1,Y1,Y0,Y1,?0,?1,?0,?1,?0,?0,?0,?0,?0,?1,?1,?1,?0,?1,?0,?0,?0,?0,?0",
            "M1,M1,M1,M3,M0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?1,N1,?1,  ,  ,  ,  ,?1,?1,?0,?1,?1,?0,?0,?1,?0,Y1,Y0,Y1,Y1,Y3,Y1,Y1,Y1,Y0,  ,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0",
            "M1,M0,M1,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?1,?1,?1,?0,?0,?1,?1,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,Y0,Y1,Y2,Y1,Y1,Y2,Y1,Y1,  ,  ,?0,?0,?1,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?1,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?1,?0,?1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?1,Y0,Y1,Y2,Y0,Y1,Y1,Y1,Y1,?0,  ,?0,?0,?0,?0,?0,?1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1",
            "?1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,?0,?0,?0,?1,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,Y3,Y1,Y3,Y1,Y4,Y1,Y1,Y1,?0,  ,  ,?0,?0,?0,?1,?0,?0,?0,?0,?0$,?0,?1,?0,?0,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,?0,?0,?0,Y0,Y1,Y1,Y0,Y0,Y1,Y0,Y0,?1,  ,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?1,?0,?1,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,Y0,Y0,Y1,Y1,Y3,Y1,Y4,Y0,?0,?0,  ,  ,  ,  ,?1,?0,?1,?1,?1,?0,?0,?1,?0,?0,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?1,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?1,?1,?1,Y0,Y0,Y0,Y2,Y1,Y0,Y1,Y1,?0,?1,?0,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?1",
            "?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?1,?1,Y1,Y0,Y0,Y2,Y1,Y1,Y0,Y1,Y0,?1,?1,  ,  ,  ,  ,?1,?0,?0,?0,?1,?0,?0,?0,?0,?1,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?1,?0,?0,?0,?0,?0,?0,Y1,Y1,Y0,Y1,Y2,Y2,Y0,Y1,?0,?1,?0,  ,  ,  ,  ,?1,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,Y1,Y0,Y3,Y1,Y0,Y1,Y0,?1,?0,?0,?0,  ,  ,  ,  ,?0,?0,?0,?0,?1,?1,?0,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,Y0,Y0,Y0,Y0,Y0,Y1,?0,?0,?0,?1,  ,  ,  ,  ,?0,?1,?0,?0,?0,?1,?0,?0,?1,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,Y0,Y1,Y0,Y1,Y0,Y0,Y1,?1,?0,?0,  ,  ,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0,?1,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,Y0,Y0,Y0,Y0,Y0,Y0,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,Y1,Y0,Y0,Y0,Y0,Y0,?0,?1,?0,?1,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?1,?0,?1,?0,?0,?0,?0,Y0,Y0,Y1,?0,?0,?0,?1,?0,?1,  ,  ,  ,  ,?0,?1,?0,?0,?1,?0,?0,?0",
            "?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,?0"
    };

    /**
     * Regional map of europe.
     */
    private static final String[] REGIONAL_MAP_EUROPE = { // NOPMD
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  , f, f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DG,  ,DW,Dg,DW,DG,SG,SW,SW,SG,SW,  ,SGe,SS,SW,SW,  ,SW,  ,SW,SG,  ,SH,SW,SW,  ,1W,1S,1W,1W,1B,1W,1B,  ,  ,1W,  ,1W,1W,1W,1W,1W,1G,1G,1W,1W,1H,1W,1W",
            " , f, f, f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f, f,  ,  ,  ,  ,  ,  ,  ,DW,DW,DW,DGg,DW,DW,SW,SW,SG,SB,SH,SG,SW,SS,SH,SH,  ,SW,  ,SW,SW,SW,SS,SW,SB,RB,SW,  ,  ,  ,1W,1W,1W,1G,1W,  ,  ,1W,1W,1W,1W,1H,1W,1Q,1W,1Q,1Ke,1W,1G,1K",
            " ,  , f, f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f, f,  ,  ,  ,DW,DH,DW,DS,Dhe,DBw,DG,SW,SG,SW,SG,SB,SW,SH,SW,SB,  ,  ,  ,  ,  ,  ,SW,SW,RB,  ,RW,RW,  ,RS,  ,1W,1B,1W,1W,1W,  ,  ,1W,1W,1W,1W,1W,1W,1W,1G,1W,2H,1W,1K,1K",
            " ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DG,DG,DG,DW,  ,DG,SG,ST,SH,SW,SH,SQ,SQ,SH,SH,  ,  , f,  ,  ,  ,SB,SW,SH,RW,RB,RS,RB,RS,  ,  ,1B,1S,1B,1W,1W,1W,1B,1Q,1W,1Q,1Q,1W,1W,1Q,2W,1G,2K,2G,1W,1W",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,  ,  ,  ,  ,  ,  ,  ,DB,  ,DH,DH,DW,DW,DW,DH,SH,SW,SH,SB,SQ,SQ,SB,  ,  , f,  ,  ,  ,SH,  ,  ,  ,  ,RB,RB,  ,RW,  ,  ,1W,1B,1W,1B,1W,1W,1W,1W,1Q,1S,1W,1Q,1W,2W,2G,2Q,2K,2G,1H,1W",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DH,DG,DS,DW,DS,DW,DG,SG,SGe,SW,SB,SB,SB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,RB,RW,RS,RS,  ,RB,1B,1W,1B,1W,RT,1W,1W,1W,1W,1W,1W,1S,2S,2G,2W,2G,2H,1H,1K",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GG,GG,  ,  ,  ,  ,  ,  ,  ,  ,DH,DH,DW,DG,  ,DW,DW,DB,SG,SW,SW,SBn,SBn,SB,  ,  ,SH,  ,  ,  ,  ,  ,  ,  ,  ,RW,RB,RB,RB,RS,RS,RW,RW,1B,1W,RB,RT,RW,RB,1H,1W,1W,1W,1W,1W,2W,2W,2G,2G,2W,1W,1W",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,  ,GG,GG,GG,  ,  ,  ,  ,  ,  ,  ,  ,DG,DHp,DG,DGe,DW,DG,DW,DB,  ,  ,SH,SH,SW,SQ,SW,SBn,  ,SW,  ,  ,  ,  ,  ,  ,  ,RB,RW,RS,RS,RW,RW,RW,RW,RW,RW,1B,RW,RW,RW,RW,RB,1W,1B,1W,1W,2Q,2B,2G,2W,2G,2G,1W,1W",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,GH,GG,GG,GH,  ,  ,  ,  ,  ,  ,DH,DB,DHv,DG,DW,DW,DB,DW,  ,SB,SW,SW,SB,SW,SB,SBn,  ,  ,  ,  ,SH,  ,  ,RB,RW,RS,RB,RQ,RW,RB,RW,RW,RW,RW,RB,RB,RW,RW,RB,RW,RW,RW,2B,2S,2B,2Q,2Q,2W,2H,2W,2H,1H,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,GH,GHv,GH,GG,GG,GB,  ,  ,  ,  ,  ,  ,DG,DB,DBn,DG,DB,DB,  ,DH,SB,SW,  ,SW,SH,SB,SB,  ,  ,  ,  ,  ,  ,  ,RW,RB,RB,RQ,  ,RQ,RQ,RB,RW,RS,RW,RW,RW,RB,RB,RW,RW,RW,RW,RW,RW,2W,2W,2W,2Q,2Q,2G,2G,2G,1G,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GB,  ,GW,GW,  ,GH,  ,  ,  ,  ,  ,  ,  ,DH,DB,DBn,DB,  ,  ,  ,DW,SB,ST,SW,  ,SW,  ,  ,  ,  ,  ,  ,SB,SB,  ,RB,RW,RW,RW,  ,RQ,RQ,RB,  ,RW,RH,RH,RW,RB,RH,RS,RW,RW,RW,RH,RB,2W,2S,2B,2B,2T,2G,2W,2G,1H,1G",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,  ,GB,GB,GW,  ,  ,  ,  ,  ,  ,  ,  ,  ,DH,DH,  ,  ,  ,  ,SHp,SH,SW,SHp,SB,  ,  ,  ,SH,  ,  ,  ,  ,SW,  ,  ,  ,RH,RW,RH,RW,RH,RB,RW,RW,RH,RB,RB,RB,RS,RB,RS,RW,RW,RW,2W,2W,2W,2B,2W,2W,2Q,2G,2G,1H,1G",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,  ,GH,GHv,GB,  ,  ,GH,GB,GB,GH,GW,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,  ,SH,SB,SW,SW,  ,SHv,  ,  ,SH,  ,  ,  ,  ,  ,WB,  ,  ,RH,RB,RB,RHv,WH,WW,WH,RH,  ,RH,RB,RW,RS,RS,RB,RB,RB,RW,RW,RW,2B,2H,2H,2B,2Q,2G,2K,1G,1G",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GG,GH,GB,GH,GB,  ,  ,  ,  ,GW,GH,GB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DHv,  ,  ,SB,SB,SB,SHv,  ,  ,  ,  ,  ,  ,  ,WB,WB,WHp,  ,  ,WH,WB,RB,RH,RHv,WW,WW,RW,RH,RS,RW,RH,RS,RS,RB,RB,RB,RB,RW,RW,2W,2Q,2B,2B,2Q,2G,2H,1G,1G",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GW,GB,GBn,GBn,  ,  ,  ,  ,GH,GHe,GH,GH,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DB,DHv,  ,  ,  ,SB,SB,  ,  ,  ,  ,  ,  ,  ,PB,PB,WB,WHp,WB,WB,WB,WW,WB,RW,RW,WBn,RH,RH,RH,RW,RB,RW,RH,RH,RB,RB,RB,RB,RB,2B,2W,2W,2B,2W,2G,2G,2Q,1K,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,GB,GBn,GBn,  ,  ,  ,  ,GH,GH,GHv,GH,GH,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DHp,  ,DH,DB,  ,  ,  ,  ,SH,  ,  ,  ,  ,  ,PW,PW,PW,WH,WH,WS,WW,WB,WW,WW,RW,WBn,WW,RW,RH,RB,RW,RW,RB,RH,RW,RB,RB,RB,RW,2W,2B,2B,2W,2W,2Q,2Q,2W,1Q,1G",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GHp,GH,GBn,GB,  ,  ,  ,GH,GHv,GHp,GH,GH,VG,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,  ,  ,DB,  ,  ,  ,  ,  ,  ,  ,  ,  ,PB,PBv,PW,PW,PW,WW,WW,WB,WB,WB,WW,WBn,WBn,WBn,WB,RB,RW,RB,RB,RB,RH,RW,RB,RS,RB,RS,RS,2B,2B,2W,2W,2H,2K,2K,1H,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,GW,GB,GH,GH,GH,  ,  ,  ,GG,GG,GG,GH,GW,GW,GB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DBn,  ,  ,  ,SW,  ,  ,  ,  ,  ,  ,  ,PBn,PBn,PW,PW,PB,WB,WW,WH,WH,WBn,WBn,WBn,WBn,WB,WB,RW,RW,RB,RB,RW,RW,RB,RB,RS,RS,RH,RH,2H,2B,2Q,2Q,2Q,2Q,2Q,1K,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,GH,GH,GHv,GW,GB,  ,  ,  ,GB,GG,GGe,GG,GH,GB,GB,GB,  ,  ,  ,  ,HB,HBn,HBn,BBn,BB,BB,DBn,DBn,  ,  ,  ,  ,PB,PB,PB,PB,  ,PH,PBn,PBn,PB,PB,WS,WS,WS,WH,WH,WB,WBn,WW,WB,WB,WB,WB,RB,RB,RB,RW,RW,RH,RB,RB,RB,RH,RB,RB,2Q,2Q,2Q,2K,2K,2H,1K,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GB,GBn,GBn,GW,GB,GB,  ,  ,  ,  ,HB,  ,HB,HBn,HBn,BB,BW,BB,DH,DW,PB,PB,PB,PB,PW,PH,PH,PB,PB,PW,PB,WBn,WB,WW,WB,WS,WH,WHg,WH,WW,WH,WB,WB,WB,WB,RB,RW,RB,RB,RH,RW,RW,RW,RB,RW,RW,RH,2Q,2Q,2Q,2Q,2K,2Q,1H,1K",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,GH,GH,GB,  ,GBn,GH,GB,GH,GB,  ,  ,  ,HB,HB,HB,HHp,HHv,BHw,BH,BW,BB,PW,PB,PH,PH,PB,PB,PB,PB,PB,PG,PB,PBv,WB,WB,WS,WW,WW,WS,WB,WW,WW,WW,WGe,WB,WB,RB,RB,RB,RB,RB,RB,RB,RB,RB,Rhe,RW,RW,2H,2H,2Q,2H,2Q,2Q,2K,1K,1Q",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,HB,HB,HHp,HB,HHw,BHp,BW,BB,BB,PBw,PB,PH,PB,PB,PB,PH,PB,PB,PW,PB,PW,PB,PW,WW,WB,WB,WH,WW,WQ,WB,WW,WHw,WG,WS,RS,RB,RB,RB,RB,RB,RB,RB,RB,RB,RW,RW,RH,RH,2H,2Q,2Q,2Q,2H,2Q,1K,1K",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FB,HB,HB,HB,HW,HHp,HHv,HHw,BHw,BH,BB,PBw,PB,PH,PH,PH,PS,PH,PB,PH,PB,PB,PB,PHp,PHp,PH,WH,WB,WH,WW,WQ,WQ,WBw,WB,WQ,WS,RS,RW,RW,RW,RB,RB,RB,RB,RB,RB,RB,RQ,2Q,2Q,2Q,2Q,2W,2K,2K,2Q,1Q,1K",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FBn,FB,  ,  ,  ,FB,FB,HB,HHv,HB,HB,HB,HH,HHe,BW,BB,BHv,BB,BHe,PH,PB,PW,PB,PH,PB,PW,PB,PB,PW,PB,PH,PH,PW,WB,WH,WQ,WQ,WW,WB,WS,WS,WS,WS,RQ,RB,RB,RB,RB,RB,RB,RB,RB,RB,RW,2Q,2K,2K,2Q,2W,2K,2K,2K,1Q,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FB,FB,  ,FBn,FBn,FB,FH,FB,FB,FB,FW,HW,HB,HB,HB,HG,HG,BB,BB,BB,BH,BH,BH,PW,PH,PW,PGe,PB,AB,PG,PB,PB,PB,PB,PB,PH,PG,WG,WG,WS,WW,WB,WB,WS,WB,RQ,RH,RB,RB,RB,RB,RB,RB,RB,RB,RB,RB,RQ,2Q,2K,2Q,2Q,2Q,3K,2K,1B,1Q",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FB,FH,FH,FB,FB,FB,FH,FH,FB,FW,FW,FH,HW,HB,HB,HH,BH,HH,BB,BW,BB,BB,BB,BB,AW,AW,AB,AB,AH,AB,AG,PB,PGe,PG,PG,PG,PG,WW,WW,WW,WQ,WB,WB,WB,WB,RQ,RQ,RB,RB,RB,RB,RB,RB,RB,RB,RB,RB,RB,RG,2W,2Q,2Q,3Q,2G,3G,3G,1W,1Q",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FB,FH,FB,FB,FHw,FB,FB,FB,FB,FW,FW,HH,HH,HB,HBn,BBn,BBn,BW,BB,BHp,BH,AH,AB,AB,AH,AH,AB,AG,AB,AB,AB,AW,PW,PW,AX,AG,WW,AW,AW,AH,RW,RW,RB,RB,RB,RB,RB,RB,RB,RB,RB,RB,RB,  ,RB,RB,3G,RQ,3Q,3Q,3G,3Q,3B,3B,1B,1W",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,FW,FB,FB,FH,FB,FB,FB,FB,FB,FHv,FH,FH,HB,BB,BBn,BB,BW,BHv,BH,AW,AW,AH,AB,AB,AB,AW,AW,AW,AH,AW,AW,AHw,AH,AW,AX,AW,AH,AH,RH,RB,RB,RB,RB,RB,RB,RB,RB,RB,RB,  ,  ,  ,RB,RB,RH,3G,3Q,3Q,3Q,3Q,3H,3G,3B,1B,1Q",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  , f,  ,  ,  ,  ,FBn,FB,FB,FW,FW,FB,FB,FH,FHe,FH,FW,BB,BG,BX,  ,BG,BGg,AX,AG,AH,AB,AB,AB,AB,AW,AB,AB,AW,AB,AB,AW,AW,AG,AG,AH,AH,AH,RW,RB,RW,RB,RB,RB,RB,  ,RB,RB,  ,  ,  ,RB,RH,RH,RB,3W,3B,3Q,3Q,3H,3H,3H,3Q,1B,1B",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FBn,FBn,FB,FB,FHv,FB,FB,FW,FW,FH,FH,BB,IX,IG,IG,IGe,AX,AG,AG,AG,AG,AW,AH,AH,AB,AW,ABn,AW,AGe,AG,AG,AG,AS,AX,AH,AW,AW,TW,RB,RB,RB,  ,  ,  ,  ,RB,  ,  ,  ,?K,3B,RG,3H,RW,RB,3B,3B,3Q,3H,3H,3H,3H,1Q,1Q",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FHw,FB,FB,FB,FB,FB,FW,FH,FH,FB,FH,  ,IG,IW,IW,IW,IB,IH,AG,IW,AW,AH,AH,AH,AH,AW,AW,ABn,AB,AHw,AG,AG,AG,ABn,AB,AW,AH,AH,TH,RB,RB,  ,  ,  ,  ,RB,RB,RW,RGw,  ,3B,3B,3B,3G,3G,3Q,3Q,3Q,3W,3K,3K,3K,3H,1H,1H",
            " ,  ,  ,  ,  ,  ,  ,EH,EB,EH,EW,  ,  ,  ,  ,  ,  ,FH,FW,FW,FB,FB,FW,FW,FW,FB,FB,FH,IG,IG,IB,IB,IHp,IB,IB,IB,AH,IB,IHv,IH,AB,AH,AW,AB,ABn,AB,AH,AG,ABn,ABn,AB,AW,AW,AH,TB,TB,TB,RB,  ,  ,  ,  ,  ,RB,RGw,  ,  ,  ,  ,  ,3H,3H,3G,3G,3H,3W,3G,3G,3G,3K,1K,1H",
            " ,  ,  ,  ,  ,  ,  ,EH,EB,EB,EW,EW,EW,EW,  ,  ,  ,FB,FB,FB,FB,FH,FW,FGw,FH,FG,FB,IH,IH,IGw,IHw,IH,IHp,IBn,IBn,  ,  ,AB,  ,IHw,AH,AB,AG,AG,AB,AB,AG,AGg,ABn,AB,AB,AB,AW,AB,TB,TB,TB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,3H,3G,3G,3H,3G,3G,1G,1H",
            " ,  ,  ,  ,  ,  ,EH,EHv,EHv,EK,EK,EG,EG,EW,EW,EH,EH,FH,FB,FB,FW,FB,FH,FG,FG,FW,FB,IH,IB,IB,IB,IB,IB,IBn,IBn,  ,  ,  ,  ,IB,IH,AH,AG,AG,AG,AH,AB,ABn,AB,AB,AB,AB,AB,TB,TB,TB,TB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,3H,3G,3X,3G,3G,1X,1G",
            " ,  ,  ,  ,  ,  ,KH,KB,KG,KK,EH,EG,EK,EG,EG,EG,EG,FG,FG,FG,FHv,FH,FH,FHp,FW,FB,FB,FH,IB,  ,  ,  ,IHv,IH,IB,IB,  ,  ,  ,  ,IB,IH,AG,AH,AGw,AG,AH,AG,AB,AB,AB,AB,TH,AH,TH,TB,TB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,3H,3G,3X,3X,1G,1X",
            " ,  ,  ,  ,  ,KB,KB,KGe,KHp,EH,EH,EH,EK,EH,EW,EB,EH,EK,FK,FG,FG,FG,FH,FB,  ,  ,FB,FB,  ,  ,  ,  ,IB,IB,IG,IHv,IB,  ,  ,  ,  ,IB,AH,AH,AG,AG,AH,AG,AW,AW,AB,AH,TG,TH,TG,TH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,3H,3G,3W,3X,1G,1G",
            " ,  ,  ,  ,  ,KB,KB,KB,KW,EH,EH,EH,EGw,EH,EW,EH,EBn,EH,EH,EH,EG,FG,FH,FB,  ,  ,  ,  ,  ,  ,  ,  ,IB,IB,IHv,IG,NB,  ,  ,  ,  ,  ,  ,  ,AH,AG,AGe,AG,AW,AW,TH,TG,TH,TW,TBn,TH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,3H,3G,3H,3G,1G,1G",
            " ,  ,  ,  ,  ,KB,KB,KB,KW,EH,EH,EH,EG,EH,EW,EH,EB,EH,EHp,EHp,EG,FG,FH,FB,  ,  ,  ,  ,  ,IW,  ,  ,IB,IB,IHw,NG,NB,  ,  ,  ,  ,  ,  ,  ,AH,AG,AG,AG,TW,TW,TH,TG,TH,TW,TBn,TH,  ,  ,  ,  ,  ,  ,  ,  ,  ,TH,TG,TG,  ,  ,  ,  ,?B,  ,  ,4W,3W,  ,4G,3G,1G,1H",
            " ,  ,  ,  ,KB,KB,KBn,KW,EB,EB,EG,EBn,EBn,EG,EH,EW,EBn,EW,EW,EW,FW,FH,FB,  ,  ,  ,  ,  ,  ,IG,  ,  ,  ,IH,IH,IG,NH,NB,NB,  ,  ,  ,  ,  ,  ,AH,AH,TH,AH,TH,TG,TW,TB,TB,TB,TB,TB,  ,  ,  ,  ,  ,  ,TH,TH,TW,TW,TH,TH,TH,TB,TH,4H,4H,4B,4B,4G,4Q,4G,3H,1K,1H",
            " ,  ,  ,  ,KB,KB,KBn,KW,KW,EGe,EW,EB,EB,EB,EG,EBn,EB,EW,EH,EH,  ,  ,  ,  ,  ,  ,  ,  ,  ,IB,  ,  ,  ,  ,IB,IB,NG,NW,NB,NB,  ,  ,  ,  ,  ,TB,TB,AH,TG,TG,TH,TBn,TB,TH,TB,TH,TB,TB,  ,TB,TB,TH,TH,TH,TH,TH,TH,TH,TB,TB,TB,TH,TG,4H,4G,4H,4G,4G,4Q,4H,1H,1H",
            " ,  ,  ,  ,KB,KBn,KB,KHv,KW,EW,EW,EW,EB,EH,EHv,EH,EB,EB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,IB,NH,NG,NG,NB,NH,  ,  ,  ,  ,TH,TG,TG,TW,TW,TW,TB,TH,TH,TH,TB,  ,  ,TB,TH,TH,TH,TH,TH,TB,TH,TH,TG,TG,TG,TG,TG,TGe,4G,4G,4H,4G,4G,4G,4G,1Q,1H",
            " ,  ,  ,  ,KB,KHw,KHw,KHv,KHv,EB,EW,EB,EB,EW,EH,EG,EH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NHp,NH,  ,  ,  ,  ,  ,  ,NB,NHp,NB,NHw,NGw,NH,NB,  ,  ,TG,TG,TG,TW,TW,TH,TBn,  ,  ,  ,TB,  ,TB,TH,THw,TH,TG,TH,TW,TH,TG,TH,TW,TW,TH,TG,TG,TH,4B,4H,4G,4H,  ,4H,4G,1Q,1Q",
            " ,  ,  ,  ,KBn,KHp,KG,KB,EB,EH,EH,EB,EK,EG,EG,EH,EG,EB,  ,  ,  ,EH,  ,EBw,  ,  ,  ,  ,NGe,NB,  ,  ,  ,  ,  ,  ,  ,NB,NH,NGg,NG,NHv,NB,NH,  ,TH,TH,TH,TW,TW,THw,  ,  ,  ,  ,  ,TB,THw,TH,TH,TH,THg,TG,TH,TH,TH,  ,TS,TW,TH,TH,TH,TH,TW,TH,4G,  ,4H,4G,4H,1H,1H",
            " ,  ,  ,  ,  ,  ,  ,EB,EH,EB,EW,EK,EG,EH,EH,EBn,EB,  ,  ,  ,  ,EB,  ,  ,  ,  ,  ,  ,NG,NB,  ,  ,  ,  ,  ,  ,  ,  ,NB,NHp,NG,  ,  ,  ,  ,  ,  ,TG,TW,TW,TH,TB,  ,  ,  ,  ,TB,TB,TH,TH,TH,TW,TH,TW,TH,TW,TH,TB,TBn,TBn,TH,TH,TH,TH,TG,TH,4H,4H,4H,4H,1H,1K",
            " ,  ,  ,  ,  ,  ,  ,  ,EH,EH,EG,EHw,EH,EH,EBn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NHv,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NH,NG,  ,  ,  ,  ,THw,  ,TH,TH,TBn,TH,TH,  ,  ,TH,  ,TBn,TBn,TW,TW,TW,TH,TB,TH,TB,TB,TH,TBn,TBn,TB,TH,TB,TG,TG,4H,4H,4G,  ,4G,1H,1H",
            " ,  ,  ,  ,  ,  ,  ,  ,EG,  ,  ,  ,EG,EGw,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NW,NW,  ,  ,  ,  ,  ,TG,TH,TH,TBn,TBn,TH,  ,TH,  ,TH,TH,TBn,TW,TW,TB,TB,TW,TH,TB,TH,TB,  ,  ,  ,TB,TB,TG,TB,TB,4B,TG,4K,4G,1G,1G",
            " ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NH,  ,  ,  ,IH,  ,TH,  ,TG,TG,TB,TB,  ,  ,  ,  ,  ,TBn,TBn,TB,TB,TH,TB,TH,TH,TH,TB,  ,  ,  ,  ,TB,TB,TH,TG,TB,TH,4G,4G,4K,1H,1Q",
            " ,  ,  ,  ,  ,  ,  ,  ,MB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NBn,NBn,NH,NB,NB,  ,  ,  ,  ,  ,  ,  ,  ,TH,TG,  ,TH,  ,TH,  ,  ,  ,TH,  ,  ,  ,  ,TB,  ,  ,  ,  ,  ,  ,  ,  ,  ,TB,TB,TH,TG,TH,4G,4G,4G,4G,1G,1G",
            " ,  ,  ,  ,  ,  ,  ,MB,MB,MB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,7B,  ,7B,  ,  ,  ,  ,  ,NBn,NHv,NW,NH,  ,  ,  ,  ,  ,  ,  ,  ,TH,  ,  ,TH,TH,  ,  , f,  ,  ,  ,TH,  ,  ,  ,  ,  ,  ,  ,TH,TH,  ,  ,  ,TB,TH,TH,TG,TG,TK,TK,4Q,4K,1H,1G",
            " ,  ,  ,  ,MB,MB,MB,MB,MW,MH,  ,  ,  ,  ,MB,MH,MH,  ,MB,MB,MB,  ,  ,  ,  ,MB,7B,7Bn,7Bn,7B,7B,  ,  ,  ,  ,  ,NB,NW,NGw,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,TB,TG,TG,  ,  ,  ,  ,TB,TB,TH,TH,4B,TH,4K,4G,4H,1W,1H",
            " ,  ,  ,MB,MHv,MHv,MH,MBn,MH,MW,MH,MB,MB,MH,MB,MW,MB,MB,MB,MB,MB,MB,MB,MB,MB,MB,7H,7H,7Bn,7Bn,  ,  ,  ,  ,  ,  ,  ,  ,NB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,TB,TH,TG,TG,TH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,TB,TB,TB,TH,TH,TB,4Q,4K,4H,1H,1K",
            " ,  ,  ,MB,MB,MB,MBn,MG,MG,MW,MW,MB,MH,MHw,MQ,MW,MH,MG,MH,MH,MG,MH,MG,MH,MG,7H,7G,7G,7B,7B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,TB,TH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,4B,4B,TB,4W,TK,4H,TQ,4G,4G,1K,1K",
            " ,  ,  ,MB,MH,MH,MH,MW,MG,MQ,MW,MQ,MQ,MH,MG,MQ,MH,MHv,MG,MG,MG,MG,MW,MH,MS,MW,7W,7H,7B,7B,7B,  ,  ,  ,  ,  ,  ,NH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,YB,4B,TB,4B,4W,4K,4D,TQ,4Q,4K,1K,1H",
            " ,  ,  ,  ,MB,MG,MG,MG,MQ,MH,MW,MG,MHv,MQ,MG,MGe,MG,MG,MG,MG,MG,MH,MW,MH,MH,MG,7W,7G,7H,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,YB,4B,4H,4W,4G,4G,TG,4K,4G,4W,1H,1H",
            " ,  ,  ,  ,MB,MB,MB,MG,MW,MH,MH,MH,MG,MW,MG,MG,MS,MH,MG,MG,MH,MH,MS,MH,MG,MS,7Hp,7H,7H,7B,  ,  ,7H,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,YB,YB,YB,YH,TB,4G,TK,4D,TH,4K,4G,1G,1H",
            " ,  ,  ,MB,MH,MH,MK,MHv,MH,MW,MK,MH,MH,MH,MG,MW,MG,MB,MH,MH,MS,MG,MW,MS,MG,MS,7G,7G,7H,7H,7H,7H,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,YB,YB,YH,  ,4Ge,TH,4K,4D,4K,4K,4K,1H,1H",
            " ,  ,MHw,MB,MK,MG,MG,MG,MG,MG,MG,MG,MG,MH,MG,MG,MK,MG,MG,MG,MG,MH,MG,MG,MH,MH,MG,7Gw,7H,7G,7W,7W,7B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,YB,YB,4B,4H,4K,4K,4K,4K,4K,4K,4H,1H,1G",
            " ,  ,MBn,MG,MG,MG,MGg,MK,MG,MW,MG,MH,MK,MG,MH,MK,MH,MG,MW,MW,MH,MG,MB,MH,MG,MG,MG,7H,7G,7W,7B,7B,7H,7B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,YB,YB,4H,4B,4S,4K,4H,4G,4G,4Q,4Q,4D,1Q,1H",
            " ,MB,MBn,MG,MG,MG,MG,MG,MK,MG,MW,MH,MW,MG,MK,MW,MG,MG,MK,MK,MG,MW,MK,MH,MG,7H,MH,7G,7G,7H,7H,7H,7H,7B,7B,7H,7H,7B,7B,  ,  ,  ,  ,  ,  ,  ,  ,6B,6B,6B,6B,  ,  ,  ,  ,  ,  ,  ,  ,YBn,YB,YB,YBn,  ,  ,  ,  ,?B,YB,YB,YB,YH,4G,4H,4H,4G,4G,4Q,4D,4K,1Q,1Q",
            "MG,MG,MG,MG,MW,MK,MW,MW,MH,MG,MK,MH,MG,MG,MK,MG,MH,MG,MG,MG,MK,MG,MH,MB,MGg,7K,7H,7H,7X,7D,7K,7K,7K,7Kp,7B,7B,7H,7B,7W,  ,  ,  ,  ,  ,  ,  ,  ,6H,6Bv,6B,6B,6B,6K,YB,YB,YB,YB,YB,YB,YBn,YB,YH,YB,YB,YB,YB,YB,YB,YB,YK,YK,YH,YH,4K,4B,TK,4D,4Q,4Q,4Q,1G,1G",
            "MG,MG,MG,MK,MW,MB,MB,MH,MD,MK,MK,MH,MD,MD,MK,MD,MK,MK,MH,MH,MK,MK,MK,MK,MK,7K,7K,7D,7X,7D,7X,7X,7K,7K,7B,7B,7H,7W,7K,7B,7K,  ,  ,  ,  ,  ,6K,6H,6B,6Bv,6H,6B,6B,6K,YK,YK,YB,YB,YH,YBn,YB,YB,YB,YB,YB,YB,YK,YB,YB,YG,YH,4G,4G,4K,4K,4D,4B,4Q,4W,4W,1G,1G",
            "MW,MK,MW,MK,MG,MG,MG,MK,MK,MH,MK,MD,MD,MK,MK,MD,MD,MK,MK,MK,MK,MK,MD,MK,7D,7K,7D,7D,7D,7K,7D,7X,7X,7K,7K,7B,7K,7B,7B,7B,7B,  ,  ,  ,  ,6B,6B,6B,6K,6K,6X,6H,6K,6K,YK,YB,YW,YS,YB,YB,YB,YB,YK,  ,YB,YK,YK,YK,YH,YH,YG,4G,4B,4S,4K,4D,4K,4H,4H,4K,1H,1G",
            "MK,MK,MK,MG,MH,MK,MK,MK,MK,MH,MK,MK,MK,MK,MD,MK,MK,MD,MK,MD,MD,MD,MD,MK,MK,MD,7K,7K,7K,7K,7K,7D,7K,7X,7K,7H,7K,7B,7B,7B,7B,7H,  ,  ,  ,6B,6Ke,6D,6K,6X,6K,6X,6K,6D,6K,YB,YK,YS,YS,YB,YB,YB,YH,  ,  ,YB,YD,YB,YH,YG,4G,4B,4H,4K,4K,4K,4H,4K,4K,4H,1H,1H",
            "MG,MG,MG,MK,MK,MK,MD,MD,8D,MD,MH,MD,MK,MK,MK,MK,MK,MD,MD,MK,MK,MK,MK,MD,MD,7D,7D,7D,7D,7K,7K,7K,7D,7X,7X,7D,7K,7K,7B,7B,7B,7K,6H,6B,6B,6K,6B,6K,6K,6X,6X,6X,6X,6K,6S,6B,YS,YW,YK,YB,YBn,YB,YH,YB,  ,YD,YD,YK,YD,YS,YG,4H,4S,4B,4K,4H,4K,4K,4K,4K,1K,1H",
            "MG,MG,MK,MK,MK,MD,MD,MD,8D,MD,MK,MK,MK,MK,MK,MK,MK,MD,MK,MK,MK,MK,MD,MD,7D,7D,7D,7D,7D,7D,7K,7K,7K,7K,7D,7D,7D,7D,7D,7B,7B,7B,6B,6B,6B,6D,6B,6D,6X,6X,6X,6D,6H,6S,6B,6B,YH,YH,YB,YB,Ybn,YB,YB,YH,  ,  ,?Ge,?B,YK,YG,YH,YB,4B,4D,4D,4D,4H,4K,4K,4K,1K,1K",
            "MK,MK,MG,MD,MD,MD,MD,MK,8D,MK,MD,MK,MK,MK,MD,MK,MK,MK,MD,MK,MK,MK,MD,8K,7D,7D,7D,7K,7D,7K,7D,7K,7K,7K,7K,7D,7D,7D,7D,7D,7B,7B,6D,6D,6D,6D,6D,6X,6X,6X,6X,6D,6D,6H,6H,6H,YB,YB,YB,YB,YBn,YH,YB,YH,  ,  ,  ,  ,YK,YG,YH,YH,4D,4D,4D,4D,4H,4K,4K,4K,1K,1K",
            "MK,MK,MG,MG,MG,MD,MD,MD,8D,MD,MD,MK,MD,MK,MK,MH,MD,MK,MD,MK,MK,MK,8K,8D,7D,7D,7D,7D,7D,7D,7K,7D,7D,7K,7D,7D,7D,7D,7D,7D,7D,7D,6D,6X,6X,6D,6X,6X,6X,6X,6X,6D,6D,6D,6D,6D,YH,YB,YB,YB,YB,YB,YH,YH,YW,  ,  ,  ,  ,?B,YH,YH,4K,4D,4D,4D,4H,4K,4D,4D,1D,1K",
            "MK,MD,MH,MH,8D,MG,MD,MD,8D,MK,MK,MK,MK,MD,MK,MD,MH,MK,MK,MK,MK,8D,8D,8D,7D,7D,7D,7K,7D,7D,7D,7K,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,6X,6X,6X,6D,6X,6X,6X,6X,6D,6D,6D,6D,6D,6D,YH,YB,YB,YB,YB,YB,YB,YBv,YH,YB,  ,  ,  ,?B,?H,YH,4H,4K,4H,4K,4K,4D,4D,4D,1D,1K",
            "MD,MH,MD,MH,8D,MD,MG,MG,8D,MK,MK,MK,MK,MD,MK,MK,8D,MK,MK,MK,8D,8D,8D,8K,7D,7D,7D,7K,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7X,6X,6X,6D,6X,6X,6X,6X,6D,6D,6K,6K,6D,6H,6H,YD,YB,YB,YB,YS,YB,YHv,YB,YW,YG,  ,  ,  ,  ,?B,?H,4H,4W,4K,4K,4D,4D,4D,4D,1D,1K",
            "MK,MD,MD,8D,8D,MK,MG,MG,8D,8D,MK,8D,MD,MK,MD,MH,8K,MK,MK,MK,8D,8D,8D,8D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7X,6X,6D,6X,6X,6X,6X,6X,6D,6D,6D,6D,6D,6H,6K,YH,YB,YB,YB,YB,YS,YB,YB,YGe,YK,YG,  ,  ,  ,  ,?B,?H,4H,4K,4K,4K,4D,4D,4D,1Kg,1D",
            "MD,MH,MD,8D,8D,8D,8D,8D,MK,MK,8D,8K,MK,MK,MK,8D,8K,MK,8D,8D,8D,8D,8D,8D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7X,7D,7D,7D,7D,7D,6D,6X,6X,6X,6X,6X,6X,6D,6K,6D,6D,6D,6Hv,6K,YK,YK,YB,YB,YB,YS,YB,YJ,YW,YG,YW,YH,  ,  ,  ,  ,?B,?H,4H,4W,4K,4K,4D,4D,1D,1D",
            "MH,MD,MH,8D,8D,8D,8D,8K,MK,8D,8K,8K,MK,MH,MH,8K,8K,8D,8D,8D,8D,8D,8D,8D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7X,7X,7X,7D,7D,7D,7D,6D,6D,6X,6X,6X,6X,6D,6D,6D,6D,6H,6K,6D,6K,YK,YK,YB,YB,YB,YS,YB,YH,YJ,YG,YH,YD,  ,  ,  ,  ,?B,?H,?H,4H,4K,4K,4D,4D,1D,1D",
            "MD,MH,MD,MK,8D,8D,8H,8D,MK,8K,8K,8K,8D,8K,8K,8H,8K,8D,8D,8D,8D,8D,8D,8D,7D,7D,7D,7D,7D,7D,7D,7D,7D,7X,7D,7X,7H,7X,7D,7D,7D,7D,6D,6D,6D,6X,6X,6D,6D,6D,6D,6D,6K,6D,6K,6D,YH,YH,YK,YB,YS,YB,YB,YB,YB,YJ,YB,YG,  ,  ,  ,  ,  ,?B,?B,4H,4H,4K,4K,4D,1D,1D",
            "MH,MD,MB,8D,8D,8H,8D,8K,8D,8K,8K,8D,8K,8K,8K,8K,8D,8D,8H,8D,8D,8D,8D,8D,7D,7D,7D,7D,7D,7D,7D,7D,7X,7D,7X,7Hg,7X,7D,7D,7K,7X,7X,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6H,YD,YK,YHv,YB,YS,YB,YB,YB,YJ,YJ,YB,YG,YD,  ,  ,  ,  ,  ,  ,?B,4H,4H,4K,4D,1D,1D",
            "MH,MH,MH,MK,8D,8D,8D,8D,8K,8D,8K,8D,8K,8K,8D,8K,8D,8H,8H,8D,8D,8D,8K,8D,7D,7D,7D,7D,7D,7D,7D,7X,7X,7X,7H,7X,7X,7D,7K,7K,7X,7X,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6H,6K,YK,YH,YK,YB,YS,YB,YW,YH,YG,YB,YB,YD,YK,  ,  ,  ,  ,  ,?B,?B,?H,4H,4K,4K,1D,1D",
            "MH,MB,MH,8D,8H,8D,8K,8K,8K,8K,8D,8K,8K,8K,8K,8H,8G,8H,8G,8D,8H,8H,8D,8D,7D,7D,7D,7D,7D,7D,7X,7X,7X,7X,7X,7D,7D,7X,7K,7X,7X,7X,6X,6D,6D,6D,6D,6D,6D,6D,6D,6D,6K,6D,6D,6D,YH,YD,YH,YB,YS,YB,YBv,YB,YB,YB,YG,YG,YD,YK,  ,  ,  ,  ,?B,?H,?H,4H,4H,4K,1D,1D",
            "MH,MH,MB,MH,8H,8H,8D,8K,8K,8K,8G,8H,8K,8G,8D,8H,8G,8H,8H,8H,8H,8D,8H,8H,7H,7D,7D,7D,7D,7X,7X,7X,7X,7X,7X,7X,7X,7K,7H,7D,7X,7X,6K,6X,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,6D,YK,YH,YK,YH,YS,YB,YB,YBv,YH,YD,YG,YD,YD,YG,  ,  ,  ,  ,?B,?H,?H,?H,4H,4D,1D,1D"
    };

    /**
     * Political map of caribbean.
     */
    private static final String[] POLITICAL_MAP_CARIBBEAN = { // NOPMD
            "?0,?0,?0,?1,?0,?1,?0,?1,?1,?1,?0,?0,?1,?0,?1,?0,?1,?1,?0,G0,G1,G0,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,  ,  ",
            "?0,?0,?0,?1,?1,?0,?1,?0,?0,?1,?0,?0,?1,?1,?1,?0,?0,?0,?0,G0,G0,G2,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?0,?0,?0,?0,?1,?0,?0,F1,F1,F0,F0,?1,?1,?0,?0,?0,?1,?1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?0,?0,?1,?0,?1,?1,F0,F1,F1,F1,F2$,  ,  ,  ,  ,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?1,?1,?0,?1,?1,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,E0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?1,?1,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,E0,E0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?1,?0,?0,?1,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,E1,E0,  ,  ,G0,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?1,?0,?1,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G0&,  ,  ,  ,?0,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?1,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,  ,  ,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?0,?0,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,E1,E0,E0,E1,E0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?1,?1,?1,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,E1,E1,E0,E2,E1,E0,E0$,E1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?0,?1,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,E1,E0,E1,  ,  ,F0&,F2,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?0,?0,?1,?1,E0,E1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ",
            "?0,?1,?1,?1,?0,E0,E1,  ,  ,  ,  ,  ,?0,?1,  ,  ,  ,  ,G0,G1,G1,  ,  ,  ,  ,  ,?1,?0,?0,  ,  ,  ,  ,  ,D2$,  ,D1,  ,  ,  ",
            "?1,?1,?0,?0,E0,E0,E0,E1,E3$,  ,  ,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,?0,E0,E0,E2,E0,E0,E1,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D2,  ,  ,  ,  ",
            "  ,  ,  ,?0,E0,E1,E0,E1,E1,E3,E0,?0,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,E1,E0,E1,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,  ,  ,F1,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?0,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?1,?1,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,I2&,  ,  ,  ,F1,  ,H1,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,E0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,E0,  ,?0,  ,I1,  ,  ,  ,  ,H0,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,  ,  ,  ,  ,  ,  ,  ,E1$,?1,  ,?1,?1,  ,  ,  ,H0&,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?0,E1,E0,?0,  ,  ,  ,E1,E0,?1,?0,?0,?0,?1,?1,H0,H1,H0,F0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,  ,E0,?0,E1,E2,  ,E0,E0,E1,E1,?0,?0,?0,?1,?1,?0,H1,H1,F1,F1,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,E1,E0,E0,?0,?1,?1,?0,?0,?0,?0,?0,?0,?0,H0,F0,F0,K4,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?1,?1,?0,?1,?1,?1,?0,?1,?0,?1,?1,?0,?1,F1,K1,K1,K2$",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?1,?0,?1,?0,?1,?0,?0,?0,?1,?0,?0,K0,K1,K0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?2,  ,  ,?1,?0,?0,?0,?0,?1,?1,?0,?1,?1,?1,?1,?0,?1,K0,K1,K1,K1",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,?1,?1,?0,?0,?0,?1,?0,?0,?0,?1,?0,?0,?0,K0,K0,K1"
    };

    /**
     * Regional map of caribbean.
     */
    private static final String[] REGIONAL_MAP_CARIBBEAN = { // NOPMD
            "?G,?H,?D,?Ge,?W,?H,?H,?H,?W,?B,?W,?H,?S,?B,?W,?B,?H,?B,?B,?B,?Bn,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,  ,  ",
            "?G,?D,?D,?G,?W,?H,?B,?H,?B,?B,?B,?W,?S,?B,?B,?W,?W,?W,?W,?Bn,?Bn,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?D,?D,?D,?G,?W,?H,?B,?B,?W,?Bn,?Bn,?Bn,?S,?B,?W,?B,?Wc,?B,?W,?Bw,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?D,?D,?D,?D,?G,?B,?H,?B,?B,?S,?Bn,?S,?B,  ,  ,  ,  ,?B,?S,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?B,?B,?W,?G,?W,?H,?W,?B,  ,  ,  ,  ,  ,  ,  ,  ,  , f,?W,?S,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?W,?W,?W,?B,?H,?W,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?S,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?B,?Bn,?B,?H,?W,?Bn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,?B,?S,  ,  ,?B,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?G,?Ge,?G,?W,?H,?Bn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,?B,  ,  ,  ,?B,?Hz,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?H,?H,?H,?G,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bc,  ,  ,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?H,?H,?H,?W,?Bc,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?Hw,?B,?W,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?W,?W,?W,?H,?Bc,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?S,?W,?B,?B,?H,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?Gz,?G,?G,?H,?W,?Bn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?B,?B,  ,  ,?B,?B,?H,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "?G,?G,?G,?B,?H,?W,?Bn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?Ge,?H,?B,  ,  ,  ,  ,  ,  ,  ,  ",
            "?B,?B,?B,?G,?B,?H,?Bn,  ,  ,  ,  ,  ,?B,?B,  ,  ,  ,  ,?B,?H,?W,  ,  ,  ,  ,  ,?B,?H,?B,  ,  ,  ,  ,  ,?H,  ,?Bw,  ,  ,  ",
            "?H,?H,?W,?H,?G,?Gg,?W,?B,?B,  ,  ,?B,?W,?S,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,?B,?H,?H,?G,?G,?H,?B,?W,?B,?S,?S,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bw,  ,  ,  ,  ",
            "  ,  ,  ,?H,?B,?B,?B,?H,?H,?H,?Gz,?S,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?W,?B,?H,?W,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,  ,  ,?B,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?H,?W,?B,?H,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?H,?H,?W,?Gg,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,  ,  ,  ,?B,  ,?Bw,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bn,?G,?W,?H,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?He,?Bn,?H,  ,  ,  ,  ,  ,  ,  ,  ,?W,  ,?Bn,  ,?Hz,  ,  ,  ,  ,?W,  ,  ,  ,  ",
            "  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?G,?Bn,  ,  ,  ,  ,  ,  ,  ,?B,?H,  ,?W,?H,  ,  ,  ,?B,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?W,?H,?B,?B,?B,  ,  ,  ,?B,?H,?Bn,?W,?H,?Bc,?W,?B,?B,?S,?S,?B,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?B,  ,?B,?H,?W,?B,  ,?B,?B,?W,?G,?H,?H,?W,?Hw,?B,?S,?W,?B,?B,?Bn,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?H,?B,?B,?W,?S,?H,?G,?W,?W,?H,?B,?B,?B,?H,?W,?W,?H,?B,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?B,?G,?H,?S,?W,?H,?W,?B,?W,?Wc,?B,?H,?H,?H,?H,?H,?Bn,?B",
            "  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?W,?H,?W,?G,?H,?W,?B,?Wz,?W,?W,?W,?H,?W,?W,?H,?B,?Bw",
            "  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,  ,  ,?H,?Gg,?W,?Ge,?H,?W,?B,?B,?W,?W,?W,?W,?W,?W,?W,?W,?H,?W",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?W,?H,?G,?W,?H,?W,?B,?W,?W,?W,?W,?W,?W,?Gg,?W,?W,?W,?H,?H"
    };

    /**
     * Political map of indies.
     */
    private static final String[] POLITICAL_MAP_INDIES = { // NOPMD
            "?0,?0,?0,?0,?0,?0,?1,?1,?0,?1,?1,?0,?1,?0,?0,?1,?1,?0,?0,?1,?0,?0,?1,?1,?1,?1,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?1,?1,?1,  ",
            "?0,?1,?0,?0,?1,?1,?1,?0,?0,?1,?1,?0,?0,?0,?1,?0,?0,?0,?1,?1,?0,?0,?1,?0,?0,?1,?0,?0,?0,?1,?0,?0,?1,?0,?1,?1,?1,?1,  ,  ",
            "?0,?0,?0,K1,K0,?0,?1,?0,?0,?0,?1,?0,?0,?1,?0,G1,G0,G0,G1,G0,?0,?0,?0,?0,?0,?1,?1,?0,?1,?1,?0,?0,?0,?0,?1,?0,?1,?0,  ,  ",
            "  ,  ,  ,K1,K1,K0,?1,?0,?0,?0,?1,?0,?0,?0,?0,G0,G1,G0,G0,G1,G0,?1,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,  ,  ,?0,  ,  ,  ",
            "  ,  ,  ,  ,K1&,K1,?1,?0,?1,?0,?0,?0,?1,?0,?1,?0,?0,?0,G0,G1,G1&,?1,?1,?1,?0,?0,?0,?1,?1,?1,?1,?0,?0,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,G1,?1,?1,?1,?0,?0,?0,?0,?1,?0,?0,?0,F4$,G1,  ,  ,?0,?1,?0,?0,?0,?0,?0,?1,?0,?1,?1,?3$,  ,  ,  ,?1,?0,  ,  ",
            "  ,  ,  ,  ,  ,K1,?0,?1,?0,?0,?0,?1,?0,?0,?1,?0,?0,  ,  ,  ,  ,  ,?0,?0,?1,?1,?1,?1,?1,?1,?0,?0,?1,  ,  ,?1,?1,  ,  ,  ",
            "  ,  ,  ,  ,  ,?0,?0,?1,?1,?0,?0,?1,?0,?0,?1,?1,  ,  ,  ,  ,  ,  ,?0,?0,?1,?1,?1,?1,?0,?0,?1,?0,?1,?1,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,?0,?0,?0,?1,?0,?1,?1,?0,?1,?0,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?1,?1,?0,?1,?0,?0,?0,?1,?0,?1,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,G2$,?0,?1,?0,?0,?0,?1,?0,?0,F0,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?1,?0,?1,?0,?0,?1,?1,?1,?1,?0,?1,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?1,?0,?1,?1,?0,?1,?1,G0,F1,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,?0,?0,?1,?1,?0,?1,?0,?0,?0,?1,?1,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?1,?0,?1,?0,?0,?0,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,?0,?0,?0,?0,?1,?1,?0,?0,?0,?0,?1,?0,?0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,K1,K0,?1,?0,?1,?0,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,?1,?0,?0,?0,?1,?1,?0,?1,?1,?1,?0,?1,?0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,K0,?1,?1,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?1,?1,?1,?1,?1,?0,?0,?0,?0,  ,  ,  ,E2",
            "  ,  ,  ,  ,  ,  ,?0,?0,?0,?0,?0,F0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?1,?1,?0,?0,?0,?0,?0,?1,  ,  ,  ,  ,E1&",
            "  ,  ,  ,  ,  ,  ,F1,?1,?1,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N2&,  ,  ,?0,?1,?0,?0,?0,?0,?1,?0,?0,  ,  ,  ,  ,  ,E1",
            "  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,F0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,  ,?0,F0,F1,F1,?0,?0,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,M2&,  ,  ,  ,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,S2&,  ,  ,?0,  ,  ,  ,F1,F0,F1,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,?0,?1,  ,  ,G1$,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,  ,  ,  ,  ,  ,F2$,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G0,G0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,S1,  ,  ,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,  ,  ,  ,  ,  ,  ,  ,?0,?0,  ,?1,  ",
            "  ,  ,  ,  ,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,  ,  ,  ,  ,  ,  ,?0,?1,?0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H0,  ,  ,  ,?0,?1,?0,  ,  ,  ,?0,?0,?0,?0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H0,H0,  ,  ,  ,?0,?1,  ,  ,H1,H1,?0,?1,?1,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H1,H0,  ,  ,  ,  ,  ,  ,H0,H0,?1,?1,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,  ,H2$,H0,?1,?1,  ,  ,  ,  ,  ,H0,H1,  ,  ,  ,  ,B0",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,  ,  ,?1,?0,?0,  ,  ,S1,  ,  ,  ,  ,  ,  ,  ,B2&",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?0,  ,  ,  ,  ,  ,  ,E1,  ,  ,?1",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,  ,?1,?1,  ,  ,  ,  ,  "
    };

    /**
     * Regional map of indies.
     */
    private static final String[] REGIONAL_MAP_INDIES = { // NOPMD
            "?H,?D,?D,?D,?H,?He,?H,?H,?W,?H,?Gg,?G,?G,?G,?G,?G,?G,?G,?Gz,?G,?Gg,?W,?W,?W,?W,?H,?Ge,?H,?W,?W,?B,?B,?W,?W,?B,?W,?H,?H,?B,  ",
            "?D,?B,?D,?H,?W,?H,?W,?W,?B,?B,?H,?H,?H,?H,?H,?B,?B,?B,?B,?B,?B,?W,?H,?G,?G,?W,?W,?G,?G,?H,?H,?B,?W,?B,?Bc,?W,?B,?B,  ,  ",
            "?D,?B,?H,?B,?B,?S,?S,?S,?B,?B,?Hg,?H,?B,?W,?B,?B,?B,?B,?B,?H,?H,?B,?W,?H,?H,?G,?H,?W,?H,?G,?Gz,?W,?W,?B,?Bw,?Bc,?B,?B,  ,  ",
            "  ,  ,  ,?B,?S,?S,?S,?Bn,?B,?W,?H,?H,?W,?B,?B,?W,?B,?B,?B,?B,?Bn,?Bn,?Bn,?W,?H,?W,?G,?Gg,?G,?H,?W,?B,?B,?Bn,  ,  ,?B,  ,  ,  ",
            "  ,  ,  ,  ,?Bn,?Bn,?Bn,?B,?B,?H,?H,?H,?H,?H,?B,?H,?B,?B,?B,?B,?Bn,?W,?Bn,?Bn,?H,?G,?H,?H,?W,?H,?H,?W,?Bn,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,?Bn,?S,?S,?H,?H,?H,?Wc,?B,?H,?H,?W,?H,?H,?B,  ,  ,?Bn,?H,?H,?W,?W,?H,?G,?G,?Ge,?G,?H,?W,  ,  ,  ,?H,?H,  ,  ",
            "  ,  ,  ,  ,  ,?Bc,?S,?H,?W,?H,?B,?H,?W,?W,?W,?H,?H,  ,  ,  ,  ,  ,?B,?B,?H,?W,?W,?W,?W,?W,?H,?H,?Bn,  ,  ,?B,?W,  ,  ,  ",
            "  ,  ,  ,  ,  ,?Bc,?B,?H,?H,?B,?H,?B,?H,?W,?W,?H,  ,  ,  ,  ,  ,  ,?B,?B,?H,?W,?B,?B,?Bw,?W,?W,?H,?W,?Bn,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,?B,?B,?H,?Be,?H,?W,?W,?W,?H,?Bn,  ,  ,  ,  , f,  ,  ,  ,?H,?W,?Hc,?W,?B,?B,?B,?W,?W,?H,?W,?Bn,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,?B,?B,?H,?H,?B,?W,?H,?W,?H,?Bn,  ,  ,  , f,  ,  ,  ,  ,?B,?W,?H,?W,?B,?B,?W,?B,?W,?G,?H,?W,?B,  ,  ,  ,  ",
            "  ,  , f,  ,  ,  ,?B,?H,?W,?H,?H,?W,?H,?Bn,?S,  ,  ,  ,  ,  ,  ,  ,  ,?S,?W,?H,?B,?W,?B,?B,?B,?W,?He,?H,?W,?W,?H,  ,  ,  ",
            "  ,  , f,  ,  ,  ,?H,?H,?W,?W,?W,?B,?H,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?H,?G,?H,?W,?B,?W,?B,?B,?W,?H,?B,?B,?B,  ,  ,  ",
            "  ,  , f,  ,  ,  ,?H,?W,?Hz,?W,?W,?Hw,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?G,?H,?H,?B,?Be,?W,?B,?B,?H,?B,?Bc,?Bc,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?H,?H,?B,?W,?H,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?G,?W,?B,?S,?B,?B,?W,?W,?H,?Bc,  ,  ,  ,?G",
            "  ,  ,  ,  ,  ,  ,?B,?H,?W,?W,?H,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?H,?B,?B,?S,?B,?S,?B,?B,?B,  ,  ,  ,  ,?H",
            "  ,  ,  ,  ,  ,  ,?B,?H,?H,?H,?Bc,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,  ,  ,?W,?S,?B,?B,?B,?W,?S,?B,?S,  ,  ,  ,  ,  ,?Hg",
            "  ,  ,  ,  ,  ,  ,  ,?H,?H,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,  ,?B,?B,?B,?W,?B,?S,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,?B,  ,  ,  ,?Bc,?W,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,  ,  ,?Hz,  ,  ,  ,?H,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,?B,?B,  ,  ,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,  ,  ,  ,  ,  ,?B,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Hw,  ,  ,?H,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,?W,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?Hw,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?He,?B,  ,  ,  ,  ,  ,  ,  ,?B,?H,  ,?H,  ",
            "  ,  ,  ,  ,?Bc,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,  ,  ,  ,  ,  ,  ,?B,?H,?W,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,  ,  ,  ,?W,?H,?W,  ,  ,  ,?W,?H,?W,?W,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Hw,?B,  ,  ,  ,?W,?B,  ,  ,?S,?W,?Gz,?W,?W,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?S,  ,  ,  ,  ,  ,  ,?W,?H,?S,?H,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bw,  ,?G,?H,?B,?S,  ,  ,  ,  ,  ,?B,?W,  ,  ,  ,  ,?Hg",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,  ,?Ge,?H,?W,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f,  ,  ,  ,  ,  ,  ,  ,?Bz,  ,  ,?B,?B,?H,  ,  ,?Hw,  ,  ,  ,  ,  ,  ,  ,?B",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?W,?W,  ,  ,  ,  ,  ,  ,?B,  ,  ,?H",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Hz,  ,?W,?B,  ,  ,  ,  ,  "
    };

    /**
     * Political map of africa.
     */
    private static final String[] POLITICAL_MAP_AFRICA = { // NOPMD
            "  ,  ,  ,  ,  ,M1,?0,?0,?1,?0,?1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,  ,  ,?1,?0,?0,?0,?1,?0,?0,?1,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?0,?1,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,  ,  ,?0,?1,?0,?0,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,F1,?0,?1,?0,?1,?0,?0,?0,?1,?0,?0,?0,?1,  ,  ,?1,?0,?0,?1,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?2,  ",
            "  ,  ,  ,  ,  ,  ,  ,F1,F1,?0,?0,?0,?0,?1,?0,?0,?1,?0,?0,?0,?1,  ,?1,?1,?0,?0,?1,P0,  ,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?1,?0,?0,?0,?0,?0,?1,?0,?0,?1,?0,?1,?1,?2,?0,?0,P1,P1&,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?1$,?0,?0,?1,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?1,  ,?1,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,?0,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?1,?0,?0,?1,?1,?0,?0,?0,?0,?0,  ,  ,?1,?1,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?0,?0,?0,?0,?1,?0,?1,?0,?0,?1,?0,?0,?0,  ,  ,?1,?1,?0,?0,?0,  ,  ,  ,  ,  ,?1,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,S1,S1,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,  ,  ,?1,?0,?0,?2$,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,S0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?1,  ,?1,?0,?0,?0,  ,  ,?0,  ,  ,?0,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?1,?1,?0,?1,?0,?1,?1,  ,  ,  ,?0,?1,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,?1,  ,  ,  ,?0,?0,?1,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,?0,?0,?0,K0,?0,  ,  ,  ,F1,?0,?1,?1,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,F1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,K0,K1&,  ,  ,  ,F1,F0,?0,?0,?1,  ,  ,  ,  ,  ",
            "  ,G0,  ,  ,  ,  ,  ,  ,  ,F1,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,K1,  ,  ,  ,  ,F1,F1,?0,?0,?1,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,P0,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,  ,  ,  ,  ,F2&,F0,?0,?0,?0,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,P1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,  ,  ,  ,  ,  ,F0,?0,?0,?0,?0,  ,  ,  ,  ,  ,F1",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,  ,  ,?0,  ,?1,?0,?0,?0,N2,  ,  ,  ,?0,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?0,?1,  ,  ,  ,  ,?0,?0,?0,N1,N0,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?0,?0,?0,?0,?0,?0,?0,?1,?1,?1,?1,?0,?0,?1,  ,  ,  ,  ,?1,?0,?1,N1,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?1,?0,?0,?0,?0,?1,?0,?0,?1,?1,?0,?0,  ,  ,  ,  ,?0,?1,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,?1,?1,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?1,?1,?0,?0,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,?0,?0,?0,?0,?0,?0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H1,H1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H2$,H0,G0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  "
    };

    /**
     * Regional map of africa.
     */
    private static final String[] REGIONAL_MAP_AFRICA = { // NOPMD
            "  ,  ,  ,  ,  ,?B,?H,?H,?H,?J,?J,?J,?J,?J,?J,?J,?Gz,?J,?J,?J,?J,?J,?B,?J,?J,?J,?B,?J,?J,?H,?B,?B,?B,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?H,?B,?J,?D,?G,?Ge,?J,?J,?J,?J,?J,?J,?J,  ,  ,?Bc,?Bn,?J,?J,?J,?J,?B,?H,?B,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,?Bn,?B,?B,?D,?G,?G,?H,?J,?J,?J,?J,?J,?J,?J,  ,  ,?J,?B,?J,?J,?J,?H,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,?B,?H,?D,?D,?G,?J,?J,?J,?J,?J,?J,?J,?J,  ,  ,?J,?J,?H,?J,?B,?J,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,  ",
            "  ,  ,  ,  ,  ,  ,  ,?B,?B,?H,?H,?H,?H,?J,?J,?J,?H,?J,?H,?J,?W,  ,?W,?S,?J,?H,?H,?B,  ,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?B,?H,?B,?J,?J,?J,?H,?H,?J,?J,?J,?Hg,?H,?Sc,?S,?S,?J,?B,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?J,?G,?H,?J,?J,?J,?J,?H,?J,?J,?J,?B,?J,  ,?J,?W,?W,?B,?B,  ,  ,  ,  ,  ,  ,  ,?Bg,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?B,?J,?J,?K,?G,?K,?H,?Jc,?H,?J,?J,?H,?J,?B,?J,  ,  ,?B,?B,?B,?B,?Bw,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?Jn,?J,?H,?H,?K,?K,?H,?Jc,?H,?J,?J,?J,?H,?J,?J,  ,  ,?J,?B,?B,?B,?Bn,  ,  ,  ,  ,  ,?B,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,?Bn,?B,?B,?B,?J,?K,?H,?H,?H,?J,?Hc,?H,?H,?H,?W,  ,  ,?W,?J,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?H,?H,?J,?H,?B,?H,?H,?H,?H,?J,?J,?B,?B,  ,?B,?B,?H,?B,  ,  ,?B,  ,  ,?Bn,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bc,?B,?H,?H,?H,?B,?H,?H,?B,?H,?J,?B,?H,?B,?B,?B,?H,?H,?B,?B,  ,  ,  ,?B,?B,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?G,?H,?B,?H,?B,?J,?B,?B,?H,?H,?H,?B,?Gg,?G,?H,?J,?B,  ,  ,  ,?Bn,?W,?H,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?B,?H,?H,?H,?H,?H,?B,?B,?H,?H,?H,?H,?B,?B,?H,?H,?B,?B,  ,  ,  ,?Bn,?W,?G,?H,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?D,?G,?H,?B,?H,?B,?H,?H,?H,?G,?G,?H,?H,?H,?B,?B,?B,  ,  ,  ,?B,?B,?J,?G,?H,  ,  ,  ,  ,  ",
            "  ,?B,  ,  ,  ,  ,  ,  ,  ,?B,?D,?G,?H,?B,?K,?K,?G,?G,?G,?G,?G,?Gz,?H,?H,?H,?B,  ,  ,  ,  ,?B,?B,?G,?J,?H,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?D,?D,?D,?K,?K,?D,?D,?B,?D,?D,?D,?D,?H,?H,?H,  ,  ,  ,  ,?B,?B,?J,?J,?H,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?H,?D,?K,?K,?D,?D,?D,?D,?D,?D,?D,?H,?H,?H,  ,  ,  ,  ,  ,?B,?W,?J,?Gz,?B,  ,  ,  ,  ,  ,?B",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?D,?G,?K,?K,?D,?D,?D,?D,?D,?D,?H,?B,?B,?B,  ,  ,?B,  ,?Bc,?J,?J,?H,?H,  ,  ,  ,?Bc,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?B,?D,?G,?K,?D,?D,?D,?D,?D,?D,?H,?B,?Bn,?B,  ,  ,  ,  ,?H,?J,?J,?H,?B,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?D,?G,?K,?K,?D,?D,?D,?K,?K,?K,?B,?Bn,?B,  ,  ,  ,  ,?J,?J,?J,?H,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?H,?D,?H,?H,?D,?Kg,?D,?K,?G,?G,?H,?B,  ,  ,  ,  ,?Bc,?H,?J,?H,?H,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?H,?B,?D,?B,?H,?D,?K,?Gz,?H,?H,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?B,?B,?H,?H,?B,?H,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?Bw,?B,?H,?W,?Bw,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f, f,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?W,?H,?Bw,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f, f,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?B,?B,?B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  , f, f,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  "
    };


}
