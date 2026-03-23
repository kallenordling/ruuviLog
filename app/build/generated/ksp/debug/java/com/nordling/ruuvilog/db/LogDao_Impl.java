package com.nordling.ruuvilog.db;

import android.database.Cursor;
import android.os.CancellationSignal;
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
import java.lang.Double;
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
public final class LogDao_Impl implements LogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LogEntry> __insertionAdapterOfLogEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByMac;

  public LogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLogEntry = new EntityInsertionAdapter<LogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `log_entries` (`id`,`mac`,`temperature`,`timestamp`,`latitude`,`longitude`,`sessionId`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getMac());
        statement.bindDouble(3, entity.getTemperature());
        statement.bindLong(4, entity.getTimestamp());
        if (entity.getLatitude() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getLongitude());
        }
        if (entity.getSessionId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getSessionId());
        }
      }
    };
    this.__preparedStmtOfDeleteByMac = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM log_entries WHERE mac = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final LogEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLogEntry.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
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
  public LiveData<List<LogEntry>> getByMac(final String mac) {
    final String _sql = "SELECT * FROM log_entries WHERE mac = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mac);
    return __db.getInvalidationTracker().createLiveData(new String[] {"log_entries"}, false, new Callable<List<LogEntry>>() {
      @Override
      @Nullable
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMac = CursorUtil.getColumnIndexOrThrow(_cursor, "mac");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMac;
            _tmpMac = _cursor.getString(_cursorIndexOfMac);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Long _tmpSessionId;
            if (_cursor.isNull(_cursorIndexOfSessionId)) {
              _tmpSessionId = null;
            } else {
              _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            }
            _item = new LogEntry(_tmpId,_tmpMac,_tmpTemperature,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpSessionId);
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
  public Object getGpsTrackByMac(final String mac,
      final Continuation<? super List<LogEntry>> $completion) {
    final String _sql = "SELECT * FROM log_entries WHERE mac = ? AND latitude IS NOT NULL ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mac);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMac = CursorUtil.getColumnIndexOrThrow(_cursor, "mac");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMac;
            _tmpMac = _cursor.getString(_cursorIndexOfMac);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Long _tmpSessionId;
            if (_cursor.isNull(_cursorIndexOfSessionId)) {
              _tmpSessionId = null;
            } else {
              _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            }
            _item = new LogEntry(_tmpId,_tmpMac,_tmpTemperature,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpSessionId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getGpsTrackBySession(final long sessionId,
      final Continuation<? super List<LogEntry>> $completion) {
    final String _sql = "SELECT * FROM log_entries WHERE sessionId = ? AND latitude IS NOT NULL ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMac = CursorUtil.getColumnIndexOrThrow(_cursor, "mac");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMac;
            _tmpMac = _cursor.getString(_cursorIndexOfMac);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Long _tmpSessionId;
            if (_cursor.isNull(_cursorIndexOfSessionId)) {
              _tmpSessionId = null;
            } else {
              _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            }
            _item = new LogEntry(_tmpId,_tmpMac,_tmpTemperature,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpSessionId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllBySession(final long sessionId,
      final Continuation<? super List<LogEntry>> $completion) {
    final String _sql = "SELECT * FROM log_entries WHERE sessionId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMac = CursorUtil.getColumnIndexOrThrow(_cursor, "mac");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMac;
            _tmpMac = _cursor.getString(_cursorIndexOfMac);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Long _tmpSessionId;
            if (_cursor.isNull(_cursorIndexOfSessionId)) {
              _tmpSessionId = null;
            } else {
              _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            }
            _item = new LogEntry(_tmpId,_tmpMac,_tmpTemperature,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpSessionId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllByMac(final String mac,
      final Continuation<? super List<LogEntry>> $completion) {
    final String _sql = "SELECT * FROM log_entries WHERE mac = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mac);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LogEntry>>() {
      @Override
      @NonNull
      public List<LogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMac = CursorUtil.getColumnIndexOrThrow(_cursor, "mac");
          final int _cursorIndexOfTemperature = CursorUtil.getColumnIndexOrThrow(_cursor, "temperature");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final List<LogEntry> _result = new ArrayList<LogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMac;
            _tmpMac = _cursor.getString(_cursorIndexOfMac);
            final double _tmpTemperature;
            _tmpTemperature = _cursor.getDouble(_cursorIndexOfTemperature);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Long _tmpSessionId;
            if (_cursor.isNull(_cursorIndexOfSessionId)) {
              _tmpSessionId = null;
            } else {
              _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            }
            _item = new LogEntry(_tmpId,_tmpMac,_tmpTemperature,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpSessionId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
