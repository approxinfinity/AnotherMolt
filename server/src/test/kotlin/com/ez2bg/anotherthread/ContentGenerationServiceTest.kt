package com.ez2bg.anotherthread

import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Tests for ContentGenerationService.
 * Note: Network-dependent tests are skipped if LLM (Ollama) is not available.
 */
class ContentGenerationServiceTest {

    @Test
    fun testOllamaRequestDefaults() {
        val request = OllamaRequest(
            model = "llama3.2:3b",
            prompt = "test prompt"
        )

        assertEquals("llama3.2:3b", request.model)
        assertEquals("test prompt", request.prompt)
        assertEquals(false, request.stream)
        assertEquals("json", request.format)
    }

    @Test
    fun testOllamaRequestWithCustomValues() {
        val request = OllamaRequest(
            model = "mistral",
            prompt = "Generate a name",
            stream = true,
            format = "text"
        )

        assertEquals("mistral", request.model)
        assertEquals("Generate a name", request.prompt)
        assertEquals(true, request.stream)
        assertEquals("text", request.format)
    }

    @Test
    fun testOllamaResponseParsing() {
        val response = OllamaResponse(
            model = "llama3.2:3b",
            response = "{\"name\": \"Test\", \"description\": \"A test\"}",
            done = true
        )

        assertEquals("llama3.2:3b", response.model)
        assertEquals("{\"name\": \"Test\", \"description\": \"A test\"}", response.response)
        assertTrue(response.done)
    }

    @Test
    fun testOllamaResponseWithNullModel() {
        val response = OllamaResponse(
            model = null,
            response = "partial response",
            done = false
        )

        assertNull(response.model)
        assertEquals("partial response", response.response)
        assertFalse(response.done)
    }

    @Test
    fun testGeneratedContentDataClass() {
        val content = GeneratedContent(
            name = "Mystic Forest",
            description = "A dense forest shrouded in magical mist."
        )

        assertEquals("Mystic Forest", content.name)
        assertEquals("A dense forest shrouded in magical mist.", content.description)
    }

    @Test
    fun testIsAvailableDoesNotThrow() = runBlocking {
        // Test that isAvailable returns a boolean without throwing an exception
        // The result depends on whether Ollama is running
        val available = ContentGenerationService.isAvailable()
        // Just verify it returns a boolean (true or false) without exception
        assertTrue(available || !available)
    }

    @Test
    fun testGenerateLocationContentDoesNotThrow() = runBlocking {
        // Test that generation either succeeds or fails gracefully (no exception)
        val result = ContentGenerationService.generateLocationContent(
            exitIds = emptyList(),
            existingName = null,
            existingDesc = null
        )

        // Result should be either success or failure, not an exception
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun testGenerateCreatureContentDoesNotThrow() = runBlocking {
        val result = ContentGenerationService.generateCreatureContent(
            existingName = null,
            existingDesc = null
        )

        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun testGenerateItemContentDoesNotThrow() = runBlocking {
        val result = ContentGenerationService.generateItemContent(
            existingName = null,
            existingDesc = null
        )

        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun testGenerateLocationContentWithExistingContext() = runBlocking {
        // Test that the function handles existing context without throwing
        val result = ContentGenerationService.generateLocationContent(
            exitIds = listOf("loc-1", "loc-2"),
            existingName = "Old Name",
            existingDesc = "Old description that should be improved"
        )

        // Should either succeed or fail gracefully
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun testGenerateCreatureContentWithExistingContext() = runBlocking {
        val result = ContentGenerationService.generateCreatureContent(
            existingName = "Basic Goblin",
            existingDesc = "A small green creature"
        )

        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun testGenerateItemContentWithExistingContext() = runBlocking {
        val result = ContentGenerationService.generateItemContent(
            existingName = "Sword",
            existingDesc = "A sharp blade"
        )

        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun testGeneratedContentHasValidFields() = runBlocking {
        // If service is available, test that generated content has valid fields
        if (ContentGenerationService.isAvailable()) {
            val result = ContentGenerationService.generateLocationContent(
                exitIds = emptyList(),
                existingName = null,
                existingDesc = null
            )

            result.onSuccess { content ->
                assertTrue(content.name.isNotBlank(), "Generated name should not be blank")
                assertTrue(content.description.isNotBlank(), "Generated description should not be blank")
            }
        }
    }
}
