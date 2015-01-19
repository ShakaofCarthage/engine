package com.eaw1805.economy.production;

import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The HorseBreedingFarm production site.
 * between 300 and 500 horses will be produced.
 * If the farm is situated on a co-ordinate that is marked by a 'p' on the regional map then the production will rise by 40%.
 */
public class HorseBreedingFarm
        extends AbstractProductionSite
        implements NaturalResourcesConstants {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public HorseBreedingFarm(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        int production = getParent().getRandomGen().nextInt(201) + 200;

        // Check that natural resource is present
        if ((getSector().getNaturalResource() != null) && (getSector().getNaturalResource().getId() == NATRES_HORSE)) {
            production *= 1.5d;
        }

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.horsebreedingfarm." + getSector().getPosition().toString(), production);
        getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_HORSE, production);
    }
}