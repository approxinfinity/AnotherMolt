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
            setBody("""{"name":"Test Location","desc":"A test location description","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
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
            setBody("""{"name":"Updated Location","desc":"Updated description","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
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

    // ========== Location Lock Tests ==========

    @Test
    fun testLockLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create a location first
        val createResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Lockable Location","desc":"Test","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val locationId = idMatch?.groupValues?.get(1) ?: fail("Could not extract location ID")

        // Lock the location
        val lockResponse = client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        val lockBody = lockResponse.bodyAsText()
        assertTrue(lockBody.contains("\"lockedBy\":\"user-123\""))

        // Unlock the location (toggle)
        val unlockResponse = client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.OK, unlockResponse.status)
        val unlockBody = unlockResponse.bodyAsText()
        assertTrue(unlockBody.contains("\"lockedBy\":null"))
    }

    @Test
    fun testLockLocationNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/locations/nonexistent-id/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ========== Creature Lock Tests ==========

    @Test
    fun testLockCreature() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create a creature first
        val createResponse = client.post("/creatures") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Lockable Creature","desc":"Test creature","itemIds":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val creatureId = idMatch?.groupValues?.get(1) ?: fail("Could not extract creature ID")

        // Lock the creature
        val lockResponse = client.put("/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"admin-user"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        val lockBody = lockResponse.bodyAsText()
        assertTrue(lockBody.contains("\"lockedBy\":\"admin-user\""))

        // Unlock the creature (toggle by same user)
        val unlockResponse = client.put("/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"admin-user"}""")
        }
        assertEquals(HttpStatusCode.OK, unlockResponse.status)
        val unlockBody = unlockResponse.bodyAsText()
        assertTrue(unlockBody.contains("\"lockedBy\":null"))
    }

    @Test
    fun testLockCreatureByDifferentUser() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create a creature
        val createResponse = client.post("/creatures") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Contested Creature","desc":"Test","itemIds":[],"featureIds":[]}""")
        }
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val creatureId = idMatch?.groupValues?.get(1) ?: fail("Could not extract creature ID")

        // Lock by user A
        val lockResponse = client.put("/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-A"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        assertTrue(lockResponse.bodyAsText().contains("\"lockedBy\":\"user-A\""))

        // User B attempts to lock - should change the lock to user B
        val lockResponse2 = client.put("/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-B"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse2.status)
        assertTrue(lockResponse2.bodyAsText().contains("\"lockedBy\":\"user-B\""))
    }

    @Test
    fun testLockCreatureNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/creatures/nonexistent-id/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ========== Item Lock Tests ==========

    @Test
    fun testLockItem() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create an item first
        val createResponse = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Lockable Item","desc":"A precious artifact","featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val itemId = idMatch?.groupValues?.get(1) ?: fail("Could not extract item ID")

        // Lock the item
        val lockResponse = client.put("/items/$itemId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"curator-001"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        val lockBody = lockResponse.bodyAsText()
        assertTrue(lockBody.contains("\"lockedBy\":\"curator-001\""))

        // Unlock the item (toggle by same user)
        val unlockResponse = client.put("/items/$itemId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"curator-001"}""")
        }
        assertEquals(HttpStatusCode.OK, unlockResponse.status)
        val unlockBody = unlockResponse.bodyAsText()
        assertTrue(unlockBody.contains("\"lockedBy\":null"))
    }

    @Test
    fun testLockItemNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/items/nonexistent-id/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testVerifyLockedFieldsReturnedInGetAll() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create and lock a location
        val createResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Locked Test Location","desc":"Test","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val locationId = idMatch?.groupValues?.get(1) ?: fail("Could not extract location ID")

        // Lock it
        client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"locker-user"}""")
        }

        // Verify lockedBy is returned in GET /locations
        val getAllResponse = client.get("/locations")
        assertEquals(HttpStatusCode.OK, getAllResponse.status)
        val allBody = getAllResponse.bodyAsText()
        assertTrue(allBody.contains("\"lockedBy\""))
        assertTrue(allBody.contains("locker-user"))
    }

    // ========== Delete Tests ==========

    @Test
    fun testDeleteLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create a location
        val createResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Deletable Location","desc":"Test","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val locationId = idMatch?.groupValues?.get(1) ?: fail("Could not extract location ID")

        // Delete the location
        val deleteResponse = client.delete("/locations/$locationId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify it's gone
        val getResponse = client.get("/locations")
        val locations = getResponse.bodyAsText()
        assertFalse(locations.contains(locationId))
    }

    @Test
    fun testDeleteLocationNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/locations/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteCreature() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create a creature
        val createResponse = client.post("/creatures") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Deletable Creature","desc":"Test","itemIds":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val creatureId = idMatch?.groupValues?.get(1) ?: fail("Could not extract creature ID")

        // Delete the creature
        val deleteResponse = client.delete("/creatures/$creatureId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify it's gone
        val getResponse = client.get("/creatures")
        val creatures = getResponse.bodyAsText()
        assertFalse(creatures.contains(creatureId))
    }

    @Test
    fun testDeleteCreatureNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/creatures/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteItem() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create an item
        val createResponse = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Deletable Item","desc":"Test","featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        val idMatch = Regex(""""id":"([^"]+)"""").find(createBody)
        val itemId = idMatch?.groupValues?.get(1) ?: fail("Could not extract item ID")

        // Delete the item
        val deleteResponse = client.delete("/items/$itemId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify it's gone
        val getResponse = client.get("/items")
        val items = getResponse.bodyAsText()
        assertFalse(items.contains(itemId))
    }

    @Test
    fun testDeleteItemNotFound() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/items/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteCreatureRemovesFromLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create a creature
        val creatureResponse = client.post("/creatures") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Creature to Delete","desc":"Test","itemIds":[],"featureIds":[]}""")
        }
        val creatureBody = creatureResponse.bodyAsText()
        val creatureIdMatch = Regex(""""id":"([^"]+)"""").find(creatureBody)
        val creatureId = creatureIdMatch?.groupValues?.get(1) ?: fail("Could not extract creature ID")

        // Create a location with that creature
        val locationResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location with Creature","desc":"Test","itemIds":[],"creatureIds":["$creatureId"],"exits":[],"featureIds":[]}""")
        }
        val locationBody = locationResponse.bodyAsText()
        val locationIdMatch = Regex(""""id":"([^"]+)"""").find(locationBody)
        val locationId = locationIdMatch?.groupValues?.get(1) ?: fail("Could not extract location ID")

        // Verify creature is in location
        val getLocationBefore = client.get("/locations")
        val beforeBody = getLocationBefore.bodyAsText()
        assertTrue(beforeBody.contains(creatureId))

        // Delete the creature
        val deleteResponse = client.delete("/creatures/$creatureId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify creature ID is removed from location's creatureIds
        val getLocationAfter = client.get("/locations")
        val afterBody = getLocationAfter.bodyAsText()
        // The creature ID should no longer appear in creatureIds array
        // Find the location and check its creatureIds
        assertFalse(afterBody.contains(""""creatureIds":["$creatureId"]"""))
    }

    @Test
    fun testDeleteItemRemovesFromLocation() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create an item
        val itemResponse = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Item to Delete","desc":"Test","featureIds":[]}""")
        }
        val itemBody = itemResponse.bodyAsText()
        val itemIdMatch = Regex(""""id":"([^"]+)"""").find(itemBody)
        val itemId = itemIdMatch?.groupValues?.get(1) ?: fail("Could not extract item ID")

        // Create a location with that item
        val locationResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location with Item","desc":"Test","itemIds":["$itemId"],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        val locationBody = locationResponse.bodyAsText()

        // Verify item is in location
        val getLocationBefore = client.get("/locations")
        val beforeBody = getLocationBefore.bodyAsText()
        assertTrue(beforeBody.contains(itemId))

        // Delete the item
        val deleteResponse = client.delete("/items/$itemId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify item ID is removed from location's itemIds
        val getLocationAfter = client.get("/locations")
        val afterBody = getLocationAfter.bodyAsText()
        assertFalse(afterBody.contains(""""itemIds":["$itemId"]"""))
    }

    @Test
    fun testDeleteLocationRemovesFromExits() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create two locations
        val location1Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location 1","desc":"Test","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        val location1Body = location1Response.bodyAsText()
        val location1IdMatch = Regex(""""id":"([^"]+)"""").find(location1Body)
        val location1Id = location1IdMatch?.groupValues?.get(1) ?: fail("Could not extract location 1 ID")

        // Create location 2 with location 1 as an exit (with NORTH direction)
        val location2Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location 2","desc":"Test","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$location1Id","direction":"NORTH"}],"featureIds":[]}""")
        }
        val location2Body = location2Response.bodyAsText()
        val location2IdMatch = Regex(""""id":"([^"]+)"""").find(location2Body)
        val location2Id = location2IdMatch?.groupValues?.get(1) ?: fail("Could not extract location 2 ID")

        // Verify location 1 is in location 2's exits
        val getLocationsBefore = client.get("/locations")
        val beforeBody = getLocationsBefore.bodyAsText()
        assertTrue(beforeBody.contains(location1Id))

        // Delete location 1
        val deleteResponse = client.delete("/locations/$location1Id")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify location 1 ID is removed from location 2's exits
        val getLocationsAfter = client.get("/locations")
        val afterBody = getLocationsAfter.bodyAsText()
        assertFalse(afterBody.contains(""""locationId":"$location1Id""""))
        // Location 2 should still exist
        assertTrue(afterBody.contains(location2Id))
    }

    @Test
    fun testExitDirectionsAreSavedAndRetrieved() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create location 1 (destination)
        val loc1Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Town Square","desc":"The central square","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        val loc1Body = loc1Response.bodyAsText()
        val loc1IdMatch = Regex(""""id":"([^"]+)"""").find(loc1Body)
        val loc1Id = loc1IdMatch?.groupValues?.get(1) ?: fail("Could not extract location 1 ID")

        // Create location 2 with exit to location 1 going NORTH
        val loc2Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"South Gate","desc":"The southern entrance","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$loc1Id","direction":"NORTH"}],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, loc2Response.status)
        val loc2Body = loc2Response.bodyAsText()
        val loc2IdMatch = Regex(""""id":"([^"]+)"""").find(loc2Body)
        val loc2Id = loc2IdMatch?.groupValues?.get(1) ?: fail("Could not extract location 2 ID")

        // Retrieve locations and verify the exit direction is preserved
        val getResponse = client.get("/locations")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val body = getResponse.bodyAsText()

        // Verify the exit with direction is in the response
        assertTrue(body.contains(""""locationId":"$loc1Id""""))
        assertTrue(body.contains(""""direction":"NORTH""""))
    }

    @Test
    fun testUpdateExitDirectionChangesLayout() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create two locations
        val loc1Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Castle","desc":"A mighty castle","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        val loc1Body = loc1Response.bodyAsText()
        val loc1IdMatch = Regex(""""id":"([^"]+)"""").find(loc1Body)
        val loc1Id = loc1IdMatch?.groupValues?.get(1) ?: fail("Could not extract location 1 ID")

        val loc2Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Village","desc":"A peaceful village","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$loc1Id","direction":"EAST"}],"featureIds":[]}""")
        }
        val loc2Body = loc2Response.bodyAsText()
        val loc2IdMatch = Regex(""""id":"([^"]+)"""").find(loc2Body)
        val loc2Id = loc2IdMatch?.groupValues?.get(1) ?: fail("Could not extract location 2 ID")

        // Verify initial direction is EAST
        val getBeforeResponse = client.get("/locations")
        var body = getBeforeResponse.bodyAsText()
        assertTrue(body.contains(""""direction":"EAST""""))

        // Update location 2 to change exit direction to WEST
        val updateResponse = client.put("/locations/$loc2Id") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Village","desc":"A peaceful village","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$loc1Id","direction":"WEST"}],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify direction changed to WEST
        val getAfterResponse = client.get("/locations")
        body = getAfterResponse.bodyAsText()
        // Should contain WEST now, and ideally not EAST for this specific exit
        assertTrue(body.contains(""""direction":"WEST""""))
    }
}
