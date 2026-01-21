package com.ez2bg.anotherthread

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun developmentBaseUrl(): String = "http://localhost:8080"

// No-op on iOS - URL cache busting is web-only
actual fun updateUrlWithCacheBuster(view: String) {}