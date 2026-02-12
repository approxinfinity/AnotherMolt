package com.ez2bg.anotherthread.database.modules

import com.ez2bg.anotherthread.database.*

/**
 * The Caves of Chaos - Based on classic D&D Module B2 "The Keep on the Borderlands" by Gary Gygax
 *
 * A ravine filled with interconnected caves, each home to different humanoid tribes:
 * - Cave A: Kobold Lair (weakest, good for level 1)
 * - Cave B: Orc Lair (first orc tribe)
 * - Cave C: Orc Lair (second orc tribe, rivals of B)
 * - Cave D: Goblin Lair
 * - Cave E: Ogre Cave (mercenary who works for goblins/hobgoblins)
 * - Cave F: Hobgoblin Lair (most organized)
 * - Cave G: Shunned Cavern (owlbear, gray oozes)
 * - Cave H: Bugbear Lair (cunning ambushers)
 * - Cave I: Caves of the Minotaur (confusing labyrinth)
 * - Cave J: Gnoll Lair
 * - Cave K: Shrine of Evil Chaos (the cult controlling everything)
 *
 * Notable features:
 * - Tribes sometimes war with each other (goblins vs hobgoblins, orc tribes rival)
 * - Secret passages connect some caves
 * - Prisoners can be rescued from hobgoblins and the temple
 * - The Evil Priest in Cave K manipulates the tribes from behind the scenes
 */
object CavesOfChaosModule : AdventureModuleSeed() {

    override val moduleId = "caves-of-chaos"
    override val moduleName = "The Caves of Chaos"
    override val moduleDescription = "A ravine filled with monster-infested caves on the borderlands of civilization. Multiple humanoid tribes vie for control, while a dark cult lurks in the deepest chambers, manipulating events from the shadows."
    override val attribution = "Inspired by D&D Module B2 - The Keep on the Borderlands by Gary Gygax (TSR, 1979)"
    override val recommendedLevelMin = 1
    override val recommendedLevelMax = 3

    override fun defineContent() {
        defineAbilities()
        defineItems()
        defineLootTables()
        defineCreatures()
        defineLocations()
        defineChests()
        defineTraps()
        defineFactions()
    }

    // ==================== ABILITIES ====================
    private fun defineAbilities() {
        // === KOBOLD ABILITIES ===
        ability("pack-tactics") {
            name = "Pack Tactics"
            description = "The kobold fights more effectively when allies are nearby, gaining advantage on attacks."
            abilityType = "passive"
            targetType = "self"
            effects = """[{"type":"passive","effect":"pack_tactics","bonusDamage":2}]"""
        }

        ability("sling-shot") {
            name = "Sling Shot"
            description = "Hurls a stone with a sling at range."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 30
            baseDamage = 4
        }

        // === GOBLIN ABILITIES ===
        ability("nimble-escape") {
            name = "Nimble Escape"
            description = "The goblin can disengage or hide as a bonus action, making them slippery foes."
            abilityType = "utility"
            targetType = "self"
            effects = """[{"type":"utility","effect":"disengage_or_hide"}]"""
        }

        ability("shortbow") {
            name = "Shortbow"
            description = "Fires an arrow from a crude shortbow."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 40
            baseDamage = 6
        }

        ability("scimitar-slash") {
            name = "Scimitar Slash"
            description = "A quick slash with a curved blade."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 6
        }

        ability("bree-yark") {
            name = "Bree-Yark!"
            description = "The goblin shouts an alarm cry, alerting nearby allies. ('Bree-Yark' means 'We surrender!' ...or does it?)"
            abilityType = "utility"
            targetType = "area"
            range = 60
            effects = """[{"type":"utility","effect":"alert_allies"}]"""
        }

        // === ORC ABILITIES ===
        ability("aggressive-charge") {
            name = "Aggressive Charge"
            description = "The orc rushes forward with brutal fury, closing distance to strike."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 10
            baseDamage = 10
            cooldownType = "short"
            cooldownRounds = 2
            effects = """[{"type":"damage","modifier":10},{"type":"movement","bonus":10}]"""
        }

        ability("greataxe-cleave") {
            name = "Greataxe Cleave"
            description = "A powerful swing with a heavy axe that can cleave through armor."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 12
        }

        ability("javelin-throw") {
            name = "Javelin Throw"
            description = "Hurls a javelin at a distant target."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 30
            baseDamage = 8
        }

        // === HOBGOBLIN ABILITIES ===
        ability("martial-discipline") {
            name = "Martial Discipline"
            description = "The hobgoblin fights with military precision, dealing extra damage when allies coordinate attacks."
            abilityType = "passive"
            targetType = "self"
            effects = """[{"type":"passive","effect":"martial_advantage","bonusDamage":7}]"""
        }

        ability("longsword-strike") {
            name = "Longsword Strike"
            description = "A disciplined strike with a well-maintained longsword."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 8
        }

        ability("longbow") {
            name = "Longbow"
            description = "Fires an arrow from a military longbow with deadly accuracy."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 60
            baseDamage = 8
        }

        ability("whip-crack") {
            name = "Whip Crack"
            description = "Cracks a whip that can stun opponents at range."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 15
            baseDamage = 6
            effects = """[{"type":"damage","modifier":6},{"type":"condition","condition":"stunned","duration":1,"chance":0.3}]"""
        }

        // === GNOLL ABILITIES ===
        ability("rampage") {
            name = "Rampage"
            description = "When the gnoll reduces a creature to 0 HP, it can immediately move and attack again."
            abilityType = "passive"
            targetType = "self"
            effects = """[{"type":"passive","effect":"rampage_on_kill"}]"""
        }

        ability("spear-thrust") {
            name = "Spear Thrust"
            description = "A vicious thrust with a barbed spear."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 10
            baseDamage = 9
        }

        ability("bite") {
            name = "Bite"
            description = "A savage bite with powerful jaws."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 7
        }

        // === BUGBEAR ABILITIES ===
        ability("surprise-attack") {
            name = "Surprise Attack"
            description = "The bugbear excels at ambushes, dealing massive bonus damage to surprised targets."
            abilityType = "passive"
            targetType = "self"
            effects = """[{"type":"passive","effect":"surprise_bonus","bonusDamage":14}]"""
        }

        ability("morningstar-crush") {
            name = "Morningstar Crush"
            description = "A brutal overhead smash with a spiked morningstar."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 11
        }

        ability("javelin-hurl") {
            name = "Javelin Hurl"
            description = "Hurls a heavy javelin with tremendous force."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 30
            baseDamage = 10
        }

        // === OGRE ABILITIES ===
        ability("greatclub-smash") {
            name = "Greatclub Smash"
            description = "A devastating blow with a massive wooden club."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 15
        }

        ability("rock-throw") {
            name = "Rock Throw"
            description = "Hurls a large rock at enemies."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 40
            baseDamage = 12
        }

        // === OWLBEAR ABILITIES ===
        ability("multiattack-claw") {
            name = "Rending Claws"
            description = "Slashes with both claws in rapid succession."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 14
        }

        ability("beak-strike") {
            name = "Beak Strike"
            description = "A powerful snap with the owlbear's hooked beak."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 10
        }

        // === MINOTAUR ABILITIES ===
        ability("gore") {
            name = "Gore"
            description = "Charges and gores with massive horns."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 10
            baseDamage = 16
            cooldownType = "short"
            cooldownRounds = 2
            effects = """[{"type":"damage","modifier":16},{"type":"condition","condition":"knocked_prone","chance":0.5}]"""
        }

        ability("greataxe-swing") {
            name = "Greataxe Swing"
            description = "A mighty swing with a massive axe."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 14
        }

        ability("labyrinth-sense") {
            name = "Labyrinth Sense"
            description = "The minotaur has perfect recall of any path it has traveled, making it impossible to lose in its lair."
            abilityType = "passive"
            targetType = "self"
            effects = """[{"type":"passive","effect":"labyrinth_sense"}]"""
        }

        // === UNDEAD ABILITIES ===
        ability("slam") {
            name = "Slam"
            description = "A heavy blow with rotting fists."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 6
        }

        ability("life-drain") {
            name = "Life Drain"
            description = "Drains the life force from a living creature, healing the wight."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 10
            cooldownType = "medium"
            cooldownRounds = 3
            effects = """[{"type":"damage","damageType":"necrotic","modifier":10},{"type":"heal","modifier":5}]"""
        }

        // === CULT ABILITIES ===
        ability("dark-blessing") {
            name = "Dark Blessing"
            description = "Channels dark energy to heal an ally."
            abilityType = "spell"
            targetType = "single_ally"
            range = 30
            baseDamage = 0
            cooldownType = "medium"
            cooldownRounds = 3
            manaCost = 10
            effects = """[{"type":"heal","modifier":12}]"""
        }

        ability("cause-light-wounds") {
            name = "Cause Light Wounds"
            description = "Inflicts wounds through dark magic. Requires touch."
            abilityType = "spell"
            targetType = "single_enemy"
            range = 5
            baseDamage = 8
            cooldownType = "short"
            cooldownRounds = 1
            manaCost = 5
            effects = """[{"type":"damage","damageType":"necrotic","modifier":8}]"""
        }

        ability("cause-fear") {
            name = "Cause Fear"
            description = "Fills an enemy with supernatural dread."
            abilityType = "spell"
            targetType = "single_enemy"
            range = 30
            baseDamage = 0
            cooldownType = "medium"
            cooldownRounds = 3
            manaCost = 8
            effects = """[{"type":"condition","condition":"frightened","duration":3}]"""
        }

        ability("hold-person") {
            name = "Hold Person"
            description = "Paralyzes a humanoid target."
            abilityType = "spell"
            targetType = "single_enemy"
            range = 30
            baseDamage = 0
            cooldownType = "long"
            cooldownRounds = 0
            manaCost = 15
            effects = """[{"type":"condition","condition":"paralyzed","duration":2}]"""
        }

        ability("snake-staff-strike") {
            name = "Snake Staff Strike"
            description = "The staff transforms into a snake and strikes, holding the victim helpless."
            abilityType = "spell"
            targetType = "single_enemy"
            range = 5
            baseDamage = 8
            cooldownType = "medium"
            cooldownRounds = 4
            effects = """[{"type":"damage","modifier":8},{"type":"condition","condition":"restrained","duration":4}]"""
        }

        ability("animate-dead") {
            name = "Animate Dead"
            description = "Raises fallen creatures as undead servants."
            abilityType = "spell"
            targetType = "area"
            range = 30
            baseDamage = 0
            cooldownType = "long"
            cooldownRounds = 0
            manaCost = 25
            effects = """[{"type":"summon","creature":"zombie","count":2}]"""
        }

        // === SPECIAL CREATURE ABILITIES ===
        ability("gray-ooze-acid") {
            name = "Corrosive Touch"
            description = "The gray ooze's touch dissolves metal and burns flesh."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 10
            effects = """[{"type":"damage","damageType":"acid","modifier":10},{"type":"special","effect":"corrode_armor"}]"""
        }

        ability("stirge-blood-drain") {
            name = "Blood Drain"
            description = "Attaches to target and drains blood each round until killed or removed."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 5
            baseDamage = 4
            effects = """[{"type":"damage","modifier":4},{"type":"dot","damageType":"blood_drain","damage":4,"duration":99}]"""
        }

        ability("fire-beetle-glow") {
            name = "Fire Glands"
            description = "The beetle's glowing glands can be harvested for light."
            abilityType = "passive"
            targetType = "self"
            effects = """[{"type":"passive","effect":"light_source","radius":10}]"""
        }

        ability("medusa-gaze") {
            name = "Petrifying Gaze"
            description = "Those who meet the medusa's gaze risk being turned to stone."
            abilityType = "combat"
            targetType = "single_enemy"
            range = 30
            baseDamage = 0
            cooldownType = "short"
            cooldownRounds = 1
            effects = """[{"type":"condition","condition":"petrified","duration":99,"saveDC":14}]"""
        }

        ability("gelatinous-cube-engulf") {
            name = "Engulf"
            description = "The cube moves over creatures, engulfing them in its acidic body."
            abilityType = "combat"
            targetType = "area"
            range = 5
            baseDamage = 12
            effects = """[{"type":"damage","damageType":"acid","modifier":12},{"type":"condition","condition":"restrained","duration":1}]"""
        }
    }

