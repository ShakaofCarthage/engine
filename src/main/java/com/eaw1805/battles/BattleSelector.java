package com.eaw1805.battles;

import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * Holds the sectors that are pending battles.
 */
public class BattleSelector {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BattleSelector.class);

    /**
     * Holds the sectors where a a battle will take place.
     */
    protected List<Sector> sectors;

    /**
     * Default constructor.
     */
    public BattleSelector() {
        LOGGER.debug("BattleSelector instantiated.");
    }

    /**
     * Process all the pending battles.
     */
    public void resolveBattles() {

/*        Iterator itr = sectors.iterator();
        while(itr.hasNext()) {
            Sector sector = (Sector) itr.next();
            List<Brigade> gbBrigades = null;
            List<Brigade> frBrigades = null;

            if ( sector != null ) {
                LOGGER.debug("loading brigade");
                gbBrigades =
                        BrigadeManager.getInstance().listByPosition(sector.getPosition());
            } else {
                LOGGER.debug("loading brigade");
            }

            sector = (Sector) itr.next();
            if ( sector != null ) {
                frBrigades =
                        BrigadeManager.getInstance().listByPosition(sector.getPosition());
                new TacticalBattleProcessor(sector,gbBrigades,frBrigades).process();
            }

        }*/

    }

    /**
     * Loads starting armies, it is used for testing purposes.
     */
    public void loadStartingArmies() {
        LOGGER.debug("loading Starting Armies");

        final Game game = GameManager.getInstance().getByID(1);
        final Region region = RegionManager.getInstance().getByID(1);

//        23,20 Eng
        final Position gbPosition = new Position();
        gbPosition.setGame(game);
        gbPosition.setRegion(region);
        gbPosition.setX(23);
        gbPosition.setY(20);

//        23,27 Fr
        final Position frPosition = new Position();
        frPosition.setGame(game);
        frPosition.setRegion(region);
        frPosition.setX(23);
        frPosition.setY(27);

//        sectors = new ArrayList<Sector>(2);
//        sectors.add(SectorManager.getInstance().getByPosition(frPosition));
//          sectors.add(SectorManager.getInstance().getByPosition(gbPosition));

        final Sector frSector = SectorManager.getInstance().getByPosition(frPosition);
        final List<Brigade> frBrigades = BrigadeManager.getInstance().listByPosition(frSector.getPosition());

        final Sector gbSector = SectorManager.getInstance().getByPosition(gbPosition);
        final List<Brigade> gbBrigades = BrigadeManager.getInstance().listByPosition(gbSector.getPosition());

        for (Brigade brigade : gbBrigades) {
            LOGGER.debug("Showing brigade: " + brigade.getName()
                    + " Nation: " + brigade.getNation().getName());
            final Set<Battalion> battalions = brigade.getBattalions();
            for (Battalion battalion : battalions) {
                LOGGER.debug("Showing battalion: " + battalion.getId()
                        + " headcount: " + battalion.getHeadcount()
                        + " Experience: " + battalion.getExperience()
                        + " Type: " + battalion.getType().getName()
                );
            }
        }

        for (Brigade brigade : frBrigades) {
            LOGGER.debug("Showing brigade: " + brigade.getName()
                    + " Nation: " + brigade.getNation().getName());
            final Set<Battalion> battalions = brigade.getBattalions();
            for (Battalion battalion : battalions) {
                LOGGER.debug("Showing battalion: " + battalion.getId()
                        + " headcount: " + battalion.getHeadcount()
                        + " Experience: " + battalion.getExperience()
                        + " Type: " + battalion.getType().getName()
                );
            }
        }

//            new TacticalBattleProcessor(frSector.getTerrain(),
//                    frBrigades,
//                    gbBrigades).process();

        LOGGER.debug("Loaded Starting Armies");
    }
}
