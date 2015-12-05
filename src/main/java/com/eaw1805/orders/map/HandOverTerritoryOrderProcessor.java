package com.eaw1805.orders.map;

import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.RelationConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.orders.AbstractOrderProcessor;
import com.eaw1805.orders.OrderInterface;
import com.eaw1805.orders.OrderProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Handing over Ships or Territory.
 * ticket #39.
 */
public class HandOverTerritoryOrderProcessor
        extends AbstractOrderProcessor
        implements OrderInterface, RegionConstants, ProductionSiteConstants, RelationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(HandOverTerritoryOrderProcessor.class);

    /**
     * Type of the order.
     */
    public static final int ORDER_TYPE = ORDER_HOVER_SEC;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public HandOverTerritoryOrderProcessor(final OrderProcessor myParent) {
        super(myParent);
        LOGGER.debug("HandOverTerritoryOrderProcessor instantiated.");
    }

    /**
     * Process this particular order.
     */
    public void process() {
        final int sectorId = Integer.parseInt(getOrder().getParameter1());

        // Retrieve Sector
        final Sector thisSector = SectorManager.getInstance().getByID(sectorId);

        if (thisSector == null) {
            getOrder().setResult(-1);
            getOrder().setExplanation("cannot find sector");

        } else {

            // Check ownership of sector
            if (thisSector.getNation().getId() == getOrder().getNation().getId()) {
                // retrieve
                final int targetId = Integer.parseInt(getOrder().getParameter2());

                // Retrieve target nation
                final Nation target = NationManager.getInstance().getByID(targetId);

                // Retrieve relation between two nations
                final NationsRelation relation = getByNations(getOrder().getNation(), target);

                if (relation == null) {
                    getOrder().setResult(-5);
                    getOrder().setExplanation("cannot hand over sector to a non-allied nation");

                } else if (relation.getRelation() == REL_ALLIANCE) {

                    // If the ceded co-ordinates do not belong to the home nation or sphere of influence of the receiving country,
                    // then ceding is only possible with density 0 or 1 co-ordinates.
                    if (getSphere(thisSector, target) == 3 && thisSector.getPopulation() > 1) {
                        getOrder().setResult(-4);
                        getOrder().setExplanation("cannot hand over highly populated sector");
                        return;
                    }

                    // check if there is a brigade of the new owner
                    final List<Battalion> lstBattalions = BattalionManager.getInstance().listByGamePosition(thisSector.getPosition(), target);
                    if (lstBattalions.isEmpty()) {
                        getOrder().setResult(-5);
                        getOrder().setExplanation("allied brigade must be stationed on the sector in question.");
                        return;
                    }

                    // The -1- density population will drop to zero.
                    if (getSphere(thisSector, target) == 3 && thisSector.getPopulation() < 2) {
                        thisSector.setPopulation(0);
                        LOGGER.warn("Dropping population density to 0");

                    } else {

                        // When ceding sectors the population size will drop depending on the region.
                        switch (thisSector.getPosition().getRegion().getId()) {
                            case EUROPE:
                                if (thisSector.getNation().getTaxRate() > target.getTaxRate()) {
                                    // If a co-ordinate is transferred from a country with higher tax rate to
                                    // a country with lower tax rate, the population will drop between 1-3 levels (randomly)
                                    final int drop = getParent().getRandomGen().nextInt(4) + 1;
                                    thisSector.setPopulation(thisSector.getPopulation() - drop);
                                    LOGGER.warn("Dropping population density by " + drop);

                                } else {
                                    // If a co-ordinate is transferred from a country with lower tax rate to a
                                    // country with higher tax rate, the population will drop between 2-5 levels.
                                    thisSector.setPopulation(thisSector.getPopulation() - getLevelDrop(target.getTaxRate() - thisSector.getNation().getTaxRate()));
                                }
                                break;

                            default:
                                // When ceding co-ordinates in the colonies,
                                // there is a 50% chance that the population density will drop by one level and
                                // a 25% chance that the population density will drop by two levels.
                                final int roll = getParent().getRandomGen().nextInt(101) + 1;
                                if (roll < 25) {
                                    thisSector.setPopulation(thisSector.getPopulation() - 2);
                                    LOGGER.warn("Dropping population density by 2");

                                } else if (roll < 75) {
                                    thisSector.setPopulation(thisSector.getPopulation() - 1);
                                    LOGGER.warn("Dropping population density by 1");
                                }
                        }
                    }

                    // For each coordinate above 3 population handed over
                    if (thisSector.getPopulation() >= 3) {
                        changeVP(getOrder().getGame(), getOrder().getNation(), SECTOR_HANDOVER, "Handover of " + thisSector.getPosition().toString());
                    }

                    // Make sure we do not have a negative population size
                    if (thisSector.getPopulation() < 0) {
                        thisSector.setPopulation(0);
                    }

                    thisSector.setNation(target);
                    SectorManager.getInstance().update(thisSector);

                    // check if sector has a barracks
                    if (thisSector.getProductionSite() != null && thisSector.getProductionSite().getId() >= PS_BARRACKS) {
                        // check if this is the first barrack in the colonies
                        final int regionId = thisSector.getPosition().getRegion().getId();
                        if (regionId != EUROPE) {
                            boolean foundMore = false;
                            final List<Barrack> lstBarracks = BarrackManager.getInstance().listByGameNation(getGame(), target);
                            for (final Barrack barrack : lstBarracks) {
                                if (barrack.getPosition().getRegion().getId() == regionId) {
                                    foundMore = true;
                                }
                            }

                            if (!foundMore) {
                                // Start up a colony
                                final List<Report> lstReports = ReportManager.getInstance().listByOwnerKey(thisSector.getTempNation(), getGame(), "colony." + regionId);
                                if (lstReports.isEmpty()) {
                                    // We gain 8 VP
                                    changeVP(getGame(), thisSector.getTempNation(), STARTUP_COLONY,
                                            "Start up a Colony in " + thisSector.getPosition().getRegion().getName());

                                    report(thisSector.getTempNation(), "colony." + regionId, "1");

                                    // Modify player's profile
                                    changeProfile(target, ProfileConstants.STARTUP_COLONY, 1);

                                    // Update achievements
                                    getParent().achievementsSetupColonies(getGame(), target);
                                }
                            }
                        }

                        // also hand-over barrack
                        final Barrack barrack = BarrackManager.getInstance().getByPosition(thisSector.getPosition());
                        barrack.setNation(target);
                        BarrackManager.getInstance().update(barrack);
                    }

                    newsPair(getOrder().getNation(), target, NEWS_POLITICAL,
                            "We handed over " + thisSector.getPosition() + " to " + target.getName(),
                            getOrder().getNation().getName() + " handed over " + thisSector.getPosition() + " to us.");

                    getOrder().setResult(1);
                    getOrder().setExplanation("handed over " + thisSector.getPosition() + " to " + target.getName());

                } else {
                    getOrder().setResult(-3);
                    getOrder().setExplanation("cannot hand over sector to a non-allied nation");
                }

            } else {
                getOrder().setResult(-2);
                getOrder().setExplanation("not owner of sector");
            }
        }
    }

    private int getLevelDrop(final int taxRateDiff) {
        final int roll = getParent().getRandomGen().nextInt(101) + 1;
        final int levelDrop;
        switch (taxRateDiff) {
            case 0:
                if (roll < 50) {
                    levelDrop = 2;

                } else if (roll < 75) {
                    levelDrop = 3;

                } else if (roll < 90) {
                    levelDrop = 4;

                } else {
                    levelDrop = 5;
                }
                break;

            case 1:
                if (roll < 40) {
                    levelDrop = 2;

                } else if (roll < 70) {
                    levelDrop = 3;

                } else if (roll < 90) {
                    levelDrop = 4;

                } else {
                    levelDrop = 5;
                }
                break;

            case 2:
                if (roll < 30) {
                    levelDrop = 2;

                } else if (roll < 70) {
                    levelDrop = 3;

                } else if (roll < 90) {
                    levelDrop = 4;

                } else {
                    levelDrop = 5;
                }
                break;

            default:
                if (roll < 20) {
                    levelDrop = 2;

                } else if (roll < 50) {
                    levelDrop = 3;

                } else if (roll < 80) {
                    levelDrop = 4;

                } else {
                    levelDrop = 5;
                }
                break;
        }

        LOGGER.warn("Dropping population density by 2-5 levels [roll=" + roll + "/drop=" + levelDrop + "]");
        return levelDrop;
    }

}
