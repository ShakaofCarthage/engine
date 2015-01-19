package com.eaw1805.events.espionage;

import com.eaw1805.core.initializers.scenario1802.army.SpyInitializer;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate Spy reports.
 */
public class GenerateSpiesEvent
        extends AbstractEventProcessor
        implements EventInterface, NationConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(GenerateSpiesEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public GenerateSpiesEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("GenerateSpiesEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Region> lstRegion = RegionManager.getInstance().list();

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);

        // identify list of nations
        final List<Nation> lstNations;
        if (getParent().getGame().getScenarioId() == HibernateUtil.DB_FREE) {
            // Process only FRANCE
            lstNations = new ArrayList<Nation>();
            lstNations.add(NationManager.getInstance().getByID(NATION_FRANCE));

        } else {
            lstNations = NationManager.getInstance().list();
            lstNations.remove(0); // Remove FREE Nation
        }

        // iterate through all records
        for (final Nation thisNation : lstNations) {
            int totSpies = getParent().getGameEngine().getGame().getTurn() + 1;
            // iterate through all regions
            for (final Region region : lstRegion) {
                final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameRegionNation(getParent().getGameEngine().getGame(), region, thisNation);
                if (lstBarracks.size() > 0) {
                    // Identify spies in this region for this nation
                    final List<Spy> lstSpies = SpyManager.getInstance().listGameRegionNation(getParent().getGameEngine().getGame(), region, thisNation);
                    int spyCount = SpyInitializer.TOT_SPIES[region.getId() - 1] - lstSpies.size();

                    // for extended spy games double the limit
                    if (getParent().getGameEngine().getGame().isExtendedEspionage()) {
                        spyCount = 2 * SpyInitializer.TOT_SPIES[region.getId() - 1] - lstSpies.size();
                    }

                    // Setup spies (if required)
                    for (int i = 0; i < spyCount; i++) {
                        final Spy thisSpy = new Spy(); //NOPMD
                        thisSpy.setNation(thisNation);
                        thisSpy.setName("New Spy " + totSpies++);
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

                        report(thisSpy.getNation(), "spy.new", "New Spy [" + thisSpy.getName() + "] was trained at " + thisSpy.getPosition().toString());
                        LOGGER.info("New Spy generated for [" + thisNation.getName() + "] at [" + region.getName() + "] at " + thisSpy.getPosition().toString());
                    }
                }
            }
        }

        LOGGER.info("GenerateSpiesEvent processed.");
    }

}
