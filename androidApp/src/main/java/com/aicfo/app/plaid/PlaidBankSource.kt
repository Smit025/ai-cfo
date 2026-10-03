package com.aicfo.app.plaid

import com.aicfo.shared.security.LocalStore
import com.aicfo.shared.security.TokenVault
import com.aicfo.shared.sync.BankFetch
import com.aicfo.shared.sync.BankLinkId
import com.aicfo.shared.sync.BankLinkSource
import java.util.concurrent.Executor

/**
 * Read-only Plaid fetch behind [BankLinkSource].
 * The access token is read from [vault] inside this class and is never logged or written to [store].
 * There is no transfer, payment, or card-charge method.
 */
internal class PlaidBankSource(
    private val vault: TokenVault,
    private val store: LocalStore,
    private val api: PlaidApi,
    private val io: Executor,
    private val post: (() -> Unit) -> Unit,
) : BankLinkSource {
    override val id: String = BankLinkId.PLAID
    override val readOnly: Boolean = true

    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        io.execute {
            val result = try {
                load()
            } catch (_: Throwable) {
                BankFetch.Unavailable("Couldn't refresh")
            }
            post { deliver(result) }
        }
    }

    override fun onDisconnected() {
        val token = try {
            vault.read(ACCESS_TOKEN_KEY)
        } catch (_: Throwable) {
            null
        }
        store.remove(CURSOR)
        if (token.isNullOrBlank() || !api.configured) return
        // The controller clears the vault as soon as this returns. The token string
        // is already captured, so the sandbox remove can finish after that clear.
        io.execute {
            try {
                api.removeItem(token)
            } catch (_: Throwable) {
                // Local unlink still stands. Do not log the token.
            }
        }
    }

    fun onRelinked() {
        store.remove(CURSOR)
    }

    private fun load(): BankFetch {
        if (!api.configured) return BankFetch.Unavailable("Plaid is not configured")
        val token = try {
            vault.read(ACCESS_TOKEN_KEY)
        } catch (_: Throwable) {
            null
        }
        if (token.isNullOrBlank()) return BankFetch.Unavailable("Couldn't refresh")
        return when (val synced = api.sync(token, store.read(CURSOR))) {
            is PlaidOutcome.Ok -> {
                val cursor = synced.value.nextCursor
                if (cursor.isBlank()) store.remove(CURSOR) else store.write(CURSOR, cursor)
                BankFetch.Ok(
                    accounts = synced.value.accounts,
                    transactions = synced.value.transactions,
                    removedTransactionIds = synced.value.removedIds,
                )
            }
            PlaidOutcome.LoginRequired -> BankFetch.LoginRequired
            is PlaidOutcome.Failed -> BankFetch.Unavailable(synced.message)
        }
    }

    companion object {
        const val ACCESS_TOKEN_KEY: String = "plaid.access_token"
        private const val CURSOR: String = "plaid_tx_cursor"
    }
}
