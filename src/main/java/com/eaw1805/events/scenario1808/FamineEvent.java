package com.eaw1805.events.scenario1808;


import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Implements Famine Event for Scenario 1808.
 */
public class FamineEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            Logger.getLogger(FamineEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public FamineEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("FamineEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        final int maxEpidemics;

        int roll = getParent().getGameEngine().getRandomGen().nextInt(101) + 1;

        // Initialize summary
        final Map<Nation, Set<Sector>> summary = new HashMap<Nation, Set<Sector>>();
        final List<Nation> lstNations = NationManager.getInstance().list();
        for (final Nation nation : lstNations) {
            summary.put(nation, new HashSet<Sector>());
        }

        // Months December-January-February: +25 to the roll
        if (month == Calendar.DECEMBER || month <= Calendar.FEBRUARY) {
            roll += 25;
        }

        if (roll <= 30) {
            maxEpidemics = 0;

        } else if (roll > 30 && roll <= 60) {
            maxEpidemics = 1;

        } else if (roll > 60 && roll <= 80) {
            maxEpidemics = 2;

        } else if (roll > 80 && roll <= 90) {
            maxEpidemics = 3;

        } else if (roll > 90 && roll <= 95) {
            maxEpidemics = 4;

        } else if (roll > 95 && roll <= 99) {
            maxEpidemics = 5;

        } else {
            maxEpidemics = 6;
        }

        // Check regions and create more epidemics
        final Region thisRegion = RegionManager.getInstance().getByID(EUROPE);
        if (maxEpidemics > 0) {
            // Choose random sectors
            final List<Sector> sectorList = SectorManager.getInstance().listLandByGameRegion(getParent().getGameEngine().getGame(), thisRegion);
            java.util.Collections.shuffle(sectorList);

            if (!sectorList.isEmpty()) {
                for (int thisEpidemic = 0; thisEpidemic < maxEpidemics; thisEpidemic++) {
                    // Pick first sector
                    final Sector thisSector = sectorList.remove(0);

                    // update summary
                    summary.get(thisSector.getNation()).add(thisSector);

                    // Set epidemic
                    applyEpidemic(thisSector);
                }
            }
        }

        // Report summary of epidemics
        for (final Nation nation : lstNations) {
            final Set<Sector> sectors = summary.get(nation);
            if (!sectors.isEmpty()) {
                final StringBuilder stringBuilder = new StringBuilder();
                for (final Sector sector : sectors) {
                    stringBuilder.append(sector.getPosition());
                    stringBuilder.append(", ");
                }
                final String sectorNames = stringBuilder.substring(0, stringBuilder.length() - 2);

                // Prepare report
                String owner = "";
                if (nation.getId() > -1) {
                    owner = " owned by " + nation.getName();
                }

                // Add first entry as base news entry for all other entries
                newsGlobal(nation, NEWS_WORLD, false, "Epidemic spread at " + sectorNames + owner, "Epidemic spread at " + sectorNames + owner);
            }
        }

        LOGGER.info("FamineEvent processed.");
    }

    /**
     * Apply the epidemic rules to this sector.
     *
     * @param thisSector the sector to apply the epidemic.
     */
    private void applyEpidemic(final Sector thisSector) {
        // Set epidemic counter
        thisSector.setEpidemic(true);

        // Population of the coordinate drops by [1]
        if (thisSector.getPopulation() > 0) {
            thisSector.setPopulation(thisSector.getPopulation() - 1);
        }

        // Check if sector has barracks
        final boolean hasBarracks = thisSector.hasBarrack();

        // percentage of loss
        final double rate, rateBarracks;
        if (thisSector.getPosition().getRegion().getId() == EUROPE) {
            rate = getParent().getGameEngine().getRandomGen().nextInt(3) + 2d;
            rateBarracks = getParent().getGameEngine().getRandomGen().nextInt(3) + 1d;

        } else {
            rate = getParent().getGameEngine().getRandomGen().nextInt(3) + 3d;
            rateBarracks = getParent().getGameEngine().getRandomGen().nextInt(3) + 2d;
        }

        // keep track of total losses
        int totLosses = 0;

        // Locate armies in this sector
        final List<Brigade> brigadeList = BrigadeManager.getInstance().listByPosition(thisSector.getPosition());

        for (final Brigade brigade : brigadeList) {
            double thisRate = rate;
            // Check if brigade is inside barracks or not
            if (hasBarracks && brigade.getNation().getId() == thisSector.getNation().getId()) {
                thisRate = rateBarracks;
            }

            // Apply losses
            for (final Battalion battalion : brigade.getBattalions()) {
                final int losses = (int) Math.ceil((battalion.getHeadcount() * thisRate) / 100d);
                totLosses += losses;

                battalion.setHeadcount(battalion.getHeadcount() - losses);
            }

            BrigadeManager.getInstance().update(brigade);
        }

        // Prepare report
        String owner = "";
        if (thisSector.getNation().getId() > -1) {
            owner = " owned by " + thisSector.getNation().getName();
        }

        String lostStr = "";
        if (totLosses > 0) {
            lostStr = ". A total of " + totLosses + " soldiers starved to death due to famine.";
        }

        // Report epidemic
        report(thisSector.getNation(), "sector.famine", "Famine spread at sector " + thisSector.getPosition().toString() + owner + lostStr);

        // Report analytic news section only if there were losses
        if (totLosses > 0) {

            // Add first entry as base news entry for all other entries
            newsGlobal(thisSector.getNation(), NEWS_WORLD, false,
                    "Famine spread at sector " + thisSector.getPosition().toString() + owner + lostStr,
                    "Famine spread at sector " + thisSector.getPosition().toString() + owner + lostStr);
        }

        LOGGER.info("Famine spread at sector " + thisSector.getPosition().toString() + owner + lostStr);

        SectorManager.getInstance().update(thisSector);
    }

}
