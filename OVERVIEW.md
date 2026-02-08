# AnotherThread - Game Overview

A MajorMUD-inspired multiplayer text adventure with AI-powered content generation and real-time combat.

## Vision

AnotherThread aims to combine the depth of classic MUD (Multi-User Dungeon) games with modern AI capabilities. Players explore a persistent world, engage in real-time combat, and contribute to world-building through AI-assisted content creation.

---

## Core Game Mechanics

### World Structure

**Locations**
- Discrete areas connected by exits (N, NE, E, SE, S, SW, W, NW, UP, DOWN)
- Each location has a name, description, and optional AI-generated image
- Can contain creatures, items, and features
- Placed on a 3D coordinate grid (X, Y, Z) for spatial organization

**Creatures**
- NPCs and monsters that inhabit locations
- Combat stats: HP, base damage, level, experience value
- Can be aggressive (attack on sight) or passive
- Drop items and experience when defeated

**Items**
- Objects that can be in locations, creature inventories, or player inventories
- Weapons grant combat abilities (see [Combat](#combat-system))
- Items have features describing their properties

**Features**
- Modular attributes organized into categories
- Can be attached to any entity type (location, creature, item, user)
- Used for terrain typing, special properties, descriptive tags

### Character System

**Users/Players**
- Persistent accounts with authentication
- Character description and AI-generated portrait
- Assigned a character class (see [Classes](#character-classes))
- Track current location, HP, level, experience

**Character Classes**
Classes define a character's abilities and combat role. Two types exist:

1. **Stock Classes** - Pre-defined, balanced classes available to all players
   - Spellcaster, Martial, Scoundrel, Bard, Alchemist, Ranger
   - See [STOCK.md](STOCK.md) for detailed balance analysis

2. **Generated Classes** - Custom classes created via AI during character creation
   - Private to the creating user (future: teachable to others)
   - LLM generates thematic abilities matching character description

### Combat System

Real-time, round-based combat inspired by MajorMUD.

**Round Structure**
1. Round begins (3 seconds duration)
2. Players queue abilities during the round
3. At round end, actions resolve in initiative order
4. Status effects tick (DoTs, HoTs, buffs expire)
5. Combat checks for end conditions
6. Repeat until victory or defeat

**Hit/Miss Mechanics (RNG System)**
| Mechanic | Value | Description |
|----------|-------|-------------|
| Base Hit Chance | 75% | Modified by accuracy vs evasion |
| Level Bonus | +2% per level diff | Higher level = more likely to hit |
| Damage Variance | ±25% | All damage/healing varies |
| Critical Hit | 5% base | Deals 2x damage |
| Glancing Blow | ~10% | Near-miss deals 50% damage |
| Hit Chance Cap | 5-95% | Always some chance to hit/miss |

**Combat Stats**
- **HP** - Health points, 0 = defeated
- **Mana** - Resource for casting spells (regenerates 2/round in combat)
- **Stamina** - Resource for physical abilities (regenerates 3/round in combat)
- **Accuracy** - Bonus to hit chance
- **Evasion** - Bonus to avoid attacks
- **Crit Bonus** - Bonus to critical hit chance
- **Initiative** - Action order within round (D20 roll)
- **Level** - Overall power, affects hit chance

**Resource System**
Each character class defines base mana and stamina pools:
- **Spellcasters** typically have higher mana pools (e.g., 30 mana, 10 stamina)
- **Martial classes** typically have higher stamina pools (e.g., 10 mana, 30 stamina)
- **Hybrid classes** have balanced pools (e.g., 20 mana, 20 stamina)

Abilities cost resources based on their type:
- **Spell abilities** cost mana (calculated from power cost)
- **Combat abilities** cost stamina (calculated from power cost)
- **Utility abilities** cost either (based on ability design)
- **Passive abilities** have no cost

Resources regenerate during combat:
- Mana: +2 per round
- Stamina: +3 per round (faster recovery for physical fighters)

**Abilities**
Each ability has:
- **Type**: spell, combat, utility, passive
- **Target**: self, single_enemy, single_ally, area, all_enemies, all_allies
- **Cooldown**: none, short (1 round), medium (3 rounds), long (5+ rounds)
- **Base Damage/Healing**: Modified by RNG
- **Effects**: buff, debuff, dot (damage over time), hot (heal over time), stun, root, slow

**Status Effects**
| Effect Type | Description |
|-------------|-------------|
| DoT (Damage over Time) | Deals damage each round |
| HoT (Heal over Time) | Heals each round |
| Buff | Positive stat modifier |
| Debuff | Negative stat modifier |
| Stun | Cannot act |
| Root | Cannot move |
| Slow | Reduced initiative |

**Weapon Abilities**
Items can grant combat abilities:
- Equipped weapons provide special attacks
- Each weapon has themed abilities matching its lore
- Example: Staff of Kazekage grants "Soothing Winds" (heal) and "Inner Balance" (buff)

### Adventure Mode UI

**Detail Views**
Tapping a creature or item in the world opens a detail view with:
- Enlarged image (150x150)
- Full description
- Stats (for creatures: Level, HP, Damage, XP value)
- Equipment info (for items: type, slot, stat bonuses, gold value)
- Action buttons (Attack, Greet for creatures; Pick up for items)

**Entity Identification**
Some item/creature stats may be hidden until identified:
- Use the Identify spell (utility ability) to reveal hidden properties
- Once identified, properties remain visible permanently for that user
- Tracked per-user in the database

**Class-Specific UI Features**
Certain character classes have unique UI enhancements:

| Class | Feature | Description |
|-------|---------|-------------|
| Ranger | Directional Attunement | Location thumbnail shows a minimap overlay with the location image behind (semi-transparent). Green-themed map shows nearby locations and connections. |

### AI Content Generation

**Text Generation (Ollama)**
- Location names and descriptions based on terrain and neighbors
- Creature descriptions based on location context
- Item descriptions based on features and context
- Character class generation from player descriptions

**Image Generation (Stable Diffusion)**
- Location artwork based on description
- Character portraits
- Creature and item images

**Class Generation Flow**
1. User creates character with description
2. User chooses "Generate Class"
3. LLM analyzes description and existing classes
4. LLM creates new class with 10 thematic abilities
5. Class is private to user (not available to others)

---

## Progression Systems

### Experience & Leveling

**Individual XP Scaling**
XP is calculated individually per player based on their level vs creature Challenge Rating (CR). This allows party play with level disparity - a level 1 player in a party with level 10 players still gets appropriate XP.

| Level Difference | Scale Factor | Description |
|------------------|--------------|-------------|
| CR >= Player+4 | 1.5x | Much harder |
| CR >= Player+2 | 1.25x | Harder |
| CR >= Player-1 | 1.0x | Appropriate |
| CR >= Player-4 | 0.5x | Easy |
| CR >= Player-8 | 0.25x | Trivial |
| CR < Player-8 | 0.1x | Grey (floor) |

**XP = creature.experienceValue × scaleFactor**

**Level Thresholds**
| Level | Total XP | HP |
|-------|----------|-----|
| 1 | 0 | 10 |
| 5 | 1,000 | 30 |
| 10 | 4,500 | 55 |
| 15 | 10,500 | 80 |
| 20 (cap) | 19,000 | 105 |

HP formula: 10 + (level - 1) × 5

**PvP Experience**
- Base XP: 20 + (opponent level × 5)
- Same scaling factors apply
- 50% reduction to discourage farming

**Creature Challenge Rating**
Each creature has:
- `challengeRating` (1-20) - Difficulty tier for XP scaling
- `experienceValue` - Base XP before scaling
- `level` - Used for combat hit/miss calculations

### Level-Gated Abilities
Characters unlock new abilities as they level up. Each ability has a minimum level requirement:
- Level 1: Basic attacks and utility abilities
- Level 3-5: Intermediate abilities
- Level 7+: Advanced abilities

The `learnedAbilityIds` field on users tracks which abilities they've unlocked.

### Future Progression (Not Yet Implemented)
- Skill trees within classes
- Equipment upgrades
- Achievement system
- Reputation with factions

---

## Stealth System

MajorMUD-inspired stealth mechanics allow players to hide and sneak.

### Hide
- Attempt to become hidden from view
- Success based on: `15% + (DEX mod * 5) + (DEX breakpoint * 8) + (level * 2) + classBonus`
- Thief-type classes get +20% bonus
- Cannot hide during combat
- Other players may detect you based on their Perception

### Sneak
- Move silently between locations
- Success based on similar formula to Hide
- Checked when entering new locations
- Failed sneak reveals you to observers

### Detection
When a hidden/sneaking player is nearby, observers roll Perception:
- `20% + (WIS mod * 5) + (WIS breakpoint * 8) + (level * 2) + classBonus`
- Rangers get +15% detection bonus
- Successful detection reveals the hidden player

---

## Hidden Ground Items

Items left on the ground become hidden over time, rewarding exploration and searching.

### Mechanics
1. **Fresh items (< 24 hours)**: Visible to everyone
2. **Old items (>= 24 hours)**: Hidden, require Search action
3. **Discovered items**: Once found by a player, remain visible to them

### Search Action
- `POST /users/{id}/search` - Search current location
- Success: `30% + (INT mod * 6) + (INT breakpoint * 8) + (level * 2) + classBonus`
- Thief-type classes get +25% search bonus
- Each hidden item rolled separately

### Integration
- Combat drops are tracked with timestamps
- Stat summaries include `searchChance` for UI display

---

## MajorMUD-Style Stat Modifiers

Attributes use a breakpoint system inspired by MajorMUD:

### Attribute Modifier
| Score | Modifier |
|-------|----------|
| 3 | -4 |
| 4-5 | -3 |
| 6-7 | -2 |
| 8-9 | -1 |
| 10-11 | 0 |
| 12-13 | +1 |
| 14-15 | +2 |
| 16-17 | +3 |
| 18+ | +4 |

### Breakpoint Bonuses
Additional bonuses at key thresholds:
- 14+: +1 breakpoint bonus
- 16+: +2 breakpoint bonus
- 18+: +3 breakpoint bonus

### Derived Stats
| Stat | Formula |
|------|---------|
| Melee Hit | +2% per STR mod |
| Melee Damage | STR mod |
| Ranged Hit | +2% per DEX mod |
| AC Bonus | DEX mod |
| HP Bonus | CON mod per level |
| Spell Power | INT mod |
| Search Chance | 30% + (INT mod * 6) + breakpoint |
| Hide Chance | 15% + (DEX mod * 5) + breakpoint |
| Sneak Chance | 15% + (DEX mod * 5) + breakpoint |

---

## Multi-Attack System

High-level characters and creatures can make multiple attacks per round.

### Calculation
- Base: 1 attack
- +1 attack at level 6
- +1 attack at level 12
- +1 attack at level 18
- Maximum: 4 attacks per round

### Combat Integration
- Each attack rolls hit/miss independently
- Damage calculated per successful hit
- Status effects may apply on any hit

---

## Encumbrance

Inventory capacity is limited by Constitution.

### Formula
- Max items = 5 + (CON / 2)
- Example: CON 14 = 12 item capacity

### Movement Restriction
- Over-encumbered players cannot move
- Must drop items before traveling

---

## Multiplayer Features

### Current
- Multiple players can be in the same location
- Real-time combat with multiple participants
- Shared world state

### Future (Not Yet Implemented)
- Party system for coordinated combat
- Trading between players
- Guild/faction system
- PvP combat (consensual)

---

## Administrative Features

**World Building**
- Visual location graph editor
- Create/edit/delete all entity types
- Bidirectional exit management
- Terrain override system

**Database Management**
- Backup/restore functionality
- Audit logging for changes

**Content Generation**
- Batch generation tools
- Generation status monitoring

---

## Implementation Gaps

See [MUSINGS.md](MUSINGS.md) for:
- Known issues and bugs
- Planned features not yet implemented
- Design decisions still to be made
- Technical debt to address

---

## Technical Documentation

For architecture, API reference, and setup:
- See [README.md](README.md) for technical documentation
- See [STOCK.md](STOCK.md) for class balance analysis
