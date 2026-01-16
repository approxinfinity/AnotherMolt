package com.ez2bg.anotherthread

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform