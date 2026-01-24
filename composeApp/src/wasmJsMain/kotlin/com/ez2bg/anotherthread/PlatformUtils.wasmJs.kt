package com.ez2bg.anotherthread

// Use js() function for Wasm JS interop
private fun dateNow(): Double = js("Date.now()")

actual fun isWebPlatform(): Boolean = true

actual fun currentTimeMillis(): Long = dateNow().toLong()
