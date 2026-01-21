package com.ez2bg.anotherthread

import com.ez2bg.anotherthread.database.Ability
import com.ez2bg.anotherthread.database.AbilityRepository
import com.ez2bg.anotherthread.database.AbilityTable
import com.ez2bg.anotherthread.database.CharacterClass
import com.ez2bg.anotherthread.database.CharacterClassRepository
import com.ez2bg.anotherthread.database.CharacterClassTable
import io.ktor.client.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Custom serializer that accepts either a string or a JSON array and converts to a string.
 * This handles LLM responses that sometimes return "effects": "[]" (string)
 * and sometimes "effects": [] (actual array).
 */
object FlexibleEffectsSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleEffects", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content
            is JsonArray -> element.toString()
            else -> element.toString()
        }
    }
}

@Serializable
data class GeneratedClass(
    val name: String,
    val description: String,
    val isSpellcaster: Boolean,
    val hitDie: Int,
    val primaryAttribute: String
)

@Serializable
data class GeneratedAbility(
    val name: String,
    val description: String,
    val abilityType: String,
    val targetType: String,
    val range: Int,
    val cooldownType: String,
    val cooldownRounds: Int = 0,
    val baseDamage: Int = 0,
    val durationRounds: Int = 0,
    @Serializable(with = FlexibleEffectsSerializer::class)
    val effects: String = "[]"
)

@Serializable
data class GeneratedClassWithAbilities(
    val characterClass: GeneratedClass,
    val abilities: List<GeneratedAbility>
)

@Serializable
data class ClassMatchResult(
    val matchedClassId: String,
    val matchedClassName: String,
    val confidence: Float,
    val reasoning: String
)

