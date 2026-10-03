package com.aicfo.app.plaid

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemoryTokenVault
import com.aicfo.shared.sync.ProviderTransaction
import com.aicfo.shared.sync.SyncStatus
import com.aicfo.shared.sync.SyncedAccount
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
    fun linkTokenRequestsTransactionsOnly() {
        val created = PlaidRequests.linkToken(
            clientId = "client-test",
            secret = "secret-test",
            clientUserId = "fw-user",
            packageName = "com.aicfo.app",
            accessTokenForUpdate = null,
        )
        assertEquals("transactions", created.getJSONArray("products").getString(0))
        assertEquals(1, created.getJSONArray("products").length())
        val filters = created.getJSONObject("account_filters")
        val depository = filters.getJSONObject("depository").getJSONArray("account_subtypes")
        assertEquals("checking", depository.getString(0))
        assertEquals("savings", depository.getString(1))
        assertEquals("credit card", filters.getJSONObject("credit").getJSONArray("account_subtypes").getString(0))
        assertEquals("com.aicfo.app", created.getString("android_package_name"))
        assertFalse(created.has("access_token"))
        val wire = created.toString()
        assertFalse(wire.contains("transfer"))
        assertFalse(wire.contains("payment_initiation"))
        assertFalse(wire.contains("processor"))

        val update = PlaidRequests.linkToken(
            clientId = "client-test",
            secret = "secret-test",
            clientUserId = "fw-user",
            packageName = "com.aicfo.app",
            accessTokenForUpdate = ACCESS,
        )
        assertFalse(update.has("products"))
        assertEquals(ACCESS, update.getString("access_token"))
    }

    @Test
    fun mapsAccountsTransactionsAndLoginRequired() {
        val accounts = PlaidResponses.accounts(
            JSONObject(
                """
                {"accounts":[
                  {"account_id":"acc_checking","name":"Plaid Checking","mask":"0000","type":"depository","subtype":"checking",
                   "balances":{"current":110.5,"iso_currency_code":"USD"}},
                  {"account_id":"acc_card","name":"Plaid Credit Card","mask":"3333","type":"credit","subtype":"credit card",
                   "balances":{"current":410,"iso_currency_code":"USD"}}
                ]}
                """.trimIndent(),
            ),
        )
        assertEquals("Plaid Checking", accounts[0].name)
        assertEquals("··0000 · Checking", accounts[0].maskLine)
        assertEquals("CASH", accounts[0].group)
        assertEquals(11_050L, accounts[0].balanceMinor)
        assertEquals("··3333 · Credit Card", accounts[1].maskLine)
        assertEquals("CARDS_AND_LOANS", accounts[1].group)

        val txs = PlaidResponses.transactions(
            JSONObject(
                """
                {"added":[{"transaction_id":"txn_abc","account_id":"acc_checking","amount":6.33,
                  "iso_currency_code":"USD","date":"2026-09-01","name":"Starbucks"}]}
                """.trimIndent(),
            ).getJSONArray("added"),
        )
        assertEquals("txn_abc", txs[0].providerTransactionId)
        assertEquals(-633L, txs[0].amountMinor)

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
        assertNull(PlaidResponses.failure(200, """{"accounts":[],"item":{"item_id":"item"}}"""))
    }

    @Test
    fun httpExchangeAndSyncStayReadOnly() {
        val calls = mutableListOf<String>()
        val transport = PlaidTransport { url, body ->
            calls += url.substringAfter(".com")
            val json = JSONObject(body)
            assertFalse(json.has("transfer"))
            when {
                url.endsWith("/link/token/create") -> PlaidHttpResult(200, """{"link_token":"link-sandbox-test-token"}""")
                url.endsWith("/item/public_token/exchange") -> {
                    assertEquals("public-sandbox-test-public-token", json.getString("public_token"))
                    PlaidHttpResult(200, """{"access_token":"$ACCESS","item_id":"item"}""")
                }
                url.endsWith("/accounts/balance/get") -> PlaidHttpResult(
                    200,
                    """{"accounts":[{"account_id":"acc_checking","name":"Plaid Checking","mask":"0000","type":"depository","subtype":"checking","balances":{"current":110,"iso_currency_code":"USD"}}]}""",
                )
                url.endsWith("/transactions/sync") -> PlaidHttpResult(
                    200,
                    """{"added":[{"transaction_id":"txn_abc","account_id":"acc_checking","amount":6.33,"iso_currency_code":"USD","date":"2026-09-01","name":"Starbucks"}],"modified":[],"removed":[{"transaction_id":"txn_old"}],"next_cursor":"cursor-2","has_more":false}""",
                )
                else -> error("unexpected $url")
            }
        }
        val api = PlaidHttpApi("https://sandbox.plaid.com", "client-test", "secret-test", transport)
        assertTrue(api.configured)
        val link = api.createLinkToken("fw-user", "com.aicfo.app", null)
        assertTrue(link is PlaidOutcome.Ok)
        val access = api.exchangePublicToken("public-sandbox-test-public-token")
        assertEquals(ACCESS, (access as PlaidOutcome.Ok).value)
        val page = api.sync(ACCESS, null) as PlaidOutcome.Ok
        assertEquals("txn_abc", page.value.transactions.single().providerTransactionId)
        assertEquals(listOf("txn_old"), page.value.removedIds)
        assertEquals("cursor-2", page.value.nextCursor)
        assertEquals(
            listOf(
                "/link/token/create",
                "/item/public_token/exchange",
                "/accounts/balance/get",
                "/transactions/sync",
            ),
            calls,
        )
    }

    @Test
    fun secondSyncUpdatesByTransactionIdAndKeepsTheTokenOutOfPrefs() {
        val store = MemoryLocalStore()
        val vault = MemoryTokenVault()
        assertTrue(vault.put(PlaidBankSource.ACCESS_TOKEN_KEY, ACCESS))
        val api = object : PlaidApi {
            var calls = 0
            override val configured: Boolean = true
            override fun createLinkToken(clientUserId: String, packageName: String, accessTokenForUpdate: String?) =
                PlaidOutcome.Failed("unused")
            override fun exchangePublicToken(publicToken: String): PlaidOutcome<String> {
                assertEquals("public-sandbox-test-public-token", publicToken)
                return PlaidOutcome.Ok(ACCESS)
            }
            override fun sync(accessToken: String, cursor: String?): PlaidOutcome<PlaidSnapshot> {
                assertEquals(ACCESS, accessToken)
                calls += 1
                val amount = if (calls == 1) -633L else -700L
                val removed = if (calls == 1) listOf("txn_old") else emptyList()
                return PlaidOutcome.Ok(
                    PlaidSnapshot(
                        accounts = listOf(checking),
                        transactions = listOf(
                            ProviderTransaction("txn_abc", "acc_checking", amount, "USD", 1_000L, "Starbucks"),
                        ),
                        removedIds = removed,
                        nextCursor = "cursor-$calls",
                    ),
                )
            }
        }
        val source = PlaidBankSource(vault, store, api, Executor { it.run() }) { it() }
        assertTrue(source.readOnly)
        assertEquals("plaid", source.id)
        val app = AiCfoController(
            vault,
            store,
            object : AppClock {
                override fun nowEpochMs(): Long = 50L
            },
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            source,
        )
        val netWorth = app.home().netWorthAmount
        val savings = app.home().savingsAmount
        store.write("plaid_link_intent", "create")
        val linker = PlaidLinker(vault, store, api, source, app, Executor { it.run() }) { it() }
        linker.onPublicToken("public-sandbox-test-public-token")
        assertEquals("txn_abc", app.ingestedTransaction("txn_abc")?.providerTransactionId)
        assertEquals(-633L, app.ingestedTransaction("txn_abc")!!.amountMinor)
        assertNull(app.ingestedTransaction("txn_old"))
        app.refreshAccounts(com.aicfo.shared.sync.SyncTrigger.PullToRefresh)
        assertEquals(1, app.ingestedTransactionCount())
        assertEquals(-700L, app.ingestedTransaction("txn_abc")!!.amountMinor)
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertEquals("Plaid Checking", app.accounts().groupAt(0).accountAt(0).name)
        assertEquals("··0000 · Checking", app.accounts().groupAt(0).accountAt(0).detail)
        assertEquals("\$110", app.accounts().groupAt(0).accountAt(0).balance)
        assertEquals("Read-only", app.accounts().groupAt(0).accountAt(0).readOnlyLabel)
        assertEquals(netWorth, app.home().netWorthAmount)
        assertEquals(savings, app.home().savingsAmount)
        val blob = listOf(
            "synced_accounts",
            "synced_transactions",
            "plaid_tx_cursor",
            "bank_link_kind",
            "plaid_link_intent",
            "plaid_client_user_id",
        ).joinToString("|") { store.read(it).orEmpty() }
        assertFalse(blob.contains(ACCESS))
        assertFalse(blob.contains("public-sandbox"))
        assertEquals(ACCESS, vault.read(PlaidBankSource.ACCESS_TOKEN_KEY))
    }

    @Test
    fun loginRequiredDoesNotLookCurrentAndUnconfiguredConnectDoesNotOpen() {
        val store = MemoryLocalStore()
        val vault = MemoryTokenVault()
        assertTrue(vault.put(PlaidBankSource.ACCESS_TOKEN_KEY, ACCESS))
        val api = object : PlaidApi {
            override val configured: Boolean = true
            override fun createLinkToken(clientUserId: String, packageName: String, accessTokenForUpdate: String?) =
                PlaidOutcome.Ok("link-sandbox-update")
            override fun exchangePublicToken(publicToken: String) = PlaidOutcome.Failed("unused")
            override fun sync(accessToken: String, cursor: String?) = PlaidOutcome.LoginRequired
        }
        val inline = Executor { command: Runnable -> command.run() }
        val source = PlaidBankSource(vault, store, api, inline) { it() }
        val app = AiCfoController(
            vault,
            store,
            object : AppClock {
                override fun nowEpochMs(): Long = 10L
            },
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            source,
        )
        store.write("banks_linked", "true")
        store.write("bank_link_kind", "plaid")
        app.refreshAccounts(com.aicfo.shared.sync.SyncTrigger.ColdStart)
        assertTrue(app.syncStatus() is SyncStatus.NeedsReauth)
        assertEquals("Reconnect", app.accounts().syncActionLabel)
        assertFalse(app.home().freshnessLabel.startsWith("Updated"))
        var opened: String? = null
        val linker = PlaidLinker(vault, store, api, source, app, inline) { it() }
        linker.opener = { opened = it }
        linker.onSyncAction(com.aicfo.shared.sync.SyncCode.NEEDS_REAUTH)
        assertEquals("link-sandbox-update", opened)
        assertEquals("update", store.read("plaid_link_intent"))

        val blocked = object : PlaidApi {
            override val configured: Boolean = false
            override fun createLinkToken(clientUserId: String, packageName: String, accessTokenForUpdate: String?) =
                error("should not create a token")
            override fun exchangePublicToken(publicToken: String) = error("should not exchange")
            override fun sync(accessToken: String, cursor: String?) = error("should not sync")
        }
        val quiet = PlaidLinker(vault, store, blocked, source, app, inline) { it() }
        var attempted = false
        quiet.opener = { attempted = true }
        quiet.connect(true)
        assertFalse(attempted)
        assertTrue(app.onboarding().linkError.contains("Plaid is not configured"))
    }

    private companion object {
        const val ACCESS = "access-sandbox-de3ce8ef-33f8-452c-a685-8671031fc0f6"
        val checking = SyncedAccount(
            id = "acc_checking",
            name = "Plaid Checking",
            maskLine = "··0000 · Checking",
            balanceMinor = 11_000,
            currency = "USD",
            group = "CASH",
            initials = "PC",
            colorHex = "#2563EB",
        )
    }
}
