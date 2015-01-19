package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.processors.RicochetCalculator;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RicochetCalculatorTest {

    private FieldBattleMap fbMap;

    @Before
    public void setup() {
        fbMap = new FieldBattleMap(12, 12);
    }

    @Test
    public void testRicochetCalculator_5x5_vs_8x8() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);

        FieldBattleSector targetSectorA = fbMap.getFieldBattleSector(8, 8);

        Set<FieldBattleSector> ricochetSectorsA = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorA);

        assertEquals(1, ricochetSectorsA.size());

        FieldBattleSector ricSectorA1 = ricochetSectorsA.iterator().next();
        assertEquals(9, ricSectorA1.getX());
        assertEquals(9, ricSectorA1.getY());

    }

    @Test
    public void testRicochetCalculator_5x5_vs_2x2() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorB = fbMap.getFieldBattleSector(2, 2);

        Set<FieldBattleSector> ricochetSectorsB = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorB);

        assertEquals(1, ricochetSectorsB.size());

        FieldBattleSector ricSectorB1 = ricochetSectorsB.iterator().next();
        assertEquals(1, ricSectorB1.getX());
        assertEquals(1, ricSectorB1.getY());
    }

    @Test
    public void testRicochetCalculator_5x5_vs_2x8() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(2, 8);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(1, ricochetSectorsC.size());

        FieldBattleSector ricSectorC1 = ricochetSectorsC.iterator().next();
        assertEquals(1, ricSectorC1.getX());
        assertEquals(9, ricSectorC1.getY());
    }

    @Test
    public void testRicochetCalculator_5x5_vs_8x2() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(8, 2);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(1, ricochetSectorsC.size());

        FieldBattleSector ricSectorC1 = ricochetSectorsC.iterator().next();
        assertEquals(9, ricSectorC1.getX());
        assertEquals(1, ricSectorC1.getY());
    }

    @Test
    public void testRicochetCalculator_5x5_vs_8x9() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(8, 9);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(9, 10)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(8, 10)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_9x8() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(9, 8);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(10, 9)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(10, 8)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_2x3() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(2, 3);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(1, 2)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(1, 3)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_3x2() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(3, 2);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(2, 1)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(3, 1)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_2x9() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(2, 9);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(2, 10)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(2, 10)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_1x8() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(1, 8);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(0, 8)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(0, 9)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_8x1() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(8, 1);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(8, 0)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(9, 0)));
    }

    @Test
    public void testRicochetCalculator_5x5_vs_9x2() {

        FieldBattleSector attackerSector = fbMap.getFieldBattleSector(5, 5);
        FieldBattleSector targetSectorC = fbMap.getFieldBattleSector(9, 2);

        Set<FieldBattleSector> ricochetSectorsC = RicochetCalculator.findRicochetSectors(attackerSector, targetSectorC);

        assertEquals(2, ricochetSectorsC.size());

        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(10, 2)));
        assertTrue(ricochetSectorsC.contains(fbMap.getFieldBattleSector(10, 1)));
    }
}
