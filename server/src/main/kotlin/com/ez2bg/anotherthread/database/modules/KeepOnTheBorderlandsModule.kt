package com.ez2bg.anotherthread.database.modules

import com.ez2bg.anotherthread.database.*

/**
 * The Keep on the Borderlands - Based on classic D&D Module B2 by Gary Gygax
 *
 * A fortified keep serving as the last bastion of civilization before the wild Borderlands.
 * This is a safe haven where adventurers can:
 * - Rest and recover at the Travelers Inn or Tavern
 * - Buy supplies from the Provisioner, Trader, and Smithy
 * - Seek healing at the Chapel
 * - Store valuables at the Loan Bank
 * - Hire mercenaries
 * - Get quests from the Castellan
 *
 * Key Areas:
 * - Outer Bailey: Main Gate, Towers, Inn, Tavern, Shops, Guild House
 * - Inner Bailey: Fortress, Chapel, Cavalry Stables, Inner Gatehouse
 *
 * Notable NPCs:
 * - The Castellan (ruler of the Keep)
 * - The Curate (head of the Chapel)
 * - The Bailiff (administrator)
 * - Captain and Corporal of the Watch
 * - Various merchants and innkeepers
 */
object KeepOnTheBorderlandsModule : AdventureModuleSeed() {

    override val moduleId = "keep-borderlands"
    override val moduleName = "The Keep on the Borderlands"
    override val moduleDescription = "A fortified keep on the edge of civilization, serving as a safe haven for adventurers heading into the dangerous Borderlands. Here you can rest, resupply, and prepare for expeditions into the nearby Caves of Chaos."
    override val attribution = "Inspired by D&D Module B2 - The Keep on the Borderlands by Gary Gygax (TSR, 1979)"
    override val recommendedLevelMin = 1
    override val recommendedLevelMax = 3

    override fun defineContent() {
        defineAbilities()
        defineItems()
        defineLootTables()
        defineCreatures()
        defineLocations()
    }

    // ==================== ABILITIES ====================
    private fun defineAbilities() {
        // Guard abilities
        ability("sword-strike") {
            name = "Sword Strike"
            description = "A disciplined strike with a well-maintained sword."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 8
        }

        ability("crossbow-shot") {
            name = "Crossbow Shot"
            description = "Fires a bolt from a heavy crossbow."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 60
            baseDamage = 10
        }

        ability("pole-arm-thrust") {
            name = "Pole Arm Thrust"
            description = "A powerful thrust with a pole arm, keeping enemies at bay."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 10
            baseDamage = 9
        }

        ability("shield-bash") {
            name = "Shield Bash"
            description = "Bashes an enemy with a sturdy shield, potentially stunning them."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 5
            effects = """[{"type":"condition","condition":"stunned","duration":1,"chance":0.25}]"""
        }

        ability("longbow-shot") {
            name = "Longbow Shot"
            description = "Fires an arrow with deadly accuracy from a military longbow."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 80
            baseDamage = 8
        }

        // Cleric/Healer abilities
        ability("cure-light-wounds") {
            name = "Cure Light Wounds"
            description = "Channels divine energy to heal minor wounds."
            abilityType = "healing"
            targetType = "single_ally"
            range = 5
            baseDamage = -12  // Negative = healing
            manaCost = 10
        }

        ability("bless") {
            name = "Bless"
            description = "Grants divine favor to allies, improving their attacks."
            abilityType = "buff"
            targetType = "area_ally"
            range = 30
            durationRounds = 10
            manaCost = 15
            effects = """[{"type":"buff","stat":"attack","modifier":2}]"""
        }

        ability("hold-person") {
            name = "Hold Person"
            description = "Paralyzes a humanoid with divine magic."
            abilityType = "control"
            targetType = "single_enemy"
            range = 30
            durationRounds = 5
            manaCost = 20
            effects = """[{"type":"condition","condition":"paralyzed","duration":5}]"""
        }

        ability("detect-magic") {
            name = "Detect Magic"
            description = "Senses magical auras in the area."
            abilityType = "utility"
            targetType = "area"
            range = 60
            manaCost = 5
        }

        // Snake Staff special ability
        ability("snake-staff-attack") {
            name = "Snake Staff Attack"
            description = "The staff transforms into a serpent that coils around and bites the target."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 10
            baseDamage = 7
            cooldownType = "long"
            cooldownRounds = 5
            effects = """[{"type":"damage","modifier":7},{"type":"condition","condition":"restrained","duration":2}]"""
        }

        // Merchant/civilian abilities
        ability("call-guards") {
            name = "Call Guards"
            description = "Shouts for help, summoning nearby guards."
            abilityType = "utility"
            targetType = "self"
            range = 100
            effects = """[{"type":"summon","creatureType":"guard","count":2}]"""
        }

        // Dog abilities
        ability("guard-dog-bite") {
            name = "Guard Dog Bite"
            description = "A trained guard dog's vicious bite."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 6
        }

        // Cavalry abilities
        ability("lance-charge") {
            name = "Lance Charge"
            description = "A devastating mounted charge with a lance."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 15
            baseDamage = 15
            cooldownType = "short"
            cooldownRounds = 3
        }
    }

