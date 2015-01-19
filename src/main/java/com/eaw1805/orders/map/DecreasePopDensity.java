package com.eaw1805.orders.map;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
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
 * Order for decreasing population density.
 * ticket #23.
 */
public class DecreasePopDensity
        extends AbstractOrderProcessor
        implements OrderInterface, RegionConstants, GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(DecreasePopDensity.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_DEC_POP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public DecreasePopDensity(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("DecreasePopDensity instantiated.");
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
            getOrder().setResult(-4);
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

            // Make sure that the sector was not conquered during the last 5 months
            if (thisSector.getConqueredCounter() == 0) {
                if (curLevel > 0) {
                    thisSector.setPopulation(curLevel - 1);
                    SectorManager.getInstance().update(thisSector);
                    double citizens;
                    if (thisSector.getTerrain().getId() == TerrainConstants.TERRAIN_D) {
                        citizens = 500;

                    } else {
                        citizens = Sector.POP_LEVELS[curLevel] - Sector.POP_LEVELS[curLevel - 1];
                    }

                    if (regionId == EUROPE) {
                        final int sphere = getSphere(thisSector, thisSector.getNation());
                        switch (sphere) {
                            case 1:
                                citizens *= HOME;
                                break;

                            case 2:
                                citizens *= SPHERE;
                                break;

                            case 3:
                            default:
                                citizens *= FOREIGN;
                                break;
                        }

                    } else {
                        citizens *= COLONIAL;
                    }

                    // Update goods used by order
                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                    usedGoods.put(GOOD_PEOPLE, (int) citizens * -1);
                    getOrder().setUsedGoodsQnt(usedGoods);

                    getParent().incTotGoods(ownerId, regionId, GOOD_PEOPLE, (int) citizens);
                    getOrder().setResult(thisSector.getPopulation() + 1);
                    getOrder().setExplanation("decreased population of sector "
                            + thisSector.getPosition().toString()
                            + " (" + thisSector.getPosition().getRegion().getName() + ") to " + thisSector.populationCount());
                } else {
                    getOrder().setResult(-1);
                    getOrder().setExplanation("cannot reduce sectors with 0 population size");
                }

            } else {
                getOrder().setResult(-2);
                getOrder().setExplanation("sector must be owned for at least 5 months");
            }

        } else {
            getOrder().setResult(-3);
            getOrder().setExplanation("not owner of sector");
        }
    }
}
