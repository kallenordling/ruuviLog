package com.nordling.ruuvilog.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Query("SELECT * FROM sessions WHERE mac = :mac ORDER BY startTime DESC")
    fun getByMac(mac: String): LiveData<List<Session>>

    @Query("DELETE FROM sessions WHERE mac = :mac")
    suspend fun deleteByMac(mac: String)
}