object ClassGenerationService {
    private val log = LoggerFactory.getLogger(ClassGenerationService::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 600_000 // 10 minutes for complex generation with abilities
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

    // Standard power budget for balanced classes
    const val STANDARD_POWER_BUDGET = 100

    /**
     * Match a character description to an existing class
     */
    suspend fun matchToExistingClass(
        characterDescription: String,
        availableClasses: List<CharacterClass>? = null
    ): Result<ClassMatchResult> = withContext(Dispatchers.IO) {
        log.info("matchToExistingClass: Starting class matching for description: '${characterDescription.take(100)}...'")
        runCatching {
            val classes = availableClasses ?: CharacterClassRepository.findPublic()
            log.info("matchToExistingClass: Found ${classes.size} public classes to match against")

            if (classes.isEmpty()) {
                log.error("matchToExistingClass: No classes available to match against")
                throw IllegalStateException("No classes available to match against")
            }

            val classesInfo = classes.joinToString("\n") { cls ->
                "- ID: ${cls.id}, Name: ${cls.name}, Type: ${if (cls.isSpellcaster) "Spellcaster" else "Martial"}, Description: ${cls.description.take(100)}"
            }
            log.debug("matchToExistingClass: Available classes:\n$classesInfo")

            val prompt = """
You are a fantasy RPG class matching system. Given a character description, match it to the most appropriate class from the available options.

Character Description:
$characterDescription

Available Classes:
$classesInfo

Analyze the character description and determine which class is the best fit. Consider:
- Whether the character seems magical or martial
- Combat style preferences implied by the description
- Personality traits that align with class archetypes
- Any specific abilities or powers mentioned

Respond with valid JSON only in this exact format:
{"matchedClassId": "the-class-id", "matchedClassName": "Class Name", "confidence": 0.85, "reasoning": "Brief explanation of why this class fits"}
            """.trimIndent()

            log.info("matchToExistingClass: Calling LLM for class matching...")
            val response = callLlm(prompt)
            log.info("matchToExistingClass: LLM response received (${response.length} chars): ${response.take(200)}")

            val result = json.decodeFromString<ClassMatchResult>(response)
            log.info("matchToExistingClass: Successfully matched to class '${result.matchedClassName}' (id=${result.matchedClassId}) with confidence ${result.confidence}")
            result
        }.onFailure { e ->
            log.error("matchToExistingClass: Failed to match class - ${e::class.simpleName}: ${e.message}", e)
        }
    }

    /**
     * Generate a completely new class with 10 balanced abilities based on a description
     */
    suspend fun generateNewClass(
        characterDescription: String,
        createdByUserId: String? = null,
        isPublic: Boolean = false
    ): Result<Pair<CharacterClass, List<Ability>>> = withContext(Dispatchers.IO) {
        log.info("generateNewClass: Starting class generation for user $createdByUserId")
        log.info("generateNewClass: Character description: '${characterDescription.take(150)}...'")
        log.info("generateNewClass: isPublic=$isPublic")

        runCatching {
            // Step 1: Generate the base class
            log.info("generateNewClass: Step 1 - Generating base class definition...")
            val classPrompt = """
You are a fantasy RPG class designer. Create a unique character class based on this description:

$characterDescription

Design a balanced class that captures the essence of this description. Determine if it should be a spellcaster or martial class based on the description.

Power Budget Rules:
- The class has a total power budget of $STANDARD_POWER_BUDGET points
- This will be distributed across 10 abilities
- Average ability cost should be around 10 points

Respond with valid JSON only in this exact format:
{"name": "Class Name", "description": "A 2-3 sentence description of the class", "isSpellcaster": true, "hitDie": 8, "primaryAttribute": "intelligence"}

Valid primaryAttribute values: strength, dexterity, constitution, intelligence, wisdom, charisma
Valid hitDie values: 6, 8, 10, 12
            """.trimIndent()

            log.info("generateNewClass: Calling LLM for class definition...")
            val classResponse = callLlm(classPrompt)
            log.info("generateNewClass: LLM class response received (${classResponse.length} chars)")
            log.debug("generateNewClass: Raw class response: $classResponse")

            val classJson = extractJsonObject(classResponse)
            log.info("generateNewClass: Extracted JSON object (${classJson.length} chars): ${classJson.take(300)}")

            val generatedClass = try {
                json.decodeFromString<GeneratedClass>(classJson)
            } catch (e: Exception) {
                log.error("generateNewClass: Failed to parse class JSON - ${e::class.simpleName}: ${e.message}")
                log.error("generateNewClass: Attempted to parse: $classJson")
                throw IllegalArgumentException("Failed to parse class from LLM response: ${classJson.take(200)}. Error: ${e.message}")
            }

            log.info("generateNewClass: Successfully parsed class '${generatedClass.name}' - isSpellcaster=${generatedClass.isSpellcaster}, hitDie=${generatedClass.hitDie}, primaryAttribute=${generatedClass.primaryAttribute}")

            // Create the CharacterClass
            val characterClass = CharacterClass(
                name = generatedClass.name,
                description = generatedClass.description,
                isSpellcaster = generatedClass.isSpellcaster,
                hitDie = generatedClass.hitDie,
                primaryAttribute = generatedClass.primaryAttribute,
                powerBudget = STANDARD_POWER_BUDGET,
                isPublic = isPublic,
                createdByUserId = createdByUserId
            )
            log.info("generateNewClass: Created CharacterClass object with id=${characterClass.id}")

            // Step 2: Generate abilities for this class
            log.info("generateNewClass: Step 2 - Generating abilities for class '${characterClass.name}'...")
            val abilities = generateAbilitiesForClass(characterClass, characterDescription)
            log.info("generateNewClass: Generated ${abilities.size} abilities")

            // Step 3: Validate and rebalance if needed
            val totalCost = abilities.sumOf { it.powerCost }
            log.info("generateNewClass: Step 3 - Validating power budget. Total cost: $totalCost, Budget: $STANDARD_POWER_BUDGET")

            val balancedAbilities = if (totalCost > STANDARD_POWER_BUDGET * 1.2) {
                log.info("generateNewClass: Over budget by ${totalCost - STANDARD_POWER_BUDGET} points, rebalancing...")
                rebalanceAbilities(abilities, STANDARD_POWER_BUDGET)
            } else {
                log.info("generateNewClass: Within budget, no rebalancing needed")
                abilities
            }

            val finalCost = balancedAbilities.sumOf { it.powerCost }
            log.info("generateNewClass: Final power cost: $finalCost")
            log.info("generateNewClass: Class generation complete - '${characterClass.name}' with ${balancedAbilities.size} abilities")

            characterClass to balancedAbilities
        }.onFailure { e ->
            log.error("generateNewClass: Failed to generate class for user $createdByUserId - ${e::class.simpleName}: ${e.message}", e)
        }
    }

    /**
     * Generate 10 abilities for a class
     */
    private suspend fun generateAbilitiesForClass(
        characterClass: CharacterClass,
        originalDescription: String
    ): List<Ability> {
        log.info("generateAbilitiesForClass: Generating abilities for class '${characterClass.name}' (id=${characterClass.id})")
        val abilityType = if (characterClass.isSpellcaster) "spell" else "combat"
        log.debug("generateAbilitiesForClass: Default ability type will be '$abilityType'")

        val prompt = """
You are a fantasy RPG ability designer. Create 10 unique, balanced abilities for this class:

Class: ${characterClass.name}
Type: ${if (characterClass.isSpellcaster) "Spellcaster" else "Martial"}
Description: ${characterClass.description}
Original Character Concept: $originalDescription

Power Budget System:
Each ability has a power cost calculated as:
- baseDamage / 5 (e.g., 50 damage = 10 points)
- Range: 0=0, 5=1, 30=2, 60=3, 120=4, 120+=5
- Target: self=0, single=1, area=3, all=5
- Cooldown: none=+5, short=+2, medium=0, long=-2
- Duration: instant=0, 1-2 rounds=+2, 3+ rounds=+4

Target total: ~$STANDARD_POWER_BUDGET points across all 10 abilities (avg 10 per ability)

Create a diverse set covering:
- 2-3 damage/attack abilities
- 2-3 utility/control abilities
- 2-3 defensive/support abilities
- 2-3 unique signature abilities

Respond with valid JSON array only in this exact format:
[
  {"name": "Ability Name", "description": "Description", "abilityType": "$abilityType", "targetType": "single_enemy", "range": 60, "cooldownType": "medium", "cooldownRounds": 3, "baseDamage": 30, "durationRounds": 0, "effects": "[]"},
  ...
]

Valid abilityType: spell, combat, utility, passive
Valid targetType: self, single_enemy, single_ally, area, all_enemies, all_allies
Valid cooldownType: none, short, medium, long
        """.trimIndent()

        log.info("generateAbilitiesForClass: Calling LLM for ability generation...")
        val response = callLlm(prompt)
        log.info("generateAbilitiesForClass: LLM ability response received (${response.length} chars)")
        log.debug("generateAbilitiesForClass: Raw abilities response: ${response.take(1000)}")

        // Parse abilities
        val generatedAbilities = try {
            log.debug("generateAbilitiesForClass: Attempting direct JSON array parse...")
            json.decodeFromString<List<GeneratedAbility>>(response)
        } catch (e: Exception) {
            log.warn("generateAbilitiesForClass: Direct parse failed (${e::class.simpleName}: ${e.message}), trying regex extraction...")
            // Try to extract JSON array from response
            val arrayMatch = Regex("""\[[\s\S]*\]""").find(response)
            if (arrayMatch != null) {
                log.info("generateAbilitiesForClass: Found JSON array via regex, attempting parse...")
                log.debug("generateAbilitiesForClass: Extracted array: ${arrayMatch.value.take(500)}")
                json.decodeFromString<List<GeneratedAbility>>(arrayMatch.value)
            } else {
                log.error("generateAbilitiesForClass: Could not find JSON array in response")
                log.error("generateAbilitiesForClass: Response was: ${response.take(500)}")
                throw IllegalArgumentException("Could not parse abilities from LLM response: ${response.take(500)}")
            }
        }

        log.info("generateAbilitiesForClass: Successfully parsed ${generatedAbilities.size} abilities")

        // Convert to Ability objects with calculated power cost
        val abilities = generatedAbilities.take(10).map { gen ->
            val ability = Ability(
                name = gen.name,
                description = gen.description,
                classId = characterClass.id,
                abilityType = gen.abilityType,
                targetType = gen.targetType,
                range = gen.range,
                cooldownType = gen.cooldownType,
                cooldownRounds = gen.cooldownRounds,
                baseDamage = gen.baseDamage,
                durationRounds = gen.durationRounds,
                effects = gen.effects
            ).withCalculatedCost()
            log.debug("generateAbilitiesForClass: Created ability '${ability.name}' with powerCost=${ability.powerCost}")
            ability
        }

        log.info("generateAbilitiesForClass: Returning ${abilities.size} abilities with total power cost ${abilities.sumOf { it.powerCost }}")
        return abilities
    }

    /**
     * Rebalance abilities to fit within power budget
     */
    private suspend fun rebalanceAbilities(
        abilities: List<Ability>,
        targetBudget: Int
    ): List<Ability> {
        val currentTotal = abilities.sumOf { it.powerCost }
        val reductionFactor = targetBudget.toFloat() / currentTotal.toFloat()

        // Simple rebalancing: reduce damage and duration proportionally
        return abilities.map { ability ->
            val newDamage = (ability.baseDamage * reductionFactor).toInt()
            val newDuration = if (reductionFactor < 0.8f && ability.durationRounds > 2) {
                ability.durationRounds - 1
            } else {
                ability.durationRounds
            }

            ability.copy(
                baseDamage = newDamage.coerceAtLeast(0),
                durationRounds = newDuration.coerceAtLeast(0)
            ).withCalculatedCost()
        }
    }

    /**
     * Save a generated class and its abilities to the database atomically.
     * If any part fails, the entire operation is rolled back.
     */
    suspend fun saveGeneratedClass(
        characterClass: CharacterClass,
        abilities: List<Ability>
    ): Pair<CharacterClass, List<Ability>> = withContext(Dispatchers.IO) {
        log.info("saveGeneratedClass: Saving class '${characterClass.name}' (id=${characterClass.id}) with ${abilities.size} abilities")
        try {
            val result = transaction {
                // Insert class
                log.debug("saveGeneratedClass: Inserting class into database...")
                CharacterClassTable.insert {
                    it[id] = characterClass.id
                    it[name] = characterClass.name
                    it[description] = characterClass.description
                    it[isSpellcaster] = characterClass.isSpellcaster
                    it[hitDie] = characterClass.hitDie
                    it[primaryAttribute] = characterClass.primaryAttribute
                    it[powerBudget] = characterClass.powerBudget
                    it[isPublic] = characterClass.isPublic
                    it[createdByUserId] = characterClass.createdByUserId
                }
                log.debug("saveGeneratedClass: Class inserted successfully")

                // Insert all abilities
                log.debug("saveGeneratedClass: Inserting ${abilities.size} abilities...")
                val savedAbilities = abilities.mapIndexed { index, ability ->
                    val abilityWithClassId = ability.copy(classId = characterClass.id)
                    AbilityTable.insert {
                        it[id] = abilityWithClassId.id
                        it[name] = abilityWithClassId.name
                        it[description] = abilityWithClassId.description
                        it[classId] = abilityWithClassId.classId
                        it[abilityType] = abilityWithClassId.abilityType
                        it[targetType] = abilityWithClassId.targetType
                        it[range] = abilityWithClassId.range
                        it[cooldownType] = abilityWithClassId.cooldownType
                        it[cooldownRounds] = abilityWithClassId.cooldownRounds
                        it[baseDamage] = abilityWithClassId.baseDamage
                        it[durationRounds] = abilityWithClassId.durationRounds
                        it[effects] = abilityWithClassId.effects
                        it[powerCost] = abilityWithClassId.powerCost
                    }
                    log.debug("saveGeneratedClass: Inserted ability ${index + 1}/${abilities.size}: '${abilityWithClassId.name}'")
                    abilityWithClassId
                }

                characterClass to savedAbilities
            }
            log.info("saveGeneratedClass: Successfully saved class '${characterClass.name}' with ${abilities.size} abilities to database")
            result
        } catch (e: Exception) {
            log.error("saveGeneratedClass: Failed to save class '${characterClass.name}' - ${e::class.simpleName}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Suggest a rebalanced version of an ability (for nerf requests)
     */
    suspend fun suggestRebalance(ability: Ability): Result<Ability> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
You are a game balance expert. This ability may be overpowered and needs adjustment.

Current Ability:
- Name: ${ability.name}
- Description: ${ability.description}
- Type: ${ability.abilityType}
- Target: ${ability.targetType}
- Range: ${ability.range}ft
- Cooldown: ${ability.cooldownType} (${ability.cooldownRounds} rounds)
- Base Damage: ${ability.baseDamage}
- Duration: ${ability.durationRounds} rounds
- Current Power Cost: ${ability.powerCost}

Suggest a balanced version with power cost around 8-12 points. Options:
- Reduce damage
- Add/extend cooldown
- Reduce duration
- Change target type to be more limited
- Reduce range

Keep the ability's core identity and fantasy intact while making it more balanced.

Respond with valid JSON only in this exact format:
{"name": "${ability.name}", "description": "Updated description", "abilityType": "${ability.abilityType}", "targetType": "single_enemy", "range": 60, "cooldownType": "medium", "cooldownRounds": 3, "baseDamage": 25, "durationRounds": 1, "effects": "[]"}
            """.trimIndent()

            val response = callLlm(prompt)
            val suggested = json.decodeFromString<GeneratedAbility>(response)

            ability.copy(
                description = suggested.description,
                targetType = suggested.targetType,
                range = suggested.range,
                cooldownType = suggested.cooldownType,
                cooldownRounds = suggested.cooldownRounds,
                baseDamage = suggested.baseDamage,
                durationRounds = suggested.durationRounds,
                effects = suggested.effects
            ).withCalculatedCost()
        }
    }

    /**
     * Extract JSON object from a potentially malformed response
     */
    private fun extractJsonObject(text: String): String {
        // Try to find JSON object pattern
        val objectMatch = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""").find(text)
        if (objectMatch != null) {
            return objectMatch.value
        }
        // If no match, return original (will fail parsing with better error)
        return text
    }

    /**
     * Extract JSON array from a potentially malformed response
     */
    private fun extractJsonArray(text: String): String {
        val arrayMatch = Regex("""\[[\s\S]*\]""").find(text)
        if (arrayMatch != null) {
            return arrayMatch.value
        }
        return text
    }

    /**
     * Call the LLM API and return raw response text
     */
    private suspend fun callLlm(prompt: String): String {
        val url = "$llmApiUrl/api/generate"
        log.info("callLlm: Sending request to $url using model '$llmModel'")
        log.debug("callLlm: Prompt length: ${prompt.length} chars")

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
        log.debug("callLlm: Raw response body: ${responseBody.take(500)}")

        // Parse NDJSON - concatenate all response fields
        val lines = responseBody.trim().split("\n")
        log.debug("callLlm: Response has ${lines.size} NDJSON lines")

        val fullResponse = StringBuilder()
        var linesParsed = 0
        var linesSkipped = 0
        for (line in lines) {
            if (line.isNotBlank()) {
                try {
                    val partialResponse: OllamaResponse = json.decodeFromString(line)
                    fullResponse.append(partialResponse.response)
                    linesParsed++
                } catch (e: Exception) {
                    log.warn("callLlm: Failed to parse NDJSON line: ${line.take(100)} - ${e.message}")
                    linesSkipped++
                }
            }
        }

        log.info("callLlm: Parsed $linesParsed NDJSON lines, skipped $linesSkipped")
        val result = fullResponse.toString().trim()
        log.info("callLlm: Final response length: ${result.length} chars")
        log.debug("callLlm: Final response content: ${result.take(300)}")

        if (result.isEmpty()) {
            log.error("callLlm: LLM returned empty response!")
            log.error("callLlm: Raw body was: ${responseBody.take(500)}")
        }

        return result
    }

    /**
     * Check if the LLM service is available
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        log.debug("isAvailable: Checking LLM availability at $llmApiUrl/api/tags")
        runCatching {
            val response = client.get("$llmApiUrl/api/tags")
            val available = response.status == HttpStatusCode.OK
            log.info("isAvailable: LLM service at $llmApiUrl is ${if (available) "AVAILABLE" else "NOT AVAILABLE"} (status=${response.status})")
            available
        }.onFailure { e ->
            log.warn("isAvailable: Failed to check LLM availability - ${e::class.simpleName}: ${e.message}")
        }.getOrDefault(false)
    }
}
