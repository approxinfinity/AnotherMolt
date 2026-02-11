package com.ez2bg.anotherthread.database.modules

import com.ez2bg.anotherthread.database.*

/**
 * The Caverns of Quasqueton - Based on classic D&D Module B1 "In Search of the Unknown" by Mike Carr
 *
 * A two-level dungeon complex carved into a rocky hill, built by two legendary figures:
 * - Rogahn the Fearless: A renowned fighter who slew a horde of barbarians single-handedly
 * - Zelligar the Unknown: A mysterious and powerful magic-user
 *
 * The two heroes disappeared years ago on an expedition into barbarian lands.
 * Now their abandoned stronghold awaits exploration, filled with:
 * - Traps and tricks (teleportation rooms, pit traps, magic mouths)
 * - The famous Room of Pools with 14 mysterious pools
 * - A fungus-filled garden room
 * - Zelligar's wizard laboratory and workroom
 * - Rogahn's trophy room with dragon skin and other trophies
 * - Wandering monsters that have moved in since abandonment
 *
 * Upper Level: The finished living quarters (rooms 1-37)
 * Lower Level: Natural caverns and unfinished areas (rooms 38-56)
 */
object QuasquetonModule : AdventureModuleSeed() {

    override val moduleId = "quasqueton"
    override val moduleName = "The Caverns of Quasqueton"
    override val moduleDescription = "The legendary stronghold of Rogahn the Fearless and Zelligar the Unknown, two heroes who vanished years ago. Their abandoned fortress, carved into a rocky hillside, awaits brave explorers willing to face its many traps, monsters, and mysteries."
    override val attribution = "Inspired by D&D Module B1 - In Search of the Unknown by Mike Carr (TSR, 1979)"
    override val recommendedLevelMin = 1
    override val recommendedLevelMax = 3

    override fun defineContent() {
        defineAbilities()
        defineItems()
        defineLootTables()
        defineCreatures()
        defineLocations()
        defineChests()
        definePools()
    }

    // ==================== ABILITIES ====================
    private fun defineAbilities() {
        // Orc abilities
        ability("orc-cleave") {
            name = "Orc Cleave"
            description = "A brutal swing with a weapon, striking with orcish fury."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 8
        }

        // Kobold abilities
        ability("kobold-stab") {
            name = "Kobold Stab"
            description = "A quick stab with a small blade."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 4
        }

        ability("kobold-sling") {
            name = "Kobold Sling"
            description = "Hurls a stone with a crude sling."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 30
            baseDamage = 3
        }

        // Troglodyte abilities
        ability("troglodyte-claw") {
            name = "Troglodyte Claw"
            description = "Rakes with sharp claws."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 6
        }

        ability("troglodyte-stench") {
            name = "Stench"
            description = "The troglodyte's foul odor nauseates nearby enemies."
            abilityType = "debuff"
            targetType = "area"
            range = 10
            durationRounds = 3
            effects = """[{"type":"debuff","stat":"attack","modifier":-2}]"""
        }

        // Giant centipede abilities
        ability("centipede-bite") {
            name = "Venomous Bite"
            description = "A poisonous bite from mandibles."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 2
            effects = """[{"type":"condition","condition":"poisoned","duration":3,"chance":0.3}]"""
        }

        // Giant rat abilities
        ability("rat-bite") {
            name = "Rat Bite"
            description = "A diseased bite from a giant rat."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 3
            effects = """[{"type":"condition","condition":"diseased","duration":5,"chance":0.1}]"""
        }

        // Berserker abilities
        ability("berserker-rage") {
            name = "Berserker Rage"
            description = "Enters a battle rage, striking with reckless fury."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 12
            cooldownType = "short"
            cooldownRounds = 3
        }

        // Fire beetle ability
        ability("fire-gland-glow") {
            name = "Luminescent Glands"
            description = "The beetle's glowing glands provide dim light even after death."
            abilityType = "passive"
            targetType = "self"
        }

        // Skeleton abilities
        ability("skeleton-slash") {
            name = "Bone Slash"
            description = "Strikes with a rusty blade wielded by skeletal hands."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 5
        }

        // Zombie abilities
        ability("zombie-slam") {
            name = "Zombie Slam"
            description = "A slow but powerful strike from undead fists."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 6
        }

        // Spider abilities
        ability("spider-bite") {
            name = "Spider Bite"
            description = "A venomous bite that can paralyze prey."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 4
            effects = """[{"type":"condition","condition":"poisoned","duration":4,"chance":0.4}]"""
        }

        ability("web-spray") {
            name = "Web Spray"
            description = "Sprays sticky webbing to entangle enemies."
            abilityType = "control"
            targetType = "single_enemy"
            range = 15
            durationRounds = 2
            cooldownType = "short"
            cooldownRounds = 3
            effects = """[{"type":"condition","condition":"restrained","duration":2}]"""
        }
    }

