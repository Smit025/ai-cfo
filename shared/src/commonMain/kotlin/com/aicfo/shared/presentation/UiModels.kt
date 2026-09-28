package com.aicfo.shared.presentation

object Gate {
    const val ONBOARDING = "ONBOARDING"
    const val LOCK = "LOCK"
    const val PAYWALL = "PAYWALL"
    const val APP = "APP"
}

object Phase {
    const val TRIAL = "TRIAL"
    const val PAYWALL = "PAYWALL"
    const val PRO = "PRO"
}

object QaOverride {
    const val NONE = "NONE"
    const val TRIAL = "TRIAL"
    const val PAYWALL = "PAYWALL"
    const val PRO = "PRO"
}

object MoveStatusCode {
    const val TODO = "TODO"
    const val DONE = "DONE"
    const val SKIPPED = "SKIPPED"
}

object LayoutCode {
    const val DEBT = "DEBT"
    const val SUBSCRIPTION = "SUBSCRIPTION"
    const val CASH = "CASH"
    const val SIMPLE = "SIMPLE"
}

object Tone {
    const val POSITIVE = "POSITIVE"
    const val WARNING = "WARNING"
    const val DANGER = "DANGER"
    const val DEFAULT = "DEFAULT"
}

data class HomeMoveModel(
    val id: String,
    val title: String,
    val impact: String,
    val body: String,
    val cta: String,
    val icon: String,
)

data class HomeModel(
    val greeting: String,
    val subtitle: String,
    val initials: String,
    val showNotificationDot: Boolean,
    val pulse: String,
    val sectionTitle: String,
    val seeAllLabel: String,
    val emptyTitle: String,
    val emptyBody: String,
    val moves: List<HomeMoveModel>,
) {
    fun moveCount(): Int = moves.size
    fun moveAt(index: Int): HomeMoveModel = moves[index]
}

data class MoveRowModel(
    val id: String,
    val rank: Int,
    val title: String,
    val impact: String,
    val impactTone: String,
    val priority: String,
    val status: String,
    val statusLabel: String,
    val showCheck: Boolean,
)

data class MovesModel(
    val title: String,
    val subtitle: String,
    val summaryPill: String,
    val todoCount: Int,
    val doneCount: Int,
    val skippedCount: Int,
    val moves: List<MoveRowModel>,
) {
    fun moveCount(): Int = moves.size
    fun moveAt(index: Int): MoveRowModel = moves[index]
}

data class FactModel(
    val value: String,
    val label: String,
    val tone: String,
)

data class DetailModel(
    val id: String,
    val backLabel: String,
    val priority: String,
    val category: String,
    val status: String,
    val statusLabel: String,
    val title: String,
    val lede: String,
    val layout: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val accountLine: String,
    val banner: String,
    val heroValue: String,
    val heroCaption: String,
    val heroSub: String,
    val sectionLabel: String,
    val statLeftValue: String,
    val statLeftLabel: String,
    val statRightValue: String,
    val statRightLabel: String,
    val merchantName: String,
    val merchantMeta: String,
    val merchantInitial: String,
    val merchantColorHex: String,
    val freeUpLabel: String,
    val freeUpValue: String,
    val facts: List<FactModel>,
) {
    fun factCount(): Int = facts.size
    fun factAt(index: Int): FactModel = facts[index]
}

data class AccountRowModel(
    val id: String,
    val name: String,
    val detail: String,
    val balance: String,
    val initials: String,
    val colorHex: String,
    val readOnlyLabel: String,
)

data class AccountGroupModel(
    val title: String,
    val accounts: List<AccountRowModel>,
) {
    fun accountCount(): Int = accounts.size
    fun accountAt(index: Int): AccountRowModel = accounts[index]
}

data class AccountsModel(
    val title: String,
    val subtitle: String,
    val trust: String,
    val linked: Boolean,
    val emptyTitle: String,
    val emptyBody: String,
    val emptyCta: String,
    val groups: List<AccountGroupModel>,
) {
    fun groupCount(): Int = groups.size
    fun groupAt(index: Int): AccountGroupModel = groups[index]
}

data class OnboardingModel(
    val step: Int,
    val stepCount: Int,
    val kicker: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val banksLinked: Boolean,
    val canAdvance: Boolean,
    val canGoBack: Boolean,
    val showConnect: Boolean,
    val connectCta: String,
    val linkedSummary: String,
    val priceLeft: String,
    val priceRight: String,
    val priceNote: String,
    val previewTitle: String,
    val previewImpact: String,
    val previewBody: String,
    val bullets: List<String>,
) {
    fun bulletCount(): Int = bullets.size
    fun bulletAt(index: Int): String = bullets[index]
}

data class PaywallModel(
    val title: String,
    val lede: String,
    val monthlyPrice: String,
    val monthlyPeriod: String,
    val yearlyPrice: String,
    val yearlyPeriod: String,
    val yearlyNote: String,
    val finePrint: String,
    val monthlyCta: String,
    val yearlyCta: String,
)

data class SettingsModel(
    val name: String,
    val meta: String,
    val initials: String,
    val notificationsEnabled: Boolean,
    val biometricEnabled: Boolean,
    val biometricHardware: Boolean,
    val planLabel: String,
    val planDetail: String,
    val banksLinked: Boolean,
    val phase: String,
    val qaEnabled: Boolean,
)

data class LockModel(
    val title: String,
    val body: String,
    val primaryCta: String,
    val hardwareAvailable: Boolean,
)

data class EntitlementSnapshot(
    val phase: String,
    val daysRemaining: Int,
    val planLabel: String,
    val planDetail: String,
    val trialConsumed: Boolean,
)
