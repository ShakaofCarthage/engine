package com.eaw1805.battles.tactical;

import com.eaw1805.battles.tactical.result.RoundStatistics;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.army.ArmyManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.army.CorpManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Army;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.army.Corp;
import com.eaw1805.data.model.map.CarrierInfo;

import java.util.ArrayList;

/**
 * Each leading commander may be captured by the enemy troops.
 */
public class CaptureCommanders
        extends AbstractTacticalBattleRound
        implements VPConstants {

    /**
     * the side that won the battle or -1 if undecided.
     */
    private final transient int winningSide;

    /**
     * if a pursuit took place.
     */
    private final transient boolean pursuitTookPlace;

    /**
     * Default constructor.
     *
     * @param caller  the processor requesting the execution of this round.
     * @param winner  the side that won the battle or -1 if undecided.
     * @param pursuit if a pursuit took place earlier in the battle.
     */
    public CaptureCommanders(final TacticalBattleProcessor caller, final int winner, final boolean pursuit) {
        super(caller, ROUND_CAPTURE_COMM);
        winningSide = winner;
        pursuitTookPlace = pursuit;
    }

    /**
     * Process the round.
     *
     * @return the statistics of the round.
     */
    public RoundStatistics process() {
        final double[] combatPoints = new double[2];
        final double[][] statRound = new double[2][4];

        if (winningSide == RoundStatistics.SIDE_NONE) {
            // Each commander has a 2% chance of getting killed
            for (int side = 0; side < 2; side++) {
                final int randomKill = getParent().getRandomGen().nextInt(100) + 1;
                if (randomKill <= 2) {
                    // Commander captured
                    statRound[side][2] = 1;

                    // Retrieve commander
                    final Commander thisCommander = getParent().getSideCommanders(side);
                    if (thisCommander != null) {
                        // Add news entry
                        getParent().news(thisCommander.getPosition().getGame(), getParent().getSideNation(side), getParent().getSideNation((side + 1) % 2).iterator().next(), NEWS_MILITARY, "Our commander (" + thisCommander.getName() + ") was killed in action during the battle at " + getParent().getField().getPosition().toString());
                        getParent().news(thisCommander.getPosition().getGame(), getParent().getSideNation((side + 1) % 2), getParent().getSideNation(side).iterator().next(), NEWS_MILITARY, "We killed the enemy commander (" + thisCommander.getName() + ") during the battle at " + getParent().getField().getPosition().toString());

                        // Lost commander in battle
                        getParent().changeVP(thisCommander.getPosition().getGame(), getParent().getSideNation(side), COMM_LOST, "Commander (" + thisCommander.getName() + ") killed in battle");

                        // Killed enemy commander in battle
                        getParent().changeVP(thisCommander.getPosition().getGame(), getParent().getSideNation((side + 1) % 2), COMM_KILL_OPPONENT, "Killing enemy commander (" + thisCommander.getName() + ") in battle");

                        // Also keep track of prisoners in player's profile
                        getParent().changeProfile(thisCommander.getPosition().getGame(), getParent().getSideNation((side + 1) % 2), ProfileConstants.ENEMY_KILLED_COM, 1);

                        // Check Achievements for killing an enemy commander
                        for (final Nation nation : getParent().getSideNation((side + 1) % 2)) {
                            getParent().achievementsCommandersKilled(nation);
                        }

                        // remove commander from command
                        if (thisCommander.getPosition().getGame().getGameId() > 0) {
                            removeCommander(thisCommander);
                        }

                        thisCommander.setDead(true);

                        // remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        thisCommander.setCarrierInfo(thisCarrying);

                        CommanderManager.getInstance().update(thisCommander);
                    }
                }
            }

        } else {
            final int losingSide = (winningSide + 1) % 2;

            // Calculate combat points
            for (int side = 0; side < 2; side++) {
                combatPoints[side] = 0;
                if (side == winningSide) {
                    // There is a 3% chance of the losing commander to be captured.
                    combatPoints[winningSide] = 3;

                    // On top of that we calculate the LC cavalry battalions.
                    for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                        if (battalion.getType().getTroopSpecsLc()
                                && !battalion.isFleeing()
                                && battalion.getHeadcount() >= 400) {

                            combatPoints[side]++;
                        }
                    }

                    // Keep track of LC battalions.
                    statRound[side][0] = combatPoints[side];

                } else {
                    for (final Battalion battalion : getParent().getSideBattalions()[side]) {
                        combatPoints[side] += battalion.getHeadcount();
                    }

                    // Keep track of headcount at the end of battle.
                    statRound[side][1] = combatPoints[side];
                }
            }

            // The commander of the losing army has a 8% chance of dying on the field of honor.
            final int randomKilled = getParent().getRandomGen().nextInt(100) + 1;
            if (randomKilled <= 8) {
                // Commander captured
                statRound[losingSide][2] = 1;

                // Retrieve commander
                final Commander losingCommander = getParent().getSideCommanders(losingSide);
                if (losingCommander != null) {
                    // Add news entry
                    getParent().news(losingCommander.getPosition().getGame(), getParent().getSideNation(losingSide), losingCommander.getNation(), NEWS_MILITARY, "Our commander (" + losingCommander.getName() + ") was killed in action during the battle at " + getParent().getField().getPosition().toString());
                    getParent().news(losingCommander.getPosition().getGame(), getParent().getSideNation(winningSide), losingCommander.getNation(), NEWS_MILITARY, "We killed the enemy commander (" + losingCommander.getName() + ") during the battle at " + getParent().getField().getPosition().toString());

                    // Lost commander in battle
                    getParent().changeVP(losingCommander.getPosition().getGame(), getParent().getSideNation(losingSide), COMM_LOST, "Commander (" + losingCommander.getName() + ") killed in battle");

                    // Killed enemy commander in battle
                    getParent().changeVP(losingCommander.getPosition().getGame(), getParent().getSideNation(winningSide), COMM_KILL_OPPONENT, "Killing enemy commander (" + losingCommander.getName() + ") in battle");

                    // Also keep track of prisoners in player's profile
                    getParent().changeProfile(losingCommander.getPosition().getGame(), getParent().getSideNation(winningSide), ProfileConstants.ENEMY_KILLED_COM, 1);

                    // Check Achievements for killing an enemy commander
                    for (final Nation nation : getParent().getSideNation(winningSide)) {
                        getParent().achievementsCommandersKilled(nation);
                    }

                    // remove commander from command
                    if (losingCommander.getPosition().getGame().getGameId() > 0) {
                        removeCommander(losingCommander);
                    }

                    losingCommander.setDead(true);

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    losingCommander.setCarrierInfo(thisCarrying);

                    CommanderManager.getInstance().update(losingCommander);
                }
            } else {
                // After each battle, there is a chance that the leading commander of the losing side may be captured.

                // By default use 3% chance in case no side won the battle
                double targetRoll = 3d;

                // If all forces of a side were completely annihilated, then 1% per LC battalion the winning side has.
                if (combatPoints[losingSide] == 0) {
                    // maximum chance 50%
                    combatPoints[winningSide] = Math.min(50, combatPoints[winningSide]);

                } else if (pursuitTookPlace) {
                    // if a side lost the field battle, and a pursuit took place, then 0,5% per LC battalion the winning side has.
                    combatPoints[winningSide] *= 0.5d;

                    // maximum chance 33%
                    combatPoints[winningSide] = Math.min(33, combatPoints[winningSide]);
                }

                targetRoll = combatPoints[winningSide];

                // Roll chances
                final int randomRoll = getParent().getRandomGen().nextInt(100) + 1;
                if (randomRoll <= targetRoll) {
                    // Commander died
                    statRound[losingSide][2] = 1;

                    // pick a random side
                    ArrayList<Nation> winners = new ArrayList<Nation>(getParent().getSideNation(winningSide));
                    java.util.Collections.shuffle(winners);

                    // Retrieve commander
                    final Commander capturedCommander = getParent().getSideCommanders(losingSide);
                    if (capturedCommander != null) {
                        // Add news entry
                        getParent().news(capturedCommander.getPosition().getGame(), getParent().getSideNation(losingSide), capturedCommander.getCaptured(), NEWS_MILITARY, "Our commander (" + capturedCommander.getName() + ") was captured by enemy forces during the battle at " + getParent().getField().getPosition().toString());
                        getParent().news(capturedCommander.getPosition().getGame(), getParent().getSideNation(winningSide), capturedCommander.getNation(), NEWS_MILITARY, "We captured the enemy commander (" + capturedCommander.getName() + ") during the battle at " + getParent().getField().getPosition().toString());

                        // Lost commander in battle
                        getParent().changeVP(capturedCommander.getPosition().getGame(), getParent().getSideNation(losingSide), COMM_LOST, "Commander (" + capturedCommander.getName() + ") captured in battle");

                        // Killed enemy commander in battle
                        getParent().changeVP(capturedCommander.getPosition().getGame(), getParent().getSideNation(winningSide), COMM_KILL_OPPONENT, "Capturing enemy commander (" + capturedCommander.getName() + ") in battle");

                        // Also keep track of prisoners in player's profile
                        getParent().changeProfile(capturedCommander.getPosition().getGame(), getParent().getSideNation(winningSide), ProfileConstants.ENEMY_KILLED_COM, 1);

                        // remove commander from command
                        if (capturedCommander.getPosition().getGame().getGameId() > 0) {
                            removeCommander(capturedCommander);
                        }

                        // Capture commander
                        capturedCommander.setCaptured(winners.get(0));

                        // remove carrier info
                        final CarrierInfo thisCarrying = new CarrierInfo();
                        thisCarrying.setCarrierType(0);
                        thisCarrying.setCarrierId(0);
                        capturedCommander.setCarrierInfo(thisCarrying);

                        CommanderManager.getInstance().update(capturedCommander);
                    }
                }
            }

            // The commander of the winning army has a 2% chance of dying on the field of honor.
            final int randomWinnerKilled = getParent().getRandomGen().nextInt(100) + 1;
            if (randomWinnerKilled <= 2) {
                // Commander captured
                statRound[winningSide][2] = 1;

                // Retrieve commander
                final Commander winningCommander = getParent().getSideCommanders(winningSide);
                if (winningCommander != null) {
                    // Add news entry
                    getParent().news(winningCommander.getPosition().getGame(), getParent().getSideNation(winningSide), winningCommander.getNation(), NEWS_MILITARY, "Our commander (" + winningCommander.getName() + ") was killed in action during the battle at " + getParent().getField().getPosition().toString());
                    getParent().news(winningCommander.getPosition().getGame(), getParent().getSideNation(losingSide), winningCommander.getNation(), NEWS_MILITARY, "We killed the enemy commander (" + winningCommander.getName() + ") during the battle at " + getParent().getField().getPosition().toString());

                    // Lost commander in battle
                    getParent().changeVP(winningCommander.getPosition().getGame(), getParent().getSideNation(winningSide), COMM_LOST, "Commander (" + winningCommander.getName() + ") killed in battle");

                    // Killed enemy commander in battle
                    getParent().changeVP(winningCommander.getPosition().getGame(), getParent().getSideNation(losingSide), COMM_KILL_OPPONENT, "Killing enemy commander (" + winningCommander.getName() + ") in battle");

                    // Also keep track of prisoners in player's profile
                    getParent().changeProfile(winningCommander.getPosition().getGame(), getParent().getSideNation(losingSide), ProfileConstants.ENEMY_KILLED_COM, 1);

                    // Check Achievements for killing an enemy commander
                    for (final Nation nation : getParent().getSideNation(losingSide)) {
                        getParent().achievementsCommandersKilled(nation);
                    }

                    // remove commander from command
                    if (winningCommander.getPosition().getGame().getGameId() > 0) {
                        removeCommander(winningCommander);
                    }

                    winningCommander.setDead(true);

                    // remove carrier info
                    final CarrierInfo thisCarrying = new CarrierInfo();
                    thisCarrying.setCarrierType(0);
                    thisCarrying.setCarrierId(0);
                    winningCommander.setCarrierInfo(thisCarrying);

                    CommanderManager.getInstance().update(winningCommander);
                }
            }

        }

        return new RoundStatistics(getRound(), getParent().getSideBattalions(), statRound);
    }

    /**
     * Strip Army from any commander in charge.
     *
     * @param armyId the id of the unit.
     */
    protected void removeFromArmy(final int armyId) {
        // Retrieve army
        final Army thisArmy = ArmyManager.getInstance().getByID(armyId);

        if (thisArmy != null) {

            // remove commander
            thisArmy.setCommander(null);

            // update entity
            ArmyManager.getInstance().update(thisArmy);
        }
    }

    /**
     * Strip Corps from any commander in charge.
     *
     * @param corpId the id of the unit.
     */
    protected void removeFromCorp(final int corpId) {
        // Retrieve corp
        final Corp thisCorp = CorpManager.getInstance().getByID(corpId);

        if (thisCorp != null) {

            // remove commander
            thisCorp.setCommander(null);

            // update entity
            CorpManager.getInstance().update(thisCorp);
        }
    }

    /**
     * Strip commander from any army/corps he is in charge
     *
     * @param thisComm the commander.
     */
    protected void removeCommander(final Commander thisComm) {
        if (thisComm.getArmy() != 0) {
            removeFromArmy(thisComm.getArmy());
            thisComm.setArmy(0);
        }

        if (thisComm.getCorp() != 0) {
            removeFromCorp(thisComm.getCorp());
            thisComm.setCorp(0);
        }
    }

}
