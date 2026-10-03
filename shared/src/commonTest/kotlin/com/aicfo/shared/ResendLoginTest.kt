package com.aicfo.shared

import com.aicfo.shared.auth.LoginHttpResult
import com.aicfo.shared.auth.LoginMail
import com.aicfo.shared.auth.LoginServerEmailAuthRepository
import com.aicfo.shared.auth.LoginServerTransport
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
import com.aicfo.shared.security.TlsPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResendLoginTest {
    @Test
    fun releaseWithoutAServerFailsClosedAndRefusesTheDebugCode() {
        val transport = RecordingTransport()
        val mail = LoginMail.repository(debugBuild = false, serverUrl = "", transport = transport)
        assertTrue(mail is UnconfiguredEmailAuthRepository)
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertFalse(app.emailSignInConfigured())
        assertEquals("", app.debugSignInCode())
        assertFalse(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertEquals("", app.reportEmail())
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.verifyMagicLink("link_test_token"))
        assertFalse(app.hasSession())
        assertTrue(transport.calls.isEmpty())
    }

    @Test
    fun releaseAsksTheServerToSendAndRefuses000000() {
        val transport = RecordingTransport(verifyCode = "482913")
        val mail = LoginMail.repository(
            debugBuild = false,
            serverUrl = "https://login.example",
            transport = transport,
        )
        assertTrue(mail is LoginServerEmailAuthRepository)
        TlsPolicy.requireHttps("https://login.example/login/code")
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.emailSignInConfigured())
        assertEquals("", app.debugSignInCode())
        assertTrue(app.submitEmail("Maya@Gmail.com"))
        assertFalse(app.hasSession())
        assertEquals(AuthStep.CODE, app.authStep())
        val send = transport.calls.single()
        assertEquals("https://login.example/login/code", send.url)
        assertTrue(send.body.contains("maya@gmail.com"))
        assertFalse(send.body.contains("000000"))
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())
        assertEquals(1, transport.calls.size)
        assertTrue(app.verifySignInCode("482913"))
        assertTrue(app.hasSession())
        assertEquals("maya@gmail.com", app.sessionEmail())
        assertTrue(transport.calls.last().url.endsWith("/login/verify"))
        assertFalse(transport.calls.any { it.body.contains("api_key") || it.body.contains("Bearer") })
    }

    @Test
    fun releaseRefuses000000EvenIfTheServerWouldAcceptIt() {
        val transport = RecordingTransport(verifyCode = StubEmailAuthRepository.DEBUG_CODE)
        val mail = LoginMail.repository(false, "https://login.example", transport)
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())
        assertEquals(1, transport.calls.size)
    }

    @Test
    fun aFailedServerPostDoesNotStartASession() {
        val transport = RecordingTransport(status = 503)
        val mail = LoginMail.repository(false, "https://login.example", transport)
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertFalse(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertFalse(app.verifySignInCode("482913"))
        assertFalse(app.hasSession())
    }

    @Test
    fun debugIgnoresAServerUrlAndDoesNotSend() {
        val transport = RecordingTransport()
        val mail = LoginMail.repository(true, "https://login.example", transport)
        assertTrue(mail is StubEmailAuthRepository)
        val app = AiCfoController(
            MemoryTokenVault(),
            MemoryLocalStore(),
            clock(),
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            MemorySecureStore(),
            mail,
        )
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertTrue(transport.calls.isEmpty())
        assertEquals(StubEmailAuthRepository.DEBUG_CODE, app.debugSignInCode())
        assertTrue(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertTrue(app.hasSession())
    }

    @Test
    fun debugBuildRejectsTheLoginServerClient() {
        val error = assertFailsWith<IllegalArgumentException> {
            AiCfoController(
                MemoryTokenVault(),
                MemoryLocalStore(),
                clock(),
                Markets.unitedStates(),
                EmptyLocalStrings,
                true,
                MemorySecureStore(),
                LoginServerEmailAuthRepository("https://login.example", RecordingTransport()),
            )
        }
        assertTrue(error.message!!.contains("must not send mail"))
    }

    private fun releaseApp(mail: com.aicfo.shared.auth.EmailAuthRepository) = AiCfoController(
        MemoryTokenVault(),
        MemoryLocalStore(),
        clock(),
        Markets.unitedStates(),
        EmptyLocalStrings,
        false,
        MemorySecureStore(),
        mail,
    )

    private fun clock() = object : AppClock {
        override fun nowEpochMs(): Long = 10L
    }

    private class RecordingTransport(
        private val status: Int = 200,
        private val verifyCode: String = "482913",
    ) : LoginServerTransport {
        val calls = mutableListOf<Call>()

        override fun post(url: String, jsonBody: String): LoginHttpResult {
            calls += Call(url, jsonBody)
            if (url.endsWith("/login/verify") && !jsonBody.contains(verifyCode)) {
                return LoginHttpResult(400, "That code is wrong or expired.")
            }
            return LoginHttpResult(status, if (status in 200..299) "" else "Couldn't send the code.")
        }
    }

    private data class Call(val url: String, val body: String)
}
