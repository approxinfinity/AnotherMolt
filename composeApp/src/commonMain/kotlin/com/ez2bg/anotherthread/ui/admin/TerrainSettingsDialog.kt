package com.ez2bg.anotherthread.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ez2bg.anotherthread.api.DesertParamsDto
import com.ez2bg.anotherthread.api.ForestParamsDto
import com.ez2bg.anotherthread.api.GrassParamsDto
import com.ez2bg.anotherthread.api.HillsParamsDto
import com.ez2bg.anotherthread.api.LakeParamsDto
import com.ez2bg.anotherthread.api.LocationDto
import com.ez2bg.anotherthread.api.MountainParamsDto
import com.ez2bg.anotherthread.api.RiverParamsDto
import com.ez2bg.anotherthread.api.StreamParamsDto
import com.ez2bg.anotherthread.api.SwampParamsDto
import com.ez2bg.anotherthread.api.TerrainOverridesDto
import com.ez2bg.anotherthread.api.UserDto
import com.ez2bg.anotherthread.ui.terrain.TerrainType
import com.ez2bg.anotherthread.ui.terrain.calculateElevationFromTerrain
import com.ez2bg.anotherthread.ui.terrain.elevationDescription
import com.ez2bg.anotherthread.ui.terrain.parseTerrainFromDescription

/**
 * Dialog for adjusting terrain rendering parameters for a location
 */
