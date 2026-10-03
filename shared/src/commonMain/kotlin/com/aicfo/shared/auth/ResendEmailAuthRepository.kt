package com.aicfo.shared.auth

/**
 * Release sender. Posts the 6-digit login code to Resend.
 * Debug builds must not construct this. [LoginMail] returns the stub instead.
 *
 * The API key is passed to [ResendTransport] and is not logged here.
 * [StubEmailAuthRepository.DEBUG_CODE] is never accepted.
 */
class ResendEmailAuthRepository(
    private val apiKey: String,
    private val fromAddress: String,
    private val transport: ResendTransport,
    private val newCode: () -> String = { sixDigitCode() },
) : EmailAuthRepository {
    private val pending = mutableMapOf<String, String>()
    private var sends: Int = 0

    init {
        require(acceptableSecret(apiKey)) { "Resend requires an API key from outside git." }
        require(acceptableFrom(fromAddress)) { "Resend requires a from address." }
    }

    override fun isConfigured(): Boolean = true

    override fun requestChallenge(email: String): EmailAuthResult {
        if (!EmailAddress.accepts(email)) {
            return EmailAuthResult(false, "Enter a valid email address.")
        }
        if (sends >= 5) {
            return EmailAuthResult(false, "Too many codes. Wait a moment and try again.")
        }
        val code = freshCode()
        val posted = transport.postEmail(apiKey, resendJson(fromAddress, email, code))
        if (posted.status !in 200..299) {
            val message = posted.error.ifBlank { "Couldn't send the code." }
            return EmailAuthResult(false, message)
        }
        sends += 1
        pending[email] = code
        return EmailAuthResult(true)
    }

    override fun verifyCode(email: String, code: String): EmailAuthResult {
        if (code == StubEmailAuthRepository.DEBUG_CODE) {
            return EmailAuthResult(false, "That code is wrong or expired.")
        }
        val expected = pending[email] ?: return EmailAuthResult(false, "Request a new code.")
        if (code != expected) return EmailAuthResult(false, "That code is wrong or expired.")
        pending.remove(email)
        return EmailAuthResult(true)
    }

    override fun verifyMagicLink(email: String, token: String): EmailAuthResult =
        EmailAuthResult(false, "This sign-in sends a code, not a link.")

    private fun freshCode(): String {
        repeat(5) {
            val code = newCode().trim()
            if (code != StubEmailAuthRepository.DEBUG_CODE && code.length == 6 && code.all { it.isDigit() }) {
                return code
            }
        }
        return "100001"
    }
}

fun interface ResendTransport {
    /**
     * HTTPS POST of the Resend JSON body.
     * Implementations must not log [apiKey] or the body (the body contains the code).
     */
    fun postEmail(apiKey: String, jsonBody: String): ResendHttpResult
}

data class ResendHttpResult(
    val status: Int,
    val error: String = "",
)

object ResendApi {
    const val ENDPOINT = "https://api.resend.com/emails"
}

/**
 * Picks the sender for a build.
 * Debug always gets the stub, even when a key is present, so a debug build cannot send mail.
 * Release with a blank key or from address fails closed.
 */
object LoginMail {
    fun repository(
        debugBuild: Boolean,
        apiKey: String,
        fromAddress: String,
        transport: ResendTransport,
    ): EmailAuthRepository {
        if (debugBuild) return StubEmailAuthRepository()
        if (!acceptableSecret(apiKey) || !acceptableFrom(fromAddress)) {
            return UnconfiguredEmailAuthRepository()
        }
        return ResendEmailAuthRepository(apiKey.trim(), fromAddress.trim(), transport)
    }
}

internal fun sixDigitCode(nextInt: (Int) -> Int = { bound -> kotlin.random.Random.nextInt(bound) }): String {
    val n = nextInt(1_000_000)
    val safe = if (n == 0) 1 else n
    return safe.toString().padStart(6, '0')
}

internal fun acceptableSecret(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isNotEmpty() && trimmed.length <= 200 && trimmed.none { it.isWhitespace() }
}

internal fun acceptableFrom(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isNotEmpty() && trimmed.length <= 200 && trimmed.none { it == '\n' || it == '\r' }
}

internal fun resendJson(from: String, to: String, code: String): String {
    val text = "Your Finwise sign-in code is $code.\n\nIf you didn't ask for this, you can ignore this email."
    return buildString {
        append("{\"from\":")
        append(jsonString(from))
        append(",\"to\":[")
        append(jsonString(to))
        append("],\"subject\":")
        append(jsonString("Your Finwise code"))
        append(",\"text\":")
        append(jsonString(text))
        append("}")
    }
}

private fun jsonString(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
    append('"')
}
