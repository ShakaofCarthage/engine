package com.eaw1805.core.initializers.scenario1802.army;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.core.initializers.scenario1802.NationsInitializer;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes Spy objects.
 */
public class SpyInitializer
        extends AbstractThreadedInitializer
        implements RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SpyInitializer.class);

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NationsInitializer.TOTAL_RECORDS * 3;

    /**
     * The spies per region.
     */
    public static final int[] TOT_SPIES = {3, 1, 1, 1};

    /**
     * Default constructor.
     */
    public SpyInitializer() {
        super();
        LOGGER.debug("SpyInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Spy> records = SpyManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("SpyInitializer invoked.");

        final Game game = GameManager.getInstance().getByID(-1);
        final List<Region> lstRegion = RegionManager.getInstance().list();
        final List<Nation> lstNations = NationManager.getInstance().list();

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);

        // Remove free nation
        lstNations.remove(0);

        // iterate through all records
        for (final Nation thisNation : lstNations) {
            int totSpies = 1;
            // iterate through all regions
            for (final Region region : lstRegion) {
                final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameRegionNation(game, region, thisNation);
                if (lstBarracks.size() > 0) {
                    // Since the nation has economy city in this region, setup spies
                    for (int i = 0; i < TOT_SPIES[region.getId() - 1]; i++) {
                        final Spy thisSpy = new Spy(); //NOPMD
                        thisSpy.setNation(thisNation);
                        thisSpy.setName("Spy " + totSpies++);
                        thisSpy.setStationary(0);

                        if (lstBarracks.size() >= totSpies) {
                            thisSpy.setPosition((Position) lstBarracks.get(totSpies - 1).getPosition().clone());
                        } else {
                            thisSpy.setPosition((Position) lstBarracks.get(0).getPosition().clone());
                        }

                        thisSpy.setColonial(region.getId() != EUROPE);
                        thisSpy.setReportBattalions("");
                        thisSpy.setReportBrigades("");
                        thisSpy.setReportShips("");
                        thisSpy.setReportNearbyShips("");
                        thisSpy.setReportTrade("");
                        thisSpy.setReportRelations(thisSpy.getNation().getId());
                        thisSpy.setCarrierInfo(emptyCarrierInfo);
                        SpyManager.getInstance().add(thisSpy);
                    }
                }
            }
        }

        LOGGER.info("SpyInitializer complete.");
    }

}
