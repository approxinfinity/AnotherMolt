package com.ez2bg.anotherthread

import kotlin.js.Date
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

actual fun isWebPlatform(): Boolean = true

actual fun currentTimeMillis(): Long = Date.now().toLong()

actual fun copyToClipboard(text: String): Boolean {
    return try {
        // Use input element - reportedly works better in Safari than textarea
        val input = document.createElement("input") as HTMLInputElement
        input.value = text
        input.style.position = "fixed"
        input.style.left = "0"
        input.style.top = "0"
        input.style.opacity = "0"

        document.body?.appendChild(input)
        input.focus()
        input.select()
        input.setSelectionRange(0, text.length)

        val success = document.execCommand("copy")
        document.body?.removeChild(input)
        success
    } catch (e: Exception) {
        false
    }
}
