package com.eaw1805.core;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.managers.PaymentHistoryManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.ProfileManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.PaymentHistory;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Profile;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import com.eaw1805.events.EventProcessor;
import com.eaw1805.orders.movement.FleetPatrolMovement;
import com.eaw1805.orders.movement.ShipPatrolMovement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Central point of access for making charges to the player accounts.
 */
public final class PaymentManager {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(PaymentManager.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static PaymentManager ourInstance = null;

    /**
     * Private constructor.
     */
    private PaymentManager() {
        // do nothing
    }

    public static PaymentManager getInstance() {
        synchronized (PaymentManager.class) {
            if (ourInstance == null) {
                ourInstance = new PaymentManager();
            }
        }

        return ourInstance;
    }

    private boolean playerIssuedOrders(final Game game, final Nation nation, final int turnID) {
        // check if player has submitted orders 2 turns ago
        final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByGameNation(game, nation, turnID);

        // ignore patrol moves
        int patrolOrdersCnt = 0;
        for (final PlayerOrder order : lstOrders) {
            if (order.getType() == OrderConstants.ORDER_M_UNIT
                    && (Integer.parseInt(order.getParameter5()) == ShipPatrolMovement.ORDER_TYPE
                    || Integer.parseInt(order.getParameter5()) == FleetPatrolMovement.ORDER_TYPE)) {
                patrolOrdersCnt++;
            }
        }

        return (!lstOrders.isEmpty() && (lstOrders.size() > patrolOrdersCnt));
    }

    public void updateAccounts(final GameEngine gameEngine, final Calendar turnCal) {
        final StringBuilder paymentDescr = new StringBuilder();
        paymentDescr.append("Game ");
        paymentDescr.append(gameEngine.getGame().getGameId());
        paymentDescr.append(" / ");

        paymentDescr.append(turnCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH));
        paymentDescr.append(" ");
        paymentDescr.append(turnCal.get(Calendar.YEAR));
        if (gameEngine.getGame().getDescription() != null && gameEngine.getGame().getDescription().length() > 0) {
            paymentDescr.append(" (");
            paymentDescr.append(gameEngine.getGame().getDescription());
            paymentDescr.append(")");
        }

        // We need this to update the player profiles
        final EventProcessor dummyProcessor = new EventProcessor(gameEngine);

        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(gameEngine.getGame().getScenarioId());
        final List<UserGame> lstUserGames = UserGameManager.getInstance().list(gameEngine.getGame());
        for (final UserGame userGame : lstUserGames) {
            // check if nation has died
            if (!gameEngine.isAlive(userGame.getNation())) {
                userGame.setAlive(false);
                userGame.setActive(false);
            }

            // Check if player has issued orders for the previous 2 turns
            if (userGame.isAlive()) {
                final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                final User user = UserManager.getInstance().getByID(userGame.getUserId());
                mainTrans.commit();
                if (user != null
                        && user.getUserId() > 53) {

                    // check if player has submitted orders 2 turns ago
                    final boolean turnTwo = playerIssuedOrders(gameEngine.getGame(), userGame.getNation(), gameEngine.getGame().getTurn() - 2);
                    final boolean turnOne = playerIssuedOrders(gameEngine.getGame(), userGame.getNation(), gameEngine.getGame().getTurn() - 1);

                    // If player has issued order this turn
                    if (turnOne) {
                        // Increase number of turns played
                        dummyProcessor.changeProfile(userGame.getGame(), userGame.getNation(), ProfileConstants.TURNS_PLAYED, 1);
                    }

                    // check also if orders are just patrol
                    if ((!turnOne && userGame.getTurnPickUp() <= gameEngine.getGame().getTurn() - 1)
                            && (!turnTwo && userGame.getTurnPickUp() <= gameEngine.getGame().getTurn() - 2)) {
                        LOGGER.info("Nation [" + userGame.getNation().getName() + "] did not receive any orders for 2 turns. Removing player " + user.getUsername());
                        userGame.setActive(false);
                        userGame.setCurrent(false);
                        userGame.setTurnDrop(userGame.getGame().getTurn());

                        // Check if this position received free credits
                        if (userGame.getOffer() > 0) {
                            final Transaction subTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                            undoSpecialOffer(userGame.getGame(), userGame.getNation(), user, userGame.getOffer());
                            subTrans.commit();
                        }

                        UserGameManager.getInstance().update(userGame);

                        // add new entry
                        final UserGame newUserGame = new UserGame();
                        newUserGame.setGame(gameEngine.getGame());
                        newUserGame.setAlive(true);
                        newUserGame.setNation(userGame.getNation());
                        newUserGame.setCost(userGame.getCost());
                        newUserGame.setHasWon(false);
                        newUserGame.setUserId(2); // Admin
                        newUserGame.setTurnPickUp(0);
                        newUserGame.setTurnDrop(0);
                        newUserGame.setOffer(0);
                        newUserGame.setCurrent(true);
                        UserGameManager.getInstance().add(newUserGame);

                        // todo: check if player is active in another game, otherwise send questionnaire
                    }
                }

                // if still active, make charges
                if (userGame.isActive()) {

                    // check if this game requires a bet
                    if (gameEngine.getGame().getBet() > 0) {
                        if (userGame.getTurnPickUp() == gameEngine.getGame().getTurn() - 1) {
                            // reduce the VPs from the user
                            changeProfile(gameEngine, userGame.getNation(), ProfileConstants.VPS, -gameEngine.getGame().getBet());
                        }
                    }

                    // Make charges
                    if (userGame.getCost() > 0) {
                        chargeUser(userGame, paymentDescr.toString());
                    }

                    // Check if this position received free credits
                    if (userGame.getOffer() > 0) {
                        // deduct used credits
                        userGame.setOffer(userGame.getOffer() - userGame.getCost());

                        // Make sure we do not get negative offered credits
                        if (userGame.getOffer() < 0) {
                            userGame.setOffer(0);
                        }
                    }

                    // Reset newsletter flags
                    userGame.setTurnFirstLoad(true);

                    if (user != null) {
                        //finally update users new last processing date.
                        final Transaction subTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
                        final User reUser = UserManager.getInstance().getByID(userGame.getUserId());
                        reUser.setLastProcDate(new Date());
                        UserManager.getInstance().update(reUser);
                        subTrans.commit();
                    }
                }
            }

            UserGameManager.getInstance().update(userGame);
        }
        theTrans.commit();
    }

