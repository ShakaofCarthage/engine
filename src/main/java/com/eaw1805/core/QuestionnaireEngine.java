package com.eaw1805.core;


import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.ProfileConstants;
import com.eaw1805.data.managers.ProfileManager;
import com.eaw1805.data.managers.QuestionnaireManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.model.Profile;
import com.eaw1805.data.model.Questionnaire;
import com.eaw1805.data.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class QuestionnaireEngine {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(QuestionnaireEngine.class);

    /**
     * Run this to check for users that should send the questionnaire.
     *
     * @param args Program arguments.
     */
    public static void main(final String[] args) {
        LOGGER.info("Questionnaire engine");
        final QuestionnaireEngine engine = new QuestionnaireEngine();
        final Calendar today = Calendar.getInstance();

        HibernateUtil.connectEntityManagers(HibernateUtil.DB_FREE);
        final Transaction thatTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        final List<User> users = UserManager.getInstance().list();
        int countActive = 0;
        int countInactive = 0;
        int countNewInactive = 0;
        //first check if there is anyone who you should send help email
        final Transaction freeTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_FREE);
        for (final User user : users) {

            //if you already sent help... don't do it again
            if (user.isHelpSent()) {
//                LOGGER.info("skipping user " + user.getUsername() + " : already sent");
                continue;
            }
            final Profile profile = ProfileManager.getInstance().getByOwnerKey(user, ProfileConstants.TURNS_PLAYED_SOLO);

            //if player played solo game then don't send email.
            if (profile != null && profile.getValue() > 1) {
//                LOGGER.info("skipping user " + user.getUsername() + " : played more than 1 turns");
                continue;
            }
            final Date regDate = new Date(user.getDateJoin() * 1000);
            final int hours = Hours.hoursBetween(new DateTime(regDate), new DateTime(today.getTime())).getHours();
            //if hours are less than 48 then don't send email.
            if (hours < 48) {
//                LOGGER.info("skipping user " + user.getUsername() + " : hours passed " + hours);
                continue;
            }
            //now send email
            LOGGER.info("Help email - sending :" + user.getUsername() + " after " + hours + " hours");

            //send the actual email.


            EmailManager.getInstance().sendHelpEmail(user, UserGameManager.getInstance().list(user));


            //finally update user
            user.setHelpSent(true);
            UserManager.getInstance().update(user);
        }
        freeTrans.commit();

        for (final User user : users) {
            //check that user has last proc date
            if (user.getLastProcDate() == null) {
                continue;
            }

            //calculate days between the end of the game and the process.
            int days = Days.daysBetween(new DateTime(user.getLastProcDate()), new DateTime(today.getTime())).getDays();
            if (days > 30 &&
                    (user.getQuestionnaireUUID() == null ||
                     user.getQuestionnaireUUID().isEmpty())) {//if is not empty.. then you've already sent him.

                //be sure he hasn't already filled this questionnaire in the past.
                final List<Questionnaire> questionnaires = QuestionnaireManager.getInstance().list(user);

                if (questionnaires.isEmpty()) {
                    countInactive++;
                    countNewInactive++;

                    //set a valid uuid.
                    user.setQuestionnaireUUID(engine.generateUUID());
                    UserManager.getInstance().update(user);
                    LOGGER.info("Questionnaire :" + user.getUsername() + " - sending - " + user.getQuestionnaireUUID());

                    //send the actual email.
                    final boolean result = EmailManager.getInstance().sendQuestionnaire(user);
                    if (!result) {
                        // failed to send mail
                        user.setQuestionnaireUUID(null);
                        UserManager.getInstance().update(user);
                    }
                } else {
                    countInactive++;
                    LOGGER.info("Questionnaire :" + user.getUsername() + " - already filled, skipping");
                }
            } else if (days <= 15) {
                countActive++;
                //no need to log this I guess
//                LOGGER.info(user.getUsername() + " - active player - " + user.getLastProcDate());
            } else if (user.getQuestionnaireUUID() != null && !user.getQuestionnaireUUID().isEmpty()) {
                countInactive++;
                LOGGER.info("Questionnaire :" + user.getUsername() + " - already sent, skipping - " + user.getQuestionnaireUUID());
            }
        }
        thatTrans.commit();
        LOGGER.info("Questionnaire :-- Active : " + countActive + ", Inactive : " + countInactive + ", New Inactive : " + countNewInactive);

        // close sessions
        HibernateUtil.getInstance().closeSessions();
    }

    /**
     * Generates a uuid that will always be unique in the database.
     *
     * @return A unique id.
     */
    public String generateUUID() {
        String out = UUID.randomUUID().toString();
        //ensure that this uuid will never be duplicate in the database.
        while (UserManager.getInstance().getByQuestionnaireUUID(out) != null) {
            out = UUID.randomUUID().toString();
        }
        return out;
    }
}
