package com.aicfo.shared.presentation

object Gate {
    const val ONBOARDING = "ONBOARDING"
    const val AUTH = "AUTH"
    const val LOCK = "LOCK"
    const val PAYWALL = "PAYWALL"
    /** Main shell (Home / Moves / Accounts / Settings). */
    const val APP = "APP"
}

object AuthStep {
    const val PHONE = "PHONE"
    const val OTP = "OTP"
    const val EMAIL = "EMAIL"
    const val UNLOCK = "UNLOCK"
    const val DONE = "DONE"
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

/** One quiet amount in the Home Needs · Wants · To save row. */
data class HomeAmountModel(
    val label: String,
    val amount: String,
    val caption: String,
    val tone: String,
)

data class HomeModel(
    val greeting: String,
    val subtitle: String,
    val initials: String,
    val showNotificationDot: Boolean,
    val savingsLabel: String,
    val savingsAmount: String,
    val savingsDelta: String,
    val savingsUp: Boolean,
    val netWorthLabel: String,
    val netWorthAmount: String,
    val netWorthDelta: String,
    val netWorthUp: Boolean,
    val runway: String,
    val hope: String,
    val sectionTitle: String,
    val seeAllLabel: String,
    val emptyTitle: String,
    val emptyBody: String,
    val snapshot: List<HomeAmountModel>,
    val moves: List<HomeMoveModel>,
    val freshnessLabel: String,
    val syncCode: String,
    val syncActionLabel: String,
    val syncStale: Boolean,
) {
    fun snapshotCount(): Int = snapshot.size
    fun snapshotAt(index: Int): HomeAmountModel = snapshot[index]
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
    val linkError: String,
    val groups: List<AccountGroupModel>,
    val freshnessLabel: String,
    val syncCode: String,
    val syncActionLabel: String,
    val syncStale: Boolean,
    val linkNote: String = "",
    val sampleCta: String = "",
) {
    fun groupCount(): Int = groups.size
    fun groupAt(index: Int): AccountGroupModel = groups[index]
}

data class OnboardingCard(
    val title: String,
    val subtitle: String,
    val impact: String,
    val icon: String,
)

data class OnboardingModel(
    val step: Int,
    val stepCount: Int,
    val kicker: String,
    val descriptor: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val footnote: String,
    val badge: String,
    val sectionLabel: String,
    val trustTitle: String,
    val trustBody: String,
    val banksLinked: Boolean,
    val canAdvance: Boolean,
    val cards: List<OnboardingCard>,
    val features: List<OnboardingCard>,
    val connectTypes: List<OnboardingCard>,
    val chips: List<String>,
    val linkError: String,
    val linkNote: String = "",
) {
    fun cardCount(): Int = cards.size
    fun cardAt(index: Int): OnboardingCard = cards[index]
    fun featureCount(): Int = features.size
    fun featureAt(index: Int): OnboardingCard = features[index]
    fun typeCount(): Int = connectTypes.size
    fun typeAt(index: Int): OnboardingCard = connectTypes[index]
    fun chipCount(): Int = chips.size
    fun chipAt(index: Int): String = chips[index]
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
    val brandTagline: String,
    val notificationsEnabled: Boolean,
    val biometricEnabled: Boolean,
    val biometricHardware: Boolean,
    val planLabel: String,
    val planDetail: String,
    val banksLinked: Boolean,
    val phase: String,
    val qaEnabled: Boolean,
    val signedIn: Boolean = false,
    val phoneMask: String = "",
    val deviceLockReady: Boolean = false,
    val sampleLink: Boolean = false,
)

data class LockModel(
    val brand: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val hardwareAvailable: Boolean,
    val kicker: String = "",
    val secondaryCta: String = "",
    val methodTitle: String = "",
    val methodDetail: String = "",
    val pinSet: Boolean = false,
    val mustCreatePin: Boolean = false,
    val preferPin: Boolean = false,
    val passcodeFallback: Boolean = false,
)

data class EntitlementSnapshot(
    val phase: String,
    val daysRemaining: Int,
    val planLabel: String,
    val planDetail: String,
    val trialConsumed: Boolean,
)
