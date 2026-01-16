package com.ez2bg.anotherthread

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun developmentBaseUrl(): String = "http://localhost:8080"