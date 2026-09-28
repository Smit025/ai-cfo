package com.aicfo.shared.market

import java.time.Instant
import java.time.ZoneId

internal actual fun zoneOffsetMillis(epochMs: Long, timeZoneId: String): Int {
    val zone = try {
        ZoneId.of(timeZoneId)
    } catch (error: Exception) {
        throw IllegalArgumentException("Unknown time zone $timeZoneId", error)
    }
    return zone.rules.getOffset(Instant.ofEpochMilli(epochMs)).totalSeconds * 1000
}
