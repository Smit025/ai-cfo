package com.aicfo.app.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aicfo.app.BuildConfig
import com.aicfo.app.i18n.AndroidLocalStrings
import com.aicfo.app.plaid.PlaidBankSource
import com.aicfo.app.plaid.PlaidLinker
import com.aicfo.app.plaid.plaidApiFromBuildConfig
import com.aicfo.app.security.AndroidKeystoreSecureStore
import com.aicfo.app.security.AndroidKeystoreTokenVault
import com.aicfo.app.security.AndroidLocalStore
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.market.Markets
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.SystemAppClock
import com.aicfo.shared.presentation.Gate
import com.aicfo.shared.domain.AppObserver
import com.aicfo.shared.sync.SyncTrigger
import java.util.concurrent.Executors

class AiCfoViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = AndroidKeystoreTokenVault(app)
    private val local = AndroidLocalStore(app)
    private val plaidApi = plaidApiFromBuildConfig()
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "plaid-sync").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())
    private val postMain: (() -> Unit) -> Unit = { block -> main.post(block) }
    private val source = PlaidBankSource(vault, local, plaidApi, io, postMain)
    val controller: AiCfoController = AiCfoController(
        vault = vault,
        store = local,
        clock = SystemAppClock(),
        market = Markets.unitedStates(),
        localStrings = AndroidLocalStrings(app),
        debugBuild = BuildConfig.DEBUG,
        secure = AndroidKeystoreSecureStore(app),
        banks = source,
    )
    internal val plaid = PlaidLinker(vault, local, plaidApi, source, controller, io, postMain)
    private var coldStartSent = false

    init {
        controller.setBankLinkAvailability(
            configured = plaidApi.configured,
            unavailableLabel = "Plaid is not configured",
            note = "",
        )
    }

    override fun onCleared() {
        io.shutdown()
        super.onCleared()
    }

    fun onAppVisible() {
        if (!coldStartSent) {
            coldStartSent = true
            controller.refreshAccounts(SyncTrigger.ColdStart)
        } else {
            controller.refreshAccounts(SyncTrigger.Foreground)
        }
    }
}

@Composable
fun AiCfoRoot(vm: AiCfoViewModel = viewModel()) {
    val controller = vm.controller
    val tick = rememberRevision(controller)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) vm.onAppVisible()
        }
        // addObserver replays ON_START when the activity is already started.
        // A second onAppVisible here would send Foreground on the same open.
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val activity = LocalContext.current.findActivity() as FragmentActivity
    LaunchedEffect(activity) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        val can = BiometricManager.from(activity).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
        controller.setBiometricHardware(can)
    }
    val gate = remember(tick) { controller.gate() }
    // Ancestor flag: descendant testTags are published as resource-ids for UiAutomator.
    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(AiColors.Bg),
    ) {
        when (gate) {
            Gate.ONBOARDING -> OnboardingScreen(
                controller = controller,
                tick = tick,
                linking = vm.plaid.busy,
                onConnect = { vm.plaid.connect(advanceIntro = true) },
            )
            Gate.AUTH -> AuthFlowScreen(controller, tick)
            Gate.LOCK -> LockScreen(controller, tick) {
                promptBiometric(activity) { ok -> controller.unlockFromBiometric(ok) }
            }
            Gate.PAYWALL -> PaywallScreen(controller, tick)
            else -> MainShell(
                controller = controller,
                tick = tick,
                linking = vm.plaid.busy,
                onConnect = { vm.plaid.connect(advanceIntro = false) },
                onSyncAction = { vm.plaid.onSyncAction(it) },
            )
        }
    }
}

@Composable
private fun MainShell(
    controller: AiCfoController,
    tick: Int,
    linking: Boolean,
    onConnect: () -> Unit,
    onSyncAction: (String) -> Unit,
) {
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
                                "HOME" -> HomeScreen(
                                    controller,
                                    tick,
                                    wide,
                                    onOpen = { id ->
                                        controller.selectMove(id)
                                        if (wide) controller.selectTab("MOVES") else showDetail = true
                                    },
                                    onSyncAction = onSyncAction,
                                )
                                "MOVES" -> MovesScreen(controller, tick, selectedId = null) { id ->
                                    controller.selectMove(id)
                                    showDetail = true
                                }
                                "ACCOUNTS" -> AccountsScreen(
                                    controller,
                                    tick,
                                    linking = linking,
                                    onConnect = onConnect,
                                    onSyncAction = onSyncAction,
                                )
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

internal fun promptBiometric(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Finwise")
        .setSubtitle("This confirms the device. It does not sign you in.")
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
