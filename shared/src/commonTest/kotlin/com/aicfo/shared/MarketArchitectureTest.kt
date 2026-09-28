package com.aicfo.shared

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.market.CopyKey
import com.aicfo.shared.market.CurrencyCode
import com.aicfo.shared.market.LocalStrings
import com.aicfo.shared.market.MarketCalendar
import com.aicfo.shared.market.MarketId
import com.aicfo.shared.market.Markets
import com.aicfo.shared.market.Money
import com.aicfo.shared.market.MoneyFormat
import com.aicfo.shared.market.MoneyMath
import com.aicfo.shared.model.MoveKind
import com.aicfo.shared.domain.moveVisible
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemoryTokenVault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketArchitectureTest {
    @Test
    fun moneyMathStaysInCurrencyAndRejectsMixes() {
        val yearly = MoneyMath.yearlyFromMonthly(Money(4_700, CurrencyCode.CAD))
        assertEquals(56_400L, yearly.minor)
        assertEquals(CurrencyCode.CAD, yearly.currency)
        assertFailsWith<IllegalArgumentException> {
            Money(1, CurrencyCode.USD) + Money(1, CurrencyCode.EUR)
        }
    }

    @Test
    fun usdFormatMatchesTheLockedBoards() {
        assertEquals("\$8,420", MoneyFormat.standard(Money(842_000, CurrencyCode.USD)))
        assertEquals("\$9.99", MoneyFormat.standard(Money(999, CurrencyCode.USD)))
        assertEquals("\$79", MoneyFormat.standard(Money(7_900, CurrencyCode.USD)))
        assertEquals("\$42.1k", MoneyFormat.compact(Money(4_210_000, CurrencyCode.USD)))
        assertEquals("+\$47/mo", MoneyFormat.signedMonthly(Money(4_700, CurrencyCode.USD), "/mo"))
        assertEquals("~\$564", MoneyFormat.approx(MoneyMath.yearlyFromMonthly(Money(4_700, CurrencyCode.USD))))
        assertEquals("CA\$8,420", MoneyFormat.standard(Money(842_000, CurrencyCode.CAD)))
        assertEquals("€9.99", MoneyFormat.standard(Money(999, CurrencyCode.EUR)))
        assertEquals("AED 39.99", MoneyFormat.standard(Money(3_999, CurrencyCode.AED)))
    }

    @Test
    fun thisMonthFollowsTheMarketZone() {
        val dubaiDawnOfOctober = 1_790_800_200_000L
        assertEquals("2026-09", MarketCalendar.monthKey(dubaiDawnOfOctober, "America/Chicago"))
        assertEquals("2026-10", MarketCalendar.monthKey(dubaiDawnOfOctober, "Asia/Dubai"))
        assertFalse(MarketCalendar.isMonth(dubaiDawnOfOctober, "America/Chicago", 2026, 10))
        assertTrue(MarketCalendar.isMonth(dubaiDawnOfOctober, "Asia/Dubai", 2026, 10))
    }

    @Test
    fun onlyTheUsPackIsShipped() {
        val us = Markets.unitedStates()
        assertTrue(us.config.shipped)
        assertEquals(MarketId.US, us.config.id)
        assertEquals("plaid", us.links.id)
        assertTrue(us.links.supports(MarketId.US))
        assertEquals("24.9% APR", us.rates.format(2_490))
        assertFalse(us.tax.enabled)
        assertEquals(2026, us.config.planYear)
        assertEquals(10, us.config.planMonth)
        assertEquals("America/Chicago", us.config.timeZoneId)

        val canada = Markets.canada()
        val europe = Markets.europe()
        val uae = Markets.uae()
        assertFalse(canada.config.shipped)
        assertFalse(europe.config.shipped)
        assertFalse(uae.config.shipped)
        assertEquals(CurrencyCode.CAD, canada.config.currency)
        assertEquals(CurrencyCode.EUR, europe.config.currency)
        assertEquals(CurrencyCode.AED, uae.config.currency)
        assertEquals("uae-local-rails", uae.links.id)
        assertTrue(uae.links.readOnly)
        assertFalse(uae.links.supports(MarketId.US))
        assertEquals("24.9% profit rate", uae.rates.format(2_490))
        assertFalse(uae.features.hysa)
        assertFalse(moveVisible(MoveKind.MOVE_IDLE_CASH, uae.features))
        assertTrue(moveVisible(MoveKind.MOVE_IDLE_CASH, us.features))
        assertEquals(uae, Markets.byId(MarketId.AE))
    }

    @Test
    fun localStringsOverrideTheCatalogAndUaeHidesHysa() {
        val local = object : LocalStrings {
            override fun text(key: String): String? =
                if (key == CopyKey.REG_NEVER_MOVE) "Never without you" else null
        }
        val us = AiCfoController(
            MemoryTokenVault(),
            MemoryLocalStore(),
            FixedClock(10L),
            Markets.unitedStates(),
            local,
        )
        us.advanceOnboarding()
        us.advanceOnboarding()
        val connect = us.onboarding()
        assertEquals("Never without you", connect.trustTitle)
        assertEquals("Read-only", connect.badge)
        assertEquals(
            "Bank-grade encryption. We don't store your login credentials.",
            connect.trustBody,
        )

        val value = AiCfoController(MemoryTokenVault(), MemoryLocalStore(), FixedClock(10L))
        value.advanceOnboarding()
        val cards = value.onboarding()
        assertEquals("+\$47/mo", cards.cardAt(0).impact)
        assertEquals("24.9% APR — kill interest first", cards.cardAt(1).subtitle)
        assertEquals("\$4,200 sitting at 0.01%", cards.cardAt(2).subtitle)

        val uae = AiCfoController(
            MemoryTokenVault(),
            MemoryLocalStore(),
            FixedClock(10L),
            Markets.uae(),
            local,
        )
        val ids = (0 until uae.moves().moveCount()).map { uae.moves().moveAt(it).id }
        assertFalse(ids.contains("idle-cash"))
        assertTrue(ids.contains("gympass"))
        assertEquals("AED 39.99", uae.paywall().monthlyPrice)
        assertEquals("Ranked actions for Oct 2026 · Maya, Austin", uae.moves().subtitle)
    }
}

private class FixedClock(var now: Long) : AppClock {
    override fun nowEpochMs(): Long = now
}
