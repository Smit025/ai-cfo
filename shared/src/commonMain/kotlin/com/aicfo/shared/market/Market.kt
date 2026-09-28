package com.aicfo.shared.market

import com.aicfo.shared.data.MayaStub

object MarketId {
    const val US: String = "US"
    const val CA: String = "CA"
    const val EU: String = "EU"
    const val AE: String = "AE"
}

object CopyKey {
    const val REG_NEVER_MOVE: String = "reg.never_move"
    const val REG_CREDENTIALS: String = "reg.credentials"
    const val REG_READ_ONLY: String = "reg.read_only"
    const val REG_LINK_CA: String = "reg.link.ca"
    const val REG_LINK_EU: String = "reg.link.eu"
    const val REG_LINK_AE: String = "reg.link.ae"
    const val GROUP_CASH: String = "accounts.group.cash"
    const val GROUP_CARDS: String = "accounts.group.cards"
    const val GROUP_INVESTMENTS: String = "accounts.group.investments"
    const val MONEY_PER_MONTH: String = "money.per_month"
    const val MONEY_SAVE: String = "money.save"
    const val HOME_DELTA_UP: String = "home.delta_up"
    const val HOME_NET_DELTA: String = "home.net_delta"
    const val HOME_RUNWAY: String = "home.runway"
    const val HOPE_OPEN: String = "hope.gympass.open"
    const val HOPE_DONE: String = "hope.gympass.done"
    const val MOVES_SUBTITLE: String = "moves.subtitle"

    fun monthShort(month: Int): String = "month.short.$month"
}

interface CopyCatalog {
    fun text(key: String): String
}

/** Platform string table. Return null to fall through to the market catalog. */
interface LocalStrings {
    fun text(key: String): String?
}

object EmptyLocalStrings : LocalStrings {
    override fun text(key: String): String? = null
}

class CopyResolver(
    private val catalog: CopyCatalog,
    private val local: LocalStrings,
) {
    fun text(key: String, vars: Map<String, String> = emptyMap()): String {
        var value = local.text(key) ?: catalog.text(key)
        vars.forEach { (name, replacement) ->
            value = value.replace("{$name}", replacement)
        }
        return value
    }
}

class MapCopy(private val values: Map<String, String>) : CopyCatalog {
    override fun text(key: String): String = values[key] ?: error("Missing copy key $key")
}

data class FeatureFlags(
    val moves: Boolean,
    val bankLink: Boolean,
    val investments: Boolean,
    val consumerDebt: Boolean,
    val hysa: Boolean,
    val taxSetAside: Boolean,
)

/**
 * Read-only account linking. US/CA/EU use Plaid-class aggregation.
 * UAE uses local open-banking rails (not implemented).
 */
interface LinkProvider {
    val id: String
    val readOnly: Boolean
    fun supports(marketId: String): Boolean
}

object PlaidLinkProvider : LinkProvider {
    override val id: String = "plaid"
    override val readOnly: Boolean = true
    override fun supports(marketId: String): Boolean =
        marketId == MarketId.US || marketId == MarketId.CA || marketId == MarketId.EU
}

/**
 * Placeholder for UAE local rails (Lean-class / CBUAE open finance).
 * No network calls. Not a Plaid market.
 */
object UaeLocalRailsProvider : LinkProvider {
    override val id: String = "uae-local-rails"
    override val readOnly: Boolean = true
    override fun supports(marketId: String): Boolean = marketId == MarketId.AE
}

/** How a market names a borrowing rate. US is APR. UAE consumer finance is not APR. */
interface RateMarket {
    val rateName: String
    fun format(basisPoints: Int): String
}

class NamedRateMarket(override val rateName: String) : RateMarket {
    override fun format(basisPoints: Int): String {
        val negative = basisPoints < 0
        val abs = kotlin.math.abs(basisPoints)
        val whole = abs / 100
        val frac = abs % 100
        val number = when {
            frac == 0 -> whole.toString()
            frac % 10 == 0 -> "$whole.${frac / 10}"
            else -> "$whole." + frac.toString().padStart(2, '0')
        }
        return (if (negative) "-" else "") + "$number% $rateName"
    }
}

/** Tax is a plugin. US estimated-tax set-aside stays in TaxVault, so this niche is off. */
interface TaxNiche {
    val id: String
    val enabled: Boolean
    val disclosureKey: String
}

