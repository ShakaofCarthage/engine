package com.eaw1805.economy.production;

import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The Mine production site.
 * Production:     Europe        Colonies
 * ore             35 - 45 units 15 - 25 units
 * gems            --             5 - 10 units
 * precious metals 10 - 30 units  5 - 15 units
 */
public class Mine
        extends AbstractProductionSite
        implements NaturalResourcesConstants {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Mine(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        final boolean isEurope = getSector().getPosition().getRegion().getId() == EUROPE;
        int production = 0;
        int type = 0;

        if (getSector().getNaturalResource() == null) {
            // This cannot happen...
            return;
        }

        switch (getSector().getNaturalResource().getId()) {

            case NATRES_METALS: // Precious metals
                if (isEurope) {
                    production = getParent().getRandomGen().nextInt(21) + 10;
                } else {
                    production = getParent().getRandomGen().nextInt(11) + 5;
                }

                // Custom Game: Boosted Production (+25% production from sites)
                if (getParent().getGame().isBoostedProduction()) {
                    production *= 1.25d;
                }

                type = GOOD_PRECIOUS;
                getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_PRECIOUS, production);
                break;

            case NATRES_ORE: // Ore
                if (isEurope) {
                    production = getParent().getRandomGen().nextInt(11) + 35;
                } else {
                    production = getParent().getRandomGen().nextInt(11) + 15;
                }

                // Custom Game: Boosted Production (+25% production from sites)
                if (getParent().getGame().isBoostedProduction()) {
                    production *= 1.25d;
                }

                type = GOOD_ORE;
                getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_ORE, production);
                break;

            case NATRES_GEMS: // Gems
                if (isEurope) {
                    production = 0;
                } else {
                    production = getParent().getRandomGen().nextInt(6) + 5;
                }

                // Custom Game: Boosted Production (+25% production from sites)
                if (getParent().getGame().isBoostedProduction()) {
                    production *= 1.25d;
                }

                type = GOOD_GEMS;
                getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_GEMS, production);
                break;

            default:
                // produce nothing
        }

        report("production.mine.type." + type + "." + getSector().getPosition().toString(), production);
    }
}
