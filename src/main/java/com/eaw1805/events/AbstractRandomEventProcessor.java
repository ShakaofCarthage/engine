package com.eaw1805.events;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ReportConstants;

/**
 * Abstract implementation of a random event processor.
 */
public abstract class AbstractRandomEventProcessor
        extends AbstractEventProcessor
        implements ReportConstants, NationConstants {

    /**
     * Captures the chance for the random event to be realised.
     */
    private final int chance;

    /**
     * Captures the number of occurrences of the random event.
     */
    private final int occurrences;

    /**
     * Default constructor.
     *
     * @param myParent       the parent object that invoked us.
     * @param myChance       the chance for the random event to be realised.
     * @param maxOccurrences the maximum number of occurrences of the random event.
     */
    public AbstractRandomEventProcessor(final EventProcessor myParent, final int myChance, final int maxOccurrences) {
        super(myParent);
        chance = myChance;
        occurrences = maxOccurrences;
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final int roll = getParent().getGameEngine().getRandomGen().nextInt(100) + 1;
        if (roll <= chance) {
            if (occurrences == 1) {
                realiseEvent();

            } else {
                final int repetitions = getParent().getGameEngine().getRandomGen().nextInt(occurrences) + 1;
                for (int rep = 0; rep < repetitions; rep++) {
                    realiseEvent();
                }
            }
        }
    }

    /**
     * Process the random event.
     */
    protected abstract void realiseEvent();

}
