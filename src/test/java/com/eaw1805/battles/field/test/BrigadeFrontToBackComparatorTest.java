package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.processors.MovementProcessor.BrigadeFrontToBackComparator;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertSame;

public class BrigadeFrontToBackComparatorTest {

    @Test
    public void testCompare() {

        Brigade brigade0 = new Brigade();
        brigade0.setFieldBattlePosition(new FieldBattlePosition(5, 3));

        Brigade brigade1 = new Brigade();
        brigade1.setFieldBattlePosition(new FieldBattlePosition(5, 2));

        Brigade brigade2 = new Brigade();
        brigade2.setFieldBattlePosition(new FieldBattlePosition(5, 4));

        Brigade brigade3 = new Brigade();
        brigade3.setFieldBattlePosition(new FieldBattlePosition(6, 4));

        List<Brigade> brigades = Arrays.asList(new Brigade[]{brigade0, brigade1, brigade2, brigade3});
        Collections.sort(brigades, new BrigadeFrontToBackComparator(0));

        assertSame(brigades.get(0), brigade2);
        assertSame(brigades.get(1), brigade3);
        assertSame(brigades.get(2), brigade0);
        assertSame(brigades.get(3), brigade1);

        Collections.sort(brigades, new BrigadeFrontToBackComparator(1));

        assertSame(brigades.get(0), brigade1);
        assertSame(brigades.get(1), brigade0);
        assertSame(brigades.get(2), brigade2);
        assertSame(brigades.get(3), brigade3);
    }

}
