package com.ez2bg.anotherthread

import kotlin.js.JsAny

@JsFun("() => window.location.hostname")
private external fun getHostname(): JsAny

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun developmentBaseUrl(): String {
    val hostname = getHostname().toString()
    return "http://$hostname:8080"
}