package com.ez2bg.anotherthread

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun developmentBaseUrl(): String = "http://192.168.1.239:8080"