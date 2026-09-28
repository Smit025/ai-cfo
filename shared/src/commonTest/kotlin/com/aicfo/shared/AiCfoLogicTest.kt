package com.aicfo.shared

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.domain.EntitlementPolicy
import com.aicfo.shared.domain.MoneyMath
import com.aicfo.shared.domain.Pricing
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.presentation.MoveStatusCode
import com.aicfo.shared.presentation.Phase
import com.aicfo.shared.presentation.QaOverride
import com.aicfo.shared.security.LinkPolicy
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemoryTokenVault
import com.aicfo.shared.security.PiiPolicy
import com.aicfo.shared.security.SafeLog
import com.aicfo.shared.security.TlsPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiCfoLogicTest {
    @Test
    fun gympassYearlySavingsMatchTheActionScreen() {
        assertEquals(564_00L, MoneyMath.yearlyFromMonthlyCents(47_00L))
    }

    @Test
    fun noForeverFreeWithoutATrial() {
        val snap = EntitlementPolicy.resolve(
            nowMs = 0L,
            trialStartedAtMs = null,
            overrideCode = QaOverride.NONE,
            subscribedPlan = "NONE",
        )
        assertEquals(Phase.PAYWALL, snap.phase)
    }

    @Test
    fun trialLastsThirtyDaysThenPaywall() {
        val start = 1_700_000_000_000L
        val clock = MutableClock(start)
        val app = newApp(clock)
        finishOnboarding(app)
        assertEquals(Gate.APP, app.gate())
        assertEquals(Phase.TRIAL, app.settings().phase)
        assertEquals(30, app.settings().planLabel.contains("30 days left").let { if (it) 30 else -1 })

        clock.now = start + Pricing.TRIAL_WINDOW_MS - 1L
        assertEquals(Phase.TRIAL, app.settings().phase)
        assertEquals(Gate.APP, app.gate())

        clock.now = start + Pricing.TRIAL_WINDOW_MS
        assertEquals(Phase.PAYWALL, app.settings().phase)
        assertEquals(Gate.PAYWALL, app.gate())
    }

    @Test
    fun qaOverrideFlipsTrialAndPaywall() {
        val app = newApp(MutableClock(10L))
        finishOnboarding(app)
        app.debugForcePaywall()
        assertEquals(Gate.PAYWALL, app.gate())
        app.debugForceTrial()
        assertEquals(Gate.APP, app.gate())
        assertEquals(Phase.TRIAL, app.settings().phase)
        app.debugForcePro()
        assertEquals(Phase.PRO, app.settings().phase)
        app.debugClearOverride()
        assertEquals(Phase.TRIAL, app.settings().phase)
    }

    @Test
    fun simulatedPurchaseClearsThePaywall() {
        val app = newApp(MutableClock(10L))
        finishOnboarding(app)
        app.debugForcePaywall()
        assertEquals(Gate.PAYWALL, app.gate())
        app.purchaseYearly()
        assertEquals(Gate.APP, app.gate())
        assertEquals(Phase.PRO, app.settings().phase)
    }

    @Test
    fun homeLeadsWithTheTopTodoMove() {
        val app = newApp(MutableClock(10L))
        finishOnboarding(app)
        val home = app.home()
        assertEquals(3, home.moveCount())
        assertEquals("gympass", home.moveAt(0).id)
        assertEquals("capital-one", home.moveAt(1).id)
        assertEquals("idle-cash", home.moveAt(2).id)
        assertTrue(home.pulse.contains("47 days"))
    }

    @Test
    fun cancelMarksDoneAndKeepSkips() {
        val app = newApp(MutableClock(10L))
        finishOnboarding(app)
        app.performSecondary("gympass")
        assertEquals(MoveStatusCode.SKIPPED, app.moveStatus("gympass"))
        assertEquals("capital-one", app.home().moveAt(0).id)

        app.performPrimary("capital-one")
        assertEquals(MoveStatusCode.TODO, app.moveStatus("capital-one"))
        assertTrue(app.detail("capital-one")!!.banner.contains("won't move the money"))

        app.performPrimary("gympass")
        assertEquals(MoveStatusCode.DONE, app.moveStatus("gympass"))
    }

    @Test
    fun connectStoresLinkTokensOnly() {
        val vault = MemoryTokenVault()
        val app = AiCfoController(vault, MemoryLocalStore(), MutableClock(10L))
        assertFalse(LinkPolicy.accepts("password=hunter2"))
        app.advanceOnboarding()
        app.advanceOnboarding()
        assertEquals(2, app.onboarding().step)
        assertFalse(app.onboarding().canAdvance)
        assertTrue(app.connectReadOnlyStub())
        assertTrue(app.onboarding().canAdvance)
        val token = vault.read("institution.chase-checking")
        assertEquals("link_stub_chase-checking", token)
        assertTrue(LinkPolicy.accepts(token!!))
        assertFalse(PiiPolicy.storesRawPasswords)
        app.disconnectAll()
        assertNull(vault.read("institution.chase-checking"))
        assertFalse(app.accounts().linked)
    }

    @Test
    fun logsRedactTokensPasswordsAndCardNumbers() {
        val cleaned = SafeLog.redact(
            "saved link_stub_chase-checking password=hunter2 4111111111111111",
        )
        assertFalse(cleaned.contains("hunter2"))
        assertFalse(cleaned.contains("link_stub_chase-checking"))
        assertFalse(cleaned.contains("4111111111111111"))
        assertTrue(cleaned.contains("[redacted]"))
        assertFalse(TlsPolicy.cleartextAllowed)
        TlsPolicy.requireHttps("https://api.aicfo.app")
    }

    @Test
    fun replayOnboardingDoesNotResetTheTrialClock() {
        val start = 5_000L
        val clock = MutableClock(start)
        val app = newApp(clock)
        finishOnboarding(app)
        clock.now = start + 10L * Pricing.DAY_MS
        app.debugReplayOnboarding()
        assertEquals(Gate.ONBOARDING, app.gate())
        finishOnboarding(app)
        assertTrue(app.settings().planLabel.contains("20 days"))
    }

    @Test
    fun movesBoardCountsTodoDoneAndSkipped() {
        val app = newApp(MutableClock(10L))
        finishOnboarding(app)
        val board = app.moves()
        assertEquals(5, board.moveCount())
        assertEquals(3, board.todoCount)
        assertEquals(1, board.doneCount)
        assertEquals(1, board.skippedCount)
        assertEquals("gympass", app.resolvedMoveId())
    }

    private fun newApp(clock: MutableClock): AiCfoController {
        return AiCfoController(MemoryTokenVault(), MemoryLocalStore(), clock)
    }

    private fun finishOnboarding(app: AiCfoController) {
        repeat(8) {
            if (app.gate() != Gate.ONBOARDING) return
            if (!app.onboarding().canAdvance) app.connectReadOnlyStub()
            app.advanceOnboarding()
        }
        assertEquals(Gate.APP, app.gate())
    }
}

private class MutableClock(var now: Long) : AppClock {
    override fun nowEpochMs(): Long = now
}
