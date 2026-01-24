package com.ez2bg.anotherthread

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class UserRoutesTest : BaseApplicationTest() {

    // ========== Authentication Tests ==========

    @Test
    fun testRegisterWithValidCredentials() = testApplication {
        application { module() }
        val client = jsonClient()

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
        application { module() }
        val client = jsonClient()

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
        application { module() }
        val client = jsonClient()

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
        application { module() }
        val client = jsonClient()

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
        assertTrue(response2.bodyAsText().contains("Username already exists"))
    }

    @Test
    fun testLoginWithValidCredentials() = testApplication {
        application { module() }
        val client = jsonClient()

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
        application { module() }
        val client = jsonClient()

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
        application { module() }
        val client = jsonClient()

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"nonexistentuser_${System.currentTimeMillis()}","password":"anypass"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ========== User CRUD Tests ==========

    @Test
    fun testGetUserById() = testApplication {
        application { module() }
        val client = jsonClient()

        val uniqueName = "getuser_${System.currentTimeMillis()}"

        // Register to create a user
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$uniqueName","password":"testpass123"}""")
        }
        val userId = extractId(registerResponse.bodyAsText())

        // Fetch user by ID
        val response = client.get("/users/$userId")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(uniqueName))
        assertTrue(body.contains(userId))
    }

    @Test
    fun testGetUserByIdNotFound() = testApplication {
        application { module() }
        val response = client.get("/users/nonexistent-user-id-12345")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUpdateUser() = testApplication {
        application { module() }
        val client = jsonClient()

        val userId = client.createTestUser("updateuser")

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
        application { module() }
        val client = jsonClient()

        val response = client.put("/users/nonexistent-user-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"desc":"test","itemIds":[],"featureIds":[]}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUpdateUserLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        val userId = client.createTestUser("locuser")

        // Update user location
        val response = client.put("/users/$userId/location") {
            contentType(ContentType.Application.Json)
            setBody("""{"locationId":"some-location-id"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testGetActiveUsersAtLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.get("/users/at-location/test-location-id")
        assertEquals(HttpStatusCode.OK, response.status)
        // Should return an array (possibly empty)
        assertTrue(response.bodyAsText().startsWith("["))
    }
}
