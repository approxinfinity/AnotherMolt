# Stock Classes Analysis

## Overview
6 stock classes with 10 abilities each (60 total abilities).

---

## Class Summary

| Class | Type | Hit Die | Primary Attr | Role Focus |
|-------|------|---------|--------------|------------|
| Spellcaster | Caster | d6 | Intelligence | Control/Utility |
| Martial | Martial | d10 | Strength | Damage/Defense |
| Scoundrel | Martial | d8 | Dexterity | Damage/Stealth |
| Bard | Caster | d8 | Charisma | Support/Control |
| Alchemist | Caster | d8 | Intelligence | Versatile/Area |
| Ranger | Martial | d10 | Dexterity | Damage/Tracking |

---

## Ability Matrix

### SPELLCASTER

| Ability | Damage Type | Role | Target | Cooldown |
|---------|-------------|------|--------|----------|
| Arcane Anchor | N/A | Control | Area | Long |
| Soul Tether | N/A | Protection | Single Ally | Medium |
| Temporal Echo | N/A | Utility | Self | Long |
| Mind Mirror | Psychic | Control | Single Enemy | Medium |
| Elemental Conversion | N/A | Terrain | Area | Long |
| Spirit Summon | N/A | Summon | Area | Long |
| Prescient Warning | N/A | Buff | All Allies | Medium |
| Mana Siphon | N/A | Debuff | Single Enemy | Long |
| Gravity Well | N/A | Control | Area | Medium |
| Life Bloom | Radiant | Healing/Damage | Area | Long |

### MARTIAL

| Ability | Damage Type | Role | Target | Cooldown |
|---------|-------------|------|--------|----------|
| Tactical Assessment | N/A | Utility/Buff | Single Enemy | Short |
| Shield Wall | N/A | Buff | Area | None |
| Precision Strike | Physical | Damage | Single Enemy | Medium |
| Shadowstep | Physical | Utility/Damage | Self | Medium |
| Battle Cry | N/A | Debuff | All Enemies | Long |
| Vital Strike | Physical | Control | Single Enemy | Medium |
| Second Wind | N/A | Healing | Self | Long |
| Riposte | Physical | Damage | Single Enemy | Short |
| Hunter's Mark | Physical | Damage | Single Enemy | None |
| Iron Will | N/A | Buff | Self | Long |

### SCOUNDREL

| Ability | Damage Type | Role | Target | Cooldown |
|---------|-------------|------|--------|----------|
| Cheap Shot | Physical | Damage/Control | Single Enemy | Medium |
| Smoke Bomb | N/A | Utility | Area | Medium |
| Backstab | Physical | Damage | Single Enemy | None |
| Evasive Roll | N/A | Defense | Self | Short |
| Poison Blade | Poison | Damage/Buff | Self | Long |
| Pickpocket | N/A | Utility | Single Enemy | Short |
| Feint | N/A | Buff/Debuff | Single Enemy | Short |
| Disappearing Act | N/A | Utility | Self | Long |
| Exploit Weakness | Physical | Damage/Debuff | Single Enemy | Long |
| Slippery | N/A | Utility | Self | None |

### BARD

| Ability | Damage Type | Role | Target | Cooldown |
|---------|-------------|------|--------|----------|
| Inspiring Melody | N/A | Buff | All Allies | Short |
| Vicious Mockery | Psychic | Damage/Debuff | Single Enemy | None |
| Song of Rest | N/A | Healing | All Allies | Long |
| Cutting Words | N/A | Debuff | Single Enemy | Short |
| Charm Person | N/A | Control | Single Enemy | Medium |
| Countercharm | N/A | Buff | All Allies | Medium |
| Hypnotic Pattern | N/A | Control | Area | Long |
| Healing Word | N/A | Healing | Single Ally | Short |
| Disguise Self | N/A | Utility | Self | Medium |
| Thunderwave | Thunder | Damage/Control | Area | Medium |

### ALCHEMIST

| Ability | Damage Type | Role | Target | Cooldown |
|---------|-------------|------|--------|----------|
| Alchemist Fire | Fire | Damage/Control | Area | Medium |
| Healing Draught | N/A | Healing | Single Ally | Short |
| Acid Flask | Acid | Damage/Debuff | Single Enemy | Medium |
| Smoke Concoction | N/A | Control | Area | Medium |
| Mutagen | N/A | Buff | Self | Long |
| Flash Powder | N/A | Control | Area | Short |
| Tanglefoot Bag | N/A | Control | Area | Medium |
| Antidote | N/A | Healing | Single Ally | Short |
| Thunderstone | Thunder | Damage/Control | Area | Medium |
| Philosopher's Stone | Variable | Healing/Damage | Single Ally | Long |

