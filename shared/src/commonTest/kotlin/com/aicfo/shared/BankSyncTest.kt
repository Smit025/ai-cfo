package com.aicfo.shared

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.domain.AppObserver
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.security.LinkPolicy
import com.aicfo.shared.security.LocalStore
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemorySecureStore
import com.aicfo.shared.security.MemoryTokenVault
import com.aicfo.shared.security.TokenVault
import com.aicfo.shared.sync.AccountRole
import com.aicfo.shared.sync.BankFetch
import com.aicfo.shared.sync.BankLinkId
import com.aicfo.shared.sync.BankLinkSource
import com.aicfo.shared.sync.MayaStubBankSource
import com.aicfo.shared.sync.ProviderTransaction
import com.aicfo.shared.sync.SyncCode
import com.aicfo.shared.sync.SyncStatus
import com.aicfo.shared.sync.SyncTrigger
import com.aicfo.shared.sync.SyncedAccount
import com.aicfo.shared.sync.TransactionLedger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BankSyncTest {
    @Test
    fun unlinkedHomeDoesNotPretendBalancesAreFresh() {
        val app = linkedApp(ManualClock(10L), linked = false)
        assertEquals("", app.home().freshnessLabel)
        assertEquals("", app.accounts().freshnessLabel)
        assertFalse(app.home().syncStale)
    }

    @Test
    fun refreshMovesThroughSyncingToSuccessAndPersistsLastSyncedAt() {
        val clock = ManualClock(1_000_000L)
        val store = MemoryLocalStore()
        val app = linkedApp(clock, store)
        val codes = mutableListOf<String>()
        app.addObserver(object : AppObserver {
            override fun onChanged() {
                codes += app.syncStatus().code()
            }
        })
        app.refreshAccounts(SyncTrigger.ColdStart)
        assertEquals(listOf(SyncCode.SYNCING, SyncCode.SUCCESS), codes)
        val status = app.syncStatus()
        assertTrue(status is SyncStatus.Success)
        assertEquals(1_000_000L, status.lastSyncedAt)
        assertEquals("1000000", store.read("last_synced_at"))
        assertEquals(SyncTrigger.ColdStart, app.lastSyncTrigger())
        assertEquals("Updated just now", app.home().freshnessLabel)
        assertEquals("Updated just now", app.accounts().freshnessLabel)
        assertEquals("\$4,812", app.accounts().groupAt(0).accountAt(0).balance)
        assertEquals(MayaStubBankSource.transactions.size, app.ingestedTransactionCount())

        clock.now = 1_000_000L + 3L * 60_000L
        assertEquals("Updated 3m ago", app.accounts().freshnessLabel)
        clock.now = 1_000_000L + 2L * 60L * 60_000L
        assertEquals("Updated 2h ago", app.home().freshnessLabel)
        clock.now = 1_000_000L + 5L * 24L * 60L * 60_000L
        assertEquals("Updated 5d ago", app.home().freshnessLabel)

        val restarted = AiCfoController(MemoryTokenVault(), store, clock)
        val restored = restarted.syncStatus()
        assertTrue(restored is SyncStatus.Success)
        assertEquals(1_000_000L, restored.lastSyncedAt)
        assertEquals("Updated 5d ago", restarted.accounts().freshnessLabel)
        assertEquals(MayaStubBankSource.transactions.size, restarted.ingestedTransactionCount())
    }

    @Test
    fun secondRefreshReconcilesByProviderTransactionId() {
        val app = linkedApp(ManualClock(50L))
        app.refreshAccounts(SyncTrigger.PullToRefresh)
        val count = app.ingestedTransactionCount()
        assertTrue(count >= 2)
        app.refreshAccounts(SyncTrigger.Foreground)
        app.refreshAccounts(SyncTrigger.Manual)
        assertEquals(count, app.ingestedTransactionCount())
        assertEquals(-4_700L, app.ingestedTransaction("maya-tx-gympass-202610")!!.amountMinor)
        assertEquals(SyncTrigger.Manual, app.lastSyncTrigger())
    }

    @Test
    fun ledgerIsIdempotentAndReconcilesChangedRows() {
        val store = MemoryLocalStore()
        val ledger = TransactionLedger(store)
        val original = tx("tx-coffee", -450, "Coffee")
        val first = ledger.ingest(listOf(original, original.copy(providerTransactionId = "")))
        assertEquals(1, first.inserted)
        assertEquals(0, first.unchanged)
        val duplicate = ledger.ingest(listOf(original, original))
        assertEquals(0, duplicate.inserted)
        assertEquals(0, duplicate.reconciled)
        assertEquals(2, duplicate.unchanged)
        val changed = ledger.ingest(listOf(original.copy(amountMinor = -500, name = "Coffee\u001fbar")))
        assertEquals(1, changed.reconciled)
        assertEquals(-500L, ledger.find("tx-coffee")!!.amountMinor)
        assertEquals(1, TransactionLedger(store).count())
        assertEquals(-500L, TransactionLedger(store).find("tx-coffee")!!.amountMinor)

        val secretId = "link_live_access_should_not_persist"
        val skipped = ledger.ingest(
            listOf(
                tx(secretId, -1, "Nope"),
                tx("tx-name", -1, "paid link_should_not_store extra"),
            ),
        )
        assertEquals(1, skipped.inserted)
        assertNull(ledger.find(secretId))
        val storedName = ledger.find("tx-name")!!.name
        assertFalse(storedName.contains("link_should_not_store"))
        val stored = store.read("synced_transactions").orEmpty()
        assertFalse(stored.contains("link_should_not_store"))
        assertFalse(stored.contains(secretId))
    }

    @Test
    fun failedSyncStaysStaleAndDoesNotLogTokens() {
        val store = MemoryLocalStore()
        val source = ScriptedSource(
            BankFetch.Unavailable("down token=link_secret_value please"),
        )
        val app = linkedApp(ManualClock(10L), store, source)
        app.refreshAccounts(SyncTrigger.ColdStart)
        val status = app.syncStatus()
        assertTrue(status is SyncStatus.Failed)
        assertFalse(status.reason.contains("link_secret_value"))
        assertFalse(status.reason.contains("token=link_"))
        assertTrue(app.accounts().syncStale)
        assertEquals("Try again", app.accounts().syncActionLabel)
        assertFalse(app.accounts().freshnessLabel.startsWith("Updated"))
        assertTrue(app.accounts().freshnessLabel.contains("aren't current"))
        assertEquals(app.accounts().freshnessLabel, app.home().freshnessLabel)
        assertEquals("FAILED", store.read("sync_state"))
    }

    @Test
    fun needsReauthSurvivesRestartUntilReconnect() {
        val clock = ManualClock(5_000L)
        val store = MemoryLocalStore()
        val source = ScriptedSource(BankFetch.Ok(MayaStubBankSource.previewAccounts(), emptyList()))
        val app = linkedApp(clock, store, source)
        app.refreshAccounts(SyncTrigger.Manual)
        assertEquals(1, source.fetches)
        clock.now = 5_000L + 60L * 60_000L
        app.debugSimulateNeedsReauth()
        assertTrue(app.syncStatus() is SyncStatus.NeedsReauth)
        assertTrue(app.home().syncStale)
        assertEquals("Reconnect", app.home().syncActionLabel)
        assertTrue(app.home().freshnessLabel.startsWith("Reconnect"))
        assertTrue(app.home().freshnessLabel.contains("1h ago"))
        assertFalse(app.accounts().freshnessLabel.startsWith("Updated"))

        app.refreshAccounts(SyncTrigger.PullToRefresh)
        assertEquals(1, source.fetches)
        assertTrue(app.syncStatus() is SyncStatus.NeedsReauth)

        val restarted = AiCfoController(
            MemoryTokenVault(),
            store,
            clock,
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            source,
        )
        assertTrue(restarted.syncStatus() is SyncStatus.NeedsReauth)
        restarted.refreshAccounts(SyncTrigger.Foreground)
        assertEquals(1, source.fetches)

        restarted.reconnectBank()
        assertTrue(restarted.syncStatus() is SyncStatus.Success)
        assertEquals(2, source.fetches)
        assertEquals("Updated just now", restarted.accounts().freshnessLabel)
        assertFalse(restarted.accounts().syncStale)
        assertEquals("false", store.read("sync_needs_reauth"))
    }

    @Test
    fun loginRequiredFromTheSourceBlocksTheNextRefresh() {
        val source = ScriptedSource(BankFetch.LoginRequired)
        val app = linkedApp(ManualClock(10L), source = source)
        app.refreshAccounts(SyncTrigger.ColdStart)
        assertEquals(1, source.fetches)
        assertTrue(app.syncStatus() is SyncStatus.NeedsReauth)
        app.refreshAccounts(SyncTrigger.Foreground)
        assertEquals(1, source.fetches)
    }

    @Test
    fun readOnlyRefusalDoesNotCallFetch() {
        val source = MovingSource()
        val app = linkedApp(ManualClock(10L), source = source)
        app.refreshAccounts(SyncTrigger.Manual)
        assertFalse(source.fetched)
        val status = app.syncStatus()
        assertTrue(status is SyncStatus.Failed)
        assertTrue(status.reason.contains("read-only"))
        assertTrue(app.accounts().freshnessLabel.contains("aren't current"))
    }

    @Test
    fun refreshDoesNotReadTheTokenVault() {
        val vault = ReadBoomVault()
        val store = MemoryLocalStore()
        val app = AiCfoController(vault, store, ManualClock(10L))
        assertTrue(app.connectReadOnlyStub())
        assertTrue(app.syncStatus() is SyncStatus.Success)
        app.refreshAccounts(SyncTrigger.Foreground)
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertTrue(vault.puts > 0)
    }

    @Test
    fun throwingSourceIsAStaleFailureWithoutTheToken() {
        val app = linkedApp(ManualClock(10L), source = ThrowingSource())
        app.refreshAccounts(SyncTrigger.ColdStart)
        val status = app.syncStatus()
        assertTrue(status is SyncStatus.Failed)
        assertFalse(status.reason.contains("link_should_not_leak"))
        assertFalse(app.home().freshnessLabel.startsWith("Updated"))
    }

    @Test
    fun deferredDeliveryStaysSyncingUntilTheSourceReturns() {
        val clock = ManualClock(80L)
        val source = DeferredSource()
        val app = linkedApp(clock, source = source)
        app.refreshAccounts(SyncTrigger.ColdStart)
        assertTrue(app.syncStatus() is SyncStatus.Syncing)
        assertEquals("Updating…", app.accounts().freshnessLabel)
        assertEquals("Updating…", app.home().freshnessLabel)
        assertEquals(0, app.ingestedTransactionCount())

        app.refreshAccounts(SyncTrigger.PullToRefresh)
        assertEquals(2, source.pending.size)
        source.pending[0](
            BankFetch.Ok(MayaStubBankSource.previewAccounts(), MayaStubBankSource.transactions),
        )
        assertTrue(app.syncStatus() is SyncStatus.Syncing)
        source.pending[1](
            BankFetch.Ok(MayaStubBankSource.previewAccounts(), MayaStubBankSource.transactions),
        )
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertEquals(MayaStubBankSource.transactions.size, app.ingestedTransactionCount())
        assertEquals(SyncTrigger.PullToRefresh, app.lastSyncTrigger())
    }

    @Test
    fun coldStartDoesNotAutoLinkTheSample() {
        val app = linkedApp(ManualClock(10L), linked = false)
        app.refreshAccounts(SyncTrigger.ColdStart)
        app.refreshAccounts(SyncTrigger.Foreground)
        assertFalse(app.accounts().linked)
        assertEquals("Nothing linked · Maya Chen", app.accounts().subtitle)
        assertEquals("", app.home().freshnessLabel)
        assertEquals("", app.accounts().freshnessLabel)
        assertTrue(app.syncStatus() is SyncStatus.Idle)
    }

    @Test
    fun simulateReconnectWithoutAPriorLinkShowsReconnect() {
        val store = MemoryLocalStore()
        val app = linkedApp(ManualClock(10L), store, linked = false)
        assertFalse(app.accounts().linked)
        app.debugSimulateNeedsReauth()
        assertTrue(app.accounts().linked)
        assertEquals("true", store.read("banks_linked"))
        assertTrue(app.syncStatus() is SyncStatus.NeedsReauth)
        assertTrue(app.home().syncStale)
        assertTrue(app.accounts().syncStale)
        assertEquals("Reconnect", app.home().syncActionLabel)
        assertEquals("Reconnect", app.accounts().syncActionLabel)
        assertEquals(
            "Reconnect to refresh balances. Last update just now.",
            app.home().freshnessLabel,
        )
        assertEquals(app.home().freshnessLabel, app.accounts().freshnessLabel)
        assertFalse(app.home().freshnessLabel.startsWith("Updated"))
        assertEquals("Maya sample · not your bank", app.accounts().subtitle)
        assertEquals("—", app.home().savingsAmount)

        app.reconnectBank()
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertEquals("Updated just now", app.home().freshnessLabel)
        assertEquals("Updated just now", app.accounts().freshnessLabel)
        assertEquals("", app.accounts().syncActionLabel)
        assertFalse(app.accounts().syncStale)
        assertEquals("false", store.read("sync_needs_reauth"))
    }

    @Test
    fun simulateFailureWithoutAPriorLinkShowsTryAgain() {
        val app = linkedApp(ManualClock(10L), linked = false)
        assertFalse(app.accounts().linked)
        app.debugSimulateSyncFailure()
        assertTrue(app.accounts().linked)
        val status = app.syncStatus()
        assertTrue(status is SyncStatus.Failed)
        assertEquals("Couldn't refresh", status.reason)
        assertEquals("Try again", app.home().syncActionLabel)
        assertEquals("Try again", app.accounts().syncActionLabel)
        assertTrue(app.home().syncStale)
        assertEquals(
            "Couldn't refresh. Last update just now.",
            app.accounts().freshnessLabel,
        )
        assertEquals(app.accounts().freshnessLabel, app.home().freshnessLabel)
        assertFalse(app.home().freshnessLabel.startsWith("Updated"))
        assertFalse(app.accounts().subtitle.startsWith("Nothing linked"))

        app.refreshAccounts(SyncTrigger.Manual)
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertEquals("Updated just now", app.home().freshnessLabel)
        assertEquals("Updated just now", app.accounts().freshnessLabel)
        assertEquals("", app.home().syncActionLabel)
    }

    @Test
    fun releaseBuildIgnoresSimulateWhenNothingIsLinked() {
        val store = MemoryLocalStore()
        val app = AiCfoController(
            MemoryTokenVault(),
            store,
            ManualClock(10L),
            Markets.unitedStates(),
            EmptyLocalStrings,
            false,
            MemorySecureStore(),
        )
        assertFalse(app.accounts().linked)
        app.debugSimulateNeedsReauth()
        app.debugSimulateSyncFailure()
        assertFalse(app.accounts().linked)
        assertTrue(app.syncStatus() is SyncStatus.Idle)
        assertEquals("", app.home().freshnessLabel)
        assertEquals("", app.accounts().freshnessLabel)
        assertEquals("Nothing linked · Maya Chen", app.accounts().subtitle)
        assertNull(store.read("banks_linked"))
        assertNull(store.read("sync_needs_reauth"))
        assertNull(store.read("sync_state"))
    }

    @Test
    fun releaseBuildIgnoresTheReconnectQaHook() {
        val store = MemoryLocalStore()
        val app = AiCfoController(
            MemoryTokenVault(),
            store,
            ManualClock(10L),
            Markets.unitedStates(),
            EmptyLocalStrings,
            false,
            MemorySecureStore(),
        )
        store.write("banks_linked", "true")
        app.refreshAccounts(SyncTrigger.ColdStart)
        assertTrue(app.syncStatus() is SyncStatus.Success)
        app.debugSimulateNeedsReauth()
        assertTrue(app.syncStatus() is SyncStatus.Success)
        assertEquals("false", store.read("sync_needs_reauth"))
        app.debugSimulateSyncFailure()
        assertTrue(app.syncStatus() is SyncStatus.Success)
    }

    @Test
    fun debugFailureKeepsTheLastSyncAndAsksToTryAgain() {
        val app = linkedApp(ManualClock(10L))
        app.refreshAccounts(SyncTrigger.ColdStart)
        app.debugSimulateSyncFailure()
        val status = app.syncStatus()
        assertTrue(status is SyncStatus.Failed)
        assertEquals("Couldn't refresh", status.reason)
        assertEquals("Try again", app.home().syncActionLabel)
        assertEquals("Try again", app.accounts().syncActionLabel)
        assertTrue(app.home().freshnessLabel.contains("Last update"))
        assertFalse(app.home().freshnessLabel.startsWith("Updated"))
    }

    @Test
    fun disconnectClearsSyncMetadata() {
        val store = MemoryLocalStore()
        val app = linkedApp(ManualClock(10L), store)
        app.refreshAccounts(SyncTrigger.Manual)
        assertTrue(app.ingestedTransactionCount() > 0)
        app.disconnectAll()
        assertEquals("", app.home().freshnessLabel)
        assertEquals(0, app.ingestedTransactionCount())
        assertNull(store.read("last_synced_at"))
        assertTrue(AiCfoController(MemoryTokenVault(), store, ManualClock(10L)).syncStatus() is SyncStatus.Idle)
    }

    @Test
    fun coldStartDoesNotInventBalancesBeforeAPlaidSync() {
        val app = linkedApp(ManualClock(10L), linked = false)
        app.refreshAccounts(SyncTrigger.ColdStart)
        assertEquals(0, app.accounts().groupCount())
        assertEquals("—", app.home().savingsAmount)
        assertEquals("—", app.home().netWorthAmount)
        assertEquals("—", app.home().snapshotAt(0).amount)
        assertEquals("gympass", app.home().moveAt(0).id)
    }

    @Test
    fun plaidSuccessUnlocksHomeFiguresAndAFailureKeepsThemStale() {
        val clock = ManualClock(1_760_544_000_000L)
        val store = MemoryLocalStore()
        val savings = synced("sav", "Plaid Savings", 100_000, AccountRole.SAVINGS, "CASH")
        val checking = synced("chk", "Plaid Checking", 50_000, AccountRole.CASH, "CASH")
        val card = synced("card", "Plaid Credit Card", 20_000, AccountRole.CREDIT, "CARDS_AND_LOANS")
        val source = PlaidFakeSource(
            BankFetch.Ok(
                accounts = listOf(savings, checking, card),
                transactions = listOf(
                    tx("rent", -2_500, "Rent", "RENT_AND_UTILITIES", clock.now),
                    tx("coffee", -1_800, "Coffee", "FOOD_AND_DRINK", clock.now),
                    tx("pay", 50_000, "Payroll", "INCOME", clock.now),
                ),
            ),
        )
        val app = linkedApp(clock, store, source)
        assertEquals("—", app.home().savingsAmount)
        app.refreshAccounts(SyncTrigger.ColdStart)
        assertEquals("\$1,000", app.home().savingsAmount)
        assertEquals("\$1,300", app.home().netWorthAmount)
        assertEquals("\$25", app.home().snapshotAt(0).amount)
        assertEquals("\$18", app.home().snapshotAt(1).amount)
        assertEquals("\$457", app.home().snapshotAt(2).amount)
        assertEquals("From your linked accounts", app.home().runway)
        assertEquals("Updated just now", app.home().freshnessLabel)
        assertFalse(app.home().syncStale)
        assertEquals("gympass", app.home().moveAt(0).id)
        assertEquals("true", store.read("real_bank_sync"))
        assertEquals(BankLinkId.PLAID, store.read("bank_link_kind"))

        source.next = BankFetch.Unavailable("down")
        app.refreshAccounts(SyncTrigger.Foreground)
        assertEquals("\$1,000", app.home().savingsAmount)
        assertEquals("\$1,300", app.home().netWorthAmount)
        assertEquals("\$457", app.home().snapshotAt(2).amount)
        assertTrue(app.home().syncStale)
        assertTrue(app.accounts().syncStale)
        assertEquals("Try again", app.home().syncActionLabel)
        assertFalse(app.home().freshnessLabel.startsWith("Updated"))
        assertTrue(app.home().freshnessLabel.contains("aren't current") || app.home().freshnessLabel.contains("Last update"))

        val restarted = AiCfoController(
            MemoryTokenVault(),
            store,
            clock,
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            source,
        )
        assertEquals("\$1,000", restarted.home().savingsAmount)
        assertEquals("\$1,300", restarted.home().netWorthAmount)
        assertTrue(restarted.syncStatus() is SyncStatus.Failed)
    }

    @Test
    fun mayaSampleAndReleaseDebugActionsDoNotUnlockHomeFigures() {
        val debug = linkedApp(ManualClock(10L), linked = false)
        assertTrue(debug.connectReadOnlyStub())
        assertTrue(debug.accounts().groupCount() > 0)
        assertEquals("—", debug.home().savingsAmount)
        assertEquals("—", debug.home().netWorthAmount)
        debug.refreshAccounts(SyncTrigger.Manual)
        assertEquals("—", debug.home().savingsAmount)

        val store = MemoryLocalStore()
        val release = AiCfoController(
            MemoryTokenVault(),
            store,
            ManualClock(10L),
            Markets.unitedStates(),
            EmptyLocalStrings,
            false,
            MemorySecureStore(),
        )
        assertFalse(release.connectReadOnlyStub())
        release.debugSimulateNeedsReauth()
        release.debugSimulateSyncFailure()
        assertFalse(release.accounts().linked)
        assertEquals("—", release.home().savingsAmount)
        assertNull(store.read("real_bank_sync"))
    }

    @Test
    fun stubSourceIsReadOnly() {
        assertTrue(MayaStubBankSource().readOnly)
        assertEquals("maya-stub", MayaStubBankSource.ID)
    }

    private fun linkedApp(
        clock: ManualClock,
        store: MemoryLocalStore = MemoryLocalStore(),
        source: BankLinkSource = MayaStubBankSource(),
        linked: Boolean = true,
    ): AiCfoController {
        if (linked) store.write("banks_linked", "true")
        return AiCfoController(
            MemoryTokenVault(),
            store,
            clock,
            Markets.unitedStates(),
            EmptyLocalStrings,
            true,
            source,
        )
    }
}

