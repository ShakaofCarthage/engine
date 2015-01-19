package com.eaw1805.core.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronExpression;

import java.util.Calendar;

/**
 * Test Cron Schedule expressions.
 */
public class CronScheduleTest {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(CronScheduleTest.class);

    public static void main(String[] args)
            throws Exception {

        final Calendar nextTurn = Calendar.getInstance();
        nextTurn.set(Calendar.HOUR_OF_DAY, 0);
        nextTurn.set(Calendar.MINUTE, 0);
        nextTurn.set(Calendar.SECOND, 0);
        nextTurn.set(Calendar.DATE, 24);

        final String expression = "0 0 0 ? * WED,SUN *";
        final CronExpression cexp = new CronExpression(expression);
        LOGGER.info(cexp.getNextValidTimeAfter(nextTurn.getTime()).toString());
    }

}
