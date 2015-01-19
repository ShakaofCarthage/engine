package com.eaw1805.economy.production;

import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The Plantation production site.
 * <p/>
 * between 90 and 150 Colonial Goods.
 */
public class Plantation
        extends AbstractProductionSite {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Plantation(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        if (getSector().getNaturalResource() == null) {
            // This cannot happen...
            return;
        }

        int production = getParent().getRandomGen().nextInt(41) + 40;

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.plantation." + getSector().getPosition().toString(), production);
        getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_COLONIAL, production);
    }
}
