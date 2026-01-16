package com.ez2bg.anotherthread

import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Tests for ImageGenerationService.
 * Note: Network-dependent tests are skipped if SD is not available.
 */
class ImageGenerationServiceTest {

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

        assertEquals(2, response.images.size)
        assertEquals("base64encodedimage1", response.images[0])
        assertEquals("base64encodedimage2", response.images[1])
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

        assertTrue(response.images.isEmpty())
    }

    @Test
    fun testGetLocalImageUrlForNonexistentFile() {
        // Test that getLocalImageUrl returns null for files that don't exist
        val result = ImageGenerationService.getLocalImageUrl("room", "nonexistent-id-12345")
        assertNull(result)
    }

    @Test
    fun testIsAvailableReturnsFalseWhenServiceDown() = runBlocking {
        // Without a running Stable Diffusion server, isAvailable should return false
        val available = ImageGenerationService.isAvailable()
        // This will be false unless SD is actually running
        // We're testing that it doesn't throw an exception
        assertFalse(available, "Expected false when SD is not running (if this fails, SD may be running)")
    }

    @Test
    fun testGenerateImageFailsGracefullyWithoutService() = runBlocking {
        // Without a running Stable Diffusion server, generateImage should fail gracefully
        val result = ImageGenerationService.generateImage(
            entityType = "room",
            entityId = "test-id",
            description = "a test description",
            entityName = "Test Room"
        )

        assertTrue(result.isFailure, "Expected failure when SD service is not available")
    }
}
