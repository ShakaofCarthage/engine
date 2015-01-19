package com.eaw1805.events;

import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.model.Nation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract implementation of a rumour event processor.
 */
public abstract class AbstractRumourEventProcessor
        extends AbstractEventProcessor
        implements ReportConstants, NationConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(AbstractRumourEventProcessor.class);

    /**
     * If the rumour is true.
     */
    private final boolean isTrue;

    /**
     * The nation that gets this rumour.
     */
    private final Nation nation;

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     * @param target   the target of the rumour.
     * @param valid    if the rumour is true.
     */
    public AbstractRumourEventProcessor(final EventProcessor myParent, final Nation target, final boolean valid) {
        super(myParent);
        isTrue = valid;
        nation = target;
    }

    /**
     * Get the target of the rumour.
     *
     * @return the target of the rumour.
     */
    public Nation getNation() {
        return nation;
    }

    /**
     * Processes the event.
     */
    public final void process() {
        try {
            if (isTrue) {
                produceTrue();
            } else {
                produceFalse();
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to produce rumour.", ex);
        }
    }

    /**
     * Process a true rumour.
     */
    protected abstract void produceTrue();

    /**
     * Process a false rumour.
     */
    protected abstract void produceFalse();

}
