package com.aicfo.shared

import com.aicfo.shared.auth.AppGate
import com.aicfo.shared.auth.AuthSession
import com.aicfo.shared.auth.EmailAddress
import com.aicfo.shared.auth.EmailAuthRepository
import com.aicfo.shared.auth.EmailAuthResult
import com.aicfo.shared.auth.Sha256
import com.aicfo.shared.auth.StubEmailAuthRepository
import com.aicfo.shared.auth.UnconfiguredEmailAuthRepository
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
    fun noSessionAfterIntroRoutesToEmail() {
        val app = debugApp()
        repeat(3) { app.primaryOnboarding() }
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertFalse(app.hasSession())
    }

    @Test
    fun emailAloneDoesNotStartASessionAndTheDebugCodeDoes() {
        val secure = MemorySecureStore()
        val app = debugApp(secure = secure)
        repeat(3) { app.primaryOnboarding() }
        assertFalse(app.submitEmail("not-an-email"))
        assertFalse(app.hasSession())
        assertEquals("", app.reportEmail())
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())

        assertTrue(app.submitEmail("Maya@Gmail.com"))
        assertEquals(AuthStep.CODE, app.authStep())
        assertEquals("m•••@gmail.com", app.maskedEmail())
        assertEquals(30, app.resendSeconds())
        assertFalse(app.hasSession())
        assertEquals("", app.reportEmail())
        assertFalse(app.verifySignInCode("123456"))
        assertFalse(app.hasSession())
        assertTrue(app.codeError().isNotEmpty())
        assertFalse(app.verifyMagicLink("https://finwise.example/magic"))
        assertFalse(app.hasSession())
        assertTrue(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertTrue(app.hasSession())
        assertEquals("maya@gmail.com", app.sessionEmail())
        assertEquals("maya@gmail.com", app.reportEmail())
        assertEquals(AuthStep.UNLOCK, app.authStep())
        val stored = secure.read("auth.session")
        assertEquals("maya@gmail.com", stored?.let(AuthSession::decode)?.email)
    }

    @Test
    fun anyMailboxCanRequestACode() {
        val app = debugApp()
        repeat(3) { app.primaryOnboarding() }
        listOf(
            "maya@gmail.com",
            "maya@outlook.com",
            "maya@icloud.com",
            "maya@me.com",
            "maya@privaterelay.appleid.com",
        ).forEach { address ->
            app.changeEmail()
            assertTrue(app.submitEmail(address))
            assertFalse(app.hasSession())
            assertEquals(AuthStep.CODE, app.authStep())
        }
    }

    @Test
    fun reportAddressIsStoredAndIsNotASecondLogin() {
        val store = MemoryLocalStore()
        val app = debugApp(store)
        repeat(3) { app.primaryOnboarding() }
        app.verifyReady("maya@studio.example")
        assertEquals("maya@studio.example", app.sessionEmail())
        assertEquals("maya@studio.example", app.reportEmail())
        assertEquals(AuthStep.UNLOCK, app.authStep())
        assertEquals(Gate.AUTH, app.gate())

        val reportOnly = debugApp(store, MemorySecureStore())
        assertEquals("maya@studio.example", reportOnly.reportEmail())
        assertFalse(reportOnly.hasSession())
        assertEquals(AuthStep.EMAIL, reportOnly.authStep())
    }

    @Test
    fun profilePhoneIsNotTheSession() {
        val app = debugApp()
        repeat(3) { app.primaryOnboarding() }
        assertFalse(app.saveProfilePhone("555"))
        assertFalse(app.hasSession())
        assertEquals(Gate.AUTH, app.gate())
        assertTrue(app.saveProfilePhone("5555551234"))
        assertEquals("+15555551234", app.profilePhone())
        assertFalse(app.hasSession())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertTrue(app.saveProfilePhone(""))
        assertEquals("", app.profilePhone())
        assertFalse(app.hasSession())
    }

    @Test
    fun aPhoneRecordIsNotASession() {
        val secure = MemorySecureStore()
        val store = MemoryLocalStore()
        secure.put("auth.session", "+15555551234|10|sess_test")
        store.write("intro_complete", "true")
        store.write("report_email", "maya@gmail.com")
        val app = debugApp(store, secure)
        assertFalse(app.hasSession())
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertNull(AuthSession.decode("+15555551234|10|sess_test"))
        assertTrue(EmailAddress.accepts("maya@outlook.com"))
    }

    @Test
    fun sessionPlusUnlockNeededRoutesToLockAndPinDoesNotReplaceLogin() {
        val store = MemoryLocalStore()
        val secure = MemorySecureStore()
        val app = debugApp(store, secure)
        repeat(3) { app.primaryOnboarding() }
        app.verifyReady("maya@studio.example")
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
        assertEquals(AuthStep.DONE, cold.authStep())
        assertFalse(cold.unlockWithPin("000000"))
        assertEquals(Gate.LOCK, cold.gate())
        assertTrue(cold.unlockWithPin("123456"))
        assertEquals(Gate.APP, cold.gate())
        assertEquals("maya@studio.example", cold.sessionEmail())
    }

    @Test
    fun logoutClearsTheSessionOnly() {
        val store = MemoryLocalStore()
        val secure = MemorySecureStore()
        val vault = MemoryTokenVault()
        val app = AiCfoController(vault, store, AuthTestClock(10L), secure)
        repeat(3) { app.primaryOnboarding() }
        app.verifyReady("maya@icloud.com")
        assertTrue(app.saveDevicePin("123456", "123456"))
        assertTrue(app.saveProfilePhone("5555550199"))
        app.primaryOnboarding()
        assertTrue(vault.put("institution.chase-checking", "link_stub_chase-checking"))
        val pin = secure.read("auth.device_pin")
        val trial = store.read("trial_started_at")
        assertTrue(app.hasSession())
        assertEquals(Gate.APP, app.gate())

        app.logOut()
        assertFalse(app.hasSession())
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertNull(secure.read("auth.session"))
        assertEquals(pin, secure.read("auth.device_pin"))
        assertEquals("true", store.read("device_pin_set"))
        assertEquals("true", store.read("auth_setup_complete"))
        assertEquals("maya@icloud.com", store.read("report_email"))
        assertEquals("+15555550199", app.profilePhone())
        assertEquals(trial, store.read("trial_started_at"))
        assertEquals("link_stub_chase-checking", vault.read("institution.chase-checking"))

        val next = debugApp(store, secure)
        assertFalse(next.hasSession())
        assertEquals(Gate.AUTH, next.gate())
        assertEquals(AuthStep.EMAIL, next.authStep())
        assertTrue(next.submitEmail("maya@icloud.com"))
        assertFalse(next.hasSession())
        assertTrue(next.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertEquals(Gate.APP, next.gate())
        assertEquals("maya@icloud.com", next.sessionEmail())
    }

    @Test
    fun magicLinkFromAFakeSenderSignsInWithoutTheCode() {
        val fake = FakeEmailAuthRepository(code = "482913", linkToken = "link_test_token")
        val app = debugApp(emailAuth = fake)
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.submitEmail("maya@outlook.com"))
        assertEquals(listOf("maya@outlook.com"), fake.requested)
        assertFalse(app.hasSession())
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())
        assertTrue(app.verifyMagicLink(fake.linkToken))
        assertTrue(app.hasSession())
        assertEquals("maya@outlook.com", app.sessionEmail())
        assertEquals("maya@outlook.com", app.reportEmail())
        assertEquals("", app.debugSignInCode())
    }

    @Test
    fun fakeCodeSignsInWithoutAMagicLink() {
        val fake = FakeEmailAuthRepository(code = "482913", linkToken = "link_other")
        val app = debugApp(emailAuth = fake)
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.submitEmail("maya@me.com"))
        assertFalse(app.verifyMagicLink("not-the-link"))
        assertFalse(app.hasSession())
        assertTrue(app.verifySignInCode("482913"))
        assertTrue(app.hasSession())
        assertEquals(AuthStep.UNLOCK, app.authStep())
    }

    @Test
    fun releaseFailsClosedWithoutAMailProvider() {
        val app = releaseApp()
        repeat(3) { app.primaryOnboarding() }
        assertEquals(Gate.AUTH, app.gate())
        assertFalse(app.emailSignInConfigured())
        assertTrue(app.emailSignInBlocker().contains("isn't configured"))
        assertEquals("", app.debugSignInCode())
        assertFalse(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertEquals("", app.reportEmail())
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.verifyMagicLink("link_test_token"))
        assertFalse(app.hasSession())
        app.debugCompleteUnlockSetup()
        assertEquals(Gate.AUTH, app.gate())
    }

    @Test
    fun releaseWithAFakeSenderStillRequiresTheCodeOrLink() {
        val fake = FakeEmailAuthRepository(code = "111222", linkToken = "link_release")
        val app = AiCfoController(
            MemoryTokenVault(),
            MemoryLocalStore(),
            AuthTestClock(10L),
            Markets.unitedStates(),
            EmptyLocalStrings,
            false,
            MemorySecureStore(),
            fake,
        )
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.emailSignInConfigured())
        assertEquals("", app.debugSignInCode())
        assertTrue(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())
        assertTrue(app.verifySignInCode("111222"))
        assertTrue(app.hasSession())
    }

    @Test
    fun releaseColdStartWithASessionLocksEvenIfBiometricsAreOff() {
        val store = MemoryLocalStore()
        val secure = MemorySecureStore()
        secure.put(
            "auth.session",
            AuthSession("maya@studio.example", 10L, "sess_test").encode(),
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
        emailAuth: EmailAuthRepository? = null,
    ) = AiCfoController(
        MemoryTokenVault(),
        store,
        AuthTestClock(10L),
        Markets.unitedStates(),
        EmptyLocalStrings,
        true,
        secure,
        emailAuth,
    )

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

    private fun AiCfoController.verifyReady(email: String) {
        assertTrue(submitEmail(email))
        assertFalse(hasSession())
        assertTrue(verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
    }
}

/**
 * Test double. Records the address and accepts either its code or its link token.
 * It does not send mail.
 */
private class FakeEmailAuthRepository(
    val code: String,
    val linkToken: String,
) : EmailAuthRepository {
    val requested = mutableListOf<String>()

    override fun isConfigured(): Boolean = true

    override fun requestChallenge(email: String): EmailAuthResult {
        if (!EmailAddress.accepts(email)) return EmailAuthResult(false, "Enter a valid email address.")
        requested += email
        return EmailAuthResult(true)
    }

    override fun verifyCode(email: String, code: String): EmailAuthResult {
        if (email !in requested) return EmailAuthResult(false, "Request a new code.")
        return if (code == this.code) {
            EmailAuthResult(true)
        } else {
            EmailAuthResult(false, "That code is wrong or expired.")
        }
    }

    override fun verifyMagicLink(email: String, token: String): EmailAuthResult {
        if (email !in requested) return EmailAuthResult(false, "Request a new link.")
        return if (token == linkToken) {
            EmailAuthResult(true)
        } else {
            EmailAuthResult(false, "That link is wrong or expired.")
        }
    }
}

private class AuthTestClock(var now: Long) : AppClock {
    override fun nowEpochMs(): Long = now
}
