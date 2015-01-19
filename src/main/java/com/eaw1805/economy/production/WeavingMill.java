package com.eaw1805.economy.production;

import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The WeavingMill production site.
 * between 50units and 200units of fabrics.
 * 2 units of wool for each unit of fabrics.
 */
public class WeavingMill
        extends AbstractProductionSite {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public WeavingMill(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        int production = getParent().getRandomGen().nextInt(151) + 50;
        int materials = production * 2;

        // Check if quantity is available
        if (getParent().getTotGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_WOOL) < materials) {
            // Less is available -- mint as much as possible
            materials = getParent().getTotGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_WOOL);
            production = materials / 2;
        }

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.weavingmill." + getSector().getPosition().toString(), production);
        getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_FABRIC, production);
        getParent().decProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_WOOL, materials);
    }
}
