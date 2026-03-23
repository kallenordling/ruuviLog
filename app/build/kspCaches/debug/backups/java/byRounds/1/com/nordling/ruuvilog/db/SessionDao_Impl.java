package com.nordling.ruuvilog.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Session> __insertionAdapterOfSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByMac;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSession = new EntityInsertionAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sessions` (`id`,`mac`,`name`,`startTime`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getMac());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getStartTime());
      }
    };
    this.__preparedStmtOfDeleteByMac = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sessions WHERE mac = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Session session, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSession.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByMac(final String mac, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByMac.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, mac);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByMac.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public LiveData<List<Session>> getByMac(final String mac) {
    final String _sql = "SELECT * FROM sessions WHERE mac = ? ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mac);
    return __db.getInvalidationTracker().createLiveData(new String[] {"sessions"}, false, new Callable<List<Session>>() {
      @Override
      @Nullable
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMac = CursorUtil.getColumnIndexOrThrow(_cursor, "mac");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMac;
            _tmpMac = _cursor.getString(_cursorIndexOfMac);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            _item = new Session(_tmpId,_tmpMac,_tmpName,_tmpStartTime);
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
