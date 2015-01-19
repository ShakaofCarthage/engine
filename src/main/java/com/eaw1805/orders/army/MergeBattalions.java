package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Order - Merge Battalions.
 * ticket:30.
 */
public class MergeBattalions
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(MergeBattalions.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_MRG_BATT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public MergeBattalions(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("MergeBattalions instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int battalionIdSrc = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source battalion
        final Battalion thisBatt = BattalionManager.getInstance().getByID(battalionIdSrc);

        if (thisBatt == null) {
            getOrder().setResult(-1);
            getOrder().setExplanation("source battalion does not exist");
            return;
        }

        // Retrieve the source brigade
        final Brigade thisBrigade = thisBatt.getBrigade();

        if (thisBrigade == null) {
            getOrder().setResult(-6);
            getOrder().setExplanation("cannot locate source brigade");
            return;
        }

        // Check ownership of source brigade
        if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();

            final int battalionIdTgt = Integer.parseInt(getOrder().getParameter2());
            final Battalion thatBatt = BattalionManager.getInstance().getByID(battalionIdTgt);

            if (thatBatt == null) {
                getOrder().setResult(-7);
                getOrder().setExplanation("target battalion does not exist");
                return;
            }

            // Retrieve the source brigade
            final Brigade thatBrigade = thatBatt.getBrigade();

            if (thatBrigade.getNation().getId() == ownerId) {
                // Check ownership of source brigade

                // check that both brigades are on the same sector
                if (thisBrigade.getPosition().equals(thatBrigade.getPosition())) {

                    // Check that both battalions are of the same type
                    if (thisBatt.getType().equals(thatBatt.getType())) {
                        // Calculate resulting experience
                        final double newExp = (thisBatt.getHeadcount() * thisBatt.getExperience()) + (thatBatt.getHeadcount() * thatBatt.getExperience()) / (thisBatt.getHeadcount() + thatBatt.getHeadcount());

                        // Update headcount of source brigade
                        thisBatt.setHeadcount(thisBatt.getHeadcount() + thatBatt.getHeadcount());
                        thisBatt.setExperience((int) newExp);

                        // check if zero or negative experience reached
                        if (thisBatt.getExperience() < 1) {
                            thisBatt.setExperience(1);
                        }

                        // make sure we do not encounter an overflow
                        if (thisBatt.getExperience() > thisBatt.getType().getMaxExp() + 2) {
                            thisBatt.setExperience(thisBatt.getType().getMaxExp() + 2);
                        }

                        // Determine the maximum headcount
                        int headcount = 800;
                        if (thisBatt.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                                || thisBatt.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                                || thisBatt.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                            headcount = 1000;
                        }

                        // Check that maximum headcount is not exceeded
                        if (thisBatt.getHeadcount() > headcount) {
                            thisBatt.setHeadcount(headcount);
                        }
                        BrigadeManager.getInstance().update(thisBrigade);

                        getOrder().setResult(1);
                        getOrder().setExplanation("battalion " + thisBatt.getOrder() + " from brigade (" + thisBrigade.getName() + ") merged with battalion " + thatBatt.getOrder() + " from brigade (" + thatBrigade.getName() + ")");

                        // Remove source battalion from source brigade
                        thatBrigade.getBattalions().remove(thatBatt);
                        BrigadeManager.getInstance().update(thatBrigade);

                    } else {
                        getOrder().setResult(-2);
                        getOrder().setExplanation("battalions are not of the same type");
                    }

                } else {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("brigades are not on same sector");
                }

            } else {
                getOrder().setResult(-4);
                getOrder().setExplanation("not owner of target brigade");
            }

        } else {
            getOrder().setResult(-5);
            getOrder().setExplanation("not owner of source brigade");
        }
    }

}
