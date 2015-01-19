package com.eaw1805.events.rumours;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractRumourEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Produces Building Fortress rumours.
 */
public class BuildFortress
        extends AbstractRumourEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BuildFortress.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     * @param isTrue   if the rumour is true.
     */
    public BuildFortress(final EventProcessor myParent, final Nation target, final boolean isTrue) {
        super(myParent, target, isTrue);
        LOGGER.debug("BuildFortress instantiated.");
    }

    /**
     * Process a true rumour.
     */
    protected final void produceTrue() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final Game thisGame = getParent().getGameEngine().getGame();

        // Find build orders
        final List<PlayerOrder> lstBuildOrders = PlayerOrderManager.getInstance().listBuildFortress(thisGame, thisGame.getTurn());

        if (!lstBuildOrders.isEmpty()) {
            java.util.Collections.shuffle(lstBuildOrders);

            boolean found = false;
            int retries = 0;
            while (!found && retries < lstBuildOrders.size()) {

                // Retrieve sector
                final Sector thisSector = SectorManager.getInstance().getByID(Integer.parseInt(lstBuildOrders.get(retries).getParameter1()));
                final Nation thisNation = thisSector.getNation();

                if (thisNation.getId() != getNation().getId()) {
                    found = true;

                    // produce news
                    news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                            thisNation.getName() + " is fortifying its holdings at " + thisSector.getPosition().toString());
                }

                retries++;
            }
        }

        theTrans.commit();
        LOGGER.debug("BuildFortress (true rumour) processed.");
    }

    /**
     * Process a false rumour.
     */
    protected final void produceFalse() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        // Pick a nation
        final Nation thisNation = pickNation(getNation());

        // Locate Sectors of the random nation
        final List<Sector> sectors = SectorManager.getInstance().listBarracksByGameNation(getParent().getGameEngine().getGame(), thisNation);

        if (!sectors.isEmpty()) {
            java.util.Collections.shuffle(sectors);

            // produce news
            news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                    thisNation.getName() + " is fortifying its holdings at " + sectors.get(0).getPosition().toString());
        }

        theTrans.commit();
        LOGGER.debug("BuildFortress (false rumour) processed.");
    }

}
