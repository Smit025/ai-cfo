package com.aicfo.app.plaid

import com.aicfo.shared.security.SafeLog
import com.aicfo.shared.sync.ProviderTransaction
import com.aicfo.shared.sync.SyncedAccount
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal object PlaidRequests {
    const val PACKAGE_NAME: String = "com.aicfo.app"

    fun linkToken(
        clientId: String,
        secret: String,
        clientUserId: String,
        packageName: String,
        accessTokenForUpdate: String?,
    ): JSONObject {
        val body = credentials(clientId, secret)
        body.put("client_name", "Finwise")
        body.put("language", "en")
        body.put("country_codes", JSONArray().put("US"))
        body.put("user", JSONObject().put("client_user_id", clientUserId))
        body.put("android_package_name", packageName)
        if (accessTokenForUpdate.isNullOrBlank()) {
            body.put("products", JSONArray().put("transactions"))
            body.put(
                "account_filters",
                JSONObject()
                    .put(
                        "depository",
                        JSONObject().put(
                            "account_subtypes",
                            JSONArray().put("checking").put("savings"),
                        ),
                    )
                    .put(
                        "credit",
                        JSONObject().put("account_subtypes", JSONArray().put("credit card")),
                    ),
            )
        } else {
            body.put("access_token", accessTokenForUpdate)
        }
        return body
    }

    fun exchange(clientId: String, secret: String, publicToken: String): JSONObject =
        credentials(clientId, secret).put("public_token", publicToken)

    fun balance(clientId: String, secret: String, accessToken: String): JSONObject =
        credentials(clientId, secret).put("access_token", accessToken)

    fun transactionsSync(
        clientId: String,
        secret: String,
        accessToken: String,
        cursor: String?,
    ): JSONObject {
        val body = credentials(clientId, secret)
            .put("access_token", accessToken)
            .put("count", 100)
        if (!cursor.isNullOrBlank()) body.put("cursor", cursor)
        return body
    }

    private fun credentials(clientId: String, secret: String): JSONObject =
        JSONObject().put("client_id", clientId).put("secret", secret)
}

internal object PlaidResponses {
    fun failure(status: Int, body: String): PlaidOutcome<Nothing>? {
        val json = runCatching { JSONObject(body) }.getOrNull()
        if (json == null) {
            return if (status in 200..299) null else PlaidOutcome.Failed("Couldn't refresh")
        }
        val code = errorCode(json)
        if (code.isBlank()) {
            return if (status in 200..299) null else PlaidOutcome.Failed("Couldn't refresh")
        }
        if (code == "ITEM_LOGIN_REQUIRED") return PlaidOutcome.LoginRequired
        val itemError = json.optJSONObject("item")?.optJSONObject("error") != null
        if (status in 200..299 && !itemError && !json.has("error_type")) return null
        val display = json.optString("display_message").ifBlank {
            when (code) {
                "PRODUCT_NOT_READY" -> "Transactions aren't ready yet."
                else -> "Couldn't refresh"
            }
        }
        return PlaidOutcome.Failed(SafeLog.redact(display).take(160).ifBlank { "Couldn't refresh" })
    }

