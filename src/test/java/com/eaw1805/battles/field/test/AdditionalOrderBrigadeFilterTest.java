package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.processors.movement.AdditionalOrderBrigadeFilter;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AdditionalOrderBrigadeFilterTest {

    private Nation nation0;
    private Nation nation1;
    private Nation nation2;

    private Brigade enemy_nation0_100men_Lin_Art;
    private Brigade enemy_nation1_300men_Col_Cav;
    private Brigade enemy_nation2_500men_Fle_Inf;
    private Brigade enemy_nation2_600men_Squ_Inf;

    private AdditionalOrderBrigadeFilter additionalOrderBrigadeFilter;
    private Set<Brigade> enemies;

    @Before
    public void setUp() {

        ArmyType artilleryArmyType = new ArmyType();
        artilleryArmyType.setType("Ar");
        ArmyType cavalryArmyType = new ArmyType();
        cavalryArmyType.setType("Ca");
        ArmyType infantryArmyType = new ArmyType();
        infantryArmyType.setType("In");

        Battalion battalion100 = new Battalion();
        battalion100.setHeadcount(100);
        battalion100.setType(artilleryArmyType);

        Battalion battalion200 = new Battalion();
        battalion200.setHeadcount(200);
        battalion200.setType(cavalryArmyType);

        Battalion battalion300 = new Battalion();
        battalion300.setHeadcount(300);
        battalion300.setType(infantryArmyType);


        nation0 = new Nation();
        nation0.setId(0);
        nation1 = new Nation();
        nation1.setId(1);
        nation2 = new Nation();
        nation2.setId(2);

        enemy_nation0_100men_Lin_Art = new Brigade();
        enemy_nation0_100men_Lin_Art.setNation(nation0);
        enemy_nation0_100men_Lin_Art.setBattalions(new HashSet<Battalion>(Arrays.asList(new Battalion[]{battalion100})));
        enemy_nation0_100men_Lin_Art.setFormation(FormationEnum.LINE.toString());
        enemy_nation1_300men_Col_Cav = new Brigade();
        enemy_nation1_300men_Col_Cav.setNation(nation1);
        enemy_nation1_300men_Col_Cav.setBattalions(new HashSet<Battalion>(Arrays.asList(new Battalion[]{battalion100, battalion200})));
        enemy_nation1_300men_Col_Cav.setFormation(FormationEnum.COLUMN.toString());
        enemy_nation2_500men_Fle_Inf = new Brigade();
        enemy_nation2_500men_Fle_Inf.setNation(nation2);
        enemy_nation2_500men_Fle_Inf.setBattalions(new HashSet<Battalion>(Arrays.asList(new Battalion[]{battalion200, battalion300})));
        enemy_nation2_500men_Fle_Inf.setFormation(FormationEnum.FLEE.toString());
        enemy_nation2_600men_Squ_Inf = new Brigade();
        enemy_nation2_600men_Squ_Inf.setNation(nation2);
        enemy_nation2_600men_Squ_Inf.setBattalions(new HashSet<Battalion>(Arrays.asList(new Battalion[]{battalion100, battalion200, battalion300})));
        enemy_nation2_600men_Squ_Inf.setFormation(FormationEnum.SQUARE.toString());

        enemies = new HashSet<Brigade>();
        enemies.add(enemy_nation0_100men_Lin_Art);
        enemies.add(enemy_nation1_300men_Col_Cav);
        enemies.add(enemy_nation2_500men_Fle_Inf);
        enemies.add(enemy_nation2_600men_Squ_Inf);

        for (Brigade brigade : enemies) {
            brigade.setArmTypeEnum(ArmyUtils.findArm(brigade));
        }

        additionalOrderBrigadeFilter = new AdditionalOrderBrigadeFilter();
    }

    @Test
    public void testTargetHeadCount() {

        Order order = new Order();
        order.setTargetHighestHeadcount(true);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation2})));
        Brigade preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation2_600men_Squ_Inf);

    }

    @Test
    public void testTargetNation() {

        Order order = new Order();
        order.setTargetHighestHeadcount(true);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation0})));
        Brigade preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation0_100men_Lin_Art);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation1})));
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation1_300men_Col_Cav);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation2})));
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertTrue(preferredEnemy == enemy_nation2_600men_Squ_Inf);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation0, nation1})));
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertTrue(preferredEnemy == enemy_nation0_100men_Lin_Art || preferredEnemy == enemy_nation1_300men_Col_Cav);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation0, nation2})));
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertTrue(preferredEnemy == enemy_nation0_100men_Lin_Art || preferredEnemy == enemy_nation2_600men_Squ_Inf);

        order.setTargetNations(new HashSet<Nation>(Arrays.asList(new Nation[]{nation1, nation2})));
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertTrue(preferredEnemy == enemy_nation1_300men_Col_Cav || preferredEnemy == enemy_nation2_600men_Squ_Inf);
    }

    @Test
    public void testTargetFormation() {

        Order order = new Order();
        order.setTargetHighestHeadcount(true);

        order.setTargetFormation(FormationEnum.LINE.toString());
        Brigade preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation0_100men_Lin_Art);

        order.setTargetFormation(FormationEnum.COLUMN.toString());
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation1_300men_Col_Cav);

        order.setTargetFormation(FormationEnum.FLEE.toString());
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation2_500men_Fle_Inf);

        order.setTargetFormation(FormationEnum.SQUARE.toString());
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation2_600men_Squ_Inf);
    }

    @Test
    public void testTargetArm() {

        Order order = new Order();
        order.setTargetHighestHeadcount(true);

        order.setTargetArm(ArmEnum.ARTILLERY.toString());
        Brigade preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation0_100men_Lin_Art);

        order.setTargetArm(ArmEnum.CAVALRY.toString());
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertEquals(preferredEnemy, enemy_nation1_300men_Col_Cav);

        order.setTargetArm(ArmEnum.INFANTRY.toString());
        preferredEnemy = additionalOrderBrigadeFilter.getPreferredEnemyForMovement(null, enemies, null, order, null);
        assertTrue(preferredEnemy == enemy_nation2_600men_Squ_Inf);

    }
}
