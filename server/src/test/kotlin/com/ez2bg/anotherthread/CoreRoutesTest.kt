package com.ez2bg.anotherthread

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

/**
 * Tests for core/basic application routes (health, root, status endpoints).
 */
class CoreRoutesTest : BaseApplicationTest() {

    @Test
    fun testRoot() = testApplication {
        application { module() }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ktor: ${Greeting().greet()}", response.bodyAsText())
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun testImageGenerationStatusEndpoint() = testApplication {
        application { module() }
        val response = client.get("/image-generation/status")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("available"))
    }

    @Test
    fun testContentGenerationStatusEndpoint() = testApplication {
        application { module() }
        val response = client.get("/generate/status")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("available"))
    }
}
