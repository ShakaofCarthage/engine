package com.eaw1805.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;
import twitter4j.media.MediaProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Central point of access of Twitter APIO.
 */
public final class TwitterManager {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(TwitterManager.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static TwitterManager ourInstance = null;

    /**
     * Private constructor.
     */
    private TwitterManager() {
        init();
    }

    public static TwitterManager getInstance() {
        synchronized (TwitterManager.class) {
            if (ourInstance == null) {
                ourInstance = new TwitterManager();
            }
        }

        return ourInstance;
    }

    /**
     * Authenticate using provided credentials.
     */
    private void init() {
        try {
            final ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey("K8mXPcLSZyRhulOpmgmB5Q")
                    .setOAuthConsumerSecret("HiaXd7N7vh0Ygfap703zuUmY6hNUbSgq29H6imM")
                    .setOAuthAccessToken("417188656-sDtDKXAYwjgNqKNMBPvVByuHZS3NoA0gKjcI1G5G")
                    .setOAuthAccessTokenSecret("q3faOcK3WISAoKdnteGx2iGhPyNnLrNndfgz5TrtQ");

            final TwitterFactory tFactory = new TwitterFactory(cb.build());

            final Twitter twitter = tFactory.getInstance();
            try {
                // get request token.
                // this will throw IllegalStateException if access token is already available
                final RequestToken requestToken = twitter.getOAuthRequestToken();
                LOGGER.debug("Got request token.");
                LOGGER.debug("Request token: " + requestToken.getToken());
                LOGGER.debug("Request token secret: " + requestToken.getTokenSecret());
                AccessToken accessToken = null;

                final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (null == accessToken) {
                    LOGGER.debug("Open the following URL and grant access to your account:");
                    LOGGER.debug(requestToken.getAuthorizationURL());
                    LOGGER.debug("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
                    final String pin = br.readLine();
                    try {
                        if (pin.length() > 0) {
                            accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                        } else {
                            accessToken = twitter.getOAuthAccessToken(requestToken);
                        }

                    } catch (TwitterException te) {
                        if (401 == te.getStatusCode()) {
                            LOGGER.debug("Unable to get the access token.");
                        } else {
                            LOGGER.error(te);
                        }
                    }
                }
                LOGGER.debug("Got access token.");
                LOGGER.debug("Access token: " + accessToken.getToken());
                LOGGER.debug("Access token secret: " + accessToken.getTokenSecret());

            } catch (IllegalStateException ie) {
                // access token is already available, or consumer key/secret is not set.
                if (!twitter.getAuthorization().isEnabled()) {
                    LOGGER.debug("OAuth consumer key/secret is not set.");
                }
            }

        } catch (TwitterException te) {
            LOGGER.debug("Failed to get timeline: " + te.getMessage());
            LOGGER.error(te);

        } catch (IOException ioe) {
            LOGGER.debug("Failed to read the system input.");
            LOGGER.error(ioe);
        }
    }

    /**
     * Change the status of the twitter account.
     *
     * @param latestStatus the status to set.
     */
    public void tweetText(final String latestStatus) {
        try {
            // The factory instance is re-useable and thread safe.
            final Twitter twitter = new TwitterFactory().getInstance();

            final Status status = twitter.updateStatus(latestStatus);
            LOGGER.debug("Successfully updated the status to [" + status.getText() + "].");

        } catch (TwitterException ex) {
            LOGGER.error(ex);
        }
    }

    /**
     * Change the status of the twitter account, including a picture.
     *
     * @param latestStatus the status to set.
     * @param imagePath    the image path.
     */
    public void tweetImage(final String latestStatus, final String imagePath) {
        try {
            // The factory instance nis re-useable and thread safe.
            new TwitterFactory().getInstance();

            final ImageUpload upload = new ImageUploadFactory().getInstance(MediaProvider.TWITTER);
            final String url = upload.upload(new File(imagePath), latestStatus);

            LOGGER.debug("Successfully updated the status to [" + url + "] + [" + latestStatus + "].");

            //final Status status = twitter.updateStatus(latestStatus + " " + url);
            //LOGGER.debug("Successfully updated the status to [" + status.getText() + "].");

        } catch (TwitterException ex) {
            LOGGER.error(ex);
        }
    }

}