    // ==================== ITEMS ====================
    private fun defineItems() {
        // === COMMON WEAPONS ===
        item("rusty-dagger") {
            name = "Rusty Dagger"
            description = "A small, pitted dagger. Barely functional but still dangerous."
            value = 2
            weapon()
            stats(attack = 2)
        }

        item("crude-spear") {
            name = "Crude Spear"
            description = "A fire-hardened wooden spear with a sharpened tip."
            value = 3
            weapon()
            stats(attack = 3)
        }

        item("hand-axe") {
            name = "Hand Axe"
            description = "A small axe suitable for throwing or melee combat."
            value = 5
            weapon()
            stats(attack = 4)
        }

        item("crude-shortbow") {
            name = "Crude Shortbow"
            description = "A poorly made bow held together with sinew and hope."
            value = 5
            weapon()
            stats(attack = 3)
        }

        item("goblin-scimitar") {
            name = "Goblin Scimitar"
            description = "A curved blade favored by goblin warriors. Wickedly sharp despite its crude appearance."
            value = 10
            weapon()
            stats(attack = 5)
        }

        item("orcish-greataxe") {
            name = "Orcish Greataxe"
            description = "A heavy, brutal axe favored by orc warriors. The blade is chipped but deadly."
            value = 25
            weapon()
            stats(attack = 8, defense = -1)
        }

        item("hobgoblin-longsword") {
            name = "Hobgoblin Longsword"
            description = "A well-maintained military sword taken from a hobgoblin soldier."
            value = 40
            weapon()
            stats(attack = 6, defense = 1)
        }

        item("bugbear-morningstar") {
            name = "Bugbear Morningstar"
            description = "A heavy spiked club favored by bugbear ambushers."
            value = 50
            weapon()
            stats(attack = 9)
        }

        // === MAGIC WEAPONS ===
        item("magic-hand-axe-plus-1") {
            name = "Hand Axe +1"
            description = "A finely crafted hand axe that gleams with magical enhancement."
            value = 200
            weapon()
            stats(attack = 7)
        }

        item("magic-sword-plus-1") {
            name = "Sword +1"
            description = "A longsword enchanted with minor magic, making it lighter and sharper."
            value = 400
            weapon()
            stats(attack = 9, defense = 1)
        }

        item("magic-sword-plus-2") {
            name = "Sword +2"
            description = "A powerful enchanted blade that glows faintly in the presence of evil."
            value = 1000
            weapon()
            stats(attack = 12, defense = 2)
        }

        item("magic-spear-plus-1") {
            name = "Spear +1"
            description = "An enchanted spear that flies true when thrown and returns to its wielder's hand."
            value = 350
            weapon()
            stats(attack = 8)
        }

        item("cursed-sword-minus-1") {
            name = "Cursed Sword"
            description = "A blade that appears magical but is actually cursed. Once wielded, it cannot be willingly discarded."
            value = 0
            weapon()
            stats(attack = 3, defense = -2)
        }

        // === ARMOR ===
        item("leather-armor") {
            name = "Leather Armor"
            description = "Basic armor made from hardened leather."
            value = 20
            armor("chest")
            stats(defense = 3)
        }

        item("chain-mail") {
            name = "Chain Mail"
            description = "Armor made of interlocking metal rings, offering good protection."
            value = 40
            armor("chest")
            stats(defense = 5, maxHp = 5)
        }

        item("plate-mail") {
            name = "Plate Mail"
            description = "Heavy armor made of metal plates, offering excellent protection but limiting mobility."
            value = 60
            armor("chest")
            stats(defense = 7, maxHp = 10)
        }

        item("gnoll-hide-armor") {
            name = "Gnoll Hide Armor"
            description = "Crudely stitched armor made from various animal hides. Smells terrible."
            value = 20
            armor("chest")
            stats(defense = 4, maxHp = 5)
        }

        item("magic-plate-plus-1") {
            name = "Plate Mail +1"
            description = "Enchanted plate armor that moves with supernatural ease."
            value = 500
            armor("chest")
            stats(defense = 9, maxHp = 15)
        }

        item("magic-shield-plus-1") {
            name = "Shield +1"
            description = "An enchanted shield that seems to deflect blows on its own."
            value = 200
            armor("off_hand")
            stats(defense = 4)
        }

        // === ACCESSORIES ===
        item("silver-chain-necklace") {
            name = "Silver Chain Necklace"
            description = "A thin silver chain worn by the kobold chieftain. Worth a fair sum."
            value = 50
        }

        item("gold-chain-necklace") {
            name = "Gold Chain Necklace"
            description = "A heavy gold chain, likely stolen from wealthy travelers."
            value = 150
        }

        item("gem-studded-belt") {
            name = "Gem-Studded Belt"
            description = "A leather belt decorated with semi-precious gems."
            value = 160
        }

        item("silver-armbands") {
            name = "Silver Armbands"
            description = "A pair of ornate silver armbands worn by the gnoll chieftain."
            value = 100
        }

        item("gold-ring-black-gem") {
            name = "Gold Ring with Black Gem"
            description = "A gold ring set with a black gem that seems to absorb light. Worn by the Evil Priest."
            value = 1400
            accessory("finger")
            stats(attack = 2, defense = 1)
        }

        item("bracelet-of-ivory") {
            name = "Ivory Bracelet"
            description = "A delicately carved ivory bracelet worth a small fortune."
            value = 100
        }

        // === MAGIC ACCESSORIES ===
        item("ring-of-protection-plus-1") {
            name = "Ring of Protection +1"
            description = "A magical ring that creates a subtle defensive barrier around the wearer."
            value = 500
            accessory("finger")
            stats(defense = 3)
        }

        item("amulet-of-protection-from-good") {
            name = "Amulet of Protection from Good"
            description = "A dark amulet that shields the wearer from attacks by creatures of good alignment. Worn by cult members."
            value = 300
            accessory("neck")
            stats(defense = 2)
        }

        item("amulet-of-protection-from-turning") {
            name = "Amulet of Protection from Turning"
            description = "This unholy amulet protects undead from being turned by clerics."
            value = 200
            accessory("neck")
        }

        item("elven-boots") {
            name = "Elven Boots"
            description = "Soft leather boots that allow the wearer to move silently."
            value = 400
            armor("feet")
            stats(defense = 1)
        }

        item("elven-cloak") {
            name = "Elven Cloak"
            description = "A shimmering cloak that helps the wearer blend into natural surroundings."
            value = 500
            armor("back")
            stats(defense = 1)
        }

        // === POTIONS ===
        item("potion-of-healing") {
            name = "Potion of Healing"
            description = "A red liquid that restores health when consumed."
            value = 50
        }

        item("potion-of-invisibility") {
            name = "Potion of Invisibility"
            description = "Drinking this potion renders the user invisible for a short time."
            value = 200
        }

        item("potion-of-poison") {
            name = "Potion of Poison"
            description = "A deadly poison disguised as a healing potion. Tastes faintly of almonds."
            value = 0
        }

        item("potion-of-gaseous-form") {
            name = "Potion of Gaseous Form"
            description = "Transforms the drinker into a cloud of mist, able to pass through small openings."
            value = 300
        }

        item("potion-of-levitation") {
            name = "Potion of Levitation"
            description = "Allows the drinker to float in the air and move vertically at will."
            value = 250
        }

        item("potion-of-stone-to-flesh") {
            name = "Potion of Stone to Flesh"
            description = "Can restore a petrified creature to normal. Enough for six uses."
            value = 800
        }

        // === SCROLLS ===
        item("scroll-of-protection-from-undead") {
            name = "Scroll of Protection from Undead"
            description = "Reading this scroll creates a barrier that undead cannot cross."
            value = 200
        }

        item("scroll-of-hold-person") {
            name = "Scroll of Hold Person"
            description = "A clerical scroll containing the Hold Person spell."
            value = 150
        }

        item("scroll-with-cleric-spells") {
            name = "Scroll of Clerical Magic"
            description = "Contains three spells: detect magic, hold person, and silence 15' radius."
            value = 400
        }

        // === STAVES & WANDS ===
        item("staff-of-healing") {
            name = "Staff of Healing"
            description = "A holy staff that can cure wounds when its charges are expended."
            value = 600
            weapon()
            stats(attack = 4, maxHp = 10)
        }

        item("wand-of-paralyzation") {
            name = "Wand of Paralyzation"
            description = "A sinister wand that can paralyze targets. Has 7 charges remaining."
            value = 700
            weapon()
            stats(attack = 3)
        }

        item("wand-of-enemy-detection") {
            name = "Wand of Enemy Detection"
            description = "Points toward the nearest hostile creature. Has 9 charges remaining."
            value = 400
        }

        item("snake-staff") {
            name = "Snake Staff"
            description = "The Evil Priest's staff that can transform into a constricting snake on command."
            value = 1000
            weapon()
            stats(attack = 7)
        }

        // === TREASURE ITEMS ===
        item("silver-goblet") {
            name = "Silver Goblet"
            description = "An ornate silver drinking cup, likely stolen from travelers."
            value = 75
        }

        item("gold-flagon") {
            name = "Golden Flagon"
            description = "A gold drinking vessel worth a small fortune."
            value = 500
        }

        item("jeweled-goblet") {
            name = "Jewel-Encrusted Goblet"
            description = "A gold goblet set with gems, found in the shallow pool."
            value = 1300
        }

        item("alabaster-statue") {
            name = "Alabaster and Gold Statue"
            description = "A 30-pound statue of alabaster and ivory. Requires strength to carry."
            value = 200
        }

        item("silver-urn") {
            name = "Silver Urn"
            description = "A blackened silver urn worth considerable gold if cleaned."
            value = 175
        }

        item("tapestry-silver-gold") {
            name = "Tapestry of Silver and Gold"
            description = "A valuable tapestry woven with silver and gold threads."
            value = 900
        }

        item("sable-cloak") {
            name = "Valuable Sable Cloak"
            description = "A luxurious cloak made from sable fur."
            value = 450
        }

        item("copper-bowl-silver") {
            name = "Silver-Chased Copper Bowl"
            description = "A copper bowl decorated with silver filigree."
            value = 75
        }

        item("malachite-bowl") {
            name = "Malachite Bowl"
            description = "A bowl carved from solid malachite, found in the Castellan's chamber."
            value = 750
        }

        // === GEMS ===
        item("gem-50gp") {
            name = "Semi-Precious Gem"
            description = "A polished semi-precious stone."
            value = 50
        }

        item("gem-100gp") {
            name = "Precious Gem"
            description = "A valuable cut gemstone that sparkles in the light."
            value = 100
        }

        item("gem-500gp") {
            name = "Large Red Gem"
            description = "A large, blood-red gem of exceptional quality."
            value = 500
        }

        item("gem-1000gp") {
            name = "Flawless Diamond"
            description = "A perfectly cut diamond that catches light beautifully."
            value = 1000
        }

        item("large-gem-chieftain") {
            name = "Large Gem on Golden Chain"
            description = "An enormous gem on a golden chain, worn by the kobold chieftain."
            value = 1200
        }

        // === SPECIAL ITEMS ===
        item("fire-beetle-gland") {
            name = "Fire Beetle Gland"
            description = "A glowing gland from a fire beetle. Provides light for 1-6 days after the beetle's death."
            value = 5
        }

        item("helm-of-alignment-change") {
            name = "Helm of Alignment Change"
            description = "A cursed helm that changes the wearer's moral alignment. Found in the Evil Priest's treasure."
            value = 0
        }

        item("demon-idol") {
            name = "Demon Idol"
            description = "A hideous idol that topples and crushes anyone other than the priest who touches it. Its gem eyes are worth 100gp each."
            value = 200
        }

        item("evil-altar-vessels") {
            name = "Bloodstained Bronze Vessels"
            description = "Ancient bronze ritual vessels from the altar, stained with old blood. Evil relics worth gold to collectors."
            value = 4000
        }

        item("rope-of-climbing") {
            name = "Rope of Climbing"
            description = "A magical rope that can climb on command."
            value = 400
        }

        // === KEYS ===
        item("key-kobold-storage") {
            name = "Kobold Storage Key"
            description = "A small iron key that opens the kobold food storage room."
            value = 1
        }

        item("key-orc-leader") {
            name = "Orc Leader's Key"
            description = "A heavy iron key worn by the orc leader."
            value = 1
        }

        item("keys-hobgoblin-slave") {
            name = "Slave Pen Keys"
            description = "Keys to the hobgoblin slave pens."
            value = 1
        }

        item("key-bugbear-spoils") {
            name = "Bugbear Chieftain's Key"
            description = "A key to the bugbear spoils room."
            value = 1
        }
    }

    // ==================== LOOT TABLES ====================
    private fun defineLootTables() {
        lootTable("kobold") {
            name = "Kobold Loot"
            item("rusty-dagger", chance = 0.1f)
        }

        lootTable("kobold-chief") {
            name = "Kobold Chieftain Loot"
            item("large-gem-chieftain", chance = 1.0f)
            item("key-kobold-storage", chance = 1.0f)
        }

        lootTable("goblin") {
            name = "Goblin Loot"
            item("rusty-dagger", chance = 0.15f)
            item("crude-shortbow", chance = 0.1f)
        }

        lootTable("goblin-chief") {
            name = "Goblin Chieftain Loot"
            item("goblin-scimitar", chance = 0.5f)
            item("silver-goblet", chance = 0.3f)
        }

        lootTable("orc") {
            name = "Orc Loot"
            item("hand-axe", chance = 0.2f)
            item("crude-spear", chance = 0.15f)
        }

        lootTable("orc-leader") {
            name = "Orc Leader Loot"
            item("orcish-greataxe", chance = 0.5f)
            item("gem-studded-belt", chance = 0.4f)
            item("key-orc-leader", chance = 1.0f)
        }

        lootTable("hobgoblin") {
            name = "Hobgoblin Loot"
            item("hobgoblin-longsword", chance = 0.15f)
        }

        lootTable("hobgoblin-chief") {
            name = "Hobgoblin Chieftain Loot"
            item("hobgoblin-longsword", chance = 1.0f)
            item("gem-100gp", chance = 0.5f)
            item("keys-hobgoblin-slave", chance = 1.0f)
        }

        lootTable("gnoll") {
            name = "Gnoll Loot"
            item("gnoll-hide-armor", chance = 0.1f)
            item("crude-spear", chance = 0.2f)
        }

        lootTable("gnoll-chief") {
            name = "Gnoll Chieftain Loot"
            item("silver-armbands", chance = 1.0f)
            item("elven-boots", chance = 1.0f)
            item("gem-50gp", chance = 0.8f, minQty = 2, maxQty = 4)
        }

        lootTable("bugbear") {
            name = "Bugbear Loot"
            item("bugbear-morningstar", chance = 0.25f)
        }

        lootTable("bugbear-chief") {
            name = "Bugbear Chieftain Loot"
            item("bugbear-morningstar", chance = 1.0f)
            item("gem-50gp", chance = 1.0f, minQty = 3)
            item("key-bugbear-spoils", chance = 1.0f)
        }

        lootTable("ogre") {
            name = "Ogre Loot"
            item("potion-of-healing", chance = 0.3f)
            item("potion-of-invisibility", chance = 0.2f)
        }

        lootTable("acolyte") {
            name = "Acolyte Loot"
            item("amulet-of-protection-from-good", chance = 0.3f)
        }

        lootTable("adept") {
            name = "Adept Loot"
            item("amulet-of-protection-from-good", chance = 1.0f)
            item("gem-50gp", chance = 0.5f)
        }

        lootTable("evil-priest") {
            name = "Evil Priest Loot"
            item("snake-staff", chance = 1.0f)
            item("gold-ring-black-gem", chance = 1.0f)
            item("magic-plate-plus-1", chance = 1.0f)
            item("magic-shield-plus-1", chance = 1.0f)
            item("magic-sword-plus-1", chance = 1.0f)
            item("amulet-of-protection-from-good", chance = 1.0f)
            item("potion-of-gaseous-form", chance = 1.0f)
        }

        lootTable("minotaur") {
            name = "Minotaur Loot"
            item("magic-spear-plus-1", chance = 1.0f)
        }

        lootTable("owlbear") {
            name = "Owlbear Loot"
            item("scroll-of-protection-from-undead", chance = 0.5f)
        }

        lootTable("treasure-chest-common") {
            name = "Common Treasure"
            item("silver-goblet", chance = 0.5f)
            item("gem-50gp", chance = 0.3f)
        }

        lootTable("treasure-chest-rare") {
            name = "Rare Treasure"
            item("gold-flagon", chance = 0.3f)
            item("gem-100gp", chance = 0.5f)
            item("potion-of-healing", chance = 0.4f)
        }

        lootTable("treasure-minotaur") {
            name = "Minotaur's Hoard"
            item("staff-of-healing", chance = 1.0f)
            item("magic-plate-plus-1", chance = 1.0f)
            item("gem-500gp", chance = 1.0f, minQty = 3)
            item("gem-100gp", chance = 1.0f, minQty = 3)
            item("potion-of-healing", chance = 1.0f, minQty = 3)
        }

        lootTable("temple-treasure") {
            name = "Temple Treasury"
            item("evil-altar-vessels", chance = 1.0f)
            item("gem-500gp", chance = 1.0f, minQty = 4)
            item("gem-1000gp", chance = 0.5f)
        }
    }

