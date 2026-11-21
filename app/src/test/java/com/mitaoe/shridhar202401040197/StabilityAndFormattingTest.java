package com.mitaoe.shridhar202401040197;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mitaoe.shridhar202401040197.ai.AiService;
import com.mitaoe.shridhar202401040197.util.TaskUtil;

import org.junit.Test;

public class StabilityAndFormattingTest {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final long WEEK_MS = 7L * DAY_MS;

    @Test
    public void testRecurrenceHandlesZeroOrUnsetDueDateWithoutLoop() {
        long now = 1_760_000_000_000L;

        long nextDaily = TaskUtil.computeNextRecurrenceTrigger(0L, "DAILY", now);
        assertEquals(now + DAY_MS, nextDaily);

        long nextWeekly = TaskUtil.computeNextRecurrenceTrigger(-1L, "WEEKLY", now);
        assertEquals(now + WEEK_MS, nextWeekly);
    }

    @Test
    public void testRecurrenceAdvancesPastDueDatesIntoFuture() {
        long now = 1_760_000_000_000L;
        // 3.5 days in the past -> must advance 4 daily cycles from due date
        long staleDue = now - (3L * DAY_MS + 12L * 60 * 60 * 1000);
        long nextTrigger = TaskUtil.computeNextRecurrenceTrigger(staleDue, "DAILY", now);

        assertTrue("Next trigger must be strictly in the future", nextTrigger > now);
        assertEquals(staleDue + 4L * DAY_MS, nextTrigger);
    }

    @Test
    public void testRecurrenceNoneLeavesTimestampUntouched() {
        long due = 1_760_000_500_000L;
        assertEquals(due, TaskUtil.computeNextRecurrenceTrigger(due, "NONE", 1_760_000_000_000L));
        assertEquals(due, TaskUtil.computeNextRecurrenceTrigger(due, null, 1_760_000_000_000L));
    }

    @Test
    public void testRequestHandleCancellationState() {
        AiService.RequestHandle handle = new AiService.RequestHandle();
        assertFalse(handle.isCancelled());
        handle.cancel();
        assertTrue(handle.isCancelled());
    }
}