    // ==================== ITEMS ====================
    private fun defineItems() {
        // === WEAPONS ===
        item("rusty-sword") {
            name = "Rusty Sword"
            description = "An old sword, pitted with rust but still serviceable."
            value = 5
            weight = 4
            weapon()
            stats(attack = 2)
        }

        item("broken-sword") {
            name = "Broken Sword"
            description = "A sword snapped off about eight inches above the pommel. Worthless."
            value = 0
            weight = 2
        }

        item("war-hammer") {
            name = "War Hammer"
            description = "A dwarven war hammer, still clutched by its fallen owner."
            value = 10
            weight = 5
            weapon()
            stats(attack = 4)
        }

        item("small-dagger") {
            name = "Small Dagger"
            description = "A small sheathed dagger found on a body."
            value = 2
            weight = 1
            weapon()
            stats(attack = 2)
        }

        item("heavy-mace") {
            name = "Heavy Mace"
            description = "A heavy mace hanging on an armor rack."
            value = 8
            weight = 6
            weapon()
            stats(attack = 4)
        }

        // === MAGIC WEAPONS ===
        item("sword-plus-one") {
            name = "Sword +1"
            description = "A finely crafted sword that gleams with magical energy."
            value = 500
            weight = 4
            weapon()
            stats(attack = 7)
        }

        item("dagger-plus-one") {
            name = "Dagger +1"
            description = "A magical dagger that seems to guide itself toward targets."
            value = 300
            weight = 1
            weapon()
            stats(attack = 5)
        }

        // === ARMOR ===
        item("dented-helm") {
            name = "Dented Helm"
            description = "A fighter's helm with a noticeable dent, rendering it unusable."
            value = 0
            weight = 3
        }

        item("broken-shield") {
            name = "Broken Wooden Shield"
            description = "A wooden shield, cracked and useless."
            value = 0
            weight = 4
        }

        item("chainmail-rusted") {
            name = "Rusted Chain Mail"
            description = "Chain mail that has seen better days, but still provides some protection."
            value = 15
            weight = 25
            armor("chest")
            stats(defense = 3)
        }

        item("shield-large") {
            name = "Large Black Shield"
            description = "A largish black shield that could only be used by a giant."
            value = 20
            weight = 15
        }

        // === MAGIC ARMOR ===
        item("ring-of-protection") {
            name = "Ring of Protection +1"
            description = "A silver ring that provides magical protection."
            value = 500
            weight = 0
            accessory("finger")
            stats(defense = 2)
        }

        // === TREASURES & VALUABLES ===
        item("gold-pieces-pouch") {
            name = "Pouch of Gold"
            description = "A belt pouch containing gold pieces."
            value = 5
            weight = 1
        }

        item("pewter-pitcher") {
            name = "Pewter Pitcher"
            description = "A pewter pitcher from the wizard's chamber."
            value = 15
            weight = 2
        }

        item("pewter-mug") {
            name = "Pewter Mug"
            description = "A pewter mug, worth a few gold."
            value = 5
            weight = 1
        }

        item("garment-pewter-studded") {
            name = "Pewter-Studded Garment"
            description = "A dusty garment studded with circular bits of pewter for ornamentation."
            value = 15
            weight = 2
        }

        item("rosewood-bed-parts") {
            name = "Rosewood Bed Parts"
            description = "Ornately carved rosewood bed frame pieces. Zelligar's name is highlighted in gold leaf on the headboard."
            value = 500
            weight = 50
        }

        item("silver-comb") {
            name = "Silver-Plated Comb"
            description = "A silver-plated comb from the mistress's chamber."
            value = 5
            weight = 0
        }

        item("perfume-bottles") {
            name = "Perfume Bottles"
            description = "Two small capped bottles half full of perfume."
            value = 10
            weight = 1
        }

        item("tortoiseshell-dish") {
            name = "Tortoiseshell Dish"
            description = "A tortoiseshell dish containing a single gold coin."
            value = 2
            weight = 1
        }

        item("walnut-plaque") {
            name = "Engraved Walnut Plaque"
            description = "A walnut plaque with an inlaid piece of silver engraved: 'To Erig, great and trusted fighter by my side, and captain of the guard at Quasqueton—against all foes we shall prevail!' Signed with an embellished 'R.'"
            value = 25
            weight = 1
        }

        item("tapestry-melissa") {
            name = "Tapestry of Melissa"
            description = "A small tapestry depicting a warrior carrying a beautiful maiden in a rescue scene. Embroidered: 'Melissa, the most dearly won and greatest of all my treasures.'"
            value = 40
            weight = 5
        }

        item("tapestry-battle") {
            name = "Battle Tapestry"
            description = "A large tapestry depicting a dragon being slain by warriors. Worth 100 gold pieces each."
            value = 100
            weight = 30
        }

        // === DRAGON TROPHIES ===
        item("dragon-skin") {
            name = "Immense Dragon Skin"
            description = "An immense dragon skin covering the north wall of the trophy room. Its brassy scales reflect any illumination brightly."
            value = 1000
            weight = 100
        }

        item("basilisk-statue") {
            name = "Basilisk in Stone"
            description = "A basilisk frozen in stone, its menacing gaze forbidding but no longer a threat."
            value = 200
            weight = 500
        }

        item("dwarfin-skeleton") {
            name = "Dwarfin Skeleton"
            description = "A dwarfin skeleton suspended from a pair of irons near the ceiling."
            value = 0
            weight = 20
        }

        item("moose-antlers") {
            name = "Giant Moose Antlers"
            description = "Two gigantic sets of moose antlers mounted on large heads."
            value = 50
            weight = 40
        }

        item("dragon-paws") {
            name = "Dragon Paws"
            description = "Four dragon paws with claws extended."
            value = 100
            weight = 20
        }

        item("stuffed-cockatrice") {
            name = "Stuffed Cockatrice"
            description = "A stuffed cockatrice, still menacing in appearance."
            value = 150
            weight = 15
        }

        item("bearskin") {
            name = "Bearskin"
            description = "A large bearskin rug."
            value = 30
            weight = 20
        }

        item("crossed-swords") {
            name = "Pair of Crossed Swords"
            description = "A decorative pair of crossed swords mounted on the wall."
            value = 40
            weight = 8
        }

        item("rams-horns") {
            name = "Ram's Horns"
            description = "A pair of impressive ram's horns."
            value = 20
            weight = 5
        }

        item("barbarian-flags") {
            name = "Barbarian Tribal Flags"
            description = "Three colorful flags recognizable as belonging to prominent barbarian tribes."
            value = 30
            weight = 3
        }

        // === WIZARD ITEMS ===
        item("zelligar-papers") {
            name = "Zelligar's Papers"
            description = "A stack of papers monogrammed with a fancy letter Z. They contain mundane matters: inventory of foodstuffs, financial accounting, and routine messages."
            value = 10
            weight = 1
        }

        item("history-book") {
            name = "History Book"
            description = "A historical work outlining the history of the civilized area within 100 miles."
            value = 20
            weight = 3
        }

        item("plant-encyclopedia") {
            name = "Encyclopedia of Plants"
            description = "A tome about various types of plants, written in the language of elves."
            value = 30
            weight = 4
        }

        item("zelligar-diary") {
            name = "Zelligar's Diary"
            description = "A diary kept by Zelligar, detailing one of his adventures. Written in his own coded script and undecipherable without magic."
            value = 50
            weight = 2
        }

        item("weather-book") {
            name = "Book on Weather"
            description = "A work discussing weather with well-illustrated drawings and cryptic notes by Zelligar in the margins."
            value = 25
            weight = 3
        }

        item("oil-lantern-empty") {
            name = "Empty Oil Lantern"
            description = "An oil lantern with no fuel, but otherwise in perfect condition."
            value = 10
            weight = 2
        }

        item("smoked-glass-bottle") {
            name = "Smoked Glass Bottle"
            description = "A single stoppered smoked glass bottle. If the cork is removed, laughing gas issues forth!"
            value = 50
            weight = 1
        }

        item("black-cat-jar") {
            name = "Jar with Black Cat"
            description = "A large clear glass jar containing a black cat's body floating in clear liquid. If the cork is unstopped, the cat springs to life and runs away!"
            value = 100
            weight = 5
        }

        item("false-gold-ring") {
            name = "Shiny Gold Ring"
            description = "A shiny 'gold' ring that appears valuable but is actually worthless brass."
            value = 0
            weight = 0
        }

        // === MAGIC ITEMS ===
        item("scroll-magic-spells") {
            name = "Scroll of Magic Spells"
            description = "A scroll containing two magic-user spells."
            value = 200
            weight = 0
        }

        item("potion-healing") {
            name = "Potion of Healing"
            description = "A potion that restores health when consumed."
            value = 50
            weight = 1
        }

        // === POOL ITEMS ===
        item("pool-liquid-healing") {
            name = "Pinkish Healing Liquid"
            description = "Strange pinkish liquid from the Pool of Healing. Cures 1-6 hit points of damage and disease."
            value = 75
            weight = 1
        }

        item("brass-key-worthless") {
            name = "Large Brass Key"
            description = "A large brass key visible at the bottom of the acid pool. It corresponds to no locks in the stronghold."
            value = 0
            weight = 1
        }

        // === MISCELLANEOUS ===
        item("fire-beetle-gland") {
            name = "Fire Beetle Gland"
            description = "A glowing gland from a fire beetle. Provides dim light for 1-6 days."
            value = 5
            weight = 0
        }

        item("marble-statue-female") {
            name = "White Marble Statue"
            description = "A carved statue of a nude female, full-size, beckoning with arms out in an alluring pose. Worth over 5,000 gold but impossible to move without major engineering."
            value = 5000
            weight = 2000
        }

        item("ale-keg") {
            name = "Ale Keg"
            description = "A keg that is long since dry but still smells slightly of brew."
            value = 2
            weight = 10
        }

        item("earthenware-mugs") {
            name = "Earthenware Tankard Mugs"
            description = "Several earthenware tankard mugs hanging from hooks."
            value = 1
            weight = 1
        }

        item("moldy-cheese") {
            name = "Moldy Cheese"
            description = "A chunk of moldy cheese covered in fuzzy green growth. Inedible."
            value = 0
            weight = 1
        }

        item("cast-iron-kettle") {
            name = "Cast Iron Kettle"
            description = "A large cast iron kettle suspended from the ceiling. Empty."
            value = 5
            weight = 15
        }

        item("rope-heavy") {
            name = "Very Heavy Rope"
            description = "A coil of very heavy rope, 200 feet in length."
            value = 10
            weight = 20
        }

        item("iron-spikes-box") {
            name = "Box of Iron Spikes"
            description = "A box containing 50 iron spikes."
            value = 5
            weight = 10
        }

        item("wooden-beams") {
            name = "Pile of Wooden Beams"
            description = "A pile of 80 wooden beams, each 10 feet long and 6 inches wide."
            value = 20
            weight = 200
        }

        item("building-mortar") {
            name = "Sack of Building Mortar"
            description = "A sack of building mortar, almost empty."
            value = 2
            weight = 5
        }

        item("stone-blocks") {
            name = "Stack of Stone Blocks"
            description = "A stack of 400 stone blocks, each about 6 by 6 by 12 inches."
            value = 50
            weight = 1000
        }

        item("hacksaw") {
            name = "Hacksaw"
            description = "A hacksaw, rusty but still functional."
            value = 3
            weight = 2
        }

        item("unfletched-arrows") {
            name = "Barrel of Unfletched Arrows"
            description = "A small barrel containing 60 unfletched arrows."
            value = 5
            weight = 10
        }
    }