    fun accounts(json: JSONObject): List<SyncedAccount> {
        val rows = json.optJSONArray("accounts") ?: return emptyList()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                mapAccount(row)?.let { add(it) }
            }
        }
    }

    fun transactions(rows: JSONArray?): List<ProviderTransaction> {
        if (rows == null) return emptyList()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                mapTransaction(row)?.let { add(it) }
            }
        }
    }

    fun removedIds(rows: JSONArray?): List<String> {
        if (rows == null) return emptyList()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = row.optString("transaction_id")
                if (id.isNotBlank()) add(id)
            }
        }
    }

    private fun mapAccount(row: JSONObject): SyncedAccount? {
        val id = row.optString("account_id")
        if (id.isBlank()) return null
        val type = row.optString("type")
        val subtype = row.optString("subtype")
        val name = row.optString("name").ifBlank { row.optString("official_name") }.ifBlank { "Account" }
        val mask = row.optString("mask")
        val typeLabel = typeLabel(subtype, type)
        val maskLine = if (mask.isBlank()) typeLabel else "··$mask · $typeLabel"
        val balances = row.optJSONObject("balances")
        val currency = balances?.optString("iso_currency_code").orEmpty()
            .ifBlank { balances?.optString("unofficial_currency_code").orEmpty() }
            .ifBlank { "USD" }
        val rawBalance = balances?.opt("current") ?: balances?.opt("available")
        val minor = moneyToMinor(rawBalance, currency)
        return SyncedAccount(
            id = id,
            name = SafeLog.redact(name).take(80),
            maskLine = maskLine,
            balanceMinor = minor,
            currency = currency.take(3),
            group = groupFor(type),
            initials = initials(name),
            colorHex = colorFor(type),
        )
    }

    private fun mapTransaction(row: JSONObject): ProviderTransaction? {
        val id = row.optString("transaction_id")
        val accountId = row.optString("account_id")
        if (id.isBlank() || accountId.isBlank()) return null
        val currency = row.optString("iso_currency_code").ifBlank {
            row.optString("unofficial_currency_code")
        }.ifBlank { "USD" }
        val name = row.optString("merchant_name").ifBlank { row.optString("name") }.ifBlank { "Transaction" }
        val minor = -moneyToMinor(row.opt("amount"), currency)
        return ProviderTransaction(
            providerTransactionId = id,
            accountId = accountId,
            amountMinor = minor,
            currency = currency.take(3),
            postedAtEpochMs = plaidEpoch(row.optString("date"), row.optString("datetime")),
            name = name,
        )
    }

    private fun errorCode(json: JSONObject): String {
        val top = json.optString("error_code")
        if (top.isNotBlank()) return top
        return json.optJSONObject("item")?.optJSONObject("error")?.optString("error_code").orEmpty()
    }
}

internal fun moneyToMinor(raw: Any?, currency: String): Long {
    if (raw == null || raw == JSONObject.NULL) return 0L
    val scale = when (currency.uppercase()) {
        "JPY", "KRW", "VND" -> 0
        "BHD", "KWD", "OMR" -> 3
        else -> 2
    }
    val decimal = try {
        BigDecimal(raw.toString())
    } catch (_: NumberFormatException) {
        return 0L
    }
    return decimal.setScale(scale, RoundingMode.HALF_UP).movePointRight(scale).longValueExact()
}

internal fun plaidEpoch(date: String?, dateTime: String?): Long {
    val stamp = dateTime?.takeIf { it.isNotBlank() } ?: date?.takeIf { it.isNotBlank() } ?: return 0L
    return try {
        if (stamp.contains('T')) {
            Instant.parse(stamp).toEpochMilli()
        } else {
            LocalDate.parse(stamp.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    } catch (_: Exception) {
        0L
    }
}

internal fun typeLabel(subtype: String, type: String): String {
    val raw = subtype.ifBlank { type }.ifBlank { "Account" }
    return raw.split(' ').filter { it.isNotBlank() }.joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercaseChar() }
    }
}

internal fun groupFor(type: String): String = when (type.lowercase()) {
    "depository" -> "CASH"
    "credit", "loan" -> "CARDS_AND_LOANS"
    "investment" -> "INVESTMENTS"
    else -> type.uppercase().ifBlank { "OTHER" }
}

internal fun colorFor(type: String): String = when (type.lowercase()) {
    "depository" -> "#2563EB"
    "credit" -> "#DC2626"
    "loan" -> "#1E293B"
    "investment" -> "#059669"
    else -> "#635BFF"
}

internal fun initials(name: String): String {
    val letters = name.split(' ', '-', '·')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.first().uppercaseChar() }
    return when {
        letters.size >= 2 -> "${letters[0]}${letters[1]}"
        letters.size == 1 -> letters[0].toString()
        else -> "AC"
    }
}
