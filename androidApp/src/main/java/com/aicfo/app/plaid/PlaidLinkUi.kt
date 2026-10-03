package com.aicfo.app.plaid

import android.app.Application
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration

/** Plaid Link SDK entry. Kept out of shared and out of the fetch path. */
internal fun openPlaidLink(application: Application, linkToken: String, launch: (PlaidHandler) -> Unit) {
    val configuration = LinkTokenConfiguration.Builder()
        .token(linkToken)
        .build()
    launch(Plaid.create(application, configuration))
}