@Composable
fun TerrainSettingsDialog(
    location: LocationDto,
    currentOverrides: TerrainOverridesDto?,
    onDismiss: () -> Unit,
    onSave: (TerrainOverridesDto) -> Unit,
    onReset: () -> Unit,
    currentUser: UserDto? = null
) {
    val isNotAuthenticated = currentUser == null
    val isLocked = location.lockedBy != null
    val isDisabled = isNotAuthenticated || isLocked
    // Parse what terrains this location has
    val detectedTerrains = remember(location) {
        parseTerrainFromDescription(location.desc, location.name)
    }

    // Local state for editing
    var overrides by remember(currentOverrides) {
        mutableStateOf(currentOverrides ?: TerrainOverridesDto())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Terrain Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Location name
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = if (isDisabled) 8.dp else 16.dp)
                )

                // Disabled indicator (not authenticated or locked)
                if (isDisabled) {
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isNotAuthenticated) "Not authenticated" else "Locked",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isNotAuthenticated) "Login required to edit settings"
                                   else "Locked by ${location.lockedBy} - settings are read-only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Elevation setting (always shown)
                    val autoElevation = calculateElevationFromTerrain(detectedTerrains)
                    TerrainSection(title = "Elevation") {
                        Text(
                            text = "Auto-detected: ${((autoElevation * 100).toInt() / 100f)} (${elevationDescription(autoElevation)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        SliderWithLabel(
                            label = "Override Elevation",
                            value = overrides.elevation ?: autoElevation,
                            valueRange = -1f..1f,
                            onValueChange = { value ->
                                overrides = overrides.copy(elevation = value)
                            },
                            enabled = !isDisabled
                        )
                        Text(
                            text = elevationDescription(overrides.elevation ?: autoElevation),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (detectedTerrains.isEmpty()) {
                        Text(
                            text = "No adjustable terrain detected for this location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                    // Forest settings
                    if (TerrainType.FOREST in detectedTerrains) {
                        TerrainSection(title = "Forest") {
                            SliderWithLabel(
                                label = "Tree Count",
                                value = (overrides.forest?.treeCount ?: 7).toFloat(),
                                valueRange = 3f..15f,
                                steps = 12,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        forest = (overrides.forest ?: ForestParamsDto()).copy(
                                            treeCount = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Tree Size",
                                value = overrides.forest?.sizeMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        forest = (overrides.forest ?: ForestParamsDto()).copy(
                                            sizeMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Lake settings
                    if (TerrainType.LAKE in detectedTerrains) {
                        // Determine if X/Y are linked (both null, both equal, or legacy diameterMultiplier is set)
                        val lakeXYLinked = overrides.lake?.let { lake ->
                            val hasLegacy = lake.diameterMultiplier != null
                            val xEqualsY = lake.diameterMultiplierX == lake.diameterMultiplierY
                            hasLegacy || (lake.diameterMultiplierX == null && lake.diameterMultiplierY == null) || xEqualsY
                        } ?: true

                        TerrainSection(title = "Lake") {
                            // Link X/Y checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Checkbox(
                                    checked = lakeXYLinked,
                                    onCheckedChange = { linked ->
                                        val currentX = overrides.lake?.diameterMultiplierX ?: overrides.lake?.diameterMultiplier ?: 1f
                                        val currentY = overrides.lake?.diameterMultiplierY ?: overrides.lake?.diameterMultiplier ?: 1f
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = if (linked) currentX else null,
                                                diameterMultiplierX = if (linked) null else currentX,
                                                diameterMultiplierY = if (linked) null else currentY
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                Text("Link X/Y Size", style = MaterialTheme.typography.bodySmall)
                            }

                            if (lakeXYLinked) {
                                // Single size slider when linked
                                SliderWithLabel(
                                    label = "Lake Size",
                                    value = overrides.lake?.diameterMultiplier ?: overrides.lake?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = value,
                                                diameterMultiplierX = null,
                                                diameterMultiplierY = null
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            } else {
                                // Separate X and Y sliders when unlinked
                                SliderWithLabel(
                                    label = "Width (X)",
                                    value = overrides.lake?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = null,
                                                diameterMultiplierX = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                SliderWithLabel(
                                    label = "Height (Y)",
                                    value = overrides.lake?.diameterMultiplierY ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            lake = (overrides.lake ?: LakeParamsDto()).copy(
                                                diameterMultiplier = null,
                                                diameterMultiplierY = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            }

                            SliderWithLabel(
                                label = "Shape Points",
                                value = (overrides.lake?.shapePoints ?: 20).toFloat(),
                                valueRange = 8f..32f,
                                steps = 24,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        lake = (overrides.lake ?: LakeParamsDto()).copy(
                                            shapePoints = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Shape Roughness",
                                value = overrides.lake?.noiseScale ?: 0.35f,
                                valueRange = 0f..1f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        lake = (overrides.lake ?: LakeParamsDto()).copy(
                                            noiseScale = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // River settings
                    if (TerrainType.RIVER in detectedTerrains) {
                        TerrainSection(title = "River") {
                            SliderWithLabel(
                                label = "River Width",
                                value = overrides.river?.widthMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        river = RiverParamsDto(widthMultiplier = value)
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Stream settings
                    if (TerrainType.STREAM in detectedTerrains) {
                        TerrainSection(title = "Stream") {
                            SliderWithLabel(
                                label = "Stream Width",
                                value = overrides.stream?.widthMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        stream = StreamParamsDto(widthMultiplier = value)
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Mountain settings
                    if (TerrainType.MOUNTAIN in detectedTerrains) {
                        TerrainSection(title = "Mountains") {
                            SliderWithLabel(
                                label = "Peak Count",
                                value = (overrides.mountain?.peakCount ?: 3).toFloat(),
                                valueRange = 1f..6f,
                                steps = 5,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        mountain = (overrides.mountain ?: MountainParamsDto()).copy(
                                            peakCount = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Mountain Height",
                                value = overrides.mountain?.heightMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        mountain = (overrides.mountain ?: MountainParamsDto()).copy(
                                            heightMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Hills settings
                    if (TerrainType.HILLS in detectedTerrains) {
                        TerrainSection(title = "Hills") {
                            SliderWithLabel(
                                label = "Hill Height",
                                value = overrides.hills?.heightMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        hills = HillsParamsDto(heightMultiplier = value)
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Grass settings
                    if (TerrainType.GRASS in detectedTerrains) {
                        TerrainSection(title = "Grass") {
                            SliderWithLabel(
                                label = "Tuft Count",
                                value = (overrides.grass?.tuftCount ?: 8).toFloat(),
                                valueRange = 3f..15f,
                                steps = 12,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        grass = GrassParamsDto(tuftCount = value.toInt())
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Desert settings
                    if (TerrainType.DESERT in detectedTerrains) {
                        TerrainSection(title = "Desert") {
                            SliderWithLabel(
                                label = "Dune Count",
                                value = (overrides.desert?.duneCount ?: 3).toFloat(),
                                valueRange = 1f..6f,
                                steps = 5,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        desert = (overrides.desert ?: DesertParamsDto()).copy(
                                            duneCount = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Dune Height",
                                value = overrides.desert?.heightMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        desert = (overrides.desert ?: DesertParamsDto()).copy(
                                            heightMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }

                    // Swamp settings
                    if (TerrainType.SWAMP in detectedTerrains) {
                        // Determine if X/Y are linked
                        val swampXYLinked = overrides.swamp?.let { swamp ->
                            val xEqualsY = swamp.diameterMultiplierX == swamp.diameterMultiplierY
                            (swamp.diameterMultiplierX == null && swamp.diameterMultiplierY == null) || xEqualsY
                        } ?: true

                        TerrainSection(title = "Swamp") {
                            SliderWithLabel(
                                label = "Density",
                                value = overrides.swamp?.densityMultiplier ?: 1f,
                                valueRange = 0.5f..2f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                            densityMultiplier = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )

                            // Link X/Y checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = swampXYLinked,
                                    onCheckedChange = { linked ->
                                        val currentX = overrides.swamp?.diameterMultiplierX ?: 1f
                                        val currentY = overrides.swamp?.diameterMultiplierY ?: 1f
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierX = if (linked) null else currentX,
                                                diameterMultiplierY = if (linked) null else currentY
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                Text("Link X/Y Size", style = MaterialTheme.typography.bodySmall)
                            }

                            if (swampXYLinked) {
                                // Single size slider when linked
                                SliderWithLabel(
                                    label = "Swamp Size",
                                    value = overrides.swamp?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierX = value,
                                                diameterMultiplierY = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            } else {
                                // Separate X and Y sliders when unlinked
                                SliderWithLabel(
                                    label = "Width (X)",
                                    value = overrides.swamp?.diameterMultiplierX ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierX = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                                SliderWithLabel(
                                    label = "Height (Y)",
                                    value = overrides.swamp?.diameterMultiplierY ?: 1f,
                                    valueRange = 0.5f..2f,
                                    onValueChange = { value ->
                                        overrides = overrides.copy(
                                            swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                                diameterMultiplierY = value
                                            )
                                        )
                                    },
                                    enabled = !isDisabled
                                )
                            }

                            SliderWithLabel(
                                label = "Shape Points",
                                value = (overrides.swamp?.shapePoints ?: 20).toFloat(),
                                valueRange = 8f..32f,
                                steps = 24,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                            shapePoints = value.toInt()
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                            SliderWithLabel(
                                label = "Shape Roughness",
                                value = overrides.swamp?.noiseScale ?: 0.35f,
                                valueRange = 0f..1f,
                                onValueChange = { value ->
                                    overrides = overrides.copy(
                                        swamp = (overrides.swamp ?: SwampParamsDto()).copy(
                                            noiseScale = value
                                        )
                                    )
                                },
                                enabled = !isDisabled
                            )
                        }
                    }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReset, enabled = !isDisabled) {
                        Text("Reset")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(overrides) }, enabled = !isDisabled) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
