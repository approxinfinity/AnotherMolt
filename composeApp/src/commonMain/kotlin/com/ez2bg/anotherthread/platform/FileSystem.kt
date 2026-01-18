package com.ez2bg.anotherthread.platform

/**
 * Platform-specific file reading result
 */
data class FileReadResult(
    val bytes: ByteArray,
    val filename: String,
    val extension: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FileReadResult
        return bytes.contentEquals(other.bytes) && filename == other.filename && extension == other.extension
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}

/**
 * Read a file from the local filesystem.
 * Returns null if the file doesn't exist or can't be read.
 */
expect fun readFileBytes(path: String): FileReadResult?
