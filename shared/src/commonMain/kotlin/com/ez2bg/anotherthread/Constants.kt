package com.ez2bg.anotherthread

const val SERVER_PORT = 8080

val API_BASE_URL: String get() = AppConfig.api.baseUrl
val API_TIMEOUT_MS: Long get() = AppConfig.api.timeoutMs