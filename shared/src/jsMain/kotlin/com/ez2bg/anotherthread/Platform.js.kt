package com.ez2bg.anotherthread

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun developmentBaseUrl(): String {
    val hostname = window.location.hostname
    return "http://$hostname:8080"
}