    // ==================== LOOT TABLES ====================
    private fun defineLootTables() {
        lootTable("orc-loot") {
            name = "Orc Loot"
            item("rusty-sword", chance = 0.3f)
            item("gold-pieces-pouch", chance = 0.2f)
        }

        lootTable("kobold-loot") {
            name = "Kobold Loot"
            item("small-dagger", chance = 0.2f)
        }

        lootTable("berserker-loot") {
            name = "Berserker Loot"
            item("war-hammer", chance = 0.2f)
            item("gold-pieces-pouch", chance = 0.3f)
        }

        lootTable("wizard-chamber") {
            name = "Wizard's Chamber Treasures"
            item("pewter-pitcher", chance = 0.5f)
            item("pewter-mug", chance = 0.8f)
            item("zelligar-papers", chance = 1.0f)
        }

        lootTable("trophy-room") {
            name = "Trophy Room Items"
            item("crossed-swords", chance = 0.5f)
            item("rams-horns", chance = 0.5f)
            item("barbarian-flags", chance = 0.5f)
        }
    }

    // ==================== CREATURES ====================
    private fun defineCreatures() {
        // Orcs
        creature("orc") {
            name = "Orc"
            description = "A brutish humanoid with grayish skin, prominent tusks, and a foul disposition. These orcs have moved into the abandoned stronghold."
            maxHp = 6
            baseDamage = 6
            damageDice = "1d6"
            level = 1
            experienceValue = 10
            challengeRating = 1
            isAggressive = true
            gold(1, 6)
            abilities("orc-cleave")
            lootTable("orc-loot")
        }

        creature("orc-leader") {
            name = "Orc Leader"
            description = "A larger, meaner orc who commands the others through intimidation and violence."
            maxHp = 10
            baseDamage = 8
            level = 2
            experienceValue = 25
            challengeRating = 2
            isAggressive = true
            gold(5, 20)
            abilities("orc-cleave")
        }

        // Kobolds
        creature("kobold") {
            name = "Kobold"
            description = "A small, reptilian humanoid with reddish-brown scales. Cowardly alone but dangerous in groups."
            maxHp = 3
            baseDamage = 3
            damageDice = "1d4"
            level = 1
            experienceValue = 5
            challengeRating = 1
            isAggressive = true
            gold(0, 3)
            abilities("kobold-stab", "kobold-sling")
            lootTable("kobold-loot")
        }

        // Troglodytes
        creature("troglodyte") {
            name = "Troglodyte"
            description = "A reptilian humanoid with chameleon-like abilities and a nauseating stench. They have moved into the lower caverns."
            maxHp = 6
            baseDamage = 5
            level = 2
            experienceValue = 20
            challengeRating = 2
            isAggressive = true
            gold(2, 8)
            abilities("troglodyte-claw", "troglodyte-stench")
        }

        // Giant Centipedes
        creature("giant-centipede") {
            name = "Giant Centipede"
            description = "A foot-long centipede with venomous mandibles. Its bite can poison the unwary."
            maxHp = 2
            baseDamage = 2
            level = 1
            experienceValue = 7
            challengeRating = 1
            isAggressive = true
            abilities("centipede-bite")
        }

        // Giant Rats
        creature("giant-rat") {
            name = "Giant Rat"
            description = "A rat the size of a small dog, with beady eyes and yellowed teeth. Carries disease."
            maxHp = 4
            baseDamage = 3
            level = 1
            experienceValue = 5
            challengeRating = 1
            isAggressive = true
            abilities("rat-bite")
        }

        // Berserkers
        creature("berserker") {
            name = "Berserker"
            description = "A crazed human warrior who has wandered into the dungeon. They fight with reckless fury."
            maxHp = 7
            baseDamage = 8
            level = 1
            experienceValue = 15
            challengeRating = 1
            isAggressive = true
            gold(1, 8)
            abilities("berserker-rage")
            lootTable("berserker-loot")
        }

        // Fire Beetles
        creature("fire-beetle") {
            name = "Fire Beetle"
            description = "A large beetle with glowing glands above its eyes and near its abdomen. The glands provide light even after the creature's death."
            maxHp = 4
            baseDamage = 4
            level = 1
            experienceValue = 8
            challengeRating = 1
            isAggressive = true
            abilities("fire-gland-glow")
        }

        // Skeletons
        creature("skeleton") {
            name = "Skeleton"
            description = "The animated bones of a long-dead warrior, held together by dark magic."
            maxHp = 5
            baseDamage = 5
            level = 1
            experienceValue = 10
            challengeRating = 1
            isAggressive = true
            abilities("skeleton-slash")
        }

        // Zombies
        creature("zombie") {
            name = "Zombie"
            description = "A shambling corpse animated by necromancy. Slow but relentless."
            maxHp = 8
            baseDamage = 6
            level = 1
            experienceValue = 12
            challengeRating = 1
            isAggressive = true
            abilities("zombie-slam")
        }

        // Spiders
        creature("giant-spider") {
            name = "Giant Spider"
            description = "A spider the size of a large dog, with venomous fangs and the ability to spray webs."
            maxHp = 8
            baseDamage = 4
            level = 2
            experienceValue = 25
            challengeRating = 2
            isAggressive = true
            abilities("spider-bite", "web-spray")
        }

        creature("black-widow-spider") {
            name = "Black Widow Spider"
            description = "A deadly spider with a distinctive red hourglass marking. Its venom is particularly potent."
            maxHp = 10
            baseDamage = 5
            level = 3
            experienceValue = 50
            challengeRating = 3
            isAggressive = true
            abilities("spider-bite", "web-spray")
        }
    }