    // ==================== CREATURES ====================
    private fun defineCreatures() {
        // === CAVE A: KOBOLDS ===
        creature("kobold") {
            name = "Kobold"
            description = "A small, reptilian humanoid with reddish-brown scales. It hisses and brandishes a tiny dagger, watching for an opportunity to strike with its pack."
            maxHp = 5
            damageDice = "1d4"
            level = 1
            experienceValue = 5
            challengeRating = 1
            isAggressive = true
            abilities("pack-tactics", "sling-shot")
            lootTable("kobold")
            gold(1, 8)
        }

        creature("kobold-guard") {
            name = "Kobold Guard"
            description = "A larger kobold in crude chain mail, armed with a hand axe. These guards throw spears at intruders."
            maxHp = 8
            damageDice = "1d6"
            level = 1
            experienceValue = 10
            challengeRating = 1
            isAggressive = true
            abilities("pack-tactics", "javelin-throw")
            lootTable("kobold")
            gold(1, 6)
        }

        creature("kobold-chieftain") {
            name = "Kobold Chieftain"
            description = "A huge kobold wearing a great golden chain with a large gem. He fights with a battle axe and commands absolute loyalty from his tribe."
            maxHp = 16
            damageDice = "2d4"
            level = 2
            experienceValue = 25
            challengeRating = 1
            isAggressive = true
            abilities("pack-tactics", "aggressive-charge")
            lootTable("kobold-chief")
            gold(5, 20)
        }

        // === CAVE B & C: ORCS ===
        creature("orc") {
            name = "Orc"
            description = "A hulking, pig-faced humanoid with gray-green skin. It snarls and hefts a weapon, ready to fight."
            maxHp = 8
            damageDice = "1d6"
            level = 1
            experienceValue = 10
            challengeRating = 1
            isAggressive = true
            abilities("aggressive-charge", "javelin-throw")
            lootTable("orc")
            gold(1, 6)
        }

        creature("orc-guard") {
            name = "Orc Guard"
            description = "An orc warrior armed with spears for throwing and melee combat. Alert and ready to raise the alarm."
            maxHp = 10
            damageDice = "1d6"
            level = 1
            experienceValue = 12
            challengeRating = 1
            isAggressive = true
            abilities("javelin-throw", "aggressive-charge")
            lootTable("orc")
            gold(2, 8)
        }

        creature("orc-leader") {
            name = "Orc Leader"
            description = "A large orc clad in chain mail with a shield, wielding a mace. He carries a ring set with a gem and wears a silver belt with a gold buckle."
            maxHp = 22
            damageDice = "1d6+2"
            level = 3
            experienceValue = 50
            challengeRating = 2
            isAggressive = true
            abilities("aggressive-charge", "greataxe-cleave")
            lootTable("orc-leader")
            gold(15, 40)
        }

        // === CAVE D: GOBLINS ===
        creature("goblin") {
            name = "Goblin"
            description = "A small, green-skinned creature with pointed ears and a wicked grin. It clutches a scimitar and watches for a chance to flee or backstab."
            maxHp = 6
            damageDice = "1d6"
            level = 1
            experienceValue = 8
            challengeRating = 1
            isAggressive = true
            abilities("nimble-escape", "scimitar-slash")
            lootTable("goblin")
            gold(1, 6)
        }

        creature("goblin-guard") {
            name = "Goblin Guard"
            description = "A goblin warrior with a spear, watching for intruders. Will alert others with a cry of 'Bree-Yark!'"
            maxHp = 6
            damageDice = "1d6"
            level = 1
            experienceValue = 8
            challengeRating = 1
            isAggressive = true
            abilities("nimble-escape", "bree-yark", "scimitar-slash")
            lootTable("goblin")
            gold(1, 6)
        }

        creature("goblin-chieftain") {
            name = "Goblin Chieftain"
            description = "The goblin leader wears chain mail and shield, wielding a sharp scimitar. Under his bed is a tapestry worth 900 gold pieces."
            maxHp = 22
            damageDice = "1d6+2"
            level = 2
            experienceValue = 35
            challengeRating = 1
            isAggressive = true
            abilities("nimble-escape", "scimitar-slash")
            lootTable("goblin-chief")
            gold(10, 25)
        }

        // === CAVE E: OGRE ===
        creature("ogre") {
            name = "Ogre"
            description = "A massive humanoid with thick hide and tremendous strength. This ogre works as a mercenary for the goblins and hobgoblins, accepting payment of 250 gold pieces to aid in their battles."
            maxHp = 50
            damageDice = "1d10+2"
            level = 4
            experienceValue = 125
            challengeRating = 2
            isAggressive = true
            abilities("greatclub-smash", "rock-throw")
            lootTable("ogre")
            gold(50, 100)
        }

        // === CAVE F: HOBGOBLINS ===
        creature("hobgoblin") {
            name = "Hobgoblin"
            description = "A tall, militaristic goblinoid in well-maintained armor. It moves with disciplined precision, part of a well-organized military force."
            maxHp = 11
            damageDice = "1d8"
            level = 1
            experienceValue = 15
            challengeRating = 1
            isAggressive = true
            abilities("martial-discipline", "longsword-strike")
            lootTable("hobgoblin")
            gold(2, 8)
        }

        creature("hobgoblin-guard") {
            name = "Hobgoblin Guard"
            description = "A hobgoblin soldier armed with crossbow and sword. These guards are alert and will summon reinforcements."
            maxHp = 12
            damageDice = "1d8"
            level = 1
            experienceValue = 15
            challengeRating = 1
            isAggressive = true
            abilities("martial-discipline", "longbow", "longsword-strike")
            lootTable("hobgoblin")
            gold(2, 8)
        }

        creature("hobgoblin-torturer") {
            name = "Hobgoblin Torturer"
            description = "A particularly ugly hobgoblin armed with whip and sword. It guards the prisoners and takes pleasure in their suffering."
            maxHp = 16
            damageDice = "1d8"
            level = 2
            experienceValue = 25
            challengeRating = 1
            isAggressive = true
            abilities("martial-discipline", "whip-crack", "longsword-strike")
            lootTable("hobgoblin")
            gold(5, 15)
        }

        creature("hobgoblin-chieftain") {
            name = "Hobgoblin Chieftain"
            description = "A great, ugly creature in plate mail and shield. He wears a gem-studded belt worth 600 gold pieces and commands his tribe with an iron fist."
            maxHp = 44
            damageDice = "1d10"
            level = 3
            experienceValue = 100
            challengeRating = 2
            isAggressive = true
            abilities("martial-discipline", "longsword-strike")
            lootTable("hobgoblin-chief")
            gold(25, 60)
        }

        // === CAVE G: SHUNNED CAVERN ===
        creature("gray-ooze") {
            name = "Gray Ooze"
            description = "A puddle of gray slime that blends with stone. Its corrosive touch dissolves metal and burns flesh."
            maxHp = 24
            damageDice = "2d8"
            level = 3
            experienceValue = 50
            challengeRating = 2
            isAggressive = true
            abilities("gray-ooze-acid")
            gold(0, 0)
        }

        creature("owlbear") {
            name = "Owlbear"
            description = "A monstrous hybrid with the body of a bear and the head of an owl. It is digesting a recently caught gnoll and will attack anything that disturbs it."
            maxHp = 60
            damageDice = "1d8"
            level = 4
            experienceValue = 125
            challengeRating = 2
            isAggressive = true
            abilities("multiattack-claw", "beak-strike")
            lootTable("owlbear")
            gold(0, 0)
        }

        // === CAVE H: BUGBEARS ===
        creature("bugbear") {
            name = "Bugbear"
            description = "A large, hairy goblinoid that moves with surprising stealth. Its eyes gleam with cruel intelligence as it waits to ambush prey."
            maxHp = 22
            damageDice = "2d8"
            level = 2
            experienceValue = 50
            challengeRating = 2
            isAggressive = true
            abilities("surprise-attack", "morningstar-crush")
            lootTable("bugbear")
            gold(5, 20)
        }

        creature("bugbear-chieftain") {
            name = "Bugbear Chieftain"
            description = "A tough old bugbear equal to an ogre in combat. He carries a key and valuable gems, and knows of a secret door to the minotaur's lair."
            maxHp = 36
            damageDice = "1d10+2"
            level = 4
            experienceValue = 125
            challengeRating = 2
            isAggressive = true
            abilities("surprise-attack", "morningstar-crush", "javelin-hurl")
            lootTable("bugbear-chief")
            gold(20, 50)
        }

        // === CAVE I: MINOTAUR'S LABYRINTH ===
        creature("stirge") {
            name = "Stirge"
            description = "A flying, mosquito-like creature that attaches to victims and drains their blood. These pests inhabit the minotaur's maze."
            maxHp = 6
            damageDice = "1d3"
            level = 1
            experienceValue = 8
            challengeRating = 1
            isAggressive = true
            abilities("stirge-blood-drain")
            gold(0, 0)
        }

        creature("fire-beetle") {
            name = "Fire Beetle"
            description = "A giant beetle with glowing glands above its eyes and abdomen. The glands continue to glow for days after the beetle's death."
            maxHp = 6
            damageDice = "2d4"
            level = 1
            experienceValue = 10
            challengeRating = 1
            isAggressive = true
            abilities("fire-beetle-glow")
            gold(0, 0)
        }

        creature("minotaur") {
            name = "Minotaur"
            description = "A fiendishly clever monster that dwells in a confusing labyrinth. It wears chain mail and carries a magical spear, keeping only the choicest treasures and tossing unwanted loot to the cave mouth."
            maxHp = 70
            damageDice = "1d6+3"
            level = 5
            experienceValue = 200
            challengeRating = 3
            isAggressive = true
            abilities("gore", "greataxe-swing", "labyrinth-sense")
            lootTable("minotaur")
            gold(100, 200)
        }

        // === CAVE J: GNOLLS ===
        creature("gnoll") {
            name = "Gnoll"
            description = "A tall, hyena-headed humanoid that reeks of carrion. Its jaws drip with saliva as it eyes potential prey."
            maxHp = 16
            damageDice = "2d8"
            level = 2
            experienceValue = 30
            challengeRating = 1
            isAggressive = true
            abilities("rampage", "spear-thrust", "bite")
            lootTable("gnoll")
            gold(3, 12)
        }

        creature("gnoll-chieftain") {
            name = "Gnoll Chieftain"
            description = "The gnoll leader wears pieces of plate mail and fights with tremendous strength. He wears silver armbands and his treasure includes elven boots taken from a dead adventurer."
            maxHp = 34
            damageDice = "2d4+2"
            level = 3
            experienceValue = 75
            challengeRating = 2
            isAggressive = true
            abilities("rampage", "spear-thrust", "bite")
            lootTable("gnoll-chief")
            gold(20, 50)
        }

        // === CAVE K: SHRINE OF EVIL CHAOS ===
        creature("zombie") {
            name = "Zombie"
            description = "A shambling corpse animated by dark magic. It attacks mindlessly, following the commands of the temple priests."
            maxHp = 12
            damageDice = "1d8"
            level = 1
            experienceValue = 15
            challengeRating = 1
            isAggressive = false // Only attacks on command or when temple is disturbed
            abilities("slam")
            gold(0, 0)
        }

        creature("zombie-guard") {
            name = "Zombie Guard"
            description = "A zombie in plate mail and shield, standing motionless until the temple is disturbed or the priests command it to attack."
            maxHp = 16
            damageDice = "1d8"
            level = 2
            experienceValue = 20
            challengeRating = 1
            isAggressive = false
            abilities("slam")
            gold(0, 0)
        }

        creature("skeleton") {
            name = "Skeleton"
            description = "Animated bones wearing scraps of chain mail and carrying rusty scimitars. Protected from turning by unholy amulets."
            maxHp = 8
            damageDice = "1d6"
            level = 1
            experienceValue = 10
            challengeRating = 1
            isAggressive = false
            abilities("scimitar-slash")
            gold(0, 0)
        }

        creature("wight") {
            name = "Wight"
            description = "An undead creature that drains the life force of the living. It lurks in the temple crypt, guarding ancient treasures."
            maxHp = 26
            damageDice = "1d6"
            level = 3
            experienceValue = 75
            challengeRating = 2
            isAggressive = true
            abilities("life-drain", "slam")
            gold(0, 0)
        }

        creature("acolyte") {
            name = "Acolyte"
            description = "A first-level evil cleric in rusty-red robes with a black cowl. They wear chain mail beneath and carry maces, serving the dark temple."
            maxHp = 8
            damageDice = "1d6"
            level = 1
            experienceValue = 15
            challengeRating = 1
            isAggressive = true
            abilities("cause-light-wounds")
            lootTable("acolyte")
            gold(5, 15)
        }

        creature("adept") {
            name = "Adept"
            description = "A second-level evil cleric in a black robe with a maroon cowl. They wear plate mail beneath and wield maces, protected by amulets from good."
            maxHp = 16
            damageDice = "1d6"
            level = 2
            experienceValue = 35
            challengeRating = 1
            isAggressive = true
            abilities("cause-light-wounds", "cause-fear")
            lootTable("adept")
            gold(10, 25)
        }

        creature("evil-priest") {
            name = "Evil Priest"
            description = "The master of the Shrine of Evil Chaos. A third-level cleric in plate mail with a shield, wielding a snake staff that can transform into a constricting serpent. His chamber contains a demon idol and a secret escape route."
            maxHp = 28
            damageDice = "1d6"
            level = 3
            experienceValue = 150
            challengeRating = 3
            isAggressive = true
            abilities("snake-staff-strike", "cause-light-wounds", "cause-fear", "hold-person")
            lootTable("evil-priest")
            gold(50, 100)
        }

        creature("torturer") {
            name = "Torturer"
            description = "A third-level fighter in chain mail under black leather garments. He wields a huge battle axe and takes sadistic pleasure in his work."
            maxHp = 38
            damageDice = "1d8+2"
            level = 3
            experienceValue = 75
            challengeRating = 2
            isAggressive = true
            abilities("greataxe-cleave")
            gold(50, 150)
        }

        creature("medusa") {
            name = "Medusa"
            description = "A creature with snakes for hair whose gaze turns victims to stone. Recently captured by the evil priest, she is chained in a cell disguised as a fair maiden. She carries a potion of stone to flesh and will bargain for her freedom."
            maxHp = 40
            damageDice = "1d6"
            level = 4
            experienceValue = 200
            challengeRating = 3
            isAggressive = true // Attacks if approached, but can be negotiated with
            abilities("medusa-gaze", "bite")
            gold(0, 0)
        }

        creature("gelatinous-cube") {
            name = "Gelatinous Cube"
            description = "A 10-foot cube of transparent, acidic jelly that engulfs and dissolves prey. The 'bones' visible inside it are actually a wand of enemy detection with 9 charges."
            maxHp = 44
            damageDice = "2d8"
            level = 4
            experienceValue = 125
            challengeRating = 2
            isAggressive = true
            abilities("gelatinous-cube-engulf")
            gold(0, 0)
        }

        // === SPECIAL: RESCUABLE PRISONERS ===
        creature("merchant-prisoner") {
            name = "Captured Merchant"
            description = "A plump, half-dead merchant scheduled to be eaten at a special banquet. If rescued, the Guild will pay 100 gold pieces and grant honorary status."
            maxHp = 8
            damageDice = "1d4"
            level = 0
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            gold(0, 0)
        }

        creature("orc-prisoner") {
            name = "Captured Orc"
            description = "An orc from a rival tribe, held prisoner. If freed and given a weapon, he will fight goblins and hobgoblins gladly, then try to escape."
            maxHp = 8
            damageDice = "1d6"
            level = 1
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            gold(0, 0)
        }

        creature("manatiarms-prisoner") {
            name = "Captured Man-at-Arms"
            description = "A former guard for the merchant, willing to serve rescuers for room, board, and weapons for one year."
            maxHp = 10
            damageDice = "1d6"
            level = 1
            experienceValue = 0
            challengeRating = 0
            isAggressive = false
            gold(0, 0)
        }

        creature("gnoll-prisoner-crazy") {
            name = "Crazed Gnoll"
            description = "A gnoll driven mad by captivity. If freed, he will snatch a weapon and attack his rescuers in his madness."
            maxHp = 10
            damageDice = "1d6"
            level = 1
            experienceValue = 15
            challengeRating = 1
            isAggressive = true // Attacks rescuers!
            gold(0, 0)
        }

        creature("hero-prisoner") {
            name = "Imprisoned Hero"
            description = "A fourth-level fighter with mighty muscles, shaggy hair and beard, and staring eyes. His enslavement has driven him to fits of berserk fury. An evil man who will either kill his rescuers or steal their best treasure."
            maxHp = 48
            damageDice = "1d4+3"
            level = 4
            experienceValue = 0
            challengeRating = 2
            isAggressive = false // Unpredictable - may turn on party
            gold(0, 0)
        }
    }

