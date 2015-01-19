package com.eaw1805.core.initializers.scenario1804.map;

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
            "G3,G1,G2,  ,  ,  ,  ,G1,G1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1,D3,D1,  ,  ,  ,S4,S1,  ,  ,  ,  ,  ",
            "G1,G1,  ,  ,  ,  ,G1,G1,G3,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,D4,D1,  ,D1,D7$,  ,  ,  ,  ,S2,  ,  ,  ",
            "G1,G2,  ,  ,  ,G1,G1,G1,G1,G4,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D1&,  ,  ,D2,  ,  ,  ,  ,  ,  ,  ,  ",
            "G4,  ,  ,  ,G0,G1,G2,G2,G1,G1,G1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,D2,D1,  ,  ,  ,S1,  ,  ,  ,  ,  ,  ",
            "G1&,  ,  ,  ,G1,G0,G1,G1,G4,G1,G2,G1,  ,  ,  ,  ,H1,H1,H2,B2,B2&,B6,D3,D2,  ,  ,  ,  ,P3,P2,P2,P4,  ",
            "  ,  ,  ,  ,  ,G1,G3,G1,G1,G2,G7$,  ,  ,  ,  ,H6$,  ,H3,H2,H2,B1,B2,B2,D1,D1,P1,P2,P4,P2,P1,P2,P2,P2",
            "  ,  ,  ,G2,G1,G1,  ,G1,G1,G1,G3,G1,  ,  ,  ,H6,H1,H1,H1,H4,B1,B1,B3,B3,P1,P2,P1,P1,P1,P2,P1,P2,P1",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,H1,H1,H1,H1,H2,B2,B1,B1,B1,P1,P1,P1,P6$,P1,P1,P0,P1,P4,P1",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,F0,H4,H1,H1,H3,H2,H3,H1,B3,B0,B2,P3,P4,P1,P1,P1,P1,P1,P1,P2,P1",
            "  ,  ,  ,  ,  ,  ,F2,F5,  ,  ,  ,F1,F1,H1,H2,H1,H2,H2,H1,H3,B1,B1,B1,B3,B2,P4,P1,P1,P1,P1,P1,P1,P3",
            "  ,  ,  ,F1,F2,  ,F1,F1,F1,F1,F5,F1,F1,F2,H1,H1,H4,H2,H1,H1,B3,B3,B1,B1,B2,B2,P2,P1,P2,P1,P1,A2,P1",
            "  ,  ,F3&,F2,F1,F1,F2,F2,F2,F1,F1,F0,F1,F1,H3,H3,H5,H3,B1,H3,B1,B1,B2,B4,B3,B1,A1,A1,A1,A3,A0,A1,A0",
            "  ,  ,  ,  ,F2,F1,F1,F1,F1,F2,F8$,F1,F1,F2,F1,H4,H1,H1&,H4,B5,B2,B3,B6$,B2,B1,A2,A1,A1,A0,A0,A0,A1,A1",
            "  ,  ,  ,  ,  ,F1,F1,F2,F1,F1,F2,F1,F1,F2,F3,F2,F2,H1,B2,B2,B2,B3,B2,B1,A1,A1,A2,A6,A1,A4,A1,A1,A1",
            "  ,  ,  ,  ,  ,  ,F2,F1,F0,F1,F2,F4,F1,F1,F1,F3,F1,B2,B1,B1,  ,B2,B1,A1,A0,A0,A2,A2,A7$,A1,A1,A1,A1",
            "  ,  ,  ,  ,  ,  ,F1,F1,F0,F1,F2,F4,F2,F1,F1,F1,F1,B4,I2,I2,I2,I1,A1,A1,A1,A2,A1,A1,A3,A1,A0,A1,A2",
            "  ,  ,  ,  ,  ,F1,F4,F1,F1,F2,F1,F2,F1,F1,F0,F3,  ,I2,I1,I1,I1,I1,I1,A1,I0,A2,A0,A1,A1,A2,A1,A1,A2",
            "  ,  ,  ,  ,  ,F1,F1,F1,F1,F1,F2,F1,F3,F1,F1,F0,I1,I1,I2&,I2,I4,I2,I1,I2,A1,I1,I1,I2,A0,A1,A1,A1,A2",
            "E1,E1,  ,  ,  ,F2,F1,F1,F2,F1,F1,F1,F1,F1,F1,I1,I5,I1,I1,I1,I2,I2,I2,  ,  ,A3&,  ,I2,A1,A1,A2,A1,A3",
            "E1,E1,E1,E0,E3,F1,F2,F2,F0,F2,F1,F1,F1,F1,F2,I2,I2,I2,I4,I1,I2,I2,I2,  ,  ,  ,  ,I5,I0,A1,A2,A1,A0",
            "E1,E1,E2,E0,E1,F1,F1,F1,F1,F0,F0,F1,F2,F0,F1,F1,I2,  ,  ,  ,I3,I2,I0,I2,  ,  ,  ,  ,I2,I2,A1,A0,A2",
            "E1,E1,E1,E1,E1,E0,F2,F1,F1,F1,F1,F3,  ,  ,F5&,F2,  ,  ,  ,  ,I2,I1,I1,I1,I2,  ,  ,  ,  ,I0,A5,A1,A0",
            "E1,E1,E1,E2,E1,E1,E0,E0,E0,F0,F0,F1,  ,  ,  ,  ,  ,  ,  ,  ,I1,I1,I1,I2,N1,  ,  ,  ,  ,  ,  ,  ,A2",
            "E2,E1,E1,E2,E1,E1,E0,E0,E0,F0,F0,F5,  ,  ,  ,  ,  ,I1,  ,  ,I1,I5,I1,N2,N1,  ,  ,  ,  ,  ,  ,  ,A2",
            "E1,E2,E3,E1,E1,E1,E1,E1,F0,F0,F1,  ,  ,  ,  ,  ,  ,I2,  ,  ,  ,I1,I2,I2,N3,N2,N1,  ,  ,  ,  ,  ,  ",
            "E1,E1,E5,E1,E1,E1,E1,E4,  ,  ,  ,  ,  ,  ,  ,  ,  ,I2,  ,  ,  ,  ,I6$,I2,N0,N1,N1,N3,  ,  ,  ,  ,  ",
            "E7$,E1,E3,E1,E1,E1&,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,I3,N1,N2,N2,N3,N4,  ,  ,  ,  ",
            "E0,E1,E1,E2,E3,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N3,N2,  ,  ,  ,  ,  ,  ,N6,N2,N1,N2,N1,N3,N1,  ,  ",
            "E2,E1,E0,E1,E1,E1,  ,  ,  ,E0,  ,G0,  ,  ,  ,  ,N2,N1,  ,  ,  ,  ,  ,  ,  ,N6$,N1,N1,N0,N1,N1,N1,  ",
            "E1,E1,E1,E1,E4,  ,  ,  ,  ,E1,  ,  ,  ,  ,  ,  ,N1&,N2,  ,  ,  ,  ,  ,  ,  ,  ,N1,N2,N2,  ,  ,  ,  ",
            "E1,E3,E1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,N0,  ,  ,  ",
            "E0,E0,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N3,N2,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,  ,  ,  ,I1",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,N1,N3,N1,N1&,N5,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,?1,  ,?3$,  ,  ,  ,  ,  ,N2,N1,N2,N1,  ,  ,  ,  ,  ,  ",
            "  ,  ,M6,M1,M1,  ,M4,M2,M3&,  ,  ,  ,  ,M1,?1,?0,?1,?1,?1,  ,  ,  ,  ,  ,N0,N1,N1,  ,  ,  ,  ,  ,  ",
            "M1,M1,M2,M1,M1,M3,M1,M1,M1,M1,M1,M1,M3,M0,?1,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,N2,  ,  ,  ,  ,  ,  ",
            "M1,M1,M1,M1,M1,M1,M0,M0,M0,M0,M0,M1,M0,M1,?0,?0,?1,?1,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  "
    };

    /**
     * Regional map of europe.
     */
    private static final String[] REGIONAL_MAP_EUROPE = { // NOPMD
            "GB,GBn,GBn,  ,  ,  ,  ,GH,GHe,GH,GH,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DB,DHv,  ,  ,  ,SB,SB,  ,  ,  ,  ,  ",
            "GBn,GBn,  ,  ,  ,  ,GH,GH,GHv,GH,GH,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DHp,  ,DH,DB,  ,  ,  ,  ,SH,  ,  ,  ",
            "GBn,GB,  ,  ,  ,GH,GHv,GHp,GH,GH,VG,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,  ,  ,DB,  ,  ,  ,  ,  ,  ,  ,  ",
            "GH,  ,  ,  ,GG,GG,GG,GH,GW,GW,GB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,DB,DBn,  ,  ,  ,SW,  ,  ,  ,  ,  ,  ",
            "GB,  ,  ,  ,GB,GG,GGe,GG,GH,GB,GB,GB,  ,  ,  ,  ,HB,HBn,HBn,BBn,BB,BB,DBn,DBn,  ,  ,  ,  ,PB,PB,PB,PB,  ",
            "  ,  ,  ,  ,  ,GB,GBn,GBn,GW,GB,GB,  ,  ,  ,  ,HB,  ,HB,HBn,HBn,BB,BW,BB,DH,DW,PB,PB,PB,PB,PW,PH,PH,PB",
            "  ,  ,  ,GH,GH,GB,  ,GBn,GH,GB,GH,GB,  ,  ,  ,HB,HB,HB,HHp,HHv,BHw,BH,BW,BB,PW,PB,PH,PH,PB,PB,PB,PB,PB",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,HB,HB,HHp,HB,HHw,BHp,BW,BB,BB,PBw,PB,PH,PB,PB,PB,PH,PB,PB,PW",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,FB,HB,HB,HB,HW,HHp,HHv,HHw,BHw,BH,BB,PBw,PB,PH,PH,PH,PS,PH,PB,PH,PB",
            "  ,  ,  ,  ,  ,  ,FBn,FB,  ,  ,  ,FB,FB,HB,HHv,HB,HB,HB,HH,HHe,BW,BB,BHv,BB,BHe,PH,PB,PW,PB,PH,PB,PW,PB",
            "  ,  ,  ,FB,FB,  ,FBn,FBn,FB,FH,FB,FB,FB,FW,HW,HB,HB,HB,HG,HG,BB,BB,BB,BH,BH,BH,PW,PH,PW,PGe,PB,AB,PG",
            "  ,  ,FB,FH,FH,FB,FB,FB,FH,FH,FB,FW,FW,FH,HW,HB,HB,HH,BH,HH,BB,BW,BB,BB,BB,BB,AW,AW,AB,AB,AH,AB,AG",
            "  ,  ,  ,  ,FB,FH,FB,FB,FHw,FB,FB,FB,FB,FW,FW,HH,HH,HB,HBn,BBn,BBn,BW,BB,BHp,BH,AH,AB,AB,AH,AH,AB,AG,AB",
            " f,  ,  ,  ,  ,FW,FB,FB,FH,FB,FB,FB,FB,FB,FHv,FH,FH,HB,BB,BBn,BB,BW,BHv,BH,AW,AW,AH,AB,AB,AB,AW,AW,AW",
            "  , f,  ,  ,  ,  ,FBn,FB,FB,FW,FW,FB,FB,FH,FHe,FH,FW,BB,BG,BX,  ,BG,BGg,AX,AG,AH,AB,AB,AB,AB,AW,AB,AB",
            "  ,  ,  ,  ,  ,  ,FBn,FBn,FB,FB,FHv,FB,FB,FW,FW,FH,FH,BB,IX,IG,IG,IGe,AX,AG,AG,AG,AG,AW,AH,AH,AB,AW,ABn",
            "  ,  ,  ,  ,  ,FHw,FB,FB,FB,FB,FB,FW,FH,FH,FB,FH,  ,IG,IW,IW,IW,IB,IH,AG,IW,AW,AH,AH,AH,AH,AW,AW,ABn",
            "  ,  ,  ,  ,  ,FH,FW,FW,FB,FB,FW,FW,FW,FB,FB,FH,IG,IG,IB,IB,IHp,IB,IB,IB,AH,IB,IHv,IH,AB,AH,AW,AB,ABn",
            "EW,EW,  ,  ,  ,FB,FB,FB,FB,FH,FW,FGw,FH,FG,FB,IH,IH,IGw,IHw,IH,IHp,IBn,IBn,  ,  ,AB,  ,IHw,AH,AB,AG,AG,AB",
            "EG,EW,EW,EH,EH,FH,FB,FB,FW,FB,FH,FG,FG,FW,FB,IH,IB,IB,IB,IB,IB,IBn,IBn,  ,  ,  ,  ,IB,IH,AH,AG,AG,AG",
            "EK,EG,EG,EG,EG,FG,FG,FG,FHv,FH,FH,FHp,FW,FB,FB,FH,IB,  ,  ,  ,IHv,IH,IB,IB,  ,  ,  ,  ,IB,IH,AG,AH,AGw",
            "EK,EH,EW,EB,EH,EK,FK,FG,FG,FG,FH,FB,  ,  ,FB,FB,  ,  ,  ,  ,IB,IB,IG,IHv,IB,  ,  ,  ,  ,IB,AH,AH,AG",
            "EGw,EH,EW,EH,EBn,EH,EH,EH,EG,FG,FH,FB,  ,  ,  ,  ,  ,  ,  ,  ,IB,IB,IHv,IG,NB,  ,  ,  ,  ,  ,  ,  ,AH",
            "EG,EH,EW,EH,EB,EH,EHp,EHp,EG,FG,FH,FB,  ,  ,  ,  ,  ,IW,  ,  ,IB,IB,IHw,NG,NB,  ,  ,  ,  ,  ,  ,  ,AH",
            "EBn,EG,EH,EW,EBn,EW,EW,EW,FW,FH,FB,  ,  ,  ,  ,  ,  ,IG,  ,  ,  ,IH,IH,IG,NH,NB,NB,  ,  ,  ,  ,  ,  ",
            "EB,EB,EG,EBn,EB,EW,EH,EH,  ,  ,  ,  ,  ,  ,  ,  ,  ,IB,  ,  ,  ,  ,IB,IB,NG,NW,NB,NB,  ,  ,  ,  ,  ",
            "EB,EH,EHv,EH,EB,EB,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,IB,NH,NG,NG,NB,NH,  ,  ,  ,  ",
            "EB,EW,EH,EG,EH,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NHp,NH,  ,  ,  ,  ,  ,  ,NB,NHp,NB,NHw,NGw,NH,NB,  ,  ",
            "EK,EG,EG,EH,EG,EB,  ,  ,  ,EH,  ,EBw,  ,  ,  ,  ,NGe,NB,  ,  ,  ,  ,  ,  ,  ,NB,NH,NGg,NG,NHv,NB,NH,  ",
            "EG,EH,EH,EBn,EB,  ,  ,  ,  ,EB,  ,  ,  ,  ,  ,  ,NG,NB,  ,  ,  ,  ,  ,  ,  ,  ,NB,NHp,NG,  ,  ,  ,  ",
            "EH,EH,EBn,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NHv,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NH,NG,  ,  ,  ",
            "EG,EGw,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NW,NW,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NH,  ,  ,  ,IH",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,NBn,NBn,NH,NB,NB,  ,  ,  ,  ,  ,  ",
            "  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,7B,  ,7B,  ,  ,  ,  ,  ,NBn,NHv,NW,NH,  ,  ,  ,  ,  ,  ",
            "  ,  ,MB,MH,MH,  ,MB,MB,MB,  ,  ,  ,  ,MB,7B,7Bn,7Bn,7B,7B,  ,  ,  ,  ,  ,NB,NW,NGw,  ,  ,  ,  ,  ,  ",
            "MB,MH,MB,MW,MB,MB,MB,MB,MB,MB,MB,MB,MB,MB,7H,7H,7Bn,7Bn,  ,  ,  ,  ,  ,  ,  ,  ,NB,  ,  ,  ,  ,  ,  ",
            "MH,MHw,MQ,MW,MH,MG,MH,MH,MG,MH,MG,MH,MG,7H,7G,7G,7B,7B,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  ,  "
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

}
