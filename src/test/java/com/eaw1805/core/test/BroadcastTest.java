package com.eaw1805.core.test;

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
 * Test broadcasting mechanisms.
 * <p/>
 * oauth.accessToken=417188656-sDtDKXAYwjgNqKNMBPvVByuHZS3NoA0gKjcI1G5G
 * oauth.accessTokenSecret=q3faOcK3WISAoKdnteGx2iGhPyNnLrNndfgz5TrtQ
 */
public class BroadcastTest {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BroadcastTest.class);

    public void init() {
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
                            te.printStackTrace();
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
                    System.exit(-1);
                }
            }

        } catch (TwitterException te) {
            te.printStackTrace();
            LOGGER.debug("Failed to get timeline: " + te.getMessage());
            System.exit(-1);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            LOGGER.debug("Failed to read the system input.");
            System.exit(-1);
        }
    }

    public void makeTweet(final String latestStatus)
            throws TwitterException {
        // The factory instance is re-useable and thread safe.
        final Twitter twitter = new TwitterFactory().getInstance();

        final ImageUpload upload = new ImageUploadFactory().getInstance(MediaProvider.TWITTER);
        final String fileUrl = "/srv/eaw1805/images/maps/s1/1/map-1-21-1-small.png";
        final String url = upload.upload(new File(fileUrl));

        LOGGER.debug("Successfully uploaded image to [" + url + "].");

        final Status status = twitter.updateStatus(latestStatus + " " + url);
        LOGGER.debug("Successfully updated the status to [" + status.getText() + "].");
    }

    public static void main(String[] argc)
            throws TwitterException {

        // Construct tester
        BroadcastTest bTest = new BroadcastTest();

        // login
        bTest.init();

        // change status
        bTest.makeTweet("Processing Game G2");
    }

}
