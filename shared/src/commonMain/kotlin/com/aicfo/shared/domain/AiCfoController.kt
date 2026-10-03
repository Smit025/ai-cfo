package com.aicfo.shared.domain

import com.aicfo.shared.auth.AppGate
import com.aicfo.shared.auth.AuthSession
import com.aicfo.shared.auth.DeviceUnlockPrefs
import com.aicfo.shared.auth.PhoneNumbers
import com.aicfo.shared.auth.PinSecret
import com.aicfo.shared.auth.ReportEmail
import com.aicfo.shared.auth.SecureKeys
import com.aicfo.shared.auth.StubOtpAuthRepository
import com.aicfo.shared.auth.UnconfiguredOtpAuthRepository
import com.aicfo.shared.auth.OtpAuthRepository
import com.aicfo.shared.data.MayaStub
import com.aicfo.shared.market.CopyKey
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
import com.aicfo.shared.presentation.AuthStep
import com.aicfo.shared.presentation.DetailModel
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
import com.aicfo.shared.security.MemorySecureStore
import com.aicfo.shared.security.SafeLog
import com.aicfo.shared.security.SecureStore
import com.aicfo.shared.security.TokenVault
import com.aicfo.shared.sync.AccountSnapshot
import com.aicfo.shared.sync.BankFetch
import com.aicfo.shared.sync.BankLinkSource
import com.aicfo.shared.sync.Freshness
import com.aicfo.shared.sync.MayaStubBankSource
import com.aicfo.shared.sync.ProviderTransaction
import com.aicfo.shared.sync.SyncCode
import com.aicfo.shared.sync.SyncLine
import com.aicfo.shared.sync.SyncStatus
import com.aicfo.shared.sync.SyncTrigger
import com.aicfo.shared.sync.SyncedAccount
import com.aicfo.shared.sync.TransactionLedger

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
    private val debugBuild: Boolean,
    private val secure: SecureStore,
) {
    private val otp: OtpAuthRepository =
        if (debugBuild) StubOtpAuthRepository() else UnconfiguredOtpAuthRepository()

    /**
     * US pack, shared English catalog, debug QA tools on.
     * Release apps must use the full constructor and pass `debugBuild = false`.
     */
    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
    ) : this(vault, store, clock, Markets.unitedStates(), EmptyLocalStrings, true, MemorySecureStore())

    /** Debug/test helper. Release apps must pass `debugBuild = false` and a real [SecureStore]. */
    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
        market: MarketPack,
        localStrings: LocalStrings,
    ) : this(vault, store, clock, market, localStrings, true, MemorySecureStore())

    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
        market: MarketPack,
        localStrings: LocalStrings,
        debugBuild: Boolean,
    ) : this(
        vault,
        store,
        clock,
        market,
        localStrings,
        debugBuild,
        if (debugBuild) MemorySecureStore() else releaseNeedsRealSecureStore(),
    )

    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
        secure: SecureStore,
    ) : this(vault, store, clock, Markets.unitedStates(), EmptyLocalStrings, true, secure)

    /**
     * Debug helper with a [BankLinkSource] other than the Maya stub.
     * Release builds must use the overload that also passes a [SecureStore].
     */
    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
        market: MarketPack,
        localStrings: LocalStrings,
        debugBuild: Boolean,
        banks: BankLinkSource,
    ) : this(vault, store, clock, market, localStrings, debugBuild) {
        this.banks = banks
    }

    /**
     * Release path for a live bank link: real secure storage and a [BankLinkSource].
     * The live Plaid port implements [BankLinkSource] and passes it here.
     */
    constructor(
        vault: TokenVault,
        store: LocalStore,
        clock: AppClock,
        market: MarketPack,
        localStrings: LocalStrings,
        debugBuild: Boolean,
        secure: SecureStore,
        banks: BankLinkSource,
    ) : this(vault, store, clock, market, localStrings, debugBuild, secure) {
        this.banks = banks
    }

    private val copy = CopyResolver(market.copy, localStrings)
    private val observers = mutableListOf<AppObserver>()
    private val statuses = mutableMapOf<String, String>()
    private val notes = mutableMapOf<String, String>()
    private var onboardingStep: Int = 0
    private var deviceUnlocked: Boolean = false
    private var biometricHardware: Boolean = false
    private var pendingPhone: String? = null
    private var otpSentAtMs: Long = 0L
    private var phoneError: String = ""
    private var otpError: String = ""
    private var emailError: String = ""
    private var pinError: String = ""
    private var selectedMoveId: String? = null
    private var tab: String = "HOME"
    private var linkError: String = ""
    private var linkConfigured: Boolean = true
    private var linkUnavailableLabel: String = "Plaid is not configured"
    private var linkNote: String = ""
    private var banks: BankLinkSource = MayaStubBankSource()
    private val mayaSample: BankLinkSource = MayaStubBankSource()
    private val ledger = TransactionLedger(store)
    private var syncStatus: SyncStatus = SyncStatus.Idle
    private var lastSyncedAt: Long? = null
    private var syncedAccounts: List<SyncedAccount> = emptyList()
    private var lastTrigger: SyncTrigger? = null
    private var syncGeneration: Int = 0

    init {
        load()
    }

    fun addObserver(observer: AppObserver) {
        observers += observer
    }

    fun removeObserver(observer: AppObserver) {
        observers -= observer
    }

    fun gate(): String = AppGate.resolve(
        hasSession = hasSession(),
        introComplete = introComplete(),
        authSetupComplete = authSetupComplete(),
        onboardingComplete = onboardingComplete(),
        deviceUnlockNeeded = deviceUnlockNeeded(),
        entitled = entitlement().phase != Phase.PAYWALL,
    )

    fun hasSession(): Boolean = readSession() != null

    fun sessionPhone(): String = readSession()?.phoneE164.orEmpty()

    fun authStep(): String {
        if (!hasSession()) return if (pendingPhone != null) AuthStep.OTP else AuthStep.PHONE
        if (!emailDecided()) return AuthStep.EMAIL
        if (!authSetupComplete()) return AuthStep.UNLOCK
        return AuthStep.DONE
    }

    fun phoneError(): String = phoneError

    fun otpError(): String = otpError

    fun emailError(): String = emailError

    fun pinError(): String = pinError

    fun maskedPhone(): String {
        val phone = pendingPhone ?: readSession()?.phoneE164 ?: return ""
        return PhoneNumbers.maskTight(phone)
    }

    fun resendSeconds(): Int {
        if (otpSentAtMs == 0L) return 0
        val elapsed = ((clock.nowEpochMs() - otpSentAtMs) / 1000L).toInt()
        return (RESEND_SEC - elapsed).coerceAtLeast(0)
    }

    fun reportEmail(): String = store.read(Keys.REPORT_EMAIL).orEmpty()

    fun debugAuthTools(): Boolean = Qa.toolsEnabled(debugBuild)

    /** Debug builds may show this on the phone screen. Empty in release. */
    fun debugOtpCode(): String = if (debugBuild) StubOtpAuthRepository.DEBUG_CODE else ""

    fun deviceUnlock(): DeviceUnlockPrefs = DeviceUnlockPrefs(
        biometricEnabled = biometricEnabled(),
        pinSet = pinConfigured(),
        passcodeFallback = passcodeConfigured(),
        setupComplete = authSetupComplete(),
    )

    fun tab(): String = tab

    fun selectTab(value: String) {
        if (value !in MainTabs) return
        tab = value
        store.write(Keys.TAB, value)
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

    fun onboarding(): OnboardingModel = OnboardingUseCase.build(
        displayOnboardingStep(),
        banksLinked(),
        market,
        copy,
        linkError,
        linkConfigured,
        linkUnavailableLabel,
        linkNote,
    )

    fun advanceOnboarding() {
        if (!onboarding().canAdvance) return
        linkError = ""
        when {
            onboardingStep < 2 -> {
                onboardingStep += 1
                store.write(Keys.STEP, onboardingStep.toString())
                publish()
            }
            !introComplete() -> finishIntro()
            else -> completeOnboarding(startTrial = true)
        }
    }

    /**
     * Primary button for welcome, the value step, and the trial.
     * The connect step does not link a bank here. Android opens Plaid Link.
     * iOS shows that Link is not available yet. Neither path uses the Maya sample.
     */
    fun primaryOnboarding() {
        if (onboardingStep == 2 && !introComplete() && !banksLinked()) {
            if (linkError.isBlank()) linkError = "Nothing was linked."
            publish()
            return
        }
        advanceOnboarding()
    }

    /**
     * Android and iOS call this before the first frame.
     * When [configured] is false, Connect says [unavailableLabel] and must not link the Maya sample.
     */
    fun setBankLinkAvailability(configured: Boolean, unavailableLabel: String, note: String) {
        linkConfigured = configured
        linkUnavailableLabel = unavailableLabel.ifBlank { "Plaid is not configured" }
        linkNote = note
        publish()
    }

    fun reportLinkError(message: String) {
        linkError = SafeLog.redact(message).take(180).ifBlank { "Couldn't link these accounts. Nothing was saved." }
        publish()
    }

    fun clearLinkError() {
        if (linkError.isEmpty()) return
        linkError = ""
        publish()
    }

    /**
     * Skip for now leaves institutions unlinked and finishes the intro.
     * Maybe later finishes the trial step without starting a trial, so the paywall shows.
     */
    fun secondaryOnboarding() {
        when {
            onboardingStep == 2 && !introComplete() -> finishIntro()
            introComplete() && authSetupComplete() && !onboardingComplete() ->
                completeOnboarding(startTrial = false)
        }
    }

    fun backOnboarding() {
        if (introComplete() && authSetupComplete() && !onboardingComplete()) return
        if (onboardingStep <= 0) return
        onboardingStep -= 1
        store.write(Keys.STEP, onboardingStep.toString())
        publish()
    }

    /**
     * Debug-only Maya Chen sample. Release builds ignore it.
     * A policy or vault failure does not crash and does not mark institutions linked.
     */
    fun connectReadOnlyStub(): Boolean {
        if (!Qa.toolsEnabled(debugBuild)) return false
        for (account in MayaStub.accounts) {
            val token = "link_stub_${account.id}"
            val stored = try {
                LinkPolicy.accepts(token) && vault.put("institution.${account.id}", token)
            } catch (_: Throwable) {
                false
            }
            if (!stored) {
                try {
                    vault.clear()
                } catch (_: Throwable) {
                    // The failure is still reported below.
                }
                store.write(Keys.BANKS, "false")
                linkError = "Couldn't link these accounts. Nothing was saved."
                publish()
                return false
            }
        }
        store.write(Keys.BANKS, "true")
        store.write(Keys.LINK_KIND, MayaStubBankSource.ID)
        linkError = ""
        SafeLog.debug("link", "debug sample linked")
        refreshAccounts(SyncTrigger.Manual)
        return true
    }

    /**
     * The platform already stored a read-only access token in the vault.
     * Marks the link and refreshes. Does not read the token and does not move money.
     */
    fun completeExternalReadOnlyLink(advanceIntro: Boolean): Boolean {
        if (!banks.readOnly) {
            reportLinkError("Bank link refused: read-only connections only")
            return false
        }
        store.write(Keys.LINK_KIND, banks.id)
        store.write(Keys.BANKS, "true")
        store.write(Keys.NEEDS_REAUTH, "false")
        linkError = ""
        SafeLog.debug("link", "read-only ${banks.id} linked")
        refreshAccounts(SyncTrigger.Manual)
        if (advanceIntro && onboardingStep == 2 && !introComplete()) {
            finishIntro()
        }
        return true
    }

    fun activeBankLinkId(): String = activeSource().id

    fun disconnectAll() {
        syncGeneration += 1
        try {
            banks.onDisconnected()
        } catch (_: Throwable) {
            // Unlink still clears local metadata.
        }
        vault.clear()
        store.write(Keys.BANKS, "false")
        store.remove(Keys.LINK_KIND)
        clearSync()
        publish()
    }

    /**
     * Refresh balances and transactions. Read-only: this never moves money.
     * Status moves to [SyncStatus.Syncing], then to success, failure, or re-auth.
     * A source may deliver after this call returns; a newer refresh cancels the older delivery.
     */
    fun refreshAccounts(reason: SyncTrigger) {
        lastTrigger = reason
        if (!banksLinked()) {
            if (syncStatus !is SyncStatus.Idle || lastSyncedAt != null || syncedAccounts.isNotEmpty()) {
                clearSync()
                publish()
            }
            return
        }
        syncGeneration += 1
        val ticket = syncGeneration
        syncStatus = SyncStatus.Syncing
        publish()
        if (!activeSource().readOnly) {
            finish(ticket, BankFetch.Unavailable("Bank link refused: read-only connections only"))
            return
        }
        if (store.read(Keys.NEEDS_REAUTH) == "true") {
            finish(ticket, BankFetch.LoginRequired)
            return
        }
        val source = activeSource()
        try {
            source.fetch(clock.nowEpochMs()) { result ->
                finish(ticket, result)
            }
        } catch (error: Throwable) {
            if (ticket == syncGeneration && syncStatus is SyncStatus.Syncing) {
                finish(ticket, BankFetch.Unavailable(error.message ?: "Couldn't refresh"))
            }
        }
        SafeLog.debug("sync", "refresh ${reason.name} ${syncStatus.code()}")
    }

    fun syncStatus(): SyncStatus = this.syncStatus

    fun lastSyncTrigger(): SyncTrigger? = lastTrigger

    fun ingestedTransactionCount(): Int = ledger.count()

    fun ingestedTransaction(providerTransactionId: String): ProviderTransaction? =
        ledger.find(providerTransactionId)

    /** Clears a re-auth flag and refreshes. Does not move money or charge a card. */
    fun reconnectBank() {
        if (!banksLinked()) return
        store.write(Keys.NEEDS_REAUTH, "false")
        refreshAccounts(SyncTrigger.Manual)
    }

    /** Debug/QA only. Leaves balances visible and marks them as needing reconnect. */
    fun debugSimulateNeedsReauth() {
        if (!Qa.toolsEnabled(debugBuild)) return
        syncGeneration += 1
        store.write(Keys.NEEDS_REAUTH, "true")
        syncStatus = SyncStatus.NeedsReauth
        persistSync()
        publish()
    }

    /** Debug/QA only. Leaves the last successful timestamp and marks the sync failed. */
    fun debugSimulateSyncFailure() {
        if (!Qa.toolsEnabled(debugBuild)) return
        syncGeneration += 1
        store.write(Keys.NEEDS_REAUTH, "false")
        syncStatus = SyncStatus.Failed("Couldn't refresh")
        persistSync()
        publish()
    }

    fun home(): HomeModel = HomeUseCase.build(
        resolved(),
        notificationsEnabled(),
        market,
        copy,
        syncLine(),
    )

    fun moves(): MovesModel = MovesUseCase.build(resolved(), market, copy)

    fun detail(id: String): DetailModel? {
        val row = resolved().firstOrNull { it.move.id == id } ?: return null
        return DetailUseCase.build(row)
    }

    fun accounts(): AccountsModel = AccountsUseCase.build(
        linked = banksLinked(),
        copy = copy,
        linkError = linkError,
        accounts = displayAccounts(),
        sync = syncLine(),
        linkConfigured = linkConfigured,
        unavailableLabel = linkUnavailableLabel,
        linkNote = linkNote,
        sampleLink = banksLinked() && activeSource().id == MayaStubBankSource.ID,
    )

    fun settings(): SettingsModel {
        val ent = entitlement()
        val profile = MayaStub.profile
        val session = readSession()
        val mask = session?.let { PhoneNumbers.maskSpaced(it.phoneE164) }.orEmpty()
        return SettingsModel(
            name = profile.fullName,
            meta = if (session != null) {
                "$mask · Signed in"
            } else {
                "${profile.occupation} · ${profile.city}, ${profile.region}"
            },
            initials = profile.initials,
            brandTagline = copy.text(CopyKey.BRAND_TAGLINE),
            notificationsEnabled = notificationsEnabled(),
            biometricEnabled = biometricEnabled(),
            biometricHardware = biometricHardware,
            planLabel = ent.planLabel,
            planDetail = ent.planDetail,
            banksLinked = banksLinked(),
            sampleLink = banksLinked() && activeSource().id == MayaStubBankSource.ID,
            phase = ent.phase,
            qaEnabled = Qa.toolsEnabled(debugBuild),
            signedIn = session != null,
            phoneMask = mask,
            deviceLockReady = biometricEnabled() || pinConfigured() || passcodeConfigured(),
        )
    }

    fun lock(): LockModel {
        val bio = biometricHardware && biometricEnabled()
        val pin = pinConfigured()
        val passcode = passcodeConfigured()
        val create = !bio && !pin && !passcode && !debugBuild
        val primary = when {
            bio -> "Unlock with biometrics"
            pin -> "Unlock with PIN"
            passcode -> "Use device passcode"
            create -> "Create a PIN"
            else -> "Continue without biometrics"
        }
        return LockModel(
            brand = copy.text(CopyKey.BRAND_TAGLINE),
            title = "Unlock Finwise",
            body = "You're still signed in. This check unlocks the device — not a new login.",
            primaryCta = primary,
            hardwareAvailable = bio,
            kicker = "WELCOME BACK",
            secondaryCta = if (bio) "Use PIN" else "",
            methodTitle = if (bio) "Biometrics" else "PIN",
            methodDetail = "Device unlock only · account stays via phone+OTP",
            pinSet = pin,
            mustCreatePin = create,
            preferPin = !bio && pin,
            passcodeFallback = passcode,
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
                "Extra payment scheduled for Friday (stub). Finwise still won't move the money."
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
        if (!enabled && debugBuild && !pinConfigured() && !passcodeConfigured()) {
            deviceUnlocked = true
        }
        publish()
    }

    fun setBiometricHardware(available: Boolean) {
        if (biometricHardware == available) return
        biometricHardware = available
        publish()
    }

    fun unlockFromBiometric(success: Boolean) {
        if (!success) return
        deviceUnlocked = true
        publish()
    }

    /** Device passcode succeeded. This is still device unlock, not a new account login. */
    fun unlockFromDevicePasscode(success: Boolean) {
        if (!success) return
        deviceUnlocked = true
        publish()
    }

    /**
     * Debug-only escape when the emulator has no biometric hardware and no PIN.
     * Release builds ignore this so the unlock gate cannot be empty.
     */
    fun unlockWithoutHardware() {
        if (!debugBuild) return
        if (biometricHardware) return
        if (pinConfigured() || passcodeConfigured()) return
        deviceUnlocked = true
        publish()
    }

    fun lockNow() {
        if (!biometricEnabled() && !pinConfigured() && !passcodeConfigured()) return
        deviceUnlocked = false
        publish()
    }

    fun submitPhone(raw: String): Boolean {
        val e164 = PhoneNumbers.toE164(raw)
        if (!PhoneNumbers.isValidUs(e164)) {
            phoneError = "Enter a valid US mobile number."
            publish()
            return false
        }
        return sendCode(e164)
    }

    fun resendOtp(): Boolean {
        val phone = pendingPhone ?: return false
        if (resendSeconds() > 0) return false
        return sendCode(phone)
    }

    fun changePhoneNumber() {
        pendingPhone = null
        otpError = ""
        otpSentAtMs = 0L
        publish()
    }

    fun verifyOtp(code: String): Boolean {
        val phone = pendingPhone
        if (phone == null) {
            otpError = "Request a new code."
            publish()
            return false
        }
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) {
            otpError = "Enter the 6-digit code."
            publish()
            return false
        }
        val result = otp.verifyCode(phone, digits)
        if (!result.ok) {
            otpError = result.message.ifBlank { "That code is wrong or expired." }
            publish()
            return false
        }
        if (!writeSession(phone)) {
            otpError = "Couldn't save this sign-in on the device."
            publish()
            return false
        }
        pendingPhone = null
        otpError = ""
        phoneError = ""
        publish()
        return true
    }

    /** Debug-only. Skips phone + OTP and persists a session. Email and unlock setup still follow. */
    fun debugSkipPhone() {
        if (!Qa.toolsEnabled(debugBuild)) return
        if (!writeSession("+15555550100")) return
        pendingPhone = null
        phoneError = ""
        otpError = ""
        publish()
    }

    fun saveReportEmail(raw: String): Boolean {
        val email = raw.trim()
        if (!ReportEmail.accepts(email)) {
            emailError = "Enter a valid email."
            publish()
            return false
        }
        store.write(Keys.REPORT_EMAIL, email)
        store.write(Keys.EMAIL_DECIDED, "true")
        emailError = ""
        publish()
        return true
    }

    fun skipReportEmail() {
        store.write(Keys.EMAIL_DECIDED, "true")
        emailError = ""
        publish()
    }

    /** First-run: user turned biometrics on. Does not replace phone + OTP. */
    fun enableBiometricUnlock(): Boolean {
        store.write(Keys.BIOMETRIC, "true")
        finishUnlockSetup()
        return true
    }

    /** iOS device passcode counts as an unlock path. It is not an account login. */
    fun enablePasscodeUnlock(): Boolean {
        store.write(Keys.PASSCODE, "true")
        finishUnlockSetup()
        return true
    }

    fun saveDevicePin(pin: String, confirm: String): Boolean {
        if (pin.length != 6 || pin.any { !it.isDigit() }) {
            pinError = "Enter a 6-digit PIN."
            publish()
            return false
        }
        if (pin != confirm) {
            pinError = "Those PINs don't match."
            publish()
            return false
        }
        val sealed = PinSecret.seal(pin, clock.nowEpochMs().toString())
        if (!secure.put(SecureKeys.PIN, sealed)) {
            pinError = "Couldn't save that PIN on this device."
            publish()
            return false
        }
        store.write(Keys.PIN_SET, "true")
        pinError = ""
        finishUnlockSetup()
        return true
    }

    fun unlockWithPin(pin: String): Boolean {
        val sealed = secure.read(SecureKeys.PIN)
        if (sealed == null || !PinSecret.matches(pin, sealed)) {
            pinError = "Wrong PIN."
            publish()
            return false
        }
        pinError = ""
        deviceUnlocked = true
        publish()
        return true
    }

    /**
     * Debug QA can leave unlock setup without a PIN so emulators are not stuck.
     * Release builds ignore this. A release build still has to pick biometrics, PIN, or passcode.
     */
    fun debugCompleteUnlockSetup() {
        if (!Qa.toolsEnabled(debugBuild)) return
        finishUnlockSetup()
    }

    /** Clears the account session. Next cold start is phone + OTP. Link tokens stay until disconnect. */
    fun logOut() {
        secure.remove(SecureKeys.SESSION)
        secure.remove(SecureKeys.PIN)
        store.remove(Keys.PIN_SET)
        store.remove(Keys.AUTH_SETUP)
        store.remove(Keys.EMAIL_DECIDED)
        store.remove(Keys.REPORT_EMAIL)
        store.remove(Keys.PASSCODE)
        store.remove(Keys.BIOMETRIC)
        pendingPhone = null
        deviceUnlocked = false
        phoneError = ""
        otpError = ""
        emailError = ""
        pinError = ""
        publish()
    }

    /** Test helper. Writes a session and marks email + unlock setup done for this process. */
    internal fun testingSeedSession(phone: String = "+15555551234") {
        if (!writeSession(phone)) return
        store.write(Keys.EMAIL_DECIDED, "true")
        store.write(Keys.AUTH_SETUP, "true")
        deviceUnlocked = true
        if (introComplete() && !onboardingComplete()) {
            onboardingStep = 3
            store.write(Keys.STEP, "3")
        }
        publish()
    }

    fun purchaseMonthly(): String = subscribe("MONTHLY")

    fun purchaseYearly(): String = subscribe("YEARLY")

    fun debugForcePaywall() = setOverride(QaOverride.PAYWALL)

    fun debugForceTrial() = setOverride(QaOverride.TRIAL)

    fun debugForcePro() = setOverride(QaOverride.PRO)

    fun debugClearOverride() = setOverride(QaOverride.NONE)

    fun debugReplayOnboarding() {
        if (!Qa.toolsEnabled(debugBuild)) return
        store.write(Keys.ONBOARDING, "false")
        store.write(Keys.INTRO, "false")
        store.write(Keys.STEP, "0")
        onboardingStep = 0
        deviceUnlocked = false
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
        if (!Qa.toolsEnabled(debugBuild)) return
        store.write(Keys.OVERRIDE, code)
        publish()
    }

    private fun completeOnboarding(startTrial: Boolean) {
        store.write(Keys.ONBOARDING, "true")
        if (startTrial && store.read(Keys.TRIAL_START).isNullOrBlank()) {
            store.write(Keys.TRIAL_START, clock.nowEpochMs().toString())
        }
        deviceUnlocked = true
        publish()
    }

    private fun entitlement() = EntitlementPolicy.resolve(
        nowMs = clock.nowEpochMs(),
        trialStartedAtMs = store.read(Keys.TRIAL_START)?.toLongOrNull(),
        overrideCode = if (Qa.toolsEnabled(debugBuild)) {
            store.read(Keys.OVERRIDE) ?: QaOverride.NONE
        } else {
            QaOverride.NONE
        },
        subscribedPlan = store.read(Keys.PLAN) ?: "NONE",
        monthlyLabel = MoneyFormat.standard(market.monthlyPrice),
        yearlyLabel = MoneyFormat.standard(market.yearlyPrice),
    )

    private fun onboardingComplete(): Boolean = store.read(Keys.ONBOARDING) == "true"

    private fun introComplete(): Boolean =
        store.read(Keys.INTRO) == "true" || onboardingComplete()

    private fun authSetupComplete(): Boolean = store.read(Keys.AUTH_SETUP) == "true"

    private fun emailDecided(): Boolean = store.read(Keys.EMAIL_DECIDED) == "true"

    private fun pinConfigured(): Boolean =
        store.read(Keys.PIN_SET) == "true" && !secure.read(SecureKeys.PIN).isNullOrBlank()

    private fun passcodeConfigured(): Boolean = store.read(Keys.PASSCODE) == "true"

    private fun deviceUnlockNeeded(): Boolean {
        if (deviceUnlocked) return false
        if (!hasSession() || !authSetupComplete() || !onboardingComplete()) return false
        val path = biometricEnabled() || pinConfigured() || passcodeConfigured()
        if (debugBuild && !path) return false
        return true
    }

    private fun displayOnboardingStep(): Int = when {
        introComplete() && authSetupComplete() && !onboardingComplete() -> 3
        else -> onboardingStep.coerceIn(0, 2)
    }

    private fun finishIntro() {
        store.write(Keys.INTRO, "true")
        publish()
    }

    private fun sendCode(e164: String): Boolean {
        val result = otp.requestCode(e164)
        if (!result.ok) {
            phoneError = result.message.ifBlank { "Couldn't send a code." }
            publish()
            return false
        }
        pendingPhone = e164
        otpSentAtMs = clock.nowEpochMs()
        phoneError = ""
        otpError = ""
        publish()
        return true
    }

    private fun writeSession(phone: String): Boolean {
        val issued = clock.nowEpochMs()
        val session = AuthSession(
            phoneE164 = phone,
            issuedAtMs = issued,
            token = "sess_${issued}_${phone.takeLast(4)}",
        )
        return secure.put(SecureKeys.SESSION, session.encode())
    }

    private fun readSession(): AuthSession? =
        secure.read(SecureKeys.SESSION)?.let(AuthSession::decode)

    private fun finishUnlockSetup() {
        if (!emailDecided()) store.write(Keys.EMAIL_DECIDED, "true")
        store.write(Keys.AUTH_SETUP, "true")
        deviceUnlocked = true
        pinError = ""
        if (introComplete() && !onboardingComplete()) {
            onboardingStep = 3
            store.write(Keys.STEP, "3")
        }
        publish()
    }

    private fun banksLinked(): Boolean = store.read(Keys.BANKS) == "true"

    private fun notificationsEnabled(): Boolean = store.read(Keys.NOTIFICATIONS) != "false"

    private fun biometricEnabled(): Boolean = when (store.read(Keys.BIOMETRIC)) {
        "true" -> true
        "false" -> false
        else -> !debugBuild
    }

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
        val savedTab = store.read(Keys.TAB)
        if (savedTab != null && savedTab in MainTabs) tab = savedTab
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
        loadSync()
    }

    private fun loadSync() {
        lastSyncedAt = store.read(Keys.LAST_SYNCED)?.toLongOrNull()
        val needsReauth = store.read(Keys.NEEDS_REAUTH) == "true"
        val state = store.read(Keys.SYNC_STATE)
        val error = store.read(Keys.SYNC_ERROR).orEmpty()
        syncStatus = when {
            needsReauth || state == SyncCode.NEEDS_REAUTH -> SyncStatus.NeedsReauth
            state == SyncCode.FAILED -> SyncStatus.Failed(error.ifBlank { "Couldn't refresh" })
            state == SyncCode.SUCCESS && lastSyncedAt != null -> SyncStatus.Success(lastSyncedAt!!)
            else -> SyncStatus.Idle
        }
        syncedAccounts = AccountSnapshot.read(store)
    }

    private fun activeSource(): BankLinkSource {
        if (Qa.toolsEnabled(debugBuild) && store.read(Keys.LINK_KIND) == MayaStubBankSource.ID) {
            return mayaSample
        }
        return banks
    }

    private fun displayAccounts(): List<SyncedAccount> {
        if (!banksLinked()) return emptyList()
        if (syncedAccounts.isNotEmpty()) return syncedAccounts
        if (activeSource().id == MayaStubBankSource.ID) return MayaStubBankSource.previewAccounts()
        return emptyList()
    }

    private fun syncLine(): SyncLine {
        if (!banksLinked()) return SyncLine("", SyncCode.IDLE, "", false)
        val action = when (syncStatus) {
            SyncStatus.NeedsReauth -> "Reconnect"
            is SyncStatus.Failed -> "Try again"
            else -> ""
        }
        val stale = syncStatus is SyncStatus.NeedsReauth || syncStatus is SyncStatus.Failed
        return SyncLine(
            freshnessLabel = Freshness.label(clock.nowEpochMs(), syncStatus, lastSyncedAt),
            syncCode = syncStatus.code(),
            syncActionLabel = action,
            syncStale = stale,
        )
    }

    private fun finish(ticket: Int, result: BankFetch) {
        if (ticket != syncGeneration) return
        when (result) {
            is BankFetch.Ok -> {
                ledger.ingest(result.transactions)
                if (result.removedTransactionIds.isNotEmpty()) ledger.drop(result.removedTransactionIds)
                syncedAccounts = result.accounts
                AccountSnapshot.write(store, result.accounts)
                lastSyncedAt = clock.nowEpochMs()
                store.write(Keys.NEEDS_REAUTH, "false")
                syncStatus = SyncStatus.Success(lastSyncedAt!!)
                persistSync()
                publish()
            }
            is BankFetch.Unavailable -> {
                val reason = SafeLog.redact(result.reason).take(180).ifBlank { "Couldn't refresh" }
                syncStatus = SyncStatus.Failed(reason)
                persistSync()
                publish()
            }
            BankFetch.LoginRequired -> {
                store.write(Keys.NEEDS_REAUTH, "true")
                syncStatus = SyncStatus.NeedsReauth
                persistSync()
                publish()
            }
        }
    }

    private fun persistSync() {
        when (val status = syncStatus) {
            SyncStatus.Syncing -> Unit
            SyncStatus.Idle -> {
                store.remove(Keys.SYNC_STATE)
                store.remove(Keys.LAST_SYNCED)
                store.remove(Keys.SYNC_ERROR)
                store.write(Keys.NEEDS_REAUTH, "false")
            }
            is SyncStatus.Success -> {
                store.write(Keys.SYNC_STATE, status.code())
                store.write(Keys.LAST_SYNCED, status.lastSyncedAt.toString())
                store.remove(Keys.SYNC_ERROR)
                store.write(Keys.NEEDS_REAUTH, "false")
            }
            is SyncStatus.Failed -> {
                store.write(Keys.SYNC_STATE, status.code())
                store.write(Keys.SYNC_ERROR, status.reason)
                if (lastSyncedAt != null) {
                    store.write(Keys.LAST_SYNCED, lastSyncedAt.toString())
                }
                store.write(Keys.NEEDS_REAUTH, "false")
            }
            SyncStatus.NeedsReauth -> {
                store.write(Keys.SYNC_STATE, SyncCode.NEEDS_REAUTH)
                store.write(Keys.NEEDS_REAUTH, "true")
                store.remove(Keys.SYNC_ERROR)
                if (lastSyncedAt != null) {
                    store.write(Keys.LAST_SYNCED, lastSyncedAt.toString())
                }
            }
        }
    }

    private fun clearSync() {
        syncStatus = SyncStatus.Idle
        lastSyncedAt = null
        syncedAccounts = emptyList()
        ledger.clear()
        AccountSnapshot.clear(store)
        persistSync()
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

private fun releaseNeedsRealSecureStore(): SecureStore {
    throw IllegalArgumentException(
        "Release builds must pass a SecureStore. MemorySecureStore is only for debug and tests.",
    )
}

object Qa {
    /** True only for a debug/DEBUG build. Release always returns false. */
    fun toolsEnabled(debugBuild: Boolean): Boolean = debugBuild
}

private val MainTabs = setOf("HOME", "MOVES", "ACCOUNTS", "SETTINGS")

private const val RESEND_SEC = 30

private object Keys {
    const val ONBOARDING = "onboarding_complete"
    const val INTRO = "intro_complete"
    const val AUTH_SETUP = "auth_setup_complete"
    const val EMAIL_DECIDED = "report_email_decided"
    const val REPORT_EMAIL = "report_email"
    const val PIN_SET = "device_pin_set"
    const val PASSCODE = "passcode_fallback"
    const val STEP = "onboarding_step"
    const val BANKS = "banks_linked"
    const val LINK_KIND = "bank_link_kind"
    const val TRIAL_START = "trial_started_at"
    const val OVERRIDE = "qa_override"
    const val PLAN = "subscribed_plan"
    const val NOTIFICATIONS = "notifications"
    const val BIOMETRIC = "biometric_enabled"
    const val STATUSES = "move_statuses"
    const val NOTES = "move_notes"
    const val TAB = "selected_tab"
    const val LAST_SYNCED = "last_synced_at"
    const val SYNC_STATE = "sync_state"
    const val SYNC_ERROR = "sync_error"
    const val NEEDS_REAUTH = "sync_needs_reauth"
}
