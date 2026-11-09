package com.example.engagement

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserEngagementManagerTest {

    private lateinit var context: Context
    private lateinit var manager: UserEngagementManager

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        manager = UserEngagementManager(context)
        manager.clearDataForTesting()
        manager.state.first { it.manualRefreshCount == 0 && !it.hasRated && !it.hasShared }
    }

    @Test
    fun `test initial state starts with zero refreshes and no prompt`() = runTest {
        val state = manager.state.first { it.activePrompt == ActivePromptType.NONE }
        assertEquals(0, state.manualRefreshCount)
        assertEquals(0, state.successfulOperations)
        assertEquals(ActivePromptType.NONE, state.activePrompt)
        assertFalse(state.hasRated)
        assertFalse(state.hasShared)
    }

    @Test
    fun `test recordManualRefresh increments both manual refreshes and operations count`() = runTest {
        manager.recordManualRefreshSync()
        manager.recordManualRefreshSync()

        val state = manager.state.first { it.manualRefreshCount == 2 }
        assertEquals(2, state.manualRefreshCount)
        assertEquals(2, state.successfulOperations)
    }

    @Test
    fun `test recordSuccessfulOperation increments successful operations`() = runTest {
        manager.recordSuccessfulOperationSync()
        manager.recordSuccessfulOperationSync()

        val state = manager.state.first { it.successfulOperations == 2 }
        assertEquals(0, state.manualRefreshCount)
        assertEquals(2, state.successfulOperations)
    }

    @Test
    fun `test emergency lockout prevents prompt even when thresholds are met`() = runTest {
        manager.setMockInstallDurationDays(4)
        repeat(6) { manager.recordManualRefreshSync() }

        // Trigger evaluation during active emergency (< 150km tremor)
        manager.evaluateAutomatedPrompts(hasActiveEmergency = true, isSupporter = false)

        assertEquals(ActivePromptType.NONE, manager.state.value.activePrompt)
    }

    @Test
    fun `test rate dialog not shown if install duration less than 3 days`() = runTest {
        manager.setMockInstallDurationDays(2) // Only 2 days post-install
        repeat(6) { manager.recordManualRefreshSync() } // Met refresh threshold

        manager.state.first { it.manualRefreshCount == 6 }
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)

        assertEquals(ActivePromptType.NONE, manager.state.value.activePrompt)
    }

    @Test
    fun `test rate dialog not shown if successful refreshes less than 5`() = runTest {
        manager.setMockInstallDurationDays(4) // 4 days post-install
        repeat(4) { manager.recordManualRefreshSync() } // Only 4 refreshes (5 needed)

        manager.state.first { it.manualRefreshCount == 4 }
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)

        assertEquals(ActivePromptType.NONE, manager.state.value.activePrompt)
    }

    @Test
    fun `test rate dialog shown after 5th successful refresh and 3 days since install`() = runTest {
        manager.setMockInstallDurationDays(3) // 3 days since install
        repeat(5) { manager.recordManualRefreshSync() } // 5 successful refreshes

        manager.state.first { it.manualRefreshCount == 5 }
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)

        assertEquals(ActivePromptType.RATE, manager.state.value.activePrompt)
    }

    @Test
    fun `test 30-day minimum cooldown strictly suppresses prompts`() = runTest {
        manager.setMockInstallDurationDays(5)
        repeat(5) { manager.recordManualRefreshSync() }
        manager.state.first { it.manualRefreshCount == 5 }

        // Triggers rate dialog
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)
        assertEquals(ActivePromptType.RATE, manager.state.value.activePrompt)

        // User dismisses dialog
        manager.dismissPrompt(ActivePromptType.RATE)
        assertEquals(ActivePromptType.NONE, manager.state.value.activePrompt)

        // Immediate subsequent evaluations within 30 days must be suppressed by 30-day cooldown
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)
        assertEquals(ActivePromptType.NONE, manager.state.value.activePrompt)
    }

    @Test
    fun `test update prompt priority over rate and share when available`() = runTest {
        manager.setMockInstallDurationDays(5)
        repeat(6) { manager.recordManualRefreshSync() }
        manager.setUpdateAvailable(true)

        manager.state.first { it.isUpdateAvailable }
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)

        // Update takes highest priority
        assertEquals(ActivePromptType.UPDATE, manager.state.value.activePrompt)
    }

    @Test
    fun `test share prompt triggers when eligible and user is not supporter`() = runTest {
        manager.setMockInstallDurationDays(4)
        repeat(5) { manager.recordManualRefreshSync() }
        // User already rated, so rate is skipped and share is evaluated
        manager.markRatingCompleted()
        manager.resetCooldownsForTesting()

        manager.state.first { it.hasRated }
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = false)

        assertEquals(ActivePromptType.SHARE, manager.state.value.activePrompt)
    }

    @Test
    fun `test supporter status permanently suppresses share prompt`() = runTest {
        manager.setMockInstallDurationDays(4)
        repeat(5) { manager.recordManualRefreshSync() }
        manager.markRatingCompleted()
        manager.resetCooldownsForTesting()

        manager.state.first { it.hasRated }
        // User is supporter -> share prompt is suppressed
        manager.evaluateAutomatedPrompts(hasActiveEmergency = false, isSupporter = true)

        assertEquals(ActivePromptType.NONE, manager.state.value.activePrompt)
    }
}
