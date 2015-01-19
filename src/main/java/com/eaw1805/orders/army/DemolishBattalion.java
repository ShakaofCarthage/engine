package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order - Demolish Battalions.
 * ticket:42.
 */
public class DemolishBattalion
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DemolishBattalion.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_D_BATT;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DemolishBattalion(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DemolishBattalion instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int battalionIDSrc = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source battalion
        final Battalion thisBatt = BattalionManager.getInstance().getByID(battalionIDSrc);

        if (thisBatt == null) {
            getOrder().setResult(-5);
            getOrder().setExplanation("cannot locate subject of order");

        } else {
            // Retrieve the source brigade
            final Brigade thisBrigade = thisBatt.getBrigade();

            if (thisBrigade == null) {
                getOrder().setResult(-4);
                getOrder().setExplanation("cannot locate brigade of battalion");
            } else {

                // Check ownership of source brigade
                if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
                    final int ownerId = getOrder().getNation().getId();
                    final Sector thisSector = SectorManager.getInstance().getByPosition(thisBrigade.getPosition());

                    // Check that sector is owner by the same nation
                    if (thisSector.getNation().getId() == ownerId) {

                        // Make sure that sector has a barrack
                        if (thisSector.hasBarrack()) {
                            final int people = (int) (thisBatt.getHeadcount() * 0.75d);
                            int horse = 0;
                            if (thisBatt.getType().needsHorse()) {
                                horse = (int) (thisBatt.getHeadcount() * 0.75d);
                            }

                            getParent().incTotGoods(ownerId, thisSector.getPosition().getRegion().getId(), GOOD_PEOPLE, people);
                            getParent().incTotGoods(ownerId, thisSector.getPosition().getRegion().getId(), GOOD_HORSE, horse);

                            // Update goods used by order
                            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                            usedGoods.put(GOOD_HORSE, -1 * horse);
                            usedGoods.put(GOOD_PEOPLE, -1 * people);
                            getOrder().setUsedGoodsQnt(usedGoods);

                            getOrder().setResult(1);
                            getOrder().setExplanation("battalion " + thisBatt.getOrder() + " of brigade " + thisBrigade.getName() + " was demolished");

                            // Remove source battalion from source brigade
                            thisBrigade.getBattalions().remove(thisBatt);
                            BrigadeManager.getInstance().update(thisBrigade);

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("sector does not have barracks");
                        }

                    } else {
                        getOrder().setResult(-3);
                        getOrder().setExplanation("not owner of sector");
                    }

                } else {
                    getOrder().setResult(-4);
                    getOrder().setExplanation("not owner of brigade");
                }
            }
        }
    }

}
