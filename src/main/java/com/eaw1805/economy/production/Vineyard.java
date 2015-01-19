package com.eaw1805.economy.production;

import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

import java.util.Calendar;

/**
 * The Vineyard production site.
 * Only between September and December; 20 to 40units of wine
 */
public class Vineyard
        extends AbstractProductionSite
        implements NaturalResourcesConstants {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Vineyard(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        final Calendar thisCal = getParent().getGameEngine().calendar();

        if (getSector().getNaturalResource() == null) {
            // This cannot happen...
            return;
        }

        // Check Natural Resource -- Vineyard production requires Natural Resource 'w'
        if (getSector().getNaturalResource().getId() == NATRES_WINE) {
            final int month = thisCal.get(Calendar.MONTH);
            if (month >= 8) {
                int production = getParent().getRandomGen().nextInt(21) + 20;

                // Custom Game: Boosted Production (+25% production from sites)
                if (getParent().getGame().isBoostedProduction()) {
                    production *= 1.25d;
                }

                report("production.vineyard." + getSector().getPosition().toString(), production);
                getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_WINE, production);
            }
        }
    }
}
