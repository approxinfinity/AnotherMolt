package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.Feature
import com.ez2bg.anotherthread.database.FeatureRepository
import com.ez2bg.anotherthread.database.Location
import com.ez2bg.anotherthread.database.LocationRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GeneratedContent(
    val name: String,
    val description: String
)

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val format: String = "json"
)

@Serializable
data class OllamaResponse(
    val model: String? = null,
    val response: String,
    @SerialName("done")
    val done: Boolean = true
)

object ContentGenerationService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 60_000 // 1 minute for LLM generation
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Disable Ollama calls during tests unless explicitly enabled with -PrunIntegrationTests=true
    // This prevents expensive LLM generation from running during normal test execution
    private val isTestMode: Boolean
        get() = System.getProperty("runIntegrationTests")?.toBoolean() != true &&
                (System.getProperty("org.gradle.test.worker") != null ||
                 System.getenv("DISABLE_LLM_GENERATION") == "true")

    // Ollama API URL - configurable via environment variable
    private val llmApiUrl: String
        get() = System.getenv("LLM_API_URL") ?: "http://127.0.0.1:11434"

    // LLM Model - configurable via environment variable
    private val llmModel: String
        get() = System.getenv("LLM_MODEL") ?: "llama3.2:3b"

    /**
     * Generate content for a location, considering adjacent locations and features for context.
     *
     * In test mode (unless -PrunIntegrationTests=true), returns failure to avoid
     * expensive LLM calls during normal test execution.
     */
    suspend fun generateLocationContent(
        exitIds: List<String>,
        featureIds: List<String> = emptyList(),
        existingName: String?,
        existingDesc: String?
    ): Result<GeneratedContent> = withContext(Dispatchers.IO) {
        // Skip LLM calls in test mode
        if (isTestMode) {
            return@withContext Result.failure(Exception("LLM generation disabled in test mode"))
        }

        runCatching {
            // Fetch adjacent locations for context
            val adjacentLocations: List<Location> = exitIds.mapNotNull { id ->
                LocationRepository.findById(id)
            }

            // Fetch features in this location for context
            val features: List<Feature> = featureIds.mapNotNull { id ->
                FeatureRepository.findById(id)
            }

            val adjacentContext = if (adjacentLocations.isNotEmpty()) {
                val adjacentInfo = adjacentLocations.mapNotNull { loc ->
                    if (loc.name.isNotBlank()) {
                        if (loc.desc.isNotBlank()) "${loc.name}: ${loc.desc}" else loc.name
                    } else null
                }
                "Adjacent locations: ${adjacentInfo.joinToString("; ")}. "
            } else ""

            val featureContext = if (features.isNotEmpty()) {
                val featureInfo = features.mapNotNull { feat ->
                    if (feat.description.isNotBlank()) "${feat.name}: ${feat.description}" else feat.name
                }
                "Features in this location: ${featureInfo.joinToString("; ")}. "
            } else ""

            val existingContext = buildString {
                if (!existingName.isNullOrBlank()) append("Location name: $existingName. ")
                if (!existingDesc.isNullOrBlank()) append("Current description to improve upon: $existingDesc. ")
            }

            val prompt = """
You are a creative fantasy game world designer. Generate a description for a location in a fantasy game world.

$existingContext$adjacentContext$featureContext

Based on the location name, adjacent locations, and any features present, generate a short atmospheric description (2-3 sentences) for this location. The description should:
- Be consistent with the location's name
- Reference or hint at the connected areas if provided
- Incorporate any features present in the room
- Hint at what players might find or experience there

Respond with valid JSON only in this exact format:
{"name": "$existingName", "description": "Description here."}
            """.trimIndent()

            callLlm(prompt)
        }
    }

    /**
     * Generate content for a creature.
     *
     * In test mode (unless -PrunIntegrationTests=true), returns failure to avoid
     * expensive LLM calls during normal test execution.
     */
    suspend fun generateCreatureContent(
        existingName: String?,
        existingDesc: String?
    ): Result<GeneratedContent> = withContext(Dispatchers.IO) {
        // Skip LLM calls in test mode
        if (isTestMode) {
            return@withContext Result.failure(Exception("LLM generation disabled in test mode"))
        }

        runCatching {
            val existingContext = buildString {
                if (!existingName.isNullOrBlank()) append("Current name: $existingName. ")
                if (!existingDesc.isNullOrBlank()) append("Current description: $existingDesc. ")
            }

            val prompt = """
You are a creative fantasy game world designer. Generate a name and description for a creature in a fantasy game world.

$existingContext

Generate a unique, memorable name and a short description (2-3 sentences) for this creature. Include physical characteristics, behavior hints, and what makes it interesting or dangerous.

Respond with valid JSON only in this exact format:
{"name": "Creature Name Here", "description": "Description here."}
            """.trimIndent()

            callLlm(prompt)
        }
    }

    /**
     * Generate content for an item.
     *
     * In test mode (unless -PrunIntegrationTests=true), returns failure to avoid
     * expensive LLM calls during normal test execution.
     */
    suspend fun generateItemContent(
        existingName: String?,
        existingDesc: String?
    ): Result<GeneratedContent> = withContext(Dispatchers.IO) {
        // Skip LLM calls in test mode
        if (isTestMode) {
            return@withContext Result.failure(Exception("LLM generation disabled in test mode"))
        }

        runCatching {
            val existingContext = buildString {
                if (!existingName.isNullOrBlank()) append("Current name: $existingName. ")
                if (!existingDesc.isNullOrBlank()) append("Current description: $existingDesc. ")
            }

            val prompt = """
You are a creative fantasy game world designer. Generate a name and description for an item in a fantasy game world.

$existingContext

Generate a unique, intriguing name and a short description (2-3 sentences) for this item. Include its appearance, potential uses, and any magical or special properties it might have.

Respond with valid JSON only in this exact format:
{"name": "Item Name Here", "description": "Description here."}
            """.trimIndent()

            callLlm(prompt)
        }
    }

    /**
     * Call the LLM API and parse the response
     */
    private suspend fun callLlm(prompt: String): GeneratedContent {
        val request = OllamaRequest(
            model = llmModel,
            prompt = prompt,
            stream = false,
            format = "json"
        )

        // Ollama returns application/x-ndjson (newline-delimited JSON) - each line has a piece of the response
        // We need to concatenate all the "response" fields together
        val responseBody: String = client.post("$llmApiUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyAsText()

        // Parse NDJSON - concatenate all response fields from each line
        val lines = responseBody.trim().split("\n")
        val fullResponse = StringBuilder()
        for (line in lines) {
            if (line.isNotBlank()) {
                try {
                    val partialResponse: OllamaResponse = json.decodeFromString(line)
                    fullResponse.append(partialResponse.response)
                } catch (e: Exception) {
                    // Skip malformed lines
                }
            }
        }

        val response = OllamaResponse(response = fullResponse.toString(), done = true)

        // Parse the JSON response from the LLM
        val responseText = response.response.trim()

        // Try to parse as GeneratedContent directly
        return try {
            json.decodeFromString<GeneratedContent>(responseText)
        } catch (e: Exception) {
            // Try to extract JSON from the response if it contains extra text
            val jsonMatch = Regex("""\{[^{}]*"name"\s*:\s*"[^"]*"[^{}]*"description"\s*:\s*"[^"]*"[^{}]*\}""")
                .find(responseText)
                ?: Regex("""\{[^{}]*"description"\s*:\s*"[^"]*"[^{}]*"name"\s*:\s*"[^"]*"[^{}]*\}""")
                    .find(responseText)

            if (jsonMatch != null) {
                json.decodeFromString<GeneratedContent>(jsonMatch.value)
            } else {
                // Last resort: try to extract name and description fields manually
                val nameMatch = Regex(""""name"\s*:\s*"([^"]*)"""").find(responseText)
                val descMatch = Regex(""""description"\s*:\s*"([^"]*)"""").find(responseText)

                if (nameMatch != null && descMatch != null) {
                    GeneratedContent(
                        name = nameMatch.groupValues[1],
                        description = descMatch.groupValues[1]
                    )
                } else {
                    throw IllegalArgumentException(
                        "Could not parse LLM response. Raw response: ${responseText.take(500)}"
                    )
                }
            }
        }
    }

    /**
     * Check if the LLM service is available.
     *
     * In test mode (unless -PrunIntegrationTests=true), returns false without
     * making network calls.
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        // Skip LLM calls in test mode
        if (isTestMode) {
            return@withContext false
        }

        runCatching {
            val response = client.get("$llmApiUrl/api/tags")
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }
}
