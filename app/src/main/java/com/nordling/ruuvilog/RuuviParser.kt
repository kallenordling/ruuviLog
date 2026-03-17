package com.nordling.ruuvilog

import java.nio.ByteBuffer
import java.nio.ByteOrder

object RuuviParser {

    const val MANUFACTURER_ID = 0x0499

    fun parse(mac: String, rssi: Int, data: ByteArray): RuuviTag? {
        if (data.isEmpty()) return null
        return when (data[0].toInt() and 0xFF) {
            5 -> parseFormat5(mac, rssi, data)
            3 -> parseFormat3(mac, rssi, data)
            else -> null
        }
    }

    private fun parseFormat5(mac: String, rssi: Int, data: ByteArray): RuuviTag? {
        if (data.size < 24) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        buf.position(1)
        val tempRaw = buf.short.toInt()
        val humRaw = buf.short.toInt() and 0xFFFF
        val presRaw = buf.short.toInt() and 0xFFFF
        val accX = buf.short.toInt()
        val accY = buf.short.toInt()
        val accZ = buf.short.toInt()
        val powerRaw = buf.short.toInt() and 0xFFFF
        val movement = buf.get().toInt() and 0xFF
        val seq = buf.short.toInt() and 0xFFFF

        val batteryMv = (powerRaw shr 5) + 1600
        val txPower = (powerRaw and 0x1F) * 2 - 40

        return RuuviTag(
            mac = mac,
            dataFormat = 5,
            rssi = rssi,
            temperature = if (tempRaw != 0x8000.toShort().toInt()) tempRaw / 200.0 else null,
            humidity = if (humRaw != 0xFFFF) humRaw / 400.0 else null,
            pressure = if (presRaw != 0xFFFF) (presRaw + 50000).toDouble() else null,
            accelX = if (accX != 0x8000.toShort().toInt()) accX else null,
            accelY = if (accY != 0x8000.toShort().toInt()) accY else null,
            accelZ = if (accZ != 0x8000.toShort().toInt()) accZ else null,
            batteryVoltage = if (powerRaw != 0xFFFF) batteryMv / 1000.0 else null,
            txPower = if (powerRaw != 0xFFFF) txPower else null,
            movementCounter = if (movement != 0xFF) movement else null,
            sequenceNumber = if (seq != 0xFFFF) seq else null
        )
    }

    private fun parseFormat3(mac: String, rssi: Int, data: ByteArray): RuuviTag? {
        if (data.size < 14) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        buf.position(1)
        val humRaw = buf.get().toInt() and 0xFF
        val tempInt = buf.get().toInt()         // signed
        val tempFrac = buf.get().toInt() and 0xFF
        val presRaw = buf.short.toInt() and 0xFFFF
        val accX = buf.short.toInt()
        val accY = buf.short.toInt()
        val accZ = buf.short.toInt()
        val battMv = buf.short.toInt() and 0xFFFF

        return RuuviTag(
            mac = mac,
            dataFormat = 3,
            rssi = rssi,
            temperature = tempInt + tempFrac / 100.0,
            humidity = humRaw * 0.5,
            pressure = (presRaw + 50000).toDouble(),
            accelX = accX,
            accelY = accY,
            accelZ = accZ,
            batteryVoltage = battMv / 1000.0,
            txPower = null,
            movementCounter = null,
            sequenceNumber = null
        )
    }
}
