package com.eaw1805.events.army;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CommanderNameManager;
import com.eaw1805.data.managers.army.RankManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.CommanderName;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implements New Commanders Events.
 */
public class NewCommandersEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(NewCommandersEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public NewCommandersEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("NewCommandersEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        LOGGER.info("NewCommandersEvent processed.");

        // Retrieve list of regions
        final List<Region> lstRegions = RegionManager.getInstance().list();
        lstRegions.remove(0); // Remove Europe

        // Iterate through nations of game
        final List<Nation> nationList = getParent().getGameEngine().getAliveNations();

        for (final Nation nation : nationList) {
            // Retrieve number of commanders for particular nation
            final List<Commander> commanderList = CommanderManager.getInstance().listGameNationAlive(getParent().getGameEngine().getGame(), nation);
            int totCommanders = commanderList.size();

            // Retrieve number of brigades for particular nation
            final List<Brigade> brigadeList = BrigadeManager.getInstance().listByGameNation(getParent().getGameEngine().getGame(), nation);
            final int totBrigades = brigadeList.size();

            // Ignore nations without any army.
            if (totBrigades < 1) {
                continue;
            }

            // Check number of commanders
            int reqCommanders = requiredCommanders(totBrigades);

            // Scenario 1808: Generals may not exceed their starting number
            if (getParent().getGame().getScenarioId() == HibernateUtil.DB_S3) {
                switch (nation.getId()) {
                    case NATION_SPAIN:
                    case NATION_GREATBRITAIN:
                        reqCommanders = 6;
                        break;

                    case NATION_FRANCE:
                        reqCommanders = 13;
                        break;

                    default:
                        reqCommanders = 0;
                        break;
                }
            }

            // Custom Games: Extended Commanders (+2 per active colony)
            if (getParent().getGame().isExtendedArrivalOfCommanders()) {
                // identify active theaters
                int activeTheaters = 1;
                for (Region region : lstRegions) {
                    final List<Sector> lstBarracks = SectorManager.getInstance().listBarracksByGameRegionNation(getParent().getGame(), region, nation);
                    if (!lstBarracks.isEmpty()) {
                        activeTheaters++;
                    }
                }

                reqCommanders += activeTheaters * 2;
            }

            // Get list of commander names
            final List<CommanderName> nameLst = CommanderNameManager.getInstance().listNation(nation);

            final CarrierInfo emptyCarrierInfo = new CarrierInfo();
            emptyCarrierInfo.setCarrierType(0);
            emptyCarrierInfo.setCarrierId(0);

            // Check if we need to generate new commanders
            while (reqCommanders > totCommanders) {
                // Identify correct number of names used
                final List<Commander> usedNamesList = CommanderManager.getInstance().listGameNation(getParent().getGameEngine().getGame(), nation);
                final int usedNames = usedNamesList.size();

                final Commander thisCommander = new Commander(); //NOPMD
                thisCommander.setNation(nation);
                thisCommander.setCaptured(nation);
                thisCommander.setRank(RankManager.getInstance().getByID(1));

                thisCommander.setName("New Commander");
                if (nameLst.size() > usedNames + 1) {
                    thisCommander.setName(nameLst.get(usedNames + 1).getName());

                } else {
                    thisCommander.setName("Recruit Commander");
                }

                // Pick a random brigade
                final int randBrigade = getRandomGen().nextInt(totBrigades);
                final Brigade thisBrigade = brigadeList.get(randBrigade);
                thisCommander.setPosition(thisBrigade.getPosition());
                thisCommander.setIntId(usedNames + 1);
                thisCommander.setCorp(0);
                thisCommander.setArmy(0);
                thisCommander.setMps(80);
                thisCommander.setComc(getRandomGen().nextInt(3) + 1);
                thisCommander.setStrc(1);
                thisCommander.setPool(true);
                thisCommander.setCarrierInfo(emptyCarrierInfo);
                CommanderManager.getInstance().add(thisCommander);

                newsSingle(nation, NEWS_MILITARY, thisCommander.getRank().getName() + " " + thisCommander.getName() + " is available for service at the " + thisCommander.getPosition().getRegion().getName() + " Officer's pool.");
                totCommanders++;
            }
        }
    }

    /**
     * Calculate number of required commanders.
     *
     * @param totBrigades -- total number of brigades.
     * @return number of required commanders.
     */
    private int requiredCommanders(final int totBrigades) {
        int reqCommanders = 3;
        if (totBrigades >= 131) {
            reqCommanders = 10;

        } else if (totBrigades >= 111) {
            reqCommanders = 9;

        } else if (totBrigades >= 91) {
            reqCommanders = 8;

        } else if (totBrigades >= 71) {
            reqCommanders = 7;

        } else if (totBrigades >= 51) {
            reqCommanders = 6;

        } else if (totBrigades >= 36) {
            reqCommanders = 5;

        } else if (totBrigades >= 21) {
            reqCommanders = 4;
        }
        return reqCommanders;
    }
}
