package com.eaw1805.events.random;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.comparators.NationSort;
import com.eaw1805.events.AbstractRandomEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Target country receives 30% more citizens this month in Europe.
 */
public class NationalismRandomEvent
        extends AbstractRandomEventProcessor
        implements EventInterface, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(NationalismRandomEvent.class);

    /**
     * Keep track of then nations that have been affected by another occurrence of this event.
     */
    private final Set<Nation> selectedList;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public NationalismRandomEvent(final EventProcessor myParent) {
        super(myParent, 15, 5);
        selectedList = new HashSet<Nation>();
        LOGGER.debug("NationalismRandomEvent instantiated.");
    }

    /**
     * Processes the random event.
     */
    protected final void realiseEvent() {
        final Transaction theTrans = HibernateUtil.getInstance().getSessionFactory(getParent().getGame().getScenarioId()).getCurrentSession().beginTransaction();
        final List<Nation> lstNation = NationManager.getInstance().list();
        final Nation free = lstNation.remove(0); // remove free nation
        final int warCount[] = new int[NATION_LAST + 1];
        for (final Nation nation : lstNation) {
            // each nation may receive this event only once per turn
            if (!selectedList.contains(nation)) {
                // Cannot happen to countries that have no 'War' relations with anyone.
                final List<NationsRelation> lstRel = RelationsManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), nation);
                for (final NationsRelation relation : lstRel) {
                    if (relation.getRelation() == REL_WAR) {
                        warCount[nation.getId()]++;
                    }
                }
                nation.setSort(warCount[nation.getId()]);
            }
        }

        if (!lstNation.isEmpty()) {
            // Sort list based on number of nations at war with
            final Nation[] sortedList = new Nation[lstNation.size()];
            lstNation.toArray(sortedList);
            Arrays.sort(sortedList, new NationSort());
            final Nation thisNation = sortedList[0];

            // Make sure that nation has at least 1 enemy at war
            if (thisNation.getSort() > 0) {
                selectedList.add(thisNation);

                // Set flag
                report(free, RE_NATI, Integer.toString(thisNation.getId()));
                report(thisNation, RE_NATI, "1");
                LOGGER.info("NationalismRandomEvent will affect " + thisNation.getName() + " this turn.");

                // Add news entry
                newsSingle(thisNation, NEWS_MILITARY, "Our ministers report a wave of nationalism from our citizens, massively arriving for recruitment.");
            }

            LOGGER.info("NationalismRandomEvent processed.");
        }

        theTrans.commit();
    }

}
