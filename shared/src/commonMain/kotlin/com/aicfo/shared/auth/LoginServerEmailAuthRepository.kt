package com.aicfo.shared.auth

/**
 * Release client. Asks the login server to send the 6-digit code and to check it.
 * This type has no Resend key and no from address.
 * Debug builds must not construct it. [LoginMail] returns the stub instead.
 * [StubEmailAuthRepository.DEBUG_CODE] is never accepted.
 */
class LoginServerEmailAuthRepository(
    serverUrl: String,
    private val transport: LoginServerTransport,
) : EmailAuthRepository {
    private val base = serverUrl.trim().trimEnd('/')

    init {
        require(acceptableServerUrl(base)) { "Release sign-in needs an https login server." }
    }

    override fun isConfigured(): Boolean = true

    override fun requestChallenge(email: String): EmailAuthResult {
        if (!EmailAddress.accepts(email)) {
            return EmailAuthResult(false, "Enter a valid email address.")
        }
        val posted = transport.post("$base/login/code", """{"email":${jsonString(email)}}""")
        if (posted.status !in 200..299) {
            return EmailAuthResult(false, posted.error.ifBlank { "Couldn't send the code." })
        }
        return EmailAuthResult(true)
    }

    override fun verifyCode(email: String, code: String): EmailAuthResult {
        if (code == StubEmailAuthRepository.DEBUG_CODE) {
            return EmailAuthResult(false, "That code is wrong or expired.")
        }
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) return EmailAuthResult(false, "Enter the 6-digit code.")
        val posted = transport.post(
            "$base/login/verify",
            """{"email":${jsonString(email)},"code":${jsonString(digits)}}""",
        )
        if (posted.status !in 200..299) {
            return EmailAuthResult(false, posted.error.ifBlank { "That code is wrong or expired." })
        }
        return EmailAuthResult(true)
    }

    override fun verifyMagicLink(email: String, token: String): EmailAuthResult =
        EmailAuthResult(false, "This sign-in sends a code, not a link.")
}

fun interface LoginServerTransport {
    /** HTTPS POST. Must not add a mail-provider key. */
    fun post(url: String, jsonBody: String): LoginHttpResult
}

data class LoginHttpResult(
    val status: Int,
    val error: String = "",
)

/**
 * Picks the sender for a build.
 * Debug always gets the stub, so a debug build cannot send mail.
 * Release with a blank or non-https server URL fails closed.
 */
object LoginMail {
    fun repository(
        debugBuild: Boolean,
        serverUrl: String,
        transport: LoginServerTransport,
    ): EmailAuthRepository {
        if (debugBuild) return StubEmailAuthRepository()
        if (!acceptableServerUrl(serverUrl)) return UnconfiguredEmailAuthRepository()
        return LoginServerEmailAuthRepository(serverUrl, transport)
    }
}

internal fun acceptableServerUrl(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.startsWith("https://") &&
        trimmed.length <= 200 &&
        trimmed.none { it.isWhitespace() }
}

private fun jsonString(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(ch)
        }
    }
    append('"')
}
