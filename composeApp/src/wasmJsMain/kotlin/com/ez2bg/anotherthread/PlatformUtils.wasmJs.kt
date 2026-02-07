package com.ez2bg.anotherthread

// Use js() function for Wasm JS interop
private fun dateNow(): Double = js("Date.now()")

actual fun isWebPlatform(): Boolean = true

actual fun currentTimeMillis(): Long = dateNow().toLong()

// External function for clipboard access
@JsFun("(text) => { navigator.clipboard.writeText(text); return true; }")
private external fun writeToClipboard(text: String): Boolean

actual fun copyToClipboard(text: String): Boolean {
    return try {
        writeToClipboard(text)
    } catch (e: Exception) {
        false
    }
}
