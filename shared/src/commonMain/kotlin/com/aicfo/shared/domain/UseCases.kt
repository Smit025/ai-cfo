package com.aicfo.shared.domain

import com.aicfo.shared.data.MayaStub
import com.aicfo.shared.market.CopyKey
import com.aicfo.shared.market.CopyResolver
import com.aicfo.shared.market.FeatureFlags
import com.aicfo.shared.market.MarketPack
import com.aicfo.shared.market.Money
import com.aicfo.shared.market.MoneyFormat
import com.aicfo.shared.market.MoneyMath
import com.aicfo.shared.model.AccountGroup
import com.aicfo.shared.model.CoachMove
import com.aicfo.shared.model.MoveKind
import com.aicfo.shared.model.copyKey
import com.aicfo.shared.model.wire
import com.aicfo.shared.sync.DerivedHome
import com.aicfo.shared.sync.HomeFigures
import com.aicfo.shared.sync.SyncLine
import com.aicfo.shared.sync.SyncedAccount
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

internal fun moveVisible(kind: MoveKind, features: FeatureFlags): Boolean = when (kind) {
    MoveKind.MOVE_IDLE_CASH -> features.moves && features.hysa
    MoveKind.EXTRA_DEBT_PAYMENT -> features.moves && features.consumerDebt
    MoveKind.CANCEL_SUBSCRIPTION,
    MoveKind.PAY_RENT,
    MoveKind.REFINANCE_CHECK,
    -> features.moves
}

