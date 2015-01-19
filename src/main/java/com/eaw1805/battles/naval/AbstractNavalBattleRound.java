package com.eaw1805.battles.naval;

import com.eaw1805.battles.naval.result.RoundStat;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.AchievementConstants;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.constants.VPConstants;
import com.eaw1805.data.managers.AchievementManager;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.ProfileManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.model.Achievement;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.Profile;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Set;

/**
 * Constitutes a common ancestor for all round processors for naval battles.
 */
public abstract class AbstractNavalBattleRound
        implements ReportConstants, VPConstants, NewsConstants {

    /**
     * Maneuvering of Fleets.
     */
    public final static int ROUND_INIT = 0;

    /**
     * Round 1: Long-Range fire of Ships-of-the-Line (50% effectiveness).
     */
    public final static int ROUND_LR_SOL = 1;

    /**
     * Round 2: Long-Range fire of all warships (75% effectiveness).
     */
    public final static int ROUND_LR_1 = 2;

    /**
     * Round 3: Long-Range fire of all warships (100% effectiveness).
     */
    public final static int ROUND_LR_2 = 3;

    /**
     * Round 4: Hand-to-Hand combat of the boarding ships.
     */
    public final static int ROUND_HC_1 = 4;

    /**
     * Round 5: Hand-to-Hand combat of the boarding ships.
     */
    public final static int ROUND_HC_2 = 5;

    /**
     * Round 6: Disengagement of ships.
     */
    public final static int ROUND_DIS = 6;

    /**
     * Determination of Winner.
     */
    public final static int ROUND_DET = 7;

    /**
     * Round 7: Capturing of merchant ships.
     */
    public final static int ROUND_CAP = 8;

    /**
     * Aftermath.
     */
    public final static int ROUND_FINAL = 9;

    /**
     * The round of the battle.
     */
    private int round;

    /**
     * The parent engine.
     */
    private final transient NavalBattleProcessor parent;

    /**
     * Default constructor.
     *
     * @param caller the processor requesting the execution of this round.
     */
    public AbstractNavalBattleRound(final NavalBattleProcessor caller) {
        parent = caller;
    }

    /**
     * Get the parent engine.
     *
     * @return instance of the parent engine.
     */
    protected NavalBattleProcessor getParent() {
        return parent;
    }

    /**
     * Get the round of the battle.
     *
     * @return the round of the battle.
     */
    public int getRound() {
        return round;
    }

    /**
     * Set the round of the battle.
     *
     * @param value the round of the battle.
     */
    public void setRound(final int value) {
        this.round = value;
    }

    /**
     * Execute the round of the naval battle.
     *
     * @return the result of the round.
     */
    public abstract RoundStat process();

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
     * Increase/Decrease the VPs of a nation.
     *
     * @param game        the Game.
     * @param owner       the Nation to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    protected final void changeVP(final Game game, final Nation owner, final int increase, final String description) {
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

            // Report VP addition in player's achievements list
            achievement(game, owner, AchievementConstants.VPS, AchievementConstants.LEVEL_1, description, 0, increase);
        }
    }

    /**
     * Increase/Decrease the VPs of a side of nation.
     *
     * @param game        the Game.
     * @param nations     the set of Nations to change VPs.
     * @param increase    the increase or decrease in VPs.
     * @param description the description of the VP change.
     */
    protected final void changeVP(final Game game, final Set<Nation> nations, final int increase, final String description) {
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
        // Ignore Free Scenario
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

            // Make sure players do not end up with negative VPs
            if (!key.equalsIgnoreCase(ProfileConstants.VPS) || increase > 0) {
                newEntry.setValue(increase);
            } else {
                newEntry.setValue(0);
            }

            ProfileManager.getInstance().add(newEntry);

        } else {

            // Make sure players do not end up with negative VPs
            if (!key.equalsIgnoreCase(ProfileConstants.VPS) || entry.getValue() + increase > 0) {
                entry.setValue(entry.getValue() + increase);

            } else {
                entry.setValue(0);
            }

            ProfileManager.getInstance().update(entry);
        }

        mainTrans.commit();
    }

    /**
     * Add an achievement entry for the particular player.
     *
     * @param game        the game related to the achievement.
     * @param owner       the nation related to the achievement.
     * @param category    the achievement category.
     * @param level       the achievement level.
     * @param description the description of the achievement.
     * @param vp          the vps related to the achievement.
     * @param ap          the achievement points related to the achievement.
     */
    public void achievement(final Game game, final Nation owner,
                            final int category, final int level,
                            final String description,
                            final int vp, final int ap) {
        // Ignore Free Scenario
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

        // Generate new entry
        final Achievement entry = new Achievement();
        entry.setUser(user);
        entry.setCategory(category);
        entry.setLevel(level);
        entry.setAnnounced(false);
        entry.setFirstLoad(false);
        entry.setDescription(description);
        entry.setVictoryPoints(vp);
        entry.setAchievementPoints(ap);

        AchievementManager.getInstance().add(entry);
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
        for (final Nation nation : nations) {
            changeProfile(game, nation, key, increase);
        }
    }

}
