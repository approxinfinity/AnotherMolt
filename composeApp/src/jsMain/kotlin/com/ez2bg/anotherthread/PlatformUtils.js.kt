package com.ez2bg.anotherthread

import kotlin.js.Date
import kotlinx.browser.window

actual fun isWebPlatform(): Boolean = true

actual fun currentTimeMillis(): Long = Date.now().toLong()

actual fun copyToClipboard(text: String): Boolean {
    return try {
        window.navigator.clipboard.writeText(text)
        true
    } catch (e: Exception) {
        false
    }
}
