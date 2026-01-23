package com.ez2bg.anotherthread.combat

import com.ez2bg.anotherthread.AppConfig
import com.ez2bg.anotherthread.api.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

/**
 * Connection state for the combat WebSocket.
 */
enum class CombatConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

/**
 * Events emitted by the combat client.
 */
sealed class CombatEvent {
    data class ConnectionStateChanged(val state: CombatConnectionState) : CombatEvent()
    data class CombatStarted(val session: CombatSessionDto, val yourCombatant: CombatantDto) : CombatEvent()
    data class RoundStarted(val sessionId: String, val roundNumber: Int, val durationMs: Long, val combatants: List<CombatantDto>) : CombatEvent()
    data class HealthUpdated(val update: HealthUpdateResponse) : CombatEvent()
    data class AbilityResolved(val result: AbilityResolvedResponse) : CombatEvent()
    data class StatusEffectChanged(val effect: StatusEffectResponse) : CombatEvent()
    data class RoundEnded(val sessionId: String, val roundNumber: Int, val combatants: List<CombatantDto>, val logEntries: List<CombatLogEntryDto>) : CombatEvent()
    data class CombatEnded(val response: CombatEndedResponse) : CombatEvent()
    data class FleeResult(val response: FleeResultResponse) : CombatEvent()
    data class AbilityQueued(val abilityId: String, val targetId: String?) : CombatEvent()
    data class CreatureMoved(val creatureId: String, val creatureName: String, val fromLocationId: String, val toLocationId: String) : CombatEvent()
    data class Error(val message: String, val code: String?) : CombatEvent()
}

/**
 * Client for managing WebSocket combat connections with automatic reconnection.
 *
 * Handles:
 * - Connection establishment and authentication
 * - Automatic reconnection with exponential backoff
 * - Session recovery after reconnect
 * - Message parsing and event dispatch
 *
 * Usage:
 * ```
 * val client = CombatClient(userId)
 *
 * // Observe events
 * client.events.collect { event ->
 *     when (event) {
 *         is CombatEvent.CombatStarted -> { ... }
 *         is CombatEvent.RoundEnded -> { ... }
 *         ...
 *     }
 * }
 *
 * // Connect and join combat
 * client.connect()
 * client.joinCombat()
 *
 * // Use abilities
 * client.useAbility(abilityId, targetId)
 *
 * // Disconnect when done
 * client.disconnect()
 * ```
 */
