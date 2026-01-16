package com.ez2bg.anotherthread

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class ApplicationTest {

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
            setBody("""{"name":"Test Location","desc":"A test location description","itemIds":[],"creatureIds":[],"exitIds":[],"features":[]}""")
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
            setBody("""{"name":"Test Creature","desc":"A fearsome beast","itemIds":[],"features":["claws","fangs"]}""")
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
            setBody("""{"name":"Updated Location","desc":"Updated description","itemIds":[],"creatureIds":[],"exitIds":[],"features":[]}""")
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
}
