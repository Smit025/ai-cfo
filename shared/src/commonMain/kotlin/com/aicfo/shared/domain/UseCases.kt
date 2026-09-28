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
import com.aicfo.shared.presentation.HomeModel
import com.aicfo.shared.presentation.HomeMoveModel
import com.aicfo.shared.presentation.MoveRowModel
import com.aicfo.shared.presentation.MoveStatusCode
import com.aicfo.shared.presentation.MovesModel
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
            pulse = MayaStub.PULSE,
            sectionTitle = MayaStub.SECTION_TITLE,
            seeAllLabel = "See all",
            emptyTitle = "You're clear this month",
            emptyBody = "Every open move is done or skipped. A new list lands at the start of next month.",
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
        val prices = "${Pricing.MONTHLY_LABEL}/mo  ·  ${Pricing.YEARLY_LABEL}/yr"
        val base = OnboardingModel(
            step = step,
            stepCount = 4,
            kicker = "",
            title = "",
            body = "",
            primaryCta = "Continue",
            banksLinked = banksLinked,
            canAdvance = step != 2 || banksLinked,
            canGoBack = step > 0,
            showConnect = step == 2,
            connectCta = if (banksLinked) "Sample linked" else "Connect read-only",
            linkedSummary = if (banksLinked) {
                "Read-only sample linked · Chase, Amex, Capital One, Nelnet, Fidelity"
            } else {
                ""
            },
            priceLeft = "",
            priceRight = "",
            priceNote = "",
            previewTitle = "",
            previewImpact = "",
            previewBody = "",
            bullets = emptyList(),
        )
        return when (step) {
            0 -> base.copy(
                kicker = "AI CFO",
                title = "What should you do with your money this month?",
                body = "A personal action coach for the few moves that matter. Not another budget.",
                previewTitle = MayaStub.move("gympass").homeTitle,
                previewImpact = MayaStub.move("gympass").impactLabel,
                previewBody = MayaStub.move("gympass").homeBody,
            )
            1 -> base.copy(
                kicker = "Monthly moves",
                title = "An action coach, not another dashboard.",
                body = "Each month AI CFO ranks a short list. Every card has a why, the math, and one next step.",
                bullets = listOf(
                    "Kill expensive debt before you invest around it",
                    "Cancel subscriptions you don't use",
                    "Move idle cash out of 0.01% checking",
                ),
            )
            2 -> base.copy(
                kicker = "Read-only",
                title = "We never move your money.",
                body = "Linking is read-only, the way a statement is. Bank passwords are never stored. You take every action yourself.",
                bullets = listOf(
                    "Read-only access to banks, cards, and loans",
                    "No bank passwords on this device",
                    "Link tokens stay in the secure vault",
                    "AI CFO cannot transfer, pay, or cancel for you",
                ),
            )
            else -> base.copy(
                kicker = "Full Pro",
                title = "Try every move free for 30 days.",
                body = "Then $prices. When the trial ends, the coach locks. There is no forever-free plan.",
                primaryCta = "Start 30-day Pro trial",
                priceLeft = Pricing.MONTHLY_LABEL,
                priceRight = Pricing.YEARLY_LABEL,
                priceNote = "per month  ·  or per year",
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