    // ==================== LOCATIONS ====================
    private fun defineLocations() {
        // === RAVINE ENTRANCE ===
        location("ravine-entrance") {
            name = "Ravine of Chaos"
            description = "The forest has been getting denser, darker, gloomier. The thick, twisted tree trunks and grasping roots seem to warn you off. Now the strange growth ends suddenly, and you step into a ravine-like area. The walls rise steeply to either side - dark, streaked rock mingled with earth. Clumps of trees grow on the floor of the ravine and up the sloping walls. You can see the black mouths of cave-like openings at varying heights. Bones and refuse litter the ground, and a flock of ravens rises croaking from the carrion they were feasting upon."
            position(0, 0)
            locationType = LocationType.OUTDOOR_GROUND
            exits {
                // Back to the Keep
                northTo("location-keep-borderlands-road-to-keep")
                // Lower caves (easier)
                enter("cave-a-entrance")
                enter("cave-b-entrance")
                enter("cave-c-entrance")
                enter("cave-d-entrance")
                // Middle caves
                enter("cave-e-entrance")
                enter("cave-f-entrance")
                // Upper caves (harder)
                enter("cave-g-entrance")
                enter("cave-h-entrance")
                enter("cave-i-entrance")
                enter("cave-j-entrance")
                enter("cave-k-entrance")
            }
        }

        // ========== CAVE A: KOBOLD LAIR ==========
        location("cave-a-entrance") {
            name = "Kobold Cave Entrance"
            description = "A low, narrow opening barely four feet high leads into darkness. 30 feet inside is a pit trap - 10 feet deep with a closing lid. Scratching sounds and high-pitched chittering echo from within."
            position(1, 0)
            locationType = LocationType.UNDERGROUND
            lockLevel = 0 // Pit trap here - could be a feature
            exits {
                south("ravine-entrance")
                north("cave-a-guard-room")
            }
        }

        location("cave-a-guard-room") {
            name = "Kobold Guard Room"
            description = "Six kobold guards in chain mail watch this area, armed with hand axes for throwing. They will throw their weapons at intruders, then flee to warn areas 4 and 6."
            position(1, -1)
            locationType = LocationType.UNDERGROUND
            creatures("kobold-guard", "kobold-guard", "kobold-guard")
            exits {
                south("cave-a-entrance")
                north("cave-a-rat-nest")
                east("cave-a-storage")
                west("cave-a-chieftain")
            }
        }

        location("cave-a-rat-nest") {
            name = "Giant Rat Nest"
            description = "This chamber reeks of garbage and waste. Eighteen giant rats live here, pets of the kobolds. Their leader is a huge fellow with a thin silver chain set with 5 small gems."
            position(1, -2)
            locationType = LocationType.UNDERGROUND
            // Giant rats not yet defined, but could add them
            exits {
                south("cave-a-guard-room")
            }
        }

        location("cave-a-storage") {
            name = "Kobold Food Storage"
            description = "The door is locked. Inside are dried meat, salted meat, grain, vegetables in sacks, boxes, barrels and piles. There are also bits and pieces of past human victims. The wine is thin and vinegary."
            position(2, -1)
            locationType = LocationType.UNDERGROUND
            lockLevel = 2
            exits {
                west("cave-a-guard-room")
            }
        }

        location("cave-a-chieftain") {
            name = "Kobold Chieftain's Room"
            description = "This huge kobold fights with a battle axe. He wears a great golden chain with a large gem around his neck, and has the key to the storage room. Five female kobolds attend him. A locked chest holds the tribe's treasure: 203 copper, 61 silver, and 22 electrum pieces. Hidden in a blanket on the wall are 50 gold pieces sewn into the hem."
            position(0, -1)
            locationType = LocationType.UNDERGROUND
            creatures("kobold-chieftain", "kobold", "kobold")
            exits {
                east("cave-a-guard-room")
                north("cave-a-common")
            }
        }

        location("cave-a-common") {
            name = "Kobold Common Chamber"
            description = "The rest of the kobold tribe lives here: 17 males, 23 females, and 8 young. Amidst the litter of cloth and scraps is a piece of silk worth 150 gold pieces - but only if the party searches carefully."
            position(0, -2)
            locationType = LocationType.UNDERGROUND
            creatures("kobold", "kobold", "kobold", "kobold")
            exits {
                south("cave-a-chieftain")
            }
        }

        // ========== CAVE B: ORC LAIR (FIRST TRIBE) ==========
        location("cave-b-entrance") {
            name = "Orc Lair Entrance"
            description = "Upon entering, you see the wall 30 feet to the north is decorated with heads and skulls of humans, elves, and dwarves in various stages of decay. Grisly greetings are placed in niches covering 100 square feet. Close inspection shows one skull is orcish - from the rival tribe at Cave C."
            position(-1, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("ravine-entrance")
                west("cave-b-guard")
            }
        }

        location("cave-b-guard") {
            name = "Orc Guard Post"
            description = "A narrowing area serves as a guard post. An orc watcher has a small window-like opening from which he can observe the entrance. A piece of gray canvas gives the impression the guard's head is part of the ghastly trophies. He will duck down and slip a goblin head into place, alerting the orcs at area 7."
            position(-2, 0)
            locationType = LocationType.UNDERGROUND
            creatures("orc-guard")
            exits {
                east("cave-b-entrance")
                north("cave-b-guards-2")
                west("cave-b-banquet")
            }
        }

        location("cave-b-guards-2") {
            name = "Orc Guard Room"
            description = "Four orcs armed with spears guard this room. They have one spear for hurling and one for melee. When alerted, they will rush to engage or flank intruders. Each carries d8 electrum pieces."
            position(-2, -1)
            locationType = LocationType.UNDERGROUND
            creatures("orc-guard", "orc-guard", "orc-guard", "orc-guard")
            exits {
                south("cave-b-guard")
            }
        }

        location("cave-b-banquet") {
            name = "Orc Banquet Area"
            description = "A great fireplace dominates the south wall, with many tables and benches filling this 30x50 foot chamber. The table at the north end has a large chair where the orc leader holds court. A small fire of charcoal burns in the fireplace. The place is empty now."
            position(-3, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("cave-b-guard")
                north("cave-b-common")
                west("cave-b-storage")
            }
        }

        location("cave-b-common") {
            name = "Orc Common Room"
            description = "Twelve male orcs, 18 females, and 9 young are quartered here. The males have 2d6 silver pieces each. The furnishings are of no value."
            position(-3, -1)
            locationType = LocationType.UNDERGROUND
            creatures("orc", "orc", "orc", "orc")
            exits {
                south("cave-b-banquet")
            }
        }

        location("cave-b-storage") {
            name = "Orc Storage Chamber"
            description = "The door is locked. Amidst stacks of supplies are 3 shields, 17 spears, and 2 battle axes in excellent condition. A small crate contains a crossbow and 60 bolts. Nothing else of value."
            position(-4, 0)
            locationType = LocationType.UNDERGROUND
            lockLevel = 2
            items("hand-axe", "crude-spear")
            exits {
                east("cave-b-banquet")
            }
        }

        location("cave-b-leader") {
            name = "Orc Leader's Room"
            description = "This large orc wears chain mail, carries a shield +1, and wields a mace. He fights as a 4 hit dice monster and adds +2 to damage. His silver belt has a gold buckle worth 160gp, and his sword has a 100gp gem in the pommel. Behind tapestries on the south wall is a secret door to the rival orc tribe's area. An alcove to the east holds arms, treasure, and 13 platinum pieces."
            position(-4, -1)
            locationType = LocationType.UNDERGROUND
            creatures("orc-leader", "orc", "orc")
            items("magic-shield-plus-1")
            exits {
                east("cave-b-common")
                south("cave-b-secret-tunnel") // Secret door to Cave C
            }
        }

        location("cave-b-secret-tunnel") {
            name = "Secret Tunnel"
            description = "A narrow, hidden passage connects the two rival orc tribes' territories. Both leaders know of this passage and sometimes meet here to discuss matters of mutual concern, though their tribes are enemies."
            position(-4, 1)
            locationType = LocationType.UNDERGROUND
            exits {
                north("cave-b-leader")
                south("cave-c-leader")
            }
        }

        // ========== CAVE C: ORC LAIR (SECOND TRIBE) ==========
        location("cave-c-entrance") {
            name = "Second Orc Lair Entrance"
            description = "The natural cave quickly turns into worked stone tunnels. Unlike the first orc lair, these orcs rely on nearly invisible trip strings 11 feet from the entrance. When triggered, a heavy weighted net drops from the ceiling and metal pieces create an alarm sound."
            position(-1, 2)
            locationType = LocationType.UNDERGROUND
            // Trap: net trap
            exits {
                north("ravine-entrance")
                south("cave-c-common")
            }
        }

        location("cave-c-common") {
            name = "Second Orc Common Chamber"
            description = "Nine male orcs with swords and shields, 8 females, and 3 young dwell here. The place is a mess. If the net trap is heard, the males will go to the entrance, arriving in 1 round."
            position(-1, 3)
            locationType = LocationType.UNDERGROUND
            creatures("orc", "orc", "orc", "orc")
            exits {
                north("cave-c-entrance")
                east("cave-c-hall")
            }
        }

        location("cave-c-hall") {
            name = "Second Orc Common Hall"
            description = "General meetings are held here, and food is cooked and eaten. Six males with crossbows, 4 non-combatant females dwell in the western forepart. A guard stands just inside the door to the leader's room and cannot be surprised."
            position(0, 3)
            locationType = LocationType.UNDERGROUND
            creatures("orc-guard", "orc-guard", "orc")
            exits {
                west("cave-c-common")
                east("cave-c-leader")
            }
        }

        location("cave-c-leader") {
            name = "Second Orc Leader's Room"
            description = "The leader wears plate mail and carries a shield. He uses a sword and attacks as a 3 hit die monster, adding +2 to damage. His silver belt has a gold buckle worth 160gp, and a magic hand axe +1 hangs on the wall. Behind a tapestry is a secret door to the rival tribe. Behind a boulder is a potion of healing and a scroll with a fireball spell."
            position(1, 3)
            locationType = LocationType.UNDERGROUND
            creatures("orc-leader", "orc")
            items("magic-hand-axe-plus-1", "potion-of-healing")
            exits {
                west("cave-c-hall")
                north("cave-b-secret-tunnel") // Secret door
            }
        }

        // ========== CAVE D: GOBLIN LAIR ==========
        location("cave-d-entrance") {
            name = "Goblin Lair Entrance"
            description = "The natural cave quickly becomes worked stone tunnels typical of goblin lairs. The passageways are very busy - every 10 feet there is a 1 in 6 chance of encountering wandering goblins who will cry 'Bree-Yark!' (which means 'We surrender!'... or does it?)"
            position(2, 2)
            locationType = LocationType.UNDERGROUND
            exits {
                north("ravine-entrance")
                south("cave-d-guard-1")
            }
        }

        location("cave-d-guard-1") {
            name = "Goblin Guard Chamber"
            description = "Six goblin guards with spears watch this area, alert for intruders of any sort, including hobgoblins from the south. Each has d4 x 10 copper and d4 silver pieces. A barrel holds 60 spears, with a small table, 2 benches, and a keg of water."
            position(2, 3)
            locationType = LocationType.UNDERGROUND
            creatures("goblin-guard", "goblin-guard", "goblin-guard")
            exits {
                north("cave-d-entrance")
                east("cave-d-guard-2")
                south("cave-d-common")
            }
        }

        location("cave-d-guard-2") {
            name = "Eastern Guard Chamber"
            description = "Six more goblin guards watch here, mainly watching to the east. If there is a cry of 'Bree-Yark!', two of these guards will rush to a secret door, toss a sack with 250 gold pieces through to the ogre, and ask him to help. The ogre will accept and attack immediately."
            position(3, 3)
            locationType = LocationType.UNDERGROUND
            creatures("goblin-guard", "goblin-guard", "goblin-guard")
            exits {
                west("cave-d-guard-1")
                east("cave-d-ogre-passage") // Secret door to ogre
            }
        }

        location("cave-d-ogre-passage") {
            name = "Secret Passage to Ogre"
            description = "This secret passage connects the goblin lair to the ogre's cave. The goblins pay the ogre 250 gold pieces to help them fight intruders."
            position(4, 3)
            locationType = LocationType.UNDERGROUND
            exits {
                west("cave-d-guard-2")
                east("cave-e-entrance")
            }
        }

        location("cave-d-common") {
            name = "Goblin Common Room"
            description = "Ten male goblins, 14 females, and 6 young dwell here. Food is prepared and eaten here, and general meetings are held. The males have d6 silver pieces each, females have 2d6 copper pieces."
            position(2, 4)
            locationType = LocationType.UNDERGROUND
            creatures("goblin", "goblin", "goblin", "goblin")
            exits {
                north("cave-d-guard-1")
                south("cave-d-chieftain")
            }
        }

        location("cave-d-chieftain") {
            name = "Goblin Chieftain's Room"
            description = "The goblin leader wears chain mail and shield and fights with a sword. Three guards with bows protect him. Several females are quartered here. Under the bed is a secret drawer containing the tribe's treasure: a tapestry with silver and gold threads worth 900 gold pieces. A silver cup worth 90gp is under his bed."
            position(2, 5)
            locationType = LocationType.UNDERGROUND
            creatures("goblin-chieftain", "goblin-guard", "goblin-guard")
            items("tapestry-silver-gold", "silver-goblet")
            exits {
                north("cave-d-common")
                south("cave-d-hobgoblin-passage") // Secret door to hobgoblin area
            }
        }

        location("cave-d-hobgoblin-passage") {
            name = "Passage to Hobgoblin Storage"
            description = "A secret passage known only to the hobgoblins, who use it to steal supplies from the goblins. The goblins are unaware of this passage."
            position(2, 6)
            locationType = LocationType.UNDERGROUND
            exits {
                north("cave-d-chieftain")
                south("cave-f-storage")
            }
        }

        // ========== CAVE E: OGRE CAVE ==========
        location("cave-e-entrance") {
            name = "Ogre Cave"
            description = "A strong, sour odor fills this cave. What appears to be a huge bear sprawled in the southwestern corner is actually a bearskin the ogre killed and uses as a bed, with leaves underneath for comfort. The ogre sits in the eastern portion, ready to do battle. He has AC 4 due to his thick hide and bearskin protection, dealing 3-12 points of damage with his strength."
            position(5, 2)
            locationType = LocationType.UNDERGROUND
            creatures("ogre")
            exits {
                north("ravine-entrance")
                west("cave-d-ogre-passage")
            }
        }

        // ========== CAVE F: HOBGOBLIN LAIR ==========
        location("cave-f-entrance") {
            name = "Hobgoblin Lair Entrance"
            description = "The entrance is guarded by a stout, barred door at the back of the entry cave. Skulls line the walls, and several are affixed to the oaken door with a warning written in common runes: 'Come in - we'd like to have you for dinner!' A careful inspection reveals a secret mechanism to slide the bar."
            position(2, 8)
            locationType = LocationType.UNDERGROUND
            lockLevel = 3 // Barred door, can be forced or mechanism found
            exits {
                north("ravine-entrance")
                south("cave-f-common-1")
            }
        }

        location("cave-f-common-1") {
            name = "Hobgoblin Common Chamber"
            description = "Five males, 8 females, and 3 young quarter here. The males watch the east door which communicates with the goblin lair. They are battle-ready. Males have d4 each of gold, silver, and copper. A small barrel of beer and odds and ends of furniture fill the room."
            position(2, 9)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin", "hobgoblin", "hobgoblin")
            exits {
                north("cave-f-entrance")
                west("cave-f-torture")
                south("cave-f-feast")
                east("cave-d-hobgoblin-passage")
            }
        }

        location("cave-f-torture") {
            name = "Hobgoblin Torture Chamber"
            description = "Two very large, ugly hobgoblins guard 6 prisoners chained to the walls. One has a whip and sword, the other chain mail. They guard: a merchant (to be eaten tonight), an orc from Cave B, a man-at-arms, the merchant's wife with a dagger +1, a crazed gnoll, and another man-at-arms. Keys hang on the opposite wall."
            position(1, 9)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin-torturer", "hobgoblin-torturer")
            creatures("merchant-prisoner", "orc-prisoner", "manatiarms-prisoner", "gnoll-prisoner-crazy")
            items("keys-hobgoblin-slave")
            exits {
                east("cave-f-common-1")
            }
        }

        location("cave-f-feast") {
            name = "Hobgoblin Feasting Hall"
            description = "This large place is used for meals, meetings, and revels. Tables and benches are set out for a coming feast. Four males, 5 females, and 9 young are working here. The head table has pewter dishes worth 25gp for the set."
            position(2, 10)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin", "hobgoblin")
            exits {
                north("cave-f-common-1")
                west("cave-f-guards")
                south("cave-f-armory")
            }
        }

        location("cave-f-guards") {
            name = "Hobgoblin Guard Room"
            description = "Six hobgoblins with crossbows guard here. Three will fire once before dropping crossbows and taking maces. They carry d4 each of gold, silver, and copper. If the door is battered or the bar falls, one will rush to alert area 27."
            position(1, 10)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin-guard", "hobgoblin-guard", "hobgoblin-guard")
            exits {
                east("cave-f-feast")
            }
        }

        location("cave-f-armory") {
            name = "Hobgoblin Armory"
            description = "Three hobgoblin guards are on duty here at all times. The chamber contains: 1 suit of man-sized plate mail, 1 suit of dwarf-sized plate mail, 3 suits of chain mail, 2 suits of elf-sized chain mail, 7 suits of leather armor, 11 shields, 6 daggers, 1 battle axe, 4 maces, 3 swords, 2 bows, 1 longbow, 13 crossbows, 51 spears, 19 pole arms, 42 helmets of various sizes."
            position(2, 11)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin-guard", "hobgoblin-guard", "hobgoblin-guard")
            items("chain-mail", "plate-mail", "hobgoblin-longsword")
            exits {
                north("cave-f-feast")
                east("cave-f-storage")
            }
        }

        location("cave-f-storage") {
            name = "Hobgoblin Storeroom"
            description = "Goods stolen from the goblins are kept here. A single guard is on duty. Many bales, boxes, crates, barrels, and sacks contain cloth, food, beer, and wine - all of no special worth. The hard-working but dim goblins continually bring supplies here through a secret door they don't know exists."
            position(3, 11)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin-guard")
            exits {
                west("cave-f-armory")
                north("cave-d-hobgoblin-passage") // Secret door the hobgoblins use
            }
        }

        location("cave-f-guards-2") {
            name = "Western Guard Room"
            description = "Two hobgoblin guards with crossbows and swords stand here. With them are 2 females who will fight. They watch for danger and will alert area 30, the other area 31, and/or area 27 as required. The room has 2 cots, a bench, a stool, and a large box of soiled clothing."
            position(1, 11)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin-guard", "hobgoblin-guard")
            exits {
                east("cave-f-armory")
                south("cave-f-chieftain")
            }
        }

        location("cave-f-chieftain") {
            name = "Hobgoblin Chieftain's Quarters"
            description = "This great, ugly creature is in plate mail and shield with a gem-studded belt worth 600gp. He has 5 platinum and 31 gold pieces. Four large female hobgoblins attend him. The room is crowded with furniture and junk, but a false bottom in an iron box holds 25 platinum, 200 gold, 115 electrum, 400 silver, plus a 100gp gem and a potion of poison. Near the fireplace is a concealed wand of paralyzation with 7 charges."
            position(1, 12)
            locationType = LocationType.UNDERGROUND
            creatures("hobgoblin-chieftain", "hobgoblin", "hobgoblin")
            items("gem-studded-belt", "wand-of-paralyzation", "potion-of-poison")
            exits {
                north("cave-f-guards-2")
                west("cave-f-slave-pen-1")
            }
        }

        location("cave-f-slave-pen-1") {
            name = "Slave Pen"
            description = "The iron door is secured by a bar, chain, and heavy padlock. Inside are slaves: 3 kobolds, 1 goblin, 4 orcs, and 2 humans - optionally add 1 dwarf and 2 elves. They are chained to the wall. All will fight the bugbears if given weapons. The dwarf and elves may agree to help adventurers."
            position(0, 12)
            locationType = LocationType.UNDERGROUND
            lockLevel = 3
            exits {
                east("cave-f-chieftain")
            }
        }

        location("cave-f-slave-pen-2") {
            name = "Second Slave Pen"
            description = "Another barred, chained, and padlocked iron door. Inside: 3 hobgoblins, 2 gnolls, 1 rebel bugbear, and 1 huge human - a seeming wildman with mighty muscles and staring eyes. He is a Hero (4th level fighter) prone to berserk fury. He is an evil man who will either kill his rescuers or steal their best treasure."
            position(0, 11)
            locationType = LocationType.UNDERGROUND
            lockLevel = 3
            creatures("hero-prisoner")
            exits {
                south("cave-f-slave-pen-1")
            }
        }

        // ========== CAVE G: SHUNNED CAVERN ==========
        location("cave-g-entrance") {
            name = "Shunned Cavern Entrance"
            description = "Even the ogre stays away from here. The creatures who dwell herein are exceptionally dangerous. Any creature foolish enough to venture out at night becomes fair game. A horrible stench is noticed as you enter the cavern area."
            position(6, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                west("ravine-entrance")
                east("cave-g-gallery")
            }
        }

        location("cave-g-gallery") {
            name = "Empty Gallery"
            description = "The odor here is awful. Bones and rotting corpses are spread among dead leaves and old branches. A careful search reveals a coin every round: 1-2 = copper, 3-4 = silver, 5-6 = electrum. The sound of searching might bring visitors..."
            position(7, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                west("cave-g-entrance")
                north("cave-g-pool")
                east("cave-g-owlbear")
            }
        }

        location("cave-g-pool") {
            name = "Shallow Pool"
            description = "This portion is very wet, with a large pool of shallow water. A few white, blind fish swim here. In the water is a jewel-encrusted goblet worth 1,300 gold pieces. Three gray oozes lurk here - one on the ceiling barely noticeable, two always in the pool. They cause 2-16 damage on the first round, then 1-8, and can destroy armor."
            position(7, -1)
            locationType = LocationType.UNDERGROUND
            creatures("gray-ooze", "gray-ooze", "gray-ooze")
            items("jeweled-goblet")
            exits {
                south("cave-g-gallery")
            }
        }

        location("cave-g-owlbear") {
            name = "Owlbear's Den"
            description = "The owlbear sleeps in the most southerly part of its den, digesting a meal of gnoll it just caught at dawn. If aroused, it will roar and rush out to attack. It has no treasure, but among the bones and sticks is a scroll tube with a scroll of protection from undead (1 in 6 chance per person searching per round to find it)."
            position(8, 0)
            locationType = LocationType.UNDERGROUND
            creatures("owlbear")
            items("scroll-of-protection-from-undead")
            exits {
                west("cave-g-gallery")
            }
        }

        // ========== CAVE H: BUGBEAR LAIR ==========
        location("cave-h-entrance") {
            name = "Bugbear Lair Entrance"
            description = "Signs beside the entrance cave are written in kobold, orcish, goblin, etc. Each says: 'Safety, security and repose for all humanoids who enter - WELCOME! (Come in and report to the first guard on the left for a hot meal and bed assignment.)'"
            position(6, -2)
            locationType = LocationType.UNDERGROUND
            exits {
                south("ravine-entrance")
                north("cave-h-guard")
            }
        }

        location("cave-h-guard") {
            name = "Bugbear Guard Room"
            description = "Three bugbears lounge on stools near a smoking brazier with meat skewers toasting over the coals. They will ignore their maces, reaching for the food and offering the skewers to newcomers - then suddenly using them as swords (+2 to hit due to surprise). Two cots and a large gong are here."
            position(6, -3)
            locationType = LocationType.UNDERGROUND
            creatures("bugbear", "bugbear", "bugbear")
            exits {
                south("cave-h-entrance")
                north("cave-h-chieftain")
                east("cave-h-spoils")
            }
        }

        location("cave-h-chieftain") {
            name = "Bugbear Chieftain's Room"
            description = "This tough old bugbear is equal to an ogre. He has a pouch with a key, 29 platinum pieces, and 3 gems worth 50gp each. With him is a female bugbear with gold earrings worth 100gp. Several pieces of silk worth 150gp are mixed in the bedding. The chieftain knows of a secret door to the minotaur's lair."
            position(6, -4)
            locationType = LocationType.UNDERGROUND
            creatures("bugbear-chieftain", "bugbear")
            items("gem-50gp", "gem-50gp", "gem-50gp")
            exits {
                south("cave-h-guard")
                west("cave-h-spoils")
                north("cave-i-entrance") // Secret door to minotaur
            }
        }

        location("cave-h-spoils") {
            name = "Bugbear Spoils Room"
            description = "The heavy door is locked, and the key is in the chieftain's pouch. Inside is a shield +1, a tray holding dried herbs (catnip?), boxes and crates of dried or salted foodstuffs, leather hides, 3 barrels of ale, a tun of wine, and a small keg of oil (20 flask capacity). All but the shield and oil are worth 400gp at the Keep."
            position(7, -3)
            locationType = LocationType.UNDERGROUND
            lockLevel = 2
            items("magic-shield-plus-1")
            exits {
                west("cave-h-guard")
                south("cave-h-common")
            }
        }

        location("cave-h-common") {
            name = "Bugbear Common Room"
            description = "Three males, 7 females, and 3 young bugbears live here. There are piles of bedding and old garments. Blackened by soot, a silver urn worth 175gp sits near the fireplace, but only close examination reveals its true value."
            position(7, -2)
            locationType = LocationType.UNDERGROUND
            creatures("bugbear", "bugbear", "bugbear")
            items("silver-urn")
            exits {
                north("cave-h-spoils")
                west("cave-h-guards-2")
            }
        }

        location("cave-h-guards-2") {
            name = "Bugbear Guard Chamber"
            description = "Two males and 3 females watch here. Each has a spear in addition to normal weapons. They tend to the slaves as well as guard the entrance. Keys to areas 40 and 41 are on the wall. Both corridors to the slave pens have meal sacks and small boxes, along with barrels of provisions and watered wine."
            position(6, -2)
            locationType = LocationType.UNDERGROUND
            creatures("bugbear", "bugbear")
            items("keys-hobgoblin-slave")
            exits {
                east("cave-h-common")
            }
        }

        // ========== CAVE I: CAVES OF THE MINOTAUR ==========
        location("cave-i-entrance") {
            name = "Minotaur's Labyrinth Entrance"
            description = "This labyrinth houses a number of nasty things, but the worst is the fiendishly clever minotaur. Immediately upon entering, adventurers feel slightly dizzy - the effects of a direction confusion spell. Directions will be named incorrectly: southeast instead of northeast, east instead of west. Only the minotaur is immune."
            position(6, -5)
            locationType = LocationType.UNDERGROUND
            exits {
                south("cave-h-chieftain") // Secret door from bugbears
                north("cave-i-stirge")
                east("cave-i-beetles-1")
            }
        }

        location("cave-i-stirge") {
            name = "Stirge Cave"
            description = "Thirteen stirges inhabit this area, squeaking and hooting hungrily. The minotaur loves to catch and eat them, so they avoid him. If hit, a stirge attaches and drains 1-4 hit points of blood each round until killed. They have no treasure."
            position(6, -6)
            locationType = LocationType.UNDERGROUND
            creatures("stirge", "stirge", "stirge", "stirge", "stirge")
            exits {
                south("cave-i-entrance")
                east("cave-i-minotaur")
            }
        }

        location("cave-i-beetles-1") {
            name = "Fire Beetle Chamber"
            description = "Three fire beetles dwell here, hungry and ready to attack. Two glands above their eyes and one on their abdomen glow with a red light, continuing to glow for 1-6 days after the beetle's death. They have no treasure."
            position(7, -5)
            locationType = LocationType.UNDERGROUND
            creatures("fire-beetle", "fire-beetle", "fire-beetle")
            exits {
                west("cave-i-entrance")
                north("cave-i-beetles-2")
            }
        }

        location("cave-i-beetles-2") {
            name = "Second Beetle Chamber"
            description = "Two more fire beetles like those in area 43 dwell here."
            position(7, -6)
            locationType = LocationType.UNDERGROUND
            creatures("fire-beetle", "fire-beetle")
            exits {
                south("cave-i-beetles-1")
                west("cave-i-minotaur")
            }
        }

        location("cave-i-minotaur") {
            name = "Minotaur's Lair"
            description = "This huge monster wears chain mail and carries a magical spear +1. He rushes forward with his spear for 4-9 points, then gores and bites. He knows the only escape is through the secret door to area 36 or out and up a large tree. The cave has skulls and bones in decorative patterns. A secret stone slab hides his treasure hoard."
            position(7, -7)
            locationType = LocationType.UNDERGROUND
            creatures("minotaur")
            exits {
                south("cave-i-beetles-2")
                west("cave-i-stirge")
                east("cave-h-chieftain") // Secret door back to bugbears
            }
        }

        // ========== CAVE J: GNOLL LAIR ==========
        location("cave-j-entrance") {
            name = "Gnoll Lair Entrance"
            description = "The entry into this is a small cave, and only at the end will worked stone be visible. If adventurers have a light or make much noise, the guards at area 46 will certainly be alerted and ready."
            position(-2, -3)
            locationType = LocationType.UNDERGROUND
            exits {
                east("ravine-entrance")
                west("cave-j-guard-1")
            }
        }

        location("cave-j-guard-1") {
            name = "Gnoll Guard Room"
            description = "Four gnolls are always on duty here. Two have bows and will shoot at intruders until melee takes place; they then run for help while the other two fight. Each gnoll has d8 each of electrum, silver, and copper pieces."
            position(-3, -3)
            locationType = LocationType.UNDERGROUND
            creatures("gnoll", "gnoll", "gnoll", "gnoll")
            exits {
                east("cave-j-entrance")
                south("cave-j-guard-2")
            }
        }

        location("cave-j-guard-2") {
            name = "Secondary Guard Room"
            description = "Three male gnolls and 5 females are quartered here. They will be ready to fight immediately. Males have d6 gold, females have d4 silver. There is rude furniture, heaps of bedding, hides and pelts on the floor - one valuable sable cloak worth 450gp - and a barrel of water in the southwest corner."
            position(-3, -4)
            locationType = LocationType.UNDERGROUND
            creatures("gnoll", "gnoll", "gnoll")
            items("sable-cloak")
            exits {
                north("cave-j-guard-1")
                south("cave-j-storage")
            }
        }

        location("cave-j-storage") {
            name = "Gnoll Armory and Storage"
            description = "Besides usual provisions: 7 shields, a suit of dwarf-sized chain mail, 12 hand axes, 3 longbows, 5 quivers of arrows (20 in each), and a cursed sword -1. One barrel of exceptionally fine ale is here - it is so good there is a 5 in 6 chance per taste that the drinker will spend 1-4 turns drinking and be at -2 to hit."
            position(-3, -5)
            locationType = LocationType.UNDERGROUND
            items("chain-mail", "hand-axe", "cursed-sword-minus-1")
            exits {
                north("cave-j-guard-2")
                west("cave-j-common")
            }
        }

        location("cave-j-common") {
            name = "Gnoll Common Room"
            description = "This place quarters the gnoll tribe - 6 males, 11 females, and 18 young who do not fight. Males have d6 each of electrum and silver, females have d10 silver pieces. The usual clutter of worthless furniture fills the room."
            position(-4, -5)
            locationType = LocationType.UNDERGROUND
            creatures("gnoll", "gnoll", "gnoll")
            exits {
                east("cave-j-storage")
                north("cave-j-chieftain")
            }
        }

        location("cave-j-chieftain") {
            name = "Gnoll Chieftain's Quarters"
            description = "The gnoll leader wears pieces of plate mail and fights with tremendous strength. His two sons also dwell here with 4 female gnolls. The chieftain has silver armbands worth 50gp each and 39 gold in his belt pouch. Under a flagstone in the fireplace alcove are 200 copper, 157 silver, 76 electrum, and 139 gold pieces. A secret door leads to Cave K - a skeleton with a broken leg lies just inside, having died trying to escape."
            position(-4, -4)
            locationType = LocationType.UNDERGROUND
            creatures("gnoll-chieftain", "gnoll", "gnoll")
            items("silver-armbands", "elven-boots")
            exits {
                south("cave-j-common")
                west("cave-k-boulder-passage") // Secret door to temple
            }
        }

        // ========== CAVE K: SHRINE OF EVIL CHAOS ==========
        location("cave-k-entrance") {
            name = "Shrine of Evil Chaos Entrance"
            description = "A faint, foul draft issues from the 20' wide cave mouth. The worn path through obscenely twisted trees gives those approaching a dim awareness of lurking evil. Red strata with bulging black veins run through the hewn rock walls beyond the entrance. The wide corridors are deathly still. A faint groaning sound and shrill piping may occasionally be heard, barely perceptible. The floors are smooth and worn by countless feet."
            position(-5, 0)
            locationType = LocationType.UNDERGROUND
            exits {
                east("ravine-entrance")
                west("cave-k-zombie-passage")
            }
        }

        location("cave-k-zombie-passage") {
            name = "Passage of the Dead"
            description = "Eight zombies shuffle through here - two files of 10 skeletons each, two of 10 zombies each. They face south and north respectively, standing motionless unless the temple at area 58 is disturbed or the priests command them. Footsteps of intruders will echo alarmingly in these vaulted halls (+2 chance of being surprised). Each undead wears an amulet of protection from turning."
            position(-6, 0)
            locationType = LocationType.UNDERGROUND
            creatures("zombie", "zombie", "zombie", "zombie")
            creatures("skeleton", "skeleton", "skeleton", "skeleton")
            exits {
                east("cave-k-entrance")
                west("cave-k-hall-skeletons")
                south("cave-k-guard-room")
            }
        }

        location("cave-k-hall-skeletons") {
            name = "Hall of Skeletons"
            description = "This unusual audience chamber has a dais and throne-like chair set with 4 large red gems (500gp each) at the south end. A dozen skeletons in rags of chain mail with rusty scimitars stand propped against the walls. They do not move when the chamber is entered, but as soon as anyone touches the dais or throne, they spring to life. Each has an amulet of protection from turning."
            position(-7, 0)
            locationType = LocationType.UNDERGROUND
            creatures("skeleton", "skeleton", "skeleton", "skeleton")
            items("gem-500gp", "gem-500gp", "gem-500gp", "gem-500gp")
            exits {
                east("cave-k-zombie-passage")
                south("cave-k-zombie-guard")
            }
        }

        location("cave-k-zombie-guard") {
            name = "Zombie Guard Room"
            description = "Eight zombies here are turned as ghouls due to their amulets. They are robed in temple garb. Anyone entering will be attacked unless wearing proper temple garments. There is no treasure."
            position(-7, 1)
            locationType = LocationType.UNDERGROUND
            creatures("zombie-guard", "zombie-guard", "zombie-guard", "zombie-guard")
            exits {
                north("cave-k-hall-skeletons")
                south("cave-k-acolytes")
            }
        }

        location("cave-k-guard-room") {
            name = "Southern Guard Room"
            description = "Another guard room with zombies in plate mail and shield. They stand unmoving unless summoned by a chant from the temple, someone enters their area, or commanded by the evil priest. Three zombies guard here."
            position(-6, 1)
            locationType = LocationType.UNDERGROUND
            creatures("zombie-guard", "zombie-guard", "zombie-guard")
            exits {
                north("cave-k-zombie-passage")
                west("cave-k-acolytes")
            }
        }

        location("cave-k-acolytes") {
            name = "Acolytes' Chamber"
            description = "Four acolytes (1st level evil clerics) in rusty-red robes with black cowls are here. Under their robes is chain mail, and they carry maces. Each has 10 gold pieces and wears an amulet of protection from good. The leader's amulet circles the wearer with a magic barrier against good-aligned attacks. Their room has 4 hard pallets, a brazier, table, 4 stools, and a cabinet with wine and cups."
            position(-7, 2)
            locationType = LocationType.UNDERGROUND
            creatures("acolyte", "acolyte", "acolyte", "acolyte")
            exits {
                north("cave-k-zombie-guard")
                east("cave-k-guard-room")
                south("cave-k-chapel")
            }
        }

        location("cave-k-chapel") {
            name = "Chapel of Evil Chaos"
            description = "This place is of red stone, the floor a mosaic checkerboard of black and red. A huge tapestry depicts a black landscape with demons and a skull-faced moon. Four black pillars support a 25' ceiling. Between them is a stone altar of red-veined black rock, stained with dried blood. Upon it are 4 ancient bronze vessels - a bowl, goblets, ewer, and pitcher - worth 4,000gp total but cursed. Touching them requires a saving throw vs. magic at -2 or become a servant of chaos within 6 days."
            position(-7, 3)
            locationType = LocationType.UNDERGROUND
            items("evil-altar-vessels")
            exits {
                north("cave-k-acolytes")
                south("cave-k-adepts")
                west("cave-k-undead-hall")
            }
        }

        location("cave-k-adepts") {
            name = "Adepts' Chamber"
            description = "Four adepts (2nd level evil clerics) in black robes with maroon cowls dwell here. They have plate mail beneath their garments and carry maces. Each has 20 gold, 5 platinum, and an amulet of protection from good. Two have cause light wounds, one has a light spell, and one has cause fear."
            position(-7, 4)
            locationType = LocationType.UNDERGROUND
            creatures("adept", "adept", "adept", "adept")
            exits {
                north("cave-k-chapel")
                west("cave-k-undead-warriors")
            }
        }

        location("cave-k-undead-hall") {
            name = "Hall of Undead Warriors"
            description = "Four files of undead warriors stand here: two files of 10 skeletons, two of 10 zombies. Upon striking the great iron bell at area 58, they will issue forth and march into the temple. The skeletons line the south wall, zombies the north. Intruders in the passage or temple trigger their attack unless wearing proper garments. They have no treasure."
            position(-8, 3)
            locationType = LocationType.UNDERGROUND
            creatures("skeleton", "skeleton", "skeleton", "skeleton")
            creatures("zombie", "zombie", "zombie", "zombie")
            exits {
                east("cave-k-chapel")
                south("cave-k-undead-warriors")
            }
        }

        location("cave-k-undead-warriors") {
            name = "Western Undead Chamber"
            description = "More undead warriors wait here to be summoned by the temple bell."
            position(-8, 4)
            locationType = LocationType.UNDERGROUND
            creatures("skeleton", "skeleton", "zombie", "zombie")
            exits {
                north("cave-k-undead-hall")
                east("cave-k-adepts")
            }
        }

        location("cave-k-temple") {
            name = "Temple of Evil Chaos"
            description = "This huge area has a 30' arched ceiling of polished black stone with swirling red veins. The floor and walls are of dull black rock, but the west wall is of translucent red stone polished to mirror smoothness. A great bell of black iron stands near the entrance. Three stone altars stand to the west - pure black, streaked red and black, and red with black flecks. At the western end is a dais with a throne of bone and ivory adorned with gold and gems worth over 2,000gp. When the party enters, candles light magically and shapeless forms dance on the western wall, chanting a hymn to chaotic evil."
            position(-8, 2)
            locationType = LocationType.UNDERGROUND
            exits {
                east("cave-k-chapel")
                south("cave-k-undead-hall")
                west("cave-k-priest-quarters")
            }
        }

        location("cave-k-priest-quarters") {
            name = "Evil Priest's Quarters"
            description = "The anteroom (59.g.) has lavish furnishings and three zombies on guard. The private chamber is that of the Evil Priest - a 3rd level cleric with plate mail +1, shield +1, sword +1, amulet of protection from good, and a snake staff. He can cast cause light wounds, cause fear, hold person, and more. His demon idol causes 2-12 damage to anyone but him who touches it. A secret wardrobe door leads to an escape passage."
            position(-9, 2)
            locationType = LocationType.UNDERGROUND
            creatures("evil-priest", "zombie-guard", "zombie-guard", "zombie-guard")
            items("demon-idol", "snake-staff", "magic-plate-plus-1", "magic-shield-plus-1", "magic-sword-plus-1")
            exits {
                east("cave-k-temple")
                north("cave-k-guest")
                south("cave-k-torture")
            }
        }

        location("cave-k-guest") {
            name = "Guest Chamber"
            description = "This lower room is for important guests. It contains a large bed, table, chairs, etc. The tapestries depict evil cruelties and obscene rites. Beneath a velvet cloth on the table is a polished mirror. There is nothing of value within."
            position(-9, 1)
            locationType = LocationType.UNDERGROUND
            exits {
                south("cave-k-priest-quarters")
            }
        }

        location("cave-k-torture") {
            name = "Temple Torture Chamber"
            description = "Various implements of torture fill this chamber: a rack, iron maiden, tongs, pincers, whips, etc. Comfortable chairs line the walls for visitors. The torturer is a 3rd level fighter in chain mail under black leather, wielding a huge battle axe. He has 135 gold pieces and a bracelet worth 700gp hidden in his mattress."
            position(-9, 3)
            locationType = LocationType.UNDERGROUND
            creatures("torturer")
            items("bracelet-of-ivory")
            exits {
                north("cave-k-priest-quarters")
                south("cave-k-crypt")
                west("cave-k-storage")
            }
        }

        location("cave-k-crypt") {
            name = "The Crypt"
            description = "The door is bolted shut. Many coffins and sarcophagi line this roughly hewn chamber - the remains of servants of the Temple of Chaos. The sixth tomb opened will contain a wight with a sword +2, scroll of protection from undead, helm of alignment change, and a silver dagger worth 800gp with gems in its pommel."
            position(-9, 4)
            locationType = LocationType.UNDERGROUND
            creatures("wight")
            items("magic-sword-plus-2", "scroll-of-protection-from-undead", "helm-of-alignment-change")
            exits {
                north("cave-k-torture")
            }
        }

        location("cave-k-storage") {
            name = "Temple Storage Chamber"
            description = "Many piles of boxes, crates, barrels, and sacks are here - the supplies of the temple. There is nothing of value, but if the party stays longer than 3 rounds, a gelatinous cube will move down the corridor and block it. Inside the cube are bones containing a wand of enemy detection with 9 charges."
            position(-10, 3)
            locationType = LocationType.UNDERGROUND
            creatures("gelatinous-cube")
            items("wand-of-enemy-detection")
            exits {
                east("cave-k-torture")
                north("cave-k-cell")
            }
        }

        location("cave-k-cell") {
            name = "The Cell"
            description = "The iron door is locked and barred, with a window. Several skeletons are chained to the wall, and in a corner is a scantily clad female who appears to be in need of rescuing. Those who approach closer will see it is actually a medusa recently captured by the evil priest! She has a potion of stone to flesh (enough for six uses) and will bargain for her freedom - but will try to 'stone' her rescuers."
            position(-10, 2)
            locationType = LocationType.UNDERGROUND
            lockLevel = 3
            creatures("medusa")
            items("potion-of-stone-to-flesh")
            exits {
                south("cave-k-storage")
            }
        }

        location("cave-k-boulder-passage") {
            name = "Boulder-Filled Passage"
            description = "Large rocks and boulders have been placed here to seal off this tunnel. It will take 100 man-turns to open a way large enough for a human to pass through. This passage leads outside to the southwest of the Caves of Chaos - or you may connect it to the Cave of the Unknown."
            position(-5, -4)
            locationType = LocationType.UNDERGROUND
            exits {
                east("cave-j-chieftain") // Secret door from gnoll chieftain
            }
        }
    }

