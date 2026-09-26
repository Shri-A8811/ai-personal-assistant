package com.mitaoe.shridhar202401040197.data.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.mitaoe.shridhar202401040197.data.model.ChatMessage;
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
public final class ChatDao_Impl implements ChatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatMessage> __insertionAdapterOfChatMessage;

  private final EntityDeletionOrUpdateAdapter<ChatMessage> __deletionAdapterOfChatMessage;

  private final SharedSQLiteStatement __preparedStmtOfClearAllMessages;

  public ChatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatMessage = new EntityInsertionAdapter<ChatMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chat_messages` (`id`,`text`,`isUser`,`timestamp`,`modelName`,`hasAction`,`actionType`,`actionTitle`,`imageUri`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final ChatMessage entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getText() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getText());
        }
        final int _tmp = entity.isUser() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getTimestamp());
        if (entity.getModelName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getModelName());
        }
        final int _tmp_1 = entity.isHasAction() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        if (entity.getActionType() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getActionType());
        }
        if (entity.getActionTitle() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getActionTitle());
        }
        if (entity.getImageUri() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getImageUri());
        }
      }
    };
    this.__deletionAdapterOfChatMessage = new EntityDeletionOrUpdateAdapter<ChatMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `chat_messages` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final ChatMessage entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfClearAllMessages = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_messages";
        return _query;
      }
    };
  }

  @Override
  public long insert(final ChatMessage message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfChatMessage.insertAndReturnId(message);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final ChatMessage message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfChatMessage.handle(message);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clearAllMessages() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllMessages.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearAllMessages.release(_stmt);
    }
  }

  @Override
  public LiveData<List<ChatMessage>> getAllMessages() {
    final String _sql = "SELECT * FROM chat_messages ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"chat_messages"}, false, new Callable<List<ChatMessage>>() {
      @Override
      @Nullable
      public List<ChatMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfIsUser = CursorUtil.getColumnIndexOrThrow(_cursor, "isUser");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfModelName = CursorUtil.getColumnIndexOrThrow(_cursor, "modelName");
          final int _cursorIndexOfHasAction = CursorUtil.getColumnIndexOrThrow(_cursor, "hasAction");
          final int _cursorIndexOfActionType = CursorUtil.getColumnIndexOrThrow(_cursor, "actionType");
          final int _cursorIndexOfActionTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "actionTitle");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final List<ChatMessage> _result = new ArrayList<ChatMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatMessage _item;
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final boolean _tmpIsUser;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUser);
            _tmpIsUser = _tmp != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpModelName;
            if (_cursor.isNull(_cursorIndexOfModelName)) {
              _tmpModelName = null;
            } else {
              _tmpModelName = _cursor.getString(_cursorIndexOfModelName);
            }
            _item = new ChatMessage(_tmpText,_tmpIsUser,_tmpTimestamp,_tmpModelName);
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            _item.setId(_tmpId);
            final boolean _tmpHasAction;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasAction);
            _tmpHasAction = _tmp_1 != 0;
            _item.setHasAction(_tmpHasAction);
            final String _tmpActionType;
            if (_cursor.isNull(_cursorIndexOfActionType)) {
              _tmpActionType = null;
            } else {
              _tmpActionType = _cursor.getString(_cursorIndexOfActionType);
            }
            _item.setActionType(_tmpActionType);
            final String _tmpActionTitle;
            if (_cursor.isNull(_cursorIndexOfActionTitle)) {
              _tmpActionTitle = null;
            } else {
              _tmpActionTitle = _cursor.getString(_cursorIndexOfActionTitle);
            }
            _item.setActionTitle(_tmpActionTitle);
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            _item.setImageUri(_tmpImageUri);
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
  public List<ChatMessage> getAllMessagesSync() {
    final String _sql = "SELECT * FROM chat_messages ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
      final int _cursorIndexOfIsUser = CursorUtil.getColumnIndexOrThrow(_cursor, "isUser");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfModelName = CursorUtil.getColumnIndexOrThrow(_cursor, "modelName");
      final int _cursorIndexOfHasAction = CursorUtil.getColumnIndexOrThrow(_cursor, "hasAction");
      final int _cursorIndexOfActionType = CursorUtil.getColumnIndexOrThrow(_cursor, "actionType");
      final int _cursorIndexOfActionTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "actionTitle");
      final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
      final List<ChatMessage> _result = new ArrayList<ChatMessage>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final ChatMessage _item;
        final String _tmpText;
        if (_cursor.isNull(_cursorIndexOfText)) {
          _tmpText = null;
        } else {
          _tmpText = _cursor.getString(_cursorIndexOfText);
        }
        final boolean _tmpIsUser;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsUser);
        _tmpIsUser = _tmp != 0;
        final long _tmpTimestamp;
        _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        final String _tmpModelName;
        if (_cursor.isNull(_cursorIndexOfModelName)) {
          _tmpModelName = null;
        } else {
          _tmpModelName = _cursor.getString(_cursorIndexOfModelName);
        }
        _item = new ChatMessage(_tmpText,_tmpIsUser,_tmpTimestamp,_tmpModelName);
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        _item.setId(_tmpId);
        final boolean _tmpHasAction;
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfHasAction);
        _tmpHasAction = _tmp_1 != 0;
        _item.setHasAction(_tmpHasAction);
        final String _tmpActionType;
        if (_cursor.isNull(_cursorIndexOfActionType)) {
          _tmpActionType = null;
        } else {
          _tmpActionType = _cursor.getString(_cursorIndexOfActionType);
        }
        _item.setActionType(_tmpActionType);
        final String _tmpActionTitle;
        if (_cursor.isNull(_cursorIndexOfActionTitle)) {
          _tmpActionTitle = null;
        } else {
          _tmpActionTitle = _cursor.getString(_cursorIndexOfActionTitle);
        }
        _item.setActionTitle(_tmpActionTitle);
        final String _tmpImageUri;
        if (_cursor.isNull(_cursorIndexOfImageUri)) {
          _tmpImageUri = null;
        } else {
          _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
        }
        _item.setImageUri(_tmpImageUri);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
