package com.aicfo.app.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

data class SplitMode(val separating: Boolean, val hingePx: Int)

@Composable
fun rememberSplitMode(): SplitMode {
    val activity = LocalContext.current.findActivity()
    val layout by WindowInfoTracker.getOrCreate(activity)
        .windowLayoutInfo(activity)
        .collectAsStateWithLifecycle(initialValue = WindowLayoutInfo(emptyList()))
    val fold = layout.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull {
        it.orientation == FoldingFeature.Orientation.VERTICAL && it.isSeparating
    }
    return SplitMode(
        separating = fold != null,
        hingePx = fold?.bounds?.width() ?: 0,
    )
}

@Composable
fun TwoPane(
    hinge: Dp,
    master: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { master() }
        if (hinge > 0.dp) {
            Spacer(Modifier.width(hinge))
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1.15f).fillMaxHeight()) { detail() }
    }
}

@Composable
fun hingeDp(mode: SplitMode): Dp {
    if (!mode.separating) return 0.dp
    return with(LocalDensity.current) { mode.hingePx.toDp() }
}

fun Context.findActivity(): ComponentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("Activity was not found")
}
