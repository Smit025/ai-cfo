package com.aicfo.shared.domain

import com.aicfo.shared.data.MayaStub
import com.aicfo.shared.model.CoachMove
import com.aicfo.shared.model.title
import com.aicfo.shared.model.wire
import com.aicfo.shared.presentation.AccountGroupModel
import com.aicfo.shared.presentation.AccountRowModel
import com.aicfo.shared.presentation.AccountsModel
import com.aicfo.shared.presentation.DetailModel
import com.aicfo.shared.presentation.FactModel
import com.aicfo.shared.presentation.HomeAmountModel
import com.aicfo.shared.presentation.HomeModel
import com.aicfo.shared.presentation.HomeMoveModel
import com.aicfo.shared.presentation.MoveRowModel
import com.aicfo.shared.presentation.MoveStatusCode
import com.aicfo.shared.presentation.MovesModel
import com.aicfo.shared.presentation.OnboardingCard
import com.aicfo.shared.presentation.OnboardingModel
import com.aicfo.shared.presentation.PaywallModel
import com.aicfo.shared.presentation.Tone

internal data class ResolvedMove(
    val move: CoachMove,
    val status: String,
    val note: String,
)

internal object HomeUseCase {
    fun build(resolved: List<ResolvedMove>, notificationsOn: Boolean): HomeModel {
        val todo = resolved
            .filter { it.status == MoveStatusCode.TODO }
            .sortedBy { it.move.rank }
            .take(3)
        return HomeModel(
            greeting = "Hello, ${MayaStub.profile.firstName}",
            subtitle = MayaStub.HOME_SUBTITLE,
            initials = MayaStub.profile.initials,
            showNotificationDot = notificationsOn,
            savingsLabel = MayaStub.SAVINGS_LABEL,
            savingsAmount = MayaStub.SAVINGS_AMOUNT,
            savingsDelta = MayaStub.SAVINGS_DELTA,
            savingsUp = true,
            netWorthLabel = MayaStub.NET_WORTH_LABEL,
            netWorthAmount = MayaStub.NET_WORTH_AMOUNT,
            netWorthDelta = MayaStub.NET_WORTH_DELTA,
            netWorthUp = true,
            runway = MayaStub.RUNWAY,
            hope = hopeLine(resolved),
            sectionTitle = MayaStub.SECTION_TITLE,
            seeAllLabel = "See all",
            emptyTitle = "You're clear this month",
            emptyBody = "Every open move is done or skipped. A new list lands at the start of next month.",
            snapshot = listOf(
                HomeAmountModel(MayaStub.NEEDS_LABEL, MayaStub.NEEDS_AMOUNT, MayaStub.NEEDS_CAPTION, Tone.DEFAULT),
                HomeAmountModel(MayaStub.WANTS_LABEL, MayaStub.WANTS_AMOUNT, MayaStub.WANTS_CAPTION, Tone.DEFAULT),
                HomeAmountModel(MayaStub.SAVE_LABEL, MayaStub.SAVE_AMOUNT, MayaStub.SAVE_CAPTION, Tone.POSITIVE),
            ),
            moves = todo.map { row ->
                HomeMoveModel(
                    id = row.move.id,
                    title = row.move.homeTitle,
                    impact = row.move.impactLabel,
                    body = row.move.homeBody,
                    cta = row.move.homeCta,
                    icon = row.move.icon,
                )
            },
        )
    }

    /**
     * Hope is the open Gympass year, or a calm confirmation once it is cancelled.
     * Keeping the subscription hides the line so Home does not scold.
     */
    private fun hopeLine(resolved: List<ResolvedMove>): String {
        val gympass = resolved.firstOrNull { it.move.id == "gympass" } ?: return ""
        val yearly = MoneyMath.yearlyFromMonthlyCents(47_00L) / 100L
        return when (gympass.status) {
            MoveStatusCode.TODO ->
                "If unused Gympass stayed cancelled this year, you'd keep ~\$$yearly more"
            MoveStatusCode.DONE ->
                "Gympass stays cancelled — about \$$yearly stays with you this year."
            else -> ""
        }
    }
}