    // ==================== ITEMS ====================
    private fun defineItems() {
        // === WEAPONS ===
        item("longsword") {
            name = "Longsword"
            description = "A well-forged longsword from the Keep's smithy."
            value = 15
            weight = 4
            weapon()
            stats(attack = 4)
        }

        item("shortsword") {
            name = "Shortsword"
            description = "A reliable shortsword, popular among guards and travelers."
            value = 10
            weight = 2
            weapon()
            stats(attack = 3)
        }

        item("dagger") {
            name = "Dagger"
            description = "A simple but effective dagger."
            value = 2
            weight = 1
            weapon()
            stats(attack = 2)
        }

        item("spear") {
            name = "Spear"
            description = "A wooden spear with an iron tip."
            value = 3
            weight = 3
            weapon()
            stats(attack = 3)
        }

        item("pole-arm") {
            name = "Pole Arm"
            description = "A long pole arm useful for keeping enemies at a distance."
            value = 7
            weight = 6
            weapon()
            stats(attack = 5)
        }

        item("crossbow") {
            name = "Heavy Crossbow"
            description = "A heavy crossbow favored by the Keep's tower guards."
            value = 25
            weight = 8
            weapon("off_hand")
            stats(attack = 5)
        }

        item("longbow") {
            name = "Longbow"
            description = "A military longbow used by the Keep's archers."
            value = 40
            weight = 3
            weapon("off_hand")
            stats(attack = 4)
        }

        item("mace") {
            name = "Mace"
            description = "A sturdy mace, favored by clerics."
            value = 5
            weight = 4
            weapon()
            stats(attack = 3)
        }

        item("hand-axe") {
            name = "Hand Axe"
            description = "A versatile hand axe, good for combat or utility."
            value = 4
            weight = 3
            weapon()
            stats(attack = 3)
        }

        item("battle-axe") {
            name = "Battle Axe"
            description = "A heavy battle axe wielded by experienced fighters."
            value = 7
            weight = 5
            weapon()
            stats(attack = 5)
        }

        // === MAGIC WEAPONS ===
        item("sword-plus-one") {
            name = "Sword +1"
            description = "A finely crafted longsword enchanted with magic. The blade gleams with a faint inner light."
            value = 500
            weight = 4
            weapon()
            stats(attack = 7)
        }

        item("magic-arrows") {
            name = "Magic Arrows +1"
            description = "A quiver of 20 arrows enchanted to strike true."
            value = 200
            weight = 1
        }

        item("dagger-plus-one") {
            name = "Dagger +1"
            description = "A magical dagger that seems to guide itself toward its target."
            value = 300
            weight = 1
            weapon()
            stats(attack = 5)
        }

        item("spear-plus-one") {
            name = "Spear +1"
            description = "A spear blessed by the Chapel, effective against evil creatures."
            value = 400
            weight = 3
            weapon()
            stats(attack = 6)
        }

        // === ARMOR ===
        item("leather-armor") {
            name = "Leather Armor"
            description = "Standard leather armor, providing basic protection."
            value = 5
            weight = 10
            armor("chest")
            stats(defense = 2)
        }

        item("chain-mail") {
            name = "Chain Mail"
            description = "Interlocking metal rings providing good protection."
            value = 40
            weight = 25
            armor("chest")
            stats(defense = 5)
        }

        item("plate-mail") {
            name = "Plate Mail"
            description = "Heavy plate armor worn by the Keep's elite guards."
            value = 60
            weight = 35
            armor("chest")
            stats(defense = 7)
        }

        item("shield") {
            name = "Shield"
            description = "A sturdy wooden shield reinforced with metal."
            value = 10
            weight = 6
            armor("off_hand")
            stats(defense = 2)
        }

        item("shield-plus-one") {
            name = "Shield +1"
            description = "A magical shield that deflects blows with supernatural ease."
            value = 400
            weight = 6
            armor("off_hand")
            stats(defense = 4)
        }

        item("plate-mail-plus-one") {
            name = "Plate Mail +1"
            description = "Enchanted plate armor worn by the Captain of the Guard."
            value = 800
            weight = 30
            armor("chest")
            stats(defense = 9)
        }

        // === ACCESSORIES ===
        item("ring-of-protection") {
            name = "Ring of Protection +1"
            description = "A silver ring that provides magical protection to its wearer."
            value = 500
            weight = 0
            accessory("finger")
            stats(defense = 2)
        }

        // === ADVENTURING GEAR (from Provisioner) ===
        item("torch") {
            name = "Torch"
            description = "A wooden torch that provides light for about an hour."
            value = 1
            weight = 1
        }

        item("rope-50ft") {
            name = "Rope (50 ft)"
            description = "Fifty feet of sturdy hemp rope."
            value = 1
            weight = 5
        }

        item("iron-rations") {
            name = "Iron Rations"
            description = "Preserved food that will last for weeks. One week's supply."
            value = 5
            weight = 5
        }

        item("standard-rations") {
            name = "Standard Rations"
            description = "Fresh food that will last about a week if kept cool."
            value = 3
            weight = 7
        }

        item("waterskin") {
            name = "Waterskin"
            description = "A leather waterskin that holds about a day's water."
            value = 1
            weight = 1
        }

        item("backpack") {
            name = "Backpack"
            description = "A leather backpack for carrying equipment."
            value = 2
            weight = 2
        }

        item("lantern") {
            name = "Lantern"
            description = "An oil lantern that provides steady light."
            value = 10
            weight = 2
        }

        item("oil-flask") {
            name = "Oil Flask"
            description = "A flask of oil for lanterns. Can also be used as a weapon."
            value = 2
            weight = 1
        }

        item("tinderbox") {
            name = "Tinderbox"
            description = "Flint, steel, and tinder for starting fires."
            value = 3
            weight = 1
        }

        item("thieves-tools") {
            name = "Thieves' Tools"
            description = "A set of lockpicks and other tools for... discrete entry."
            value = 35
            weight = 1
        }

        item("holy-water") {
            name = "Holy Water"
            description = "A vial of water blessed at the Chapel, effective against undead."
            value = 25
            weight = 1
        }

        item("wolvesbane") {
            name = "Wolvesbane"
            description = "A bundle of dried wolvesbane, said to repel lycanthropes."
            value = 10
            weight = 0
        }

        item("garlic") {
            name = "Garlic"
            description = "A braid of garlic cloves, said to ward off vampires."
            value = 5
            weight = 0
        }

        item("mirror-small") {
            name = "Small Mirror"
            description = "A small polished mirror, useful for looking around corners."
            value = 5
            weight = 0
        }

        item("iron-spikes") {
            name = "Iron Spikes (12)"
            description = "A dozen iron spikes for securing doors or climbing."
            value = 1
            weight = 3
        }

        item("wooden-pole") {
            name = "Wooden Pole (10 ft)"
            description = "A 10-foot wooden pole for probing traps and reaching high places."
            value = 1
            weight = 5
        }

        // === TRADER GOODS ===
        item("fur-trimmed-cape") {
            name = "Fur-Trimmed Cape"
            description = "A warm cape trimmed with fine fur."
            value = 75
            weight = 3
        }

        item("dagger-jeweled") {
            name = "Dagger with Jeweled Scabbard"
            description = "An ornate dagger with a gem-encrusted scabbard. More for show than combat."
            value = 600
            weight = 1
            weapon()
            stats(attack = 2)
        }

        item("crystal-decanter") {
            name = "Crystal Decanter"
            description = "A beautiful crystal decanter, perfect for fine wines."
            value = 45
            weight = 2
        }

        item("jade-ring") {
            name = "Jade Ring"
            description = "A ring carved from pure jade."
            value = 400
            weight = 0
            accessory("finger")
        }

        item("gold-silver-belt") {
            name = "Gold and Silver Belt"
            description = "An ornate belt of gold and silver filigree."
            value = 90
            weight = 1
        }

        // === JEWELRY (from Jewel Merchant) ===
        item("jeweled-bracelet") {
            name = "Jeweled Bracelet"
            description = "A gold bracelet set with small gems."
            value = 600
            weight = 0
            accessory("wrist")
        }

        item("pearl-necklace") {
            name = "Pearl Necklace"
            description = "A strand of matched pearls."
            value = 1200
            weight = 0
            accessory("neck")
        }

        item("gold-earrings") {
            name = "Gold Earrings"
            description = "A pair of finely crafted gold earrings."
            value = 300
            weight = 0
        }

        item("gem-ruby") {
            name = "Ruby"
            description = "A deep red ruby of excellent quality."
            value = 100
            weight = 0
        }

        item("gem-emerald") {
            name = "Emerald"
            description = "A vivid green emerald."
            value = 100
            weight = 0
        }

        item("gem-sapphire") {
            name = "Sapphire"
            description = "A brilliant blue sapphire."
            value = 100
            weight = 0
        }

        // === POTIONS (from Chapel) ===
        item("potion-healing") {
            name = "Potion of Healing"
            description = "A red potion that heals wounds when consumed."
            value = 50
            weight = 1
        }

        item("potion-cure-disease") {
            name = "Potion of Cure Disease"
            description = "A bitter potion that cures most diseases."
            value = 100
            weight = 1
        }

        // === FOOD & DRINK (from Tavern/Inn) ===
        item("ale-mug") {
            name = "Mug of Ale"
            description = "A hearty mug of the Keep's finest ale."
            value = 1
            weight = 1
        }

        item("wine-bottle") {
            name = "Bottle of Wine"
            description = "A bottle of decent wine from distant vineyards."
            value = 1
            weight = 2
        }

        item("honey-mead") {
            name = "Honey Mead"
            description = "Sweet honey mead, a favorite at the Tavern."
            value = 1
            weight = 1
        }

        item("roast-fowl") {
            name = "Roast Fowl"
            description = "A delicious roasted chicken, still hot."
            value = 1
            weight = 2
        }

        item("hot-pie") {
            name = "Hot Pie"
            description = "A meat pie fresh from the oven."
            value = 1
            weight = 1
        }

        // === SPECIAL ITEMS ===
        item("snake-staff") {
            name = "Snake Staff"
            description = "A magical staff that can transform into a living serpent on command. The snake coils around enemies, restraining them."
            value = 2000
            weight = 4
            weapon()
            stats(attack = 3)
            abilityIds = listOf(abilityId("snake-staff-attack"))
        }

        item("bloodstone-gem") {
            name = "Bloodstone Gem"
            description = "A dark gem with red flecks, radiating subtle power."
            value = 500
            weight = 0
        }

        item("gold-altar-service") {
            name = "Gold Altar Service Set"
            description = "A complete gold altar service set of exceptional craftsmanship."
            value = 6000
            weight = 20
        }

        item("silver-flagon") {
            name = "Silver Flagon and Tankard"
            description = "A matched silver flagon and tankard set."
            value = 750
            weight = 3
        }

        item("ivory-tusk-carved") {
            name = "Carved Ivory Tusk"
            description = "An intricately carved ivory tusk depicting battle scenes."
            value = 50
            weight = 2
        }
    }

