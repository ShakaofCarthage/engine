package com.eaw1805.battles.field;

import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.apache.commons.lang3.StringUtils;

public class OrderSanitizer {

	/**
	 * Sanitizes an order
	 * @param brigade the brigade bearing the order
	 * @param order the order
	 * @param basicOrder flag to denote whether this is the basic order
	 */
	public static void sanitizeOrder(Brigade brigade, Order order, boolean basicOrder) {
		if (basicOrder
                && StringUtils.isEmpty(order.getOrderType())) {
            order.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);
        }

        if (basicOrder
                && StringUtils.isEmpty(order.getFormation())) {
            order.setFormationEnum(FormationEnum.COLUMN);
        }

        if (order != null
                && StringUtils.isNotEmpty(order.getOrderType())
                && !ArmyUtils.canFormFormation(brigade, order.getFormationEnum())) {
        	order.setFormationEnum(FormationEnum.COLUMN);
        }
        
        if(order != null
        		&& !StringUtils.isEmpty(order.getOrderType())
        		&& order.getOrderTypeEnum() == OrdersEnum.FOLLOW_DETACHMENT
        		&& order.getDetachmentLeaderId() == -1) {
        	order.setOrderTypeEnum(OrdersEnum.DEFEND_POSITION);        	
        }
	}
}
