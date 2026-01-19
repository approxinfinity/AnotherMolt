package com.ez2bg.anotherthread

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun developmentBaseUrl(): String {
    val hostname = window.location.hostname
    val protocol = window.location.protocol
    // If accessing via Cloudflare tunnel, use api subdomain for backend
    return if (hostname == "anotherthread.ez2bgood.com") {
        "$protocol//api.ez2bgood.com"
    } else if (hostname.endsWith(".trycloudflare.com")) {
        "https://vegetarian-distance-fires-chip.trycloudflare.com"
    } else {
        "http://$hostname:8081"
    }
}