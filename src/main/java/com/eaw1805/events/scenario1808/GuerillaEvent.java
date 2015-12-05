package com.eaw1805.events.scenario1808;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import com.eaw1805.events.RebellionEvent;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Scenario 1808.
 * 3% chance for each French-occupied Spanish coordinate to Rebel.
 * Exception: no roll if there is either an army or a barrack in the coordinate.
 */
public class GuerillaEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, ReportConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(RebellionEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public GuerillaEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("GuerillaEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Game initGame = GameManager.getInstance().getByID(-1);
        final Game thisGame = getParent().getGameEngine().getGame();

        // Locate conquered sectors
        final List<Sector> lstConquered = SectorManager.getInstance().listConquered(thisGame);

        // locate first that does not have any army stationed
        for (final Sector sector : lstConquered) {
            final Position thisPos = (Position) sector.getPosition().clone();
            thisPos.setGame(initGame);
            final Sector initSector = SectorManager.getInstance().getByPosition(thisPos);
            final Nation originalNation = initSector.getNation();

            // check only spanish sectors
            if (originalNation.getId() == NATION_SPAIN && sector.getNation().getId() == NATION_FRANCE) {
                // Exception: no roll if there is either an army or a barrack in the coordinate.
                final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByPositionNation(sector.getPosition(), sector.getNation());
                if (!lstBrigades.isEmpty()) {
                    continue;
                }

                if (sector.hasBarrack()) {
                    continue;
                }

                // 3% chance for each French-occupied Spanish coordinate to Rebel
                final int roll = getRandomGen().nextInt(101) + 1;
                if (roll <= 3) {
                    // Sector has rebelled !
                    LOGGER.info("Sector " + sector.getPosition().toString() + " has rebelled against " + sector.getNation().getName() + " and return to the control of " + originalNation.getName());

                    // Produce news
                    newsPair(sector.getNation(), originalNation, NEWS_WORLD,
                            "Sector " + sector.getPosition().toString() + " have rebelled against us and claim to be part of their home nation " + originalNation.getName(),
                            "Sector " + sector.getPosition().toString() + " have rebelled against the conqueror and return back to our control.");

                    // change ownership
                    sector.setNation(originalNation);
                    sector.setTempNation(originalNation);
                    sector.setConqueredCounter(0);
                    sector.setRebelled(true);
                    SectorManager.getInstance().update(sector);
                } else {
                    LOGGER.info("Sector " + sector.getPosition().toString() + " did NOT rebelled against " + sector.getNation().getName());
                }
            }
        }

        LOGGER.info("GuerillaEvent processed.");
    }
}
