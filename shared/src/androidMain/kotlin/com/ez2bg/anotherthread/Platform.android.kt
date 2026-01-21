package com.ez2bg.anotherthread

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun developmentBaseUrl(): String = "http://10.0.2.2:8080"

// No-op on Android - URL cache busting is web-only
actual fun updateUrlWithCacheBuster(view: String) {}

// No URL params on Android
actual fun getInitialViewParam(): String? = null
actual fun getLocationParam(): String? = null