    // ==================== CHESTS ====================
    private fun defineChests() {
        chest("kobold-chief-chest") {
            name = "Kobold Chieftain's Chest"
            description = "A locked chest containing the kobold tribe's treasure: 203 copper, 61 silver, and 22 electrum pieces."
            locationSuffix = "cave-a-chieftain"
            guardianCreatureSuffix = "kobold-chieftain"
            isLocked = true
            lockDifficulty = 1
            lootTableSuffix = "treasure-chest-common"
            goldAmount = 50
        }

        chest("orc-leader-chest") {
            name = "Orc Leader's Iron Chest"
            description = "A heavy iron chest in the orc leader's alcove, containing 205 copper, 286 silver, 81 gold, and 13 platinum pieces."
            locationSuffix = "cave-b-leader"
            guardianCreatureSuffix = "orc-leader"
            isLocked = true
            lockDifficulty = 2
            lootTableSuffix = "treasure-chest-rare"
            goldAmount = 200
        }

        chest("bugbear-spoils-chest") {
            name = "Bugbear Spoils Chest"
            description = "A gray chest stuck up on a ledge near the ceiling, containing 1,462 silver pieces, a 30-pound alabaster statue worth 200gp, and 2 potions of healing."
            locationSuffix = "cave-h-spoils"
            guardianCreatureSuffix = "bugbear-chieftain"
            isLocked = true
            lockDifficulty = 2
            lootTableSuffix = "treasure-chest-rare"
            goldAmount = 200
        }

        chest("minotaur-hoard") {
            name = "Minotaur's Hidden Hoard"
            description = "Behind a secret stone slab is the minotaur's treasure: a locked chest with poison needle (930 gold, 310 electrum), a staff of healing, plate mail +1, a locked coffer with 3 potion bottles (gaseous form, healing, growth), and a locked chest with jewelry worth 1,600, 900, and 600gp respectively."
            locationSuffix = "cave-i-minotaur"
            guardianCreatureSuffix = "minotaur"
            isLocked = true
            lockDifficulty = 3
            lootTableSuffix = "treasure-minotaur"
            goldAmount = 930
        }

        chest("evil-priest-wardrobe") {
            name = "Evil Priest's Secret Wardrobe"
            description = "When the secret door in the back of the wardrobe is opened, 500 gold pieces and 50 gems worth 10gp each spill out to hopefully cause pursuers to stop for the loot."
            locationSuffix = "cave-k-priest-quarters"
            guardianCreatureSuffix = "evil-priest"
            isLocked = true
            lockDifficulty = 3
            lootTableSuffix = "temple-treasure"
            goldAmount = 1000
        }
    }