data class MarketConfig(
    val id: String,
    val displayName: String,
    val currency: String,
    val localeTag: String,
    val timeZoneId: String,
    val shipped: Boolean,
    val planYear: Int,
    val planMonth: Int,
    val disclosureKey: String,
)

interface MarketPack {
    val config: MarketConfig
    val features: FeatureFlags
    val links: LinkProvider
    val rates: RateMarket
    val tax: TaxNiche
    val copy: CopyCatalog
    val monthlyPrice: Money
    val yearlyPrice: Money
}

data class MarketSnapshot(
    val id: String,
    val currency: String,
    val timeZoneId: String,
    val localeTag: String,
    val linkProviderId: String,
    val shipped: Boolean,
    val planMonthKey: String,
    val movesEnabled: Boolean,
    val hysaEnabled: Boolean,
    val taxSetAsideEnabled: Boolean,
)

fun MarketPack.snapshot(): MarketSnapshot = MarketSnapshot(
    id = config.id,
    currency = config.currency,
    timeZoneId = config.timeZoneId,
    localeTag = config.localeTag,
    linkProviderId = links.id,
    shipped = config.shipped,
    planMonthKey = "%04d-%02d".format(config.planYear, config.planMonth),
    movesEnabled = features.moves,
    hysaEnabled = features.hysa,
    taxSetAsideEnabled = features.taxSetAside,
)

private val englishCopy: Map<String, String> = mapOf(
    CopyKey.REG_NEVER_MOVE to "We never move money without you",
    CopyKey.REG_CREDENTIALS to "Bank-grade encryption. We don't store your login credentials.",
    CopyKey.REG_READ_ONLY to "Read-only",
    CopyKey.REG_LINK_CA to "Read-only connection for Canadian institutions (Plaid). This market is not shipped.",
    CopyKey.REG_LINK_EU to "Read-only connection for European institutions (Plaid). This market is not shipped.",
    CopyKey.REG_LINK_AE to "Read-only connection via UAE open-banking rails. This market is not shipped.",
    CopyKey.GROUP_CASH to "CASH",
    CopyKey.GROUP_CARDS to "CARDS & LOANS",
    CopyKey.GROUP_INVESTMENTS to "INVESTMENTS",
    CopyKey.MONEY_PER_MONTH to "/mo",
    CopyKey.MONEY_SAVE to "Save {amount}",
    CopyKey.HOME_DELTA_UP to "↑ {amount} this month",
    CopyKey.HOME_NET_DELTA to "↑ 2.1% MoM",
    CopyKey.HOME_RUNWAY to "47 days runway · quietly building",
    CopyKey.HOPE_OPEN to "If unused Gympass stayed cancelled this year, you'd keep {amount} more",
    CopyKey.HOPE_DONE to "Gympass stays cancelled — about {amount} stays with you this year.",
    CopyKey.MOVES_SUBTITLE to "Ranked actions for {month} · {name}, {city}",
    CopyKey.monthShort(1) to "Jan",
    CopyKey.monthShort(2) to "Feb",
    CopyKey.monthShort(3) to "Mar",
    CopyKey.monthShort(4) to "Apr",
    CopyKey.monthShort(5) to "May",
    CopyKey.monthShort(6) to "Jun",
    CopyKey.monthShort(7) to "Jul",
    CopyKey.monthShort(8) to "Aug",
    CopyKey.monthShort(9) to "Sep",
    CopyKey.monthShort(10) to "Oct",
    CopyKey.monthShort(11) to "Nov",
    CopyKey.monthShort(12) to "Dec",
)

private fun packCopy(overrides: Map<String, String> = emptyMap()): CopyCatalog =
    MapCopy(englishCopy + overrides)

private val coachingFlags = FeatureFlags(
    moves = true,
    bankLink = true,
    investments = true,
    consumerDebt = true,
    hysa = true,
    taxSetAside = false,
)

private object UsTax : TaxNiche {
    override val id: String = "us.estimated-tax"
    override val enabled: Boolean = false
    override val disclosureKey: String = ""
}

private object OffTax : TaxNiche {
    override val id: String = "none"
    override val enabled: Boolean = false
    override val disclosureKey: String = ""
}

