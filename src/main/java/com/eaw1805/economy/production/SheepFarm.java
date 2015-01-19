package com.eaw1805.economy.production;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The SheepFarm production site.
 * Between 30 and 70 of wool.
 * If the farm is on a co-ordinate that is marked by a "v" on the regional map then the production will rise by 40%.
 */
public class SheepFarm
        extends AbstractProductionSite
        implements NaturalResourcesConstants, NationConstants {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public SheepFarm(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        final int nationId = getSector().getNation().getId();
        int production = getParent().getRandomGen().nextInt(41) + 30;

        // Check natural resource to increase production
        if ((getSector().getNaturalResource() != null) && (getSector().getNaturalResource().getId() == NATRES_SHEEP)) {
            production *= 1.5d;
        }

        // Agricultural Trait
        if (nationId == NATION_AUSTRIA
                || nationId == NATION_SPAIN
                || nationId == NATION_ITALY
                || nationId == NATION_MOROCCO
                || nationId == NATION_NAPLES
                || nationId == NATION_RUSSIA
                || nationId == NATION_OTTOMAN
                || nationId == NATION_WARSAW) {
            production *= 1.2d;
        }

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.sheepfarm." + getSector().getPosition().toString(), production);
        getParent().incProdGoods(nationId, getSector().getPosition().getRegion().getId(), GOOD_WOOL, production);
    }
}