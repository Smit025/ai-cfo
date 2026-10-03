package com.aicfo.shared.auth

import com.aicfo.shared.presentation.Gate

/**
 * Cold-start router.
 *
 * First launch (no session): onboarding intro (welcome, actions, connect) → AUTH
 * (email, then a code or magic link, then unlock setup) → trial step → lock or paywall or main.
 *
 * Returning cold start: session is already stored, so the email prompt is skipped.
 * Device unlock runs before the paywall. Logout, reinstall, or a cleared
 * secure record is what brings the email prompt back.
 *
 * [authSetupComplete] stays false after the address is verified until an unlock
 * path exists, so a kill during PIN setup resumes AUTH.
 */
object AppGate {
    fun resolve(
        hasSession: Boolean,
        introComplete: Boolean,
        authSetupComplete: Boolean,
        onboardingComplete: Boolean,
        deviceUnlockNeeded: Boolean,
        entitled: Boolean,
    ): String {
        if (!introComplete) return Gate.ONBOARDING
        if (!hasSession || !authSetupComplete) return Gate.AUTH
        if (!onboardingComplete) return Gate.ONBOARDING
        if (deviceUnlockNeeded) return Gate.LOCK
        if (!entitled) return Gate.PAYWALL
        return Gate.APP
    }
}
