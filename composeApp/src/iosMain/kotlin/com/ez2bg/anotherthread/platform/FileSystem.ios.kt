package com.ez2bg.anotherthread.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun readFileBytes(path: String): FileReadResult? {
    return try {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path)) return null

        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }

        val filename = path.substringAfterLast("/")
        val extension = filename.substringAfterLast(".", "").lowercase()

        FileReadResult(
            bytes = bytes,
            filename = filename,
            extension = extension
        )
    } catch (e: Exception) {
        null
    }
}

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
