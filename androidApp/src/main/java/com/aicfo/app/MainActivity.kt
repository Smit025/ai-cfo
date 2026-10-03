package com.aicfo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.aicfo.app.plaid.PlaidLinker
import com.aicfo.app.plaid.openPlaidLink
import com.aicfo.app.theme.AiCfoTheme
import com.aicfo.app.ui.AiCfoRoot
import com.aicfo.app.ui.AiCfoViewModel
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.PlaidHandler
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

class MainActivity : FragmentActivity() {
    private var plaidHost: PlaidLinker? = null

    private val openPlaid: ActivityResultLauncher<PlaidHandler> =
        registerForActivityResult(FastOpenPlaidLink()) { result ->
            val host = plaidHost ?: return@registerForActivityResult
            when (result) {
                is LinkSuccess -> host.onPublicToken(result.publicToken)
                is LinkExit -> host.onExit(
                    cancelled = result.error == null,
                    displayMessage = result.error?.displayMessage,
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[AiCfoViewModel::class.java]
        plaidHost = vm.plaid
        vm.plaid.opener = { token ->
            openPlaidLink(application, token) { handler -> openPlaid.launch(handler) }
        }
        setContent {
            AiCfoTheme {
                AiCfoRoot(vm)
            }
        }
    }
}
