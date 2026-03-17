package com.nordling.ruuvilog

data class RuuviTag(
    val mac: String,
    val dataFormat: Int,
    val temperature: Double?,
    val humidity: Double?,
    val pressure: Double?,
    val accelX: Int?,
    val accelY: Int?,
    val accelZ: Int?,
    val batteryVoltage: Double?,
    val txPower: Int?,
    val movementCounter: Int?,
    val sequenceNumber: Int?,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis()
)
