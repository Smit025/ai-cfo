package com.aicfo.shared.domain

import com.aicfo.shared.data.MayaStub
import com.aicfo.shared.market.CopyResolver
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.LocalStrings
import com.aicfo.shared.market.MarketCalendar
import com.aicfo.shared.market.MarketPack
import com.aicfo.shared.market.MarketSnapshot
import com.aicfo.shared.market.Markets
import com.aicfo.shared.market.MoneyFormat
import com.aicfo.shared.market.snapshot
import com.aicfo.shared.model.MoveKind
import com.aicfo.shared.model.wire
import com.aicfo.shared.presentation.AccountsModel
import com.aicfo.shared.presentation.DetailModel
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.presentation.HomeModel
import com.aicfo.shared.presentation.LockModel
import com.aicfo.shared.presentation.MovesModel
import com.aicfo.shared.presentation.OnboardingModel
import com.aicfo.shared.presentation.PaywallModel
import com.aicfo.shared.presentation.Phase
import com.aicfo.shared.presentation.QaOverride
import com.aicfo.shared.presentation.SettingsModel
import com.aicfo.shared.security.LinkPolicy
import com.aicfo.shared.security.LocalStore
import com.aicfo.shared.security.SafeLog
import com.aicfo.shared.security.TokenVault

interface AppObserver {
    fun onChanged()
}

/**
 * Single entry the native Android and iOS apps call.
 * Domain rules, the Maya stub, and entitlement live here — not in the UIs.
 */
