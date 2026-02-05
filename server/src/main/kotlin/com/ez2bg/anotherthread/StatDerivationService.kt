package com.ez2bg.anotherthread

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class DerivedAttributes(
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val qualityBonus: Int,
    val reasoning: String,
    val missingAreas: List<String> = emptyList()
)

@Serializable
data class DeriveAttributesRequest(
    val description: String,
    val followUpAnswers: Map<String, String> = emptyMap()
)

@Serializable
data class CommitAttributesRequest(
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val qualityBonus: Int
)

object StatDerivationService {
    private val log = LoggerFactory.getLogger(StatDerivationService::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 300_000 // 5 minutes
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val llmApiUrl: String
        get() = System.getenv("LLM_API_URL") ?: "http://127.0.0.1:11434"

    private val llmModel: String
        get() = System.getenv("LLM_MODEL") ?: "llama3.2:3b"

    private const val BASE_POINT_POOL = 72
    private const val MIN_STAT = 3
    private const val MAX_STAT = 20
    private const val MAX_QUALITY_BONUS = 3

    suspend fun deriveAttributes(
        description: String,
        followUpAnswers: Map<String, String> = emptyMap()
    ): Result<DerivedAttributes> = withContext(Dispatchers.IO) {
        log.info("deriveAttributes: Starting for description: '${description.take(100)}...'")

        runCatching {
            val answersSection = if (followUpAnswers.isNotEmpty()) {
                "\nAdditional details from follow-up questions:\n" +
                    followUpAnswers.entries.joinToString("\n") { (q, a) -> "Q: $q\nA: $a" }
            } else ""

            val prompt = """
You are a D&D character attribute analyzer. Given a character description, derive the 6 D&D attributes and assess description quality.

Character Description:
$description
$answersSection

Assign attributes on a 3-20 scale. Consider:
- STR (Strength): Physical power, carrying capacity, melee attacks
- DEX (Dexterity): Agility, reflexes, ranged accuracy, stealth
- CON (Constitution): Endurance, health, toughness, stamina
- INT (Intelligence): Logic, memory, arcane knowledge, learning
- WIS (Wisdom): Perception, intuition, insight, divine connection
- CHA (Charisma): Personality, leadership, persuasion, presence

Point Economy: Distribute exactly $BASE_POINT_POOL points total (average 12 per stat). Then assess description quality:
- qualityBonus +1 to +3: Detailed, vivid, creative description with clear personality and physical traits
- qualityBonus 0: Average description with some detail
- qualityBonus -1 to -3: Vague, generic, or very brief description

Each stat must be between $MIN_STAT and $MAX_STAT.

Also identify which areas the description lacks detail about:
- "physical" if STR/DEX/CON traits are unclear
- "mental" if INT/WIS traits are unclear
- "social" if CHA traits are unclear

Respond with valid JSON only:
{"strength": 12, "dexterity": 14, "constitution": 10, "intelligence": 15, "wisdom": 11, "charisma": 10, "qualityBonus": 0, "reasoning": "Brief explanation of stat choices", "missingAreas": []}
            """.trimIndent()

            log.info("deriveAttributes: Calling LLM...")
            val response = callLlm(prompt)
            log.info("deriveAttributes: LLM response received (${response.length} chars)")

            val rawAttributes = json.decodeFromString<DerivedAttributes>(extractJsonObject(response))
            log.info("deriveAttributes: Parsed raw attributes - STR=${rawAttributes.strength} DEX=${rawAttributes.dexterity} CON=${rawAttributes.constitution} INT=${rawAttributes.intelligence} WIS=${rawAttributes.wisdom} CHA=${rawAttributes.charisma} quality=${rawAttributes.qualityBonus}")

            // Enforce point economy server-side
            val balanced = balanceStats(rawAttributes)
            log.info("deriveAttributes: Balanced attributes - STR=${balanced.strength} DEX=${balanced.dexterity} CON=${balanced.constitution} INT=${balanced.intelligence} WIS=${balanced.wisdom} CHA=${balanced.charisma} quality=${balanced.qualityBonus} total=${balanced.total()}")

            balanced
        }.onFailure { e ->
            log.error("deriveAttributes: Failed - ${e::class.simpleName}: ${e.message}", e)
        }
    }

    private fun DerivedAttributes.total(): Int =
        strength + dexterity + constitution + intelligence + wisdom + charisma

    private fun balanceStats(raw: DerivedAttributes): DerivedAttributes {
        // Clamp quality bonus
        val qualityBonus = raw.qualityBonus.coerceIn(-MAX_QUALITY_BONUS, MAX_QUALITY_BONUS)
        val targetTotal = BASE_POINT_POOL + qualityBonus

        // Clamp individual stats first
        var stats = listOf(
            raw.strength.coerceIn(MIN_STAT, MAX_STAT),
            raw.dexterity.coerceIn(MIN_STAT, MAX_STAT),
            raw.constitution.coerceIn(MIN_STAT, MAX_STAT),
            raw.intelligence.coerceIn(MIN_STAT, MAX_STAT),
            raw.wisdom.coerceIn(MIN_STAT, MAX_STAT),
            raw.charisma.coerceIn(MIN_STAT, MAX_STAT)
        ).toMutableList()

        var currentTotal = stats.sum()

        // Redistribute to hit target total
        while (currentTotal != targetTotal) {
            if (currentTotal > targetTotal) {
                // Find highest stat that can be reduced
                val maxIdx = stats.indices
                    .filter { stats[it] > MIN_STAT }
                    .maxByOrNull { stats[it] } ?: break
                stats[maxIdx]--
                currentTotal--
            } else {
                // Find lowest stat that can be increased
                val minIdx = stats.indices
                    .filter { stats[it] < MAX_STAT }
                    .minByOrNull { stats[it] } ?: break
                stats[minIdx]++
                currentTotal++
            }
        }

        return raw.copy(
            strength = stats[0],
            dexterity = stats[1],
            constitution = stats[2],
            intelligence = stats[3],
            wisdom = stats[4],
            charisma = stats[5],
            qualityBonus = qualityBonus
        )
    }

    private fun extractJsonObject(text: String): String {
        val objectMatch = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""").find(text)
        return objectMatch?.value ?: text
    }

    private suspend fun callLlm(prompt: String): String {
        val url = "$llmApiUrl/api/generate"
        log.info("callLlm: Sending request to $url using model '$llmModel'")

        val request = OllamaRequest(
            model = llmModel,
            prompt = prompt,
            stream = false,
            format = "json"
        )

        val startTime = System.currentTimeMillis()
        val responseBody: String = try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.bodyAsText()
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            log.error("callLlm: HTTP request failed after ${elapsed}ms - ${e::class.simpleName}: ${e.message}", e)
            throw e
        }

        val elapsed = System.currentTimeMillis() - startTime
        log.info("callLlm: Received response in ${elapsed}ms (${responseBody.length} chars)")

        // Parse NDJSON
        val lines = responseBody.trim().split("\n")
        val fullResponse = StringBuilder()
        for (line in lines) {
            if (line.isNotBlank()) {
                try {
                    val partialResponse: OllamaResponse = json.decodeFromString(line)
                    fullResponse.append(partialResponse.response)
                } catch (e: Exception) {
                    log.warn("callLlm: Failed to parse NDJSON line: ${line.take(100)}")
                }
            }
        }

        val result = fullResponse.toString().trim()
        log.info("callLlm: Final response length: ${result.length} chars")

        if (result.isEmpty()) {
            log.error("callLlm: LLM returned empty response!")
            throw IllegalStateException("LLM returned empty response")
        }

        return result
    }
}
