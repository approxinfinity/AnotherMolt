package com.ez2bg.anotherthread

object AppConfig {
    val environment: Environment = Environment.DEVELOPMENT

    val api = ApiConfig(
        baseUrl = when (environment) {
            Environment.DEVELOPMENT -> developmentBaseUrl()
            Environment.STAGING -> "https://staging-api.example.com"
            Environment.PRODUCTION -> "https://api.example.com"
        },
        version = "v1",
        timeoutMs = 30_000L
    )

    val features = FeatureFlags(
        enableAnalytics = environment == Environment.PRODUCTION,
        enableDebugLogging = environment == Environment.DEVELOPMENT
    )
}

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

data class ApiConfig(
    val baseUrl: String,
    val version: String,
    val timeoutMs: Long
) {
    val apiUrl: String get() = "$baseUrl/$version"
}

data class FeatureFlags(
    val enableAnalytics: Boolean,
    val enableDebugLogging: Boolean
)

expect fun developmentBaseUrl(): String
