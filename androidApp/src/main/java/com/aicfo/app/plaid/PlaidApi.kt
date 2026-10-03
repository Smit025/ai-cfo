package com.aicfo.app.plaid

import com.aicfo.shared.sync.ProviderTransaction
import com.aicfo.shared.sync.SyncedAccount

internal sealed class PlaidOutcome<out T> {
    data class Ok<T>(val value: T) : PlaidOutcome<T>()
    data object LoginRequired : PlaidOutcome<Nothing>()
    data class Failed(val message: String) : PlaidOutcome<Nothing>()
}

internal data class PlaidSnapshot(
    val accounts: List<SyncedAccount>,
    val transactions: List<ProviderTransaction>,
    val removedIds: List<String>,
    val nextCursor: String,
)

/**
 * Read-only Plaid HTTP. Link-token create, public-token exchange, balances, and transactions/sync.
 * No transfer, payment, or card-charge call.
 */
internal interface PlaidApi {
    val configured: Boolean

    fun createLinkToken(
        clientUserId: String,
        packageName: String,
        accessTokenForUpdate: String?,
    ): PlaidOutcome<String>

    fun exchangePublicToken(publicToken: String): PlaidOutcome<String>

    fun sync(accessToken: String, cursor: String?): PlaidOutcome<PlaidSnapshot>
}

internal fun interface PlaidTransport {
    fun post(url: String, body: String): PlaidHttpResult
}

internal data class PlaidHttpResult(val status: Int, val body: String)

internal fun plaidHost(env: String): String = when (env.trim().lowercase()) {
    "sandbox" -> "https://sandbox.plaid.com"
    "development" -> "https://development.plaid.com"
    "production" -> "https://production.plaid.com"
    else -> ""
}
