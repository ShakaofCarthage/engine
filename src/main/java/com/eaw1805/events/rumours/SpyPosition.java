package com.eaw1805.events.rumours;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.army.SpyManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Spy;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractRumourEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Produces Spy Position rumours.
 */
public class SpyPosition
        extends AbstractRumourEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(SpyPosition.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     * @param isTrue   if the rumour is true.
     */
    public SpyPosition(final EventProcessor myParent, final Nation target, final boolean isTrue) {
        super(myParent, target, isTrue);
        LOGGER.debug("SpyPosition instantiated.");
    }

    /**
     * Process a true rumour.
     */
    protected final void produceTrue() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Pick a nation
        final Nation thisNation = pickNation(getNation());

        final List<Spy> lstSpies = SpyManager.getInstance().listGameNation(getParent().getGameEngine().getGame(), thisNation);

        if (!lstSpies.isEmpty() && thisNation != null) {
            java.util.Collections.shuffle(lstSpies);

            // produce news
            news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                    "A spy of " + thisNation.getName() + " is rumoured to hang out at " + lstSpies.get(0).getPosition().toString());
        }

        theTrans.commit();
        LOGGER.debug("SpyPosition (true rumour) processed.");
    }

    /**
     * Process a false rumour.
     */
    protected final void produceFalse() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Pick a nation
        final Nation thisNation = pickNation(getNation());

        // Locate a random region
        final List<Region> regions = RegionManager.getInstance().list();
        java.util.Collections.shuffle(regions);

        // Locate Sectors of the random nation
        final List<Sector> sectors = SectorManager.getInstance().listLandByGameRegion(getParent().getGameEngine().getGame(), regions.get(0));
        java.util.Collections.shuffle(sectors);

        // produce news
        news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                "A spy of " + thisNation.getName() + " is rumoured to hang out at " + sectors.get(0).getPosition().toString());

        theTrans.commit();
        LOGGER.debug("SpyPosition (false rumour) processed.");
    }

}
