package com.eaw1805.economy.production;

import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The Quarry production site.
 * between 500 and 900units of stones.
 */
public class Quarry
        extends AbstractProductionSite {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Quarry(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        int production = getParent().getRandomGen().nextInt(401) + 500;

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.quarry." + getSector().getPosition().toString(), production);
        getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_STONE, production);
    }
}