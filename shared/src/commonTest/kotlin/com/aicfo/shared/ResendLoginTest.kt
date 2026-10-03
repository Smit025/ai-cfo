package com.aicfo.shared

import com.aicfo.shared.auth.LoginMail
import com.aicfo.shared.auth.MailConfig
import com.aicfo.shared.auth.ResendApi
import com.aicfo.shared.auth.ResendEmailAuthRepository
import com.aicfo.shared.auth.ResendHttpResult
import com.aicfo.shared.auth.ResendTransport
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
import com.aicfo.shared.security.SafeLog
import com.aicfo.shared.security.TlsPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResendLoginTest {
    @Test
    fun releaseWithoutAKeyFailsClosedAndRefusesTheDebugCode() {
        val transport = RecordingTransport()
        val mail = LoginMail.repository(
            debugBuild = false,
            apiKey = "",
            fromAddress = "login@example.com",
            transport = transport,
        )
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
    fun releaseResendSendsTheCodeAndRefuses000000() {
        val transport = RecordingTransport()
        val mail = LoginMail.repository(
            debugBuild = false,
            apiKey = "test-key",
            fromAddress = "Finwise <login@example.com>",
            transport = transport,
        )
        assertTrue(mail is ResendEmailAuthRepository)
        assertEquals("https://api.resend.com/emails", ResendApi.ENDPOINT)
        TlsPolicy.requireHttps(ResendApi.ENDPOINT)
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.emailSignInConfigured())
        assertEquals("", app.debugSignInCode())
        assertTrue(app.submitEmail("Maya@Gmail.com"))
        assertFalse(app.hasSession())
        assertEquals(AuthStep.CODE, app.authStep())
        assertEquals(1, transport.calls.size)
        val call = transport.calls.single()
        assertEquals("test-key", call.apiKey)
        assertTrue(call.jsonBody.contains("maya@gmail.com"))
        assertTrue(call.jsonBody.contains("Finwise <login@example.com>"))
        val code = Regex("""\b(\d{6})\b""").find(call.jsonBody)?.groupValues?.get(1)
        assertTrue(code != null && code != StubEmailAuthRepository.DEBUG_CODE)
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())
        assertTrue(app.verifySignInCode(code!!))
        assertTrue(app.hasSession())
        assertEquals("maya@gmail.com", app.sessionEmail())
    }

    @Test
    fun releaseStillRefuses000000WhenThatIsWhatWouldHaveBeenSent() {
        val transport = RecordingTransport()
        val mail = ResendEmailAuthRepository(
            apiKey = "test-key",
            fromAddress = "login@example.com",
            transport = transport,
            newCode = { StubEmailAuthRepository.DEBUG_CODE },
        )
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertTrue(app.submitEmail("maya@outlook.com"))
        assertFalse(app.hasSession())
        assertFalse(callBodyContainsDebugCode(transport))
        assertFalse(app.verifySignInCode(StubEmailAuthRepository.DEBUG_CODE))
        assertFalse(app.hasSession())
    }

    @Test
    fun aFailedResendPostDoesNotStartASession() {
        val transport = RecordingTransport(status = 500)
        val mail = LoginMail.repository(false, "test-key", "login@example.com", transport)
        val app = releaseApp(mail)
        repeat(3) { app.primaryOnboarding() }
        assertFalse(app.submitEmail("maya@gmail.com"))
        assertFalse(app.hasSession())
        assertEquals(Gate.AUTH, app.gate())
        assertEquals(AuthStep.EMAIL, app.authStep())
        assertFalse(app.verifySignInCode("123456"))
        assertFalse(app.hasSession())
    }

    @Test
    fun debugIgnoresAConfiguredKeyAndDoesNotSend() {
        val transport = RecordingTransport()
        val mail = LoginMail.repository(
            debugBuild = true,
            apiKey = "test-key",
            fromAddress = "login@example.com",
            transport = transport,
        )
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
    fun debugBuildRejectsTheResendSender() {
        val error = assertFailsWith<IllegalArgumentException> {
            AiCfoController(
                MemoryTokenVault(),
                MemoryLocalStore(),
                clock(),
                Markets.unitedStates(),
                EmptyLocalStrings,
                true,
                MemorySecureStore(),
                ResendEmailAuthRepository("test-key", "login@example.com", RecordingTransport()),
            )
        }
        assertTrue(error.message!!.contains("must not send mail"))
    }

    @Test
    fun mailConfigReadsTheEnvironmentBeforeTheGitignoredFile() {
        val file = """
            # Do not commit a real key.
            FINWISE_RESEND_API_KEY=
            FINWISE_RESEND_API_KEY=file-key
            FINWISE_RESEND_FROM=login@example.com
        """.trimIndent()
        assertEquals("" to "", MailConfig.resolve({ null }, null))
        assertEquals("" to "", MailConfig.resolve({ null }, "# FINWISE_RESEND_API_KEY=\n"))
        val fromFile = MailConfig.resolve({ null }, file)
        assertEquals("file-key", fromFile.first)
        assertEquals("login@example.com", fromFile.second)
        val fromEnv = MailConfig.resolve(
            { name -> if (name == MailConfig.API_KEY) "env-key" else null },
            file,
        )
        assertEquals("env-key", fromEnv.first)
        assertEquals("login@example.com", fromEnv.second)
        val cleaned = SafeLog.redact("Authorization: Bearer test-key FINWISE_RESEND_API_KEY=file-key")
        assertFalse(cleaned.contains("test-key"))
        assertFalse(cleaned.contains("file-key"))
    }

    private fun callBodyContainsDebugCode(transport: RecordingTransport): Boolean {
        return transport.calls.any { it.jsonBody.contains(StubEmailAuthRepository.DEBUG_CODE) }
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

    private class RecordingTransport(private val status: Int = 200) : ResendTransport {
        val calls = mutableListOf<Call>()

        override fun postEmail(apiKey: String, jsonBody: String): ResendHttpResult {
            calls += Call(apiKey, jsonBody)
            return ResendHttpResult(status, if (status in 200..299) "" else "Couldn't send the code.")
        }
    }

    private data class Call(val apiKey: String, val jsonBody: String)
}