class CombatClient(
    private val userId: String,
    private val httpClient: HttpClient = createWebSocketClient()
) {
    companion object {
        private const val TAG = "CombatClient"

        // Reconnection settings
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
        private const val RECONNECT_BACKOFF_MULTIPLIER = 2.0
        private const val MAX_RECONNECT_ATTEMPTS = 10

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

        private fun createWebSocketClient(): HttpClient {
            return HttpClient {
                install(WebSockets)
            }
        }
    }

    // Connection state
    private val _connectionState = MutableStateFlow(CombatConnectionState.DISCONNECTED)
    val connectionState: StateFlow<CombatConnectionState> = _connectionState.asStateFlow()

    // Event stream
    private val _events = MutableSharedFlow<CombatEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<CombatEvent> = _events.asSharedFlow()

    // Current session info (for reconnection)
    private var currentSessionId: String? = null
    private var lastKnownCombatants: List<CombatantDto> = emptyList()

    // WebSocket session
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var reconnectAttempts = 0
    private var shouldReconnect = false

    // Coroutine scope for the client
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Connect to the combat WebSocket server.
     */
    fun connect() {
        if (_connectionState.value == CombatConnectionState.CONNECTED ||
            _connectionState.value == CombatConnectionState.CONNECTING) {
            return
        }

        shouldReconnect = true
        reconnectAttempts = 0
        startConnection()
    }

    /**
     * Disconnect from the combat server.
     */
    fun disconnect() {
        shouldReconnect = false
        connectionJob?.cancel()
        connectionJob = null

        scope.launch {
            try {
                webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "User disconnect"))
            } catch (e: Exception) {
                // Ignore close errors
            }
            webSocketSession = null
            currentSessionId = null
            lastKnownCombatants = emptyList()
            _connectionState.value = CombatConnectionState.DISCONNECTED
            _events.emit(CombatEvent.ConnectionStateChanged(CombatConnectionState.DISCONNECTED))
        }
    }

    /**
     * Join or create a combat session at the user's current location.
     */
    suspend fun joinCombat(targetCreatureIds: List<String> = emptyList()) {
        val session = webSocketSession ?: return
        val request = JoinCombatRequest(
            userId = userId,
            targetCreatureIds = targetCreatureIds
        )
        val message = json.encodeToString(JoinCombatRequest.serializer(), request)
        session.send(Frame.Text(message))
    }

    /**
     * Queue an ability to use this round.
     */
    suspend fun useAbility(abilityId: String, targetId: String? = null) {
        val session = webSocketSession ?: return
        val sessionId = currentSessionId ?: return

        val request = UseAbilityRequest(
            userId = userId,
            sessionId = sessionId,
            abilityId = abilityId,
            targetId = targetId
        )
        val message = json.encodeToString(UseAbilityRequest.serializer(), request)
        session.send(Frame.Text(message))
    }

    /**
     * Attempt to flee from combat.
     */
    suspend fun flee() {
        val session = webSocketSession ?: return
        val sessionId = currentSessionId ?: return

        val request = FleeCombatRequest(
            userId = userId,
            sessionId = sessionId
        )
        val message = json.encodeToString(FleeCombatRequest.serializer(), request)
        session.send(Frame.Text(message))
    }

    /**
     * Leave combat (disconnect from session without fleeing).
     */
    suspend fun leaveCombat() {
        val session = webSocketSession ?: return
        val sessionId = currentSessionId ?: return

        val request = LeaveCombatRequest(
            userId = userId,
            sessionId = sessionId
        )
        val message = json.encodeToString(LeaveCombatRequest.serializer(), request)
        session.send(Frame.Text(message))

        currentSessionId = null
        lastKnownCombatants = emptyList()
    }

    private fun startConnection() {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            val isReconnect = reconnectAttempts > 0
            _connectionState.value = if (isReconnect) CombatConnectionState.RECONNECTING else CombatConnectionState.CONNECTING
            _events.emit(CombatEvent.ConnectionStateChanged(_connectionState.value))

            try {
                val baseUrl = AppConfig.api.baseUrl
                val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
                val fullUrl = "$wsUrl/combat?userId=$userId"

                println("$TAG: Connecting to $fullUrl (attempt ${reconnectAttempts + 1})")

                httpClient.webSocket(fullUrl) {
                    webSocketSession = this
                    _connectionState.value = CombatConnectionState.CONNECTED
                    _events.emit(CombatEvent.ConnectionStateChanged(CombatConnectionState.CONNECTED))
                    reconnectAttempts = 0

                    println("$TAG: Connected successfully")

                    // If we were in a session before, try to rejoin
                    if (isReconnect && currentSessionId != null) {
                        println("$TAG: Attempting to rejoin session $currentSessionId")
                        joinCombat()
                    }

                    // Listen for incoming messages
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    handleMessage(text)
                                }
                                is Frame.Close -> {
                                    println("$TAG: Received close frame")
                                    break
                                }
                                else -> { /* Ignore other frame types */ }
                            }
                        }
                    } catch (e: Exception) {
                        println("$TAG: Error receiving messages: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("$TAG: Connection failed: ${e.message}")
                webSocketSession = null
            }

            // Connection closed or failed - attempt reconnect if appropriate
            webSocketSession = null

            if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                val delay = calculateReconnectDelay()
                println("$TAG: Will reconnect in ${delay}ms (attempt ${reconnectAttempts + 1})")

                _connectionState.value = CombatConnectionState.RECONNECTING
                _events.emit(CombatEvent.ConnectionStateChanged(CombatConnectionState.RECONNECTING))

                delay(delay)
                reconnectAttempts++
                startConnection()
            } else if (shouldReconnect) {
                println("$TAG: Max reconnect attempts reached")
                _connectionState.value = CombatConnectionState.DISCONNECTED
                _events.emit(CombatEvent.ConnectionStateChanged(CombatConnectionState.DISCONNECTED))
                _events.emit(CombatEvent.Error("Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts", "MAX_RECONNECT"))
            } else {
                _connectionState.value = CombatConnectionState.DISCONNECTED
                _events.emit(CombatEvent.ConnectionStateChanged(CombatConnectionState.DISCONNECTED))
            }
        }
    }

    private fun calculateReconnectDelay(): Long {
        var delay = INITIAL_RECONNECT_DELAY_MS
        repeat(reconnectAttempts) {
            delay = (delay * RECONNECT_BACKOFF_MULTIPLIER).toLong()
        }
        return minOf(delay, MAX_RECONNECT_DELAY_MS)
    }

    private suspend fun handleMessage(text: String) {
        try {
            // Parse based on the "type" field in the message
            when {
                text.contains("CombatStartedMessage") || text.contains("\"session\"") && text.contains("\"yourCombatant\"") -> {
                    val response = json.decodeFromString<CombatStartedResponse>(extractPayload(text))
                    currentSessionId = response.session.id
                    lastKnownCombatants = response.session.combatants
                    _events.emit(CombatEvent.CombatStarted(response.session, response.yourCombatant))
                }

                text.contains("RoundStartMessage") || text.contains("\"roundDurationMs\"") -> {
                    val response = json.decodeFromString<RoundStartResponse>(extractPayload(text))
                    lastKnownCombatants = response.combatants
                    _events.emit(CombatEvent.RoundStarted(response.sessionId, response.roundNumber, response.roundDurationMs, response.combatants))
                }

                text.contains("HealthUpdateMessage") || text.contains("\"changeAmount\"") -> {
                    val response = json.decodeFromString<HealthUpdateResponse>(extractPayload(text))
                    _events.emit(CombatEvent.HealthUpdated(response))
                }

                text.contains("AbilityResolvedMessage") -> {
                    val response = json.decodeFromString<AbilityResolvedResponse>(extractPayload(text))
                    _events.emit(CombatEvent.AbilityResolved(response))
                }

                text.contains("StatusEffectMessage") -> {
                    val response = json.decodeFromString<StatusEffectResponse>(extractPayload(text))
                    _events.emit(CombatEvent.StatusEffectChanged(response))
                }

                text.contains("RoundEndMessage") || text.contains("\"logEntries\"") -> {
                    val response = json.decodeFromString<RoundEndResponse>(extractPayload(text))
                    lastKnownCombatants = response.combatants
                    _events.emit(CombatEvent.RoundEnded(response.sessionId, response.roundNumber, response.combatants, response.logEntries))
                }

                text.contains("CombatEndedMessage") || text.contains("\"victors\"") -> {
                    val response = json.decodeFromString<CombatEndedResponse>(extractPayload(text))
                    currentSessionId = null
                    lastKnownCombatants = emptyList()
                    _events.emit(CombatEvent.CombatEnded(response))
                }

                text.contains("FleeResultMessage") -> {
                    val response = json.decodeFromString<FleeResultResponse>(extractPayload(text))
                    if (response.success) {
                        currentSessionId = null
                        lastKnownCombatants = emptyList()
                    }
                    _events.emit(CombatEvent.FleeResult(response))
                }

                text.contains("AbilityQueuedMessage") -> {
                    val response = json.decodeFromString<AbilityQueuedResponse>(extractPayload(text))
                    _events.emit(CombatEvent.AbilityQueued(response.abilityId, response.targetId))
                }

                text.contains("CombatErrorMessage") || text.contains("\"error\"") && text.contains("\"code\"") -> {
                    val response = json.decodeFromString<CombatErrorResponse>(extractPayload(text))
                    _events.emit(CombatEvent.Error(response.error, response.code))
                }

                text.contains("CreatureMovedMessage") || (text.contains("\"creatureId\"") && text.contains("\"fromLocationId\"")) -> {
                    val response = json.decodeFromString<CreatureMovedResponse>(extractPayload(text))
                    _events.emit(CombatEvent.CreatureMoved(response.creatureId, response.creatureName, response.fromLocationId, response.toLocationId))
                }

                else -> {
                    println("$TAG: Unknown message type: ${text.take(100)}")
                }
            }
        } catch (e: Exception) {
            println("$TAG: Error parsing message: ${e.message}")
            println("$TAG: Message was: ${text.take(200)}")
        }
    }

    /**
     * Extract the payload from a typed message.
     * The server sends messages with a "type" wrapper, e.g.:
     * {"type":"com.ez2bg.anotherthread.combat.CombatStartedMessage","session":{...},"yourCombatant":{...}}
     *
     * We need to extract just the payload part for deserialization.
     */
    private fun extractPayload(text: String): String {
        // If the message has a "type" field with a class name, we can still parse it
        // because kotlinx.serialization will ignore unknown keys
        return text
    }

    /**
     * Clean up resources.
     */
    fun dispose() {
        disconnect()
        scope.cancel()
    }
}
