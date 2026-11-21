package com.mitaoe.shridhar202401040197.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
import com.mitaoe.shridhar202401040197.data.model.Conversation;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import com.mitaoe.shridhar202401040197.data.model.TaskItem;

@Database(entities = {TaskItem.class, NoteItem.class, ChatMessage.class, Conversation.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TaskDao taskDao();
    public abstract NoteDao noteDao();
    public abstract ChatDao chatDao();
    public abstract ConversationDao conversationDao();

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final String TAG = "AppDatabase";
    private static final String DB_NAME = "ai_personal_assistant.db";

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    INSTANCE = buildDatabase(appContext);
                    // Defensive check: proactively verify database can open and schema validates.
                    // Narrow recovery to schema/corruption exceptions and preserve a backup copy before rebuild.
                    try {
                        INSTANCE.getOpenHelper().getWritableDatabase();
                    } catch (IllegalStateException | android.database.sqlite.SQLiteException t) {
                        android.util.Log.e(TAG, "Database schema validation or corruption detected, backing up & rebuilding", t);
                        try {
                            INSTANCE.close();
                        } catch (Exception ignored) {}
                        backupDatabaseFileQuietly(appContext);
                        appContext.deleteDatabase(DB_NAME);
                        INSTANCE = buildDatabase(appContext);
                        try {
                            INSTANCE.getOpenHelper().getWritableDatabase();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return INSTANCE;
    }

    private static void backupDatabaseFileQuietly(Context context) {
        try {
            java.io.File src = context.getDatabasePath(DB_NAME);
            if (src != null && src.exists()) {
                java.io.File dst = new java.io.File(src.getParentFile(), "ai_personal_assistant_recovery.db");
                try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(dst)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static AppDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                DB_NAME
        )
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build();
    }
}
