package com.eaw1805.battles.tactical;

import com.eaw1805.battles.tactical.handtohand.CavalryHandToHandCombat;
import com.eaw1805.battles.tactical.handtohand.CavalryPursuit;
import com.eaw1805.battles.tactical.handtohand.DisengagementHandToHandCombat;
import com.eaw1805.battles.tactical.handtohand.TroopHandToHandCombat;
import com.eaw1805.battles.tactical.longrange.ArtilleryLongRangeCombat;
import com.eaw1805.battles.tactical.longrange.DisengagementLongRangeCombat;
import com.eaw1805.battles.tactical.longrange.SkirmishLongRangeCombat;
import com.eaw1805.battles.tactical.longrange.TroopLongRangeCombat;
import com.eaw1805.battles.tactical.morale.MoraleCheckAttackedByCavalry;
import com.eaw1805.battles.tactical.morale.MoraleCheckInitial;
import com.eaw1805.battles.tactical.morale.MoraleCheckIntermediate;
import com.eaw1805.battles.tactical.morale.MoraleCheckLate;
import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.AchievementConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProductionSiteConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.AchievementManager;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.ProfileManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.battles.TacticalBattleReportManager;
import com.eaw1805.data.managers.map.ProductionSiteManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Achievement;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.Profile;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.comparators.BattalionExperience;
import com.eaw1805.data.model.battles.TacticalBattleReport;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * This class is responsible for executing the tactical battles.
 */
