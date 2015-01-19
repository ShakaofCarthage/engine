package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.utils.MathUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MathUtilsTest {

    @Test
    public void test() {

        for (int i = 0; i < 1000; i++) {
            double random = MathUtils.generateRandomDoubleInRange(1.5d, 3.5d);
            assertTrue(1.5d <= random);
            assertTrue(random <= 3.5d);
        }
    }

}
