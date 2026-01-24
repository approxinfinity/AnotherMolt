package com.ez2bg.anotherthread

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class LocationRoutesTest : BaseApplicationTest() {

    @Test
    fun testGetLocationsEmpty() = testApplication {
        application { module() }
        val response = client.get("/locations")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Test
    fun testCreateLocation() = testApplication {
        application { module() }
        val client = jsonClient()

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
    fun testUpdateLocationNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.put("/locations/nonexistent-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated Location","desc":"Updated description","itemIds":[],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Deletable Location")

        val deleteResponse = client.delete("/locations/$locationId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val getResponse = client.get("/locations")
        assertFalse(getResponse.bodyAsText().contains(locationId))
    }

    @Test
    fun testDeleteLocationNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.delete("/locations/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteLocationRemovesFromExits() = testApplication {
        application { module() }
        val client = jsonClient()

        // Create two locations
        val location1Id = client.createTestLocation("Location 1")

        // Create location 2 with location 1 as an exit
        val loc2Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location 2","desc":"Test","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$location1Id","direction":"NORTH"}],"featureIds":[]}""")
        }
        val location2Id = extractId(loc2Response.bodyAsText())

        // Verify location 1 is in location 2's exits
        val beforeBody = client.get("/locations").bodyAsText()
        assertTrue(beforeBody.contains(location1Id))

        // Delete location 1
        assertEquals(HttpStatusCode.NoContent, client.delete("/locations/$location1Id").status)

        // Verify location 1 ID is removed from location 2's exits
        val afterBody = client.get("/locations").bodyAsText()
        assertFalse(afterBody.contains(""""locationId":"$location1Id""""))
        assertTrue(afterBody.contains(location2Id))
    }

    // ========== Lock Tests ==========

    @Test
    fun testLockLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Lockable Location")

        // Lock the location
        val lockResponse = client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        assertTrue(lockResponse.bodyAsText().contains("\"lockedBy\":\"user-123\""))

        // Unlock the location (toggle)
        val unlockResponse = client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.OK, unlockResponse.status)
        assertTrue(unlockResponse.bodyAsText().contains("\"lockedBy\":null"))
    }

    @Test
    fun testLockLocationNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.put("/locations/nonexistent-id/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testVerifyLockedFieldsReturnedInGetAll() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Locked Test Location")

        // Lock it
        client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"locker-user"}""")
        }

        // Verify lockedBy is returned in GET /locations
        val allBody = client.get("/locations").bodyAsText()
        assertTrue(allBody.contains("\"lockedBy\""))
        assertTrue(allBody.contains("locker-user"))
    }

    // ========== Exit Direction Tests ==========

    @Test
    fun testExitDirectionsAreSavedAndRetrieved() = testApplication {
        application { module() }
        val client = jsonClient()

        val loc1Id = client.createTestLocation("Town Square", "The central square")

        // Create location 2 with exit to location 1 going NORTH
        val loc2Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"South Gate","desc":"The southern entrance","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$loc1Id","direction":"NORTH"}],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, loc2Response.status)

        // Retrieve locations and verify the exit direction is preserved
        val body = client.get("/locations").bodyAsText()
        assertTrue(body.contains(""""locationId":"$loc1Id""""))
        assertTrue(body.contains(""""direction":"NORTH""""))
    }

    @Test
    fun testUpdateExitDirectionChangesLayout() = testApplication {
        application { module() }
        val client = jsonClient()

        val loc1Id = client.createTestLocation("Castle", "A mighty castle")

        val loc2Response = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Village","desc":"A peaceful village","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$loc1Id","direction":"EAST"}],"featureIds":[]}""")
        }
        val loc2Id = extractId(loc2Response.bodyAsText())

        // Verify initial direction is EAST
        assertTrue(client.get("/locations").bodyAsText().contains(""""direction":"EAST""""))

        // Update to change exit direction to WEST
        val updateResponse = client.put("/locations/$loc2Id") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Village","desc":"A peaceful village","itemIds":[],"creatureIds":[],"exits":[{"locationId":"$loc1Id","direction":"WEST"}],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify direction changed to WEST
        assertTrue(client.get("/locations").bodyAsText().contains(""""direction":"WEST""""))
    }

    // ========== Terrain Override Tests ==========

    @Test
    fun testUpdateTerrainOverrideOnLockedLocationByDifferentUserFails() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Terrain Test Location")

        // Lock by user-A
        client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-A"}""")
        }

        // Try to update terrain overrides as user-B (should fail)
        val updateResponse = client.put("/locations/$locationId/terrain-overrides") {
            contentType(ContentType.Application.Json)
            header("X-User-Id", "user-B")
            setBody("""{"forest":{"treeCount":50}}""")
        }
        assertEquals(HttpStatusCode.Forbidden, updateResponse.status)
    }

    @Test
    fun testUpdateTerrainOverrideOnLockedLocationBySameUserSucceeds() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Same User Terrain Test")

        // Lock by user-A
        client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-A"}""")
        }

        // Update as same user (should succeed)
        val updateResponse = client.put("/locations/$locationId/terrain-overrides") {
            contentType(ContentType.Application.Json)
            header("X-User-Id", "user-A")
            setBody("""{"forest":{"treeCount":100}}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("\"treeCount\":100"))
    }

    @Test
    fun testDeleteTerrainOverrideOnLockedLocationByDifferentUserFails() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Delete Terrain Test")

        // Add terrain overrides
        client.put("/locations/$locationId/terrain-overrides") {
            contentType(ContentType.Application.Json)
            header("X-User-Id", "user-A")
            setBody("""{"mountain":{"peakCount":5}}""")
        }

        // Lock by user-A
        client.put("/locations/$locationId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-A"}""")
        }

        // Try to delete as user-B (should fail)
        val deleteResponse = client.delete("/locations/$locationId/terrain-overrides") {
            header("X-User-Id", "user-B")
        }
        assertEquals(HttpStatusCode.Forbidden, deleteResponse.status)
    }

    @Test
    fun testUpdateTerrainOverrideOnUnlockedLocationSucceeds() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Unlocked Terrain Test")

        val updateResponse = client.put("/locations/$locationId/terrain-overrides") {
            contentType(ContentType.Application.Json)
            header("X-User-Id", "any-user")
            setBody("""{"lake":{"diameterMultiplier":1.5}}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("\"diameterMultiplier\":1.5"))
    }

    @Test
    fun testGetTerrainOverrideDoesNotRequireAuthentication() = testApplication {
        application { module() }
        val client = jsonClient()

        val locationId = client.createTestLocation("Read Terrain Test")

        val getResponse = client.get("/locations/$locationId/terrain-overrides")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("\"locationId\":\"$locationId\""))
    }
}
