package com.mitaoe.shridhar202401040197.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mitaoe.shridhar202401040197.data.model.TaskItem;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TaskItem task);

    @Update
    void update(TaskItem task);

    @Delete
    void delete(TaskItem task);

    @Query("SELECT * FROM tasks ORDER BY dueDateMillis ASC, id DESC")
    LiveData<List<TaskItem>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDateMillis ASC")
    LiveData<List<TaskItem>> getPendingTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY dueDateMillis DESC")
    LiveData<List<TaskItem>> getCompletedTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDateMillis > 0 AND dueDateMillis <= :endOfDayMillis ORDER BY dueDateMillis ASC")
    LiveData<List<TaskItem>> getTasksDueToday(long endOfDayMillis);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    List<TaskItem> getPendingTasksSync();

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    TaskItem getTaskById(long id);

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    void setTaskCompleted(long id, boolean completed);
}
