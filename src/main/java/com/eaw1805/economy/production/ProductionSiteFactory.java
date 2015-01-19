package com.eaw1805.economy.production;

import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

/**
 * Constructs the corresponding production site.
 */
public final class ProductionSiteFactory
        implements ProductionSiteConstants {

    /**
     * The Economy processor that invoked us.
     */
    private final transient AbstractMaintenance parent;

    /**
     * Default constructor.
     *
     * @param myParent the Economy processor that invoked us.
     */
    public ProductionSiteFactory(final AbstractMaintenance myParent) {
        parent = myParent;
    }

    /**
     * Instantiates the corresponding object.
     *
     * @param thisSector the Sector where the production site is located.
     * @return the ProductionSite instance.
     */
    public AbstractProductionSite construct(final Sector thisSector) {
        AbstractProductionSite siteObj;
        switch (thisSector.getProductionSite().getId()) {
            case PS_FACTORY:
                siteObj = new Factory(parent, thisSector);
                break;

            case PS_MILL:
                siteObj = new WeavingMill(parent, thisSector);
                break;

            case PS_MINT:
                siteObj = new Mint(parent, thisSector);
                break;

            case PS_ESTATE:
                siteObj = new Estate(parent, thisSector);
                break;

            case PS_FARM_SHEEP:
                siteObj = new SheepFarm(parent, thisSector);
                break;

            case PS_FARM_HORSE:
                siteObj = new HorseBreedingFarm(parent, thisSector);
                break;

            case PS_LUMBERCAMP:
                siteObj = new Lumbercamp(parent, thisSector);
                break;

            case PS_QUARRY:
                siteObj = new Quarry(parent, thisSector);
                break;

            case PS_MINE:
                siteObj = new Mine(parent, thisSector);
                break;

            case PS_VINEYARD:
                siteObj = new Vineyard(parent, thisSector);
                break;

            case PS_PLANTATION:
                siteObj = new Plantation(parent, thisSector);
                break;

            default:
                siteObj = new DummyProductionSite(parent, thisSector);
        }

        return siteObj;
    }

}
