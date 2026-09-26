package com.mitaoe.shridhar202401040197.ai;

import android.content.Context;

import com.mitaoe.shridhar202401040197.data.database.AppDatabase;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;
import com.mitaoe.shridhar202401040197.service.NotificationHelper;
import com.mitaoe.shridhar202401040197.util.DateTimeUtil;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaturalLanguageActionParser {

    public enum ActionType {
        NONE,
        CREATE_TASK,
        CREATE_REMINDER,
        CREATE_NOTE
    }

    public static class ActionResult {
        public ActionType type = ActionType.NONE;
        public String title = "";
        public String details = "";
        public long dueMillis = 0;
        public String priority = "MEDIUM";
        public String confirmationMessage = "";
        public boolean isSuccess = false;
    }

    public static ActionResult parseAndExecute(Context context, String userInput) {
        ActionResult result = new ActionResult();
        if (userInput == null || userInput.trim().isEmpty()) return result;

        String lower = userInput.toLowerCase(Locale.ROOT).trim();

        // 1. Detect Note Intent: "take a note:", "create note:", "note down:", "add note:"
        if (lower.startsWith("note:") || lower.startsWith("note down") || lower.startsWith("take a note") || lower.startsWith("create note") || lower.startsWith("write note")) {
            result.type = ActionType.CREATE_NOTE;
            String content = userInput.replaceFirst("(?i)^(note:\\s*|note down:?\\s*|take a note:?\\s*|create note:?\\s*|write note:?\\s*)", "").trim();
            if (content.isEmpty()) content = userInput;

            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            result.title = title;
            result.details = content;

            AppDatabase db = AppDatabase.getInstance(context);
            NoteItem note = new NoteItem(title, content, System.currentTimeMillis(), "Assistant", "#1E1E1E");
            long id = db.noteDao().insert(note);
            result.isSuccess = id > 0;
            result.confirmationMessage = "📝 Note saved:\n**" + title + "**";
            return result;
        }

        // 2. Detect Reminder Intent: "remind me to...", "set a reminder for...", "reminder: ..."
        boolean isReminder = lower.contains("remind me to") || lower.contains("set a reminder") || lower.startsWith("reminder:") || lower.contains("remind me");
        boolean isTask = lower.startsWith("add task") || lower.startsWith("new task") || lower.startsWith("create task") || lower.contains("task to");

        if (isReminder || isTask) {
            result.type = isReminder ? ActionType.CREATE_REMINDER : ActionType.CREATE_TASK;

            // Priority detection
            if (lower.contains("urgent") || lower.contains("high priority") || lower.contains("asap") || lower.contains("important")) {
                result.priority = "HIGH";
            } else if (lower.contains("low priority")) {
                result.priority = "LOW";
            } else {
                result.priority = "MEDIUM";
            }

            // Parse Date & Time
            long dueMillis = DateTimeUtil.parseNaturalDate(userInput);
            result.dueMillis = dueMillis;

            // Extract Title
            String cleaned = userInput.replaceFirst("(?i)^(remind me to\\s*|set a reminder for\\s*|reminder:\\s*|remind me\\s*|add task:?\\s*|new task:?\\s*|create task:?\\s*)", "").trim();

            // Strip time phrases from title for clean display
            cleaned = cleaned.replaceAll("(?i)(tomorrow|today|tonight|at \\d{1,2}(:\\d{2})?\\s*(am|pm)?|with (high|low|medium) priority)", "").trim();
            if (cleaned.endsWith("at") || cleaned.endsWith("on") || cleaned.endsWith("for")) {
                cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
            }

            if (cleaned.isEmpty()) cleaned = "Personal Reminder";
            // Capitalize first letter
            cleaned = cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
            result.title = cleaned;

            // Insert into Database
            AppDatabase db = AppDatabase.getInstance(context);
            TaskItem task = new TaskItem(
                    result.title,
                    isReminder ? "Created by Assistant reminder" : "Created by Assistant task",
                    result.dueMillis,
                    result.priority,
                    isReminder ? "Reminders" : "Tasks",
                    isReminder && result.dueMillis > System.currentTimeMillis()
            );

            long taskId = db.taskDao().insert(task);
            result.isSuccess = taskId > 0;

            if (task.isHasReminder() && task.getDueDateMillis() > System.currentTimeMillis()) {
                NotificationHelper.scheduleAlarm(context, taskId, task.getTitle(), task.getDueDateMillis());
            }

            if (isReminder) {
                String whenStr = result.dueMillis > 0 ? " for **" + DateTimeUtil.formatDateTime(result.dueMillis) + "**" : "";
                result.confirmationMessage = "⏰ Reminder scheduled" + whenStr + ":\n**" + result.title + "** (Priority: " + result.priority + ")";
            } else {
                String whenStr = result.dueMillis > 0 ? " (Due: " + DateTimeUtil.formatDateTime(result.dueMillis) + ")" : "";
                result.confirmationMessage = "✅ Task added to your list:\n**" + result.title + "**" + whenStr;
            }

            return result;
        }

        return result;
    }
}
