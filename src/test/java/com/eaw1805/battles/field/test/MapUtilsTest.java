package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.utils.MapUtils;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertSame;

public class MapUtilsTest {

    @Test
    public void test() {
    	
    	FieldBattleSector sector0 = new FieldBattleSector();
    	sector0.setX(10);
    	sector0.setY(10);
    	
    	FieldBattleSector sector1 = new FieldBattleSector();
    	sector1.setX(11);
    	sector1.setY(11);
    	
    	FieldBattleSector sector2 = new FieldBattleSector();
    	sector2.setX(12);
    	sector2.setY(12);
    	
    	FieldBattleSector sector3 = new FieldBattleSector();
    	sector3.setX(4);
    	sector3.setY(4);
    	
    	FieldBattleSector sectorOfReference = new FieldBattleSector();
    	sectorOfReference.setX(5);
    	sectorOfReference.setY(5);

    	List<FieldBattleSector> ordered = MapUtils.orderByDistance(sectorOfReference, Arrays.asList(new FieldBattleSector[]{sector0, sector1, sector2, sector3}));
    	assertSame(ordered.get(0), sector3);
    	assertSame(ordered.get(1), sector0);
    	assertSame(ordered.get(2), sector1);
    	assertSame(ordered.get(3), sector2);
}

}
