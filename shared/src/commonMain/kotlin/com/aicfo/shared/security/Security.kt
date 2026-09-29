package com.aicfo.shared.security

/**
 * Platform token vault. Implementations must encrypt at rest
 * (Android Keystore, iOS Keychain) and must never log values.
 * Raw bank passwords are not accepted — only link tokens.
 */
interface TokenVault {
    fun put(key: String, value: String): Boolean
    fun read(key: String): String?
    fun clear()
}

/** Non-secret flags: onboarding, trial clock, move status, sync metadata. Not for tokens. */
interface LocalStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

class MemoryTokenVault : TokenVault {
    private val values = mutableMapOf<String, String>()

    override fun put(key: String, value: String): Boolean {
        if (!LinkPolicy.accepts(value)) return false
        values[key] = value
        return true
    }

    override fun read(key: String): String? = values[key]

    override fun clear() {
        values.clear()
    }
}

class MemoryLocalStore : LocalStore {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

object LinkPolicy {
    fun accepts(token: String): Boolean {
        if (!token.startsWith("link_")) return false
        if (token.length > 180) return false
        val lower = token.lowercase()
        if (lower.contains("password")) return false
        if (lower.contains("ssn")) return false
        if (token.any { it.isWhitespace() }) return false
        return true
    }
}

object PiiPolicy {
    const val storesRawPasswords: Boolean = false
    const val storesFullAccountNumbers: Boolean = false

    fun masked(last4: String): String = "··$last4"
}

/**
 * TLS and certificate-pinning hooks. No live network ships in this scaffold.
 * Replace [spkiPins] with the production SPKI pin before any API host is called.
 */
object TlsPolicy {
    const val cleartextAllowed: Boolean = false
    const val apiHost: String = "api.aicfo.app"
    val spkiPins: List<String> = listOf("sha256/REPLACE_WITH_PRODUCTION_SPKI_PIN")

    fun requireHttps(url: String) {
        require(url.startsWith("https://")) { "Cleartext is not allowed" }
    }
}

object SafeLog {
    private val token = Regex("""link_[A-Za-z0-9_\-]+""")
    private val secretAssign = Regex("""(?i)\b(password|passwd|secret|token|ssn|cvv)\b\s*[:=]\s*\S+""")
    private val email = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val longNumber = Regex("""\b\d{13,19}\b""")

    fun redact(message: String): String {
        val secrets = secretAssign.replace(message) { match ->
            val key = match.groupValues[1]
            "$key=[redacted]"
        }
        return secrets
            .replace(token, "link_[redacted]")
            .replace(email, "[redacted-email]")
            .replace(longNumber, "[redacted-number]")
    }

    /** Returns the redacted line. Does not forward the original to a platform log. */
    fun debug(tag: String, message: String): String = redact("[$tag] $message")
}
