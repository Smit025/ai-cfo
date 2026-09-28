package com.aicfo.shared.auth

/**
 * SMS one-time-password provider.
 *
 * The debug stub accepts [StubOtpAuthRepository.DEBUG_CODE]. A later Firebase Phone Auth
 * or Twilio Verify client implements this same interface. This scaffold does not ship provider keys.
 */
interface OtpAuthRepository {
    fun requestCode(phoneE164: String): OtpResult
    fun verifyCode(phoneE164: String, code: String): OtpResult
}

data class OtpResult(
    val ok: Boolean,
    val message: String = "",
)

/** Debug / QA provider. Release builds use [UnconfiguredOtpAuthRepository] until a live provider is wired. */
class StubOtpAuthRepository : OtpAuthRepository {
    private var sends: Int = 0

    override fun requestCode(phoneE164: String): OtpResult {
        if (!PhoneNumbers.isValidUs(phoneE164)) {
            return OtpResult(false, "Enter a valid US mobile number.")
        }
        sends += 1
        if (sends > 5) {
            return OtpResult(false, "Too many codes. Wait a moment and try again.")
        }
        return OtpResult(true)
    }

    override fun verifyCode(phoneE164: String, code: String): OtpResult {
        if (code == DEBUG_CODE) return OtpResult(true)
        return OtpResult(false, "That code is wrong or expired.")
    }

    companion object {
        const val DEBUG_CODE = "000000"
    }
}

/** Placeholder until Firebase Phone Auth or Twilio Verify is connected. Never accepts a code. */
class UnconfiguredOtpAuthRepository : OtpAuthRepository {
    override fun requestCode(phoneE164: String): OtpResult =
        OtpResult(false, "Text codes aren't connected yet.")

    override fun verifyCode(phoneE164: String, code: String): OtpResult =
        OtpResult(false, "Text codes aren't connected yet.")
}
