package com.nordling.ruuvilog.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mac: String,
    val name: String,
    val startTime: Long = System.currentTimeMillis()
)
