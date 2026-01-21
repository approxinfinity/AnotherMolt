package com.ez2bg.anotherthread

import kotlin.js.JsAny
import kotlin.random.Random

@JsFun("() => window.location.hostname")
private external fun getHostname(): JsAny

@JsFun("() => window.APP_CONFIG?.tunnelBackendUrl || null")
private external fun getTunnelBackendUrl(): JsAny?

@JsFun("() => window.APP_CONFIG?.localBackendPort || 8081")
private external fun getLocalBackendPort(): Int

@JsFun("(url) => window.history.replaceState(null, '', url)")
private external fun replaceUrl(url: String)

@JsFun("() => new URLSearchParams(window.location.search).get('v') || null")
private external fun getUrlViewParam(): JsAny?

@JsFun("() => new URLSearchParams(window.location.search).get('loc') || null")
private external fun getUrlLocationParam(): JsAny?

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun developmentBaseUrl(): String {
    val hostname = getHostname().toString()
    val tunnelUrl = getTunnelBackendUrl()?.toString()
    val localPort = getLocalBackendPort()

    // If on ez2bgood.com, use api subdomain for backend
    return if (hostname == "anotherthread.ez2bgood.com") {
        "https://api.ez2bgood.com"
    } else if (tunnelUrl != null && tunnelUrl != "null" && hostname.contains("trycloudflare.com")) {
        tunnelUrl
    } else {
        "http://$hostname:$localPort"
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
    replaceUrl(newUrl)
}

actual fun getInitialViewParam(): String? {
    val value = getUrlViewParam()?.toString()
    return value?.takeIf { it.isNotEmpty() && it != "null" }
}

actual fun getLocationParam(): String? {
    val value = getUrlLocationParam()?.toString()
    return value?.takeIf { it.isNotEmpty() && it != "null" }
}
