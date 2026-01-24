package com.ez2bg.anotherthread

/**
 * Check if we're running on a web platform (JS or Wasm).
 * Used to determine auth behavior:
 * - Web: Uses HttpOnly cookies for session (auto-sent by browser)
 * - Native: Uses Authorization header with stored token
 */
expect fun isWebPlatform(): Boolean

/**
 * Get current time in milliseconds since epoch.
 */
expect fun currentTimeMillis(): Long
