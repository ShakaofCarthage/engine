package com.eaw1805.events;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.BarrackManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Barrack;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Examine conquered territories and produce rebellions.
 */
public class RebellionEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, ReportConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(RebellionEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public RebellionEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("RebellionEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Game initGame = GameManager.getInstance().getByID(-1);

        final Game thisGame = getParent().getGameEngine().getGame();
        final List<Nation> activeNations = getParent().getGameEngine().getAliveNations();

        // Retrieve taxation orders
        final Map<Integer, Integer> taxOrders = new HashMap<Integer, Integer>();
        final List<PlayerOrder> lstTaxOrders = PlayerOrderManager.getInstance().listTaxOrders(thisGame, thisGame.getTurn());
        for (PlayerOrder taxOrder : lstTaxOrders) {
            taxOrders.put(taxOrder.getNation().getId(), Integer.parseInt(taxOrder.getParameter1()));
        }

        // Locate conquered sectors
        final List<Sector> lstConquered = SectorManager.getInstance().listConquered(thisGame);

        // locate first that does not have any army stationed
        for (final Sector sector : lstConquered) {
            final Position thisPos = (Position) sector.getPosition().clone();
            thisPos.setGame(initGame);
            final Sector initSector = SectorManager.getInstance().getByPosition(thisPos);
            final Nation originalNation = initSector.getNation();

            // Some initial sectors are outside SOI
            if (originalNation.getId() == sector.getNation().getId()) {
                continue;
            }

            if (sector.hasBarrack()) {
                continue;
            }

            // Rebellions only take place in occupied home nation coordinates of active enemies
            // (must be at war and also a non-dead empire).
            if (!activeNations.contains(originalNation)) {
                continue;
            }

            // Rebellions take place against active enemies
            final NationsRelation relation = getByNations(sector.getNation(), originalNation);
            if (relation == null || relation.getRelation() != REL_WAR) {
                continue;
            }

            // In the presence of a brigade, this effect is negated.
            final List<Brigade> lstBrigades = BrigadeManager.getInstance().listByPositionNation(sector.getPosition(), sector.getNation());
            if (!lstBrigades.isEmpty()) {
                continue;
            }

            // Each coordinate has a 2% probability to rebel
            int baseRoll = 3;

            // If the empire uses 'harsh' or “low” taxation the chance for rebellion is doubled or halved respectively.
            // check taxation orders
            if (taxOrders.containsKey(sector.getNation().getId())) {
                switch (taxOrders.get(sector.getNation().getId())) {
                    case OrderConstants.TAX_HARSH:
                        // chances to rebel increase by 20%
                        baseRoll = 8;//40;
                        break;

                    case OrderConstants.TAX_LOW:
                        // chances to rebel decease by 40%
                        baseRoll = 2;//20;
                        break;

                    default:
                        // leave as is
                }
            } else {
                // no effect on base roll
            }

            // Certain areas in the map run a higher risk that average to rebel.
            // Spain (any occupying force)
            // Naples & Sicily (any occupying force)
            final String polSphere = String.valueOf(sector.getPoliticalSphere());
            if (polSphere.equalsIgnoreCase("E") || polSphere.equalsIgnoreCase("N")) {
                // chances to rebel increase by 5%
                baseRoll += 4;//20;
            }

            // France (if controlled by G. Britain)
            // G. Britain (if controlled by France)
            // Duchy of Warsaw (if controlled by Russia)
            if ((polSphere.equalsIgnoreCase("G") && sector.getNation().getId() == NATION_FRANCE)
                    || (polSphere.equalsIgnoreCase("F") && sector.getNation().getId() == NATION_GREATBRITAIN)
                    || (polSphere.equalsIgnoreCase("W") && sector.getNation().getId() == NATION_RUSSIA)) {
                // chances to rebel increase by 5%
                baseRoll += 4;
            }

            LOGGER.info("Rolling rebellion for " + sector.getPosition().toString() + " with target " + baseRoll);

            // Check roll
            final int roll = getRandomGen().nextInt(101) + 1;
            if (roll < baseRoll) {
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
                // check if sector has a barracks
                if (sector.hasBarrack()) {
                    // also hand-over barrack
                    final Barrack barrack = BarrackManager.getInstance().getByPosition(sector.getPosition());
                    barrack.setNation(originalNation);
                    BarrackManager.getInstance().update(barrack);
                }

            } else {
                LOGGER.info("Sector " + sector.getPosition().toString() + " did NOT rebelled against " + sector.getNation().getName());
            }
        }

        LOGGER.info("RebellionEvent processed.");
    }

}
