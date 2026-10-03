package com.aicfo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.domain.AppObserver
import com.aicfo.shared.presentation.AuthStep
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.presentation.Phase
import com.aicfo.shared.sync.SyncStatus
import com.aicfo.shared.sync.SyncTrigger
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemorySecureStore
import com.aicfo.shared.security.MemoryTokenVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AutomationSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun pillTabsExposeTagDescriptionAndTextOnTheClickableChip() {
        val selected = mutableStateOf("HOME")
        rule.setContent {
            PillNav(selected = selected.value, onSelect = { selected.value = it })
        }

        listOf(
            AutomationTags.NAV_HOME to "Home",
            AutomationTags.NAV_MOVES to "Moves",
            AutomationTags.NAV_ACCOUNTS to "Accounts",
            AutomationTags.NAV_SETTINGS to "Settings",
        ).forEach { (tag, label) ->
            rule.onNodeWithTag(tag)
                .assertHasClickAction()
                .assertContentDescriptionEquals(label)
                .assertTextEquals(label)
            rule.onAllNodesWithContentDescription(label).assertCountEquals(1)
        }

        rule.onNodeWithTag(AutomationTags.NAV_MOVES).performClick()
        rule.runOnIdle { assertEquals("MOVES", selected.value) }
        rule.onNodeWithTag(AutomationTags.NAV_ACCOUNTS).performClick()
        rule.runOnIdle { assertEquals("ACCOUNTS", selected.value) }
        rule.onNodeWithTag(AutomationTags.NAV_SETTINGS).performClick()
        rule.runOnIdle { assertEquals("SETTINGS", selected.value) }
    }

    @Test
    fun settingsQaBlockExposesStableTagsInDebug() {
        val controller = AiCfoController(
            vault = MemoryTokenVault(),
            store = MemoryLocalStore(),
            clock = object : AppClock {
                override fun nowEpochMs(): Long = 10L
            },
            market = Markets.unitedStates(),
            localStrings = EmptyLocalStrings,
            debugBuild = true,
        )
        rule.setContent { SettingsScreen(controller, tick = 0) }

        rule.onNodeWithTag(AutomationTags.QA_TRIAL_PAYWALL)
            .assertContentDescriptionEquals("QA · trial / paywall")
        rule.onNodeWithTag(AutomationTags.QA_SHOW_PAYWALL)
            .assertHasClickAction()
            .assertContentDescriptionEquals("Show paywall")
            .assertTextEquals("Show paywall")
        rule.onNodeWithTag(AutomationTags.QA_RESTORE_TRIAL)
            .assertContentDescriptionEquals("Restore trial")
        rule.onNodeWithTag(AutomationTags.QA_SIMULATE_PRO)
            .assertContentDescriptionEquals("Simulate Pro")
        rule.onNodeWithTag(AutomationTags.QA_CLEAR_OVERRIDE)
            .assertContentDescriptionEquals("Clear QA override")
        rule.onNodeWithTag(AutomationTags.QA_REPLAY_ONBOARDING)
            .assertContentDescriptionEquals("Replay onboarding")
        rule.onNodeWithTag(AutomationTags.QA_SIMULATE_RECONNECT)
            .performScrollTo()
            .assertHasClickAction()
            .assertContentDescriptionEquals("Simulate bank reconnect")
            .performClick()
        rule.runOnIdle {
            assertTrue(controller.syncStatus() is SyncStatus.NeedsReauth)
        }
    }

    @Test
    fun accountsAndHomeShowFreshnessAndReconnect() {
        val controller = debugController()
        assertTrue(controller.connectReadOnlyStub())
        rule.setContent {
            var tick by remember { mutableIntStateOf(0) }
            DisposableEffect(controller) {
                val observer = object : AppObserver {
                    override fun onChanged() {
                        tick += 1
                    }
                }
                controller.addObserver(observer)
                onDispose { controller.removeObserver(observer) }
            }
            AccountsScreen(controller, tick)
        }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
        rule.runOnIdle { controller.debugSimulateNeedsReauth() }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS_ACTION)
            .assertTextEquals("Reconnect")
            .performClick()
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
        rule.runOnIdle {
            assertTrue(controller.syncStatus() is SyncStatus.Success)
        }

        rule.onNodeWithTag(AutomationTags.ACCOUNTS_PULL).performTouchInput {
            swipeDown(startY = 80f, endY = 700f, durationMillis = 200)
        }
        rule.runOnIdle {
            assertEquals(SyncTrigger.PullToRefresh, controller.lastSyncTrigger())
        }
    }

    @Test
    fun homeWealthStripShowsTheSameFreshnessLine() {
        val controller = debugController()
        assertTrue(controller.connectReadOnlyStub())
        rule.setContent { HomeScreen(controller, tick = 0, wide = false) {} }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
    }

    @Test
    fun linkReadOnlySampleShowsFreshnessOnAccountsAndHome() {
        val controller = debugController()
        assertFalse(controller.accounts().linked)
        val showHome = mutableStateOf(false)
        rule.setContent {
            if (showHome.value) RevisingHome(controller) else RevisingAccounts(controller)
        }
        rule.onNodeWithText("Nothing linked · Maya Chen").assertIsDisplayed()
        rule.onNodeWithText("Connect securely").assertIsDisplayed()
        rule.onNodeWithText("Link read-only sample").performScrollTo().performClick()
        rule.onNodeWithText("Maya sample · not your bank").assertIsDisplayed()
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
        rule.onAllNodesWithTag(AutomationTags.BANK_FRESHNESS_ACTION).assertCountEquals(0)

        rule.runOnIdle { showHome.value = true }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
        rule.onAllNodesWithTag(AutomationTags.BANK_FRESHNESS_ACTION).assertCountEquals(0)
    }

    @Test
    fun simulateReconnectWithoutAPriorLinkShowsReconnect() {
        val controller = debugController()
        assertFalse(controller.accounts().linked)
        val showHome = mutableStateOf(false)
        rule.runOnIdle { controller.debugSimulateNeedsReauth() }
        rule.setContent {
            if (showHome.value) RevisingHome(controller) else RevisingAccounts(controller)
        }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS)
            .assertTextEquals("Reconnect to refresh balances. Last update just now.")
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS_ACTION)
            .assertTextEquals("Reconnect")
            .assertHasClickAction()

        rule.runOnIdle { showHome.value = true }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS)
            .assertTextEquals("Reconnect to refresh balances. Last update just now.")
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS_ACTION)
            .assertTextEquals("Reconnect")
            .performClick()
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
        rule.onAllNodesWithTag(AutomationTags.BANK_FRESHNESS_ACTION).assertCountEquals(0)
        rule.runOnIdle {
            assertTrue(controller.syncStatus() is SyncStatus.Success)
            assertTrue(controller.accounts().linked)
        }
    }

    @Test
    fun simulateFailureWithoutAPriorLinkShowsTryAgain() {
        val controller = debugController()
        assertFalse(controller.accounts().linked)
        val showHome = mutableStateOf(true)
        rule.runOnIdle { controller.debugSimulateSyncFailure() }
        rule.setContent {
            if (showHome.value) RevisingHome(controller) else RevisingAccounts(controller)
        }
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS)
            .assertTextEquals("Couldn't refresh. Last update just now.")
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS_ACTION)
            .assertTextEquals("Try again")
            .assertHasClickAction()

        rule.runOnIdle { showHome.value = false }
        rule.onAllNodesWithText("Nothing linked · Maya Chen").assertCountEquals(0)
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS)
            .assertTextEquals("Couldn't refresh. Last update just now.")
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS_ACTION)
            .assertTextEquals("Try again")
            .performClick()
        rule.onNodeWithTag(AutomationTags.BANK_FRESHNESS).assertTextEquals("Updated just now")
        rule.onAllNodesWithTag(AutomationTags.BANK_FRESHNESS_ACTION).assertCountEquals(0)
        rule.runOnIdle {
            assertTrue(controller.syncStatus() is SyncStatus.Success)
        }
    }

    @Test
    fun releaseIgnoresSimulateAndKeepsTheUnlinkedEmptyState() {
        val controller = AiCfoController(
            vault = MemoryTokenVault(),
            store = MemoryLocalStore(),
            clock = object : AppClock {
                override fun nowEpochMs(): Long = 10L
            },
            market = Markets.unitedStates(),
            localStrings = EmptyLocalStrings,
            debugBuild = false,
            secure = MemorySecureStore(),
        )
        controller.debugSimulateNeedsReauth()
        controller.debugSimulateSyncFailure()
        rule.setContent { AccountsScreen(controller, tick = 0) }
        rule.onNodeWithText("Nothing linked · Maya Chen").assertIsDisplayed()
        rule.onNodeWithText("Connect securely").assertIsDisplayed()
        rule.onAllNodesWithText("Link read-only sample").assertCountEquals(0)
        rule.onAllNodesWithTag(AutomationTags.BANK_FRESHNESS).assertCountEquals(0)
        rule.onAllNodesWithTag(AutomationTags.BANK_FRESHNESS_ACTION).assertCountEquals(0)
        rule.runOnIdle {
            assertTrue(controller.syncStatus() is SyncStatus.Idle)
            assertFalse(controller.accounts().linked)
        }
    }

    @Test
    fun paywallCtasDismissTheHardPaywall() {
        val controller = debugController()
        reachMain(controller)
        controller.debugForcePaywall()
        rule.setContent { PaywallScreen(controller, tick = 0) }

        rule.onNodeWithTag(AutomationTags.PAYWALL_CONTINUE_YEARLY)
            .assertHasClickAction()
            .assertTextEquals("Continue · \$79/yr")
            .assertContentDescriptionEquals("Continue · \$79/yr")
        rule.onAllNodesWithText("Continue · \$79/yr").assertCountEquals(1)
        rule.onNodeWithTag(AutomationTags.PAYWALL_CONTINUE_MONTHLY)
            .assertTextEquals("Continue · \$9.99/mo")
            .assertContentDescriptionEquals("Continue · \$9.99/mo")
        rule.onAllNodesWithText("Continue · \$9.99/mo").assertCountEquals(1)

        rule.onNodeWithTag(AutomationTags.QA_RETURN_TO_TRIAL)
            .performScrollTo()
            .assertTextEquals("QA: return to trial")
            .performClick()
        rule.runOnIdle {
            assertEquals(Gate.APP, controller.gate())
            assertEquals(Phase.TRIAL, controller.settings().phase)
        }

        controller.debugForcePaywall()
        rule.onNodeWithTag(AutomationTags.PAYWALL_CONTINUE_YEARLY).performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(Gate.APP, controller.gate())
            assertEquals(Phase.PRO, controller.settings().phase)
        }

        controller.debugForcePaywall()
        rule.onNodeWithTag(AutomationTags.PAYWALL_CONTINUE_MONTHLY).performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(Gate.APP, controller.gate())
            assertEquals(Phase.PRO, controller.settings().phase)
            assertEquals("Finwise Pro · monthly", controller.settings().planLabel)
        }
    }

    @Test
    fun authScreensRenderThePhoneEmailPinAndColdUnlockBoards() {
        val controller = debugController()
        repeat(8) {
            if (controller.gate() != Gate.ONBOARDING) return@repeat
            if (controller.onboarding().step == 2 && controller.onboarding().secondaryCta == "Skip for now") {
                controller.secondaryOnboarding()
            } else {
                controller.primaryOnboarding()
            }
        }
        assertEquals(Gate.AUTH, controller.gate())
        assertEquals(AuthStep.PHONE, controller.authStep())
        val tick = mutableStateOf(0)
        val showLock = mutableStateOf(false)
        rule.setContent {
            if (showLock.value) {
                LockScreen(controller, tick.value) {}
            } else {
                AuthFlowScreen(controller, tick.value)
            }
        }
        rule.onNodeWithText("What's your number?").assertIsDisplayed()
        rule.onNodeWithText("We'll text a one-time code. No password to remember.").assertIsDisplayed()
        rule.onNodeWithText("Continue").assertIsDisplayed()

        controller.debugSkipPhone()
        tick.value = 1
        rule.onNodeWithText("Where should we send your wins?").assertIsDisplayed()
        rule.onNodeWithText("Skip for now").assertIsDisplayed()

        controller.skipReportEmail()
        tick.value = 2
        rule.onNodeWithText("Create a 6-digit PIN").assertIsDisplayed()
        rule.onNodeWithText("Enter PIN · confirm next").assertIsDisplayed()

        controller.saveDevicePin("123456", "123456")
        controller.primaryOnboarding()
        controller.lockNow()
        assertEquals(Gate.LOCK, controller.gate())
        showLock.value = true
        tick.value = 3
        rule.onNodeWithText("Enter your PIN").assertIsDisplayed()
        rule.onNodeWithText("Device unlock only. Your account stays signed in with phone + OTP.")
            .assertIsDisplayed()

        controller.setBiometricHardware(true)
        controller.setBiometricEnabled(true)
        tick.value = 4
        rule.onNodeWithText("WELCOME BACK").assertIsDisplayed()
        rule.onNodeWithText("Unlock Finwise").assertIsDisplayed()
        rule.onNodeWithText("Unlock with biometrics").performScrollTo().assertIsDisplayed()
    }

    private fun reachMain(controller: AiCfoController) {
        repeat(8) {
            if (controller.gate() != Gate.ONBOARDING) return@repeat
            if (controller.onboarding().step == 2 && controller.onboarding().secondaryCta == "Skip for now") {
                controller.secondaryOnboarding()
            } else {
                controller.primaryOnboarding()
            }
        }
        if (controller.gate() == Gate.AUTH) {
            controller.debugSkipPhone()
            controller.skipReportEmail()
            controller.debugCompleteUnlockSetup()
        }
        repeat(4) {
            if (controller.gate() != Gate.ONBOARDING) return@repeat
            controller.primaryOnboarding()
        }
    }

    private fun debugController(): AiCfoController = AiCfoController(
        vault = MemoryTokenVault(),
        store = MemoryLocalStore(),
        clock = object : AppClock {
            override fun nowEpochMs(): Long = 10L
        },
        market = Markets.unitedStates(),
        localStrings = EmptyLocalStrings,
        debugBuild = true,
    )
}

@Composable
private fun RevisingAccounts(controller: AiCfoController) {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(controller) {
        val observer = object : AppObserver {
            override fun onChanged() {
                tick += 1
            }
        }
        controller.addObserver(observer)
        onDispose { controller.removeObserver(observer) }
    }
    AccountsScreen(controller, tick)
}

@Composable
private fun RevisingHome(controller: AiCfoController) {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(controller) {
        val observer = object : AppObserver {
            override fun onChanged() {
                tick += 1
            }
        }
        controller.addObserver(observer)
        onDispose { controller.removeObserver(observer) }
    }
    HomeScreen(controller, tick, wide = false) {}
}
