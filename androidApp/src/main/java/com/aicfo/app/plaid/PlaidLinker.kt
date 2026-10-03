package com.aicfo.app.plaid

import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.security.LocalStore
import com.aicfo.shared.security.SafeLog
import com.aicfo.shared.security.TokenVault
import com.aicfo.shared.sync.BankLinkId
import com.aicfo.shared.sync.SyncCode
import com.aicfo.shared.sync.SyncTrigger
import java.util.UUID
import java.util.concurrent.Executor

/**
 * Opens Plaid Link and exchanges the public token.
 * The public token stays in memory for the exchange call only.
 * The access token is written to the vault and nowhere else.
 */
internal class PlaidLinker(
    private val vault: TokenVault,
    private val store: LocalStore,
    private val api: PlaidApi,
    private val source: PlaidBankSource,
    private val controller: AiCfoController,
    private val io: Executor,
    private val main: (() -> Unit) -> Unit,
) {
    var opener: ((String) -> Unit)? = null

    fun connect(advanceIntro: Boolean) {
        if (!controller.preparePlaidLink()) return
        if (!api.configured) {
            controller.reportLinkError("Plaid is not configured")
            return
        }
        start(update = false, advanceIntro = advanceIntro)
    }

    fun onSyncAction(code: String) {
        if (!controller.preparePlaidLink()) return
        when (code) {
            SyncCode.NEEDS_REAUTH -> {
                if (controller.activeBankLinkId() == BankLinkId.PLAID && api.configured) {
                    start(update = true, advanceIntro = false)
                } else {
                    controller.reconnectBank()
                }
            }
            SyncCode.FAILED -> controller.refreshAccounts(SyncTrigger.Manual)
        }
    }

    fun onPublicToken(publicToken: String) {
        val intent = store.read(INTENT).orEmpty()
        store.remove(INTENT)
        if (intent == "update") {
            controller.reconnectBank()
            return
        }
        if (publicToken.isBlank()) {
            controller.reportLinkError("Couldn't link these accounts. Nothing was saved.")
            return
        }
        io.execute {
            val exchanged = api.exchangePublicToken(publicToken)
            main {
                when (exchanged) {
                    is PlaidOutcome.Ok -> saveAccessToken(exchanged.value, intent == "create-intro")
                    PlaidOutcome.LoginRequired ->
                        controller.reportLinkError("Couldn't link these accounts. Nothing was saved.")
                    is PlaidOutcome.Failed -> controller.reportLinkError(exchanged.message)
                }
            }
        }
    }

    fun onExit(cancelled: Boolean, displayMessage: String?, errorMessage: String? = null) {
        store.remove(INTENT)
        if (cancelled) return
        val message = listOf(displayMessage, errorMessage)
            .firstNotNullOfOrNull { raw -> visibleLinkMessage(raw) }
            ?: "Couldn't link these accounts. Nothing was saved."
        controller.reportLinkError(message)
    }

    private fun start(update: Boolean, advanceIntro: Boolean) {
        if (!api.configured) {
            controller.reportLinkError("Plaid is not configured")
            return
        }
        controller.clearLinkError()
        val userId = clientUserId()
        io.execute {
            val existing = if (update) readAccessToken() else null
            if (update && existing.isNullOrBlank()) {
                main { controller.reportLinkError("Reconnect to refresh balances.") }
                return@execute
            }
            val created = api.createLinkToken(
                clientUserId = userId,
                packageName = PlaidRequests.PACKAGE_NAME,
                accessTokenForUpdate = if (update) existing else null,
            )
            main {
                when (created) {
                    is PlaidOutcome.Ok -> present(created.value, update, advanceIntro)
                    PlaidOutcome.LoginRequired -> controller.reportLinkError("Couldn't open your bank.")
                    is PlaidOutcome.Failed -> controller.reportLinkError(created.message)
                }
            }
        }
    }

    private fun present(linkToken: String, update: Boolean, advanceIntro: Boolean) {
        store.write(
            INTENT,
            when {
                update -> "update"
                advanceIntro -> "create-intro"
                else -> "create"
            },
        )
        val open = opener
        if (open == null) {
            store.remove(INTENT)
            controller.reportLinkError("Couldn't open Plaid Link.")
            return
        }
        try {
            open(linkToken)
        } catch (_: Throwable) {
            store.remove(INTENT)
            controller.reportLinkError("Couldn't open Plaid Link.")
        }
    }

    private fun saveAccessToken(accessToken: String, advanceIntro: Boolean) {
        val stored = try {
            vault.put(PlaidBankSource.ACCESS_TOKEN_KEY, accessToken)
        } catch (_: Throwable) {
            false
        }
        if (!stored) {
            controller.reportLinkError("Couldn't link these accounts. Nothing was saved.")
            return
        }
        source.onRelinked()
        if (!controller.completeExternalReadOnlyLink(advanceIntro)) {
            // Unpaid rejection already cleared the vault, including this token.
            return
        }
    }

    private fun readAccessToken(): String? = try {
        vault.read(PlaidBankSource.ACCESS_TOKEN_KEY)
    } catch (_: Throwable) {
        null
    }

    private fun visibleLinkMessage(raw: String?): String? {
        val cleaned = raw?.let { SafeLog.redact(it).take(180).trim() }.orEmpty()
        if (cleaned.isEmpty() || cleaned.equals("null", ignoreCase = true)) return null
        return cleaned
    }

    private fun clientUserId(): String {
        val existing = store.read(USER)
        if (!existing.isNullOrBlank() && !existing.contains("access-") && !existing.contains("public-")) {
            return existing
        }
        val created = "fw-" + UUID.randomUUID().toString()
        store.write(USER, created)
        return created
    }

    private companion object {
        const val INTENT = "plaid_link_intent"
        const val USER = "plaid_client_user_id"
    }
}
