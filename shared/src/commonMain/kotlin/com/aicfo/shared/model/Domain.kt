package com.aicfo.shared.model

import com.aicfo.shared.market.CopyKey
import com.aicfo.shared.market.Money

internal enum class MoveKind {
    CANCEL_SUBSCRIPTION,
    EXTRA_DEBT_PAYMENT,
    MOVE_IDLE_CASH,
    PAY_RENT,
    REFINANCE_CHECK,
}

internal enum class MoveStatus {
    TODO,
    DONE,
    SKIPPED,
}

internal enum class Priority {
    P1,
    P2,
    P3,
}

internal enum class AccountGroup {
    CASH,
    CARDS_AND_LOANS,
    INVESTMENTS,
}

internal data class Profile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val city: String,
    val region: String,
    val occupation: String,
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String get() = "${firstName.take(1)}${lastName.take(1)}"
}

internal data class LinkedAccount(
    val id: String,
    val name: String,
    val maskLine: String,
    val balance: Money,
    val balanceLabel: String,
    val group: AccountGroup,
    val initials: String,
    val colorHex: String,
)

internal data class Fact(
    val value: String,
    val label: String,
    val tone: String,
)

internal data class DetailCopy(
    val title: String,
    val category: String,
    val lede: String,
    val layout: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val accountLine: String,
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
    val facts: List<Fact>,
)

internal data class CoachMove(
    val id: String,
    val rank: Int,
    val kind: MoveKind,
    val listTitle: String,
    val homeTitle: String,
    val impactLabel: String,
    val impactTone: String,
    val priority: Priority,
    val defaultStatus: MoveStatus,
    val homeBody: String,
    val homeCta: String,
    val icon: String,
    val detail: DetailCopy,
)

internal fun Priority.wire(): String = name

internal fun MoveStatus.wire(): String = name

internal fun AccountGroup.copyKey(): String = when (this) {
    AccountGroup.CASH -> CopyKey.GROUP_CASH
    AccountGroup.CARDS_AND_LOANS -> CopyKey.GROUP_CARDS
    AccountGroup.INVESTMENTS -> CopyKey.GROUP_INVESTMENTS
}
