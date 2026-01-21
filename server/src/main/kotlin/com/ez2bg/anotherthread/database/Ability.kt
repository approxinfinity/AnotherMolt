package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Serializable
data class Ability(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val classId: String? = null,
    val abilityType: String,    // "spell", "combat", "utility", "passive"
    val targetType: String,     // "self", "single_enemy", "single_ally", "area", "all_enemies", "all_allies"
    val range: Int,             // in feet, 0 for self/melee
    val cooldownType: String,   // "none", "short", "medium", "long"
    val cooldownRounds: Int = 0,
    val effects: String = "[]", // JSON array of effect objects
    val imageUrl: String? = null,
    // Power budget fields
    val baseDamage: Int = 0,
    val durationRounds: Int = 0,
    val powerCost: Int = 10     // Calculated total, default to average
) {
    /**
     * Calculate the power cost based on ability attributes.
     * Budget breakdown:
     * - Damage: baseDamage / 5
     * - Range: 0=0, 5=1, 30=2, 60=3, 120=4, 120+=5
     * - Target: self=0, single=1, area=3, all=5
     * - Cooldown: none=+5, short=+2, medium=0, long=-2
     * - Duration: instant=0, 1-2 rounds=+2, 3+ rounds=+4
     * - Effects: parsed from JSON, each effect type adds cost
     */
    fun calculatePowerCost(): Int {
        var cost = 0

        // Damage cost
        cost += baseDamage / 5

        // Range cost
        cost += when {
            range <= 0 -> 0
            range <= 5 -> 1
            range <= 30 -> 2
            range <= 60 -> 3
            range <= 120 -> 4
            else -> 5
        }

        // Target type cost
        cost += when (targetType) {
            "self" -> 0
            "single_enemy", "single_ally" -> 1
            "area" -> 3
            "all_enemies", "all_allies" -> 5
            else -> 1
        }

        // Cooldown modifier (can reduce cost)
        cost += when (cooldownType) {
            "none" -> 5
            "short" -> 2
            "medium" -> 0
            "long" -> -2
            else -> 0
        }

        // Duration cost
        cost += when {
            durationRounds <= 0 -> 0
            durationRounds <= 2 -> 2
            else -> 4
        }

        // Effect costs from JSON (simplified parsing)
        if (effects.contains("heal")) cost += 3
        if (effects.contains("stun")) cost += 4
        if (effects.contains("immobilize") || effects.contains("root")) cost += 5
        if (effects.contains("buff")) cost += 2
        if (effects.contains("debuff")) cost += 3

        return cost.coerceAtLeast(1) // Minimum 1 power cost
    }

    /** Create a copy with recalculated power cost */
    fun withCalculatedCost(): Ability = copy(powerCost = calculatePowerCost())
}

object AbilityRepository {
    private fun ResultRow.toAbility(): Ability = Ability(
        id = this[AbilityTable.id],
        name = this[AbilityTable.name],
        description = this[AbilityTable.description],
        classId = this[AbilityTable.classId],
        abilityType = this[AbilityTable.abilityType],
        targetType = this[AbilityTable.targetType],
        range = this[AbilityTable.range],
        cooldownType = this[AbilityTable.cooldownType],
        cooldownRounds = this[AbilityTable.cooldownRounds],
        effects = this[AbilityTable.effects],
        imageUrl = this[AbilityTable.imageUrl],
        baseDamage = this[AbilityTable.baseDamage],
        durationRounds = this[AbilityTable.durationRounds],
        powerCost = this[AbilityTable.powerCost]
    )

    fun create(ability: Ability): Ability = transaction {
        val abilityWithCost = ability.withCalculatedCost()
        AbilityTable.insert {
            it[id] = abilityWithCost.id
            it[name] = abilityWithCost.name
            it[description] = abilityWithCost.description
            it[classId] = abilityWithCost.classId
            it[abilityType] = abilityWithCost.abilityType
            it[targetType] = abilityWithCost.targetType
            it[range] = abilityWithCost.range
            it[cooldownType] = abilityWithCost.cooldownType
            it[cooldownRounds] = abilityWithCost.cooldownRounds
            it[effects] = abilityWithCost.effects
            it[imageUrl] = abilityWithCost.imageUrl
            it[baseDamage] = abilityWithCost.baseDamage
            it[durationRounds] = abilityWithCost.durationRounds
            it[powerCost] = abilityWithCost.powerCost
        }
        abilityWithCost
    }

    fun findAll(): List<Ability> = transaction {
        AbilityTable.selectAll().map { it.toAbility() }
    }

    fun findById(id: String): Ability? = transaction {
        AbilityTable.selectAll()
            .where { AbilityTable.id eq id }
            .map { it.toAbility() }
            .singleOrNull()
    }

    fun findByClassId(classId: String): List<Ability> = transaction {
        AbilityTable.selectAll()
            .where { AbilityTable.classId eq classId }
            .map { it.toAbility() }
    }

    fun findByType(abilityType: String): List<Ability> = transaction {
        AbilityTable.selectAll()
            .where { AbilityTable.abilityType eq abilityType }
            .map { it.toAbility() }
    }

    fun findSpells(): List<Ability> = findByType("spell")

    fun findCombatAbilities(): List<Ability> = findByType("combat")

    fun update(ability: Ability): Boolean = transaction {
        val abilityWithCost = ability.withCalculatedCost()
        AbilityTable.update({ AbilityTable.id eq ability.id }) {
            it[name] = abilityWithCost.name
            it[description] = abilityWithCost.description
            it[classId] = abilityWithCost.classId
            it[abilityType] = abilityWithCost.abilityType
            it[targetType] = abilityWithCost.targetType
            it[range] = abilityWithCost.range
            it[cooldownType] = abilityWithCost.cooldownType
            it[cooldownRounds] = abilityWithCost.cooldownRounds
            it[effects] = abilityWithCost.effects
            it[imageUrl] = abilityWithCost.imageUrl
            it[baseDamage] = abilityWithCost.baseDamage
            it[durationRounds] = abilityWithCost.durationRounds
            it[powerCost] = abilityWithCost.powerCost
        } > 0
    }

    fun updateImageUrl(id: String, imageUrl: String): Boolean = transaction {
        AbilityTable.update({ AbilityTable.id eq id }) {
            it[AbilityTable.imageUrl] = imageUrl
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        AbilityTable.deleteWhere { AbilityTable.id eq id } > 0
    }
}
