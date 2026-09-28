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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicfo.app.theme.AiColors
import com.aicfo.shared.domain.AiCfoController
import com.aicfo.shared.presentation.MoveRowModel
import com.aicfo.shared.presentation.MoveStatusCode

@Composable
fun MovesScreen(
    controller: AiCfoController,
    tick: Int,
    selectedId: String?,
    onOpen: (String) -> Unit,
) {
    val board = remember(tick) { controller.moves() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(board.title, color = AiColors.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            Spacer(Modifier.height(4.dp))
            Text(board.subtitle, color = AiColors.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                board.summaryPill,
                color = AiColors.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(AiColors.AccentSoft)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("${board.todoCount}", "To do", Modifier.weight(1f))
                StatCard("${board.doneCount}", "Done", Modifier.weight(1f))
                StatCard("${board.skippedCount}", "Skipped", Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }
        items(board.moves, key = { it.id }) { row ->
            MoveRow(row, selected = row.id == selectedId) { onOpen(row.id) }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.softCard(radius = 20.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = AiColors.Text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = AiColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun MoveRow(row: MoveRowModel, selected: Boolean, onClick: () -> Unit) {
    val bg = when {
        selected -> AiColors.AccentSoft
        row.status == MoveStatusCode.DONE -> AiColors.DoneCard
        else -> AiColors.Card
    }
    Row(
        Modifier
            .fillMaxWidth()
            .softCard(color = bg)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.showCheck) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AiColors.SuccessSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = "Done", tint = AiColors.Success, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AiColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text("${row.rank}", color = AiColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title, color = AiColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaPill(row.priority, row.priority)
                MetaPill(row.statusLabel, row.status)
            }
        }
        Spacer(Modifier.size(8.dp))
        ImpactPill(row.impact, row.impactTone)
    }
}
