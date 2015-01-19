package com.eaw1805.orders.army;

import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Order for Exchanging Battalions.
 * ticket #21.
 */
public class ExchangeBattalions
        extends AbstractOrderProcessor {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ExchangeBattalions.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_ADDTO_BRIGADE;

    private static final Set<Double> SWAPS = new HashSet<Double>();
    
    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ExchangeBattalions(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ExchangeBattalions instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int battalionId = Integer.parseInt(getOrder().getParameter1());

        final Battalion thisBatt = BattalionManager.getInstance().getByID(battalionId);
        if (thisBatt == null) {
            getOrder().setResult(-4);
            getOrder().setExplanation("source battalion does not exist");
            return;
        }

        // Retrieve the source brigade
        final Brigade thisBrigade = thisBatt.getBrigade();
        if (thisBrigade == null) {
            getOrder().setResult(-5);
            getOrder().setExplanation("source brigade does not exist");
            return;
        }

        // Check ownership of source brigade
        if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();

            final int newBrigadeId = Integer.parseInt(getOrder().getParameter2());
            final Brigade thatBrigade = BrigadeManager.getInstance().getByID(newBrigadeId);

            if (thatBrigade == null) {
                getOrder().setResult(-6);
                getOrder().setExplanation("target brigade does not exist");

            } else {
                if (thisBrigade.getBrigadeId() == thatBrigade.getBrigadeId()) {
                    //swaps inside the same brigade are not allowed in the client.
                    //so doing it on the server will most likely lead to issues
                    getOrder().setResult(1);
                    getOrder().setExplanation("");
                    return;
                }
                // Check ownership of source brigade
                if (thatBrigade.getNation().getId() == ownerId) {

                    // check that both brigades are on the same sector
                    if (thisBrigade.getPosition().equals(thatBrigade.getPosition())) {
                        final int newBattalionPos = Integer.parseInt(getOrder().getParameter3());
                        final int oldBattalionPos = thisBatt.getOrder();

                        // Retrieve the target battalion
                        Battalion thatBatt = null;
                        for (final Battalion thatBattalion : thatBrigade.getBattalions()) {
                            if (thatBattalion.getOrder() == newBattalionPos) {
                                thatBatt = thatBattalion;
                                break;
                            }
                        }

                        // Check if this is a reverse order
                        if (thatBatt != null && SWAPS.contains(thisBatt.getId() + thatBatt.getId() / 100000d)) {
                            // This is a reverse order
                            getOrder().setResult(1);
                            getOrder().setExplanation("order ignored (battalions already exchanged)");
                            return;

                        } else if (thatBatt == null && thatBrigade.getBattalions().size() >= 6) {
                            // make sure we do not have more than 6 battalions
                            getOrder().setResult(1);
                            getOrder().setExplanation("brigade cannot hold more than 6 battalions");
                            return;

                        }

                        // Assign thisBattalion to new Brigade
                        thisBatt.setOrder(newBattalionPos);
                        thisBatt.setBrigade(thatBrigade);
                        BattalionManager.getInstance().update(thisBatt);

                        // Remove source battalion from source brigade
                        thisBrigade.getBattalions().remove(thisBatt);

                        // Add source battalion to target brigade
                        thatBrigade.getBattalions().add(thisBatt);

                        if (thatBatt != null) {
                            // Flip orders
                            thatBatt.setOrder(oldBattalionPos);
                            thatBatt.setBrigade(thisBrigade);
                            BattalionManager.getInstance().update(thatBatt);

                            // Remove target battalion from source brigade
                            thatBrigade.getBattalions().remove(thatBatt);

                            // Add target battalion to target brigade
                            thisBrigade.getBattalions().add(thatBatt);

                            BattalionManager.getInstance().update(thatBatt);

                            SWAPS.add(thatBatt.getId() + thisBatt.getId() / 100000d);
                            
                            getOrder().setExplanation("battalion " + oldBattalionPos + " of brigade '" + thisBrigade.getName()
                                    + "' swapped with battalion " + newBattalionPos + " of brigade '" + thatBrigade.getName());

                        } else {
                            getOrder().setExplanation("battalion " + thisBatt.getOrder() + " of brigade '" + thisBrigade.getName()
                                    + "' moved to brigade '" + thatBrigade.getName() + "'");
                        }

                        // update movement points of brigade
                        thisBrigade.updateMP();
                        thatBrigade.updateMP();

                        // Make sure that the battalions of each brigade a properly numbered
                        // NO! this could cause false swaps in case of other exchanges(?)!
//                        int pos = 1;
//                        for (Battalion battalion : thisBrigade.getBattalions()) {
//                            battalion.setOrder(pos++);
//                        }
//
//                        pos = 1;
//                        for (Battalion battalion : thatBrigade.getBattalions()) {
//                            battalion.setOrder(pos++);
//                        }

                        BrigadeManager.getInstance().update(thisBrigade);
                        BrigadeManager.getInstance().update(thatBrigade);

                        getOrder().setResult(1);

                    } else {
                        getOrder().setResult(-1);
                        getOrder().setExplanation("brigades are not on same sector");
                    }

                } else {
                    getOrder().setResult(-2);
                    getOrder().setExplanation("not owner of target brigade");
                }
            }

        } else {
            getOrder().setResult(-3);
            getOrder().setExplanation("not owner of source brigade");
        }
    }
}
