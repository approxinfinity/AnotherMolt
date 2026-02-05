package com.ez2bg.anotherthread.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ez2bg.anotherthread.api.*
import com.ez2bg.anotherthread.storage.AuthStorage
import com.ez2bg.anotherthread.ui.BackgroundImageGenerationManager
import com.ez2bg.anotherthread.ui.EntityImage
import kotlinx.coroutines.launch

private val DarkBackground = Color(0xFF1A1A2E)
private val AccentColor = Color(0xFF6366F1)
private val LightBlue = Color(0xFFBFDBFE)
private val SurfaceColor = Color.White.copy(alpha = 0.05f)

/**
 * Full-screen dark-themed character creation screen.
 * Shown after onboarding authentication for new users.
 */
@Composable
fun CharacterCreationScreen(
    user: UserDto,
    onComplete: (UserDto) -> Unit,
    onExploreAsGhost: () -> Unit
) {
    var desc by remember { mutableStateOf(user.desc) }
    var imageUrl by remember { mutableStateOf(user.imageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Generation options
    var genClassChecked by remember { mutableStateOf(true) }
    var genImageChecked by remember { mutableStateOf(true) }

    // Stat derivation state
    var derivedAttributes by remember { mutableStateOf<DerivedAttributesDto?>(null) }
    var isDerivingStats by remember { mutableStateOf(false) }
    var statsCommitted by remember { mutableStateOf(user.attributesGeneratedAt != null) }
    var followUpAnswers by remember { mutableStateOf<MutableMap<String, String>>(mutableMapOf()) }

    // Class assignment state
    var assignedClass by remember { mutableStateOf<CharacterClassDto?>(null) }
    var characterClassId by remember { mutableStateOf(user.characterClassId) }
    var classAbilities by remember { mutableStateOf<List<AbilityDto>>(emptyList()) }
    var abilitiesExpanded by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(user) }

    // Collect class generation status
    val classGenerationStatus by AsyncOperationRepository.classGenerationStatus.collectAsState()
    val isClassGenerating = user.id in classGenerationStatus

    // Track image generation
    val generatingEntities by BackgroundImageGenerationManager.generatingEntities.collectAsState()
    val isImageGenerating = user.id in generatingEntities

    // Resume polling on load if class generation was in progress
    LaunchedEffect(user.id) {
        val startedAt = user.classGenerationStartedAt
        if (startedAt != null && user.characterClassId == null) {
            AsyncOperationRepository.resumeClassGenerationPolling(user.id, startedAt)
        }
    }

    // Collect class generation completions
    LaunchedEffect(user.id) {
        AsyncOperationRepository.classGenerationCompletions.collect { result ->
            if (result.userId == user.id) {
                if (result.success) {
                    characterClassId = result.user?.characterClassId
                    assignedClass = result.characterClass
                    result.user?.let { updatedUser ->
                        AuthStorage.saveUser(updatedUser)
                        currentUser = updatedUser
                    }
                    message = "Class generated!"
                } else {
                    message = result.errorMessage ?: "Class generation failed. Please try again."
                }
            }
        }
    }

    // Fetch assigned class details and abilities
    LaunchedEffect(characterClassId) {
        val classId = characterClassId
        if (classId != null) {
            ApiClient.getCharacterClass(classId).onSuccess { fetchedClass ->
                assignedClass = fetchedClass
            }
            ApiClient.getAbilitiesByClass(classId).onSuccess { abilities ->
                classAbilities = abilities.sortedBy { it.name.lowercase() }
            }
        }
    }

    // Check if character is complete (has class and not generating)
    val isCharacterComplete = characterClassId != null && !isClassGenerating

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Create Your Character",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome, ${user.name}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Character image
            Surface(
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceColor
            ) {
                EntityImage(
                    imageUrl = imageUrl,
                    contentDescription = "Character image",
                    isGenerating = isImageGenerating
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content area (scrollable part)
            Surface(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(),
                color = SurfaceColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Show creation form if no class assigned
                    if (characterClassId == null) {
                        // Description field
                        OutlinedTextField(
                            value = desc,
                            onValueChange = {
                                desc = it
                                // Reset derived stats when description changes
                                if (derivedAttributes != null) {
                                    derivedAttributes = null
                                    statsCommitted = false
                                }
                            },
                            label = { Text("Describe your character") },
                            placeholder = { Text("A mysterious wanderer with keen eyes and strong arms...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            enabled = !isLoading && !isClassGenerating && !isDerivingStats,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedBorderColor = LightBlue.copy(alpha = 0.5f),
                                focusedBorderColor = LightBlue,
                                unfocusedLabelColor = LightBlue.copy(alpha = 0.7f),
                                focusedLabelColor = LightBlue,
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                cursorColor = LightBlue
                            )
                        )

                        // Helper text
                        Text(
                            text = "Your description determines your D&D stats and class. Write vividly for bonus stat points!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        // Step 1: Derive Stats button (if no stats derived yet)
                        if (derivedAttributes == null && !statsCommitted) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isDerivingStats = true
                                        message = null
                                        ApiClient.deriveAttributes(
                                            userId = user.id,
                                            description = desc,
                                            followUpAnswers = followUpAnswers.toMap()
                                        ).onSuccess { attrs ->
                                            derivedAttributes = attrs
                                            message = null
                                        }.onFailure { error ->
                                            message = "Error deriving stats: ${error.message}"
                                        }
                                        isDerivingStats = false
                                    }
                                },
                                enabled = !isDerivingStats && desc.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                            ) {
                                if (isDerivingStats) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isDerivingStats) "Analyzing description..." else "Derive Stats")
                            }
                        }

                        // Step 2: Show derived stats preview
                        derivedAttributes?.let { attrs ->
                            StatPreviewCard(attrs)

                            // Follow-up questions for missing areas
                            if (attrs.missingAreas.isNotEmpty() && !statsCommitted) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B69).copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Tell us more...",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LightBlue
                                        )
                                        attrs.missingAreas.forEach { area ->
                                            val question = when (area) {
                                                "physical" -> "How would you describe your character's physical build and agility?"
                                                "mental" -> "How sharp is your character's mind? Are they scholarly, street-smart, or intuitive?"
                                                "social" -> "How does your character interact with others? Are they charismatic, intimidating, or reserved?"
                                                else -> return@forEach
                                            }
                                            OutlinedTextField(
                                                value = followUpAnswers[area] ?: "",
                                                onValueChange = { followUpAnswers = followUpAnswers.toMutableMap().also { m -> m[area] = it } },
                                                label = { Text(area.replaceFirstChar { c -> c.uppercase() }) },
                                                placeholder = { Text(question) },
                                                modifier = Modifier.fillMaxWidth(),
                                                minLines = 2,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedTextColor = Color.White,
                                                    focusedTextColor = Color.White,
                                                    unfocusedBorderColor = LightBlue.copy(alpha = 0.3f),
                                                    focusedBorderColor = LightBlue,
                                                    unfocusedLabelColor = LightBlue.copy(alpha = 0.7f),
                                                    focusedLabelColor = LightBlue,
                                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                                    cursorColor = LightBlue
                                                )
                                            )
                                        }

                                        // Re-derive with follow-up answers
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isDerivingStats = true
                                                    ApiClient.deriveAttributes(
                                                        userId = user.id,
                                                        description = desc,
                                                        followUpAnswers = followUpAnswers.toMap()
                                                    ).onSuccess { newAttrs ->
                                                        derivedAttributes = newAttrs
                                                    }.onFailure { error ->
                                                        message = "Error: ${error.message}"
                                                    }
                                                    isDerivingStats = false
                                                }
                                            },
                                            enabled = !isDerivingStats && followUpAnswers.values.any { it.isNotBlank() },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor.copy(alpha = 0.8f))
                                        ) {
                                            Text("Refine Stats")
                                        }
                                    }
                                }
                            }

                            // Step 3: Confirm & Create button
                            if (!statsCommitted) {
                                // Generation checkboxes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = genClassChecked,
                                            onCheckedChange = { genClassChecked = it },
                                            enabled = !isLoading && !isClassGenerating,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = LightBlue,
                                                uncheckedColor = LightBlue.copy(alpha = 0.5f),
                                                checkmarkColor = DarkBackground
                                            )
                                        )
                                        Text(
                                            text = "Generate Class",
                                            color = Color.White,
                                            modifier = Modifier.clickable(enabled = !isLoading && !isClassGenerating) {
                                                genClassChecked = !genClassChecked
                                            }
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = genImageChecked,
                                            onCheckedChange = { genImageChecked = it },
                                            enabled = !isLoading && !isClassGenerating && !isImageGenerating,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = LightBlue,
                                                uncheckedColor = LightBlue.copy(alpha = 0.5f),
                                                checkmarkColor = DarkBackground
                                            )
                                        )
                                        Text(
                                            text = "Generate Image",
                                            color = Color.White,
                                            modifier = Modifier.clickable(enabled = !isLoading && !isClassGenerating && !isImageGenerating) {
                                                genImageChecked = !genImageChecked
                                            }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            message = null

                                            // Commit attributes
                                            ApiClient.commitAttributes(user.id, attrs).onSuccess { updatedUser ->
                                                AuthStorage.saveUser(updatedUser)
                                                currentUser = updatedUser
                                                statsCommitted = true

                                                // Save profile description
                                                val request = UpdateUserRequest(
                                                    desc = desc,
                                                    itemIds = user.itemIds,
                                                    featureIds = user.featureIds
                                                )
                                                ApiClient.updateUser(user.id, request).onSuccess { u ->
                                                    AuthStorage.saveUser(u)
                                                    currentUser = u
                                                }

                                                // Trigger image generation
                                                if (genImageChecked && desc.isNotBlank()) {
                                                    BackgroundImageGenerationManager.startGeneration(
                                                        entityType = "user",
                                                        entityId = user.id,
                                                        name = user.name,
                                                        description = desc,
                                                        featureIds = user.featureIds
                                                    )
                                                }

                                                // Start class generation
                                                if (desc.isNotBlank()) {
                                                    message = "Stats confirmed! Creating your class..."
                                                    AsyncOperationRepository.startClassGeneration(
                                                        userId = user.id,
                                                        characterDescription = desc,
                                                        generateNew = genClassChecked
                                                    )
                                                }
                                            }.onFailure { error ->
                                                message = "Error: ${error.message}"
                                            }
                                            isLoading = false
                                        }
                                    },
                                    enabled = !isLoading && !isClassGenerating,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(if (isLoading) "Creating..." else "Confirm Stats & Create Character")
                                }
                            }
                        }
                    }

                    // Class generation in progress
                    if (isClassGenerating && assignedClass == null) {
                        val statusMessage = classGenerationStatus[user.id]?.message ?: "Generating class..."
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AccentColor.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = LightBlue
                                )
                                Column {
                                    Text(
                                        text = statusMessage,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "You can explore the world while you wait!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Explore as Ghost button
                        OutlinedButton(
                            onClick = onExploreAsGhost,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explore as Ghost")
                        }
                    }

                    // Show assigned class when complete
                    assignedClass?.let { charClass ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AccentColor.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = LightBlue
                                    )
                                    Text(
                                        text = "Your Class",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = charClass.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = if (charClass.isSpellcaster) "Spellcaster" else "Martial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightBlue
                                )
                                Text(
                                    text = charClass.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Abilities section
                        if (classAbilities.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { abilitiesExpanded = !abilitiesExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (charClass.isSpellcaster) "Spells (${classAbilities.size})" else "Abilities (${classAbilities.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = if (abilitiesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (abilitiesExpanded) "Collapse" else "Expand",
                                            tint = Color.White
                                        )
                                    }

                                    if (abilitiesExpanded) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            classAbilities.forEach { ability ->
                                                AbilityCard(ability)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Message display
                    message?.let {
                        Text(
                            text = it,
                            color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error else LightBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Begin Adventure button (only when complete)
            if (isCharacterComplete) {
                Button(
                    onClick = { onComplete(currentUser) },
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Begin Adventure", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatPreviewCard(attrs: DerivedAttributesDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with quality bonus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Attributes",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                val bonusColor = when {
                    attrs.qualityBonus > 0 -> Color(0xFF4ADE80)
                    attrs.qualityBonus < 0 -> Color(0xFFF87171)
                    else -> Color.White.copy(alpha = 0.6f)
                }
                val bonusText = when {
                    attrs.qualityBonus > 0 -> "+${attrs.qualityBonus} bonus"
                    attrs.qualityBonus < 0 -> "${attrs.qualityBonus} penalty"
                    else -> "standard"
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bonusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = bonusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = bonusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 2x3 stat grid
            val stats = listOf(
                "STR" to attrs.strength,
                "DEX" to attrs.dexterity,
                "CON" to attrs.constitution,
                "INT" to attrs.intelligence,
                "WIS" to attrs.wisdom,
                "CHA" to attrs.charisma
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in stats.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (name, value) ->
                            StatBox(name, value, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Total and reasoning
            val total = attrs.strength + attrs.dexterity + attrs.constitution +
                attrs.intelligence + attrs.wisdom + attrs.charisma
            Text(
                text = "Total: $total points (base 72 ${if (attrs.qualityBonus >= 0) "+" else ""}${attrs.qualityBonus})",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = attrs.reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatBox(name: String, value: Int, modifier: Modifier = Modifier) {
    val mod = (value - 10) / 2
    val modStr = if (mod >= 0) "+$mod" else "$mod"
    val statColor = when {
        value >= 16 -> Color(0xFF4ADE80)
        value >= 13 -> Color(0xFF60A5FA)
        value >= 9 -> Color.White
        else -> Color(0xFFF87171)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = statColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "($modStr)",
                style = MaterialTheme.typography.bodySmall,
                color = statColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AbilityCard(ability: AbilityDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ability.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AccentColor.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Cost: ${ability.powerCost}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ability.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
