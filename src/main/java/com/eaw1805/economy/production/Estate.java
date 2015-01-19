package com.eaw1805.economy.production;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.NaturalResourcesConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;

import java.util.Calendar;

/**
 * The Estate production site.
 * 75 x z(1.2) x CF.
 * The production of estates depends on their position on the map and the season.
 */
public class Estate
        extends AbstractProductionSite
        implements NaturalResourcesConstants, NationConstants, ReportConstants {

    /**
     * If arctic zone suffers from severe winter.
     */
    private final transient boolean hasArctic;

    /**
     * If central zone suffers from severe winter.
     */
    private final transient boolean hasCentral;

    /**
     * If mediterranean zone suffers from severe winter.
     */
    private final transient boolean hasMediterranean;

    /**
     * Captures if the Random Event excellent harvest is in effect.
     */
    private final int excellentHarvest;

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public Estate(final AbstractMaintenance myParent, final Sector thisSector) {
        super(myParent, thisSector);

        final Nation freeNation = NationManager.getInstance().getByID(-1);
        hasArctic = getReport(freeNation, "winter.arctic").equals("1");
        hasCentral = getReport(freeNation, "winter.central").equals("1");
        hasMediterranean = getReport(freeNation, "winter.mediterranean").equals("1");

        // Random Event: Workers Strike
        final Nation free = NationManager.getInstance().getByID(NATION_NEUTRAL);
        excellentHarvest = retrieveReportAsInt(free, myParent.getGame().getTurn(), RE_HARV);
    }

    /**
     * Process the production site.
     */
    @Override
    protected void process() {
        final int nationId = getSector().getNation().getId();
        final double climateFactor = getCF();
        final double randomFactor = (10d + getParent().getRandomGen().nextInt(11)) / 10d;
        double production = 75d * climateFactor * randomFactor;

        // Check Natural Resource bonus
        if ((getSector().getNaturalResource() != null) && (getSector().getNaturalResource().getId() == NATRES_FOOD)) {
            production *= 1.33d;
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
            production *= 1.2;
        }

        // Random Event: Excellent Harvest
        if (nationId == excellentHarvest) {
            production *= 2d;
        }

        // Custom Game: Boosted Production (+25% production from sites)
        if (getParent().getGame().isBoostedProduction()) {
            production *= 1.25d;
        }

        final int finalProduction = (int) production;
        report("production.estate." + getSector().getPosition().toString(), finalProduction);
        getParent().incProdGoods(getSector().getNation().getId(), getSector().getPosition().getRegion().getId(), GOOD_FOOD, finalProduction);
        getParent().getStrRandomFactor().append(randomFactor);
        getParent().getStrRandomFactor().append(" ");
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getCF() {
        double climateFactor;
        switch (getSector().getPosition().getRegion().getId()) {
            case EUROPE:
                if (getSector().getPosition().getY() < 10) {
                    climateFactor = getEuropeHigh();

                    if (hasArctic) {
                        // Food Production from estates is halved
                        climateFactor /= 2d;
                    }

                } else if ((getSector().getPosition().getY() >= 10) && (getSector().getPosition().getY() < 35)) {
                    climateFactor = getEuropeMed();

                    if (hasCentral) {
                        // Food Production from estates is halved
                        climateFactor /= 2d;
                    }

                } else {
                    climateFactor = getEuropeLow();

                    if (hasMediterranean) {
                        // Food Production from estates is halved
                        climateFactor /= 2d;
                    }
                }
                break;

            case CARIBBEAN:
                climateFactor = getCaribbean();
                break;

            case INDIES:
                climateFactor = getIndies();
                break;

            case AFRICA:
                climateFactor = getAfrica();
                break;

            default:
                climateFactor = 0.1d;
        }

        return climateFactor;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getEuropeHigh() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        double climateFactor;
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
                // Dec - Jan
                climateFactor = 0.2d;
                break;

            case Calendar.FEBRUARY:
            case Calendar.MARCH:
            case Calendar.APRIL:
                // Feb - Apr
                climateFactor = 0.25d;
                break;

            case Calendar.MAY:
            case Calendar.JUNE:
                // May - Jun
                climateFactor = 0.5d;
                break;

            case Calendar.JULY:
            case Calendar.AUGUST:
                // Jul - Aug
                climateFactor = 1.15d;
                break;

            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                // Sep - Nov
                climateFactor = 0.4d;
                break;

            default:
                climateFactor = 0.4d;
        }
        return climateFactor;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getEuropeMed() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        double climateFactor;
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
                // Dec - Jan
                climateFactor = 0.2d;
                break;

            case Calendar.FEBRUARY:
            case Calendar.MARCH:
            case Calendar.APRIL:
                // Feb - Apr
                climateFactor = 0.4d;
                break;

            case Calendar.MAY:
            case Calendar.JUNE:
                // May - Jun
                climateFactor = 1.4d;
                break;

            case Calendar.JULY:
            case Calendar.AUGUST:
                // Jul - Aug
                climateFactor = 1.65d;
                break;

            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                // Sep - Nov
                climateFactor = 0.5d;
                break;

            default:
                climateFactor = 0.5d;
        }
        return climateFactor;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getEuropeLow() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        double climateFactor;
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
                // Dec - Jan
                climateFactor = 0.2d;
                break;

            case Calendar.FEBRUARY:
            case Calendar.MARCH:
            case Calendar.APRIL:
                // Feb - Apr
                climateFactor = 0.5d;
                break;

            case Calendar.MAY:
            case Calendar.JUNE:
                // May - Jun
                climateFactor = 1.5d;
                break;

            case Calendar.JULY:
            case Calendar.AUGUST:
                // Jul - Aug
                climateFactor = 1.85d;
                break;

            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                // Sep - Nov
                climateFactor = 0.5d;
                break;

            default:
                climateFactor = 0.5d;
        }
        return climateFactor;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getCaribbean() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        double climateFactor;
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
                // Dec - Jan
                climateFactor = 0.2d;
                break;

            case Calendar.FEBRUARY:
            case Calendar.MARCH:
            case Calendar.APRIL:
                // Feb - Apr
                climateFactor = 0.6d;
                break;

            case Calendar.MAY:
            case Calendar.JUNE:
                // May - Jun
                climateFactor = 0.75d;
                break;

            case Calendar.JULY:
            case Calendar.AUGUST:
                // Jul - Aug
                climateFactor = 0.15d;
                break;

            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                // Sep - Nov
                climateFactor = 0.2d;
                break;

            default:
                climateFactor = 0.2d;
        }
        return climateFactor;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getIndies() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        double climateFactor;
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
                // Dec - Jan
                climateFactor = 0.55d;
                break;

            case Calendar.FEBRUARY:
            case Calendar.MARCH:
            case Calendar.APRIL:
                // Feb - Apr
                climateFactor = 0.8d;
                break;

            case Calendar.MAY:
            case Calendar.JUNE:
                // May - Jun
                climateFactor = 0.15d;
                break;

            case Calendar.JULY:
            case Calendar.AUGUST:
                // Jul - Aug
                climateFactor = 0.2d;
                break;

            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                // Sep - Nov
                climateFactor = 0.3d;
                break;

            default:
                climateFactor = 0.3d;
        }
        return climateFactor;
    }

    /**
     * The production of estates depends on their position on the map and the season.
     *
     * @return the climate factor.
     */
    private double getAfrica() {
        final Calendar thisCal = getParent().getGameEngine().calendar();
        final int month = thisCal.get(Calendar.MONTH);
        double climateFactor;
        switch (month) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
                // Dec - Jan
                climateFactor = 0.85d;
                break;

            case Calendar.FEBRUARY:
            case Calendar.MARCH:
            case Calendar.APRIL:
                // Feb - Apr
                climateFactor = 0.7d;
                break;

            case Calendar.MAY:
            case Calendar.JUNE:
                // May - Jun
                climateFactor = 0.4d;
                break;

            case Calendar.JULY:
            case Calendar.AUGUST:
                // Jul - Aug
                climateFactor = 0.15d;
                break;

            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                // Sep - Nov
                climateFactor = 0.2d;
                break;

            default:
                climateFactor = 0.2d;
        }
        return climateFactor;
    }
}