    // ==================== LOCATIONS ====================
    private fun defineLocations() {
        // === ENTRANCE ===
        location("entrance") {
            name = "Cave Entrance"
            description = "A cave-like opening, somewhat obscured by vegetation, is noticeable at the end of a treacherous pathway leading to a craggy outcropping of black rock. A large wooden door blocks the passage, bits of wood chipped away from its edge showing it has been forced before."
            position(0, 0, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                north("alcoves-1")
            }
        }

        // === UPPER LEVEL ===
        location("alcoves-1") {
            name = "Guard Alcoves"
            description = "Three pairs of alcoves line the entrance corridor, intended for guards to defend against invaders. The first pair is empty and barren. The second pair conceals one-way secret doors. The third pair contains magic mouths that boom warnings to intruders: 'WHO DARES ENTER THIS PLACE AND INTRUDE UPON THE SANCTUARY OF ITS INHABITANTS?'"
            position(0, 1, 0)
            locationType = LocationType.UNDERGROUND
            creatures("kobold", "kobold")
            exits {
                south("entrance")
                north("corridor-1")
            }
        }

        location("corridor-1") {
            name = "Bloodstained Corridor"
            description = "At the top of steps, corridors meet from east to west. A grisly sight awaits—the remains of a battle where no less than five combatants died. Three were adventurers, their opponents two guards. Bodies in various states of decomposition are arrayed here, and the stench is strong."
            position(0, 2, 0)
            locationType = LocationType.UNDERGROUND
            items("broken-sword", "dented-helm", "gold-pieces-pouch", "small-dagger")
            exits {
                south("alcoves-1")
                east("corridor-east")
                west("corridor-west")
                north("kitchen")
            }
        }

        location("corridor-east") {
            name = "Eastern Corridor"
            description = "A long corridor stretching east, with doors leading to various chambers. The blackish slate walls are smoothly hewn."
            position(2, 2, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                west("corridor-1")
                north("room-of-pools")
                east("captains-chamber")
                south("worship-area")
            }
        }

        location("corridor-west") {
            name = "Western Corridor"
            description = "A corridor leading west toward the living quarters. The air grows slightly warmer here."
            position(-2, 2, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("corridor-1")
                north("dining-room")
                west("wizards-chamber")
                south("rogahns-chamber")
            }
        }

        location("kitchen") {
            name = "Kitchen"
            description = "The food preparation area for the complex, a very long room with variety of details. Two large cooking pits occupy the southwest corner, each large enough to cook an animal as large as a deer. Long tables line each wall, covered with scattered containers, some upturned with spilled contents moldering. One chunk of moldy cheese is covered in fuzzy green growth."
            position(0, 3, 0)
            locationType = LocationType.UNDERGROUND
            creatures("giant-rat", "giant-rat")
            items("moldy-cheese", "cast-iron-kettle")
            exits {
                south("corridor-1")
                east("lounge")
            }
        }

        location("dining-room") {
            name = "Dining Room"
            description = "The main dining hall for the complex, moderately decorated but frugal. A nicely carved wooden mantle surrounds the room at a height of 7 feet. Two ornately carved walnut chairs stand out—the personal seats of Zelligar and Rogahn—fixed to the floor. Lesser tables and chairs are scattered about, showing wear but obviously not used recently."
            position(-1, 3, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                south("corridor-west")
                east("kitchen")
                west("wizards-chamber")
            }
        }

        location("lounge") {
            name = "Lounge"
            description = "An anteroom apparently designed for before-dinner and after-dinner activity. Several earthenware tankard mugs hang from hooks on one wall (many more are missing). An ale keg, long since dry, stands in one corner. At the center of the room is a carved statue of a nude female, full-size, made of white marble. It seems anchored to the floor and would be impossible to move."
            position(1, 3, 0)
            locationType = LocationType.UNDERGROUND
            items("earthenware-mugs", "ale-keg", "marble-statue-female")
            exits {
                west("kitchen")
                north("wizards-chamber")
            }
        }

        location("wizards-chamber") {
            name = "Wizard's Chamber"
            description = "Zelligar's personal chamber is actually a rather austere abode. A very large and detailed stone carving runs the length of the north wall, depicting a mighty wizard (obviously Zelligar) on a hilltop casting a spell, with an entire army fleeing in confused panic below. Zelligar's bed of ornately carved rosewood dominates the room, his name highlighted in gold leaf on the headboard."
            position(-2, 3, 0)
            locationType = LocationType.UNDERGROUND
            items("rosewood-bed-parts", "pewter-pitcher", "pewter-mug", "oil-lantern-empty")
            exits {
                east("dining-room")
                south("wizards-closet")
                west("wizards-annex")
            }
        }

        location("wizards-closet") {
            name = "Wizard's Closet"
            description = "Zelligar's closet is rather large for its purpose but actually somewhat barren. Several bolts of cloth are stacked in one corner, dusty and moth-eaten. On one wall hang several garments, mostly coats and cloaks. One is remarkable—studded with circular bits of pewter for ornamentation. A wooden stand holds four large books that apparently belong in the library."
            position(-2, 2, 0)
            locationType = LocationType.UNDERGROUND
            items("garment-pewter-studded", "history-book", "plant-encyclopedia", "zelligar-diary", "weather-book")
            exits {
                north("wizards-chamber")
            }
        }

        location("wizards-annex") {
            name = "Wizard's Annex"
            description = "An unusually-shaped room used for meditation, study, and the practice of magic spells. The triangular widening at the south end appears charred and partially melted, as if scorched by intense heat. At the south end is a magnificent sight: two large wooden chests overflowing with riches, gold pieces arrayed around them. But wait—this treasure is an illusion that vanishes when touched!"
            position(-3, 3, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("wizards-chamber")
                south("wizards-workroom")
            }
        }

        location("wizards-workroom") {
            name = "Wizard's Workroom"
            description = "A facility designed for the study and practice of magic. Several large wooden tables fill the room, one overturned. The top of the prominent central table is a slab of smooth black slate, hidden by a thick layer of dust. Wooden cabinets along the north wall contain various chemical compounds and supplies, including a large jar with a black cat floating in clear liquid..."
            position(-3, 2, 0)
            locationType = LocationType.UNDERGROUND
            items("black-cat-jar", "zelligar-papers")
            exits {
                north("wizards-annex")
                east("wizards-laboratory")
            }
        }

        location("wizards-laboratory") {
            name = "Wizard's Laboratory"
            description = "Zelligar's lab is a strange but fascinating place. A large human skeleton hangs from the ceiling in the northeast corner—it would be discovered to be a barbarian chieftain's remains. Several wooden tables hold equipment and devices. On one table sits a single stoppered smoked glass bottle—if uncorked, laughing gas issues forth, causing uncontrollable laughter for 1-6 rounds!"
            position(-2, 1, 0)
            locationType = LocationType.UNDERGROUND
            creatures("orc", "orc")
            items("smoked-glass-bottle")
            exits {
                west("wizards-workroom")
                south("storeroom")
            }
        }

        location("storeroom") {
            name = "Storeroom"
            description = "Hidden by a secret door, this irregularly shaped room contains quantities of supplies which are only a bare fraction of its capacity. Approximately 60 barrels and casks are within the room, each marked with a letter code denoting contents: whole barley, wheat flour, rye flour, salt pork, dill pickles, raisins, fish in brine, dried apples, ale, honey, wine, water, and more."
            position(-2, 0, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                north("wizards-laboratory")
            }
        }

        location("supply-room") {
            name = "Supply Room"
            description = "The stronghold's supply room is rather empty, containing mostly construction supplies: a coil of very heavy rope (200'), a box of 50 iron spikes, a pile of 80 wooden beams, a sack of building mortar (almost empty), 400 stone blocks, six wooden doors, and a jug of dried glue."
            position(-1, 0, 0)
            locationType = LocationType.UNDERGROUND
            items("rope-heavy", "iron-spikes-box", "wooden-beams", "building-mortar", "stone-blocks")
            exits {
                east("library")
            }
        }

        location("library") {
            name = "Library"
            description = "Quasqueton's library lies behind ornately carved oaken doors. The floor is covered with dust, but beneath is a beautiful polished red granite surface. In the center, white granite blocks form the letters R & Z with an ampersand between. Small cages in the north wall contain fire beetles, their eerie reddish glow illuminating the room mysteriously."
            position(0, 0, 0)
            locationType = LocationType.UNDERGROUND
            creatures("fire-beetle", "fire-beetle")
            exits {
                west("supply-room")
                east("implement-room")
            }
        }

        location("implement-room") {
            name = "Implement Room"
            description = "An elongated room used primarily for storage of tools and implements. Contents include: a box of wooden pegs, a coil of light rope (50'), a coil of heavy chain (70'), 32 mining picks (all unusable), 15 chisels, 13 shovels, 11 empty barrels, 29 iron bars, 4 hacksaws, a mason's toolbox, a cobbler's toolbox, and 60 unfletched arrows."
            position(1, 0, 0)
            locationType = LocationType.UNDERGROUND
            items("hacksaw", "unfletched-arrows")
            exits {
                west("library")
                north("auxiliary-storeroom")
            }
        }

        location("auxiliary-storeroom") {
            name = "Auxiliary Storeroom"
            description = "This extra storeroom is empty of goods and supplies. In one corner is a pile of rock rubble."
            position(1, 1, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                south("implement-room")
                east("teleport-room-1")
            }
        }

        location("teleport-room-1") {
            name = "Crystalline Chamber"
            description = "A room of equal size and shape to another somewhere in the complex. At the corner farthest from the door is a shiny, sparkling outcropping of crystalline rock which dazzles when light is reflected off it. Entering this room triggers a teleportation effect, transporting the entire party to the identical other room without their knowledge!"
            position(2, 1, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                west("auxiliary-storeroom")
            }
        }

        location("teleport-room-2") {
            name = "Crystalline Chamber"
            description = "A room of equal size and shape to another somewhere in the complex. At the corner farthest from the door is a shiny, sparkling outcropping of crystalline rock which dazzles when light is reflected off it. This is the destination of the teleportation effect from the other identical room."
            position(3, 1, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                south("smithy")
            }
        }

        location("smithy") {
            name = "Smithy"
            description = "An irregularly shaped room that seems almost two separate parts. An eerie wind whistles through the upper areas near the ceiling. Three fire pits lie dormant in the northeast portion. In the center is a gigantic forging anvil, with a hand bellows hanging on the wall to the west. Blacksmith's tools and irons hang on the walls."
            position(3, 0, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                north("teleport-room-2")
                west("access-room")
            }
        }

        location("access-room") {
            name = "Access Room"
            description = "This room adjoins the smithy and provides vertical access to the lower level of the stronghold. Log sections of various sizes are stacked in the northeast corner. In the southeast portion is a large hole in the floor about 3 feet across. Looking down with a light source reveals it to be approximately 40 feet to the floor of the lower level. A large iron ring is anchored to the south wall near the hole."
            position(2, 0, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("smithy")
                down("lower-cavern-entrance")
            }
        }

        location("dead-end-room") {
            name = "Dead End Room"
            description = "A turning corridor winds inward until ending in a dead end room. The walls are unfinished, and apparently this area of the stronghold was reserved for future development."
            position(-3, 0, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("storeroom")
            }
        }

        location("meeting-room") {
            name = "Meeting Room"
            description = "A long and narrow room apparently serving as an auditorium or meeting room. Ten wooden benches are scattered about, each about 15 feet in length. A large stone slab at the north end serves as a sort of stage. On the north wall are four decorative cloth banners of red, green, blue, and yellow—now deteriorated and rotting."
            position(-3, 1, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                south("dead-end-room")
                east("garden-room")
            }
        }

        location("garden-room") {
            name = "Garden Room"
            description = "Once the showplace of the entire stronghold, the garden has become a botanical nightmare. With no one to tend the gardens, molds and fungi have grown out of control. The floor is covered with a carpet of tufted molds extending to all walls and even onto parts of the ceiling, in a rainbow of colors. Mushrooms of a hundred different kinds grow everywhere—including a 'grove' of giants with 8-foot caps!"
            position(-2, 1, 0)
            locationType = LocationType.UNDERGROUND
            creatures("giant-centipede", "giant-centipede", "giant-centipede")
            exits {
                west("meeting-room")
                north("storage-room")
            }
        }

        location("storage-room") {
            name = "Storage Room"
            description = "This room is used primarily for furniture storage. There are three large oaken tables, a number of chairs, and fourteen wooden stools stacked against the walls. In the corner opposite the door is a woodworking table with a crude vise attached, along with small saws and carpenter's equipment."
            position(-2, 2, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                south("garden-room")
                east("mistress-chamber")
            }
        }

        location("mistress-chamber") {
            name = "Mistress's Chamber"
            description = "This room is more tastefully decorated than the spartan living quarters found elsewhere. It is the personal chamber of Rogahn's mistress and lover, Melissa. A large walnut bed against the west wall has a canopy of embroidered green cloth. A small tapestry on the east wall depicts a warrior carrying a beautiful maiden, with the words: 'Melissa, the most dearly won and greatest of all my treasures.'"
            position(-1, 2, 0)
            locationType = LocationType.UNDERGROUND
            items("silver-comb", "perfume-bottles", "tortoiseshell-dish", "tapestry-melissa")
            exits {
                west("storage-room")
            }
        }

        location("rogahns-chamber") {
            name = "Rogahn's Chamber"
            description = "Rogahn's personal quarters are rather simple and spartan, showing his taste for the utilitarian rather than regal. The curving walls are covered with vertical strips of rough-finished fir wood. In each of the four curved corners is a different wall hanging tapestry depicting: a dragon being slain, a great battle in a mountain pass, a warrior and maiden on horseback, and a hero and wizard on a hilltop."
            position(-3, 1, 0)
            locationType = LocationType.UNDERGROUND
            items("tapestry-battle")
            exits {
                north("corridor-west")
                south("secret-bedroom")
            }
        }

        location("secret-bedroom") {
            name = "Secret Bedroom"
            description = "Opposite the secret door on the west wall is a bed made of maple with a feather mattress. The baseboard has an engraved letter R, but is otherwise devoid of detail. A free-standing cabinet alongside the bed contains general garments: cloaks, a leather vest, a buckskin shirt, a metal corselet, and boots."
            position(-3, 0, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                north("rogahns-chamber")
            }
        }

        location("trophy-room") {
            name = "Trophy Room"
            description = "The stronghold's trophy room consists of various curiosities accumulated over the years. Covering the north wall is an immense dragon skin, its brassy scales reflecting any illumination brightly! At the west end is a basilisk frozen in stone. On the east wall hangs a dwarfin skeleton. Also present: giant moose antlers, dragon paws with claws extended, a stuffed cockatrice, a bearskin, crossed swords, ram's horns, and barbarian tribal flags."
            position(-1, 1, 0)
            locationType = LocationType.UNDERGROUND
            items("dragon-skin", "basilisk-statue", "dwarfin-skeleton", "moose-antlers", "dragon-paws", "stuffed-cockatrice", "bearskin", "crossed-swords", "rams-horns", "barbarian-flags")
            exits {
                west("rogahns-chamber")
            }
        }

        location("throne-room") {
            name = "Throne Room"
            description = "The throne room, mostly for show, consists of two great chairs on a raised stone platform overlooking a rectangular court. The court is flanked by four large stone pillars. The floor is smooth black slate, while the two chairs are sculpted from gigantic blocks of white marble. Great draperies in yellow and purple hang on the wall behind the platform."
            position(1, 2, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                west("corridor-east")
            }
        }

        location("worship-area") {
            name = "Worship Area"
            description = "The stronghold's worship area is no more than a token gesture to the gods. On the back wall is a rock carving of a great idol—the image of a horned head with an evil visage, about 4 feet wide and 6 feet high, surrounded by religious symbols and runes. The floor is smooth black slate. In the center is a circular depression or pit, 5 feet across and 3 feet deep—a sacrifice pit with residual ash at the bottom."
            position(2, 1, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                north("corridor-east")
            }
        }

        location("captains-chamber") {
            name = "Captain's Chamber"
            description = "Home for Erig, Rogahn's friend and captain of the guard. A rather simple room with few furnishings. A crude bed sits in the southeast corner, with a table alongside. On the table is a stoneware crock, a large earthenware tankard mug, and a small hand mirror. A wooden chest on the south wall contains garments and a leather pouch with an engraved walnut plaque: 'To Erig, great and trusted fighter...'"
            position(3, 2, 0)
            locationType = LocationType.UNDERGROUND
            items("walnut-plaque")
            exits {
                west("corridor-east")
            }
        }

        location("armory") {
            name = "Armory"
            description = "In the northeast corner is a wooden keg stand with a single barrel marked 'SD'—if broken open, ale issues forth. On the wall at the western extremity are numerous pegs and brackets for holding arms and armor. The wall is mostly empty, except for two shields and a heavy mace hanging thereon."
            position(3, 3, 0)
            locationType = LocationType.UNDERGROUND
            items("shield-large", "heavy-mace")
            exits {
                south("captains-chamber")
                west("room-of-pools")
            }
        }

        location("room-of-pools") {
            name = "Room of Pools"
            description = "This room is the largest on the upper level, and quite different from all others. The walls are rough blackish stone, and the floor is covered with ceramic tiles in golden brown, with white and black patterns forming striking designs. Arrayed throughout the room are fourteen different pools, each about ten feet in diameter with sides sloping to a maximum depth of five feet. Each pool contains a different mysterious liquid..."
            position(2, 3, 0)
            locationType = LocationType.UNDERGROUND
            items("pool-liquid-healing", "brass-key-worthless")
            exits {
                east("armory")
                south("corridor-east")
                down("lower-cavern-main")
            }
        }

        // === LOWER LEVEL ===
        location("lower-cavern-entrance") {
            name = "Lower Cavern Entrance"
            description = "A natural cavern at the bottom of the shaft from the access room above. The air is heavy, wet, and musty. Dust lies everywhere. The irregular rock walls are rough and blackish."
            position(2, 0, -1)
            locationType = LocationType.UNDERGROUND
            exits {
                up("access-room")
                west("fungus-cavern")
            }
        }

        location("fungus-cavern") {
            name = "Fungus Cavern"
            description = "A large natural cavern with various types of fungi growing in patches. The air is thick with spores. Some of the mushrooms glow faintly with bioluminescence."
            position(1, 0, -1)
            locationType = LocationType.UNDERGROUND
            creatures("troglodyte", "troglodyte")
            exits {
                east("lower-cavern-entrance")
                north("underground-lake")
                west("spider-lair")
            }
        }

        location("underground-lake") {
            name = "Underground Lake"
            description = "A vast underground lake fills most of this cavern. The water is dark and still. Strange ripples occasionally disturb the surface, suggesting something lives in the depths."
            position(1, 1, -1)
            locationType = LocationType.UNDERGROUND
            creatures("giant-rat", "giant-rat", "giant-rat")
            exits {
                south("fungus-cavern")
                east("lower-cavern-main")
            }
        }

        location("lower-cavern-main") {
            name = "Lower Cavern Main"
            description = "The main cavern of the lower level, with passages leading in multiple directions. Stalactites hang from the ceiling, and the sound of dripping water echoes throughout."
            position(2, 1, -1)
            locationType = LocationType.UNDERGROUND
            exits {
                west("underground-lake")
                up("room-of-pools")
                south("orc-camp")
                east("bone-pit")
            }
        }

        location("orc-camp") {
            name = "Orc Encampment"
            description = "A group of orcs has made camp in this cavern. Crude bedrolls, a fire pit with smoldering embers, and gnawed bones litter the area. The orcs have claimed this section of the lower level as their territory."
            position(2, 0, -1)
            locationType = LocationType.UNDERGROUND
            creatures("orc", "orc", "orc", "orc-leader")
            exits {
                north("lower-cavern-main")
            }
        }

        location("bone-pit") {
            name = "Bone Pit"
            description = "A deep pit filled with bones—the remains of countless creatures, both animal and humanoid. The smell is overpowering. Something moves among the bones..."
            position(3, 1, -1)
            locationType = LocationType.UNDERGROUND
            creatures("skeleton", "skeleton", "zombie")
            exits {
                west("lower-cavern-main")
            }
        }

        location("spider-lair") {
            name = "Spider Lair"
            description = "Thick webs fill this cavern from floor to ceiling. Cocooned shapes—some disturbingly humanoid—hang from the webs. The resident spiders are always hungry for fresh prey."
            position(0, 0, -1)
            locationType = LocationType.UNDERGROUND
            creatures("giant-spider", "giant-spider", "black-widow-spider")
            exits {
                east("fungus-cavern")
            }
        }
    }

