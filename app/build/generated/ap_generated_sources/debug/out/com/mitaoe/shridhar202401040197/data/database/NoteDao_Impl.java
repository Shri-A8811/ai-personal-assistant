package com.mitaoe.shridhar202401040197.data.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.mitaoe.shridhar202401040197.data.model.NoteItem;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NoteDao_Impl implements NoteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NoteItem> __insertionAdapterOfNoteItem;

  private final EntityDeletionOrUpdateAdapter<NoteItem> __deletionAdapterOfNoteItem;

  private final EntityDeletionOrUpdateAdapter<NoteItem> __updateAdapterOfNoteItem;

  public NoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNoteItem = new EntityInsertionAdapter<NoteItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notes` (`id`,`title`,`content`,`updatedAtMillis`,`category`,`colorHex`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final NoteItem entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getContent() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getContent());
        }
        statement.bindLong(4, entity.getUpdatedAtMillis());
        if (entity.getCategory() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCategory());
        }
        if (entity.getColorHex() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getColorHex());
        }
      }
    };
    this.__deletionAdapterOfNoteItem = new EntityDeletionOrUpdateAdapter<NoteItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `notes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final NoteItem entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfNoteItem = new EntityDeletionOrUpdateAdapter<NoteItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notes` SET `id` = ?,`title` = ?,`content` = ?,`updatedAtMillis` = ?,`category` = ?,`colorHex` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final NoteItem entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getContent() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getContent());
        }
        statement.bindLong(4, entity.getUpdatedAtMillis());
        if (entity.getCategory() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCategory());
        }
        if (entity.getColorHex() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getColorHex());
        }
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public long insert(final NoteItem note) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfNoteItem.insertAndReturnId(note);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final NoteItem note) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfNoteItem.handle(note);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final NoteItem note) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfNoteItem.handle(note);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<NoteItem>> getAllNotes() {
    final String _sql = "SELECT * FROM notes ORDER BY updatedAtMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"notes"}, false, new Callable<List<NoteItem>>() {
      @Override
      @Nullable
      public List<NoteItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfUpdatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAtMillis");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final List<NoteItem> _result = new ArrayList<NoteItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteItem _item;
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpContent;
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null;
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
            }
            final long _tmpUpdatedAtMillis;
            _tmpUpdatedAtMillis = _cursor.getLong(_cursorIndexOfUpdatedAtMillis);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            _item = new NoteItem(_tmpTitle,_tmpContent,_tmpUpdatedAtMillis,_tmpCategory,_tmpColorHex);
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            _item.setId(_tmpId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public List<NoteItem> getAllNotesSync() {
    final String _sql = "SELECT * FROM notes ORDER BY updatedAtMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
      final int _cursorIndexOfUpdatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAtMillis");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
      final List<NoteItem> _result = new ArrayList<NoteItem>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final NoteItem _item;
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        final String _tmpContent;
        if (_cursor.isNull(_cursorIndexOfContent)) {
          _tmpContent = null;
        } else {
          _tmpContent = _cursor.getString(_cursorIndexOfContent);
        }
        final long _tmpUpdatedAtMillis;
        _tmpUpdatedAtMillis = _cursor.getLong(_cursorIndexOfUpdatedAtMillis);
        final String _tmpCategory;
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _tmpCategory = null;
        } else {
          _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
        }
        final String _tmpColorHex;
        if (_cursor.isNull(_cursorIndexOfColorHex)) {
          _tmpColorHex = null;
        } else {
          _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
        }
        _item = new NoteItem(_tmpTitle,_tmpContent,_tmpUpdatedAtMillis,_tmpCategory,_tmpColorHex);
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        _item.setId(_tmpId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public NoteItem getNoteById(final long id) {
    final String _sql = "SELECT * FROM notes WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
      final int _cursorIndexOfUpdatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAtMillis");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
      final NoteItem _result;
      if (_cursor.moveToFirst()) {
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        final String _tmpContent;
        if (_cursor.isNull(_cursorIndexOfContent)) {
          _tmpContent = null;
        } else {
          _tmpContent = _cursor.getString(_cursorIndexOfContent);
        }
        final long _tmpUpdatedAtMillis;
        _tmpUpdatedAtMillis = _cursor.getLong(_cursorIndexOfUpdatedAtMillis);
        final String _tmpCategory;
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _tmpCategory = null;
        } else {
          _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
        }
        final String _tmpColorHex;
        if (_cursor.isNull(_cursorIndexOfColorHex)) {
          _tmpColorHex = null;
        } else {
          _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
        }
        _result = new NoteItem(_tmpTitle,_tmpContent,_tmpUpdatedAtMillis,_tmpCategory,_tmpColorHex);
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        _result.setId(_tmpId);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<NoteItem>> searchNotes(final String query) {
    final String _sql = "SELECT * FROM notes WHERE title LIKE '%' || ? || '%' OR content LIKE '%' || ? || '%' ORDER BY updatedAtMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (query == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, query);
    }
    _argIndex = 2;
    if (query == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, query);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"notes"}, false, new Callable<List<NoteItem>>() {
      @Override
      @Nullable
      public List<NoteItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfUpdatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAtMillis");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final List<NoteItem> _result = new ArrayList<NoteItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteItem _item;
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpContent;
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null;
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
            }
            final long _tmpUpdatedAtMillis;
            _tmpUpdatedAtMillis = _cursor.getLong(_cursorIndexOfUpdatedAtMillis);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpColorHex;
            if (_cursor.isNull(_cursorIndexOfColorHex)) {
              _tmpColorHex = null;
            } else {
              _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            }
            _item = new NoteItem(_tmpTitle,_tmpContent,_tmpUpdatedAtMillis,_tmpCategory,_tmpColorHex);
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            _item.setId(_tmpId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
