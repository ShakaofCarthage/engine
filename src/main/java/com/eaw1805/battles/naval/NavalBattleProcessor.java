package com.eaw1805.battles.naval;

import com.eaw1805.battles.naval.handtohand.HandToHandCombat;
import com.eaw1805.battles.naval.longrange.R2LongRangeFire;
import com.eaw1805.battles.naval.longrange.R3LongRangeFire;
import com.eaw1805.battles.naval.longrange.SOLLongRangeFire;
import com.eaw1805.battles.naval.result.RoundStat;
import com.eaw1805.battles.naval.result.ShipPair;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.battles.NavalBattleReportManager;
import com.eaw1805.data.managers.fleet.ShipManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.battles.NavalBattleReport;
import com.eaw1805.data.model.fleet.Ship;
import com.eaw1805.data.model.map.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * This class is responsible for executing the naval battles.
 */
public class NavalBattleProcessor
        implements ReportConstants, VPConstants {

    /**
     * Clear weather.
     */
    public static final int WEATHER_CLEAR = 0;

    /**
     * Rainy weather.
     */
    public static final int WEATHER_RAIN = 1;

    /**
     * Stormy weather.
     */
    public static final int WEATHER_STORM = 2;

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(NavalBattleProcessor.class);

    /**
     * The weather of the battle.
     */
    private final transient int weather;

    /**
     * The ships of each side.
     */
    private transient List<Ship>[] sideShips;

    /**
     * The ships of each side grouped by ship class.
     */
    private transient List<Ship>[][] sideShipsSC;

    /**
     * Pair of ships for hand-to-hand combat.
     */
    private transient Set<ShipPair> lstShipPairs;

    /**
     * The random generator.
     */
    private final transient Random randomGen;

    /**
     * The winner of the battle.
     */
    private transient int winner;

    /**
     * Set of nations participating in each side.
     */
    private final transient Set<Nation>[] sideNations;

    /**
     * The game where the battle takes place.
     */
    private transient Game thisGame;

    /**
     * Indicates if this battle is a result of a random event.
     */
    private final transient boolean priracyRandomEvent;

    /**
     * Initializes the TacticalBattleProcessor.
     *
     * @param value         The weather of the battle.
     * @param side1         The first side of the battle.
     * @param side2         The other side of the battle.
     * @param isRandomEvent if this battle is a result of a random event.
     */
    @SuppressWarnings("unchecked")
    public NavalBattleProcessor(final int value,
                                final List<Ship> side1,
                                final List<Ship> side2,
                                final boolean isRandomEvent) {
        weather = value;
        priracyRandomEvent = isRandomEvent;

        final List<Ship>[] shipArray = new ArrayList[2];
        shipArray[0] = side1;
        shipArray[1] = side2;

        // set ships of each side and group per ship class
        setSideShips(shipArray);

        // Set of fleets participating in each side.
        final Set<Integer>[] sideFleets = new Set[2];
        sideNations = new Set[2];
        for (int side = 0; side < 2; side++) {
            if (thisGame == null) {
                thisGame = shipArray[side].get(0).getPosition().getGame();
            }

            sideNations[side] = new HashSet<Nation>();
            sideFleets[side] = new HashSet<Integer>();

            for (final Ship ship : shipArray[side]) {
                sideNations[side].add(ship.getNation());
                if (ship.getFleet() > 0) {
                    sideFleets[side].add(ship.getFleet());
                }
            }
        }

        lstShipPairs = new HashSet<ShipPair>();

        randomGen = new Random();
    }

    /**
     * Access the random number generator instance.
     *
     * @return the random number generator instance.
     */
    public Random getRandomGen() {
        return randomGen;
    }

    /**
     * Get the winner of the battle.
     *
     * @return the winner of the battle.
     */
    public int getWinner() {
        return winner;
    }

    /**
     * Access the weather type where the naval battle takes place.
     *
     * @return the weather of the battle.
     */
    public int getWeather() {
        return weather;
    }

    /**
     * Access the ships of each side.
     *
     * @return the ships of each side.
     */
    public List<Ship>[] getSideShips() {
        return sideShips;
    }

    /**
     * Set the ships of each side.
     *
     * @param value the ships of each side.
     */
    @SuppressWarnings("unchecked")
    public final void setSideShips(final List<Ship>[] value) {
        sideShips = value;

        // Group ships per ship class
        sideShipsSC = new ArrayList[2][6];
        for (int side = 0; side < 2; side++) {
            for (int shipClass = 0; shipClass < 6; shipClass++) {
                sideShipsSC[side][shipClass] = new ArrayList<Ship>();
            }

            for (final Ship ship : sideShips[side]) {
                if (ship.getCapturedByNation() == 0) {
                    sideShipsSC[side][ship.getType().getShipClass()].add(ship);
                }
            }
        }
    }

    /**
     * Access the ships of each side for a particular ship class.
     *
     * @param side      the side.
     * @param shipClass the ship class.
     * @return the ships of a side for a particular ship class.
     */
    public List<Ship> getSideShipsSC(final int side, final int shipClass) {
        return sideShipsSC[side][shipClass];
    }

    /**
     * Get the list of ship pairs as participated in hand-to-hand combat.
     *
     * @return set of ship pairs as participated in hand-to-hand combat.
     */
    public Set<ShipPair> getShipPairs() {
        return lstShipPairs;
    }

    /**
     * Set list of ship pairs as participated in hand-to-hand combat.
     *
     * @param value the set of ship pairs as participated in hand-to-hand combat.
     */
    public void setShipPairs(final Set<ShipPair> value) {
        this.lstShipPairs = value;
    }

    /**
     * Process the tactical battle.
     * See page p.85 of manual.
     *
     * @return the statistics of each of the 15 rounds of the battle.
     */
    public List<RoundStat> process() {
        final List<RoundStat> lstRoundStats = new ArrayList<RoundStat>();

        LOGGER.info("Processing naval battle with weather conditions " + getWeather());

        // Keep track of initial fleet sizes -- Maneuvering of Fleets.
        final RoundStat initStat = new RoundStat(AbstractNavalBattleRound.ROUND_INIT, getSideShips());
        lstRoundStats.add(initStat);

        // Round 1: Long-Range fire of Ships-of-the-Line (50% effectiveness).
        LOGGER.debug("Round 1: Long-Range fire of Ships-of-the-Line (50% effectiveness)");
        final SOLLongRangeFire solLRF = new SOLLongRangeFire(this);
        lstRoundStats.add(solLRF.process());

        // Round 2: Long-Range fire of all warships (75% effectiveness).
        LOGGER.debug("Round 2: Long-Range fire of all warships (75% effectiveness)");
        final R2LongRangeFire r2LRF = new R2LongRangeFire(this);
        lstRoundStats.add(r2LRF.process());

        // Round 3: Long-Range fire of all warships (100% effectiveness).
        LOGGER.debug("Round 3: Long-Range fire of all warships (100% effectiveness)");
        final R3LongRangeFire r3LRF = new R3LongRangeFire(this);
        lstRoundStats.add(r3LRF.process());

        // Round 4: Hand-to-Hand combat of the boarding ships.
        LOGGER.debug("Round 4: Hand-to-Hand combat of the boarding ships");
        final HandToHandCombat hth1Combat = new HandToHandCombat(this, AbstractNavalBattleRound.ROUND_HC_1);
        lstRoundStats.add(hth1Combat.process());

        // Round 5: Hand-to-Hand combat of the boarding ships.
        LOGGER.debug("Round 5: Hand-to-Hand combat of the boarding ships");
        final HandToHandCombat hth2Combat = new HandToHandCombat(this, AbstractNavalBattleRound.ROUND_HC_2);
        lstRoundStats.add(hth2Combat.process());

        // Round 6: Disengagement of ships.
        LOGGER.debug("Round 6: Disengagement of ships");
        final RoundStat finalStat = new RoundStat(AbstractNavalBattleRound.ROUND_DIS, getSideShips());
        lstRoundStats.add(finalStat);

        // End battle: Determination of Winner
        LOGGER.debug("End naval battle: Determination of Winner");
        final DetermineWinner roundDW = new DetermineWinner(this, initStat, finalStat);
        final RoundStat detRound = roundDW.process();
        winner = detRound.getResult();
        lstRoundStats.add(detRound);

        // Round 7: Capturing of merchant ships.
        LOGGER.debug("Round 7: Capturing of merchant ships");
        final CaptureMerchantShips capMS = new CaptureMerchantShips(this, detRound);
        lstRoundStats.add(capMS.process());

        // Aftermath -- Keep track of final fleet sizes
        lstRoundStats.add(new RoundStat(AbstractNavalBattleRound.ROUND_FINAL, getSideShips()));

        return lstRoundStats;
    }

    /**
     * Get the nations participating in one of the sides.
     *
     * @param side the side.
     * @return the nations participating.
     */
    public Set<Nation> getSideNation(final int side) {
        return sideNations[side];
    }

    /**
     * Get the game instance.
     *
     * @return the game instance.
     */
    public Game getGame() {
        return thisGame;
    }

    /**
     * Get if the battle is related to a random event.
     *
     * @return true, if the battle is related to a random event.
     */
    public boolean getPriracyRandomEvent() {
        return priracyRandomEvent;
    }

    /**
     * Entry point for testing purposes.
     *
     * @param args not used.
     */
    public static void main(final String[] args) {
        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);

        // Make sure we have an active transaction
        final Session thatSession = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_S1).getCurrentSession();
        final Transaction thatTrans = thatSession.beginTransaction();

        try {
            // Select troops for side 1
            final List<Ship> thisSide = ShipManager.getInstance().listGameNation(GameManager.getInstance().getByID(1),
                    NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));

            // Select troops for side 2
            final List<Ship> thatSide = ShipManager.getInstance().listGameNation(GameManager.getInstance().getByID(1),
                    NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN));

            final NavalBattleProcessor tbp = new NavalBattleProcessor(WEATHER_CLEAR, thisSide, thatSide, true);
            final List<RoundStat> result = tbp.process();

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(result);
            os.close();
            zos.close();
            baos.close();
            LOGGER.debug(baos.toByteArray().length);

            // Select troops for side 1
            final Position thisPosition = new Position();
            thisPosition.setGame(GameManager.getInstance().getByID(1));
            thisPosition.setRegion(RegionManager.getInstance().getByID(RegionConstants.EUROPE));
            thisPosition.setX(4);
            thisPosition.setY(4);

            final Set<Nation> side1 = new HashSet<Nation>();
            side1.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));

            final Set<Nation> side2 = new HashSet<Nation>();
            side2.add(NationManager.getInstance().getByID(NationConstants.NATION_GREATBRITAIN));

            // Store results
            final NavalBattleReport nbr = new NavalBattleReport();
            nbr.setPosition(thisPosition);
            nbr.setTurn(-1);
            nbr.setSide1(side1);
            nbr.setSide2(side2);
            nbr.setWinner(tbp.getWinner());

            // Save analytical results
            //tbr.setStats(Hibernate.createBlob(baos.toByteArray()));
            nbr.setStats(baos.toByteArray());

            NavalBattleReportManager.getInstance().add(nbr);

        } catch (Exception ex) {
            ex.printStackTrace(); // NOPMD
        }

        thatTrans.commit();
    }

}
