package com.eaw1805.core.initializers;

/**
 * Interface for database initializers.
 */
public interface Initializer extends java.lang.Runnable {

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    boolean needsInitialization();

    /**
     * Initializes the database by populating it with the proper records.
     */
    void initialize();

}
