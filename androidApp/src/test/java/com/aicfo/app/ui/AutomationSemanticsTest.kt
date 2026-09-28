package com.aicfo.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemoryTokenVault
import org.junit.Assert.assertEquals
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
    }
}
