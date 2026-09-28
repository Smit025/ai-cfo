package com.aicfo.shared

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.domain.EntitlementPolicy
import com.aicfo.shared.domain.Qa
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.market.MoneyMath
import com.aicfo.shared.domain.Pricing
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.presentation.MoveStatusCode
import com.aicfo.shared.presentation.Phase
import com.aicfo.shared.presentation.QaOverride
import com.aicfo.shared.security.LinkPolicy
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemoryTokenVault
import com.aicfo.shared.security.TokenVault
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
    fun returnToTrialAndSimulatedPurchasesDismissAForcedPaywall() {
        val start = 1_700_000_000_000L
        val clock = MutableClock(start)
        val app = newApp(clock)
        finishOnboarding(app)
        clock.now = start + Pricing.TRIAL_WINDOW_MS
        assertEquals(Gate.PAYWALL, app.gate())
        assertEquals("Your 30-day Pro trial has ended", app.paywall().title)

        app.debugForceTrial()
        assertEquals(Gate.APP, app.gate())
        assertEquals(Phase.TRIAL, app.settings().phase)

        app.debugForcePaywall()
        assertEquals(Gate.PAYWALL, app.gate())
        assertEquals("Your 30-day Pro trial has ended", app.paywall().title)
        app.purchaseYearly()
        assertEquals(Gate.APP, app.gate())
        assertEquals(Phase.PRO, app.settings().phase)
        assertEquals("Finwise Pro · yearly", app.settings().planLabel)

        app.debugForcePaywall()
        app.purchaseMonthly()
        assertEquals(Gate.APP, app.gate())
        assertEquals(Phase.PRO, app.settings().phase)
        assertEquals("Finwise Pro · monthly", app.settings().planLabel)
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
        assertEquals("\$8,420", home.savingsAmount)
        assertEquals("\$42.1k", home.netWorthAmount)
        assertTrue(home.savingsUp)
        assertTrue(home.netWorthUp)
        assertEquals("47 days runway · quietly building", home.runway)
        assertEquals(
            "If unused Gympass stayed cancelled this year, you'd keep ~\$564 more",
            home.hope,
        )
        assertEquals(3, home.snapshotCount())
        assertEquals("Needs", home.snapshotAt(0).label)
        assertEquals("\$2,840", home.snapshotAt(0).amount)
        assertEquals("Wants", home.snapshotAt(1).label)
        assertEquals("To save", home.snapshotAt(2).label)
        assertEquals("\$890", home.snapshotAt(2).amount)
        assertEquals("Last check-in 86 days ago — paying for nothing.", home.moveAt(0).body)
    }

    @Test
    fun cancelMarksDoneAndKeepSkips() {
        val app = newApp(MutableClock(10L))
        finishOnboarding(app)
        app.performSecondary("gympass")
        assertEquals(MoveStatusCode.SKIPPED, app.moveStatus("gympass"))
        assertEquals("capital-one", app.home().moveAt(0).id)
        assertEquals("", app.home().hope)

        app.performPrimary("capital-one")
        assertEquals(MoveStatusCode.TODO, app.moveStatus("capital-one"))
        assertTrue(app.detail("capital-one")!!.banner.contains("won't move the money"))

        app.performPrimary("gympass")
        assertEquals(MoveStatusCode.DONE, app.moveStatus("gympass"))
        assertTrue(app.home().hope.contains("\$564 stays with you"))
    }

    @Test
    fun connectStoresLinkTokensOnly() {
        val vault = MemoryTokenVault()
        val app = AiCfoController(vault, MemoryLocalStore(), MutableClock(10L))
        assertFalse(LinkPolicy.accepts("password=hunter2"))
        app.advanceOnboarding()
        app.advanceOnboarding()
        val connect = app.onboarding()
        assertEquals(2, connect.step)
        assertTrue(connect.canAdvance)
        assertEquals("Connect securely", connect.primaryCta)
        assertEquals("Skip for now", connect.secondaryCta)
        assertTrue(app.connectReadOnlyStub())
        val token = vault.read("institution.chase-checking")
        assertEquals("link_stub_chase-checking", token)
        assertTrue(LinkPolicy.accepts(token!!))
        assertFalse(PiiPolicy.storesRawPasswords)
        app.disconnectAll()
        assertNull(vault.read("institution.chase-checking"))
        assertFalse(app.accounts().linked)
    }

    @Test
    fun onboardingBoardsMatchCopyAndCtas() {
        val vault = MemoryTokenVault()
        val app = AiCfoController(vault, MemoryLocalStore(), MutableClock(10L))
        val welcome = app.onboarding()
        assertEquals("FINWISE", welcome.kicker)
        assertEquals("Your AI CFO", welcome.descriptor)
        assertEquals("Finwise — Your AI CFO", app.settings().brandTagline)
        assertEquals("Finwise — Your AI CFO", app.lock().brand)
        assertEquals("Your money,\nwhat to do next", welcome.title)
        assertEquals("Continue", welcome.primaryCta)
        assertEquals("", welcome.secondaryCta)

        app.primaryOnboarding()
        val value = app.onboarding()
        assertEquals("ACTIONS, NOT CHARTS", value.kicker)
        assertEquals(3, value.cardCount())
        assertEquals("Cancel unused Gympass", value.cardAt(0).title)
        assertEquals("+\$47/mo", value.cardAt(0).impact)
        assertEquals("Park idle cash in HYSA", value.cardAt(2).title)

        app.primaryOnboarding()
        assertEquals(2, app.onboarding().step)
        app.secondaryOnboarding()
        assertEquals(3, app.onboarding().step)
        assertNull(vault.read("institution.chase-checking"))
        assertFalse(app.accounts().linked)

        val trial = app.onboarding()
        assertEquals("30 days free · Pro", trial.badge)
        assertEquals("Start free 30-day trial", trial.primaryCta)
        assertEquals("Maybe later", trial.secondaryCta)
        assertEquals(4, trial.featureCount())
        assertEquals("Then paywall", trial.chipAt(2))
        app.secondaryOnboarding()
        assertEquals(Gate.PAYWALL, app.gate())
        assertEquals(Phase.PAYWALL, app.settings().phase)
    }

    @Test
    fun connectSecurelyLinksThenContinues() {
        val vault = MemoryTokenVault()
        val app = AiCfoController(vault, MemoryLocalStore(), MutableClock(10L))
        app.advanceOnboarding()
        app.advanceOnboarding()
        app.primaryOnboarding()
        assertEquals(3, app.onboarding().step)
        assertEquals("link_stub_chase-checking", vault.read("institution.chase-checking"))
        assertTrue(app.accounts().linked)
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
    fun releaseBuildCannotForceTrialOrPaywall() {
        val store = MemoryLocalStore()
        store.write("qa_override", QaOverride.PAYWALL)
        val app = releaseApp(MutableClock(10L), store)
        assertFalse(Qa.toolsEnabled(false))
        assertFalse(app.settings().qaEnabled)
        finishOnboarding(app)
        assertEquals(Phase.TRIAL, app.settings().phase)
        assertEquals(Gate.APP, app.gate())

        app.debugForcePaywall()
        app.debugForceTrial()
        app.debugForcePro()
        app.debugClearOverride()
        app.debugReplayOnboarding()

        assertEquals(QaOverride.PAYWALL, store.read("qa_override"))
        assertEquals(Phase.TRIAL, app.settings().phase)
        assertEquals(Gate.APP, app.gate())
    }

    @Test
    fun debugBuildLeavesBiometricOffUntilTheUserOptsIn() {
        val store = MemoryLocalStore()
        val app = AiCfoController(MemoryTokenVault(), store, MutableClock(10L))
        finishOnboarding(app)
        assertFalse(app.settings().biometricEnabled)
        val restarted = AiCfoController(MemoryTokenVault(), store, MutableClock(10L))
        assertFalse(restarted.settings().biometricEnabled)
        assertEquals(Gate.APP, restarted.gate())
        restarted.setBiometricEnabled(true)
        assertTrue(restarted.settings().biometricEnabled)
        restarted.lockNow()
        assertEquals(Gate.LOCK, restarted.gate())
    }

    @Test
    fun releaseBuildLocksWhenBiometricPreferenceIsUnset() {
        val store = MemoryLocalStore()
        val app = releaseApp(MutableClock(10L), store)
        finishOnboarding(app)
        assertTrue(app.settings().biometricEnabled)
        val restarted = releaseApp(MutableClock(10L), store)
        assertTrue(restarted.settings().biometricEnabled)
        assertEquals(Gate.LOCK, restarted.gate())
        restarted.setBiometricEnabled(false)
        val optedOut = releaseApp(MutableClock(10L), store)
        assertFalse(optedOut.settings().biometricEnabled)
        assertEquals(Gate.APP, optedOut.gate())
    }

    @Test
    fun lastMainTabSurvivesANewController() {
        val store = MemoryLocalStore()
        val app = AiCfoController(MemoryTokenVault(), store, MutableClock(10L))
        app.selectTab("ACCOUNTS")
        assertEquals("ACCOUNTS", app.tab())
        val restarted = AiCfoController(MemoryTokenVault(), store, MutableClock(10L))
        assertEquals("ACCOUNTS", restarted.tab())
        restarted.selectTab("NOT_A_TAB")
        assertEquals("ACCOUNTS", restarted.tab())
        restarted.selectTab("SETTINGS")
        assertEquals("SETTINGS", AiCfoController(MemoryTokenVault(), store, MutableClock(10L)).tab())
    }

    @Test
    fun connectFailureDoesNotCrashOrAdvance() {
        val vault = FailSecondVault()
        val app = AiCfoController(vault, MemoryLocalStore(), MutableClock(10L))
        app.advanceOnboarding()
        app.advanceOnboarding()
        assertEquals(2, app.onboarding().step)
        app.primaryOnboarding()
        assertEquals(2, app.onboarding().step)
        assertFalse(app.accounts().linked)
        assertTrue(app.onboarding().linkError.contains("Nothing was saved"))
        assertTrue(app.accounts().linkError.contains("Nothing was saved"))
        assertTrue(vault.cleared)
        assertNull(vault.read("institution.chase-checking"))
    }

    @Test
    fun connectVaultThrowStaysUnlinked() {
        val app = AiCfoController(ThrowingVault(), MemoryLocalStore(), MutableClock(10L))
        app.advanceOnboarding()
        app.advanceOnboarding()
        assertFalse(app.connectReadOnlyStub())
        assertEquals(2, app.onboarding().step)
        assertFalse(app.accounts().linked)
        assertTrue(app.accounts().linkError.isNotEmpty())
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

    private fun releaseApp(clock: MutableClock, store: MemoryLocalStore = MemoryLocalStore()): AiCfoController {
        return AiCfoController(
            MemoryTokenVault(),
            store,
            clock,
            Markets.unitedStates(),
            EmptyLocalStrings,
            false,
        )
    }

    private fun finishOnboarding(app: AiCfoController) {
        repeat(8) {
            if (app.gate() != Gate.ONBOARDING) return
            app.primaryOnboarding()
        }
        assertEquals(Gate.APP, app.gate())
    }
}

private class MutableClock(var now: Long) : AppClock {
    override fun nowEpochMs(): Long = now
}

private class FailSecondVault : TokenVault {
    private val values = mutableMapOf<String, String>()
    private var puts = 0
    var cleared: Boolean = false

    override fun put(key: String, value: String): Boolean {
        puts += 1
        if (puts >= 2) return false
        values[key] = value
        return true
    }

    override fun read(key: String): String? = values[key]

    override fun clear() {
        cleared = true
        values.clear()
    }
}

private class ThrowingVault : TokenVault {
    override fun put(key: String, value: String): Boolean {
        throw IllegalStateException("keystore")
    }

    override fun read(key: String): String? = null

    override fun clear() = Unit
}
