package com.ez2bg.anotherthread

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIPasteboard

actual fun isWebPlatform(): Boolean = false

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun copyToClipboard(text: String): Boolean {
    return try {
        UIPasteboard.generalPasteboard.string = text
        true
    } catch (e: Exception) {
        false
    }
}
