package com.ez2bg.anotherthread

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun isWebPlatform(): Boolean = false

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
