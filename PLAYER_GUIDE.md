# AnotherThread Player Guide

Welcome to AnotherThread, a multiplayer text adventure inspired by classic MUDs like MajorMUD.

---

## Getting Started

### Creating Your Character

1. **Register** an account with a username and password
2. **Describe** your character - this determines your appearance and influences class generation
3. **Choose a class** - pick a stock class or generate a custom one based on your description
4. **Get your portrait** - an AI-generated image based on your description

### Your First Steps

Once logged in, you'll find yourself in the world. Use these basic commands:
- Look around to see your location, exits, creatures, and items
- Move using exits (North, South, East, West, Up, Down, etc.)
- Check your stats to see HP, level, and experience

---

## Combat

Combat in AnotherThread is **real-time and round-based**. Rounds last 3 seconds.

### How Combat Works

1. **Enter combat** by attacking a creature or being attacked by an aggressive one
2. **Queue abilities** during each round
3. **Actions resolve** at the end of the round in initiative order
4. **Repeat** until you win, lose, or flee

### Combat Tips

- **Queue early** - don't wait until the last second
- **Watch your HP** - if it hits 0, you're defeated
- **Use abilities wisely** - some have cooldowns
- **Flee if needed** - there's no shame in retreat (50% success chance)

### Understanding Hits and Misses

| What Happened | What It Means |
|---------------|---------------|
| **Hit** | Normal damage dealt |
| **Critical Hit** | 2x damage! (5% base chance) |
| **Glancing Blow** | 50% damage (near miss) |
| **Miss** | No damage dealt |

Higher level targets are harder to hit. Higher level attackers hit more often.

### Status Effects

During combat, you may be affected by:

| Effect | What It Does |
|--------|--------------|
| **DoT** (Burning, Poison) | Damage each round |
| **HoT** (Regenerating) | Healing each round |
| **Buff** | Temporary stat bonus |
| **Debuff** | Temporary stat penalty |
| **Stun** | Cannot act |
| **Root** | Cannot move |
| **Slow** | Act later in initiative |

---

## Leveling Up

### Gaining Experience

You earn XP by defeating creatures. The amount depends on how challenging the fight was for YOU specifically:

| Difficulty | XP Multiplier |
|------------|---------------|
| Much harder (CR 4+ above you) | 1.5x |
| Harder (CR 2-3 above you) | 1.25x |
| Appropriate (CR within 1 of you) | 1.0x |
| Easy (CR 2-4 below you) | 0.5x |
| Trivial (CR 5-8 below you) | 0.25x |
| Grey (CR 9+ below you) | 0.1x |

**Party play is carry-friendly!** If a level 10 friend helps you kill a tough creature, you get full XP based on YOUR level - the creature doesn't become "grey" just because your friend is high level.

### Level Progression

| Level | Total XP Needed | Max HP |
|-------|-----------------|--------|
| 1 | 0 | 10 |
| 2 | 100 | 15 |
| 3 | 300 | 20 |
| 4 | 600 | 25 |
| 5 | 1,000 | 30 |
| 10 | 4,500 | 55 |
| 15 | 10,500 | 80 |
| 20 (max) | 19,000 | 105 |

**When you level up:**
- Your max HP increases by 5
- You're fully healed
- You become slightly better at hitting enemies

---

## Classes & Abilities

### Stock Classes

These pre-made classes are available to everyone:

- **Spellcaster** - Ranged magic damage and utility
- **Martial** - Melee combat and defense
- **Scoundrel** - Stealth, crits, and debuffs
- **Bard** - Buffs, heals, and crowd control
- **Alchemist** - DoTs, HoTs, and transformations
- **Ranger** - Balanced ranged/melee with tracking

### Custom Classes

During character creation, you can generate a unique class based on your character description. These classes are private to you and themed to match your character's story.

### Ability Types

| Type | When to Use |
|------|-------------|
| **Attack** | Deal damage to enemies |
| **Heal** | Restore HP to yourself or allies |
| **Buff** | Boost stats temporarily |
| **Debuff** | Weaken enemies |
| **Utility** | Non-combat abilities like teleport, invisibility |

### Cooldowns

Some abilities can't be spammed:
- **None** - Use every round
- **Short** (1-2 rounds) - Minor cooldown
- **Medium** (3-4 rounds) - Plan your use
- **Long** (5+ rounds) - Save for the right moment

---

## Spells (Utility)

Beyond combat abilities, some spells work outside of combat:

| Spell | What It Does |
|-------|--------------|
| **Light** | Illuminate dark areas |
| **Recall** | Teleport to your home point |
| **Teleport** | Travel to known locations |
| **Detect Secret** | Reveal hidden exits and traps |
| **Invisibility** | Avoid detection |
| **Phase Walk** | Pass through walls to adjacent rooms |
| **Levitate** | Access UP/DOWN without stairs |
| **Unlock** | Open locked doors |

Utility spells have **cooldowns** (some measured in real-time seconds) and may have **level requirements**.

---

## The World

### Locations

The world is made of interconnected locations. Each has:
- A name and description
- Exits to other locations (N, NE, E, SE, S, SW, W, NW, UP, DOWN)
- Possibly creatures, items, and special features

### Creatures

Creatures range from harmless wildlife to dangerous monsters:

- **Passive** creatures won't attack unless provoked
- **Aggressive** creatures attack on sight
- **Challenge Rating (CR)** indicates difficulty - match it to your level
- Creatures drop **items** and **experience** when defeated

### Items

Items can be found, looted, or purchased:
- **Weapons** grant special combat abilities
- **Equipment** (future) will provide stat bonuses
- Items have descriptions and properties

---

## Tips for Success

1. **Start with appropriate challenges** - fight creatures near your level
2. **Learn your abilities** - know what each one does and when to use it
3. **Watch cooldowns** - don't waste powerful abilities
4. **Party up** - combat is easier with friends (and XP scales individually!)
5. **Explore** - the world is vast and full of secrets
6. **Don't fear death** - it's part of the adventure

---

## Getting Help

- Check the in-game help commands
- Ask other players
- Report bugs at the project repository

Good luck, adventurer!
