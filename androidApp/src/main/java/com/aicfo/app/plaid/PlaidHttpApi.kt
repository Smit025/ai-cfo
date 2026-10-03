package com.aicfo.app.plaid

import com.aicfo.app.BuildConfig
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal class PlaidHttpApi(
    private val host: String,
    private val clientId: String,
    private val secret: String,
    private val transport: PlaidTransport,
) : PlaidApi {
    override val configured: Boolean =
        host == PLAID_SANDBOX_HOST && clientId.isNotBlank() && secret.isNotBlank()

    override fun createLinkToken(
        clientUserId: String,
        packageName: String,
        accessTokenForUpdate: String?,
    ): PlaidOutcome<String> {
        if (!configured) return PlaidOutcome.Failed("Plaid is not configured")
        val body = PlaidRequests.linkToken(clientId, secret, clientUserId, packageName, accessTokenForUpdate)
        return when (val response = postJson("/link/token/create", body)) {
            is PlaidOutcome.Ok -> {
                val token = response.value.optString("link_token")
                if (token.isBlank()) PlaidOutcome.Failed("Couldn't open your bank.") else PlaidOutcome.Ok(token)
            }
            PlaidOutcome.LoginRequired -> PlaidOutcome.Failed("Couldn't open your bank.")
            is PlaidOutcome.Failed ->
                if (response.message == "Couldn't refresh") PlaidOutcome.Failed("Couldn't open your bank.")
                else response
        }
    }

    override fun exchangePublicToken(publicToken: String): PlaidOutcome<String> {
        if (!configured) return PlaidOutcome.Failed("Plaid is not configured")
        if (publicToken.isBlank()) return PlaidOutcome.Failed("Couldn't link these accounts. Nothing was saved.")
        val body = PlaidRequests.exchange(clientId, secret, publicToken)
        return when (val response = postJson("/item/public_token/exchange", body)) {
            is PlaidOutcome.Ok -> {
                val access = response.value.optString("access_token")
                if (access.isBlank()) {
                    PlaidOutcome.Failed("Couldn't link these accounts. Nothing was saved.")
                } else {
                    PlaidOutcome.Ok(access)
                }
            }
            PlaidOutcome.LoginRequired -> PlaidOutcome.Failed("Couldn't link these accounts. Nothing was saved.")
            is PlaidOutcome.Failed -> response
        }
    }

    override fun sync(accessToken: String, cursor: String?): PlaidOutcome<PlaidSnapshot> {
        if (!configured) return PlaidOutcome.Failed("Plaid is not configured")
        if (accessToken.isBlank()) return PlaidOutcome.Failed("Couldn't refresh")
        val balances = postJson("/accounts/balance/get", PlaidRequests.balance(clientId, secret, accessToken))
        val accountsJson = when (balances) {
            is PlaidOutcome.Ok -> balances.value
            PlaidOutcome.LoginRequired -> return PlaidOutcome.LoginRequired
            is PlaidOutcome.Failed -> return balances
        }
        val accounts = PlaidResponses.accounts(accountsJson)
        val transactions = mutableListOf<com.aicfo.shared.sync.ProviderTransaction>()
        val removed = mutableListOf<String>()
        var pageCursor = cursor?.takeIf { it.isNotBlank() }
        var nextCursor = pageCursor.orEmpty()
        var pages = 0
        while (pages < MAX_PAGES) {
            val page = postJson(
                "/transactions/sync",
                PlaidRequests.transactionsSync(clientId, secret, accessToken, pageCursor),
            )
            val json = when (page) {
                is PlaidOutcome.Ok -> page.value
                PlaidOutcome.LoginRequired -> return PlaidOutcome.LoginRequired
                is PlaidOutcome.Failed -> return page
            }
            transactions += PlaidResponses.transactions(json.optJSONArray("added"))
            transactions += PlaidResponses.transactions(json.optJSONArray("modified"))
            removed += PlaidResponses.removedIds(json.optJSONArray("removed"))
            nextCursor = json.optString("next_cursor")
            pages += 1
            if (!json.optBoolean("has_more")) break
            if (nextCursor.isBlank()) break
            pageCursor = nextCursor
        }
        val incoming = transactions.map { it.providerTransactionId }.toSet()
        return PlaidOutcome.Ok(
            PlaidSnapshot(
                accounts = accounts,
                transactions = transactions,
                removedIds = removed.filter { it !in incoming },
                nextCursor = nextCursor,
            ),
        )
    }

    override fun removeItem(accessToken: String): PlaidOutcome<Unit> {
        if (!configured) return PlaidOutcome.Failed("Plaid is not configured")
        if (accessToken.isBlank()) return PlaidOutcome.Failed("Couldn't disconnect")
        when (val response = postJson("/item/remove", PlaidRequests.removeItem(clientId, secret, accessToken))) {
            is PlaidOutcome.Ok -> return PlaidOutcome.Ok(Unit)
            PlaidOutcome.LoginRequired -> return PlaidOutcome.LoginRequired
            is PlaidOutcome.Failed -> return PlaidOutcome.Failed(response.message)
        }
    }

    private fun postJson(path: String, body: JSONObject): PlaidOutcome<JSONObject> {
        if (!host.startsWith("https://")) return PlaidOutcome.Failed("Couldn't reach Plaid.")
        val result = try {
            transport.post(host + path, body.toString())
        } catch (_: Exception) {
            return PlaidOutcome.Failed("Couldn't reach Plaid.")
        }
        val failure = PlaidResponses.failure(result.status, result.body)
        if (result.status !in 200..299) return failure ?: PlaidOutcome.Failed("Couldn't refresh")
        if (failure is PlaidOutcome.LoginRequired || failure is PlaidOutcome.Failed) return failure
        val json = runCatching { JSONObject(result.body) }.getOrNull()
            ?: return PlaidOutcome.Failed("Couldn't refresh")
        return PlaidOutcome.Ok(json)
    }

    private companion object {
        const val MAX_PAGES = 20
    }
}

internal object HttpsPlaidTransport : PlaidTransport {
    override fun post(url: String, body: String): PlaidHttpResult {
        if (!url.startsWith("https://")) error("Cleartext is not allowed")
        if (!url.startsWith(PLAID_SANDBOX_HOST)) error("Only Plaid sandbox is allowed")
        val connection = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Plaid-Version", "2020-09-14")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { reader ->
                val buffer = CharArray(2048)
                val out = StringBuilder()
                while (out.length < MAX_BYTES) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    val room = MAX_BYTES - out.length
                    out.append(buffer, 0, minOf(read, room))
                }
                out.toString()
            }.orEmpty()
            return PlaidHttpResult(status, text)
        } finally {
            connection.disconnect()
        }
    }

    private const val MAX_BYTES = 1_000_000
}

internal fun plaidApiFromBuildConfig(): PlaidApi {
    val clientId = BuildConfig.PLAID_CLIENT_ID.trim()
    val secret = BuildConfig.PLAID_SECRET.trim()
    return PlaidHttpApi(
        host = PLAID_SANDBOX_HOST,
        clientId = clientId,
        secret = secret,
        transport = HttpsPlaidTransport,
    )
}
