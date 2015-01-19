package com.eaw1805.events;

import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Implements Epidemics Events.
 */
public class EpidemicsEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(EpidemicsEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public EpidemicsEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("EpidemicsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        int maxEpidemics[] = new int[REGION_LAST + 1];
        int totEpidemics[] = new int[REGION_LAST + 1];

        // Initialize summary
        final Map<Nation, Set<Sector>> summary = new HashMap<Nation, Set<Sector>>();
        final List<Nation> lstNations = NationManager.getInstance().list();
        for (final Nation nation : lstNations) {
            summary.put(nation, new HashSet<Sector>());
        }

        // Remove old epidemics and rebellions
        SectorManager.getInstance().removeEpidemics(getParent().getGameEngine().getGame());

        // Check max number of epidemics for Europe
        if (month >= Calendar.MARCH && month <= Calendar.NOVEMBER) {
            maxEpidemics[EUROPE] = getParent().getGameEngine().getRandomGen().nextInt(4) + 6;

        } else {
            maxEpidemics[EUROPE] = getParent().getGameEngine().getRandomGen().nextInt(4) + 9;
        }

        // Check max number of epidemics for Colonies
        if (month >= Calendar.JUNE && month <= Calendar.AUGUST) {
            for (int region = CARIBBEAN; region <= AFRICA; region++) {
                maxEpidemics[region] = getParent().getGameEngine().getRandomGen().nextInt(2) + 2;
            }

        } else {
            for (int region = CARIBBEAN; region <= AFRICA; region++) {
                maxEpidemics[region] = getParent().getGameEngine().getRandomGen().nextInt(2) + 3;
            }
        }

        // Identify sectors with more than 500 battalions in Europe or 50 battalions in Colonies
        final Map<Sector, BigInteger> armyPos = BattalionManager.getInstance().listBattalions(getParent().getGameEngine().getGame());

        for (final Map.Entry<Sector, BigInteger> entry : armyPos.entrySet()) {
            final Sector thisSector = entry.getKey();
            if ((thisSector.getPosition().getRegion().getId() == EUROPE && entry.getValue().intValue() >= 500)
                    || (thisSector.getPosition().getRegion().getId() != EUROPE && entry.getValue().intValue() >= 50)) {

                // Roll dice to see if an epidemic will spread
                double target;
                if (thisSector.getPosition().getRegion().getId() == EUROPE) {
                    target = entry.getValue().intValue() / 500d;
                    if (target > 10d) {
                        target = 10d;
                    }

                } else {
                    target = entry.getValue().intValue() / 50d;
                    if (target > 10d) {
                        target = 10d;
                    }
                }

                // Check random
                if (getParent().getGameEngine().getRandomGen().nextInt(100) < target) {
                    // Increase counter
                    totEpidemics[thisSector.getPosition().getRegion().getId()]++;

                    // update summary
                    summary.get(thisSector.getNation()).add(thisSector);

                    // Set epidemic
                    applyEpidemic(entry.getKey());
                }
            }
        }

        // Check regions and create more epidemics
        for (int region = EUROPE; region <= AFRICA; region++) {
            final Region thisRegion = RegionManager.getInstance().getByID(region);
            final int epidemics = maxEpidemics[region] - totEpidemics[region];
            if (epidemics > 0) {
                // Choose random sectors
                final List<Sector> sectorList = SectorManager.getInstance().listLandByGameRegion(getParent().getGameEngine().getGame(), thisRegion);
                java.util.Collections.shuffle(sectorList);

                if (!sectorList.isEmpty()) {
                    for (int thisEpidemic = 0; thisEpidemic < epidemics; thisEpidemic++) {
                        // Pick first sector
                        final Sector thisSector = sectorList.remove(0);

                        // update summary
                        summary.get(thisSector.getNation()).add(thisSector);

                        // Set epidemic
                        applyEpidemic(thisSector);
                    }
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

        LOGGER.info("EpidemicsEvent processed.");
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
            lostStr = ". A total of " + totLosses + " soldiers were infected and died.";
        }

        // Report epidemic
        report(thisSector.getNation(), "sector.epidemic", "An epidemic spread at sector " + thisSector.getPosition().toString() + owner + lostStr);

        // Report analytic news section only if there were losses
        if (totLosses > 0) {

            // Add first entry as base news entry for all other entries
            newsGlobal(thisSector.getNation(), NEWS_WORLD, false,
                    "An epidemic spread at sector " + thisSector.getPosition().toString() + owner + lostStr,
                    "An epidemic spread at sector " + thisSector.getPosition().toString() + owner + lostStr);
        }

        LOGGER.info("An epidemic spread at sector " + thisSector.getPosition().toString() + owner + lostStr);

        SectorManager.getInstance().update(thisSector);
    }

}
