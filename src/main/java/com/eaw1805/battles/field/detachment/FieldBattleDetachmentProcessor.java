package com.eaw1805.battles.field.detachment;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps record of detachments of a field battle.
 *
 * @author fragkakis
 */
public class FieldBattleDetachmentProcessor {

    private FieldBattleProcessor parent;

    private Map<Brigade, Set<Brigade>> leadersToFollowers = new HashMap<Brigade, Set<Brigade>>();
    private Map<Brigade, Brigade> followersToLeaders = new HashMap<Brigade, Brigade>();

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldBattleDetachmentProcessor.class);

    public FieldBattleDetachmentProcessor(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    public void setParent(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    public void calculateCurrentDetachmentsForSide(int side) {
        leadersToFollowers.clear();
        followersToLeaders.clear();

        Set<Brigade> aliveSideBrigades = new HashSet<Brigade>(parent.getSideBrigades()[side]);

        for (Brigade brigade : aliveSideBrigades) {
            Order currentOrder = parent.findCurrentOrder(brigade);

            if (currentOrder.getOrderTypeEnum() == OrdersEnum.FOLLOW_DETACHMENT) {
                Brigade leader = getBrigadeWithId(aliveSideBrigades, currentOrder.getDetachmentLeaderId());
                registerDetachmentParticipation(brigade, leader);
            }
        }

        LOGGER.debug("Registered detachments: {}", leadersToFollowers);
        LOGGER.debug("Registered followers to leaders: {}", followersToLeaders);
    }

    private Brigade getBrigadeWithId(Set<Brigade> brigades, int brigadeId) {
        for (Brigade brigade : brigades) {
            if (brigade.getBrigadeId() == brigadeId) {
                return brigade;
            }
        }
        throw new IllegalArgumentException("There is no leader brigade with id " + brigadeId);
    }

    private void registerDetachmentParticipation(Brigade brigade, Brigade leader) {
        if (!leadersToFollowers.containsKey(leader)) {
            leadersToFollowers.put(leader, new HashSet<Brigade>());
        }
        leadersToFollowers.get(leader).add(brigade);

        followersToLeaders.put(brigade, leader);
    }

    public void removeBrigadeFromDetachments(Brigade brigade) {
    	
        // if it is a follower
        if (followersToLeaders.keySet().contains(brigade)) {
        	LOGGER.debug("Removing {} (leader) from detachments", brigade);
            removeFollower(brigade);
        }
        // if it is a leader
        if (leadersToFollowers.keySet().contains(brigade)) {
        	LOGGER.debug("Removing {} (follower) from detachments", brigade);
            removeLeader(brigade);
        }
    }

    private void removeFollower(Brigade brigade) {
        Brigade leader = followersToLeaders.get(brigade);
        followersToLeaders.remove(brigade);

        leadersToFollowers.get(leader).remove(brigade);
        if (leadersToFollowers.get(leader).isEmpty()) {
            leadersToFollowers.remove(leader);
        }
    }

    private void removeLeader(Brigade brigade) {
        Set<Brigade> followers = leadersToFollowers.get(brigade);

        // the followers are ordered to retreat
        for (Brigade follower : followers) {
            followersToLeaders.remove(follower);

            Order retreatOrder = new Order();
            retreatOrder.setOrderTypeEnum(OrdersEnum.RETREAT);
            retreatOrder.setCheckpoint1(new FieldBattlePosition(-1, -1));
            retreatOrder.setFormationEnum(FormationEnum.COLUMN);
            follower.setBasicOrder(retreatOrder);
            follower.setAdditionalOrder(null);
        }

        leadersToFollowers.remove(brigade);
    }

    public Brigade getLeader(Brigade follower) {
        return followersToLeaders.get(follower);
    }

    public Set<Brigade> getFollowers(Brigade leader) {
        return leadersToFollowers.get(leader);
    }

    public boolean isLeader(Brigade brigade) {
        return leadersToFollowers.keySet().contains(brigade);
    }

    public Set<Brigade> getAllLeaders() {
        return leadersToFollowers.keySet();
    }

    public Set<Brigade> getAllFollowers() {
        return followersToLeaders.keySet();
    }

}
