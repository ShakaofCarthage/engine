package com.eaw1805.economy.production;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.MaintenanceConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.economy.AbstractMaintenance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides basic utilities for the production site map.
 */
public abstract class AbstractProductionSite
        implements GoodConstants, RegionConstants, MaintenanceConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(AbstractProductionSite.class);

    /**
     * The Economy processor that invoked us.
     */
    private final transient AbstractMaintenance parent;

    /**
     * The sector where the production site is located.
     */
    private final transient Sector sector;

    /**
     * Should not use this constructor.
     */
    public AbstractProductionSite() {
        parent = null;
        sector = null;
        LOGGER.debug("AbstractProductionSite dirty instantiation.");
    }

    /**
     * Default constructor.
     *
     * @param myParent   the Economy processor that invoked us.
     * @param thisSector the Sector where the production site is located.
     */
    public AbstractProductionSite(final AbstractMaintenance myParent, final Sector thisSector) {
        parent = myParent;
        sector = thisSector;
    }

    /**
     * Produce goods.
     */
    public final void advance() {
        final int sizeCheck = checkSize();

        switch (sizeCheck) {
            case 0:
                if (getSector().getConqueredCounter() < 5) {
                    process();
                }
                break;

            case -1:
                report("production.undersized." + getSector().getPosition().toString(), sizeCheck);
                news(getSector().getNation(), getSector().getNation(), NewsConstants.NEWS_ECONOMY, getSector().getProductionSite().getId(), "Production site at " + getSector().getPosition() + " cannot operate as sector has too few inhabitants.");
                break;

            default:
            case 1:
                report("production.oversized." + getSector().getPosition().toString(), sizeCheck);
                news(getSector().getNation(), getSector().getNation(), NewsConstants.NEWS_ECONOMY, getSector().getProductionSite().getId(), "Production site at " + getSector().getPosition() + " cannot operate as sector has too many inhabitants.");
                break;
        }
    }

    /**
     * Attrition of workers.
     *
     * @return actual attrition of workers.
     */
    public final int maintain() {
        int attrition = 0;
        if (checkSize() == 0) {
            final int minAttr = sector.getProductionSite().getAttritionMin();
            final int rangeAttr = sector.getProductionSite().getAttritionMax() - minAttr;
            if (sector.getProductionSite().getAttritionMax() > 0) {
                final int rand = parent.getRandomGen().nextInt(rangeAttr + 1) + minAttr;
                attrition = (int) (MAX_WORKERS * rand / 100d);
            }
        }

        return attrition;
    }

    /**
     * Check the size of the sector to make sure that the production site can operate properly.
     *
     * @return if the size of the sector is correct.
     */
    protected int checkSize() {
        if ((getSector().getPopulation() >= getSector().getProductionSite().getMinPopDensity())
                && (getSector().getPopulation() <= getSector().getProductionSite().getMaxPopDensity())) {
            return 0;
        } else if (getSector().getPopulation() < getSector().getProductionSite().getMinPopDensity()) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Produce goods.
     */
    protected abstract void process();

    /**
     * Access the economy processor that invoked us.
     *
     * @return the economy processor that invoked us.
     */
    protected final AbstractMaintenance getParent() {
        return parent;
    }

    /**
     * Get the sector where the production site is located.
     *
     * @return the sector where the production site is located.
     */
    protected final Sector getSector() {
        return sector;
    }

    /**
     * Add a report entry for this turn.
     *
     * @param key   the key of the report entry.
     * @param value the value of the report entry.
     */
    public void report(final String key, final int value) {
        final Report thisReport = new Report();
        thisReport.setGame(getParent().getGame());
        thisReport.setTurn(getParent().getGame().getTurn());
        thisReport.setNation(getSector().getNation());
        thisReport.setKey(key);
        thisReport.setValue(Integer.toString(value));
        ReportManager.getInstance().add(thisReport);
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected int retrieveReportAsInt(final Nation owner, final int turn, final String key) {
        final String value = retrieveReport(owner, turn, key);

        // Check if string is empty
        if (value.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    /**
     * Retrieve a report entry.
     *
     * @param owner the Owner of the report entry.
     * @param turn  the Turn of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    protected String retrieveReport(final Nation owner, final int turn, final String key) {
        try {
            final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getParent().getGame(), turn, key);
            if (thisReport == null) {
                return "";
            }
            return thisReport.getValue();

        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Retrieve a report entry for this turn.
     *
     * @param owner the owner of the report entry.
     * @param key   the key of the report entry.
     * @return the value of the report entry.
     */
    public String getReport(final Nation owner, final String key) {
        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner, getParent().getGame(), getParent().getGame().getTurn(), key);
        if (thisReport == null) {
            return "";
        } else {
            return thisReport.getValue();
        }
    }

    /**
     * Add a news entry for this turn.
     *
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Nation nation, final Nation subject, final int type, final int baseNewsId, final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(parent.getGame());
        thisNewsEntry.setTurn(parent.getGame().getTurn());
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
    }

}
