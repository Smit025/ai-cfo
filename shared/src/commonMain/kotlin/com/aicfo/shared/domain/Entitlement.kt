package com.aicfo.shared.domain

import com.aicfo.shared.market.MoneyFormat
import com.aicfo.shared.market.UsMarketPack
import com.aicfo.shared.presentation.EntitlementSnapshot
import com.aicfo.shared.presentation.Phase
import com.aicfo.shared.presentation.QaOverride

object Pricing {
    const val TRIAL_DAYS: Int = 30
    val MONTHLY_CENTS: Long = UsMarketPack.monthlyPrice.minor
    val YEARLY_CENTS: Long = UsMarketPack.yearlyPrice.minor
    val MONTHLY_LABEL: String get() = MoneyFormat.standard(UsMarketPack.monthlyPrice)
    val YEARLY_LABEL: String get() = MoneyFormat.standard(UsMarketPack.yearlyPrice)
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
        monthlyLabel: String = Pricing.MONTHLY_LABEL,
        yearlyLabel: String = Pricing.YEARLY_LABEL,
    ): EntitlementSnapshot {
        if (overrideCode == QaOverride.PAYWALL) {
            return snapshot(Phase.PAYWALL, 0, trialConsumed = trialStartedAtMs != null, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        if (overrideCode == QaOverride.TRIAL) {
            return snapshot(Phase.TRIAL, Pricing.TRIAL_DAYS, trialConsumed = false, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        if (overrideCode == QaOverride.PRO) {
            return snapshot(Phase.PRO, 0, trialConsumed = true, plan = subscribedPlan.ifBlank { "MONTHLY" }, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        if (subscribedPlan == "MONTHLY" || subscribedPlan == "YEARLY") {
            return snapshot(Phase.PRO, 0, trialConsumed = true, plan = subscribedPlan, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        if (trialStartedAtMs == null) {
            return snapshot(Phase.PAYWALL, 0, trialConsumed = false, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        val elapsed = nowMs - trialStartedAtMs
        if (elapsed < 0) {
            return snapshot(Phase.TRIAL, Pricing.TRIAL_DAYS, trialConsumed = false, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        val left = Pricing.TRIAL_WINDOW_MS - elapsed
        if (left <= 0L) {
            return snapshot(Phase.PAYWALL, 0, trialConsumed = true, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
        }
        val days = ((left + Pricing.DAY_MS - 1) / Pricing.DAY_MS).toInt()
        return snapshot(Phase.TRIAL, days, trialConsumed = false, monthlyLabel = monthlyLabel, yearlyLabel = yearlyLabel)
    }

    private fun snapshot(
        phase: String,
        daysRemaining: Int,
        trialConsumed: Boolean,
        plan: String = "NONE",
        monthlyLabel: String,
        yearlyLabel: String,
    ): EntitlementSnapshot {
        val label = when (phase) {
            Phase.TRIAL -> "Pro trial · $daysRemaining days left"
            Phase.PRO -> if (plan == "YEARLY") "AI CFO Pro · yearly" else "AI CFO Pro · monthly"
            else -> if (trialConsumed) "Trial ended" else "Pro required"
        }
        val detail = when (phase) {
            Phase.TRIAL -> "Then $monthlyLabel/month or $yearlyLabel/year. No free plan."
            Phase.PRO -> if (plan == "YEARLY") {
                "Subscribed · $yearlyLabel/year (simulated)."
            } else {
                "Subscribed · $monthlyLabel/month (simulated)."
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

