package com.ez2bg.anotherthread.platform

import kotlin.js.Date

// JS platform doesn't have direct filesystem access
actual fun readFileBytes(path: String): FileReadResult? {
    return null
}

actual fun currentTimeMillis(): Long = Date.now().toLong()
