package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Target army federation in the colonies raided by natives.
 * Attricion rate x5 for this turn, reported as casualties.
 * If no applicable army, re-roll the event.
 */
public class RaidRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RaidRandomEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RaidRandomEvent(final EventProcessor myParent) {
        super(myParent, 17, 2);
        LOGGER.debug("RaidRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        final List<Army> lstArmies = ArmyManager.getInstance().listColonies(getParent().getGameEngine().getGame());
        final List<Army> lstFinalArmies = new ArrayList<Army>();
        for (final Army army : lstArmies) {
            // check that army is not positioned inside barracks
            final Sector thisSector = SectorManager.getInstance().getByPosition(army.getPosition());
            if (!thisSector.hasBarrack()) {
                // check that army has less than 40 battalions
                int totBatt = 0;
                final List<Corp> lstCorps = CorpManager.getInstance().listByArmy(getParent().getGameEngine().getGame(), army.getArmyId());
                for (final Corp theCorp : lstCorps) {
                    final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByCorp(getParent().getGameEngine().getGame(), theCorp.getCorpId());
                    for (final Brigade brigade : lstBrigades) {
                        totBatt += brigade.getBattalions().size();
                    }
                }

                if (totBatt < 40) {
                    lstFinalArmies.add(army);
                }
            }
        }

        // Choose a nation at random
        if (!lstFinalArmies.isEmpty()) {
            java.util.Collections.shuffle(lstFinalArmies);
            final Army thisArmy = lstFinalArmies.get(0);

            // Set flag
            report(free, RE_RAID, Integer.toString(thisArmy.getArmyId()));
            report(thisArmy.getNation(), RE_RAID, "1");
            LOGGER.info("RaidRandomEvent will affect " + thisArmy.getNation().getName() + " this turn.");

            // Add news entry
            newsSingle(thisArmy.getNation(), NEWS_MILITARY, "Our army " + thisArmy.getName() + " in the colonies is raided by natives.");
        }

        theTrans.commit();
        LOGGER.info("RaidRandomEvent processed.");
    }

}
