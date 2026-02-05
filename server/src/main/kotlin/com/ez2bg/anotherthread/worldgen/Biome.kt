package com.ez2bg.anotherthread.worldgen

/**
 * Biome types based on Whittaker diagram.
 * Each biome has terrain descriptors for name/description generation.
 */
enum class Biome(
    val displayName: String,
    val terrainWords: List<String>,
    val featureWords: List<String>,
    val colorHex: Long
) {
    // High elevation, low moisture
    SCORCHED("Scorched Badlands",
        listOf("charred", "blackened", "ash-covered", "volcanic"),
        listOf("lava flows", "obsidian shards", "sulfur vents"),
        0xFF555555),
    BARE("Bare Rock",
        listOf("barren", "rocky", "wind-swept", "exposed"),
        listOf("scattered boulders", "gravel beds", "stone outcrops"),
        0xFF888888),
    TUNDRA("Frozen Tundra",
        listOf("frozen", "icy", "windswept", "desolate"),
        listOf("permafrost", "lichen", "scattered stones"),
        0xFFDDDDBB),
    SNOW("Snow Fields",
        listOf("snow-covered", "pristine", "frozen", "gleaming"),
        listOf("snowdrifts", "ice crystals", "frozen pools"),
        0xFFFFFFFF),

    // Mid-high elevation
    TEMPERATE_DESERT("Temperate Desert",
        listOf("arid", "sandy", "dry", "sun-baked"),
        listOf("sand dunes", "cacti", "dried brush"),
        0xFFE4E8CA),
    SHRUBLAND("Shrubland",
        listOf("scrubby", "dry", "sparse", "brushy"),
        listOf("thorny bushes", "dry grass", "scattered shrubs"),
        0xFFC4CCBB),
    TAIGA("Taiga Forest",
        listOf("coniferous", "cold", "pine-scented", "northern"),
        listOf("pine trees", "fir groves", "frozen streams"),
        0xFFCCD4BB),

    // Mid elevation
    GRASSLAND("Grassland",
        listOf("grassy", "open", "rolling", "windswept"),
        listOf("tall grass", "wildflowers", "grazing grounds"),
        0xFF88AA55),
    TEMPERATE_DECIDUOUS_FOREST("Deciduous Forest",
        listOf("leafy", "dappled", "verdant", "autumnal"),
        listOf("oak trees", "maple groves", "fallen leaves"),
        0xFF679459),
    TEMPERATE_RAIN_FOREST("Temperate Rainforest",
        listOf("lush", "moss-covered", "misty", "ancient"),
        listOf("giant ferns", "moss-draped trees", "mushrooms"),
        0xFF448855),

    // Low elevation
    SUBTROPICAL_DESERT("Subtropical Desert",
        listOf("scorching", "sandy", "oasis-dotted", "endless"),
        listOf("palm oases", "mirages", "sand storms"),
        0xFFD2B98B),
    TROPICAL_SEASONAL_FOREST("Seasonal Forest",
        listOf("tropical", "seasonal", "lush", "vibrant"),
        listOf("exotic flowers", "fruit trees", "colorful birds"),
        0xFF559944),
    TROPICAL_RAIN_FOREST("Tropical Rainforest",
        listOf("dense", "humid", "teeming", "primordial"),
        listOf("jungle vines", "exotic wildlife", "waterfalls"),
        0xFF337755),

    // Water and coast
    OCEAN("Open Ocean",
        listOf("vast", "deep", "endless", "blue"),
        listOf("waves", "sea creatures", "distant ships"),
        0xFF4488AA),
    LAKE("Lake",
        listOf("calm", "reflective", "serene", "deep"),
        listOf("water lilies", "fish", "reeds"),
        0xFF5588AA),
    COAST("Coast",
        listOf("sandy", "wave-lapped", "salty", "breezy"),
        listOf("shells", "driftwood", "tidal pools"),
        0xFFD0C8A0),
    MARSH("Marsh",
        listOf("boggy", "murky", "reedy", "misty"),
        listOf("cattails", "frogs", "stagnant pools"),
        0xFF4A6A5A),

    // Special terrain
    RIVER("River",
        listOf("flowing", "rushing", "meandering", "clear"),
        listOf("rapids", "fish", "river stones"),
        0xFF6B8E9F),
    MOUNTAIN("Mountain Peak",
        listOf("towering", "craggy", "majestic", "snow-capped"),
        listOf("cliffs", "eagles", "mountain goats"),
        0xFF7A7A9A),
    HILLS("Rolling Hills",
        listOf("rolling", "gentle", "pastoral", "green"),
        listOf("sheep", "stone walls", "windmills"),
        0xFF6B8F65);

    fun randomTerrainWord(): String = terrainWords.random()
    fun randomFeatureWord(): String = featureWords.random()
}
