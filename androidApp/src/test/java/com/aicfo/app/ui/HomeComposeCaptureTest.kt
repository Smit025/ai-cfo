package com.aicfo.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aicfo.app.theme.AiCfoTheme
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.domain.AppClock
import com.aicfo.shared.domain.AppObserver
import com.aicfo.shared.market.EmptyLocalStrings
import com.aicfo.shared.market.Markets
import com.aicfo.shared.security.MemoryLocalStore
import com.aicfo.shared.security.MemorySecureStore
import com.aicfo.shared.security.MemoryTokenVault
import com.aicfo.shared.sync.AccountRole
import com.aicfo.shared.sync.BankFetch
import com.aicfo.shared.sync.BankLinkId
import com.aicfo.shared.sync.BankLinkSource
import com.aicfo.shared.sync.ProviderTransaction
import com.aicfo.shared.sync.SyncTrigger
import com.aicfo.shared.sync.SyncedAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

/**
 * Labeled Compose fallback for the three Home boards.
 * Runs only when HOME_CAPTURE_DIR is set, so the normal unit-test job does not write files.
 * Not a device screenshot.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w360dp-h800dp-xxhdpi")
class HomeComposeCaptureTest {
    @Test
    fun captureThreeHomeStatesWhenRequested() {
        val dirPath = System.getenv("HOME_CAPTURE_DIR") ?: return
        val dir = File(dirPath)
        dir.mkdirs()

        val empty = plainController()
        assertEquals("—", empty.home().savingsAmount)
        assertEquals("—", empty.home().netWorthAmount)
        assertEquals("gympass", empty.home().moveAt(0).id)
        assertFalse(empty.home().freshnessLabel.contains("Updated"))
        draw(dir, "home-before-plaid-sync.png") { HomeChrome(empty) }

        val now = 1_760_544_000_000L
        val source = CapturePlaidSource(sampleFetch(now))
        val synced = plaidController(now, source)
        assertTrue(synced.completeExternalReadOnlyLink(false))
        assertEquals("\$1,000", synced.home().savingsAmount)
        assertEquals("\$1,300", synced.home().netWorthAmount)
        assertEquals("\$25", synced.home().snapshotAt(0).amount)
        assertEquals("\$18", synced.home().snapshotAt(1).amount)
        assertEquals("\$457", synced.home().snapshotAt(2).amount)
        assertEquals("Updated just now", synced.home().freshnessLabel)
        draw(dir, "home-after-sync.png") { HomeChrome(synced) }

        source.next = BankFetch.Unavailable("Couldn't refresh")
        synced.refreshAccounts(SyncTrigger.Foreground)
        assertEquals("\$1,000", synced.home().savingsAmount)
        assertEquals("\$1,300", synced.home().netWorthAmount)
        assertTrue(synced.home().syncStale)
        assertEquals("Couldn't refresh. Last update just now.", synced.home().freshnessLabel)
        assertEquals("Try again", synced.home().syncActionLabel)
        draw(dir, "home-stale.png") { HomeChrome(synced) }
    }

    private fun draw(dir: File, name: String, content: @Composable () -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent { AiCfoTheme { content() } }
        ShadowLooper.idleMainLooper()
        val view = activity.window.decorView
        val width = 1080
        val height = 2400
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        ShadowLooper.idleMainLooper()
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        FileOutputStream(File(dir, name)).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        }
        bitmap.recycle()
        controller.pause().stop().destroy()
    }
}

private fun plainController(): AiCfoController = AiCfoController(
    MemoryTokenVault(),
    MemoryLocalStore(),
    object : AppClock {
        override fun nowEpochMs(): Long = 1_760_544_000_000L
    },
    Markets.unitedStates(),
    EmptyLocalStrings,
    true,
    MemorySecureStore(),
)

private fun plaidController(now: Long, source: BankLinkSource): AiCfoController = AiCfoController(
    MemoryTokenVault(),
    MemoryLocalStore(),
    object : AppClock {
        override fun nowEpochMs(): Long = now
    },
    Markets.unitedStates(),
    EmptyLocalStrings,
    true,
    MemorySecureStore(),
    source,
)

private fun sampleFetch(now: Long) = BankFetch.Ok(
    accounts = listOf(
        account("sav", "Plaid Savings", 100_000, AccountRole.SAVINGS, "CASH"),
        account("chk", "Plaid Checking", 50_000, AccountRole.CASH, "CASH"),
        account("card", "Plaid Credit Card", 20_000, AccountRole.CREDIT, "CARDS_AND_LOANS"),
    ),
    transactions = listOf(
        tx("rent", -2_500, "Rent", "RENT_AND_UTILITIES", now),
        tx("coffee", -1_800, "Coffee", "FOOD_AND_DRINK", now),
        tx("pay", 50_000, "Payroll", "INCOME", now),
    ),
)

private fun account(id: String, name: String, minor: Long, role: String, group: String) = SyncedAccount(
    id = id,
    name = name,
    maskLine = "··0000",
    balanceMinor = minor,
    currency = "USD",
    group = group,
    initials = "PL",
    colorHex = "#2563EB",
    role = role,
)

private fun tx(id: String, amount: Long, name: String, category: String, postedAt: Long) = ProviderTransaction(
    providerTransactionId = id,
    accountId = "chk",
    amountMinor = amount,
    currency = "USD",
    postedAtEpochMs = postedAt,
    name = name,
    category = category,
)

private class CapturePlaidSource(var next: BankFetch) : BankLinkSource {
    override val id: String = BankLinkId.PLAID
    override val readOnly: Boolean = true
    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        deliver(next)
    }
}

@Composable
private fun HomeChrome(controller: AiCfoController) {
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
    Box(Modifier.fillMaxSize().background(AiColors.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxSize()) {
                HomeScreen(controller, tick, wide = false, onOpen = {})
            }
        }
        PillNav(
            selected = "HOME",
            onSelect = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }
}
