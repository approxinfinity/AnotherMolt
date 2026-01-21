package com.ez2bg.anotherthread

import kotlinx.browser.window
import kotlin.random.Random

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

private fun generateCacheBusterId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

actual fun updateUrlWithCacheBuster(view: String) {
    val cacheBuster = generateCacheBusterId()
    val newUrl = if (view.isNotEmpty()) {
        "?v=$view&_=$cacheBuster"
    } else {
        "?_=$cacheBuster"
    }
    window.history.replaceState(null, "", newUrl)
}