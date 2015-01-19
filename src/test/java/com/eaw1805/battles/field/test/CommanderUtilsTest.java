package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.utils.CommanderUtils;
import com.eaw1805.data.model.army.Commander;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class CommanderUtilsTest {

    @Test
    public void testGetCommanderInfluenceRadius() {
        Commander commander = new Commander();

        commander.setStrc(16);
        assertSame(4, CommanderUtils.getCommanderInfluenceRadius(commander));

        commander.setStrc(20);
        assertSame(4, CommanderUtils.getCommanderInfluenceRadius(commander));

        commander.setStrc(21);
        assertSame(5, CommanderUtils.getCommanderInfluenceRadius(commander));

        commander.setStrc(25);
        assertSame(5, CommanderUtils.getCommanderInfluenceRadius(commander));
    }

}
