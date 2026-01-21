package com.ez2bg.anotherthread

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Update the browser URL with a cache-busting parameter.
 * On web platforms, this updates the URL query string.
 * On other platforms, this is a no-op.
 */
expect fun updateUrlWithCacheBuster(view: String = "")