    /**
     * Remove remaining free credits and double AP/CP.
     *
     * @param thisGame   the game.
     * @param thisNation the nation.
     * @param thisUser   the user.
     */
    private void undoSpecialOffer(final Game thisGame, final Nation thisNation, final User thisUser, final int credits) {
        final User admin = UserManager.getInstance().getByUserName("admin");

        // Add credits to user
        thisUser.setCreditFree(thisUser.getCreditFree() - credits);
        UserManager.getInstance().update(thisUser);

        admin.setCreditFree(admin.getCreditFree() + credits);
        UserManager.getInstance().update(admin);

        // keep track of history
        final PaymentHistory receiverUserPayment = new PaymentHistory();
        receiverUserPayment.setUser(thisUser);
        receiverUserPayment.setComment("Removing unused FREE credits due to drop of " + thisNation.getName() + " G" + thisGame.getGameId());
        receiverUserPayment.setDate(new Date());
        receiverUserPayment.setAgent(admin);
        receiverUserPayment.setType(PaymentHistory.TYPE_OFFER);
        receiverUserPayment.setCreditFree(thisUser.getCreditFree());
        receiverUserPayment.setCreditBought(thisUser.getCreditBought());
        receiverUserPayment.setCreditTransferred(thisUser.getCreditTransferred());
        receiverUserPayment.setChargeBought(0);
        receiverUserPayment.setChargeFree(-credits);
        receiverUserPayment.setChargeTransferred(0);
        PaymentHistoryManager.getInstance().add(receiverUserPayment);

        final PaymentHistory adminUserPayment = new PaymentHistory();
        adminUserPayment.setUser(admin);
        adminUserPayment.setComment("Removed unused FREE credits from user " + thisUser.getUsername() + " due to drop of " + thisNation.getName() + " G" + thisGame.getGameId());
        adminUserPayment.setDate(new Date());
        adminUserPayment.setAgent(thisUser);
        adminUserPayment.setType(PaymentHistory.TYPE_OFFER);
        adminUserPayment.setCreditFree(admin.getCreditFree());
        adminUserPayment.setCreditBought(admin.getCreditBought());
        adminUserPayment.setCreditTransferred(admin.getCreditTransferred());
        adminUserPayment.setChargeBought(0);
        adminUserPayment.setChargeFree(credits);
        adminUserPayment.setChargeTransferred(0);
        PaymentHistoryManager.getInstance().add(adminUserPayment);
    }