internal object HomeUseCase {
    fun build(
        resolved: List<ResolvedMove>,
        notificationsOn: Boolean,
        market: MarketPack,
        copy: CopyResolver,
        sync: SyncLine,
        bank: DerivedHome?,
    ): HomeModel {
        val todo = resolved
            .filter { it.status == MoveStatusCode.TODO && moveVisible(it.move.kind, market.features) }
            .sortedBy { it.move.rank }
            .take(3)
        val unlocked = bank != null
        return HomeModel(
            greeting = "Hello, ${MayaStub.profile.firstName}",
            subtitle = MayaStub.HOME_SUBTITLE,
            initials = MayaStub.profile.initials,
            showNotificationDot = notificationsOn,
            savingsLabel = MayaStub.SAVINGS_LABEL,
            savingsAmount = moneyOrHidden(bank?.savings),
            savingsDelta = "",
            savingsUp = true,
            netWorthLabel = MayaStub.NET_WORTH_LABEL,
            netWorthAmount = moneyOrHidden(bank?.netWorth, compact = true),
            netWorthDelta = "",
            netWorthUp = true,
            runway = if (unlocked) HomeFigures.FROM_BANK else HomeFigures.WAITING,
            hope = hopeLine(resolved, copy),
            sectionTitle = MayaStub.SECTION_TITLE,
            seeAllLabel = "See all",
            emptyTitle = "You're clear this month",
            emptyBody = "Every open move is done or skipped. A new list lands at the start of next month.",
            snapshot = listOf(
                HomeAmountModel(MayaStub.NEEDS_LABEL, moneyOrHidden(bank?.needs), MayaStub.NEEDS_CAPTION, Tone.DEFAULT),
                HomeAmountModel(MayaStub.WANTS_LABEL, moneyOrHidden(bank?.wants), MayaStub.WANTS_CAPTION, Tone.DEFAULT),
                HomeAmountModel(
                    MayaStub.SAVE_LABEL,
                    moneyOrHidden(bank?.toSave),
                    MayaStub.SAVE_CAPTION,
                    if ((bank?.toSave?.minor ?: 0L) > 0L) Tone.POSITIVE else Tone.DEFAULT,
                ),
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
            freshnessLabel = sync.freshnessLabel,
            syncCode = sync.syncCode,
            syncActionLabel = sync.syncActionLabel,
            syncStale = sync.syncStale,
        )
    }

    /**
     * Hope is the open Gympass year, or a calm confirmation once it is cancelled.
     * Keeping the subscription hides the line so Home does not scold.
     */
    private fun hopeLine(resolved: List<ResolvedMove>, copy: CopyResolver): String {
        val gympass = resolved.firstOrNull { it.move.id == "gympass" } ?: return ""
        val yearly = MoneyMath.yearlyFromMonthly(MayaStub.GYMPASS_MONTHLY)
        return when (gympass.status) {
            MoveStatusCode.TODO -> copy.text(CopyKey.HOPE_OPEN, mapOf("amount" to MoneyFormat.approx(yearly)))
            MoveStatusCode.DONE -> copy.text(CopyKey.HOPE_DONE, mapOf("amount" to MoneyFormat.standard(yearly)))
            else -> ""
        }
    }

    private fun moneyOrHidden(money: Money?, compact: Boolean = false): String {
        if (money == null) return HomeFigures.HIDDEN
        return if (compact) MoneyFormat.compact(money) else MoneyFormat.standard(money)
    }
}

internal object MovesUseCase {
    fun build(resolved: List<ResolvedMove>, market: MarketPack, copy: CopyResolver): MovesModel {
        val profile = MayaStub.profile
        val month = "${copy.text(CopyKey.monthShort(market.config.planMonth))} ${market.config.planYear}"
        val rows = resolved
            .filter { moveVisible(it.move.kind, market.features) }
            .sortedBy { it.move.rank }
            .map { row ->
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
            subtitle = copy.text(
                CopyKey.MOVES_SUBTITLE,
                mapOf("month" to month, "name" to profile.firstName, "city" to profile.city),
            ),
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
    fun build(
        linked: Boolean,
        copy: CopyResolver,
        linkError: String,
        accounts: List<SyncedAccount>,
        sync: SyncLine,
        linkConfigured: Boolean,
        unavailableLabel: String,
        linkNote: String,
        sampleLink: Boolean,
        sampleCta: String,
    ): AccountsModel {
        val profile = MayaStub.profile
        val readOnly = copy.text(CopyKey.REG_READ_ONLY)
        val grouped = accounts.groupBy { it.group }
        val ordered = grouped.keys.sortedBy { groupOrder(it) }
        val groups = if (!linked) {
            emptyList()
        } else {
            ordered.map { group ->
                AccountGroupModel(
                    title = groupTitle(group, copy),
                    accounts = grouped.getValue(group).map { account ->
                        AccountRowModel(
                            id = account.id,
                            name = account.name,
                            detail = account.maskLine,
                            balance = balanceLabel(account),
                            initials = account.initials,
                            colorHex = account.colorHex,
                            readOnlyLabel = readOnly,
                        )
                    },
                )
            }
        }
        return AccountsModel(
            title = "Accounts",
            subtitle = when {
                !linked -> "Nothing linked · ${profile.fullName}"
                sampleLink -> "Maya sample · not your bank"
                else -> "Connected read-only · ${profile.fullName}"
            },
            trust = "Read-only access. We never move money or store credentials. You take every action.",
            linked = linked,
            emptyTitle = "No institutions linked",
            emptyBody = "Connections are read-only. Bank passwords are never stored on this device.",
            emptyCta = if (linkConfigured) "Connect securely" else unavailableLabel,
            linkError = linkError,
            groups = groups,
            freshnessLabel = sync.freshnessLabel,
            syncCode = sync.syncCode,
            syncActionLabel = sync.syncActionLabel,
            syncStale = sync.syncStale,
            linkNote = linkNote,
            sampleCta = sampleCta,
        )
    }

    private fun groupOrder(group: String): Int {
        val known = runCatching { AccountGroup.valueOf(group) }.getOrNull()
        return known?.ordinal ?: (AccountGroup.entries.size + 1)
    }

    private fun groupTitle(group: String, copy: CopyResolver): String {
        val known = runCatching { AccountGroup.valueOf(group) }.getOrNull()
        return if (known != null) copy.text(known.copyKey()) else group
    }

    private fun balanceLabel(account: SyncedAccount): String {
        if (account.currency.length != 3) return account.balanceMinor.toString()
        return MoneyFormat.standard(Money(account.balanceMinor, account.currency))
    }
}

internal object OnboardingUseCase {
    fun build(
        step: Int,
        banksLinked: Boolean,
        market: MarketPack,
        copy: CopyResolver,
        linkError: String,
        linkConfigured: Boolean,
        unavailableLabel: String,
        linkNote: String,
    ): OnboardingModel {
        val empty = OnboardingModel(
            step = step,
            stepCount = 4,
            kicker = "",
            descriptor = "",
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
            linkError = linkError,
        )
        return when (step) {
            0 -> empty.copy(
                kicker = "FINWISE",
                descriptor = "Your AI CFO",
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
                        impact = MoneyFormat.signedMonthly(MayaStub.GYMPASS_MONTHLY, copy.text(CopyKey.MONEY_PER_MONTH)),
                        icon = "CLOCK",
                    ),
                    OnboardingCard(
                        title = "Pay expensive card debt",
                        subtitle = "${market.rates.format(MayaStub.CAPITAL_ONE_APR_BPS)} — kill interest first",
                        impact = copy.text(CopyKey.MONEY_SAVE, mapOf("amount" to MoneyFormat.standard(MayaStub.CARD_SAVE))),
                        icon = "CARD",
                    ),
                    OnboardingCard(
                        title = "Park idle cash in HYSA",
                        subtitle = "${MoneyFormat.standard(MayaStub.IDLE_CASH)} sitting at 0.01%",
                        impact = MoneyFormat.signedMonthly(MayaStub.IDLE_MONTHLY, copy.text(CopyKey.MONEY_PER_MONTH)),
                        icon = "CASH",
                    ),
                ),
            )
            2 -> empty.copy(
                kicker = "CONNECT ACCOUNTS",
                title = "See your money\nin one calm place",
                body = "Link banks, cards, loans, and investments so we can surface this month's moves.",
                primaryCta = if (linkConfigured) "Connect securely" else unavailableLabel,
                secondaryCta = "Skip for now",
                linkNote = linkNote,
                badge = copy.text(CopyKey.REG_READ_ONLY),
                trustTitle = copy.text(CopyKey.REG_NEVER_MOVE),
                trustBody = copy.text(market.config.disclosureKey),
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
    fun build(trialConsumed: Boolean, monthlyLabel: String, yearlyLabel: String): PaywallModel = PaywallModel(
        title = if (trialConsumed) "Your 30-day Pro trial has ended" else "Finwise Pro",
        lede = "The monthly action coach stays on Pro. Specific moves, the math, and a next step — not a free dashboard.",
        monthlyPrice = monthlyLabel,
        monthlyPeriod = "/ month",
        yearlyPrice = yearlyLabel,
        yearlyPeriod = "/ year",
        yearlyNote = "Best value",
        finePrint = "No free plan after the trial. Purchases in this build are simulated.",
        monthlyCta = "Continue · $monthlyLabel/mo",
        yearlyCta = "Continue · $yearlyLabel/yr",
    )
}

internal fun statusLabel(status: String): String = when (status) {
    MoveStatusCode.DONE -> "Done"
    MoveStatusCode.SKIPPED -> "Skipped"
    else -> "To do"
}

internal fun toneOrDefault(tone: String): String = tone.ifBlank { Tone.DEFAULT }
