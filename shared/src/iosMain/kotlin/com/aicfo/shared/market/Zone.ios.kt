package com.aicfo.shared.market

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName

internal actual fun zoneOffsetMillis(epochMs: Long, timeZoneId: String): Int {
    val zone = NSTimeZone.timeZoneWithName(timeZoneId)
        ?: throw IllegalArgumentException("Unknown time zone $timeZoneId")
    val date = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)
    return (zone.secondsFromGMTForDate(date) * 1000L).toInt()
}
