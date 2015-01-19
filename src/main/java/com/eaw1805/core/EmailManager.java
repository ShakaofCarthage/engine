package com.eaw1805.core;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.NewsConstants;
import com.eaw1805.data.constants.OrderConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.NewsManager;
import com.eaw1805.data.managers.PlayerOrderManager;
import com.eaw1805.data.managers.ReportManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.managers.UserManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.News;
import com.eaw1805.data.model.PlayerOrder;
import com.eaw1805.data.model.Report;
import com.eaw1805.data.model.User;
import com.eaw1805.data.model.UserGame;
import com.eaw1805.data.model.paypal.PaypalTransaction;
import com.eaw1805.orders.movement.FleetPatrolMovement;
import com.eaw1805.orders.movement.ShipPatrolMovement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

/**
 * Sends out e-mails.
 */
public class EmailManager {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(EmailManager.class);

    private static final String SMTP_HOST_NAME = "5.9.130.194";
    private static final String SMTP_AUTH_USER = "engine";
    private static final String SMTP_AUTH_PWD = "eng123456passwd";
    private static final boolean SMTP_SSL = false;

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static EmailManager ourInstance = null;

    /**
     * Private constructor.
     */
    private EmailManager() {
        // do nothing
    }

    public static EmailManager getInstance() {
        synchronized (EmailManager.class) {
            if (ourInstance == null) {
                ourInstance = new EmailManager();
            }
        }

        return ourInstance;
    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            final String username = SMTP_AUTH_USER;
            final String password = SMTP_AUTH_PWD;
            return new PasswordAuthentication(username, password);
        }
    }

    private Properties prepareProperties() {
        // Get system properties
        final Properties properties = System.getProperties();

        // Setup mail server
        if (SMTP_SSL) {
            // Use the following if you need SSL
            properties.put("mail.smtp.socketFactory.port", 465);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.fallback", "false");

            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.port", "465");

        } else {
            properties.put("mail.smtp.port", "25");
        }

        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.host", SMTP_HOST_NAME);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.user", SMTP_AUTH_USER);
        properties.put("mail.password", SMTP_AUTH_PWD);

        return properties;
    }

    /**
     * Send an e-mail.
     *
     * @param recipient Recipient's email ID needs to be mentioned.
     * @param subject   the subject of the e-mail.
     * @param text      the body of the e-mail.
     * @param filename1 the filename of EUROPE's map for the attachment.
     * @param filename2 the filename of AFRICA's map for the attachment.
     * @param filename3 the filename of CARRIBEAN's map for the attachment.
     * @param filename4 the filename of INDIES' map for the attachment.
     */
    public void sendEmail(final String recipient, final String subject, final String text,
                          final String filename1, final String filename2, final String filename3, final String filename4) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            // Set Subject: header field
            message.setSubject(subject);

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text);

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);

            // Set the email attachment file 1
            if (filename1 != null) {
                final MimeBodyPart attachmentPart1 = new MimeBodyPart();
                final FileDataSource fileDataSource1 = new FileDataSource(filename1) {
                    @Override
                    public String getContentType() {
                        return "image/png";
                    }
                };
                attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
                attachmentPart1.setFileName(fileDataSource1.getName());
                attachmentPart1.setDescription("Map of Europe");
                multipart.addBodyPart(attachmentPart1);
            }

            // Set the email attachment file 2
            if (filename2 != null) {
                final MimeBodyPart attachmentPart2 = new MimeBodyPart();
                final FileDataSource fileDataSource2 = new FileDataSource(filename2) {
                    @Override
                    public String getContentType() {
                        return "image/png";
                    }
                };
                attachmentPart2.setDataHandler(new DataHandler(fileDataSource2));
                attachmentPart2.setFileName(fileDataSource2.getName());
                attachmentPart2.setDescription("Map of Africa");
                multipart.addBodyPart(attachmentPart2);
            }

            // Set the email attachment file 3
            if (filename3 != null) {
                final MimeBodyPart attachmentPart3 = new MimeBodyPart();
                final FileDataSource fileDataSource3 = new FileDataSource(filename3) {
                    @Override
                    public String getContentType() {
                        return "image/png";
                    }
                };
                attachmentPart3.setDataHandler(new DataHandler(fileDataSource3));
                attachmentPart3.setFileName(fileDataSource3.getName());
                attachmentPart3.setDescription("Map of Caribbean");
                multipart.addBodyPart(attachmentPart3);
            }

            // Set the email attachment file 4
            if (filename4 != null) {
                final MimeBodyPart attachmentPart4 = new MimeBodyPart();
                final FileDataSource fileDataSource4 = new FileDataSource(filename4) {
                    @Override
                    public String getContentType() {
                        return "image/png";
                    }
                };
                attachmentPart4.setDataHandler(new DataHandler(fileDataSource4));
                attachmentPart4.setFileName(fileDataSource4.getName());
                attachmentPart4.setDescription("Map of Indies");
                multipart.addBodyPart(attachmentPart4);
            }

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart5 = new MimeBodyPart();
            final FileDataSource fileDataSource5 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart5.setDataHandler(new DataHandler(fileDataSource5));
            attachmentPart5.setFileName(fileDataSource5.getName());
            attachmentPart5.setDescription("Oplon Games");
            multipart.addBodyPart(attachmentPart5);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent message to " + recipient);

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

    /**
     * Send e-mail for a solo-game.
     *
     * @param thisUser  the player.
     * @param subject   the subject of the e-mail.
     * @param text      the body of the e-mail.
     * @param filename1 the filename of EUROPE's map for the attachment.
     * @param filename2 the filename of CARRIBEAN's map for the attachment.
     */
    public void sendMail1804(final User thisUser,
                             final String subject,
                             final String text,
                             final String filename1, final String filename2) {
        sendEmail(thisUser.getEmail(), subject, text, filename1, null, filename2, null);
    }

    /**
     * Send e-mail to all active players of a game.
     *
     * @param thisGame     the game.
     * @param subject      the subject of the e-mail.
     * @param text         the body of the e-mail.
     * @param textNoOrders the body of the e-mail in case the user did not issue any orders.
     * @param filename1    the filename of EUROPE's map for the attachment.
     * @param filename2    the filename of AFRICA's map for the attachment.
     * @param filename3    the filename of CARRIBEAN's map for the attachment.
     * @param filename4    the filename of INDIES' map for the attachment.
     */
    public void sendMail(final Game thisGame, final String subject,
                         final String text, final String textNoOrders,
                         final String filename1, final String filename2, final String filename3, final String filename4) {
        final Transaction mainTrans = HibernateUtil.getInstance().beginTransaction(HibernateUtil.DB_MAIN);
        final List<UserGame> lstUsers = UserGameManager.getInstance().list(thisGame);
        for (final UserGame userGame : lstUsers) {
            // Do not send out mails to admin accounts
            final User user = UserManager.getInstance().getByID(userGame.getUserId());
            if (user != null
                    && user.getEnableNotifications()//be sure user wants to receive emails
                    && user.getUserId() > 53
                    && userGame.isAlive()
                    && userGame.isActive()) {

                // constructing VPs list
                final StringBuilder vpBody = new StringBuilder();

                // Report VPs
                boolean vpAcquired = false;
                final List<News> lstNewsEntries = NewsManager.getInstance().listGameNation(thisGame, thisGame.getTurn() - 1, userGame.getNation());
                for (final News entry : lstNewsEntries) {
                    if (entry.getType() == NewsConstants.NEWS_VP) {
                        if (!vpAcquired) {
                            vpBody.append("Victory Points acquired during this turn: \n\n");
                            vpAcquired = true;
                        }

                        if (entry.getBaseNewsId() > 0) {
                            vpBody.append("+");
                        }
                        vpBody.append(entry.getBaseNewsId());
                        vpBody.append(" VP");

                        if (Math.abs(entry.getBaseNewsId()) > 1) {
                            vpBody.append("s");
                        }

                        vpBody.append(" ");
                        vpBody.append(entry.getText());
                        vpBody.append("\n");
                    }
                }

                if (vpAcquired) {
                    vpBody.append("\n");
                }

                vpBody.append("Current Victory Points: ");
                vpBody.append(retrieveVPs(thisGame, userGame.getNation(), thisGame.getTurn() - 1));
                vpBody.append("\n\n");

                // Identify VP Placeholder and replace
                final String finalText = text.replaceAll(GameEngine.VP_PLACEHOLDER, vpBody.toString());
                final String finalTextNoOrders = textNoOrders.replaceAll(GameEngine.VP_PLACEHOLDER, vpBody.toString());

                // check if player has submitted orders
                final List<PlayerOrder> lstOrders = PlayerOrderManager.getInstance().listByGameNation(thisGame, userGame.getNation(), thisGame.getTurn() - 1);

                // ignore patrol moves
                int patrolOrdersCnt = 0;
                for (final PlayerOrder order : lstOrders) {
                    if (order.getType() == OrderConstants.ORDER_M_UNIT
                            && (Integer.parseInt(order.getParameter5()) == ShipPatrolMovement.ORDER_TYPE
                            || Integer.parseInt(order.getParameter5()) == FleetPatrolMovement.ORDER_TYPE)) {
                        patrolOrdersCnt++;
                    }
                }

                // check also if orders are just patrol
                if ((lstOrders.isEmpty() || lstOrders.size() == patrolOrdersCnt) && userGame.getTurnPickUp() < thisGame.getTurn()) {
                    LOGGER.info("Sending No-Orders reminder to " + user.getEmail());
                    sendEmail(user.getEmail(), subject, finalTextNoOrders, filename1, filename2, filename3, filename4);

                } else {
                    sendEmail(user.getEmail(), subject, finalText, filename1, filename2, filename3, filename4);
                }
            }
        }
        mainTrans.commit();
    }

    /**
     * Retrieve the victory points of the country.
     *
     * @param thisGame   the game to look up.
     * @param thisNation the nation to look up.
     * @param turnInt    the turn number.
     * @return the victory points of the country.
     */
    protected int retrieveVPs(final Game thisGame, final Nation thisNation, final int turnInt) {
        final Report thisRep = ReportManager.getInstance().getByOwnerTurnKey(thisNation, thisGame, turnInt, ReportConstants.N_VP);
        int totVP = 0;
        if (thisRep != null) {
            totVP = Integer.parseInt(thisRep.getValue());
        }

        return totVP;
    }

    /**
     * Send an e-mail.
     *
     * @param recipient Recipient's email ID needs to be mentioned.
     * @param subject   the subject of the e-mail.
     * @param text      the body of the e-mail.
     */
    public void sendNotification(final String recipient, final String subject, final String text) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            // Set Subject: header field
            message.setSubject("[EaW1805] " + subject);

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text);

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent notification to " + recipient);

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

    /**
     * Send an email to a user about canceling an invitation.
     *
     * @param recipient The user to send.
     * @param gameOwner The owner of the game, same person that canceled the invitation.
     * @param userGame  The user game instance.
     */
    public void sendInvitationCancellationNotification(final User recipient, final User gameOwner, final UserGame userGame) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient.getEmail()));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

            // Set Subject: header field
            message.setSubject("[EaW1805] Invitation to join G" + userGame.getGame().getGameId() + " revoked");

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            StringBuilder text = new StringBuilder();
            text.append("Dear ").append(recipient.getUsername()).append(",\n\n");
            text.append("The invitation to join game G").append(userGame.getGame().getGameId()).append(" war revoked.\n\n");
            text.append("You can talk with the Game Creator about the game via personal messaging.\n\n");
            text.append("Check out the game creator from the following link:\nhttp://www.eaw1805.com/user/").append(gameOwner.getUsername()).append("\n\n");
            text.append("Good luck & Have fun!\n");
            text.append("The Eaw 1805 team\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text.toString());

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent invitation cancellation to " + recipient.getEmail());

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

    public void sendHelpEmail(final User recipient, final List<UserGame> userGames) {
        // Sender's email ID needs to be mentioned
        final String from = "support@eaw1805.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient.getEmail()));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));


            // Set Subject: header field
            message.setSubject("[EaW1805] Welcome to Empires at War 1805");

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            StringBuilder text = new StringBuilder();
            text.append("Dear ").append(recipient.getUsername()).append(",\n\n")
                    .append("We noticed that you have registered but you have not played the solo game yet, we hope that you did not run into any issues. If you have, please let us know, we are here to help!");

            if (!userGames.isEmpty()) {
                text.append("\n" +
                        "\nYour solo game is here:\n\n" +
                        "http://www.eaw1805.com/play/scenario/1804/game/").append(userGames.get(0).getGame().getGameId()).append("/nation/5");
            } else {
                text.append("\n" +
                        "\nYou can create a new solo game here:\n" +
                        "\nhttp://www.eaw1805.com/joingame/free");
            }
            text.append("\n\nFeel free to ask us anything :)\n\n\n")
                    .append("Happy Campaigning, \n")
                    .append("The EaW 1805 team\n\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text.toString());

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent questionnaire to " + recipient.getEmail());

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex, ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex, mex);
        }
    }

    /**
     * Send a questionnaire message.
     *
     * @param recipient the recipient of the email.
     */
    public boolean sendQuestionnaire(final User recipient) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient.getEmail()));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));