    public void chargeUser(final UserGame position, final String paymentDescr) {
        final StringBuilder paymentText = new StringBuilder();
        paymentText.append(position.getNation().getName());
        paymentText.append(" / ");
        paymentText.append(paymentDescr);

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        final User user = UserManager.getInstance().getByID(position.getUserId());
        final User admin = UserManager.getInstance().getByID(2);

        int remains = position.getCost();

        if (user == null) {
            LOGGER.error("check this, null user? " + position.getUserId());
            mainTrans.commit();
            return;
        }

        // First try to use free credits
        if (user != null && user.getCreditFree() > 0 && remains > 0) {
            // add payment history
            final PaymentHistory trans = new PaymentHistory();
            trans.setUser(user);
            trans.setAgent(admin);
            trans.setType(PaymentHistory.TYPE_PROCESSING);
            user.setCreditFree(user.getCreditFree() - remains);

            int payment = remains;

            if (user.getCreditFree() < 0) {
                remains = -1 * user.getCreditFree();
                payment -= remains;

                user.setCreditFree(0);

            } else {
                remains = 0;
            }

            // track history
            trans.setCreditFree(user.getCreditFree());
            trans.setCreditBought(user.getCreditBought());
            trans.setCreditTransferred(user.getCreditTransferred());

            // add amount
            trans.setChargeFree(-payment);
            trans.setDate(new Date());
            trans.setComment(paymentText.toString());
            PaymentHistoryManager.getInstance().add(trans);
        }

        // then try to use transferred credits
        if (remains > 0 && user.getCreditTransferred() > 0) {
            // add payment history
            final PaymentHistory trans = new PaymentHistory();
            trans.setUser(user);
            trans.setAgent(admin);
            trans.setType(PaymentHistory.TYPE_PROCESSING);

            user.setCreditTransferred(user.getCreditTransferred() - remains);
            int payment = remains;

            if (user.getCreditTransferred() < 0) {
                remains = -1 * user.getCreditTransferred();
                payment -= remains;
                user.setCreditTransferred(0);

            } else {
                remains = 0;
            }

            // track history
            trans.setCreditFree(user.getCreditFree());
            trans.setCreditBought(user.getCreditBought());
            trans.setCreditTransferred(user.getCreditTransferred());

            // add amount
            trans.setChargeTransferred(-payment);
            trans.setDate(new Date());
            trans.setComment(paymentText.toString());
            PaymentHistoryManager.getInstance().add(trans);
        }

        // then try to use paid credits
        if (remains > 0) {
            // add payment history
            final PaymentHistory trans = new PaymentHistory();
            trans.setUser(user);
            trans.setAgent(admin);
            trans.setType(PaymentHistory.TYPE_PROCESSING);

            user.setCreditBought(user.getCreditBought() - remains);

            // track history
            trans.setCreditFree(user.getCreditFree());
            trans.setCreditBought(user.getCreditBought());
            trans.setCreditTransferred(user.getCreditTransferred());

            // add amount
            trans.setChargeBought(-remains);
            trans.setDate(new Date());
            trans.setComment(paymentText.toString());
            PaymentHistoryManager.getInstance().add(trans);

            if (user.getCreditBought() < 0) {
                // negative balance
                try {
                    EmailManager.getInstance().sendCreditAlert(user, position);

                } catch (Exception ex) {
                    LOGGER.debug("Unable to send credit alert to " + user.getUsername() + " / " + user.getEmail());
                }
            }
        }

        UserManager.getInstance().update(user);
        mainTrans.commit();
    }

    /**
     * Increase/Decrease a profile attribute for the player of the nation.
     *
     * @param gameEngine the game engine instance.
     * @param owner      the Nation to change the profile of the player.
     * @param key        the profile key of the player.
     * @param increase   the increase or decrease in the profile entry.
     */
    public final void changeProfile(final GameEngine gameEngine, final Nation owner, final String key, final int increase) {
        final Game game = gameEngine.getGame();
        // Ignore Free Scenario
        if (game.getGameId() < 0 || game.getScenarioId() <= HibernateUtil.DB_MAIN || owner.getId() <= 0) {
            return;
        }

        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);

        // Retrieve user for particular nation
        User user = gameEngine.getUser(owner);

        if (user == null) {
            // Check this
            user = UserManager.getInstance().getByID(2);
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

}
