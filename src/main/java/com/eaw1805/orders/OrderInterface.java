package com.eaw1805.orders;

import com.eaw1805.data.model.PlayerOrder;

/**
 * Common Interface for all game orders.
 */
public interface OrderInterface {

    /**
     * Returns the order object.
     *
     * @return the order object.
     */
    PlayerOrder getOrder();

    /**
     * Sets the particular order.
     *
     * @param thisOrder the new PlayerOrder instance.
     */
    void setOrder(final PlayerOrder thisOrder);

    /**
     * Processes the order.
     */
    void process();

}
