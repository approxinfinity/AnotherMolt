package com.ez2bg.anotherthread.database

/**
 * Seed data for spell categories and example spell features.
 * Spells are implemented as Features with JSON data in the `data` field.
 */
object SpellFeatureSeed {

    // Well-known category IDs for reference
    const val CATEGORY_SPELLS = "spell-category-root"
    const val CATEGORY_COMBAT_SPELLS = "spell-category-combat"
    const val CATEGORY_UTILITY_SPELLS = "spell-category-utility"
    const val CATEGORY_PASSIVE_ABILITIES = "spell-category-passive"

    /**
     * Seeds spell categories and example spells if they don't exist.
     */
    fun seedIfEmpty() {
        val existingCategories = FeatureCategoryRepository.findAll()
        val existingCategoryNames = existingCategories.map { it.name }.toSet()

        // Seed categories
        if ("Spells" !in existingCategoryNames) {
            seedSpellCategories()
        }

        // Seed example utility spells
        val existingFeatures = FeatureRepository.findAll()
        val existingFeatureNames = existingFeatures.map { it.name }.toSet()

        if ("Phase Walk" !in existingFeatureNames) {
            seedExampleUtilitySpells()
        }
    }

    private fun seedSpellCategories() {
        // Parent category for all spells
        FeatureCategoryRepository.create(
            FeatureCategory(
                id = CATEGORY_SPELLS,
                name = "Spells",
                description = "Magical abilities that can be cast by characters. Includes combat spells, utility spells, and passive abilities."
            )
        )

        // Combat spells subcategory
        FeatureCategoryRepository.create(
            FeatureCategory(
                id = CATEGORY_COMBAT_SPELLS,
                name = "Combat Spells",
                description = "Spells used during combat for damage, healing, buffs, debuffs, and status effects."
            )
        )

        // Utility spells subcategory
        FeatureCategoryRepository.create(
            FeatureCategory(
                id = CATEGORY_UTILITY_SPELLS,
                name = "Utility Spells",
                description = "Non-combat spells for movement, detection, stealth, and environmental manipulation."
            )
        )

        // Passive abilities subcategory
        FeatureCategoryRepository.create(
            FeatureCategory(
                id = CATEGORY_PASSIVE_ABILITIES,
                name = "Passive Abilities",
                description = "Always-active abilities that provide ongoing benefits based on triggers or conditions."
            )
        )

        println("Seeded spell categories")
    }

    private fun seedExampleUtilitySpells() {
        // Phase Walk - move through walls to adjacent tile
        FeatureRepository.create(
            Feature(
                name = "Phase Walk",
                description = "Shift your body partially into the ethereal plane, allowing you to pass through solid matter. Move to any adjacent tile regardless of exits or obstacles.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "phase_walk",
                        "params": {
                            "range": 1,
                            "ignoresExits": true,
                            "ignoresTerrain": ["wall", "door", "barrier"]
                        }
                    },
                    "cooldown": {
                        "type": "uses_per_day",
                        "value": 3
                    },
                    "cost": {
                        "mana": 25
                    },
                    "requirements": {
                        "level": 5
                    }
                }"""
            )
        )

        // Detect Secret - reveal hidden exits and traps
        FeatureRepository.create(
            Feature(
                name = "Detect Secret",
                description = "Attune your senses to the hidden and concealed. Reveals secret doors, hidden exits, traps, and invisible creatures within range.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "detect_secret",
                        "params": {
                            "range": 30,
                            "reveals": ["hidden_exit", "trap", "invisible_creature", "illusion"],
                            "duration": 60
                        }
                    },
                    "cooldown": {
                        "type": "seconds",
                        "value": 120
                    },
                    "cost": {
                        "mana": 10
                    },
                    "requirements": {
                        "level": 1
                    }
                }"""
            )
        )

        // Invisibility - avoid detection
        FeatureRepository.create(
            Feature(
                name = "Invisibility",
                description = "Bend light around your form, becoming completely invisible. Movement and observation are unhindered, but the effect breaks when you attack or cast another spell.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "invisibility",
                        "params": {
                            "target": "self",
                            "duration": 300,
                            "breaksOn": ["attack", "cast_spell", "interact_hostile"]
                        }
                    },
                    "cooldown": {
                        "type": "seconds",
                        "value": 600
                    },
                    "cost": {
                        "mana": 35
                    },
                    "requirements": {
                        "level": 3
                    }
                }"""
            )
        )

        // Levitate - access UP/DOWN without stairs
        FeatureRepository.create(
            Feature(
                name = "Levitate",
                description = "Rise gently into the air, gaining the ability to move vertically. Access UP and DOWN exits regardless of stairs, ladders, or other physical means.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "levitate",
                        "params": {
                            "verticalAccess": true,
                            "duration": 600,
                            "maxAltitude": 100
                        }
                    },
                    "cooldown": {
                        "type": "seconds",
                        "value": 300
                    },
                    "cost": {
                        "mana": 20
                    },
                    "requirements": {
                        "level": 3
                    }
                }"""
            )
        )

        // Teleport - jump to known location
        FeatureRepository.create(
            Feature(
                name = "Teleport",
                description = "Tear a momentary rift in space to transport yourself to a location you have previously visited. The more familiar you are with the destination, the safer the journey.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "teleport",
                        "params": {
                            "targetType": "known_location",
                            "maxDistance": null,
                            "castTime": 10,
                            "familiarityBonus": true
                        }
                    },
                    "cooldown": {
                        "type": "seconds",
                        "value": 3600
                    },
                    "cost": {
                        "mana": 50
                    },
                    "requirements": {
                        "level": 9
                    }
                }"""
            )
        )

        // Recall - return to bind point
        FeatureRepository.create(
            Feature(
                name = "Recall",
                description = "Invoke your bond to your sanctuary, instantly transporting you back to your designated home or bind point. Cannot be used in combat.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "recall",
                        "params": {
                            "targetType": "bind_point",
                            "castTime": 10,
                            "interruptedByCombat": true
                        }
                    },
                    "cooldown": {
                        "type": "seconds",
                        "value": 1800
                    },
                    "cost": {
                        "mana": 30
                    },
                    "requirements": {
                        "level": 1
                    }
                }"""
            )
        )

        // Light - illuminate dark areas
        FeatureRepository.create(
            Feature(
                name = "Light",
                description = "Conjure a floating orb of soft light that illuminates the surrounding area. The light follows you and reveals details hidden in darkness.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "light",
                        "params": {
                            "radius": 40,
                            "duration": 3600,
                            "follows": true,
                            "brightness": "normal"
                        }
                    },
                    "cooldown": {
                        "type": "none",
                        "value": 0
                    },
                    "cost": {
                        "mana": 5
                    },
                    "requirements": {
                        "level": 1
                    }
                }"""
            )
        )

        // Unlock - open locked doors/chests
        FeatureRepository.create(
            Feature(
                name = "Unlock",
                description = "Channel magical energy into a lock mechanism, causing it to spring open. More powerful locks require greater skill to overcome.",
                featureCategoryId = CATEGORY_UTILITY_SPELLS,
                data = """{
                    "spellType": "utility",
                    "utility": {
                        "action": "unlock",
                        "params": {
                            "maxDifficulty": "hard",
                            "breaksLock": false,
                            "range": 5
                        }
                    },
                    "cooldown": {
                        "type": "seconds",
                        "value": 60
                    },
                    "cost": {
                        "mana": 15
                    },
                    "requirements": {
                        "level": 2
                    }
                }"""
            )
        )

        println("Seeded 8 example utility spells")
    }
}