    // ==================== TRAPS ====================
    private fun defineTraps() {
        // === CAVE A: KOBOLD LAIR ===
        // The kobolds have a pit trap near their entrance
        trap("kobold-pit") {
            name = "Kobold Pit Trap"
            description = "A 10-foot deep pit with a closing lid, designed to trap intruders. The kobolds check it regularly."
            locationSuffix = "cave-a-entrance"
            detectDifficulty = 2
            disarmDifficulty = 2
            pit(depth = 10, damageDice = "1d6", saveDC = 12)
            resetsAfterRounds = 60 // Kobolds reset it after a while
        }

        // === CAVE C: SECOND ORC LAIR ===
        // Trip wire alarm at entrance
        trap("orc-tripwire-alarm") {
            name = "Weighted Net Alarm"
            description = "Nearly invisible trip strings 11 feet from the entrance. When triggered, a heavy weighted net drops and metal pieces create an alarm sound."
            locationSuffix = "cave-c-entrance"
            detectDifficulty = 3
            disarmDifficulty = 2
            trigger(TrapTrigger.TRIPWIRE)
            alarm(listOf("orc-guard", "orc", "orc"), "The net falls on you and metal pieces clatter loudly, alerting the orcs!")
        }

        // === CAVE D: GOBLIN LAIR ===
        // Goblins often use dart traps
        trap("goblin-dart-wall") {
            name = "Poisoned Dart Wall"
            description = "A section of wall conceals spring-loaded darts tipped with a mild paralytic."
            locationSuffix = "cave-d-guard-1"
            detectDifficulty = 2
            disarmDifficulty = 3
            trigger(TrapTrigger.PRESSURE_PLATE)
            dart(damageDice = "1d4", saveDC = 12, poisoned = true, poisonDuration = 5)
        }

        // === CAVE F: HOBGOBLIN LAIR ===
        // The militaristic hobgoblins have more sophisticated traps
        trap("hobgoblin-spear-trap") {
            name = "Spear Trap"
            description = "A pressure plate triggers spears to thrust up from concealed holes in the floor."
            locationSuffix = "cave-f-entrance"
            detectDifficulty = 3
            disarmDifficulty = 3
            spear(damageDice = "2d6", saveDC = 14)
        }

        // Alarm in the torture room
        trap("hobgoblin-alarm-bell") {
            name = "Alarm Bell"
            description = "A thin wire near the door rings a bell if disturbed, alerting the guards."
            locationSuffix = "cave-f-torture"
            detectDifficulty = 2
            disarmDifficulty = 1
            trigger(TrapTrigger.TRIPWIRE)
            alarm(listOf("hobgoblin", "hobgoblin"), "A bell rings out, echoing through the corridors!")
        }

        // === CAVE G: SHUNNED CAVERN ===
        // Natural hazards - the gray ooze pit
        trap("ooze-pit") {
            name = "Gray Ooze Pit"
            description = "The floor here is unstable, concealing a pit filled with corrosive gray ooze."
            locationSuffix = "cave-g-gallery"
            detectDifficulty = 4
            disarmDifficulty = 0 // Can't be disarmed, only avoided
            isArmed = true
            pit(depth = 5, damageDice = "2d6", saveDC = 13)
            message("You fall into a pit of writhing gray ooze! The acidic slime burns!")
        }

        // === CAVE H: BUGBEAR LAIR ===
        // Bugbears are master ambushers - they have a cage trap
        trap("bugbear-cage-drop") {
            name = "Dropping Cage"
            description = "A large iron cage hangs above, ready to drop on unsuspecting intruders."
            locationSuffix = "cave-h-entrance"
            detectDifficulty = 3
            disarmDifficulty = 4
            trigger(TrapTrigger.PRESSURE_PLATE)
            cage(duration = 10, saveDC = 14)
        }

        // === CAVE I: MINOTAUR LABYRINTH ===
        // The minotaur's lair has several traps
        trap("minotaur-boulder") {
            name = "Rolling Boulder"
            description = "A massive boulder is balanced precariously, ready to roll down the corridor when triggered."
            locationSuffix = "cave-i-entrance"
            detectDifficulty = 2
            disarmDifficulty = 5 // Very hard to stop a boulder
            trigger(TrapTrigger.TRIPWIRE)
            boulder(damageDice = "4d6", saveDC = 15)
        }

        // Poison needle on the minotaur's treasure chest
        trap("minotaur-treasure-needle") {
            name = "Poison Needle Lock"
            description = "The lock on the minotaur's treasure chest is fitted with a poison needle."
            locationSuffix = "cave-i-minotaur"
            detectDifficulty = 3
            disarmDifficulty = 3
            trigger(TrapTrigger.CHEST)
            poisonNeedle(damageDice = "1", poisonDuration = 20, saveDC = 14)
        }

        // === CAVE J: GNOLL LAIR ===
        // Gnolls use crude but effective traps
        trap("gnoll-spike-pit") {
            name = "Spiked Pit"
            description = "A pit trap concealed by a thin layer of hides and dirt, with sharpened stakes at the bottom."
            locationSuffix = "cave-j-entrance"
            detectDifficulty = 2
            disarmDifficulty = 2
            pit(depth = 15, damageDice = "2d6+2", saveDC = 13)
            message("You fall into a pit lined with sharpened stakes!")
        }

        // === CAVE K: TEMPLE OF EVIL CHAOS ===
        // The most dangerous traps are in the evil temple
        trap("temple-fire-jet") {
            name = "Sacred Fire Jets"
            description = "Hidden nozzles in the walls spray unholy fire at intruders who step on the wrong floor tile."
            locationSuffix = "cave-k-chapel"
            detectDifficulty = 4
            disarmDifficulty = 4
            trigger(TrapTrigger.PRESSURE_PLATE)
            fire(damageDice = "3d6", saveDC = 14)
        }

        trap("temple-teleport") {
            name = "Banishment Circle"
            description = "A magical sigil on the floor teleports intruders to the cell area."
            locationSuffix = "cave-k-temple"
            detectDifficulty = 5
            disarmDifficulty = 5
            trigger(TrapTrigger.MOVEMENT)
            teleport("cave-k-cell", "Dark magic swirls around you and you find yourself elsewhere!")
        }

        trap("zombie-pit") {
            name = "Zombie Pit"
            description = "A pit filled with grasping undead hands that try to pull victims down."
            locationSuffix = "cave-k-zombie-passage"
            detectDifficulty = 3
            disarmDifficulty = 0 // Can't disarm zombies
            pit(depth = 10, damageDice = "1d6", saveDC = 12)
            message("You fall into a pit of grasping zombie hands!")
        }

        // The boulder trap from the original module
        trap("temple-boulder") {
            name = "Temple Guardian Boulder"
            description = "A massive stone sphere ready to roll down the passage, crushing all in its path."
            locationSuffix = "cave-k-boulder-passage"
            detectDifficulty = 2
            disarmDifficulty = 5
            trigger(TrapTrigger.TRIPWIRE)
            boulder(damageDice = "6d6", saveDC = 16)
        }

        // Magic trap on the crypt
        trap("crypt-curse") {
            name = "Curse of the Crypt"
            description = "Ancient magic protects the crypt, cursing those who disturb its contents."
            locationSuffix = "cave-k-crypt"
            detectDifficulty = 4
            disarmDifficulty = 5
            trigger(TrapTrigger.INTERACTION)
            magic(
                condition = "cursed",
                conditionDuration = 100,
                message = "Dark energy crackles as you disturb the crypt. You feel cursed!"
            )
        }
    }

