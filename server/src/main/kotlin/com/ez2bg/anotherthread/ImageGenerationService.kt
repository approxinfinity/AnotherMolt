package com.ez2bg.anotherthread

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

@Serializable
data class Text2ImageRequest(
    val prompt: String,
    @SerialName("negative_prompt")
    val negativePrompt: String = "blurry, bad quality, distorted, ugly, deformed",
    val steps: Int = 20,
    val width: Int = 512,
    val height: Int = 512,
    @SerialName("cfg_scale")
    val cfgScale: Double = 7.0,
    @SerialName("sampler_name")
    val samplerName: String = "Euler a",
    @SerialName("batch_size")
    val batchSize: Int = 1
)

@Serializable
data class Text2ImageResponse(
    val images: List<String>? = null, // Base64 encoded images
    val parameters: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val info: String? = null,
    val error: String? = null,
    val detail: String? = null
)

object ImageGenerationService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 120_000 // 2 minutes for image generation
        }
    }

    // Stable Diffusion WebUI API URL - configurable via environment variable
    private val sdApiUrl: String
        get() = System.getenv("STABLE_DIFFUSION_URL") ?: "http://127.0.0.1:7860"

    // Base directory for all files
    private val baseFileDir: File
        get() = File(System.getenv("FILE_DIR") ?: "data/files")

    // Directory to save generated images (subfolder of files)
    private val imageDir: File
        get() = File(baseFileDir, "imageGen").also { it.mkdirs() }

    /**
     * Generate an image from description text and save it locally.
     * Returns the relative URL path to the saved image.
     */
    suspend fun generateImage(
        entityType: String,
        entityId: String,
        description: String,
        entityName: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Build a prompt optimized for game entity visualization
            val prompt = buildPrompt(entityType, description, entityName)

            val request = Text2ImageRequest(
                prompt = prompt,
                negativePrompt = "text, watermark, signature, blurry, bad quality, distorted, ugly, deformed, out of frame",
                steps = 25,
                width = 512,
                height = 512,
                cfgScale = 7.5
            )

            val httpResponse = client.post("$sdApiUrl/sdapi/v1/txt2img") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                throw Exception("API error (${httpResponse.status}): $errorBody")
            }

            val response: Text2ImageResponse = httpResponse.body()

            // Check for error in response
            if (response.error != null) {
                throw Exception("SD error: ${response.error}")
            }
            if (response.detail != null) {
                throw Exception("SD error: ${response.detail}")
            }

            val images = response.images
            if (images.isNullOrEmpty()) {
                throw Exception("No images generated")
            }

            // Decode base64 image and save to file
            val imageData = Base64.getDecoder().decode(images.first())
            val filename = "${entityType}_${entityId}.png"
            val imageFile = File(imageDir, filename)
            imageFile.writeBytes(imageData)

            // Return the relative path that can be served
            "/files/imageGen/$filename"
        }
    }

    /**
     * Build an optimized prompt based on entity type and description
     */
    private fun buildPrompt(entityType: String, description: String, entityName: String): String {
        val prefix = when (entityType.lowercase()) {
            "location" -> "fantasy game environment, detailed interior scene,"
            "creature" -> "fantasy creature portrait, detailed character art,"
            "item" -> "fantasy item, detailed object illustration, game asset,"
            "user" -> "fantasy character portrait, detailed character art, adventurer,"
            else -> "fantasy art, detailed illustration,"
        }

        val nameContext = if (entityName.isNotBlank()) "$entityName, " else ""

        return "$prefix $nameContext$description, high quality, detailed, digital art, concept art, artstation"
    }

    /**
     * Check if the Stable Diffusion service is available
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.get("$sdApiUrl/sdapi/v1/sd-models")
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    /**
     * Get the URL for an entity's image if it exists locally
     */
    fun getLocalImageUrl(entityType: String, entityId: String): String? {
        val filename = "${entityType}_${entityId}.png"
        val imageFile = File(imageDir, filename)
        return if (imageFile.exists()) "/files/imageGen/$filename" else null
    }
}
