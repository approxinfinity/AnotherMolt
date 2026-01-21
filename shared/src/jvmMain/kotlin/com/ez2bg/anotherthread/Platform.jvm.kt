package com.ez2bg.anotherthread

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun developmentBaseUrl(): String = "http://localhost:8080"

// No-op on JVM Desktop - URL cache busting is web-only
actual fun updateUrlWithCacheBuster(view: String) {}

// No URL params on JVM Desktop
actual fun getInitialViewParam(): String? = null
actual fun getLocationParam(): String? = null