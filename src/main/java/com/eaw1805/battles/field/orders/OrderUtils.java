package com.eaw1805.battles.field.orders;

import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.Order;

/**
 * Utility class for field battle orders.
 * @author fragkakis
 *
 */
public class OrderUtils {

	/**
	 * Checks whether the last checkpoint of the order has been reached.
	 * @param order the order
	 * @return true if the last checkpoint has been reached, false otherwise
	 */
	public static boolean lastCheckpointReached(Order order) {
		return (order.getCheckpoint3()!=null && order.getCheckpoint3().exists()) ? order.isReachedCheckpoint3() :
			(order.getCheckpoint2()!=null && order.getCheckpoint2().exists()) ? order.isReachedCheckpoint2() :
			(order.getCheckpoint1()!=null && order.getCheckpoint1().exists()) ? order.isReachedCheckpoint1() : true;
	}
	
	/**
	 * Returns the next (not reached) checkpoint for a field battle order.
	 * @param order the order
	 * @return the next not reached checkpoint, null if all checkpoints have been reached.
	 */
	public static FieldBattlePosition nextCheckpoint(Order order) {

        FieldBattlePosition nextCheckPoint = null;

        if (order.getCheckpoint3()!=null && order.getCheckpoint3().exists() && order.isReachedCheckpoint2()) {
            nextCheckPoint = order.getCheckpoint3();
        } else if (order.getCheckpoint2()!=null && order.getCheckpoint2().exists() && order.isReachedCheckpoint1()) {
            nextCheckPoint = order.getCheckpoint2();
        } else if (order.getCheckpoint1()!=null && order.getCheckpoint1().exists() && !order.isReachedCheckpoint1()) {
            nextCheckPoint = order.getCheckpoint1();
        }

        return nextCheckPoint;
    }
	 
}