    // ==================== CHESTS ====================
    private fun defineChests() {
        chest("wizards-chest") {
            name = "Zelligar's Locked Chest"
            description = "A sturdy chest in Zelligar's chamber with a brass lock. The drawer handle has a pin trap!"
            locationSuffix = "wizards-chamber"
            isLocked = true
            lockDifficulty = 3
            bashDifficulty = 4
            goldAmount = 50
            lootTableSuffix = "wizard-chamber"
        }

        chest("captains-chest") {
            name = "Erig's Wooden Chest"
            description = "A wooden chest belonging to Erig, captain of the guard."
            locationSuffix = "captains-chamber"
            isLocked = true
            lockDifficulty = 2
            bashDifficulty = 2
            goldAmount = 25
        }

        chest("orc-treasure") {
            name = "Orc Hoard"
            description = "A pile of stolen goods the orcs have accumulated."
            locationSuffix = "orc-camp"
            guardianCreatureSuffix = "orc-leader"
            isLocked = false
            goldAmount = 75
            lootTableSuffix = "orc-loot"
        }
    }

    // ==================== POOLS ====================
    /**
     * The famous Room of Pools from B1 - 14 magical pools with different effects.
     * Based on the original module's description of each pool's properties.
     */
    private fun definePools() {
        // Pool 1: Clear, still water - Actually healing
        pool("pool-1-healing") {
            name = "Pool of Clear Water"
            description = "A pool of perfectly clear, still water. It looks refreshing and pure."
            locationSuffix = "room-of-pools"
            liquidColor = "clear"
            liquidAppearance = "still"
            healing(dice = "1d6+1", curesDisease = true, curesPoison = false)
            identifyDifficulty = 2
            usesPerDay = 1
            message("You drink the clear water. It is cool and refreshing, and you feel your wounds begin to heal!")
            secretMessage("This pool has healing properties and cures disease.")
        }

        // Pool 2: Pinkish liquid - Extra healing
        pool("pool-2-pink-healing") {
            name = "Pool of Pink Liquid"
            description = "A pool containing a pinkish liquid with a faint floral scent."
            locationSuffix = "room-of-pools"
            liquidColor = "pink"
            liquidAppearance = "still"
            healing(dice = "2d4", curesDisease = true, curesPoison = true)
            identifyDifficulty = 3
            usesPerDay = 1
            message("The pink liquid is surprisingly sweet. Your wounds close and any ailments fade away!")
            secretMessage("A powerful healing pool that cures both disease and poison.")
        }

        // Pool 3: Green, bubbling - Acid damage
        pool("pool-3-acid") {
            name = "Pool of Green Liquid"
            description = "A pool of sickly green liquid that bubbles and hisses. Wisps of vapor rise from its surface."
            locationSuffix = "room-of-pools"
            liquidColor = "green"
            liquidAppearance = "bubbling"
            damage(dice = "1d8", type = "acid")
            identifyDifficulty = 1
            message("The green liquid burns! It's acid!")
            secretMessage("Highly corrosive acid - do not touch!")
        }

        // Pool 4: Dark red - Wine
        pool("pool-4-wine") {
            name = "Pool of Dark Red Liquid"
            description = "A pool of dark red liquid that smells faintly of fermentation."
            locationSuffix = "room-of-pools"
            liquidColor = "dark red"
            liquidAppearance = "still"
            wine("It's wine! Quite good, actually. You feel slightly relaxed.")
            identifyDifficulty = 1
            message("The liquid is wine! It has a rich, full-bodied flavor.")
            secretMessage("An apparently inexhaustible pool of fine red wine.")
        }

        // Pool 5: Milky white - Slow poison
        pool("pool-5-poison") {
            name = "Pool of Milky White Liquid"
            description = "A pool of opaque, milky white liquid with no discernible odor."
            locationSuffix = "room-of-pools"
            liquidColor = "milky white"
            liquidAppearance = "still"
            poison(damageDice = "1d4", durationRounds = 10, chance = 0.75f)
            identifyDifficulty = 4
            message("The liquid tastes slightly bitter... Your stomach begins to churn ominously.")
            secretMessage("Slow-acting poison! Damages over time.")
        }

        // Pool 6: Pale blue - Strength buff
        pool("pool-6-strength") {
            name = "Pool of Pale Blue Liquid"
            description = "A pool of pale blue liquid that seems to shimmer with inner light."
            locationSuffix = "room-of-pools"
            liquidColor = "pale blue"
            liquidAppearance = "shimmering"
            buff("strength", modifier = 2, durationMinutes = 30)
            identifyDifficulty = 3
            usesPerDay = 1
            message("Power surges through your muscles! You feel incredibly strong!")
            secretMessage("Temporarily increases strength by +2 for 30 minutes.")
        }

        // Pool 7: Golden - Speed/dexterity buff
        pool("pool-7-speed") {
            name = "Pool of Golden Liquid"
            description = "A pool of liquid that gleams like molten gold, constantly swirling."
            locationSuffix = "room-of-pools"
            liquidColor = "golden"
            liquidAppearance = "swirling"
            buff("dexterity", modifier = 2, durationMinutes = 20)
            identifyDifficulty = 3
            usesPerDay = 1
            message("You feel light as a feather and quick as lightning!")
            secretMessage("Temporarily increases dexterity by +2 for 20 minutes.")
        }

        // Pool 8: Black - Weakness debuff
        pool("pool-8-weakness") {
            name = "Pool of Black Liquid"
            description = "A pool of inky black liquid that seems to absorb all light around it."
            locationSuffix = "room-of-pools"
            liquidColor = "black"
            liquidAppearance = "still"
            debuff("strength", modifier = 2, durationMinutes = 30)
            identifyDifficulty = 3
            message("A terrible weakness washes over you...")
            secretMessage("Temporarily decreases strength by -2 for 30 minutes.")
        }

        // Pool 9: Orange - Sleep inducing
        pool("pool-9-sleep") {
            name = "Pool of Orange Liquid"
            description = "A pool of warm orange liquid with a pleasant, drowsy aroma."
            locationSuffix = "room-of-pools"
            liquidColor = "orange"
            liquidAppearance = "still"
            condition("sleeping", duration = 60, chance = 0.7f)
            identifyDifficulty = 2
            message("Overwhelming drowsiness washes over you... so tired...")
            secretMessage("Induces magical sleep with 70% chance.")
        }

        // Pool 10: Silver - Charm effect
        pool("pool-10-charm") {
            name = "Pool of Silver Liquid"
            description = "A pool of shimmering silver liquid that reflects your face in strange ways."
            locationSuffix = "room-of-pools"
            liquidColor = "silver"
            liquidAppearance = "shimmering"
            condition("charmed", duration = 30, chance = 0.5f)
            identifyDifficulty = 4
            message("Everything seems wonderful! The world is beautiful!")
            secretMessage("Has a 50% chance to charm the drinker.")
        }

        // Pool 11: Bronze/copper - Contains treasure (brass key)
        pool("pool-11-treasure") {
            name = "Pool of Copper-Colored Liquid"
            description = "A pool of liquid the color of old copper. Something glints at the bottom."
            locationSuffix = "room-of-pools"
            liquidColor = "copper"
            liquidAppearance = "still"
            treasure(itemSuffix = "brass-key-worthless", gold = 0)
            identifyDifficulty = 1
            isOneTimeUse = true
            message("You reach into the pool and fish out a large brass key!")
            secretMessage("Contains a brass key at the bottom (worthless, fits no locks).")
        }

        // Pool 12: Purple - Teleportation
        pool("pool-12-teleport") {
            name = "Pool of Purple Liquid"
            description = "A pool of deep purple liquid that swirls in hypnotic patterns."
            locationSuffix = "room-of-pools"
            liquidColor = "purple"
            liquidAppearance = "swirling"
            teleport("lower-cavern-main")
            identifyDifficulty = 5
            message("The world spins around you! When your vision clears, you're somewhere else!")
            secretMessage("Teleports to the Lower Cavern Main area.")
        }

        // Pool 13: Glowing green - Minor healing + green glow effect
        pool("pool-13-green-glow") {
            name = "Pool of Glowing Green Liquid"
            description = "A pool of bright green liquid that emits an eerie phosphorescent glow."
            locationSuffix = "room-of-pools"
            liquidColor = "bright green"
            liquidAppearance = "glowing"
            healing(amount = 2)
            identifyDifficulty = 2
            message("The glowing liquid tastes like mint! You feel slightly better, and your skin glows green for a moment.")
            secretMessage("Heals 2 HP and makes the drinker glow briefly.")
        }

        // Pool 14: Empty/dry
        pool("pool-14-empty") {
            name = "Empty Pool"
            description = "This pool is completely dry, with only mineral deposits and dust at the bottom."
            locationSuffix = "room-of-pools"
            liquidColor = "none"
            liquidAppearance = "dry"
            empty("The pool is bone dry. Whatever was here is long gone.")
            identifyDifficulty = 0
        }
    }
}