### RANGER

| Ability | Damage Type | Role | Target | Cooldown |
|---------|-------------|------|--------|----------|
| Favored Enemy | Physical | Damage | Self | None |
| Volley | Piercing | Damage | Area | Medium |
| Natural Explorer | N/A | Utility | All Allies | None |
| Ensnaring Strike | Piercing | Control/Damage | Single Enemy | Medium |
| Multiattack | Physical | Damage | Single Enemy | None |
| Pass Without Trace | N/A | Buff | All Allies | Long |
| Colossus Slayer | Physical | Damage | Single Enemy | None |
| Escape the Horde | N/A | Utility | Self | None |
| Spike Growth | Piercing | Control/Damage | Area | Long |
| Conjure Barrage | Physical | Damage | Area | Long |

---

## Statistics

### By Damage Type
| Type | Count |
|------|-------|
| N/A (Non-damaging) | 37 |
| Physical/Piercing | 15 |
| Psychic | 2 |
| Thunder | 2 |
| Fire | 1 |
| Acid | 1 |
| Poison | 1 |
| Radiant | 1 |

### By Role (Primary)
| Role | Count |
|------|-------|
| Damage | 17 |
| Control | 13 |
| Utility | 12 |
| Buff | 8 |
| Healing | 6 |
| Debuff | 3 |
| Summon | 1 |

### By Target Type
| Target | Count |
|--------|-------|
| Single Enemy | 21 |
| Self | 15 |
| Area | 14 |
| All Allies | 6 |
| Single Ally | 4 |
| All Enemies | 1 |

### By Cooldown
| Cooldown | Count |
|----------|-------|
| Long (1/rest) | 20 |
| Medium (3 rounds) | 20 |
| Short (1 round) | 10 |
| None (at-will) | 10 |

---

## Class Balance Analysis

### Power Distribution by Class
| Class | Damage | Control | Utility | Support | Healing |
|-------|--------|---------|---------|---------|---------|
| Spellcaster | 1 | 4 | 2 | 2 | 1 |
| Martial | 4 | 1 | 2 | 2 | 1 |
| Scoundrel | 4 | 1 | 4 | 1 | 0 |
| Bard | 2 | 2 | 1 | 3 | 2 |
| Alchemist | 3 | 4 | 0 | 1 | 2 |
| Ranger | 5 | 2 | 2 | 1 | 0 |

### Playstyle Categories
| Class | Aggressive | Defensive | Support | Complexity |
|-------|------------|-----------|---------|------------|
| Spellcaster | Low | Medium | High | Advanced |
| Martial | High | High | Low | Beginner |
| Scoundrel | High | Medium | Low | Intermediate |
| Bard | Low | Low | High | Intermediate |
| Alchemist | Medium | Medium | Medium | Advanced |
| Ranger | High | Low | Medium | Beginner |

### Synergy Potential
| Class | Solo | Party Buffer | Party Debuffer | Combo Enabler |
|-------|------|--------------|----------------|---------------|
| Spellcaster | Medium | Medium | High | High |
| Martial | High | Low | Medium | Low |
| Scoundrel | High | Low | Medium | Medium |
| Bard | Low | High | High | High |
| Alchemist | Medium | Medium | Medium | Medium |
| Ranger | High | Medium | Low | Low |

---

## Ability Type Distribution per Class

| Class | Spell | Combat | Utility | Passive |
|-------|-------|--------|---------|---------|
| Spellcaster | 10 | 0 | 0 | 0 |
| Martial | 0 | 6 | 3 | 1 |
| Scoundrel | 0 | 4 | 5 | 1 |
| Bard | 10 | 0 | 0 | 0 |
| Alchemist | 10 | 0 | 0 | 0 |
| Ranger | 2 | 4 | 0 | 4 |

---

## Cooldown Economy per Class

| Class | None | Short | Medium | Long |
|-------|------|-------|--------|------|
| Spellcaster | 0 | 0 | 4 | 6 |
| Martial | 2 | 2 | 3 | 3 |
| Scoundrel | 2 | 4 | 2 | 2 |
| Bard | 1 | 3 | 4 | 2 |
| Alchemist | 0 | 3 | 5 | 2 |
| Ranger | 5 | 0 | 3 | 2 |

**Notes:**
- Ranger has most at-will abilities (5) - sustained damage dealer
- Spellcaster has most long cooldowns (6) - high-impact bursts
- Scoundrel has most short cooldowns (4) - tactical flexibility