//            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

            // Set Subject: header field
            message.setSubject("[EaW1805] Questionnaire");

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            StringBuilder text = new StringBuilder();
            text.append("Dear ").append(recipient.getUsername()).append(",\n\n");

            text.append("You've been with us for a while now and we hope you are enjoying our service.\n");
            text.append("Please take a minute to tell us about your experience in Empires at War and how we can help you make the most of it.\n\n");
            text.append("Answer to the following question by clicking the link below, and help us improve our game: \n\n");
            text.append("http://www.eaw1805.com/questionnaire/").append(recipient.getQuestionnaireUUID());
            text.append("\n\n");
            text.append("We are committed to constant improvement and your feedback plays an important role in our updates and product development. If you \n");
            text.append("need any additional help please send us an email at support@eaw1805.com \n\n");
            text.append("Thank you for choosing EaW1805 !\n\n");
            text.append("Kind Regards, \n");
            text.append("Oplon Games\n\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text.toString());

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent questionnaire to " + recipient.getEmail());
            return true;

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex, ex);
            return false;

        } catch (MessagingException mex) {
            LOGGER.error(mex, mex);
            return false;
        }
    }

    /**
     * Send an email to a user about a new game invitation.
     *
     * @param recipient The user to send.
     * @param gameOwner The owner of the game, same person that canceled the invitation.
     * @param userGame  The user game instance.
     */
    public void sendInvitationNotification(final User recipient, final User gameOwner, final UserGame userGame) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient.getEmail()));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

            // Set Subject: header field
            message.setSubject("[EaW1805] Invitation to join G" + userGame.getGame().getGameId());

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            StringBuilder text = new StringBuilder();
            text.append("Dear ").append(recipient.getUsername()).append(",\n\n");
            text.append(gameOwner.getUsername()).append(" is the owner of game G").append(userGame.getGame().getGameId()).append(" and invites you to join with ").append(userGame.getNation().getName()).append("!\n\n");
            text.append("To reply to this invitation, simply login to your account and click on \"accept\" via the following link:\nhttp://www.eaw1805.com/games\n\n");
            text.append("To get more details about the game click here:\nhttp://www.eaw1805.com/scenario/").append(userGame.getGame().getScenarioIdToString()).append("/game/").append(userGame.getGame().getGameId()).append("/info\n\n");
            text.append("Check out the game creator from the following link:\nhttp://www.eaw1805.com/user/").append(gameOwner.getUsername()).append("\n\n");
            text.append("You can talk with the Game Creator about a different position or further game details directly via personal messaging.\n\n");
            text.append("Good luck & Have fun!\n");
            text.append("The Eaw 1805 team\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text.toString());

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent invitation notification to " + recipient.getEmail());

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

    /**
     * Send an e-mail.
     *
     * @param sender   Sender email.
     * @param fullName Sender's full name.
     * @param subject  the subject of the e-mail.
     * @param text     the body of the e-mail.
     */
    public void sendContact(final String sender, final String fullName, final String subject, final String text)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team"));

        // Set Subject: header field
        message.setSubject("[EaW1805] " + subject);

        // Set reply-to
        final Address replyAddr[] = new Address[1];
        final InternetAddress reply = new InternetAddress(sender);
        reply.setPersonal(fullName);
        replyAddr[0] = reply;
        message.setReplyTo(replyAddr);

        message.addRecipient(Message.RecipientType.CC, new InternetAddress(sender, fullName));

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(text);

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        final Transport transport = session.getTransport();
        transport.connect();
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
        LOGGER.info("Contact Form sent for " + sender);
    }

    /**
     * Send an e-mail.
     *
     * @param newUser The new user.
     */
    public void sendWelcome(final User newUser)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);
        message.addRecipient(Message.RecipientType.BCC, new InternetAddress("admin@eaw1805.com", "Empires at War 1805 Team"));

        // Set Subject: header field
        message.setSubject("Welcome to Empires at War 1805");

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(newUser.getEmail(), newUser.getFullname()));

        // Prepare text
        final StringBuilder strText = new StringBuilder();
        strText.append("Dear ");
        strText.append(newUser.getUsername());
        strText.append(",\n\n");
        strText.append("Welcome to Empires at War 1805.\n\n");
        strText.append("You can log in to your account via the following link:\n\n");
        strText.append("http://www.eaw1805.com/login\n\n");

        strText.append("We have added 50 free credits to your account so that you can start playing without delays.\n\n");

        strText.append("*SPECIAL OFFERS*: \n\n");

        //strText.append("  o If you are a student then you are entitled to 150 additional free credits. Send us an e-mail to support@eaw1805.com and we will add the credits to your account!\n\n");

        strText.append("  o Get 200 free credits by spreading the word of EAW to your friends. Follow the 6 simple steps:\n");
        strText.append("    http://www.eaw1805.com/settings\n\n");

        strText.append("  o Get 60 free credits by picking up an position in an on-going EAW game. Check out the available positions:\n");
        strText.append("    http://www.eaw1805.com/joingame/running\n\n");

        strText.append("You can find valuable information about the game in the Quick Start Guide:\n\n");
        strText.append("http://www.eaw1805.com/help/introduction\n\n");

        strText.append("We have prepared a set of video tutorials to help you get useful insights on the game mechanics:\n\n");
        strText.append("http://www.eaw1805.com/help\n\n");

        strText.append("Detailed information about the game rules and the mechanisms of Empires at War 1805 are available in the Players' Handbook:\n\n");
        strText.append("http://www.eaw1805.com/handbook\n\n");

        strText.append("For questions about the rules and the user interface, we suggest that you use the forums where all players post their comments along with replies from the GMs:\n\n");
        strText.append("http://forum.eaw1805.com\n\n");

        strText.append("We hope that you will enjoy playing Empires at War 1805,\nOplon Games");

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(strText.toString());

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        try {
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("New User Form sent for " + newUser.getUsername() + "/" + newUser.getFullname());

        } catch (Exception ex) {
            LOGGER.info("Failed to send New User Form for " + newUser.getUsername() + "/" + newUser.getFullname());
        }
    }

    /**
     * Send an e-mail.
     *
     * @param thisUser The new user.
     */
    public void sendUsername(final User thisUser)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);

        // Set Subject: header field
        message.setSubject("[EaW1805] Retrieve your Username");

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(thisUser.getEmail(), thisUser.getFullname()));

        // Prepare text
        final StringBuilder strText = new StringBuilder();
        strText.append("Dear ");
        strText.append(thisUser.getUsername());
        strText.append(",\n\n");
        strText.append("This is an automated mail generated after a request for retrieving your username for Empires at War 1805.\n\n");
        strText.append("Your username is:\n\n");
        strText.append(thisUser.getUsername());

        strText.append("\n\nYou can log in to your account via the following link:\n\n");
        strText.append("http://www.eaw1805.com/login\n\n");

        strText.append("We hope that you enjoy playing Empires at War 1805,\nOplon Games");

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(strText.toString());

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        final Transport transport = session.getTransport();
        transport.connect();
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
        LOGGER.info("Username sent for " + thisUser.getUsername() + "/" + thisUser.getFullname());
    }

    /**
     * Send an e-mail.
     *
     * @param thisUser The new user.
     */
    public void sendPassword(final User thisUser, final String newPassword)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);

        // Set Subject: header field
        message.setSubject("[EaW1805] Retrieve your Username");

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(thisUser.getEmail(), thisUser.getFullname()));

        // Prepare text
        final StringBuilder strText = new StringBuilder();
        strText.append("Dear ");
        strText.append(thisUser.getUsername());
        strText.append(",\n\n");
        strText.append("This is an automated mail generated after a request for retrieving your passowrd for Empires at War 1805.\n\n");
        strText.append("Your username is:\n\n");
        strText.append(thisUser.getUsername());
        strText.append("Your new password is:\n\n");
        strText.append(newPassword);

        strText.append("\n\nYou can log in to your account via the following link:\n\n");
        strText.append("http://www.eaw1805.com/login\n\n");

        strText.append("\n\nFor security reasons, we highly recommend to change your password as soon as you log in to the system via the following link:\n\n");
        strText.append("http://www.eaw1805.com/settings\n\n");

        strText.append("We hope that you enjoy playing Empires at War 1805,\nOplon Games");

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(strText.toString());

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        final Transport transport = session.getTransport();
        transport.connect();
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
        LOGGER.info("New Password sent for " + thisUser.getUsername() + "/" + thisUser.getFullname());
    }

    /**
     * Send an e-mail.
     *
     * @param thisUser The new user.
     */
    public void sendCreditAlert(final User thisUser, final UserGame position)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);

        // Set Subject: header field
        message.setSubject("[EaW1805] Account Credits Alarm");

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(thisUser.getEmail(), thisUser.getFullname()));

        // Prepare text
        final StringBuilder strText = new StringBuilder();
        strText.append("Dear ");
        strText.append(thisUser.getUsername());
        strText.append(",\n\n");
        strText.append("This is an automated mail generated after the processing of game ");
        strText.append(position.getGame().getGameId());
        strText.append(" (");
        if (position.getGame().getDescription() != null) {
            strText.append(position.getGame().getDescription());
        }
        strText.append(") has completed.\n\n");
        strText.append("Our records show that your account is critically low in credits.\n\n");
        strText.append("Please send in some credit before next processing so that you can continue playing. If there are no credit in your account before next processing, the engine will automatically drop you out of all games you participate.\n\n");
        strText.append("You can buy some more credit from the following link:\n\n");
        strText.append("http://www.eaw1805.com/settings\n\n");
        strText.append("If you have any questions or issues regarding your account, please don't hesitate to contact us at support@oplongames.com\n\n");
        strText.append("We hope that you enjoy playing Empires at War 1805,\nOplon Games");

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(strText.toString());

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        final Transport transport = session.getTransport();
        transport.connect();
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
        LOGGER.info("Credit Alert sent for " + thisUser.getUsername() + "/" + thisUser.getFullname());
    }

    /**
     * Send an e-mail.
     *
     * @param thisUser The new user.
     */
    public void sendPaymentReceipt(final User thisUser, final int credits, final String transactionId)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);
        message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

        // Set Subject: header field
        message.setSubject("[EaW1805] Payment Received");

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(thisUser.getEmail(), thisUser.getFullname()));

        // Prepare text
        final StringBuilder strText = new StringBuilder();
        strText.append("Dear ");
        strText.append(thisUser.getUsername());
        strText.append(",\n\n");
        strText.append("This is an automated mail generated after your credit purchase for Empires at War 1805.\n\n");
        strText.append("You have purchased ");
        strText.append(credits);
        strText.append(" credits.\n\n");

        strText.append("If you wish to receive a receipt for your payment, you need to fill in following form:\n\n");
        strText.append("http://www.eaw1805.com/receipt/id/");
        strText.append(transactionId);
        strText.append("\n\n");

        strText.append("We hope that you enjoy playing Empires at War 1805,\nOplon Games");

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(strText.toString());

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        final Transport transport = session.getTransport();
        transport.connect();
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
        LOGGER.info("Payment notification sent for " + thisUser.getUsername() + "/" + thisUser.getFullname());
    }

    /**
     * Send an e-mail.
     *
     * @param thisUser The new user.
     */
    public void sendPaymentReceiptRequest(final User thisUser, final PaypalTransaction thisTrans)
            throws MessagingException, UnsupportedEncodingException {

        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        // Create a default MimeMessage object.
        final MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        final InternetAddress senderAddr = new InternetAddress(from);
        senderAddr.setPersonal("EaW1805 Team");
        message.setFrom(senderAddr);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("payment@eaw1805.com", "Empires at War 1805 Team"));

        // Set Subject: header field
        message.setSubject("[EaW1805] Payment Received");

        // Set To: header field of the header.
        message.addRecipient(Message.RecipientType.CC, new InternetAddress(thisUser.getEmail(), thisUser.getFullname()));

        // Set reply-to
        final Address replyAddr[] = new Address[1];
        final InternetAddress reply = new InternetAddress(thisUser.getEmail());
        reply.setPersonal(thisUser.getFullname());
        replyAddr[0] = reply;
        message.setReplyTo(replyAddr);

        // Prepare text
        final StringBuilder strText = new StringBuilder();
        strText.append("This is an automated mail generated by the user's request for a receipt of credit purchase for Empires at War 1805.\n\n");
        strText.append("User:\t\t");
        strText.append(thisUser.getUsername());
        strText.append("\n\n");

        strText.append("Credits:\t");
        strText.append(thisTrans.getPmHistory().getChargeBought());
        strText.append("\n\n");

        strText.append("Amount:\t\t");
        strText.append(thisTrans.getGrossAmount());
        strText.append(" Euro\n\n");

        strText.append("Postal Address:\n\n");
        strText.append(thisTrans.getPayerName());
        strText.append("\n");
        strText.append(thisTrans.getPayerAddress());
        strText.append("\n");
        strText.append(thisTrans.getPayerCity());
        strText.append(" ");
        strText.append(thisTrans.getPayerPOCode());
        strText.append("\n");
        strText.append(thisTrans.getPayerCountry());
        strText.append("\n\n");

        strText.append("You can review your PayPal transaction via the following link:\n\n");
        strText.append("https://www.paypal.com/vst/id=");
        strText.append(thisTrans.getTransactionId());
        strText.append("\n\n");

        // Set the email message text.
        final MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(strText.toString());

        // Set the email attachment file 1
        final MimeBodyPart attachmentPart1 = new MimeBodyPart();
        final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
        attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
        attachmentPart1.setFileName(fileDataSource1.getName());
        attachmentPart1.setDescription("Oplon Games");

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart1);

        message.setContent(multipart);

        // Send message
        final Transport transport = session.getTransport();
        transport.connect();
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
        LOGGER.info("Payment receipt request sent for " + thisUser.getUsername() + "/" + thisUser.getFullname());
    }

    /**
     * Send notification for new inbox message.
     *
     * @param thisMessage The new message.
     */
    public void sendMessageNotification(final com.eaw1805.data.model.Message thisMessage) {
        try {
            // Sender's email ID needs to be mentioned
            final String from = "engine@oplon-games.com";

            // Get system properties
            final Properties properties = prepareProperties();

            // Authenticate
            final Authenticator auth = new SMTPAuthenticator();

            // Get the default Session object.
            final Session session = Session.getInstance(properties, auth);
            session.setDebug(false);

            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set Reply-to
            if (thisMessage.getSender().getPublicMail()) {
                // Set reply-to
                final Address replyAddr[] = new Address[1];
                final InternetAddress reply = new InternetAddress(thisMessage.getSender().getEmail(), thisMessage.getSender().getUsername());
                reply.setPersonal(thisMessage.getSender().getUsername());
                replyAddr[0] = reply;
                message.setReplyTo(replyAddr);
            }

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(thisMessage.getReceiver().getEmail(),
                    thisMessage.getReceiver().getFullname()));

            // Set Subject: header field
            message.setSubject("[EaW1805] " + thisMessage.getSubject());

            // Prepare text
            final StringBuilder strText = new StringBuilder();
            strText.append(thisMessage.getSender().getUsername()).append(" sent you a message.\n\n");
            strText.append("Subject:\t\t");
            strText.append(thisMessage.getSubject());
            strText.append("\n\n");

            strText.append(thisMessage.getBodyMessage());
            strText.append("\n\n");

            strText.append("To reply to this message, follow the link below:\n");
            strText.append("http://www.eaw1805.com/inbox/private/"
                    + thisMessage.getRootId() + "/view");
            strText.append("\n\n");

            strText.append("Want to control which emails you receive from EaW 1805? Go to:\n");
            strText.append("http://www.eaw1805.com/settings");
            strText.append("\n\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(strText.toString());

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Inbox notification sent for " + thisMessage.getReceiver().getUsername() + "/" + thisMessage.getReceiver().getFullname());

        } catch (final Exception e) {
            LOGGER.error("Exception: ", e);
        }
    }

    /**
     * Send an e-mail in rich html format.
     *
     * @param recipient Recipient's email ID needs to be mentioned.
     * @param subject   the subject of the e-mail.
     * @param text      the body of the e-mail.
     */
    public void sendUpdate(final String recipient, final String subject, final String text) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

            // Set Subject: header field
            message.setSubject(subject);

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text);

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent update message to " + recipient);

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

    /**
     * Send notification to a user that a game starts.
     * Game has already been updated and has the next process date updated.
     *
     * @param game The game to send the notification for.
     * @param user The user to send the notification.
     */
    public void sendGameLaunchNotification(final Game game, final User user) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

            // Set Subject: header field
            message.setSubject("[EaW1805] Launch of G" + game.getGameId());

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            StringBuilder text = new StringBuilder();
            text.append("Dear ").append(user.getUsername()).append(",\n\n");
            text.append("Game G").append(game.getGameId()).append(" has started!\n\n");

            // Add link to game info page
            text.append("You can play your nation via the following link:\n\n");
            text.append("http://www.eaw1805.com/play/scenario/").append(game.getScenarioIdToString()).append("/game/");
            text.append(game.getGameId());
            text.append("\n\n");

            // Report new deadline
            text.append("The first turn will be processed on ");
            text.append(game.getDateNextProc());
            text.append("\n\n");

            text.append("To get more details about the game click here:\nhttp://www.eaw1805.com/scenario/").append(game.getScenarioIdToString()).append("/game/").append(game.getGameId()).append("/info\n\n");
            text.append("Good luck & Have fun!\n");
            text.append("The Eaw 1805 team\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text.toString());

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent game launched notification to " + user.getEmail());

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

    /**
     * Send notification to a user that a game got canceled.
     * To be called only for players that joined this game.
     *
     * @param game The game to send the email for.
     * @param user The user to send the email.
     */
    public void sendGameCancellationNotification(final Game game, final User user) {
        // Sender's email ID needs to be mentioned
        final String from = "engine@oplon-games.com";

        // Get system properties
        final Properties properties = prepareProperties();

        // Authenticate
        final Authenticator auth = new SMTPAuthenticator();

        // Get the default Session object.
        final Session session = Session.getInstance(properties, auth);
        session.setDebug(false);

        try {
            // Create a default MimeMessage object.
            final MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            final InternetAddress senderAddr = new InternetAddress(from);
            senderAddr.setPersonal("EaW1805 Team");
            message.setFrom(senderAddr);

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("engine@eaw1805.com", "Empires at War 1805 Team"));

            // Set Subject: header field
            message.setSubject("[EaW1805] Cancellation of G" + game.getGameId());

            // Set reply-to
            final Address replyAddr[] = new Address[1];
            final InternetAddress reply = new InternetAddress("support@eaw1805.com", "Empires at War 1805 Team");
            reply.setPersonal("EaW1805 Support");
            replyAddr[0] = reply;
            message.setReplyTo(replyAddr);

            StringBuilder text = new StringBuilder();
            text.append("Dear ").append(user.getUsername()).append(",\n\n");
            text.append("Game G").append(game.getGameId()).append(" was just cancelled...\n\n");

            text.append("You can talk with the Game Creator for further details via personal messaging.\n\n");

            text.append("If you wish to play another position in an on-going EAW game, check out the available positions:\n");
            text.append("    http://www.eaw1805.com/joingame/running\n\n");

            text.append("We hope to see you in another game!\n");
            text.append("The Eaw 1805 team\n");

            // Set the email message text.
            final MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(text.toString());

            // Set the email attachment file 1
            final MimeBodyPart attachmentPart1 = new MimeBodyPart();
            final FileDataSource fileDataSource1 = new FileDataSource("/srv/eaw1805/images/logo/oplon-games.png") {
                @Override
                public String getContentType() {
                    return "image/png";
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileDataSource1.getName());
            attachmentPart1.setDescription("Oplon Games");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            multipart.addBodyPart(attachmentPart1);

            message.setContent(multipart);

            // Send message
            final Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.info("Sent game cancellation notification to " + user.getEmail());

        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(ex);

        } catch (MessagingException mex) {
            LOGGER.error(mex);
        }
    }

}