internal object MovesUseCase {
    fun build(resolved: List<ResolvedMove>): MovesModel {
        val rows = resolved.sortedBy { it.move.rank }.map { row ->
            MoveRowModel(
                id = row.move.id,
                rank = row.move.rank,
                title = row.move.listTitle,
                impact = row.move.impactLabel,
                impactTone = row.move.impactTone,
                priority = row.move.priority.wire(),
                status = row.status,
                statusLabel = statusLabel(row.status),
                showCheck = row.status == MoveStatusCode.DONE,
            )
        }
        return MovesModel(
            title = MayaStub.SECTION_TITLE,
            subtitle = MayaStub.MOVES_SUBTITLE,
            summaryPill = MayaStub.SUMMARY_PILL,
            todoCount = rows.count { it.status == MoveStatusCode.TODO },
            doneCount = rows.count { it.status == MoveStatusCode.DONE },
            skippedCount = rows.count { it.status == MoveStatusCode.SKIPPED },
            moves = rows,
        )
    }
}

internal object DetailUseCase {
    fun build(row: ResolvedMove): DetailModel {
        val copy = row.move.detail
        return DetailModel(
            id = row.move.id,
            backLabel = "Moves",
            priority = row.move.priority.wire(),
            category = copy.category,
            status = row.status,
            statusLabel = statusLabel(row.status),
            title = copy.title,
            lede = copy.lede,
            layout = copy.layout,
            body = copy.body,
            primaryCta = copy.primaryCta,
            secondaryCta = copy.secondaryCta,
            accountLine = copy.accountLine,
            banner = row.note,
            heroValue = copy.heroValue,
            heroCaption = copy.heroCaption,
            heroSub = copy.heroSub,
            sectionLabel = copy.sectionLabel,
            statLeftValue = copy.statLeftValue,
            statLeftLabel = copy.statLeftLabel,
            statRightValue = copy.statRightValue,
            statRightLabel = copy.statRightLabel,
            merchantName = copy.merchantName,
            merchantMeta = copy.merchantMeta,
            merchantInitial = copy.merchantInitial,
            merchantColorHex = copy.merchantColorHex,
            freeUpLabel = copy.freeUpLabel,
            freeUpValue = copy.freeUpValue,
            facts = copy.facts.map { FactModel(it.value, it.label, it.tone) },
        )
    }
}

internal object AccountsUseCase {
    fun build(linked: Boolean): AccountsModel {
        val profile = MayaStub.profile
        val groups = if (!linked) {
            emptyList()
        } else {
            MayaStub.accounts
                .groupBy { it.group }
                .toList()
                .sortedBy { it.first.ordinal }
                .map { (group, accounts) ->
                    AccountGroupModel(
                        title = group.title(),
                        accounts = accounts.map { account ->
                            AccountRowModel(
                                id = account.id,
                                name = account.name,
                                detail = account.maskLine,
                                balance = account.balanceLabel,
                                initials = account.initials,
                                colorHex = account.colorHex,
                                readOnlyLabel = "Read-only",
                            )
                        },
                    )
                }
        }
        return AccountsModel(
            title = "Accounts",
            subtitle = if (linked) {
                "Connected read-only · ${profile.fullName}"
            } else {
                "Nothing linked · ${profile.fullName}"
            },
            trust = "Read-only access. We never move money or store credentials. You take every action.",
            linked = linked,
            emptyTitle = "No institutions linked",
            emptyBody = "Connections are read-only. Bank passwords are never stored on this device.",
            emptyCta = "Link read-only sample",
            groups = groups,
        )
    }
}

