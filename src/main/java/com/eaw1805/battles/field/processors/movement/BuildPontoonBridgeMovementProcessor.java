package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;

import java.util.Set;

/**
 * Movement processor for the "Build pontoon bridge" order.
 *
 * @author fragkakis
 */
public class BuildPontoonBridgeMovementProcessor extends BaseOrderMovementProcessor {

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public BuildPontoonBridgeMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    @Override
    protected void afterCheckpointsOrderSpecificMovement(Brigade brigade,
                                                         Set<FieldBattleSector> visibleEnemySectors,
                                                         Set<Brigade> visibleEnemies,
                                                         BaseFieldBattlePathCalculator pathCalculator, Order order,
                                                         int remainingMps) {

        int ourSide = findSide(brigade.getNation());
        FieldBattleSector currentLocation = movementProcessor.getParent().getSector(brigade);
        FieldBattleSector destination = null;

        for (int i = 1; i <= 3; i++) {

            FieldBattleSector possibleBuildBridgeSector = getSectorForward(ourSide, currentLocation, i);
            if (possibleBuildBridgeSector.isMinorRiver() || possibleBuildBridgeSector.isMajorRiver()) {
                if (possibleBuildBridgeSector.hasSectorBridge()) {
                    // bridge has been build here, see if additional bridge sectors need to be built (major river)
                    continue;
                } else {
                    // no bridge, this is where we must build it
                    destination = getSectorForward(ourSide, currentLocation, i - 1);
                    break;
                }
            } else {
                // either this is no river or this is the other side, nothing to build
                break;
            }
        }

        if (destination != null && currentLocation != destination) {
            proceedTowardsSector(brigade, destination, remainingMps, pathCalculator, true);
        }
    }

    private FieldBattleSector getSectorForward(int side, FieldBattleSector sector, int steps) {
        int x = sector.getX();
        int y = sector.getY();
        int yDisplacement = side == 0 ? 1 : -1;
        return sector.getMap().getFieldBattleSector(x, y + yDisplacement * steps);
    }

}
