package com.aicfo.login

import com.sun.net.httpserver.HttpServer
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginServerTest {
    @Test
    fun missingKeyDoesNotCallResend() {
        val resend = RecordingPoster()
        val service = LoginService(secrets = { MailSecrets("", "") }, resend = resend)
        assertEquals(503, service.requestCode("maya@gmail.com").status)
        assertTrue(resend.calls.isEmpty())
        assertEquals(400, service.verify("maya@gmail.com", "000000").status)
    }

    @Test
    fun configuredServerSendsACodeAndRefuses000000() {
        val resend = RecordingPoster()
        val service = LoginService(
            secrets = { MailSecrets("test-key", "Finwise <login@example.com>") },
            resend = resend,
        )
        assertEquals(200, service.requestCode("Maya@Gmail.com").status)
        val call = resend.calls.single()
        assertEquals("test-key", call.apiKey)
        assertEquals("Finwise <login@example.com>", call.from)
        assertEquals("maya@gmail.com", call.to)
        assertEquals(6, call.code.length)
        assertTrue(call.code.all { it.isDigit() })
        assertFalse(call.code == "000000")
        assertEquals("https://api.resend.com/emails", JvmResendPoster.ENDPOINT)
        assertTrue(call.json.contains(call.code))
        assertEquals(400, service.verify("maya@gmail.com", "000000").status)
        assertEquals(200, service.verify("maya@gmail.com", call.code).status)
        assertEquals(400, service.verify("maya@gmail.com", call.code).status)
    }

    @Test
    fun generatorCannotMakeTheDebugCodeValid() {
        val resend = RecordingPoster()
        val service = LoginService(
            secrets = { MailSecrets("test-key", "login@example.com") },
            resend = resend,
            newCode = { "000000" },
        )
        assertEquals(200, service.requestCode("maya@outlook.com").status)
        assertFalse(resend.calls.single().code == "000000")
        assertEquals(400, service.verify("maya@outlook.com", "000000").status)
    }

    @Test
    fun secretsComeFromTheEnvironmentBeforeTheFile() {
        val file = """
            # names only
            FINWISE_RESEND_API_KEY=
            FINWISE_RESEND_API_KEY=file-key
            FINWISE_RESEND_FROM=login@example.com
        """.trimIndent()
        assertEquals("", MailSecrets.resolve({ null }, null).apiKey)
        assertEquals("", MailSecrets.resolve({ null }, "# FINWISE_RESEND_API_KEY=\n").apiKey)
        val fromFile = MailSecrets.resolve({ null }, file)
        assertEquals("file-key", fromFile.apiKey)
        assertEquals("login@example.com", fromFile.fromAddress)
        val fromEnv = MailSecrets.resolve(
            { name -> if (name == MailSecrets.API_KEY) "env-key" else null },
            file,
        )
        assertEquals("env-key", fromEnv.apiKey)
        assertEquals("login@example.com", fromEnv.fromAddress)
    }

    @Test
    fun httpPostsSendAndVerifyAndHideTheCode() {
        val resend = RecordingPoster()
        val service = LoginService(
            secrets = { MailSecrets("test-key", "login@example.com") },
            resend = resend,
        )
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        LoginHttp(service).attach(server)
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            val sent = http(base, "/login/code", """{"email":"maya@gmail.com"}""")
            assertEquals(200, sent.status)
            assertFalse(sent.body.contains(resend.calls.single().code))
            assertEquals(405, http(base, "/login/code", """{"email":"maya@gmail.com"}""", method = "GET").status)
            val wrong = http(base, "/login/verify", """{"email":"maya@gmail.com","code":"000000"}""")
            assertEquals(400, wrong.status)
            assertFalse(wrong.body.contains("000000"))
            val code = resend.calls.single().code
            val ok = http(base, "/login/verify", """{"email":"maya@gmail.com","code":"$code"}""")
            assertEquals(200, ok.status)
            assertFalse(ok.body.contains(code))
        } finally {
            server.stop(0)
        }
    }

    private fun http(base: String, path: String, body: String, method: String = "POST"): HttpBody {
        val connection = (URL(base + path).openConnection() as HttpURLConnection)
        connection.requestMethod = method
        if (method == "POST") {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val status = connection.responseCode
        val stream = if (status >= 400) connection.errorStream else connection.inputStream
        val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        connection.disconnect()
        return HttpBody(status, text)
    }

    private class RecordingPoster : ResendPoster {
        val calls = mutableListOf<Call>()

        override fun post(secrets: MailSecrets, to: String, code: String): Int {
            calls += Call(secrets.apiKey, secrets.fromAddress, to, code, resendJson(secrets.fromAddress, to, code))
            return 200
        }
    }

    private data class Call(
        val apiKey: String,
        val from: String,
        val to: String,
        val code: String,
        val json: String,
    )

    private data class HttpBody(val status: Int, val body: String)
}
