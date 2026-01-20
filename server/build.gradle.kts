plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    application
}

group = "com.ez2bg.anotherthread"
version = "1.0.0"
application {
    mainClass.set("com.ez2bg.anotherthread.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serializationJson)

    // HTTP Client for Stable Diffusion API
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.sqlite.jdbc)

    // Password hashing
    implementation(libs.bcrypt)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

// Pass system property to control integration test execution
// Run integration tests with: ./gradlew test -PrunIntegrationTests=true
tasks.withType<Test> {
    val runIntegrationTests = project.findProperty("runIntegrationTests")?.toString()?.toBoolean() ?: false
    systemProperty("runIntegrationTests", runIntegrationTests.toString())
}