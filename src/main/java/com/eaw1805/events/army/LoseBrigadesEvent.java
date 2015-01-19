package com.eaw1805.events.army;

import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implements Lose Brigades Events.
 */
public class LoseBrigadesEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(LoseBrigadesEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public LoseBrigadesEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("LoseBrigadesEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Brigade> brigadeList = BrigadeManager.getInstance().listByGame(getParent().getGameEngine().getGame());
        final Set<Integer> deletedBrigadeIds = new TreeSet<Integer>();
        final List<Brigade> brigadesToDelete = new ArrayList<Brigade>();
        for (final Brigade brigade : brigadeList) {
            if (brigade.getBattalions().size() < 2) {
                // Check that Brigade is not loaded
                boolean isLoaded = false;
                for (Battalion battalion : brigade.getBattalions()) {
                    isLoaded |= (battalion.getCarrierInfo().getCarrierId() != 0);
                }

                if (isLoaded) {
                    continue;
                }

                boolean found = false;

                // Try to find another brigade in the same coordinate to merge
                final List<Brigade> sameCoordinate = BrigadeManager.getInstance().listByPositionNation(brigade.getPosition(), brigade.getNation());
                for (final Brigade thatBrigade : sameCoordinate) {
                    if (thatBrigade.getBrigadeId() != brigade.getBrigadeId()
                            && !deletedBrigadeIds.contains(thatBrigade.getBrigadeId())
                            && thatBrigade.getBattalions().size() < 6) {
                        LOGGER.info("Brigade " + brigade.getBrigadeId() + " was too small and was merged with Brigade " + thatBrigade.getBrigadeId());

                        for (final Battalion battalion : brigade.getBattalions()) {
                            final Battalion copy = (Battalion) battalion.clone();
                            copy.setId(0);
                            thatBrigade.getBattalions().add(copy);
                        }

                        // Re-do ordering of battalions
                        int order = 1;
                        for (final Battalion thatBattalion : thatBrigade.getBattalions()) {
                            thatBattalion.setOrder(order++);
                        }

                        BrigadeManager.getInstance().update(thatBrigade);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    LOGGER.info("Brigade " + brigade.getBrigadeId() + " of Nation [" + brigade.getNation().getName() + "] was too small and was disbanded.");
                }

                // Remove brigade
                deletedBrigadeIds.add(brigade.getBrigadeId());
                brigadesToDelete.add(brigade);
            }
        }
        for (Brigade brigade : brigadesToDelete) {
            BrigadeManager.getInstance().delete(brigade);
        }

        LOGGER.info("LoseBrigadesEvent processed.");
    }
}