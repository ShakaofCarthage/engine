package com.eaw1805.core;

import com.eaw1805.data.constants.CacheConstants;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlImageInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.eaw1805.core.support.QuietCssErrorHandler;
import com.eaw1805.core.support.SilentIncorrectnessListener;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.UserGameManager;
import com.eaw1805.data.model.Engine;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.UserGame;
import net.sourceforge.htmlunit.corejs.javascript.EvaluatorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Connect to Website and Evict Cache.
 */
public class WebsiteCacheManager
        implements CacheConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(WebsiteCacheManager.class);

    /**
     * The Web Client Object.
     */
    private final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_2);

    /**
     * Default Constructor.
     *
     * @param javaEnabled enable javascript while connecting to the EaW website.
     */
    public WebsiteCacheManager(final boolean javaEnabled) {
        //Initialize Web Client.
        webClient.setJavaScriptEnabled(javaEnabled);
        webClient.setIncorrectnessListener(new SilentIncorrectnessListener());
        webClient.setCssErrorHandler(new QuietCssErrorHandler());
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
    }

    public void connect() throws IOException, InterruptedException, EvaluatorException {
        final HtmlPage page = webClient.getPage(EAW_HOME_URL);
        LOGGER.debug("Connecting to EAW [" + EAW_HOME_URL + "]");

        //Get Login HTML form.
        final HtmlForm htmlForm = page.getFormByName("f");

        //Set username and password
        htmlForm.getInputByName("j_username").setValueAttribute(USERNAME);
        htmlForm.getInputByName("j_password").setValueAttribute(PASSWORD);

        //Get the submit button
        final HtmlImageInput button = htmlForm.getInputByName("submit");

        //Login and redirect to home page.
        final Page homePage = button.click();
        homePage.getWebResponse();
                //.executeJavaScript("window.location.href = '" + EAW_HOME_URL + "';").getNewPage();
    }

    /**
     * Call Evict Controller.
     *
     * @param cacheName the cacheName.
     * @throws java.io.IOException IOException.
     */
    public void evictCache(final String cacheName) throws IOException {
        LOGGER.debug("Evicting Cache: " + cacheName);
        webClient.getPage(EAW_CLEAR_CACHE_URL + cacheName);
    }

    /**
     * Call Game Specific pages.
     *
     * @throws java.io.IOException IOException.
     */
    public void callGameSpecificPages() throws IOException {
        // Call Game specific pages.
        final ExecutorService executorService = Executors.newFixedThreadPool(Engine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        // Call fixes pages
        for (final String staticPage : STATIC_PAGES) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    try {
                        LOGGER.debug("Call : " + staticPage);
                        webClient.getPage(staticPage);

                    } catch (Exception ex) {
                        LOGGER.error(ex);
                    }

                    return true;
                }
            }));
        }

        final List<String> urls = new ArrayList<String>();

        //Get the list of games.
        for (int scenario = HibernateUtil.DB_FIRST; scenario <= HibernateUtil.DB_LAST; scenario++) {
            // Set the session factories to all stores
            HibernateUtil.connectEntityManagers(scenario);
            LOGGER.debug("Setting scenario " + scenario);

            final Transaction trans = HibernateUtil.getInstance().beginTransaction(scenario);
            final List<Game> lstGames = GameManager.getInstance().list();

            // retrieve active nations
            for (final Game game : lstGames) {
                urls.add(GAME_URL.replace("xxx", game.getScenarioIdToString()).replace("yyy", String.valueOf(game.getGameId())));

                if (!game.getEnded()) {
                    final List<UserGame> lstUserGame = UserGameManager.getInstance().list(game);
                    for (UserGame position : lstUserGame) {
                        if (position.isAlive()) {
                            for (final String staticPage : CLIENT_URL) {
                                LOGGER.debug(staticPage.replace("xxx", position.getGame().getScenarioIdToString())
                                        .replace("yyy", String.valueOf(position.getGame().getGameId()))
                                        .replace("zzz", String.valueOf(position.getNation().getId())));

                                try {
                                    webClient.getPage(staticPage.replace("xxx", position.getGame().getScenarioIdToString())
                                            .replace("yyy", String.valueOf(position.getGame().getGameId()))
                                            .replace("zzz", String.valueOf(position.getNation().getId())));
                                } catch (Exception ex) {
                                    LOGGER.error("Error loading scenario " + scenario, ex);
                                }
                            }
                        }
                    }
                }
            }

            trans.rollback();
        }

        for (final String url : urls) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    try {
                        LOGGER.debug("Call Game URL : " + url);
                        webClient.getPage(url);

                    } catch (Exception ex) {
                        LOGGER.error("Loading cache", ex);
                    }

                    return true;
                }
            }));
        }

        // wait for the execution all tasks
        try {
            // wait for all tasks to complete before continuing
            for (Future<Boolean> task : futures) {
                task.get();
            }

            executorService.shutdownNow();

        } catch (Exception ex) {
            LOGGER.error("Task execution interrupted", ex);
        }
    }

    /**
     * Call Game Specific pages.
     *
     * @param game the Game page to refresh.
     * @throws java.io.IOException IOException.
     */
    public void callGameSpecificPages(final Game game, final boolean client) throws IOException {
        // Call Game specific pages.
        final ExecutorService executorService = Executors.newFixedThreadPool(Engine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        // Call fixes pages
        for (final String staticPage : STATIC_PAGES) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    try {
                        LOGGER.debug("Call : " + staticPage);
                        webClient.getPage(staticPage);

                    } catch (Exception ex) {
                        LOGGER.error(ex);
                    }

                    return true;
                }
            }));
        }

        final Transaction trans = HibernateUtil.getInstance().beginTransaction(game.getScenarioId());
        final List<String> urls = new ArrayList<String>();

        urls.add(GAME_URL.replace("xxx", game.getScenarioIdToString()).replace("yyy", String.valueOf(game.getGameId())));

        // retrieve active nations
        if (client && !game.getEnded()) {
            final List<Nation> lstNations = NationManager.getInstance().list();
            lstNations.remove(0);
            for (final Nation nation : lstNations) {
                final List<UserGame> lstUserGame = UserGameManager.getInstance().list(game, nation);
                if (!lstUserGame.isEmpty()
                        && lstUserGame.get(0).isAlive()) {

                    for (final String staticPage : CLIENT_URL) {
                        urls.add(staticPage.replace("xxx", game.getScenarioIdToString())
                                .replace("yyy", String.valueOf(game.getGameId()))
                                .replace("zzz", String.valueOf(nation.getId())));
                    }
                }
            }
        }
        trans.commit();

        for (final String url : urls) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    try {
                        LOGGER.debug("Call Game URL : " + url);
                        webClient.getPage(url);

                    } catch (Exception ex) {
                        LOGGER.error("Loading cache", ex);
                    }

                    return true;
                }
            }));
        }

        // wait for the execution all tasks
        try {
            // wait for all tasks to complete before continuing
            for (Future<Boolean> task : futures) {
                task.get();
            }

            executorService.shutdownNow();

        } catch (Exception ex) {
            LOGGER.error("Task execution interrupted", ex);
        }
    }

    /**
     * Call Game Specific pages.
     *
     * @throws java.io.IOException IOException.
     */
    public void callPublicPages() throws IOException {
        LOGGER.debug("Call Public EAW pages");

        //Get the list of nations.
        final Transaction trans = HibernateUtil.getInstance().getSessionFactory(HibernateUtil.DB_FIRST).getCurrentSession().beginTransaction();
        final List<Nation> nations = NationManager.getInstance().list();
        nations.remove(0);
        trans.rollback();

        // Call Game specific pages.
        final ExecutorService executorService = Executors.newFixedThreadPool(Engine.MAX_THREADS);
        final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        // Call fixes pages
        for (final String staticPage : PUBLIC_PAGES) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    try {
                        LOGGER.debug("Call : " + staticPage);
                        webClient.getPage(staticPage);
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                    }

                    return true;
                }
            }));
        }

        // Call Scenario Pages
        for (final Nation nation : nations) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    try {
                        LOGGER.debug("Call Scenario info URL, id : " + nation.getId());
                        webClient.getPage(SCENARIO_PAGES + nation.getId());
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                    }

                    return true;
                }
            }));
        }

        // wait for the execution all tasks
        try {
            // wait for all tasks to complete before continuing
            for (Future<Boolean> task : futures) {
                task.get();
            }

            executorService.shutdownNow();

        } catch (Exception ex) {
            LOGGER.error("Task execution interrupted", ex);
        }
    }

    /**
     * Simple execution.
     *
     * @param args no arguments needed here
     */
    public static void main(final String[] args) {
        // Set the session factories to all stores
        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);

        final WebsiteCacheManager client = new WebsiteCacheManager(false);

        // Connect
        try {
            client.connect();

        } catch (final IOException e) {
            LOGGER.error("Unable to Connect", e);

        } catch (final InterruptedException e) {
            LOGGER.error("Unable to Connect", e);
        }

        LOGGER.info("Call Public EAW pages");
        try {
            client.callPublicPages();
            client.callGameSpecificPages();

        } catch (final IOException e) {
            LOGGER.error("Cannot load public EAW pages", e);
        }

    }
}
