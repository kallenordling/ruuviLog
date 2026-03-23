package com.nordling.ruuvilog.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LogDao _logDao;

  private volatile SessionDao _sessionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `log_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mac` TEXT NOT NULL, `temperature` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `sessionId` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mac` TEXT NOT NULL, `name` TEXT NOT NULL, `startTime` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9bf2dc67aefcdce0f831bd6ff92a6c41')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `log_entries`");
        db.execSQL("DROP TABLE IF EXISTS `sessions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLogEntries = new HashMap<String, TableInfo.Column>(7);
        _columnsLogEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("mac", new TableInfo.Column("mac", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("temperature", new TableInfo.Column("temperature", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogEntries.put("sessionId", new TableInfo.Column("sessionId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLogEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLogEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLogEntries = new TableInfo("log_entries", _columnsLogEntries, _foreignKeysLogEntries, _indicesLogEntries);
        final TableInfo _existingLogEntries = TableInfo.read(db, "log_entries");
        if (!_infoLogEntries.equals(_existingLogEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "log_entries(com.nordling.ruuvilog.db.LogEntry).\n"
                  + " Expected:\n" + _infoLogEntries + "\n"
                  + " Found:\n" + _existingLogEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsSessions = new HashMap<String, TableInfo.Column>(4);
        _columnsSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessions.put("mac", new TableInfo.Column("mac", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessions.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessions.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSessions = new TableInfo("sessions", _columnsSessions, _foreignKeysSessions, _indicesSessions);
        final TableInfo _existingSessions = TableInfo.read(db, "sessions");
        if (!_infoSessions.equals(_existingSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "sessions(com.nordling.ruuvilog.db.Session).\n"
                  + " Expected:\n" + _infoSessions + "\n"
                  + " Found:\n" + _existingSessions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9bf2dc67aefcdce0f831bd6ff92a6c41", "a2c7d2446f21e819f8c805db85b6041f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "log_entries","sessions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `log_entries`");
      _db.execSQL("DELETE FROM `sessions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LogDao.class, LogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SessionDao.class, SessionDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LogDao logDao() {
    if (_logDao != null) {
      return _logDao;
    } else {
      synchronized(this) {
        if(_logDao == null) {
          _logDao = new LogDao_Impl(this);
        }
        return _logDao;
      }
    }
  }

  @Override
  public SessionDao sessionDao() {
    if (_sessionDao != null) {
      return _sessionDao;
    } else {
      synchronized(this) {
        if(_sessionDao == null) {
          _sessionDao = new SessionDao_Impl(this);
        }
        return _sessionDao;
      }
    }
  }
}
