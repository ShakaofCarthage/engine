package com.eaw1805.events.fleet;

import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.List;

/**
 * Implements Weather Events.
 */
public class StormEvent
        extends AbstractEventProcessor
        implements EventInterface {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(StormEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public StormEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("StormEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final Game thisGame = getParent().getGameEngine().getGame();

        // Remove all storms and keep only the centers
        SectorManager.getInstance().removeStorms(thisGame);

        // Go through each region separately
        processArctic();
        processCentral();
        processMed();
        processCaribbean();
        processIndies();
        processAfrica();

        // Move Storms
        final List<Sector> lstStorms = SectorManager.getInstance().listStormsByGame(thisGame);
        for (final Sector storm : lstStorms) {
            moveStorm(storm);
        }

        // Expand storms
        final List<Sector> lstNewStorms = SectorManager.getInstance().listStormsByGame(thisGame);
        for (final Sector storm : lstNewStorms) {
            expandStorm(storm);
        }

        LOGGER.info("StormEvent processed.");
    }

    /**
     * Expand a storm up to 8..32 sectors in a random shape.
     *
     * @param thisStorm the storm to expand.
     */
    private void expandStorm(final Sector thisStorm) {
        int totSize = getParent().getGameEngine().getRandomGen().nextInt(15) + 8;
        final int stormX = thisStorm.getPosition().getX();
        final int stormY = thisStorm.getPosition().getY();
        for (int thisX = stormX - 5; thisX <= stormX + 5; thisX++) {
            for (int thisY = stormY - 4; thisY <= stormY + 4; thisY++) {
                final int roll = getParent().getGameEngine().getRandomGen().nextInt(101) + 1;
                final int distance = (int) Math.sqrt((stormX - thisX) ^ 2 + (stormY - thisY) ^ 2);
                final int chance;
                switch (distance) {
                    case 0:
                        chance = 80;
                        break;

                    case 1:
                        chance = 70;
                        break;

                    case 2:
                        chance = 60;
                        break;

                    case 3:
                        chance = 50;
                        break;

                    default:
                        chance = 60;
                        break;
                }

                if (totSize > 0 && roll <= chance) {
                    // Locate sector
                    final Position newPos = (Position) thisStorm.getPosition().clone();
                    newPos.setX(thisX);
                    newPos.setY(thisY);

                    // Try to find if sector exists
                    final Sector nextSector = SectorManager.getInstance().getByPosition(newPos);

                    if ((nextSector != null) && (nextSector.getTerrain().getId() == TerrainConstants.TERRAIN_O)) {
                        nextSector.setStorm(1);
                        SectorManager.getInstance().update(nextSector);

                        totSize--;
                    }
                }
            }
        }
    }

    /**
     * Move a storm for up to 5 sectors in a random direction.
     *
     * @param thisStorm the storm to move.
     */
    private void moveStorm(final Sector thisStorm) {
        int totMoves = getParent().getGameEngine().getRandomGen().nextInt(5) + 1;
        int attempts = 0;
        Sector theSector = thisStorm;
        while (totMoves > 0 && attempts < 10) {
            // Choose random direction
            final int direction = getParent().getGameEngine().getRandomGen().nextInt(9) + 1;

            // Calculate next sector
            final Position newPos = calculateNextPosition(theSector.getPosition(), direction);

            // Try to find if sector exists
            final Sector nextSector = SectorManager.getInstance().getByPosition(newPos);

            attempts++;

            // Move Storm if this is a sea sector
            if ((nextSector != null) && (nextSector.getTerrain().getId() == TerrainConstants.TERRAIN_O)) {
                theSector.setStorm(0);
                SectorManager.getInstance().update(theSector);

                nextSector.setStorm(2);
                SectorManager.getInstance().update(nextSector);

                theSector = nextSector;
                totMoves--;
            }
        }
    }

    /**
     * Calculate the next position of the storm.
     *
     * @param thisPos   the current position.
     * @param direction the direction of movement.
     * @return the next position.
     */
    private Position calculateNextPosition(final Position thisPos, final int direction) {
        final Position newPos = (Position) thisPos.clone();
        switch (direction) {
            case 1: // NW
                newPos.setX(newPos.getX() - 1);
                newPos.setY(newPos.getY() - 1);
                break;

            case 2: // N
                newPos.setY(newPos.getY() - 1);
                break;

            case 3: // NE
                newPos.setX(newPos.getX() + 1);
                newPos.setY(newPos.getY() - 1);
                break;

            case 4: // E
                newPos.setX(newPos.getX() - 1);
                break;

            case 5: // W
                newPos.setX(newPos.getX() + 1);
                break;

            case 6: // SW
                newPos.setX(newPos.getX() - 1);
                newPos.setY(newPos.getY() + 1);
                break;

            case 7: // S
                newPos.setY(newPos.getY() + 1);
                break;

            case 8: // SE
            default:
                newPos.setX(newPos.getX() + 1);
                newPos.setY(newPos.getY() + 1);
                break;
        }
        return newPos;
    }

    /**
     * Process storms in Arctic Europe.
     */
    private void processArctic() {
        final Game thisGame = getParent().getGameEngine().getGame();

        // Retrieve existing storms from region
        final List<Sector> lstStorms = SectorManager.getInstance().listArcStormsByGame(thisGame);

        // Retrieve sea sectors without any storm
        final List<Sector> lstSea = SectorManager.getInstance().listArcSeaByGame(thisGame);

        if (!lstSea.isEmpty()) {
            LOGGER.info(lstStorms.size() + " storms in Arctic Europe");

            // Process storms based on month
            final int gameMonth = identifyMonth();
            switch (gameMonth) {
                case Calendar.OCTOBER: {
                    // Generate 0..2 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(3);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.NOVEMBER: {
                    // Each existing storm has 20% chance to disappear
                    removeRandomStorm(lstStorms, 20d);

                    // Generate 1..4 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(5) + 1;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.DECEMBER:
                case Calendar.JANUARY:
                case Calendar.FEBRUARY: {
                    // Each existing storm has 25% chance to disappear
                    removeRandomStorm(lstStorms, 25d);

                    // Generate 2..6 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(5) + 2;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.MARCH: {
                    // Each existing storm has 30% chance to disappear
                    removeRandomStorm(lstStorms, 30d);

                    // Generate 1..3 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(3) + 1;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.APRIL: {
                    // Each existing storm has 20% chance to disappear
                    removeRandomStorm(lstStorms, 20d);

                    // Generate 0..2 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(3);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.MAY:
                default:
                    // Remove all storms
                    if (!lstStorms.isEmpty()) {
                        removeAllStorms(lstStorms);
                        LOGGER.info("All storms removed");
                    }
            }
        }
    }

    /**
     * Process storms in Central Europe.
     */
    private void processCentral() {
        final Game thisGame = getParent().getGameEngine().getGame();

        // Retrieve existing storms from region
        final List<Sector> lstStorms = SectorManager.getInstance().listCentralStormsByGame(thisGame);

        // Retrieve sea sectors without any storm
        final List<Sector> lstSea = SectorManager.getInstance().listCentralSeaByGame(thisGame);

        if (!lstSea.isEmpty()) {
            LOGGER.info(lstStorms.size() + " storms in Central Europe");

            // Process storms based on month
            final int gameMonth = identifyMonth();
            switch (gameMonth) {
                case Calendar.NOVEMBER: {
                    // Generate 0..4 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(5);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.DECEMBER:
                case Calendar.JANUARY:
                case Calendar.FEBRUARY: {
                    // Each existing storm has 30% chance to disappear
                    removeRandomStorm(lstStorms, 30d);

                    // Generate 2..5 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(3) + 2;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.MARCH: {
                    // Each existing storm has 40% chance to disappear
                    removeRandomStorm(lstStorms, 40d);

                    // Generate 0..2 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(3);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.APRIL:
                default:
                    // Remove all storms
                    if (!lstStorms.isEmpty()) {
                        removeAllStorms(lstStorms);
                        LOGGER.info("All storms removed");
                    }
            }
        }
    }

    /**
     * Process storms in Mediterranean.
     */
    private void processMed() {
        final Game thisGame = getParent().getGameEngine().getGame();

        // Retrieve existing storms from region
        final List<Sector> lstStorms = SectorManager.getInstance().listMedStormsByGame(thisGame);

        // Retrieve sea sectors without any storm
        final List<Sector> lstSea = SectorManager.getInstance().listMedSeaByGame(thisGame);

        if (!lstSea.isEmpty()) {
            LOGGER.info(lstStorms.size() + " storms in Mediterranean");

            // Process storms based on month
            final int gameMonth = identifyMonth();
            switch (gameMonth) {
                case Calendar.DECEMBER:
                case Calendar.JANUARY:
                case Calendar.FEBRUARY: {
                    // Each existing storm has 35% chance to disappear
                    removeRandomStorm(lstStorms, 35d);

                    // Generate 1..5 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(5) + 1;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.MARCH: {
                    // Each existing storm has 40% chance to disappear
                    removeRandomStorm(lstStorms, 40d);

                    // Generate 0..2 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(3);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.APRIL:
                default:
                    // Remove all storms
                    if (!lstStorms.isEmpty()) {
                        removeAllStorms(lstStorms);
                        LOGGER.info("All storms removed");
                    }
            }
        }
    }

    /**
     * Process storms in Caribbean.
     */
    private void processCaribbean() {
        final Region thisRegion = RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN);
        final Game thisGame = getParent().getGameEngine().getGame();

        // Retrieve existing storms from region
        final List<Sector> lstStorms = SectorManager.getInstance().listStormsByGameRegion(thisGame, thisRegion);

        // Retrieve sea sectors without any storm
        final List<Sector> lstSea = SectorManager.getInstance().listSeaByGameRegion(thisGame, thisRegion, false);

        if (!lstSea.isEmpty()) {
            LOGGER.info(lstStorms.size() + " storms in Caribbean");

            // Process storms based on month
            final int gameMonth = identifyMonth();
            switch (gameMonth) {
                case Calendar.NOVEMBER: {
                    // Generate 6..12 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(7) + 6;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.DECEMBER:
                case Calendar.JANUARY:
                case Calendar.FEBRUARY: {
                    // Remove 0..1 storms
                    final int removeStorms = getParent().getGameEngine().getRandomGen().nextInt(2);
                    removeRandomStorm(lstStorms, removeStorms);

                    // Generate 0..1 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(2);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.MARCH:
                default:
                    // Remove all storms
                    if (!lstStorms.isEmpty()) {
                        removeAllStorms(lstStorms);
                        LOGGER.info("All storms removed");
                    }
            }
        }
    }

    /**
     * Process storms in Indies.
     */
    private void processIndies() {
        final Region thisRegion = RegionManager.getInstance().getByID(RegionConstants.INDIES);
        final Game thisGame = getParent().getGameEngine().getGame();

        // Retrieve existing storms from region
        final List<Sector> lstStorms = SectorManager.getInstance().listStormsByGameRegion(thisGame, thisRegion);

        // Retrieve sea sectors without any storm
        final List<Sector> lstSea = SectorManager.getInstance().listSeaByGameRegion(thisGame, thisRegion, false);

        if (!lstSea.isEmpty()) {

            LOGGER.info(lstStorms.size() + " storms in Indies");

            // Process storms based on month
            final int gameMonth = identifyMonth();
            switch (gameMonth) {
                case Calendar.APRIL: {
                    // Generate 4..10 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(7) + 4;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.MAY:
                case Calendar.JUNE:
                case Calendar.JULY: {
                    // Remove 0..1 storms
                    final int removeStorms = getParent().getGameEngine().getRandomGen().nextInt(2);
                    removeRandomStorm(lstStorms, removeStorms);

                    // Generate 0..1 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(2);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.AUGUST:
                default:
                    // Remove all storms
                    if (!lstStorms.isEmpty()) {
                        removeAllStorms(lstStorms);
                        LOGGER.info("All storms removed");
                    }
            }
        }
    }

    /**
     * Process storms in Africa.
     */
    private void processAfrica() {
        final Region thisRegion = RegionManager.getInstance().getByID(RegionConstants.AFRICA);
        final Game thisGame = getParent().getGameEngine().getGame();

        // Retrieve existing storms from region
        final List<Sector> lstStorms = SectorManager.getInstance().listStormsByGameRegion(thisGame, thisRegion);

        // Retrieve sea sectors without any storm
        final List<Sector> lstSea = SectorManager.getInstance().listSeaByGameRegion(thisGame, thisRegion, false);

        if (!lstSea.isEmpty()) {
            LOGGER.info(lstStorms.size() + " storms in Africa");

            // Process storms based on month
            final int gameMonth = identifyMonth();
            switch (gameMonth) {
                case Calendar.MAY: {
                    // Generate 4..10 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(7) + 4;
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.JUNE:
                case Calendar.JULY:
                case Calendar.AUGUST: {
                    // Remove 0..1 storms
                    final int removeStorms = getParent().getGameEngine().getRandomGen().nextInt(2);
                    removeRandomStorm(lstStorms, removeStorms);

                    // Generate 0..1 storms
                    final int newStorms = getParent().getGameEngine().getRandomGen().nextInt(2);
                    generateNewStorms(lstSea, newStorms);

                    break;
                }

                case Calendar.SEPTEMBER:
                default:
                    // Remove all storms
                    if (!lstStorms.isEmpty()) {
                        removeAllStorms(lstStorms);
                        LOGGER.info("All storms removed");
                    }
            }
        }
    }

    /**
     * Remove storms randomly from given region.
     *
     * @param lstStorms    list of sea sectors in region with storm.
     * @param removeStorms number of storms to remove.
     */
    private void removeRandomStorm(final List<Sector> lstStorms, final int removeStorms) {
        for (int storm = 0; storm < removeStorms; storm++) {
            final int index = getParent().getGameEngine().getRandomGen().nextInt(lstStorms.size());
            lstStorms.get(index).setStorm(0);
            LOGGER.info("Storm centered at " + lstStorms.get(index).getPosition() + " removed");
            SectorManager.getInstance().update(lstStorms.get(index));
        }
    }

    /**
     * Remove storms with a given probability from given region.
     *
     * @param lstStorms list of sea sectors in region with storm.
     * @param chance    probability for a given storm to be removed.
     */
    private void removeRandomStorm(final List<Sector> lstStorms, final double chance) {
        for (final Sector thisSector : lstStorms) {
            final int roll = getParent().getGameEngine().getRandomGen().nextInt(101) + 1;
            if (roll <= chance) {
                thisSector.setStorm(0);
                LOGGER.info("Storm centered at " + thisSector.getPosition() + " removed");
                SectorManager.getInstance().update(thisSector);
            }
        }
    }

    /**
     * Remove all storms from given region.
     *
     * @param lstStorms list of sea sectors in region with storm.
     */
    private void removeAllStorms(final List<Sector> lstStorms) {
        for (final Sector thisSector : lstStorms) {
            thisSector.setStorm(0);
            LOGGER.info("Storm centered at " + thisSector.getPosition() + " removed");
            SectorManager.getInstance().update(thisSector);
        }
    }

    /**
     * Generate new storms randomly on given region.
     *
     * @param lstSea    list of sea sectors in region.
     * @param newStorms number of new storms to generate.
     */
    private void generateNewStorms(final List<Sector> lstSea, final int newStorms) {
        for (int storm = 0; storm < newStorms; storm++) {
            final int index = getParent().getGameEngine().getRandomGen().nextInt(lstSea.size());
            lstSea.get(index).setStorm(2);
            LOGGER.info("New Storm generated with center at " + lstSea.get(index).getPosition());
            SectorManager.getInstance().update(lstSea.get(index));
            lstSea.remove(index);
        }
    }

    /**
     * Identify the month of the game.
     *
     * @return the month of the game.
     */
    private int identifyMonth() {
        // identify month
        final Calendar thisCal = getParent().getGameEngine().calendar();
        return thisCal.get(Calendar.MONTH);
    }


}
