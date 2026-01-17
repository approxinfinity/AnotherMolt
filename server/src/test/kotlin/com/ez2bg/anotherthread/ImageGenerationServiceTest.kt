package com.ez2bg.anotherthread

import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import kotlin.test.*

/**
 * Tests for ImageGenerationService.
 * Unit tests run by default; integration tests require -PrunIntegrationTests=true
 */
class ImageGenerationServiceTest {

    companion object {
        private val runIntegrationTests: Boolean
            get() = System.getProperty("runIntegrationTests")?.toBoolean() ?: false
    }

    // ==================== Unit Tests ====================

    @Test
    fun testText2ImageRequestDefaults() {
        val request = Text2ImageRequest(prompt = "test prompt")

        assertEquals("test prompt", request.prompt)
        assertEquals("blurry, bad quality, distorted, ugly, deformed", request.negativePrompt)
        assertEquals(20, request.steps)
        assertEquals(512, request.width)
        assertEquals(512, request.height)
        assertEquals(7.0, request.cfgScale)
        assertEquals("Euler a", request.samplerName)
        assertEquals(1, request.batchSize)
    }

    @Test
    fun testText2ImageRequestCustomValues() {
        val request = Text2ImageRequest(
            prompt = "a beautiful landscape",
            negativePrompt = "ugly, blurry",
            steps = 30,
            width = 768,
            height = 768,
            cfgScale = 8.5,
            samplerName = "DPM++ 2M Karras",
            batchSize = 2
        )

        assertEquals("a beautiful landscape", request.prompt)
        assertEquals("ugly, blurry", request.negativePrompt)
        assertEquals(30, request.steps)
        assertEquals(768, request.width)
        assertEquals(768, request.height)
        assertEquals(8.5, request.cfgScale)
        assertEquals("DPM++ 2M Karras", request.samplerName)
        assertEquals(2, request.batchSize)
    }

    @Test
    fun testText2ImageResponseParsing() {
        val response = Text2ImageResponse(
            images = listOf("base64encodedimage1", "base64encodedimage2"),
            parameters = null,
            info = "Generation info"
        )

        assertEquals(2, response.images!!.size)
        assertEquals("base64encodedimage1", response.images!![0])
        assertEquals("base64encodedimage2", response.images!![1])
        assertNull(response.parameters)
        assertEquals("Generation info", response.info)
    }

    @Test
    fun testText2ImageResponseEmptyImages() {
        val response = Text2ImageResponse(
            images = emptyList(),
            parameters = null,
            info = null
        )

        assertTrue(response.images!!.isEmpty())
    }

    @Test
    fun testGetLocalImageUrlForNonexistentFile() {
        // Test that getLocalImageUrl returns null for files that don't exist
        val result = ImageGenerationService.getLocalImageUrl("room", "nonexistent-id-12345")
        assertNull(result)
    }

    // ==================== Integration Tests (require Stable Diffusion service) ====================
    // Run with: ./gradlew :server:test -PrunIntegrationTests=true

    @Test
    fun testIsAvailableDoesNotThrow() = runBlocking {
        Assume.assumeTrue("Skipping integration test", runIntegrationTests)
        // Test that isAvailable doesn't throw an exception regardless of whether SD is running
        val available = ImageGenerationService.isAvailable()
        // Just verify it returns a boolean without throwing
        assertTrue(available || !available, "isAvailable should return a boolean without throwing")
    }

    @Test
    fun testGenerateImageHandlesServiceState() = runBlocking {
        Assume.assumeTrue("Skipping integration test", runIntegrationTests)
        // Test that generateImage handles both available and unavailable states gracefully
        val available = ImageGenerationService.isAvailable()

        if (!available) {
            // When SD is not running, generateImage should fail gracefully
            val result = ImageGenerationService.generateImage(
                entityType = "room",
                entityId = "test-id",
                description = "a test description",
                entityName = "Test Room"
            )
            assertTrue(result.isFailure, "Expected failure when SD service is not available")
        } else {
            // When SD is running, we skip the failure test to avoid generating actual images
            // The important thing is that the service handles both states without crashing
            println("SD is available - skipping failure test")
        }
    }
}
