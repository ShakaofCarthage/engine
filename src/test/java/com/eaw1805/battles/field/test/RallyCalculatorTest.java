package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.morale.RallyCalculator;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class RallyCalculatorTest {

    private FieldBattleProcessor fieldBattleProcessor;
    private RallyCalculator rallyCalc;
    private Brigade brigade;
    private Brigade allyCrackBrigade;
    private Brigade allyEliteBrigade;
    private Brigade enemyCavalryBrigade;
    private Brigade enemyNonCavalryBrigade;

    @Before
    public void setUp() {

        brigade = new Brigade();
        addBattalions(brigade, 500);

        allyCrackBrigade = new Brigade();
        addBattalions(allyCrackBrigade, 500);
        allyCrackBrigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
        new ArrayList<Battalion>(allyCrackBrigade.getBattalions()).get(0).getType().setCrack(true);

        allyEliteBrigade = new Brigade();
        addBattalions(allyEliteBrigade, 500);
        allyEliteBrigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
        new ArrayList<Battalion>(allyEliteBrigade.getBattalions()).get(0).getType().setElite(true);

        enemyCavalryBrigade = new Brigade();
        addBattalions(enemyCavalryBrigade, 500);
        enemyCavalryBrigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
        new ArrayList<Battalion>(enemyCavalryBrigade.getBattalions()).get(0).getType().setType("Ca");

        enemyNonCavalryBrigade = new Brigade();
        addBattalions(enemyNonCavalryBrigade, 500);
        enemyNonCavalryBrigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);

        FieldBattleMap fbMap = new FieldBattleMap(50, 50);
        FieldBattleSector currentLocation = fbMap.getFieldBattleSector(10, 10);

        fieldBattleProcessor = mock(FieldBattleProcessor.class);
        when(fieldBattleProcessor.findSide(any(Brigade.class))).thenReturn(0);
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(0))).thenReturn(Collections.<Brigade>emptySet());
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(1))).thenReturn(Collections.<Brigade>emptySet());
        when(fieldBattleProcessor.getSector(any(Brigade.class))).thenReturn(currentLocation);

        rallyCalc = new RallyCalculator(fieldBattleProcessor);
    }


    @Test
    public void computeRallyModifier() {
        // no enemies within 10 tiles +10%
        assertSame(10, rallyCalc.computeRallyModifier(brigade));

        // no enemies within 10 tiles +10%
        // non-routing crack ally within 5 tiles +10%
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(0))).thenReturn(new HashSet<Brigade>(Arrays.asList(new Brigade[]{allyCrackBrigade})));
        assertSame(20, rallyCalc.computeRallyModifier(brigade));

        // no enemies within 10 tiles +10%
        // routing crack ally within 5 tiles, so no bonus
        allyCrackBrigade.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
        assertSame(10, rallyCalc.computeRallyModifier(brigade));

        // no enemies within 10 tiles +10%
        // non-routing elite ally within 5 tiles +10%
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(0))).thenReturn(new HashSet<Brigade>(Arrays.asList(new Brigade[]{allyEliteBrigade})));
        assertSame(20, rallyCalc.computeRallyModifier(brigade));

        // no enemies within 10 tiles +10%
        // routing elite ally within 5 tiles, so no bonus
        allyEliteBrigade.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
        assertSame(10, rallyCalc.computeRallyModifier(brigade));

        // enemy within 10 tiles, so no bonus
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(0))).thenReturn(Collections.<Brigade>emptySet());
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(1))).thenReturn(new HashSet<Brigade>(Arrays.asList(new Brigade[]{enemyNonCavalryBrigade})));
        assertSame(0, rallyCalc.computeRallyModifier(brigade));

        // enemy non-routing cavalry within 10 tiles, -10%
        when(fieldBattleProcessor.findBrigadesOfSide(anySet(), eq(1))).thenReturn(new HashSet<Brigade>(Arrays.asList(new Brigade[]{enemyCavalryBrigade})));
        assertSame(-10, rallyCalc.computeRallyModifier(brigade));

        // enemy routing cavalry within 10 tiles, so no bonus
        enemyCavalryBrigade.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
        assertSame(0, rallyCalc.computeRallyModifier(brigade));

    }

    private void addBattalions(Brigade brigade, Integer battalionHeadCount) {
        brigade.setBattalions(new HashSet<Battalion>());
        for (int i = 0; i < 6; i++) {
            Battalion battalion = new Battalion();

            battalion.setHeadcount(battalionHeadCount == null ? MathUtils.generateRandomIntInRange(300, 800) : battalionHeadCount);
            ArmyType armyType = new ArmyType();
            armyType.setName("ar");
            armyType.setType("ar");
            armyType.setLongRange(8);
            armyType.setLongCombat(7);
            armyType.setHandCombat(8);
            battalion.setType(armyType);
            battalion.setExperience(3);
            brigade.getBattalions().add(battalion);
        }
    }

}
