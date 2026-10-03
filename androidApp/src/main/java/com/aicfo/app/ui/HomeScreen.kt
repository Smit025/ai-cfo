package com.aicfo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.presentation.HomeAmountModel
import com.aicfo.shared.presentation.HomeModel
import com.aicfo.shared.presentation.HomeMoveModel
import com.aicfo.shared.presentation.Tone
import com.aicfo.shared.sync.SyncCode
import com.aicfo.shared.sync.SyncTrigger

private val Hairline = Color(0xFFE6E8EE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    controller: AiCfoController,
    tick: Int,
    wide: Boolean,
    onSync: (String) -> Unit = { performSyncAction(controller, it) },
    onOpen: (String) -> Unit,
) {
    val freshnessTick = rememberFreshnessTick()
    val home = remember(tick, freshnessTick) { controller.home() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        PullToRefreshBox(
            isRefreshing = home.syncCode == SyncCode.SYNCING,
            onRefresh = { controller.refreshAccounts(SyncTrigger.PullToRefresh) },
            modifier = Modifier
                .widthIn(max = if (wide) 480.dp else 900.dp)
                .fillMaxSize(),
        ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            HomeHeader(home)
            Spacer(Modifier.height(18.dp))
            WealthStrip(home) { onSync(home.syncCode) }
            if (home.hope.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HopeLine(home.hope)
            }
            Spacer(Modifier.height(12.dp))
            MonthSnapshot(home)
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    home.sectionTitle,
                    color = AiColors.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    home.seeAllLabel,
                    color = AiColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { controller.selectTab("MOVES") },
                )
            }
            Spacer(Modifier.height(14.dp))
            if (home.moveCount() == 0) {
                Column(Modifier.fillMaxWidth().softCard().padding(20.dp)) {
                    Text(home.emptyTitle, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(home.emptyBody, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (index in 0 until home.moveCount()) {
                        val move = home.moveAt(index)
                        HomeMoveCard(move) { onOpen(move.id) }
                    }
                }
            }
        }
        }
    }
}

private fun performSyncAction(controller: AiCfoController, code: String) {
    when (code) {
        SyncCode.NEEDS_REAUTH -> controller.reconnectBank()
        SyncCode.FAILED -> controller.refreshAccounts(SyncTrigger.Manual)
    }
}

@Composable
private fun HomeHeader(home: HomeModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(home.greeting, color = AiColors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(home.subtitle, color = AiColors.Muted, fontSize = 15.sp)
        }
        Box {
            Box(
                Modifier.size(44.dp).softCard(radius = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = AiColors.Text)
            }
            if (home.showNotificationDot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AiColors.Danger),
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(AiColors.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(home.initials, color = AiColors.Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun WealthStrip(home: HomeModel, onSync: () -> Unit) {
    Column(Modifier.fillMaxWidth().softCard(radius = 22.dp).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row {
            WealthColumn(home.savingsLabel, home.savingsAmount, home.savingsDelta, home.savingsUp, Modifier.weight(1f))
            WealthColumn(home.netWorthLabel, home.netWorthAmount, home.netWorthDelta, home.netWorthUp, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(AiColors.Accent))
            Spacer(Modifier.width(8.dp))
            Text(home.runway, color = AiColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (home.freshnessLabel.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FreshnessLine(home.freshnessLabel, home.syncActionLabel, home.syncCode, onSync)
        }
    }
}

@Composable
private fun WealthColumn(label: String, amount: String, delta: String, up: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = AiColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(amount, color = AiColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (delta.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DeltaPill(delta, up)
        }
    }
}

@Composable
private fun DeltaPill(text: String, up: Boolean) {
    val bg = if (up) AiColors.SuccessSoft else AiColors.Chip
    val fg = if (up) AiColors.Success else AiColors.Muted
    Text(
        text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun HopeLine(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AiColors.SuccessSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFD7F3E6)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Payments, contentDescription = null, tint = AiColors.Success, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = AiColors.Success, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MonthSnapshot(home: HomeModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .softCard(radius = 22.dp)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val count = home.snapshotCount()
        for (index in 0 until count) {
            if (index > 0) {
                Box(Modifier.width(1.dp).height(52.dp).background(Hairline))
            }
            SnapshotCell(home.snapshotAt(index), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SnapshotCell(amount: HomeAmountModel, modifier: Modifier) {
    val valueColor = if (amount.tone == Tone.POSITIVE) AiColors.Success else AiColors.Text
    Column(modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(amount.label, color = AiColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(amount.amount, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(
            amount.caption,
            color = AiColors.Muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeMoveCard(move: HomeMoveModel, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .softCard()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconBubble(move.icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(move.title, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(4.dp))
            Text(move.body, color = AiColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            Text("${move.cta}  →", color = AiColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        ImpactPill(move.impact)
    }
}
