package com.aicfo.shared

import com.aicfo.shared.auth.AppGate
import com.aicfo.shared.auth.AuthSession
import com.aicfo.shared.auth.PhoneNumbers
import com.aicfo.shared.auth.Sha256
import com.aicfo.shared.auth.StubOtpAuthRepository
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.presentation.AuthStep
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemorySecureStore
import com.aicfo.shared.security.MemoryTokenVault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthGateTest {
    @Test
    fun sha256MatchesTheEmptyAndAbcVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.hex(ByteArray(0)),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.hex("abc"),
        )
    }

    @Test
    fun gateOrderMatchesTheSessionRules() {
        assertEquals(
            Gate.ONBOARDING,
            AppGate.resolve(
                hasSession = false,
                introComplete = false,
                authSetupComplete = false,
                onboardingComplete = false,
                deviceUnlockNeeded = false,
                entitled = false,
            ),
        )
        assertEquals(
            Gate.AUTH,
            AppGate.resolve(
                hasSession = false,
                introComplete = true,
                authSetupComplete = false,
                onboardingComplete = false,
                deviceUnlockNeeded = false,
                entitled = false,
            ),
        )
        assertEquals(
            Gate.AUTH,
            AppGate.resolve(
                hasSession = true,
                introComplete = true,
                authSetupComplete = false,
                onboardingComplete = false,
                deviceUnlockNeeded = true,
                entitled = false,
            ),
        )
        assertEquals(
            Gate.ONBOARDING,
            AppGate.resolve(
                hasSession = true,
                introComplete = true,
                authSetupComplete = true,
                onboardingComplete = false,
                deviceUnlockNeeded = true,
                entitled = false,
            ),
        )
        assertEquals(
            Gate.LOCK,
            AppGate.resolve(
                hasSession = true,
                introComplete = true,
                authSetupComplete = true,
                onboardingComplete = true,
                deviceUnlockNeeded = true,
                entitled = false,
            ),
        )
        assertEquals(
            Gate.PAYWALL,
            AppGate.resolve(
                hasSession = true,
                introComplete = true,
                authSetupComplete = true,
                onboardingComplete = true,
                deviceUnlockNeeded = false,
                entitled = false,
            ),
        )
        assertEquals(
            Gate.APP,
            AppGate.resolve(
                hasSession = true,
                introComplete = true,
                authSetupComplete = true,
                onboardingComplete = true,
                deviceUnlockNeeded = false,
                entitled = true,
            ),
        )
    }

    @Test
    fun noSessionAfterIntroRoutesToAuth() {
        val app = debugApp()
        passIntro(app)
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.PHONE, app.authStep())
        assertFalse(app.hasSession())
    }

    @Test
    fun debugOtpPersistsSessionAndRejectsAWrongCode() {
        val secure = MemorySecureStore()
        val app = debugApp(secure = secure)
        passIntro(app)
        assertFalse(app.submitPhone("555"))
        assertFalse(app.hasSession())
        assertTrue(app.submitPhone("5555551234"))
        assertEquals(AuthStep.OTP, app.authStep())
        assertEquals("+1····1234", app.maskedPhone())
        assertEquals(30, app.resendSeconds())
        assertFalse(app.verifyOtp("123456"))
        assertFalse(app.hasSession())
        assertTrue(app.otpError().isNotEmpty())
        assertTrue(app.verifyOtp(StubOtpAuthRepository.DEBUG_CODE))
        assertTrue(app.hasSession())
        assertEquals("+15555551234", app.sessionPhone())
        assertEquals(AuthStep.EMAIL, app.authStep())
        val stored = secure.read("auth.session")
        assertEquals("+15555551234", stored?.let(AuthSession::decode)?.phoneE164)
    }

    @Test
    fun emailIsNotAnAccountLogin() {
        val app = debugApp()
        passIntro(app)
        app.verifyReady()
        val phone = app.sessionPhone()
        assertFalse(app.saveReportEmail("not-an-email"))
        assertTrue(app.saveReportEmail("maya@studio.example"))
        assertEquals(phone, app.sessionPhone())
        assertEquals("maya@studio.example", app.reportEmail())
        assertEquals(AuthStep.UNLOCK, app.authStep())
        assertEquals(Gate.AUTH, app.gate())
    }

    @Test
    fun sessionPlusUnlockNeededRoutesToLockAndPinDoesNotReplaceLogin() {
        val store = MemoryLocalStore()
        val secure = MemorySecureStore()
        val app = debugApp(store, secure)
        passIntro(app)
        app.verifyReady()
        app.skipReportEmail()
        assertFalse(app.saveDevicePin("12345", "12345"))
        assertFalse(app.saveDevicePin("123456", "654321"))
        assertTrue(app.saveDevicePin("123456", "123456"))
        assertNull(store.read("auth.device_pin"))
        assertTrue(secure.read("auth.device_pin")!!.contains(":"))
        app.primaryOnboarding()
        assertEquals(Gate.APP, app.gate())

        val cold = debugApp(store, secure)
        assertTrue(cold.hasSession())
        assertEquals(Gate.LOCK, cold.gate())
        assertFalse(cold.unlockWithPin("000000"))
        assertEquals(Gate.LOCK, cold.gate())
        assertTrue(cold.unlockWithPin("123456"))
        assertEquals(Gate.APP, cold.gate())
        assertEquals("+15555551234", cold.sessionPhone())
    }

    @Test
    fun logoutClearsTheSessionAndTheNextOpenIsPhone() {
        val store = MemoryLocalStore()
        val secure = MemorySecureStore()
        val app = debugApp(store, secure)
        passIntro(app)
        app.testingSeedSession()
        app.primaryOnboarding()
        assertTrue(app.hasSession())
        assertEquals(Gate.APP, app.gate())

        app.logOut()
        assertFalse(app.hasSession())
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.PHONE, app.authStep())
        assertNull(secure.read("auth.session"))

        val next = debugApp(store, secure)
        assertFalse(next.hasSession())
        assertEquals(Gate.AUTH, next.gate())
        assertEquals(AuthStep.PHONE, next.authStep())
    }

    @Test
    fun releaseRejectsTheDebugCodeAndCannotSkipThePhoneScreen() {
        val app = releaseApp()
        passIntro(app)
        assertEquals(Gate.AUTH, app.gate())
        assertFalse(app.submitPhone("5555551234"))
        assertFalse(app.hasSession())
        app.debugSkipPhone()
        assertFalse(app.hasSession())
        assertEquals("", app.debugOtpCode())
        app.debugCompleteUnlockSetup()
        assertEquals(Gate.AUTH, app.gate())
    }

    @Test
    fun releaseColdStartWithASessionLocksEvenIfBiometricsAreOff() {
        val store = MemoryLocalStore()
        val secure = MemorySecureStore()
        secure.put(
            "auth.session",
            AuthSession("+15555551234", 10L, "sess_test").encode(),
        )
        store.write("intro_complete", "true")
        store.write("onboarding_complete", "true")
        store.write("auth_setup_complete", "true")
        store.write("trial_started_at", "10")
        store.write("biometric_enabled", "false")
        val app = releaseApp(store, secure)
        assertEquals(Gate.LOCK, app.gate())
        assertTrue(app.lock().mustCreatePin)
        app.unlockWithoutHardware()
        assertEquals(Gate.LOCK, app.gate())
    }

    private fun debugApp(
        store: MemoryLocalStore = MemoryLocalStore(),
        secure: MemorySecureStore = MemorySecureStore(),
    ) = AiCfoController(MemoryTokenVault(), store, AuthTestClock(10L), secure)

    private fun releaseApp(
        store: MemoryLocalStore = MemoryLocalStore(),
        secure: MemorySecureStore = MemorySecureStore(),
    ) = AiCfoController(
        MemoryTokenVault(),
        store,
        AuthTestClock(10L),
        Markets.unitedStates(),
        EmptyLocalStrings,
        false,
        secure,
    )

    private fun AiCfoController.verifyReady() {
        assertTrue(submitPhone("5555551234"))
        assertTrue(verifyOtp(StubOtpAuthRepository.DEBUG_CODE))
    }

    private fun passIntro(app: AiCfoController) {
        app.primaryOnboarding()
        app.primaryOnboarding()
        app.secondaryOnboarding()
    }
}

private class AuthTestClock(var now: Long) : AppClock {
    override fun nowEpochMs(): Long = now
}