    // ==================== FACTIONS ====================
    private fun defineFactions() {
        // === KOBOLD TRIBE ===
        // The weakest tribe, kobolds are cowardly but cunning
        faction("kobold") {
            name = "Kobold Tribe"
            description = "A tribe of small, cowardly reptilian creatures who rely on traps and numbers. They fear the stronger tribes and worship dragons."
            homeLocationSuffix = "cave-a-chieftain"
            hostile()
            leaderCreatureSuffix = "kobold-chieftain"
            territory(
                "cave-a-entrance", "cave-a-guard", "cave-a-common",
                "cave-a-chieftain", "cave-a-storage"
            )
            enemies("gnoll")  // Gnolls prey on kobolds
            goals("Survive", "Find dragon patron", "Avoid stronger tribes")
        }

        // === GOBLIN TRIBE ===
        // Allied with the ogre, rivals of the hobgoblins
        faction("goblin") {
            name = "Goblin Tribe"
            description = "A tribe of cruel, cunning goblins who use hit-and-run tactics. They've hired an ogre mercenary and resent the hobgoblins' authority."
            homeLocationSuffix = "cave-d-chief"
            hostile()
            leaderCreatureSuffix = "goblin-king"
            territory(
                "cave-d-entrance", "cave-d-guard-1", "cave-d-guard-2",
                "cave-d-common", "cave-d-chief", "cave-d-treasure",
                "cave-e-entrance", "cave-e-ogre"  // Ogre is their ally
            )
            allies("ogre")  // Ogre is their hired muscle
            enemies("hobgoblin")  // Long-standing rivalry
            goals("Raid caravans", "Overthrow hobgoblins", "Acquire treasure")
        }

        // === OGRE (Ally of Goblins) ===
        faction("ogre") {
            name = "Ogre Mercenary"
            description = "A lone ogre who works as a mercenary for the goblins and hobgoblins. He's motivated purely by gold and food."
            homeLocationSuffix = "cave-e-ogre"
            hostilityLevel = 60  // Can be bribed
            canNegotiate = true
            territory("cave-e-entrance", "cave-e-ogre")
            allies("goblin", "hobgoblin")
            goals("Get paid", "Eat well", "Avoid danger")
        }

        // === ORC TRIBE A (Larger tribe) ===
        faction("orc") {
            name = "Orc Tribe of the Bloody Fang"
            description = "A large orc tribe led by a brutal chief. They are rivals with the other orc tribe in the ravine and suspicious of all outsiders."
            homeLocationSuffix = "cave-b-chieftain"
            hostile()
            leaderCreatureSuffix = "orc-chieftain"
            territory(
                "cave-b-entrance", "cave-b-guard", "cave-b-storage",
                "cave-b-common", "cave-b-chieftain"
            )
            enemies("orc-rival")  // The other orc tribe
            goals("Raid settlements", "Destroy rival tribe", "Prove strength")
        }

        // === ORC TRIBE B (Smaller rival tribe) ===
        faction("orc-rival") {
            name = "Orc Tribe of the Broken Skull"
            description = "A smaller orc tribe that constantly feuds with the larger Bloody Fang tribe. They are more desperate and willing to take risks."
            homeLocationSuffix = "cave-c-chieftain"
            hostile()
            leaderCreatureSuffix = "orc-chieftain-2"
            territory(
                "cave-c-entrance", "cave-c-guard", "cave-c-storage",
                "cave-c-common", "cave-c-chieftain"
            )
            enemies("orc")  // The other orc tribe
            goals("Survive", "Destroy rival tribe", "Gain power")
        }

        // === HOBGOBLIN LEGION ===
        // The most organized, they dominate many of the other tribes
        faction("hobgoblin") {
            name = "Hobgoblin Legion"
            description = "A disciplined military force of hobgoblins who consider themselves superior to the other tribes. They maintain order through fear and strength."
            homeLocationSuffix = "cave-f-captain"
            hostile()
            leaderCreatureSuffix = "hobgoblin-captain"
            territory(
                "cave-f-entrance", "cave-f-guard", "cave-f-barracks",
                "cave-f-common", "cave-f-captain", "cave-f-torture",
                "cave-f-armory", "cave-f-prison"
            )
            allies("ogre")  // Also employ the ogre
            enemies("goblin")  // View goblins as inferior
            goals("Maintain order", "Expand territory", "Crush goblins")
        }

        // === BUGBEAR GANG ===
        // Cunning ambushers who prey on everyone
        faction("bugbear") {
            name = "Bugbear Ambushers"
            description = "A gang of stealthy bugbears who live by ambushing travelers and raiding the other tribes' camps when they're weak."
            homeLocationSuffix = "cave-h-chief"
            hostile()
            leaderCreatureSuffix = "bugbear-chief"
            territory(
                "cave-h-entrance", "cave-h-guard", "cave-h-common", "cave-h-chief"
            )
            // Bugbears don't have real allies - they exploit everyone
            goals("Ambush prey", "Steal from other tribes", "Avoid open combat")
        }

        // === GNOLL PACK ===
        // Savage hyena-folk who worship demons
        faction("gnoll") {
            name = "Gnoll Pack"
            description = "A savage pack of gnolls driven by hunger and their demonic bloodlust. They are feared by the smaller tribes."
            homeLocationSuffix = "cave-j-chief"
            hostile()
            noNegotiation()  // Gnolls are too savage to negotiate
            leaderCreatureSuffix = "gnoll-chieftain"
            territory(
                "cave-j-entrance", "cave-j-guard", "cave-j-common", "cave-j-chief"
            )
            enemies("kobold")  // Kobolds are easy prey
            goals("Feed the pack", "Spread chaos", "Serve demon lords")
        }

        // === MINOTAUR ===
        // Solitary beast in the labyrinth
        faction("minotaur") {
            name = "Minotaur of the Labyrinth"
            description = "A solitary minotaur who guards its labyrinthine lair. It has no allies and attacks any who enter its domain."
            homeLocationSuffix = "cave-i-minotaur"
            hostile()
            noNegotiation()  // The minotaur is a beast
            leaderCreatureSuffix = "minotaur"
            territory(
                "cave-i-entrance", "cave-i-stirge", "cave-i-beetles-1",
                "cave-i-beetles-2", "cave-i-minotaur"
            )
            goals("Guard territory", "Kill intruders")
        }

        // === OWLBEAR ===
        // Territorial beast in the shunned cavern
        faction("owlbear") {
            name = "Shunned Cavern Predator"
            description = "A territorial owlbear that has claimed the shunned cavern as its lair. Even the other tribes avoid this area."
            homeLocationSuffix = "cave-g-owlbear"
            hostile()
            noNegotiation()  // Beast
            leaderCreatureSuffix = "owlbear"
            territory(
                "cave-g-entrance", "cave-g-gallery", "cave-g-owlbear"
            )
            goals("Hunt prey", "Defend territory")
        }

        // === CULT OF EVIL CHAOS ===
        // The true power behind the caves
        faction("evil-temple") {
            name = "Cult of Evil Chaos"
            description = "A sinister cult that manipulates the humanoid tribes from the shadows. Led by a powerful evil priest, they seek to spread chaos and corruption."
            homeLocationSuffix = "cave-k-priest-quarters"
            hostile()
            leaderCreatureSuffix = "evil-priest"
            territory(
                "cave-k-entrance", "cave-k-antechamber", "cave-k-chapel",
                "cave-k-priest-quarters", "cave-k-temple", "cave-k-cell",
                "cave-k-zombie-passage", "cave-k-boulder-passage", "cave-k-crypt"
            )
            // The cult secretly manipulates everyone
            goals("Spread chaos", "Corrupt the tribes", "Summon dark powers")
        }

        // === FACTION RELATIONSHIPS ===
        // Goblins vs Hobgoblins (mutual hatred)
        factionRelation("goblin", "hobgoblin", -75)
        factionRelation("hobgoblin", "goblin", -75)

        // Orc tribes hate each other
        factionRelation("orc", "orc-rival", -100)
        factionRelation("orc-rival", "orc", -100)

        // Gnolls prey on kobolds
        factionRelation("gnoll", "kobold", -50)
        factionRelation("kobold", "gnoll", -80)  // Kobolds fear gnolls more

        // Ogre works with goblins and hobgoblins
        factionRelation("ogre", "goblin", 50)
        factionRelation("goblin", "ogre", 40)
        factionRelation("ogre", "hobgoblin", 30)
        factionRelation("hobgoblin", "ogre", 20)

        // Evil temple manipulates everyone (they don't know it)
        factionRelation("evil-temple", "hobgoblin", 25)  // Temple influences hobgoblins
        factionRelation("evil-temple", "gnoll", 25)  // Temple uses gnolls

        // Everyone fears the bugbears' ambushes
        factionRelation("kobold", "bugbear", -60)
        factionRelation("goblin", "bugbear", -40)
    }
}
