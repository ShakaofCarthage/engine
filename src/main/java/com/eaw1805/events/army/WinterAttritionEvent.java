package com.eaw1805.events.army;

import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.BattalionManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Applies extra attrition due to severe winter to battalions that are outside a barrack.
 */
public class WinterAttritionEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(WinterAttritionEvent.class);

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
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public WinterAttritionEvent(final EventProcessor myParent) {
        super(myParent);

        final Nation freeNation = NationManager.getInstance().getByID(-1);
        hasArctic = getReport(freeNation, "winter.arctic").equals("1");
        hasCentral = getReport(freeNation, "winter.central").equals("1");
        hasMediterranean = getReport(freeNation, "winter.mediterranean").equals("1");

        LOGGER.debug("WinterAttritionEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        if (!hasArctic && !hasCentral && !hasMediterranean) {
            LOGGER.info("Good weather across continent.");
            return;
        }

        final Map<Sector, BigInteger> sectors = BattalionManager.getInstance().listBattalions(getParent().getGameEngine().getGame());
        for (final Sector sector : sectors.keySet()) {
            // Check if this sector does not have a barrack
            if (!sector.hasBarrack()) {
                final double attrition = getParent().getGameEngine().getRandomGen().nextInt(2) + 1;
                LOGGER.info("Battalions located at " + sector.getPosition() + " suffer extra loses [" + attrition + "%] from exposure to extreme cold");

                // Check if this sector is affected by one of the winter effects
                if ((hasArctic && sector.getPosition().getY() <= 10)
                        || (hasCentral && sector.getPosition().getY() >= 11 && sector.getPosition().getY() <= 35)
                        || (hasMediterranean && sector.getPosition().getY() > 35)) {
                    final List<Battalion> lstBattalion = BattalionManager.getInstance().listByGamePosition(sector.getPosition());
                    for (final Battalion battalion : lstBattalion) {
                        // Reduce headcount
                        battalion.setHeadcount((int) ((battalion.getHeadcount() * (100d - attrition)) / 100d));
                        BattalionManager.getInstance().update(battalion);
                    }
                }
            }
        }

        LOGGER.info("WinterAttritionEvent completed.");
    }
}