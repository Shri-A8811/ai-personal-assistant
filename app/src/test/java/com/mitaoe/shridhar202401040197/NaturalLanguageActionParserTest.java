package com.mitaoe.shridhar202401040197;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.mitaoe.shridhar202401040197.ai.NaturalLanguageActionParser;
import com.mitaoe.shridhar202401040197.ai.NaturalLanguageActionParser.ActionResult;
import com.mitaoe.shridhar202401040197.ai.NaturalLanguageActionParser.ActionType;

import org.junit.Test;

public class NaturalLanguageActionParserTest {

    @Test
    public void testNoteCreationIntent() {
        ActionResult res = NaturalLanguageActionParser.parseIntent("note down: Buy groceries and eggs");
        assertEquals(ActionType.CREATE_NOTE, res.type);
        assertTrue(res.title.contains("Buy groceries"));
        assertEquals("Buy groceries and eggs", res.details);

        ActionResult res2 = NaturalLanguageActionParser.parseIntent("take a note: Meeting rescheduled to 4pm");
        assertEquals(ActionType.CREATE_NOTE, res2.type);
        assertTrue(res2.details.contains("Meeting rescheduled"));
    }

    @Test
    public void testReminderCreationWithRelativeTime() {
        ActionResult res = NaturalLanguageActionParser.parseIntent("remind me to call John in 2 hours");
        assertEquals(ActionType.CREATE_REMINDER, res.type);
        assertTrue(res.title.toLowerCase().contains("call john"));
        assertTrue(res.dueMillis > System.currentTimeMillis());
        assertEquals("NONE", res.recurrence);
    }

    @Test
    public void testDailyRecurringReminder() {
        ActionResult res = NaturalLanguageActionParser.parseIntent("remind me daily to take medicine at 9am");
        assertEquals(ActionType.CREATE_REMINDER, res.type);
        assertEquals("DAILY", res.recurrence);
        assertTrue(res.title.toLowerCase().contains("take medicine"));
    }

    @Test
    public void testWeeklyRecurringTask() {
        ActionResult res = NaturalLanguageActionParser.parseIntent("add task review budget every week");
        assertEquals(ActionType.CREATE_TASK, res.type);
        assertEquals("WEEKLY", res.recurrence);
        assertTrue(res.title.toLowerCase().contains("review budget"));
    }

    @Test
    public void testPriorityDetection() {
        ActionResult resHigh = NaturalLanguageActionParser.parseIntent("remind me to submit college fee urgently");
        assertEquals("HIGH", resHigh.priority);

        ActionResult resLow = NaturalLanguageActionParser.parseIntent("create task with low priority organize desk");
        assertEquals("LOW", resLow.priority);

        ActionResult resNormal = NaturalLanguageActionParser.parseIntent("new task finish report");
        assertEquals("MEDIUM", resNormal.priority);
    }

    @Test
    public void testNonActionInput() {
        ActionResult res = NaturalLanguageActionParser.parseIntent("What is the capital of France?");
        assertEquals(ActionType.NONE, res.type);

        ActionResult resEmpty = NaturalLanguageActionParser.parseIntent("");
        assertEquals(ActionType.NONE, resEmpty.type);
    }

    @Test
    public void testQuestionsDoNotTriggerActions() {
        // Questions with inquiry words should NEVER trigger task/reminder creation
        assertEquals(ActionType.NONE, NaturalLanguageActionParser.parseIntent("Can you remind me how recursion works?").type);
        assertEquals(ActionType.NONE, NaturalLanguageActionParser.parseIntent("What is a good task to learn Python?").type);
        assertEquals(ActionType.NONE, NaturalLanguageActionParser.parseIntent("Did you give me a task to complete?").type);
        assertEquals(ActionType.NONE, NaturalLanguageActionParser.parseIntent("Why did you remind me?").type);
        assertEquals(ActionType.NONE, NaturalLanguageActionParser.parseIntent("remind me what time the train arrives").type);
        assertEquals(ActionType.NONE, NaturalLanguageActionParser.parseIntent("Do I have a task to finish?").type);
    }

    @Test
    public void testPoliteCommandsStripPolitePrefixAndPunctuation() {
        ActionResult res = NaturalLanguageActionParser.parseIntent("Can you remind me to buy milk tomorrow at 5pm?");
        assertEquals(ActionType.CREATE_REMINDER, res.type);
        assertEquals("Buy milk", res.title);

        ActionResult resTask = NaturalLanguageActionParser.parseIntent("Please add a task to clean the kitchen.");
        assertEquals(ActionType.CREATE_TASK, resTask.type);
        assertEquals("Clean the kitchen", resTask.title);
    }

    @Test
    public void testWordClippingNamesAndNounsPreserved() {
        ActionResult resJason = NaturalLanguageActionParser.parseIntent("remind me to call Jason");
        assertEquals("Call Jason", resJason.title);

        ActionResult resBoat = NaturalLanguageActionParser.parseIntent("remind me to inspect the boat");
        assertEquals("Inspect the boat", resBoat.title);
    }
}
