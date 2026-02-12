package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.DatabaseConfig
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.fail

/**
 * Base class for application tests providing common setup and utilities.
 */
abstract class BaseApplicationTest {

    companion object {
        init {
            // Set test database path via system property before any tests run
            System.setProperty("TEST_DB_PATH", TestDatabaseConfig.getDbPath())
        }
    }

    @BeforeTest
    fun setup() {
        TestDatabaseConfig.init()
        DatabaseConfig.clearAllTables()
    }

    /**
     * Creates a test client with JSON content negotiation configured.
     */
    protected fun ApplicationTestBuilder.jsonClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    /**
     * Extracts an ID from a JSON response body using regex.
     */
    protected fun extractId(body: String): String {
        val idMatch = Regex(""""id":"([^"]+)"""").find(body)
        return idMatch?.groupValues?.get(1) ?: fail("Could not extract ID from response")
    }

    /**
     * Creates a test user and returns the user ID.
     */
    protected suspend fun HttpClient.createTestUser(uniqueSuffix: String = System.currentTimeMillis().toString()): String {
        val response = post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"testuser_$uniqueSuffix","password":"testpass123"}""")
        }
        return extractId(response.bodyAsText())
    }

    /**
     * Creates a test location and returns the location ID.
     */
    protected suspend fun HttpClient.createTestLocation(
        name: String = "Test Location",
        desc: String = "A test location"
    ): String {
        val response = post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","desc":"$desc","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        return extractId(response.bodyAsText())
    }

    /**
     * Creates a test creature and returns the creature ID.
     */
    protected suspend fun HttpClient.createTestCreature(
        name: String = "Test Creature",
        desc: String = "A test creature"
    ): String {
        val response = post("/creatures") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","desc":"$desc","itemIds":[],"featureIds":[]}""")
        }
        return extractId(response.bodyAsText())
    }

    /**
     * Creates a test item and returns the item ID.
     */
    protected suspend fun HttpClient.createTestItem(
        name: String = "Test Item",
        desc: String = "A test item"
    ): String {
        val response = post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","desc":"$desc","featureIds":[]}""")
        }
        return extractId(response.bodyAsText())
    }
}
