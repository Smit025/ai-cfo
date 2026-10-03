package com.aicfo.shared.sync

import com.aicfo.shared.data.MayaStub
import com.aicfo.shared.model.LinkedAccount
import com.aicfo.shared.security.LocalStore
import com.aicfo.shared.security.SafeLog

/**
 * Why a refresh was requested. The apps pass this into [com.aicfo.shared.domain.AiCfoController.refreshAccounts].
 * Cold start, returning to the foreground, and pull-to-refresh all run the same state machine.
 */
enum class SyncTrigger {
    ColdStart,
    Foreground,
    PullToRefresh,
    Manual,
}

object SyncCode {
    const val IDLE = "IDLE"
    const val SYNCING = "SYNCING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val NEEDS_REAUTH = "NEEDS_REAUTH"
}

/**
 * Bank-link freshness. A failed or re-auth state must not be presented as a current balance.
 * [Success.lastSyncedAt] is epoch milliseconds.
 */
sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Success(val lastSyncedAt: Long) : SyncStatus()
    data class Failed(val reason: String) : SyncStatus()
    data object NeedsReauth : SyncStatus()

    fun code(): String = when (this) {
        Idle -> SyncCode.IDLE
        Syncing -> SyncCode.SYNCING
        is Success -> SyncCode.SUCCESS
        is Failed -> SyncCode.FAILED
        NeedsReauth -> SyncCode.NEEDS_REAUTH
    }
}

/**
 * One institution transaction, keyed by the provider's own id.
 * Re-delivering the same id updates that row. It never inserts a second copy.
 */
data class ProviderTransaction(
    val providerTransactionId: String,
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val postedAtEpochMs: Long,
    val name: String,
)

/** Balance row returned by a [BankLinkSource]. Display labels are formatted later. */
data class SyncedAccount(
    val id: String,
    val name: String,
    val maskLine: String,
    val balanceMinor: Long,
    val currency: String,
    val group: String,
    val initials: String,
    val colorHex: String,
)

sealed class BankFetch {
    data class Ok(
        val accounts: List<SyncedAccount>,
        val transactions: List<ProviderTransaction>,
        val removedTransactionIds: List<String> = emptyList(),
    ) : BankFetch()

    data class Unavailable(val reason: String) : BankFetch()
    data object LoginRequired : BankFetch()
}

/**
 * Bank-link provider. Read-only fetch of balances and transactions.
 *
 * Swap [MayaStubBankSource] for a Plaid implementation without changing the
 * controller's sync state machine. There is no transfer, payment, or
 * card-charge method — Finwise never moves money.
 *
 * [fetch] delivers exactly once, on the thread that called it. The stub
 * delivers before returning. A live source may return immediately and deliver
 * later only by posting back onto that same thread. Do not log link tokens.
 * Read the vault inside the platform source; this interface does not take a token.
 *
 * There is no transfer, payment, or card-charge method.
 */
interface BankLinkSource {
    val id: String

    /** Must be true. The controller will not call [fetch] when this is false. */
    val readOnly: Boolean

    fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit)

    /** Drop provider cursors or other link state. Must not log tokens. */
    fun onDisconnected() {}
}

data class IngestReport(
    val inserted: Int,
    val reconciled: Int,
    val unchanged: Int,
)

/**
 * Idempotent transaction store. The primary key is [ProviderTransaction.providerTransactionId].
 * Prefs are enough for the stub. A live Plaid port can move this map to a database
 * and keep the same key.
 */
class TransactionLedger(private val store: LocalStore) {
    fun ingest(incoming: List<ProviderTransaction>): IngestReport {
        val current = load().toMutableMap()
        var inserted = 0
        var reconciled = 0
        var unchanged = 0
        for (raw in incoming) {
            val tx = sanitize(raw) ?: continue
            val prior = current[tx.providerTransactionId]
            when {
                prior == null -> {
                    current[tx.providerTransactionId] = tx
                    inserted += 1
                }
                prior == tx -> unchanged += 1
                else -> {
                    current[tx.providerTransactionId] = tx
                    reconciled += 1
                }
            }
        }
        save(current)
        return IngestReport(inserted = inserted, reconciled = reconciled, unchanged = unchanged)
    }

    fun count(): Int = load().size

    fun find(providerTransactionId: String): ProviderTransaction? = load()[providerTransactionId]

    fun clear() {
        store.remove(KEY)
    }

    fun drop(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val current = load().toMutableMap()
        var changed = false
        for (id in ids) {
            if (current.remove(id) != null) changed = true
        }
        if (changed) save(current)
    }

    private fun load(): Map<String, ProviderTransaction> {
        val raw = store.read(KEY).orEmpty()
        if (raw.isEmpty()) return emptyMap()
        val rows = linkedMapOf<String, ProviderTransaction>()
        raw.split(RECORD).forEach { line ->
            decode(line)?.let { rows[it.providerTransactionId] = it }
        }
        return rows
    }

    private fun save(rows: Map<String, ProviderTransaction>) {
        if (rows.isEmpty()) {
            store.remove(KEY)
            return
        }
        store.write(KEY, rows.values.joinToString(RECORD) { encode(it) })
    }

    private fun sanitize(tx: ProviderTransaction): ProviderTransaction? {
        val id = tx.providerTransactionId
        if (id.isBlank()) return null
        if (id.startsWith("link_")) return null
        if (id.indexOf(RECORD) >= 0 || id.indexOf(FIELD) >= 0) return null
        if (tx.accountId.indexOf(RECORD) >= 0 || tx.accountId.indexOf(FIELD) >= 0) return null
        if (tx.currency.length != 3) return null
        val name = SafeLog.redact(tx.name).replace(RECORD, " ").replace(FIELD, " ")
        return tx.copy(name = name)
    }

