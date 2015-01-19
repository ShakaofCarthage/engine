package com.eaw1805.battles.field.utils;

import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;

import java.util.Collection;

/**
 * Utility methods for armies.
 *
 * @author fragkakis
 */
public class ArmyUtils {

    /**
     * Finds the total headcount of a collection of brigades.
     *
     * @param brigades the brigades
     * @return the total headcount
     */
    public static int findHeadCount(Collection<Brigade> brigades) {
        int headcount = 0;
        for (Brigade brigade : brigades) {
            headcount += findBrigadeHeadCount(brigade);
        }
        return headcount;
    }

    /**
     * Finds the total headcount for a brigade.
     *
     * @param brigade the brigade
     * @return the headcount
     */
    public static int findBrigadeHeadCount(Brigade brigade) {
        int headcount = 0;
        for (Battalion battalion : brigade.getBattalions()) {
            headcount += battalion.getHeadcount();
        }
        return headcount;
    }

    /**
     * Checks whether a brigade can form a skirmish formation. This is the case when more than
     * half its battalions are capable of forming a skirmish formation.
     *
     * @param brigade the brigade
     * @return true of false
     */
    public static boolean canFormSkirmish(Brigade brigade) {
        return canFormFormation(brigade, FormationEnum.SKIRMISH);
    }

    /**
     * Checks whether a brigade can form a square. This is the case when more than
     * half its battalions are capable of forming a square.
     *
     * @param brigade the brigade
     * @return true of false
     */
    public static boolean canFormSquare(Brigade brigade) {
        return canFormFormation(brigade, FormationEnum.SQUARE);
    }

    /**
     * Checks whether a brigade can form a line. This is the case when more than
     * half its battalions are capable of forming a line.
     *
     * @param brigade the brigade
     * @return true of false
     */
    public static boolean canFormLine(Brigade brigade) {
        return canFormFormation(brigade, FormationEnum.LINE);
    }

    public static boolean canFormFormation(Brigade brigade, FormationEnum formation) {
        int formationCapableBattalions = 0;
        for (Battalion battalion : brigade.getBattalions()) {
            switch (formation) {
                case SKIRMISH:
                    formationCapableBattalions += battalion.getType().getFormationSk() ? 1 : 0;
                    break;
                case LINE:
                    formationCapableBattalions += battalion.getType().getFormationLi() ? 1 : 0;
                    break;
                case SQUARE:
                    formationCapableBattalions += battalion.getType().getFormationSq() ? 1 : 0;
                    break;
                default:
                    return true;
            }
        }
        return formationCapableBattalions * 2 > brigade.getBattalions().size();
    }


    /**
     * Checks whether a collection of brigades contains a brigade of a certain
     * arm that is unbroken (is not routing).
     *
     * @param brigades the brigades
     * @param arm      the arm
     * @return true or false
     */
    public static boolean containsUnbrokenBrigadeOfArm(Collection<Brigade> brigades, ArmEnum arm) {
        for (Brigade brigade : brigades) {
            if (!brigade.isRouting() && ArmyUtils.findArm(brigade) == arm) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the "overall" arm type of a brigade, depending on the headcount of the comprising battalions.
     *
     * @param brigade the brigade
     * @return the arm
     */
    public static ArmEnum findArm(Brigade brigade) {

        ArmEnum armEnum = null;

        int infantryCount = 0;
        int cavalryCount = 0;
        int artilleryCount = 0;

        for (Battalion battalion : brigade.getBattalions()) {

            switch (ArmyUtils.findArm(battalion)) {
                case INFANTRY:
                    infantryCount++;
                    break;
                case CAVALRY:
                    cavalryCount++;
                    break;
                case ARTILLERY:
                default:
                    artilleryCount++;
                    break;
            }
        }
        if (infantryCount >= cavalryCount && infantryCount >= artilleryCount) {
            armEnum = ArmEnum.INFANTRY;
        } else if (cavalryCount >= infantryCount && cavalryCount >= artilleryCount) {
            armEnum = ArmEnum.CAVALRY;
        } else if (artilleryCount >= infantryCount && artilleryCount >= cavalryCount) {
            armEnum = ArmEnum.ARTILLERY;
        }
        return armEnum;
    }

    /**
     * Finds the arm of a battalion.
     *
     * @param battalion the battalion
     * @return the arm
     */
    public static ArmEnum findArm(Battalion battalion) {

        /**
         * Troop type.
         * In - Infantry
         * Kt - Colonial Troops
         * Co - Colonial Auxiliaries
         * CI - Crack Infantry
         * EI - Elite Infantry

         * Ca - Cavalry
         * MC - Mounted Colonial Auxiliaries
         * CC - Crack Cavalry
         * EC - Elite Cavalry

         * MA - Mounted Artillery
         * Ar - Artillery
         * Ha - Heavy Artillery
         */

        ArmEnum arm = null;

        String battalionType = battalion.getType().getType();


        if ("In".equalsIgnoreCase(battalionType) || "Ci".equalsIgnoreCase(battalionType)
                || "Kt".equalsIgnoreCase(battalionType) || "Co".equalsIgnoreCase(battalionType)
                || "EI".equalsIgnoreCase(battalionType)) {
            arm = ArmEnum.INFANTRY;

        } else if ("Ca".equalsIgnoreCase(battalionType) || "MC".equalsIgnoreCase(battalionType)
                || "CC".equalsIgnoreCase(battalionType) || "EC".equalsIgnoreCase(battalionType)) {
            arm = ArmEnum.CAVALRY;

        } else if ("MA".equalsIgnoreCase(battalionType) || "Ar".equalsIgnoreCase(battalionType)
                || "Ha".equalsIgnoreCase(battalionType)) {
            arm = ArmEnum.ARTILLERY;
        }
        return arm;
    }

    public static int countBattalionsOfArm(Brigade brigade, ArmEnum armEnum) {
        int armCount = 0;
        for (Battalion battalion : brigade.getBattalions()) {
            if (ArmyUtils.findArm(battalion) == armEnum) {
                armCount++;
            }
        }
        return armCount;
    }
}