public class TacticalBattleProcessor
        implements VPConstants, ReportConstants, NewsConstants {

    /**
     * All troop types.
     */
    public static final int TROOP_TOT = 4;

    /**
     * Infantry troop types.
     */
    public static final int TROOP_INF = 0;

    /**
     * Cavalry troop types.
     */
    public static final int TROOP_CAV = 1;

    /**
     * Artillery troop types.
     */
    public static final int TROOP_ART = 2;

    /**
     * Engineers troop types.
     */
    public static final int TROOP_ENG = 3;

    /**
     * Battalion count.
     */
    public static final int TPE_BATTCNT = 0;

    /**
     * Battalion headcount.
     */
    public static final int TPE_BATTSIZE = 1;

    /**
     * Fleeing Battalion count.
     */
    public static final int TPE_FL_BATTCNT = 2;

    /**
     * Fleeing Battalion headcount.
     */
    public static final int TPE_FL_BATTSIZE = 3;

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TacticalBattleProcessor.class);

    /**
     * The sector where the battle will take place.
     */
    private final transient Sector field;

    /**
     * The fortress of the sector's owner.
     */
    private final transient int fortress;

    /**
     * The battalions of each side.
     */
    private final transient List<Battalion>[] sideBattalions;

    /**
     * The commanders of each side.
     */
    private final transient Commander[] sideCommanders;

    /**
     * The random generator.
     */
    private final transient Random randomGen;

    /**
     * The winner of the battle.
     */
    private int winner;

    /**
     * Set of nations participating in each side.
     */
    private final transient Set<Nation>[] sideNations;

    /**
     * Set of corps participating in each side.
     */
    private final transient Set<Integer>[] sideCorps;

    /**
     * The game where the battle takes place.
     */
    private transient Game thisGame;

    /**
     * Initializes the TacticalBattleProcessor.
     *
     * @param sector    The sector where the battle takes place.
     * @param valueFort The level of the fort (if any).
     * @param side1     The defending army.
     * @param side2     The attacking army.
     * @param comm1     The defending commander.
     * @param comm2     The attacking commander.
     */
    @SuppressWarnings("unchecked")
    public TacticalBattleProcessor(final Sector sector,
                                   final int valueFort,
                                   final List<Battalion> side1,
                                   final List<Battalion> side2,
                                   final Commander comm1,
                                   final Commander comm2) {
        field = sector;
        fortress = valueFort;

        sideBattalions = new ArrayList[2];
        sideBattalions[0] = side1;
        sideBattalions[1] = side2;

        sideCommanders = new Commander[2];
        if (comm1 != null && !comm1.getDead()) {
            sideCommanders[0] = comm1;
        }

        if (comm2 != null && !comm2.getDead()) {
            sideCommanders[1] = comm2;
        }

        randomGen = new Random();
        winner = RoundStatistics.SIDE_NONE;

        sideCorps = new Set[2];
        sideNations = new Set[2];
        for (int side = 0; side < 2; side++) {
            if (thisGame == null) {
                thisGame = sideBattalions[side].get(0).getBrigade().getPosition().getGame();
            }

            sideNations[side] = new HashSet<Nation>();
            sideCorps[side] = new HashSet<Integer>();
            for (final Battalion battalion : sideBattalions[side]) {
                sideNations[side].add(battalion.getBrigade().getNation());
                if (battalion.getBrigade().getCorp() != null && battalion.getBrigade().getCorp() > 0) {
                    sideCorps[side].add(battalion.getBrigade().getCorp());
                }
            }
        }
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
     * Access the sector where the battle takes place.
     *
     * @return the sector where the battle takes place.
     */
    public Sector getField() {
        return field;
    }

    /**
     * Get the region where the battle takes place.
     *
     * @return the region where the battle takes place.
     */
    public Region getRegion() {
        return field.getPosition().getRegion();
    }

    /**
     * Get the level of the fortress (if any).
     *
     * @return the level of the fortress.
     */
    public int getFortress() {
        return fortress;
    }

    /**
     * Process the tactical battle.
     * See page p.85 of manual.
     *
     * @return the statistics of each of the 15 rounds of the battle.
     */
    public List<RoundStatistics> process() {
        final Map<Integer, RoundStatistics> statMap = new HashMap<Integer, RoundStatistics>();
        final List<RoundStatistics> rStats = new ArrayList<empire.battles.tactical.result.RoundStatistics>();

        LOGGER.info("Processing tactical battle in "
                + field.getTerrain().getName()
                + " (" + field.getTerrain().getCode() + ")");

        // Scenario 1808
        // Spanish Nationalism special event
        int[] spanishTroopsInit = new int[2];

        // Clear morale issues & increase experience due to commander
        for (int side = 0; side < 2; side++) {
            for (final Battalion battalion : getSideBattalions()[side]) {
                battalion.setFleeing(false);

                // make sure battalion maximum experience does not overflow calculations
                if (battalion.getExperience() > battalion.getType().getMaxExp() + 2) {
                    battalion.setExperience(battalion.getType().getMaxExp() + 2);
                }

                // make sure minimum experience
                if (battalion.getExperience() < 1) {
                    battalion.setExperience(1);
                }

                // make sure battalion headcount does not overflow calculations
                if (battalion.getHeadcount() > 1000) {
                    battalion.setHeadcount(1000);
                }

                // check minimum headcount
                if (battalion.getHeadcount() < 1) {
                    battalion.setHeadcount(0);
                }

                // Count initial number of spanish troops
                if (NationConstants.NATION_SPAIN == battalion.getType().getNation().getId()) {
                    spanishTroopsInit[side] += battalion.getHeadcount();
                }
            }

            if (sideCommanders[side] != null) {
                // Determine which unit's will increase their experience due to the influence of their commander
                final Battalion[] sortedList = new Battalion[getSideBattalions()[side].size()];
                getSideBattalions()[side].toArray(sortedList);

                // Sort battalions first by experience, then by headcount
                Arrays.sort(sortedList, new BattalionExperience());

                // increase the experience factor of as many of the battalions as the command capability allows.
                final int maxBatt = Math.min(sortedList.length, sideCommanders[side].getComc());
                for (int batt = 0; batt < maxBatt; batt++) {
                    sortedList[batt].setExpIncByComm(true);
                }
            }
        }

        // Keep track of initial army sizes
        RoundStatistics rstat = new RoundStatistics(AbstractTacticalBattleRound.ROUND_INIT, getSideBattalions(), new double[2][4]);
        rStats.add(rstat);
        statMap.put(rstat.getRound(), rstat);

        // Round 1: Artillery long-range combat (Heavy and Light artillery only)
        LOGGER.debug("Round 1: Artillery long-range combat (Artillery fire)");
        final empire.battles.tactical.longrange.ArtilleryLongRangeCombat round1 = new ArtilleryLongRangeCombat(this);
        rstat = round1.process();
        if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
            rStats.add(rstat);
            statMap.put(rstat.getRound(), rstat);
        }


        // No-longer used. All types of artillery fires during round 1.
        // Round 2: Mounted Artillery long-range combat (Mounted artillery only)
        //LOGGER.debug("Round 2: Mounted Artillery long-range combat (Mounted artillery only)");
        //final MountedArtilleryLongRangeCombat round2 = new MountedArtilleryLongRangeCombat(this);
        //rstat = round2.process();
        //if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
        //    rStats.add(rstat);
        //    statMap.put(rstat.getRound(), rstat);
        //}

        // Conditions for a Fortress-Battle
        final int totArtilleryCP = (int) rstat.getSideStat()[1][0];
        final boolean continueBattle;
        if (getFortress() > 0) {
            LOGGER.debug("Fighting against fortified barrack - total attacking CP: " + totArtilleryCP);

            // If by the end of round 2, the assaulting army has inflicted enough damage on the fort's walls
            // (by creating enough "artillery battle points"), then the battle continues and the assaulting army
            // attacks the defenders.
            // If however the assaulting army has not inflicted enough damage ("artillery battle points")
            // by the end of round 2, then it remains at a safe distance from the fort and the battle ends there.
            continueBattle = (totArtilleryCP > getFortBreachLevel());

        } else {
            continueBattle = true;
        }

        if (continueBattle) {
            // Round 3: Morale Check +20%
            LOGGER.debug("Round 3: Morale Check +20%");
            final MoraleCheckInitial round3 = new MoraleCheckInitial(this, statMap);
            rstat = round3.process();
            rStats.add(rstat);
            statMap.put(rstat.getRound(), rstat);

            // Round 4: Skirmishers long-range combat (Sk units with Range of 7)
            LOGGER.debug("Round 4: Skirmishers long-range combat (Sk units with Range of 7)");
            final SkirmishLongRangeCombat round4 = new SkirmishLongRangeCombat(this);
            rstat = round4.process();
            if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                rStats.add(rstat);
                statMap.put(rstat.getRound(), rstat);
            }

            // Round 5: Troop long-range combat (all units with Range of 3-6, all artillery)
            LOGGER.debug("Round 5: Troop long-range combat (all units with Range of 3-6, all artillery)");
            final TroopLongRangeCombat round5 = new TroopLongRangeCombat(this);
            rstat = round5.process();
            if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                rStats.add(rstat);
                statMap.put(rstat.getRound(), rstat);
            }

            // Round 6: Morale Check +10%
            LOGGER.debug("Round 6: Morale Check +10%");
            final MoraleCheckIntermediate round6 = new MoraleCheckIntermediate(this, statMap);
            rstat = round6.process();
            rStats.add(rstat);
            statMap.put(rstat.getRound(), rstat);

            // Round 7: Hand-to-Hand combat (all units)
            LOGGER.debug("Round 7: Hand-to-Hand combat (all units)");
            final TroopHandToHandCombat round7 = new TroopHandToHandCombat(this);
            rstat = round7.process();
            if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                rStats.add(rstat);
                statMap.put(rstat.getRound(), rstat);
            }

            // Round 8: Morale Check
            LOGGER.debug("Round 8: Morale Check");
            final MoraleCheckLate round8 = new MoraleCheckLate(this, statMap);
            rstat = round8.process();
            rStats.add(rstat);
            statMap.put(rstat.getRound(), rstat);

            // Conditions for a Fortress-Battle
            if (getFortress() == 0) {
                // Cavalry units are of limited use in fort battles,
                // therefore round 9 (Cavalry hand-to-hand combat) does not take place in such battles.

                // Round 9: Cavalry Hand-to-Hand combat (cavalry only)
                LOGGER.debug("Round 9: Cavalry Hand-to-Hand combat (cavalry only)");
                final CavalryHandToHandCombat round9 = new CavalryHandToHandCombat(this);
                rstat = round9.process();
                if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                    rStats.add(rstat);
                    statMap.put(rstat.getRound(), rstat);
                }

                // Round 10: Morale Check (only for units attacked by cavalry)
                LOGGER.debug("Round 10: Morale Check (only for units attacked by cavalry)");
                final MoraleCheckAttackedByCavalry round10 = new MoraleCheckAttackedByCavalry(this, statMap);
                rstat = round10.process();
                if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                    rStats.add(rstat);
                    statMap.put(rstat.getRound(), rstat);
                }
            }

            // Round 11: Disengagement Hand-to-Hand combat
            LOGGER.debug("Round 11: Disengagement Hand-to-Hand combat");
            final DisengagementHandToHandCombat round11 = new DisengagementHandToHandCombat(this);
            rstat = round11.process();
            if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                rStats.add(rstat);
                statMap.put(rstat.getRound(), rstat);
            }

            // Round 12: Disengagement long-range combat (Artillery & Skirmish units only)
            LOGGER.debug("Round 12: Disengagement long-range combat (Artillery & Skirmish units only)");
            final DisengagementLongRangeCombat round12 = new DisengagementLongRangeCombat(this);
            rstat = round12.process();
            if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                rStats.add(rstat);
                statMap.put(rstat.getRound(), rstat);
            }
        }

        // End battle: Determination of Winner
        LOGGER.debug("End battle: Determination of Winner");
        final double[][] finalStats = decideWinner(statMap);
        rstat = new RoundStatistics(AbstractTacticalBattleRound.ROUND_WINNER, getSideBattalions(), finalStats);
        rStats.add(rstat);
        statMap.put(rstat.getRound(), rstat);
        final int corpsInvolved = TB_WIN * Math.min(TB_MAX, (sideCorps[RoundStatistics.SIDE_A].size() + sideCorps[RoundStatistics.SIDE_B].size()));

        if (continueBattle) {
            // determine winner
            if (finalStats[0][3] > finalStats[1][3] && finalStats[0][3] / finalStats[1][3] >= 1.6d) {
                winner = RoundStatistics.SIDE_A;
                final int loser = RoundStatistics.SIDE_B;
                LOGGER.debug("1st side won the tactical battle");

                if (thisGame.getGameId() > 0) {
                    changeVP(thisGame, sideNations[winner], corpsInvolved, "Won a tactical battle at " + field.getPosition().toString());
                    changeVP(thisGame, sideNations[loser], corpsInvolved * TB_LOSE, "Lost a tactical battle at " + field.getPosition().toString());

                    // Modify player's profile
                    changeProfile(thisGame, sideNations[winner], ProfileConstants.BATTLES_TACTICAL_WON, 1);
                    changeProfile(thisGame, sideNations[loser], ProfileConstants.BATTLES_TACTICAL_LOST, 1);

                    // Check Achievements for winning Tactical Battles
                    for (final Nation nation : sideNations[winner]) {
                        achievementsTacticalBattles(nation);
                    }

                    // raise lost battle indicator to all loser battalions
                    for (final Battalion battalion : sideBattalions[loser]) {
                        battalion.setHasLost(true);
                    }

                    // Defending a fortress (draw or defender wins)
                    if (fortress == 4) {
                        changeVP(thisGame, sideNations[winner], DEFEND_HUGE, "Defended the huge fortress at " + field.getPosition().toString());

                    } else if (fortress == 3) {
                        changeVP(thisGame, sideNations[winner], DEFEND_LARGE, "Defended the large fortress at " + field.getPosition().toString());
                    }

                    // Scenario 1808
                    // Spanish Nationalism special event
                    if (HibernateUtil.DB_S3 == thisGame.getScenarioId()) {
                        spanishNationalism(spanishTroopsInit, loser);
                    }
                }

            } else if (finalStats[0][3] < finalStats[1][3] && finalStats[1][3] / finalStats[0][3] >= 1.6d) {
                winner = RoundStatistics.SIDE_B;
                final int loser = RoundStatistics.SIDE_A;
                LOGGER.debug("2nd side won the tactical battle");

                if (thisGame.getGameId() > 0) {
                    changeVP(thisGame, sideNations[winner], corpsInvolved, "Won a tactical battle at " + field.getPosition().toString());
                    changeVP(thisGame, sideNations[loser], corpsInvolved * TB_LOSE, "Lost a tactical battle at " + field.getPosition().toString());

                    // Modify player's profile
                    changeProfile(thisGame, sideNations[winner], ProfileConstants.BATTLES_TACTICAL_WON, 1);
                    changeProfile(thisGame, sideNations[loser], ProfileConstants.BATTLES_TACTICAL_LOST, 1);

                    // Check Achievements for winning Tactical Battles
                    for (final Nation nation : sideNations[winner]) {
                        achievementsTacticalBattles(nation);
                    }

                    // raise lost battle indicator to all loser battalions
                    for (final Battalion battalion : sideBattalions[loser]) {
                        battalion.setHasLost(true);
                    }

                    // Scenario 1808
                    // Spanish Nationalism special event
                    if (HibernateUtil.DB_S3 == thisGame.getScenarioId()) {
                        spanishNationalism(spanishTroopsInit, loser);
                    }
                }

                // VPs for winning / conquering a fortress are awarded when sector
                // changes owner (see orderProcessor).

            } else {
                LOGGER.debug("Tactical Battle was undecided");

                if (thisGame.getGameId() > 0) {
                    // Modify player's profile
                    changeProfile(thisGame, sideNations[RoundStatistics.SIDE_A], ProfileConstants.BATTLES_TACTICAL_DRAW, 1);
                    changeProfile(thisGame, sideNations[RoundStatistics.SIDE_B], ProfileConstants.BATTLES_TACTICAL_DRAW, 1);

                    // Attacker managed to conquer a fortress
                    if (fortress == 4) {

                        changeVP(thisGame, sideNations[RoundStatistics.SIDE_A], DEFEND_HUGE, "Successfully defended the huge fortress at " + field.getPosition().toString());


                    } else if (fortress == 3) {
                        changeVP(thisGame, sideNations[RoundStatistics.SIDE_A], DEFEND_LARGE, "Successfully defended the large fortress at " + field.getPosition().toString());
                    }
                }
            }

        } else {
            // Attacker did not manage to breach the walls
            winner = RoundStatistics.SIDE_A;
            final int loser = RoundStatistics.SIDE_B;
            LOGGER.debug("1st side won the tactical battle");

            if (thisGame.getGameId() > 0) {
                changeVP(thisGame, sideNations[winner], corpsInvolved, "Won a tactical battle at " + field.getPosition().toString());
                changeVP(thisGame, sideNations[loser], corpsInvolved * TB_LOSE, "Lost a tactical battle at " + field.getPosition().toString());

                // Modify player's profile
                changeProfile(thisGame, sideNations[winner], ProfileConstants.BATTLES_TACTICAL_WON, 1);
                changeProfile(thisGame, sideNations[loser], ProfileConstants.BATTLES_TACTICAL_LOST, 1);

                // Check Achievements for winning Tactical Battles
                for (final Nation nation : sideNations[winner]) {
                    achievementsTacticalBattles(nation);
                }

                // Defending a fortress (draw or defender wins)
                if (fortress == 4) {
                    changeVP(thisGame, sideNations[winner], DEFEND_HUGE, "Successfully defended the huge fortress at " + field.getPosition().toString());

                } else if (fortress == 3) {
                    changeVP(thisGame, sideNations[winner], DEFEND_LARGE, "Successfully defended the large fortress at " + field.getPosition().toString());
                }

                // raise lost battle indicator to all loser battalions
                for (final Battalion battalion : getSideBattalions()[loser]) {
                    battalion.setHasLost(true);
                }
            }
        }

        // Update players profiles
        if (thisGame.getGameId() > 0) {
            changeProfile(thisGame, sideNations[RoundStatistics.SIDE_A], ProfileConstants.BATTLES_TACTICAL, 1);
            changeProfile(thisGame, sideNations[RoundStatistics.SIDE_B], ProfileConstants.BATTLES_TACTICAL, 1);
        }

        // At the end of a tactical battle at a fortress, a check is made for fortress degrading
        // Conditions for a Fortress-Battle
        boolean pursuitTookPlace = false;
        if (getFortress() > 0) {
            // check destruction of forts.
            determineFortDegradation(totArtilleryCP);

        } else {
            // Cavalry units are of limited use in fort battles,
            // therefore round 13 (Cavalry pursuit) does not take place in such battles.
            // Round 13 : Pursuit
            LOGGER.debug("Round 13 : Pursuit");
            final CavalryPursuit round13 = new CavalryPursuit(this, winner);
            rstat = round13.process();
            if (rstat.getSideStat()[0][0] + rstat.getSideStat()[1][0] > 0) {
                rStats.add(rstat);
                statMap.put(rstat.getRound(), rstat);
                pursuitTookPlace = true;
            }
        }

        // Raise Experience
        LOGGER.debug("Aftermath: Raise Experience");
        final RaiseExperience aftermath = new RaiseExperience(this, winner);
        rstat = aftermath.process();
        rStats.add(rstat);
        statMap.put(rstat.getRound(), rstat);

        // Capture Commanders
        if (winner != RoundStatistics.SIDE_NONE) {
            LOGGER.debug("Aftermath: Capture Commanders");
            final CaptureCommanders capComm = new CaptureCommanders(this, winner, pursuitTookPlace);
            rstat = capComm.process();
            rStats.add(rstat);
            statMap.put(rstat.getRound(), rstat);
        }

        return rStats;
    }

    /**
     * Spanish Nationalism special event.
     *
     * Every time Spanish forces lose a battle, a random number of citizens
     * (between 5% to 30% of Spanish casualties in the battle lost) will reappear in their warehouse.
     *
     * @param spanishTroopsInit -- initial headcount of spanish troop.
     * @param loser -- the side that lost the battle.
     */
    private void spanishNationalism(final int[] spanishTroopsInit, final int loser) {
        // Check if spaniards are among the losers
        boolean spanishInvolved = false;
        for (final Nation nation : sideNations[loser]) {
            if (nation.getId() == NationConstants.NATION_SPAIN) {
                spanishInvolved = true;
                break;
            }
        }

        // Every time Spanish forces lose a battle,
        // a random number of citizens
        // (between 5% to 30% of Spanish casualties in the battle lost)
        // will reappear in their warehouse.
        if (spanishInvolved) {
            // Sector has rebelled !
            LOGGER.info("Spanish side lost in scenario 1808 - Spanish Nationalism special event activated");

            final int roll = getRandomGen().nextInt(25) + 6;
            int spanishTroopsFinal = 0;

            // count final number of spanish troops
            for (final Battalion battalion : getSideBattalions()[loser]) {

                // Count final number of spanish troops
                if (NationConstants.NATION_SPAIN == battalion.getType().getNation().getId()) {
                    spanishTroopsFinal += battalion.getHeadcount();
                }
            }

            final int casualties = spanishTroopsInit[loser] - spanishTroopsFinal;
            final int nationalism = (int) ((casualties * roll) / 100d);

            news(field.getPosition().getGame(), getSideNation(loser),
                    NationManager.getInstance().getByID(NationConstants.NATION_SPAIN), NEWS_MILITARY,
                    nationalism + " spanish deserters from the last battle regained their confidence in Spain and report back in your warehouse for service!");

            // Retrieve warehouse
            final Warehouse spanishWarehouse = WarehouseManager.getInstance().getByNationRegion(thisGame,
                    NationManager.getInstance().getByID(NationConstants.NATION_SPAIN),
                    RegionManager.getInstance().getByID(RegionConstants.EUROPE));

            final Map<Integer, Integer> storedGoods =  spanishWarehouse.getStoredGoodsQnt();
            final int people = storedGoods.get(GoodConstants.GOOD_PEOPLE);
            storedGoods.put(GoodConstants.GOOD_PEOPLE, people + nationalism);
            spanishWarehouse.setStoredGoodsQnt(storedGoods);
            WarehouseManager.getInstance().update(spanishWarehouse);

            LOGGER.info(nationalism + " citizens added to SPAIN/EUROPE warehouse");
        }
    }

    /**
     * Determine which side won the battle.
     *
     * @param roundStats the statistics of each round.
     * @return the
     */
    protected double[][] decideWinner(final Map<Integer, RoundStatistics> roundStats) {
        double[][] stats = new double[2][4];

        // Retrieve initial headcount
        int[] headcount = new int[2];
        for (int side = 0; side < 2; side++) {
            headcount[side] += roundStats.get(AbstractTacticalBattleRound.ROUND_INIT).getArmySizes()[side][TROOP_TOT][TPE_BATTSIZE];
        }

        // Calculate total casualties
        int[] casualties = new int[2];
        final int[] rounds = {AbstractTacticalBattleRound.ROUND_ARTILLERY_HLA,
                AbstractTacticalBattleRound.ROUND_ARTILLERY_MA,
                AbstractTacticalBattleRound.ROUND_LONGRANGE_SK,
                AbstractTacticalBattleRound.ROUND_LONGRANGE,
                AbstractTacticalBattleRound.ROUND_HANDCOMBAT,
                AbstractTacticalBattleRound.ROUND_HANDCOMBAT_CAV,
                AbstractTacticalBattleRound.ROUND_HANDCOMBAT_D,
                AbstractTacticalBattleRound.ROUND_HANDCOMBAT_LR};

        for (final int round : rounds) {
            for (int side = 0; side < 2; side++) {
                if (roundStats.containsKey(round)) {
                    casualties[side] += (int) roundStats.get(round).getSideStat()[side][1];
                }
            }
        }

        // Calculate routed units
        int[] routed = new int[2];
        for (int side = 0; side < 2; side++) {
            final int[][][] armySize = roundStats.get(AbstractTacticalBattleRound.ROUND_INIT).calcArmySizes(getSideBattalions());
            routed[side] += armySize[side][TROOP_TOT][TPE_FL_BATTSIZE];
        }

        // Calculate ratio of casualties
        final double[] ratio = new double[2];
        for (int side = 0; side < 2; side++) {
            if (casualties[side] + routed[side] == 0) {
                ratio[side] = headcount[side];

            } else {
                ratio[side] = headcount[side] / (casualties[side] + routed[side] / 2d);
            }

            // Make sure we will not try to divide by zero
            if (ratio[side] == 0) {
                ratio[side] = 1;
            }

            LOGGER.debug("[" + side + "] init hc=" + headcount[side] + ", cas=" + casualties[side] + ", rout=" + routed[side] + ", ratio=" + ratio[side]);

            stats[side][0] = headcount[side];
            stats[side][1] = casualties[side];
            stats[side][2] = routed[side];
            stats[side][3] = ratio[side];
        }

        return stats;
    }

    /**
     * Determine the necessary combat points to create a breach.
     *
     * @return the required combat points to create a breach.
     */
    public double getFortBreachLevel() {
        // Determine fortress level for side0
        double fortBonus;
        switch (getFortress()) {

            case 1:
                // small fort
                fortBonus = 1000d;
                break;

            case 2:
                // medium fort
                fortBonus = 2000d;
                break;

            case 3:
                // Large fort
                fortBonus = 4000d;
                break;

            case 4:
                // Huge fort
                fortBonus = 8000d;
                break;

            default:
                fortBonus = 1d;
                break;
        }

        return fortBonus;
    }

    /**
     * If one side owns a fortress on the battlefield then they suffer fewer losses.
     * Determine the extra protection based on the size of the fortress.
     *
     * @return the extra protection.
     */
    public double getFortDefenceBonus() {
        // Determine fortress bonus for side0
        double fortBonus;
        switch (getFortress()) {

            case 1:
                // small fort
                fortBonus = .8d;
                break;

            case 2:
                // medium fort
                fortBonus = .65d;
                break;

            case 3:
                // Large fort
                fortBonus = .5d;
                break;

            case 4:
                // Huge fort
                fortBonus = .3d;
                break;

            default:
                fortBonus = 1d;
                break;
        }

        return fortBonus;
    }

    /**
     * All fortress have their own artillery which will take part in Tactical battles.
     * This artillery has long range fire points depending on the size of the fortress.
     *
     * @return the long range fire points.
     */
    public double getFortAttackFactor() {
        // Determine fortress bonus for side0
        double fortBonus;
        switch (getFortress()) {

            case 1:
                // small fort
                fortBonus = 1500d;
                break;

            case 2:
                // medium fort
                fortBonus = 3000d;
                break;

            case 3:
                // Large fort
                fortBonus = 6000d;
                break;

            case 4:
                // Huge fort
                fortBonus = 10000d;
                break;

            default:
                fortBonus = 1d;
                break;
        }

        return fortBonus;
    }

    /**
     * At the end of a tactical battle at a fortress, a check is made for fortress degrading:
     * - if combat points (Artillery+pioneers) of attacker at end of round 2 are LESS than the number of points required for breach, 5% the fort will degrade 1 level.
     * - if combat points (Artillery+pioneers) of attacker at end of round 2 are equal/greater than the number of points required for breach, 10% the fort will degrade 1 level.
     * - if combat points (Artillery+pioneers) of attacker at end of round 2 are TWICE GREATER than the number of points required for breach, 25% the fort will degrade 1 level and 10% will degrade two levels.
     * - if combat points (Artillery+pioneers) of attacker at end of round 2 are THRICE GREATER than the number of points required for breach, 45% the fort will degrade 1 level and 25% will degrade two levels.
     * <p/>
     * All above percentages are DOUBLED if the owner of the fortress LOST the battle. If it was a draw/victory, apply the above percentages.
     *
     * @param totArtilleryCP the combat points (Artillery+pioneers) of attacker at end of round 2.
     */
    private void determineFortDegradation(final int totArtilleryCP) {
        int targetRollOne;
        int targetRollTwo = 0;
        if (totArtilleryCP < getFortBreachLevel()) {
            targetRollOne = 5;

        } else if (totArtilleryCP >= 3 * getFortBreachLevel()) {
            targetRollOne = 45;
            targetRollTwo = 25;

        } else if (totArtilleryCP >= 2 * getFortBreachLevel()) {
            targetRollOne = 25;
            targetRollTwo = 10;

        } else {
            targetRollOne = 10;
        }

        // All above percentages are DOUBLED if the owner of the fortress LOST the battle.
        if (winner == RoundStatistics.SIDE_B) {
            targetRollOne *= 2;
            targetRollTwo *= 2;
        }

        // Random roll
        final int roll = getRandomGen().nextInt(100) + 1;
        if (roll < targetRollTwo) {
            LOGGER.debug("The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was degraded by 2 levels!");

            // Degrade by 2 levels
            switch (field.getProductionSite().getId()) {
                case ProductionSiteConstants.PS_BARRACKS_FS:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was completely destroyed!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was completely destroyed!");

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS));
                    break;

                case ProductionSiteConstants.PS_BARRACKS_FM:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was completely destroyed!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was completely destroyed!");

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS));
                    break;

                case ProductionSiteConstants.PS_BARRACKS_FL:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was degraded by 2 levels!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was degraded by 2 levels!");

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FS));

                    // Check if a complex production site is under construction
                    if (field.getBuildProgress() > 0) {
                        // construction is disrupted
                        field.setBuildProgress(0);
                    }

                    break;

                case ProductionSiteConstants.PS_BARRACKS_FH:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was degraded by 2 levels!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was degraded by 2 levels!");

                    // Check if a complex production site is under construction
                    if (field.getBuildProgress() > 0) {
                        // construction is disrupted
                        field.setBuildProgress(0);
                    }

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FM));
                    break;

                default:
                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS));
                    break;
            }

            // Update records (if not a mock-up battle)
            if (field.getId() > 0) {
                SectorManager.getInstance().update(field);
            }

        } else if (roll < targetRollOne) {
            LOGGER.debug("The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was degraded by 1 level!");

            // Degrade by 1 level
            switch (field.getProductionSite().getId()) {
                case ProductionSiteConstants.PS_BARRACKS_FS:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was completely destroyed!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was completely destroyed!");

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS));
                    break;

                case ProductionSiteConstants.PS_BARRACKS_FM:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was degraded by 1 level!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was degraded by 1 level!");

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FS));
                    break;

                case ProductionSiteConstants.PS_BARRACKS_FL:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was degraded by 1 level!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was degraded by 1 level!");

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FM));

                    // Check if a complex production site is under construction
                    if (field.getBuildProgress() > 0) {
                        // construction is disrupted
                        field.setBuildProgress(0);
                    }
                    break;

                case ProductionSiteConstants.PS_BARRACKS_FH:
                    // Add news entry
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_A), getSideNation(RoundStatistics.SIDE_B).iterator().next(), NEWS_MILITARY, "The " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " was battered by the enemy artillery during the tactical battle and was degraded by 1 level!");
                    news(field.getPosition().getGame(), getSideNation(RoundStatistics.SIDE_B), getSideNation(RoundStatistics.SIDE_A).iterator().next(), NEWS_MILITARY, "Our artillery bombarded the " + field.getProductionSite().getName() + " at " + field.getPosition().toString() + " and damaged the fortification to the point that it was degraded by 1 level!");

                    // Check if a complex production site is under construction
                    if (field.getBuildProgress() > 0) {
                        // construction is disrupted
                        field.setBuildProgress(0);
                    }

                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS_FL));
                    break;

                default:
                    field.setProductionSite(ProductionSiteManager.getInstance().getByID(ProductionSiteConstants.PS_BARRACKS));
                    break;
            }

            // Update records (if not a mock-up battle)
            if (field.getId() > 0) {
                SectorManager.getInstance().update(field);
            }
        }
    }

    /**
     * Access the battalions of each side.
     *
     * @return the battalions of each side.
     */
    public List<Battalion>[] getSideBattalions() {
        return sideBattalions.clone();
    }

    /**
     * Access the commander of each side.
     *
     * @param side to retrieve.
     * @return the commander of each side.
     */
    public Commander getSideCommanders(final int side) {
        return sideCommanders[side];
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
     * Add a news entry for this turn.
     *
     * @param game         the game of the news entry.
     * @param nation       the owner of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param baseNewsId   the base news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected int news(final Game game, final Nation nation, final Nation subject, final int type, final int baseNewsId, final String announcement) {
        final News thisNewsEntry = new News();
        thisNewsEntry.setGame(game);
        thisNewsEntry.setTurn(game.getTurn());
        thisNewsEntry.setNation(nation);
        thisNewsEntry.setSubject(subject);
        thisNewsEntry.setType(type);
        thisNewsEntry.setBaseNewsId(baseNewsId);
        thisNewsEntry.setAnnouncement(false);
        thisNewsEntry.setText(announcement);
        NewsManager.getInstance().add(thisNewsEntry);

        return thisNewsEntry.getNewsId();
    }

    /**
     * Add a news entry for this turn.
     *
     * @param game         the game of the news entry.
     * @param nation       the set of owners of the news entry.
     * @param subject      the subject of the news entry.
     * @param type         the type of the news entry.
     * @param announcement the value of the news entry.
     * @return the ID of the new entry.
     */
    protected void news(final Game game, final Set<Nation> nation, final Nation subject, final int type, final String announcement) {
        int baseNewsId = 0;
        for (Nation thisNation : nation) {
            final News thisNewsEntry = new News();
            thisNewsEntry.setGame(game);
            thisNewsEntry.setTurn(game.getTurn());
            thisNewsEntry.setNation(thisNation);
            thisNewsEntry.setSubject(subject);
            thisNewsEntry.setType(type);
            thisNewsEntry.setBaseNewsId(baseNewsId);
            thisNewsEntry.setAnnouncement(false);
            thisNewsEntry.setText(announcement);
            NewsManager.getInstance().add(thisNewsEntry);

            if (baseNewsId == 0) {
                baseNewsId = thisNewsEntry.getNewsId();
            }
        }
    }

    /**
     * Increase/Decrease the VPs of a nation.
     *
     * @param game        the game instance.
     * @param owner       the Nation to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    protected final void changeVP(final Game game, final Nation owner,
                                  final int increase, final String description) {

        if (game.getGameId() < 0) {
            return;
        }

        final Report thisReport = ReportManager.getInstance().getByOwnerTurnKey(owner,
                game, game.getTurn(), N_VP);

        if (thisReport != null) {
            final int currentVP = Integer.parseInt(thisReport.getValue());

            // Make sure we do not end up with negative VP
            if (currentVP + increase > 0) {
                thisReport.setValue(Integer.toString(currentVP + increase));

            } else {
                thisReport.setValue("0");
            }

            ReportManager.getInstance().update(thisReport);

            // Report addition
            news(game, owner, owner, NEWS_VP, increase, description);

            // Modify player's profile
            changeProfile(game, owner, ProfileConstants.VPS, increase);
        }
    }

    /**
     * Increase/Decrease the VPs of a side of nation.
     *
     * @param game        the game instance.
     * @param nations     the set of Nations to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    protected final void changeVP(final Game game, final Set<Nation> nations,
                                  final int increase, final String description) {
        if (game.getGameId() < 0) {
            return;
        }

        for (final Nation nation : nations) {
            changeVP(game, nation, increase, description);
        }
    }

    /**
     * Increase/Decrease a profile attribute for the player of the nation.
     *
     * @param game     the game instance.
     * @param owner    the Nation to change the profile of the player.
     * @param key      the profile key of the player.
     * @param increase the increase or decrease in the profile entry.
     */
    public final void changeProfile(final Game game, final Nation owner, final String key, final int increase) {
        if (game.getGameId() < 0 || game.getScenarioId() <= HibernateUtil.DB_MAIN || owner.getId() <= 0) {
            return;
        }

        // Retrieve user for particular nation
        final List<UserGame> lstEntries = UserGameManager.getInstance().list(game, owner);
        User user;

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        if (lstEntries.isEmpty()) {
            // retrieve admin
            user = UserManager.getInstance().getByID(2);

        } else {
            // retrieve user
            user = UserManager.getInstance().getByID(lstEntries.get(0).getUserId());

            if (user == null) {
                // Check this
                user = UserManager.getInstance().getByID(2);
            }
        }

        // Retrieve profile entry
        final Profile entry = ProfileManager.getInstance().getByOwnerKey(user, key);

        // If the entry does not exist, create
        if (entry == null) {
            final Profile newEntry = new Profile();
            newEntry.setUser(user);
            newEntry.setKey(key);
            newEntry.setValue(increase);
            ProfileManager.getInstance().add(newEntry);

        } else {
            entry.setValue(entry.getValue() + increase);
            ProfileManager.getInstance().update(entry);
        }

        mainTrans.commit();
    }

    /**
     * Increase/Decrease a profile attribute for the player of the nation.
     *
     * @param game     the game instance.
     * @param nations  the set of Nation to change the profile of the corresponding players.
     * @param key      the profile key of the player.
     * @param increase the increase or decrease in the profile entry.
     */
    public final void changeProfile(final Game game, final Set<Nation> nations, final String key, final int increase) {
        if (game.getGameId() < 0) {
            return;
        }

        for (final Nation nation : nations) {
            changeProfile(game, nation, key, increase);
        }
    }

    /**
     * Get the player of the nation.
     *
     * @param game  the game instance.
     * @param owner the Nation to change the profile of the player.
     * @return the User assigned to this nation.
     */
    public final User getUser(final Game game, final Nation owner) {
        // Ignore Free Scenario
        if (game.getScenarioId() <= HibernateUtil.DB_MAIN) {
            return null;
        }

        // Retrieve user for particular nation
        final List<UserGame> lstEntries = UserGameManager.getInstance().list(game, owner);
        User user;

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        if (lstEntries.isEmpty()) {
            // retrieve admin
            user = UserManager.getInstance().getByID(2);

        } else {
            // retrieve user
            user = UserManager.getInstance().getByID(lstEntries.get(0).getUserId());

            if (user == null) {
                // Check this
                user = UserManager.getInstance().getByID(2);
            }
        }

        mainTrans.commit();

        return user;
    }

    /**
     * Get the profile attribute for the player of the nation.
     *
     * @param game the game instance.
     * @param user the Player to change the profile.
     * @param key  the profile key of the player.
     * @return the value for the profile entry.
     */
    public final int getProfile(final Game game, final User user, final String key) {
        // Ignore Free Scenario
        if (game.getScenarioId() <= HibernateUtil.DB_MAIN) {
            return 0;
        }

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve profile entry
        final Profile entry = ProfileManager.getInstance().getByOwnerKey(user, key);

        mainTrans.commit();

        // If the entry does not exist, create
        if (entry == null) {
            return 0;

        } else {
            return entry.getValue();
        }
    }

    /**
     * Check and set specific achievement.
     *
     * @param nation the Nation to check.
     */
    private void achievementsTacticalBattles(Nation nation) {
        final User owner = getUser(thisGame, nation);
        final int totWonBattles = getProfile(thisGame, owner, ProfileConstants.BATTLES_TACTICAL_WON);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.TACTICAL_L_MIN; level <= AchievementConstants.TACTICAL_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.TACTICAL_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.TACTICAL, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.TACTICAL);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.TACTICAL_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.TACTICAL_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
    }

    /**
     * Check and set specific achievement.
     *
     * @param nation the Nation to check.
     */
    public void achievementsCommandersKilled(Nation nation) {
        final User owner = getUser(thisGame, nation);
        final int totWonBattles = getProfile(thisGame, owner, ProfileConstants.ENEMY_KILLED_COM);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        for (int level = AchievementConstants.COMKILL_L_MIN; level <= AchievementConstants.COMKILL_L_MAX; level++) {
            if (totWonBattles >= AchievementConstants.COMKILL_L[level]
                    && !AchievementManager.getInstance().checkPlayerCategoryLevel(owner, AchievementConstants.COMKILL, level)) {

                // Generate new entry
                final Achievement entry = new Achievement();
                entry.setUser(owner);
                entry.setCategory(AchievementConstants.COMKILL);
                entry.setLevel(level);
                entry.setAnnounced(false);
                entry.setFirstLoad(false);
                entry.setDescription(AchievementConstants.COMKILL_STR[level]);
                entry.setVictoryPoints(0);
                entry.setAchievementPoints(AchievementConstants.COMKILL_AP[level]);
                AchievementManager.getInstance().add(entry);
            }
        }

        mainTrans.commit();
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
            // Fort level
            final int thisFort = 0;

            // Select troops for side 1
            final Position thisPosition = new Position();
            thisPosition.setGame(GameManager.getInstance().getByID(7));
            thisPosition.setRegion(RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN));
            thisPosition.setX(12);
            thisPosition.setY(19);

            final List<Brigade> thisSide = BrigadeManager.getInstance().listByPosition(thisPosition);
            final List<Battalion> thisArmy = new ArrayList<Battalion>();
            for (final Brigade brigade : thisSide) {
                for (final Battalion battalion : brigade.getBattalions()) {
                    thisArmy.add(battalion);
                }
            }

            // Select troops for side 1
            final Position thisPosition2 = new Position();
            thisPosition2.setGame(GameManager.getInstance().getByID(7));
            thisPosition2.setRegion(RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN));
            thisPosition2.setX(12);
            thisPosition2.setY(18);

            final List<Brigade> thisSide2 = BrigadeManager.getInstance().listByPosition(thisPosition2);
            for (final Brigade brigade : thisSide2) {
                for (final Battalion battalion : brigade.getBattalions()) {
                    thisArmy.add(battalion);
                }
            }

            // Select troops for side 1
            final Position thisPosition3 = new Position();
            thisPosition3.setGame(GameManager.getInstance().getByID(7));
            thisPosition3.setRegion(RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN));
            thisPosition3.setX(12);
            thisPosition3.setY(17);

            final List<Brigade> thisSide3 = BrigadeManager.getInstance().listByPosition(thisPosition3);
            for (final Brigade brigade : thisSide3) {
                for (final Battalion battalion : brigade.getBattalions()) {
                    thisArmy.add(battalion);
                }
            }

            final Commander comm1 = CommanderManager.getInstance().getByID(891);

            // Select troops for side 2
            final Position thatPosition = new Position();
            thatPosition.setGame(GameManager.getInstance().getByID(7));
            thatPosition.setRegion(RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN));
            thatPosition.setX(14);
            thatPosition.setY(20);

            final List<Brigade> thatSide = BrigadeManager.getInstance().listByPosition(thatPosition);
            final List<Battalion> thatArmy = new ArrayList<Battalion>();
            for (final Brigade brigade : thatSide) {
                for (final Battalion battalion : brigade.getBattalions()) {
                    thatArmy.add(battalion);
                }
            }

            final Commander comm2 = CommanderManager.getInstance().getByID(610);

            // Retrieve Sector
            final Position sectorPosition = new Position();
            sectorPosition.setGame(GameManager.getInstance().getByID(7));
            sectorPosition.setRegion(RegionManager.getInstance().getByID(RegionConstants.CARIBBEAN));
            sectorPosition.setX(13);
            sectorPosition.setY(20);
            final Sector thisSector = SectorManager.getInstance().getByPosition(sectorPosition);

            final TacticalBattleProcessor tbp = new TacticalBattleProcessor(thisSector, thisFort,
                    thisArmy, thatArmy, comm1, comm2);
            final List<RoundStatistics> result = tbp.process();

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(baos);
            final ObjectOutputStream os = new ObjectOutputStream(zos);
            os.writeObject(result);
            os.close();
            zos.close();
            baos.close();
            LOGGER.debug(baos.toByteArray().length);

            final Set<Nation> side1 = new HashSet<Nation>();
            side1.add(NationManager.getInstance().getByID(6));

            final Set<Nation> side2 = new HashSet<Nation>();
            side2.add(NationManager.getInstance().getByID(4));

            // Store results
            final TacticalBattleReport tbr = new TacticalBattleReport();
            tbr.setPosition(thisPosition);
            tbr.setTurn(-1);
            tbr.setSide1(side1);
            tbr.setSide2(side2);
            tbr.setComm1(comm1);
            tbr.setComm2(comm2);
            tbr.setWinner(tbp.getWinner());
            tbr.setFort("?");

            // Save analytical results
            //tbr.setStats(Hibernate.createBlob(baos.toByteArray()));
            tbr.setStats(baos.toByteArray());

            TacticalBattleReportManager.getInstance().add(tbr);

        } catch (Exception ex) {
            ex.printStackTrace(); // NOPMD
        }

        thatTrans.commit();
    }

}
