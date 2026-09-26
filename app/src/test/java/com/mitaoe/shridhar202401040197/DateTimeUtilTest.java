package com.mitaoe.shridhar202401040197;

import com.mitaoe.shridhar202401040197.ai.NaturalLanguageActionParser;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateTimeUtilTest {

    @Test
    public void testParseTomorrowAt5pmWithQuantities() {
        // "buy 2 apples tomorrow at 5pm" - should parse 5pm (17:00), not 2!
        long result = DateTimeUtil.parseNaturalDate("Remind me to buy 2 apples tomorrow at 5pm");
        assertTrue("Result should be greater than 0", result > 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(result);

        assertEquals("Hour should be 17 (5 PM)", 17, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Minute should be 0", 0, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testParseQuantityWithoutTime() {
        // "Read chapter 4 tomorrow" - should default to 9 AM, not 4 AM/PM!
        long result = DateTimeUtil.parseNaturalDate("Read chapter 4 tomorrow");
        assertTrue("Result should be greater than 0", result > 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(result);

        assertEquals("Hour should default to 9 AM for non-time numbers", 9, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testParseExplicitMinutesAndAm() {
        long result = DateTimeUtil.parseNaturalDate("Call doctor tomorrow at 8:45 am");
        assertTrue(result > 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(result);

        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testParse24HourFormat() {
        long result = DateTimeUtil.parseNaturalDate("Deploy release tomorrow at 18:30");
        assertTrue(result > 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(result);

        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
    }
}
