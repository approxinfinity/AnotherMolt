package com.ez2bg.anotherthread

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun isWebPlatform(): Boolean = false

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun copyToClipboard(text: String): Boolean {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
        true
    } catch (e: Exception) {
        false
    }
}
