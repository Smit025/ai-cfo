package com.aicfo.shared.auth

/**
 * Provider-neutral email sign-in.
 *
 * Any inbox can be the account (Gmail, Outlook, Apple Mail, or another domain).
 * A one-time code or a magic-link token is the proof. Submitting the address
 * never creates a session, and this type does not send the monthly savings report.
 *
 * Debug builds use [StubEmailAuthRepository] and do not send mail.
 * Release builds use [LoginServerEmailAuthRepository] when a login-server URL is
 * set outside the app binary, and [UnconfiguredEmailAuthRepository] otherwise.
 * The Resend key stays on that server. See `docs/AUTH.md`.
 */
interface EmailAuthRepository {
    /** False when this build cannot send a code or a link. */
    fun isConfigured(): Boolean

    fun requestChallenge(email: String): EmailAuthResult

    fun verifyCode(email: String, code: String): EmailAuthResult

    fun verifyMagicLink(email: String, token: String): EmailAuthResult
}

data class EmailAuthResult(
    val ok: Boolean,
    val message: String = "",
)

/** Address checks for sign-in and for the future monthly report. Not a second factor. */
object EmailAddress {
    private val pattern = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    fun accepts(raw: String): Boolean {
        val value = raw.trim()
        if (value.length > 120 || value.any { it.isWhitespace() }) return false
        return pattern.matches(value)
    }

    /** Trimmed, lowercased address, or null when [accepts] fails. */
    fun normalize(raw: String): String? {
        val value = raw.trim().lowercase()
        if (!accepts(value)) return null
        return value
    }

    /** Code-screen label: m•••@studio.example */
    fun mask(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return "•••"
        return email.take(1) + "•••" + email.substring(at)
    }
}

/**
 * Debug / QA sender. It does not contact a mailbox.
 * [DEBUG_CODE] is accepted only after [requestChallenge] for that address.
 * Magic links are not issued here — tests that need a link pass their own fake.
 */
class StubEmailAuthRepository : EmailAuthRepository {
    private val pending = mutableSetOf<String>()
    private var sends: Int = 0

    override fun isConfigured(): Boolean = true

    override fun requestChallenge(email: String): EmailAuthResult {
        if (!EmailAddress.accepts(email)) {
            return EmailAuthResult(false, "Enter a valid email address.")
        }
        sends += 1
        if (sends > 5) {
            return EmailAuthResult(false, "Too many codes. Wait a moment and try again.")
        }
        pending += email
        return EmailAuthResult(true)
    }

    override fun verifyCode(email: String, code: String): EmailAuthResult {
        if (email !in pending) return EmailAuthResult(false, "Request a new code.")
        if (code == DEBUG_CODE) return EmailAuthResult(true)
        return EmailAuthResult(false, "That code is wrong or expired.")
    }

    override fun verifyMagicLink(email: String, token: String): EmailAuthResult =
        EmailAuthResult(false, "This debug build checks code $DEBUG_CODE. It does not send a magic link.")

    companion object {
        const val DEBUG_CODE = "000000"
    }
}

/**
 * Release placeholder. Never accepts a code or a link, and never writes a session.
 * A human connects a mail provider by implementing [EmailAuthRepository] and passing it in.
 */
class UnconfiguredEmailAuthRepository : EmailAuthRepository {
    override fun isConfigured(): Boolean = false

    override fun requestChallenge(email: String): EmailAuthResult = EmailAuthResult(false, NOT_CONFIGURED)

    override fun verifyCode(email: String, code: String): EmailAuthResult = EmailAuthResult(false, NOT_CONFIGURED)

    override fun verifyMagicLink(email: String, token: String): EmailAuthResult =
        EmailAuthResult(false, NOT_CONFIGURED)

    companion object {
        const val NOT_CONFIGURED =
            "Email sign-in isn't configured. A mail provider has to be connected before a code or link can be sent."
    }
}
