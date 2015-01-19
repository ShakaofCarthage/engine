package com.eaw1805.events.rumours;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractRumourEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Produces Naval Position rumours.
 */
public class NavalPosition
        extends AbstractRumourEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(NavalPosition.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     * @param isTrue   if the rumour is true.
     */
    public NavalPosition(final EventProcessor myParent, final Nation target, final boolean isTrue) {
        super(myParent, target, isTrue);
        LOGGER.debug("NavalPosition instantiated.");
    }

    /**
     * Process a true rumour.
     */
    protected final void produceTrue() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Pick a nation
        final Nation thisNation = pickNation(getNation());

        // Locate battalions
        final Map<Sector, BigInteger> positions = ShipManager.getInstance().countShips(getParent().getGameEngine().getGame(), thisNation);

        // If there are no ships to complete the criteria for this country, no rumour is produced.
        if (!positions.isEmpty() && thisNation != null) {
            final List<Sector> sectors = new ArrayList<Sector>(positions.keySet());
            java.util.Collections.shuffle(sectors);

            if (!sectors.isEmpty()) {
                // produce news
                if (sectors.get(0).getPosition() == null) {
                    LOGGER.fatal(sectors.get(0).getId() + " position is missing !");

                } else {
                    news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                            "Merchants have seen the colors of a fleet of " + thisNation.getName() + " at " + sectors.get(0).getPosition().toString());
                }
            }
        }

        theTrans.commit();
        LOGGER.debug("NavalPosition (true rumour) processed.");
    }

    /**
     * Process a false rumour.
     */
    protected final void produceFalse() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Pick a nation
        final Nation thisNation = pickNation(getNation());

        // Locate Sectors of the random nation
        final List<Sector> sectors = SectorManager.getInstance().listSeaByGame(getParent().getGameEngine().getGame());
        java.util.Collections.shuffle(sectors);

        // produce news
        news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                "Merchants have seen the colors of a fleet of " + thisNation.getName() + " at " + sectors.get(0).getPosition().toString());

        theTrans.commit();
        LOGGER.debug("NavalPosition (false rumour) processed.");
    }

}
