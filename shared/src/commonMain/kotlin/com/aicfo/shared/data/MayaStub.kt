package com.aicfo.shared.data

import com.aicfo.shared.market.CurrencyCode
import com.aicfo.shared.market.Money
import com.aicfo.shared.market.MoneyFormat
import com.aicfo.shared.model.AccountGroup
import com.aicfo.shared.model.CoachMove
import com.aicfo.shared.model.DetailCopy
import com.aicfo.shared.model.Fact
import com.aicfo.shared.model.LinkedAccount
import com.aicfo.shared.model.MoveKind
import com.aicfo.shared.model.MoveStatus
import com.aicfo.shared.model.Priority
import com.aicfo.shared.model.Profile
import com.aicfo.shared.presentation.LayoutCode
import com.aicfo.shared.presentation.Tone

/**
 * October 2026 coach plan for Maya Chen, freelance product designer in Austin.
 * Figures on the cards are the authored plan (they match the look pack).
 * Home v1.2 wealth and snapshot figures are the locked board, not a sum of the
 * linked balances. Amounts are [Money] in USD. Gympass yearly savings are
 * [com.aicfo.shared.market.MoneyMath].
 */
internal object MayaStub {
    val profile: Profile = Profile(
        id = "maya",
        firstName = "Maya",
        lastName = "Chen",
        city = "Austin",
        region = "TX",
        occupation = "Freelance product designer",
    )

    const val HOME_SUBTITLE: String = "Tuesday · Austin"
    const val PLAN_YEAR: Int = 2026
    const val PLAN_MONTH: Int = 10
    const val SAVINGS_LABEL: String = "Savings"
    val SAVINGS: Money = usd(842_000)
    val SAVINGS_DELTA: Money = usd(34_000)
    const val NET_WORTH_LABEL: String = "Net worth"
    val NET_WORTH: Money = usd(4_210_000)
    const val NEEDS_LABEL: String = "Needs"
    val NEEDS: Money = usd(284_000)
    const val NEEDS_CAPTION: String = "rent, groceries..."
    const val WANTS_LABEL: String = "Wants"
    val WANTS: Money = usd(62_000)
    const val WANTS_CAPTION: String = "discretionary"
    const val SAVE_LABEL: String = "To save"
    val TO_SAVE: Money = usd(89_000)
    const val SAVE_CAPTION: String = "left this month"
    val GYMPASS_MONTHLY: Money = usd(4_700)
    val CARD_SAVE: Money = usd(18_000)
    val IDLE_MONTHLY: Money = usd(1_800)
    val IDLE_CASH: Money = usd(420_000)
    const val CAPITAL_ONE_APR_BPS: Int = 2_490
    const val SUMMARY_PILL: String = "October · 5 moves · ~\$265/mo upside"
    const val SECTION_TITLE: String = "This month's moves"

    val accounts: List<LinkedAccount> = listOf(
        LinkedAccount(
            id = "chase-checking",
            name = "Chase Checking",
            maskLine = "··4821 · 0.01% APY",
            balance = usd(481_200),
            balanceLabel = MoneyFormat.standard(usd(481_200)),
            group = AccountGroup.CASH,
            initials = "CH",
            colorHex = "#2563EB",
        ),
        LinkedAccount(
            id = "chase-savings",
            name = "Chase Savings",
            maskLine = "··1190 · emergency",
            balance = usd(240_000),
            balanceLabel = MoneyFormat.standard(usd(240_000)),
            group = AccountGroup.CASH,
            initials = "CH",
            colorHex = "#2563EB",
        ),
        LinkedAccount(
            id = "amex",
            name = "Amex Blue Cash",
            maskLine = "··1008 · due Oct 12",
            balance = usd(124_000),
            balanceLabel = MoneyFormat.standard(usd(124_000)),
            group = AccountGroup.CARDS_AND_LOANS,
            initials = "AX",
            colorHex = "#2563EB",
        ),
        LinkedAccount(
            id = "capital-one",
            name = "Capital One Quicksilver",
            maskLine = "··4912 · 24.9% APR",
            balance = usd(384_000),
            balanceLabel = MoneyFormat.standard(usd(384_000)),
            group = AccountGroup.CARDS_AND_LOANS,
            initials = "C1",
            colorHex = "#DC2626",
        ),
        LinkedAccount(
            id = "nelnet",
            name = "Nelnet Student Loan",
            maskLine = "Federal · 5.5%",
            balance = usd(1_842_000),
            balanceLabel = MoneyFormat.standard(usd(1_842_000)),
            group = AccountGroup.CARDS_AND_LOANS,
            initials = "NL",
            colorHex = "#1E293B",
        ),
        LinkedAccount(
            id = "fidelity",
            name = "Fidelity Brokerage",
            maskLine = "··7743 · taxable",
            balance = usd(689_000),
            balanceLabel = MoneyFormat.standard(usd(689_000)),
            group = AccountGroup.INVESTMENTS,
            initials = "FD",
            colorHex = "#059669",
        ),
    )

