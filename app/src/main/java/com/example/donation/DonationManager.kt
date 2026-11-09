package com.example.donation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DonationState(
    val isSupporter: Boolean = false,
    val supporterTier: String = "",
    val operationsCompleted: Int = 0,
    val launchCount: Int = 0,
    val daysSinceInstall: Int = 0,
    val canShowValuePrompt: Boolean = false,
    val pendingMilestone: Int? = null
)

enum class PlayBillingTier(
    val id: String,
    val title: String,
    val price: String,
    val iconEmoji: String,
    val description: String
) {
    TIER_1("coffee_tip", "Coffee Tip", "$1.99", "☕", "Fuel server monitoring & real-time USGS telemetry"),
    TIER_2("seismologist", "Seismologist Supporter", "$4.99", "🔬", "Help maintain 3D globe rendering & wave simulators"),
    TIER_3("geophysics_patron", "Geophysics Patron", "$9.99", "🌍", "Sponsor high-speed real-time seismic alert infrastructure"),
    TIER_4("quakecast_champion", "QuakeCast Champion", "$19.99", "🏆", "Champion independent, 100% ad-free earthquake safety")
}

class DonationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("quakecast_donation_prefs", Context.MODE_PRIVATE)

    private val _donationState = MutableStateFlow(loadState())
    val donationState: StateFlow<DonationState> = _donationState.asStateFlow()

    private var activePendingMilestone: Int? = null

    init {
        recordLaunch()
    }

    private fun loadState(): DonationState {
        val isSupporter = prefs.getBoolean("is_supporter", false)
        val supporterTier = prefs.getString("supporter_tier", "") ?: ""
        val operations = prefs.getInt("operations_completed", 0)
        val launches = prefs.getInt("launch_count", 1)

        val installTime = prefs.getLong("first_install_time", 0L).let {
            if (it == 0L) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong("first_install_time", now).apply()
                now
            } else it
        }

        val daysSinceInstall = ((System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)).toInt()
        val lastDismissed = prefs.getLong("last_dismissed_prompt_time", 0L)
        val cooldownPassed = (System.currentTimeMillis() - lastDismissed) > (14L * 24 * 60 * 60 * 1000)

        // Satisfaction Safeguard criteria:
        // 1. Not already a supporter
        // 2. Completed at least 3 successful operations
        // 3. Cooldown period elapsed
        val canShow = !isSupporter && operations >= 3 && cooldownPassed

        return DonationState(
            isSupporter = isSupporter,
            supporterTier = supporterTier,
            operationsCompleted = operations,
            launchCount = launches,
            daysSinceInstall = daysSinceInstall,
            canShowValuePrompt = canShow,
            pendingMilestone = activePendingMilestone
        )
    }

    fun recordSuccessfulOperation() {
        val current = prefs.getInt("operations_completed", 0) + 1
        prefs.edit().putInt("operations_completed", current).apply()

        // Check for 10th or 25th milestone trigger
        if ((current == 10 || current == 25) && !prefs.getBoolean("milestone_${current}_notified", false)) {
            prefs.edit().putBoolean("milestone_${current}_notified", true).apply()
            activePendingMilestone = current
        }

        refreshState()
    }

    fun clearPendingMilestone() {
        activePendingMilestone = null
        refreshState()
    }

    private fun recordLaunch() {
        val current = prefs.getInt("launch_count", 0) + 1
        prefs.edit().putInt("launch_count", current).apply()
        refreshState()
    }

    fun dismissPrompt() {
        prefs.edit().putLong("last_dismissed_prompt_time", System.currentTimeMillis()).apply()
        refreshState()
    }

    fun markAsSupporter(tier: PlayBillingTier) {
        prefs.edit()
            .putBoolean("is_supporter", true)
            .putString("supporter_tier", tier.title)
            .putLong("supporter_timestamp", System.currentTimeMillis())
            .apply()
        refreshState()
    }

    private fun refreshState() {
        _donationState.value = loadState()
    }
}
