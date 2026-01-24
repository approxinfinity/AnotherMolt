package com.ez2bg.anotherthread

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class CreatureRoutesTest : BaseApplicationTest() {

    @Test
    fun testGetCreaturesEmpty() = testApplication {
        application { module() }
        val response = client.get("/creatures")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Test
    fun testCreateCreature() = testApplication {
        application { module() }
        val client = jsonClient()

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
    fun testUpdateCreatureNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.put("/creatures/nonexistent-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Ghost Creature","desc":"Doesn't exist","itemIds":[],"featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteCreature() = testApplication {
        application { module() }
        val client = jsonClient()

        val creatureId = client.createTestCreature("Deletable Creature")

        val deleteResponse = client.delete("/creatures/$creatureId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val getResponse = client.get("/creatures")
        assertFalse(getResponse.bodyAsText().contains(creatureId))
    }

    @Test
    fun testDeleteCreatureNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.delete("/creatures/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteCreatureRemovesFromLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        val creatureId = client.createTestCreature("Creature to Delete")

        // Create a location with that creature
        val locationResponse = client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location with Creature","desc":"Test","itemIds":[],"creatureIds":["$creatureId"],"exits":[],"featureIds":[]}""")
        }

        // Verify creature is in location
        val beforeBody = client.get("/locations").bodyAsText()
        assertTrue(beforeBody.contains(creatureId))

        // Delete the creature
        assertEquals(HttpStatusCode.NoContent, client.delete("/creatures/$creatureId").status)

        // Verify creature ID is removed from location's creatureIds
        val afterBody = client.get("/locations").bodyAsText()
        assertFalse(afterBody.contains(""""creatureIds":["$creatureId"]"""))
    }

    // ========== Lock Tests ==========

    @Test
    fun testLockCreature() = testApplication {
        application { module() }
        val client = jsonClient()

        val creatureId = client.createTestCreature("Lockable Creature")

        // Lock the creature
        val lockResponse = client.put("/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"admin-user"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        assertTrue(lockResponse.bodyAsText().contains("\"lockedBy\":\"admin-user\""))

        // Unlock the creature (toggle by same user)
        val unlockResponse = client.put("/creatures/$creatureId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"admin-user"}""")
        }
        assertEquals(HttpStatusCode.OK, unlockResponse.status)
        assertTrue(unlockResponse.bodyAsText().contains("\"lockedBy\":null"))
    }

    @Test
    fun testLockCreatureByDifferentUser() = testApplication {
        application { module() }
        val client = jsonClient()

        val creatureId = client.createTestCreature("Contested Creature")

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
        application { module() }
        val client = jsonClient()

        val response = client.put("/creatures/nonexistent-id/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
