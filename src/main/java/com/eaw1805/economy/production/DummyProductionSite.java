package com.eaw1805.economy.production;

import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * Production Sites that do not affect production and do not have any attrition.
 */
public class DummyProductionSite extends AbstractProductionSite {

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public DummyProductionSite(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        // do nothing
    }

}