    val moves: List<CoachMove> = listOf(
        move(
            id = "gympass",
            rank = 1,
            kind = MoveKind.CANCEL_SUBSCRIPTION,
            listTitle = "Cancel unused Gympass",
            homeTitle = "Cancel unused Gympass",
            impact = MoneyFormat.signedMonthly(GYMPASS_MONTHLY, "/mo"),
            tone = Tone.POSITIVE,
            priority = Priority.P1,
            status = MoveStatus.TODO,
            homeBody = "Last check-in 86 days ago — paying for nothing.",
            homeCta = "Cancel · save \$47/mo",
            icon = "CLOCK",
            detail = detail(
                title = "Cancel unused Gympass",
                category = "Useless subscription",
                lede = "You're paying for a gym you haven't walked into since July.",
                layout = LayoutCode.SUBSCRIPTION,
                body = "Spotify and Adobe Creative Cloud look active. Gympass is the clear cancel — you already have a climbing gym membership via class packs.",
                primary = "Open cancel page",
                secondary = "Keep it",
                accountLine = "Amex · ··1008 · Read-only",
                merchantName = "Gympass",
                merchantMeta = "Fitness · billed via Amex",
                merchantInitial = "G",
                merchantColor = "#16A34A",
                freeUpLabel = "You'd free up",
                freeUpValue = "\$564 / year",
                facts = listOf(
                    Fact("\$47", "per month", Tone.DEFAULT),
                    Fact("86 days", "since last use", Tone.WARNING),
                    Fact("Jul 5", "last check-in", Tone.DEFAULT),
                    Fact("\$423", "spent unused YTD", Tone.DEFAULT),
                ),
            ),
        ),
        move(
            id = "capital-one",
            rank = 2,
            kind = MoveKind.EXTRA_DEBT_PAYMENT,
            listTitle = "Pay \$400 extra on Capital One",
            homeTitle = "Pay \$400 extra on Capital One",
            impact = "Save ${MoneyFormat.standard(CARD_SAVE)}",
            tone = Tone.POSITIVE,
            priority = Priority.P1,
            status = MoveStatus.TODO,
            homeBody = "24.9% APR eating ~\$62/mo. Kill it first.",
            homeCta = "Schedule extra payment",
            icon = "CARD",
            detail = detail(
                title = "Pay down your Capital One card",
                category = "Expensive debt",
                lede = "This is your most expensive debt. Every extra dollar here beats investing this month.",
                layout = LayoutCode.DEBT,
                body = "Keep the minimum (~\$95), then schedule \$400 extra from checking on Friday after your client invoice lands. You'll free ~\$62/mo in interest drag within a quarter.",
                primary = "Schedule extra payment",
                secondary = "Remind me Friday",
                accountLine = "Capital One · ··4912 · Read-only",
                heroValue = "24.9%",
                heroCaption = "APR on \$3,840 balance",
                heroSub = "≈ \$62 interest this month",
                sectionLabel = "If you pay \$400 extra this month",
                statLeftValue = "\$180",
                statLeftLabel = "interest saved",
                statRightValue = "4 mo",
                statRightLabel = "faster payoff",
            ),
        ),
        move(
            id = "idle-cash",
            rank = 3,
            kind = MoveKind.MOVE_IDLE_CASH,
            listTitle = "Move idle cash to HYSA",
            homeTitle = "Move \$4,200 idle cash to HYSA",
            impact = MoneyFormat.signedMonthly(IDLE_MONTHLY, "/mo"),
            tone = Tone.POSITIVE,
            priority = Priority.P2,
            status = MoveStatus.TODO,
            homeBody = "0.01% checking → Ally 4.20% APY.",
            homeCta = "Open transfer guide",
            icon = "CASH",
            detail = detail(
                title = "Move \$4,200 idle cash to a HYSA",
                category = "Idle cash",
                lede = "This cash is sitting in 0.01% checking. Ally pays 4.20% APY.",
                layout = LayoutCode.CASH,
                body = "Keep enough in checking for rent (\$1,850) and the Capital One extra payment. Move the idle \$4,200 yourself — AI CFO will not transfer it.",
                primary = "Open transfer guide",
                secondary = "Remind me later",
                accountLine = "Chase Checking · ··4821 · Read-only",
                heroValue = "0.01%",
                heroCaption = "checking APY now",
                heroSub = "Ally pays 4.20% APY",
                sectionLabel = "If you move \$4,200",
                statLeftValue = "+\$18",
                statLeftLabel = "more per month",
                statRightValue = "~\$216",
                statRightLabel = "over a year",
            ),
        ),
        move(
            id = "rent",
            rank = 4,
            kind = MoveKind.PAY_RENT,
            listTitle = "Pay rent before Oct 1",
            homeTitle = "Pay rent before Oct 1",
            impact = "Avoid late fee",
            tone = Tone.WARNING,
            priority = Priority.P2,
            status = MoveStatus.DONE,
            homeBody = "Rent is \$1,850. October is already covered.",
            homeCta = "View rent move",
            icon = "CASH",
            detail = detail(
                title = "Pay rent before Oct 1",
                category = "Housing",
                lede = "Rent is \$1,850. Paying before the 1st avoids the late fee.",
                layout = LayoutCode.SIMPLE,
                body = "October is already marked done. Keep a checking buffer so the next ACH doesn't bounce.",
                primary = "Marked done",
                secondary = "Remind me next month",
                accountLine = "Chase Checking · ··4821 · Read-only",
                heroValue = "\$1,850",
                heroCaption = "rent · Austin",
                heroSub = "Due before Oct 1 · paid",
            ),
        ),
        move(
            id = "refinance",
            rank = 5,
            kind = MoveKind.REFINANCE_CHECK,
            listTitle = "Refinance student loan check",
            homeTitle = "Refinance student loan check",
            impact = "${MoneyFormat.approx(usd(4_000))}/mo?",
            tone = Tone.POSITIVE,
            priority = Priority.P3,
            status = MoveStatus.SKIPPED,
            homeBody = "Federal 5.5% on \$18,420. A private refi can cost protections.",
            homeCta = "Review the check",
            icon = "CARD",
            detail = detail(
                title = "Refinance student loan check",
                category = "Student loan",
                lede = "A private refinance might save about \$40 a month — and it can cost federal protections.",
                layout = LayoutCode.SIMPLE,
                body = "Nelnet is a federal loan at 5.5%. Refinancing privately can lower the rate, but you give up income-driven repayment and forgiveness paths. Left skipped until a quote clearly wins.",
                primary = "Reopen check",
                secondary = "Keep skipped",
                accountLine = "Nelnet · Federal · Read-only",
                heroValue = "5.5%",
                heroCaption = "Federal · \$18,420 balance",
                heroSub = "~\$40/mo is the optimistic case",
            ),
        ),
    )

