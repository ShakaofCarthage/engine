package com.eaw1805.orders.army;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
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
 * Order - Demolish Brigade.
 * ticket:198.
 */
public class DemolishBrigade
        extends AbstractOrderProcessor
        implements GoodConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DemolishBrigade.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_D_BRIG;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DemolishBrigade(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DemolishBrigade instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int brigadeId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the source brigade
        final Brigade thisBrigade = BrigadeManager.getInstance().getByID(brigadeId);

        if (thisBrigade == null) {
            getOrder().setResult(-4);
            getOrder().setExplanation("cannot locate brigade");

        } else {

            // Check ownership of source brigade
            if (thisBrigade.getNation().getId() == getOrder().getNation().getId()) {
                final int ownerId = getOrder().getNation().getId();
                final Sector thisSector = SectorManager.getInstance().getByPosition(thisBrigade.getPosition());

                // Check that sector is owner by the same nation
                if (thisSector.getNation().getId() == ownerId) {

                    // Make sure that sector has a barrack
                    if (thisSector.hasBarrack()) {
                        int people = 0;
                        int horse = 0;

                        // iterate through battalions and calculate total people and horses
                        for (final Battalion battalion : thisBrigade.getBattalions()) {
                            people += (int) (battalion.getHeadcount() * 0.75d);
                            if (battalion.getType().needsHorse()) {
                                horse += (int) (battalion.getHeadcount() * 0.75d);
                            }
                        }

                        getParent().incTotGoods(ownerId, thisSector.getPosition().getRegion().getId(), GOOD_PEOPLE, people);
                        getParent().incTotGoods(ownerId, thisSector.getPosition().getRegion().getId(), GOOD_HORSE, horse);

                        // Update goods used by order
                        final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                        usedGoods.put(GOOD_HORSE, -1 * horse);
                        usedGoods.put(GOOD_PEOPLE, -1 * people);
                        getOrder().setUsedGoodsQnt(usedGoods);

                        getOrder().setResult(1);
                        getOrder().setExplanation("brigade " + thisBrigade.getName() + " was demolished");

                        // Remove source battalion from source brigade
                        BrigadeManager.getInstance().delete(thisBrigade);

                    } else {
                        getOrder().setResult(-1);
                        getOrder().setExplanation("sector does not have barracks");
                    }

                } else {
                    getOrder().setResult(-2);
                    getOrder().setExplanation("not owner of sector");
                }

            } else {
                getOrder().setResult(-3);
                getOrder().setExplanation("not owner of brigade");
            }
        }
    }

}
