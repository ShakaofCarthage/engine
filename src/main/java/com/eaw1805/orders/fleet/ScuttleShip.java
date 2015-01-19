package com.eaw1805.orders.fleet;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Order for Removing Ships.
 * ticket #21.
 */
public class ScuttleShip
        extends AbstractOrderProcessor
        implements GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ScuttleShip.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_SCUTTLE_SHIP;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public ScuttleShip(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("ScuttleShip instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int shipId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve the ship that we wish to destroy
        final Ship thisShip = ShipManager.getInstance().getByID(shipId);

        if (thisShip == null) {
            getOrder().setResult(-2);
            getOrder().setExplanation("cannot locate subject of order");

        } else {

            // Check ownership of ship
            if (thisShip.getNation().getId() == getOrder().getNation().getId()) {
                final int ownerId = getOrder().getNation().getId();
                final int regionId = thisShip.getPosition().getRegion().getId();

                // Update order's region of effect
                getOrder().setRegion(thisShip.getPosition().getRegion());

                final Sector thisSector = SectorManager.getInstance().getByPosition(thisShip.getPosition());

                if (thisSector.getTerrain().getId() != TerrainConstants.TERRAIN_O) {

                    // Calculate amount of Industrial Points that will be recovered from the destruction
                    final int inPt = (int) (thisShip.getType().getIndPt() * thisShip.getCondition() / 200d);

                    // Calculate amount of Fabrics that will be recovered from the destruction
                    final int fabrics = (int) (thisShip.getType().getFabrics() * thisShip.getCondition() / 300d);

                    // Add to the corresponding regional warehouse
                    getParent().incTotGoods(ownerId, regionId, GOOD_INPT, inPt);
                    getParent().incTotGoods(ownerId, regionId, GOOD_FABRIC, fabrics);

                    // Update goods used by order
                    final Map<Integer, Integer> usedGoods = new HashMap<Integer, Integer>();
                    usedGoods.put(GOOD_INPT, inPt * -1);
                    usedGoods.put(GOOD_FABRIC, fabrics * -1);
                    getOrder().setUsedGoodsQnt(usedGoods);

                    getOrder().setResult(1);
                    getOrder().setExplanation("ship " + thisShip.getName() + " [" + thisShip.getType().getTypeId() + "] scuttled - " + inPt + " InPt, " + fabrics + " Fabrics salvaged.");

                    destroyLoadedUnits(thisShip, true);
                } else {

                    getOrder().setResult(1);
                    getOrder().setExplanation("ship " + thisShip.getName() + " [" + thisShip.getType().getTypeId() + "] scuttled - nothing salvaged.");

                    destroyLoadedUnits(thisShip, false);
                }

                // For each type 4 or 5 class ship scuttled (-1 per ship)
                if (thisShip.getType().getShipClass() == 4) {
                    changeVP(getOrder().getGame(), getOrder().getNation(), SHIP_SCUTTLED_4, "Handover class-4 ship '" + thisShip.getName() + "'");

                } else if (thisShip.getType().getShipClass() == 5) {
                    changeVP(getOrder().getGame(), getOrder().getNation(), SHIP_SCUTTLED_5, "Handover class-5 ship '" + thisShip.getName() + "'");
                }

                // remove ship
                ShipManager.getInstance().delete(thisShip);

            } else {
                getOrder().setResult(-1);
                getOrder().setExplanation("not owner of ship");
            }
        }
    }



}
