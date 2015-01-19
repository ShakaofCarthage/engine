package com.eaw1805.battles.field.processors;

import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds the sectors where ricochet fire is inflicted. In the following map, A
 * is the attacker, Tn is a target, Rn is the main ricochet sector, rn is the
 * secondary ricochet sector.
 * <p/>
 * <pre>
 * **********************************
 *  |  |  |  |  |  |  |  |  |  |  *
 *  |  |R6|r7|R7|R1|R2|r2|R3|  |  *
 *  |  |r5|T6|T7|T1|T2|T3|r4|  |  *
 *  |  |R5|T5|  |  |  |T4|R4|  |  *
 *  |  |R4|T4|  |A |  |T5|R5|  |  *
 *  |  |R3|T3|  |  |  |T6|R6|  |  *
 *  |  |r3|T2|T1|T9|T8|T7|r6|  |  *
 *  |  |R2|r1|R1|R9|R8|r8|R7|  |  *
 *  |  |  |  |  |  |  |  |  |  |  *
 * *********************************
 * </pre>
 *
 * @author fragkakis
 */
public class RicochetCalculator {
    public static Set<FieldBattleSector> findRicochetSectors(FieldBattleSector attackerSector, FieldBattleSector targetSector) {

        Set<FieldBattleSector> ricochetSectors = new HashSet<FieldBattleSector>();

        int mainX = -1;
        int mainY = -1;

        boolean attackerIsLeft = attackerSector.getX() < targetSector.getX();
        boolean attackerIsRight = targetSector.getX() < attackerSector.getX();

        if (attackerIsLeft) {
            mainX = targetSector.getX() + 1;
        } else if (attackerIsRight) {
            mainX = targetSector.getX() - 1;
        } else {
            mainX = targetSector.getX();
        }

        boolean attackerIsUp = attackerSector.getY() < targetSector.getY();
        boolean attackerIsDown = targetSector.getY() < attackerSector.getY();

        if (attackerIsUp) {
            mainY = targetSector.getY() + 1;
        } else if (attackerIsDown) {
            mainY = targetSector.getY() - 1;
        } else {
            mainY = targetSector.getY();
        }

        addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX, mainY);

        // Additional ricochet sector
        int diffX = Math.abs(attackerSector.getX() - targetSector.getX());
        int diffY = Math.abs(attackerSector.getY() - targetSector.getY());

        // if they are NOT in the same line
        if (diffX != 0 && diffY != 0 && diffX != diffY) {
            if (attackerIsLeft && attackerIsDown) {
                if (diffX < diffY) {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX - 1, mainY);
                } else {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX, mainY + 1);
                }

            } else if (attackerIsLeft && attackerIsUp) {
                if (diffX < diffY) {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX - 1, mainY);
                } else {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX, mainY - 1);
                }

            } else if (attackerIsRight && attackerIsUp) {
                if (diffX < diffY) {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX + 1, mainY);
                } else {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX, mainY - 1);
                }

            } else if (attackerIsRight && attackerIsDown) {
                if (diffX < diffY) {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX + 1, mainY);
                } else {
                    addSectorIfInMap(ricochetSectors, attackerSector.getMap(), mainX, mainY + 1);
                }
            }
        }


        return ricochetSectors;
    }

    private static void addSectorIfInMap(Set<FieldBattleSector> sectors, FieldBattleMap fbMap, int x, int y) {
        if (0 <= x
                && x <= fbMap.getSizeX() - 1
                && 0 <= y
                && y <= fbMap.getSizeY() - 1) {
            sectors.add(fbMap.getFieldBattleSector(x, y));
        }
    }

}
