package com.eaw1805.battles.field.processors.movement;

import com.eaw1805.battles.field.movement.BaseFieldBattlePathCalculator;
import com.eaw1805.battles.field.processors.MovementProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * Movement processor for the "Move to engage" order.
 *
 * @author fragkakis
 */
public class MoveToFireMovementProcessor extends BaseOffensiveOrderMovementProcessor {

    private static final int DISTANCE_FROM_TARGET = 2;
    private final static Logger LOGGER = LoggerFactory.getLogger(MoveToFireMovementProcessor.class);

    /**
     * Constructor.
     *
     * @param movementProcessor
     */
    public MoveToFireMovementProcessor(MovementProcessor movementProcessor) {
        super(movementProcessor);
    }

    protected FieldBattleSector findPreferredDestinationForEnemyGroup(Brigade brigade, BaseFieldBattlePathCalculator pathCalculator,
                                                                      FieldBattleSector currentLocation, Collection<Brigade> enemies, int remainingMps, Order order) {

        int nearestEnemyCost = Integer.MAX_VALUE;
        FieldBattleSector destination = null;

        for (Brigade enemy : enemies) {
            FieldBattleSector enemySector = movementProcessor.getParent().getSector(enemy);

            Set<FieldBattleSector> enemySectorNeighbours = MapUtils.findSectorsInRing(enemySector, DISTANCE_FROM_TARGET);

            if (enemySectorNeighbours.contains(currentLocation)) {
                // we are already in a place to fire one of the candidate enemies with no movement cost.
                destination = currentLocation;
                break;
            }


            for (FieldBattleSector enemySectorNeighbour : enemySectorNeighbours) {
                int enemySectorNeighbourCost = pathCalculator.findCost(currentLocation, enemySectorNeighbour,
                        brigade.getArmTypeEnum(), brigade.getFormationEnum(), true);
                if (enemySectorNeighbourCost < nearestEnemyCost) {
                    destination = enemySectorNeighbour;
                    nearestEnemyCost = enemySectorNeighbourCost;
                }
            }
        }
        LOGGER.debug("Moving from {} towards {} with total cost {}", new Object[]{currentLocation, destination, nearestEnemyCost});
        return destination;
    }

}
