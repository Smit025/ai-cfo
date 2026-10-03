package com.aicfo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aicfo.app.plaid.PlaidLinker
import com.aicfo.app.theme.AiCfoTheme
import com.aicfo.app.ui.AiCfoRoot
import com.aicfo.app.ui.AiCfoViewModel
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

class MainActivity : FragmentActivity() {
    private var linker: PlaidLinker? = null

    private val plaidLink: ActivityResultLauncher<PlaidHandler> =
        registerForActivityResult(FastOpenPlaidLink()) { result ->
            val active = linker ?: return@registerForActivityResult
            when (result) {
                is LinkSuccess -> active.onPublicToken(result.publicToken)
                is LinkExit -> active.onExit(
                    result.error == null,
                    result.error?.displayMessage,
                    result.error?.errorMessage,
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            AiCfoTheme {
                val vm: AiCfoViewModel = viewModel()
                LaunchedEffect(vm) {
                    linker = vm.linker
                    vm.linker.opener = { linkToken ->
                        val configuration = LinkTokenConfiguration.Builder()
                            .token(linkToken)
                            .build()
                        plaidLink.launch(Plaid.create(application, configuration))
                    }
                }
                AiCfoRoot(vm)
            }
        }
    }
}
