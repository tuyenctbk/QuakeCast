package com.example.engagement

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

// Application-wide DataStore instance for user engagement telemetry
val Context.engagementDataStore: DataStore<Preferences> by preferencesDataStore(name = "quakecast_engagement")

enum class ActivePromptType {
    NONE,
    UPDATE,
    RATE,
    SHARE
}

/**
 * Snapshot of user engagement telemetry and active dialog status.
 */
data class UserEngagementState(
    val activePrompt: ActivePromptType = ActivePromptType.NONE,
    val installDurationDays: Int = 0,
    val manualRefreshCount: Int = 0,
    val successfulOperations: Int = 0,
    val launchCount: Int = 0,
    val hasRated: Boolean = false,
    val hasShared: Boolean = false,
    val lastGlobalPromptTime: Long = 0L,
    val updateDismissedTime: Long = 0L,
    val rateDismissedTime: Long = 0L,
    val shareDismissedTime: Long = 0L,
    val isUpdateAvailable: Boolean = false
) {
    // Backward compatibility accessors
    val daysSinceInstall: Int get() = installDurationDays
    val operationsCompleted: Int get() = successfulOperations
}

typealias EngagementState = UserEngagementState

/**
 * UserEngagementManager tracks successful app operations and install duration using
 * Jetpack DataStore Preferences. It implements triggering logic checking predefined thresholds
 * (e.g., 5 successful refreshes, 3 days since install) to show rate/share/update dialogs,
 * while enforcing a strict 30-day minimum cooldown between prompts to ensure non-intrusive behavior.
 */
class UserEngagementManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val dataStore = context.engagementDataStore

    companion object {
        val KEY_FIRST_INSTALL_TIME = longPreferencesKey("first_install_time")
        val KEY_LAUNCH_COUNT = intPreferencesKey("launch_count")
        val KEY_MANUAL_REFRESH_COUNT = intPreferencesKey("manual_refresh_count")
        val KEY_SUCCESSFUL_OPERATIONS = intPreferencesKey("successful_operations")
        val KEY_LAST_GLOBAL_PROMPT_TIME = longPreferencesKey("last_global_prompt_time")

        // Update Keys
        val KEY_UPDATE_DISMISSED_TIME = longPreferencesKey("update_dismissed_time")
        val KEY_UPDATE_PROMPT_COUNT = intPreferencesKey("update_prompt_count")
        val KEY_AVAILABLE_UPDATE_VERSION = booleanPreferencesKey("available_update_version")

        // Rate Keys
        val KEY_HAS_RATED = booleanPreferencesKey("has_rated")
        val KEY_RATE_DISMISSED_TIME = longPreferencesKey("rate_dismissed_time")
        val KEY_RATE_PROMPT_COUNT = intPreferencesKey("rate_prompt_count")

        // Share Keys
        val KEY_HAS_SHARED = booleanPreferencesKey("has_shared")
        val KEY_SHARE_DISMISSED_TIME = longPreferencesKey("share_dismissed_time")
        val KEY_SHARE_PROMPT_COUNT = intPreferencesKey("share_prompt_count")

        // Qualification & Cooldown Thresholds
        const val MINIMUM_COOLDOWN_DAYS = 30L
        const val MINIMUM_COOLDOWN_MS = MINIMUM_COOLDOWN_DAYS * 24 * 60 * 60 * 1000L // 30-day minimum cooldown between prompts

        const val MIN_INSTALL_DAYS_THRESHOLD = 3 // 3 days since install
        const val MIN_SUCCESSFUL_REFRESHES_THRESHOLD = 5 // 5 successful refreshes
    }

    private val _state = MutableStateFlow(UserEngagementState())
    val state: StateFlow<UserEngagementState> = _state.asStateFlow()

    init {
        // Observe DataStore updates reactively and sync into StateFlow
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .onEach { prefs ->
                val now = System.currentTimeMillis()
                val installTime = prefs[KEY_FIRST_INSTALL_TIME]
                val durationDays = if (installTime != null) {
                    ((now - installTime) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                } else {
                    0
                }

                _state.update { current ->
                    current.copy(
                        installDurationDays = if (installTime != null) durationDays else current.installDurationDays,
                        manualRefreshCount = prefs[KEY_MANUAL_REFRESH_COUNT] ?: 0,
                        successfulOperations = prefs[KEY_SUCCESSFUL_OPERATIONS] ?: 0,
                        launchCount = prefs[KEY_LAUNCH_COUNT] ?: 1,
                        hasRated = prefs[KEY_HAS_RATED] ?: false,
                        hasShared = prefs[KEY_HAS_SHARED] ?: false,
                        lastGlobalPromptTime = prefs[KEY_LAST_GLOBAL_PROMPT_TIME] ?: 0L,
                        updateDismissedTime = prefs[KEY_UPDATE_DISMISSED_TIME] ?: 0L,
                        rateDismissedTime = prefs[KEY_RATE_DISMISSED_TIME] ?: 0L,
                        shareDismissedTime = prefs[KEY_SHARE_DISMISSED_TIME] ?: 0L,
                        isUpdateAvailable = prefs[KEY_AVAILABLE_UPDATE_VERSION] ?: false
                    )
                }
            }
            .launchIn(scope)

        scope.launch {
            recordLaunchInternal()
        }
    }

    private suspend fun recordLaunchInternal() {
        dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            if (prefs[KEY_FIRST_INSTALL_TIME] == null) {
                prefs[KEY_FIRST_INSTALL_TIME] = now
            }
            val currentLaunches = prefs[KEY_LAUNCH_COUNT] ?: 0
            prefs[KEY_LAUNCH_COUNT] = currentLaunches + 1
        }
    }

    /**
     * Records a manual earthquake refresh (e.g. shake-to-refresh or tap refresh button).
     * Increments both manual refresh count and successful operations in DataStore.
     */
    fun recordManualRefresh() {
        scope.launch {
            recordManualRefreshSync()
        }
    }

    suspend fun recordManualRefreshSync() {
        _state.update {
            it.copy(
                manualRefreshCount = it.manualRefreshCount + 1,
                successfulOperations = it.successfulOperations + 1
            )
        }
        dataStore.edit { prefs ->
            val refreshes = (prefs[KEY_MANUAL_REFRESH_COUNT] ?: 0) + 1
            val operations = (prefs[KEY_SUCCESSFUL_OPERATIONS] ?: 0) + 1
            prefs[KEY_MANUAL_REFRESH_COUNT] = refreshes
            prefs[KEY_SUCCESSFUL_OPERATIONS] = operations
        }
    }

    /**
     * Records a successful user operation (timeframe change, location radius update, safety guide milestone).
     */
    fun recordSuccessfulOperation() {
        scope.launch {
            recordSuccessfulOperationSync()
        }
    }

    suspend fun recordSuccessfulOperationSync() {
        _state.update {
            it.copy(successfulOperations = it.successfulOperations + 1)
        }
        dataStore.edit { prefs ->
            val operations = (prefs[KEY_SUCCESSFUL_OPERATIONS] ?: 0) + 1
            prefs[KEY_SUCCESSFUL_OPERATIONS] = operations
        }
    }

    // Alias for compatibility
    fun recordOperation() = recordSuccessfulOperation()

    /**
     * Evaluates whether an automated popup (Update, Rate, Share) should trigger based on:
     * 1. Emergency safety safeguard (zero proximity warnings or recent tremors)
     * 2. Strict minimum 30-day global cooldown between ANY prompts
     * 3. Predefined threshold criteria: >= 3 days post-install and >= 5 successful refreshes
     * 4. Priority: Update > Rate > Share
     */
    fun evaluateAutomatedPrompts(hasActiveEmergency: Boolean, isSupporter: Boolean) {
        // Strict Emergency Lockout: Never interrupt active tremors or proximity alarms
        if (hasActiveEmergency) return

        // If a dialog is already showing, do not interrupt
        if (_state.value.activePrompt != ActivePromptType.NONE) return

        val currentState = _state.value
        val now = System.currentTimeMillis()

        // 30-Day Minimum Global Cooldown between ANY prompts
        val globalCooldownPassed = (now - currentState.lastGlobalPromptTime) >= MINIMUM_COOLDOWN_MS
        if (!globalCooldownPassed) return

        // PRIORITY 1: UPDATE PROMPT
        if (currentState.isUpdateAvailable && (now - currentState.updateDismissedTime) >= MINIMUM_COOLDOWN_MS) {
            triggerPrompt(ActivePromptType.UPDATE)
            return
        }

        // Qualification thresholds: >= 3 days since install AND >= 5 successful refreshes (or operations)
        val meetsEngagementThresholds = currentState.installDurationDays >= MIN_INSTALL_DAYS_THRESHOLD &&
                (currentState.manualRefreshCount >= MIN_SUCCESSFUL_REFRESHES_THRESHOLD ||
                        currentState.successfulOperations >= MIN_SUCCESSFUL_REFRESHES_THRESHOLD)

        // PRIORITY 2: RATE PROMPT
        val isEligibleForRating = !currentState.hasRated &&
                meetsEngagementThresholds &&
                (now - currentState.rateDismissedTime) >= MINIMUM_COOLDOWN_MS

        if (isEligibleForRating) {
            triggerPrompt(ActivePromptType.RATE)
            return
        }

        // PRIORITY 3: SHARE PROMPT (Encourage family safety readiness)
        val isEligibleForShare = !currentState.hasShared &&
                !isSupporter &&
                meetsEngagementThresholds &&
                (now - currentState.shareDismissedTime) >= MINIMUM_COOLDOWN_MS

        if (isEligibleForShare) {
            triggerPrompt(ActivePromptType.SHARE)
            return
        }
    }

    fun triggerPrompt(type: ActivePromptType) {
        val now = System.currentTimeMillis()
        _state.update { it.copy(activePrompt = type, lastGlobalPromptTime = now) }

        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_LAST_GLOBAL_PROMPT_TIME] = now
                when (type) {
                    ActivePromptType.UPDATE -> {
                        val count = (prefs[KEY_UPDATE_PROMPT_COUNT] ?: 0) + 1
                        prefs[KEY_UPDATE_PROMPT_COUNT] = count
                    }
                    ActivePromptType.RATE -> {
                        val count = (prefs[KEY_RATE_PROMPT_COUNT] ?: 0) + 1
                        prefs[KEY_RATE_PROMPT_COUNT] = count
                    }
                    ActivePromptType.SHARE -> {
                        val count = (prefs[KEY_SHARE_PROMPT_COUNT] ?: 0) + 1
                        prefs[KEY_SHARE_PROMPT_COUNT] = count
                    }
                    ActivePromptType.NONE -> {}
                }
            }
        }
    }

    fun dismissPrompt(type: ActivePromptType) {
        val now = System.currentTimeMillis()
        _state.update { current ->
            when (type) {
                ActivePromptType.UPDATE -> current.copy(activePrompt = ActivePromptType.NONE, updateDismissedTime = now)
                ActivePromptType.RATE -> current.copy(activePrompt = ActivePromptType.NONE, rateDismissedTime = now)
                ActivePromptType.SHARE -> current.copy(activePrompt = ActivePromptType.NONE, shareDismissedTime = now)
                ActivePromptType.NONE -> current.copy(activePrompt = ActivePromptType.NONE)
            }
        }

        scope.launch {
            dataStore.edit { prefs ->
                when (type) {
                    ActivePromptType.UPDATE -> prefs[KEY_UPDATE_DISMISSED_TIME] = now
                    ActivePromptType.RATE -> prefs[KEY_RATE_DISMISSED_TIME] = now
                    ActivePromptType.SHARE -> prefs[KEY_SHARE_DISMISSED_TIME] = now
                    ActivePromptType.NONE -> {}
                }
            }
        }
    }

    fun markRatingCompleted() {
        _state.update { it.copy(activePrompt = ActivePromptType.NONE, hasRated = true) }
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_HAS_RATED] = true
            }
        }
    }

    fun markShareCompleted() {
        _state.update { it.copy(activePrompt = ActivePromptType.NONE, hasShared = true) }
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_HAS_SHARED] = true
            }
        }
    }

    fun setUpdateAvailable(available: Boolean) {
        _state.update { it.copy(isUpdateAvailable = available) }
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_AVAILABLE_UPDATE_VERSION] = available
            }
        }
    }

    suspend fun setMockInstallDurationDays(days: Int) {
        val now = System.currentTimeMillis()
        val mockInstallTime = now - (days.toLong() * 24 * 60 * 60 * 1000L) - 10000L
        _state.update { it.copy(installDurationDays = days) }
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_INSTALL_TIME] = mockInstallTime
        }
    }

    suspend fun resetCooldownsForTesting() {
        _state.update {
            it.copy(
                lastGlobalPromptTime = 0L,
                updateDismissedTime = 0L,
                rateDismissedTime = 0L,
                shareDismissedTime = 0L
            )
        }
        dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_GLOBAL_PROMPT_TIME)
            prefs.remove(KEY_UPDATE_DISMISSED_TIME)
            prefs.remove(KEY_RATE_DISMISSED_TIME)
            prefs.remove(KEY_SHARE_DISMISSED_TIME)
        }
    }

    suspend fun clearDataForTesting() {
        _state.value = UserEngagementState()
        dataStore.edit { it.clear() }
    }
}

// Backward-compatible alias
typealias EngagementManager = UserEngagementManager
