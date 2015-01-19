package com.eaw1805.core;

/**
 * Exception in case of an invalid game identifier.
 */
public class InvalidGameIdentifier extends Exception {

    /**
     * For serialization purposes.
     */
    static final long serialVersionUID = -3387516993124229948L;

    /**
     * Default constructor.
     */
    public InvalidGameIdentifier() {
        super("Invalid Game Identifier");
    }

}