    // ==================== LOOT TABLES ====================
    private fun defineLootTables() {
        lootTable("guard-common") {
            name = "Common Guard Loot"
            item("shortsword", chance = 0.3f)
            item("dagger", chance = 0.5f)
        }

        lootTable("merchant") {
            name = "Merchant Valuables"
            item("gem-ruby", chance = 0.1f)
            item("gem-emerald", chance = 0.1f)
            item("gem-sapphire", chance = 0.1f)
        }
    }

    // ==================== CREATURES (NPCs) ====================
    private fun defineCreatures() {
        // === GUARDS & SOLDIERS ===
        creature("man-at-arms") {
            name = "Man-at-Arms"
            description = "A trained soldier of the Keep, wearing plate mail and carrying a pole arm."
            maxHp = 5
            baseDamage = 6
            level = 1
            experienceValue = 0  // Non-hostile NPCs
            challengeRating = 1
            isAggressive = false
            isAlly = true
            abilities("pole-arm-thrust", "sword-strike")
        }

        creature("crossbowman") {
            name = "Crossbowman"
            description = "A guard stationed in the towers, skilled with the heavy crossbow."
            maxHp = 4
            baseDamage = 6
            level = 1
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
            abilities("crossbow-shot", "sword-strike")
        }

        creature("corporal-of-watch") {
            name = "Corporal of the Watch"
            description = "A gruff but fair soldier in plate mail. He records all who enter and leave the Keep."
            maxHp = 15
            baseDamage = 8
            level = 2
            experienceValue = 0
            challengeRating = 2
            isAggressive = false
            isAlly = true
            abilities("sword-strike", "shield-bash")
        }

        creature("captain-of-guard") {
            name = "Captain of the Guard"
            description = "A kind, friendly man in magical plate mail. He is an excellent leader and sometimes moves about the Outer Bailey in disguise."
            maxHp = 24
            baseDamage = 12
            level = 3
            experienceValue = 0
            challengeRating = 3
            isAggressive = false
            isAlly = true
            abilities("sword-strike", "shield-bash", "pole-arm-thrust")
        }

        creature("sergeant-of-guard") {
            name = "Sergeant of the Guard"
            description = "A very strong fellow who loves to drink and brawl. Despite his rough nature, he's fiercely loyal to the Keep."
            maxHp = 16
            baseDamage = 10
            level = 2
            experienceValue = 0
            challengeRating = 2
            isAggressive = false
            isAlly = true
            abilities("sword-strike", "shield-bash")
        }

        creature("guardsman") {
            name = "Guardsman"
            description = "One of the 24 guardsmen stationed throughout the Keep, wearing chain mail and carrying sword and shield."
            maxHp = 5
            baseDamage = 6
            level = 1
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
            abilities("sword-strike", "shield-bash")
        }

        creature("cavalryman") {
            name = "Cavalryman"
            description = "A mounted soldier from the Keep's cavalry, equipped with lance and sword."
            maxHp = 8
            baseDamage = 10
            level = 2
            experienceValue = 0
            challengeRating = 2
            isAggressive = false
            isAlly = true
            abilities("lance-charge", "sword-strike")
        }

        // === OFFICIALS ===
        creature("bailiff") {
            name = "The Bailiff"
            description = "The superintendent of the outer bailey. A stern man in magical plate mail who wields a sword +1 and keeps meticulous records."
            maxHp = 22
            baseDamage = 10
            level = 3
            experienceValue = 0
            challengeRating = 3
            isAggressive = false
            isAlly = true
            abilities("sword-strike", "longbow-shot")
        }

        creature("castellan") {
            name = "The Castellan"
            description = "The ruler of the Keep, a noble warrior in shining armor. He is wise, just, and always seeking adventurers to help defend the Borderlands."
            maxHp = 35
            baseDamage = 14
            level = 5
            experienceValue = 0
            challengeRating = 5
            isAggressive = false
            isAlly = true
            abilities("sword-strike", "shield-bash")
        }

        // === CLERGY ===
        creature("curate") {
            name = "The Curate"
            description = "The spiritual leader of the Keep, a powerful cleric in plate mail who wields either a mace or the mysterious Snake Staff."
            maxHp = 24
            baseDamage = 8
            level = 5
            experienceValue = 0
            challengeRating = 4
            isAggressive = false
            isAlly = true
            abilities("mace", "cure-light-wounds", "bless", "hold-person", "snake-staff-attack")
        }

        creature("acolyte") {
            name = "Acolyte"
            description = "One of the Curate's assistants, sworn to silence until attaining priesthood. They wear plate mail and wield maces."
            maxHp = 6
            baseDamage = 6
            level = 1
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
            abilities("mace", "cure-light-wounds")
        }

        creature("priest-visitor") {
            name = "The Visiting Priest"
            description = "A jovial priest staying at the Keep to discuss theology. Beware - he and his acolytes are secretly agents of Chaos, spying on adventurers!"
            maxHp = 18
            baseDamage = 8
            level = 4
            experienceValue = 50
            challengeRating = 3
            isAggressive = false  // Until revealed
            abilities("mace", "cure-light-wounds", "hold-person")
        }

        // === MERCHANTS ===
        creature("jewel-merchant") {
            name = "Jewel Merchant"
            description = "A wealthy merchant and his wife, dealing in gems and fine jewelry. They're guarded by fighters and a trained dog."
            maxHp = 3
            baseDamage = 2
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
            abilities("call-guards")
        }

        creature("merchant-guard") {
            name = "Merchant Guard"
            description = "A 2nd level fighter guarding the jewel merchant, wearing chainmail and wielding sword and dagger."
            maxHp = 12
            baseDamage = 8
            level = 2
            experienceValue = 0
            challengeRating = 2
            isAggressive = false
            isAlly = true
            abilities("sword-strike")
        }

        creature("guard-dog") {
            name = "Guard Dog"
            description = "A huge dog trained to kill, protecting its master's treasures."
            maxHp = 12
            baseDamage = 6
            level = 2
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
            abilities("guard-dog-bite")
        }

        creature("smith") {
            name = "The Smith"
            description = "A burly man in leather armor who uses his hammer as a weapon. He runs the smithy with his two assistants."
            maxHp = 11
            baseDamage = 6
            level = 1
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
        }

        creature("smith-assistant") {
            name = "Smith's Assistant"
            description = "One of the smith's helpers, ready to grab any weapon if needed."
            maxHp = 5
            baseDamage = 4
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        creature("provisioner") {
            name = "The Provisioner"
            description = "The owner of the provision shop, selling all manner of adventuring supplies. He has leather armor and a spear for emergencies."
            maxHp = 3
            baseDamage = 4
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        creature("trader") {
            name = "The Trader"
            description = "A shrewd merchant dealing in armor, weapons, and rare goods. He's very interested in buying furs from adventurers."
            maxHp = 2
            baseDamage = 4
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        creature("banker") {
            name = "The Banker"
            description = "A retired 3rd level fighter who now runs the Loan Bank. He offers secure storage and loans at 10% interest."
            maxHp = 12
            baseDamage = 8
            level = 3
            experienceValue = 0
            challengeRating = 2
            isAggressive = false
            isAlly = true
            abilities("sword-strike")
        }

        creature("bank-guard") {
            name = "Bank Guard"
            description = "A mercenary fighter in plate mail, armed with battle axe and crossbow, guarding the bank."
            maxHp = 7
            baseDamage = 8
            level = 1
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
            abilities("crossbow-shot", "sword-strike")
        }

        creature("bank-clerk") {
            name = "Bank Clerk"
            description = "A scrawny old man who handles transactions. Don't be fooled - he's a 2nd level magic-user!"
            maxHp = 5
            baseDamage = 2
            level = 2
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            isAlly = true
        }

        creature("guild-master") {
            name = "The Guild Master"
            description = "The influential head of the Guild House, who carefully watches all trade passing through the Keep."
            maxHp = 4
            baseDamage = 4
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        // === INNKEEPERS & TAVERNERS ===
        creature("innkeeper") {
            name = "The Innkeeper"
            description = "The proprietor of the Travelers Inn, a normal man of no fighting ability. His family lives in a small loft above."
            maxHp = 3
            baseDamage = 2
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        creature("taverner") {
            name = "The Taverner"
            description = "The owner of the tavern, serving excellent food and drinks. He knows many rumors about the Caves of Chaos."
            maxHp = 6
            baseDamage = 4
            level = 1
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        creature("serving-wench") {
            name = "Serving Wench"
            description = "One of the tavern's servers, hurrying between tables with mugs and plates."
            maxHp = 2
            baseDamage = 1
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }

        creature("mercenary") {
            name = "Mercenary"
            description = "A sellsword drinking at the tavern, available for hire. Wears leather armor and carries sword and dagger."
            maxHp = 5
            baseDamage = 6
            level = 1
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
            abilities("sword-strike")
        }

        // === ANIMALS ===
        creature("war-horse") {
            name = "War Horse"
            description = "A trained war horse from the cavalry stables, bred for battle."
            maxHp = 11
            baseDamage = 6
            level = 2
            experienceValue = 0
            challengeRating = 1
            isAggressive = false
        }

        creature("riding-horse") {
            name = "Riding Horse"
            description = "A reliable riding horse, suitable for travel."
            maxHp = 8
            baseDamage = 4
            level = 1
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
        }

        creature("mule") {
            name = "Mule"
            description = "A sturdy mule for carrying supplies."
            maxHp = 7
            baseDamage = 2
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
        }

        creature("stable-hand") {
            name = "Stable Hand"
            description = "A lackey working in the stables, tending to horses and gear."
            maxHp = 2
            baseDamage = 2
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            isAlly = true
        }
    }

    // ==================== LOCATIONS ====================
    private fun defineLocations() {
        // === APPROACH TO THE KEEP ===
        location("road-to-keep") {
            name = "Road to the Keep"
            description = "A narrow, rocky track winds up toward the Keep. The path falls away to a steep cliff on the left, while a sheer wall of natural stone rises on the right. Ahead, the main gate of the Keep looms, flanked by two tall towers."
            position(0, 5, 0)
            locationType = LocationType.OUTDOOR_GROUND
            exits {
                north("main-gate")
                southTo("location-caves-of-chaos-ravine-entrance")  // Connect to Caves of Chaos
            }
        }

        // === OUTER BAILEY - MAIN GATE AREA ===
        location("main-gate") {
            name = "Main Gate"
            description = "Two towers 30 feet high flank a gatehouse 20 feet high. A deep crevice in front is spanned by a drawbridge. Guards in blue cloaks watch from atop the towers, crossbows ready. The Corporal of the Watch stands ready to question all who enter."
            position(0, 4, 0)
            locationType = LocationType.OUTDOOR_GROUND
            creatures("man-at-arms", "man-at-arms", "corporal-of-watch")
            exits {
                south("road-to-keep")
                north("entry-yard")
                up("east-flanking-tower")
            }
        }

        location("east-flanking-tower") {
            name = "East Flanking Tower"
            description = "A tower bristling with defensive devices. Four crossbowmen are on duty, with eight more resting inside. The floors contain supplies of bolts, arrows, spears, rocks, and barrels of oil for defense."
            position(1, 4, 1)
            locationType = LocationType.INDOOR
            creatures("crossbowman", "crossbowman", "crossbowman", "crossbowman")
            exits {
                down("main-gate")
                west("west-flanking-tower")
            }
        }

        location("west-flanking-tower") {
            name = "West Flanking Tower"
            description = "The western flanking tower, identical to its eastern twin. Guards peer out through arrow slits, watching the approach road for threats."
            position(-1, 4, 1)
            locationType = LocationType.INDOOR
            creatures("crossbowman", "crossbowman", "crossbowman", "crossbowman")
            exits {
                down("main-gate")
                east("east-flanking-tower")
            }
        }

        location("entry-yard") {
            name = "Entry Yard"
            description = "A narrow, paved area inside the gates. All entrants must dismount and stable their animals here. The Corporal of the Watch records names, while lackeys tend to mounts. The bustle of commerce and garrison life fills the air."
            position(0, 3, 0)
            locationType = LocationType.OUTDOOR_GROUND
            creatures("man-at-arms", "man-at-arms")
            exits {
                south("main-gate")
                east("common-stable")
                west("common-warehouse")
                north("outer-bailey-square")
            }
        }

        location("common-stable") {
            name = "Common Stable"
            description = "A long building with a 3-foot parapet atop its flat roof. 5-8 lackeys tend to horses and gear inside. Various light horses, draft horses, and mules rest in their stalls."
            position(1, 3, 0)
            locationType = LocationType.INDOOR
            creatures("stable-hand", "stable-hand", "stable-hand")
            items("riding-horse", "mule")
            exits {
                west("entry-yard")
            }
        }

        location("common-warehouse") {
            name = "Common Warehouse"
            description = "Visiting merchants store their goods here until sold or taken elsewhere. Inside are wagons, carts, boxes, barrels, and bales containing various goods. The Corporal of the Watch has the keys."
            position(-1, 3, 0)
            locationType = LocationType.INDOOR
            exits {
                east("entry-yard")
            }
        }

        // === OUTER BAILEY - MAIN SQUARE ===
        location("outer-bailey-square") {
            name = "Outer Bailey Square"
            description = "The heart of the Keep's outer bailey, a wide paved square where merchants set up stalls on market days. A large fountain gushes in the center. Surrounding buildings house shops, the tavern, and the inn."
            position(0, 2, 0)
            locationType = LocationType.OUTDOOR_GROUND
            exits {
                south("entry-yard")
                north("inner-gatehouse")
                east("provisioner-shop")
                west("smithy")
                northeast("traders-shop")
                northwest("loan-bank")
            }
        }

        location("fountain-square") {
            name = "Fountain Square"
            description = "A large, gushing fountain dominates the center of the square. On holidays, local farmers and tradesmen set up small booths to sell their goods. The water is cool and refreshing."
            position(0, 1, 0)
            locationType = LocationType.OUTDOOR_GROUND
            exits {
                north("outer-bailey-square")
                south("tavern")
                east("travelers-inn")
                west("guild-house")
            }
        }

        // === SHOPS ===
        location("smithy") {
            name = "Smithy and Armorer"
            description = "A building about 20 feet high with a forge, bellows, and anvil on the lower floor. The clang of hammer on metal rings out as the smith and his assistants work. Weapons line the walls: swords, axes, spears, and a suit of chain mail."
            position(-1, 2, 0)
            locationType = LocationType.INDOOR
            creatures("smith", "smith-assistant", "smith-assistant")
            items("longsword", "shortsword", "dagger", "spear", "pole-arm", "mace", "hand-axe", "battle-axe", "chain-mail")
            exits {
                east("outer-bailey-square")
            }
        }

        location("provisioner-shop") {
            name = "Provisioner"
            description = "A low building where all equipment needed for dungeon adventuring is sold. The provisioner stocks everything from torches to rations to rope. He has a few shields but directs armor-seekers to the trader next door."
            position(1, 2, 0)
            locationType = LocationType.INDOOR
            creatures("provisioner")
            items("torch", "rope-50ft", "iron-rations", "waterskin", "backpack", "lantern", "oil-flask", "tinderbox", "iron-spikes", "wooden-pole", "holy-water")
            exits {
                west("outer-bailey-square")
                north("traders-shop")
            }
        }

        location("traders-shop") {
            name = "Trader"
            description = "A shop dealing in armor, weapons, salt, spices, cloth, rare woods, and furs. The trader and his two sons eagerly buy furs from adventurers at their stated value. Leather armor, chain mail, and shields hang on display."
            position(1, 1, 0)
            locationType = LocationType.INDOOR
            creatures("trader")
            items("leather-armor", "chain-mail", "plate-mail", "shield", "fur-trimmed-cape", "dagger-jeweled", "crystal-decanter", "jade-ring", "gold-silver-belt")
            exits {
                south("provisioner-shop")
                southwest("outer-bailey-square")
            }
        }

        location("loan-bank") {
            name = "Loan Bank"
            description = "Here anyone can change money or gems for a 10% fee. The banker offers secure storage and loans at 10% interest. A sign proclaims the place is under the Keep's direct protection. A guard watches the door, crossbow at the ready."
            position(-1, 1, 0)
            locationType = LocationType.INDOOR
            creatures("banker", "bank-guard", "bank-clerk")
            items("ivory-tusk-carved", "thieves-tools")
            exits {
                southeast("outer-bailey-square")
            }
        }

        // === PRIVATE APARTMENTS ===
        location("jewel-merchant-apartment") {
            name = "Jewel Merchant's Apartment"
            description = "One of the large private apartments along the south wall, occupied by a wealthy jewel merchant and his wife. Two guards and a huge trained dog protect a locked iron box containing 200 platinum pieces and 100 gold."
            position(2, 0, 0)
            locationType = LocationType.INDOOR
            creatures("jewel-merchant", "merchant-guard", "merchant-guard", "guard-dog")
            items("gem-ruby", "gem-emerald", "gem-sapphire", "jeweled-bracelet", "pearl-necklace", "gold-earrings")
            exits {
                west("fountain-square")
            }
        }

        location("priest-apartment") {
            name = "Visiting Priest's Apartment"
            description = "The western portion houses a jovial priest who discusses theology with the learned. His chambers are well-furnished with a cozy fire. His two acolytes follow vows of silence. Beware - they are secretly agents of Chaos!"
            position(-2, 0, 0)
            locationType = LocationType.INDOOR
            creatures("priest-visitor", "acolyte", "acolyte")
            exits {
                east("fountain-square")
            }
        }

        // === TAVERN & INN ===
        location("tavern") {
            name = "The Tavern"
            description = "The favorite gathering place of visitors and inhabitants alike. The food is excellent, the drinks generous. 4-16 patrons fill the common room at any hour. A menu lists: Ale 1cp, Small Beer 1sp, Wine 1cp, Honey Mead 1gp, Roast Fowl 1gp, Hot Pie 1cp."
            position(0, 0, 0)
            locationType = LocationType.INDOOR
            creatures("taverner", "serving-wench", "serving-wench", "mercenary", "mercenary")
            items("ale-mug", "wine-bottle", "honey-mead", "roast-fowl", "hot-pie")
            exits {
                north("fountain-square")
                up("tavern-upstairs")
            }
        }

        location("tavern-upstairs") {
            name = "Tavern Upper Floor"
            description = "The taverner's family lives here. The cellar below stores food and drink, with hidden treasure: 82 copper, 29 silver, 40 electrum, and 17 gold pieces concealed under flour bags."
            position(0, 0, 1)
            locationType = LocationType.INDOOR
            exits {
                down("tavern")
            }
        }

        location("travelers-inn") {
            name = "Travelers Inn"
            description = "A long, low structure with five small private rooms and a large common sleeping room. Private rooms cost 1gp per night, the common room only 1 silver. The innkeeper and his family live in a small loft above."
            position(1, 0, 0)
            locationType = LocationType.INDOOR
            creatures("innkeeper")
            exits {
                west("fountain-square")
            }
        }

        // === GUILD HOUSE & OFFICIALS ===
        location("guild-house") {
            name = "Guild House"
            description = "A two-story building offering hospitality to traveling guild members. The Guild Master and his clerks carefully observe all trade passing through the Keep. Four armed guildsmen stand guard."
            position(-1, 0, 0)
            locationType = LocationType.INDOOR
            creatures("guild-master", "guardsman", "guardsman")
            exits {
                east("fountain-square")
            }
        }

        location("bailiff-tower") {
            name = "Bailiff's Tower"
            description = "The superintendent of the outer bailey lives here. The Bailiff in his magical plate mail maintains order and records. A scribe assists with paperwork. The bailiff's quarters are well-furnished with a quiver of 20 arrows, 3 of which are magical."
            position(2, 2, 0)
            locationType = LocationType.INDOOR
            creatures("bailiff")
            items("magic-arrows")
            exits {
                west("outer-bailey-square")
            }
        }

        location("watch-tower") {
            name = "Watch Tower"
            description = "A 45-foot tower with all the usual defensive devices. Six men-at-arms in chain mail guard here, carrying bows and swords. The Captain of the Watch and his lieutenant maintain quarters on the first floor."
            position(2, 3, 0)
            locationType = LocationType.INDOOR
            creatures("captain-of-guard", "guardsman", "guardsman", "guardsman")
            items("sword-plus-one", "shield-plus-one")
            exits {
                west("entry-yard")
            }
        }

        // === INNER BAILEY ===
        location("inner-gatehouse") {
            name = "Inner Gatehouse"
            description = "A stone structure like a small fort, with 15-foot walls and a 30-foot battlement. The heavy gates are doublebound with iron and spiked. Six guards are on duty at all times. No visitor is allowed beyond without invitation or special permits."
            position(0, -1, 0)
            locationType = LocationType.INDOOR
            creatures("sergeant-of-guard", "guardsman", "guardsman", "guardsman", "guardsman", "guardsman", "guardsman")
            exits {
                south("outer-bailey-square")
                north("inner-bailey")
            }
        }

        location("inner-bailey") {
            name = "Inner Bailey"
            description = "A grass-covered training area where troops drill daily. During daylight, a dozen or more soldiers practice weapons. Practice dummies and jousting targets line the field."
            position(0, -2, 0)
            locationType = LocationType.OUTDOOR_GROUND
            creatures("guardsman", "guardsman", "cavalryman", "cavalryman")
            exits {
                south("inner-gatehouse")
                north("keep-fortress")
                east("cavalry-stables")
                west("chapel")
            }
        }

        location("small-tower") {
            name = "Small Tower"
            description = "A typical tower housing eight guardsmen in chain mail with crossbows and swords. Two are on duty atop the tower at all times, the other six rest in the chamber below."
            position(1, -1, 0)
            locationType = LocationType.INDOOR
            creatures("guardsman", "guardsman", "crossbowman", "crossbowman")
            exits {
                west("inner-gatehouse")
            }
        }

        location("guard-tower") {
            name = "Guard Tower"
            description = "A 50-foot tower housing 24 guardsmen. Their commander is the Corporal of the Guard, armed with sword and magical dagger. Supplies of food, weapons, and oil fill the upper floors."
            position(-1, -1, 0)
            locationType = LocationType.INDOOR
            creatures("corporal-of-watch", "guardsman", "guardsman", "guardsman", "guardsman")
            items("dagger-plus-one")
            exits {
                east("inner-gatehouse")
            }
        }

        location("cavalry-stables") {
            name = "Cavalry Stables"
            description = "Here are kept 30 war horses and 4 riding horses, tended by lackeys and guarded by two men-at-arms. The horses are well-trained for battle."
            position(1, -2, 0)
            locationType = LocationType.INDOOR
            creatures("stable-hand", "stable-hand", "man-at-arms", "man-at-arms")
            items("war-horse", "riding-horse")
            exits {
                west("inner-bailey")
            }
        }

        location("great-tower") {
            name = "Great Tower"
            description = "A 60-foot tower housing 24 guardsmen under another Corporal. One-third carry crossbows, one-third bows, one-third pole arms. A commanding view of the entire Keep stretches below."
            position(1, -3, 0)
            locationType = LocationType.INDOOR
            creatures("corporal-of-watch", "crossbowman", "crossbowman", "man-at-arms", "man-at-arms")
            exits {
                south("cavalry-stables")
                west("keep-fortress")
            }
        }

        // === CHAPEL ===
        location("chapel") {
            name = "Chapel"
            description = "The spiritual center of the Keep, with a peaked roof two stories tall. The interior is one large room with an altar at the eastern end. A colored glass window worth 350gp catches the morning light. An offering box contains 1-100 silver pieces."
            position(-1, -2, 0)
            locationType = LocationType.INDOOR
            creatures("curate", "acolyte", "acolyte", "acolyte")
            items("potion-healing", "potion-healing", "potion-healing", "potion-cure-disease", "holy-water")
            exits {
                east("inner-bailey")
                down("chapel-cellar")
            }
        }

        location("chapel-cellar") {
            name = "Chapel Cellar"
            description = "The Curate and his assistants have their quarters here. A locked room stores the clerics' armor and weapons. Hidden in a secret compartment beneath the offering box pedestal are magical scrolls and potions."
            position(-1, -2, -1)
            locationType = LocationType.INDOOR
            items("snake-staff", "ring-of-protection")
            exits {
                up("chapel")
            }
        }

        // === THE KEEP FORTRESS ===
        location("keep-fortress") {
            name = "The Keep Fortress"
            description = "The main fortress of the Keep, solidly built to withstand attack. The lowest level has a 15-foot front section with 60-foot round flanking towers. Inside is a great hall, armory, and chambers for meetings."
            position(0, -3, 0)
            locationType = LocationType.INDOOR
            creatures("guardsman", "guardsman", "guardsman", "guardsman")
            exits {
                south("inner-bailey")
                east("great-tower")
                up("castellan-quarters")
                down("fortress-dungeon")
            }
        }

        location("castellan-quarters") {
            name = "Castellan's Quarters"
            description = "The private chambers of the Castellan, ruler of the Keep. The rooms are well-decorated with heavy furniture. Rooms for up to 36 cavalrymen and special guests occupy this floor."
            position(0, -3, 1)
            locationType = LocationType.INDOOR
            creatures("castellan")
            items("plate-mail-plus-one", "sword-plus-one", "silver-flagon")
            exits {
                down("keep-fortress")
            }
        }

        location("fortress-dungeon") {
            name = "Fortress Dungeon"
            description = "The cellars below hold vast stores of provisions, quarters for servants, a cistern, and a dungeon with four stout cells. Prisoners awaiting judgment are held here under heavy guard."
            position(0, -3, -1)
            locationType = LocationType.INDOOR
            creatures("guardsman", "guardsman")
            exits {
                up("keep-fortress")
            }
        }
    }
}
