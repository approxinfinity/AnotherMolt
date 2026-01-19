package com.ez2bg.anotherthread.platform

import java.io.File

actual fun readFileBytes(path: String): FileReadResult? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        FileReadResult(
            bytes = file.readBytes(),
            filename = file.name,
            extension = file.extension.lowercase()
        )
    } catch (e: Exception) {
        null
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
