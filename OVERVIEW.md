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
- **Accuracy** - Bonus to hit chance
- **Evasion** - Bonus to avoid attacks
- **Crit Bonus** - Bonus to critical hit chance
- **Initiative** - Action order within round (D20 roll)
- **Level** - Overall power, affects hit chance

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

### Future Progression (Not Yet Implemented)
- Skill trees within classes
- Equipment upgrades
- Achievement system
- Reputation with factions

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
