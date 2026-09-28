package com.aicfo.app.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aicfo.app.i18n.AndroidLocalStrings
import com.aicfo.app.security.AndroidKeystoreTokenVault
import com.aicfo.app.security.AndroidLocalStore
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.market.Markets
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.SystemAppClock
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.domain.AppObserver

class AiCfoViewModel(app: Application) : AndroidViewModel(app) {
    val controller: AiCfoController = AiCfoController(
        vault = AndroidKeystoreTokenVault(app),
        store = AndroidLocalStore(app),
        clock = SystemAppClock(),
        market = Markets.unitedStates(),
        localStrings = AndroidLocalStrings(app),
    )
}

@Composable
fun AiCfoRoot(vm: AiCfoViewModel = viewModel()) {
    val controller = vm.controller
    val tick = rememberRevision(controller)
    val activity = LocalContext.current.findActivity() as FragmentActivity
    LaunchedEffect(activity) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        val can = BiometricManager.from(activity).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
        controller.setBiometricHardware(can)
    }
    val gate = remember(tick) { controller.gate() }
    Box(Modifier.fillMaxSize().background(AiColors.Bg)) {
        when (gate) {
            Gate.ONBOARDING -> OnboardingScreen(controller, tick)
            Gate.LOCK -> LockScreen(controller, tick) { launchBiometric(activity, controller) }
            Gate.PAYWALL -> PaywallScreen(controller, tick)
            else -> MainShell(controller, tick)
        }
    }
}

@Composable
private fun MainShell(controller: AiCfoController, tick: Int) {
    val tab = controller.tab()
    var showDetail by remember { mutableStateOf(false) }
    val splitMode = rememberSplitMode()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp || splitMode.separating
        val hinge = hingeDp(splitMode)
        LaunchedEffect(wide, showDetail) {
            if (wide && showDetail) {
                showDetail = false
                controller.selectTab("MOVES")
            }
        }
        BackHandler(enabled = showDetail && !wide) { showDetail = false }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxSize()) {
                when {
                    wide && tab == "MOVES" -> TwoPane(
                        hinge = hinge,
                        master = {
                            MovesScreen(
                                controller = controller,
                                tick = tick,
                                selectedId = controller.resolvedMoveId(),
                                onOpen = { controller.selectMove(it) },
                            )
                        },
                        detail = {
                            ActionDetailScreen(
                                controller = controller,
                                tick = tick,
                                moveId = controller.resolvedMoveId(),
                                onBack = {},
                                showBack = false,
                            )
                        },
                    )
                    showDetail && !wide -> ActionDetailScreen(
                        controller = controller,
                        tick = tick,
                        moveId = controller.selectedMoveId() ?: controller.resolvedMoveId(),
                        onBack = { showDetail = false },
                        showBack = true,
                    )
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        val widthMod = if (wide && tab != "MOVES") {
                            Modifier.fillMaxSize().padding(horizontal = 24.dp)
                        } else {
                            Modifier.fillMaxSize()
                        }
                        Box(widthMod) {
                            when (tab) {
                                "HOME" -> HomeScreen(controller, tick, wide) { id ->
                                    controller.selectMove(id)
                                    if (wide) controller.selectTab("MOVES") else showDetail = true
                                }
                                "MOVES" -> MovesScreen(controller, tick, selectedId = null) { id ->
                                    controller.selectMove(id)
                                    showDetail = true
                                }
                                "ACCOUNTS" -> AccountsScreen(controller, tick)
                                else -> SettingsScreen(controller, tick)
                            }
                        }
                    }
                }
            }
        }
        if (!(showDetail && !wide)) {
            PillNav(
                selected = tab,
                onSelect = {
                    showDetail = false
                    controller.selectTab(it)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            )
        }
    }
}

private fun launchBiometric(activity: FragmentActivity, controller: AiCfoController) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                controller.unlockFromBiometric(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                controller.unlockFromBiometric(false)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock AI CFO")
        .setSubtitle("Confirm it's you")
        .setNegativeButtonText("Cancel")
        .build()
    prompt.authenticate(info)
}

@Composable
private fun rememberRevision(controller: AiCfoController): Int {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(controller) {
        val observer = object : AppObserver {
            override fun onChanged() {
                tick += 1
            }
        }
        controller.addObserver(observer)
        onDispose { controller.removeObserver(observer) }
    }
    return tick
}