    fun move(id: String): CoachMove = moves.first { it.id == id }
}

private fun move(
    id: String,
    rank: Int,
    kind: MoveKind,
    listTitle: String,
    homeTitle: String,
    impact: String,
    tone: String,
    priority: Priority,
    status: MoveStatus,
    homeBody: String,
    homeCta: String,
    icon: String,
    detail: DetailCopy,
): CoachMove = CoachMove(
    id = id,
    rank = rank,
    kind = kind,
    listTitle = listTitle,
    homeTitle = homeTitle,
    impactLabel = impact,
    impactTone = tone,
    priority = priority,
    defaultStatus = status,
    homeBody = homeBody,
    homeCta = homeCta,
    icon = icon,
    detail = detail,
)

private fun detail(
    title: String,
    category: String,
    lede: String,
    layout: String,
    body: String,
    primary: String,
    secondary: String,
    accountLine: String,
    heroValue: String = "",
    heroCaption: String = "",
    heroSub: String = "",
    sectionLabel: String = "",
    statLeftValue: String = "",
    statLeftLabel: String = "",
    statRightValue: String = "",
    statRightLabel: String = "",
    merchantName: String = "",
    merchantMeta: String = "",
    merchantInitial: String = "",
    merchantColor: String = "",
    freeUpLabel: String = "",
    freeUpValue: String = "",
    facts: List<Fact> = emptyList(),
): DetailCopy = DetailCopy(
    title = title,
    category = category,
    lede = lede,
    layout = layout,
    body = body,
    primaryCta = primary,
    secondaryCta = secondary,
    accountLine = accountLine,
    heroValue = heroValue,
    heroCaption = heroCaption,
    heroSub = heroSub,
    sectionLabel = sectionLabel,
    statLeftValue = statLeftValue,
    statLeftLabel = statLeftLabel,
    statRightValue = statRightValue,
    statRightLabel = statRightLabel,
    merchantName = merchantName,
    merchantMeta = merchantMeta,
    merchantInitial = merchantInitial,
    merchantColorHex = merchantColor,
    freeUpLabel = freeUpLabel,
    freeUpValue = freeUpValue,
    facts = facts,
)

private fun usd(minor: Long): Money = Money(minor, CurrencyCode.USD)
