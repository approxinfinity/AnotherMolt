package com.ez2bg.anotherthread.routes

import com.ez2bg.anotherthread.database.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("PuzzleRoutes")

fun Route.puzzleRoutes() {
    route("/puzzles") {
        // Get all puzzles (admin)
        get {
            call.respond(PuzzleRepository.findAll())
        }

        // Get puzzle by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val puzzle = PuzzleRepository.findById(id)
            if (puzzle != null) {
                call.respond(puzzle)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Create puzzle (admin)
        post {
            val puzzle = call.receive<Puzzle>()
            val created = PuzzleRepository.create(puzzle)
            call.respond(HttpStatusCode.Created, created)
        }

        // Update puzzle (admin)
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val puzzle = call.receive<Puzzle>()
            if (puzzle.id != id) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID mismatch"))
                return@put
            }
            val success = PuzzleRepository.update(puzzle)
            if (success) {
                call.respond(HttpStatusCode.OK, puzzle)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Delete puzzle (admin)
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val success = PuzzleRepository.delete(id)
            if (success) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Get puzzles at a location
        get("/at-location/{locationId}") {
            val locationId = call.parameters["locationId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val puzzles = PuzzleRepository.findByLocationId(locationId)
            call.respond(puzzles)
        }

        // Get player's progress on a puzzle
        get("/{id}/progress") {
            val puzzleId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val puzzle = PuzzleRepository.findById(puzzleId)
            if (puzzle == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Puzzle not found"))
                return@get
            }

            val progress = PuzzleRepository.getProgress(userId, puzzleId)
            call.respond(PuzzleProgressResponse(
                puzzleId = puzzleId,
                progress = progress,
                levers = puzzle.levers.map { lever ->
                    LeverState(
                        id = lever.id,
                        name = lever.name,
                        description = if (lever.id in progress.leversPulled) lever.pulledDescription else lever.description,
                        isPulled = lever.id in progress.leversPulled
                    )
                },
                isSolved = progress.solved,
                revealedPassages = if (progress.solved) puzzle.secretPassages else emptyList()
            ))
        }

        // Pull a lever
        post("/{id}/pull-lever") {
            val puzzleId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            @Serializable
            data class PullLeverRequest(val leverId: String)

            val request = call.receive<PullLeverRequest>()

            val puzzle = PuzzleRepository.findById(puzzleId)
            if (puzzle == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Puzzle not found"))
                return@post
            }

            // Verify lever exists
            val lever = puzzle.levers.find { it.id == request.leverId }
            if (lever == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Lever not found in this puzzle"))
                return@post
            }

            // Get current progress
            var progress = PuzzleRepository.getProgress(userId, puzzleId)

            // Check if already solved and not repeatable
            if (progress.solved && !puzzle.isRepeatable) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Puzzle already solved"))
                return@post
            }

            // Reset if already solved and repeatable
            if (progress.solved && puzzle.isRepeatable) {
                progress = PuzzleProgress()
            }

            // Check if this lever is already pulled
            if (request.leverId in progress.leversPulled) {
                call.respond(HttpStatusCode.OK, PullLeverResponse(
                    success = true,
                    message = "The lever is already pulled.",
                    leverState = LeverState(
                        id = lever.id,
                        name = lever.name,
                        description = lever.pulledDescription,
                        isPulled = true
                    ),
                    puzzleSolved = false
                ))
                return@post
            }

            // Pull the lever
            val newLeversPulled = progress.leversPulled + request.leverId
            var puzzleSolved = false
            var message = "You pull the ${lever.name}. ${lever.pulledDescription}"
            var passagesRevealed = progress.passagesRevealed

            // Check if puzzle is now solved based on type
            when (puzzle.puzzleType) {
                PuzzleType.LEVER_SEQUENCE -> {
                    // Check if the sequence matches so far
                    val expectedSoFar = puzzle.requiredSequence.take(newLeversPulled.size)
                    if (newLeversPulled == expectedSoFar) {
                        // Correct so far
                        if (newLeversPulled.size == puzzle.requiredSequence.size) {
                            // Solved!
                            puzzleSolved = true
                            message = "$message\n\n${puzzle.solvedMessage}"
                            passagesRevealed = puzzle.secretPassages.map { it.id }
                        }
                    } else {
                        // Wrong sequence
                        message = "$message\n\n${puzzle.failureMessage}"
                        if (puzzle.resetOnFailure) {
                            // Reset progress
                            PuzzleRepository.resetProgress(userId, puzzleId)
                            call.respond(HttpStatusCode.OK, PullLeverResponse(
                                success = true,
                                message = message + "\n\nThe levers reset to their original positions.",
                                leverState = LeverState(
                                    id = lever.id,
                                    name = lever.name,
                                    description = lever.description,
                                    isPulled = false
                                ),
                                puzzleSolved = false,
                                puzzleReset = true
                            ))
                            return@post
                        }
                    }
                }

                PuzzleType.LEVER_COMBINATION -> {
                    // Check if all required levers are pulled (order doesn't matter)
                    if (puzzle.requiredLevers.all { it in newLeversPulled }) {
                        puzzleSolved = true
                        message = "$message\n\n${puzzle.solvedMessage}"
                        passagesRevealed = puzzle.secretPassages.map { it.id }
                    }
                }

                PuzzleType.SEARCH_AND_ENTER, PuzzleType.BUTTON_PRESS -> {
                    // Handle other puzzle types if needed
                }
            }

            // Save progress
            val newProgress = progress.copy(
                leversPulled = newLeversPulled,
                solved = puzzleSolved,
                solvedAt = if (puzzleSolved) System.currentTimeMillis() else null,
                passagesRevealed = passagesRevealed
            )
            PuzzleRepository.saveProgress(userId, puzzleId, newProgress)

            // Award rewards if solved
            if (puzzleSolved) {
                if (puzzle.goldReward > 0) {
                    UserRepository.addGold(userId, puzzle.goldReward)
                    message = "$message\nYou found ${puzzle.goldReward} gold!"
                }
                if (puzzle.itemRewards.isNotEmpty()) {
                    UserRepository.addItems(userId, puzzle.itemRewards)
                    val itemNames = puzzle.itemRewards.mapNotNull { ItemRepository.findById(it)?.name }
                    message = "$message\nYou found: ${itemNames.joinToString(", ")}"
                }
            }

            log.info("User $userId pulled lever ${lever.name} in puzzle $puzzleId. Solved: $puzzleSolved")

            call.respond(HttpStatusCode.OK, PullLeverResponse(
                success = true,
                message = message,
                leverState = LeverState(
                    id = lever.id,
                    name = lever.name,
                    description = lever.pulledDescription,
                    isPulled = true
                ),
                puzzleSolved = puzzleSolved,
                revealedPassages = if (puzzleSolved) puzzle.secretPassages else null
            ))
        }

        // Search for secret passages (for SEARCH_AND_ENTER type)
        post("/{id}/search") {
            val puzzleId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val puzzle = PuzzleRepository.findById(puzzleId)
            if (puzzle == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Puzzle not found"))
                return@post
            }

            if (puzzle.puzzleType != PuzzleType.SEARCH_AND_ENTER) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "This puzzle doesn't support searching"))
                return@post
            }

            var progress = PuzzleRepository.getProgress(userId, puzzleId)

            if (progress.solved && !puzzle.isRepeatable) {
                call.respond(HttpStatusCode.OK, PuzzleSearchResponse(
                    found = true,
                    message = "You've already found the secret passage here.",
                    passages = puzzle.secretPassages
                ))
                return@post
            }

            // Mark as solved - searching reveals the passage
            val newProgress = progress.copy(
                solved = true,
                solvedAt = System.currentTimeMillis(),
                passagesRevealed = puzzle.secretPassages.map { it.id }
            )
            PuzzleRepository.saveProgress(userId, puzzleId, newProgress)

            val hintMessage = puzzle.secretPassages.firstOrNull()?.searchHint
                ?: "You discover a hidden passage!"

            call.respond(HttpStatusCode.OK, PuzzleSearchResponse(
                found = true,
                message = hintMessage,
                passages = puzzle.secretPassages
            ))
        }

        // Reset puzzle progress (admin or for repeatable puzzles)
        post("/{id}/reset") {
            val puzzleId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id") ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val puzzle = PuzzleRepository.findById(puzzleId)
            if (puzzle == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Puzzle not found"))
                return@post
            }

            if (!puzzle.isRepeatable) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "This puzzle cannot be reset"))
                return@post
            }

            PuzzleRepository.resetProgress(userId, puzzleId)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Puzzle reset successfully"))
        }
    }
}

// Response DTOs
@Serializable
data class LeverState(
    val id: String,
    val name: String,
    val description: String,
    val isPulled: Boolean
)

@Serializable
data class PuzzleProgressResponse(
    val puzzleId: String,
    val progress: PuzzleProgress,
    val levers: List<LeverState>,
    val isSolved: Boolean,
    val revealedPassages: List<SecretPassage>
)

@Serializable
data class PullLeverResponse(
    val success: Boolean,
    val message: String,
    val leverState: LeverState,
    val puzzleSolved: Boolean,
    val puzzleReset: Boolean = false,
    val revealedPassages: List<SecretPassage>? = null
)

@Serializable
data class PuzzleSearchResponse(
    val found: Boolean,
    val message: String,
    val passages: List<SecretPassage>
)
