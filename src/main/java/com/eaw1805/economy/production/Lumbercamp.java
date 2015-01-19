package com.eaw1805.economy.production;

import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The Lumbercamp production site.
 * between 200 and 500units.
 * If the camp is in the Taiga then only 100 to 200units will be produced.
 */
public class Lumbercamp
        extends AbstractProductionSite
        implements TerrainConstants {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Lumbercamp(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        int production = getParent().getRandomGen().nextInt(201) + 200;

        // Check terrain type is Taiga
        if (getSector().getTerrain().getId() == TERRAIN_T || getSector().getTerrain().getId() == TERRAIN_J) {
            production = getParent().getRandomGen().nextInt(101) + 100;
        }

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.lumbercamp." + getSector().getPosition().toString(), production);
        getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_WOOD, production);
    }
}