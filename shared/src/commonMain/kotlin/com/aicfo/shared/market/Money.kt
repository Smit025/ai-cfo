package com.aicfo.shared.market

/**
 * Minor units (cents, fils) plus an ISO currency code.
 * Math never assumes a dollar sign or a 2-decimal US string.
 */
data class Money(
    val minor: Long,
    val currency: String,
) {
    init {
        require(currency.length == 3) { "Currency code must be ISO-4217: $currency" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot add $currency to ${other.currency}"
        }
        return Money(minor + other.minor, currency)
    }

    operator fun times(count: Int): Money = Money(minor * count.toLong(), currency)
}

object CurrencyCode {
    const val USD: String = "USD"
    const val CAD: String = "CAD"
    const val EUR: String = "EUR"
    const val AED: String = "AED"
}

/**
 * Deterministic scaffold formatter. Production can replace this with
 * platform NumberFormat / NSNumberFormatter; the Money value stays the source.
 * USD matches the locked US boards ($8,420, $9.99, $42.1k).
 */
object MoneyFormat {
    fun standard(money: Money): String {
        val negative = money.minor < 0
        val abs = kotlin.math.abs(money.minor)
        val dollars = abs / 100
        val cents = (abs % 100).toInt()
        val number = if (cents == 0) {
            group(dollars)
        } else {
            group(dollars) + "." + cents.toString().padStart(2, '0')
        }
        return (if (negative) "-" else "") + symbol(money.currency) + number
    }

    /** $42.1k style, used for the Home net-worth figure. */
    fun compact(money: Money): String {
        val negative = money.minor < 0
        val dollars = kotlin.math.abs(money.minor) / 100
        val body = if (dollars >= 10_000) {
            val thousands = dollars / 1000
            val tenth = (dollars % 1000) / 100
            if (tenth == 0L) "${thousands}k" else "${thousands}.${tenth}k"
        } else {
            group(dollars)
        }
        return (if (negative) "-" else "") + symbol(money.currency) + body
    }

    fun signedMonthly(money: Money, perMonthSuffix: String): String {
        val sign = if (money.minor > 0) "+" else ""
        return sign + standard(money) + perMonthSuffix
    }

    fun approx(money: Money): String = "~" + standard(money)

    private fun symbol(currency: String): String = when (currency) {
        CurrencyCode.USD -> "$"
        CurrencyCode.CAD -> "CA$"
        CurrencyCode.EUR -> "€"
        CurrencyCode.AED -> "AED "
        else -> "$currency "
    }

    private fun group(value: Long): String {
        val raw = value.toString()
        val grouped = StringBuilder()
        raw.forEachIndexed { index, char ->
            if (index > 0 && (raw.length - index) % 3 == 0) grouped.append(',')
            grouped.append(char)
        }
        return grouped.toString()
    }
}

object MoneyMath {
    fun yearlyFromMonthly(monthly: Money): Money = monthly * 12

    /** US Gympass check: 4700 minor units × 12 = 56400. Prefer [yearlyFromMonthly]. */
    fun yearlyFromMonthlyCents(monthlyCents: Long): Long =
        yearlyFromMonthly(Money(monthlyCents, CurrencyCode.USD)).minor
}
