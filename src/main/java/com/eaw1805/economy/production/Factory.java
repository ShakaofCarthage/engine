package com.eaw1805.economy.production;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The Factory production site.
 * Between 1000 and 3000 EcPt.
 * for 100 EcPts: 1 of ore, 5 of fabrics, 20 of wood.
 * Factories in the colonies: Production efficiency falls by 25% while using the same amount of materials,
 * i.e. in the colonies 1 unit of ore, 5 of fabrics and 20units of wood will only produce 75 EcPts, not 100.
 */
public class Factory
        extends AbstractProductionSite
        implements NationConstants, ReportConstants {

    /**
     * Captures if the Random Event workers on strike is in effect.
     */
    private final int workersStrike;

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Factory(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);

        // Random Event: Workers Strike
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        workersStrike = retrieveReportAsInt(free, myParent.getGame().getTurn(), RE_STRI);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        final int nationId = getSector().getNation().getId();
        final int regionId = getSector().getPosition().getRegion().getId();
        final boolean isEurope = regionId == EUROPE;
        int batch = getParent().getRandomGen().nextInt(21) + 10;

        // Check if ORE is available
        if (getParent().getTotGoods(nationId, regionId, GOOD_ORE) < batch) {
            // Less is available -- produce as much as possible
            batch = getParent().getTotGoods(nationId, regionId, GOOD_ORE);
        }

        // Check if FABRICS are available
        if (getParent().getTotGoods(nationId, regionId, GOOD_FABRIC) < 5 * batch) {
            // Less is available -- produce as much as possible
            batch = getParent().getTotGoods(nationId, regionId, GOOD_FABRIC) / 5;
        }

        // Check if WOOD are available
        if (getParent().getTotGoods(nationId, regionId, GOOD_WOOD) < 20 * batch) {
            // Less is available -- produce as much as possible
            batch = getParent().getTotGoods(nationId, regionId, GOOD_WOOD) / 20;
        }

        double production;
        if (isEurope) {
            production = batch * 100d;

        } else {
            production = batch * 75d;
        }

        // Militaristic Bonus
        if (nationId == NationConstants.NATION_DENMARK
                || nationId == NationConstants.NATION_SWEDEN) {
            production *= 1.1d;
        }

        // Random Event: Workers Strike
        if (nationId == workersStrike) {
            production *= 0.9d;
        }

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        report("production.factory." + getSector().getPosition().toString(), (int) production);
        getParent().incProdGoods(nationId, regionId, GOOD_INPT, (int) production);
        getParent().decProdGoods(nationId, regionId, GOOD_ORE, batch);
        getParent().decProdGoods(nationId, regionId, GOOD_FABRIC, 5 * batch);
        getParent().decProdGoods(nationId, regionId, GOOD_WOOD, 20 * batch);
    }
}
