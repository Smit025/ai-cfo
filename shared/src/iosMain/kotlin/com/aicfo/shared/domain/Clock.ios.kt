package com.aicfo.shared.domain

import platform.Foundation.NSDate

internal actual fun platformNowMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