    private fun encode(tx: ProviderTransaction): String = listOf(
        tx.providerTransactionId,
        tx.accountId,
        tx.amountMinor.toString(),
        tx.currency,
        tx.postedAtEpochMs.toString(),
        tx.name,
    ).joinToString(FIELD)

    private fun decode(line: String): ProviderTransaction? {
        val bits = line.split(FIELD)
        if (bits.size != 6) return null
        val amount = bits[2].toLongOrNull() ?: return null
        val posted = bits[4].toLongOrNull() ?: return null
        if (bits[0].isBlank() || bits[0].startsWith("link_")) return null
        return ProviderTransaction(
            providerTransactionId = bits[0],
            accountId = bits[1],
            amountMinor = amount,
            currency = bits[3],
            postedAtEpochMs = posted,
            name = bits[5],
        )
    }

    private companion object {
        const val KEY = "synced_transactions"
        const val RECORD = "\u001e"
        const val FIELD = "\u001f"
    }
}

internal object AccountSnapshot {
    private const val KEY = "synced_accounts"

    fun read(store: LocalStore): List<SyncedAccount> {
        val raw = store.read(KEY).orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.split(RECORD).mapNotNull { decode(it) }
    }

    fun write(store: LocalStore, accounts: List<SyncedAccount>) {
        if (accounts.isEmpty()) {
            store.remove(KEY)
            return
        }
        store.write(KEY, accounts.joinToString(RECORD) { encode(it) })
    }

    fun clear(store: LocalStore) {
        store.remove(KEY)
    }

    private fun encode(account: SyncedAccount): String = listOf(
        clean(account.id),
        clean(account.name),
        clean(account.maskLine),
        account.balanceMinor.toString(),
        clean(account.currency),
        clean(account.group),
        clean(account.initials),
        clean(account.colorHex),
    ).joinToString(FIELD)

    private fun decode(line: String): SyncedAccount? {
        val bits = line.split(FIELD)
        if (bits.size != 8 || bits[0].isEmpty()) return null
        val minor = bits[3].toLongOrNull() ?: return null
        return SyncedAccount(
            id = bits[0],
            name = bits[1],
            maskLine = bits[2],
            balanceMinor = minor,
            currency = bits[4],
            group = bits[5],
            initials = bits[6],
            colorHex = bits[7],
        )
    }

    private fun clean(value: String): String = value.replace(RECORD, " ").replace(FIELD, " ")

    private const val RECORD = "\u001e"
    private const val FIELD = "\u001f"
}

internal data class SyncLine(
    val freshnessLabel: String,
    val syncCode: String,
    val syncActionLabel: String,
    val syncStale: Boolean,
)

internal object Freshness {
    fun label(nowMs: Long, status: SyncStatus, lastSyncedAt: Long?): String = when (status) {
        SyncStatus.Idle -> "Not synced yet"
        SyncStatus.Syncing -> "Updating…"
        is SyncStatus.Success -> "Updated ${relative(nowMs, status.lastSyncedAt)}"
        is SyncStatus.Failed -> stale(status.reason.ifBlank { "Couldn't refresh" }, lastSyncedAt, nowMs)
        SyncStatus.NeedsReauth -> stale("Reconnect to refresh balances", lastSyncedAt, nowMs)
    }

    private fun stale(lead: String, lastSyncedAt: Long?, nowMs: Long): String {
        val head = if (lead.endsWith(".")) lead else "$lead."
        return if (lastSyncedAt == null) {
            "$head Balances aren't current."
        } else {
            "$head Last update ${relative(nowMs, lastSyncedAt)}."
        }
    }

    fun relative(nowMs: Long, thenMs: Long): String {
        val minutes = (nowMs - thenMs).coerceAtLeast(0L) / 60_000L
        return when {
            minutes < 1L -> "just now"
            minutes < 60L -> "${minutes}m ago"
            minutes < 60L * 24L -> "${minutes / 60L}h ago"
            else -> "${minutes / (60L * 24L)}d ago"
        }
    }
}

/**
 * October 2026 sample balances and a fixed transaction set.
 * Provider ids are stable so a second refresh reconciles instead of duplicating.
 */
class MayaStubBankSource : BankLinkSource {
    override val id: String = ID
    override val readOnly: Boolean = true

    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        deliver(BankFetch.Ok(accounts = previewAccounts(), transactions = transactions))
    }

    companion object {
        const val ID: String = "maya-stub"

        fun previewAccounts(): List<SyncedAccount> = MayaStub.accounts.map { it.toSyncedAccount() }

        val transactions: List<ProviderTransaction> = listOf(
            ProviderTransaction(
                providerTransactionId = "maya-tx-gympass-202610",
                accountId = "amex",
                amountMinor = -4_700,
                currency = "USD",
                postedAtEpochMs = 1_759_276_800_000L,
                name = "Gympass",
            ),
            ProviderTransaction(
                providerTransactionId = "maya-tx-rent-202610",
                accountId = "chase-checking",
                amountMinor = -185_000,
                currency = "USD",
                postedAtEpochMs = 1_759_104_000_000L,
                name = "Rent",
            ),
            ProviderTransaction(
                providerTransactionId = "maya-tx-spotify-202610",
                accountId = "amex",
                amountMinor = -1_199,
                currency = "USD",
                postedAtEpochMs = 1_759_190_400_000L,
                name = "Spotify",
            ),
        )
    }
}

internal fun LinkedAccount.toSyncedAccount(): SyncedAccount = SyncedAccount(
    id = id,
    name = name,
    maskLine = maskLine,
    balanceMinor = balance.minor,
    currency = balance.currency,
    group = group.name,
    initials = initials,
    colorHex = colorHex,
)
