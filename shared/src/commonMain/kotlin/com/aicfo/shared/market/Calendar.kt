package com.aicfo.shared.market

/**
 * Civil calendar in a market timezone. "This month" is the year-month of
 * [epochMs] in that zone, not the device's default zone.
 */
object MarketCalendar {
    fun monthKey(epochMs: Long, timeZoneId: String): String {
        val (year, month) = yearMonth(epochMs, timeZoneId)
        return "%04d-%02d".format(year, month)
    }

    fun isMonth(epochMs: Long, timeZoneId: String, year: Int, month: Int): Boolean {
        val found = yearMonth(epochMs, timeZoneId)
        return found.first == year && found.second == month
    }

    fun yearMonth(epochMs: Long, timeZoneId: String): Pair<Int, Int> {
        val offset = zoneOffsetMillis(epochMs, timeZoneId)
        val local = epochMs + offset
        val days = floorDiv(local, DAY_MS)
        val (year, month, _) = civilFromUnixDays(days)
        return year to month
    }

    private const val DAY_MS: Long = 86_400_000L

    private fun floorDiv(value: Long, divisor: Long): Long {
        var q = value / divisor
        val r = value % divisor
        if (r != 0L && ((value < 0) != (divisor < 0))) q -= 1
        return q
    }

    /** Howard Hinnant civil_from_days. Unix day 0 is 1970-01-01. */
    private fun civilFromUnixDays(z: Long): Triple<Int, Int, Int> {
        var days = z + 719468
        val era = if (days >= 0) days / 146097 else (days - 146096) / 146097
        val doe = (days - era * 146097).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        var year = yoe + era.toInt() * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = doy - (153 * mp + 2) / 5 + 1
        val month = if (mp < 10) mp + 3 else mp - 9
        if (month <= 2) year += 1
        return Triple(year, month, day)
    }
}

internal expect fun zoneOffsetMillis(epochMs: Long, timeZoneId: String): Int
