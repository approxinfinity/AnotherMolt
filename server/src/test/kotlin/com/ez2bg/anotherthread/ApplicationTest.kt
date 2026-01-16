package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.DatabaseConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.*

class ApplicationTest {

    companion object {
        private var initialized = false
        private val testDbFile = File.createTempFile("app_test_db", ".db").also { it.deleteOnExit() }

        init {
            // Set test database path via system property before any tests run
            System.setProperty("TEST_DB_PATH", testDbFile.absolutePath)
        }
    }

    @BeforeTest
    fun setup() {
        // Initialize database once, clear before each test
        if (!initialized) {
            DatabaseConfig.init(testDbFile.absolutePath)
            initialized = true
        }
        DatabaseConfig.clearAllTables()
    }

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ktor: ${Greeting().greet()}", response.bodyAsText())
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun testGetLocationsEmpty() = testApplication {
        application {
            module()
        }
        val response = client.get("/locations")
        assertEquals(HttpStatusCode.OK, response.status)
        // Response should be a JSON array (possibly empty or with existing data)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Test
    fun testGetCreaturesEmpty() = testApplication {
        application {
            module()
        }
        val response = client.get("/creatures")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Test
    fun testGetItemsEmpty() = testApplication {
        application {
            module()
        }
        val response = client.get("/items")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Test
    fun testCreateLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test Location","desc":"A test location description","itemIds":[],"creatureIds":[],"exitIds":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("Test Location"))
        assertTrue(body.contains("A test location description"))
    }

    @Test
    fun testCreateCreature() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/creatures") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test Creature","desc":"A fearsome beast","itemIds":[],"featureIds":["claws","fangs"]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("Test Creature"))
        assertTrue(body.contains("A fearsome beast"))
    }

    @Test
    fun testCreateItem() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Magic Sword","desc":"A glowing blade","featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("Magic Sword"))
        assertTrue(body.contains("A glowing blade"))
    }

    @Test
    fun testUpdateLocationNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/locations/nonexistent-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated Location","desc":"Updated description","itemIds":[],"creatureIds":[],"exitIds":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testImageGenerationStatusEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/image-generation/status")
        assertEquals(HttpStatusCode.OK, response.status)
        // Should return JSON with "available" key
        assertTrue(response.bodyAsText().contains("available"))
    }

    // ========== Content Generation Endpoint Tests ==========

    @Test
    fun testContentGenerationStatusEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/generate/status")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("available"))
    }

    // ========== User Authentication Tests ==========

    @Test
    fun testRegisterWithValidCredentials() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "testuser_${System.currentTimeMillis()}"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"))
        assertTrue(body.contains("Registration successful"))
        assertTrue(body.contains(uniqueName))
    }

    @Test
    fun testRegisterWithBlankName() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":false"))
        assertTrue(body.contains("Name and password are required"))
    }

    @Test
    fun testRegisterWithShortPassword() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "shortpw_${System.currentTimeMillis()}"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":false"))
        assertTrue(body.contains("Password must be at least 4 characters"))
    }

    @Test
    fun testRegisterDuplicateUsername() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "duplicate_${System.currentTimeMillis()}"

        // First registration should succeed
        val response1 = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        assertEquals(HttpStatusCode.Created, response1.status)

        // Second registration with same name should fail
        val response2 = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"differentpass"}""")
        }
        assertEquals(HttpStatusCode.Conflict, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.contains("Username already exists"))
    }

    @Test
    fun testLoginWithValidCredentials() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "logintest_${System.currentTimeMillis()}"

        // Register first
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        // Then login
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"))
        assertTrue(body.contains("Login successful"))
        assertTrue(body.contains(uniqueName))
    }

    @Test
    fun testLoginWithInvalidPassword() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "badpwlogin_${System.currentTimeMillis()}"

        // Register first
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"correctpass"}""")
        }

        // Login with wrong password
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"wrongpass"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":false"))
        assertTrue(body.contains("Invalid username or password"))
    }

    @Test
    fun testLoginWithNonexistentUser() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"nonexistentuser_${System.currentTimeMillis()}","password":"anypass"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ========== User CRUD Tests ==========

    @Test
    fun testGetUserById() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "getuser_${System.currentTimeMillis()}"

        // Register to create a user
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        // Extract user ID from response
        val registerBody = registerResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(registerBody)
        val userId = idMatch?.groupValues?.get(1) ?: fail("Could not extract user ID")

        // Fetch user by ID
        val response = client.get("/users/$userId")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(uniqueName))
        assertTrue(body.contains(userId))
    }

    @Test
    fun testGetUserByIdNotFound() = testApplication {
        application {
            module()
        }
        val response = client.get("/users/nonexistent-user-id-12345")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUpdateUser() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "updateuser_${System.currentTimeMillis()}"

        // Register to create a user
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        val registerBody = registerResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(registerBody)
        val userId = idMatch?.groupValues?.get(1) ?: fail("Could not extract user ID")

        // Update user
        val response = client.put("/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody("""{"desc":"A brave adventurer","itemIds":["sword","shield"],"featureIds":["strong","brave"]}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("A brave adventurer"))
        assertTrue(body.contains("sword"))
        assertTrue(body.contains("shield"))
    }

    @Test
    fun testUpdateUserNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/users/nonexistent-user-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"desc":"test","itemIds":[],"featureIds":[]}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUpdateUserLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val uniqueName = "locuser_${System.currentTimeMillis()}"

        // Register to create a user
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        val registerBody = registerResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(registerBody)
        val userId = idMatch?.groupValues?.get(1) ?: fail("Could not extract user ID")

        // Update user location
        val response = client.put("/users/$userId/location") {
            contentType(ContentType.Application.Json)
            setBody("""{"locationId":"some-location-id"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testGetActiveUsersAtLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("/users/at-location/test-location-id")
        assertEquals(HttpStatusCode.OK, response.status)
        // Should return an array (possibly empty)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    // ========== Creature Update Test ==========

    @Test
    fun testUpdateCreatureNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/creatures/nonexistent-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Ghost Creature","desc":"Doesn't exist","itemIds":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ========== Item Update Test ==========

    @Test
    fun testUpdateItemNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/items/nonexistent-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Ghost Item","desc":"Doesn't exist","featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
