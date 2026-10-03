package com.aicfo.app.plaid

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.domain.Pricing
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.security.LinkPolicy
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemorySecureStore
import com.aicfo.shared.security.MemoryTokenVault
import com.aicfo.shared.sync.BankLinkId
import com.aicfo.shared.sync.SyncStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaidReadOnlyTest {
    @Test
    fun linkTokenRequestsTransactionsOnlyAndSandboxHostRejectsProduction() {
        val created = PlaidRequests.linkToken(
            clientId = "client-test",
            secret = "secret-test",
            clientUserId = "fw-user",
            packageName = "com.aicfo.app",
            accessTokenForUpdate = null,
        )
        assertEquals("transactions", created.getJSONArray("products").getString(0))
        assertEquals(1, created.getJSONArray("products").length())
        assertEquals("com.aicfo.app", created.getString("android_package_name"))
        assertFalse(created.has("access_token"))
        val wire = created.toString()
        assertFalse(wire.contains("transfer"))
        assertFalse(wire.contains("payment_initiation"))
        assertFalse(wire.contains("\"auth\""))
        val api = PlaidHttpApi("https://production.plaid.com", "client-test", "secret-test", PlaidTransport { _, _ ->
            error("production must not be called")
        })
        assertFalse(api.configured)
        assertTrue(PlaidHttpApi(PLAID_SANDBOX_HOST, "client-test", "secret-test", PlaidTransport { _, _ ->
            PlaidHttpResult(500, "{}")
        }).configured)
        assertFalse(PlaidHttpApi(PLAID_SANDBOX_HOST, "", "secret-test", PlaidTransport { _, _ ->
            error("missing client id")
        }).configured)
    }

    @Test
    fun mapsAccountsTransactionsAndRedactsLoginErrors() {
        val accounts = PlaidResponses.accounts(
            JSONObject(
                """
                {"accounts":[
                  {"account_id":"acc_sav","name":"Plaid Saving","mask":"1111","type":"depository","subtype":"savings",
                   "balances":{"current":100,"iso_currency_code":"USD"}},
                  {"account_id":"acc_chk","name":"Plaid Checking","mask":"0000","type":"depository","subtype":"checking",
                   "balances":{"current":50.5,"iso_currency_code":"USD"}},
                  {"account_id":"acc_card","name":"Plaid Credit Card","mask":"3333","type":"credit","subtype":"credit card",
                   "balances":{"current":20,"iso_currency_code":"USD"}}
                ]}
                """.trimIndent(),
            ),
        )
        assertEquals("savings", accounts[0].role)
        assertEquals(10_000L, accounts[0].balanceMinor)
        assertEquals("cash", accounts[1].role)
        assertEquals(5_050L, accounts[1].balanceMinor)
        assertEquals("credit", accounts[2].role)
        assertEquals("CARDS_AND_LOANS", accounts[2].group)

        val txs = PlaidResponses.transactions(
            JSONObject(
                """
                {"added":[{"transaction_id":"txn_abc","account_id":"acc_chk","amount":6.33,
                  "iso_currency_code":"USD","date":"2026-10-02","name":"Starbucks",
                  "personal_finance_category":{"primary":"FOOD_AND_DRINK"}}]}
                """.trimIndent(),
            ).getJSONArray("added"),
        )
        assertEquals("txn_abc", txs[0].providerTransactionId)
        assertEquals(-633L, txs[0].amountMinor)
        assertEquals("FOOD_AND_DRINK", txs[0].category)
        assertTrue(txs[0].postedAtEpochMs > 0L)

        val login = PlaidResponses.failure(
            400,
            """{"error_code":"ITEM_LOGIN_REQUIRED","error_type":"ITEM_ERROR","display_message":null}""",
        )
        assertTrue(login is PlaidOutcome.LoginRequired)
        val leaked = PlaidResponses.failure(
            400,
            """{"error_code":"INTERNAL_SERVER_ERROR","error_type":"API_ERROR","display_message":"see $ACCESS"}""",
        )
        assertTrue(leaked is PlaidOutcome.Failed)
        assertFalse((leaked as PlaidOutcome.Failed).message.contains("access-sandbox"))

        val jsonNull = JSONObject("""{"display_message":null,"error_message":null}""")
        assertEquals("null", jsonNull.optString("display_message"))
        val hidden = PlaidResponses.failure(
            400,
            """{"error_code":"INVALID_FIELD","error_type":"INVALID_REQUEST","display_message":null,"error_message":"link token was refused"}""",
        )
        assertTrue(hidden is PlaidOutcome.Failed)
        assertEquals("link token was refused", (hidden as PlaidOutcome.Failed).message)
    }

    @Test
    fun exchangeStoresTheAccessTokenOnlyInTheVault() {
        val store = MemoryLocalStore()
        val vault = MemoryTokenVault()
        val calls = mutableListOf<String>()
        val transport = PlaidTransport { url, body ->
            calls += url.substringAfter(".com")
            val json = JSONObject(body)
            assertFalse(json.has("transfer"))
            when {
                url.endsWith("/item/public_token/exchange") ->
                    PlaidHttpResult(200, """{"access_token":"$ACCESS","item_id":"item-1"}""")
                url.endsWith("/accounts/balance/get") -> PlaidHttpResult(
                    200,
                    """{"accounts":[{"account_id":"acc_sav","name":"Plaid Saving","mask":"1111","type":"depository","subtype":"savings","balances":{"current":100,"iso_currency_code":"USD"}}]}""",
                )
                url.endsWith("/transactions/sync") -> PlaidHttpResult(
                    200,
                    """{"added":[{"transaction_id":"txn_abc","account_id":"acc_sav","amount":12.00,"iso_currency_code":"USD","date":"2026-10-02","name":"Rent","personal_finance_category":{"primary":"RENT_AND_UTILITIES"}}],"modified":[],"removed":[{"transaction_id":"txn_old"}],"next_cursor":"cursor-2","has_more":false}""",
                )
                url.endsWith("/item/remove") -> {
                    assertEquals(ACCESS, json.getString("access_token"))
                    assertTrue(url.startsWith(PLAID_SANDBOX_HOST))
                    PlaidHttpResult(200, """{"request_id":"req-remove"}""")
                }
                else -> error("unexpected $url")
            }
        }
        val api = PlaidHttpApi(PLAID_SANDBOX_HOST, "client-test", "secret-test", transport)
        val direct = Executor { it.run() }
        val source = PlaidBankSource(vault, store, api, direct) { it() }
        val app = AiCfoController(
            vault,
            store,
            object : AppClock {
                override fun nowEpochMs(): Long = 1_790_942_400_000L
            },
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            MemorySecureStore(),
            source,
        )
        val linker = PlaidLinker(vault, store, api, source, app, direct) { it() }
        linker.onPublicToken("public-sandbox-test-public-token")
        assertEquals(ACCESS, vault.read(PlaidBankSource.ACCESS_TOKEN_KEY))
        assertTrue(LinkPolicy.accepts(ACCESS))
        assertTrue(app.accounts().linked)
        assertEquals(BankLinkId.PLAID, app.activeBankLinkId())
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertEquals("\$100", app.home().savingsAmount)
        assertEquals("\$12", app.home().snapshotAt(0).amount)
        val prefs = store.read("synced_transactions").orEmpty() + store.read("plaid_tx_cursor").orEmpty()
        assertFalse(prefs.contains(ACCESS))
        assertFalse(prefs.contains("secret-test"))
        assertFalse(prefs.contains("public-sandbox"))
        assertEquals("cursor-2", store.read("plaid_tx_cursor"))
        assertNull(store.read("plaid_link_intent"))
        app.disconnectAll()
        assertNull(vault.read(PlaidBankSource.ACCESS_TOKEN_KEY))
        assertEquals("—", app.home().savingsAmount)
        assertTrue(calls.any { it.endsWith("/item/remove") })
        assertFalse(calls.any { it.contains("transfer") })
        assertFalse(calls.any { it.contains("production.plaid.com") })
    }

    @Test
    fun removeItemStaysOnSandboxAndAFailedRemoveStillDropsTheLocalToken() {
        var called = false
        val production = PlaidHttpApi(
            "https://production.plaid.com",
            "client-test",
            "secret-test",
            PlaidTransport { _, _ ->
                called = true
                error("production must not be called")
            },
        )
        assertFalse(production.configured)
        assertTrue(production.removeItem(ACCESS) is PlaidOutcome.Failed)
        assertFalse(called)

        val vault = MemoryTokenVault()
        val store = MemoryLocalStore()
        assertTrue(vault.put(PlaidBankSource.ACCESS_TOKEN_KEY, ACCESS))
        val api = PlaidHttpApi(PLAID_SANDBOX_HOST, "client-test", "secret-test", PlaidTransport { url, _ ->
            assertTrue(url.startsWith(PLAID_SANDBOX_HOST))
            assertTrue(url.endsWith("/item/remove"))
            error("sandbox remove failed")
        })
        val source = PlaidBankSource(vault, store, api, Executor { it.run() }) { it() }
        source.onDisconnected()
        vault.clear()
        assertNull(vault.read(PlaidBankSource.ACCESS_TOKEN_KEY))
    }

    @Test
    fun unpaidTrialDoesNotOpenPlaidLink() {
        val store = MemoryLocalStore()
        val vault = MemoryTokenVault()
        val calls = mutableListOf<String>()
        val api = PlaidHttpApi(PLAID_SANDBOX_HOST, "client-test", "secret-test", PlaidTransport { url, _ ->
            calls += url
            error("link must not open")
        })
        val direct = Executor { it.run() }
        val source = PlaidBankSource(vault, store, api, direct) { it() }
        val now = 1_700_000_000_000L
        val app = AiCfoController(
            vault,
            store,
            object : AppClock {
                override fun nowEpochMs(): Long = now + Pricing.TRIAL_WINDOW_MS
            },
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            MemorySecureStore(),
            source,
        )
        store.write("onboarding_complete", "true")
        store.write("trial_started_at", now.toString())
        val linker = PlaidLinker(vault, store, api, source, app, direct) { it() }
        linker.opener = { error("Link UI must not open") }
        linker.connect(advanceIntro = false)
        assertTrue(calls.isEmpty())
        assertFalse(app.plaidConnectionAllowed())
        assertEquals(
            "Your Pro trial has ended. Subscribe to connect a bank.",
            app.accounts().linkError,
        )
        assertNull(vault.read(PlaidBankSource.ACCESS_TOKEN_KEY))
    }

    @Test
    fun missingCredentialsDoNotOpenLinkOrInventAToken() {
        val store = MemoryLocalStore()
        val vault = MemoryTokenVault()
        val api = PlaidHttpApi(PLAID_SANDBOX_HOST, "", "", PlaidTransport { _, _ -> error("no network") })
        val direct = Executor { it.run() }
        val source = PlaidBankSource(vault, store, api, direct) { it() }
        val app = AiCfoController(
            vault,
            store,
            object : AppClock {
                override fun nowEpochMs(): Long = 10L
            },
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            MemorySecureStore(),
            source,
        )
        app.setBankLinkAvailability(false, "Plaid is not configured", "Add sandbox keys.")
        val linker = PlaidLinker(vault, store, api, source, app, direct) { it() }
        linker.connect(advanceIntro = true)
        assertFalse(app.accounts().linked)
        assertEquals("Plaid is not configured", app.accounts().linkError)
        assertNull(vault.read(PlaidBankSource.ACCESS_TOKEN_KEY))
        assertEquals("—", app.home().savingsAmount)
        assertTrue(app.home().moveCount() > 0)
    }

    @Test
    fun nullDisplayMessageDoesNotOpenLinkOrShowTheWordNull() {
        var opened: String? = "not-called"
        val app = linkedApp(
            PlaidTransport { url, _ ->
                assertTrue(url.startsWith(PLAID_SANDBOX_HOST))
                assertTrue(url.endsWith("/link/token/create"))
                PlaidHttpResult(
                    400,
                    """{"error_type":"INVALID_REQUEST","error_code":"INVALID_FIELD","display_message":null,"error_message":"link token was refused"}""",
                )
            },
        )
        app.linker.opener = { opened = it }
        app.linker.connect(advanceIntro = false)
        assertEquals("not-called", opened)
        assertEquals("link token was refused", app.controller.accounts().linkError)
        assertFalse(app.controller.accounts().linked)
    }

    @Test
    fun linkTokenOpensPlaidLink() {
        var opened: String? = null
        val app = linkedApp(
            PlaidTransport { url, _ ->
                assertTrue(url.endsWith("/link/token/create"))
                PlaidHttpResult(200, """{"link_token":"link-sandbox-test-token"}""")
            },
        )
        app.linker.opener = { opened = it }
        app.linker.connect(advanceIntro = false)
        assertEquals("link-sandbox-test-token", opened)
        assertEquals("", app.controller.accounts().linkError)
    }

    @Test
    fun exitWithANullDisplayMessageUsesTheErrorMessage() {
        val app = linkedApp(PlaidTransport { _, _ -> error("no network") })
        app.linker.onExit(cancelled = false, displayMessage = "null", errorMessage = "invalid link token")
        assertEquals("invalid link token", app.controller.accounts().linkError)
        app.linker.onExit(cancelled = true, displayMessage = null, errorMessage = "should stay")
        assertEquals("invalid link token", app.controller.accounts().linkError)
    }

    private fun linkedApp(transport: PlaidTransport): LinkHarness {
        val store = MemoryLocalStore()
        val vault = MemoryTokenVault()
        val api = PlaidHttpApi(PLAID_SANDBOX_HOST, "client-test", "secret-test", transport)
        val direct = Executor { it.run() }
        val source = PlaidBankSource(vault, store, api, direct) { it() }
        val controller = AiCfoController(
            vault,
            store,
            object : AppClock {
                override fun nowEpochMs(): Long = 10L
            },
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            MemorySecureStore(),
            source,
        )
        val linker = PlaidLinker(vault, store, api, source, controller, direct) { it() }
        return LinkHarness(controller, linker)
    }

    private class LinkHarness(
        val controller: AiCfoController,
        val linker: PlaidLinker,
    )

    private companion object {
        const val ACCESS: String = "access-sandbox-de3ce8ef-33f8-452c-a685-8671031fc0f6"
    }
}