internal object OnboardingUseCase {
    fun build(step: Int, banksLinked: Boolean): OnboardingModel {
        val empty = OnboardingModel(
            step = step,
            stepCount = 4,
            kicker = "",
            title = "",
            body = "",
            primaryCta = "Continue",
            secondaryCta = "",
            footnote = "",
            badge = "",
            sectionLabel = "",
            trustTitle = "",
            trustBody = "",
            banksLinked = banksLinked,
            canAdvance = true,
            cards = emptyList(),
            features = emptyList(),
            connectTypes = emptyList(),
            chips = emptyList(),
        )
        return when (step) {
            0 -> empty.copy(
                kicker = "AI CFO",
                title = "Your money,\nwhat to do next",
                body = "A calm coach for this month's moves — not another budget dashboard.",
            )
            1 -> empty.copy(
                kicker = "ACTIONS, NOT CHARTS",
                title = "We tell you what\nto do this month",
                body = "Specific moves with plain-English why — so you act, not stare at dashboards.",
                footnote = "We don't lead with budgets, pie charts, or net-worth dashboards. Actions first.",
                cards = listOf(
                    OnboardingCard(
                        title = "Cancel unused Gympass",
                        subtitle = "Last check-in 86 days ago",
                        impact = "+\$47/mo",
                        icon = "CLOCK",
                    ),
                    OnboardingCard(
                        title = "Pay expensive card debt",
                        subtitle = "24.9% APR — kill interest first",
                        impact = "Save \$180",
                        icon = "CARD",
                    ),
                    OnboardingCard(
                        title = "Park idle cash in HYSA",
                        subtitle = "\$4,200 sitting at 0.01%",
                        impact = "+\$18/mo",
                        icon = "CASH",
                    ),
                ),
            )
            2 -> empty.copy(
                kicker = "CONNECT ACCOUNTS",
                title = "See your money\nin one calm place",
                body = "Link banks, cards, loans, and investments so we can surface this month's moves.",
                primaryCta = "Connect securely",
                secondaryCta = "Skip for now",
                badge = "Read-only",
                trustTitle = "We never move money without you",
                trustBody = "Bank-grade encryption. We don't store your login credentials.",
                sectionLabel = "WHAT YOU CAN CONNECT",
                connectTypes = listOf(
                    OnboardingCard("Bank", "Checking · Savings", "", "BANK"),
                    OnboardingCard("Cards", "Credit · Debit", "", "CARD"),
                    OnboardingCard("Loans", "Student · Auto", "", "LOAN"),
                    OnboardingCard("Investments", "Brokerage · 401k", "", "INVEST"),
                ),
            )
            else -> empty.copy(
                badge = "30 days free · Pro",
                title = "Start your free\nPro trial",
                body = "Full access to every move this month. No charge today.",
                primaryCta = "Start free 30-day trial",
                secondaryCta = "Maybe later",
                sectionLabel = "WHAT'S INCLUDED",
                footnote = "After 30 days, Pro continues on a paid plan. Cancel before then — no charge.",
                features = listOf(
                    OnboardingCard(
                        "Full moves list",
                        "Every prioritized action for the month, ranked",
                        "",
                        "",
                    ),
                    OnboardingCard(
                        "Unlimited actions",
                        "Cancel, schedule, transfer guides — no caps",
                        "",
                        "",
                    ),
                    OnboardingCard(
                        "All connected accounts",
                        "Bank · Cards · Loans · Investments",
                        "",
                        "",
                    ),
                    OnboardingCard(
                        "Plain-English why + math",
                        "Know exactly why each move matters",
                        "",
                        "",
                    ),
                ),
                chips = listOf("No charge today", "Cancel anytime", "Then paywall"),
            )
        }
    }
}

internal object PaywallUseCase {
    fun build(trialConsumed: Boolean): PaywallModel = PaywallModel(
        title = if (trialConsumed) "Your 30-day Pro trial has ended" else "AI CFO Pro",
        lede = "The monthly action coach stays on Pro. Specific moves, the math, and a next step — not a free dashboard.",
        monthlyPrice = Pricing.MONTHLY_LABEL,
        monthlyPeriod = "/ month",
        yearlyPrice = Pricing.YEARLY_LABEL,
        yearlyPeriod = "/ year",
        yearlyNote = "Best value",
        finePrint = "No free plan after the trial. Purchases in this build are simulated.",
        monthlyCta = "Continue · ${Pricing.MONTHLY_LABEL}/mo",
        yearlyCta = "Continue · ${Pricing.YEARLY_LABEL}/yr",
    )
}

internal fun statusLabel(status: String): String = when (status) {
    MoveStatusCode.DONE -> "Done"
    MoveStatusCode.SKIPPED -> "Skipped"
    else -> "To do"
}

internal fun toneOrDefault(tone: String): String = tone.ifBlank { Tone.DEFAULT }
