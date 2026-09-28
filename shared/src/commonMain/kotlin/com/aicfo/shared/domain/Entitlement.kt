package com.aicfo.shared.domain

import com.aicfo.shared.presentation.EntitlementSnapshot
import com.aicfo.shared.presentation.Phase
import com.aicfo.shared.presentation.QaOverride

object Pricing {
    const val TRIAL_DAYS: Int = 30
    const val MONTHLY_CENTS: Long = 999
    const val YEARLY_CENTS: Long = 7900
    const val MONTHLY_LABEL: String = "\$9.99"
    const val YEARLY_LABEL: String = "\$79"
    const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    const val TRIAL_WINDOW_MS: Long = TRIAL_DAYS * DAY_MS
}

interface AppClock {
    fun nowEpochMs(): Long
}

class SystemAppClock : AppClock {
    override fun nowEpochMs(): Long = platformNowMillis()
}

internal expect fun platformNowMillis(): Long

object EntitlementPolicy {
    fun resolve(
        nowMs: Long,
        trialStartedAtMs: Long?,
        overrideCode: String,
        subscribedPlan: String,
    ): EntitlementSnapshot {
        if (overrideCode == QaOverride.PAYWALL) {
            return snapshot(Phase.PAYWALL, 0, trialConsumed = trialStartedAtMs != null)
        }
        if (overrideCode == QaOverride.TRIAL) {
            return snapshot(Phase.TRIAL, Pricing.TRIAL_DAYS, trialConsumed = false)
        }
        if (overrideCode == QaOverride.PRO) {
            return snapshot(Phase.PRO, 0, trialConsumed = true, plan = subscribedPlan.ifBlank { "MONTHLY" })
        }
        if (subscribedPlan == "MONTHLY" || subscribedPlan == "YEARLY") {
            return snapshot(Phase.PRO, 0, trialConsumed = true, plan = subscribedPlan)
        }
        if (trialStartedAtMs == null) {
            return snapshot(Phase.PAYWALL, 0, trialConsumed = false)
        }
        val elapsed = nowMs - trialStartedAtMs
        if (elapsed < 0) {
            return snapshot(Phase.TRIAL, Pricing.TRIAL_DAYS, trialConsumed = false)
        }
        val left = Pricing.TRIAL_WINDOW_MS - elapsed
        if (left <= 0L) {
            return snapshot(Phase.PAYWALL, 0, trialConsumed = true)
        }
        val days = ((left + Pricing.DAY_MS - 1) / Pricing.DAY_MS).toInt()
        return snapshot(Phase.TRIAL, days, trialConsumed = false)
    }

    private fun snapshot(
        phase: String,
        daysRemaining: Int,
        trialConsumed: Boolean,
        plan: String = "NONE",
    ): EntitlementSnapshot {
        val label = when (phase) {
            Phase.TRIAL -> "Pro trial · $daysRemaining days left"
            Phase.PRO -> if (plan == "YEARLY") "AI CFO Pro · yearly" else "AI CFO Pro · monthly"
            else -> if (trialConsumed) "Trial ended" else "Pro required"
        }
        val detail = when (phase) {
            Phase.TRIAL -> "Then ${Pricing.MONTHLY_LABEL}/month or ${Pricing.YEARLY_LABEL}/year. No free plan."
            Phase.PRO -> if (plan == "YEARLY") {
                "Subscribed · ${Pricing.YEARLY_LABEL}/year (simulated)."
            } else {
                "Subscribed · ${Pricing.MONTHLY_LABEL}/month (simulated)."
            }
            else -> "Subscribe to keep this month's moves. There is no free plan."
        }
        return EntitlementSnapshot(
            phase = phase,
            daysRemaining = daysRemaining,
            planLabel = label,
            planDetail = detail,
            trialConsumed = trialConsumed,
        )
    }
}

/** \$47 x 12 = \$564, the Gympass yearly figure on the action screen. */
object MoneyMath {
    fun yearlyFromMonthlyCents(monthlyCents: Long): Long = monthlyCents * 12L
}
