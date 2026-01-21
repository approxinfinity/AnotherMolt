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

/**
 * Get the initial view parameter from the URL.
 * On web platforms, this reads the 'v' query parameter.
 * On other platforms, this returns null.
 */
expect fun getInitialViewParam(): String?

/**
 * Get the location parameter from the URL.
 * On web platforms, this reads the 'loc' query parameter.
 * On other platforms, this returns null.
 */
expect fun getLocationParam(): String?