package com.ez2bg.anotherthread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual fun isWebPlatform(): Boolean = false

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// Note: Android clipboard requires a Context. This is a placeholder that returns false.
// For proper implementation, use a Composable with LocalContext or pass context explicitly.
actual fun copyToClipboard(text: String): Boolean {
    // Android clipboard operations require Context which isn't available here.
    // The UI layer should handle clipboard operations directly using LocalClipboardManager.
    return false
}
