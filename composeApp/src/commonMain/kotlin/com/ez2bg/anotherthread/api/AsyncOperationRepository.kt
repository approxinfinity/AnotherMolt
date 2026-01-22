package com.ez2bg.anotherthread.api

import com.ez2bg.anotherthread.platform.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Result of a class generation operation
 */
data class ClassGenerationResult(
    val userId: String,
    val success: Boolean,
    val characterClass: CharacterClassDto? = null,
    val user: UserDto? = null,
    val errorMessage: String? = null
)

/**
 * Status of an ongoing class generation operation
 */
data class ClassGenerationStatus(
    val userId: String,
    val startedAt: Long,
    val message: String = "Generating class..."
)

/**
 * Singleton repository for managing async server operations.
 *
 * This repository survives ViewModel/composable lifecycle changes and provides
 * Flows that UI components can collect to observe operation status.
 *
 * Key features:
 * - Survives navigation and configuration changes
 * - Provides StateFlows for operation status
 * - Provides SharedFlows for one-time completion events
 * - Handles polling for async operations that don't have websocket support
 *
 * Usage:
 * ```kotlin
 * // Start class generation
 * AsyncOperationRepository.startClassGeneration(userId, description, generateNew = true)
 *
 * // Observe status in composable
 * val generatingUsers by AsyncOperationRepository.classGenerationStatus.collectAsState()
 *
 * // Collect completion events
 * LaunchedEffect(Unit) {
 *     AsyncOperationRepository.classGenerationCompletions.collect { result ->
 *         // Handle completion
 *     }
 * }
 * ```
 */
object AsyncOperationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Class generation timeout (10 minutes)
    private const val CLASS_GENERATION_TIMEOUT_MS = 10 * 60 * 1000L
    private const val POLL_INTERVAL_MS = 5000L

    // Track users currently having their class generated
    private val _classGenerationStatus = MutableStateFlow<Map<String, ClassGenerationStatus>>(emptyMap())
    val classGenerationStatus = _classGenerationStatus.asStateFlow()

    // Emit completed class generations
    private val _classGenerationCompletions = MutableSharedFlow<ClassGenerationResult>(extraBufferCapacity = 10)
    val classGenerationCompletions = _classGenerationCompletions.asSharedFlow()

    // Track errors by user ID
    private val _classGenerationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val classGenerationErrors = _classGenerationErrors.asStateFlow()

    // Track active polling jobs by user ID
    private val pollingJobs = mutableMapOf<String, Job>()

    /**
     * Check if class generation is in progress for a user
     */
    fun isClassGenerating(userId: String): Boolean = userId in _classGenerationStatus.value

    /**
     * Get error message for a user's class generation, if any
     */
    fun getClassGenerationError(userId: String): String? = _classGenerationErrors.value[userId]

    /**
     * Clear error for a user's class generation
     */
    fun clearClassGenerationError(userId: String) {
        _classGenerationErrors.value = _classGenerationErrors.value - userId
    }

    /**
     * Start class generation for a user.
     *
     * @param userId The user ID
     * @param characterDescription The character description for class matching/generation
     * @param generateNew If true, generates a completely new class; if false, matches to existing class
     */
    fun startClassGeneration(
        userId: String,
        characterDescription: String,
        generateNew: Boolean
    ) {
        // Don't start if already generating for this user
        if (isClassGenerating(userId)) return

        // Clear any previous errors
        _classGenerationErrors.value = _classGenerationErrors.value - userId

        // Mark as generating
        val startTime = currentTimeMillis()
        _classGenerationStatus.value = _classGenerationStatus.value + (userId to ClassGenerationStatus(
            userId = userId,
            startedAt = startTime,
            message = if (generateNew) "Generating new class..." else "Finding best class match..."
        ))

        // Start the request and polling job
        val job = scope.launch {
            // First, make the assign-class request
            val result = ApiClient.assignClass(
                userId = userId,
                request = AssignClassRequest(
                    generateClass = generateNew,
                    characterDescription = characterDescription
                )
            )

            result.onSuccess { response ->
                if (response.assignedClass != null) {
                    // Synchronous completion - class was assigned immediately
                    emitCompletion(
                        ClassGenerationResult(
                            userId = userId,
                            success = true,
                            characterClass = response.assignedClass,
                            user = response.user
                        )
                    )
                    removeGenerationStatus(userId)
                } else if (response.success) {
                    // Async generation started - update message and start polling
                    updateMessage(userId, response.message ?: "Class generation in progress...")
                    startPolling(userId)
                } else {
                    // Failed
                    emitFailure(userId, response.message ?: "Failed to start class generation")
                }
            }.onFailure { error ->
                emitFailure(userId, error.message ?: "Failed to start class generation")
            }
        }

        pollingJobs[userId] = job
    }

    /**
     * Resume polling for a user whose class generation was already in progress.
     * Call this when the UI loads and detects an ongoing generation from classGenerationStartedAt.
     */
    fun resumeClassGenerationPolling(userId: String, startedAt: Long) {
        // Don't start if already polling
        if (isClassGenerating(userId)) return

        val elapsed = currentTimeMillis() - startedAt
        if (elapsed >= CLASS_GENERATION_TIMEOUT_MS) {
            // Already timed out
            return
        }

        // Mark as generating
        _classGenerationStatus.value = _classGenerationStatus.value + (userId to ClassGenerationStatus(
            userId = userId,
            startedAt = startedAt,
            message = "Class generation in progress..."
        ))

        // Start polling
        startPolling(userId)
    }

    /**
     * Cancel class generation polling for a user
     */
    fun cancelClassGeneration(userId: String) {
        pollingJobs[userId]?.cancel()
        pollingJobs.remove(userId)
        removeGenerationStatus(userId)
    }

    private fun startPolling(userId: String) {
        // Cancel any existing polling job
        pollingJobs[userId]?.cancel()

        val job = scope.launch {
            val status = _classGenerationStatus.value[userId] ?: return@launch
            val startTime = status.startedAt

            while (isClassGenerating(userId)) {
                val elapsed = currentTimeMillis() - startTime

                // Check timeout
                if (elapsed >= CLASS_GENERATION_TIMEOUT_MS) {
                    emitFailure(userId, "Class generation timed out. Please try again.")
                    return@launch
                }

                // Wait before polling
                delay(POLL_INTERVAL_MS)

                // Check if still generating (might have been cancelled)
                if (!isClassGenerating(userId)) return@launch

                // Poll for status
                ApiClient.getUser(userId).onSuccess { updatedUser ->
                    if (updatedUser != null) {
                        if (updatedUser.characterClassId != null) {
                            // Class was assigned - fetch the class details
                            ApiClient.getCharacterClass(updatedUser.characterClassId).onSuccess { characterClass ->
                                emitCompletion(
                                    ClassGenerationResult(
                                        userId = userId,
                                        success = true,
                                        characterClass = characterClass,
                                        user = updatedUser
                                    )
                                )
                            }.onFailure {
                                // Class was assigned but couldn't fetch details - still report success
                                emitCompletion(
                                    ClassGenerationResult(
                                        userId = userId,
                                        success = true,
                                        characterClass = null,
                                        user = updatedUser
                                    )
                                )
                            }
                            removeGenerationStatus(userId)
                        } else if (updatedUser.classGenerationStartedAt == null) {
                            // Generation was cleared without assigning a class - failed
                            emitFailure(userId, "Class generation failed. Please try again.")
                        }
                        // Otherwise, still generating - continue polling
                    }
                }.onFailure { error ->
                    // Network error during polling - continue trying
                    updateMessage(userId, "Checking status... (${error.message})")
                }
            }
        }

        pollingJobs[userId] = job
    }

    private fun updateMessage(userId: String, message: String) {
        val current = _classGenerationStatus.value[userId] ?: return
        _classGenerationStatus.value = _classGenerationStatus.value + (userId to current.copy(message = message))
    }

    private fun removeGenerationStatus(userId: String) {
        _classGenerationStatus.value = _classGenerationStatus.value - userId
        pollingJobs[userId]?.cancel()
        pollingJobs.remove(userId)
    }

    private suspend fun emitCompletion(result: ClassGenerationResult) {
        _classGenerationCompletions.emit(result)
    }

    private suspend fun emitFailure(userId: String, message: String) {
        _classGenerationErrors.value = _classGenerationErrors.value + (userId to message)
        _classGenerationCompletions.emit(
            ClassGenerationResult(
                userId = userId,
                success = false,
                errorMessage = message
            )
        )
        removeGenerationStatus(userId)
    }
}
