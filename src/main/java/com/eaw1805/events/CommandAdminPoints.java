package com.eaw1805.events;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.GameConstants;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.economy.TradeCityManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.economy.TradeCity;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calculate Command and Administrative Points.
 */
public class CommandAdminPoints
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, GoodConstants, NationConstants, ReportConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CommandAdminPoints.class);

    private final Game thisGame;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public CommandAdminPoints(final EventProcessor myParent) {
        super(myParent);
        thisGame = myParent.getGameEngine().getGame();
        LOGGER.debug("CommandAdminPoints instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Region europe = RegionManager.getInstance().getByID(EUROPE);
        final List<Region> lstRegions = RegionManager.getInstance().list();
        lstRegions.remove(0); // Remove Europe

        final List<Nation> nationList;
        switch (getParent().getGame().getScenarioId()) {
            case HibernateUtil.DB_FREE:
                nationList = new ArrayList<Nation>();

                // add single nation
                nationList.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                break;

            case HibernateUtil.DB_S3:
                nationList = new ArrayList<Nation>();
                nationList.add(NationManager.getInstance().getByID(NationConstants.NATION_SPAIN));
                nationList.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));
                nationList.add(NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN));
                break;

            case HibernateUtil.DB_S1:
            case HibernateUtil.DB_S2:
            default:
                nationList = NationManager.getInstance().list();
                nationList.remove(0); // Remove Free Nation
                break;
        }


        // check duration of game
        final double modifier;
        switch (thisGame.getType()) {
            case GameConstants.DURATION_SHORT:
                modifier = .7d;
                break;

            case GameConstants.DURATION_LONG:
                modifier = 1.3d;
                break;

            case GameConstants.DURATION_NORMAL:
            default:
                modifier = 1d;
        }

        // Look-up capitals, original owners and current owners
        Map<Integer, Boolean> stillInControlOfCapital = checkCapitalOwners(thisGame);

        // Look-up trade cities and count owners
        final int cityOwners[][] = countTradeCityOwners();

        for (final Nation nation : nationList) {
            // Retrieve warehouse
            final Warehouse thisWarehouse = WarehouseManager.getInstance().getByNationRegion(thisGame, nation, europe);

            // Calculate command points
            int baseCP = 12;

            // +2 Per commander
            final List<Commander> commanderList = CommanderManager.getInstance().listGameNationAlive(thisGame, nation);
            baseCP += commanderList.size() * 2;

            // +5 Militaristic Trait
            if (nation.getId() == NATION_RHINE || nation.getId() == NATION_FRANCE || nation.getId() == NATION_PRUSSIA) {
                baseCP += 5;
            }

            // +4 Maritime Trait
            if (nation.getId() == NATION_GREATBRITAIN || nation.getId() == NATION_PORTUGAL) {
                baseCP += 4;
            }

            // +6 per established Colonial theater
            int activeTheaters = 0;
            for (Region region : lstRegions) {
                final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameRegionNation(thisGame, region, nation);
                if (!lstBarracks.isEmpty()) {
                    activeTheaters++;
                }
            }
            baseCP += activeTheaters * 6;


            // Calculate administrative points
            int baseAP = 15;

            // +5 Industrious Trait
            if (nation.getId() == NATION_DENMARK || nation.getId() == NATION_SWEDEN) {
                baseAP += 5;
            }

            // +3 Agricultural Trait
            if (nation.getId() == NATION_AUSTRIA
                    || nation.getId() == NATION_SPAIN
                    || nation.getId() == NATION_ITALY
                    || nation.getId() == NATION_MOROCCO
                    || nation.getId() == NATION_NAPLES
                    || nation.getId() == NATION_RUSSIA
                    || nation.getId() == NATION_OTTOMAN
                    || nation.getId() == NATION_WARSAW) {
                baseAP += 3;
            }

            // +3 Per Trade City controlled in Europe
            baseAP += 3 * cityOwners[nation.getId()][EUROPE];

            // +1 Per Trade City controlled in the Colonies
            for (Region region : lstRegions) {
                baseAP += cityOwners[nation.getId()][region.getId()];
            }

            // Check if still in control of capital
            final boolean capitolLost = !stillInControlOfCapital.get(nation.getId());
            if (capitolLost) {
                // -4 Lose Capital
                baseCP -= 4;
                baseAP -= 4;
            }

            // Retrieve number of VPs
            final String vps = getReport(nation, N_VP);
            final int acquiredVPs;
            if (vps.isEmpty()) {
                acquiredVPs = 0;
            } else {
                acquiredVPs = Integer.parseInt(getReport(nation, N_VP));
            }

            final int goalVPs = (int) Math.floor(100d * acquiredVPs / (nation.getVpWin() * modifier));
            if (goalVPs > 80) {
                // +4 Having 80%+ of country's VPs
                baseCP += 8;
                baseAP += 4;

            } else if (goalVPs > 60) {
                // +3 Having 60%-80% of country's VPs
                baseCP += 6;
                baseAP += 3;

            } else if (goalVPs > 40) {
                // +2 Having 40%-60% of country's VPs
                baseCP += 4;
                baseAP += 2;

            } else if (goalVPs > 20) {
                // +1 Having 20%-40% of country's VPs
                baseCP += 2;
                baseAP += 1;
            }

            // Custom Games: Boosted C&A (+20%)
            if (getParent().getGame().isBoostedCAPoints()) {
                baseCP += (int) (baseCP * 2d / 10d);
                baseAP += (int) (baseAP * 2d / 10d);
            }

            // Update Warehouse
            thisWarehouse.getStoredGoodsQnt().put(GOOD_CP, baseCP);
            thisWarehouse.getStoredGoodsQnt().put(GOOD_AP, baseAP);
            WarehouseManager.getInstance().update(thisWarehouse);
        }
    }

    /**
     * Count trade cities owned by each player.
     *
     * @return a double array for each owner counting the total number of trade cities.
     */
    private int[][] countTradeCityOwners() {
        final int cityOwners[][] = new int[NATION_LAST + 1][REGION_LAST + 1];
        final List<TradeCity> cityList = TradeCityManager.getInstance().listByGame(thisGame);

        for (final TradeCity tradeCity : cityList) {
            final Sector thisSector = SectorManager.getInstance().getByPosition(tradeCity.getPosition());

            if (thisSector.getNation().getId() > 0) {
                cityOwners[thisSector.getNation().getId()][thisSector.getPosition().getRegion().getId()]++;
            }
        }

        return cityOwners;
    }

}
