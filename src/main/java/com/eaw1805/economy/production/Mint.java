package com.eaw1805.economy.production;

import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * The Mint production site.
 * <p/>
 * Production: between 10 and 20units of Precious Metals will be minted to
 * money, each unit will yield between 30,000 and 45,000 money.
 */
public class Mint
        extends AbstractProductionSite {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Mint(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        final int moneyRate = 30000;
        int production = getParent().getRandomGen().nextInt(11) + 10;
        // Check if quantity is available
        if (getParent().getTotGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_PRECIOUS) < production) {
            // Less is available -- mint as much as possible
            production = getParent().getTotGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_PRECIOUS);
        }

        int totMoney = moneyRate * production;

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            totMoney *= 1.25d;
        }

        report("production.mint." + getSector().getPosition().toString(), totMoney);
        getParent().incProdGoods(getSector().getNation().getId(), EUROPE, GOOD_MONEY, totMoney);
        getParent().decProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_PRECIOUS, production);
    }
}