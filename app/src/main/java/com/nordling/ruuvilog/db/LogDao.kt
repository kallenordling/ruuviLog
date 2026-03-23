package com.nordling.ruuvilog.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {

    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM log_entries WHERE mac = :mac ORDER BY timestamp DESC")
    fun getByMac(mac: String): LiveData<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE mac = :mac AND latitude IS NOT NULL ORDER BY timestamp ASC")
    suspend fun getGpsTrackByMac(mac: String): List<LogEntry>

    @Query("DELETE FROM log_entries WHERE mac = :mac")
    suspend fun deleteByMac(mac: String)
}