object UsMarketPack : MarketPack {
    override val config: MarketConfig = MarketConfig(
        id = MarketId.US,
        displayName = "United States",
        currency = CurrencyCode.USD,
        localeTag = "en-US",
        timeZoneId = "America/Chicago",
        shipped = true,
        planYear = MayaStub.PLAN_YEAR,
        planMonth = MayaStub.PLAN_MONTH,
        disclosureKey = CopyKey.REG_CREDENTIALS,
    )
    override val features: FeatureFlags = coachingFlags
    override val links: LinkProvider = PlaidLinkProvider
    override val rates: RateMarket = NamedRateMarket("APR")
    override val tax: TaxNiche = UsTax
    override val copy: CopyCatalog = packCopy()
    override val monthlyPrice: Money = Money(999, CurrencyCode.USD)
    override val yearlyPrice: Money = Money(7_900, CurrencyCode.USD)
}

/** Not shipped. Plaid Canada, CAD, America/Toronto. No coach plan yet. */
object CaMarketPack : MarketPack {
    override val config: MarketConfig = MarketConfig(
        id = MarketId.CA,
        displayName = "Canada",
        currency = CurrencyCode.CAD,
        localeTag = "en-CA",
        timeZoneId = "America/Toronto",
        shipped = false,
        planYear = 2026,
        planMonth = 10,
        disclosureKey = CopyKey.REG_LINK_CA,
    )
    override val features: FeatureFlags = coachingFlags
    override val links: LinkProvider = PlaidLinkProvider
    override val rates: RateMarket = NamedRateMarket("APR")
    override val tax: TaxNiche = OffTax
    override val copy: CopyCatalog = packCopy()
    override val monthlyPrice: Money = Money(1_299, CurrencyCode.CAD)
    override val yearlyPrice: Money = Money(9_900, CurrencyCode.CAD)
}

/**
 * Not shipped. One EU pack is a stand-in (EUR, Europe/Berlin).
 * A real launch splits member states, languages, and consumer-credit rules.
 */
object EuMarketPack : MarketPack {
    override val config: MarketConfig = MarketConfig(
        id = MarketId.EU,
        displayName = "Europe",
        currency = CurrencyCode.EUR,
        localeTag = "en-GB",
        timeZoneId = "Europe/Berlin",
        shipped = false,
        planYear = 2026,
        planMonth = 10,
        disclosureKey = CopyKey.REG_LINK_EU,
    )
    override val features: FeatureFlags = coachingFlags
    override val links: LinkProvider = PlaidLinkProvider
    override val rates: RateMarket = NamedRateMarket("APR")
    override val tax: TaxNiche = OffTax
    override val copy: CopyCatalog = packCopy()
    override val monthlyPrice: Money = Money(999, CurrencyCode.EUR)
    override val yearlyPrice: Money = Money(7_900, CurrencyCode.EUR)
}

/**
 * Not shipped. Local rails, Asia/Dubai (no DST), AED.
 * Borrowing is a profit rate, not US APR. HYSA-style cash moves stay off
 * until an Islamic savings product is specified. No personal income-tax niche.
 */
object AeMarketPack : MarketPack {
    override val config: MarketConfig = MarketConfig(
        id = MarketId.AE,
        displayName = "United Arab Emirates",
        currency = CurrencyCode.AED,
        localeTag = "en-AE",
        timeZoneId = "Asia/Dubai",
        shipped = false,
        planYear = 2026,
        planMonth = 10,
        disclosureKey = CopyKey.REG_LINK_AE,
    )
    override val features: FeatureFlags = coachingFlags.copy(hysa = false, taxSetAside = false)
    override val links: LinkProvider = UaeLocalRailsProvider
    override val rates: RateMarket = NamedRateMarket("profit rate")
    override val tax: TaxNiche = OffTax
    override val copy: CopyCatalog = packCopy()
    override val monthlyPrice: Money = Money(3_999, CurrencyCode.AED)
    override val yearlyPrice: Money = Money(29_900, CurrencyCode.AED)
}

object Markets {
    fun unitedStates(): MarketPack = UsMarketPack
    fun canada(): MarketPack = CaMarketPack
    fun europe(): MarketPack = EuMarketPack
    fun uae(): MarketPack = AeMarketPack

    fun byId(id: String): MarketPack = when (id) {
        MarketId.US -> unitedStates()
        MarketId.CA -> canada()
        MarketId.EU -> europe()
        MarketId.AE -> uae()
        else -> throw IllegalArgumentException("Unknown market $id")
    }
}
