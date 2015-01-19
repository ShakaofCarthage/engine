package com.eaw1805.core.initializers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic initializer that does all the initialization as a separate thread.
 */
public abstract class AbstractThreadedInitializer
        implements Initializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(AbstractThreadedInitializer.class);

    /**
     * List of other initializes that we depend.
     */
    private final transient List<Thread> dependencies; // NOPMD

    /**
     * Default constructor.
     */
    public AbstractThreadedInitializer() {
        dependencies = new ArrayList<Thread>(); // NOPMD
    }

    /**
     * Add dependency to another thread.
     *
     * @param thisThread the thread object.
     */
    public void addDependency(final Thread thisThread) { // NOPMD
        dependencies.add(thisThread);
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see java.lang.Thread#run()
     */
    public void run() {
        // Wait for dependencies to conclude
        for (final Thread thisThread : dependencies) {  // NOPMD
            try {
                thisThread.join();
            } catch (InterruptedException iex) {
                LOGGER.trace("Interrupted while waiting for dependency to complete.", iex);
            }
        }

        // Retrieve session for this thread
//        final Session thisSession = GameManager.getInstance().getSessionFactory().openSession();
//        final Transaction thisTrans = thisSession.beginTransaction();

        // Check if initialization of this entity is required
        if (needsInitialization()) {
            // perform initialization
            initialize();

            //thisTrans.commit();
            //} else {
            //thisTrans.rollback();
        }
        //thisSession.close();
    }
}
