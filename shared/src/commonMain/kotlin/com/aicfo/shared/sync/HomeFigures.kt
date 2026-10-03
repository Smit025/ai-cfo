package com.aicfo.shared.sync

import com.aicfo.shared.market.MarketCalendar
import com.aicfo.shared.market.Money

/**
 * Home dollar figures that come from a bank sync.
 * Savings and net worth are sums of synced balances.
 * Needs, wants, and left-to-save are this month's categorized transactions.
 * A missing figure stays null so the UI can show a placeholder instead of a sample balance.
 */
internal data class DerivedHome(
    val savings: Money?,
    val netWorth: Money?,
    val needs: Money?,
    val wants: Money?,
    val toSave: Money?,
)

internal object HomeFigures {
    const val HIDDEN: String = "—"
    const val WAITING: String = "Balances show after a bank sync"
    const val FROM_BANK: String = "From your linked accounts"

    fun derive(
        accounts: List<SyncedAccount>,
        transactions: List<ProviderTransaction>,
        currency: String,
        monthKey: String,
        timeZoneId: String,
    ): DerivedHome {
        val rows = accounts.filter { it.currency == currency }
        if (rows.isEmpty()) {
            return DerivedHome(null, null, null, null, null)
        }
        val savings = sum(rows, AccountRole.SAVINGS)
        val cash = sum(rows, AccountRole.CASH)
        val invested = sum(rows, AccountRole.INVESTMENT)
        val credit = sum(rows, AccountRole.CREDIT)
        val loans = sum(rows, AccountRole.LOAN)
        val net = savings + cash + invested - credit - loans
        val month = transactions.filter {
            it.currency == currency &&
                it.postedAtEpochMs > 0L &&
                MarketCalendar.monthKey(it.postedAtEpochMs, timeZoneId) == monthKey
        }
        var needs = 0L
        var wants = 0L
        var income = 0L
        for (tx in month) {
            val category = tx.category.uppercase()
            when {
                category == "INCOME" && tx.amountMinor > 0L -> income += tx.amountMinor
                category in NEEDS && tx.amountMinor < 0L -> needs += -tx.amountMinor
                category in WANTS && tx.amountMinor < 0L -> wants += -tx.amountMinor
            }
        }
        return DerivedHome(
            savings = Money(savings, currency),
            netWorth = Money(net, currency),
            needs = Money(needs, currency),
            wants = Money(wants, currency),
            toSave = Money(income - needs - wants, currency),
        )
    }

    private fun sum(rows: List<SyncedAccount>, role: String): Long =
        rows.filter { it.role == role }.sumOf { it.balanceMinor }

    private val NEEDS = setOf(
        "RENT_AND_UTILITIES",
        "LOAN_PAYMENTS",
        "MEDICAL",
        "TRANSPORTATION",
        "BANK_FEES",
        "GOVERNMENT_AND_NON_PROFIT",
    )

    private val WANTS = setOf(
        "ENTERTAINMENT",
        "GENERAL_MERCHANDISE",
        "TRAVEL",
        "PERSONAL_CARE",
        "GENERAL_SERVICES",
        "HOME_IMPROVEMENT",
        "FOOD_AND_DRINK",
        "RECREATION",
    )
}