class AiCfoController(
    private val vault: TokenVault,
    private val store: LocalStore,
    private val clock: AppClock,
    private val market: MarketPack,
    private val localStrings: LocalStrings,
) {
    /** US pack, shared English catalog. Platform string tables can override keys. */
    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
    ) : this(vault, store, clock, Markets.unitedStates(), EmptyLocalStrings)

    private val copy = CopyResolver(market.copy, localStrings)
    private val observers = mutableListOf<AppObserver>()
    private val statuses = mutableMapOf<String, String>()
    private val notes = mutableMapOf<String, String>()
    private var onboardingStep: Int = 0
    private var sessionUnlocked: Boolean = false
    private var biometricHardware: Boolean = false
    private var selectedMoveId: String? = null
    private var tab: String = "HOME"

    init {
        load()
    }

    fun addObserver(observer: AppObserver) {
        observers += observer
    }

    fun removeObserver(observer: AppObserver) {
        observers -= observer
    }

    fun gate(): String {
        if (!onboardingComplete()) return Gate.ONBOARDING
        if (biometricEnabled() && !sessionUnlocked) return Gate.LOCK
        if (entitlement().phase == Phase.PAYWALL) return Gate.PAYWALL
        return Gate.APP
    }

    fun tab(): String = tab

    fun selectTab(value: String) {
        tab = value
        publish()
    }

    fun selectedMoveId(): String? = selectedMoveId

    fun selectMove(id: String) {
        selectedMoveId = id
        publish()
    }

    fun clearMoveSelection() {
        selectedMoveId = null
        publish()
    }

    fun resolvedMoveId(): String {
        val current = selectedMoveId
        if (current != null && MayaStub.moves.any { it.id == current }) return current
        val rows = resolved()
        return rows.firstOrNull { it.status == "TODO" }?.move?.id ?: rows.first().move.id
    }

    fun market(): MarketSnapshot = market.snapshot()

    fun planMonthIsNow(): Boolean = MarketCalendar.isMonth(
        clock.nowEpochMs(),
        market.config.timeZoneId,
        market.config.planYear,
        market.config.planMonth,
    )

    fun onboarding(): OnboardingModel = OnboardingUseCase.build(onboardingStep, banksLinked(), market, copy)

    fun advanceOnboarding() {
        if (!onboarding().canAdvance) return
        if (onboardingStep >= 3) {
            completeOnboarding(startTrial = true)
        } else {
            onboardingStep += 1
            store.write(Keys.STEP, onboardingStep.toString())
            publish()
        }
    }

    /** Primary button. Connect securely links the read-only sample, then continues. */
    fun primaryOnboarding() {
        if (onboardingStep == 2) {
            connectReadOnlyStub()
        }
        advanceOnboarding()
    }

    /**
     * Skip for now leaves institutions unlinked.
     * Maybe later finishes onboarding without starting a trial, so the paywall shows.
     */
    fun secondaryOnboarding() {
        when (onboardingStep) {
            2 -> advanceOnboarding()
            3 -> completeOnboarding(startTrial = false)
        }
    }

    fun backOnboarding() {
        if (onboardingStep <= 0) return
        onboardingStep -= 1
        store.write(Keys.STEP, onboardingStep.toString())
        publish()
    }

    fun connectReadOnlyStub(): Boolean {
        MayaStub.accounts.forEach { account ->
            val token = "link_stub_${account.id}"
            check(LinkPolicy.accepts(token))
            val stored = vault.put("institution.${account.id}", token)
            check(stored)
        }
        store.write(Keys.BANKS, "true")
        SafeLog.debug("link", "read-only sample linked")
        publish()
        return true
    }

    fun disconnectAll() {
        vault.clear()
        store.write(Keys.BANKS, "false")
        publish()
    }

    fun home(): HomeModel = HomeUseCase.build(resolved(), notificationsEnabled(), market, copy)

    fun moves(): MovesModel = MovesUseCase.build(resolved(), market, copy)

    fun detail(id: String): DetailModel? {
        val row = resolved().firstOrNull { it.move.id == id } ?: return null
        return DetailUseCase.build(row)
    }

    fun accounts(): AccountsModel = AccountsUseCase.build(banksLinked(), copy)

    fun settings(): SettingsModel {
        val ent = entitlement()
        val profile = MayaStub.profile
        return SettingsModel(
            name = profile.fullName,
            meta = "${profile.occupation} · ${profile.city}, ${profile.region}",
            initials = profile.initials,
            notificationsEnabled = notificationsEnabled(),
            biometricEnabled = biometricEnabled(),
            biometricHardware = biometricHardware,
            planLabel = ent.planLabel,
            planDetail = ent.planDetail,
            banksLinked = banksLinked(),
            phase = ent.phase,
            qaEnabled = Qa.toolsEnabled,
        )
    }

    fun lock(): LockModel {
        val hardware = biometricHardware
        return LockModel(
            title = "Unlock AI CFO",
            body = if (hardware) {
                "Your moves stay on this phone. Confirm it's you to open the coach."
            } else {
                "This device has no biometric hardware. The gate is still on. Continue to open the sample coach. On a phone with Face ID or a fingerprint, this is a real biometric prompt."
            },
            primaryCta = if (hardware) "Unlock with biometrics" else "Continue without biometrics",
            hardwareAvailable = hardware,
        )
    }

    fun paywall(): PaywallModel = PaywallUseCase.build(
        entitlement().trialConsumed,
        MoneyFormat.standard(market.monthlyPrice),
        MoneyFormat.standard(market.yearlyPrice),
    )

    fun performPrimary(id: String): String {
        val move = MayaStub.moves.firstOrNull { it.id == id } ?: return ""
        val message = when (move.kind) {
            MoveKind.CANCEL_SUBSCRIPTION -> {
                setStatus(id, "DONE")
                "Cancel page opened (stub). This move is marked done."
            }
            MoveKind.EXTRA_DEBT_PAYMENT -> {
                "Extra payment scheduled for Friday (stub). AI CFO still won't move the money."
            }
            MoveKind.MOVE_IDLE_CASH -> {
                "Transfer guide opened (stub). You move the cash at your bank."
            }
            MoveKind.PAY_RENT -> {
                setStatus(id, "DONE")
                "Rent is marked done for this month."
            }
            MoveKind.REFINANCE_CHECK -> {
                setStatus(id, "TODO")
                "Refinance check reopened."
            }
        }
        setNote(id, message)
        publish()
        return message
    }

    fun performSecondary(id: String): String {
        val move = MayaStub.moves.firstOrNull { it.id == id } ?: return ""
        val message = when (move.kind) {
            MoveKind.CANCEL_SUBSCRIPTION -> {
                setStatus(id, "SKIPPED")
                "Kept Gympass. This move is skipped for the month."
            }
            MoveKind.EXTRA_DEBT_PAYMENT -> "Reminder set for Friday."
            MoveKind.MOVE_IDLE_CASH -> "Reminder set."
            MoveKind.PAY_RENT -> "Reminder set for next month."
            MoveKind.REFINANCE_CHECK -> {
                setStatus(id, "SKIPPED")
                "Left skipped. Federal protections stay in place."
            }
        }
        setNote(id, message)
        publish()
        return message
    }

    fun moveStatus(id: String): String {
        val move = MayaStub.moves.first { it.id == id }
        return statuses[id] ?: move.defaultStatus.wire()
    }

    fun setNotifications(enabled: Boolean) {
        store.write(Keys.NOTIFICATIONS, if (enabled) "true" else "false")
        publish()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        store.write(Keys.BIOMETRIC, if (enabled) "true" else "false")
        if (!enabled) sessionUnlocked = true
        publish()
    }

    fun setBiometricHardware(available: Boolean) {
        if (biometricHardware == available) return
        biometricHardware = available
        publish()
    }

    fun unlockFromBiometric(success: Boolean) {
        if (!success) return
        sessionUnlocked = true
        publish()
    }

    fun unlockWithoutHardware() {
        if (biometricHardware) return
        sessionUnlocked = true
        publish()
    }

    fun lockNow() {
        if (!biometricEnabled()) return
        sessionUnlocked = false
        publish()
    }

    fun purchaseMonthly(): String = subscribe("MONTHLY")

    fun purchaseYearly(): String = subscribe("YEARLY")

    fun debugForcePaywall() = setOverride(QaOverride.PAYWALL)

    fun debugForceTrial() = setOverride(QaOverride.TRIAL)

    fun debugForcePro() = setOverride(QaOverride.PRO)

    fun debugClearOverride() = setOverride(QaOverride.NONE)

    fun debugReplayOnboarding() {
        store.write(Keys.ONBOARDING, "false")
        store.write(Keys.STEP, "0")
        onboardingStep = 0
        sessionUnlocked = false
        publish()
    }

    private fun subscribe(plan: String): String {
        store.write(Keys.PLAN, plan)
        store.write(Keys.OVERRIDE, QaOverride.NONE)
        publish()
        val price = if (plan == "YEARLY") market.yearlyPrice else market.monthlyPrice
        val period = if (plan == "YEARLY") "year" else "month"
        return "Subscribed · ${MoneyFormat.standard(price)}/$period (simulated)."
    }

    private fun setOverride(code: String) {
        store.write(Keys.OVERRIDE, code)
        publish()
    }

    private fun completeOnboarding(startTrial: Boolean) {
        store.write(Keys.ONBOARDING, "true")
        if (startTrial && store.read(Keys.TRIAL_START).isNullOrBlank()) {
            store.write(Keys.TRIAL_START, clock.nowEpochMs().toString())
        }
        sessionUnlocked = true
        publish()
    }

    private fun entitlement() = EntitlementPolicy.resolve(
        nowMs = clock.nowEpochMs(),
        trialStartedAtMs = store.read(Keys.TRIAL_START)?.toLongOrNull(),
        overrideCode = store.read(Keys.OVERRIDE) ?: QaOverride.NONE,
        subscribedPlan = store.read(Keys.PLAN) ?: "NONE",
        monthlyLabel = MoneyFormat.standard(market.monthlyPrice),
        yearlyLabel = MoneyFormat.standard(market.yearlyPrice),
    )

    private fun onboardingComplete(): Boolean = store.read(Keys.ONBOARDING) == "true"

    private fun banksLinked(): Boolean = store.read(Keys.BANKS) == "true"

    private fun notificationsEnabled(): Boolean = store.read(Keys.NOTIFICATIONS) != "false"

    private fun biometricEnabled(): Boolean = store.read(Keys.BIOMETRIC) != "false"

    private fun resolved(): List<ResolvedMove> = MayaStub.moves.map { move ->
        ResolvedMove(
            move = move,
            status = statuses[move.id] ?: move.defaultStatus.wire(),
            note = notes[move.id].orEmpty(),
        )
    }

    private fun setStatus(id: String, status: String) {
        statuses[id] = status
        persistStatuses()
    }

    private fun setNote(id: String, note: String) {
        notes[id] = note
        persistNotes()
    }

    private fun load() {
        onboardingStep = store.read(Keys.STEP)?.toIntOrNull()?.coerceIn(0, 3) ?: 0
        val rawStatus = store.read(Keys.STATUSES).orEmpty()
        if (rawStatus.isNotEmpty()) {
            rawStatus.split(';').forEach { part ->
                val bits = part.split(':')
                if (bits.size == 2 && bits[0].isNotEmpty()) statuses[bits[0]] = bits[1]
            }
        }
        val rawNotes = store.read(Keys.NOTES).orEmpty()
        if (rawNotes.isNotEmpty()) {
            rawNotes.split('\u001e').forEach { part ->
                val bits = part.split('\u001f', limit = 2)
                if (bits.size == 2) notes[bits[0]] = bits[1]
            }
        }
    }

    private fun persistStatuses() {
        val encoded = statuses.entries.joinToString(";") { "${it.key}:${it.value}" }
        store.write(Keys.STATUSES, encoded)
    }

    private fun persistNotes() {
        val encoded = notes.entries.joinToString("\u001e") { entry ->
            val clean = entry.value.replace('\u001e', ' ').replace('\u001f', ' ')
            "${entry.key}\u001f$clean"
        }
        store.write(Keys.NOTES, encoded)
    }

    private fun publish() {
        observers.toList().forEach { it.onChanged() }
    }
}

object Qa {
    const val toolsEnabled: Boolean = true
}

private object Keys {
    const val ONBOARDING = "onboarding_complete"
    const val STEP = "onboarding_step"
    const val BANKS = "banks_linked"
    const val TRIAL_START = "trial_started_at"
    const val OVERRIDE = "qa_override"
    const val PLAN = "subscribed_plan"
    const val NOTIFICATIONS = "notifications"
    const val BIOMETRIC = "biometric_enabled"
    const val STATUSES = "move_statuses"
    const val NOTES = "move_notes"
}
