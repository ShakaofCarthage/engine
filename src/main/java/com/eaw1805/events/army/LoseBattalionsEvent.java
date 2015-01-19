package com.eaw1805.events.army;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implements Lose Battalions Events.
 */
public class LoseBattalionsEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(LoseBattalionsEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public LoseBattalionsEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("LoseBattalionsEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final List<Battalion> battalionList = BattalionManager.getInstance().listSmallByGame(getParent().getGameEngine().getGame());

        for (final Battalion battalion : battalionList) {
            // Make sure battalion is not loaded
            if (battalion.getCarrierInfo().getCarrierId() != 0) {
                continue;
            }

            // Remove from brigade
            final Brigade thisBrigade = battalion.getBrigade();
            thisBrigade.getBattalions().remove(battalion);

            // indicate if another battalion of the same type has been located
            boolean found = false;

            if (battalion.getHeadcount() > 0) {

                // Try to find a battalion of the same type in the same brigade
                for (final Battalion otherBattalion : thisBrigade.getBattalions()) {

                    // Determine the maximum headcount
                    int headcount = 800;
                    if (battalion.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                            || battalion.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                            || battalion.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                        headcount = 1000;
                    }

                    if (otherBattalion.getType().getId() == battalion.getType().getId()
                            && otherBattalion.getHeadcount() < headcount) {
                        // Calculate resulting experience
                        final double newExp = (otherBattalion.getHeadcount() * otherBattalion.getExperience()) + (battalion.getHeadcount() * battalion.getExperience()) / (otherBattalion.getHeadcount() + battalion.getHeadcount());

                        // Merge the two battalions
                        otherBattalion.setHeadcount(otherBattalion.getHeadcount() + battalion.getHeadcount());
                        otherBattalion.setExperience((int) newExp);

                        // check if zero or negative experience reached
                        if (otherBattalion.getExperience() < 1) {
                            otherBattalion.setExperience(1);
                        }

                        // make sure we do not encounter an overflow
                        if (otherBattalion.getExperience() > otherBattalion.getType().getMaxExp() + 2) {
                            otherBattalion.setExperience(otherBattalion.getType().getMaxExp() + 2);
                        }

                        // check new size
                        if (otherBattalion.getHeadcount() > headcount) {
                            otherBattalion.setHeadcount(headcount);
                        }

                        LOGGER.info("Battalion " + battalion.getId() + " of Nation [" + thisBrigade.getNation().getName() + "] was too small and merged with Battalion " + otherBattalion.getId());
                        BattalionManager.getInstance().update(otherBattalion);
                        found = true;

                        battalion.setHeadcount(0);
                        BattalionManager.getInstance().update(battalion);
                        break;
                    }
                }

                if (!found) {
                    final List<Battalion> sameTypeBattalion = BattalionManager.getInstance().listByType(battalion);
                    for (final Battalion otherBattalion : sameTypeBattalion) {
                        if (otherBattalion == null) {
                            continue;
                        }

                        // Make sure battalion is not loaded
                        // fix for ticket:1670
                        if (otherBattalion.getCarrierInfo().getCarrierId() != 0) {
                            continue;
                        }

                        // Determine the maximum headcount
                        int headcount = 800;
                        if (battalion.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                                || battalion.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                                || battalion.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                            headcount = 1000;
                        }

                        // Calculate resulting experience
                        final double newExp = (otherBattalion.getHeadcount() * otherBattalion.getExperience()) + (battalion.getHeadcount() * battalion.getExperience()) / (otherBattalion.getHeadcount() + battalion.getHeadcount());

                        // Merge the two battalions
                        otherBattalion.setHeadcount(otherBattalion.getHeadcount() + battalion.getHeadcount());
                        otherBattalion.setExperience((int) newExp);

                        // make sure we do not encounter an overflow
                        if (otherBattalion.getExperience() > otherBattalion.getType().getMaxExp() + 2) {
                            otherBattalion.setExperience(otherBattalion.getType().getMaxExp() + 2);
                        }

                        // check new size
                        if (otherBattalion.getHeadcount() > headcount) {
                            otherBattalion.setHeadcount(headcount);
                        }

                        // check new size
                        if (otherBattalion.getHeadcount() > headcount) {
                            otherBattalion.setHeadcount(headcount);
                        }

                        LOGGER.info("Battalion " + battalion.getId() + " of Nation [" + thisBrigade.getNation().getName() + "] was too small and merged with Battalion " + otherBattalion.getId());
                        BattalionManager.getInstance().update(otherBattalion);

                        battalion.setHeadcount(0);
                        BattalionManager.getInstance().update(battalion);
                        break;
                    }
                }
            }

            BrigadeManager.getInstance().update(battalion.getBrigade());
            BattalionManager.getInstance().delete(battalion);
        }

        LOGGER.info("LoseBattalionsEvent processed.");
    }
}
