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
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * Analysis types supported by the PDF analysis service.
 */
enum class PdfAnalysisType {
    MAP,        // ASCII map with locations and connections
    CLASSES,    // Character classes with abilities
    ITEMS,      // Item lists with descriptions
    CREATURES,  // Creature/monster lists
    ABILITIES   // Standalone ability lists
}

/**
 * Extracted location from a map PDF.
 */
@Serializable
data class ExtractedLocation(
    val id: String,
    val name: String,
    val description: String,
    val exits: List<ExtractedExit> = emptyList(),
    val features: List<String> = emptyList()
)

@Serializable
data class ExtractedExit(
    val targetId: String,
    val direction: String
)

/**
 * Extracted character class from a PDF.
 */
@Serializable
data class ExtractedClass(
    val name: String,
    val description: String,
    val isSpellcaster: Boolean = false,
    val hitDie: Int = 8,
    val primaryAttribute: String = "strength",
    val abilities: List<ExtractedAbility> = emptyList()
)

/**
 * Extracted ability from a PDF.
 */
@Serializable
data class ExtractedAbility(
    val name: String,
    val description: String,
    val abilityType: String = "combat",  // spell, combat, utility, passive
    val targetType: String = "single_enemy",
    val cooldownType: String = "none"
)

/**
 * Extracted item from a PDF.
 */
@Serializable
data class ExtractedItem(
    val name: String,
    val description: String,
    val itemType: String = "misc",  // weapon, armor, consumable, misc
    val features: List<String> = emptyList()
)

/**
 * Extracted creature from a PDF.
 */
@Serializable
data class ExtractedCreature(
    val name: String,
    val description: String,
    val level: Int = 1,
    val isAggressive: Boolean = false,
    val abilities: List<String> = emptyList()
)

/**
 * Result of PDF analysis.
 */
@Serializable
data class PdfAnalysisResult(
    val success: Boolean,
    val analysisType: String,
    val locations: List<ExtractedLocation> = emptyList(),
    val classes: List<ExtractedClass> = emptyList(),
    val items: List<ExtractedItem> = emptyList(),
    val creatures: List<ExtractedCreature> = emptyList(),
    val abilities: List<ExtractedAbility> = emptyList(),
    val rawText: String? = null,
    val error: String? = null
)

