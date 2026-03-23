package com.nordling.ruuvilog.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LogEntry::class, Session::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE log_entries ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE log_entries ADD COLUMN longitude REAL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mac TEXT NOT NULL, name TEXT NOT NULL, startTime INTEGER NOT NULL)")
                database.execSQL("ALTER TABLE log_entries ADD COLUMN sessionId INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ruuvi_log.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
