package com.aicfo.shared.auth

/**
 * Account session persisted after the first successful phone OTP.
 * Biometric and PIN checks are not part of this record — they never sign the user in.
 */
data class AuthSession(
    val phoneE164: String,
    val issuedAtMs: Long,
    val token: String,
) {
    fun encode(): String = "$phoneE164|$issuedAtMs|$token"

    companion object {
        fun decode(raw: String): AuthSession? {
            val parts = raw.split('|')
            if (parts.size != 3) return null
            if (!PhoneNumbers.isValidUs(parts[0])) return null
            val issued = parts[1].toLongOrNull() ?: return null
            if (parts[2].isBlank()) return null
            return AuthSession(parts[0], issued, parts[2])
        }
    }
}

/** Local device-unlock choices. Stored apart from [AuthSession]. */
data class DeviceUnlockPrefs(
    val biometricEnabled: Boolean,
    val pinSet: Boolean,
    val passcodeFallback: Boolean,
    val setupComplete: Boolean,
)

internal object PinSecret {
    fun seal(pin: String, salt: String): String = "$salt:${Sha256.hex("$salt:$pin")}"

    fun matches(pin: String, sealed: String): Boolean {
        val salt = sealed.substringBefore(':', missingDelimiterValue = "")
        if (salt.isEmpty() || !sealed.contains(':')) return false
        val expected = seal(pin, salt)
        if (expected.length != sealed.length) return false
        var diff = 0
        for (i in expected.indices) {
            diff = diff or (expected[i].code xor sealed[i].code)
        }
        return diff == 0
    }
}

internal object SecureKeys {
    const val SESSION = "auth.session"
    const val PIN = "auth.device_pin"
}
