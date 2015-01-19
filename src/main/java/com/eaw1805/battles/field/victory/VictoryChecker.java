package com.eaw1805.battles.field.victory;

import com.eaw1805.battles.field.FieldBattleProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VictoryChecker {

    private FieldBattleProcessor parent;
    private static final Logger LOGGER = LoggerFactory.getLogger(VictoryChecker.class);

    public VictoryChecker(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    /**
     * Checks if there is a winner. This happens when the morale of a side is at
     * least double compared to the morale of the other side.
     *
     * @return the winning side (0 or 1) or -1 if no winner yet.
     */
    public int fieldBattleWinner() {

        int sideMorale0 = parent.getMoraleChecker().calculateArmyMorale(0);
        int sideMorale1 = parent.getMoraleChecker().calculateArmyMorale(1);

        LOGGER.debug("Morale of Side0: {}, morale of Side1: {}", new Object[]{sideMorale0, sideMorale1});

        if (sideMorale0 * 2 < sideMorale1) {
            return 1;
        } else if (sideMorale1 * 2 < sideMorale0) {
            return 0;
        } else {
            // no winner yet
            return -1;
        }
    }

}
