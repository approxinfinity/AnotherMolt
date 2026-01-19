package com.ez2bg.anotherthread

import kotlin.js.JsAny

@JsFun("() => window.location.hostname")
private external fun getHostname(): JsAny

@JsFun("() => window.APP_CONFIG?.tunnelBackendUrl || null")
private external fun getTunnelBackendUrl(): JsAny?

@JsFun("() => window.APP_CONFIG?.localBackendPort || 8081")
private external fun getLocalBackendPort(): Int

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
