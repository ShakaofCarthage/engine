package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.battles.field.visibility.FieldBattleVisibilityProcessor;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldBattleVisibilityProcessorTest {

    private FieldBattleVisibilityProcessor visibilityProcessor;
    private FieldBattleProcessor fbProcessor;
    private FieldBattleMap fbMap;
    private Brigade brigade0;
    private Brigade brigade1;
    private Brigade brigadeObstacle0;
    private Brigade brigadeObstacle1;

    @Before
    public void setUp() throws Exception {

        brigade0 = new Brigade();
        brigade0.setBrigadeId(0);
        brigade0.setFormationEnum(FormationEnum.LINE);
        brigade1 = new Brigade();
        brigade1.setBrigadeId(1);
        brigade1.setFormationEnum(FormationEnum.LINE);
        brigadeObstacle0 = new Brigade();
        brigadeObstacle0.setBrigadeId(700);
        brigadeObstacle0.setFormationEnum(FormationEnum.SKIRMISH);
        brigadeObstacle1 = new Brigade();
        brigadeObstacle1.setBrigadeId(701);
        brigadeObstacle1.setFormationEnum(FormationEnum.LINE);

        fbMap = new FieldBattleMap(10, 5);
        for (FieldBattleSector sector : MapUtils.getAllSectors(fbMap)) {
            sector.setAltitude(1);
        }
        fbProcessor = mock(FieldBattleProcessor.class);
        when(fbProcessor.getFbMap()).thenReturn(fbMap);
        when(fbProcessor.findSide(brigade0)).thenReturn(0);
        when(fbProcessor.findSide(brigade1)).thenReturn(1);
        when(fbProcessor.findSide(brigadeObstacle0)).thenReturn(0);
        when(fbProcessor.findSide(brigadeObstacle1)).thenReturn(1);

        visibilityProcessor = new FieldBattleVisibilityProcessor(fbProcessor);
    }

    private void setBrigadePosition(Brigade brigade, int x, int y) {
        brigade.setFieldBattlePosition(new FieldBattlePosition(x, y));
        FieldBattleSector fieldBattleSector = fbMap.getFieldBattleSector(x, y);
        when(fbProcessor.getSector(brigade)).thenReturn(fieldBattleSector);
        when(fbProcessor.getBrigadeInSector(fieldBattleSector)).thenReturn(brigade);
    }

    @Test
    public void testVisible1() {
        setBrigadePosition(brigade0, 2, 1);
        setBrigadePosition(brigadeObstacle0, 4, 2);
        setBrigadePosition(brigade1, 7, 3);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
        assertTrue(visibilityProcessor.visible(brigadeObstacle0, brigade1));
        assertTrue(visibilityProcessor.visible(brigade1, brigadeObstacle0));
        assertTrue(visibilityProcessor.visible(brigade1, brigade0));
    }

    @Test
    public void testVisible2() {
        setBrigadePosition(brigade0, 1, 1);
        setBrigadePosition(brigade1, 7, 4);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testVisible3() {
        setBrigadePosition(brigade0, 4, 3);
        setBrigadePosition(brigade1, 5, 3);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testVisible4() {
        setBrigadePosition(brigade0, 4, 3);
        setBrigadePosition(brigade1, 5, 4);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testVisibleInEdgeOfForest() {
        setBrigadePosition(brigade0, 2, 3);
        setBrigadePosition(brigade1, 7, 3);
        fbMap.getFieldBattleSector(7, 3).setForest(true);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisibleInInnerOfForest() {
        setBrigadePosition(brigade0, 2, 3);
        setBrigadePosition(brigade1, 7, 3);
        fbMap.getFieldBattleSector(6, 3).setForest(true);
        fbMap.getFieldBattleSector(7, 3).setForest(true);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testVisibleInEdgeOfVillage() {
        setBrigadePosition(brigade0, 2, 3);
        setBrigadePosition(brigade1, 7, 3);
        fbMap.getFieldBattleSector(7, 3).setVillage(5);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisibleInInnerOfVillage() {
        setBrigadePosition(brigade0, 2, 3);
        setBrigadePosition(brigade1, 7, 3);
        fbMap.getFieldBattleSector(6, 3).setVillage(5);
        fbMap.getFieldBattleSector(7, 3).setVillage(5);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible1() {
        setBrigadePosition(brigade0, 2, 2);
        setBrigadePosition(brigade1, 7, 2);
        setBrigadePosition(brigadeObstacle1, 5, 2);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible2() {
        setBrigadePosition(brigade0, 1, 1);
        setBrigadePosition(brigade1, 4, 4);
        setBrigadePosition(brigadeObstacle1, 3, 3);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible3() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        setBrigadePosition(brigadeObstacle1, 4, 2);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible4() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        fbMap.getFieldBattleSector(4, 2).setVillage(5);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible5() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        fbMap.getFieldBattleSector(4, 2).setChateau(4);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible6() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        fbMap.getFieldBattleSector(4, 2).setForest(true);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible7() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        fbMap.getFieldBattleSector(4, 2).setWall(3);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible8() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        fbMap.getFieldBattleSector(4, 2).setTown(7);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testNotVisible9() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 4);
        fbMap.getFieldBattleSector(4, 2).setAltitude(2);

        assertFalse(visibilityProcessor.visible(brigade0, brigade1));
    }

    @Test
    public void testVisible() {
        setBrigadePosition(brigade0, 0, 0);
        setBrigadePosition(brigade1, 9, 0);

        fbMap.getFieldBattleSector(9, 0).setAltitude(2);

        assertTrue(visibilityProcessor.visible(brigade0, brigade1));
    }
}
