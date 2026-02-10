package com.ez2bg.anotherthread.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * A lever that can be pulled as part of a puzzle.
 */
@Serializable
data class Lever(
    val id: String,
    val name: String,
    val description: String,
    val pulledDescription: String = "The lever has been pulled down."
)

/**
 * A secret passage that can be revealed when puzzle conditions are met.
 */
@Serializable
data class SecretPassage(
    val id: String,
    val name: String,
    val description: String,
    val targetLocationId: String,
    val direction: ExitDirection = ExitDirection.ENTER,
    val searchHint: String? = null  // Hint shown when player searches the room
)

/**
 * Puzzle type determines the solving mechanics.
 */
enum class PuzzleType {
    LEVER_SEQUENCE,      // Pull levers in specific order
    LEVER_COMBINATION,   // Pull specific levers (any order)
    SEARCH_AND_ENTER,    // Search to find hidden passage
    BUTTON_PRESS         // Press buttons in sequence
}

/**
 * A puzzle that can unlock secret passages or grant rewards.
 */
@Serializable
data class Puzzle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val locationId: String,  // Where this puzzle is located
    val puzzleType: PuzzleType = PuzzleType.LEVER_COMBINATION,
    val levers: List<Lever> = emptyList(),
    val requiredSequence: List<String> = emptyList(),  // Lever IDs in required order (for LEVER_SEQUENCE)
    val requiredLevers: List<String> = emptyList(),    // Lever IDs that must be pulled (for LEVER_COMBINATION)
    val secretPassages: List<SecretPassage> = emptyList(),
    val solvedMessage: String = "You hear a click as something unlocks!",
    val failureMessage: String = "Nothing happens.",
    val resetOnFailure: Boolean = false,  // Reset all levers if wrong sequence
    val isRepeatable: Boolean = false,    // Can be solved again after reset
    val goldReward: Int = 0,
    val itemRewards: List<String> = emptyList()  // Item IDs to grant
)

/**
 * Tracks a player's progress on a specific puzzle.
 */
@Serializable
data class PuzzleProgress(
    val leversPulled: List<String> = emptyList(),  // IDs of levers pulled, in order
    val solved: Boolean = false,
    val solvedAt: Long? = null,
    val passagesRevealed: List<String> = emptyList()  // IDs of secret passages now visible
)

object PuzzleRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ResultRow.toPuzzle(): Puzzle {
        val puzzleJson = this[PuzzleTable.puzzleData]
        return json.decodeFromString<Puzzle>(puzzleJson)
    }

    fun create(puzzle: Puzzle): Puzzle = transaction {
        PuzzleTable.insert {
            it[id] = puzzle.id
            it[locationId] = puzzle.locationId
            it[puzzleData] = json.encodeToString(puzzle)
        }
        puzzle
    }

    fun findAll(): List<Puzzle> = transaction {
        PuzzleTable.selectAll().map { it.toPuzzle() }
    }

    fun findById(id: String): Puzzle? = transaction {
        PuzzleTable.selectAll()
            .where { PuzzleTable.id eq id }
            .map { it.toPuzzle() }
            .singleOrNull()
    }

    fun findByLocationId(locationId: String): List<Puzzle> = transaction {
        PuzzleTable.selectAll()
            .where { PuzzleTable.locationId eq locationId }
            .map { it.toPuzzle() }
    }

    fun update(puzzle: Puzzle): Boolean = transaction {
        PuzzleTable.update({ PuzzleTable.id eq puzzle.id }) {
            it[locationId] = puzzle.locationId
            it[puzzleData] = json.encodeToString(puzzle)
        } > 0
    }

    fun delete(id: String): Boolean = transaction {
        PuzzleTable.deleteWhere { PuzzleTable.id eq id } > 0
    }

    // ========================================================================
    // Puzzle Progress (uses FeatureState under the hood)
    // ========================================================================

    private fun progressKey(puzzleId: String) = "puzzle_progress_$puzzleId"

    fun getProgress(userId: String, puzzleId: String): PuzzleProgress {
        val state = FeatureStateRepository.getState(userId, progressKey(puzzleId))
        return if (state != null) {
            try {
                json.decodeFromString<PuzzleProgress>(state.value)
            } catch (e: Exception) {
                PuzzleProgress()
            }
        } else {
            PuzzleProgress()
        }
    }

    fun saveProgress(userId: String, puzzleId: String, progress: PuzzleProgress): Boolean {
        return FeatureStateRepository.setState(
            userId,
            progressKey(puzzleId),
            json.encodeToString(progress)
        )
    }

    fun resetProgress(userId: String, puzzleId: String): Boolean {
        return saveProgress(userId, puzzleId, PuzzleProgress())
    }
}
