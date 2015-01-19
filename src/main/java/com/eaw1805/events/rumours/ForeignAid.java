package com.eaw1805.events.rumours;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ArmyConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.events.AbstractRumourEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces Foreign Aid rumours.
 */
public class ForeignAid
        extends AbstractRumourEventProcessor
        implements EventInterface, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ForeignAid.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     * @param isTrue   if the rumour is true.
     */
    public ForeignAid(final EventProcessor myParent, final Nation target, final boolean isTrue) {
        super(myParent, target, isTrue);
        LOGGER.debug("ForeignAid instantiated.");
    }

    /**
     * Process a true rumour.
     */
    protected final void produceTrue() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        final Game thisGame = getParent().getGameEngine().getGame();

        // Find transfer orders
        final List<PlayerOrder> lstTransferOrders = PlayerOrderManager.getInstance().listTransferOrders(thisGame);

        if (!lstTransferOrders.isEmpty()) {
            java.util.Collections.shuffle(lstTransferOrders);

            boolean found = false;
            int retries = 0;
            while (!found && retries < lstTransferOrders.size()) {
                final PlayerOrder thisOrder = lstTransferOrders.get(retries);
                final int sourceTPE = Integer.parseInt(thisOrder.getParameter1());
                final int sourceID = Integer.parseInt(thisOrder.getParameter2());
                final int targetID = Integer.parseInt(thisOrder.getParameter4());

                // Identify nation
                final Nation thisNation;
                if (sourceTPE == ArmyConstants.BARRACK) {
                    thisNation = NationManager.getInstance().getByID(sourceID);

                } else {
                    thisNation = NationManager.getInstance().getByID(targetID);
                }

                if (thisNation.getId() != getNation().getId()
                        && thisNation.getId() != thisOrder.getNation().getId()) {
                    found = true;

                    // produce news
                    news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                            "Agents report that " + thisNation.getName() + " and " + thisOrder.getNation().getName() + " exchanged resources this month.");
                }

                retries++;
            }
        }

        theTrans.commit();
        LOGGER.debug("ForeignAid (true rumour) processed.");
    }

    /**
     * Process a false rumour.
     */
    protected final void produceFalse() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();

        boolean found = false;
        int retries = 0;

        while (!found && retries < 10) {
            retries++;

            // Pick a nation
            final Nation thisNation = pickNation(getNation());

            // Retrieve relations
            final List<NationsRelation> lstRelations = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), thisNation);

            // Select all relations above neutral
            final List<NationsRelation> finalList = new ArrayList<NationsRelation>();
            for (final NationsRelation thisRelation : lstRelations) {
                if (thisRelation.getRelation() <= REL_TRADE && thisRelation.getTarget().getId() != getNation().getId()) {
                    finalList.add(thisRelation);
                }
            }

            if (!finalList.isEmpty()) {

                // Pick random relation
                java.util.Collections.shuffle(finalList);

                final NationsRelation relation = finalList.get(0);

                // produce news
                news(getNation(), thisNation, NEWS_FOREIGN, false, 0,
                        "Agents report that " + relation.getNation().getName() + " sent resources to " + relation.getTarget().getName() + " this month.");

                found = true;
            }
        }

        theTrans.commit();
        LOGGER.debug("ForeignAid (false rumour) processed.");
    }

}
