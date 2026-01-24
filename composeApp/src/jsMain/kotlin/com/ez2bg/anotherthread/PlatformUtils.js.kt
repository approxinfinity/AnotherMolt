package com.ez2bg.anotherthread

import kotlin.js.Date

actual fun isWebPlatform(): Boolean = true

actual fun currentTimeMillis(): Long = Date.now().toLong()
