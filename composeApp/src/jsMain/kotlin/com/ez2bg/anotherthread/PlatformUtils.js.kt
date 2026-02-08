package com.ez2bg.anotherthread

import kotlin.js.Date
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

actual fun isWebPlatform(): Boolean = true

actual fun currentTimeMillis(): Long = Date.now().toLong()

actual fun copyToClipboard(text: String): Boolean {
    return try {
        // Use the older execCommand approach which is synchronous and more reliable
        val textArea = document.createElement("textarea") as HTMLTextAreaElement
        textArea.value = text
        textArea.style.position = "fixed"
        textArea.style.left = "-9999px"
        document.body?.appendChild(textArea)
        textArea.select()
        val success = document.execCommand("copy")
        document.body?.removeChild(textArea)
        success
    } catch (e: Exception) {
        console.log("Clipboard copy failed: ${e.message}")
        false
    }
}
