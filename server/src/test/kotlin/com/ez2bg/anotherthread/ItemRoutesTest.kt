package com.ez2bg.anotherthread

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ItemRoutesTest : BaseApplicationTest() {

    @Test
    fun testGetItemsEmpty() = testApplication {
        application { module() }
        val response = client.get("/items")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Test
    fun testCreateItem() = testApplication {
        application { module() }
        val client = jsonClient()

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
    fun testUpdateItemNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.put("/items/nonexistent-id-12345") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Ghost Item","desc":"Doesn't exist","featureIds":[]}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteItem() = testApplication {
        application { module() }
        val client = jsonClient()

        val itemId = client.createTestItem("Deletable Item")

        val deleteResponse = client.delete("/items/$itemId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val getResponse = client.get("/items")
        assertFalse(getResponse.bodyAsText().contains(itemId))
    }

    @Test
    fun testDeleteItemNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.delete("/items/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteItemRemovesFromLocation() = testApplication {
        application { module() }
        val client = jsonClient()

        val itemId = client.createTestItem("Item to Delete")

        // Create a location with that item
        client.post("/locations") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Location with Item","desc":"Test","itemIds":["$itemId"],"creatureIds":[],"exits":[],"featureIds":[]}""")
        }

        // Verify item is in location
        val beforeBody = client.get("/locations").bodyAsText()
        assertTrue(beforeBody.contains(itemId))

        // Delete the item
        assertEquals(HttpStatusCode.NoContent, client.delete("/items/$itemId").status)

        // Verify item ID is removed from location's itemIds
        val afterBody = client.get("/locations").bodyAsText()
        assertFalse(afterBody.contains(""""itemIds":["$itemId"]"""))
    }

    // ========== Lock Tests ==========

    @Test
    fun testLockItem() = testApplication {
        application { module() }
        val client = jsonClient()

        val itemId = client.createTestItem("Lockable Item", "A precious artifact")

        // Lock the item
        val lockResponse = client.put("/items/$itemId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"curator-001"}""")
        }
        assertEquals(HttpStatusCode.OK, lockResponse.status)
        assertTrue(lockResponse.bodyAsText().contains("\"lockedBy\":\"curator-001\""))

        // Unlock the item (toggle by same user)
        val unlockResponse = client.put("/items/$itemId/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"curator-001"}""")
        }
        assertEquals(HttpStatusCode.OK, unlockResponse.status)
        assertTrue(unlockResponse.bodyAsText().contains("\"lockedBy\":null"))
    }

    @Test
    fun testLockItemNotFound() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.put("/items/nonexistent-id/lock") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"user-123"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
