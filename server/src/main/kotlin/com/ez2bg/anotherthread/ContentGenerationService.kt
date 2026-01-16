package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.Location
import com.ez2bg.anotherthread.database.LocationRepository
import io.ktor.client.*
import io.ktor.client.call.*
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

    // Ollama API URL - configurable via environment variable
    private val llmApiUrl: String
        get() = System.getenv("LLM_API_URL") ?: "http://127.0.0.1:11434"

    // LLM Model - configurable via environment variable
    private val llmModel: String
        get() = System.getenv("LLM_MODEL") ?: "llama3.2:3b"

    /**
     * Generate content for a location, considering adjacent locations for context
     */
    suspend fun generateLocationContent(
        exitIds: List<String>,
        existingName: String?,
        existingDesc: String?
    ): Result<GeneratedContent> = withContext(Dispatchers.IO) {
        runCatching {
            // Fetch adjacent locations for context
            val adjacentLocations: List<Location> = exitIds.mapNotNull { id ->
                LocationRepository.findById(id)
            }

            val contextInfo = if (adjacentLocations.isNotEmpty()) {
                val adjacentNames = adjacentLocations.mapNotNull { loc ->
                    if (loc.name.isNotBlank()) loc.name else null
                }
                val adjacentDescs = adjacentLocations.mapNotNull { loc ->
                    if (loc.desc.isNotBlank()) "${loc.name}: ${loc.desc}" else null
                }
                buildString {
                    if (adjacentNames.isNotEmpty()) {
                        append("This location is connected to: ${adjacentNames.joinToString(", ")}. ")
                    }
                    if (adjacentDescs.isNotEmpty()) {
                        append("Adjacent areas: ${adjacentDescs.joinToString("; ")}. ")
                    }
                }
            } else ""

            val existingContext = buildString {
                if (!existingName.isNullOrBlank()) append("Current name: $existingName. ")
                if (!existingDesc.isNullOrBlank()) append("Current description: $existingDesc. ")
            }

            val prompt = """
You are a creative fantasy game world designer. Generate a name and description for a location in a fantasy game world.

$contextInfo$existingContext

Generate a unique, evocative name and a short atmospheric description (2-3 sentences) for this location. The description should hint at what players might find or experience there.

Respond with valid JSON only in this exact format:
{"name": "Location Name Here", "description": "Description here."}
            """.trimIndent()

            callLlm(prompt)
        }
    }

    /**
     * Generate content for a creature
     */
    suspend fun generateCreatureContent(
        existingName: String?,
        existingDesc: String?
    ): Result<GeneratedContent> = withContext(Dispatchers.IO) {
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
     * Generate content for an item
     */
    suspend fun generateItemContent(
        existingName: String?,
        existingDesc: String?
    ): Result<GeneratedContent> = withContext(Dispatchers.IO) {
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

        val response: OllamaResponse = client.post("$llmApiUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        // Parse the JSON response from the LLM
        return json.decodeFromString<GeneratedContent>(response.response)
    }

    /**
     * Check if the LLM service is available
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.get("$llmApiUrl/api/tags")
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }
}