object PdfService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 120_000 // 2 minutes for complex analysis
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
     * Extract text from a PDF file.
     */
    fun extractText(pdfFile: File): String {
        return Loader.loadPDF(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            stripper.getText(document)
        }
    }

    /**
     * Extract text from PDF bytes.
     */
    fun extractText(pdfBytes: ByteArray): String {
        return Loader.loadPDF(pdfBytes).use { document ->
            val stripper = PDFTextStripper()
            stripper.getText(document)
        }
    }

    /**
     * Analyze a PDF file and extract structured data based on the analysis type.
     */
    suspend fun analyzePdf(
        pdfBytes: ByteArray,
        analysisType: PdfAnalysisType,
        areaId: String? = null
    ): PdfAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val text = extractText(pdfBytes)

            when (analysisType) {
                PdfAnalysisType.MAP -> analyzeMap(text, areaId ?: "imported")
                PdfAnalysisType.CLASSES -> analyzeClasses(text)
                PdfAnalysisType.ITEMS -> analyzeItems(text)
                PdfAnalysisType.CREATURES -> analyzeCreatures(text)
                PdfAnalysisType.ABILITIES -> analyzeAbilities(text)
            }
        } catch (e: Exception) {
            PdfAnalysisResult(
                success = false,
                analysisType = analysisType.name,
                error = "Failed to analyze PDF: ${e.message}"
            )
        }
    }

    /**
     * Analyze an ASCII map using two-pass extraction:
     * Pass 1: Extract legend entries (location names/symbols)
     * Pass 2: Generate descriptions and identify connections
     */
    private suspend fun analyzeMap(text: String, areaId: String): PdfAnalysisResult {
        // PASS 1: Extract legend entries
        val pass1Prompt = """
You are analyzing an ASCII map from a MUD game. Extract ONLY the legend entries.

Here is the map text:
```
$text
```

TASK: Find all legend entries. These are lines like:
- "G: Grand Stair" or "G: Grand Stair -->"
- "Y: Yellow House" or "Y:Fungus (poison)"
- "D: Down to Gaunt One Elder"
- "@: Secret Passage"

Also note any labeled areas mentioned (like "YELLOW HOUSE", "HAZY SWAMP", "CRACKED PLAINS").

Respond with valid JSON only:
{
  "legendEntries": [
    {"symbol": "G", "name": "Grand Stair", "notes": "entrance point"},
    {"symbol": "Y", "name": "Yellow House", "notes": ""},
    {"symbol": "D", "name": "Down to Gaunt One Elder", "notes": "leads down"},
    {"symbol": "@", "name": "Secret Passage", "notes": "hidden"}
  ],
  "namedAreas": ["Yellow House", "Hazy Swamp", "Cracked Plains"]
}
        """.trimIndent()

        return try {
            // Pass 1: Get legend entries
            val pass1Response = callLlm(pass1Prompt)
            val legendData = parseLegendResponse(pass1Response)

            if (legendData.isEmpty()) {
                return PdfAnalysisResult(
                    success = false,
                    analysisType = "MAP",
                    rawText = text.take(2000),
                    error = "No legend entries found in PDF"
                )
            }

            // PASS 2: Generate locations with descriptions and connections
            val legendList = legendData.joinToString("\n") { "- ${it.first}: ${it.second}" }

            val pass2Prompt = """
You are creating game locations from an ASCII map legend. Here are the legend entries found:

$legendList

And here is the original map for reference:
```
${text.take(4000)}
```

TASK: For each legend entry, create a location with:
1. A unique ID using the pattern "${areaId}-001", "${areaId}-002", etc.
2. An atmospheric name (can expand on the legend name)
3. A 2-3 sentence description fitting the fantasy theme
4. Connections to OTHER legend locations based on the map paths
5. Special features mentioned in the legend notes

IMPORTANT for connections:
- Only connect to OTHER locations from this list
- Use targetId matching the IDs you assign
- Directions: NORTH, SOUTH, EAST, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST, ENTER (for portals/stairs)
- If a location leads to an external area (like "Hazy Swamp"), note it in features but don't create an exit

Respond with valid JSON only:
{
  "locations": [
    {
      "id": "${areaId}-001",
      "name": "The Grand Staircase",
      "description": "A sweeping stone stairway descends into the depths. Ancient runes carved into the bannister glow faintly with forgotten magic.",
      "exits": [
        {"targetId": "${areaId}-002", "direction": "SOUTH"},
        {"targetId": "${areaId}-003", "direction": "ENTER"}
      ],
      "features": ["entrance", "ancient runes"]
    }
  ]
}
            """.trimIndent()

            val pass2Response = callLlm(pass2Prompt)
            val locations = parseLocationsResponse(pass2Response)

            PdfAnalysisResult(
                success = true,
                analysisType = "MAP",
                locations = locations,
                rawText = text.take(2000)
            )
        } catch (e: Exception) {
            PdfAnalysisResult(
                success = false,
                analysisType = "MAP",
                rawText = text.take(2000),
                error = "Failed to parse map: ${e.message}"
            )
        }
    }

    /**
     * Parse legend response from pass 1.
     * Returns list of (symbol, name) pairs.
     */
    private fun parseLegendResponse(response: String): List<Pair<String, String>> {
        return try {
            val wrapper = json.decodeFromString<LegendWrapper>(response)
            wrapper.legendEntries.map { it.symbol to it.name }
        } catch (e: Exception) {
            // Try to extract JSON from response
            val jsonMatch = Regex("""\{[\s\S]*"legendEntries"[\s\S]*\}""").find(response)
            if (jsonMatch != null) {
                try {
                    val wrapper = json.decodeFromString<LegendWrapper>(jsonMatch.value)
                    wrapper.legendEntries.map { it.symbol to it.name }
                } catch (e2: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    @Serializable
    private data class LegendEntry(
        val symbol: String,
        val name: String,
        val notes: String = ""
    )

    @Serializable
    private data class LegendWrapper(
        val legendEntries: List<LegendEntry>,
        val namedAreas: List<String> = emptyList()
    )

    /**
     * Analyze text for character classes.
     */
    private suspend fun analyzeClasses(text: String): PdfAnalysisResult {
        val prompt = """
You are analyzing a document describing character classes for a fantasy RPG game.

Here is the text:
```
${text.take(8000)}
```

Extract all character classes mentioned. For each class:
1. Name of the class
2. A description of what the class does
3. Whether it's a spellcaster (true/false)
4. Suggested hit die (6, 8, 10, or 12)
5. Primary attribute (strength, dexterity, constitution, intelligence, wisdom, charisma)
6. List of abilities/spells the class has

Respond with valid JSON only in this exact format:
{
  "classes": [
    {
      "name": "Class Name",
      "description": "Class description",
      "isSpellcaster": false,
      "hitDie": 8,
      "primaryAttribute": "strength",
      "abilities": [
        {
          "name": "Ability Name",
          "description": "What it does",
          "abilityType": "combat",
          "targetType": "single_enemy",
          "cooldownType": "short"
        }
      ]
    }
  ]
}
        """.trimIndent()

        return try {
            val response = callLlm(prompt)
            val classes = parseClassesResponse(response)
            PdfAnalysisResult(
                success = true,
                analysisType = "CLASSES",
                classes = classes,
                rawText = text.take(2000)
            )
        } catch (e: Exception) {
            PdfAnalysisResult(
                success = false,
                analysisType = "CLASSES",
                rawText = text.take(2000),
                error = "Failed to parse classes: ${e.message}"
            )
        }
    }

    /**
     * Analyze text for items.
     */
    private suspend fun analyzeItems(text: String): PdfAnalysisResult {
        val prompt = """
You are analyzing a document describing items for a fantasy RPG game.

Here is the text:
```
${text.take(8000)}
```

Extract all items mentioned. For each item:
1. Name of the item
2. Description of what it is/does
3. Item type (weapon, armor, consumable, misc)
4. Any special features or magical properties

Respond with valid JSON only in this exact format:
{
  "items": [
    {
      "name": "Item Name",
      "description": "Item description",
      "itemType": "weapon",
      "features": ["feature1", "feature2"]
    }
  ]
}
        """.trimIndent()

        return try {
            val response = callLlm(prompt)
            val items = parseItemsResponse(response)
            PdfAnalysisResult(
                success = true,
                analysisType = "ITEMS",
                items = items,
                rawText = text.take(2000)
            )
        } catch (e: Exception) {
            PdfAnalysisResult(
                success = false,
                analysisType = "ITEMS",
                rawText = text.take(2000),
                error = "Failed to parse items: ${e.message}"
            )
        }
    }

    /**
     * Analyze text for creatures/monsters.
     */
    private suspend fun analyzeCreatures(text: String): PdfAnalysisResult {
        val prompt = """
You are analyzing a document describing creatures/monsters for a fantasy RPG game.

Here is the text:
```
${text.take(8000)}
```

Extract all creatures mentioned. For each creature:
1. Name of the creature
2. Description of appearance and behavior
3. Suggested level (1-20)
4. Whether it's aggressive by default
5. Any special abilities it has

Respond with valid JSON only in this exact format:
{
  "creatures": [
    {
      "name": "Creature Name",
      "description": "Creature description",
      "level": 5,
      "isAggressive": true,
      "abilities": ["ability1", "ability2"]
    }
  ]
}
        """.trimIndent()

        return try {
            val response = callLlm(prompt)
            val creatures = parseCreaturesResponse(response)
            PdfAnalysisResult(
                success = true,
                analysisType = "CREATURES",
                creatures = creatures,
                rawText = text.take(2000)
            )
        } catch (e: Exception) {
            PdfAnalysisResult(
                success = false,
                analysisType = "CREATURES",
                rawText = text.take(2000),
                error = "Failed to parse creatures: ${e.message}"
            )
        }
    }

    /**
     * Analyze text for standalone abilities.
     */
    private suspend fun analyzeAbilities(text: String): PdfAnalysisResult {
        val prompt = """
You are analyzing a document describing abilities/spells for a fantasy RPG game.

Here is the text:
```
${text.take(8000)}
```

Extract all abilities/spells mentioned. For each ability:
1. Name of the ability
2. Description of what it does
3. Type (spell, combat, utility, passive)
4. Target type (self, single_enemy, single_ally, area, all_enemies, all_allies)
5. Cooldown type (none, short, medium, long)

Respond with valid JSON only in this exact format:
{
  "abilities": [
    {
      "name": "Ability Name",
      "description": "What the ability does",
      "abilityType": "spell",
      "targetType": "single_enemy",
      "cooldownType": "medium"
    }
  ]
}
        """.trimIndent()

        return try {
            val response = callLlm(prompt)
            val abilities = parseAbilitiesResponse(response)
            PdfAnalysisResult(
                success = true,
                analysisType = "ABILITIES",
                abilities = abilities,
                rawText = text.take(2000)
            )
        } catch (e: Exception) {
            PdfAnalysisResult(
                success = false,
                analysisType = "ABILITIES",
                rawText = text.take(2000),
                error = "Failed to parse abilities: ${e.message}"
            )
        }
    }

    /**
     * Call the LLM API.
     */
    private suspend fun callLlm(prompt: String): String {
        val request = OllamaRequest(
            model = llmModel,
            prompt = prompt,
            stream = false,
            format = "json"
        )

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

        return fullResponse.toString()
    }

    // Parse response helpers

    @Serializable
    private data class LocationsWrapper(val locations: List<ExtractedLocation>)

    @Serializable
    private data class ClassesWrapper(val classes: List<ExtractedClass>)

    @Serializable
    private data class ItemsWrapper(val items: List<ExtractedItem>)

    @Serializable
    private data class CreaturesWrapper(val creatures: List<ExtractedCreature>)

    @Serializable
    private data class AbilitiesWrapper(val abilities: List<ExtractedAbility>)

    private fun parseLocationsResponse(response: String): List<ExtractedLocation> {
        return try {
            json.decodeFromString<LocationsWrapper>(response).locations
        } catch (e: Exception) {
            // Try to extract JSON from response
            val jsonMatch = Regex("""\{[\s\S]*"locations"[\s\S]*\}""").find(response)
            if (jsonMatch != null) {
                json.decodeFromString<LocationsWrapper>(jsonMatch.value).locations
            } else {
                emptyList()
            }
        }
    }

    private fun parseClassesResponse(response: String): List<ExtractedClass> {
        return try {
            json.decodeFromString<ClassesWrapper>(response).classes
        } catch (e: Exception) {
            val jsonMatch = Regex("""\{[\s\S]*"classes"[\s\S]*\}""").find(response)
            if (jsonMatch != null) {
                json.decodeFromString<ClassesWrapper>(jsonMatch.value).classes
            } else {
                emptyList()
            }
        }
    }

    private fun parseItemsResponse(response: String): List<ExtractedItem> {
        return try {
            json.decodeFromString<ItemsWrapper>(response).items
        } catch (e: Exception) {
            val jsonMatch = Regex("""\{[\s\S]*"items"[\s\S]*\}""").find(response)
            if (jsonMatch != null) {
                json.decodeFromString<ItemsWrapper>(jsonMatch.value).items
            } else {
                emptyList()
            }
        }
    }

    private fun parseCreaturesResponse(response: String): List<ExtractedCreature> {
        return try {
            json.decodeFromString<CreaturesWrapper>(response).creatures
        } catch (e: Exception) {
            val jsonMatch = Regex("""\{[\s\S]*"creatures"[\s\S]*\}""").find(response)
            if (jsonMatch != null) {
                json.decodeFromString<CreaturesWrapper>(jsonMatch.value).creatures
            } else {
                emptyList()
            }
        }
    }

    private fun parseAbilitiesResponse(response: String): List<ExtractedAbility> {
        return try {
            json.decodeFromString<AbilitiesWrapper>(response).abilities
        } catch (e: Exception) {
            val jsonMatch = Regex("""\{[\s\S]*"abilities"[\s\S]*\}""").find(response)
            if (jsonMatch != null) {
                json.decodeFromString<AbilitiesWrapper>(jsonMatch.value).abilities
            } else {
                emptyList()
            }
        }
    }
}
