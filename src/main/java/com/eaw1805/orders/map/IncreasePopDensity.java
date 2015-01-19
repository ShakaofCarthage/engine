package com.eaw1805.orders.map;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order for Raising the Population Density of a Co-ordinate.
 * ticket #24.
 */
public class IncreasePopDensity
        extends AbstractOrderProcessor
        implements OrderInterface, GoodConstants, NationConstants, RegionConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(IncreasePopDensity.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_INC_POP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public IncreasePopDensity(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("IncreasePopDensity instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        // Retrieve the sector ID
        final int sectorID = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the sector that we wish to increase the population density
        final Sector thisSector = SectorManager.getInstance().getByID(sectorID);

        if (thisSector == null) {
            getOrder().setResult(-6);
            getOrder().setExplanation("cannot find sector");
            return;
        }

        // Update order's region of effect
        getOrder().setRegion(thisSector.getPosition().getRegion());

        // Make sure that the owner is the same
        if (thisSector.getNation().getId() == getOrder().getNation().getId()) {
            final int ownerId = getOrder().getNation().getId();
            final int regionId = thisSector.getPosition().getRegion().getId();
            final int curLevel = thisSector.getPopulation();

            // Make sure that the maximum level is not reached
            if (thisSector.getPopulation() < thisSector.getTerrain().getMaxDensity()) {

                // Make sure that the available people are in the corresponding warehouse
                final int reqCitizens = Sector.REQ_CITIZENS[curLevel];
                if (getParent().getTotGoods(ownerId, regionId, GOOD_PEOPLE) >= reqCitizens) {
                    // Make sure that the available stone/wood are in the corresponding warehouse
                    final int reqStone = Sector.REQ_STONE[curLevel];
                    final boolean needsWood = ((thisSector.getPosition().getRegion().getId() != EUROPE) || (ownerId == NATION_RUSSIA)) && (curLevel < 3);
                    if (needsWood) {

                        if (getParent().getTotGoods(ownerId, regionId, GOOD_WOOD) >= reqStone) {
                            thisSector.setPopulation(curLevel + 1);
                            SectorManager.getInstance().update(thisSector);

                            getParent().decTotGoods(ownerId, regionId, GOOD_WOOD, reqStone);
                            getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, reqCitizens);

                            // Update goods used by order
                            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                            usedGoods.put(GOOD_PEOPLE, reqCitizens);
                            usedGoods.put(GOOD_WOOD, reqStone);
                            getOrder().setUsedGoodsQnt(usedGoods);

                            getOrder().setResult(curLevel + 1);
                            getOrder().setExplanation("increased population of sector "
                                    + thisSector.getPosition().toString()
                                    + " (" + thisSector.getPosition().getRegion().getName() + ") to " + thisSector.populationCount());

                        } else {
                            getOrder().setResult(-1);
                            report("IncreasePopDensity." + getOrder().getOrderId() + ".result", "failed - not enough wood at regional warehouse");
                        }

                    } else {

                        if (getParent().getTotGoods(ownerId, regionId, GOOD_STONE) >= reqStone) {
                            thisSector.setPopulation(curLevel + 1);
                            SectorManager.getInstance().update(thisSector);

                            getParent().decTotGoods(ownerId, regionId, GOOD_STONE, reqStone);
                            getParent().decTotGoods(ownerId, regionId, GOOD_PEOPLE, reqCitizens);

                            // Update goods used by order
                            final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                            usedGoods.put(GOOD_PEOPLE, reqCitizens);
                            usedGoods.put(GOOD_STONE, reqStone);
                            getOrder().setUsedGoodsQnt(usedGoods);

                            getOrder().setResult(curLevel + 1);
                            getOrder().setExplanation("increased population of sector "
                                    + thisSector.getPosition().toString()
                                    + " (" + thisSector.getPosition().getRegion().getName() + ") to " + thisSector.populationCount());

                        } else {
                            getOrder().setResult(-2);
                            getOrder().setExplanation("not enough stone at regional warehouse");
                        }
                    }

                } else {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("not enough citizens at regional warehouse");
                }

            } else {
                getOrder().setResult(-4);
                getOrder().setExplanation("sector reached maximum density");
            }

        } else {
            getOrder().setResult(-5);
            getOrder().setExplanation("not owner of sector");
        }

    }
}
