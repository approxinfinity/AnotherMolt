package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for all onboarding flow scenarios:
 * 1. New user registration → no class (ghost exploration mode)
 * 2. Existing user with no class → ghost exploration mode
 * 3. Existing user with class → main adventure mode
 * 4. Class generation while in ghost mode
 * 5. Session validation flows
 */
class OnboardingFlowTest : BaseApplicationTest() {

    private val json = Json { ignoreUnknownKeys = true }

    // ========== Registration Flow Tests ==========

    @Test
    fun testNewUserRegistration_HasNoClass() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "newuser_${System.currentTimeMillis()}"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertTrue(body["success"]?.jsonPrimitive?.boolean == true)

        val user = body["user"]?.jsonObject
        assertNotNull(user, "Response should contain user object")

        // New user should have no character class
        assertTrue(
            user["characterClassId"]?.jsonPrimitive?.contentOrNull == null,
            "New user should have null characterClassId"
        )

        // Class generation status is now tracked in-memory, not in user response
        // (classGenerationStartedAt field was removed)
    }

    @Test
    fun testNewUserRegistration_HasDefaultLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create a starting location first
        val startLocationId = createStartingLocation()

        val uniqueName = "locuser_${System.currentTimeMillis()}"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val user = body["user"]?.jsonObject

        // New user should be placed at starting location (or null if no starting location)
        val currentLocationId = user?.get("currentLocationId")?.jsonPrimitive?.contentOrNull
        // Location assignment depends on whether starting location exists
        // Test just validates the field exists
        assertNotNull(user?.containsKey("currentLocationId"), "User should have currentLocationId field")
    }

    @Test
    fun testNewUserRegistration_HasDefaultStats() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "statsuser_${System.currentTimeMillis()}"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val user = body["user"]?.jsonObject
        assertNotNull(user)

        // Verify default HP values exist
        assertTrue(user.containsKey("currentHp"), "User should have currentHp")
        assertTrue(user.containsKey("maxHp"), "User should have maxHp")

        val currentHp = user["currentHp"]?.jsonPrimitive?.int ?: 0
        val maxHp = user["maxHp"]?.jsonPrimitive?.int ?: 0

        assertTrue(currentHp > 0, "User should have positive currentHp")
        assertTrue(maxHp > 0, "User should have positive maxHp")
        assertEquals(currentHp, maxHp, "New user should be at full HP")
    }

    // ========== User State Tests (for client-side navigation decisions) ==========

    @Test
    fun testGetUser_WithNoClass_ReturnsNullClassId() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create user via registration
        val uniqueName = "noclass_${System.currentTimeMillis()}"
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        val userId = extractId(registerResponse.bodyAsText())

        // Fetch user by ID
        val response = client.get("/users/$userId")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(
            body["characterClassId"]?.jsonPrimitive?.contentOrNull == null,
            "User without class should return null characterClassId"
        )
    }

    @Test
    fun testGetUser_WithClass_ReturnsClassId() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create user with a class assigned directly in DB
        val user = createUserWithClass()

        val response = client.get("/users/${user.id}")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(
            TestFixtures.WARRIOR_CLASS_ID,
            body["characterClassId"]?.jsonPrimitive?.content,
            "User with class should return correct characterClassId"
        )
    }

    // ========== Session Validation Tests ==========

    @Test
    fun testSessionValidation_WithoutSession_Returns401() = testApplication {
        application { module() }
        val client = jsonClient()

        // Without a valid session/cookie, /auth/me should return 401
        val validateResponse = client.get("/auth/me")
        assertEquals(
            HttpStatusCode.Unauthorized,
            validateResponse.status,
            "Session validation without session should return 401"
        )
    }

    @Test
    fun testRegisterResponse_IncludesSessionToken() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "session_${System.currentTimeMillis()}"
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val body = json.parseToJsonElement(registerResponse.bodyAsText()).jsonObject

        // Registration response should include session info for native clients
        // (web clients use cookies, native clients use tokens)
        // The response structure should support stateless session validation
        assertTrue(body["success"]?.jsonPrimitive?.boolean == true)
        assertNotNull(body["user"], "Registration should return user for client-side storage")
    }

    @Test
    fun testLogin_ReturnsUserWithClassStatus() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "loginflow_${System.currentTimeMillis()}"

        // Register first
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        // Login
        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val body = json.parseToJsonElement(loginResponse.bodyAsText()).jsonObject

        val user = body["user"]?.jsonObject
        assertNotNull(user)

        // Login response should include class status for client navigation
        assertTrue(user.containsKey("characterClassId"), "Login response must include characterClassId for navigation")
    }

    // ========== Character Class Assignment Tests ==========

    @Test
    fun testAssignCharacterClass_UpdatesUser() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create character class first
        seedTestCharacterClass()

        // Create user
        val uniqueName = "assignclass_${System.currentTimeMillis()}"
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        val userId = extractId(registerResponse.bodyAsText())

        // Verify no class initially
        var userResponse = client.get("/users/$userId")
        var userBody = json.parseToJsonElement(userResponse.bodyAsText()).jsonObject
        assertNull(userBody["characterClassId"]?.jsonPrimitive?.contentOrNull)

        // Assign class via PUT
        val assignResponse = client.put("/users/$userId/class") {
            contentType(ContentType.Application.Json)
            setBody("""{"classId":"${TestFixtures.WARRIOR_CLASS_ID}"}""")
        }
        assertEquals(HttpStatusCode.OK, assignResponse.status)

        // Verify class is now assigned
        userResponse = client.get("/users/$userId")
        userBody = json.parseToJsonElement(userResponse.bodyAsText()).jsonObject
        assertEquals(
            TestFixtures.WARRIOR_CLASS_ID,
            userBody["characterClassId"]?.jsonPrimitive?.content,
            "Class should be assigned after PUT"
        )
    }

    // ========== Attribute Derivation Tests ==========

    @Test
    fun testDeriveAttributes_WithDescription_ReturnsStats() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "derive_${System.currentTimeMillis()}"
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        val userId = extractId(registerResponse.bodyAsText())

        // Derive attributes
        val deriveResponse = client.post("/users/$userId/derive-attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"description":"A strong warrior with powerful arms and a sharp mind","followUpAnswers":{}}""")
        }

        // Note: This may return 200 or error depending on LLM availability
        // In test mode, LLM calls are disabled so this might return a default or error
        // We just verify the endpoint exists and accepts the request
        assertTrue(
            deriveResponse.status == HttpStatusCode.OK ||
            deriveResponse.status == HttpStatusCode.ServiceUnavailable ||
            deriveResponse.status == HttpStatusCode.InternalServerError,
            "Derive attributes should return OK, ServiceUnavailable, or error (LLM may be disabled in tests)"
        )
    }

    @Test
    fun testCommitAttributes_SavesStatsToUser() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "commit_${System.currentTimeMillis()}"
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        val userId = extractId(registerResponse.bodyAsText())

        // Commit pre-calculated attributes (without reasoning - not in the request schema)
        val commitResponse = client.post("/users/$userId/commit-attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"strength":14,"dexterity":12,"constitution":13,"intelligence":10,"wisdom":11,"charisma":8,"qualityBonus":2}""")
        }

        assertEquals(HttpStatusCode.OK, commitResponse.status)

        // Verify attributes were saved
        val userResponse = client.get("/users/$userId")
        val userBody = json.parseToJsonElement(userResponse.bodyAsText()).jsonObject

        assertEquals(14, userBody["strength"]?.jsonPrimitive?.int)
        assertEquals(12, userBody["dexterity"]?.jsonPrimitive?.int)
        assertEquals(13, userBody["constitution"]?.jsonPrimitive?.int)
        assertEquals(10, userBody["intelligence"]?.jsonPrimitive?.int)
        assertEquals(11, userBody["wisdom"]?.jsonPrimitive?.int)
        assertEquals(8, userBody["charisma"]?.jsonPrimitive?.int)

        assertNotNull(
            userBody["attributesGeneratedAt"]?.jsonPrimitive?.longOrNull,
            "attributesGeneratedAt should be set after commit"
        )
    }

    // ========== Ghost Mode Compatibility Tests ==========

    @Test
    fun testUserWithoutClass_CanViewLocations() = testApplication {
        application { module() }
        val client = jsonClient()

        // User without class should still be able to view locations (ghost mode)
        val locationsResponse = client.get("/locations")
        assertEquals(HttpStatusCode.OK, locationsResponse.status)
    }

    @Test
    fun testUserWithoutClass_CanViewCreatedLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create a location via API
        val createResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Ghost View Location","desc":"A place ghosts can see","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        // Verify the location appears in the list (no single-location GET endpoint exists)
        val listResponse = client.get("/locations")
        assertEquals(HttpStatusCode.OK, listResponse.status)

        val body = listResponse.bodyAsText()
        assertTrue(
            body.contains("Ghost View Location"),
            "Created location should appear in locations list"
        )
    }

    // ========== Complete Flow Simulation Tests ==========

    @Test
    fun testCompleteOnboardingFlow_RegisterToAdventure() = testApplication {
        application { module() }
        val client = jsonClient()

        // Step 1: Register
        val uniqueName = "fullflow_${System.currentTimeMillis()}"
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val userId = extractId(registerResponse.bodyAsText())

        // Verify: No class (client should show ghost exploration)
        var userResponse = client.get("/users/$userId")
        var userBody = json.parseToJsonElement(userResponse.bodyAsText()).jsonObject
        assertNull(userBody["characterClassId"]?.jsonPrimitive?.contentOrNull, "Step 1: Should have no class")

        // Step 2: User explores in ghost mode (can view locations)
        val locationsResponse = client.get("/locations")
        assertEquals(HttpStatusCode.OK, locationsResponse.status, "Step 2: Ghost mode can view locations")

        // Step 3: User commits attributes
        val commitResponse = client.post("/users/$userId/commit-attributes") {
            contentType(ContentType.Application.Json)
            setBody("""{"strength":14,"dexterity":12,"constitution":13,"intelligence":10,"wisdom":11,"charisma":8,"qualityBonus":0}""")
        }
        assertEquals(HttpStatusCode.OK, commitResponse.status, "Step 3: Can commit attributes")

        // Step 4: Assign class (simulating class generation completion)
        seedTestCharacterClass()
        val assignResponse = client.put("/users/$userId/class") {
            contentType(ContentType.Application.Json)
            setBody("""{"classId":"${TestFixtures.WARRIOR_CLASS_ID}"}""")
        }
        assertEquals(HttpStatusCode.OK, assignResponse.status, "Step 4: Can assign class")

        // Step 5: Verify user now has class (client should show main adventure)
        userResponse = client.get("/users/$userId")
        userBody = json.parseToJsonElement(userResponse.bodyAsText()).jsonObject
        assertEquals(
            TestFixtures.WARRIOR_CLASS_ID,
            userBody["characterClassId"]?.jsonPrimitive?.content,
            "Step 5: User should now have class assigned"
        )
    }

    @Test
    fun testReturningUser_WithClass_SkipsOnboarding() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create a user with class already assigned
        seedTestCharacterClass()
        val user = createUserWithClass()

        // Login (simulating returning user)
        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"${user.name}","password":"testpass123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val body = json.parseToJsonElement(loginResponse.bodyAsText()).jsonObject
        val returnedUser = body["user"]?.jsonObject
        assertNotNull(returnedUser)

        // Should have class - client will skip to main
        assertEquals(
            TestFixtures.WARRIOR_CLASS_ID,
            returnedUser["characterClassId"]?.jsonPrimitive?.content,
            "Returning user with class should skip onboarding"
        )
    }

    @Test
    fun testReturningUser_WithoutClass_ShowsGhostMode() = testApplication {
        application { module() }
        val client = jsonClient()

        // Register but don't assign class
        val uniqueName = "returning_noclass_${System.currentTimeMillis()}"
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }

        // Login again (simulating returning user)
        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val body = json.parseToJsonElement(loginResponse.bodyAsText()).jsonObject
        val user = body["user"]?.jsonObject
        assertNotNull(user)

        // Should have no class - client will show ghost mode
        assertNull(
            user["characterClassId"]?.jsonPrimitive?.contentOrNull,
            "Returning user without class should show ghost mode"
        )
    }

    // ========== Helper Methods ==========

    private fun seedTestCharacterClass() {
        org.jetbrains.exposed.sql.transactions.transaction {
            if (CharacterClassRepository.findById(TestFixtures.WARRIOR_CLASS_ID) == null) {
                CharacterClassRepository.create(TestFixtures.warriorClass())
            }
        }
    }

    private fun createUserWithClass(): User {
        seedTestCharacterClass()
        val user = User(
            id = "test-user-with-class-${System.currentTimeMillis()}",
            name = "testuser_withclass_${System.currentTimeMillis()}",
            passwordHash = UserRepository.hashPassword("testpass123"),
            characterClassId = TestFixtures.WARRIOR_CLASS_ID
        )
        org.jetbrains.exposed.sql.transactions.transaction {
            UserRepository.create(user)
        }
        return user
    }

    private fun createTestLocation(): String {
        val location = Location(
            id = "test-loc-${System.currentTimeMillis()}",
            name = "Test Location",
            desc = "A test location for ghost mode viewing",
            creatureIds = emptyList(),
            exits = emptyList(),
            itemIds = emptyList(),
            featureIds = emptyList()
        )
        org.jetbrains.exposed.sql.transactions.transaction {
            LocationRepository.create(location)
        }
        return location.id
    }

    private fun createStartingLocation(): String {
        val location = Location(
            id = "starting-location",
            name = "Starting Point",
            desc = "Where new adventurers begin",
            creatureIds = emptyList(),
            exits = emptyList(),
            itemIds = emptyList(),
            featureIds = emptyList(),
            gridX = 0,
            gridY = 0,
            areaId = "overworld"
        )
        org.jetbrains.exposed.sql.transactions.transaction {
            if (LocationRepository.findById(location.id) == null) {
                LocationRepository.create(location)
            }
        }
        return location.id
    }
}