private class ManualClock(var now: Long) : AppClock {
    override fun nowEpochMs(): Long = now
}

private fun tx(
    id: String,
    amount: Long,
    name: String,
    category: String = "",
    postedAtEpochMs: Long = 1L,
) = ProviderTransaction(
    providerTransactionId = id,
    accountId = "chase-checking",
    amountMinor = amount,
    currency = "USD",
    postedAtEpochMs = postedAtEpochMs,
    name = name,
    category = category,
)

private fun synced(id: String, name: String, minor: Long, role: String, group: String) = SyncedAccount(
    id = id,
    name = name,
    maskLine = "··0000",
    balanceMinor = minor,
    currency = "USD",
    group = group,
    initials = "PL",
    colorHex = "#2563EB",
    role = role,
)

private class PlaidFakeSource(var next: BankFetch) : BankLinkSource {
    override val id: String = BankLinkId.PLAID
    override val readOnly: Boolean = true
    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        deliver(next)
    }
}

private class ScriptedSource(var next: BankFetch) : BankLinkSource {
    var fetches: Int = 0
    override val id: String = "scripted"
    override val readOnly: Boolean = true
    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        fetches += 1
        deliver(next)
    }
}

private class MovingSource : BankLinkSource {
    var fetched: Boolean = false
    override val id: String = "mover"
    override val readOnly: Boolean = false
    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        fetched = true
        deliver(BankFetch.Ok(emptyList(), emptyList()))
    }
}

private class ThrowingSource : BankLinkSource {
    override val id: String = "throw"
    override val readOnly: Boolean = true
    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        throw IllegalStateException("link_should_not_leak token=abc")
    }
}

private class DeferredSource : BankLinkSource {
    val pending = mutableListOf<(BankFetch) -> Unit>()
    override val id: String = "deferred"
    override val readOnly: Boolean = true
    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        pending += deliver
    }
}

private class ReadBoomVault : TokenVault {
    private val values = mutableMapOf<String, String>()
    var puts: Int = 0

    override fun put(key: String, value: String): Boolean {
        if (!LinkPolicy.accepts(value)) return false
        puts += 1
        values[key] = value
        return true
    }

    override fun read(key: String): String? = throw IllegalStateException("vault read")

    override fun clear() {
        values.clear()
    }
}
