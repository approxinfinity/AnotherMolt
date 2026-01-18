package com.ez2bg.anotherthread.platform

// JS platform doesn't have direct filesystem access
actual fun readFileBytes(path: String): FileReadResult? {
    return null
}
